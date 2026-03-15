import org.junit.jupiter.api.Test;

import javax.swing.plaf.SpinnerUI;
import java.lang.reflect.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.jupiter.api.Assertions.*;

public class MyReentrantLockTest {

    NonReentrantLockFactory factory = new NonReentrantLockFactoryImpl();

    static class NonReentrantLockImpl implements NonReentrantLock {

        private final ReentrantLock Lock = new ReentrantLock();

        @Override
        public void lock() throws IllegalMonitorStateException {
            if (Lock.isHeldByCurrentThread()) {
                throw new IllegalMonitorStateException("Repeated lock on NonReentrantLock");
            }
            Lock.lock();
        }

        @Override
        public void unlock() throws IllegalMonitorStateException {
            if (!Lock.isHeldByCurrentThread()) {
                throw new IllegalMonitorStateException("Unlocking thread is not the owner");
            }
            Lock.unlock();
        }

    }

    static class NonReentrantLockFactoryImpl implements NonReentrantLockFactory {

        public NonReentrantLock create() {
            return new NonReentrantLockImpl();
        }

    }

    @Test
    public void testSingleLock() throws NoSuchFieldException, IllegalAccessException {
        MyReentrantLock l = new MyReentrantLock(factory);
        l.lock();
        l.unlock();

        Field field = MyReentrantLock.class.getDeclaredField("owner");
        field.setAccessible(true);
        assertNull(field.get(l));
    }

    @Test
    public void testDoubleLock() throws NoSuchFieldException, IllegalAccessException {
        MyReentrantLock l = new MyReentrantLock(factory);
        l.lock();
        l.lock();
        l.unlock();
        l.unlock();

        Field field = MyReentrantLock.class.getDeclaredField("owner");
        field.setAccessible(true);
        assertNull(field.get(l));
    }


    @Test
    public void testUnlockByOther() throws InterruptedException {
        MyReentrantLock l = new MyReentrantLock(factory);
        l.lock();
        Thread other = new Thread(() -> {
            assertThrows(IllegalMonitorStateException.class, l::unlock);
        });
        other.start();
        other.join();
        l.unlock();
    }

    @Test
    public void testMutualLocking() throws InterruptedException {
        MyReentrantLock l = new MyReentrantLock(factory);
        final int[] counter = {0};
        int threads = 8;
        int iterations = 16384;
        CountDownLatch ready = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    l.lock();
                    try {
                        counter[0]++;
                    } finally {
                        l.unlock();
                    }
                }
                ready.countDown();
            }).start();
        }
        ready.await();

        assertEquals(threads * iterations, counter[0]);
    }

}
