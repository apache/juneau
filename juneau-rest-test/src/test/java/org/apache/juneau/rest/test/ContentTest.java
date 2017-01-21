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

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;

import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;

public class ContentTest {

	private static String URL = "/testContent";

	//====================================================================================================
	// Basic tests using @Body parameter
	//====================================================================================================
	@Test
	public void testUsingContentParam() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Body boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?body=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?body=(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?body=$b(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?body=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean?body=(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean?body=$b(false)", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?body=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Body Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?body=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?body=(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?body=$b(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?body=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?body=(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?body=$b(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?body=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Body int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?body=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int?body=(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int?body=$n(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?body=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Body Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?body=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?body=(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?body=$n(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?body=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Body float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?body=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float?body=(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float?body=$n(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?body=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Body Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?body=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?body=(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?body=$n(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?body=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Body TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?body=(a=b,c=d)", null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?body=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Body DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?body=" + UonSerializer.DEFAULT.serialize(b), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/B?body=" + UonSerializer.DEFAULT_SIMPLE.serialize(b), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Body DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?body=" + UonSerializer.DEFAULT.serialize(x), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/C?body=" + UonSerializer.DEFAULT_SIMPLE.serialize(x), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests using &Body parameter with &Accept=text/json
	//====================================================================================================
	@Test
	public void testUsingContentParamJsonHeader() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple").setHeader("Content-Type", "text/json");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Body boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?body=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?body=false", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?body=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Body Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?body=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?body=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?body=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Body int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?body=-123", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?body=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Body Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?body=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?body=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Body float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?body=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?body=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Body Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?body=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?body=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Body TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?body=" + encode("{a:'b',c:'d'}"), null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?body=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?body=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Body DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?body=" + encode(JsonSerializer.DEFAULT_LAX.serialize(b)), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Body DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?body=" + encode(JsonSerializer.DEFAULT_LAX.serialize(x)), null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests using &Body parameter with &Accept=text/json
	//====================================================================================================
	@Test
	public void testUsingContentParamJsonParam() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Body boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?body=true&Content-Type=text/json", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?body=false&Content-Type=text/json", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?body=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Body Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?body=true&Content-Type=text/json", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?body=false&Content-Type=text/json", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?body=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Body int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?body=-123&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?body=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Body Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?body=-123&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?body=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Body float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?body=-1.23&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?body=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Body Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?body=-1.23&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?body=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Body TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?body=" + encode("{a:'b',c:'d'}") + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?body=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?body=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Body DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?body=" + encode(JsonSerializer.DEFAULT_LAX.serialize(b)) + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Body DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?body=" + encode(JsonSerializer.DEFAULT_LAX.serialize(x)) + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests using HTTP body content
	//====================================================================================================
	@Test
	public void testUsingContent() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple").setHeader("Content-Type", "text/uon").setSerializer(PlainTextSerializer.class);
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Body boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean", "true").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean", "(true)").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean", "$b(true)").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean", "false").getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean", "(false)").getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean", "$b(false)").getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?noTrace=true", "%00").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Body Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean", "true").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean", "(true)").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean", "$b(true)").getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean", "false").getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean", "(false)").getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean", "$b(false)").getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean", "\u0000").getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Body int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int", "-123").getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int", "(-123)").getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int", "$n(-123)").getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?noTrace=true", "%00").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Body Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer", "-123").getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer", "(-123)").getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer", "$n(-123)").getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer", "\u0000").getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Body float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float", "-1.23").getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float", "(-1.23)").getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float", "$n(-1.23)").getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?noTrace=true", "\u0000").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Body Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float", "-1.23").getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float", "(-1.23)").getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float", "$n(-1.23)").getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float", "\u0000").getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Body TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map", "(a=b,c=d)").getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map", "\u0000").getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?noTrace=true", "bad").getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Body DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B", "" + UonSerializer.DEFAULT.serialize(b)).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/B", "" + UonSerializer.DEFAULT_SIMPLE.serialize(b)).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Body DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C", "" + UonSerializer.DEFAULT.serialize(x)).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/C", "" + UonSerializer.DEFAULT_SIMPLE.serialize(x)).getResponseAsString();
		assertEquals("{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}


	private String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
