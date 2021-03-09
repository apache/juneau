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
package org.apache.juneau.rest.processors;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.http.response.*;

/**
 * Response processor for {@link Reader} objects.
 *
 * <p>
 * Simply pipes the contents of the {@link Reader} to {@link RestResponse#getNegotiatedWriter()}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmReturnTypes}
 * </ul>
 */
public final class ReaderProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public boolean process(RestCall call) throws IOException, NotAcceptable, BasicHttpException {
		if (call.getOutputInfo().isChildOf(Reader.class)) {
			RestResponse res = call.getRestResponse();
			try (Reader r = res.getOutput(Reader.class); Writer w = res.getNegotiatedWriter()) {
				pipe(r, w);
			}
			return true;
		}
		return false;
	}
}

