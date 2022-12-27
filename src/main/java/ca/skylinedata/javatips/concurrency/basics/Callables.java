package ca.skylinedata.javatips.concurrency.basics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class Callables {

    public static void main(String[] args) {
        log.info("Defining lambda callable");
        Callable<String> c = () -> {
            Thread.sleep(1000);
            log.info("Collable running after 1 sec");
            return "result @" + System.currentTimeMillis();
        };

        // one way is to create a new thread every time we need to execute a task
        // Thread t = new Thread(r);
        // t.start();

        // or - more efficient - create an executor and reuse the thread many times
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> f1 = executor.submit(c);
        // the next task will be waiting queue, order is kept strict
        Future<String> f2 = executor.submit(c);
        log.info("Future 1 done? {}", f1.isDone());
        log.info("Future 2 done? {}", f2.isDone());

        // we will no longer accept any new tasks
        executor.shutdown();
        try {
            executor.submit(c);
            log.info("This message will never be logged due to the line above throwing an exception");
        } catch (RejectedExecutionException e) {
            log.info("As expected: RejectedExecutionException. The executor is no longer available for the new tasks");
        }


        try {
            // let's await for 1.5 sec only, then let the main thread continue
            executor.awaitTermination(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // one task is done by now
        log.info("Future 1 done now: {}", f1.isDone());
        // the other - not
        log.info("Future 2 done? {}", f2.isDone());

        try {
            log.info("Future 1 result: {}", f1.get());
            // this will take another 0.5 second to complete
            log.info("Future 2 result: {}", f2.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
