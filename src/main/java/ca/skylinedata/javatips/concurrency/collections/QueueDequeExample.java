package ca.skylinedata.javatips.concurrency.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueDequeExample {
	
	public static void main(String[] args) {
		log.info("Starting QueueDequeExample");

		// * Queue implementations are:
		// * ArrayBlockingQueue, ConcurrentLinkedQueue, DelayQueue, 
		// * LinkedBlockingQueue, LinkedTransferQueue, PriorityBlockingQueue, PriorityQueue etc.
		
		BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(2); // thread-safe, bounded by a fixed size array, not extendable
		Queue<String> fifo = new ConcurrentLinkedQueue<>(); // thread-safe, unbounded, extendable
		

		log.info("Queue capacity: {}", blockingQueue.remainingCapacity());
		// adding to a blocking queue that is full?
		log.info("Queue size {}, adding item: {}. Result: {}", blockingQueue.size(), "A", blockingQueue.add("A"));
		log.info("Queue size {}, adding item: {}. Result: {}", blockingQueue.size(), "B", blockingQueue.add("B"));
		
		// add (Exception), put (blocks), offer (return false), offer with timeout (return false after timeout)	

		// Will not throw an exception:
		log.info("Queue size {}, offering item: {}. Result: {}", blockingQueue.size(), "C1", blockingQueue.offer("C1"));

		// Will throw an exception:
		try {
			log.info("Queue size {}, adding item: {}. Result: {}", blockingQueue.size(), "C2", blockingQueue.add("C2"));
		} catch (IllegalStateException e) {
			log.error("Exception on adding element when the queue is full: {}", e.getMessage());
		}
		
		log.info("<--------------------------------------------->");

		fifo.add("A");
		fifo.add("B");
		log.info("Fifo Queue size {}, adding item: {}. Result: {}", fifo.size(), "C", fifo.add("C"));
		log.info("Fifo Queue size {}, removing item, FIFO item was: {}", fifo.size(), fifo.remove());
		log.info("Fifo Queue size {}, removing item, FIFO item was: {}", fifo.size(), fifo.remove());
		log.info("Fifo Queue size {}, removing item, FIFO item was: {}", fifo.size(), fifo.remove());

		// Now that the queue is empty, let's try removing an item

		// poll() and peak() will return null, remove() and element() will throw Exception.
		// take() will block 

		// This will not throw an exception:
		log.info("Fifo Queue size {}, removing item, FIFO item was: {}", fifo.size(), fifo.poll());
		// Will throw an exception:
		try {
			log.info("Fifo Queue size {}, removing item, FIFO item was: {}", fifo.size(), fifo.remove());
		} catch (NoSuchElementException e) {
			log.error("Exception on removing an element when the queue is empty!");
		}
		
		log.info("<--------------------------------------------->");
		
		// Deque implementations are:
		// ArrayDeque, ConcurrentLinkedDeque, LinkedBlockingDeque, LinkedList
		Deque<String> fifoAndLifo = new ArrayDeque<>(5);
		Deque<String> stack = new LinkedList<>();
		fifoAndLifo.addLast("One");
		fifoAndLifo.addLast("Two");
		fifoAndLifo.addLast("Three");
		fifoAndLifo.addLast("Four");
		log.info("Deque has {} elements, removing first: {}", fifoAndLifo.size(), fifoAndLifo.removeFirst());
		log.info("Now deque has {} elements, removing first: {}", fifoAndLifo.size(), fifoAndLifo.removeFirst());
		log.info("Now deque has {} elements, removing last: {}", fifoAndLifo.size(), fifoAndLifo.removeLast());
		log.info("Deque has {} element, remaining one is {}", fifoAndLifo.size(), fifoAndLifo.pollFirst());
		
		log.info("<--------------------------------------------->");
		stack.push("One");
		stack.push("Two");
		stack.push("Three");
		log.info("Stack has {} elements, first is {}", stack.size(), stack.pop());
		log.info("Now stack has {} elements, first is {}", stack.size(), stack.pop());
		
		// Note that the java.util.Vector and java.util.Stack are old and obsolete, we don't want to use them in the new code and we want to remove their use in the legacy code
		
	}

}
