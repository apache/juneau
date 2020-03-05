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

import java.io.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.protocol.*;

/**
 * Interface that allows you to override the handling of HTTP requests.
 *
 * <p>
 * Providing this implementation is the equivalent to overriding the {@link RestClient#execute(HttpHost,HttpEntityEnclosingRequestBase,HttpContext)}
 * and {@link RestClient#execute(HttpHost,HttpRequestBase,HttpContext)} methods.
 *
 * <p>
 * This can also be accomplished by providing your own {@link RestClientBuilder#connectionManager(org.apache.http.conn.HttpClientConnectionManager)}
 * or subclassing {@link RestClient}, but this provides a simpler way of handling the requests yourself.
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
 * 	<li class='jm'>{@link RestClientBuilder#callHandler(Class)}
 * 	<li class='jm'>{@link RestClientBuilder#callHandler(RestCallHandler)}
 * </ul>
 */
public interface RestCallHandler {

	/**
	 * Execute the specified body request (e.g. POST/PUT).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException;

	/**
	 * Execute the specified no-body request (e.g. GET/DELETE).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException;
}
