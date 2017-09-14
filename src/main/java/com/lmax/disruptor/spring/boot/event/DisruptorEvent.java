package com.lmax.disruptor.spring.boot.event;

import java.util.EventObject;

/**
 * 
 * @className	： DisruptorEvent
 * @description	： 事件(Event) 就是通过 Disruptor 进行交换的数据类型。
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 * @date		： 2017年4月20日 下午9:29:21
 * @version 	V1.0
 */
@SuppressWarnings("serial")
public abstract class DisruptorEvent extends EventObject {

	/** System time when the event happened */
	private final long timestamp;
	/** Route Expression*/
	private String routeExpression;
	
	/**
	 * Create a new ConsumeEvent.
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public DisruptorEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Return the system time in milliseconds when the event happened.
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

	public String getRouteExpression() {
		return routeExpression;
	}

	public void setRouteExpression(String routeExpression) {
		this.routeExpression = routeExpression;
	}
	
}
