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
import static org.apache.juneau.internal.ExceptionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link MethodExecStore} objects.
 */
@FluentSetters
public class MethodExecStoreBuilder {

	ThrownStore thrownStore;
	private Class<? extends MethodExecStore> implClass;
	BeanStore beanStore;
	Class<? extends MethodExecStats> statsImplClass;

	/**
	 * Create a new {@link MethodExecStore} using this builder.
	 *
	 * @return A new {@link MethodExecStore}
	 */
	public MethodExecStore build() {
		try {
			Class<? extends MethodExecStore> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanStore.of(beanStore).addBeans(MethodExecStoreBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends MethodExecStore> getDefaultImplClass() {
		return MethodExecStore.class;
	}

	/**
	 * Specifies the bean store to use for instantiating the {@link MethodExecStore} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link MethodExecStore} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStoreBuilder beanStore(BeanStore value) {
		this.beanStore = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link MethodExecStore} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStoreBuilder implClass(Class<? extends MethodExecStore> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link MethodExecStats} to use for individual method statistics.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public MethodExecStoreBuilder statsImplClass(Class<? extends MethodExecStats> value) {
		this.statsImplClass = value;
		return this;
	}

	/**
	 * Specifies the store to use for gathering statistics on thrown exceptions.
	 *
	 * <p>
	 * Can be used to capture thrown exception stats across multiple {@link MethodExecStore} objects.
	 *
	 * <p>
	 * If not specified, one will be created by default for the {@link MethodExecStore} object.
	 *
	 * @param thrownStore The store to use for gathering statistics on thrown exceptions.
	 * @return This object (for method chaining).
	 */
	public MethodExecStoreBuilder thrownStore(ThrownStore thrownStore) {
		this.thrownStore = thrownStore;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
