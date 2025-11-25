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
package org.apache.juneau.rest.arg;

import static org.apache.juneau.common.utils.StringUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.common.reflect.*;

/**
 * General exception due to a malformed Java parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 *
 * @serial exclude
 */
public class ArgException extends InternalServerError {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param pi The parameter with the issue.
	 * @param msg The message.
	 * @param args The message args.
	 */
	public ArgException(ParameterInfo pi, String msg, Object...args) {
		super(mformat(msg, args) + " on parameter " + pi.getIndex() + " of method " + pi.getMethod().getFullName() + ".");
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected ArgException(ArgException copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from InternalServerError */
	public ArgException copy() {
		return new ArgException(this);
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setHeader2(String name, Object value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	@Override /* Overridden from InternalServerError */
	public ArgException setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}