package com.lmax.disruptor.spring.boot.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChain;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChainResolver;
import com.lmax.disruptor.spring.boot.handler.chain.ProxiedHandlerChain;

/**
 * Disruptor 事件分发实现
 */
public class DisruptorEventHandler extends AbstractRouteableEventHandler<DisruptorEvent> implements EventHandler<DisruptorEvent>{
	

	public DisruptorEventHandler(HandlerChainResolver<DisruptorEvent> filterChainResolver) {
		super(filterChainResolver);
	}
	
	/*
	 * 责任链入口
	 */
	@Override
	public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) throws Exception {
		
		//构造原始链对象
		HandlerChain<DisruptorEvent> originalChain = new ProxiedHandlerChain();
		//执行事件处理链
		this.doHandler(event, originalChain);
		
	}

}

