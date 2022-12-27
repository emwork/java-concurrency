package ca.skylinedata.javatips.concurrency.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConcurrentMapExample {
	
	public static void main(String[] args) throws InterruptedException{
		log.info("ConcurrentMapExample start");
		
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		// we can increment and decrement value 1000_000 times and the end result should be the initial value, right?
		// let's test
		log.info("Plain Map returns inconsistent result: {}", getAndPutConcurrently(new HashMap<>(), executor, 1000_000));
		log.info("ConcurrentMap returns consistent result (0):{}", getAndPutConcurrently(new ConcurrentHashMap<>(), executor, 1000_000));

		// ConcurrentHashMap is an implementation of a fully concurrent map
		// If a set is needed - Concurrent Hash Set are implemented via ConcurrentHashMap.newKeySet()
		
		// There's also ConcurrentSkipListMap - an implementation that doesn't reply on any synchronization, uses AtomicReference operations
		// it's based on a SkipList structure, that was used to create LinkedList

		log.info("ConcurrentSkipListMap returns consistent result (0):{}", getAndPutConcurrently(new ConcurrentSkipListMap<>(), executor, 1000_000));
		
		// use ConcurrentSkipListSet concurrently: one thread adds items to the set from N to 0, the other moves in the opposite direction
		// where the threads overlap - the middle set of the set will get removed concurrently
		// the number of the remaining items will be consistently = initial size - # of removed
		int initialSize = 3000;
		log.info("ConcurrentSkipListSet example:");
		log.info("# of removed items is consistently equal to initial-remaining: {}", 
				initialSize - addAndRemoveConcurrently(new ConcurrentSkipListSet<String>(), executor, initialSize));
		
		executor.shutdown();
		
	}
	
	
	static int getAndPutConcurrently(Map<String, Integer> hm, ExecutorService executor, final int loops) throws InterruptedException {

		hm.put("k1", 0);
		
		executor.submit(() -> {
			for (int i=loops; i>0; i--) {
				hm.compute("k1", (k,v) -> ++v);
			}
		});

		executor.submit(() -> {
			for (int i=loops; i>0; i--) {
				hm.compute("k1", (k,v) -> --v);
			}
		});

		log.info("Sleep main thread for 1 sec");
		TimeUnit.SECONDS.sleep(1);
		return hm.get("k1");
	}

	static int addAndRemoveConcurrently(Set<String> set, ExecutorService executor, final int loops) throws InterruptedException {

		executor.submit(() -> {
			for (int i=loops; i>0; i--) {
				set.add("item-" + i);
			}
		});

		executor.submit(() -> {
			long itemCounter = 0;
			for (int i=0; i<loops; i++) {
				if (set.contains("item-"+i)) {
					itemCounter++;
					set.remove("item-" + i);
				}
			}
			log.info("items matched and removed by the reader thread:     " + itemCounter);
		});

		log.info("Sleep main thread for 1 sec");
		TimeUnit.SECONDS.sleep(1);
		return set.size();
	}
}
