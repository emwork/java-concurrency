package ca.skylinedata.javatips.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletingFutureExample {

	public static void main(String[] args) {
		
		CompletingFutureExample main = new CompletingFutureExample();
		
		// you can also try completingAfuture example by passing any argument to the CompletableFutureMain
		log.info("CompletableFuture: example of completing a future");
		main.completingAfuture();
		
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	// This is just an example how we can force-complete a future if we decide so. Something we couldn't do with a regular Future
	// ---------------------------------------------------------------------------------------------------------------------------
	public void completingAfuture() {

		ExecutorService executorService = Executors.newSingleThreadExecutor();

		// creating an incomplete future, will run with ForkJoinPool.commonPool
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
        	try {
            	log.info("Running a slowly-executing step");
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	// if the other thread doesn't complete my future I will return this:
        	return "Slowly-executing step that will make the other thread impatient";
        }); 

        String impatientCompletion = "Waited over 1 second. Enough!";
    	log.info(" ... set up some expected result: '{}'", impatientCompletion);
        Future<String> f = executorService.submit( () -> {
        	log.info("Running a separate thread that will wait 1 sec and then complete Slowly-executing Step's future");
            TimeUnit.SECONDS.sleep(1);
            completableFuture.complete(impatientCompletion); // completing the incomplete future
            return "Impatient thread done";
        });


        String result;
        int i = 1;
		try {
	        while (!completableFuture.isDone()) { // checking the future for completion
	        	log.info(" ... main thread checking the future for completion, attempt {}", i++);
	            TimeUnit.SECONDS.sleep(1);
	        }
			result = completableFuture.get();
	        log.info("got Slowly-executing step's completableFuture result: [{}]", result);
	        if (impatientCompletion.contentEquals(result)) {
		    	log.info("expected result confirmed:                            '{}'", result);
	        }
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} // reading value of the completed future
		
		try {
			log.info("By the way, the executorService also did its job: {}", f.get());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

        executorService.shutdown();

	}
	
}
