package ca.skylinedata.javatips.concurrency.locks;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class CacheWithReadWriteLock {

    private Map<Long, String> cache = new HashMap<>();

    // * HashMap is not thread-safe, so concurrent access from several threads will cause a race condition.
    // * race condition can be prevented by managing the cache access via read/write locks
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();


    public static void main(String[] args) throws InterruptedException {
        log.info("Cache:");

        CacheWithReadWriteLock cache = new CacheWithReadWriteLock();


        class Producer implements Callable<String> {
            private Random rand = new Random();

            public String call() {
                int i = 10_000;
                while (i-- > 0) {
                    long k = rand.nextInt(10_000);
                    cache.put(k, ""+k);
                    if (null == cache.get(k)) {
                        log.warn("The key {} hasn't been put in cache", k);
                    }
                }
                return "producer done, cache {}" + cache.size();
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        log.info("Putting values");
        try {
            for (int i=0; i<4; i++) {
                executorService.submit(new Producer());
            }
        } finally {
            executorService.shutdown();
        }
        for (int i=0; i<10; i++) {
            Thread.sleep(1000);
            log.info("cache size: {}", cache.size());
        }

    }
    
    public String put(Long k, String v) {
        writeLock.lock();
        try {
            return cache.put(k,v);
        } finally {
            writeLock.unlock();
        }
    }

    public String get (Long k) {
        readLock.lock();
        try {
            return cache.get(k);
        } finally {
            readLock.unlock();
        }
    }

    public long size() {
        return cache.size();
    }

}
