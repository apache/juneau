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
import static org.apache.juneau.http.classic.response.UriTooLong.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.*;
import org.apache.juneau.http.classic.header.*;

/**
 * Exception representing an HTTP 414 (URI Too Long).
 *
 * <p>
 * The URI provided was too long for the server to process.
 * <br>Often the result of too much data being encoded as a query-string of a GET request, in which case it should be converted to a POST request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description = REASON_PHRASE)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for HTTP exception hierarchy
})
public class UriTooLong extends BasicHttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 414;

	/** Reason phrase */
	public static final String REASON_PHRASE = "URI Too Long";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Reusable unmodifiable instance */
	public static final UriTooLong INSTANCE = new UriTooLong().unmodifiable();

	/**
	 * Constructor.
	 */
	public UriTooLong() {
		this((Throwable)null, REASON_PHRASE);
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
	public UriTooLong(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments in the message.
	 */
	public UriTooLong(String msg, Object...args) {
		this((Throwable)null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public UriTooLong(Throwable cause) {
		this(cause, cause == null ? REASON_PHRASE : cause.getMessage());
	}

	/**
	 * Constructor.
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args The message arguments.
	 */
	public UriTooLong(Throwable cause, String msg, Object...args) {
		super(STATUS_CODE, cause, msg, args);
		setStatusLine(STATUS_LINE.copy());
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy.  Must not be <jk>null</jk>.
	 */
	protected UriTooLong(UriTooLong copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a modifiable copy of this bean.
	 *
	 * @return A new modifiable bean.
	 */
	public UriTooLong copy() {
		return new UriTooLong(this);
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setHeader2(String name, Object value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* Overridden from BasicRuntimeException */
	public UriTooLong setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setStatusCode2(int code) throws IllegalStateException {
		super.setStatusCode2(code);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public UriTooLong unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link UriTooLong} exception.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends UriTooLong implements UnmodifiableBean {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param copyFrom The exception to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(UriTooLong copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpException */
		protected BasicHttpException modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}