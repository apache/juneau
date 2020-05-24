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
package org.apache.juneau.rest.client2;

import static org.junit.Assert.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Body annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MarshallTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}

		public void check() {
			assertEquals(f, 1);
		}

		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	public static Bean bean = Bean.create();

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Bean postA01(@Body Bean b, @Header("Accept") String accept, @Header("Content-Type") String ct, @Header("X-Accept") String xaccept, @Header("X-Content-Type") String xct) {
			assertEquals("Accept doesn't match", nn(xaccept), nn(accept));
			assertEquals("Content-Type doesn't match", nn(xct), nn(ct));
			return b;
		}
	}

	private static String nn(String s) {
		return s == null ? "nil" : s;
	}


	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Single language support
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient a1a = MockRestClient.create(A.class).simpleJson().build();
	private static RestClient a1b = MockRestClient.create(A.class).json().build();
	private static RestClient a1c = MockRestClient.create(A.class).xml().build();
	private static RestClient a1d = MockRestClient.create(A.class).html().build();
	private static RestClient a1e = MockRestClient.create(A.class).plainText().build();
	private static RestClient a1f = MockRestClient.create(A.class).msgPack().build();
	private static RestClient a1g = MockRestClient.create(A.class).uon().build();
	private static RestClient a1h = MockRestClient.create(A.class).urlEnc().build();
	private static RestClient a1i = MockRestClient.create(A.class).openApi().build();

	@Test
	public void a01_singleLanguages() throws Exception {
		a1a.post("/a01", bean)
			.header("X-Accept", "application/json+simple")
			.header("X-Content-Type", "application/json+simple")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1b.post("/a01", bean)
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1c.post("/a01", bean)
			.header("X-Accept", "text/xml")
			.header("X-Content-Type", "text/xml")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1d.post("/a01", bean)
			.header("X-Accept", "text/html")
			.header("X-Content-Type", "text/html")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1e.post("/a01", bean)
			.header("X-Accept", "text/plain")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1f.post("/a01", bean)
			.header("X-Accept", "octal/msgpack")
			.header("X-Content-Type", "octal/msgpack")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1g.post("/a01", bean)
			.header("X-Accept", "text/uon")
			.header("X-Content-Type", "text/uon")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1h.post("/a01", bean)
			.header("X-Accept", "application/x-www-form-urlencoded")
			.header("X-Content-Type", "application/x-www-form-urlencoded")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		a1i.post("/a01", bean)
			.header("X-Accept", "text/openapi")
			.header("X-Content-Type", "text/openapi")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	@Test
	public void a02_singleLanguagesWithOverride() throws Exception {
		a1a.post("/a01", bean)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	private static RestClient a1j = MockRestClient.create(A.class).build();

	@Test
	public void a03_noLanguages() throws Exception {
		a1j.post("/a01", bean)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.body("{f:1}")
			.run()
			.assertStatusCode().equals(200)
			.assertBody().equals("{\"f\":1}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Multiple language support
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient b1 = MockRestClient
		.create(A.class)
		.simpleJson()
		.json()
		.xml()
		.html()
		.plainText()
		.msgPack()
		.uon()
		.urlEnc()
		.openApi()
		.build();

	@Test
	public void b01_multiLanguages() throws Exception {
		b1.post("/a01", bean)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "text/xml")
			.header("Content-Type", "text/xml")
			.header("X-Accept", "text/xml")
			.header("X-Content-Type", "text/xml")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "text/html")
			.header("Content-Type", "text/html")
			.header("X-Accept", "text/html")
			.header("X-Content-Type", "text/html")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "text/plain")
			.header("Content-Type", "text/plain")
			.header("X-Accept", "text/plain")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "octal/msgpack")
			.header("Content-Type", "octal/msgpack")
			.header("X-Accept", "octal/msgpack")
			.header("X-Content-Type", "octal/msgpack")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "text/uon")
			.header("Content-Type", "text/uon")
			.header("X-Accept", "text/uon")
			.header("X-Content-Type", "text/uon")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "application/x-www-form-urlencoded")
			.header("Content-Type", "application/x-www-form-urlencoded")
			.header("X-Accept", "application/x-www-form-urlencoded")
			.header("X-Content-Type", "application/x-www-form-urlencoded")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		b1.post("/a01", bean)
			.header("Accept", "text/openapi")
			.header("Content-Type", "text/openapi")
			.header("X-Accept", "text/openapi")
			.header("X-Content-Type", "text/openapi")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	@Test
	public void b02_multiLanguages_default() throws Exception {
		// Bean will be serialized using toString().
		b1.post("/a01", bean)
			.header("X-Accept", "nil")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Universal language support
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient c1 = MockRestClient
		.create(A.class)
		.universal()
		.build();

	@Test
	public void c01_universal() throws Exception {
		c1.post("/a01", bean)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "text/xml")
			.header("Content-Type", "text/xml")
			.header("X-Accept", "text/xml")
			.header("X-Content-Type", "text/xml")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "text/html")
			.header("Content-Type", "text/html")
			.header("X-Accept", "text/html")
			.header("X-Content-Type", "text/html")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "text/plain")
			.header("Content-Type", "text/plain")
			.header("X-Accept", "text/plain")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "octal/msgpack")
			.header("Content-Type", "octal/msgpack")
			.header("X-Accept", "octal/msgpack")
			.header("X-Content-Type", "octal/msgpack")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "text/uon")
			.header("Content-Type", "text/uon")
			.header("X-Accept", "text/uon")
			.header("X-Content-Type", "text/uon")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "application/x-www-form-urlencoded")
			.header("Content-Type", "application/x-www-form-urlencoded")
			.header("X-Accept", "application/x-www-form-urlencoded")
			.header("X-Content-Type", "application/x-www-form-urlencoded")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		c1.post("/a01", bean)
			.header("Accept", "text/openapi")
			.header("Content-Type", "text/openapi")
			.header("X-Accept", "text/openapi")
			.header("X-Content-Type", "text/openapi")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	@Test
	public void c02_universal_default() throws Exception {
		c1.post("/a01", bean)
			.header("X-Accept", "nil")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Universal language support with default headers
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient d1 = MockRestClient
		.create(A.class)
		.universal()
		.header("Accept", "application/json")
		.header("Content-Type", "application/json")
		.build();

	@Test
	public void d01_universal() throws Exception {
		d1.post("/a01", bean)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "text/xml")
			.header("Content-Type", "text/xml")
			.header("X-Accept", "text/xml")
			.header("X-Content-Type", "text/xml")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "text/html")
			.header("Content-Type", "text/html")
			.header("X-Accept", "text/html")
			.header("X-Content-Type", "text/html")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "text/plain")
			.header("Content-Type", "text/plain")
			.header("X-Accept", "text/plain")
			.header("X-Content-Type", "text/plain")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "octal/msgpack")
			.header("Content-Type", "octal/msgpack")
			.header("X-Accept", "octal/msgpack")
			.header("X-Content-Type", "octal/msgpack")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "text/uon")
			.header("Content-Type", "text/uon")
			.header("X-Accept", "text/uon")
			.header("X-Content-Type", "text/uon")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "application/x-www-form-urlencoded")
			.header("Content-Type", "application/x-www-form-urlencoded")
			.header("X-Accept", "application/x-www-form-urlencoded")
			.header("X-Content-Type", "application/x-www-form-urlencoded")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
		d1.post("/a01", bean)
			.header("Accept", "text/openapi")
			.header("Content-Type", "text/openapi")
			.header("X-Accept", "text/openapi")
			.header("X-Content-Type", "text/openapi")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}

	@Test
	public void d02_universal_default() throws Exception {
		d1.post("/a01", bean)
			.header("X-Accept", "application/json")
			.header("X-Content-Type", "application/json")
			.run()
			.assertStatusCode().equals(200)
			.getBody().as(Bean.class).check();
	}
}