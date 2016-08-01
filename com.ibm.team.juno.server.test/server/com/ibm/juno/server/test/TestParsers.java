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
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates correct parser is used.
 */
@RestResource(
	path="/testParsers",
	parsers=TestParsers.TestParserA.class,
	serializers=PlainTextSerializer.class
)
public class TestParsers extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@Consumes("text/a")
	public static class TestParserA extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)("text/a - " + IOUtils.read(in).trim());
		}
	}

	//====================================================================================================
	// Parser defined on class.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOnClass")
	public String testParserOnClass(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Parser defined on method.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOnMethod", parsers=TestParserB.class)
	public String testParserOnMethod(@Content String in) {
		return in;
	}

	@Consumes("text/b")
	public static class TestParserB extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader r, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)("text/b - " + IOUtils.read(r).trim());
		}
	}

	//====================================================================================================
	// Parser overridden on method.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOverriddenOnMethod", parsers={TestParserB.class,TestParserC.class}, parsersInherit=PARSERS)
	public String testParserOverriddenOnMethod(@Content String in) {
		return in;
	}

	@Consumes("text/c")
	public static class TestParserC extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader r, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)("text/c - " + IOUtils.read(r).trim());
		}
	}

	//====================================================================================================
	// Parser with different Accept than Content-Type.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserWithDifferentMediaTypes", parsers={TestParserD.class}, parsersInherit=PARSERS)
	public String testParserWithDifferentMediaTypes(@Content String in) {
		return in;
	}

	@Consumes({"text/a","text/d"})
	public static class TestParserD extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader r, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			return (T)("text/d - " + IOUtils.read(r).trim());
		}
	}

	//====================================================================================================
	// Check for valid error response.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testValidErrorResponse")
	public String testValidErrorResponse(@Content String in) {
		return in;
	}
}
