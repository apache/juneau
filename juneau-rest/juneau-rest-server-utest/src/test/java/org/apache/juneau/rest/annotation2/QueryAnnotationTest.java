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
package org.apache.juneau.rest.annotation2;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @Query annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QueryAnnotationTest {

	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public String get(RestRequest req, @Query(name="p1",allowEmptyValue=true) String p1, @Query(name="p2",allowEmptyValue=true) int p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"],p2=["+p2+","+q.getString("p2")+","+q.get("p2", int.class)+"]";
		}
		@RestMethod
		public String post(RestRequest req, @Query(name="p1",allowEmptyValue=true) String p1, @Query(name="p2",allowEmptyValue=true) int p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"],p2=["+p2+","+q.getString("p2")+","+q.get("p2", int.class)+"]";
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_get() throws Exception {
		a.get("?p1=p1&p2=2").execute().assertBody("p1=[p1,p1,p1],p2=[2,2,2]");
		a.get("?p1&p2").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.get("?p1=&p2=").execute().assertBody("p1=[,,],p2=[0,,0]");
		a.get("/").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.get("?p1").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.get("?p1=").execute().assertBody("p1=[,,],p2=[0,null,0]");
		a.get("?p2").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.get("?p2=").execute().assertBody("p1=[null,null,null],p2=[0,,0]");
		a.get("?p1=foo&p2").execute().assertBody("p1=[foo,foo,foo],p2=[0,null,0]");
		a.get("?p1&p2=1").execute().assertBody("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.get("?p1="+x+"&p2=1").execute().assertBody("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}
	@Test
	public void a02_post() throws Exception {
		a.post("?p1=p1&p2=2", null).execute().assertBody("p1=[p1,p1,p1],p2=[2,2,2]");
		a.post("?p1&p2", null).execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("?p1=&p2=", null).execute().assertBody("p1=[,,],p2=[0,,0]");
		a.post("/", null).execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("?p1", null).execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("?p1=", null).execute().assertBody("p1=[,,],p2=[0,null,0]");
		a.post("?p2", null).execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("?p2=", null).execute().assertBody("p1=[null,null,null],p2=[0,,0]");
		a.post("?p1=foo&p2", null).execute().assertBody("p1=[foo,foo,foo],p2=[0,null,0]");
		a.post("?p1&p2=1", null).execute().assertBody("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("?p1="+x+"&p2=1", null).execute().assertBody("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}

	//=================================================================================================================
	// UON parameters
	//=================================================================================================================

	@Rest
	public static class B {
		@RestMethod(name=GET,path="/get1")
		public String get1(RestRequest req, @Query(name="p1") String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
		@RestMethod(name=GET,path="/get2")
		public String get2(RestRequest req, @Query(name="p1",format="uon") String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
		@RestMethod(name=POST,path="/post1")
		public String post1(RestRequest req, @Query(name="p1") String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
		@RestMethod(name=POST,path="/post2")
		public String post2(RestRequest req, @Query(name="p1",format="uon") String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
	}
	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01_get1() throws Exception {
		b.get("/get1?p1=p1").execute().assertBody("p1=[p1,p1,p1]");
		b.get("/get1?p1='p1'").execute().assertBody("p1=['p1','p1','p1']");
	}
	@Test
	public void b02_get2() throws Exception {
		b.get("/get2?p1=p1").execute().assertBody("p1=[p1,p1,p1]");
		b.get("/get2?p1='p1'").execute().assertBody("p1=[p1,'p1','p1']");
	}
	@Test
	public void b03_post1() throws Exception {
		b.post("/post1?p1=p1", null).execute().assertBody("p1=[p1,p1,p1]");
		b.post("/post1?p1='p1'", null).execute().assertBody("p1=['p1','p1','p1']");
	}
	@Test
	public void b04_post2() throws Exception {
		b.post("/post2?p1=p1", null).execute().assertBody("p1=[p1,p1,p1]");
		b.post("/post2?p1='p1'", null).execute().assertBody("p1=[p1,'p1','p1']");
	}

	//=================================================================================================================
	// Multipart parameters (e.g. &key=val1,&key=val2).
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class C {
		public static class C01 {
			public String a;
			public int b;
			public boolean c;
		}

		@RestMethod
		public Object c01(@Query(value="x",collectionFormat="multi") String[] x) {
			return x;
		}
		@RestMethod
		public Object c02(@Query(value="x",collectionFormat="multi") int[] x) {
			return x;
		}
		@RestMethod
		public Object c03(@Query(value="x",collectionFormat="multi") List<String> x) {
			return x;
		}
		@RestMethod
		public Object c04(@Query(value="x",collectionFormat="multi") List<Integer> x) {
			return x;
		}
		@RestMethod
		public Object c05(@Query(value="x",collectionFormat="multi",items=@Items(format="uon")) C01[] x) {
			return x;
		}
		@RestMethod
		public Object c06(@Query(value="x",collectionFormat="multi",items=@Items(format="uon")) List<C01> x) {
			return x;
		}
	}
	static MockRest c = MockRest.build(C.class);

	@Test
	public void c01_StringArray() throws Exception {
		c.get("/c01?x=a").execute().assertBody("['a']");
		c.get("/c01?x=a&x=b").execute().assertBody("['a','b']");
	}
	@Test
	public void c02_intArray() throws Exception {
		c.get("/c02?x=1").execute().assertBody("[1]");
		c.get("/c02?x=1&x=2").execute().assertBody("[1,2]");
	}
	@Test
	public void c03_ListOfStrings() throws Exception {
		c.get("/c03?x=a").execute().assertBody("['a']");
		c.get("/c03?x=a&x=b").execute().assertBody("['a','b']");
	}
	@Test
	public void c04_ListOfIntegers() throws Exception {
		c.get("/c04?x=1").execute().assertBody("[1]");
		c.get("/c04?x=1&x=2").execute().assertBody("[1,2]");
	}
	@Test
	public void c05_BeanArray() throws Exception {
		c.get("/c05?x=(a=1,b=2,c=false)").execute().assertBody("[{a:'1',b:2,c:false}]");
		c.get("/c05?x=(a=1,b=2,c=false)&x=(a=3,b=4,c=true)").execute().assertBody("[{a:'1',b:2,c:false},{a:'3',b:4,c:true}]");
	}
	@Test
	public void c06_ListOfBeans() throws Exception {
		c.get("/c06?x=(a=1,b=2,c=false)").execute().assertBody("[{a:'1',b:2,c:false}]");
		c.get("/c06?x=(a=1,b=2,c=false)&x=(a=3,b=4,c=true)").execute().assertBody("[{a:'1',b:2,c:false},{a:'3',b:4,c:true}]");
	}

	//=================================================================================================================
	// Default values.
	//=================================================================================================================

	@Rest
	public static class D {
		@RestMethod(defaultQuery={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap d01(RequestQuery query) {
			return new ObjectMap()
				.append("f1", query.getString("f1"))
				.append("f2", query.getString("f2"))
				.append("f3", query.getString("f3"));
		}
		@RestMethod
		public ObjectMap d02(@Query("f1") String f1, @Query("f2") String f2, @Query("f3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod
		public ObjectMap d03(@Query(value="f1",_default="1") String f1, @Query(value="f2",_default="2") String f2, @Query(value="f3",_default="3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(defaultQuery={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap d04(@Query(value="f1",_default="4") String f1, @Query(value="f2",_default="5") String f2, @Query(value="f3",_default="6") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}
	static MockRest d = MockRest.build(D.class, null);

	@Test
	public void d01_defaultQuery() throws Exception {
		d.get("/d01").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		d.get("/d01").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d02_annotatedQuery() throws Exception {
		d.get("/d02").execute().assertBody("{f1:null,f2:null,f3:null}");
		d.get("/d02").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d03_annotatedQueryDefault() throws Exception {
		d.get("/d03").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		d.get("/d03").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d04_annotatedAndDefaultQuery() throws Exception {
		d.get("/d04").execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
		d.get("/d04").query("f1",7).query("f2",8).query("f3",9).execute().assertBody("{f1:'7',f2:'8',f3:'9'}");
	}

	//=================================================================================================================
	// Optional query parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class E {
		@RestMethod(name=GET,path="/a")
		public Object a(@Query("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/b")
		public Object b(@Query("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/c")
		public Object c(@Query("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/d")
		public Object d(@Query("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}
	static MockRest e = MockRest.create(E.class).json().build();

	@Test
	public void e01_optionalParam_integer() throws Exception {
		e.get("/a?f1=123").execute().assertStatus(200).assertBody("123");
		e.get("/a").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void e02_optionalParam_bean() throws Exception {
		e.get("/b?f1=(a=1,b=foo)").execute().assertStatus(200).assertBody("{a:1,b:'foo'}");
		e.get("/b").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void e03_optionalParam_listOfBeans() throws Exception {
		e.get("/c?f1=@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
		e.get("/c").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void e04_optionalParam_listOfOptionals() throws Exception {
		e.get("/d?f1=@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
		e.get("/d").execute().assertStatus(200).assertBody("null");
	}


	//=================================================================================================================
	// @Query on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@Query(
			name="Q",
			description= {"a","b"},
			type="string"
		)
		public static class SA01 {
			public SA01(String x) {}
		}
		@RestMethod
		public void sa01(SA01 q) {}

		@Query(
			name="Q",
			api={
				"description: 'a\nb',",
				"type:'string'"
			}
		)
		public static class SA02 {
			public SA02(String x) {}
		}
		@RestMethod
		public void sa02(SA02 q) {}

		@Query(
			name="Q",
			api={
				"description: 'b\nc',",
				"type:'string'"
			},
			description= {"a","b"},
			type="string"
		)
		public static class SA03 {
			public SA03(String x) {}
		}
		@RestMethod
		public void sa03(SA03 q) {}

		@Query("Q")
		public static class SA04 {}
		@RestMethod
		public void sa04(SA04 q) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_Query_onPojo_basic() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa01","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa02_Query_onPojo_api() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa02","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa03_Query_onPojo_mixed() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa03","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa04_Query_onPojo_value() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa04","get","query","Q");
		assertEquals("Q", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SB {

		@Query(name="Q")
		public static class SB01 {}
		@RestMethod
		public void sb01(SB01 q) {}

		@Query("Q")
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02(SB02 q) {}

		@Query("Q")
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03(SB03 q) {}

		@Query("Q")
		public static class SB04 {}
		@RestMethod
		public void sb04(SB04 q) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_Query_onPojo_schemaValue() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb01","get","query","Q");
		assertObjectEquals("{'in':'query',name:'Q',type:'string'}", x);
	}
	@Test
	public void sb02_Query_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb02","get","query","Q");
		assertObjectEquals("{'in':'query',name:'Q',type:'object',schema:{properties:{f1:{type:'string'}}}}", x);
	}
	@Test
	public void sb03_Query_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb03","get","query","Q");
		assertObjectEquals("{'in':'query',name:'Q',type:'array',items:{type:'string'}}", x);
	}
	@Test
	public void sb04_Query_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb04","get","query","Q");
		assertObjectEquals("{'in':'query',name:'Q',type:'string'}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SC {

		@Query(name="Q", example={"{f1:'a'}"})
		public static class SC01 {
			public String f1;
		}
		@RestMethod
		public void sc01(SC01 q) {}
	}

	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01_Query_onPojo_example() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/sc01","get","query","Q");
		assertEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @Query on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TA {

		@RestMethod
		public void ta01(
			@Query(
				name="Q",
				description= {"a","b"},
				type="string"
			)
			String q) {}

		@RestMethod
		public void ta02(
			@Query(
				name="Q",
				api={
					"description: 'a\nb',",
					"type:'string'"
				}
			)
			String q) {}

		@RestMethod
		public void ta03(
			@Query(
				name="Q",
				api={
					"description: 'b\nc',",
					"type:'string'"
				},
				description= {"a","b"},
				type="string"
			)
			String q) {}

		@RestMethod
		public void ta04(@Query("Q") String q) {}
	}

	static Swagger ta = getSwagger(TA.class);

	@Test
	public void ta01_Query_onParameter_basic() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta01","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta02_Query_onParameter_api() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta02","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta03_Query_onParameter_mixed() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta03","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta04_Query_onParameter_value() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta04","get","query","Q");
		assertEquals("Q", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TB {

		@RestMethod
		public void tb01(@Query(name="Q") String q) {}
	}

	static Swagger tb = getSwagger(TB.class);

	@Test
	public void tb01_Query_onParameter_schemaValue() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb01","get","query","Q");
		assertObjectEquals("{'in':'query',name:'Q',type:'string'}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TC {

		@RestMethod
		public void tc01(@Query(name="Q", example={"a","b"}) String q) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_Query_onParameter_example() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/tc01","get","query","Q");
		assertEquals("a\nb", x.getExample());
	}
}
