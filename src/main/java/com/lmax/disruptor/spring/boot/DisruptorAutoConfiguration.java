package com.lmax.disruptor.spring.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.spring.boot.annotation.DisruptorEventHandler;
import com.lmax.disruptor.spring.boot.config.EventHandlerDefinition;
import com.lmax.disruptor.spring.boot.config.Ini;
import com.lmax.disruptor.spring.boot.context.DisruptorApplicationContext;
import com.lmax.disruptor.spring.boot.context.DisruptorEventAwareProcessor;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.event.factory.DisruptorEventThreadFactory;
import com.lmax.disruptor.spring.boot.event.handler.DisruptorEventDispatcher;
import com.lmax.disruptor.spring.boot.event.handler.DisruptorHandler;
import com.lmax.disruptor.spring.boot.event.handler.Nameable;
import com.lmax.disruptor.spring.boot.event.handler.chain.HandlerChainManager;
import com.lmax.disruptor.spring.boot.event.handler.chain.def.DefaultHandlerChainManager;
import com.lmax.disruptor.spring.boot.event.handler.chain.def.PathMatchingHandlerChainResolver;
import com.lmax.disruptor.spring.boot.event.translator.DisruptorEventTranslator;
import com.lmax.disruptor.spring.boot.hooks.DisruptorShutdownHook;
import com.lmax.disruptor.spring.boot.util.StringUtils;
import com.lmax.disruptor.spring.boot.util.WaitStrategys;

