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

import static org.apache.juneau.http.response.Accepted.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 202 Accepted</c> response.
 *
 * <p>
 * The request has been accepted for processing, but the processing has not been completed.
 * The request might or might not be eventually acted upon, and may be disallowed when processing occurs.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class Accepted extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 202;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Accepted";

	/**
	 * Default unmodifiable instance.
	 *
	 * <br>Response body contains the reason phrase.
	 */
	public static final Accepted INSTANCE = create().body(REASON_PHRASE).unmodifiable();

	/**
	 * Static creator.
	 *
	 * @return A new instance of this bean.
	 */
	public static Accepted create() {
		return new Accepted();
	}

	/**
	 * Constructor.
	 */
	public Accepted() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param body Body of the response.  Can be <jk>null</jk>.
	 */
	public Accepted(String body) {
		super(STATUS_CODE, REASON_PHRASE);
		body(body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted body(String value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted body(HttpEntity value) {
		super.body(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted headers(Header...values) {
		super.headers(values);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted reasonPhrase(String value) {
		super.reasonPhrase(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted statusCode(int value) {
		super.statusCode(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResponse */
	public Accepted unmodifiable() {
		super.unmodifiable();
		return this;
	}

	// </FluentSetters>
}