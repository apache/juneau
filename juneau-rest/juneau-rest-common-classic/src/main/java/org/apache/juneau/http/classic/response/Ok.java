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
package org.apache.juneau.http.classic.response;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.http.classic.response.Ok.*;

import org.apache.http.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.*;

/**
 * Represents an <c>HTTP 200 OK</c> response.
 *
 * <p>
 * Standard response for successful HTTP requests. The actual response will depend on the request method used.
 * In a GET request, the response will contain an entity corresponding to the requested resource.
 * In a POST request, the response will contain an entity describing or containing the result of the action.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description = REASON_PHRASE)
public class Ok extends BasicHttpResponse<Ok> {

	/** HTTP status code */
	public static final int STATUS_CODE = 200;

	/** Reason phrase */
	public static final String REASON_PHRASE = "OK";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final Ok INSTANCE = new Ok().unmodifiable();

	/** Reusable unmodifiable instance */
	public static final Ok OK = INSTANCE;

	/**
	 * Constructor.
	 */
	public Ok() {
		super(STATUS_LINE);
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
	public Ok(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.  Must not be <jk>null</jk>.
	 */
	public Ok(Ok copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public Ok copy() {
		return new Ok(this);
	}

	@Override /* Overridden from BasicHttpResponse */
	public Ok unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link Ok} response.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends Ok implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The response to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(Ok copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpResponse */
		protected Ok modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}