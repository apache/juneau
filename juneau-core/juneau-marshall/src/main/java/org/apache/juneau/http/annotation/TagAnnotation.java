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

import org.apache.juneau.jsonschema.annotation.*;

/**
 * A concrete implementation of the {@link Tag} annotation.
 */
public class TagAnnotation implements Tag {

	private String name="";
	private String[] description={}, value={};
	private ExternalDocs externalDocs = new ExternalDocsAnnotation();

	@Override /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Tag.class;
	}

	@Override /* Tag */
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public TagAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override /* Tag */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public TagAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Tag */
	public ExternalDocs externalDocs() {
		return externalDocs;
	}

	/**
	 * Sets the <c>externalDocs</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public TagAnnotation externalDocs(ExternalDocs value) {
		this.externalDocs = value;
		return this;
	}

	@Override /* Tag */
	public String[] value() {
		return value;
	}
	/**
	 * Sets the <c></c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public TagAnnotation value(String[] value) {
		this.value = value;
		return this;
	}
}
