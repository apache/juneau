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

/**
 * A basic implementation of the {@link StatusLine} interface.
 */
public class BasicStatusLine implements StatusLine {

	private ProtocolVersion DEFAULT_PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	private final ProtocolVersion protocolVersion;
	private final int statusCode;
	private final String reasonPhrase;
	private final Locale locale;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static BasicStatusLineBuilder create() {
		return new BasicStatusLineBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public BasicStatusLine(BasicStatusLineBuilder builder) {
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
	public BasicStatusLineBuilder builder() {
		return create().protocolVersion(protocolVersion).statusCode(statusCode).reasonPhrase(reasonPhrase);
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
}
