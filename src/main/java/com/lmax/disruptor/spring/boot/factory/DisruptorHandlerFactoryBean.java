package com.lmax.disruptor.spring.boot.factory;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.CollectionUtils;

import com.lmax.disruptor.spring.boot.config.Ini;
import com.lmax.disruptor.spring.boot.event.DisruptorEvent;
import com.lmax.disruptor.spring.boot.handler.AbstractRouteableEventHandler;
import com.lmax.disruptor.spring.boot.handler.DisruptorEventHandler;
import com.lmax.disruptor.spring.boot.handler.DisruptorHandler;
import com.lmax.disruptor.spring.boot.handler.Nameable;
import com.lmax.disruptor.spring.boot.handler.chain.HandlerChainManager;
import com.lmax.disruptor.spring.boot.handler.chain.def.DefaultHandlerChainManager;
import com.lmax.disruptor.spring.boot.handler.chain.def.PathMatchingHandlerChainResolver;

public class DisruptorHandlerFactoryBean implements FactoryBean<DisruptorHandler<DisruptorEvent>> {

	/**
	 * 处理器定义
	 */
	private Map<String, DisruptorHandler<DisruptorEvent>> handlers;
	
	/**
	 * 处理器链定义
	 */
	private Map<String, String> handlerChainDefinitionMap;
	
	private AbstractRouteableEventHandler<DisruptorEvent> instance;

	public DisruptorHandlerFactoryBean() {
		handlers = new LinkedHashMap<String, DisruptorHandler<DisruptorEvent>>();
		handlerChainDefinitionMap = new LinkedHashMap<String, String>();
	}

	@Override
	public DisruptorHandler<DisruptorEvent> getObject() throws Exception {
		if(instance == null){
			instance = createInstance();
		}
		return instance;
	}

	@Override
	public Class<?> getObjectType() {
		return DisruptorEventHandler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
	
	public Map<String, String> getHandlerChainDefinitionMap() {
		return handlerChainDefinitionMap;
	}

	public void setHandlerChainDefinitionMap(Map<String, String> handlerChainDefinitionMap) {
		this.handlerChainDefinitionMap = handlerChainDefinitionMap;
	}

	public Map<String, DisruptorHandler<DisruptorEvent>> getHandlers() {
		return handlers;
	}

	public void setHandlers(Map<String, DisruptorHandler<DisruptorEvent>> handlers) {
		this.handlers = handlers;
	}
	
	public void setHandlerChainDefinitions(String definitions) {
        Ini ini = new Ini();
        ini.load(definitions);
        Ini.Section section = ini.getSection("urls");
        if (CollectionUtils.isEmpty(section)) {
            section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
        }
        setHandlerChainDefinitionMap(section);
    }
	
	protected HandlerChainManager<DisruptorEvent> createHandlerChainManager() {

		HandlerChainManager<DisruptorEvent> manager = new DefaultHandlerChainManager();
		Map<String, DisruptorHandler<DisruptorEvent>> handlers = getHandlers();
		if (!CollectionUtils.isEmpty(handlers)) {
			for (Map.Entry<String, DisruptorHandler<DisruptorEvent>> entry : handlers.entrySet()) {
				String name = entry.getKey();
				DisruptorHandler<DisruptorEvent> handler = entry.getValue();
				if (handler instanceof Nameable) {
					((Nameable) handler).setName(name);
				}
				manager.addHandler(name, handler);
			}
		}

		Map<String, String> chains = getHandlerChainDefinitionMap();
		if (!CollectionUtils.isEmpty(chains)) {
			for (Map.Entry<String, String> entry : chains.entrySet()) {
				// topic/tags/keys
				String url = entry.getKey();
				String chainDefinition = entry.getValue();
				manager.createChain(url, chainDefinition);
			}
		}

		return manager;
	}
	
	protected AbstractRouteableEventHandler<DisruptorEvent> createInstance() throws Exception {
		HandlerChainManager<DisruptorEvent> manager = createHandlerChainManager();
        PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
        chainResolver.setHandlerChainManager(manager);
        return new DisruptorEventHandler(chainResolver);
    }
	
	
	
}
