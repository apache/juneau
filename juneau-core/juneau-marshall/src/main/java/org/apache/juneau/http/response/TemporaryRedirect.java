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

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 307 Temporary Redirect</c> response.
 *
 * <p>
 * In this case, the request should be repeated with another URI; however, future requests should still use the original URI.
 * In contrast to how 302 was historically implemented, the request method is not allowed to be changed when reissuing the original request.
 * For example, a POST request should be repeated using another POST request.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
public class TemporaryRedirect extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 307;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Temporary Redirect";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create().statusCode(STATUS_CODE).reasonPhrase(REASON_PHRASE).build();

	/** Reusable unmodifiable instance */
	public static final TemporaryRedirect INSTANCE = create().unmodifiable().build();

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder bean.
	 */
	public static HttpResponseBuilder<TemporaryRedirect> create() {
		return new HttpResponseBuilder<>(TemporaryRedirect.class).statusLine(STATUS_LINE);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this exception.
	 */
	public TemporaryRedirect(HttpResponseBuilder<?> builder) {
		super(builder);
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
	public TemporaryRedirect(HttpResponse response) {
		this(create().copyFrom(response));
		assertStatusCode(response);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	@Override /* BasicHttpResponse */
	public HttpResponseBuilder<TemporaryRedirect> copy() {
		return new HttpResponseBuilder<>(this);
	}
}