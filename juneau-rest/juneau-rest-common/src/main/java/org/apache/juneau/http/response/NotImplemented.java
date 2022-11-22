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

import static org.apache.juneau.http.response.NotImplemented.*;

import java.text.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Exception representing an HTTP 501 (Not Implemented).
 *
 * <p>
 * The server either does not recognize the request method, or it lacks the ability to fulfill the request.
 * <br>Usually this implies future availability (e.g., a new feature of a web-service API).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 *
 * @serial exclude
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
@FluentSetters
public class NotImplemented extends BasicHttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 501;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Not Implemented";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Reusable unmodifiable instance */
	public static final NotImplemented INSTANCE = new NotImplemented().setUnmodifiable();

	/**
	 * Constructor.
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args The message arguments.
	 */
	public NotImplemented(Throwable cause, String msg, Object...args) {
		super(STATUS_CODE, cause, msg, args);
		setStatusLine(STATUS_LINE.copy());
	}

	/**
	 * Constructor.
	 */
	public NotImplemented() {
		this((Throwable)null, REASON_PHRASE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public NotImplemented(String msg, Object...args) {
		this((Throwable)null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public NotImplemented(Throwable cause) {
		this(cause, cause == null ? REASON_PHRASE : cause.getMessage());
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
	public NotImplemented(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy.
	 */
	protected NotImplemented(NotImplemented copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a modifiable copy of this bean.
	 *
	 * @return A new modifiable bean.
	 */
	public NotImplemented copy() {
		return new NotImplemented(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.BasicRuntimeException */
	public NotImplemented setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.BasicRuntimeException */
	public NotImplemented setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setHeader2(String name, Object value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setStatusCode2(int code) throws IllegalStateException{
		super.setStatusCode2(code);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.response.BasicHttpException */
	public NotImplemented setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	// </FluentSetters>
}
