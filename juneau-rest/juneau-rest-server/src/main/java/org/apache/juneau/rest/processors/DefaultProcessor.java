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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.util.FinishablePrintWriter;
import org.apache.juneau.rest.util.FinishableServletOutputStream;
import org.apache.juneau.serializer.*;

/**
 * Response processor for POJOs not handled by other processors.
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
public class DefaultProcessor implements ResponseProcessor {

	@SuppressWarnings("resource")
	@Override /* ResponseHandler */
	public int process(RestCall call) throws IOException, InternalServerError, NotAcceptable {
		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();
		SerializerGroup g = res.getOpContext().getSerializers();
		String accept = req.getHeader("Accept").orElse("*/*");
		SerializerMatch sm = g.getSerializerMatch(accept);
		HttpPartSchema schema = null;
		HttpPartSerializerSession ps = req.getPartSerializerSession();

		Optional<Optional<Object>> output = res.getOutput();
		Object o = output.isPresent() ? output.get().orElse(null) : null;

		ResponseBeanMeta rm = res.getResponseMeta();
		if (rm == null)
			rm = req.getOpContext().getResponseBeanMeta(o);

		if (rm != null) {

			boolean isThrowable = rm.getClassMeta().isChildOf(Throwable.class);
			if (isThrowable && o != null) {
				Throwable t = (Throwable)o;
				res.setHeader(Thrown.of(t));
				if (req.isDebug())
					t.printStackTrace();
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
				String n = hm.getPartName().orElse(null);
				try {
					Object ho = hm.getGetter().invoke(o);
					HttpPartSchema partSchema = hm.getSchema();
					if ("*".equals(n)) {
						for (Object ho2 : iterate(ho)) {
							if (ho2 instanceof Map.Entry) {
								@SuppressWarnings("rawtypes")
								Map.Entry x = (Map.Entry)ho2;
								String k = stringify(x.getKey());
								Object v = x.getValue();
								res.setHeader(new HttpPart(k, RESPONSE_HEADER, partSchema.getProperty(k), hm.getSerializerSession().orElse(ps), v));
							} else if (ho2 instanceof SerializedHeader) {
								SerializedHeader x = ((SerializedHeader)ho2);
								x = x.copyWithSerializerAndSchema(ps, partSchema.getProperty(x.getName()));
								res.setHeader(x.getName(), x.getValue());
							} else if (ho2 instanceof SerializedPart) {
								SerializedPart x = ((SerializedPart)ho2).copy().serializerIfNotSet(ps);
								x.schemaIfNotSet(partSchema.getProperty(x.getName()));
								res.setHeader(x.getName(), x.getValue());
							} else if (ho2 instanceof BasicUriHeader) {
								BasicUriHeader x = (BasicUriHeader)ho2;
								res.setHeader(x.getName(), req.getUriResolver().resolve(x.getValue()));
							} else if (ho2 instanceof Header) {
								Header x = (Header)ho2;
								res.setHeader(x.getName(), x.getValue());
							} else if (ho2 instanceof NameValuePair) {
								NameValuePair x = (NameValuePair)ho2;
								res.setHeader(x.getName(), x.getValue());
							} else {
								throw new InternalServerError("Invalid type ''{0}'' for header ''{1}''", ho2 == null ? null : ho2.getClass().getName(), n);
							}
						}
					} else {
						if (ho instanceof SerializedHeader) {
							SerializedHeader x = ((SerializedHeader)ho).copyWithSerializerAndSchema(ps, schema);
							res.setHeader(x.getName(), x.getValue());
						} else if (ho instanceof SerializedPart) {
							SerializedPart x = ((SerializedPart)ho).copy().serializerIfNotSet(ps);
							x.schemaIfNotSet(schema);
							res.setHeader(x.getName(), x.getValue());
						} else if (ho instanceof Header) {
							Header x = (Header)ho;
							res.setHeader(x.getName(), x.getValue());
						} else if (ho instanceof NameValuePair) {
							NameValuePair x = (NameValuePair)ho;
							res.setHeader(x.getName(), x.getValue());
						} else {
							res.setHeader(new HttpPart(n, RESPONSE_HEADER, hm.getSchema(), hm.getSerializerSession().orElse(ps), ho));
						}
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
							m.invoke(o, res.getOutputStream());
						else if (ptt == Writer.class)
							m.invoke(o, res.getWriter());
						return 1;
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
				res.header(contentLength(contentLength));
			try (OutputStream os = res.getNegotiatedOutputStream()) {
				e.writeTo(os);
				os.flush();
			}
			return 1;
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
						.properties(req.getAttributes().asMap())
						.javaMethod(req.getOpContext().getJavaMethod())
						.locale(req.getLocale())
						.timeZone(req.getTimeZone().orElse(null))
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
			return 1;
		}

		// Non-existent Accept or plain/text can just be serialized as-is.
		if (o != null && (isEmpty(accept) || accept.startsWith("text/plain") || accept.contains("*/*"))) {
			String out = null;
			if (isEmpty(res.getContentType()))
				res.setContentType("text/plain");
			if (o instanceof InputStream) {
				try (OutputStream os = res.getNegotiatedOutputStream()) {
					pipe((InputStream)o, os);
				}
			} else if (o instanceof Reader) {
				try (FinishablePrintWriter w = res.getNegotiatedWriter()) {
					pipe((Reader)o, w);
					w.finish();
				}
			} else {
				out = req.getBeanSession().getClassMetaForObject(o).toString(o);
				FinishablePrintWriter w = res.getNegotiatedWriter();
				w.append(out);
				w.flush();
				w.finish();
			}
			return 1;
		}

		if (o == null)
			return 1;

		throw new NotAcceptable(
			"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
			req.getHeader("Accept").orElse(""), g.getSupportedMediaTypes()
		);
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
		throw new InternalServerError("Could not iterate over Headers of type ''{0}''", o.getClass().getName());
	}
 }
