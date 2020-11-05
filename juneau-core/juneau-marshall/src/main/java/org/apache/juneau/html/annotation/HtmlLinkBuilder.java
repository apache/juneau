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
package org.apache.juneau.html.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link HtmlLink} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class HtmlLinkBuilder extends TargetedAnnotationTBuilder {

	/** Default value */
	public static final HtmlLink DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static HtmlLinkBuilder create() {
		return new HtmlLinkBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static HtmlLinkBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static HtmlLinkBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements HtmlLink {

		private final String nameProperty, uriProperty;

		Impl(HtmlLinkBuilder b) {
			super(b);
			this.nameProperty = b.nameProperty;
			this.uriProperty = b.uriProperty;
			postConstruct();
		}

		@Override /* HtmlLink */
		public String nameProperty() {
			return nameProperty;
		}

		@Override /* HtmlLink */
		public String uriProperty() {
			return uriProperty;
		}
	}


	String nameProperty="", uriProperty="";

	/**
	 * Constructor.
	 */
	public HtmlLinkBuilder() {
		super(HtmlLink.class);
	}

	/**
	 * Instantiates a new {@link HtmlLink @HtmlLink} object initialized with this builder.
	 *
	 * @return A new {@link HtmlLink @HtmlLink} object.
	 */
	public HtmlLink build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link HtmlLink#nameProperty()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlLinkBuilder nameProperty(String value) {
		this.nameProperty = value;
		return this;
	}

	/**
	 * Sets the {@link HtmlLink#uriProperty()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlLinkBuilder uriProperty(String value) {
		this.uriProperty = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public HtmlLinkBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public HtmlLinkBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public HtmlLinkBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
