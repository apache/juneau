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
package org.apache.juneau.xml.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * A concrete implementation of the {@link Xml} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class XmlAnnotation implements Xml {

	private String
		on = "",
		childName = "",
		namespace = "",
		prefix = "";
	private XmlFormat
		format = XmlFormat.DEFAULT;

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Xml#on()}
	 */
	public XmlAnnotation(String on) {
		this.on = on;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Xml.class;
	}

	@Override
	public String childName() {
		return childName;
	}

	/**
	 * Sets the <c>childName</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlAnnotation childName(String value) {
		this.childName = value;
		return this;
	}

	@Override
	public XmlFormat format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlAnnotation format(XmlFormat value) {
		this.format = value;
		return this;
	}

	@Override
	public String namespace() {
		return namespace;
	}

	/**
	 * Sets the <c>namespace</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlAnnotation namespace(String value) {
		this.namespace = value;
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
	public XmlAnnotation on(String value) {
		this.on = value;
		return this;
	}

	@Override
	public String prefix() {
		return prefix;
	}

	/**
	 * Sets the <c>prefix</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlAnnotation prefix(String value) {
		this.prefix = value;
		return this;
	}
}
