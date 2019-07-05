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

import org.apache.http.*;

/**
 * Used to intercept http connection responses to allow modification of that response before processing and for
 * listening for call lifecycle events.
 *
 * <p>
 * Useful if you want to prevent {@link RestCallException RestCallExceptions} from being thrown on error conditions.
 */
public abstract class RestCallInterceptor {

	/**
	 * Called when {@link RestCall} object is created.
	 *
	 * @param restCall The restCall object invoking this method.
	 */
	public void onInit(RestCall restCall) {}

	/**
	 * Called immediately after an HTTP response has been received.
	 *
	 * @param statusCode The HTTP status code received.
	 * @param restCall The restCall object invoking this method.
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 */
	public void onConnect(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res) {}

	/**
	 * Called if retry is going to be attempted.
	 *
	 * @param statusCode The HTTP status code received.
	 * @param restCall The restCall object invoking this method.
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @param ex The exception thrown from the client.
	 */
	public void onRetry(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res, Exception ex) {}

	/**
	 * Called when {@link RestCall#close()} is called.
	 *
	 * @param restCall The restCall object invoking this method.
	 * @throws RestCallException Error occurred during call.
	 */
	public void onClose(RestCall restCall) throws RestCallException {}
}
