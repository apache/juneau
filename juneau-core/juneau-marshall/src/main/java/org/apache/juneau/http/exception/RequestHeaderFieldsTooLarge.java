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
package org.apache.juneau.http.exception;

import static org.apache.juneau.http.exception.RequestHeaderFieldsTooLarge.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;

/**
 * Exception representing an HTTP 431 (Request Header Fields Too Large).
 *
 * <p>
 * The server is unwilling to process the request because either an individual header field, or all the header fields collectively, are too large.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
public class RequestHeaderFieldsTooLarge extends HttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 431;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Request Header Fields Too Large";

	/** Reusable unmodifiable instance. */
	public static final RequestHeaderFieldsTooLarge INSTANCE = create().unmodifiable(true).build();

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder bean.
	 */
	public static HttpExceptionBuilder<RequestHeaderFieldsTooLarge> create() {
		return new HttpExceptionBuilder<>(RequestHeaderFieldsTooLarge.class).statusCode(STATUS_CODE).reasonPhrase(REASON_PHRASE);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this exception.
	 */
	public RequestHeaderFieldsTooLarge(HttpExceptionBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args The message arguments.
	 */
	public RequestHeaderFieldsTooLarge(Throwable cause, String msg, Object...args) {
		this(create().causedBy(cause).message(msg, args));
	}

	/**
	 * Constructor.
	 */
	public RequestHeaderFieldsTooLarge() {
		this(create().build());
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 */
	public RequestHeaderFieldsTooLarge(String msg) {
		this(create().message(msg));
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public RequestHeaderFieldsTooLarge(String msg, Object...args) {
		this(create().message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public RequestHeaderFieldsTooLarge(Throwable cause) {
		this(create().causedBy(cause));
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpExceptionBuilder<RequestHeaderFieldsTooLarge> builder() {
		return super.builder(RequestHeaderFieldsTooLarge.class).copyFrom(this);
	}
}