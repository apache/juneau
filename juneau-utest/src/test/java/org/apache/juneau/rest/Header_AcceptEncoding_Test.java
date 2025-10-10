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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Header_AcceptEncoding_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test with no compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp
		public String get() {
			return "foo";
		}
	}

	@Test void a01_noCompression() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/").run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("*")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("identity")).run().assertContent("foo");

		// The following should all match identity.
		a.get("/").header(AcceptEncoding.of("mycoding")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("identity;q=0.8,mycoding;q=0.6")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("mycoding;q=0.8,identity;q=0.6")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("mycoding;q=0.8,*;q=0.6")).run().assertContent("foo");
		a.get("/").header(AcceptEncoding.of("*;q=0.8,myencoding;q=0.6")).run().assertContent("foo");

		a.get("?noTrace=true").header(AcceptEncoding.of("mycoding,identity;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").header(AcceptEncoding.of("mycoding,*;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").header(AcceptEncoding.of("identity;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").header(AcceptEncoding.of("identity;q=0.0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").header(AcceptEncoding.of("*;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").header(AcceptEncoding.of("*;q=0.0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['identity']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(encoders=MyEncoder.class)
	public static class B {
		@RestOp
		public String get() {
			return "foo";
		}
	}

	@Test void b01_withCompression() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/").run().assertContent("foo");
		b.get("/").header(AcceptEncoding.of("")).run().assertContent("foo");
		b.get("/").header(AcceptEncoding.of("identity")).run().assertContent("foo");
		b.get("/").header(AcceptEncoding.of("identity;q=0.8,mycoding;q=0.6")).run().assertContent("foo");

		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("*")).run().getContent().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("mycoding")).run().getContent().asBytes()));

		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("mycoding,identity;q=0")).run().getContent().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("mycoding,*;q=0")).run().getContent().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("mycoding;q=0.8,identity;q=0.6")).run().getContent().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/").header(AcceptEncoding.of("mycoding;q=0.8,*;q=0.6")).run().getContent().asBytes()));

		b.get("?noTrace=true").header(AcceptEncoding.of("identity;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").header(AcceptEncoding.of("identity;q=0.0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").header(AcceptEncoding.of("*;q=0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("?noTrace=true").header(AcceptEncoding.of("*;q=0.0")).run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with compression enabled but with servlet using output stream directly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(encoders=MyEncoder.class)
	public static class C {
		@RestGet
		public void a(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getOutputStream() directly.
			res.setContentType("text/direct");
			var os = res.getOutputStream();
			os.write("foo".getBytes());
			os.flush();
		}
		@RestGet
		public void b(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getWriter() directly.
			var w = res.getWriter();
			w.append("foo");
			w.flush();
		}
		@RestGet
		public void c(RestResponse res) throws Exception {
			// This method uses getNegotiatedWriter() which should use GZip encoding.
			var w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
			w.close();
		}
		@RestGet(encoders={IdentityEncoder.class})
		public void d(RestResponse res) throws Exception {
			// This method overrides the set of encoders at the method level and so shouldn't use GZip encoding.
			var w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
		}
	}

	@Test void c01_direct() throws Exception {
		var c = MockRestClient.build(C.class);

		c.get("/a")
			.header(AcceptEncoding.of("mycoding"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertHeader("Content-Type").is("text/direct")
			.assertContent("foo");
		c.get("/a")
			.header(AcceptEncoding.of("*"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertHeader("Content-Type").is("text/direct")
			.assertContent("foo");

		c.get("/b")
			.header(AcceptEncoding.of("mycoding"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertContent("foo");
		c.get("/b")
			.header(AcceptEncoding.of("*"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertContent("foo");

		byte[] body;
		body = c.get("/c")
			.header(AcceptEncoding.of("mycoding"))
			.run()
			.assertHeader("Content-Encoding").is("mycoding")
			.getContent().asBytes();
		assertEquals("foo", StringUtils.decompress(body));
		body = c.get("/c")
			.header(AcceptEncoding.of("*"))
			.run()
			.assertHeader("Content-Encoding").is("mycoding")
			.getContent().asBytes();
		assertEquals("foo", StringUtils.decompress(body));

		c.get("/d")
			.header(AcceptEncoding.of("mycoding"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertContent("foo");
		c.get("/d")
			.header(AcceptEncoding.of("*"))
			.run()
			.assertHeader("Content-Encoding").isNull() // Should not be set
			.assertContent("foo");
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
