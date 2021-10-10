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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;

/**
 * Response handler for {@link Response @Response}-annotated objects.
 */
public final class ResponseBeanProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestCall call) throws IOException {

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();
		HttpPartSerializer defaultPartSerializer = req.getOpContext().getPartSerializer();

		Object output = res.getOutput(Object.class);

		if (output == null)
			return NEXT;

		if (! (output.getClass().getAnnotation(Response.class) != null || res.getResponseBeanMeta() != null))
			return NEXT;

		ResponseBeanMeta rm = res.getResponseBeanMeta();
		if (rm == null)
			rm = req.getOpContext().getResponseBeanMeta(output);

		ResponseBeanPropertyMeta stm = rm.getStatusMethod();
		if (stm != null) {
			try {
				res.setStatus((int)stm.getGetter().invoke(output));
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not get status.");
			}
		} else if (rm.getCode() != 0) {
			res.setStatus(rm.getCode());
		}

		for (ResponseBeanPropertyMeta hm : rm.getHeaderMethods()) {
			String n = hm.getPartName().orElse(null);
			try {
				Object o = hm.getGetter().invoke(output);
				HttpPartSchema ps = hm.getSchema();
				if ("*".equals(n)) {
					for (Object o2 : iterate(o)) {
						Header h = null;
						if (o2 instanceof Map.Entry) {
							@SuppressWarnings("rawtypes")
							Map.Entry x = (Map.Entry)o2;
							String k = stringify(x.getKey());
							h = new SerializedHeader(k, x.getValue(), hm.getSerializer().orElse(defaultPartSerializer).getPartSession(), ps.getProperty(k), true);
						} else if (o2 instanceof Header) {
							h = (Header)o2;
						} else if (o2 instanceof NameValuePair) {
							h = BasicHeader.of((NameValuePair)o2);
						} else {
							throw new InternalServerError("Invalid type ''{0}'' for header ''{1}''", className(o2), n);
						}
						res.addHeader(h);
					}
				} else {
					Header h = null;
					if (o instanceof Header)
						h = (Header)o;
					else if (o instanceof NameValuePair)
						h = BasicHeader.of((NameValuePair)o);
					else
						h = new SerializedHeader(n, o, hm.getSerializer().orElse(defaultPartSerializer).getPartSession(), ps, true);
					res.addHeader(h);
				}
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not set header ''{0}''", n);
			}
		}

		ResponseBeanPropertyMeta bm = rm.getBodyMethod();

		if (bm != null) {
			Method m = bm.getGetter();
			try {
				Class<?>[] pt = m.getParameterTypes();
				if (pt.length == 1) {
					Class<?> ptt = pt[0];
					if (ptt == OutputStream.class)
						m.invoke(output, res.getOutputStream());
					else if (ptt == Writer.class)
						m.invoke(output, res.getWriter());
					return 1;
				}
				res.setOutput(m.invoke(output));
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not get body.");
			}
		}

		return NEXT;  // Let PojoProcessor serialize it.
	}

	private Iterable<?> iterate(Object o) {
		if (o == null)
			return Collections.emptyList();
		if (o instanceof Map)
			return ((Map<?,?>)o).entrySet();
		if (o.getClass().isArray())
			return Arrays.asList((Object[])o);
		if (o instanceof Collection)
			return (Collection<?>)o;
		throw new InternalServerError("Could not iterate over Headers of type ''{0}''", className(o));
	}
}
