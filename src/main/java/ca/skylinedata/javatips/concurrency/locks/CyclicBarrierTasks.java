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
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CyclicBarrierTasks {

	private static final int SLEEP_SECONDS_MAX = 13; // barrier will always reach open state since we have enough time for work
	// when the barrier should time out, entering broken state
	private static final int BARRIER_TIMEOUT = 30;
	
	
	public static void main(String[] args) {
		log.info("Starting Barrier demo, as a set of EOD reports that need to finish before the cutoff time");
		log.info("Random report preparation time will determine if EOD will be able to close (run this multiple times to see different scenarios, as long as you make SLEEP_SECONDS_MAX > UNFINISHED_REPORT_TIMEOUT)");

		// make sure to size the executor (>=3) and the reporters properly, otherwise you'll cause the barrier to break prematurely
		int threads = 3;
		int reporters = 3;
		
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		
		// we will have a callback executed when the barrier is opening
		Runnable barrierCallback = () -> log.info("<=== Barrier opening! ===>");		
		CyclicBarrier barrier = new CyclicBarrier(reporters, barrierCallback );
		
		List<Future<String>> futures = new ArrayList<>();
		try {
			for (int i=0; i<reporters; i++) {
				EodReport f = new EodReport(barrier, "Daily Transactions Report #" + i);
				futures.add(executorService.submit(f));
			}
			log.info("{} reports submitted for execution, now main thread will block until all the futures are done", reporters);
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
	
	public static class EodReport implements Callable<String> {

		private CyclicBarrier barrier;
		private String title;
		
		public EodReport(CyclicBarrier b, String title) {
			this.barrier = b;
			this.title = title;
		}
		
		@Override
		public String call() throws Exception {
			log.info("===> {} - starting report generation", title);
			Random r = new Random();
			long sleepMs = r.nextInt(CyclicBarrierTasks.SLEEP_SECONDS_MAX)*1000 + 100;
			log.info("     {} generation will take: {} ms", title, sleepMs);
			Thread.sleep(sleepMs);
			log.info("     {} just completed (generation time {} ms), waiting on others to complete", title, sleepMs);
			
			// Wait until all reporters have invoked await on this barrier - signaling they've generated their reports
			int arrivalIndex = barrier.await(CyclicBarrierTasks.BARRIER_TIMEOUT, TimeUnit.SECONDS);

			if (arrivalIndex == 0) {
				// index 0 means the last-to-arrive, and we delegate the task of logging the below message to that one thread 
				log.info("Last-to-arrive thread ({}): all details reports got generated, let's email them all at the same time!", title);
			}
			// here we can email the reports at the same time
			// ...
			return title + " generated, emailed all ok";
		}
		
	}
}
