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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.util.FinishablePrintWriter;
import org.apache.juneau.rest.util.FinishableServletOutputStream;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Response handler for POJOs not handled by other handlers.
 *
 * <p>
 * This uses the serializers defined on the response to serialize the POJO.
 *
 * <p>
 * The {@link Serializer} used is based on the <c>Accept</c> header on the request.
 *
 * <p>
 * The <c>Content-Type</c> header is set to the mime-type defined on the selected serializer based on the
 * <c>produces</c> value passed in through the constructor.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmReturnTypes}
 * </ul>
 */
public class DefaultHandler implements ResponseHandler {

	@SuppressWarnings("resource")
	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res) throws IOException, InternalServerError, NotAcceptable {
		SerializerGroup g = res.getSerializers();
		String accept = req.getHeaders().getString("Accept", "*/*");
		SerializerMatch sm = g.getSerializerMatch(accept);
		HttpPartSchema schema = null;

		Object o = res.getOutput();

		ResponseBeanMeta rm = res.getResponseMeta();
		if (rm == null)
			rm = req.getOpContext().getResponseBeanMeta(o);

		if (rm != null) {

			boolean isThrowable = rm.getClassMeta().isType(Throwable.class);
			if (isThrowable) {
				res.setHeaderSafe("Exception-Name", rm.getClassMeta().getName());
				res.setHeaderSafe("Exception-Message", ((Throwable)o).getMessage());
				if (req.isDebug())
					((Throwable)o).printStackTrace();
			}

			ResponseBeanPropertyMeta stm = rm.getStatusMethod();
			if (stm != null) {
				try {
					res.setStatus((int)stm.getGetter().invoke(o));
				} catch (Exception e) {
					throw new InternalServerError(e, "Could not get status.");
				}
			} else if (rm.getCode() != 0) {
				res.setStatus(rm.getCode());
			}

			for (ResponseBeanPropertyMeta hm : rm.getHeaderMethods()) {
				try {
					Object ho = hm.getGetter().invoke(o);
					String n = hm.getPartName();
					if ("*".equals(n)) {
						for (Object ho2 : iterate(ho)) {
							if (ho2 instanceof Map.Entry) {
								@SuppressWarnings("rawtypes")
								Map.Entry e = (Map.Entry)ho2;
								String k = stringify(e.getKey());
								Object v = e.getValue();
								HttpPartSchema s = hm.getSchema().getProperty(k);
								res.setHeader(new HttpPart(k, RESPONSE_HEADER, s, hm.getSerializer(req.getPartSerializerSession()), v));
							} else if (ho2 instanceof NameValuePair) {
								NameValuePair p = (NameValuePair)ho2;
								res.setHeader(p.getName(), p.getValue());
							} else {
								throw new InternalServerError("Invalid type ''{0}'' for header ''{1}''", hm.getPartName(), ho2 == null ? null : ho2.getClass().getName());
							}
						}
					} else {
						if (ho instanceof NameValuePair) {
							NameValuePair p = (NameValuePair)ho;
							res.setHeader(p.getName(), p.getValue());
						} else {
							res.setHeader(new HttpPart(n, RESPONSE_HEADER, hm.getSchema(), hm.getSerializer(req.getPartSerializerSession()), ho));
						}
					}
				} catch (Exception e) {
					throw new InternalServerError(e, "Could not set header ''{0}''", hm.getPartName());
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
							m.invoke(o, res.getOutputStream());
						else if (ptt == Writer.class)
							m.invoke(o, res.getWriter());
						return true;
					}
					o = m.invoke(o);
				} catch (Exception e) {
					throw new InternalServerError(e, "Could not get body.");
				}
			}

			schema = rm.getSchema();
		}

		if (o instanceof HttpEntity) {
			HttpEntity e = (HttpEntity)o;
			res.header(e.getContentType()).header(e.getContentEncoding());
			long contentLength = e.getContentLength();
			if (contentLength >= 0)
				res.header(ContentLength.of(contentLength));
			try (OutputStream os = res.getNegotiatedOutputStream()) {
				e.writeTo(os);
				os.flush();
			}
			return true;
		}

		if (sm != null) {
			Serializer s = sm.getSerializer();
			MediaType mediaType = res.getMediaType();
			if (mediaType == null)
				mediaType = sm.getMediaType();

			MediaType responseType = s.getResponseContentType();
			if (responseType == null)
				responseType = mediaType;

			res.setContentType(responseType.toString());

			try {
				if (req.isPlainText())
					res.setContentType("text/plain");
				SerializerSession session = s.createSession(
					SerializerSessionArgs
						.create()
						.properties(req.getAttributes())
						.javaMethod(req.getOpContext().getJavaMethod())
						.locale(req.getLocale())
						.timeZone(req.getHeaders().getTimeZone())
						.mediaType(mediaType)
						.streamCharset(res.getCharset())
						.schema(schema)
						.debug(req.isDebug() ? true : null)
						.uriContext(req.getUriContext())
						.useWhitespace(req.isPlainText() ? true : null)
						.resolver(req.getVarResolverSession())
				);

				for (Map.Entry<String,String> h : session.getResponseHeaders().entrySet())
					res.setHeaderSafe(h.getKey(), h.getValue());

				if (! session.isWriterSerializer()) {
					if (req.isPlainText()) {
						FinishablePrintWriter w = res.getNegotiatedWriter();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						session.serialize(o, baos);
						w.write(StringUtils.toSpacedHex(baos.toByteArray()));
						w.flush();
						w.finish();
					} else {
						FinishableServletOutputStream os = res.getNegotiatedOutputStream();
						session.serialize(o, os);
						os.flush();
						os.finish();
					}
				} else {
					FinishablePrintWriter w = res.getNegotiatedWriter();
					session.serialize(o, w);
					w.flush();
					w.finish();
				}
			} catch (SerializeException e) {
				throw new InternalServerError(e);
			}
			return true;
		}

		// Non-existent Accept or plain/text can just be serialized as-is.
		if (o != null && (isEmpty(accept) || accept.startsWith("text/plain") || accept.contains("*/*"))) {
			String out = null;
			if (isEmpty(res.getContentType()))
				res.setContentType("text/plain");
			if (o instanceof InputStream) {
				try (OutputStream os = res.getNegotiatedOutputStream()) {
					IOPipe.create(o, os).run();
					os.flush();
				}
			} else if (o instanceof Reader) {
				try (FinishablePrintWriter w = res.getNegotiatedWriter()) {
					IOPipe.create(o, w).run();
					w.flush();
					w.finish();
				}
			} else {
				out = req.getBeanSession().getClassMetaForObject(o).toString(o);
				FinishablePrintWriter w = res.getNegotiatedWriter();
				w.append(out);
				w.flush();
				w.finish();
			}
			return true;
		}

		throw new NotAcceptable(
			"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
			req.getHeaders().getString("Accept", ""), g.getSupportedMediaTypes()
		);
	}

	private Iterable<?> iterate(Object o) {
		if (o == null)
			return Collections.emptyList();
		if (o instanceof Map)
			return ((Map<?,?>)o).entrySet();
		if (o.getClass().isArray())
			return Arrays.asList(o);
		if (o instanceof Collection)
			return (Collection<?>)o;
		throw new InternalServerError("Could not iterate over Headers of type ''{0}''", o.getClass().getName());
	}
 }
