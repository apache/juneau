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

import java.util.*;

/**
 * Represent the basic interface for an HTTP response.
 *
 * <p>
 * Used as a shim between the server and client APIs that allow the <c>RestClient</c>
 * class to send and receive mocked requests using the <c>MockRest</c> interface.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use <c>org.apache.juneau.rest.mock2</c>
 * </div>
 */
@Deprecated
public interface MockHttpResponse {

	/**
	 * Returns the status code of the response.
	 *
	 * @return The status code of the response.
	 */
	int getStatus();

	/**
	 * Returns the status message of the response.
	 *
	 * @return The status message of the response.
	 */
	String getMessage();

	/**
	 * Returns the headers of the response.
	 *
	 * @return The headers of the response.
	 */
	Map<String,String[]> getHeaders();

	/**
	 * Returns the body of the response.
	 *
	 * @return The body of the response.
	 */
	byte[] getBody();
}
