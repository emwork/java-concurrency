package ca.skylinedata.javatips.concurrency.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerProducerWithBlockingQueue {

	public static void main(String[] args) {
		log.info("Starting consumer/producer with a BlockingQueue");
		BlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);

		Callable<String> producer = () -> {
			int count = 0;
			while (count++ < 50) {
				queue.put("" + count);
			}
			return "Produced " +  (count-1);
		};

		Callable<String> consumer = () -> {
			int count = 0;
			while (count++ < 50) {
				queue.take();
			}
			return "Consumed " + (count-1);
		};
		
		List<Callable<String>> callables = new ArrayList<>();
		int producers = 5;
		for (int i=0; i < producers; i++) {
			callables.add(consumer);
			callables.add(producer);
		}
		ExecutorService executorService = Executors.newFixedThreadPool(producers * 2);
		try {
			List<Future<String>> futures = executorService.invokeAll(callables);
			futures.forEach(f -> {
				try {
					log.info("callable result: " + f.get());
				} catch (InterruptedException | ExecutionException e) {
					log.error("", e);
				}
			});
		} catch (InterruptedException e) {
			log.error("", e);
		} finally {
			executorService.shutdown();
		}
		
		
		
	}
	
}
