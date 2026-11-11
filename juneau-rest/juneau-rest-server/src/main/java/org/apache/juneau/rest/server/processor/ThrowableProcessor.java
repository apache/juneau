/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.processor;

import java.io.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.rrpc.*;

/**
 * Response handler for {@link Throwable} objects.
 *
 * <p>
 * Adds a <c>Thrown</c> header (exception class name and message) to the response and returns <c>0</c> so that the
 * processor chain can continue.
 *
 * <p>
 * The header is only added when the exception was deliberately thrown for a client audience (a
 * {@link BasicHttpException} or a class annotated with {@link Response @Response}), when
 * {@link RestContext#isRenderResponseStackTraces() renderResponseStackTraces} is enabled, or when the operation is an
 * RRPC dispatch target (the {@code @Remote} client relies on the header to reconstruct the exception type).
 * Any other, unplanned exception keeps its class name and message out of the response by default.
 *
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ThrowableProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {
		RestResponse res = opSession.getResponse();
		Throwable t = res.getContent(Throwable.class);
		if (t != null && isExposable(opSession, t))
			res.addHeader(Thrown.of(t));
		return NEXT; // Continue processing as bean.
	}

	private static boolean isExposable(RestOpSession opSession, Throwable t) {
		return t instanceof BasicHttpException
			|| ClassInfo.of(t).hasAnnotation(Response.class)
			|| opSession.getRestContext().isRenderResponseStackTraces()
			|| opSession.getContext() instanceof RrpcRestOpContext;
	}
}