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
package org.apache.juneau.http.response;

import java.net.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all predefined responses in this package that typically include a <c>Location</c> header.
 */
@FluentSetters
public abstract class BasicLocationHttpResponse extends BasicHttpResponse {

	private URI location;

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The HTTP status reason phrase.
	 */
	protected BasicLocationHttpResponse(int statusCode, String reasonPhrase) {
		super(statusCode, reasonPhrase);
	}

	/**
	 * Constructor.
	 *
	 * @param statusLine The HTTP status line.
	 */
	protected BasicLocationHttpResponse(StatusLine statusLine) {
		super(statusLine);
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicLocationHttpResponse location(URI value) {
		assertModifiable();
		this.location = value;
		return this;
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicLocationHttpResponse location(String value) {
		return location(URI.create(value));
	}

	/**
	 * @return <c>Location</c> header value.
	 */
	@ResponseHeader(name="Location", description="Location of resource.")
	public URI getLocation() {
		return location;
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse body(String value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse body(HttpEntity value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse headers(Header...values) {
		super.headers(values);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse reasonPhrase(String value) {
		super.reasonPhrase(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse statusCode(int value) {
		super.statusCode(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public BasicLocationHttpResponse unmodifiable() {
		super.unmodifiable();
		return this;
	}

	// </FluentSetters>
}
