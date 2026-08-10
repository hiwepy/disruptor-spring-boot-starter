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

import com.lmax.disruptor.event.DisruptorEventPublisherAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link BeanPostProcessor} that injects the {@link DisruptorEventPublisher} into beans
 * implementing {@link DisruptorEventPublisherAware}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class DisruptorEventAwareProcessor implements ApplicationContextAware ,BeanPostProcessor, InitializingBean {

	private DisruptorApplicationContext disruptorContext;
	private ApplicationContext applicationContext;

	/**
	 * Create a new DisruptorEventAwareProcessor.
	 */
	public DisruptorEventAwareProcessor() {
	}

	/**
	 * Invokes the relevant Aware callbacks on the bean before it is initialized.
	 * @param bean the bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance, possibly modified
	 * @throws BeansException in case of errors
	 */
	@Override
	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		if (bean instanceof Aware) {
			invokeAwareInterfaces(bean);
		}
		return bean;
	}

	/**
	 * Injects the Disruptor event publisher into beans that implement
	 * {@link DisruptorEventPublisherAware}.
	 * @param bean the bean instance to process
	 */
	protected void invokeAwareInterfaces(Object bean) {
		if (bean instanceof DisruptorEventPublisherAware disruptorEventPublisherAware) {
			disruptorEventPublisherAware.setDisruptorEventPublisher( this.disruptorContext );
		}
	}

	/**
	 * Returns the bean unchanged after initialization.
	 * @param bean the bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * Initializes the {@link DisruptorApplicationContext} once all properties have been
	 * set.
	 * @throws Exception if initialization fails
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		disruptorContext = new DisruptorApplicationContext();
		disruptorContext.setApplicationContext(applicationContext);
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


}
