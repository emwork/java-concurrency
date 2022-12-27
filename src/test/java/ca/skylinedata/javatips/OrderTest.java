package ca.skylinedata.javatips;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ca.skylinedata.javatips.async.CompletableFutureExample;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class OrderTest {

	@Test
	public void testOrderProcessingDuration() throws InterruptedException {
		
		long start = System.currentTimeMillis();
		
		CompletableFutureExample cfe = new CompletableFutureExample();
		CompletableFutureExample.StockOrder o =cfe.receiveOrderIn3Sec();
		o = cfe.validateOrderIn1Sec(o);
		o = cfe.executeOrderIn3Sec(o);
		cfe.sendConfirmationIn2Sec(o);
		
		long duration = System.currentTimeMillis() - start;		
 		log.info("Single-thread order processing took {} ms", duration);
 
 		assertTrue(duration >= 9000, "Expecting Single-thread order processing to take 3+1+3+2 = 9 seconds");
	}

}
