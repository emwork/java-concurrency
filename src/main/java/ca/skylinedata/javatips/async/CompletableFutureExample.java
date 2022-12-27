package ca.skylinedata.javatips.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletableFutureExample {

	public static void main(String[] args) {
		
		// 1. wait for an order to come = let's say it takes 3 seconds on avg
		// 2. validate the order and accept if passes validation = let's say it takes 1 second on avg
		// 3. wait for order to get executed = let's say it takes 3 seconds on avg
		// 4. send order execution confirmation to a user = let's say it takes 2 seconds on avg
		
		// if executed in a blocking manner, sequentially the steps would run on a single thread and would take 3+1+3+2=9 seconds
		// if 5 orders were submitted and executed in a blocking manner it would take 5*9=45 seconds
		
		// let's see if running some tasks in parallel can speed things up!
		CompletableFutureExample main = new CompletableFutureExample();
		
		log.info("CompletableFuture: executing trading orders");
		main.completableFutureExample();
		
	}
	
	
	
	public void completableFutureExample() {

		int orders = 10;
		int threads = 4;
		log.info("Let's process {} orders in {} threads. It takes about 9 sec to process an order sequentially.", orders, threads);
		
		long start = System.currentTimeMillis();
		
		// the (reasonably) more workers we get to participate in order processing steps - the faster it is due to work sharing!
		// but if you get only one worker - the flow will take 9 sec * # of orders
		// and if you get two workers - you will get some performance benefits of the async execution (about 40-50%)
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i=orders; i>0; i-- ) {
			// use default ForkJoinPool.commonPool()
			CompletableFuture<Void> orderProcessingPipeline = CompletableFuture.supplyAsync(this::receiveOrderIn3Sec, executor)
	        		.thenApplyAsync(stockOrder -> validateOrderIn1Sec(stockOrder), executor)
	        		.thenApplyAsync(stockOrder -> executeOrderIn3Sec(stockOrder), executor)
	        		.thenAcceptAsync(stockOrder -> sendConfirmationIn2Sec(stockOrder), executor);
        	futures.add(orderProcessingPipeline);
		}
		
        // note - we can still get some other work done in parallel - the main thread is not blocked
        someUsefulActivity();
        
        // we are going to use CountDown since we need an object with a counter. It doesn't need to be thread-safe since only the main thread uses it.
        CountDown countDown = new CountDown(orders);
        while (countDown.getCount() > 0) {
        	countDown.resetCount();
    		futures.stream().forEach(f -> {
    			if (f.isDone()) {
    				countDown.countDown();
    			}
    		});
    		if (countDown.getCount() > 0) {
    			log.info("            ... check: {} orders are still executing/sending confirmation. Will recheck in 2 sec.", countDown.getCount());
	            try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
        }      
        
        
        //now that we know all futures are done - we can shutdown the executor
        executor.shutdown();
        try {
            log.info("Awaiting for the executor to terminate");
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        log.info("Total execution time is {} ms, which is faster than {} ms of sequential order execution ({} orders x 9 sec each)",  
        		System.currentTimeMillis() - start, orders*9000, orders);

	}
	
	private void someUsefulActivity() {
		log.info("While the orders are getting processed, main thread will  do something useful for 6 seconds: catch up on sleep :-) ");
        try {
			TimeUnit.SECONDS.sleep(6);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	AtomicInteger orderId = new AtomicInteger(0);

	public StockOrder receiveOrderIn3Sec() {
        try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        StockOrder o = new StockOrder("Buy", 100, "IBM", 50, orderId.incrementAndGet());
		log.info(" >>>> order #{} received", o.id);
		return o;
	}

	public StockOrder validateOrderIn1Sec(StockOrder o) {
        try {
			TimeUnit.SECONDS.sleep(1);
			o.status = "ACCEPTED";
			log.info("order #{} validated OK", o.id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public StockOrder executeOrderIn3Sec(StockOrder o) {
        try {
			TimeUnit.SECONDS.sleep(3);
			o.status = "EXECUTED";
			log.info("order #{} executed", o.id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public void sendConfirmationIn2Sec(StockOrder o) {
        try {
			TimeUnit.SECONDS.sleep(2);
			log.info(" *** CONFIRMATION: {} {} order executed at ${} for order #{}", o.symbol, o.buySell, o.price, o.id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

	public void notifyAbout(String s) {
		log.info("Notification: {}", s);
	}
	
	public static class StockOrder {
		int qty;
		float price;
		String symbol;
		String buySell;
		String status = "NEW";
		int id;
		
		public StockOrder(String action, int qty, String symbol, float limitPrice, int id) {
			this.symbol = symbol;
			this.buySell = action;
			this.price = limitPrice;
			this.qty = qty;
			this.id=id;
		}
	}
	
	public static class CountDown {
		private int orders;
		private int count;
		public CountDown(int orders) {
			this.orders = orders;
			this.count = orders;
		}
		public void countDown() {
			count--;
		}
		public void resetCount() {
			this.count = orders;
		}
		public int getCount() {
			return count;
		}
	}
	

}
