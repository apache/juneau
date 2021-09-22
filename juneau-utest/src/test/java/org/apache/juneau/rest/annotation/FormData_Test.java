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
package org.apache.juneau.rest.annotation;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class FormData_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Simple tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(parsers=UrlEncodingParser.class)
	public static class A {
		@RestPost
		public String a(RestRequest req, @FormData(name="p1",allowEmptyValue=true) String p1, @FormData(name="p2",allowEmptyValue=true) int p2) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").asString().orElse(null)+"],p2=["+p2+","+f.get("p2").orElse(null)+","+f.get("p2").asType(int.class).orElse(null)+"]";
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.post("/a", "p1=p1&p2=2").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[p1,p1,p1],p2=[2,2,2]");
		a.post("/a", "p1&p2").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1=&p2=").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[,,],p2=[0,,0]");
		a.post("/a", "").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1=").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[,,],p2=[0,null,0]");
		a.post("/a", "p2").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p2=").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[0,,0]");
		a.post("/a", "p1=foo&p2").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[foo,foo,foo],p2=[0,null,0]");
		a.post("/a", "p1&p2=1").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("/a", "p1="+x+"&p2=1").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// UON parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestPost
		public String a(RestRequest req, @FormData(value="p1") String p1) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").orElse(null)+"]";
		}
		@RestPost
		public String b(RestRequest req, @FormData(value="p1",format="uon") String p1) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").orElse(null)+"]";
		}
	}

	@Test
	public void b01_uonParameters() throws Exception {
		RestClient b = MockRestClient.build(B.class);

		b.post("/a", "p1=p1").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[p1,p1,p1]");
		b.post("/a", "p1='p1'").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=['p1','p1','p1']");

		b.post("/b", "p1=p1").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[p1,p1,p1]");
		b.post("/b", "p1='p1'").contentType("application/x-www-form-urlencoded").run().assertBody().is("p1=[p1,'p1','p1']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default values.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestPost(defaultRequestFormData={"f1:1","f2=2"," f3 : 3 "})
		public OMap a(RequestFormParams formData) {
			return OMap.create()
				.a("f1", formData.getString("f1"))
				.a("f2", formData.getString("f2"))
				.a("f3", formData.getString("f3"));
		}
		@RestPost
		public OMap b(@FormData("f1") String f1, @FormData("f2") String f2, @FormData("f3") String f3) {
			return OMap.create()
				.a("f1", f1)
				.a("f2", f2)
				.a("f3", f3);
		}
		@RestPost
		public OMap c(@FormData(value="f1",_default="1") String f1, @FormData(value="f2",_default="2") String f2, @FormData(value="f3",_default="3") String f3) {
			return OMap.create()
				.a("f1", f1)
				.a("f2", f2)
				.a("f3", f3);
		}
		@RestPost(defaultRequestFormData={"f1:1","f2=2"," f3 : 3 "})
		public OMap d(@FormData(value="f1",_default="4") String f1, @FormData(value="f2",_default="5") String f2, @FormData(value="f3",_default="6") String f3) {
			return OMap.create()
				.a("f1", f1)
				.a("f2", f2)
				.a("f3", f3);
		}
	}

	@Test
	public void c01_defaultFormData() throws Exception {
		RestClient c = MockRestClient.build(C.class);

		c.post("/a").contentType("application/x-www-form-urlencoded").run().assertBody().is("{f1:'1',f2:'2',f3:'3'}");
		c.post("/a").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertBody().is("{f1:'4',f2:'5',f3:'6'}");

		c.post("/b").contentType("application/x-www-form-urlencoded").run().assertBody().is("{f1:null,f2:null,f3:null}");
		c.post("/b").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertBody().is("{f1:'4',f2:'5',f3:'6'}");

		c.post("/c").contentType("application/x-www-form-urlencoded").run().assertBody().is("{f1:'1',f2:'2',f3:'3'}");
		c.post("/c").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertBody().is("{f1:'4',f2:'5',f3:'6'}");

		c.post("/d").contentType("application/x-www-form-urlencoded").run().assertBody().is("{f1:'4',f2:'5',f3:'6'}");
		c.post("/d").contentType("application/x-www-form-urlencoded").formData("f1",7).formData("f2",8).formData("f3",9).run().assertBody().is("{f1:'7',f2:'8',f3:'9'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional form data parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class D {
		@RestPost
		public Object a(@FormData("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object b(@FormData("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object c(@FormData("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object d(@FormData("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void d01_optionalParams() throws Exception {
		RestClient d = MockRestClient.create(D.class).accept("application/json").contentType("application/x-www-form-urlencoded").build();

		d.post("/a", "f1=123")
			.run()
			.assertCode().is(200)
			.assertBody().is("123");
		d.post("/a", "null")
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		d.post("/b", "f1=a=1,b=foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		d.post("/b", "null")
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		d.post("/c", "f1=@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		d.post("/c", "null")
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		d.post("/d", "f1=@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		d.post("/d", "null")
			.run()
			.assertCode().is(200)
			.assertBody().is("null");
	}
}
