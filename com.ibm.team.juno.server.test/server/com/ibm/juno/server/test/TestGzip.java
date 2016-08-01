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

import com.ibm.juno.core.encoders.*;
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
public class TestGzip {

	//================================================================================
	// Encoder for "myencoding" encoding
	//================================================================================
	public static class MyEncoder extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"mycoding"};
		}
	}

	//====================================================================================================
	// Test with no compression enabled.
	//====================================================================================================
	@RestResource(
		path="/testGzipOff",
		serializers=PlainTextSerializer.class,
		parsers=PlainTextParser.class
	)
	public static class TestGzipOff extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestMethod(name="GET", path="/")
		public String test1get() {
			return "foo";
		}
		@RestMethod(name="PUT", path="/")
		public String test1put(@Content String in) {
			return in;
		}
	}

	//====================================================================================================
	// Test with compression enabled.
	//====================================================================================================
	@RestResource(
		path="/testGzipOn",
		serializers=PlainTextSerializer.class,
		parsers=PlainTextParser.class,
		encoders=MyEncoder.class
	)
	public static class TestGzipOn extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestMethod(name="GET", path="/")
		public String test1() {
			return "foo";
		}
		@RestMethod(name="PUT", path="/")
		public String test1put(@Content String in) {
			return in;
		}
		// This method bypasses the content type and encoding from
		// the serializers and encoders when calling getOutputStream() directly.
		@RestMethod(name="GET", path="/direct")
		public void direct(RestResponse res) throws Exception {
			res.setContentType("text/direct");
			OutputStream os = res.getOutputStream();
			os.write("test".getBytes());
			os.flush();
		}

		// This method bypasses the content type and encoding from
		// the serializers and encoders when calling getWriter() directly.
		@RestMethod(name="GET", path="/direct2")
		public void direct2(RestResponse res) throws Exception {
			Writer w = res.getWriter();
			w.append("test");
			w.flush();
		}

		// This method uses getNegotiatedWriter() which should use GZip encoding.
		@RestMethod(name="GET", path="/direct3")
		public void direct3(RestResponse res) throws Exception {
			Writer w = res.getNegotiatedWriter();
			w.append("test");
			w.flush();
		}

		// This method overrides the set of encoders at the method level and so shouldn't use GZip encoding.
		@RestMethod(name="GET", path="/direct4", inheritEncoders=false)
		public void direct4(RestResponse res) throws Exception {
			Writer w = res.getNegotiatedWriter();
			w.append("test");
			w.flush();
		}
	}
}
