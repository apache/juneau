/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.function.*;

/**
 * Base for long headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpLongHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_LONG = 1;

	private final Long value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpLongHeader} by parsing the given wire-format value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value. May be {@code null} or empty.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpLongHeader of(String name, String wireValue) {
		return new HttpLongHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpLongHeader} with the given typed value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValue The long value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpLongHeader of(String name, Long typedValue) {
		return new HttpLongHeader(name, typedValue);
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param wireValue Wire value. Can be <jk>null</jk> or empty, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpLongHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = toLong(wireValue);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param typedValue The long value. Can be <jk>null</jk>.
	 */
	protected HttpLongHeader(String name, Long typedValue) {
		super(name, s(typedValue));
		this.value = typedValue;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param supplier The lazy value supplier. Must not be <jk>null</jk>.
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_LONG}.
	 */
	protected HttpLongHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> s(((Supplier<Long>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<Long> asLong() {
		return o(toLong());
	}

	/**
	 * Returns the wire-format value of this header.
	 *
	 * @return The wire value, or <jk>null</jk> if the value is unset.
	 */
	@Override
	public String getValue() {
		return s(toLong());
	}

	/**
	 * Returns the parsed value of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk>.
	 * @return The parsed value, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public Long orElse(Long other) {
		return asLong().orElse(other);
	}

	/**
	 * Returns the parsed value of this header.
	 *
	 * @return The parsed value, or <jk>null</jk> if the value is unset.
	 */
	public Long toLong() {
		if (lazyMode == LAZY_LONG)
			return ((Supplier<Long>) lazySupplier).get();
		if (value != null)
			return value;
		return toLong(super.getValue());
	}

	private static Long toLong(String value) {
		if (value == null)
			return null;
		return parseLong(value, () -> iaex("Value '%s' could not be parsed as a long.", value));
	}
}
