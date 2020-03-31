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
package org.apache.juneau.rest.client2;

/**
 * Used to intercept http connection responses to allow modification of that response before processing and for
 * listening for call lifecycle events.
 *
 * <p>
 * Useful if you want to prevent {@link RestCallException RestCallExceptions} from being thrown on error conditions.
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
 * 	<li class='jm'>{@link RestClientBuilder#interceptors(Class...)}
 * 	<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...)}
 * </ul>
 */
public interface RestCallInterceptor {

	/**
	 * Called immediately after {@link RestRequest} object is created and all headers/query/form-data has been
	 * set on the request from the client.
	 *
	 * @param req The HTTP request object.
	 * @throws Exception Any exception can be thrown.
	 */
	public void onInit(RestRequest req) throws Exception;

	/**
	 * Called immediately after an HTTP response has been received.
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @throws Exception Any exception can be thrown.
	 */
	public void onConnect(RestRequest req, RestResponse res) throws Exception;

	/**
	 * Called when the response body is consumed.
	 *
	 * @param req The request object.
	 * @param res The response object.
	 * @throws RestCallException Error occurred during call.
	 * @throws Exception Any exception can be thrown.
	 */
	public void onClose(RestRequest req, RestResponse res) throws Exception;
}
