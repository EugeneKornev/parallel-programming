/**
 * A reentrant lock implementation that uses a non-reentrant lock.
 */
class MyReentrantLock {

    private final NonReentrantLockFactory factory;
    private Thread owner;
    private int counter;
    private NonReentrantLock Lock;

    public MyReentrantLock(NonReentrantLockFactory factory) {
        this.factory = factory;
        this.owner = null;
        this.Lock = factory.create();
    }

    /**
     * Tries to acquire the lock. This happens only when it free.
     *
     * @return whether the lock was acquired or not
     */
    public boolean tryLock() {
        Lock.lock();
        Thread current = Thread.currentThread();
        try {
            if (owner == null) {
                owner = current;
                counter = 1;
                return true;
            }

            if (owner == current) {
                counter++;
                return true;
            }

        } finally {
            Lock.unlock();
        }
        return false;
    }


    /**
     * Acquires the lock
     */
    public void lock() {
        long dealy = 1;
        while (!tryLock()) {
            try {
                Thread.sleep(dealy);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            dealy = Math.min(dealy * 2, 512);
        }
    }


    /**
     * Releases the lock
     *
     * @throws IllegalMonitorStateException when non-owner thread tries to unlock
     */
    public void unlock() throws IllegalMonitorStateException {
        Lock.lock();
        Thread current = Thread.currentThread();
        try {
            if (owner == current) {
                if (--counter == 0) {
                    owner = null;
                }
            } else {
                throw new IllegalMonitorStateException("MyReentrantLock owned by " + owner
                        + " cannot be unlocked by " + current);
            }
        } finally {
            Lock.unlock();
        }
    }
}