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
package org.apache.juneau.rest.mock2;


import java.io.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;

/**
 * Represents a single header on an HTTP response.
 *
 * <p>
 * Provides convenience methods for retrieving and asserting response header values.
 */
public class MockServletResponseHeader {

	private MockServletResponse response;
	private final String name, value;

	/**
	 * Constructor.
	 *
	 * @param response The response object.
	 * @param name The response header name.
	 * @param value The response header value, of <jk>null</jk> if header not specified.
	 */
	public MockServletResponseHeader(MockServletResponse response, String name, String value) {
		this.response = response;
		this.name = name;
		this.value = value;
	}

	/**
	 * Returns the value of this header as a string.
	 *
	 * @return The value of this header as a string, or <jk>null</jk> if header was not present.
	 */
	public String asString() {
		return value;
	}

	/**
	 * Returns the value of this header as a CSV array header.
	 *
	 * @return The value of this header as a CSV array header, or <jk>null</jk> if header was not present.
	 */
	public BasicCsvArrayHeader asCsvArrayHeader() {
		return value == null ? null : new BasicCsvArrayHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as a date header.
	 *
	 * @return The value of this header as a date header, or <jk>null</jk> if header was not present.
	 */
	public BasicDateHeader asDateHeader() {
		return value == null ? null : new BasicDateHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as an entity validator array header.
	 *
	 * @return The value of this header as an entity validator array header, or <jk>null</jk> if header was not present.
	 */
	public BasicEntityValidatorArrayHeader asEntityValidatorArrayHeader() {
		return value == null ? null : new BasicEntityValidatorArrayHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as an integer header.
	 *
	 * @return The value of this header as an integer header, or <jk>null</jk> if header was not present.
	 */
	public BasicIntegerHeader asIntegerHeader() {
		return value == null ? null : new BasicIntegerHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as a long header.
	 *
	 * @return The value of this header as a long header, or <jk>null</jk> if header was not present.
	 */
	public BasicLongHeader asLongHeader() {
		return value == null ? null : new BasicLongHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as a range array header.
	 *
	 * @return The value of this header as a range array header, or <jk>null</jk> if header was not present.
	 */
	public BasicRangeArrayHeader asRangeArrayHeader() {
		return value == null ? null : new BasicRangeArrayHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as a string header.
	 *
	 * @return The value of this header as a string header, or <jk>null</jk> if header was not present.
	 */
	public BasicHeader asHeader() {
		return value == null ? null : new BasicHeader(getName(), value);
	}

	/**
	 * Returns the value of this header as a URI header.
	 *
	 * @return The value of this header as a URI header, or <jk>null</jk> if header was not present.
	 */
	public BasicUriHeader asUriHeader() {
		return value == null ? null : new BasicUriHeader(getName(), value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists();
	 *
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).equals(<js>"application/json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).passes(x -&gt; x.equals(<js>"application/json"</js>));
	 *
	 * 	<jc>// Validates the content type is JSON by just checking for substring.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).contains(<js>"json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using regular expression.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the Content-Type header exists and is JSON.</jc>
	 * 	MediaType mediaType = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws IOException If REST call failed.
	 */
	public FluentStringAssertion<MockServletResponse> assertThat() throws IOException {
		return new FluentStringAssertion<>(asString(), response);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on an integer response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response age is greater than 1.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertIntHeader(<js>"Age"</js>).isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws IOException If REST call failed.
	 */
	public FluentIntegerAssertion<MockServletResponse> assertThatInt() throws IOException {
		BasicIntegerHeader h = asIntegerHeader();
		return new FluentIntegerAssertion<>(h == null ? -1 : h.asInt(), response);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a long response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response length is not too long.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertLongHeader(<js>"Length"</js>).isLessThan(100000);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws IOException If REST call failed.
	 */
	public FluentLongAssertion<MockServletResponse> assertThatLong() throws IOException {
		BasicLongHeader h = asLongHeader();
		return new FluentLongAssertion<>(h == null ? -1 : h.asLong(), response);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a date response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content is not expired.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertDateHeader(<js>"Expires"</js>).isBefore(<jk>new</jk> Date());
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws IOException If REST call failed.
	 */
	public FluentDateAssertion<MockServletResponse> assertThatDate() throws IOException {
		BasicDateHeader h = asDateHeader();
		return new FluentDateAssertion<>(h == null ? null : h.asDate(), response);
	}

	/**
	 * Gets the name of this pair.
	 *
	 * @return The name of this pair, never <jk>null</jk>.
	 */
	public String getName() {
		return name;
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + asString();
	}
}
