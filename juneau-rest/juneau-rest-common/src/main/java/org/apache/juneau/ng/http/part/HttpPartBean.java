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
package org.apache.juneau.ng.http.part;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.Utils.eq;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.ng.http.*;

/**
 * Root concrete implementation of {@link HttpPart}.
 *
 * <p>
 * Holds a name/value pair representing a query parameter, form field, or path variable.  The value may be
 * supplied eagerly (as a {@link String}) or lazily (via a {@link Supplier}).  A {@code null} value signals
 * that the part should be omitted from the request.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// Eager</jc>
 * 	HttpPart <jv>p1</jv> = HttpPartBean.<jsm>of</jsm>(<js>"status"</js>, <js>"active"</js>);
 *
 * 	<jc>// Lazy — evaluated at request-send time</jc>
 * 	HttpPart <jv>p2</jv> = HttpPartBean.<jsm>of</jsm>(<js>"page"</js>, () -> String.valueOf(currentPage()));
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class HttpPartBean implements HttpPart {

	private final String name;
	private final Supplier<String> valueSupplier;

	/**
	 * Constructor.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 */
	protected HttpPartBean(String name, String value) {
		this.name = assertArgNotNull("name", name);
		this.valueSupplier = () -> value;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier that provides the value at request time. Must not be <jk>null</jk>.
	 */
	protected HttpPartBean(String name, Supplier<String> valueSupplier) {
		this.name = assertArgNotNull("name", name);
		this.valueSupplier = assertArgNotNull("valueSupplier", valueSupplier);
	}

	/**
	 * Creates an {@link HttpPartBean} with an eager string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpPartBean of(String name, String value) {
		return new HttpPartBean(name, value);
	}

	/**
	 * Creates an {@link HttpPartBean} with a lazy value supplier.
	 *
	 * <p>
	 * The supplier is evaluated at request-send time; if it returns {@code null} the part is omitted.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the part value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpPartBean of(String name, Supplier<String> valueSupplier) {
		return new HttpPartBean(name, valueSupplier);
	}

	@Override /* HttpPart */
	public String getName() {
		return name;
	}

	@Override /* HttpPart */
	public String getValue() {
		return valueSupplier.get();
	}

	@Override /* Object */
	public String toString() {
		return name + "=" + getValue();
	}

	@Override /* Object */
	public boolean equals(Object obj) {
		return (obj instanceof HttpPartBean o2) && eq(this, o2, (x, y) -> eq(x.name, y.name) && eq(x.getValue(), y.getValue()));
	}

	@Override /* Object */
	public int hashCode() {
		return Objects.hash(name, getValue());
	}
}
