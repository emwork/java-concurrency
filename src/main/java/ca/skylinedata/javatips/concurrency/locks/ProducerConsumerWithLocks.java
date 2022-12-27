package ca.skylinedata.javatips.concurrency.locks;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ProducerConsumerWithLocks {
	
	final static int warehouseCapacity = 50;
	final static Random rand = new Random();

    public static void main(String[] args) {
        log.info("Creating producers and consumers: from assembly line - to limited capacity warehouse - to dealtership");

        // CarAssemblyLine builds cars and ships them to the Warehouse, Dealerships get cars from Warehouse and sell them
        // if the warehouse is full - it cannot accept any more cars and the carAssemblyLines will be blocked
        // if all the cars are sold out, and the warehouse is empty - the dealerships are blocked
        // it is important to make sure that there are no conflicts in the process: we don't want the same car to be sold by 2 different dealerships
        // and we don't want any cars to get lost when delivering them to the warehouse
        // this is concurrency application in our daily life

        List<CarAssemblyLine> carAssemblyLines = new ArrayList<>();
        Warehouse warehouse = new Warehouse(); // shared object
        List<CarDealership> dealerships = new ArrayList<>();
        
        for (int i=0; i<3; i++) {
            carAssemblyLines.add(new CarAssemblyLine(warehouse));
            dealerships.add(new CarDealership(warehouse));
        }

        List<Callable<String>> autoIndustry = new ArrayList<>();
        autoIndustry.addAll(carAssemblyLines);
        autoIndustry.addAll(dealerships);

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        // VALIDATION: the number of cars assembled and sold should be the same!
        try {
            List<Future<String>> futures = executorService.invokeAll(autoIndustry);
            futures.forEach(f -> {
                try {
                    log.info("Done, callable result = " + f.get());
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

    // Warehouse can accept only 50 cars at a time (warehouseCapacity)
    static class Warehouse {
        public List<Integer> parkingLot = new ArrayList<>();
    }
    static class WarehouseLock {
        public static Lock l = new ReentrantLock();
        public static Condition isEmptyCondition = l.newCondition();
        public static Condition isFullCondition = l.newCondition();
    }

    /**
     * Sell cars on one thread
     */
    static class CarDealership implements Callable<String> {

        private List<Integer> parkingLot;
        
    	CarDealership(Warehouse warehouse) {
    		parkingLot = warehouse.parkingLot;
    	}
    	
        @Override
        public String call() throws Exception {
            int count = 0;
            while (count++ < ProducerConsumerWithLocks.warehouseCapacity) {
                try {
                	// get the lock in order to access guarded parkingLot
                    WarehouseLock.l.lock();
                    while (isEmpty(parkingLot)) {
                        // wait
                        // parking the thread and releasing the l lock temporarily, waiting to get notified by a carAssemblyLine
                        WarehouseLock.isEmptyCondition.await();
                    }
                    // once the condition is met we re-gain the lock automatically and can continue
                    sleep(rand.nextInt(30));
                    parkingLot.remove(parkingLot.size() - 1);
                    // signal that the parkingLot is no longer full so that a carAssemblyLine can build more cars 
                    WarehouseLock.isFullCondition.signalAll();
                } finally {
                    WarehouseLock.l.unlock();
                }
                sleep(20); // the logs will be easier to read if we slow down a bit
            }
            log.info("<=== sold {} cars", count - 1);
            return "Sold cars: " + (count - 1);
        }

        private boolean isEmpty(List<Integer> parkingLot) {
            return parkingLot.isEmpty();
        }
    }

    /**
     * Assemble cars on another thread
     */
    static class CarAssemblyLine implements Callable<String> {

        private List<Integer> parkingLot;
        
        CarAssemblyLine(Warehouse warehouse) {
    		parkingLot = warehouse.parkingLot;
    	}
    	
        @Override
        public String call() throws Exception {
            int count = 0;
            while (count++ < ProducerConsumerWithLocks.warehouseCapacity) {
                try {
                	// get the lock in order to access guarded parkingLot
                    WarehouseLock.l.lock();
                    while (isFull(parkingLot)) {
                        // wait
                        // parking the thread and releasing the l lock, waiting to get notified by a dealership
                        WarehouseLock.isFullCondition.await();
                    }
                    // once the condition is met we re-gain the lock automatically and can continue
                    sleep(rand.nextInt(30));
                    parkingLot.add(1);
                    // signal that the parkingLot is no longer empty so that a dealership can sell more cars  
                    WarehouseLock.isEmptyCondition.signalAll();
                } finally {
                    WarehouseLock.l.unlock();
                }
                sleep(20); // the logs will be easier to read if we slow down a bit
            }
            log.info("===> assembled {} cars", count - 1);
            return "Assembled cars: " + (count - 1);
        }

        private boolean isFull(List<Integer> parkingLot) {
            return parkingLot.size() > ProducerConsumerWithLocks.warehouseCapacity - 1;
        }
    }
    
    static void sleep(int ms) {
    	try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
