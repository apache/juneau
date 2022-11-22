// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http.header;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.annotation.*;

/**
 * Category of headers that consist of a single boolean value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Foo: true
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header
@Schema(type="boolean")
public class BasicBooleanHeader extends BasicHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicBooleanHeader of(String name, String value) {
		return value == null ? null : new BasicBooleanHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicBooleanHeader of(String name, Boolean value) {
		return value == null ? null : new BasicBooleanHeader(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicBooleanHeader of(String name, Supplier<Boolean> value) {
		return value == null ? null : new BasicBooleanHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Boolean value;
	private final Supplier<Boolean> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicBooleanHeader(String name, String value) {
		super(name, value);
		this.value = isEmpty(value) ? null : Boolean.valueOf(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicBooleanHeader(String name, Boolean value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicBooleanHeader(String name, Supplier<Boolean> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		return stringify(value());
	}

	/**
	 * Returns the header value as a {@link Boolean} wrapped in an {@link Optional}.
	 *
	 * @return The header value as a {@link Boolean} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> asBoolean() {
		return optional(value());
	}

	/**
	 * Returns the header value as a {@link Boolean}.
	 *
	 * @return The header value as a {@link Boolean}.  Can be <jk>null</jk>.
	 */
	public Boolean toBoolean() {
		return value();
	}

	/**
	 * Returns <jk>true</jk> if the header value is <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if the header value is <jk>true</jk>.
	 */
	public boolean isTrue() {
		Boolean x = value();
		return x == null ? value : x;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response header Foo is true.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Foo"</js>).asBooleanHeader().assertBoolean().isTrue();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentBooleanAssertion<BasicBooleanHeader> assertBoolean() {
		return new FluentBooleanAssertion<>(value(), this);
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asBoolean().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public Boolean orElse(Boolean other) {
		Boolean x = value();
		return x != null ? x : other;
	}

	private Boolean value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
