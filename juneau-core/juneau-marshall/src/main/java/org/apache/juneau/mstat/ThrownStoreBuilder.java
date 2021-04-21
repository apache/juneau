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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link ThrownStore} objects.
 */
@FluentSetters
public class ThrownStoreBuilder {

	ThrownStore parent;
	private Class<? extends ThrownStore> implClass;
	BeanStore beanStore;
	Class<? extends ThrownStats> statsImplClass;
	Set<Class<?>> ignoreClasses;

	/**
	 * Create a new {@link ThrownStore} using this builder.
	 *
	 * @return A new {@link ThrownStore}
	 */
	public ThrownStore build() {
		try {
			Class<? extends ThrownStore> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanStore.of(beanStore).addBeans(ThrownStoreBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends ThrownStore> getDefaultImplClass() {
		return ThrownStore.class;
	}

	/**
	 * Specifies the bean store to use for instantiating the {@link ThrownStore} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link ThrownStore} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStoreBuilder beanStore(BeanStore value) {
		this.beanStore = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link ThrownStore} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStoreBuilder implClass(Class<? extends ThrownStore> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link ThrownStats} to use for individual method statistics.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStoreBuilder statsImplClass(Class<? extends ThrownStats> value) {
		this.statsImplClass = value;
		return this;
	}

	/**
	 * Specifies the parent store of this store.
	 *
	 * <p>
	 * Parent stores are used for aggregating statistics across multiple child stores.
	 * <br>The {@link ThrownStore#GLOBAL} store can be used for aggregating all thrown exceptions in a single JVM.
	 *
	 * @param value The parent store.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ThrownStoreBuilder parent(ThrownStore value) {
		this.parent = value;
		return this;
	}

	/**
	 * Specifies the list of classes to ignore when calculating stack traces.
	 *
	 * <p>
	 * Stack trace elements that are the specified class will be ignored.
	 *
	 * @param value The list of classes to ignore.
	 * @return This object (for method chaining).
	 */
	public ThrownStoreBuilder ignoreClasses(Class<?>...value) {
		this.ignoreClasses = ASet.of(value);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
