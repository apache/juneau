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
package org.apache.juneau.rest.server.mcp.v20260728;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Emits a genuinely empty (zero-byte) HTTP body for JSON-RPC notifications.
 *
 * <p>
 * {@link McpRevision#dispatch} returns {@code null} for a notification (a request with no {@code id}),
 * per the JSON-RPC 2.0 spec's "no response" contract. Left to the default response-processor chain, the
 * framework's catch-all {@link org.apache.juneau.rest.server.processor.SerializedPojoProcessor} would
 * still hand that {@code null} to the negotiated serializer, which writes the four-byte JSON literal
 * {@code null} rather than an empty body — correct generic POJO-serialization behavior, but wrong for a
 * spec-mandated empty notification response.
 *
 * <p>
 * This processor short-circuits before that happens: a {@code null} response content is FINISHED with
 * no bytes written, while any non-{@code null} content is left to the rest of the chain (returns
 * {@link #NEXT}). It implements {@link ViewRenderer} purely to opt into that interface's documented
 * ordering guarantee — {@link org.apache.juneau.rest.server.processor.ResponseProcessorList} always
 * repositions {@link ViewRenderer}s immediately before the first
 * {@link org.apache.juneau.rest.server.processor.CatchAllResponseProcessor} — so this runs before
 * {@code SerializedPojoProcessor} regardless of {@code @Rest(responseProcessors)} registration order.
 * This class renders no view; it never returns {@link #NEXT} for a reason a real view renderer would.
 *
 * @serial exclude
 */
final class McpNotificationResponseProcessor implements ViewRenderer {

	@Override
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var res = opSession.getResponse();
		if (res.getContent(Object.class) != null)
			return NEXT;
		res.getNegotiatedOutputStream().finish();
		return FINISHED;
	}
}
