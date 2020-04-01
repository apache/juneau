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
import org.apache.http.protocol.*;

/**
 * A default implementation of a {@link RestCallInterceptor}.
 *
 * All default methods are no-ops.
 */
public abstract class BasicRestCallInterceptor implements RestCallInterceptor {

	@Override /* RestCallInterceptor */
	public void onInit(RestRequest req) throws Exception {}

	@Override /* HttpRequestInterceptor */
	public void process(HttpRequest request, HttpContext context) {}

	@Override /* HttpResponseInterceptor */
	public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {}

	@Override /* RestCallInterceptor */
	public void onConnect(RestRequest req, RestResponse res) throws Exception {}

	@Override /* RestCallInterceptor */
	public void onClose(RestRequest req, RestResponse res) throws Exception {}
}
