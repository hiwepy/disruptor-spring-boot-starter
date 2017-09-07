package com.lmax.disruptor.spring.boot.handler;

import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChain;

public interface DisruptorHandler<T extends DisruptorEvent> {

	public void doHandler(T event, HandlerChain<T> handlerChain) throws Exception;
	
}
