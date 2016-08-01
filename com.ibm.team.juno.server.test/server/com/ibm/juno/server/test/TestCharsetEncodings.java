/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testCharsetEncodings",
	defaultRequestHeaders={"Accept: text/s", "Content-Type: text/p"},
	parsers={TestCharsetEncodings.CtParser.class}, serializers={TestCharsetEncodings.ASerializer.class}
)
public class TestCharsetEncodings extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Consumes("text/p")
	public static class CtParser extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
			return (T)IOUtils.read(in);
		}
	}

	@Produces("text/s")
	public static class ASerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write(o.toString());
		}
	}

	@RestMethod(name="PUT", path="/")
	public String test1(RestRequest req, @Content String in) {
		return req.getCharacterEncoding() + "/" + in + "/" + req.getCharacterEncoding();
	}
}
