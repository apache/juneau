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
 * A concrete implementation of the {@link HtmlLink} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class HtmlLinkAnnotation extends TargetedAnnotation.OnClass implements HtmlLink {

	private String
		nameProperty = "",
		uriProperty = "";

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link HtmlLink#on()}
	 */
	public HtmlLinkAnnotation(String...on) {
		on(on);
	}

	@Override
	public String nameProperty() {
		return nameProperty;
	}

	/**
	 * Sets the <c>nameProperty</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlLinkAnnotation nameProperty(String value) {
		this.nameProperty = value;
		return this;
	}

	@Override
	public String uriProperty() {
		return uriProperty;
	}

	/**
	 * Sets the <c>uriProperty</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlLinkAnnotation uriProperty(String value) {
		this.uriProperty = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotation */
	public HtmlLinkAnnotation on(String...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public HtmlLinkAnnotation on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public HtmlLinkAnnotation onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
