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
package org.apache.juneau.json.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * A concrete implementation of the {@link Json} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class JsonAnnotation implements Json {

	private String on = "";
	private String wrapperAttr = "";

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Json#on()}
	 */
	public JsonAnnotation(String on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Json#on()}
	 */
	public JsonAnnotation(Class<?> on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Json#on()}
	 */
	public JsonAnnotation(Method on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Json#on()}
	 */
	public JsonAnnotation(Field on) {
		on(on);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Json.class;
	}

	@Override
	public String on() {
		return on;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JsonAnnotation on(String value) {
		this.on = value;
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JsonAnnotation on(Class<?> value) {
		this.on = value.getName();
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JsonAnnotation on(Method value) {
		this.on = MethodInfo.of(value).getFullName();
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JsonAnnotation on(Field value) {
		this.on = value.getName();
		return this;
	}

	@Override
	public String wrapperAttr() {
		return wrapperAttr;
	}

	/**
	 * Sets the <c>wrapperAttr</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JsonAnnotation wrapperAttr(String value) {
		this.wrapperAttr = value;
		return this;
	}

}
