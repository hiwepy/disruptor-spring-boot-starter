package com.lmax.disruptor.spring.boot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.factory.DisruptorEventThreadFactory;
import com.lmax.disruptor.spring.boot.handler.DisruptorEventHandler;
import com.lmax.disruptor.spring.boot.hooks.DisruptorShutdownHook;
import com.lmax.disruptor.spring.boot.util.WaitStrategys;

@Configuration
@ConditionalOnClass({ Disruptor.class })
@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ DisruptorProperties.class })
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE - 8)
@SuppressWarnings("unchecked")
public class DisruptorAutoConfiguration {

	/**
	 * 决定一个消费者将如何等待生产者将Event置入Disruptor的策略。用来权衡当生产者无法将新的事件放进RingBuffer时的处理策略。
	 * （例如：当生产者太快，消费者太慢，会导致生成者获取不到新的事件槽来插入新事件，则会根据该策略进行处理，默认会堵塞）
	 */
	@Bean
	@ConditionalOnMissingBean
	public WaitStrategy waitStrategy() {
		return WaitStrategys.YIELDING_WAIT;
	}

	@Bean
	@ConditionalOnMissingBean
	public ThreadFactory threadFactory() {
		return new DisruptorEventThreadFactory();
	}

	/**
	 * http://blog.csdn.net/a314368439/article/details/72642653?utm_source=itdadao&utm_medium=referral
	 * <p>创建RingBuffer</p>
	 * <p>1 eventFactory 为
	 * <p>2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍 </p>
	 * @param eventFactory 工厂类对象，用于创建一个个的LongEvent， LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
	 * @param ringBufferSize RingBuffer缓冲区大小
	 * @param executor 线程池，Disruptor内部的对数据进行接收处理时调用
	 * @param producerType 用来指定数据生成者有一个还是多个，有两个可选值ProducerType.SINGLE和ProducerType.MULTI
	 * @param waitStrategy 一种策略，用来均衡数据生产者和消费者之间的处理效率，默认提供了3个实现类
	 */
	@Bean
	@ConditionalOnClass({ RingBuffer.class })
	protected RingBuffer<DisruptorEvent> ringBuffer(DisruptorProperties properties, WaitStrategy waitStrategy,
			EventFactory<DisruptorEvent> eventFactory,
			@Autowired(required = false) DisruptorEventHandler preEventHandler,
			@Autowired(required = false) DisruptorEventHandler postEventHandler) {

		// 创建线程池
		ExecutorService executor = Executors.newFixedThreadPool(properties.getRingThreadNumbers());
		/*
		 * 第一个参数叫EventFactory，从名字上理解就是“事件工厂”，其实它的职责就是产生数据填充RingBuffer的区块。
		 * 第二个参数是RingBuffer的大小，它必须是2的指数倍 目的是为了将求模运算转为&运算提高效率
		 * 第三个参数是RingBuffer的生产都在没有可用区块的时候(可能是消费者（或者说是事件处理器） 太慢了)的等待策略
		 */
		RingBuffer<DisruptorEvent> ringBuffer = null;
		if (properties.isMultiProducer()) {
			// RingBuffer.createMultiProducer创建一个多生产者的RingBuffer
			ringBuffer = RingBuffer.createMultiProducer(eventFactory, properties.getRingBufferSize(), waitStrategy);
		} else {
			// RingBuffer.createSingleProducer创建一个单生产者的RingBuffer
			ringBuffer = RingBuffer.createSingleProducer(eventFactory, properties.getRingBufferSize(), waitStrategy);
		}
		
		//单个处理器
		if (null != preEventHandler) {
			// 创建SequenceBarrier
			SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
			// 创建消息处理器
			BatchEventProcessor<DisruptorEvent> transProcessor = new BatchEventProcessor<DisruptorEvent>(
					ringBuffer, sequenceBarrier, preEventHandler);
			// 这一部的目的是让RingBuffer根据消费者的状态 如果只有一个消费者的情况可以省略
			ringBuffer.addGatingSequences(transProcessor.getSequence());
			// 把消息处理器提交到线程池
			executor.submit(transProcessor);
		}
		
		return ringBuffer;
	}
	
	/**
	 * http://blog.csdn.net/a314368439/article/details/72642653?utm_source=itdadao&utm_medium=referral
	 * <p>创建Disruptor</p>
	 * <p>1 eventFactory 为
	 * <p>2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍 </p>
	 * @param eventFactory 工厂类对象，用于创建一个个的LongEvent， LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
	 * @param ringBufferSize RingBuffer缓冲区大小
	 * @param executor 线程池，Disruptor内部的对数据进行接收处理时调用
	 * @param producerType 用来指定数据生成者有一个还是多个，有两个可选值ProducerType.SINGLE和ProducerType.MULTI
	 * @param waitStrategy 一种策略，用来均衡数据生产者和消费者之间的处理效率，默认提供了3个实现类
	 */
	@Bean
	@ConditionalOnClass({ Disruptor.class })
	@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
	protected Disruptor<DisruptorEvent> disruptor(DisruptorProperties properties, 
			WaitStrategy waitStrategy,
			ThreadFactory threadFactory, 
			EventFactory<DisruptorEvent> eventFactory,
			@Autowired(required = false) DisruptorEventHandler preEventHandler,
			@Autowired(required = false) DisruptorEventHandler postEventHandler) {

		Disruptor<DisruptorEvent> disruptor = null;
		if (properties.isMultiProducer()) {
			disruptor = new Disruptor<DisruptorEvent>(eventFactory, properties.getRingBufferSize(), threadFactory,
					ProducerType.MULTI, waitStrategy);
		} else {
			disruptor = new Disruptor<DisruptorEvent>(eventFactory, properties.getRingBufferSize(), threadFactory,
					ProducerType.SINGLE, waitStrategy);
		}
		
		if (!ObjectUtils.isEmpty(preEventHandler)) {
			//连接消费事件方法，其中EventHandler的是为消费者消费消息的实现类
			// 使用disruptor创建消费者组
			EventHandlerGroup<DisruptorEvent> handlerGroup = disruptor.handleEventsWith(preEventHandler);
			//后置处理;可以在完成前面的逻辑后执行新的逻辑
			if(!ObjectUtils.isEmpty(postEventHandler)) {
				// 完成前置事件处理之后执行后置事件处理
				handlerGroup.then(postEventHandler);
			}
		}
		
		// 启动
		disruptor.start();

		/**
		 * 应用退出时，要调用shutdown来清理资源，关闭网络连接，从MetaQ服务器上注销自己
		 * 注意：我们建议应用在JBOSS、Tomcat等容器的退出钩子里调用shutdown方法
		 */
		Runtime.getRuntime().addShutdownHook(new DisruptorShutdownHook(disruptor));

		return disruptor;

	}
	
}
