package ca.skylinedata.javatips.concurrency.basics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Casing {
	
	public static void main(String[] args) {
		// CASing is cheaper than locking with synchronization, but:
		// CASing is similar to optimistic locking - threads will keep trying to Compare and Swap operation until it succeeds
		// Thus under certain loads, e.g. if concurrency is very high or threads are doing the updates at the very same time 
		//   - CASing will cause high CPU utilization and should not be used, 
		//   - while locking + synchronizing access to the memory will produce a better performance 
		//     (threads will wait for each other as opposed to keep retrying to C.A.S.)
		BrokenCounter.brokenDemo();
		AtomicCounter.goodDemo();
	}

	public static class BrokenCounter {
		
		// Broken example: Non-Atomic Counter, cannot handle race condition
		public static int counter = 0;

		public static void brokenDemo() {
			log.info("Demo: non-Compare-And-Swap");

			final int threads = 10;
			final int cycles = 100000;
			ExecutorService executorService = Executors.newFixedThreadPool(threads);
			List<Future<?>> futures = new ArrayList<Future<?>>();
			
			try {
				// Increment and decrement the counter 10000 times, hoping to get 0 at the end.
				// But this should fail in 99% of the runs due to the race condition, if your CPU is slow enough
				for (int j = 0; j < threads/2; j++) {
					futures.add(executorService.submit(() -> {
						for (int i = 0; i < cycles; i++) {
							BrokenCounter.counter++;
						}
					}));
					futures.add(executorService.submit(() -> {
						for (int i = 0; i < cycles; i++) {
							BrokenCounter.counter--;
						}
					}));
				}
				futures.forEach(f -> {
					try {
						f.get();
					} catch (InterruptedException | ExecutionException e) {
						log.error("", e);
					}
				});
				log.info("Corrupted Counter, very unlikely to be zero: {}", BrokenCounter.counter);
			} finally {
				executorService.shutdown();
				log.info("Executor shutdown");
			}
		}
	}

	public static class AtomicCounter {
		
		// Good example: Atomic Counter. 
		// We could have use AtomicInteger too.
		public static LongAdder counter = new LongAdder();

		public static void goodDemo() {
			log.info("Demo: Compare-And-Swap");

			final int threads = 10;
			final int cycles = 100000;
			ExecutorService executorService = Executors.newFixedThreadPool(threads);
			List<Future<?>> futures = new ArrayList<Future<?>>();
			
			try {
				// Increment and decrement the counter 10000 times, hoping to get 0 at the end.
				// This will work in 100% of the runs
				for (int j = 0; j < threads/2; j++) {
					futures.add(executorService.submit(() -> {
						for (int i = 0; i < cycles; i++) {
							AtomicCounter.counter.increment();
						}
					}));
					futures.add(executorService.submit(() -> {
						for (int i = 0; i < cycles; i++) {
							AtomicCounter.counter.decrement();
						}
					}));
				}
				futures.forEach(f -> {
					try {
						f.get();
					} catch (InterruptedException | ExecutionException e) {
						log.error("", e);
					}
				});
				log.info("C.A.S. Counter, will be zero: {}", AtomicCounter.counter.sum());
			} finally {
				executorService.shutdown();
				log.info("Executor shutdown");
			}
		}
	}
	

}
