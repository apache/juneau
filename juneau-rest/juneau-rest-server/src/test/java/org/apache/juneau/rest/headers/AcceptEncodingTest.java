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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.encoders.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Test behavior involving Accept-Encoding header.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
	
	@RestResource
	public static class A {
		@RestMethod(name=GET)
		public String get() {
			return "foo";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_noCompression() throws Exception {
		a.request("GET", "/").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("*").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("identity").execute().assertBody("foo");
	}

	@Test
	public void a02_noCompression_qValues() throws Exception {
		// The following should all match identity.
		a.request("GET", "/").acceptEncoding("mycoding").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("identity;q=0.8,mycoding;q=0.6").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("mycoding;q=0.8,identity;q=0.6").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("mycoding;q=0.8,*;q=0.6").execute().assertBody("foo");
		a.request("GET", "/").acceptEncoding("*;q=0.8,myencoding;q=0.6").execute().assertBody("foo");
	}
	
	@Test
	public void a03_noCompression_nomatch() throws Exception {
		a.request("GET", "?noTrace=true").acceptEncoding("mycoding,identity;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.request("GET", "?noTrace=true").acceptEncoding("mycoding,*;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: ['identity']"
			);
		a.request("GET", "?noTrace=true").acceptEncoding("identity;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['identity']"
			);
		a.request("GET", "?noTrace=true").acceptEncoding("identity;q=0.0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['identity']"
			);
		a.request("GET", "?noTrace=true").acceptEncoding("*;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['identity']"
			);
		a.request("GET", "?noTrace=true").acceptEncoding("*;q=0.0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['identity']"
			);
	}

	//=================================================================================================================
	// Test with compression enabled.
	//=================================================================================================================
	
	@RestResource(encoders=MyEncoder.class)
	public static class B {
		@RestMethod(name=GET)
		public String test1() {
			return "foo";
		}
	}
	static MockRest b = MockRest.create(B.class);
	
	@Test
	public void b01_withCompression_identity() throws Exception {
		b.request("GET", "/").execute().assertBody("foo");
		b.request("GET", "/").acceptEncoding("").execute().assertBody("foo");
		b.request("GET", "/").acceptEncoding("identity").execute().assertBody("foo");
	}
	@Test
	public void b02_withCompression_identity_qValues() throws Exception {
		b.request("GET", "/").acceptEncoding("identity;q=0.8,mycoding;q=0.6").execute().assertBody("foo");
	}
	@Test
	public void b03_withCompression_gzip() throws Exception {
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("*").execute().getBody()));
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("mycoding").execute().getBody()));
	}
	@Test
	public void b04_withCompression_gzip_qValues() throws Exception {
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("mycoding,identity;q=0").execute().getBody()));
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("mycoding,*;q=0").execute().getBody()));
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("mycoding;q=0.8,identity;q=0.6").execute().getBody()));
		assertEquals("foo", decompress(b.request("GET", "/").acceptEncoding("mycoding;q=0.8,*;q=0.6").execute().getBody()));
	}
	@Test
	public void b05_withCompression_nomatch() throws Exception {
		b.request("GET", "?noTrace=true").acceptEncoding("identity;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.request("GET", "?noTrace=true").acceptEncoding("identity;q=0.0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.request("GET", "?noTrace=true").acceptEncoding("*;q=0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		b.request("GET", "?noTrace=true").acceptEncoding("*;q=0.0").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
	}


	//=================================================================================================================
	// Test with compression enabled but with servlet using output stream directly.
	//=================================================================================================================

	@RestResource(encoders=MyEncoder.class)
	@SuppressWarnings("resource")
	public static class C {
		@RestMethod(name=GET, path="/direct1")
		public void c01(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getOutputStream() directly.
			res.setContentType("text/direct");
			OutputStream os = res.getOutputStream();
			os.write("foo".getBytes());
			os.flush();
		}
		@RestMethod(name=GET, path="/direct2")
		public void c02(RestResponse res) throws Exception {
			// This method bypasses the content type and encoding from
			// the serializers and encoders when calling getWriter() directly.
			Writer w = res.getWriter();
			w.append("foo");
			w.flush();
		}
		@RestMethod(name=GET, path="/direct3")
		public void c03(RestResponse res) throws Exception {
			// This method uses getNegotiatedWriter() which should use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
			w.close();
		}
		@RestMethod(name=GET, path="/direct4", encoders={IdentityEncoder.class})
		public void c04(RestResponse res) throws Exception {
			// This method overrides the set of encoders at the method level and so shouldn't use GZip encoding.
			Writer w = res.getNegotiatedWriter();
			w.append("foo");
			w.flush();
		}
	}
	static MockRest c = MockRest.create(C.class);
	
	@Test
	public void c01_direct1() throws Exception {
		c.request("GET", "/direct1").acceptEncoding("mycoding").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertHeader("Content-Type", "text/direct")
			.assertBody("foo");
		c.request("GET", "/direct1").acceptEncoding("*").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertHeader("Content-Type", "text/direct")
			.assertBody("foo");
	}	
	@Test
	public void c02_direct2() throws Exception {
		c.request("GET", "/direct2").acceptEncoding("mycoding").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertBody("foo");
		c.request("GET", "/direct2").acceptEncoding("*").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertBody("foo");
	}	
	@Test
	public void c03_direct3() throws Exception {
		byte[] body;
		body = c.request("GET", "/direct3").acceptEncoding("mycoding").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.getBody();
		assertEquals("foo", decompress(body));
		body = c.request("GET", "/direct3").acceptEncoding("*").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.getBody();
		assertEquals("foo", decompress(body));
	}	
	@Test
	public void c04_direct4() throws Exception {
		c.request("GET", "/direct4").acceptEncoding("mycoding").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertBody("foo");
		c.request("GET", "/direct4").acceptEncoding("*").execute()
			.assertHeader("Content-Encoding", null) // Should not be set
			.assertBody("foo");
	}	
}
