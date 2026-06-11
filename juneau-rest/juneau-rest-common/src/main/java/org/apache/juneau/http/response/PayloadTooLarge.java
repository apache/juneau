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
package org.apache.juneau.http.response;

/**
 * Represents an <c>HTTP 413 Payload Too Large</c> error response.
 *
 * <p>
 * The request is larger than the server is willing or able to process.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class PayloadTooLarge extends BasicHttpException {

	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 413;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Payload Too Large";

	/**
	 * Constructor with no message.
	 */
	public PayloadTooLarge() {
		super(STATUS_CODE, REASON_PHRASE);
	}

	/**
	 * Constructor with a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty. Supports both {@link java.text.MessageFormat} ({@code {0}}) and {@link String#format(String, Object...) String.format} ({@code %s}) placeholders.
	 * @param args Optional message arguments.
	 */
	public PayloadTooLarge(String msg, Object...args) {
		super(STATUS_CODE, REASON_PHRASE, msg, args);
	}

	/**
	 * Constructor with a cause.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public PayloadTooLarge(Throwable cause) {
		super(STATUS_CODE, REASON_PHRASE, cause, cause != null ? cause.getMessage() : null);
	}

	/**
	 * Constructor with a cause and a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty. Supports both {@link java.text.MessageFormat} ({@code {0}}) and {@link String#format(String, Object...) String.format} ({@code %s}) placeholders.
	 * @param args Optional message arguments.
	 */
	public PayloadTooLarge(Throwable cause, String msg, Object...args) {
		super(STATUS_CODE, REASON_PHRASE, cause, msg, args);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	public PayloadTooLarge(PayloadTooLarge copyFrom) {
		super(copyFrom);
	}
}