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

import static org.apache.juneau.rest.test.TestUtils.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class NlsTest extends RestTestcase {

	private static String URL = "/testNls";

	// ====================================================================================================
	// test1 - Pull labels from annotations only.
	// ====================================================================================================
	@Test
	public void test1() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test1").getResponse(Swagger.class);
		assertObjectEquals("{title:'Test1.a',description:'Test1.b'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'Test1.f'},{'in':'header',name:'D',type:'string',description:'Test1.g'},{'in':'header',name:'D2',type:'string',description:'Test1.j'},{'in':'header',name:'g'},{'in':'path',name:'a',type:'string',description:'Test1.d',required:true},{'in':'path',name:'a2',type:'string',description:'Test1.h',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',type:'string',description:'Test1.e'},{'in':'query',name:'b2',type:'string',description:'Test1.i'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'OK'},'201':{description:'Test1.l',headers:{bar:{description:'Test1.m',type:'string'}}}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test2 - Pull labels from resource bundles only - simple keys.
	// ====================================================================================================
	@Test
	public void test2() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test2").getResponse(Swagger.class);
		assertObjectEquals("{title:'Test2.a',description:'Test2.b'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'Test2.f'},{'in':'header',name:'D',description:'Test2.g'},{'in':'header',name:'D2',description:'Test2.j'},{'in':'header',name:'g'},{'in':'path',name:'a',description:'Test2.d',required:true},{'in':'path',name:'a2',description:'Test2.h',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',description:'Test2.e'},{'in':'query',name:'b2',description:'Test2.i'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'OK2'},'201':{description:'Test2.l'}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test3 - Pull labels from resource bundles only - keys with class names.
	// ====================================================================================================
	@Test
	public void test3() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test3").getResponse(Swagger.class);
		assertObjectEquals("{title:'Test3.a',description:'Test3.b'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'Test3.f'},{'in':'header',name:'D',description:'Test3.g'},{'in':'header',name:'D2',description:'Test3.j'},{'in':'header',name:'g'},{'in':'path',name:'a',description:'Test3.d',required:true},{'in':'path',name:'a2',description:'Test3.h',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',description:'Test3.e'},{'in':'query',name:'b2',description:'Test3.i'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'OK3'},'201':{description:'Test3.l'}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test4 - Pull labels from resource bundles only. Values have localized variables to resolve.
	// ====================================================================================================
	@Test
	public void test4() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test4").getResponse(Swagger.class);
		assertObjectEquals("{title:'baz',description:'baz'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'baz'},{'in':'header',name:'D',description:'baz'},{'in':'header',name:'D2',description:'baz'},{'in':'header',name:'g'},{'in':'path',name:'a',description:'baz',required:true},{'in':'path',name:'a2',description:'baz',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',description:'baz'},{'in':'query',name:'b2',description:'baz'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'foobazfoobazfoo'},'201':{description:'baz'}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test5 - Pull labels from resource bundles only. Values have request variables to resolve.
	// ====================================================================================================
	@Test
	public void test5() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test5").getResponse(Swagger.class);
		assertObjectEquals("{title:'baz2',description:'baz2'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'baz2'},{'in':'header',name:'D',description:'baz2'},{'in':'header',name:'D2',description:'baz2'},{'in':'header',name:'g'},{'in':'path',name:'a',description:'baz2',required:true},{'in':'path',name:'a2',description:'baz2',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',description:'baz2'},{'in':'query',name:'b2',description:'baz2'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'foobaz2foobaz2foo'},'201':{description:'baz2'}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}

	// ====================================================================================================
	// test6 - Pull labels from annotations only, but annotations contain variables.
	// ====================================================================================================
	@Test
	public void test6() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);

		Swagger s = client.doOptions(URL + "/test6").getResponse(Swagger.class);
		assertObjectEquals("{title:'baz',description:'baz'}", s.getInfo());
		assertObjectEquals("[{'in':'body',description:'baz'},{'in':'header',name:'D',type:'string',description:'baz'},{'in':'header',name:'D2',type:'string',description:'baz'},{'in':'header',name:'g'},{'in':'path',name:'a',type:'string',description:'baz',required:true},{'in':'path',name:'a2',type:'string',description:'baz',required:true},{'in':'path',name:'e',required:true},{'in':'query',name:'b',type:'string',description:'baz'},{'in':'query',name:'b2',type:'string',description:'baz'},{'in':'query',name:'f'}]", s.getPaths().get("/{a}").get("post").getParameters());
		assertObjectEquals("{'200':{description:'OK'},'201':{description:'baz',headers:{bar:{description:'baz',type:'string'}}}}", s.getPaths().get("/{a}").get("post").getResponses());

		client.closeQuietly();
	}
}
