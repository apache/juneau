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
package org.apache.juneau.rest.reshandlers;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.utils.*;

/**
 * Response handler for {@link InputStream} objects.
 *
 * <p>
 * Simply pipes the contents of the {@link InputStream} to {@link RestResponse#getNegotiatedOutputStream()}.
 *
 * <p>
 * Sets the <c>Content-Type</c> response header to whatever was set via {@link RestResponse#setContentType(String)}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmReturnTypes}
 * </ul>
 */
public final class InputStreamHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestCall call) throws IOException, NotAcceptable, HttpException {
		RestResponse res = call.getRestResponse();
		if (res.isOutputType(InputStream.class)) {
			try (OutputStream os = res.getNegotiatedOutputStream()) {
				IOPipe.create(res.getOutput(InputStream.class), os).run();
			}
			return true;
		}
		return false;
	}
}
