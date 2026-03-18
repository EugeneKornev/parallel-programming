import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    void testAwaitTermination() throws InterruptedException {
        service.submit(() -> {
            Thread.sleep(100);
            return 1;
        });
        service.shutdown();
//        Thread.sleep(100);
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
}
