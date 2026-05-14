/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.inject;

import java.util.function.*;

/**
 * Extends {@link BeanStore} with methods for adding beans.
 *
 * <p>
 * This interface provides write operations for bean stores, allowing beans to be added
 * either as direct instances or as suppliers for lazy initialization.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	WritableBeanStore <jv>beanStore</jv> = ...;
 *
 * 	<jc>// Add unnamed bean</jc>
 * 	<jv>beanStore</jv>.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService());
 *
 * 	<jc>// Add named bean</jc>
 * 	<jv>beanStore</jv>.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService(), <js>"primary"</js>);
 *
 * 	<jc>// Add bean supplier for lazy initialization</jc>
 * 	<jv>beanStore</jv>.addSupplier(MyService.<jk>class</jk>, () -&gt; <jk>new</jk> MyService());
 *
 * 	<jc>// Add named bean supplier</jc>
 * 	<jv>beanStore</jv>.addSupplier(MyService.<jk>class</jk>, () -&gt; <jk>new</jk> MyService(), <js>"secondary"</js>);
 *
 * 	<jc>// Clear all beans</jc>
 * 	<jv>beanStore</jv>.clear();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore} - Read-only bean lookup interface
 * </ul>
 */
public interface WritableBeanStore extends BeanStore, AutoCloseable {

	/**
	 * Same as {@link #addBean(Class,Object)} but returns the bean instead of this object for fluent calls.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @return The bean.
	 */
	@SuppressWarnings("resource") // addBean returns this; the discarded return is the store the caller already holds
	default <T> T add(Class<T> beanType, T bean) {
		addBean(beanType, bean);
		return bean;
	}

	/**
	 * Same as {@link #addBean(Class,Object,String)} but returns the bean instead of this object for fluent calls.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return The bean.
	 */
	@SuppressWarnings("resource") // addBean returns this; the discarded return is the store the caller already holds
	default <T> T add(Class<T> beanType, T bean, String name) {
		addBean(beanType, bean, name);
		return bean;
	}

	/**
	 * Adds an unnamed bean of the specified type to this store.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addBean(Class<T> beanType, T bean);

	/**
	 * Adds a named bean of the specified type to this store.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addBean(Class<T> beanType, T bean, String name);

	/**
	 * Adds a supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addSupplier(Class<T> beanType, Supplier<T> supplier);

	/**
	 * Adds a supplier for a named bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addSupplier(Class<T> beanType, Supplier<T> supplier, String name);

	/**
	 * Adds a fallback supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * Default suppliers are consulted only after regular {@linkplain #addBean(Class,Object) entries} and the regular {@linkplain BasicBeanStore#BasicBeanStore(BeanStore) parent}
	 * chain have been searched.  They serve as a "use this if nothing else has provided one" hook,
	 * useful for memoizer-backed framework defaults that should not shadow explicit user
	 * registrations or beans inherited from an overriding parent (e.g. a Spring application context).
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> supplier);

	/**
	 * Adds a fallback supplier for a named bean of the specified type to this store.
	 *
	 * <p>
	 * Default suppliers are consulted only after regular {@linkplain #addBean(Class,Object,String) entries} and the regular
	 * parent chain have been searched.  See {@link #addDefaultSupplier(Class,Supplier)}.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> supplier, String name);

	/**
	 * Removes all beans from this store.
	 *
	 * <p>
	 * This operation only affects this store and does not affect any parent stores
	 * that may be associated with this store.
	 *
	 * @return This object for method chaining.
	 */
	WritableBeanStore clear();

	/**
	 * Returns <jk>true</jk> if this store has a {@linkplain #addDefaultSupplier(Class,Supplier) default supplier}
	 * registered locally for the specified unnamed bean type.
	 *
	 * <p>
	 * This is intended for callers that need to distinguish "framework default present" from "any binding
	 * exists" — for example, REST initialization uses this signal to skip types that are managed by an
	 * internal memoizer.  Parent and overriding-parent stores are <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @return <jk>true</jk> if a default supplier for the unnamed bean type is registered on this store.
	 */
	boolean hasDefaultSupplier(Class<?> beanType);

	/**
	 * Returns <jk>true</jk> if this store has a {@linkplain #addDefaultSupplier(Class,Supplier,String) default supplier}
	 * registered locally for the specified bean type and name.
	 *
	 * <p>
	 * See {@link #hasDefaultSupplier(Class)} for the rationale.  Parent and overriding-parent stores are
	 * <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return <jk>true</jk> if a default supplier for the bean type and name is registered on this store.
	 */
	boolean hasDefaultSupplier(Class<?> beanType, String name);

	/**
	 * Registers a default implementation class for the specified bean type.
	 *
	 * <p>
	 * Type bindings are consulted by callers (notably {@link BeanInstantiator} and the various REST
	 * memoizers) when resolving the impl class to instantiate for <c>beanType</c>.  This is the
	 * type-binding analogue of {@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier}: it
	 * provides a deferred-construction default that can be overridden by later resolution steps
	 * (e.g. an annotation override).
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to bind.
	 * @param implType The implementation class to instantiate when this type is resolved.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addBeanType(Class<T> beanType, Class<? extends T> implType);

	/**
	 * Registers a configuration class and its {@link Bean}-annotated members.
	 *
	 * @param configType The configuration type.
	 * @return This object.
	 */
	WritableBeanStore registerConfiguration(Class<?> configType);

	/**
	 * Registers multiple configuration classes.
	 *
	 * @param configTypes The configuration types.
	 * @return This object.
	 */
	@SuppressWarnings("resource") // registerConfiguration returns this; the discarded return is the store the caller already holds
	default WritableBeanStore registerConfigurations(Class<?>... configTypes) {
		if (configTypes != null)
			for (var c : configTypes)
				registerConfiguration(c);
		return this;
	}

	/**
	 * Closes this bean store, invoking pre-destroy callbacks.
	 *
	 * @throws BeanCreationException on shutdown failures.
	 */
	@Override
	void close() throws BeanCreationException;
}

