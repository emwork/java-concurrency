package ca.skylinedata.javatips.concurrency.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SemaphoreTasks {
	
	
	public static void main(String[] args) {
		log.info("Starting Semaphore demo, as a set of drive-through passes to obtain for going through a limited number of lanes");

		int threads = 10;
		int passesAvailable = 2;
		boolean fairness = true; //semaphore will guarantee first-in first-out granting of permits under contention
		
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		Semaphore tollBooth = new Semaphore(passesAvailable, fairness);
		
		
		List<Future<String>> futures = new ArrayList<>();
		try {
			for (int i=0; i<threads; i++) {
				TouristCar f = new TouristCar(tollBooth, "Car #" + i);
				futures.add(executorService.submit(f));
			}
			log.info("{} tourist cars submitted for travelling, while only {} passes are available, now main thread will block until all the futures are done", threads, passesAvailable);
			futures.forEach(ff -> {
				try {
					// let's block the main thread, letting each of the futures to complete
					// log.debug("Blocking for future #{}",ff.hashCode());
					log.info("* Callable returned: {}",ff.get());
				} catch (InterruptedException | ExecutionException  e) {
					log.error("Exception while getting future result for " + ff.hashCode() + ":", e);
				}
			});
		} finally {
			log.info("executorService shutting down");
			executorService.shutdown();
		}
		
	}
	
	public static class TouristCar implements Callable<String> {

		private Semaphore tollBooth;
		private String title;
		
		public TouristCar(Semaphore tb, String title) {
			this.tollBooth = tb;
			this.title = title;
		}
		
		@Override
		public String call() throws Exception {
			long startTime = System.currentTimeMillis();
			log.info("===> {} - driving up to the Toll Booth", title);
			Random r = new Random();
			long drivingMs = 5000 + r.nextInt(3)*1000 + 100;
			Thread.sleep(drivingMs);
			
			//Wait until a permit is available
			log.info("     {} got there in {} ms, getting a permit", title, drivingMs);
			tollBooth.acquire();
			
			// Hold the permit (while other cars are waiting for the pass to free up), take some additional time to drive through and the release the permit
			Thread.sleep(drivingMs);
			tollBooth.release();
			
			long completionTime = System.currentTimeMillis() - startTime;
			long blockedTime = completionTime - drivingMs*2;
			log.info("<=== {} passed through the Booth in {} ms total ({} of them waiting driving and {} waiting for a permit)", title, completionTime, drivingMs*2, blockedTime);

			return title + " passed through";
		}
		
	}
}
