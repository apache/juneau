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
package org.apache.juneau.bean.openapi3;

import org.apache.juneau.commons.utils.*;

/**
 * Copy utilities for the OpenAPI 3.0 bean module.
 *
 * <p>
 * Extends {@link CopyUtils} so that a single static-import-on-demand of this class
 * (<c>import static org.apache.juneau.bean.openapi3.OpenApiCopyUtils.*;</c>) brings in both the inherited
 * collection/map/array <c>copyOf(...)</c> overloads <b>and</b> the OpenAPI bean <c>copyOf(&lt;BeanType&gt;)</c>
 * overloads declared here, resolved via ordinary Java overload resolution.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link CopyUtils}
 * </ul>
 */
public class OpenApiCopyUtils extends CopyUtils {

	/** Constructor. */
	protected OpenApiCopyUtils() {}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Components copyOf(Components value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Contact copyOf(Contact value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Discriminator copyOf(Discriminator value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ExternalDocumentation copyOf(ExternalDocumentation value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Info copyOf(Info value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Items copyOf(Items value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static License copyOf(License value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static OAuthFlow copyOf(OAuthFlow value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Operation copyOf(Operation value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static RequestBodyInfo copyOf(RequestBodyInfo value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static SchemaInfo copyOf(SchemaInfo value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Server copyOf(Server value) {
		return value == null ? null : value.copy();
	}

	/**
	 * Null-safe copy: returns a deep copy of the specified value, or <jk>null</jk> if the value is <jk>null</jk>.
	 *
	 * @param value The value to copy.  Can be <jk>null</jk>.
	 * @return A copy of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Xml copyOf(Xml value) {
		return value == null ? null : value.copy();
	}
}
