/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;

public class TestContentTest {

	private static String URL = "/testContent";

	//====================================================================================================
	// Basic tests using &Content parameter
	//====================================================================================================
	@Test
	public void testUsingContentParam() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Content boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?content=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?content=(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?content=$b(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?content=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean?content=(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/boolean?content=$b(false)", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?content=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Content Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?content=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?content=(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?content=$b(true)", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?content=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?content=(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?content=$b(false)", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?content=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Content int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?content=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int?content=(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/int?content=$n(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?content=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Content Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?content=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?content=(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?content=$n(-123)", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?content=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Content float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?content=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float?content=(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/float?content=$n(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?content=%00&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Content Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?content=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?content=(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?content=$n(-1.23)", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?content=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Content TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?content=(a=b,c=d)", null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?content=%00", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Content DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?content=" + UonSerializer.DEFAULT.serialize(b), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/B?content=" + UonSerializer.DEFAULT_SIMPLE.serialize(b), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Content DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?content=" + UonSerializer.DEFAULT.serialize(x), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/C?content=" + UonSerializer.DEFAULT_SIMPLE.serialize(x), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests using &Content parameter with &Accept=text/json
	//====================================================================================================
	@Test
	public void testUsingContentParamJsonHeader() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple").setHeader("Content-Type", "text/json");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Content boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?content=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?content=false", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?content=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Content Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?content=true", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?content=false", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?content=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Content int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?content=-123", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?content=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Content Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?content=-123", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?content=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Content float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?content=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?content=null&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Content Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?content=-1.23", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?content=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Content TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?content=" + encode("{a:'b',c:'d'}"), null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?content=null", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?content=bad&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Content DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?content=" + encode(JsonSerializer.DEFAULT_LAX.serialize(b)), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Content DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?content=" + encode(JsonSerializer.DEFAULT_LAX.serialize(x)), null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests using &Content parameter with &Accept=text/json
	//====================================================================================================
	@Test
	public void testUsingContentParamJsonParam() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple");
		String r;

		//	@RestMethod(name="POST", path="/boolean")
		//	public boolean testBool(@Content boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/boolean?content=true&Content-Type=text/json", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/boolean?content=false&Content-Type=text/json", null).getResponseAsString();
		assertEquals("false", r);
		try {
			r = c.doPost(URL + "/boolean?content=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/boolean?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}


		//	@RestMethod(name="POST", path="/Boolean")
		//	public Boolean testBoolean(@Content Boolean b) {
		//		return b;
		//	}
		r = c.doPost(URL + "/Boolean?content=true&Content-Type=text/json", null).getResponseAsString();
		assertEquals("true", r);
		r = c.doPost(URL + "/Boolean?content=false&Content-Type=text/json", null).getResponseAsString();
		assertEquals("false", r);
		r = c.doPost(URL + "/Boolean?content=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Boolean?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/int")
		//	public int testInt(@Content int i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/int?content=-123&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-123", r);
		try {
			r = c.doPost(URL + "/int?content=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/int?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Integer")
		//	public Integer testInteger(@Content Integer i) {
		//		return i;
		//	}
		r = c.doPost(URL + "/Integer?content=-123&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-123", r);
		r = c.doPost(URL + "/Integer?content=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Integer?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/float")
		//	public float testFloat(@Content float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/float?content=-1.23&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-1.23", r);
		try {
			r = c.doPost(URL + "/float?content=null&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}
		try {
			r = c.doPost(URL + "/float?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Float")
		//	public Float testFloat2(@Content Float f) {
		//		return f;
		//	}
		r = c.doPost(URL + "/Float?content=-1.23&Content-Type=text/json", null).getResponseAsString();
		assertEquals("-1.23", r);
		r = c.doPost(URL + "/Float?content=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Float?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/Map")
		//	public TreeMap<String,String> testMap(@Content TreeMap<String,String> m) {
		//		return m;
		//	}
		r = c.doPost(URL + "/Map?content=" + encode("{a:'b',c:'d'}") + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{a:'b',c:'d'}", r);
		r = c.doPost(URL + "/Map?content=null&Content-Type=text/json", null).getResponseAsString();
		assertEquals("null", r);
		try {
			r = c.doPost(URL + "/Map?content=bad&Content-Type=text/json&noTrace=true", null).getResponseAsString();
			fail("Exception expected!");
		} catch (RestCallException e) {
			assertEquals(400, e.getResponseCode());
		}

		//	@RestMethod(name="POST", path="/B")
		//	public DTO2s.B testPojo1(@Content DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B?content=" + encode(JsonSerializer.DEFAULT_LAX.serialize(b)) + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Content DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C?content=" + encode(JsonSerializer.DEFAULT_LAX.serialize(x)) + "&Content-Type=text/json", null).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

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
		//	public boolean testBool(@Content boolean b) {
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
		//	public Boolean testBoolean(@Content Boolean b) {
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
		//	public int testInt(@Content int i) {
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
		//	public Integer testInteger(@Content Integer i) {
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
		//	public float testFloat(@Content float f) {
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
		//	public Float testFloat2(@Content Float f) {
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
		//	public TreeMap<String,String> testMap(@Content TreeMap<String,String> m) {
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
		//	public DTO2s.B testPojo1(@Content DTO2s.B b) {
		//		return b;
		//	}
		DTOs.B b = DTOs.B.create();
		r = c.doPost(URL + "/B", "" + UonSerializer.DEFAULT.serialize(b)).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/B", "" + UonSerializer.DEFAULT_SIMPLE.serialize(b)).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

		//	@RestMethod(name="POST", path="/C")
		//	public DTO2s.C testPojo2(@Content DTO2s.C c) {
		//		return c;
		//	}
		DTOs.C x = DTOs.C.create();
		r = c.doPost(URL + "/C", "" + UonSerializer.DEFAULT.serialize(x)).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);
		r = c.doPost(URL + "/C", "" + UonSerializer.DEFAULT_SIMPLE.serialize(x)).getResponseAsString();
		assertEquals("{f1:['a','b'],f2:['c','d'],f3:[1,2],f4:[3,4],f5:[['e','f'],['g','h']],f6:[['i','j'],['k','l']],f7:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f8:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f9:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}", r);

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
