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
package org.apache.juneau.http;

import static org.apache.juneau.internal.ObjectUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.message.*;
import org.apache.juneau.http.BasicStatusLine;
import org.apache.juneau.internal.*;

/**
 * A basic implementation of the {@link StatusLine} interface.
 */
public class BasicStatusLine implements StatusLine {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {

		ProtocolVersion protocolVersion;
		Integer statusCode;
		String reasonPhrase;
		Locale locale;
		ReasonPhraseCatalog reasonPhraseCatalog;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy.
		 */
		protected Builder(BasicStatusLine copyFrom) {
			protocolVersion = copyFrom.protocolVersion;
			statusCode = copyFrom.statusCode;
			reasonPhrase = copyFrom.reasonPhrase;
			locale = copyFrom.locale;
		}

		/**
		 * Creates a new {@link BasicStatusLine} bean based on the contents of this builder.
		 *
		 * @return A new {@link BasicStatusLine} bean.
		 */
		public BasicStatusLine build() {
			return new BasicStatusLine(this);
		}

		/**
		 * Sets the protocol version on the status line.
		 *
		 * <p>
		 * If not specified, <js>"HTTP/1.1"</js> will be used.
		 *
		 * @param value The new value.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder protocolVersion(ProtocolVersion value) {
			this.protocolVersion = value;
			return this;
		}

		/**
		 * Sets the status code on the status line.
		 *
		 * <p>
		 * If not specified, <c>0</c> will be used.
		 *
		 * @param value The new value.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder statusCode(int value) {
			this.statusCode = value;
			return this;
		}

		/**
		 * Sets the reason phrase on the status line.
		 *
		 * <p>
		 * If not specified, the reason phrase will be retrieved from the reason phrase catalog
		 * using the locale on this builder.
		 *
		 * @param value The new value.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder reasonPhrase(String value) {
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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder reasonPhraseCatalog(ReasonPhraseCatalog value) {
			this.reasonPhraseCatalog = value;
			return this;
		}

		/**
		 * Sets the locale used to retrieve reason phrases.
		 *
		 * <p>
		 * If not specified, uses {@link Locale#getDefault()}.
		 *
		 * @param value The new value.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder locale(Locale value) {
			this.locale = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private ProtocolVersion DEFAULT_PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	final ProtocolVersion protocolVersion;
	final int statusCode;
	final String reasonPhrase;
	final Locale locale;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public BasicStatusLine(Builder builder) {
		this.protocolVersion = firstNonNull(builder.protocolVersion, DEFAULT_PROTOCOL_VERSION);
		this.statusCode = firstNonNull(builder.statusCode, 0);
		this.locale = firstNonNull(builder.locale, Locale.getDefault());

		String reasonPhrase = builder.reasonPhrase;
		if (reasonPhrase == null) {
			ReasonPhraseCatalog rfc = firstNonNull(builder.reasonPhraseCatalog, EnglishReasonPhraseCatalog.INSTANCE);
			reasonPhrase = rfc.getReason(statusCode, locale);
		}
		this.reasonPhrase = reasonPhrase;
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* StatusLine */
	public ProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	@Override /* StatusLine */
	public int getStatusCode() {
		return statusCode;
	}

	@Override /* StatusLine */
	public String getReasonPhrase() {
		return reasonPhrase;
	}

	/**
	 * Returns the locale of this status line.
	 *
	 * @return The locale of this status line.
	 */
	public Locale getLocale() {
		return locale;
	}

	@Override /* Object */
	public String toString() {
		return BasicLineFormatter.INSTANCE.formatStatusLine(null, this).toString();
	}
}
