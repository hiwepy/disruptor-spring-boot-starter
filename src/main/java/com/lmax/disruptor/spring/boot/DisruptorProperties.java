package com.lmax.disruptor.spring.boot;

import java.util.ArrayList;
import java.util.List;

import com.lmax.disruptor.dsl.ProducerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.lmax.disruptor.spring.boot.config.EventHandlerDefinition;

@ConfigurationProperties(DisruptorProperties.PREFIX)
@Data
public class DisruptorProperties {

	public static final String PREFIX = "spring.disruptor";

	/**
	 * Enable Disruptor.
	 */
	private boolean enabled = false;
	/**
	 * RingBuffer缓冲区大小, 默认 1024
	 */
	private int ringBufferSize = 1024;
	/**
	 * 消息消费线程池大小, 默认 4
	 * */
	private int ringThreadNumbers = 4;
	/**
	 * 是否对生产者，如果是则通过 RingBuffer.createMultiProducer创建一个多生产者的RingBuffer，否则通过RingBuffer.createSingleProducer创建一个单生产者的RingBuffer
	 * */
	private ProducerType producerType = ProducerType.SINGLE;
	/**
	 * 消费者等待生产者将Event置入Disruptor的策略。用来权衡当生产者无法将新的事件放进RingBuffer时的处理策略。
	 * （例如：当生产者太快，消费者太慢，会导致生成者获取不到新的事件槽来插入新事件，则会根据该策略进行处理，默认会堵塞）
	 * BLOCKING_WAIT：是最低效的策略，但其对CPU的消耗最小并且在各种不同部署环境中能提供更加一致的性能表现
	 * SLEEPING_WAIT：性能表现跟BlockingWaitStrategy差不多，对CPU的消耗也类似，但其对生产者线程的影响最小，适合用于异步日志类似的场景
	 * YIELDING_WAIT：可以被用在低延迟系统中的两个策略之一，这种策略在减低系统延迟的同时也会增加CPU运算量。YieldingWaitStrategy策略会循环等待sequence增加到合适的值。循环中调用Thread.yield()允许其他准备好的线程执行。
	 * 	如果需要高性能而且事件消费者线程比逻辑内核少的时候，推荐使用YieldingWaitStrategy策略。例如：在开启超线程的时候。
	 * BUSYSPIN_WAIT：性能最高的等待策略，同时也是对部署环境要求最高的策略。这个性能最好用在事件处理线程比物理内核数目还要小的时候。例如：在禁用超线程技术的时候。
	 */
	private WaitStrategy waitStrategy = WaitStrategy.BLOCKING_WAIT;
	/**
	 * 消息出来责任链
	 */
	private List<EventHandlerDefinition> handlerDefinitions = new ArrayList<EventHandlerDefinition>();

}