package com.sherlocky.interview.javase.singleton;

/**
 * 懒汉（双重校验锁）实现单例
 * @author zhangcx
 * @date 2018-05-23
 */
public class LazySingleton {

    /**
     * 引用类型使用双重检查锁有可能会失效，是因为指令重排造成的。
     * 直接原因也就是 初始化一个对象并使一个引用指向他这个过程不是原子的。
     * 导致了可能会出现引用指向了对象并未初始化好的那块堆内存，
     * 此时，使用 volatile 修饰对象引用，防止重排序即可解决。
     */
    private static volatile LazySingleton instance;
    // 对于 ``long`` 和 ``double`` 的基本类型，双重检查模式仍然是适用的
    // private static long instance;
    
    // 私有构造方法
    private LazySingleton() {}
   
    // 双重校验锁
    public static LazySingleton getInstance() {
        if (instance == null) {
            synchronized (LazySingleton.class) {
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}