import java.lang.Thread;

public class Case1 {

    public static void main(String[] args) throws Exception {
        Thread b = new Thread(() -> {
            System.out.println("It's thread b");
            throw new RuntimeException("Exception from thread b");
        });

        Thread a = new Thread(() -> {
            System.out.println("It's thread a");
            b.start();
            try {
                b.join();
            } catch (InterruptedException e) {
                System.out.println("Exception has been caught");
            };
        });

        a.start();
        a.join();
        System.out.println("It's thread main. Finished without exceptions");
    }
}
