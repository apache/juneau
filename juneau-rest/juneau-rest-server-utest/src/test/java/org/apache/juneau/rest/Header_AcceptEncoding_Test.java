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
package org.apache.juneau.rest;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.encoders.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Header_AcceptEncoding_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Test with no compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestMethod
		public String get() {
			return "foo";
		}
	}

	@Test
	public void a01_noCompression() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/").run().assertBody().is("foo");
		a.get("/").acceptEncoding("").run().assertBody().is("foo");
		a.get("/").acceptEncoding("*").run().assertBody().is("foo");
		a.get("/").acceptEncoding("identity").run().assertBody().is("foo");

		// The following should all match identity.
		a.get("/").acceptEncoding("mycoding").run().assertBody().is("foo");
		a.get("/").acceptEncoding("identity;q=0.8,mycoding;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("mycoding;q=0.8,identity;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("mycoding;q=0.8,*;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("*;q=0.8,myencoding;q=0.6").run().assertBody().is("foo");

		a.get("?noTrace=true").acceptEncoding("mycoding,identity;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("mycoding,*;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("identity;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("identity;q=0.0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("*;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("*;q=0.0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['identity']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(encoders=MyEncoder.class)
	public static class B {
		@RestMethod
		public String get() {
			return "foo";
		}
	}

	@Test
	public void b01_withCompression() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.get("/").run().assertBody().is("foo");
		b.get("/").acceptEncoding("").run().assertBody().is("foo");
		b.get("/").acceptEncoding("identity").run().assertBody().is("foo");
		b.get("/").acceptEncoding("identity;q=0.8,mycoding;q=0.6").run().assertBody().is("foo");

		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("*").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("mycoding").run().getBody().asBytes()));

		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("mycoding,identity;q=0").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("mycoding,*;q=0").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("mycoding;q=0.8,identity;q=0.6").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").acceptEncoding("mycoding;q=0.8,*;q=0.6").run().getBody().asBytes()));

		b.get("?noTrace=true").acceptEncoding("identity;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").acceptEncoding("identity;q=0.0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").acceptEncoding("*;q=0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").acceptEncoding("*;q=0.0").run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with compression enabled but with servlet using output stream directly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(encoders=MyEncoder.class)
	@SuppressWarnings("resource")
	public static class C {
		@RestMethod
		public void a(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getOutputStream() directly.
			res.setContentType("text/direct");
			OutputStream os = res.getOutputStream();
			os.write("foo".getBytes());
			os.flush();
		}
		@RestMethod
		public void b(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getWriter() directly.
			Writer w = res.getWriter();
			w.append("foo");
			w.flush();
		}
		@RestMethod
		public void c(RestResponse res) throws Exception {
			// This method uses getNegotiatedWriter() which should use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
			w.close();
		}
		@RestMethod(encoders={IdentityEncoder.class})
		public void d(RestResponse res) throws Exception {
			// This method overrides the set of encoders at the method level and so shouldn't use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
		}
	}

	@Test
	public void c01_direct() throws Exception {
		RestClient c = MockRestClient.build(C.class);

		c.get("/a")
			.acceptEncoding("mycoding")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertStringHeader("Content-Type").is("text/direct")
			.assertBody().is("foo");
		c.get("/a")
			.acceptEncoding("*")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertStringHeader("Content-Type").is("text/direct")
			.assertBody().is("foo");

		c.get("/b")
			.acceptEncoding("mycoding")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
		c.get("/b")
			.acceptEncoding("*")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");

		byte[] body;
		body = c.get("/c")
			.acceptEncoding("mycoding")
			.run()
			.assertStringHeader("Content-Encoding").is("mycoding")
			.getBody().asBytes();
		assertEquals("foo", StringUtils.decompress(body));
		body = c.get("/c")
			.acceptEncoding("*")
			.run()
			.assertStringHeader("Content-Encoding").is("mycoding")
			.getBody().asBytes();
		assertEquals("foo", StringUtils.decompress(body));

		c.get("/d")
			.acceptEncoding("mycoding")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
		c.get("/d")
			.acceptEncoding("*")
			.run()
			.assertStringHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static class MyEncoder extends GzipEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"mycoding"};
		}
	}
}
