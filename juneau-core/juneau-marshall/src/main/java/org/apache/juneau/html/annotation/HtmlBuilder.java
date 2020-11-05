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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;

/**
 * Builder class for the {@link Html} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class HtmlBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Html DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static HtmlBuilder create() {
		return new HtmlBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static HtmlBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static HtmlBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Html {

		private boolean noTableHeaders, noTables;
		private Class<? extends HtmlRender> render;
		private final String anchorText, link;
		private HtmlFormat format;

		Impl(HtmlBuilder b) {
			super(b);
			this.anchorText = b.anchorText;
			this.format = b.format;
			this.link = b.link;
			this.noTableHeaders = b.noTableHeaders;
			this.noTables = b.noTables;
			this.render = b.render;
			postConstruct();
		}

		@Override /* Html */
		public String anchorText() {
			return anchorText;
		}

		@Override /* Html */
		public HtmlFormat format() {
			return format;
		}

		@Override /* Html */
		public String link() {
			return link;
		}

		@Override /* Html */
		public boolean noTableHeaders() {
			return noTableHeaders;
		}

		@Override /* Html */
		public boolean noTables() {
			return noTables;
		}

		@Override /* Html */
		public Class<? extends HtmlRender> render() {
			return render;
		}
	}


	String anchorText="", link="";
	HtmlFormat format=HtmlFormat.HTML;
	boolean noTableHeaders, noTables;
	Class<? extends HtmlRender> render=HtmlRender.class;

	/**
	 * Constructor.
	 */
	public HtmlBuilder() {
		super(Html.class);
	}

	/**
	 * Instantiates a new {@link Html @Html} object initialized with this builder.
	 *
	 * @return A new {@link Html @Html} object.
	 */
	public Html build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Html#anchorText()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder anchorText(String value) {
		this.anchorText = value;
		return this;
	}

	/**
	 * Sets the {@link Html#format()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder format(HtmlFormat value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Html#link()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder link(String value) {
		this.link = value;
		return this;
	}

	/**
	 * Sets the {@link Html#noTableHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder noTableHeaders(boolean value) {
		this.noTableHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link Html#noTables()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder noTables(boolean value) {
		this.noTables = value;
		return this;
	}

	/**
	 * Sets the {@link Html#render()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlBuilder render(Class<? extends HtmlRender> value) {
		this.render = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public HtmlBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public HtmlBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public HtmlBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public HtmlBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public HtmlBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
