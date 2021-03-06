package com.sherlocky.interview.javase.foreach;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 阿里巴巴禁止在 foreach 循环里进行元素的 remove/add 操作
 * <p>
 * 但是如果在遍历过程中，不通过Iterator，而是通过集合类自身的方法对集合进行添加/删除操作。那么在Iterator进行下一次的遍历时，经检测发现有一次集合的修改操作并未通过自身进行，那么可能是发生了并发被其他线程执行的，这时候就会抛出异常，来提示用户可能发生了并发修改，这就是所谓的 fail-fast 机制。
 * </p>
 * 可参考： https://halo.sherlocky.com/archives/java#%E9%98%BF%E9%87%8C%E5%B7%B4%E5%B7%B4%E7%A6%81%E6%AD%A2%E5%9C%A8-foreach-%E5%BE%AA%E7%8E%AF%E9%87%8C%E8%BF%9B%E8%A1%8C%E5%85%83%E7%B4%A0%E7%9A%84-removeadd-%E6%93%8D%E4%BD%9C
 *
 * @author: zhangcx
 * @date: 2020/5/12 9:16
 * @since:
 */
public class ForeachRemoveTest {
    public static void main(String[] args) {
        // 1, 2
        List<String> list = newList();
        print(list);

        /**
         * 正例--直接使用 Iterator 进行操作
         */
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if ("1".equals(item)) {
                iterator.remove();
            }
        }
        print(list);

        /**
         * 如果是JDK1.8+，没有并发访问的情况下，可以使用：
         * Collection.removeIf(Predicate<? super E> filter)方法删除，使代码更优雅。
         */
        list = newList();
        list.removeIf(s -> "1".equals(s));
        print(list);

        /**
         * 正例--普通for循环删除
         */
        list = newList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("1")) {
                // 其实存在一个问题，那就是 remove 操作会改变 List 中元素的下标，
                // 可能存在漏删的情况
                list.remove(i);
            }
        }
        print(list);

        /**
         * 正例--使用 Java 8 中提供的 filter 过滤
         */
        List<String> userNames = new ArrayList<String>() {{
            add("Hollis");
            add("hollis");
            add("HollisChuang");
            add("H");
        }};
        // 把集合转换成流，对于流有一种 filter 操作， 可以对原始 Stream 进行某项测试，通过测试的元素被留下来生成一个新 Stream
        userNames = userNames.stream().filter(userName -> !userName.equals("Hollis")).collect(Collectors.toList());
        print(userNames);

        /**
         * 正例--直接使用 fail-safe 的集合类
         *
         * 在 Java 中，除了一些普通的集合类以外，还有一些采用了 fail-safe （安全失败）机制的集合类：``java.util.concurrent``包下容器。
         * 这样的集合容器在遍历时不是直接在集合内容上访问的，而是先复制原有集合内容，在拷贝的集合上进行遍历。
         * 由于迭代时是对原集合的拷贝进行遍历，所以在遍历过程中对原集合所作的修改并不能被迭代器检测到，
         *      所以不会触发 ConcurrentModificationException。
         *
         * 基于拷贝内容的优点是避免了 ConcurrentModificationException，但同样地，迭代器并不能访问到修改后的内容，
         * 即：迭代器遍历的是开始遍历那一刻拿到的集合拷贝，在遍历期间原集合发生的修改迭代器是不知道的。
         */
        ConcurrentLinkedDeque<String> userNamesDeque = new ConcurrentLinkedDeque<String>() {{
            add("Hollis");
            add("hollis");
            add("HollisChuang");
            add("H");
        }};
        for (String userName : userNamesDeque) {
            if (userName.equals("Hollis")) {
                userNamesDeque.remove();
            }
        }

        /** ------------------------------------------------------------------------- */

        /**
         * 反例
         * foreach 是语法糖，本质就是 while循环 + Iterator 的遍历:
         *
         * <ul>
         - modCount是ArrayList中的一个成员变量。它表示该集合实际被修改的次数。
         - expectedModCount 是 ArrayList中的一个内部类——Itr中的成员变量。expectedModCount表示这个迭代器期望该集合被修改的次数。其值是在ArrayList.iterator方法被调用的时候初始化的。只有通过迭代器对集合进行操作，该值才会改变。
         - Itr是一个Iterator的实现，使用ArrayList.iterator方法可以获取到的迭代器就是Itr类的实例。
         * </ul>
         */
        list = newList();
        int loopTime = 1;
        for (String item : list) {
            System.out.println("循环第 " + loopTime++ + " 次~");
            // 此时不报错,因为只循环了一次
            if ("1".equals(item)) {
                list.remove(item);
            }
        }
        print(list);

        list = newList();
        loopTime = 1;
        for (String item : list) {
            // 改为2 会报： java.util.ConcurrentModificationException，循环第3次时报错了
            /**
             * 1不报错，2报错的具体原因是要搞明白：循环体执行了几次？
             * 主要看 ArrayList子类Itr.hasNext()方法中【cursor!=size】这个条件决定的(注意:此处是!=而不是<)
             */
            /**
             * 其实只要在删除之后，立刻结束循环体，不要再继续进行遍历也可以避免报错
             */
            System.out.println("循环第 " + loopTime++ + " 次~");
            if ("2".equals(item)) {
                list.remove(item);
            }
        }
        /**
         * 【结论来了】：
         * 在单线程的情况下，只要你的ArrayList集合大小大于等于2(假设大小为n，即size=n)，你删除倒数第二个元素的时候，
         * cursor从0进行了n-1次的加一操作，size(即n)进行了一次减1的操作,所以n-1=n-1，即cursor=size。
         * 因为判断条件返回为false，虽然你的modCount变化了。但是不会进入下次循环，
         * 就不会触发modCount和expectedModCount的检查，也就不会抛出ConcurrentModifyException.
         */
        print(list);

        /**
         * 另外上述说的抛出ConcurrentModificationException主要是指ArrayList，
         * 如果使用 CopyOnWriteArrayList 集合（快照技术），则不会报错，不过也不建议使用 foreach 中直接删除。
         */
        list = new CopyOnWriteArrayList<String>();
        list.add("1");
        list.add("2");
        for (String item : list) {
            // 不报错
            if ("2".equals(item)) {
                list.remove(item);
            }
        }
        print(list);

        /** ------------------------------------------------------------------------- */

        /**
         * 正例--增强for循环其实也可以，需要特殊操作一下
         */
        userNames = new ArrayList<String>() {{
            add("Hollis");
            add("hollis");
            add("HollisChuang");
            add("H");
        }};
        /**
         * 如果我们非常确定在一个集合中，某个即将删除的元素只包含一个的话，
         * 比如对 Set 进行操作，那么其实也是可以使用增强 for 循环的，
         * 只要在删除之后，立刻结束循环体，不要再继续进行遍历就可以了，
         * 也就是说不让代码执行到下一次的 next 方法。
         */
        for (String userName : userNames) {
            if (userName.equals("Hollis")) {
                userNames.remove(userName);
                break;
            }
        }
        print(userNames);
    }

    private static List<String> newList() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        return list;
    }

    private static void print(List<String> list) {
        list.forEach(System.out::print);
        System.out.println();
    }
}
