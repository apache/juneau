/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.response;

import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;

/**
 * Response handler for POJOs not handled by other handlers.
 * <p>
 * This uses the serializers defined on the response to serialize the POJO.
 * <p>
 * The {@link Serializer} used is based on the <code>Accept</code> header on the request.
 * <p>
 * The <code>Content-Type</code> header is set to the mime-type defined on the selected
 * 	serializer based on the {@link Produces#contentType() @Produces.contentType} annotation.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class DefaultHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		SerializerGroup g = res.getSerializerGroup();
		String accept = req.getHeader("Accept", "");
		String matchingAccept = g.findMatch(accept);
		if (matchingAccept != null) {
			Serializer<?> r = g.getSerializer(matchingAccept);
			String contentType = r.getResponseContentType();
			if (contentType == null)
				contentType = res.getContentType();
			if (contentType == null)
				contentType = matchingAccept;
			res.setContentType(contentType);
			ObjectMap headers = r.getResponseHeaders(res.getProperties());
			if (headers != null)
				for (String key : headers.keySet())
					res.setHeader(key, headers.getString(key));

			try {
				ObjectMap p = res.getProperties();
				if (req.isPlainText()) {
					p.put(SerializerProperties.SERIALIZER_useIndentation, true);
					res.setContentType("text/plain");
				}
				p.append("mediaType", matchingAccept).append("characterEncoding", res.getCharacterEncoding());
				SerializerContext ctx = r.createContext(p, req.getJavaMethod());
				if (! r.isWriterSerializer()) {
					OutputStreamSerializer s2 = (OutputStreamSerializer)r;
					OutputStream os = res.getNegotiatedOutputStream();
					s2.serialize(output, os, ctx);
					os.close();
				} else {
					WriterSerializer s2 = (WriterSerializer)r;
					Writer w = res.getNegotiatedWriter();
					s2.serialize(output, w, ctx);
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
