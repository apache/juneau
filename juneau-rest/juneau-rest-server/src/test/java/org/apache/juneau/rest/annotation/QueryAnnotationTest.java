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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.Items;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @Query annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("javadoc")
public class QueryAnnotationTest {
	
	//=================================================================================================================
	// Setup
	//=================================================================================================================

	private static Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		RestRequest req = rc.getCallHandler().createRequest(new MockServletRequest());
		RestInfoProvider ip = rc.getInfoProvider();
		return ip.getSwagger(req);
	}

	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(name=GET)
		public String get(RestRequest req, @Query("p1") String p1, @Query("p2") int p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"],p2=["+p2+","+q.getString("p2")+","+q.get("p2", int.class)+"]";
		}
		@RestMethod(name=POST)
		public String post(RestRequest req, @Query("p1") String p1, @Query("p2") int p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"],p2=["+p2+","+q.getString("p2")+","+q.get("p2", int.class)+"]";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
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
	// Plain parameters
	//=================================================================================================================

	@RestResource
	public static class B {
		@RestMethod(name=GET)
		public String get(RestRequest req, @Query(value="p1",parser=SimplePartParser.class) String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
		@RestMethod(name=POST)
		public String post(RestRequest req, @Query(value="p1",parser=SimplePartParser.class) String p1) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+req.getQuery().getString("p1")+","+q.get("p1", String.class)+"]";
		}
	}
	static MockRest b = MockRest.create(B.class);
	
	@Test
	public void b01_get() throws Exception {
		b.get("?p1=p1").execute().assertBody("p1=[p1,p1,p1]");
		b.get("?p1='p1'").execute().assertBody("p1=['p1','p1',p1]");
	}
	@Test
	public void b02_post() throws Exception {
		b.post("?p1=p1", null).execute().assertBody("p1=[p1,p1,p1]");
		b.post("?p1='p1'", null).execute().assertBody("p1=['p1','p1',p1]");
	}
	
	//=================================================================================================================
	// Multipart parameters (e.g. &key=val1,&key=val2).
	//=================================================================================================================

	@RestResource(serializers=JsonSerializer.Simple.class)
	public static class C {
		public static class C01 {
			public String a;
			public int b;
			public boolean c;
		}
		
		@RestMethod(name=GET, path="/StringArray")
		public Object c01(@Query(value="x",multipart=true) String[] x) {
			return x;
		}
		@RestMethod(name=GET, path="/intArray")
		public Object c02(@Query(value="x",multipart=true) int[] x) {
			return x;
		}
		@RestMethod(name=GET, path="/ListOfStrings")
		public Object c03(@Query(value="x",multipart=true) List<String> x) {
			return x;
		}
		@RestMethod(name=GET, path="/ListOfIntegers")
		public Object c04(@Query(value="x",multipart=true) List<Integer> x) {
			return x;
		}
		@RestMethod(name=GET, path="/BeanArray")
		public Object c05(@Query(value="x",multipart=true) C01[] x) {
			return x;
		}
		@RestMethod(name=GET, path="/ListOfBeans")
		public Object c06(@Query(value="x",multipart=true) List<C01> x) {
			return x;
		}
	}
	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_StringArray() throws Exception {
		c.get("/StringArray?x=a").execute().assertBody("['a']");
		c.get("/StringArray?x=a&x=b").execute().assertBody("['a','b']");
	}
	@Test
	public void c02_intArray() throws Exception {
		c.get("/intArray?x=1").execute().assertBody("[1]");
		c.get("/intArray?x=1&x=2").execute().assertBody("[1,2]");
	}
	@Test
	public void c03_ListOfStrings() throws Exception {
		c.get("/ListOfStrings?x=a").execute().assertBody("['a']");
		c.get("/ListOfStrings?x=a&x=b").execute().assertBody("['a','b']");
	}
	@Test
	public void c04_ListOfIntegers() throws Exception {
		c.get("/ListOfIntegers?x=1").execute().assertBody("[1]");
		c.get("/ListOfIntegers?x=1&x=2").execute().assertBody("[1,2]");
	}
	@Test
	public void c05_BeanArray() throws Exception {
		c.get("/BeanArray?x=(a=1,b=2,c=false)").execute().assertBody("[{a:'1',b:2,c:false}]");
		c.get("/BeanArray?x=(a=1,b=2,c=false)&x=(a=3,b=4,c=true)").execute().assertBody("[{a:'1',b:2,c:false},{a:'3',b:4,c:true}]");
	}
	@Test
	public void c06_ListOfBeans() throws Exception {
		c.get("/ListOfBeans?x=(a=1,b=2,c=false)").execute().assertBody("[{a:'1',b:2,c:false}]");
		c.get("/ListOfBeans?x=(a=1,b=2,c=false)&x=(a=3,b=4,c=true)").execute().assertBody("[{a:'1',b:2,c:false},{a:'3',b:4,c:true}]");
	}
	
	//=================================================================================================================
	// Default values.
	//=================================================================================================================
	
	@RestResource
	public static class D {
		@RestMethod(name=GET, path="/defaultQuery", defaultQuery={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap d01(RequestQuery query) {
			return new ObjectMap()
				.append("f1", query.getString("f1"))
				.append("f2", query.getString("f2"))
				.append("f3", query.getString("f3"));
		}
		@RestMethod(name=GET, path="/annotatedQuery")
		public ObjectMap d02(@Query("f1") String f1, @Query("f2") String f2, @Query("f3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=GET, path="/annotatedQueryDefault")
		public ObjectMap d03(@Query(value="f1",_default="1") String f1, @Query(value="f2",_default="2") String f2, @Query(value="f3",_default="3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=GET, path="/annotatedAndDefaultQuery", defaultQuery={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap d04(@Query(value="f1",_default="4") String f1, @Query(value="f2",_default="5") String f2, @Query(value="f3",_default="6") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}
	static MockRest d = MockRest.create(D.class);

	@Test
	public void d01_defaultQuery() throws Exception {
		d.get("/defaultQuery").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		d.get("/defaultQuery").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d02_annotatedQuery() throws Exception {
		d.get("/annotatedQuery").execute().assertBody("{f1:null,f2:null,f3:null}");
		d.get("/annotatedQuery").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d03_annotatedQueryDefault() throws Exception {
		d.get("/annotatedQueryDefault").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		d.get("/annotatedQueryDefault").query("f1",4).query("f2",5).query("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void d04_annotatedAndDefaultQuery() throws Exception {
		d.get("/annotatedAndDefaultQuery").execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
		d.get("/annotatedAndDefaultQuery").query("f1",7).query("f2",8).query("f3",9).execute().assertBody("{f1:'7',f2:'8',f3:'9'}");
	}
	
	//=================================================================================================================
	// @Query on POJO
	//=================================================================================================================
	
	@RestResource()
	public static class SA {

		@Query(
			name="Q", 
			description= {"a","b"},
			required="true",
			type="a",
			format="a",
			pattern="a",
			collectionFormat="a",
			maximum="1",
			minimum="1",
			multipleOf="1",
			maxLength="1",
			minLength="1",
			maxItems="1",
			minItems="1",
			allowEmptyValue="true",
			exclusiveMaximum="true",
			exclusiveMinimum="true",
			uniqueItems="true",
			schema=@Schema(type="a"),
			_default="a",
			_enum="a, b",
			items=@Items(type="a"),
			example="'a'"
		)
		public static class SA00 {
			public SA00(String x) {}
		}
		
		@RestMethod(name=GET, path="/basic")
		public void sa00(SA00 q) {}

		@Query("Q")
		public static class SA01 {}
		
		@RestMethod(name=GET,path="/value")
		public void sa01(SA01 q) {}

		@Query(name="Q", schema=@Schema(" type:'b' "))
		public static class SA19b {}
		
		@RestMethod(name=GET,path="/schema2")
		public void sa19b(SA19b q) {}

		@Query("Q")
		public static class SA19c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sa19c(SA19c q) {}

		@Query("Q")
		public static class SA19d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sa19d(SA19d q) {}

		@Query("Q")
		public static class SA19e {}

		@RestMethod(name=GET,path="/schema5")
		public void sa19e(SA19e q) {}
		
		@Query(name="Q", _default={"a","b"})
		public static class SA21b {}
		
		@RestMethod(name=GET,path="/_default2")
		public void sa21b(SA21b q) {}

		@Query(name="Q", _enum={" ['a','b'] "})
		public static class SA22b {}
		
		@RestMethod(name=GET,path="/_enum2")
		public void sa22b(SA22b q) {}

		@Query(name="Q", items=@Items(" type: 'b' "))
		public static class SA23b {}
		
		@RestMethod(name=GET,path="/items2")
		public void sa23b(SA23b q) {}

		@Query(name="Q", example={"{f1:'a'}"})
		public static class SA24b {
			public String f1;
		}
		
		@RestMethod(name=GET,path="/example2")
		public void sa24b(SA24b q) {}
	}
	
	@Test
	public void sa00a_Query_onPojo_basic() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getParameter("query", "Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getPattern());
		assertEquals("a", x.getCollectionFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertEquals("a", x.getDefault());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertObjectEquals("'a'", x.getExample());
	}
	@Test
	public void sa01_Query_onPojo_value() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getParameter("query", "Q");
		assertEquals("Q", x.getName());
	}
	@Test
	public void sa19b_Query_onPojo_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema2").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sa19c_Query_onPojo_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema3").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sa19d_Query_onPojo_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema4").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sa19e_Query_onPojo_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema5").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sa21b_Query_onPojo_default2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_default2").get("get").getParameter("query", "Q");
		assertEquals("a\nb", x.getDefault());
	}
	@Test
	public void sa22b_Query_onPojo_enum2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_enum2").get("get").getParameter("query", "Q");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sa23b_Query_onPojo_items2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/items2").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sa24b_Query_onPojo_example2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/example2").get("get").getParameter("query", "Q");
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
		
	//=================================================================================================================
	// @Query on parameter
	//=================================================================================================================

	@RestResource()
	public static class SB {
		
		@RestMethod(name=GET,path="/basic")
		public void sb00(
			@Query(
				name="Q",
				description= {"a","b"},
				required="true",
				type="a",
				format="a",
				pattern="a",
				collectionFormat="a",
				maximum="1",
				minimum="1",
				multipleOf="1",
				maxLength="1",
				minLength="1",
				maxItems="1",
				minItems="1",
				allowEmptyValue="true",
				exclusiveMaximum="true",
				exclusiveMinimum="true",
				uniqueItems="true",
				schema=@Schema(type="a"),
				_default="a",
				_enum="a,b",
				items=@Items(type="a"),
				example="a"
			) 
			String q) {}

		@RestMethod(name=GET,path="/value")
		public void sb02(@Query("Q") String q) {}

		@RestMethod(name=GET,path="/schema2")
		public void sb19b(@Query(name="Q", schema=@Schema( " type: 'b' ")) String q) {}

		@RestMethod(name=GET,path="/_default2")
		public void sb20b(@Query(name="Q", _default={"a","b"}) String q) {}

		@RestMethod(name=GET,path="/_enum2")
		public void sb21b(@Query(name="Q", _enum= {" ['a','b'] "}) String q) {}

		@RestMethod(name=GET,path="/items2")
		public void sb22b(@Query(name="Q", items=@Items(" type:'b' ")) String q) {}

		@RestMethod(name=GET,path="/example2")
		public void sb23b(@Query(name="Q", example={"a","b"}) String q) {}
	}
	
	@Test
	public void sb00_Query_onParameter_basic() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getParameter("query", "Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getPattern());
		assertEquals("a", x.getCollectionFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertEquals("a", x.getDefault());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertEquals("a", x.getExample());
	}
	@Test
	public void sb02_Query_onParameter_value() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getParameter("query", "Q");
		assertEquals("Q", x.getName());
	}
	@Test
	public void sb19b_Query_onParameter_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema2").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb20b_Query_onParameter__default2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_default2").get("get").getParameter("query", "Q");
		assertEquals("a\nb", x.getDefault());
	}
	@Test
	public void sb21b_Query_onParameter__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_enum2").get("get").getParameter("query", "Q");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sb22b_Query_onParameter_items2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/items2").get("get").getParameter("query", "Q");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sb23b_Query_onParameter_example2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/example2").get("get").getParameter("query", "Q");
		assertEquals("a\nb", x.getExample());
	}
}
