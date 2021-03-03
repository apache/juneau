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

import static org.apache.juneau.http.response.NetworkAuthenticationRequired.*;

import java.text.*;

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;

/**
 * Exception representing an HTTP 511 (Network Authentication Required).
 *
 * <p>
 * The client needs to authenticate to gain network access.
 * <br>Intended for use by intercepting proxies used to control access to the network (e.g., "captive portals" used to require agreement to Terms of Service before granting full Internet access via a Wi-Fi hotspot).
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
public class NetworkAuthenticationRequired extends BasicHttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 511;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Network Authentication Required";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create().statusCode(STATUS_CODE).reasonPhrase(REASON_PHRASE).build();

	/** Reusable unmodifiable instance */
	public static final NetworkAuthenticationRequired INSTANCE = create().unmodifiable().build();

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder bean.
	 */
	public static HttpExceptionBuilder<NetworkAuthenticationRequired> create() {
		return new HttpExceptionBuilder<>(NetworkAuthenticationRequired.class).statusLine(STATUS_LINE);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this exception.
	 */
	public NetworkAuthenticationRequired(HttpExceptionBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args The message arguments.
	 */
	public NetworkAuthenticationRequired(Throwable cause, String msg, Object...args) {
		this(create().causedBy(cause).message(msg, args));
	}

	/**
	 * Constructor.
	 */
	public NetworkAuthenticationRequired() {
		this(create());
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public NetworkAuthenticationRequired(String msg, Object...args) {
		this(create().message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public NetworkAuthenticationRequired(Throwable cause) {
		this(create().causedBy(cause));
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
	public NetworkAuthenticationRequired(HttpResponse response) {
		this(create().copyFrom(response));
		assertStatusCode(response);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpExceptionBuilder<NetworkAuthenticationRequired> builder() {
		return super.builder(NetworkAuthenticationRequired.class).copyFrom(this);
	}
}