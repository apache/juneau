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
import static org.apache.juneau.http.classic.response.MultiStatus.*;

import org.apache.http.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.*;

/**
 * Represents an <c>HTTP 207 Multi-Status</c> response.
 *
 * <p>
 * The message body that follows is by default an XML message and can contain a number of separate response codes, depending on how many sub-requests were made.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description = REASON_PHRASE)
public class MultiStatus extends BasicHttpResponse<MultiStatus> {

	/** HTTP status code */
	public static final int STATUS_CODE = 207;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Multi-Status";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Default unmodifiable instance */
	public static final MultiStatus INSTANCE = new MultiStatus().unmodifiable();

	/**
	 * Constructor.
	 */
	public MultiStatus() {
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
	public MultiStatus(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.  Must not be <jk>null</jk>.
	 */
	public MultiStatus(MultiStatus copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public MultiStatus copy() {
		return new MultiStatus(this);
	}

	@Override /* Overridden from BasicHttpResponse */
	public MultiStatus unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link MultiStatus} response.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends MultiStatus implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The response to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(MultiStatus copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpResponse */
		protected MultiStatus modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}