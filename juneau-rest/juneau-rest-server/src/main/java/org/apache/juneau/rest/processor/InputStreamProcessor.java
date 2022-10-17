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
package org.apache.juneau.rest.processor;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.rest.*;

/**
 * Response processor for {@link InputStream} objects.
 *
 * <p>
 * Simply pipes the contents of the {@link InputStream} to {@link RestResponse#getNegotiatedOutputStream()}.
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.jrs.ResponseProcessors">Response Processors</a>
 * </ul>
 */
public final class InputStreamProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		RestResponse res = opSession.getResponse();
		InputStream is = res.getContent(InputStream.class);

		if (is == null)
			return NEXT;

		try (InputStream is2 = is; OutputStream os = res.getNegotiatedOutputStream()) {
			pipe(is2, os);
		}

		return FINISHED;
	}
}
