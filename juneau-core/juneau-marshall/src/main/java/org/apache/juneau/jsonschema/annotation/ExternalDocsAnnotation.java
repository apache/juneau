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
package org.apache.juneau.jsonschema.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * A concrete implementation of the {@link ExternalDocs} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ExternalDocsAnnotation implements ExternalDocs {

	private String
		url = "";
	private String[]
		description = new String[0],
		value = new String[0];

	@Override
	public Class<? extends Annotation> annotationType() {
		return ExternalDocs.class;
	}

	@Override
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExternalDocsAnnotation description(String...value) {
		this.description = value;
		return this;
	}

	@Override
	public String url() {
		return url;
	}

	/**
	 * Sets the <c>url</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExternalDocsAnnotation url(String value) {
		this.url = value;
		return this;
	}

	@Override
	public String[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExternalDocsAnnotation value(String...value) {
		this.value = value;
		return this;
	}
}
