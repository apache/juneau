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
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;

/**
 * Response handler for POJOs not handled by other handlers.
 * 
 * <p>
 * This uses the serializers defined on the response to serialize the POJO.
 * 
 * <p>
 * The {@link Serializer} used is based on the <code>Accept</code> header on the request.
 * 
 * <p>
 * The <code>Content-Type</code> header is set to the mime-type defined on the selected serializer based on the
 * <code>produces</code> value passed in through the constructor.
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.MethodReturnTypes">Overview &gt; Method Return Types</a>
 * </ul>
 */
public class DefaultHandler implements ResponseHandler {

	@SuppressWarnings("resource")
	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		SerializerGroup g = res.getSerializers();
		String accept = req.getHeaders().getString("Accept", "");
		SerializerMatch sm = g.getSerializerMatch(accept);
		if (sm != null) {
			Serializer s = sm.getSerializer();
			MediaType mediaType = res.getMediaType();
			if (mediaType == null)
				mediaType = sm.getMediaType();
			res.setContentType(mediaType.toString());

			try {
				RequestProperties p = res.getProperties();
				if (req.isPlainText()) {
					res.setContentType("text/plain");
				}
				p.append("mediaType", mediaType).append("characterEncoding", res.getCharacterEncoding());

				SerializerSession session = s.createSession(new SerializerSessionArgs(p, req.getJavaMethod(), req.getLocale(), req.getHeaders().getTimeZone(), mediaType, req.getUriContext()));

				for (Map.Entry<String,String> h : session.getResponseHeaders().entrySet())
					res.setHeader(h.getKey(), h.getValue());

				if (! session.isWriterSerializer()) {
					if (req.isPlainText()) {
						Writer w = res.getNegotiatedWriter();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						session.serialize(output, baos);
						w.write(StringUtils.toSpacedHex(baos.toByteArray()));
						w.close();  // Leave open if exception occurs.
					} else {
						OutputStream os = res.getNegotiatedOutputStream();
						session.serialize(output, os);
						os.close();  // Leave open if exception occurs.
					}
				} else {
					Writer w = res.getNegotiatedWriter();
					session.serialize(output, w);
					w.close();  // Leave open if exception occurs.
				}
			} catch (SerializeException e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		} else {
			throw new RestException(SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
				req.getHeaders().getString("Accept", ""), g.getSupportedMediaTypes()
			);
		}
		return true;
	}
}
