package com.lmax.disruptor.spring.boot;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.EventTranslatorThreeArg;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.spring.boot.context.DisruptorEventAwareProcessor;
import com.lmax.disruptor.spring.boot.event.DisruptorApplicationEvent;
import com.lmax.disruptor.event.DisruptorEvent;
import com.lmax.disruptor.event.DisruptorEventFactory;
import com.lmax.disruptor.event.handler.DisruptorEventDispatcher;
import com.lmax.disruptor.event.translator.DisruptorEventOneArgTranslator;
import com.lmax.disruptor.event.translator.DisruptorEventThreeArgTranslator;
import com.lmax.disruptor.event.translator.DisruptorEventTwoArgTranslator;
import com.lmax.disruptor.hooks.DisruptorShutdownHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.OrderComparator;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import com.lmax.disruptor.DisruptorTemplate;

@Configuration
@ConditionalOnClass({ Disruptor.class })
@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ DisruptorProperties.class })
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
/**
 * Spring Boot auto-configuration for the LMAX Disruptor event-processing framework.
 * <p>
 * Registers the {@link Disruptor} instance together with its event factory, event
 * translators, a {@link DisruptorTemplate}, a Spring {@link ApplicationListener} that
 * bridges {@link DisruptorApplicationEvent}s into the ring buffer, and a
 * {@link DisruptorEventAwareProcessor} for injecting the event publisher. The Disruptor
 * is configured and started when the {@code spring.disruptor.enabled} property is set to
 * {@code true}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class DisruptorAutoConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;


	/**
	 * Creates the {@link EventFactory} bean that produces {@link DisruptorEvent}
	 * instances pre-allocated in the ring buffer.
	 * @return a {@link DisruptorEventFactory} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public EventFactory<DisruptorEvent> eventFactory() {
		return new DisruptorEventFactory();
	}

	/**
	 * Creates and starts the {@link Disruptor} instance, wiring the discovered event
	 * handlers into the ring buffer and registering a shutdown hook to clean up resources
	 * on JVM exit.
	 * @param properties the Disruptor configuration properties
	 * @param eventFactory the factory used to pre-allocate {@link DisruptorEvent}
	 *        instances in the ring buffer (the number of instances equals
	 *        {@link DisruptorProperties#getRingBufferSize()})
	 * @return the started {@link Disruptor} instance
	 */
	@Bean
	@ConditionalOnClass({ Disruptor.class })
	@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
	public Disruptor<DisruptorEvent> disruptor(DisruptorProperties properties,
											   EventFactory<DisruptorEvent> eventFactory) {

		Disruptor<DisruptorEvent> disruptor = new Disruptor<>(eventFactory, properties.getRingBufferSize(), properties.getThreadFactory(), properties.getProducerType(), properties.getWaitStrategy().get());

		List<DisruptorEventDispatcher> disruptorEventHandlers = new DisruptorEventHandlerCreater(applicationContext).create(properties);
		if (!ObjectUtils.isEmpty(disruptorEventHandlers)) {

			// Sort handlers by order.
			Collections.sort(disruptorEventHandlers, new OrderComparator());

			// Build a consumer group using the Disruptor.
			EventHandlerGroup<DisruptorEvent> handlerGroup = null;
			for (int i = 0; i < disruptorEventHandlers.size(); i++) {
				// Connect the event handler; EventHandler is the consumer implementation.
				DisruptorEventDispatcher eventHandler = disruptorEventHandlers.get(i);
				if(i < 1) {
					handlerGroup = disruptor.handleEventsWith(eventHandler);
				} else {
					// Run the next handler after the previous ones complete.
					handlerGroup.then(eventHandler);
				}
			}
		}

		// Start the Disruptor.
		disruptor.start();

		// On application exit, call shutdown to release resources and close connections.
		// It is recommended to invoke shutdown from container exit hooks (e.g. JBoss,
		// Tomcat).
		Runtime.getRuntime().addShutdownHook(new DisruptorShutdownHook(disruptor));

		return disruptor;

	}

	/**
	 * Creates the single-argument {@link EventTranslatorOneArg} bean used to publish
	 * events with one argument to the ring buffer.
	 * @return a {@link DisruptorEventOneArgTranslator} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public EventTranslatorOneArg<DisruptorEvent, DisruptorEvent> oneArgEventTranslator() {
		return new DisruptorEventOneArgTranslator();
	}

	/**
	 * Creates the two-argument {@link EventTranslatorTwoArg} bean used to publish events
	 * with two arguments to the ring buffer.
	 * @return a {@link DisruptorEventTwoArgTranslator} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public EventTranslatorTwoArg<DisruptorEvent, String, String> twoArgEventTranslator() {
		return new DisruptorEventTwoArgTranslator();
	}

	/**
	 * Creates the three-argument {@link EventTranslatorThreeArg} bean used to publish
	 * events with three arguments to the ring buffer.
	 * @return a {@link DisruptorEventThreeArgTranslator} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public EventTranslatorThreeArg<DisruptorEvent, String, String, String> threeArgEventTranslator() {
		return new DisruptorEventThreeArgTranslator();
	}

	/**
	 * Creates the {@link DisruptorTemplate} bean used as the high-level entry point for
	 * publishing events to the ring buffer.
	 * @param disruptor the Disruptor instance
	 * @param oneArgEventTranslator the single-argument event translator
	 * @return a new {@link DisruptorTemplate}
	 */
	@Bean
	@ConditionalOnMissingBean
	public DisruptorTemplate disruptorTemplate(Disruptor<DisruptorEvent> disruptor,
											   EventTranslatorOneArg<DisruptorEvent, DisruptorEvent> oneArgEventTranslator) {
		return new DisruptorTemplate(disruptor, oneArgEventTranslator);
	}

	/**
	 * Creates the {@link ApplicationListener} bean that bridges Spring
	 * {@link DisruptorApplicationEvent}s into the Disruptor ring buffer.
	 * @param disruptor the Disruptor instance
	 * @param oneArgEventTranslator the single-argument event translator
	 * @return an application listener that publishes the wrapped event to the ring buffer
	 */
	@Bean
	@ConditionalOnMissingBean
	public ApplicationListener<DisruptorApplicationEvent> disruptorEventListener(Disruptor<DisruptorEvent> disruptor,
			EventTranslatorOneArg<DisruptorEvent, DisruptorEvent> oneArgEventTranslator) {
		return appEvent -> {
            DisruptorEvent event = (DisruptorEvent) appEvent.getSource();
            disruptor.publishEvent(oneArgEventTranslator, event);
        };
	}

	/**
	 * Creates the {@link DisruptorEventAwareProcessor} bean post-processor that injects
	 * the Disruptor event publisher into aware beans.
	 * @return a new {@link DisruptorEventAwareProcessor}
	 */
	@Bean
	public DisruptorEventAwareProcessor disruptorEventAwareProcessor() {
		return new DisruptorEventAwareProcessor();
	}

	/**
	 * Sets the owning Spring {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @throws BeansException in case of context access errors
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Returns the owning Spring {@link ApplicationContext}.
	 * @return the application context
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
