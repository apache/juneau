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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.http.*;

/**
 * Represents an <c>HTTP 421 Misdirected Request</c> error response.
 *
 * <p>
 * The request was directed at a server that is not able to produce a response.
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
public class MisdirectedRequest extends BasicHttpException {

	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 421;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Misdirected Request";

	/**
	 * Constructor with no message.
	 */
	public MisdirectedRequest() {
		super(STATUS_CODE, REASON_PHRASE);
	}

	/**
	 * Constructor with a {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a {@link String#format(String, Object...) String.format}-style format string (<c>%s</c> placeholders) when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public MisdirectedRequest(String msg, Object...args) {
		super(STATUS_CODE, REASON_PHRASE, msg, args);
	}

	/**
	 * Constructor with a cause.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public MisdirectedRequest(Throwable cause) {
		super(STATUS_CODE, REASON_PHRASE, cause, cause != null ? cause.getMessage() : null);
	}

	/**
	 * Constructor with a cause and a {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a {@link String#format(String, Object...) String.format}-style format string (<c>%s</c> placeholders) when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public MisdirectedRequest(Throwable cause, String msg, Object...args) {
		super(STATUS_CODE, REASON_PHRASE, cause, msg, args);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	public MisdirectedRequest(MisdirectedRequest copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicHttpException */
	public MisdirectedRequest unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link MisdirectedRequest} exception.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends MisdirectedRequest implements UnmodifiableBean {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param copyFrom The exception to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(MisdirectedRequest copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpException */
		protected BasicHttpException modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}