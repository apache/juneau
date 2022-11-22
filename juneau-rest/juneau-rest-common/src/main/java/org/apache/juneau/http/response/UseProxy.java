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

import static org.apache.juneau.http.response.UseProxy.*;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 305 Use Proxy</c> response.
 *
 * <p>
 * The requested resource is available only through a proxy, the address for which is provided in the response.
 * Many HTTP clients (such as Mozilla and Internet Explorer) do not correctly handle responses with this status code, primarily for security reasons.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
@FluentSetters
public class UseProxy extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 305;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Use Proxy";

	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final UseProxy INSTANCE = new UseProxy().setUnmodifiable();

	/**
	 * Constructor.
	 */
	public UseProxy() {
		super(STATUS_LINE);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public UseProxy(UseProxy copyFrom) {
		super(copyFrom);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when parsing an HTTP response.
	 *
	 * @param response The HTTP response to copy from.  Must not be <jk>null</jk>.
	 * @throws AssertionError If HTTP response status code does not match what was expected.
	 */
	public UseProxy(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public UseProxy copy() {
		return new UseProxy(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setHeader2(Header value) {
		super.setHeader2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setHeader2(String name, String value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setLocation(String value) {
		super.setLocation(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setLocation(URI value) {
		super.setLocation(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setStatusCode2(int value) {
		super.setStatusCode2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpResponse */
	public UseProxy setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}