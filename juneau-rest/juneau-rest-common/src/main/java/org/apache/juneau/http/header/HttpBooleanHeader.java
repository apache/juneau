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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.utils.*;

/**
 * Base for boolean headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpBooleanHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_BOOLEAN = 1;

	private final Boolean value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpBooleanHeader} by parsing the given wire-format value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value (e.g. {@code "true"}). May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpBooleanHeader of(String name, String wireValue) {
		return new HttpBooleanHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpBooleanHeader} with the given typed value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValue The boolean value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpBooleanHeader of(String name, Boolean typedValue) {
		return new HttpBooleanHeader(name, typedValue);
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param wireValue Wire value (e.g. {@code "true"}). Can be <jk>null</jk> or empty, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpBooleanHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = ie(wireValue) ? null : Boolean.valueOf(b(wireValue));
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param typedValue The boolean value. Can be <jk>null</jk>, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpBooleanHeader(String name, Boolean typedValue) {
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
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_BOOLEAN}.
	 */
	protected HttpBooleanHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> s(((Supplier<Boolean>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<Boolean> asBoolean() {
		return o(toBoolean());
	}

	/**
	 * Returns the wire-format value of this header.
	 *
	 * @return The wire value, or <jk>null</jk> if the value is unset.
	 */
	@Override
	public String getValue() {
		return s(toBoolean());
	}

	/**
	 * Returns <jk>true</jk> if the parsed value is <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if the parsed value is <jk>true</jk>; <jk>false</jk> if it is <jk>false</jk> or unset.
	 */
	public boolean isTrue() {
		return ObjectUtils.isTrue(toBoolean());
	}

	/**
	 * Returns the parsed value of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk> to allow a <jk>null</jk> result when the header is unset.
	 * @return The parsed value, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public Boolean orElse(Boolean other) {
		var x = toBoolean();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the parsed value of this header.
	 *
	 * @return The parsed value, or <jk>null</jk> if the value is unset.
	 */
	public Boolean toBoolean() {
		if (lazyMode == LAZY_BOOLEAN)
			return ((Supplier<Boolean>) lazySupplier).get();
		if (value != null)
			return value;
		var v = super.getValue();
		return ie(v) ? null : Boolean.valueOf(b(v));
	}
}
