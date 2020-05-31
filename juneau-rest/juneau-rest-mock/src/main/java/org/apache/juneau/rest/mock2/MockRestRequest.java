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
package org.apache.juneau.rest.mock2;

import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client2.*;

/**
 * An implementation of {@link HttpServletRequest} for mocking purposes.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
 */
public class MockRestRequest extends org.apache.juneau.rest.client2.RestRequest {

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param request The wrapped Apache HTTP client request object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected MockRestRequest(RestClient client, HttpRequestBase request) throws RestCallException {
		super(client, request);
	}
	/**
	 * Creates a {@link RestResponse} object from the specified {@link HttpResponse} object.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own specialized {@link RestResponse} objects.
	 *
	 * @param client The client that created the request.
	 * @param httpResponse The response object to wrap.
	 * @param parser The parser to use to parse the response.
	 *
	 * @return A new {@link RestResponse} object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	@Override
	protected MockRestResponse createResponse(RestClient client, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new MockRestResponse(client, this, httpResponse, parser);
	}
}
