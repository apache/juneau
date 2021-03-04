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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;

/**
 * Category of headers that consist of a single string value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Host: www.myhost.com:8080
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
*/
public class BasicStringHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new {@link BasicStringHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicStringHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringHeader(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new {@link BasicStringHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicStringHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringHeader(name, value);
	}

	private String parsed;

	/**
	 * Constructor
	 *
	 * @param name The header name.
	 * @param value
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicStringHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getStringHeader(<js>"Content-Type"</js>).assertThat().exists();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentStringAssertion<BasicStringHeader> assertString() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	@Override /* Header */
	public String getValue() {
		return getParsedValue();
	}

	@Override /* BasicHeader */
	public Optional<String> asString() {
		return Optional.ofNullable(getParsedValue());
	}

	private String getParsedValue() {
		if (parsed != null)
			return parsed;
		return stringify(getRawValue());
	}
}
