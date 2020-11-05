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
package org.apache.juneau.http.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;

/**
 * Builder class for the {@link Request} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class RequestBuilder extends TargetedAnnotationTBuilder {

	/** Default value */
	public static final Request DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static RequestBuilder create() {
		return new RequestBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static RequestBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static RequestBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Request {

		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;

		Impl(RequestBuilder b) {
			super(b);
			this.parser = b.parser;
			this.serializer = b.serializer;
			postConstruct();
		}

		@Override /* Request */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Request */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}
	}


	Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;

	/**
	 * Constructor.
	 */
	public RequestBuilder() {
		super(Request.class);
	}

	/**
	 * Instantiates a new {@link Request @Request} object initialized with this builder.
	 *
	 * @return A new {@link Request @Request} object.
	 */
	public Request build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Request#parser} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RequestBuilder parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	/**
	 * Sets the {@link Request#serializer} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RequestBuilder serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public RequestBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public RequestBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public RequestBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
