/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import static com.ibm.juno.server.annotation.Inherit.*;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testDefaultContentTypes",
	defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
	parsers={TestDefaultContentTypes.P1.class,TestDefaultContentTypes.P2.class}, serializers={TestDefaultContentTypes.S1.class,TestDefaultContentTypes.S2.class}
)
@SuppressWarnings("synthetic-access")
public class TestDefaultContentTypes extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Consumes("text/p1")
	public static class P1 extends DummyParser { public P1() {super("p1");}}

	@Consumes("text/p2")
	public static class P2 extends DummyParser { public P2() {super("p2");}}

	@Consumes("text/p3")
	public static class P3 extends DummyParser { public P3() {super("p3");}}

	@Produces("text/s1")
	public static class S1 extends DummySerializer { public S1() {super("s1");}}

	@Produces("text/s2")
	public static class S2 extends DummySerializer { public S2() {super("s2");}}

	@Produces("text/s3")
	public static class S3 extends DummySerializer { public S3() {super("s3");}}

	/**
	 * Test that default Accept and Content-Type headers on servlet annotation are picked up.
	 */
	@RestMethod(name="PUT", path="/testDefaultHeadersOnServletAnnotation")
	public String testDefaultHeadersOnServletAnnotation(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodParsersSerializers", parsers=P3.class, serializers=S3.class)
	public String testRestMethodParsersSerializers(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodAddParsersSerializers", parsers=P3.class, parsersInherit=PARSERS, serializers=S3.class, serializersInherit=SERIALIZERS)
	public String testRestMethodAddParsersSerializers(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Various Accept incantations.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testAccept")
	public String testAccept(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodParserSerializerAnnotations", defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, serializers=S3.class)
	public String testRestMethodParserSerializerAnnotations(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodAddParsersSerializersAnnotations", defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, parsersInherit=PARSERS, serializers=S3.class, serializersInherit=SERIALIZERS)
	public String testRestMethodAddParsersSerializersAnnotations(@Content String in) {
		return in;
	}

	public static class DummyParser extends ReaderParser {
		private String name;
		private DummyParser(String name) {
			this.name = name;
		}
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)name;
		}
	}

	public static class DummySerializer extends WriterSerializer {
		private String name;
		private DummySerializer(String name) {
			this.name = name;
		}
		@Override /* Serializer */
		protected void doSerialize(Object output, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			out.write(name + "/" + output);
		}
	}
}
