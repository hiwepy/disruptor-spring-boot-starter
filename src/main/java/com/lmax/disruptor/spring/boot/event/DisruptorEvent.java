package com.lmax.disruptor.spring.boot.event;

import java.util.EventObject;

@SuppressWarnings("serial")
public abstract class DisruptorEvent extends EventObject {

	/** System time when the event happened */
	private final long timestamp;
	
	private String ruleExpression;
	
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

	public String getRuleExpression() {
		return ruleExpression;
	}

	public void setRuleExpression(String ruleExpression) {
		this.ruleExpression = ruleExpression;
	}
	
}
