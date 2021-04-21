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

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.annotation.*;

/**
 * Category of headers that consist of a single boolean value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Foo: true
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header(type="boolean")
public class BasicBooleanHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicBooleanHeader of(String name, String value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicBooleanHeader(name, value);
	}

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicBooleanHeader of(String name, Boolean value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicBooleanHeader(name, value);
	}

	/**
	 * Convenience creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicBooleanHeader of(String name, Supplier<Boolean> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicBooleanHeader(name, value);
	}


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
	 */
	public BasicBooleanHeader(String name, String value) {
		super(name, value);
		this.value = parse(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public BasicBooleanHeader(String name, Boolean value) {
		super(name, serialize(value));
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
	 */
	public BasicBooleanHeader(String name, Supplier<Boolean> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null)
			return serialize(supplier.get());
		return super.getValue();
	}

	/**
	 * Returns the header value as a boolean.
	 *
	 * @return The header value as a boolean.
	 */
	public Optional<Boolean> asBoolean() {
		if (supplier != null)
			return ofNullable(supplier.get());
		return ofNullable(value);
	}

	/**
	 * Returns <jk>true</jk> if the header value is <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if the header value is <jk>true</jk>.
	 */
	public boolean isTrue() {
		return asBoolean().orElse(false).booleanValue();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response header Foo is true.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Foo"</js>).asBooleanHeader().assertBoolean().isTrue();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentBooleanAssertion<BasicBooleanHeader> assertBoolean() {
		return new FluentBooleanAssertion<>(asBoolean().orElse(null), this);
	}

	private static String serialize(Boolean value) {
		return stringify(value);
	}

	private Boolean parse(String value) {
		return value == null ? null : Boolean.parseBoolean(value);
	}
}
