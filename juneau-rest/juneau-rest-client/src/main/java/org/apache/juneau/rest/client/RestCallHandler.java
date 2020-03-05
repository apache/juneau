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
import org.apache.http.client.*;
import org.apache.http.client.methods.*;

/**
 * Interface that allows you to override the handling of HTTP requests.
 *
 * <p>
 * Providing this implementation is the equivalent to overriding the {@link RestClient#execute(HttpEntityEnclosingRequestBase)}
 * and {@link RestClient#execute(HttpRequestBase)} methods.
 *
 * <p>
 * This can also be accomplished by providing your own {@link RestClientBuilder#connectionManager(org.apache.http.conn.HttpClientConnectionManager)},
 * but this provides a simpler way of handling the requests yourself.
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
 * 	<li class='jf'>{@link RestClientBuilder#callHandler(Class)}
 * 	<li class='jf'>{@link RestClientBuilder#callHandler(RestCallHandler)}
 * </ul>
 *
 * @deprecated Use {@link org.apache.juneau.rest.client2.RestCallHandler} class.
 */
@Deprecated
public interface RestCallHandler {

	/**
	 * Execute the specified body request (e.g. POST/PUT).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param req The HTTP request.
	 * @return The HTTP response.
	 * @throws IOException Stream exception occurred.
	 * @throws ClientProtocolException Signals an error in the HTTP protocol.
	 */
	HttpResponse execute(HttpEntityEnclosingRequestBase req) throws ClientProtocolException, IOException;

	/**
	 * Execute the specified no-body request (e.g. GET/DELETE).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param req The HTTP request.
	 * @return The HTTP response.
	 * @throws IOException Stream exception occurred.
	 * @throws ClientProtocolException Signals an error in the HTTP protocol.
	 */
	HttpResponse execute(HttpRequestBase req) throws ClientProtocolException, IOException;
}
