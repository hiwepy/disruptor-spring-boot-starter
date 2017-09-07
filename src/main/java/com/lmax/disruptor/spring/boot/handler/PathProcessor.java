package com.lmax.disruptor.spring.boot.handler;

import com.lmax.disruptor.spring.boot.event.DisruptorEvent;

/**
 * 给Handler设置路径
 */
public interface PathProcessor<T extends DisruptorEvent> {
	
	DisruptorHandler<T> processPath(String path);

}
