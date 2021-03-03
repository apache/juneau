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

import static org.apache.juneau.http.response.NotModified.*;

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 304 Not Modified</c> response.
 *
 * <p>
 * Indicates that the resource has not been modified since the version specified by the request headers If-Modified-Since or If-None-Match.
 * In such case, there is no need to retransmit the resource since the client still has a previously-downloaded copy.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class NotModified extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 304;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Not Modified";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create().statusCode(STATUS_CODE).reasonPhrase(REASON_PHRASE).build();

	/** Reusable unmodifiable instance */
	public static final NotModified INSTANCE = create().unmodifiable().build();

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder bean.
	 */
	public static HttpResponseBuilder<NotModified> create() {
		return new HttpResponseBuilder<>(NotModified.class).statusLine(STATUS_LINE);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this exception.
	 */
	public NotModified(HttpResponseBuilder<?> builder) {
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
	public NotModified(HttpResponse response) {
		this(create().copyFrom(response));
		assertStatusCode(response);
	}
}