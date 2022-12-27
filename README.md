# Concurrent collections

When considering concurrent collections and maps it's important to know your workload.
While there's no one solution that solves all the problems, and there are pros and cons in every approach.
For example, the number of threads could be in low numbers - 3, 5, 10, 20 - as opposed to running tens of thousands threads, and obviously approach cannot be the same.
Or you may have a large vs low count of the concurrently worked on objects.
If you use synchronization and you have a low count of objects you will be blocking too many of your threads.
If you use copy-on-write and you have a spike in writes, you will have high memory consumption, high CPU and high GC activity.

[See examples: Concurrency basics](#concurrency-basics)

[See examples: Java concurrent collections implementation](#java-concurrent-collections-implementation)


### Copy-on-write

Copy-on-write will not use locking at all for reads, and there are structures for lists and sets.
The writes will simply create new objects and synchronously update references to the new objects from the old ones (the threads that are actively working with the old objects will not see the change though, only the new reads will see the new objects)
This works extremely well for the applications that have very few writes, but a ton of reads - for example reading app config that rarely changes.
Java classes that implement copy-on-write:
	CopyOnWriteArrayList<?>
	CopyOnWriteArraySet<?>

[See examples: Java concurrent collections implementation](#java-concurrent-collections-implementation)

### Queues and double-ended queues
In concurrency situations you cold use Queue, Deque, ArrayBlockingQueue, ConcurrentLinkedQueue - depending on your application load.

- ArrayBlockingQueue (FIFO) is implemented in a way that it's not extendable, since it's backed by a fixed size array
- ConcurrentLinkedQueue (FIFO) is on the other hand extendable/unbounded
- Queue can be seen as a FIFO structure
- Deque is both FIFO and LIFO (or a stack), and it should be noted that there's no "pure" stack implementation in Java. (java.util.Stack is old and inefficient)

When considering how to handle a full blocking queue recall that there are these methods:
- add (will throw IllegalStateException if the queue is full)
- put (will block, waiting for space to become available if the queue is full) 
- offer (will return false if the capacity had been reached) 
- offer with timeout (will return false after timeout, if the capacity had been reached)	

When the blocking queue is empty and you are trying to get an element - these are approaches you can take to handle this situation:
- poll() will return null if the queue is empty
- peek() will return null if the queue is empty
- remove() will throw NoSuchElementException - if this queue is empty
- element() will throw NoSuchElementException - if this queue is empty
- take() will block if the queue is empty

Deque is implemented by ConcurrentLinkedDeque and LinkedBlockingDeque
 - these have methods to add/remove elements from the hear or tail

[See examples: Java concurrent collections implementation](#java-concurrent-collections-implementation)
 

### Concurrent Map, Set
All ConcurrentMap operations are thread-safe, retrieval operations do not entail locking, and there isn't any support for locking the entire table in a way that prevents all access

Get can overlap with put and remove, no blocking will occur. You will simply get the result of the most recently computed operation (put/delete)

ConcurrentMap operations are atomic, e.g.
 - putIfAbsent(arg0, arg1);
 - remove(arg0), (same as removeIfPresent)
 - replace(k, v)
 - replace(k, oldV, newV)
		
Also: ConcurrentHashMaps support a set of sequential and parallel bulk operations that are designed to be safely, and often sensibly, applied even with maps that are being concurrently updated by other threads. 

Parallel methods are:
 - search(parallelismThreshold, searchFunction)
 - reduce(parallelismThreshold, transformer, reducer)
 - forEach(parallelismThreshold, biConsumer);
 

Concurrent hash sets are implemented via ConcurrentHashMap.newKeySet()

ConcurrentSkipListMap is another implementation of ConcurrentMap. It's a fully concurrent map, with its keys sorted, based on a SkipList structure (used to create LinkedList).  
Insertion, removal, update, and access operations safely execute concurrently by multiple threads.  
This implementation doesn't reply on any synchronization, it uses AtomicReference operations.  
ConcurrentSkipListMap is useful when iteration order is important, since ConcurrentHashMaps doesn't guarantee the order.

There's also a ConcurrentSkipListSet, based on a SkipList structure. Behavior and implementation is similar to ConcurrentSkipListMap.

[See examples: Java concurrent collections implementation](#java-concurrent-collections-implementation)

### CyclicBarrier
CyclicBarrier is a concurrency structure that allows multiple threads to run until a certain point (a barrier) and then wait for each other until the whole group has reached the barrier before continuing.
The call to the await() method is that dividing point - and it allows threads to either wait indefinitely or until a timeout has been reached.
The barrier can throw a TimeoutException and enter a "broken" state, notifying other parties of the state change.
It's possible to have a callback executed once the barrier opens, executing a specific task before the "after-the-barrier-point" code continues.
The barrier is called cyclic because it can be re-used after the threads waiting for it to open are released. The barrier opens to release the threads and then closes.

[See examples: Locks, Semaphores, Barriers and Producer-Consumer implementation](#locks-semaphores-barriers-and-producer-consumer-implementation)

### Semaphore 
Semaphore is a concurrency structure similar to the CyclicBarrier, but much simpler.
It allows creating a limited number of the pass-through permits, these can be concurrently acquired, and - after doing some work - released for other threads to be acquired in turn. These permits can be used to limit access to a concurrently used resource.

[See examples: Locks, Semaphores, Barriers and Producer-Consumer implementation](#locks-semaphores-barriers-and-producer-consumer-implementation)


### Concurrency alternative: Async solutions with CompletableFuture, CompletionStage

CompletableFuture adds the following benefits to the regular Future construct:
- Exception handling
- Chaining and combining futures together in pipeline-like flows (performing async computations that depend on the other async computations)
- Attaching a callback that gets executed when the future is completed
- java.util.concurrent.CompletionStage is implemented by CompletableFuture and allows performing further actions after other(s) CompletionStage/CompletableFuture completes


For example, to attach a callback to the future we would use CompletableFuture.thenAccept(Consumer<? super T>) if your Supplier that was passed to supplyAsync() returns T.

You can also chain multiple callbacks, using thenApply like so:
CompletableFuture.supplyAsync(this::mySupplyFunction)
        		.thenApply(s  -> {log.info("Step 2: gets mySupplyFunction result {}", s); return 5; })
        		.thenAccept(s ->  log.info("Step 3: gets step's two result (integer 5): {}", s));
        		
The CompletableFuture's methods that end in "Async" (e.g. thenApplyAsync) will run each stage on a different thread, whereas thenApply, thenAccept etc. will continue on the thread where supplyAsync started.

[See examples: Async execution with CompletableFuture](#async-execution-with-completablefuture)


-----------------------------------------------


## Explanation of the code examples

### Concurrency basics
1. ca.skylinedata.javatips.concurrency.basics.**Callables** - this example covers Java Callable, ExecutorService, Future
1. ca.skylinedata.javatips.concurrency.basics.**Runnables** - this example covers Java Runnable, ExecutorService, Future, CountDownLatch
1. ca.skylinedata.javatips.concurrency.basics.**ExecutorsServiceDemo** - this example covers Java Runnable, ExecutorService
1. ca.skylinedata.javatips.concurrency.basics.**Casing** - this example covers Java Atomic classes that implement Compare-And-Swap design patterns


### Locks, Semaphores, Barriers and Producer-Consumer implementation
1. ca.skylinedata.javatips.concurrency.locks.**ReentrantLockDemo** - this example covers ReentrantLock, ExecutorService
1. ca.skylinedata.javatips.concurrency.locks.**CacheWithReadWriteLock** - this example covers ReadWriteLock, ExecutorService
1. ca.skylinedata.javatips.concurrency.locks.**CyclicBarrierTasks** - this example covers CyclicBarrier, ExecutorService
1. ca.skylinedata.javatips.concurrency.locks.**CyclicBarrierExceptionHandling** - this example covers CyclicBarrier, ExecutorService, Runnable, Future, BrokenBarrierExceptions
1. ca.skylinedata.javatips.concurrency.locks.**SemaphoreTasks** - this example covers Semaphore, ExecutorService
1. ca.skylinedata.javatips.concurrency.locks.**ProducerConsumerWithLocks** - this example covers Locks, Conditions, Callable, ExecutorService


### Java concurrent collections implementation
1. ca.skylinedata.javatips.concurrency.collections.**ConcurrentMapExample** - this example covers ConcurrentHashMap, ConcurrentSkipListMap, ConcurrentSkipListSet
1. ca.skylinedata.javatips.concurrency.collections.**QueueDequeExample** - this example covers ArrayBlockingQueue, ConcurrentLinkedQueue, Deque
1. ca.skylinedata.javatips.concurrency.collections.**CopyOnWriteExample** - this example covers CopyOnWriteArrayList used in in high concurrency situations
1. ca.skylinedata.javatips.concurrency.collections.**ConsumerProducerWithBlockingQueue** - this example covers ArrayBlockingQueue, Callable. ExecutorService

### Async execution with CompletableFuture
1. ca.skylinedata.javatips.async.**CompletableFutureExample** is a demo that uses CompletableFuture to define multi-threaded order processing pipeline
1. ca.skylinedata.javatips.async.**CompletingFutureExample** is a CompletableFuture example how we can force-complete a future if we decide so. Something we couldn't do with a regular Future


## How to run
- You can checkout the project with git clone command and open it in your favourite IDE, then run each demo as necessary, adjusting code and seeing the results
- Or you can run from the command line using maven exec plugin, like so:

```
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.basics.Callables"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.basics.Runnables"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.basics.ExecutorsServiceDemo"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.basics.Casing"

mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.ReentrantLockDemo"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.CacheWithReadWriteLock"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.CyclicBarrierTasks"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.CyclicBarrierExceptionHandling"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.SemaphoreTasks"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.locks.ProducerConsumerWithLocks"

mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.collections.ConcurrentMapExample"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.collections.QueueDequeExample"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.collections.CopyOnWriteExample"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.concurrency.collections.ConsumerProducerWithBlockingQueue"

mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.async.CompletableFutureExample"
mvn exec:java -Dexec.mainClass="ca.skylinedata.javatips.async.CompletingFutureExample"
```
