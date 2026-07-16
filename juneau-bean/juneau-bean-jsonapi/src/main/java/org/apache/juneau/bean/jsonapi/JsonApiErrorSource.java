/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.jsonapi;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Represents the {@code source} sub-object of a JSON:API Error Object.
 *
 * <p>
 * Per <a href="https://jsonapi.org/format/#error-objects">JSON:API v1.1 &#167; Error Objects</a>, the
 * {@code source} object can carry a {@code pointer} (JSON Pointer to the offending element), a
 * {@code parameter} (the URI query parameter name), and/or a {@code header} (the request header name).
 */
@Marshalled
public class JsonApiErrorSource {

	private String pointer;
	private String parameter;
	private String header;

	/**
	 * Default constructor.
	 */
	public JsonApiErrorSource() { /* intentionally empty */ }

	/**
	 * Bean property getter:  <property>pointer</property>.
	 *
	 * @return The value of the <property>pointer</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getPointer() { return pointer; }

	/**
	 * Bean property setter:  <property>pointer</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiErrorSource setPointer(String value) {
		pointer = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>parameter</property>.
	 *
	 * @return The value of the <property>parameter</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getParameter() { return parameter; }

	/**
	 * Bean property setter:  <property>parameter</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiErrorSource setParameter(String value) {
		parameter = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>header</property>.
	 *
	 * @return The value of the <property>header</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getHeader() { return header; }

	/**
	 * Bean property setter:  <property>header</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public JsonApiErrorSource setHeader(String value) {
		header = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}
}
