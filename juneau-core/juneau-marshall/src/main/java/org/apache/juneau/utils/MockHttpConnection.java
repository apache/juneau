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
 * Represent the basic connection for mock HTTP requests.
 *
 * <p>
 * Used as a shim between the server and client APIs that allow the <c>RestClient</c>
 * class to send and receive mocked requests using the <c>MockRest</c> interface.
 *
 * @deprecated Use <c>org.apache.juneau.rest.mock2</c>
 */
@Deprecated
public interface MockHttpConnection {

	/**
	 * Creates a mocked HTTP request.
	 *
	 * @param method The HTTP request method.
	 * @param path The HTTP request path.
	 * @param body The HTTP request body.
	 * @return A new mock request.
	 * @throws Exception
	 */
	MockHttpRequest request(String method, String path, Object body) throws Exception;
}
