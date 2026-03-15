interface NonReentrantLock {
    void lock() throws IllegalMonitorStateException;
    void unlock() throws IllegalMonitorStateException;
}