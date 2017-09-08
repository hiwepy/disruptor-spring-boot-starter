package com.lmax.disruptor.spring.boot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(DisruptorEventHandlerDefinitionProperties.PREFIX)
public class DisruptorEventHandlerDefinitionProperties {

	public static final String PREFIX = "spring.disruptor.event";

	/**
	 * 前置处理器链定义
	 */
	private String preDefinitions = null;
	private Map<String /* rule */, String /* handler names */> preDefinitionMap = new LinkedHashMap<String, String>();
	/**
	 * 后置处理器链定义
	 */
	private String postDefinitions = null;
	private Map<String /* rule */, String /* handler names */> postDefinitionMap = new LinkedHashMap<String, String>();

	public String getPreDefinitions() {
		return preDefinitions;
	}

	public void setPreDefinitions(String preDefinitions) {
		this.preDefinitions = preDefinitions;
	}

	public Map<String, String> getPreDefinitionMap() {
		return preDefinitionMap;
	}

	public void setPreDefinitionMap(Map<String, String> preDefinitionMap) {
		this.preDefinitionMap = preDefinitionMap;
	}

	public String getPostDefinitions() {
		return postDefinitions;
	}

	public void setPostDefinitions(String postDefinitions) {
		this.postDefinitions = postDefinitions;
	}

	public Map<String, String> getPostDefinitionMap() {
		return postDefinitionMap;
	}

	public void setPostDefinitionMap(Map<String, String> postDefinitionMap) {
		this.postDefinitionMap = postDefinitionMap;
	}

}
