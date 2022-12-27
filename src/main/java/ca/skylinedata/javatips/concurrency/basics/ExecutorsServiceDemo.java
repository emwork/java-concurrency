package ca.skylinedata.javatips.concurrency.basics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ExecutorsServiceDemo {

    public static void main(String[] args) {
        log.info("Defining lambda runnable ");
        Runnable r = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            log.info("Runnable running after 1 sec");
        };

        // one way is to create a new thread every time we need to execute a task
        // Thread t = new Thread(r);
        // t.start();

        // or - more efficient - create an executor and reuse the thread many times
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(r);
        // the next task will be waiting in a queue, order is kept strict
        executor.execute(r);
        executor.shutdown();
        log.info("There is a 1-second gap between the two thread executions since we used SingleThreadExecutor and the execution order is strict");

    }
}
