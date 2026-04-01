import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class MyExecutorServiceWithShutdownTest {


    static class MyFutureImpl<T> implements MyFuture<T> {

        private final Future<T> future;

        public MyFutureImpl(Future<T> f) {
            this.future = f;
        }

        @Override
        public T get() throws ExecutionException {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionException(e);
            }
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }
    }

    static class MyExecutorServiceImpl implements MyExecutorService {

        private final ExecutorService executor;

        public MyExecutorServiceImpl(ExecutorService e) {
            this.executor = e;
        }

        @Override
        public <T> MyFuture<T> submit(Callable<T> task) {
            Future<T> future = executor.submit(task);
            return new MyFutureImpl<>(future);
        }
    }

    private final MyExecutorService executor = new MyExecutorServiceImpl(Executors.newFixedThreadPool(2));
    private final MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(executor);

    @Test
    void testSubmitSingleAndGetResult() throws ExecutionException {
        MyFuture<Integer> f = service.submit(() -> 42);
        assertEquals(42, f.get());
    }

    @Test
    void testSubmitMultipleAndGetResult() throws ExecutionException {
        MyFuture<Integer> a = service.submit(() -> {
            int x = 165;
            int y = 7;
            return x / y;
        });
        MyFuture<String> b = service.submit(() -> "Task" + "_B");
        assertEquals(23, a.get());
        assertEquals("Task_B", b.get());;
    }

    @Test
    void testProhibitSubmits() {
        service.shutdown();
        assertThrows(IllegalArgumentException.class, () -> service.submit(() -> {
            return null;
        }));
    }

    @Test
    void testIsShutdown() {
        assertFalse(service.isShutdown());;
        service.shutdown();
        assertTrue(service.isShutdown());
    }

    @Test
    void testAwaitTermination() {
        service.submit(() -> {
            Thread.sleep(100);
            return 1;
        });
        service.shutdown();
        assertTrue(service.awaitTermination());
        assertTrue(service.isTerminated());
        assertTrue(service.isShutdown());
    }

    @Test
    void testShutdownNow() {
        service.submit(() -> 10);
        var remaining = service.shutdownNow();
        assertNotNull(remaining);
    }

    @Test
    void testMultipleSubmit() throws InterruptedException {
        int N = 1000;
        final int K = 12;
        int[] numbers = new int[N];
        CountDownLatch latch = new CountDownLatch(N);
        for (int i = 0; i < N; i++) {
            final int j = i;
            service.submit(() -> {
                numbers[j] = K;
                latch.countDown();
                return null;
            });
        }
        latch.await();
        service.shutdown();
        service.awaitTermination();
        assertTrue(service.shutdownNow().isEmpty() && Arrays.stream(numbers).sum() == N * K);
    }

    @Test
    void testShutdownFromTask() throws InterruptedException {
        final int[] temp = new int[1];
        temp[0] = 0;
        CountDownLatch latch = new CountDownLatch(2);
        service.submit(() -> {
            temp[0] = 1;
            latch.countDown();
            return temp[0];
        });
        service.submit(() -> {
            service.shutdown();
            latch.countDown();
            return null;
        });
        latch.await();
        assertTrue(service.isShutdown());
    }

    @Test
    void testNonEmptyShutdownNow() throws InterruptedException {
        MyExecutorServiceWithShutdown s = new MyExecutorServiceWithShutdown(
                new MyExecutorServiceImpl(Executors.newFixedThreadPool(1)));
        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        s.submit(() -> {
            firstTaskStarted.countDown();
            Thread.sleep(1000);
            return null;
        });
        firstTaskStarted.await();
        for (int i = 0; i < 10; i++) {
            s.submit(() -> 42);
        }
        assertFalse(s.shutdownNow().isEmpty());
    }


    @Test
    void testAwaitInOtherThread() throws InterruptedException {
        MyFuture<?> f2 = service.submit(() -> {
            service.shutdown();
            service.awaitTermination();
            return null;
        });
        long N = 500;
        assertThrows(AssertionError.class, () -> {
            assertTimeoutPreemptively(Duration.ofMillis(N), () -> {
                service.awaitTermination();
            });
        });
    }

    @Test
    void testMultipleAwaitTerminationThreads() throws InterruptedException {
        long timeToSleep = 1000;
        service.submit(() -> {
            Thread.sleep(timeToSleep);
            return null;
        });
        service.shutdown();
        int N = 2;
        CountDownLatch threadsAwaitedTermination = new CountDownLatch(N);
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
               service.awaitTermination();
               threadsAwaitedTermination.countDown();
            }).start();
        }
        long M = timeToSleep + 100;
        assertTimeoutPreemptively(Duration.ofMillis(M), () -> {
            threadsAwaitedTermination.await();
        });
    }

    @Test
    void testMyFutureGet() throws InterruptedException {
        MyExecutorServiceWithShutdown s = new MyExecutorServiceWithShutdown(
                new MyExecutorServiceImpl(Executors.newFixedThreadPool(1)));

        CountDownLatch allTasksSubmitted = new CountDownLatch(1);
        CountDownLatch threadsJoined = new CountDownLatch(2);
        CountDownLatch serviceUnderShutdown = new CountDownLatch(1);

        new Thread(() -> { // Thread A
            for (int i = 0; i < 10; i++) {
                s.submit(() -> {
                    Thread.sleep(500);
                    return null;
                });
            }
            MyFuture<Integer> f = s.submit(() -> 43);
            allTasksSubmitted.countDown();
            try {
                serviceUnderShutdown.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                assertEquals(43, f.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            threadsJoined.countDown();
        }).start();

        new Thread(() -> { // Thread B
            try {
                allTasksSubmitted.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var l = s.shutdownNow();
            serviceUnderShutdown.countDown();
            threadsJoined.countDown();
        }).start();

        threadsJoined.await();
    }
}
