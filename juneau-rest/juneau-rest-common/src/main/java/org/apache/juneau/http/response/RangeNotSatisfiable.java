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

import static org.apache.juneau.http.response.RangeNotSatisfiable.*;

import java.text.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;

/**
 * Exception representing an HTTP 416 (Range Not Satisfiable).
 *
 * <p>
 * The client has asked for a portion of the file (byte serving), but the server cannot supply that portion.
 * <br>For example, if the client asked for a part of the file that lies beyond the end of the file.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
public class RangeNotSatisfiable extends BasicHttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 416;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Range Not Satisfiable";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Reusable unmodifiable instance */
	public static final RangeNotSatisfiable INSTANCE = new RangeNotSatisfiable().setUnmodifiable();

	/**
	 * Constructor.
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args The message arguments.
	 */
	public RangeNotSatisfiable(Throwable cause, String msg, Object...args) {
		super(STATUS_CODE, cause, msg, args);
		setStatusLine(STATUS_LINE.copy());
	}

	/**
	 * Constructor.
	 */
	public RangeNotSatisfiable() {
		this((Throwable)null, REASON_PHRASE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public RangeNotSatisfiable(String msg, Object...args) {
		this((Throwable)null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public RangeNotSatisfiable(Throwable cause) {
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
	public RangeNotSatisfiable(HttpResponse response) {
		super(response);
		assertStatusCode(response);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy.
	 */
	protected RangeNotSatisfiable(RangeNotSatisfiable copyFrom) {
		super(copyFrom);
	}

	/**
	 * Creates a modifiable copy of this bean.
	 *
	 * @return A new modifiable bean.
	 */
	public RangeNotSatisfiable copy() {
		return new RangeNotSatisfiable(this);
	}
	@Override /* Overridden from BasicRuntimeException */
	public RangeNotSatisfiable setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}

	@Override /* Overridden from BasicRuntimeException */
	public RangeNotSatisfiable setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setHeader2(String name, Object value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setStatusCode2(int code) throws IllegalStateException{
		super.setStatusCode2(code);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}
	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpException */
	public RangeNotSatisfiable setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}
}