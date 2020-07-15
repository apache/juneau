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

/**
 * A concrete implementation of the {@link Contact} annotation.
 */
public class ContactAnnotation implements Contact {

	private String name="", url="", email="";
	private String[] value={};

	@Override /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Contact.class;
	}

	@Override /* Contact */
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ContactAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override /* Contact */
	public String url() {
		return url;
	}

	/**
	 * Sets the <c>url</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ContactAnnotation url(String value) {
		this.url = value;
		return this;
	}

	@Override /* Contact */
	public String email() {
		return email;
	}

	/**
	 * Sets the <c>email</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ContactAnnotation email(String value) {
		this.email = value;
		return this;
	}

	@Override /* Contact */
	public String[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ContactAnnotation value(String[]  value) {
		this.value = value;
		return this;
	}
}
