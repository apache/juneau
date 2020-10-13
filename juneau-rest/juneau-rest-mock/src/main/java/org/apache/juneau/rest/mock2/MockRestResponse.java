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

import org.apache.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;

/**
 * A subclass of {@link RestResponse} with additional features for mocked testing.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock}
 * </ul>
*/
public class MockRestResponse extends org.apache.juneau.rest.client.RestResponse {

	/**
	 * Constructor.
	 *
	 * @param client The RestClient that created this response.
	 * @param request The REST request.
	 * @param response The HTTP response.  Can be <jk>null</jk>.
	 * @param parser The overridden parser passed into {@link RestRequest#parser(Parser)}.
	 */
	public MockRestResponse(RestClient client, RestRequest request, HttpResponse response, Parser parser) {
		super(client, request, response, parser);
		((MockRestClient)client).currentResponse(this);
	}
}
