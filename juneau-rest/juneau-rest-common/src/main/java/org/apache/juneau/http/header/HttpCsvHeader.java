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
 * Base for simple comma-separated token lists.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpCsvHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_TOKENS = 1;

	private final String[] eagerTokens;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpCsvHeader} by parsing the given comma-delimited wire value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value. May be {@code null} or empty.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpCsvHeader of(String name, String wireValue) {
		return new HttpCsvHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpCsvHeader} with the given typed values.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValues The token values. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpCsvHeader of(String name, String...typedValues) {
		return new HttpCsvHeader(name, typedValues);
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Wire value. Can be <jk>null</jk> or empty.
	 */
	protected HttpCsvHeader(String name, String value) {
		super(name, value);
		this.eagerTokens = null;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param values The token values. Can be <jk>null</jk>.
	 */
	protected HttpCsvHeader(String name, String... values) {
		super(name, values == null || values.length == 0 ? null : join(values, ", "));
		this.eagerTokens = values == null ? null : cp(values);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param supplier The lazy value supplier. Must not be <jk>null</jk>.
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_TOKENS}.
	 */
	protected HttpCsvHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> {
				var t = ((Supplier<String[]>) supplier).get();
				if (t == null)
					return null;
				return join(t, ", ");
			});
		this.eagerTokens = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<String[]> asArray() {
		return o(cp(csvTokens()));
	}

	public Optional<List<String>> asList() {
		return o(u(l(csvTokens())));
	}

	public boolean contains(String val) {
		var t = csvTokens();
		if (t == null)
			return false;
		for (var v : t)
			if (eq(v, val))
				return true;
		return false;
	}

	public boolean containsIgnoreCase(String val) {
		var t = csvTokens();
		if (t == null)
			return false;
		for (var v : t)
			if (eqic(v, val))
				return true;
		return false;
	}

	/**
	 * Returns the comma-delimited wire-format value of this header.
	 *
	 * @return The wire value, or <jk>null</jk> if the value is unset.
	 */
	@Override
	public String getValue() {
		var t = csvTokens();
		return t == null ? null : join(t, ", ");
	}

	/**
	 * Returns the parsed tokens of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk>.
	 * @return The parsed tokens, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public String[] orElse(String[] other) {
		var x = csvTokens();
		return nn(x) ? cp(x) : other;
	}

	/**
	 * Returns the parsed tokens of this header.
	 *
	 * @return The parsed tokens, or <jk>null</jk> if the value is unset.
	 */
	public String[] toArray() {
		return cp(csvTokens());
	}

	public List<String> toList() {
		return u(l(csvTokens()));
	}

	private String[] csvTokens() {
		if (lazyMode == LAZY_TOKENS)
			return ((Supplier<String[]>) lazySupplier).get();
		if (eagerTokens != null)
			return eagerTokens;
		return splita(super.getValue());
	}
}
