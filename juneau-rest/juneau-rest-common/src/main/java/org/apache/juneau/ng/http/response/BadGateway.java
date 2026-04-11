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
package org.apache.juneau.ng.http.response;

/**
 * Represents an <c>HTTP 502 Bad Gateway</c> error response.
 *
 * <p>
 * The server was acting as a gateway or proxy and received an invalid response from the upstream server.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class BadGateway extends BasicHttpException {

	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 502;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Bad Gateway";

	/**
	 * Constructor with no message.
	 */
	public BadGateway() {
		super(STATUS_CODE, REASON_PHRASE);
	}

	/**
	 * Constructor with a detail message.
	 *
	 * @param message The detail message. May be <jk>null</jk>.
	 */
	public BadGateway(String message) {
		super(STATUS_CODE, REASON_PHRASE, message);
	}

	/**
	 * Constructor with a cause.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public BadGateway(Throwable cause) {
		super(STATUS_CODE, REASON_PHRASE, cause != null ? cause.getMessage() : null, cause);
	}

	/**
	 * Constructor with a detail message and cause.
	 *
	 * @param message The detail message. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public BadGateway(String message, Throwable cause) {
		super(STATUS_CODE, REASON_PHRASE, message, cause);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	public BadGateway(BadGateway copyFrom) {
		super(copyFrom);
	}
}
