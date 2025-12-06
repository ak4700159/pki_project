package practice;

public class MyThread {
    public static void main(String args[]){
        Runnable task = new Task();
        Thread subTread1 = new Thread(task);
        Thread subTread2 = new Thread(task);
        subTread1.start();
        subTread2.start();
    }
}

class Task implements Runnable {
    @Override
    public void run() {
        int sum = 0;
        for (int index = 0; index < 10; index++) {
            sum += index;
            System.out.println(sum);
        }
        System.out.println( Thread.currentThread() + "최종 합 : " + sum);
    }

}