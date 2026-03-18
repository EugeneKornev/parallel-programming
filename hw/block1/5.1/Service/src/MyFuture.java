import java.util.concurrent.ExecutionException;

interface MyFuture<V> {
    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     * <p>
     * Returns:
     *   the computed result
     * <p>
     * Throws:
     *   ExecutionException - if the computation threw an exception
     *
     */
    public V get() throws ExecutionException;

    /**
     * Returns `true` if this task completed. Completion may be due to normal termination or
     * an exception -- in all of these cases, this method will return true.
     */
    public boolean isDone();
}

