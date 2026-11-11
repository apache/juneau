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
package org.apache.juneau.rest.processor;

import java.io.*;

import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.rrpc.*;
import org.apache.juneau.rest.util.*;

/**
 * Response handler for {@link Throwable} objects.
 *
 * <p>
 * Adds a <c>Thrown</c> header to the response and returns <c>0</c> so that the processor chain can continue.
 *
 * <p>
 * The header carries the exception class and message, so it is only added for planned exceptions
 * (those that are {@link BasicHttpException HTTP exceptions} or annotated with {@link Response @Response}),
 * for RRPC dispatch targets (where the remote proxy uses it to reconstruct the thrown exception), or when
 * {@link RestContext.Builder#renderResponseStackTraces() renderResponseStackTraces} is enabled.
 * For any other exception, the exception detail is not exposed to the client and the response body is limited to the HTTP status text.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ThrowableProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		RestResponse res = opSession.getResponse();
		Throwable t = res.getContent(Throwable.class);

		if (t == null)
			return NEXT;

		if (isExposable(opSession, t))
			res.addHeader(Thrown.of(t));
		else
			res.setContent(RestUtils.getHttpResponseText(res.getStatus()));  // Don't serialize the exception itself.

		return NEXT; // Continue processing as bean.
	}

	private static boolean isExposable(RestOpSession opSession, Throwable t) {
		return opSession.getRestContext().isRenderResponseStackTraces()
			|| t instanceof BasicHttpException
			|| t.getClass().isAnnotationPresent(Response.class)
			|| opSession.getContext() instanceof RrpcRestOpContext;
	}
}
