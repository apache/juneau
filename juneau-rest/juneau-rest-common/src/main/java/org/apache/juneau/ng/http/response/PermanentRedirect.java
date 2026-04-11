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

import org.apache.juneau.ng.http.*;

/**
 * Represents an <c>HTTP 308 Permanent Redirect</c> response.
 *
 * <p>
 * The request and all future requests should be repeated using another URI.
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
public class PermanentRedirect extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 308;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Permanent Redirect";

	/** Default status line */
	private static final HttpStatusLine STATUS_LINE = HttpStatusLineBean.of(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final PermanentRedirect INSTANCE = new PermanentRedirect();

	/**
	 * Constructor.
	 */
	public PermanentRedirect() {
		super(STATUS_LINE);
	}

	/**
	 * Constructor with a response body.
	 *
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public PermanentRedirect(HttpBody body) {
		super(STATUS_LINE, body);
	}

	/**
	 * Constructor with a plain-text string body.
	 *
	 * @param body The response body as a plain-text string. May be <jk>null</jk>.
	 */
	public PermanentRedirect(String body) {
		super(STATUS_LINE, body);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from. Must not be <jk>null</jk>.
	 */
	public PermanentRedirect(PermanentRedirect copyFrom) {
		super(copyFrom);
	}
}
