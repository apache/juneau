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

import static org.apache.juneau.commons.utils.StringUtils.parseLong;
import static org.apache.juneau.commons.utils.ThrowableUtils.illegalArg;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is a long integer.
 *
 * <p>
 * Mirrors the semantics of {@code BasicLongPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S2160" // Equality is fully determined by name + getValue() in HttpPartBean; the typed fields are derived from the wire value and add no state to compare
})
public class HttpLongPart extends HttpPartBean {

	/**
	 * Creates an {@link HttpLongPart} with the given name and long value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The long value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpLongPart of(String name, Long value) {
		return new HttpLongPart(name, value);
	}

	/**
	 * Creates an {@link HttpLongPart} by parsing the given wire value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpLongPart ofString(String name, String value) {
		return new HttpLongPart(name, value);
	}

	/**
	 * Creates an {@link HttpLongPart} with a lazy long supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the long value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpLongPart ofLazy(String name, Supplier<Long> supplier) {
		return new HttpLongPart(name, supplier);
	}

	private final Long typedValue;
	private final Supplier<Long> typedSupplier;

	/**
	 * Constructor accepting a wire-format string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 */
	public HttpLongPart(String name, String value) {
		super(name, value);
		this.typedValue = toLong(value);
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a {@link Long} value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The long value. May be <jk>null</jk>.
	 */
	public HttpLongPart(String name, Long value) {
		super(name, s(value));
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy {@link Long} supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the long value. Must not be <jk>null</jk>.
	 */
	public HttpLongPart(String name, Supplier<Long> supplier) {
		super(name, () -> s(supplier.get()));
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as an {@link Optional}{@code <Long>}.
	 *
	 * @return The long value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<Long> asLong() {
		return opt(toLong());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public Long orElse(Long other) {
		return asLong().orElse(other);
	}

	/**
	 * Returns the value as a {@link Long}, or {@code null} if not set.
	 *
	 * @return The long value, possibly {@code null}.
	 */
	public Long toLong() {
		if (typedSupplier != null)
			return typedSupplier.get();
		return typedValue;
	}

	private static Long toLong(String value) {
		if (e(value))
			return null;
		return parseLong(value, () -> illegalArg("Value ''{0}'' could not be parsed as a long.", value));
	}
}
