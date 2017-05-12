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
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests client-side form posts.
 */
public class FormDataTest extends RestTestcase {

	private static String URL = "/testFormData";
	RestClient client = TestMicroservice.DEFAULT_CLIENT;

	//====================================================================================================
	// Form data tests using RestCall.formData() method.
	//====================================================================================================
	@Test
	public void testFormDataMethod() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;
		String r;

		r = c.doPost(URL)
			.formData("foo", 123)
			.formData("bar", "baz")
			.getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=123&bar=baz]", r);
	}

	//====================================================================================================
	// Form data tests using RestClient.doPost(NameValuePairs).
	//====================================================================================================
	@Test
	public void testDoPostNameValuePairs() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;
		String r;

		r = c.doPost(URL, new NameValuePairs().append("foo",123).append("bar","baz"))
			.getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=123&bar=baz]", r);
	}

	public static class A {
		public int f1 = 1;
	}

	//====================================================================================================
	// Form data tests using RestClientBuilder.plainTextParams().
	//====================================================================================================
	@Test
	public void testPlainTextParams() throws Exception {
		RestClient c = TestMicroservice.client(UrlEncodingSerializer.class, UrlEncodingParser.class).plainTextParams().build();
		String r;

		Map<String,Object> m = new AMap<String,Object>()
			.append("foo", "foo")
			.append("'foo'", "'foo'")
			.append("(foo)", "(foo)")
			.append("@(foo)", "@(foo)");

		r = c.doPost(URL, m).getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&'foo'='foo'&(foo)=(foo)&@(foo)=@(foo)]", r);

		List<String> l = new AList<String>().appendAll("foo", "'foo'", "(foo)", "@(foo)");
		r = c.doPost(URL, l).getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[0=foo&1='foo'&2=(foo)&3=@(foo)]", r);

		NameValuePairs nvp = new NameValuePairs()
			.append("foo", "foo")
			.append("'foo'", "'foo'")
			.append("(foo)", "(foo)")
			.append("@(foo)", "@(foo)");
		r = c.doPost(URL, nvp).getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&%27foo%27=%27foo%27&%28foo%29=%28foo%29&%40%28foo%29=%40%28foo%29]", r);

		r = c.doPost(URL)
			.formData("foo", "foo")
			.formData("'foo'", "'foo'")
			.formData("(foo)", "(foo)")
			.formData("@(foo)", "@(foo)")
			.getResponseAsString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&%27foo%27=%27foo%27&%28foo%29=%28foo%29&%40%28foo%29=%40%28foo%29]", r);
	}

	//====================================================================================================
	// Default values.
	//====================================================================================================

	@Test
	public void defaultFormData() throws Exception {
		assertObjectEquals("{f1:'1',f2:'2',f3:'3'}", client.doPost(URL + "/defaultFormData").getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/defaultFormData").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/defaultFormData").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedFormData() throws Exception {
		assertObjectEquals("{f1:null,f2:null,f3:null}", client.doPost(URL + "/annotatedFormData").getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/annotatedFormData").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/annotatedFormData").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedFormDataDefault() throws Exception {
		assertObjectEquals("{f1:'1',f2:'2',f3:'3'}", client.doPost(URL + "/annotatedFormDataDefault").getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/annotatedFormDataDefault").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/annotatedFormDataDefault").formData("f1",4).formData("f2",5).formData("f3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedAndDefaultFormData() throws Exception {
		assertObjectEquals("{f1:'4',f2:'5',f3:'6'}", client.doPost(URL + "/annotatedAndDefaultFormData").getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'7',f2:'8',f3:'9'}", client.doPost(URL + "/annotatedAndDefaultFormData").formData("f1",7).formData("f2",8).formData("f3",9).getResponse(ObjectMap.class));
		assertObjectEquals("{f1:'7',f2:'8',f3:'9'}", client.doPost(URL + "/annotatedAndDefaultFormData").formData("f1",7).formData("f2",8).formData("f3",9).getResponse(ObjectMap.class));
	}
}
