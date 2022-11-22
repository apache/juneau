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
package org.apache.juneau.cp;

import static org.apache.juneau.internal.CollectionUtils.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Utility class for instantiating a Context bean.
 *
 * <p>
 * Contains either a pre-existing Context bean, or a builder for that bean.
 * If it's a builder, then annotations can be applied to it.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The bean type.
 */
public class ContextBeanCreator<T> {

	/**
	 * Creator.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.
	 * @return A new creator object.
	 */
	public static <T> ContextBeanCreator<T> create(Class<T> type) {
		return new ContextBeanCreator<>(type);
	}

	private Class<T> type;
	private T impl;
	private Context.Builder builder;

	/**
	 * Constructor.
	 *
	 * @param type The bean type.
	 */
	protected ContextBeanCreator(Class<T> type) {
		this.type = type;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The creator to copy from.
	 */
	protected ContextBeanCreator(ContextBeanCreator<T> copyFrom) {
		this.type = copyFrom.type;
		this.impl = copyFrom.impl;
		this.builder = copyFrom.builder == null ? null : copyFrom.builder.copy();
	}

	/**
	 * Sets an already instantiated object on this creator.
	 *
	 * @param value The bean to set.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public ContextBeanCreator<T> impl(Object value) {
		this.impl = (T)value;
		return this;
	}

	/**
	 * Sets the implementation type of the bean.
	 *
	 * <p>
	 * The class type must extend from {@link Context} and have a builder create method.
	 *
	 * @param value The bean type.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public ContextBeanCreator<T> type(Class<? extends T> value) {
		builder = Context.createBuilder((Class<? extends Context>) value);
		if (builder == null)
			throw new RuntimeException("Creator for class {0} not found." + value.getName());
		return this;
	}

	/**
	 * Returns access to the inner builder if the builder exists and is of the specified type.
	 *
	 * @param <B> The builder class type.
	 * @param c The builder class type.
	 * @return An optional containing the builder if it exists.
	 */
	public <B extends Context.Builder> Optional<B> builder(Class<B> c) {
		return optional(c.isInstance(builder) ? c.cast(builder) : null);
	}

	/**
	 * Applies an operation to the builder in this creator object.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 * The operation is ignored if the builder isn't the specified type.
	 *
	 * @param <B> The builder class type.
	 * @param c The builder class type.
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public <B extends Context.Builder> ContextBeanCreator<T> builder(Class<B> c, Consumer<B> operation) {
		if (c.isInstance(builder))
			operation.accept(c.cast(builder));
		return this;
	}

	/**
	 * Returns true if any of the annotations/appliers can be applied to the inner builder (if it has one).
	 *
	 * @param work The work to check.
	 * @return This object.
	 */
	public boolean canApply(AnnotationWorkList work) {
		if (builder != null)
			return (builder.canApply(work));
		return false;
	}

	/**
	 * Applies the specified annotations to all applicable serializer builders in this group.
	 *
	 * @param work The annotations to apply.
	 * @return This object.
	 */
	public ContextBeanCreator<T> apply(AnnotationWorkList work) {
		if (builder != null)
			builder.apply(work);
		return this;
	}

	/**
	 * Creates a new copy of this creator.
	 *
	 * @return A new copy of this creator.
	 */
	public ContextBeanCreator<T> copy() {
		return new ContextBeanCreator<>(this);
	}

	/**
	 * Returns the built bean.
	 *
	 * @return The built bean.
	 */
	@SuppressWarnings("unchecked")
	public T create() {
		if (impl != null)
			return impl;
		if (builder != null)
			return (T)builder.build();
		return null;
	}
}
