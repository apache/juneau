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
package org.apache.juneau.rest.response;

import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;

/**
 * Response handler for POJOs not handled by other handlers.
 * <p>
 * This uses the serializers defined on the response to serialize the POJO.
 * <p>
 * The {@link Serializer} used is based on the <code>Accept</code> header on the request.
 * <p>
 * The <code>Content-Type</code> header is set to the mime-type defined on the selected
 * 	serializer based on the {@link Produces#contentType() @Produces.contentType} annotation.
 */
public class DefaultHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		SerializerGroup g = res.getSerializerGroup();
		String accept = req.getHeader("Accept", "");
		SerializerMatch sm = g.getSerializerMatch(accept);
		if (sm != null) {
			Serializer s = sm.getSerializer();
			MediaType mediaType = res.getMediaType();
			if (mediaType == null)
				mediaType = sm.getMediaType();
			res.setContentType(mediaType.toString());
			ObjectMap headers = s.getResponseHeaders(res.getProperties());
			if (headers != null)
				for (String key : headers.keySet())
					res.setHeader(key, headers.getString(key));

			try {
				ObjectMap p = res.getProperties();
				if (req.isPlainText()) {
					p.put(SerializerContext.SERIALIZER_useWhitespace, true);
					res.setContentType("text/plain");
				}
				p.append("mediaType", mediaType).append("characterEncoding", res.getCharacterEncoding());
				if (! s.isWriterSerializer()) {
					OutputStreamSerializer s2 = (OutputStreamSerializer)s;
					OutputStream os = res.getNegotiatedOutputStream();
					SerializerSession session = s.createSession(os, p, req.getJavaMethod(), req.getLocale(), req.getTimeZone(), mediaType);
					s2.serialize(session, output);
					os.close();
				} else {
					WriterSerializer s2 = (WriterSerializer)s;
					Writer w = res.getNegotiatedWriter();
					SerializerSession session = s.createSession(w, p, req.getJavaMethod(), req.getLocale(), req.getTimeZone(), mediaType);
					s2.serialize(session, output);
					w.close();
				}
			} catch (SerializeException e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		} else {
			throw new RestException(SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
				req.getHeader("Accept", ""), g.getSupportedMediaTypes()
			);
		}
		return true;
	}
}
