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
 * An {@link org.apache.juneau.http.HttpPart} whose value is a string.
 *
 * <p>
 * Mirrors the semantics of {@code BasicStringPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency.
 *
 * @since 10.0.0
 */
public class HttpStringPart extends HttpPartBean {

	/**
	 * Creates an {@link HttpStringPart} with the given name and string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpStringPart of(String name, String value) {
		return new HttpStringPart(name, value);
	}

	/**
	 * Creates an {@link HttpStringPart} with the given name and lazy value supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpStringPart of(String name, Supplier<String> valueSupplier) {
		return new HttpStringPart(name, valueSupplier);
	}

	/**
	 * Parses a part pair of the form {@code "name=value"} (or {@code "name:value"}) into an
	 * {@link HttpStringPart}.
	 *
	 * <p>
	 * If the input does not contain {@code '='} or {@code ':'}, the entire string is treated as a part name
	 * with an empty value.
	 *
	 * @param pair The part pair. May be {@code null}.
	 * @return A new instance, or {@code null} if {@code pair} is {@code null}.
	 */
	public static HttpStringPart ofPair(String pair) {
		if (pair == null)
			return null;
		var i = pair.indexOf('=');
		if (i == -1)
			i = pair.indexOf(':');
		if (i == -1)
			return of(pair, "");
		return of(pair.substring(0, i).trim(), pair.substring(i + 1).trim());
	}

	/**
	 * Constructor.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 */
	protected HttpStringPart(String name, String value) {
		super(name, value);
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier that provides the value at request time. Must not be <jk>null</jk>.
	 */
	protected HttpStringPart(String name, Supplier<String> valueSupplier) {
		super(name, valueSupplier);
	}

	/**
	 * Returns the value as an {@link Optional}.
	 *
	 * @return The value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<String> asString() {
		return opt(getValue());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public String orElse(String other) {
		var x = getValue();
		return nn(x) ? x : other;
	}
}
