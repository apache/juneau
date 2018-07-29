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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.util.*;
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.RestMethod.MethodReturnTypes">Overview &gt; juneau-rest-server &gt; Method Return Types</a>
 * </ul>
 */
public class DefaultHandler implements ResponseHandler {

	@SuppressWarnings("resource")
	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, InternalServerError, NotAcceptable {
		SerializerGroup g = res.getSerializers();
		String accept = req.getHeaders().getString("Accept", "");
		SerializerMatch sm = g.getSerializerMatch(accept);

		RestMethodReturn rmr = req.getRestMethodReturn();
		res.setStatus(rmr.getCode());

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
				RequestProperties p = res.getProperties();
				if (req.isPlainText()) {
					res.setContentType("text/plain");
				}
				p.append("mediaType", mediaType).append("characterEncoding", res.getCharacterEncoding());

				SerializerSession session = s.createSession(new SerializerSessionArgs(p, req.getJavaMethod(), req.getLocale(), req.getHeaders().getTimeZone(), mediaType, req.isDebug() ? true : null, req.getUriContext(), req.isPlainText() ? true : null));

				for (Map.Entry<String,String> h : session.getResponseHeaders().entrySet())
					res.setHeader(h.getKey(), h.getValue());

				if (! session.isWriterSerializer()) {
					if (req.isPlainText()) {
						FinishablePrintWriter w = res.getNegotiatedWriter();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						session.serialize(output, baos);
						w.write(StringUtils.toSpacedHex(baos.toByteArray()));
						w.flush();
						w.finish();
					} else {
						FinishableServletOutputStream os = res.getNegotiatedOutputStream();
						session.serialize(output, os);
						os.flush();
						os.finish();
					}
				} else {
					FinishablePrintWriter w = res.getNegotiatedWriter();
					session.serialize(output, w);
					w.flush();
					w.finish();
				}
			} catch (SerializeException e) {
				throw new InternalServerError(e);
			}
			return true;

		}

		HttpPartSerializer ps = rmr.getPartSerializer();
		if (ps != null) {
			try {
				FinishablePrintWriter w = res.getNegotiatedWriter();
				w.append(ps.serialize(rmr.getSchema(), output));
				w.flush();
				w.finish();
			} catch (SchemaValidationException | SerializeException e) {
				throw new InternalServerError(e);
			}
			return true;
		}

		// Non-existent Accept or plain/text can just be serialized as-is.
		if ("".equals(accept) || "plain/text".equals(accept)) {
			FinishablePrintWriter w = res.getNegotiatedWriter();
			ClassMeta<?> cm = req.getBeanSession().getClassMetaForObject(output);
			if (cm != null)
				w.append(cm.toString(output));
			w.flush();
			w.finish();
			return true;
		}

		throw new NotAcceptable(
			"Unsupported media-type in request header ''Accept'': ''{0}''\n\tSupported media-types: {1}",
			req.getHeaders().getString("Accept", ""), g.getSupportedMediaTypes()
		);
	}
}
