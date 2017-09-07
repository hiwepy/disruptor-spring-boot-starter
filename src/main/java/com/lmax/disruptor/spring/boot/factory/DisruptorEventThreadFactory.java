package com.lmax.disruptor.spring.boot.factory;

import java.util.concurrent.ThreadFactory;

public class DisruptorEventThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r);
	}

}
