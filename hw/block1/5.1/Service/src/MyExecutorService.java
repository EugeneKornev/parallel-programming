import java.util.concurrent.Callable;

interface MyExecutorService {
    /**
     * Submits a value-returning task for execution and returns a `MyFuture` representing the pending results of the task.
     * The `MyFuture`s `get` method will return the task's result upon successful completion.
     */
    <T> MyFuture<T> submit(Callable<T> task);
}