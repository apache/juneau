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

import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for {@link BasicStatusLine} beans.
 */
@FluentSetters
public class BasicStatusLineBuilder {

	ProtocolVersion protocolVersion;
	Integer statusCode;
	String reasonPhrase;
	Locale locale;
	ReasonPhraseCatalog reasonPhraseCatalog;

	/**
	 * Constructor.
	 */
	public BasicStatusLineBuilder() {}

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
	public BasicStatusLineBuilder protocolVersion(ProtocolVersion value) {
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
	public BasicStatusLineBuilder statusCode(int value) {
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
	public BasicStatusLineBuilder reasonPhrase(String value) {
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
	public BasicStatusLineBuilder reasonPhraseCatalog(ReasonPhraseCatalog value) {
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
	public BasicStatusLineBuilder locale(Locale value) {
		this.locale = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
