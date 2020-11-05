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
package org.apache.juneau.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;

/**
 * Builder class for the {@link Swap} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class SwapBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Swap DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static SwapBuilder create() {
		return new SwapBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SwapBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SwapBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Swap {

		private final Class<?> impl, value;
		private final String template;
		private final String[] mediaTypes;

		Impl(SwapBuilder b) {
			super(b);
			this.impl = b.impl;
			this.mediaTypes = copyOf(b.mediaTypes);
			this.template = b.template;
			this.value = b.value;
			postConstruct();
		}

		@Override /* Swap */
		public Class<?> impl() {
			return impl;
		}

		@Override /* Swap */
		public String[] mediaTypes() {
			return mediaTypes;
		}

		@Override /* Swap */
		public String template() {
			return template;
		}

		@Override /* Swap */
		public Class<?> value() {
			return value;
		}
	}


	Class<?> impl=Null.class, value=Null.class;
	String template="";
	String[] mediaTypes={};

	/**
	 * Constructor.
	 */
	public SwapBuilder() {
		super(Swap.class);
	}

	/**
	 * Instantiates a new {@link Swap @Swap} object initialized with this builder.
	 *
	 * @return A new {@link Swap @Swap} object.
	 */
	public Swap build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Swap#impl()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SwapBuilder impl(Class<?> value) {
		this.impl = value;
		return this;
	}

	/**
	 * Sets the {@link Swap#mediaTypes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SwapBuilder mediaTypes(String...value) {
		this.mediaTypes = value;
		return this;
	}

	/**
	 * Sets the {@link Swap#template()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SwapBuilder template(String value) {
		this.template = value;
		return this;
	}

	/**
	 * Sets the {@link Swap#value()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SwapBuilder value(Class<?> value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public SwapBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SwapBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SwapBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SwapBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SwapBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
