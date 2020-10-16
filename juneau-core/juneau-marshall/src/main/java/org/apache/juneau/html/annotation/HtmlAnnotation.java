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
 * A concrete implementation of the {@link Html} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class HtmlAnnotation extends TargetedAnnotation.OnClassMethodField implements Html {

	private String
		anchorText = "",
		link = "";
	private HtmlFormat
		format = HtmlFormat.HTML;
	private boolean
		noTableHeaders = false,
		noTables = false;
	private Class<? extends HtmlRender>
		render = HtmlRender.class;

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Html#on()}
	 */
	public HtmlAnnotation(String...on) {
		on(on);
	}

	@Override
	public String anchorText() {
		return anchorText;
	}

	/**
	 * Sets the <c>anchorText</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation anchorText(String value) {
		this.anchorText = value;
		return this;
	}

	@Override
	public HtmlFormat format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation format(HtmlFormat value) {
		this.format = value;
		return this;
	}

	@Override
	public String link() {
		return link;
	}

	/**
	 * Sets the <c>xxx</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation link(String value) {
		this.link = value;
		return this;
	}

	@Override
	public boolean noTableHeaders() {
		return noTableHeaders;
	}

	/**
	 * Sets the <c>noTableHeaders</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation noTableHeaders(boolean value) {
		this.noTableHeaders = value;
		return this;
	}

	@Override
	public boolean noTables() {
		return noTables;
	}

	/**
	 * Sets the <c>noTables</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation noTables(boolean value) {
		this.noTables = value;
		return this;
	}

	@Override
	public Class<? extends HtmlRender> render() {
		return render;
	}

	/**
	 * Sets the <c>render</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlAnnotation render(Class<? extends HtmlRender> value) {
		this.render = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotation */
	public HtmlAnnotation on(String...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public HtmlAnnotation on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public HtmlAnnotation onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - OnClassMethodField */
	public HtmlAnnotation on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClassMethodField */
	public HtmlAnnotation on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
