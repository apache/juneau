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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is an integer.
 *
 * <p>
 * Mirrors the semantics of {@code BasicIntegerPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S2160" // Equality is fully determined by name + getValue() in HttpPartBean; the typed fields are derived from the wire value and add no state to compare
})
public class HttpIntegerPart extends HttpPartBean {

	/**
	 * Creates an {@link HttpIntegerPart} with the given name and integer value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The integer value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpIntegerPart of(String name, Integer value) {
		return new HttpIntegerPart(name, value);
	}

	/**
	 * Creates an {@link HttpIntegerPart} by parsing the given wire value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpIntegerPart ofString(String name, String value) {
		return new HttpIntegerPart(name, value);
	}

	/**
	 * Creates an {@link HttpIntegerPart} with a lazy integer supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the integer value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpIntegerPart ofLazy(String name, Supplier<Integer> supplier) {
		return new HttpIntegerPart(name, supplier);
	}

	private final Integer typedValue;
	private final Supplier<Integer> typedSupplier;

	/**
	 * Constructor accepting a wire-format string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 */
	public HttpIntegerPart(String name, String value) {
		super(name, value);
		this.typedValue = toInteger(value);
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting an {@link Integer} value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The integer value. May be <jk>null</jk>.
	 */
	public HttpIntegerPart(String name, Integer value) {
		super(name, s(value));
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy {@link Integer} supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the integer value. Must not be <jk>null</jk>.
	 */
	public HttpIntegerPart(String name, Supplier<Integer> supplier) {
		super(name, () -> s(supplier.get()));
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as an {@link Optional}{@code <Integer>}.
	 *
	 * @return The integer value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<Integer> asInteger() {
		return o(toInteger());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public Integer orElse(Integer other) {
		return asInteger().orElse(other);
	}

	/**
	 * Returns the value as an {@link Integer}, or {@code null} if not set.
	 *
	 * @return The integer value, possibly {@code null}.
	 */
	public Integer toInteger() {
		if (typedSupplier != null)
			return typedSupplier.get();
		return typedValue;
	}

	private static Integer toInteger(String value) {
		if (ie(value))
			return null;
		return parseInt(value, () -> iaex("Value ''{0}'' could not be parsed as an integer.", value));
	}
}
