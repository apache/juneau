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

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.httppart.*;
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
	// Assertions
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
