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

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link MethodExecStats} objects.
 */
public class MethodExecStatsBuilder {

	Method method;
	ThrownStore thrownStore;

	Class<? extends MethodExecStats> implClass;
	BeanFactory beanFactory;

	/**
	 * Create a new {@link MethodExecStats} using this builder.
	 *
	 * @return A new {@link ThrownStats}
	 */
	public MethodExecStats build() {
		try {
			Class<? extends MethodExecStats> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanFactory.of(beanFactory).addBeans(MethodExecStatsBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends MethodExecStats> getDefaultImplClass() {
		return MethodExecStats.class;
	}

	/**
	 * Specifies the bean factory to use for instantiating the {@link MethodExecStats} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link MethodExecStats} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStatsBuilder beanFactory(BeanFactory value) {
		this.beanFactory = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link MethodExecStats} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStatsBuilder implClass(Class<? extends MethodExecStats> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies the Java method.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStatsBuilder method(Method value) {
		this.method = value;
		return this;
	}

	/**
	 * Specifies the thrown store for tracking exceptions.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStatsBuilder thrownStore(ThrownStore value) {
		this.thrownStore = value;
		return this;
	}
}
