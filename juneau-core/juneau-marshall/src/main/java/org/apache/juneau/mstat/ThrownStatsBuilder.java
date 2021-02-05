// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.mstat;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link ThrownStats} objects.
 */
public class ThrownStatsBuilder {

	Throwable throwable;
	long hash;
	List<String> stackTrace;
	ThrownStats causedBy;

	Class<? extends ThrownStats> implClass;
	BeanFactory beanFactory;

	/**
	 * Create a new {@link ThrownStats} using this builder.
	 *
	 * @return A new {@link ThrownStats}
	 */
	public ThrownStats build() {
		try {
			Class<? extends ThrownStats> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanFactory.of(beanFactory).addBeans(ThrownStatsBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends ThrownStats> getDefaultImplClass() {
		return ThrownStats.class;
	}

	/**
	 * Specifies the bean factory to use for instantiating the {@link ThrownStats} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link ThrownStats} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder beanFactory(BeanFactory value) {
		this.beanFactory = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link ThrownStats} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder implClass(Class<? extends ThrownStats> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies the thrown exception.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder throwable(Throwable value) {
		this.throwable = value;
		return this;
	}

	/**
	 * Specifies the calculated hash.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder hash(long value) {
		this.hash = value;
		return this;
	}

	/**
	 * Specifies the normalized stacktrace.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder stackTrace(List<String> value) {
		this.stackTrace = value;
		return this;
	}

	/**
	 * Specifies the caused-by exception.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder causedBy(ThrownStats value) {
		this.causedBy = value;
		return this;
	}
}
