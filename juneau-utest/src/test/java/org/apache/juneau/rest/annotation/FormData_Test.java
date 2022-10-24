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

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.httppart.*;
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
		public String a(RestRequest req, @FormData("p1") @Schema(allowEmptyValue=true) String p1, @FormData("p2") @Schema(allowEmptyValue=true) int p2) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").asString().orElse(null)+"],p2=["+p2+","+f.get("p2").orElse(null)+","+f.get("p2").as(int.class).orElse(null)+"]";
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.post("/a", "p1=p1&p2=2").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[p1,p1,p1],p2=[2,2,2]");
		a.post("/a", "p1&p2").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1=&p2=").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[,,],p2=[0,,0]");
		a.post("/a", "").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p1=").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[,,],p2=[0,null,0]");
		a.post("/a", "p2").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/a", "p2=").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[0,,0]");
		a.post("/a", "p1=foo&p2").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[foo,foo,foo],p2=[0,null,0]");
		a.post("/a", "p1&p2=1").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("/a", "p1="+x+"&p2=1").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// UON parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestPost
		public String a(RestRequest req, @FormData("p1") String p1) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").orElse(null)+"]";
		}
		@RestPost
		public String b(RestRequest req, @FormData("p1") @Schema(format="uon") String p1) throws Exception {
			RequestFormParams f = req.getFormParams();
			return "p1=["+p1+","+f.get("p1").orElse(null)+","+f.get("p1").orElse(null)+"]";
		}
	}

	@Test
	public void b01_uonParameters() throws Exception {
		RestClient b = MockRestClient.build(B.class);

		b.post("/a", "p1=p1").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[p1,p1,p1]");
		b.post("/a", "p1='p1'").contentType("application/x-www-form-urlencoded").run().assertContent("p1=['p1','p1','p1']");

		b.post("/b", "p1=p1").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[p1,p1,p1]");
		b.post("/b", "p1='p1'").contentType("application/x-www-form-urlencoded").run().assertContent("p1=[p1,'p1','p1']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default values.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestPost(defaultRequestFormData={"f1:1","f2=2"," f3 : 3 "})
		public JsonMap a(RequestFormParams formData) {
			return JsonMap.create()
				.append("f1", formData.get("f1").asString())
				.append("f2", formData.get("f2").asString())
				.append("f3", formData.get("f3").asString());
		}
		@RestPost
		public JsonMap b(@FormData("f1") String f1, @FormData("f2") String f2, @FormData("f3") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestPost
		public JsonMap c(@FormData("f1") @Schema(_default="1") String f1, @FormData("f2") @Schema(_default="2") String f2, @FormData("f3") @Schema(_default="3") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestPost(defaultRequestFormData={"f1:1","f2=2"," f3 : 3 "})
		public JsonMap d(@FormData("f1") @Schema(_default="4") String f1, @FormData("f2") @Schema(_default="5") String f2, @FormData("f3") @Schema(_default="6") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}

	@Test
	public void c01_defaultFormData() throws Exception {
		RestClient c = MockRestClient.build(C.class);

		c.post("/a").contentType("application/x-www-form-urlencoded").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		c.post("/a").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");

		c.post("/b").contentType("application/x-www-form-urlencoded").run().assertContent("{f1:null,f2:null,f3:null}");
		c.post("/b").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");

		c.post("/c").contentType("application/x-www-form-urlencoded").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		c.post("/c").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");

		c.post("/d").contentType("application/x-www-form-urlencoded").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		c.post("/d").contentType("application/x-www-form-urlencoded").formData("f1",7).formData("f2",8).formData("f3",9).run().assertContent("{f1:'7',f2:'8',f3:'9'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional form data parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
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
			.assertStatus(200)
			.assertContent("123");
		d.post("/a", "null")
			.run()
			.assertStatus(200)
			.assertContent("null");

		d.post("/b", "f1=a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		d.post("/b", "null")
			.run()
			.assertStatus(200)
			.assertContent("null");

		d.post("/c", "f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		d.post("/c", "null")
			.run()
			.assertStatus(200)
			.assertContent("null");

		d.post("/d", "f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		d.post("/d", "null")
			.run()
			.assertStatus(200)
			.assertContent("null");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default form data parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class F {
		@RestPost
		public Object a1(@FormData(name="f1",def="1") Integer f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object a2(@FormData(name="f1",def="1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object b1(@FormData(name="f1",def="a=2,b=bar") ABean f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object b2(@FormData(name="f1",def="a=2,b=bar") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object c1(@FormData(name="f1",def="@((a=2,b=bar))") List<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object c2(@FormData(name="f1",def="@((a=2,b=bar))") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost
		public Object d(@FormData(name="f1",def="@((a=2,b=bar))") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void f01_defaultParams() throws Exception {
		RestClient f = MockRestClient.create(F.class).accept("application/json").contentType("application/x-www-form-urlencoded").build();

		f.post("/a1", "f1=123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		f.post("/a1", "")
			.run()
			.assertStatus(200)
			.assertContent("1");
		f.post("/a2", "f1=123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		f.post("/a2", "")
			.run()
			.assertStatus(200)
			.assertContent("1");

		f.post("/b1", "f1=a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		f.post("/b1", "")
			.run()
			.assertStatus(200)
			.assertContent("{a:2,b:'bar'}");
		f.post("/b2", "f1=a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		f.post("/b2", "")
			.run()
			.assertStatus(200)
			.assertContent("{a:2,b:'bar'}");

		f.post("/c1", "f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		f.post("/c1", "null")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");
		f.post("/c2", "f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		f.post("/c2", "null")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");


		f.post("/d", "f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		f.post("/d", "null")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");
	}
}
