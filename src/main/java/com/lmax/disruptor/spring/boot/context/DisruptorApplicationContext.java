/*
 * Copyright (c) 2017, hiwepy (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.lmax.disruptor.spring.boot.context;


import com.lmax.disruptor.event.DisruptorEventPublisher;
import com.lmax.disruptor.spring.boot.event.DisruptorApplicationEvent;
import com.lmax.disruptor.event.DisruptorEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Application-context-aware implementation of {@link DisruptorEventPublisher} that
 * publishes {@link DisruptorEvent}s by wrapping them in a Spring
 * {@link DisruptorApplicationEvent}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 1.0.0
 */
public class DisruptorApplicationContext implements ApplicationContextAware, DisruptorEventPublisher {

	protected ApplicationContext applicationContext;

	/**
	 * Publishes the given {@link DisruptorEvent} by delegating to the Spring application
	 * context wrapped in a {@link DisruptorApplicationEvent}.
	 * @param event the Disruptor event to publish
	 */
	@Override
	public void publishEvent(DisruptorEvent event) {
		applicationContext.publishEvent(new DisruptorApplicationEvent(event));
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
	

