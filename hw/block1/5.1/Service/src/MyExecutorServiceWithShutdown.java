import java.util.*;
import java.util.concurrent.Callable;

class MyExecutorServiceWithShutdown {

    private final MyExecutorService service;

    private enum State {
        Created,
        AcceptingTasks,
        UnderShutdown,
        Terminated
    }

    private State state;
    private final Set<MyFuture<?>> runningTasks = new HashSet<>();
    private final List<Callable<?>> waitingTasks = new LinkedList<>();


    public MyExecutorServiceWithShutdown(MyExecutorService service) {
        this.service = service;
        this.state = State.AcceptingTasks;
    }

    /**
     * Forwarder to `this.service.submit`.
     *
     * @throws IllegalArgumentException if user tries to submit task after `shutdown`.
     */
    public synchronized <T> MyFuture<T> submit(Callable<T> task) throws IllegalArgumentException {
        if (state == State.UnderShutdown || state == State.Terminated) {
            throw new IllegalArgumentException("Executor already shutdown");
        }
        waitingTasks.add(task);
        MyFuture<T>[] temp = new MyFuture[1];
        MyFuture<T> future = service.submit(() -> {
            try {
                return task.call();
            } finally {
                synchronized (this) {
                    runningTasks.remove(temp[0]);
                }
                finishTasks();
            }
        });
        waitingTasks.remove(task);
        temp[0] = future;
        runningTasks.add(future);
        return future;
    }

    private synchronized void finishTasks() {
        runningTasks.removeIf(MyFuture::isDone);
        if (state == State.UnderShutdown && runningTasks.isEmpty()) {
            state = State.Terminated;
            notify();
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * <p>
     * This method does not wait for previously submitted tasks to complete execution. Use `awaitTermination` to do that.
     */
    public synchronized void shutdown() {
        if (state == State.AcceptingTasks) {
            state = State.UnderShutdown;

            if (runningTasks.isEmpty()) {
                state = State.Terminated;
                notify();
            }
        }
    }

    /**
     * Returns true if this executor has been shut down.
     * <p>
     * True does not mean all submitted tasks has been completed. Use `isTerminated` to check that.
     */
    public synchronized boolean isShutdown() {
        return state == State.UnderShutdown || state == State.Terminated;
    }

    /**
     * Returns true if all tasks have completed following shut down.
     * Note that isTerminated is never true unless either shutdown or shutdownNow was called first.
     */
    public synchronized boolean isTerminated() {
        return state == State.Terminated;
    }

    /**
     * Forbids submission of new tasks (equivalent to `shutdown`), halts the processing of waiting tasks and
     * returns a list of the tasks that were awaiting execution.
     * <p>
     * This method does not wait for actively executing tasks to terminate. Any already executing task **will not** be returned
     * by this method. Use `awaitTermination` to ensure all tasks are finished.
     */
    public synchronized List<Callable<?>> shutdownNow() {
        shutdown();
        List<Callable<?>> notReady = new ArrayList<>(waitingTasks);
        waitingTasks.clear();
        if (runningTasks.isEmpty()) {
            state = State.Terminated;
            notify();
        }
        return notReady;
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request.
     */
    public synchronized boolean awaitTermination() {
        while (state != State.Terminated) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}