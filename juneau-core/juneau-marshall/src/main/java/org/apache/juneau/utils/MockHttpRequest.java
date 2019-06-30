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
package org.apache.juneau.utils;

/**
 * Represent the basic interface for an HTTP rquest.
 *
 * <p>
 * Used as a shim between the server and client APIs that allow the <c>RestClient</c>
 * class to send and receive mocked requests using the <c>MockRest</c> interface.
 *
 * @deprecated Use <c>org.apache.juneau.rest.mock2</c>
 */
@Deprecated
public interface MockHttpRequest {

	/**
	 * Sets the URI of the request.
	 *
	 * @param uri The URI of the request.
	 * @return This object (for method chaining).
	 */
	MockHttpRequest uri(String uri);

	/**
	 * Sets the URI of the request.
	 *
	 * @param method The URI of the request.
	 * @return This object (for method chaining).
	 */
	MockHttpRequest method(String method);

	/**
	 * Sets a header on the request.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	MockHttpRequest header(String name, Object value);

	/**
	 * Sets the body of the request.
	 *
	 * @param body The body of the request.
	 * @return This object (for method chaining).
	 */
	MockHttpRequest body(Object body);

	/**
	 * Executes the request and returns the response.
	 *
	 * @return The response for the request.
	 * @throws Exception
	 */
	MockHttpResponse execute() throws Exception;

}
