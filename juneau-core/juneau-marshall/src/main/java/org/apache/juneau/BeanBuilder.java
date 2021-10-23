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
package org.apache.juneau;

import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Optional.*;

import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Base class for bean builders.
 *
 * @param <T> The bean type that the builder creates.
 */
@FluentSetters
public class BeanBuilder<T> {

	private Class<? extends T> type, defaultType;
	private T impl;
	private Object outer;
	private BeanStore beanStore = BeanStore.INSTANCE;

	/**
	 * Constructor.
	 *
	 * @param defaultType The default bean type that this builder creates.
	 */
	protected BeanBuilder(Class<? extends T> defaultType) {
		this.defaultType = type = defaultType;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean store to copy from.
	 */
	protected BeanBuilder(BeanBuilder<T> copyFrom) {
		type = copyFrom.type;
		impl = copyFrom.impl;
		outer = copyFrom.outer;
		beanStore = copyFrom.beanStore;
	}

	/**
	 * Creates a copy of this builder.
	 *
	 * @return A copy of this builder.
	 */
	public BeanBuilder<T> copy() {
		return new BeanBuilder<>(this);
	}

	/**
	 * Creates the bean.
	 *
	 * @return A new bean.
	 */
	public T build() {
		if (impl != null)
			return impl;
		if (type == null || type == defaultType)
			return buildDefault();
		return creator().run();
	}

	/**
	 * Instantiates the creator for this bean.
	 *
	 * <p>
	 * Subclasses can override this to provide specialized handling.
	 *
	 * @return The creator for this bean.
	 */
	protected BeanCreator<? extends T> creator() {
		return beanStore
			.creator(type().orElseThrow(()->runtimeException("Type not specified.")))
			.outer(outer)
			.builder(this);
	}

	/**
	 * Creates the bean when the bean type is <jk>null</jk> or is the default value.
	 *
	 * @return A new bean.
	 */
	protected T buildDefault() {
		return beanStore
			.creator(type().orElseThrow(()->runtimeException("Type not specified.")))
			.outer(outer)
			.builder(this)
			.run();
	}

	/**
	 * Overrides the bean type produced by the {@link #build()} method.
	 *
	 * <p>
	 * Use this method if you want to instantiated a bean subclass.
	 *
	 * @param value The setting value.
	 * @return  This object.
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public BeanBuilder<T> type(Class<?> value) {
		type = (Class<T>)value;
		return this;
	}

	/**
	 * Returns the implementation type specified via {@link #type(Class)}.
	 *
	 * @return The implementation type specified via {@link #type(Class)}.
	 */
	protected Optional<Class<? extends T>> type() {
		return ofNullable(type);
	}

	/**
	 * Overrides the bean returned by the {@link #build()} method.
	 *
	 * <p>
	 * Use this method if you want this builder to return an already-instantiated bean.
	 *
	 * @param value The setting value.
	 * @return  This object.
	 */
	@FluentSetter
	@SuppressWarnings("unchecked")
	public BeanBuilder<T> impl(Object value) {
		this.impl = (T)value;
		return this;
	}

	/**
	 * Returns the override bean specified via {@link #impl(Object)}.
	 *
	 * @return The override bean specified via {@link #impl(Object)}.
	 */
	protected Optional<T> impl() {
		return ofNullable(impl);
	}

	/**
	 * Specifies the outer bean context.
	 *
	 * <p>
	 * This should be the instance of the outer object such as the servlet object when constructing inner classes
	 * of the servlet class.
	 *
	 * @param value The setting value.
	 * @return  This object.
	 */
	@FluentSetter
	public BeanBuilder<T> outer(Object value) {
		outer = value;
		return this;
	}

	/**
	 * Returns the outer bean context specified via {@link #outer(Object)}.
	 *
	 * @return The outer bean context specified via {@link #outer(Object)}.
	 */
	public Optional<Object> outer() {
		return ofNullable(outer);
	}

	/**
	 * The bean store to use for instantiating the bean.
	 *
	 * <p>
	 * The bean store can be used to inject beans into parameters of the constructor of the bean.
	 *
	 * @param value The setting value.
	 * @return  This object.
	 */
	@FluentSetter
	public BeanBuilder<T> beanStore(BeanStore value) {
		beanStore = value;
		return this;
	}

	/**
	 * Returns the bean store specified via {@link #outer(Object)}.
	 *
	 * @return The bean store specified via {@link #outer(Object)}.
	 */
	public Optional<BeanStore> beanStore() {
		return ofNullable(beanStore);
	}

	// <FluentSetters>

	// </FluentSetters>
}
