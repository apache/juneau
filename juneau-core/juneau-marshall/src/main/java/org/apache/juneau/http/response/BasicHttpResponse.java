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

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.BasicHeader;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all predefined responses in this package.
 */
@Response
@BeanIgnore
@FluentSetters
public abstract class BasicHttpResponse extends org.apache.http.message.BasicHttpResponse {

	private boolean unmodifiable;

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The HTTP status reason phrase.
	 */
	protected BasicHttpResponse(int statusCode, String reasonPhrase) {
		this(new BasicStatusLine(new ProtocolVersion("HTTP",1,1), statusCode, reasonPhrase));
	}

	/**
	 * Constructor.
	 *
	 * @param statusLine The HTTP status line.
	 */
	protected BasicHttpResponse(StatusLine statusLine) {
		super(statusLine);
	}

	/**
	 * Overrides the status code on this response.
	 *
	 * @param value The new status code value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse statusCode(int value) {
		setStatusCode(value);
		return this;
	}

	/**
	 * Overrides the reason phrase on this response.
	 *
	 * @param value The new reason value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse reasonPhrase(String value) {
		setReasonPhrase(value);
		return this;
	}

	/**
	 * Adds a header to this response.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse header(String name, Object value) {
		addHeader(new BasicHeader(name, value));
		return this;
	}

	/**
	 * Adds headers to this response.
	 *
	 * @param values The header values.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse headers(Header...values) {
		for (Header h : values)
			if (h != null)
				addHeader(h);
		return this;
	}

	/**
	 * Sets the entity on this response.
	 *
	 * @param value The entity on this response.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse body(HttpEntity value) {
		setEntity(value);
		return this;
	}

	/**
	 * Sets the entity on this response.
	 *
	 * @param value The entity on this response.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse body(String value) {
		try {
			setEntity(value == null ? null : new StringEntity(value));
		} catch (UnsupportedEncodingException e) { /* Not possible */ }
		return this;
	}

	/**
	 * Causes any modifications to this bean to throw an {@link UnsupportedOperationException}.
	 *
	 * <p>
	 * TODO - Need to make sure headers are not modifiable through various getters.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResponse unmodifiable() {
		this.unmodifiable = true;
		return this;
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected void assertModifiable() {
		if (unmodifiable)
			throw new UnsupportedOperationException("Bean is read-only");
	}

	@Override
	public void setStatusLine(StatusLine value) {
		assertModifiable();
		super.setStatusLine(value);
	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code) {
		assertModifiable();
		super.setStatusLine(ver, code);
	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		assertModifiable();
		super.setStatusLine(ver, code, reason);
	}

	@Override
	public void setStatusCode(int value) {
		assertModifiable();
		super.setStatusCode(value);
	}

	@Override
	public void setReasonPhrase(String value) {
		assertModifiable();
		super.setReasonPhrase(value);
	}

	@Override
	public void setEntity(HttpEntity value) {
		assertModifiable();
		super.setEntity(value);
	}

	@Override
	public void setLocale(Locale value) {
		assertModifiable();
		super.setLocale(value);
	}

	@Override
	public void addHeader(Header value) {
		assertModifiable();
		super.addHeader(value);
	}

	@Override
	public void addHeader(String name, String value) {
		assertModifiable();
		super.addHeader(name, value);
	}

	@Override
	public void setHeader(Header value) {
		assertModifiable();
		super.setHeader(value);
	}

	@Override
	public void setHeader(String name, String value) {
		assertModifiable();
		super.setHeader(name, value);
	}

	@Override
	public void setHeaders(Header[] value) {
		assertModifiable();
		super.setHeaders(value);
	}

	@Override
	public void removeHeader(Header value) {
		assertModifiable();
		super.removeHeader(value);
	}

	@Override
	public void removeHeaders(String name) {
		assertModifiable();
		super.removeHeaders(name);
	}

	@ResponseHeader("*")
	@Override /* Resource */
	public Header[] getAllHeaders() {
		return super.getAllHeaders();
	}

	/**
	 * Returns the status code on this response.
	 *
	 * @return The status code on this response.
	 */
	@ResponseStatus
	public int getStatusCode() {
		return super.getStatusLine().getStatusCode();
	}

	@Override
	@ResponseBody
	public HttpEntity getEntity() {
		return super.getEntity();
	}

	// <FluentSetters>

	// </FluentSetters>
}
