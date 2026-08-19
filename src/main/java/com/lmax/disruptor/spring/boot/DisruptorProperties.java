package com.lmax.disruptor.spring.boot;

import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.config.EventHandlerDefinition;
import com.lmax.disruptor.thread.DisruptorThreadFactory;
import com.lmax.disruptor.thread.DisruptorWaitStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the LMAX Disruptor integration, bound to the
 * {@code spring.disruptor} prefix.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@ConfigurationProperties(DisruptorProperties.PREFIX)
@Data
/**
 * <p>Auto-configuration for DisruptorProperties.</p>
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class DisruptorProperties {

	public static final String PREFIX = "spring.disruptor";

	/** Enable Disruptor. */
	private boolean enabled = false;

	private DisruptorThreadFactory threadFactory = DisruptorThreadFactory.DEFAULT_THREAD_FACTORY;
	private DisruptorWaitStrategy waitStrategy = DisruptorWaitStrategy.YIELDING_WAIT;
   	private ProducerType producerType = ProducerType.SINGLE;

	/** Whether to automatically create the RingBuffer object. */
	private boolean ringBuffer = false;
	/** RingBuffer buffer size, default 1024. */
	private int ringBufferSize = 1024;

	private int maxBatchSize = Integer.MAX_VALUE;
	/** Message consumer thread pool size, default 4. */
	private int ringThreadNumbers = 4;
	/** Whether to use multiple producers; when true a multi-producer RingBuffer is
	 *  created via RingBuffer.createMultiProducer, otherwise a single-producer RingBuffer
	 *  is created via RingBuffer.createSingleProducer. */
	private boolean multiProducer = false;


	/** Message-processing handler chain. */
	private List<EventHandlerDefinition> handlerDefinitions = new ArrayList<EventHandlerDefinition>();


}