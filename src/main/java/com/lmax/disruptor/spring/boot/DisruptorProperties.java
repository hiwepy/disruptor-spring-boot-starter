package com.lmax.disruptor.spring.boot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.lmax.disruptor.spring.boot.config.EventHandlerDefinition;

@ConfigurationProperties(DisruptorProperties.PREFIX)
public class DisruptorProperties {

	public static final String PREFIX = "spring.disruptor";

	/**
	 * Enable Disruptor.
	 */
	private boolean enabled = false;
	private int ringBufferSize = 1024;
	private int ringThreadNumbers = 4;
	private boolean multiProducer = false;
	 /**
     * 
     */
    private List<EventHandlerDefinition> handlerDefinitions = new ArrayList<EventHandlerDefinition>();
    
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isMultiProducer() {
		return multiProducer;
	}

	public void setMultiProducer(boolean multiProducer) {
		this.multiProducer = multiProducer;
	}

	public int getRingBufferSize() {
		return ringBufferSize;
	}

	public void setRingBufferSize(int ringBufferSize) {
		this.ringBufferSize = ringBufferSize;
	}

	public int getRingThreadNumbers() {
		return ringThreadNumbers;
	}

	public void setRingThreadNumbers(int ringThreadNumbers) {
		this.ringThreadNumbers = ringThreadNumbers;
	}

	public List<EventHandlerDefinition> getHandlerDefinitions() {
		return handlerDefinitions;
	}

	public void setHandlerDefinitions(List<EventHandlerDefinition> handlerDefinitions) {
		this.handlerDefinitions = handlerDefinitions;
	}
	
}