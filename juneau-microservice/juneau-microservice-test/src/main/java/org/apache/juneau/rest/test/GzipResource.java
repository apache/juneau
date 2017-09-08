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
package org.apache.juneau.rest.test;

import java.io.*;

import org.apache.juneau.encoders.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
public class GzipResource {

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
		public String test1put(@Body String in) {
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
		public String test1put(@Body String in) {
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
