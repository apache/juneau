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

import static org.apache.juneau.internal.CollectionUtils.*;
import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Base class for bean builders.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The bean type that the builder creates.
 */
@FluentSetters
public class BeanBuilder<T> {

	private Class<? extends T> type, defaultType;
	private T impl;
	private final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @param defaultType The default bean type that this builder creates.
	 */
	protected BeanBuilder(Class<? extends T> defaultType, BeanStore beanStore) {
		this.defaultType = type = defaultType;
		this.beanStore = beanStore;
	}

	/**
	 * Constructor.
	 *
	 * @param defaultType The type of bean being created.
	 */
	protected BeanBuilder(Class<? extends T> defaultType) {
		this(defaultType, BeanStore.INSTANCE);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean store to copy from.
	 */
	protected BeanBuilder(BeanBuilder<T> copyFrom) {
		type = copyFrom.type;
		impl = copyFrom.impl;
		beanStore = copyFrom.beanStore;
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
			.createBean(type().orElseThrow(()->new RuntimeException("Type not specified.")))
			.builder(BeanBuilder.class, this);
	}

	/**
	 * Creates the bean when the bean type is <jk>null</jk> or is the default value.
	 *
	 * @return A new bean.
	 */
	protected T buildDefault() {
		return beanStore
			.createBean(type().orElseThrow(()->new RuntimeException("Type not specified.")))
			.builder(BeanBuilder.class, this)
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
		return optional(type);
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
		return optional(impl);
	}

	/**
	 * Returns the bean store passed in through the constructor.
	 *
	 * @return The bean store passed in through the constructor.
	 */
	public BeanStore beanStore() {
		return beanStore;
	}

	// <FluentSetters>

	// </FluentSetters>
}
