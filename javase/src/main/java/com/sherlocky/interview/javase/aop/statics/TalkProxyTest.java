package com.sherlocky.interview.javase.aop.statics;

import com.sherlocky.interview.javase.aop.ITalk;
import com.sherlocky.interview.javase.aop.PeopleTalk;

public class TalkProxyTest {

	public static void main(String[] args) {
		// 不需要执行额外方法
		ITalk people = new PeopleTalk("sherlcok", "4 month");
		people.talk("唱歌");
		System.out.println("=====================");
		
		// 需要执行额外方法 (切面)
		ITalk talker = new TalkProxy(people);
		talker.talk("I am proxy~");
	}
	
}
