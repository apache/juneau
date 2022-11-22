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
package org.apache.juneau.rest.client;

import java.io.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.protocol.*;

/**
 * Default HTTP call handler.
 *
 * Can be subclasses and specified via {@link RestClient.Builder#callHandler()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class BasicRestCallHandler implements RestCallHandler {

	private final RestClient client;

	/**
	 * Constructor.
	 *
	 * @param client The client to use for handling requests.
	 */
	public BasicRestCallHandler(RestClient client) {
		this.client = client;
	}

	@Override /* RestCallHandler */
	public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
		return target == null ? client.execute((HttpUriRequest)request, context) : client.execute(target, request, context);
	}
}
