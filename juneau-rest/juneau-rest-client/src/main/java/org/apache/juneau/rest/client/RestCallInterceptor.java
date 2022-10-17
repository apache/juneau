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

/**
 * Used to intercept http connection responses to allow modification of that response before processing and for
 * listening for call lifecycle events.
 *
 * <p>
 * The {@link BasicRestCallInterceptor} is provided as an adapter class for implementing this interface.
 *
 * <p>
 * Note that the {@link RestClient} class itself implements this interface so you can achieve the same results by
 * overriding the methods on the client class as well.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Specialized client that adds a header to every request.</jc>
 * 	<jk>public class</jk> MyRestClient <jk>extends</jk> RestClient {
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> onInit(RestRequest <jv>req</jv>) {
 * 			<jv>req</jv>.header(<js>"Foo"</js>, <js>"bar"</js>);
 * 		}
 * 	}
 *
 *	<jc>// Instantiate the client.</jc>
 *	MyRestClient <jv>client</jv> = RestClient
 *		.<jsm>create</jsm>()
 *		.json()
 *		.build(MyRestClient.<jk>class</jk>);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link RestClient.Builder#interceptors(Object...)}
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public interface RestCallInterceptor {

	/**
	 * Called immediately after {@link RestRequest} object is created and all headers/query/form-data has been
	 * copied from the client to the request object.
	 *
	 * @param req The HTTP request object.
	 * @throws Exception
	 * 	Any exception can be thrown.
	 * 	<br>If not a {@link RestCallException} or {@link RuntimeException}, will be wrapped in a {@link RestCallException}.
	 */
	void onInit(RestRequest req) throws Exception;

	/**
	 * Called immediately after an HTTP response has been received.
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @throws Exception
	 * 	Any exception can be thrown.
	 * 	<br>If not a {@link RestCallException} or {@link RuntimeException}, will be wrapped in a {@link RestCallException}.
	 */
	void onConnect(RestRequest req, RestResponse res) throws Exception;

	/**
	 * Called when the response body is consumed.
	 *
	 * @param req The request object.
	 * @param res The response object.
	 * @throws RestCallException Error occurred during call.
	 * @throws Exception
	 * 	Any exception can be thrown.
	 * 	<br>If not a {@link RestCallException} or {@link RuntimeException}, will be wrapped in a {@link RestCallException}.
	 */
	void onClose(RestRequest req, RestResponse res) throws Exception;
}
