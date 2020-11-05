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
package org.apache.juneau.urlencoding.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link UrlEncoding} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class UrlEncodingBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final UrlEncoding DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static UrlEncodingBuilder create() {
		return new UrlEncodingBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static UrlEncodingBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static UrlEncodingBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements UrlEncoding {

		private final boolean expandedParams;

		Impl(UrlEncodingBuilder b) {
			super(b);
			this.expandedParams = b.expandedParams;
			postConstruct();
		}


		@Override /* UrlEncoding */
		public boolean expandedParams() {
			return expandedParams;
		}
	}

	boolean expandedParams;

	/**
	 * Constructor.
	 */
	public UrlEncodingBuilder() {
		super(UrlEncoding.class);
	}

	/**
	 * Instantiates a new {@link UrlEncoding @UrlEncoding} object initialized with this builder.
	 *
	 * @return A new {@link UrlEncoding @UrlEncoding} object.
	 */
	public UrlEncoding build() {
		return new Impl(this);
	}


	/**
	 * Sets the {@link UrlEncoding#expandedParams} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public UrlEncodingBuilder expandedParams(boolean value) {
		this.expandedParams = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public UrlEncodingBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public UrlEncodingBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public UrlEncodingBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public UrlEncodingBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public UrlEncodingBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}