package com.lmax.disruptor.spring.boot;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.event.factory.DisruptorBindEventFactory;
import com.lmax.disruptor.spring.boot.event.handler.DisruptorEventDispatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnClass({ Disruptor.class })
@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ DisruptorProperties.class })
public class RingBufferAutoConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Bean
	@ConditionalOnMissingBean
	public EventFactory<DisruptorEvent> eventFactory() {
		return new DisruptorBindEventFactory();
	}
	
	/**
	 * 
	 * <p>
	 * 创建RingBuffer
	 * </p>
	 * <p>
	 * 1 eventFactory 为
	 * <p>
	 * 2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍
	 * </p>
	 * 
	 * @param properties		：配置参数
	 * @param eventFactory		：  工厂类对象，用于创建一个个的LongEvent， LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
	 * @param preEventHandler	：事件前置处理器
	 * @param postEventHandler	： 事件后置处理器
	 * @return {@link RingBuffer} instance 
	 */
	@Bean
	@ConditionalOnClass({ RingBuffer.class })
	@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "ring-buffer", havingValue = "true")
	public RingBuffer<DisruptorEvent> ringBuffer(DisruptorProperties properties,
			EventFactory<DisruptorEvent> eventFactory,
			@Autowired(required = false) DisruptorEventDispatcher preEventHandler,
			@Autowired(required = false) DisruptorEventDispatcher postEventHandler) {
		// http://blog.csdn.net/a314368439/article/details/72642653?utm_source=itdadao&utm_medium=referral
		// 创建线程池
		ExecutorService executor = Executors.newFixedThreadPool(properties.getRingThreadNumbers());
		/*
		 * 第一个参数叫EventFactory，从名字上理解就是“事件工厂”，其实它的职责就是产生数据填充RingBuffer的区块。
		 * 第二个参数是RingBuffer的大小，它必须是2的指数倍 目的是为了将求模运算转为&运算提高效率
		 * 第三个参数是RingBuffer的生产都在没有可用区块的时候(可能是消费者（或者说是事件处理器） 太慢了)的等待策略
		 */
		RingBuffer<DisruptorEvent> ringBuffer = null;
		if (ProducerType.MULTI.equals(properties.getProducerType())) {
			// RingBuffer.createMultiProducer创建一个多生产者的RingBuffer
			ringBuffer = RingBuffer.createMultiProducer(eventFactory, properties.getRingBufferSize(), properties.getWaitStrategy().get());
		} else {
			// RingBuffer.createSingleProducer创建一个单生产者的RingBuffer
			ringBuffer = RingBuffer.createSingleProducer(eventFactory, properties.getRingBufferSize(), properties.getWaitStrategy().get());
		}

		// 单个处理器
		if (null != preEventHandler) {
			// 创建SequenceBarrier
			SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
			// 创建消息处理器
			BatchEventProcessor<DisruptorEvent> transProcessor = new BatchEventProcessor<DisruptorEvent>(ringBuffer,
					sequenceBarrier, preEventHandler);
			// 这一部的目的是让RingBuffer根据消费者的状态 如果只有一个消费者的情况可以省略
			ringBuffer.addGatingSequences(transProcessor.getSequence());
			// 把消息处理器提交到线程池
			executor.submit(transProcessor);
		}

		return ringBuffer;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
