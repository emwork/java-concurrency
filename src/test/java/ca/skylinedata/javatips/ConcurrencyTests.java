package ca.skylinedata.javatips;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ca.skylinedata.javatips.concurrency.basics.SampleRunnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
class ConcurrencyTests {

	@Test
	public void waitingForAtestThreadToComplete() throws InterruptedException {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		CountDownLatch waitLatch = new CountDownLatch(10);
		executorService.execute(new SampleRunnable(waitLatch));

		log.info("How do you make Junit 'wait' for a test thread to complete? Use a latch in JUnit main thread to wait up to - for example - 3 seconds");
		// Causes the current thread to wait until the latch has counted down to zero
		waitLatch.await(3000, TimeUnit.MILLISECONDS);
		log.info("try commenting out the line above to see that without it the test will terminate before the executor get a chance to run the submitted thread");
	}

}
