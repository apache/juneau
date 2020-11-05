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

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;

/**
 * Builder class for the {@link Example} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ExampleBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Example DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static ExampleBuilder create() {
		return new ExampleBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static ExampleBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static ExampleBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Example {

		private final String value;

		Impl(ExampleBuilder b) {
			super(b);
			this.value = b.value;
			postConstruct();
		}

		@Override /* Example */
		public String value() {
			return value;
		}
	}


	String value="";

	/**
	 * Constructor.
	 */
	public ExampleBuilder() {
		super(Example.class);
	}

	/**
	 * Instantiates a new {@link Example @Example} object initialized with this builder.
	 *
	 * @return A new {@link Example @Example} object.
	 */
	public Example build() {
		return new Impl(this);
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExampleBuilder value(String value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public ExampleBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public ExampleBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public ExampleBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public ExampleBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public ExampleBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
