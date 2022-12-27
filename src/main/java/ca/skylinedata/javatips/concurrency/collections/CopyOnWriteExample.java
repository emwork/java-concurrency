package ca.skylinedata.javatips.concurrency.collections;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CopyOnWriteExample {

	public static void main(String[] args) {
		
		// Collections in high concurrency situations: we need to keep in mind that 
		// 25 threads are not the same as 25K - and there are different solutions for each load
		// There's a copy-on-write structure for lists and sets
		// For read it uses no locking at all 
		// But the writes create new structure, and re-point old to new in a synchronized way
		// This makes these structures extremely performant when there are tons of reads and the number or writes is very low, e.g. when storing a platform configuration

		log.info("Starting CopyOnWriteExample, to demonstrate a typical approach to this type of structure");
		
		CopyOnWriteArrayList<String> al = new CopyOnWriteArrayList<>();
		// CopyOnWriteArraySet<?> s = new CopyOnWriteArraySet<>(); // a CopyOnWrite set implementation is also available for your needs 
		
		al.add("resolution:3840 x 2160");
		al.add("rate:120");
		al.add("bluetooth:active");
		
		AtomicBoolean continueRunning = new AtomicBoolean(true);
		
		int threads = 100; // if you are running on a huge server - set it to 5 or 10 K and see: all the threads created, working very fast with the arraylist
		ExecutorService executorService = Executors.newFixedThreadPool(threads+1);
		log.info("Creating {} threads that will run in a thread pool of {}", threads, threads+1);
		for (int i=threads; i>0; i--) {
			executorService.submit(() -> {
				while (continueRunning.get()) {
					 al.forEach(e -> {
						// do something with the element... 
						try {
							TimeUnit.MILLISECONDS.sleep(50);
						} catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					 });
				}
			});
		}

		log.info("Creating a single thread that will run in a thread pool of {}, it will write to CopyOnWriteArrayList, rarely", threads+1);
		executorService.submit(() -> {
			// sometimes we make some writes to the CopyOnWriteArrayList
			for (int i=3; i>0; i--) {
				try {
					TimeUnit.SECONDS.sleep(4);
					al.add("new entry #" + i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		// let it run for a few seconds
		log.info("Letting these threads go through the CopyOnWriteArrayList elements for 20 seconds");
		try {
			TimeUnit.SECONDS.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		log.info("Stopping the threads");
		continueRunning.set(false);
		
		executorService.shutdown();
		
		// print the ArrayList elements
		al.forEach(e -> log.info("ArrayList item: {}", e));
		
		log.info("Cleaned up... Done.");
		
	}	
	
	
	
	
}
