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

import static org.apache.juneau.http.response.TemporaryRedirect.*;

import java.net.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 307 Temporary Redirect</c> response.
 *
 * <p>
 * In this case, the request should be repeated with another URI; however, future requests should still use the original URI.
 * In contrast to how 302 was historically implemented, the request method is not allowed to be changed when reissuing the original request.
 * For example, a POST request should be repeated using another POST request.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class TemporaryRedirect extends BasicLocationHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 307;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Temporary Redirect";

	/**
	 * Default unmodifiable instance.
	 *
	 * <br>Response body contains the reason phrase.
	 */
	public static final TemporaryRedirect INSTANCE = create().body(REASON_PHRASE).unmodifiable();

	/**
	 * Static creator.
	 *
	 * @return A new instance of this bean.
	 */
	public static TemporaryRedirect create() {
		return new TemporaryRedirect();
	}

	/**
	 * Constructor.
	 */
	public TemporaryRedirect() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param body Body of the response.  Can be <jk>null</jk>.
	 */
	public TemporaryRedirect(String body) {
		super(STATUS_CODE, REASON_PHRASE);
		body(body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect body(String value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect body(HttpEntity value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect headers(Header...values) {
		super.headers(values);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect reasonPhrase(String value) {
		super.reasonPhrase(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect statusCode(int value) {
		super.statusCode(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public TemporaryRedirect unmodifiable() {
		super.unmodifiable();
		return this;
	}

	@Override /* GENERATED - BasicLocationHttpResponse */
	public TemporaryRedirect location(String value) {
		super.location(value);
		return this;
	}

	@Override /* GENERATED - BasicLocationHttpResponse */
	public TemporaryRedirect location(URI value) {
		super.location(value);
		return this;
	}

	// </FluentSetters>
}