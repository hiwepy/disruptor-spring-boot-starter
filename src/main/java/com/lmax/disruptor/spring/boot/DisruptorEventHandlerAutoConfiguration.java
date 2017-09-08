package com.lmax.disruptor.spring.boot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.spring.boot.config.Ini;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.handler.DisruptorEventHandler;
import com.lmax.disruptor.spring.boot.handler.DisruptorHandler;
import com.lmax.disruptor.spring.boot.handler.Nameable;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChainManager;
import com.lmax.disruptor.spring.boot.handler.chain.def.DefaultHandlerChainManager;
import com.lmax.disruptor.spring.boot.handler.chain.def.PathMatchingHandlerChainResolver;
import com.lmax.disruptor.spring.boot.util.StringUtils;

@Configuration
@ConditionalOnClass({ Disruptor.class })
@ConditionalOnProperty(name = DisruptorEventHandlerDefinitionProperties.PREFIX, matchIfMissing = true)
@EnableConfigurationProperties({ DisruptorEventHandlerDefinitionProperties.class })
public class DisruptorEventHandlerAutoConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;
	
	/**
	 * 后置前置处理器定义
	 */
	@Bean("disruptorHandlers")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, DisruptorHandler<DisruptorEvent>> disruptorHandlers() {

		Map<String, DisruptorHandler<DisruptorEvent>> disruptorPreHandlers = new LinkedHashMap<String, DisruptorHandler<DisruptorEvent>>();

		Map<String, DisruptorHandler> beansOfType = getApplicationContext().getBeansOfType(DisruptorHandler.class);
		if (!ObjectUtils.isEmpty(beansOfType)) {
			Iterator<Entry<String, DisruptorHandler>> ite = beansOfType.entrySet().iterator();
			while (ite.hasNext()) {
				Entry<String, DisruptorHandler> entry = ite.next();
				if (entry.getValue() instanceof DisruptorEventHandler ) {
					//跳过入口实现类
					continue;
				}
				disruptorPreHandlers.put(entry.getKey(), entry.getValue());
			}
		}
		// BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(), EventHandler.class);

		return disruptorPreHandlers;
	}
	
	
	@Bean("preDisruptorEventHandler")
	public DisruptorEventHandler preDisruptorEventHandler(
			DisruptorEventHandlerDefinitionProperties properties,
			@Qualifier("disruptorHandlers") Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {

		Map<String, String> handlerChainDefinitionMap = null;
		if( StringUtils.isNotEmpty(properties.getPreDefinitions())) {
			handlerChainDefinitionMap = this.parseHandlerChainDefinitions(properties.getPreDefinitions());
		} else if (!CollectionUtils.isEmpty(properties.getPreDefinitionMap())) {
			handlerChainDefinitionMap = properties.getPreDefinitionMap();
		}
		
		HandlerChainManager<DisruptorEvent> manager = createHandlerChainManager(eventHandlers , handlerChainDefinitionMap);
        PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
        chainResolver.setHandlerChainManager(manager);
        return new DisruptorEventHandler(chainResolver);
	}
	
	@Bean("postDisruptorEventHandler")
	public DisruptorEventHandler postDisruptorEventHandler(
			DisruptorEventHandlerDefinitionProperties properties,
			@Qualifier("disruptorHandlers") Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {
		
		Map<String, String> handlerChainDefinitionMap = null;
		if( StringUtils.isNotEmpty(properties.getPostDefinitions())) {
			handlerChainDefinitionMap = this.parseHandlerChainDefinitions(properties.getPostDefinitions());
		} else if (!CollectionUtils.isEmpty(properties.getPostDefinitionMap())) {
			handlerChainDefinitionMap = properties.getPostDefinitionMap();
		}
		
		HandlerChainManager<DisruptorEvent> manager = createHandlerChainManager(eventHandlers , handlerChainDefinitionMap);
        PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
        chainResolver.setHandlerChainManager(manager);
        return new DisruptorEventHandler(chainResolver);
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


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
}
