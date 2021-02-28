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

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 305 Use Proxy</c> response.
 *
 * <p>
 * The requested resource is available only through a proxy, the address for which is provided in the response.
 * Many HTTP clients (such as Mozilla and Internet Explorer) do not correctly handle responses with this status code, primarily for security reasons.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class UseProxy extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 305;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Use Proxy";

	/**
	 * Default unmodifiable instance.
	 *
	 * <br>Response body contains the reason phrase.
	 */
	public static final UseProxy INSTANCE = create().body(REASON_PHRASE).unmodifiable();

	/**
	 * Static creator.
	 *
	 * @return A new instance of this bean.
	 */
	public static UseProxy create() {
		return new UseProxy();
	}

	/**
	 * Constructor.
	 */
	public UseProxy() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param body Body of the response.  Can be <jk>null</jk>.
	 */
	public UseProxy(String body) {
		super(STATUS_CODE, REASON_PHRASE);
		body(body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy body(String value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy body(HttpEntity value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy headers(Header...values) {
		super.headers(values);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy reasonPhrase(String value) {
		super.reasonPhrase(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy statusCode(int value) {
		super.statusCode(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public UseProxy unmodifiable() {
		super.unmodifiable();
		return this;
	}

	// </FluentSetters>
}