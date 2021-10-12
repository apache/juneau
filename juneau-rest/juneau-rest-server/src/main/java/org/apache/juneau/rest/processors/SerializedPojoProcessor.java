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

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Response handler for plain-old Java objects.
 */
public final class SerializedPojoProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestCall call) throws IOException, NotAcceptable, BasicHttpException {
		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();
		SerializerMatch sm = res.getSerializerMatch().orElse(null);
		HttpPartSchema schema = res.getBodySchema().orElse(null);

		Object o = res.getOutput(Object.class);

		if (sm != null) {
			try {
				Serializer s = sm.getSerializer();
				MediaType mediaType = res.getMediaType();
				if (mediaType == null)
					mediaType = sm.getMediaType();

				MediaType responseType = s.getResponseContentType();
				if (responseType == null)
					responseType = mediaType;

				if (req.isPlainText())
					res.setHeader(ContentType.TEXT_PLAIN);
				else
					res.setHeader(ContentType.of(responseType.toString()));

				SerializerSession session = s
					.createSession()
					.properties(req.getAttributes().asMap())
					.javaMethod(req.getOpContext().getJavaMethod())
					.locale(req.getLocale())
					.timeZone(req.getTimeZone().orElse(null))
					.mediaType(mediaType)
					.apply(WriterSerializerSession.Builder.class, x -> x.streamCharset(res.getCharset()).useWhitespace(req.isPlainText() ? true : null))
					.schema(schema)
					.debug(req.isDebug() ? true : null)
					.uriContext(req.getUriContext())
					.resolver(req.getVarResolverSession())
					.build();

				for (Map.Entry<String,String> h : session.getResponseHeaders().entrySet())
					res.addHeader(h.getKey(), h.getValue());

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
			return FINISHED;
		}

		if (o == null)
			return FINISHED;

		throw new NotAcceptable(
			"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
			req.getHeader("Accept").orElse(""), json(res.getOpContext().getSerializers().getSupportedMediaTypes())
		);
	}
}

