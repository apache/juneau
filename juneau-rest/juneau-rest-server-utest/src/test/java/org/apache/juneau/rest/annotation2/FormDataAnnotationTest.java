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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @FormData annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormDataAnnotationTest {

	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

	@Rest(parsers=UrlEncodingParser.class)
	public static class A {
		@RestMethod
		public String post(RestRequest req, @FormData(name="p1",allowEmptyValue=true) String p1, @FormData(name="p2",allowEmptyValue=true) int p2) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"],p2=["+p2+","+req.getFormData().getString("p2")+","+f.get("p2", int.class)+"]";
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01() throws Exception {
		a.post("", "p1=p1&p2=2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1],p2=[2,2,2]");
		a.post("", "p1&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("", "p1=&p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,,0]");
		a.post("", "").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("", "p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("", "p1=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,null,0]");
		a.post("", "p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.post("", "p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,,0]");
		a.post("", "p1=foo&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[foo,foo,foo],p2=[0,null,0]");
		a.post("", "p1&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("", "p1="+x+"&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}

	//=================================================================================================================
	// UON parameters
	//=================================================================================================================

	@Rest
	public static class B {
		@RestMethod(name=POST,path="/post1")
		public String post1(RestRequest req, @FormData(value="p1") String p1) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"]";
		}
		@RestMethod(name=POST,path="/post2")
		public String post2(RestRequest req, @FormData(value="p1",format="uon") String p1) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"]";
		}
	}
	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01() throws Exception {
		b.post("/post1", "p1=p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1]");
		b.post("/post1", "p1='p1'").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=['p1','p1','p1']");
	}
	@Test
	public void b02() throws Exception {
		b.post("/post2", "p1=p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1]");
		b.post("/post2", "p1='p1'").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,'p1','p1']");
	}

	//=================================================================================================================
	// Default values.
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod(name=POST, path="/defaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap c01(RequestFormData formData) {
			return new ObjectMap()
				.append("f1", formData.getString("f1"))
				.append("f2", formData.getString("f2"))
				.append("f3", formData.getString("f3"));
		}
		@RestMethod(name=POST, path="/annotatedFormData")
		public ObjectMap c02(@FormData("f1") String f1, @FormData("f2") String f2, @FormData("f3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=POST, path="/annotatedFormDataDefault")
		public ObjectMap c03(@FormData(value="f1",_default="1") String f1, @FormData(value="f2",_default="2") String f2, @FormData(value="f3",_default="3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=POST, path="/annotatedAndDefaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap c04(@FormData(value="f1",_default="4") String f1, @FormData(value="f2",_default="5") String f2, @FormData(value="f3",_default="6") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}
	static MockRest c = MockRest.build(C.class, null);

	@Test
	public void c01_defaultFormData() throws Exception {
		c.post("/defaultFormData", null).contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		c.post("/defaultFormData", null).contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c02_annotatedFormData() throws Exception {
		c.post("/annotatedFormData", null).contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:null,f2:null,f3:null}");
		c.post("/annotatedFormData", null).contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c03_annotatedFormDataDefault() throws Exception {
		c.post("/annotatedFormDataDefault", null).contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		c.post("/annotatedFormDataDefault", null).contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c04_annotatedAndDefaultFormData() throws Exception {
		c.post("/annotatedAndDefaultFormData", null).contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
		c.post("/annotatedAndDefaultFormData", null).contentType("application/x-www-form-urlencoded").formData("f1",7).formData("f2",8).formData("f3",9).execute().assertBody("{f1:'7',f2:'8',f3:'9'}");
	}

	//=================================================================================================================
	// Optional form data parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class D {
		@RestMethod(name=POST,path="/a")
		public Object a(@FormData("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=POST,path="/b")
		public Object b(@FormData("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=POST,path="/c")
		public Object c(@FormData("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=POST,path="/d")
		public Object d(@FormData("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}
	static MockRest d = MockRest.create(D.class).accept("application/json").contentType("application/x-www-form-urlencoded").build();

	@Test
	public void d01_optionalParam_integer() throws Exception {
		d.post("/a", "f1=123").execute().assertStatus(200).assertBody("123");
		d.post("/a", "null").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void d02_optionalParam_bean() throws Exception {
		d.post("/b", "f1=(a=1,b=foo)").execute().assertStatus(200).assertBody("{a:1,b:'foo'}");
		d.post("/b", "null").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void d03_optionalParam_listOfBeans() throws Exception {
		d.post("/c", "f1=@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
		d.post("/c", "null").execute().assertStatus(200).assertBody("null");
	}

	@Test
	public void d04_optionalParam_listOfOptionals() throws Exception {
		d.post("/d", "f1=@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
		d.post("/d", "null").execute().assertStatus(200).assertBody("null");
	}


	//=================================================================================================================
	// @FormData on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@FormData(
			name="F",
			description= {"a","b"},
			type="string"
		)
		public static class SA01 {
			public SA01(String x) {}
		}
		@RestMethod
		public void sa01(SA01 f) {}

		@FormData(
			name="F",
			api={
				"description:'a\nb',",
				"type:'string'"
			}
		)
		public static class SA02 {
			public SA02(String x) {}
		}
		@RestMethod
		public void sa02(SA02 f) {}

		@FormData(
			name="F",
			api={
				"description:'b\nc',",
				"type:'string'"
			},
			description= {"a","b"},
			type="string"
		)
		public static class SA03 {
			public SA03(String x) {}
		}
		@RestMethod
		public void sa03(SA03 f) {}

		@FormData("F")
		public static class SA04 {}
		@RestMethod
		public void sa04(SA04 f) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_FormData_onPojo_basic() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa01","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa02_FormData_onPojo_api() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa02","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa03_FormData_onPojo_mixed() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa03","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa04_FormData_onPojo_value() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa04","get","formData","F");
		assertEquals("F", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SB {

		@FormData(name="F")
		public static class SB01 {}
		@RestMethod
		public void sb01(SB01 f) {}

		@FormData("F")
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02(SB02 f) {}

		@FormData("F")
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03(SB03 f) {}

		@FormData("F")
		public static class SB04 {}
		@RestMethod
		public void sb04(SB04 f) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_FormData_onPojo_value() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb01","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'string'}", x);
	}
	@Test
	public void sb02_FormData_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb02","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}", x);
	}
	@Test
	public void sb03_FormData_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb03","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'array',items:{type:'string'}}", x);
	}
	@Test
	public void sb04_FormData_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb04","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'string'}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SC {

		@FormData(name="F", example={"{f1:'a'}"})
		public static class SC01 {
			public String f1;
		}
		@RestMethod
		public void sc01(SC01 f) {}
	}

	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01_FormData_onPojo_example() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/sc01","get","formData","F");
		assertEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @FormData on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TA {

		@RestMethod
		public void ta01(
			@FormData(
				name="F",
				description={"a","b"},
				type="string"
			) String f) {}

		@RestMethod
		public void ta02(
			@FormData(
				name="F",
				api={
					"description:'a\nb',",
					"type:'string'",
				}
			) String f) {}

		@RestMethod
		public void ta03(
			@FormData(
				name="F",
				api={
					"description:'b\nc',",
					"type:'string'",
				},
				description={"a","b"},
				type="string"
			) String f) {}

		@RestMethod
		public void ta04(@FormData("F") String f) {}
	}

	static Swagger ta = getSwagger(TA.class);

	@Test
	public void ta01_FormData_onParameter_basic() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta01","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta02_FormData_onParameter_api() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta02","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta03_FormData_onParameter_mixed() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta03","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta04_FormData_onParameter_value() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta04","get","formData","F");
		assertEquals("F", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TB {

		@RestMethod
		public void tb01(@FormData(name="F") String f) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod
		public void tb02(@FormData("F") TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void tb03(@FormData("F") TB03 b) {}

		public static class TB04 {}
		@RestMethod
		public void tb04(@FormData("F") TB04 b) {}

		@RestMethod
		public void tb05(@FormData("F") Integer b) {}

		@RestMethod
		public void tb06(@FormData("F") Boolean b) {}
	}

	static Swagger tb = getSwagger(TB.class);

	@Test
	public void tb01_FormData_onParameter_schemaValue() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb01","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'string'}", x);
	}
	@Test
	public void tb02_FormData_onParameter_autoDetectBean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb02","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}", x);
	}
	@Test
	public void tb03_FormData_onParameter_autoDetectList() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb03","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'array',items:{type:'string'}}", x);
	}
	@Test
	public void tb04_FormData_onParameter_autoDetectStringObject() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb04","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'string'}", x);
	}
	@Test
	public void tb05_FormData_onParameter_autoDetectInteger() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb05","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'integer',format:'int32'}", x);
	}
	@Test
	public void tb06_FormData_onParameter_autoDetectBoolean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb06","get","formData","F");
		assertObjectEquals("{'in':'formData',name:'F',type:'boolean'}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TC {

		@RestMethod
		public void tc01(@FormData(name="F", example="{f1:'a'}") String f) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_FormData_onParameter_example() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/tc01","get","formData","F");
		assertEquals("{f1:'a'}", x.getExample());
	}
}
