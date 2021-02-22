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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all predefined responses in this package.
 *
 * <p>
 * Consists simply of a simple string message.
 */
@Response
@FluentSetters
public abstract class HttpResponse {

	private final String message;
	private AMap<String,Object> headers = AMap.create();

	/**
	 * Constructor.
	 *
	 * @param message Message to send as the response.
	 */
	protected HttpResponse(String message) {
		this.message = message;
	}

	/**
	 * Add an HTTP header to this response.
	 *
	 * @param name The header name.
	 * @param val The header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponse header(String name, Object val) {
		headers.a(name, val);
		return this;
	}

	/**
	 * Returns the headers associated with this exception.
	 *
	 * @return The headers associated with this exception.
	 */
	@ResponseHeader("*")
	@BeanIgnore
	public Map<String,Object> getHeaders() {
		return headers;
	}

	@ResponseBody
	@Override /* Object */
	public String toString() {
		return message;
	}

	// <FluentSetters>

	// </FluentSetters>
}
