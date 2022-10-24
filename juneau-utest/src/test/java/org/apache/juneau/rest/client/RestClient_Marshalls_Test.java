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
package org.apache.juneau.rest.client;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.header.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Marshalls_Test {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}

		public void check() {
			assertEquals(f,1);
		}

		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	public static Bean bean = Bean.create();

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Bean a01(@Content Bean b, @Header("Accept") String accept, @Header("Content-Type") String ct, @Header("X-Accept") String xaccept, @Header("X-Content-Type") String xct) {
			assertEquals("Accept doesn't match",nn(xaccept),nn(accept));
			assertEquals("Content-Type doesn't match",nn(xct),nn(ct));
			return b;
		}
	}

	private static String nn(String s) {
		return s == null ? "nil" : s;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Single language support
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_singleLanguages() throws Exception {
		RestClient x1 = client().json5().build();
		RestClient x2 = client().json().build();
		RestClient x3 = client().xml().build();
		RestClient x4 = client().html().build();
		RestClient x5 = client().plainText().build();
		RestClient x6 = client().msgPack().build();
		RestClient x7 = client().uon().build();
		RestClient x8 = client().urlEnc().build();
		RestClient x9 = client().openApi().build();
		RestClient x10 = client().htmlDoc().build();
		RestClient x11 = client().htmlStrippedDoc().build();
		x1.post("/a01",bean).header("X-Accept","application/json5").header("X-Content-Type","application/json5").run().assertStatus(200).getContent().as(Bean.class).check();
		x2.post("/a01",bean).header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
		x3.post("/a01",bean).header("X-Accept","text/xml").header("X-Content-Type","text/xml").run().assertStatus(200).getContent().as(Bean.class).check();
		x4.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html").run().assertStatus(200).getContent().as(Bean.class).check();
		x5.post("/a01",bean).header("X-Accept","text/plain").header("X-Content-Type","text/plain").run().assertStatus(200).getContent().as(Bean.class).check();
		x6.post("/a01",bean).header("X-Accept","octal/msgpack").header("X-Content-Type","octal/msgpack").run().assertStatus(200).getContent().as(Bean.class).check();
		x7.post("/a01",bean).header("X-Accept","text/uon").header("X-Content-Type","text/uon").run().assertStatus(200).getContent().as(Bean.class).check();
		x8.post("/a01",bean).header("X-Accept","application/x-www-form-urlencoded").header("X-Content-Type","application/x-www-form-urlencoded").run().assertStatus(200).getContent().as(Bean.class).check();
		x9.post("/a01",bean).header("X-Accept","text/openapi").header("X-Content-Type","text/openapi").run().assertStatus(200).getContent().as(Bean.class).check();
		x10.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html").run().assertStatus(200).getContent().as(Bean.class).check();
		x11.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html+stripped").run().assertStatus(200).getContent().as(Bean.class).check();

		// With override
		x1.post("/a01",bean).header("Accept","application/json").header("Content-Type","application/json").header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
	}

	@Test
	public void a02_singleLanguages_perRequest() throws Exception {
		RestClient x = client().build();
		x.post("/a01",bean).header("X-Accept","application/json5").header("X-Content-Type","application/json5").json5().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","application/json").header("X-Content-Type","application/json").json().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/xml").header("X-Content-Type","text/xml").xml().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html").html().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/plain").header("X-Content-Type","text/plain").plainText().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","octal/msgpack").header("X-Content-Type","octal/msgpack").msgPack().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/uon").header("X-Content-Type","text/uon").uon().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","application/x-www-form-urlencoded").header("X-Content-Type","application/x-www-form-urlencoded").urlEnc().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/openapi").header("X-Content-Type","text/openapi").openApi().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html").htmlDoc().run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("X-Accept","text/html").header("X-Content-Type","text/html+stripped").htmlStrippedDoc().run().assertStatus(200).getContent().as(Bean.class).check();
	}

	@Test
	public void a03_noLanguages() throws Exception {
		RestClient x = client().build();
		x.post("/a01",bean).header("Accept","application/json").header("Content-Type","application/json").header("X-Accept","application/json").header("X-Content-Type","application/json").content("{f:1}").run().assertStatus(200).assertContent("{\"f\":1}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Multiple language support
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_multiLanguages() throws Exception {
		RestClient x = client().json5().json().xml().html().plainText().msgPack().uon().urlEnc().openApi().build();
		x.post("/a01",bean).header("Accept","application/json").header("Content-Type","application/json").header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/xml").header("Content-Type","text/xml").header("X-Accept","text/xml").header("X-Content-Type","text/xml").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/html").header("Content-Type","text/html").header("X-Accept","text/html").header("X-Content-Type","text/html").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/plain").header("Content-Type","text/plain").header("X-Accept","text/plain").header("X-Content-Type","text/plain").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","octal/msgpack").header("Content-Type","octal/msgpack").header("X-Accept","octal/msgpack").header("X-Content-Type","octal/msgpack").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/uon").header("Content-Type","text/uon").header("X-Accept","text/uon").header("X-Content-Type","text/uon").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","application/x-www-form-urlencoded").header("Content-Type","application/x-www-form-urlencoded").header("X-Accept","application/x-www-form-urlencoded").header("X-Content-Type","application/x-www-form-urlencoded").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/openapi").header("Content-Type","text/openapi").header("X-Accept","text/openapi").header("X-Content-Type","text/openapi").run().assertStatus(200).getContent().as(Bean.class).check();

		assertThrown(()->x.post("/a01",bean).run()).asMessage().is("Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Universal language support
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_universal() throws Exception {
		RestClient x = client().universal().build();
		x.post("/a01",bean).header("Accept","application/json").header("Content-Type","application/json").header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/xml").header("Content-Type","text/xml").header("X-Accept","text/xml").header("X-Content-Type","text/xml").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/html").header("Content-Type","text/html").header("X-Accept","text/html").header("X-Content-Type","text/html").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/plain").header("Content-Type","text/plain").header("X-Accept","text/plain").header("X-Content-Type","text/plain").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","octal/msgpack").header("Content-Type","octal/msgpack").header("X-Accept","octal/msgpack").header("X-Content-Type","octal/msgpack").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/uon").header("Content-Type","text/uon").header("X-Accept","text/uon").header("X-Content-Type","text/uon").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","application/x-www-form-urlencoded").header("Content-Type","application/x-www-form-urlencoded").header("X-Accept","application/x-www-form-urlencoded").header("X-Content-Type","application/x-www-form-urlencoded").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/openapi").header("Content-Type","text/openapi").header("X-Accept","text/openapi").header("X-Content-Type","text/openapi").run().assertStatus(200).getContent().as(Bean.class).check();

		assertThrown(()->x.post("/a01",bean).run()).asMessage().is("Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Universal language support with default headers
	//------------------------------------------------------------------------------------------------------------------


	@Test
	public void d01_universal() throws Exception {
		RestClient x = client().universal().headersDefault(Accept.APPLICATION_JSON, ContentType.APPLICATION_JSON).build();
		x.post("/a01",bean).header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","application/json").header("Content-Type","application/json").header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/xml").header("Content-Type","text/xml").header("X-Accept","text/xml").header("X-Content-Type","text/xml").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/html").header("Content-Type","text/html").header("X-Accept","text/html").header("X-Content-Type","text/html").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/plain").header("Content-Type","text/plain").header("X-Accept","text/plain").header("X-Content-Type","text/plain").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","octal/msgpack").header("Content-Type","octal/msgpack").header("X-Accept","octal/msgpack").header("X-Content-Type","octal/msgpack").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/uon").header("Content-Type","text/uon").header("X-Accept","text/uon").header("X-Content-Type","text/uon").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","application/x-www-form-urlencoded").header("Content-Type","application/x-www-form-urlencoded").header("X-Accept","application/x-www-form-urlencoded").header("X-Content-Type","application/x-www-form-urlencoded").run().assertStatus(200).getContent().as(Bean.class).check();
		x.post("/a01",bean).header("Accept","text/openapi").header("Content-Type","text/openapi").header("X-Accept","text/openapi").header("X-Content-Type","text/openapi").run().assertStatus(200).getContent().as(Bean.class).check();

		// Default
		x.post("/a01",bean).header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
	}

	@Test
	public void d03_nullMarshalls() throws Exception {
		RestClient x = client().marshaller(null).marshallers(Json.DEFAULT,null).build();
		x.post("/a01",bean).header("X-Accept","application/json").header("X-Content-Type","application/json").run().assertStatus(200).getContent().as(Bean.class).check();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}