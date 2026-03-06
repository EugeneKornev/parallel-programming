import java.lang.Thread;

public class SingleThread {
    public static void main(String[] args) throws InterruptedException {
        Thread a = new Thread(() -> {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        a.start();
        a.join();
    }
}
