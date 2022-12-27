package ca.skylinedata.javatips.concurrency.locks;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ReentrantLockDemo {
	
	static int sharedCounter = 0;

    public static void main(String[] args) {
        log.info("Reentrant lock as a way of running a thread-safe piece of code");

        ReentrantLock lock = new ReentrantLock();
        /*
         * Below is a typical pattern when working with the ReentrantLock
        try {
            lock.lock();
            // lock.lockInterruptibly(); // we allow other threads to interrupt us while we wait for the lock to become available
            // lock.tryLock(); // 
            // ... do some thread-safe work
        } finally {
            lock.unlock();
        }
        */

        ExecutorService executor = Executors.newFixedThreadPool(9);
        executor.execute(new TryLockRunner(lock, "Runner-1"));
        executor.execute(new TryLockRunner(lock, "Runner-2"));
        executor.execute(new TryLockRunner(lock, "Runner-3"));
        executor.execute(new TryLockRunner(lock, "Runner-4"));
        executor.execute(new TryLockRunner(lock, "Runner-5"));
        
        // separate group of threads will use lock.lock() call and work with ReentrantLockDemo.sharedCounter
        executor.execute(new LockRunner(lock, "Incrementer-1"));
        executor.execute(new LockRunner(lock, "Incrementer-2"));
        executor.execute(new LockRunner(lock, "Decrementer-3"));
        executor.execute(new LockRunner(lock, "Decrementer-4"));
        executor.shutdown();
        
        try {
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        log.info("ReentrantLockDemo.sharedCounter was safely incremented and decremented by 4 threads, it should remain zero: {}", ReentrantLockDemo.sharedCounter);

    }

	static void sleep(int sleepMs) {
		try {
			TimeUnit.MILLISECONDS.sleep(sleepMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

    public static class TryLockRunner implements Runnable {
    	
    	private ReentrantLock lock;
    	private String name;
    	
    	public TryLockRunner (ReentrantLock lock, String name) {
    		this.lock = lock;
    		this.name = name;
    	}
    	
    	@Override
    	public void run() {
    		Random r = new Random();
    		int sleepMs = r.nextInt(1000);
    		log.info("{} running. Now will sleep for {} ms", name, sleepMs);
    		try {
    			ReentrantLockDemo.sleep(sleepMs);
	    		log.info("{} woke up. Now will try acquiring the lock", name);
	    		boolean gotLock = lock.tryLock(100, TimeUnit.MILLISECONDS);
	    		if (gotLock) {
	    			log.info("===> {} got the lock. {}'s lock hold count= {}", name, name, lock.getHoldCount());
	    			ReentrantLockDemo.sleep(sleepMs);
	    			lock.unlock();
	    			log.info("<=== {} released lock. {}'s lock hold count= {}", name, name, lock.getHoldCount());
	    		} else {
	    			log.info(" :-( {} - failed to get the lock. {}'s lock hold count= {}", name, name, lock.getHoldCount());
	    		}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}    		
    	}
    }
    
    public static class LockRunner implements Runnable {
    	
    	private ReentrantLock lock;
    	private String name;
    	
    	public LockRunner (ReentrantLock lock, String name) {
    		this.lock = lock;
    		this.name = name;
    	}
    	
    	@Override
    	public void run() {
    		Random r = new Random();
    		int sleepMs = r.nextInt(1000);
    		log.info("{} running. Now will sleep for {} ms", name, sleepMs);
			ReentrantLockDemo.sleep(sleepMs);
    		log.info("{} woke up. Now will try acquiring the lock", name);
    		
    		try {
	    		lock.lock();
	    		log.info("{} got the lock. {}'s lock hold count= {}", name, name, lock.getHoldCount());
	    		// work with some shared resource
	    		someThreadSafeWork(sleepMs); 
			} finally {
				lock.unlock();
    			log.info("{} released lock. {}'s lock hold count= {}", name, name, lock.getHoldCount());
			}
    		
    	}

    	// we will access a shared resource from multiple threads - so this method will need to run in a guarded section of the thread
    	private void someThreadSafeWork(int sleepMs) {
    		if (name.startsWith("Incrementer")) {
    			ReentrantLockDemo.sharedCounter++;
    		} else {
    			ReentrantLockDemo.sharedCounter--;
    		}
			log.info(">>> set counter to {}", ReentrantLockDemo.sharedCounter);
    		ReentrantLockDemo.sleep(sleepMs);
    	}
    	
    }
}
