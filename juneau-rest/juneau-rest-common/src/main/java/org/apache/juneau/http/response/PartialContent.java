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
 * Represents an <c>HTTP 206 Partial Content</c> response.
 *
 * <p>
 * The server is delivering only part of the resource (byte serving) due to a range header sent by the client.
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
public class PartialContent extends BasicHttpResponse<PartialContent> {

	/** HTTP status code */
	public static final int STATUS_CODE = 206;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Partial Content";

	/** Default status line */
	private static final HttpStatusLine STATUS_LINE = HttpStatusLineBean.of(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final PartialContent INSTANCE = new PartialContent().unmodifiable();

	/**
	 * Constructor.
	 */
	public PartialContent() {
		super(STATUS_LINE);
	}

	/**
	 * Constructor with a response body.
	 *
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public PartialContent(HttpBody body) {
		super(STATUS_LINE, body);
	}

	/**
	 * Constructor with a plain-text string body.
	 *
	 * @param body The response body as a plain-text string. May be <jk>null</jk>.
	 */
	public PartialContent(String body) {
		super(STATUS_LINE, body);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from. Must not be <jk>null</jk>.
	 */
	public PartialContent(PartialContent copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicHttpResponse */
	public PartialContent unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link PartialContent} response.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends PartialContent implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The response to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(PartialContent copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpResponse */
		protected PartialContent modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
