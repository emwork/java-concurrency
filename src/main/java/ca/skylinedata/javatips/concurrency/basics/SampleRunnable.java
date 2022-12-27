package ca.skylinedata.javatips.concurrency.basics;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class SampleRunnable implements Runnable {

    final CountDownLatch waitLatch;
    private Random random = new Random();
    private boolean running = true;

    public SampleRunnable(final CountDownLatch waitLatch) {
        this.waitLatch = waitLatch;
    }

    @SneakyThrows
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int i = 1;
        while (this.running) {
            log.info("Iteration {}, time in thread {} ms: ", i++, System.currentTimeMillis() - start);
            Thread.sleep(random.nextInt(200));
            waitLatch.countDown();
        }
    }

	public void stopRunning() {
		this.running = false;
	}

}
