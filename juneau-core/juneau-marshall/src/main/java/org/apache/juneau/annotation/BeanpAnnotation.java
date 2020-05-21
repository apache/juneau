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
import org.apache.juneau.reflect.*;

/**
 * A concrete implementation of the {@link Beanp} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BeanpAnnotation implements Beanp {

	private String
		on = "",
		name = "",
		value = "",
		properties = "",
		format = "",
		ro = "",
		wo = "";
	private Class<?>
		type = Object.class;
	private Class<?>[]
		params = new Class[0],
		dictionary = new Class[0];

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Beanp#on()}
	 */
	public BeanpAnnotation(String on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Beanp#on()}
	 */
	public BeanpAnnotation(Method on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Beanp#on()}
	 */
	public BeanpAnnotation(Field on) {
		on(on);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Beanp.class;
	}

	@Override
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override
	public String value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation value(String value) {
		this.value = value;
		return this;
	}

	@Override
	public Class<?> type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation type(Class<?> value) {
		this.type = value;
		return this;
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
	public BeanpAnnotation on(String value) {
		this.on = value;
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation on(Method value) {
		this.on = MethodInfo.of(value).getFullName();
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation on(Field value) {
		this.on = value.getName();
		return this;
	}

	@Override
	public Class<?>[] params() {
		return params;
	}

	/**
	 * Sets the <c>params</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation params(Class<?>...value) {
		this.params = value;
		return this;
	}

	@Override
	public String properties() {
		return properties;
	}

	/**
	 * Sets the <c>properties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation properties(String value) {
		this.properties = value;
		return this;
	}

	@Override
	public Class<?>[] dictionary() {
		return dictionary;
	}

	/**
	 * Sets the <c>dictionary</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation dictionary(Class<?>...value) {
		this.dictionary = value;
		return this;
	}

	@Override
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override
	public String ro() {
		return ro;
	}

	/**
	 * Sets the <c>ro</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation ro(String value) {
		this.ro = value;
		return this;
	}

	@Override
	public String wo() {
		return wo;
	}

	/**
	 * Sets the <c>wo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpAnnotation wo(String value) {
		this.wo = value;
		return this;
	}
}
