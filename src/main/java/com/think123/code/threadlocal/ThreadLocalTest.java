package com.think123.code.threadlocal;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;

public class ThreadLocalTest {

    @Test
    public void testSingeThread() throws InterruptedException {
        ThreadLocal threadLocal = new ThreadLocal();
        InheritableThreadLocal inheritableThreadLocal = new InheritableThreadLocal();
        threadLocal.set("threadLocal");
        inheritableThreadLocal.set("inheritableThreadLocal");

        String main = Thread.currentThread().getName();
        System.out.println("[" + main + "]threadLocal: " + threadLocal.get());
        System.out.println("[" + main + "]inheritableThreadLocal: " + inheritableThreadLocal.get());
        System.out.println();

        CountDownLatch cd = new CountDownLatch(1);

        new Thread(() -> {

            String name = Thread.currentThread().getName();
            System.out.println("[" + name + "]threadLocal: " + threadLocal.get());
            System.out.println("[" + name + "]inheritableThreadLocal: " + inheritableThreadLocal.get());
            inheritableThreadLocal.set("change inheritableThreadLocal");
            System.out.println("[" + name + "]inheritableThreadLocal: " + inheritableThreadLocal.get());

            cd.countDown();

        }).start();


        cd.await();

        System.out.println();
        System.out.println("[" + main + "]threadLocal: " + threadLocal.get());
        System.out.println("[" + main + "]inheritableThreadLocal: " + inheritableThreadLocal.get());
    }


    @Test
    public void testThreadPool() throws InterruptedException {
        InheritableThreadLocal inheritableThreadLocal = new InheritableThreadLocal();
        inheritableThreadLocal.set("aaa");

        String main = Thread.currentThread().getName();
        System.out.println("[" + main + "]: " + inheritableThreadLocal.get());
        System.out.println();

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CountDownLatch cd = new CountDownLatch(1);

        executorService.execute(new Thread(() -> {
            String name = Thread.currentThread().getName();
            System.out.println("[" + name + "]: " + inheritableThreadLocal.get());
            inheritableThreadLocal.set("bbb");
            System.out.println("[" + name + "]: " + inheritableThreadLocal.get());

            cd.countDown();

        }));

        cd.await();

        System.out.println();

        CountDownLatch cd2 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executorService.execute(new Thread(() -> {
                String name = Thread.currentThread().getName();
                System.out.println("[" + name + "]: " + inheritableThreadLocal.get());
                cd2.countDown();
            }));
        }

        cd2.await();


        System.out.println();
        System.out.println("[" + main + "]: " + inheritableThreadLocal.get());
        System.out.println();

        executorService.shutdown();
    }

    @Test
    public void testThreadPoolWithTransmittableThreadLocal() throws InterruptedException {
        TransmittableThreadLocal threadLocal = new TransmittableThreadLocal ();
        threadLocal.set("aaa");

        String main = Thread.currentThread().getName();
        System.out.println("[" + main + "]: " + threadLocal.get());
        System.out.println();

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CountDownLatch cd = new CountDownLatch(1);

        executorService.execute(TtlRunnable.get(() -> {
            String name = Thread.currentThread().getName();
            System.out.println("[" + name + "]:" + threadLocal.get());
            threadLocal.set("bbb");
            System.out.println("[" + name + "]:" + threadLocal.get());

            cd.countDown();
        }));

        cd.await();

        System.out.println();

        CountDownLatch cd2 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executorService.execute(TtlRunnable.get(() -> {
                String name = Thread.currentThread().getName();
                System.out.println("[" + name + "]:" + threadLocal.get());
                cd2.countDown();
            }));
        }

        cd2.await();


        System.out.println();
        System.out.println("[" + main + "]: " + threadLocal.get());
        System.out.println();

        executorService.shutdown();
    }



}
