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

import static org.apache.juneau.http.response.EarlyHints.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents an <c>HTTP 103 Early Hints</c> response.
 *
 * <p>
 * Used to return some response headers before final HTTP message.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Response
@StatusCode(STATUS_CODE)
@Schema(description=REASON_PHRASE)
@FluentSetters
public class EarlyHints extends BasicHttpResponse {

	/** HTTP status code */
	public static final int STATUS_CODE = 103;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Early Hints";

	/** Default status line */
	private static final BasicStatusLine STATUS_LINE = BasicStatusLine.create(STATUS_CODE, REASON_PHRASE);

	/** Reusable unmodifiable instance */
	public static final EarlyHints INSTANCE = create().unmodifiable().build();

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder bean.
	 */
	public static HttpResponseBuilder<EarlyHints> create() {
		return new HttpResponseBuilder<>(EarlyHints.class).statusLine(STATUS_LINE.copy());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this exception.
	 */
	public EarlyHints(HttpResponseBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	@Override /* BasicHttpResponse */
	public HttpResponseBuilder<EarlyHints> copy() {
		return new HttpResponseBuilder<>(this);
	}

	// <FluentSetters>

	// </FluentSetters>
}