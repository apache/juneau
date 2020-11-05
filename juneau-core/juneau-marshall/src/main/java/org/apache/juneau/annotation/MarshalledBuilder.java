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

import org.apache.juneau.*;

/**
 * Builder class for the {@link Marshalled} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class MarshalledBuilder extends TargetedAnnotationTBuilder {

	/** Default value */
	public static final Marshalled DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static MarshalledBuilder create() {
		return new MarshalledBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static MarshalledBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static MarshalledBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Marshalled {

		private final Class<?> implClass;
		private final String example;

		Impl(MarshalledBuilder b) {
			super(b);
			this.example = b.example;
			this.implClass = b.implClass;
			postConstruct();
		}

		@Override /* Marshalled */
		public String example() {
			return example;
		}

		@Override /* Marshalled */
		public Class<?> implClass() {
			return implClass;
		}
	}


	Class<?> implClass=Null.class;
	String example="";

	/**
	 * Constructor.
	 */
	public MarshalledBuilder() {
		super(Marshalled.class);
	}

	/**
	 * Instantiates a new {@link Marshalled @Marshalled} object initialized with this builder.
	 *
	 * @return A new {@link Marshalled @Marshalled} object.
	 */
	public Marshalled build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Marshalled#example()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MarshalledBuilder example(String value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link Marshalled#implClass()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MarshalledBuilder implClass(Class<?> value) {
		this.implClass = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public MarshalledBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public MarshalledBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public MarshalledBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
