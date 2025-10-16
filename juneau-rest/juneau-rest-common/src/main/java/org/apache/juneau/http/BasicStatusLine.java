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
package org.apache.juneau.http;

import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.message.*;
import org.apache.juneau.common.utils.*;

/**
 * A basic implementation of the {@link StatusLine} interface.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class BasicStatusLine implements StatusLine {
	/**
	 * Instantiates a new instance of this bean.
	 *
	 * @return A new bean.
	 */
	public static BasicStatusLine create() {
		return new BasicStatusLine();
	}

	/**
	 * Instantiates a new instance of this bean.
	 *
	 * @param statusCode The initial status code.
	 * @param reasonPhrase The initial reason phrase.
	 * @return A new bean.
	 */
	public static BasicStatusLine create(int statusCode, String reasonPhrase) {
		return new BasicStatusLine().setStatusCode(statusCode).setReasonPhrase(reasonPhrase);
	}
	private ProtocolVersion DEFAULT_PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	private ProtocolVersion protocolVersion = DEFAULT_PROTOCOL_VERSION;
	private int statusCode;
	private String reasonPhrase;
	private ReasonPhraseCatalog reasonPhraseCatalog;
	private Locale locale = Locale.getDefault();
	private boolean unmodifiable;

	/**
	 * Constructor.
	 */
	public BasicStatusLine() {
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The status line being copied.
	 */
	protected BasicStatusLine(BasicStatusLine copyFrom) {
		this.protocolVersion = copyFrom.protocolVersion;
		this.statusCode = copyFrom.statusCode;
		this.reasonPhrase = copyFrom.reasonPhrase;
		this.locale = copyFrom.locale;
	}

	/**
	 * Returns a copy of this bean.
	 *
	 * @return A copy of this bean.
	 */
	public BasicStatusLine copy() {
		return new BasicStatusLine(this);
	}
	/**
	 * Returns the locale of this status line.
	 *
	 * @return The locale of this status line.
	 */
	public Locale getLocale() {
		return locale;
	}

	@Override /* Overridden from StatusLine */
	public ProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	@Override /* Overridden from StatusLine */
	public String getReasonPhrase() {
		if (reasonPhrase == null) {
			ReasonPhraseCatalog rfc = Utils.firstNonNull(reasonPhraseCatalog, EnglishReasonPhraseCatalog.INSTANCE);
			return rfc.getReason(statusCode, locale);
		}
		return reasonPhrase;
	}

	@Override /* Overridden from StatusLine */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets the locale used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link Locale#getDefault()}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setLocale(Locale value) {
		assertModifiable();
		this.locale = value;
		return this;
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setProtocolVersion(ProtocolVersion value) {
		assertModifiable();
		this.protocolVersion = value;
		return this;
	}

	/**
	 * Sets the reason phrase on the status line.
	 *
	 * <p>
	 * If not specified, the reason phrase will be retrieved from the reason phrase catalog
	 * using the locale on this bean.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setReasonPhrase(String value) {
		assertModifiable();
		this.reasonPhrase = value;
		return this;
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		assertModifiable();
		this.reasonPhraseCatalog = value;
		return this;
	}

	/**
	 * Sets the status code on the status line.
	 *
	 * <p>
	 * If not specified, <c>0</c> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setStatusCode(int value) {
		assertModifiable();
		this.statusCode = value;
		return this;
	}

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	public BasicStatusLine setUnmodifiable() {
		unmodifiable = true;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return BasicLineFormatter.INSTANCE.formatStatusLine(null, this).toString();
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw new UnsupportedOperationException("Bean is read-only");
	}
}