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

import static org.apache.juneau.http.response.PermanentRedirect.*;

import java.net.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 308 Permanent Redirect</c> response.
 *
 * <p>
 * The request and all future requests should be repeated using another URI. 307 and 308 parallel the behaviors of 302 and 301, but do not allow the HTTP method to change.
 * So, for example, submitting a form to a permanently redirected resource may continue smoothly.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class PermanentRedirect extends BasicLocationHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 308;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Permanent Redirect";

	/**
	 * Default unmodifiable instance.
	 *
	 * <br>Response body contains the reason phrase.
	 */
	public static final PermanentRedirect INSTANCE = create().body(REASON_PHRASE).unmodifiable();

	/**
	 * Static creator.
	 *
	 * @return A new instance of this bean.
	 */
	public static PermanentRedirect create() {
		return new PermanentRedirect();
	}

	/**
	 * Constructor.
	 */
	public PermanentRedirect() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param body Body of the response.  Can be <jk>null</jk>.
	 */
	public PermanentRedirect(String body) {
		super(STATUS_CODE, REASON_PHRASE);
		body(body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect body(String value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect body(HttpEntity value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect headers(Header...values) {
		super.headers(values);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect reasonPhrase(String value) {
		super.reasonPhrase(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect statusCode(int value) {
		super.statusCode(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public PermanentRedirect unmodifiable() {
		super.unmodifiable();
		return this;
	}

	@Override /* GENERATED - BasicLocationHttpResponse */
	public PermanentRedirect location(String value) {
		super.location(value);
		return this;
	}

	@Override /* GENERATED - BasicLocationHttpResponse */
	public PermanentRedirect location(URI value) {
		super.location(value);
		return this;
	}

	// </FluentSetters>
}