package com.cy.distributedlockdemo;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.CountDownLatch;

/**
 * @author congyang.guo
 */
public class DistributedLock {


    static int count = 50;

    public static void genarNo() {
        try {
            count--;
            System.out.println(count);
        } finally {

        }
    }


    public static void main(String[] args) throws Exception {

        //1 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        //2 通过工厂创建连接
        CuratorFramework cf = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
//                    .namespace("super")
                .build();
        //3 开启连接
        cf.start();
        int id = 2;

        //4 分布式锁
        final CountDownLatch countdown = new CountDownLatch(1);
        final InterProcessMutex lock = new InterProcessMutex(cf, "/lock" + id);
        for (int i = 0; i < 50; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        countdown.await();
                        //加锁
                        lock.acquire();
                        //-------------业务处理开始
                        genarNo();

                        //-------------业务处理结束
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            //释放
                            lock.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "t" + i).start();
        }

        countdown.countDown();
    }
}
