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
	path="/testGroups"
)
public class TestGroups extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Produces({"text/s1","text/s2"})
	public static class SSerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object output, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write("text/s," + output);
		}
	}

	@Consumes({"text/p1","text/p2"})
	public static class PParser extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)IOUtils.read(in);
		}
	}


	@Override /* RestServlet */
	public SerializerGroup createSerializers() throws Exception {
		return new SerializerGroup().append(SSerializer.class);
	}

	@Override /* RestServlet */
	public ParserGroup createParsers() throws Exception {
		return new ParserGroup().append(PParser.class);
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_get() {
		return "GET";
	}

	@RestMethod(name="PUT", path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_put(@Content String in) {
		return in;
	}
}
