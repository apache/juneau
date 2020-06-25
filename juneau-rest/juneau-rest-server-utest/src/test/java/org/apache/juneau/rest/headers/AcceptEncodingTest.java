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
package org.apache.juneau.rest.headers;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.encoders.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AcceptEncodingTest {

	//=================================================================================================================
	// Setup classes
	//=================================================================================================================

	public static class MyEncoder extends GzipEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"mycoding"};
		}
	}

	//=================================================================================================================
	// Test with no compression enabled.
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public String get() {
			return "foo";
		}
	}
	static MockRestClient a = MockRestClient.buildLax(A.class);

	@Test
	public void a01_noCompression() throws Exception {
		a.get("/").run().assertBody().is("foo");
		a.get("/").acceptEncoding("").run().assertBody().is("foo");
		a.get("/").acceptEncoding("*").run().assertBody().is("foo");
		a.get("/").acceptEncoding("identity").run().assertBody().is("foo");
	}

	@Test
	public void a02_noCompression_qValues() throws Exception {
		// The following should all match identity.
		a.get("/").acceptEncoding("mycoding").run().assertBody().is("foo");
		a.get("/").acceptEncoding("identity;q=0.8,mycoding;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("mycoding;q=0.8,identity;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("mycoding;q=0.8,*;q=0.6").run().assertBody().is("foo");
		a.get("/").acceptEncoding("*;q=0.8,myencoding;q=0.6").run().assertBody().is("foo");
	}

	@Test
	public void a03_noCompression_nomatch() throws Exception {
		a.get("?noTrace=true").acceptEncoding("mycoding,identity;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("mycoding,*;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("identity;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("identity;q=0.0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("*;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['identity']"
			);
		a.get("?noTrace=true").acceptEncoding("*;q=0.0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['identity']"
			);
	}

	//=================================================================================================================
	// Test with compression enabled.
	//=================================================================================================================

	@Rest(encoders=MyEncoder.class)
	public static class B {
		@RestMethod
		public String b01() {
			return "foo";
		}
	}
	static MockRestClient b = MockRestClient.buildLax(B.class);

	@Test
	public void b01_withCompression_identity() throws Exception {
		b.get("/b01").run().assertBody().is("foo");
		b.get("/b01").acceptEncoding("").run().assertBody().is("foo");
		b.get("/b01").acceptEncoding("identity").run().assertBody().is("foo");
	}
	@Test
	public void b02_withCompression_identity_qValues() throws Exception {
		b.get("/b01").acceptEncoding("identity;q=0.8,mycoding;q=0.6").run().assertBody().is("foo");
	}
	@Test
	public void b03_withCompression_gzip() throws Exception {
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("*").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("mycoding").run().getBody().asBytes()));
	}
	@Test
	public void b04_withCompression_gzip_qValues() throws Exception {
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("mycoding,identity;q=0").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("mycoding,*;q=0").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("mycoding;q=0.8,identity;q=0.6").run().getBody().asBytes()));
		assertEquals("foo", StringUtils.decompress(b.get("/b01").acceptEncoding("mycoding;q=0.8,*;q=0.6").run().getBody().asBytes()));
	}
	@Test
	public void b05_withCompression_nomatch() throws Exception {
		b.get("/b01?noTrace=true").acceptEncoding("identity;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("/b01?noTrace=true").acceptEncoding("identity;q=0.0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("/b01?noTrace=true").acceptEncoding("*;q=0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.get("/b01?noTrace=true").acceptEncoding("*;q=0.0").run()
			.assertStatus().is(406)
			.assertBody().contains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
	}


	//=================================================================================================================
	// Test with compression enabled but with servlet using output stream directly.
	//=================================================================================================================

	@Rest(encoders=MyEncoder.class)
	@SuppressWarnings("resource")
	public static class C {
		@RestMethod
		public void c01(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getOutputStream() directly.
			res.setContentType("text/direct");
			OutputStream os = res.getOutputStream();
			os.write("foo".getBytes());
			os.flush();
		}
		@RestMethod
		public void c02(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getWriter() directly.
			Writer w = res.getWriter();
			w.append("foo");
			w.flush();
		}
		@RestMethod
		public void c03(RestResponse res) throws Exception {
			// This method uses getNegotiatedWriter() which should use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
			w.close();
		}
		@RestMethod(encoders={IdentityEncoder.class})
		public void c04(RestResponse res) throws Exception {
			// This method overrides the set of encoders at the method level and so shouldn't use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
		}
	}
	static MockRestClient c = MockRestClient.build(C.class);

	@Test
	public void c01_direct1() throws Exception {
		c.get("/c01")
			.acceptEncoding("mycoding")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertHeader("Content-Type").is("text/direct")
			.assertBody().is("foo");
		c.get("/c01")
			.acceptEncoding("*")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertHeader("Content-Type").is("text/direct")
			.assertBody().is("foo");
	}
	@Test
	public void c02_direct2() throws Exception {
		c.get("/c02")
			.acceptEncoding("mycoding")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
		c.get("/c02")
			.acceptEncoding("*")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
	}
	@Test
	public void c03_direct3() throws Exception {
		byte[] body;
		body = c.get("/c03")
			.acceptEncoding("mycoding")
			.run()
			.assertHeader("Content-Encoding").is("mycoding")
			.getBody().asBytes();
		assertEquals("foo", StringUtils.decompress(body));
		body = c.get("/c03")
			.acceptEncoding("*")
			.run()
			.assertHeader("Content-Encoding").is("mycoding")
			.getBody().asBytes();
		assertEquals("foo", StringUtils.decompress(body));
	}
	@Test
	public void c04_direct4() throws Exception {
		c.get("/c04")
			.acceptEncoding("mycoding")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
		c.get("/c04")
			.acceptEncoding("*")
			.run()
			.assertHeader("Content-Encoding").doesNotExist() // Should not be set
			.assertBody().is("foo");
	}
}
