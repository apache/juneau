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
package org.apache.juneau.rest;

import static org.apache.juneau.httppart.HttpPartType.*;

import java.time.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.assertions.*;

/**
 * Represents a single header on an HTTP request.
 */
public class RequestHeader extends RequestHttpPart implements Header {

	private final String value;
	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The header name.
	 * @param value The header value.
	 */
	public RequestHeader(RestRequest request, String name, String value) {
		super(HEADER, request, name);
		this.value = value;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RequestHttpPart */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the value of this header as an integer.
	 *
	 * @return The value of this header as an integer, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Integer> asInteger() {
		return asIntegerHeader().asInteger();
	}

	/**
	 * Returns the value of this header as a boolean.
	 *
	 * @return The value of this header as a boolean, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Boolean> asBoolean() {
		return asBooleanHeader().asBoolean();
	}

	/**
	 * Returns the value of this header as a long.
	 *
	 * @return The value of this header as a long, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Long> asLong() {
		return asLongHeader().asLong();
	}

	/**
	 * Returns the value of this header as a date.
	 *
	 * @return The value of this header as a date, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<ZonedDateTime> asDate() {
		return asDateHeader().asZonedDateTime();
	}

	/**
	 * Returns the value of this header as a list from a comma-delimited string.
	 *
	 * @return The value of this header as a list from a comma-delimited string, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<List<String>> asCsvArray() {
		return asCsvArrayHeader().asList();
	}

	/**
	 * Returns the value of this header as a {@link BasicHeader}.
	 *
	 * @param c The subclass of {@link BasicHeader} to instantiate.
	 * @param <T> The subclass of {@link BasicHeader} to instantiate.
	 * @return The value of this header as a string, never <jk>null</jk>.
	 */
	public <T extends BasicHeader> T asHeader(Class<T> c) {
		try {
			ClassInfo ci = ClassInfo.of(c);
			ConstructorInfo cc = ci.getConstructor(Visibility.PUBLIC, String.class);
			if (cc != null)
				return cc.invoke(orElse(null));
			cc = ci.getConstructor(Visibility.PUBLIC, String.class, String.class);
			if (cc != null)
				return cc.invoke(getName(), orElse(null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new BasicRuntimeException("Could not determine a method to construct type {0}", c.getClass().getName());
	}

	/**
	 * Returns the value of this header as a {@link BasicCsvArrayHeader}.
	 *
	 * @return The value of this header as a  {@link BasicCsvArrayHeader}, never <jk>null</jk>.
	 */
	public BasicCsvArrayHeader asCsvArrayHeader() {
		return new BasicCsvArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicDateHeader}.
	 *
	 * @return The value of this header as a {@link BasicDateHeader}, never <jk>null</jk>.
	 */
	public BasicDateHeader asDateHeader() {
		return new BasicDateHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicEntityTagArrayHeader}.
	 *
	 * @return The value of this header as a {@link BasicEntityTagArrayHeader}, never <jk>null</jk>.
	 */
	public BasicEntityTagArrayHeader asEntityTagArrayHeader() {
		return new BasicEntityTagArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicEntityTagHeader}.
	 *
	 * @return The value of this header as a {@link BasicEntityTagHeader}, never <jk>null</jk>.
	 */
	public BasicEntityTagHeader asEntityTagHeader() {
		return new BasicEntityTagHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicIntegerHeader}.
	 *
	 * @return The value of this header as a {@link BasicIntegerHeader}, never <jk>null</jk>.
	 */
	public BasicIntegerHeader asIntegerHeader() {
		return new BasicIntegerHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicBooleanHeader}.
	 *
	 * @return The value of this header as a {@link BasicBooleanHeader}, never <jk>null</jk>.
	 */
	public BasicBooleanHeader asBooleanHeader() {
		return new BasicBooleanHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicLongHeader}.
	 *
	 * @return The value of this header as a {@link BasicLongHeader}, never <jk>null</jk>.
	 */
	public BasicLongHeader asLongHeader() {
		return new BasicLongHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicStringRangeArrayHeader}.
	 *
	 * @return The value of this header as a {@link BasicStringRangeArrayHeader}, never <jk>null</jk>.
	 */
	public BasicStringRangeArrayHeader asStringRangeArrayHeader() {
		return new BasicStringRangeArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicStringHeader}.
	 *
	 * @return The value of this header as a {@link BasicStringHeader}, never <jk>null</jk>.
	 */
	public BasicStringHeader asStringHeader() {
		return new BasicStringHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicUriHeader}.
	 *
	 * @return The value of this header as a {@link BasicUriHeader}, never <jk>null</jk>.
	 */
	public BasicUriHeader asUriHeader() {
		return new BasicUriHeader(getName(), getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this request header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"Content-Type"</js>)
	 * 		.assertValue().is(<js>"application/json"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentRequestHeaderAssertion<RequestHeader> assertValue() {
		return new FluentRequestHeaderAssertion<>(this, this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws BasicHttpException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws BasicHttpException {
		return new HeaderElement[0];
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestHeader schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestHeader parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}

	// </FluentSetters>
}
