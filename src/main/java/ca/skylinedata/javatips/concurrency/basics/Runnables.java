package ca.skylinedata.javatips.concurrency.basics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class Runnables {

    public static void main(String[] args) {
    	
        // Multi-threading: one way is to create a new thread every time we need to execute a task is to create an instance of Thread and start it:
        // Thread t = new Thread(r);
        // t.start();
    	
    	CountDownLatch waitLatch = new CountDownLatch(9);
    	SampleRunnable st = new SampleRunnable(waitLatch);
		
		Thread t = new Thread(st);
		t.start();
		
		try {
			waitLatch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		st.stopRunning();

		// or define a lambda runnable
        log.info("<=== Defined lambda runnable ");
        Runnable r = () -> {
            try {Thread.sleep(1000);} catch (InterruptedException e) { e.printStackTrace(); }
            log.info("Runnable lambda after 1 sec of sleep");
        };
        // start that thread
        (new Thread(r)).start();

        try {Thread.sleep(1100);} catch (InterruptedException e) { e.printStackTrace(); }

        // or - more efficient - create an executor and reuse the thread many times
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        log.info("<=== Submitting 2 lambda runnables to the executor");
        Future f1 = executor.submit(r);
        // the next task will be waiting queue, order is kept strict
        Future f2 = executor.submit(r);
        log.info("Future 1 done? {}", f1.isDone());
        log.info("Future 2 done? {}", f2.isDone());

        // we will no longer accept any new tasks
        executor.shutdown();
        try {
            executor.submit(r);
            log.info("This message will never be logged due to the lien above throwing an exception");
        } catch (RejectedExecutionException e) {
            log.info("<=== As expected: RejectedExecutionException. The executor is no longer available for the new tasks");
        }


        try {
            // let's await for 1.5 sec only, then let the main thread continue
        	// awaitTermination() blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
            executor.awaitTermination(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // one task is done by now
        log.info("Future 1 done now: {}", f1.isDone());
        // the other - not
        log.info("Future 2 done? {}", f2.isDone());

        if (f1.isDone() && !f2.isDone()) {
        	log.info("<=== As expected: future 1 is done while future 2 is not done yet");
        }
    }
}
