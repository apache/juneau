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

import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.HttpPart;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.rest.server.*;

/**
 * Response handler for {@link Response @Response}-annotated objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",  // ResponseBeanProcessor manages Closeable resources
	"java:S3740" // Raw Class/BeanMeta types used for reflective response bean processing where generic type parameters are erased
})
public class ResponseBeanProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	@SuppressWarnings({
		"java:S6541", // Session objects are single-threaded by design
		"java:S3776", // Cognitive complexity acceptable for this specific logic
	})
	public int process(RestOpSession opSession) throws IOException {

		var req = opSession.getRequest();
		var res = opSession.getResponse();
		var defaultPartSerializer = req.getOpContext().getPartSerializer();

		var output = res.getContent(Object.class);

		if (output == null || ! (nn(output.getClass().getAnnotation(Response.class)) || nn(res.getResponseBeanMeta())))
			return NEXT;

		var rm = res.getResponseBeanMeta();
		if (rm == null)
			rm = req.getOpContext().getResponseBeanMeta(output);

		var stm = rm.getStatusMethod();
		if (nn(stm)) {
			try {
				res.setStatus((int)stm.getGetter().invoke(output));
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not get status.");
			}
		} else if (rm.getCode() != 0) {
			res.setStatus(rm.getCode());
		}

		for (var hm : rm.getHeaderMethods()) {
			var n = hm.getPartName().orElse(null);
			try {
				var o = hm.getGetter().invoke(output);
				var ps = hm.getSchema();
				var serializer = hm.getSerializer().orElse(defaultPartSerializer).getPartSession();
				if ("*".equals(n)) {
					for (var o2 : iterate(o)) {
						HttpHeader h;
						if (o2 instanceof Map.Entry o22) {
							var k = s(o22.getKey());
							h = toHeader(k, o22.getValue(), serializer, ps != null ? ps.getProperty(k) : null);
						} else if (o2 instanceof HttpHeader o22) {
							h = o22;
						} else if (o2 instanceof HttpPart o23) {
							h = HttpStringHeader.of(o23.getName(), o23.getValue());
						} else {
							throw new InternalServerError("Invalid type '%s' for header '%s'", cn(o2), n);
						}
						res.addHeader(h);
					}
				} else {
					HttpHeader h;
					if (o instanceof HttpHeader o2)
						h = o2;
					else if (o instanceof HttpPart o3)
						h = HttpStringHeader.of(o3.getName(), o3.getValue());
					else
						h = toHeader(n, o, serializer, ps);
					res.addHeader(h);
				}
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not set header '%s'", n);
			}
		}

		var bm = rm.getContentMethod();

		if (nn(bm)) {
			var m = bm.getGetter();
			try {
				var pt = m.getParameterTypes();
				if (pt.length == 1) {
					var ptt = pt[0];
					if (ptt == OutputStream.class)
						m.invoke(output, res.getOutputStream());
					else if (ptt == Writer.class)
						m.invoke(output, res.getWriter());
					return 1;
				}
				res.setContent(m.invoke(output));
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not get content.");
			}
		}

		return NEXT;  // Let PojoProcessor serialize it.
	}

	/**
	 * Serializes the given value to a String using the given part serializer/schema, then returns it as an
	 * {@link HttpStringHeader}. Returns {@code null} only if the serialized value would be a blank string
	 * and {@code skipIfEmpty} semantics apply.
	 */
	@SuppressWarnings({
		"java:S112" // Propagates the checked exceptions thrown by HttpPartSerializerSession.write(...); the caller already re-wraps them in InternalServerError.
	})
	private static HttpHeader toHeader(String name, Object value, HttpPartSerializerSession session, HttpPartSchema schema) throws Exception {
		String v;
		if (session != null)
			v = session.write(HttpPartType.HEADER, schema, value);
		else
			v = value == null ? null : value.toString();
		if (v != null && v.isEmpty())
			v = null;
		return HttpStringHeader.of(name, v);
	}

	private static Iterable<?> iterate(Object o) {
		if (o == null)
			return Collections.emptyList();
		if (o instanceof Map<?,?> m)
			return m.entrySet();
		if (isArray(o))
			return l((Object[])o);
		if (o instanceof Collection<?> c)
			return c;
		throw new InternalServerError("Could not iterate over Headers of type '%s'", cn(o));
	}
}
