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

import org.apache.juneau.*;
import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.junit.*;

public class CT_TestNls {

	private static String URL = "/testNls";

	// ====================================================================================================
	// test1 - Pull labels from annotations only.
	// ====================================================================================================
	@Test
	public void test1() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test1").getResponse(ObjectMap.class);
		assertEquals("Test1.a", r.getString("label"));
		assertEquals("Test1.b", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(0);
		assertEquals("test1", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'Test1.d'},{category:'attr',name:'a2',description:'Test1.h'},{category:'attr',name:'e'},{category:'content',name:'',description:'Test1.f'},{category:'foo',name:'bar',description:'Test1.k'},{category:'header',name:'D',description:'Test1.g'},{category:'header',name:'D2',description:'Test1.j'},{category:'header',name:'g'},{category:'param',name:'b',description:'Test1.e'},{category:'param',name:'b2',description:'Test1.i'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'OK',output:[]},{status:201,description:'Test1.l',output:[{category:'foo',name:'bar',description:'Test1.m'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test2 - Pull labels from resource bundles only - simple keys.
	// ====================================================================================================
	@Test
	public void test2() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test2").getResponse(ObjectMap.class);
		assertEquals("Test2.a", r.getString("label"));
		assertEquals("Test2.b", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(0);
		assertEquals("test2", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'Test2.d'},{category:'attr',name:'a2',description:'Test2.h'},{category:'attr',name:'e'},{category:'content',name:'',description:'Test2.f'},{category:'foo',name:'bar',description:'Test2.k'},{category:'header',name:'D',description:'Test2.g'},{category:'header',name:'D2',description:'Test2.j'},{category:'header',name:'g'},{category:'param',name:'b',description:'Test2.e'},{category:'param',name:'b2',description:'Test2.i'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'OK2',output:[]},{status:201,description:'Test2.l',output:[{category:'foo',name:'bar',description:'Test2.m'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test3 - Pull labels from resource bundles only - keys with class names.
	// ====================================================================================================
	@Test
	public void test3() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test3").getResponse(ObjectMap.class);
		assertEquals("Test3.a", r.getString("label"));
		assertEquals("Test3.b", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(1);
		assertEquals("test3", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'Test3.d'},{category:'attr',name:'a2',description:'Test3.h'},{category:'attr',name:'e'},{category:'content',name:'',description:'Test3.f'},{category:'foo',name:'bar',description:'Test3.k'},{category:'header',name:'D',description:'Test3.g'},{category:'header',name:'D2',description:'Test3.j'},{category:'header',name:'g'},{category:'param',name:'b',description:'Test3.e'},{category:'param',name:'b2',description:'Test3.i'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'OK3',output:[]},{status:201,description:'Test3.l',output:[{category:'foo',name:'bar',description:'Test3.m'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test4 - Pull labels from resource bundles only. Values have localized variables to resolve.
	// ====================================================================================================
	@Test
	public void test4() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test4").getResponse(ObjectMap.class);
		assertEquals("baz", r.getString("label"));
		assertEquals("baz", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(0);
		assertEquals("test4", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'baz'},{category:'attr',name:'a2',description:'baz'},{category:'attr',name:'e'},{category:'content',name:'',description:'baz'},{category:'foo',name:'bar',description:'baz'},{category:'header',name:'D',description:'baz'},{category:'header',name:'D2',description:'baz'},{category:'header',name:'g'},{category:'param',name:'b',description:'baz'},{category:'param',name:'b2',description:'baz'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'foobazfoobazfoo',output:[]},{status:201,description:'baz',output:[{category:'foo',name:'bar',description:'baz'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test5 - Pull labels from resource bundles only. Values have request variables to resolve.
	// ====================================================================================================
	@Test
	public void test5() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test5").getResponse(ObjectMap.class);
		assertEquals("baz2", r.getString("label"));
		assertEquals("baz2", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(0);
		assertEquals("test5", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'baz2'},{category:'attr',name:'a2',description:'baz2'},{category:'attr',name:'e'},{category:'content',name:'',description:'baz2'},{category:'foo',name:'bar',description:'baz2'},{category:'header',name:'D',description:'baz2'},{category:'header',name:'D2',description:'baz2'},{category:'header',name:'g'},{category:'param',name:'b',description:'baz2'},{category:'param',name:'b2',description:'baz2'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'foobaz2foobaz2foo',output:[]},{status:201,description:'baz2',output:[{category:'foo',name:'bar',description:'baz2'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test6 - Pull labels from annotations only, but annotations contain variables.
	// ====================================================================================================
	@Test
	public void test6() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r = null;
		String expected = null;

		// Labels all pulled from annotations.
		r = client.doOptions(URL + "/test6").getResponse(ObjectMap.class);
		assertEquals("baz", r.getString("label"));
		assertEquals("baz", r.getString("description"));
		r = r.getObjectList("methods").getObjectMap(0);
		assertEquals("test6", r.getString("javaMethod"));
		assertEquals("POST", r.getString("httpMethod"));
		expected = "[{category:'attr',name:'a',description:'baz'},{category:'attr',name:'a2',description:'baz'},{category:'attr',name:'e'},{category:'content',name:'',description:'baz'},{category:'foo',name:'bar',description:'baz'},{category:'header',name:'D',description:'baz'},{category:'header',name:'D2',description:'baz'},{category:'header',name:'g'},{category:'param',name:'b',description:'baz'},{category:'param',name:'b2',description:'baz'},{category:'param',name:'f'}]";
		assertEquals(expected, r.getObjectList("input").toString());
		expected = "[{status:200,description:'OK',output:[]},{status:201,description:'baz',output:[{category:'foo',name:'bar',description:'baz'}]}]";
		assertEquals(expected, r.getObjectList("responses").toString());

		client.closeQuietly();
	}

}
