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
package com.lmax.disruptor.spring.boot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring {@link ApplicationEvent} wrapping a {@link DisruptorEvent}, optionally carrying a
 * bound data object.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Getter
public class DisruptorApplicationEvent extends ApplicationEvent {

	/**
	 * The data object bound to this event.
	 */
	protected Object bind;

	/**
	 * Constructs a new event with the given source and bound data object.
	 * @param source the component that published the event
	 * @param bind the data object bound to this event
	 */
	public DisruptorApplicationEvent(Object source, Object bind) {
		super(source);
		this.bind = bind;
	}

	/**
	 * Constructs a new event with the given source and no bound data object.
	 * @param source the component that published the event
	 */
	public DisruptorApplicationEvent(Object source) {
		super(source);
	}

	/**
	 * Binds the given data object to this event.
	 * @param bind the data object to bind
	 */
    public void bind(Object bind) {
		this.bind = bind;
	}


}
