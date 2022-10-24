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
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Query_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Simple tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public String a(RestRequest req, @Query("p1") @Schema(aev=true) String p1, @Query("p2") @Schema(aev=true) int p2) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"],p2=["+p2+","+q.get("p2").orElse(null)+","+q.get("p2").asInteger().orElse(0)+"]";
		}
		@RestPost
		public String b(RestRequest req, @Query("p1") @Schema(aev=true) String p1, @Query("p2") @Schema(aev=true) int p2) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"],p2=["+p2+","+q.get("p2").orElse(null)+","+q.get("p2").asInteger().orElse(0)+"]";
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);

		a.get("/a?p1=p1&p2=2").run().assertContent("p1=[p1,p1,p1],p2=[2,2,2]");
		a.get("/a?p1&p2").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.get("/a?p1=&p2=").run().assertContent("p1=[,,],p2=[0,,0]");
		a.get("/a").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.get("/a?p1").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.get("/a?p1=").run().assertContent("p1=[,,],p2=[0,null,0]");
		a.get("/a?p2").run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.get("/a?p2=").run().assertContent("p1=[null,null,null],p2=[0,,0]");
		a.get("/a?p1=foo&p2").run().assertContent("p1=[foo,foo,foo],p2=[0,null,0]");
		a.get("/a?p1&p2=1").run().assertContent("p1=[null,null,null],p2=[1,1,1]");
		String x1 = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.get("/a?p1="+x1+"&p2=1").run().assertContent("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");

		a.post("/b?p1=p1&p2=2", null).run().assertContent("p1=[p1,p1,p1],p2=[2,2,2]");
		a.post("/b?p1&p2", null).run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/b?p1=&p2=", null).run().assertContent("p1=[,,],p2=[0,,0]");
		a.post("/b", null).run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/b?p1", null).run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/b?p1=", null).run().assertContent("p1=[,,],p2=[0,null,0]");
		a.post("/b?p2", null).run().assertContent("p1=[null,null,null],p2=[0,null,0]");
		a.post("/b?p2=", null).run().assertContent("p1=[null,null,null],p2=[0,,0]");
		a.post("/b?p1=foo&p2", null).run().assertContent("p1=[foo,foo,foo],p2=[0,null,0]");
		a.post("/b?p1&p2=1", null).run().assertContent("p1=[null,null,null],p2=[1,1,1]");
		String x2 = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("/b?p1="+x2+"&p2=1", null).run().assertContent("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// UON parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet
		public String a(RestRequest req, @Query("p1") String p1) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"]";
		}
		@RestGet
		public String b(RestRequest req, @Query("p1") @Schema(f="uon") String p1) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"]";
		}
		@RestPost
		public String c(RestRequest req, @Query("p1") String p1) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"]";
		}
		@RestPost
		public String d(RestRequest req, @Query("p1") @Schema(f="uon") String p1) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.get("p1").orElse(null)+","+q.get("p1").asString().orElse(null)+"]";
		}
	}

	@Test
	public void b01_uonParameters() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/a?p1=p1").run().assertContent("p1=[p1,p1,p1]");
		b.get("/a?p1='p1'").run().assertContent("p1=['p1','p1','p1']");
		b.get("/b?p1=p1").run().assertContent("p1=[p1,p1,p1]");
		b.get("/b?p1='p1'").run().assertContent("p1=[p1,'p1','p1']");
		b.post("/c?p1=p1", null).run().assertContent("p1=[p1,p1,p1]");
		b.post("/c?p1='p1'", null).run().assertContent("p1=['p1','p1','p1']");
		b.post("/d?p1=p1", null).run().assertContent("p1=[p1,p1,p1]");
		b.post("/d?p1='p1'", null).run().assertContent("p1=[p1,'p1','p1']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Multipart parameters (e.g. &key=val1,&key=val2).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class C {
		public static class C1 {
			public String a;
			public int b;
			public boolean c;
		}

		@RestGet
		public Object a(@Query("x") @Schema(cf="multi") String[] x) {
			return x;
		}
		@RestGet
		public Object b(@Query("x") @Schema(cf="multi") int[] x) {
			return x;
		}
		@RestGet
		public Object c(@Query("x") @Schema(cf="multi") List<String> x) {
			return x;
		}
		@RestGet
		public Object d(@Query("x") @Schema(cf="multi") List<Integer> x) {
			return x;
		}
		@RestGet
		public Object e(@Query("x") @Schema(cf="multi",items=@Items(f="uon")) C1[] x) {
			return x;
		}
		@RestGet
		public Object f(@Query("x") @Schema(cf="multi",items=@Items(f="uon")) List<C1> x) {
			return x;
		}
	}

	@Test
	public void c01_multipartParams() throws Exception {
		RestClient c = MockRestClient.build(C.class);
		c.get("/a?x=a").run().assertContent("['a']");
		c.get("/a?x=a&x=b").run().assertContent("['a','b']");
		c.get("/b?x=1").run().assertContent("[1]");
		c.get("/b?x=1&x=2").run().assertContent("[1,2]");
		c.get("/c?x=a").run().assertContent("['a']");
		c.get("/c?x=a&x=b").run().assertContent("['a','b']");
		c.get("/d?x=1").run().assertContent("[1]");
		c.get("/d?x=1&x=2").run().assertContent("[1,2]");
		c.get("/e?x=a=1,b=2,c=false").run().assertContent("[{a:'1,b=2,c=false',b:0,c:false}]");
		c.get("/e?x=a=1,b=2,c=false&x=a=3,b=4,c=true").run().assertContent("[{a:'1,b=2,c=false',b:0,c:false},{a:'3,b=4,c=true',b:0,c:false}]");
		c.get("/f?x=a=1,b=2,c=false").run().assertContent("[{a:'1,b=2,c=false',b:0,c:false}]");
		c.get("/f?x=a=1,b=2,c=false&x=a=3,b=4,c=true").run().assertContent("[{a:'1,b=2,c=false',b:0,c:false},{a:'3,b=4,c=true',b:0,c:false}]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default values.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet(defaultRequestQueryData={"f1:1","f2=2"," f3 : 3 "})
		public JsonMap a(RequestQueryParams query) {
			return JsonMap.create()
				.append("f1", query.get("f1").asString())
				.append("f2", query.get("f2").asString())
				.append("f3", query.get("f3").asString());
		}
		@RestGet
		public JsonMap b(@Query("f1") String f1, @Query("f2") String f2, @Query("f3") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestGet
		public JsonMap c(@Query("f1") @Schema(df="1") String f1, @Query("f2") @Schema(df="2") String f2, @Query("f3") @Schema(df="3") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestGet(defaultRequestQueryData={"f1:1","f2=2"," f3 : 3 "})
		public JsonMap d(@Query("f1") @Schema(df="4") String f1, @Query("f2") @Schema(df="5") String f2, @Query("f3") @Schema(df="6") String f3) {
			return JsonMap.create()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}

	@Test
	public void d01_defaultValues() throws Exception {
		RestClient d = MockRestClient.build(D.class);
		d.get("/a").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		d.get("/a").queryData("f1",4).queryData("f2",5).queryData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");
		d.get("/b").run().assertContent("{f1:null,f2:null,f3:null}");
		d.get("/b").queryData("f1",4).queryData("f2",5).queryData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");
		d.get("/c").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		d.get("/c").queryData("f1",4).queryData("f2",5).queryData("f3",6).run().assertContent("{f1:'4',f2:'5',f3:'6'}");
		d.get("/d").run().assertContent("{f1:'1',f2:'2',f3:'3'}");
		d.get("/d").queryData("f1",7).queryData("f2",8).queryData("f3",9).run().assertContent("{f1:'7',f2:'8',f3:'9'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional query parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class E {
		@RestGet
		public Object a(@Query("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object b(@Query("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object c(@Query("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object d(@Query("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void e01_optionalParams() throws Exception {
		RestClient e = MockRestClient.buildJson(E.class);
		e.get("/a?f1=123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		e.get("/a")
			.run()
			.assertStatus(200)
			.assertContent("null");
		e.get("/b?f1=a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		e.get("/b")
			.run()
			.assertStatus(200)
			.assertContent("null");
		e.get("/c?f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		e.get("/c")
			.run()
			.assertStatus(200)
			.assertContent("null");
		e.get("/d?f1=@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		e.get("/d")
			.run()
			.assertStatus(200)
			.assertContent("null");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class F {
		@RestGet
		public Object a1(@Query(name="f1", def="1") Integer f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object a2(@Query(name="f1", def="1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1.get();
		}
		@RestGet
		public Object b1(@Query(name="f1", def="a=1,b=foo") ABean f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object b2(@Query(name="f1", def="a=1,b=foo") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1.get();
		}
		@RestGet
		public Object c1(@Query(name="f1", def="@((a=1,b=foo))") List<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestGet
		public Object c2(@Query(name="f1", def="@((a=1,b=foo))") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1.get();
		}
		@RestGet
		public Object d(@Query(name="f1", def="@((a=1,b=foo))") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void f01_defaultParams() throws Exception {
		RestClient f = MockRestClient.buildJson(F.class);
		f.get("/a1?f1=123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		f.get("/a1")
			.run()
			.assertStatus(200)
			.assertContent("1");
		f.get("/a2?f1=123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		f.get("/a2")
			.run()
			.assertStatus(200)
			.assertContent("1");
		f.get("/b1?f1=a=2,b=bar")
			.run()
			.assertStatus(200)
			.assertContent("{a:2,b:'bar'}");
		f.get("/b1")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		f.get("/b2?f1=a=2,b=bar")
			.run()
			.assertStatus(200)
			.assertContent("{a:2,b:'bar'}");
		f.get("/b2")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		f.get("/c1?f1=@((a=2,b=bar))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");
		f.get("/c1")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		f.get("/c2?f1=@((a=2,b=bar))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");
		f.get("/c2")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		f.get("/d?f1=@((a=2,b=bar))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:2,b:'bar'}]");
		f.get("/d")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
	}
}
