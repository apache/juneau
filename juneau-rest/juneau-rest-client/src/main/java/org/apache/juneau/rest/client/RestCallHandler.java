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
import org.apache.http.protocol.*;

/**
 * Interface that allows you to override the handling of HTTP requests.
 *
 * <p>
 * Providing this implementation is the equivalent to overriding the {@link RestClient#execute(HttpHost,HttpRequest,HttpContext)}.
 * <br>This can also be accomplished by providing your own {@link RestClient.Builder#connectionManager(org.apache.http.conn.HttpClientConnectionManager) connection manager}
 * or subclassing {@link RestClient}, but this provides a simpler way of handling the requests yourself.
 *
 * <p>
 * The constructor on the implementation class can optionally take in any of the following parameters:
 * <ul>
 * 	<li>{@link RestClient} - The client using this handler.
 * </ul>
 *
 * <p>
 * The {@link BasicRestCallHandler} shows an example of a simple pass-through handler.  Note that you must handle
 * the case where {@link HttpHost} is null.
 *
 * <p class='bjava'>
 * 	<jk>public class</jk> BasicRestCallHandler <jk>implements</jk> RestCallHandler {
 *
 * 		<jk>private final</jk> RestClient <jf>client</jf>;
 *
 * 		<jk>public</jk> BasicRestCallHandler(RestClient <jv>client</jv>) {
 * 			<jk>this</jk>.<jf>client</jf> = <jv>client</jv>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> HttpResponse run(HttpHost <jv>target</jv>, HttpRequest <jv>request</jv>, HttpContext <jv>context</jv>) <jk>throws</jk> IOException {
 * 			<jk>if</jk> (<jv>target</jv> == <jk>null</jk>)
 * 				<jk>return</jk> <jf>client</jf>.execute((HttpUriRequest)<jv>request</jv>, <jv>context</jv>);
 * 			<jk>return</jk> <jf>client</jf>.execute(<jv>target</jv>, <jv>request</jv>, <jv>context</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link RestClient.Builder#callHandler()}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public interface RestCallHandler {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Execute the specified request.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.  Must be an instance of {@link HttpUriRequest} if the target is <jk>null</jk>.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws ClientProtocolException, IOException;
}
