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


import static org.apache.juneau.commons.utils.StringUtils.parseInt;
import static org.apache.juneau.commons.utils.ThrowableUtils.illegalArg;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * Base for integer headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpIntegerHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_INTEGER = 1;

	private final Integer value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpIntegerHeader} by parsing the given wire-format value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value. May be {@code null} or empty.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpIntegerHeader of(String name, String wireValue) {
		return new HttpIntegerHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpIntegerHeader} with the given typed value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValue The integer value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpIntegerHeader of(String name, Integer typedValue) {
		return new HttpIntegerHeader(name, typedValue);
	}

	protected HttpIntegerHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = toInteger(wireValue);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpIntegerHeader(String name, Integer typedValue) {
		super(name, s(typedValue));
		this.value = typedValue;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpIntegerHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> s(((Supplier<Integer>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<Integer> asInteger() {
		return opt(toInteger());
	}

	@Override
	public String getValue() {
		return s(toInteger());
	}

	public Integer orElse(Integer other) {
		return asInteger().orElse(other);
	}

	public Integer toInteger() {
		if (lazyMode == LAZY_INTEGER)
			return ((Supplier<Integer>) lazySupplier).get();
		if (value != null)
			return value;
		return toInteger(super.getValue());
	}

	private static Integer toInteger(String value) {
		if (value == null)
			return null;
		return parseInt(value, () -> illegalArg("Value ''{0}'' could not be parsed as an integer.", value));
	}
}
