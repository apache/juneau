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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is a comma-delimited list of strings.
 *
 * <p>
 * Mirrors the semantics of {@code BasicCsvArrayPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency.
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S2160" // Equality is fully determined by name + getValue() in HttpPartBean; the typed fields are derived from the wire value and add no state to compare
})
public class HttpCsvArrayPart extends HttpPartBean {

	private static final String[] EMPTY = {};

	/**
	 * Creates an {@link HttpCsvArrayPart} with the given name and array value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The array of values. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpCsvArrayPart of(String name, String...value) {
		return new HttpCsvArrayPart(name, value);
	}

	/**
	 * Creates an {@link HttpCsvArrayPart} by parsing the given comma-delimited wire value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpCsvArrayPart ofString(String name, String value) {
		return new HttpCsvArrayPart(name, value);
	}

	/**
	 * Creates an {@link HttpCsvArrayPart} with a lazy array supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the array value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpCsvArrayPart ofLazy(String name, Supplier<String[]> supplier) {
		return new HttpCsvArrayPart(name, supplier);
	}

	private final String[] typedValue;
	private final Supplier<String[]> typedSupplier;

	/**
	 * Constructor accepting an array of values.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The array of values. May be <jk>null</jk>.
	 */
	public HttpCsvArrayPart(String name, String...value) {
		super(name, value == null ? null : join(value, ','));
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a comma-delimited wire string.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 */
	public HttpCsvArrayPart(String name, String value) {
		super(name, value);
		this.typedValue = splita(value);
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy supplier of array values.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the array value. Must not be <jk>null</jk>.
	 */
	public HttpCsvArrayPart(String name, Supplier<String[]> supplier) {
		super(name, () -> {
			var v = supplier.get();
			return v == null ? null : join(v, ',');
		});
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as a copy of the underlying array, wrapped in an {@link Optional}.
	 *
	 * @return The array value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<String[]> asArray() {
		return opt(toArray());
	}

	/**
	 * Returns the value as an unmodifiable {@link List}, wrapped in an {@link Optional}.
	 *
	 * @return The list value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<List<String>> asList() {
		return opt(toList());
	}

	/**
	 * Returns {@code true} if this part contains the given value (case-sensitive).
	 *
	 * @param val The value to check for.
	 * @return {@code true} if the value is present.
	 */
	public boolean contains(String val) {
		if (val == null)
			return false;
		var arr = value();
		if (arr == null)
			return false;
		for (var v : arr)
			if (eq(v, val))
				return true;
		return false;
	}

	/**
	 * Returns {@code true} if this part contains the given value (case-insensitive).
	 *
	 * @param val The value to check for.
	 * @return {@code true} if the value is present.
	 */
	public boolean containsIgnoreCase(String val) {
		if (val == null)
			return false;
		var arr = value();
		if (arr == null)
			return false;
		for (var v : arr)
			if (eqic(v, val))
				return true;
		return false;
	}

	/**
	 * Returns a copy of the underlying array, or {@code null} if not set.
	 *
	 * @return The array, possibly {@code null}.
	 */
	public String[] toArray() {
		return copyOf(value());
	}

	/**
	 * Returns the value as an unmodifiable {@link List}, or {@code null} if not set.
	 *
	 * @return The list, possibly {@code null}.
	 */
	public List<String> toList() {
		return u(l(value()));
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public String[] orElse(String[] other) {
		var x = value();
		return nn(x) ? x : other;
	}

	private String[] value() {
		if (typedSupplier != null) {
			var v = typedSupplier.get();
			return v == null ? EMPTY : v;
		}
		return typedValue;
	}
}
