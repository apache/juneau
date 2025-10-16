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
package org.apache.juneau.http.response;

import static org.apache.juneau.http.response.NonAuthoritiveInformation.*;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;

/**
 * Represents an <c>HTTP 203 Non-Authoritative Information</c> response.
 *
 * <p>
 * The server is a transforming proxy (e.g. a Web accelerator) that received a 200 OK from its origin, but is returning a modified version of the origin's response.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
public class NonAuthoritiveInformation extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 203;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Non-Authoritative Information";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final NonAuthoritiveInformation INSTANCE = new NonAuthoritiveInformation().setUnmodifiable();

	/**
	 * Constructor.
	 */
	public NonAuthoritiveInformation() {
		super(STATUS_LINE);
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
	public NonAuthoritiveInformation(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public NonAuthoritiveInformation(NonAuthoritiveInformation copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public NonAuthoritiveInformation copy() {
		return new NonAuthoritiveInformation(this);
	}
	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setHeader2(Header value) {
		super.setHeader2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setHeader2(String name, String value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setLocation(String value) {
		super.setLocation(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setLocation(URI value) {
		super.setLocation(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setStatusCode2(int value) {
		super.setStatusCode2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	@Override /* Overridden from BasicHttpResponse */
	public NonAuthoritiveInformation setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}