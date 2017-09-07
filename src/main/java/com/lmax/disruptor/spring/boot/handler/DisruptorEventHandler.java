package com.lmax.disruptor.spring.boot.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChain;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChainResolver;

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
		
		HandlerChain<DisruptorEvent> originalChain = new HandlerChain<DisruptorEvent>() {
			
			@Override
			public void onEvent(DisruptorEvent event) throws Exception {
				
			}
			
		};
		
		this.doHandlerInternal(event, originalChain);
		
	}

}