@Configuration
@ConditionalOnClass({ Disruptor.class })
@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ DisruptorProperties.class })
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE - 8)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DisruptorAutoConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;
	/**
	 * 处理器链定义
	 */
	private Map<String, String> handlerChainDefinitionMap = new HashMap<String, String>();
	
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
	 * <p>
	 * 创建RingBuffer
	 * </p>
	 * <p>
	 * 1 eventFactory 为
	 * <p>
	 * 2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍
	 * </p>
	 * 
	 * @param eventFactory
	 *            工厂类对象，用于创建一个个的LongEvent，
	 *            LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
	 * @param ringBufferSize
	 *            RingBuffer缓冲区大小
	 * @param executor
	 *            线程池，Disruptor内部的对数据进行接收处理时调用
	 * @param producerType
	 *            用来指定数据生成者有一个还是多个，有两个可选值ProducerType.SINGLE和ProducerType.MULTI
	 * @param waitStrategy
	 *            一种策略，用来均衡数据生产者和消费者之间的处理效率，默认提供了3个实现类
	 */
	@Bean
	@ConditionalOnClass({ RingBuffer.class })
	protected RingBuffer<DisruptorEvent> ringBuffer(DisruptorProperties properties, WaitStrategy waitStrategy,
			EventFactory<DisruptorEvent> eventFactory,
			@Autowired(required = false) DisruptorEventDispatcher preEventHandler,
			@Autowired(required = false) DisruptorEventDispatcher postEventHandler) {

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

	/**
	 * Handler实现集合
	 */
	@Bean("disruptorHandlers")
	public Map<String, DisruptorHandler<DisruptorEvent>> disruptorHandlers() {

		Map<String, DisruptorHandler<DisruptorEvent>> disruptorPreHandlers = new LinkedHashMap<String, DisruptorHandler<DisruptorEvent>>();

		Map<String, DisruptorHandler> beansOfType = getApplicationContext().getBeansOfType(DisruptorHandler.class);
		if (!ObjectUtils.isEmpty(beansOfType)) {
			Iterator<Entry<String, DisruptorHandler>> ite = beansOfType.entrySet().iterator();
			while (ite.hasNext()) {
				Entry<String, DisruptorHandler> entry = ite.next();
				if (entry.getValue() instanceof DisruptorEventDispatcher) {
					// 跳过入口实现类
					continue;
				}
				
				DisruptorEventHandler annotationType = getApplicationContext().findAnnotationOnBean(entry.getKey(), DisruptorEventHandler.class);
				if(annotationType != null) {
					handlerChainDefinitionMap.put(annotationType.ruleExpress(), annotationType.handler());
				}
				
				disruptorPreHandlers.put(entry.getKey(), entry.getValue());
			}
		}
		// BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(),
		// EventHandler.class);

		return disruptorPreHandlers;
	}

	/**
	 * 处理器链集合
	 */
	@Bean("disruptorEventHandlers")
	public List<DisruptorEventDispatcher> disruptorEventHandlers(DisruptorProperties properties,
			@Qualifier("disruptorHandlers") Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {
		// 获取定义 拦截链规则
		List<EventHandlerDefinition> handlerDefinitions = properties.getHandlerDefinitions();
		// 未定义，则使用默认规则
		if (CollectionUtils.isEmpty(handlerDefinitions)) {
			return null;
		}

		// 拦截器集合
		List<DisruptorEventDispatcher> disruptorEventHandlers = new ArrayList<DisruptorEventDispatcher>();
		// 迭代拦截器规则
		for (EventHandlerDefinition handlerDefinition : handlerDefinitions) {

			// 构造DisruptorEventHandler
			disruptorEventHandlers.add(this.createDisruptorEventHandler(handlerDefinition, eventHandlers));

		}

		// 进行排序
		Collections.sort(disruptorEventHandlers, new OrderComparator());

		return disruptorEventHandlers;
	}

	/**
	 * 
	 * @description ： 构造DisruptorEventHandler
	 * @param handlerDefinition
	 * @param eventHandlers
	 * @return
	 */
	protected DisruptorEventDispatcher createDisruptorEventHandler(EventHandlerDefinition handlerDefinition,
			Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {

		if (StringUtils.isNotEmpty(handlerDefinition.getDefinitions())) {
			handlerChainDefinitionMap.putAll(this.parseHandlerChainDefinitions(handlerDefinition.getDefinitions()));
		} else if (!CollectionUtils.isEmpty(handlerDefinition.getDefinitionMap())) {
			handlerChainDefinitionMap.putAll(handlerDefinition.getDefinitionMap());
		}

		HandlerChainManager<DisruptorEvent> manager = createHandlerChainManager(eventHandlers, handlerChainDefinitionMap);
		PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
		chainResolver.setHandlerChainManager(manager);
		return new DisruptorEventDispatcher(chainResolver, handlerDefinition.getOrder());
	}

	protected Map<String, String> parseHandlerChainDefinitions(String definitions) {
		Ini ini = new Ini();
		ini.load(definitions);
		Ini.Section section = ini.getSection("urls");
		if (CollectionUtils.isEmpty(section)) {
			section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
		}
		return section;
	}

	protected HandlerChainManager<DisruptorEvent> createHandlerChainManager(
			Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers,
			Map<String, String> handlerChainDefinitionMap) {

		HandlerChainManager<DisruptorEvent> manager = new DefaultHandlerChainManager();
		if (!CollectionUtils.isEmpty(eventHandlers)) {
			for (Map.Entry<String, DisruptorHandler<DisruptorEvent>> entry : eventHandlers.entrySet()) {
				String name = entry.getKey();
				DisruptorHandler<DisruptorEvent> handler = entry.getValue();
				if (handler instanceof Nameable) {
					((Nameable) handler).setName(name);
				}
				manager.addHandler(name, handler);
			}
		}

		if (!CollectionUtils.isEmpty(handlerChainDefinitionMap)) {
			for (Map.Entry<String, String> entry : handlerChainDefinitionMap.entrySet()) {
				// ant匹配规则
				String rule = entry.getKey();
				String chainDefinition = entry.getValue();
				manager.createChain(rule, chainDefinition);
			}
		}

		return manager;
	}

	/**
	 * http://blog.csdn.net/a314368439/article/details/72642653?utm_source=itdadao&utm_medium=referral
	 * <p>
	 * 创建Disruptor
	 * </p>
	 * <p>
	 * 1 eventFactory 为
	 * <p>
	 * 2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍
	 * </p>
	 * 
	 * @param eventFactory
	 *            工厂类对象，用于创建一个个的LongEvent，
	 *            LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
	 * @param ringBufferSize
	 *            RingBuffer缓冲区大小
	 * @param executor
	 *            线程池，Disruptor内部的对数据进行接收处理时调用
	 * @param producerType
	 *            用来指定数据生成者有一个还是多个，有两个可选值ProducerType.SINGLE和ProducerType.MULTI
	 * @param waitStrategy
	 *            一种策略，用来均衡数据生产者和消费者之间的处理效率，默认提供了3个实现类
	 */
	@Bean
	@ConditionalOnClass({ Disruptor.class })
	@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
	protected Disruptor<DisruptorEvent> disruptor(
			DisruptorProperties properties, 
			WaitStrategy waitStrategy,
			ThreadFactory threadFactory, 
			EventFactory<DisruptorEvent> eventFactory,
			@Autowired(required = false)
			@Qualifier("disruptorEventHandlers") 
			List<DisruptorEventDispatcher> disruptorEventHandlers) {

		Disruptor<DisruptorEvent> disruptor = null;
		if (properties.isMultiProducer()) {
			disruptor = new Disruptor<DisruptorEvent>(eventFactory, properties.getRingBufferSize(), threadFactory,
					ProducerType.MULTI, waitStrategy);
		} else {
			disruptor = new Disruptor<DisruptorEvent>(eventFactory, properties.getRingBufferSize(), threadFactory,
					ProducerType.SINGLE, waitStrategy);
		}

		if (!ObjectUtils.isEmpty(disruptorEventHandlers)) {
			
			// 进行排序
			Collections.sort(disruptorEventHandlers, new OrderComparator());
			
			// 使用disruptor创建消费者组
			EventHandlerGroup<DisruptorEvent> handlerGroup = null;
			for (int i = 0; i < disruptorEventHandlers.size(); i++) {
				// 连接消费事件方法，其中EventHandler的是为消费者消费消息的实现类
				DisruptorEventDispatcher eventHandler = disruptorEventHandlers.get(i);
				if(i < 1) {
					handlerGroup = disruptor.handleEventsWith(eventHandler);
				} else {
					// 完成前置事件处理之后执行后置事件处理
					handlerGroup.then(eventHandler);
				}
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
	
	@Bean
	@ConditionalOnMissingBean
	public EventTranslatorOneArg<DisruptorEvent, Object> eventTranslator() {
		return new DisruptorEventTranslator();
	}
	
	@Bean
	@ConditionalOnBean({ Disruptor.class })
	public DisruptorApplicationContext disruptorContext(
			Disruptor<DisruptorEvent> disruptor,
			EventTranslatorOneArg<DisruptorEvent, Object> eventTranslator) {
		DisruptorApplicationContext disruptorContext = new DisruptorApplicationContext();
		
		disruptorContext.setApplicationContext(getApplicationContext());
		disruptorContext.setDisruptor(disruptor);
		disruptorContext.setEventTranslator(eventTranslator);
		
		return disruptorContext;
	}
	
	@Bean
	@ConditionalOnBean({ DisruptorApplicationContext.class })
	public DisruptorEventAwareProcessor disruptorEventAwareProcessor(DisruptorApplicationContext disruptorContext) {
		return new DisruptorEventAwareProcessor(disruptorContext);
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
