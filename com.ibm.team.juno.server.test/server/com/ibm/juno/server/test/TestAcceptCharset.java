/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static com.ibm.juno.server.RestServletProperties.*;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testAcceptCharset",
	serializers={PlainTextSerializer.class},
	properties={
		// Some versions of Jetty default to ISO8601, so specify UTF-8 for test consistency.
		@Property(name=REST_defaultCharset,value="utf-8")
	}
)
public class TestAcceptCharset extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test that Q-values are being resolved correctly.
	//====================================================================================================
	@RestMethod(name="GET", path="/testQValues")
	public String testQValues() {
		return "foo";
	}

	//====================================================================================================
	// Validate various Accept-Charset variations.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testCharsetOnResponse", parsers=TestParser.class, serializers=TestSerializer.class)
	public String testCharsetOnResponse(@Content String in) {
		return in;
	}

	@Consumes("text/plain")
	public static class TestParser extends InputStreamParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(InputStream in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
			return (T)ctx.getProperties().getString("characterEncoding");
		}
	}

	@Produces("text/plain")
	public static class TestSerializer extends OutputStreamSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, OutputStream out, SerializerContext ctx) throws IOException, SerializeException {
			Writer w = new OutputStreamWriter(out);
			w.append(o.toString()).append('/').append(ctx.getProperties().getString("characterEncoding"));
			w.flush();
			w.close();
		}
	}
}
