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
package org.apache.juneau.http.part;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is a boolean.
 *
 * <p>
 * Mirrors the semantics of {@code BasicBooleanPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency. The wire value uses {@code "true"} / {@code "false"}.
 *
 * @since 9.5.0
 */
public class HttpBooleanPart extends HttpPartBean {

	/**
	 * Creates an {@link HttpBooleanPart} with the given name and boolean value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The boolean value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpBooleanPart of(String name, Boolean value) {
		return new HttpBooleanPart(name, value);
	}

	/**
	 * Creates an {@link HttpBooleanPart} by parsing the given string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpBooleanPart ofString(String name, String value) {
		return new HttpBooleanPart(name, value);
	}

	/**
	 * Creates an {@link HttpBooleanPart} with a lazy boolean supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the boolean value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpBooleanPart ofLazy(String name, Supplier<Boolean> supplier) {
		return new HttpBooleanPart(name, supplier);
	}

	private final Boolean typedValue;
	private final Supplier<Boolean> typedSupplier;

	/**
	 * Constructor accepting a wire-format string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value (e.g. {@code "true"}). May be <jk>null</jk> or empty.
	 */
	public HttpBooleanPart(String name, String value) {
		super(name, value);
		this.typedValue = e(value) ? null : Boolean.valueOf(bool(value));
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a {@link Boolean} value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The boolean value. May be <jk>null</jk>.
	 */
	public HttpBooleanPart(String name, Boolean value) {
		super(name, value == null ? null : value.toString());
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy {@link Boolean} supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the boolean value. Must not be <jk>null</jk>.
	 */
	public HttpBooleanPart(String name, Supplier<Boolean> supplier) {
		super(name, () -> {
			var v = supplier.get();
			return v == null ? null : v.toString();
		});
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as an {@link Optional}{@code <Boolean>}.
	 *
	 * @return The boolean value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<Boolean> asBoolean() {
		return opt(toBoolean());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public Boolean orElse(Boolean other) {
		var x = toBoolean();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the value as a {@link Boolean}, or {@code null} if not set.
	 *
	 * @return The boolean value, possibly {@code null}.
	 */
	public Boolean toBoolean() {
		if (typedSupplier != null)
			return typedSupplier.get();
		return typedValue;
	}
}
