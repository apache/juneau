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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * Response handler for {@link Response @Response}-annotated objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
@SuppressWarnings("resource")
public class ResponseBeanProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
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
				if ("*".equals(n)) {
					for (var o2 : iterate(o)) {
						var h = (Header)null;
						if (o2 instanceof Map.Entry o22) {
							var x = o22;
							var k = s(x.getKey());
							h = new SerializedHeader(k, x.getValue(), hm.getSerializer().orElse(defaultPartSerializer).getPartSession(), ps.getProperty(k), true);
						} else if (o2 instanceof Header o22) {
							h = o22;
						} else if (o2 instanceof NameValuePair o23) {
							h = BasicHeader.of(o23);
						} else {
							throw new InternalServerError("Invalid type ''{0}'' for header ''{1}''", cn(o2), n);
						}
						res.addHeader(h);
					}
				} else {
					var h = (Header)null;
					if (o instanceof Header o2)
						h = o2;
					else if (o instanceof NameValuePair o3)
						h = BasicHeader.of(o3);
					else
						h = new SerializedHeader(n, o, hm.getSerializer().orElse(defaultPartSerializer).getPartSession(), ps, true);
					res.addHeader(h);
				}
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not set header ''{0}''", n);
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

	private static Iterable<?> iterate(Object o) {
		if (o == null)
			return Collections.emptyList();
		if (o instanceof Map<?,?> m)
			return m.entrySet();
		if (isArray(o))
			return l((Object[])o);
		if (o instanceof Collection<?> c)
			return c;
		throw new InternalServerError("Could not iterate over Headers of type ''{0}''", cn(o));
	}
}