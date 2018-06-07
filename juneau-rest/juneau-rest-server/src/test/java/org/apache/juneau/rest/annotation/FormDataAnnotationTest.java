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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.Items;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @FormData annotation.
 */
@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormDataAnnotationTest {
	
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

	@RestResource(parsers=UrlEncodingParser.class)
	public static class A {
		@RestMethod(name=POST)
		public String post(RestRequest req, @FormData("p1") String p1, @FormData("p2") int p2) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"],p2=["+p2+","+req.getFormData().getString("p2")+","+f.get("p2", int.class)+"]";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
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
	// Plain parameters
	//=================================================================================================================

	@RestResource
	public static class B {
		@RestMethod(name=POST)
		public String post(RestRequest req, @FormData(value="p1",parser=SimplePartParser.class) String p1) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"]";
		}
	}
	static MockRest b = MockRest.create(B.class);
	
	@Test
	public void b01() throws Exception {
		b.post("", "p1=p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1]");
		b.post("", "p1='p1'").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=['p1','p1',p1]");
	}
	
	//=================================================================================================================
	// Default values.
	//=================================================================================================================

	@RestResource
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
	static MockRest c = MockRest.create(C.class);

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
	// @FormData on POJO
	//=================================================================================================================
	
	@RestResource()
	public static class SA {

		@FormData(
			name="F",
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
			_enum=" a,b ",
			items=@Items(type="a"),
			example="a"
		)
		public static class SA00 {
			public SA00(String x) {}
		}
		
		@RestMethod(name=GET,path="/basic")
		public void sa00(SA00 f) {}

		@FormData("F")
		public static class SA02 {}
		
		@RestMethod(name=GET,path="/value")
		public void sa02(SA02 f) {}
		
		@FormData(name="F", schema=@Schema(" type:'b' "))
		public static class SA20b {}
		
		@RestMethod(name=GET,path="/schema2")
		public void sa20b(SA20b f) {}

		@FormData("F")
		public static class SA20c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sa20c(SA20c f) {}

		@FormData("F")
		public static class SA20d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sa20d(SA20d f) {}

		@FormData("F")
		public static class SA20e {}

		@RestMethod(name=GET,path="/schema5")
		public void sa20e(SA20e f) {}

		@FormData(name="F", _default={"a","b"})
		public static class SA23 {}
		
		@RestMethod(name=GET,path="/_default2")
		public void sa23(SA23 f) {}

		@FormData(name="F", _enum={ "['a','b']" })
		public static class SA25 {}
		
		@RestMethod(name=GET,path="/_enum2")
		public void sa25(SA25 f) {}

		@FormData(name="F", items=@Items(" type:'b' "))
		public static class SA27 {}
		
		@RestMethod(name=GET,path="/items2")
		public void sa27(SA27 f) {}

		@FormData(name="F", example={"{f1:'a'}"})
		public static class SA29 {
			public String f1;
		}
		
		@RestMethod(name=GET,path="/example2")
		public void sa29(SA29 f) {}
	}
	
	@Test
	public void sa00_FormData_onPojo_basic() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getParameter("formData", "F");
		assertEquals("F", x.getName());
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
	public void sa02_FormData_onPojo_value() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getParameter("formData", "F");
		assertEquals("F", x.getName());
	}
	@Test
	public void sa20b_FormData_onPojo_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema2").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sa20c_FormData_onPojo_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema3").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sa20d_FormData_onPojo_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema4").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sa20e_FormData_onPojo_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema5").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sa23_FormData_onPojo__default2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_default2").get("get").getParameter("formData", "F");
		assertEquals("a\nb", x.getDefault());
	}
	@Test
	public void sa25_FormData_onPojo__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_enum2").get("get").getParameter("formData", "F");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sa27_FormData_onPojo_items2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/items2").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sa29_FormData_onPojo_example2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/example2").get("get").getParameter("formData", "F");
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
	
	//=================================================================================================================
	// @FormData on parameter
	//=================================================================================================================

	@RestResource()
	public static class SB {

		@RestMethod(name=GET,path="/basic")
		public void sb00(
			@FormData(
				name="F",
				description={"a","b"},
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
				example="'a'"
			) String f) {}

		@RestMethod(name=GET,path="/value")
		public void sb02(@FormData("F") String f) {}

		@RestMethod(name=GET,path="/schema2")
		public void sb21b(@FormData(name="F", schema=@Schema(" type:'b' ")) String f) {}

		public static class SB21c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sb21c(@FormData("F") SB21c b) {}

		public static class SB21d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sb21d(@FormData("F") SB21d b) {}

		public static class SB21e {}

		@RestMethod(name=GET,path="/schema5")
		public void sb21e(@FormData("F") SB21e b) {}

		@RestMethod(name=GET,path="/schema6")
		public void sb21f(@FormData("F") Integer b) {}

		@RestMethod(name=GET,path="/schema7")
		public void sb21g(@FormData("F") Boolean b) {}
		
		@RestMethod(name=GET,path="/_default2")
		public void sb24(@FormData(name="F", _default={"a","b"}) String f) {}

		@RestMethod(name=GET,path="/_enum2")
		public void sb26(@FormData(name="F", _enum={" ['a','b'] "}) String f) {}

		@RestMethod(name=GET,path="/items2")
		public void sb28(@FormData(name="F", items=@Items(" type:'b' ")) String f) {}

		@RestMethod(name=GET,path="/example2")
		public void sb30(@FormData(name="F", example="{f1:'a'}") String f) {}
	}

	@Test
	public void sb00_FormData_onParameter_basic() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getParameter("formData", "F");
		assertEquals("F", x.getName());
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
	public void sb02_FormData_onParameter_value() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getParameter("formData", "F");
		assertEquals("F", x.getName());
	}
	@Test
	public void sb21b_FormData_onParameter_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema2").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb21c_FormData_onParameter_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema3").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb21d_FormData_onParameter_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema4").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb21e_FormData_onParameter_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema5").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb21f_FormData_onParameter_schema6() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema6").get("get").getParameter("formData", "F");
		assertObjectEquals("{format:'int32',type:'integer'}", x.getSchema());
	}
	@Test
	public void sb21g_FormData_onParameter_schema7() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema7").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'boolean'}", x.getSchema());
	}
	@Test
	public void sb24_FormData_onParameter__default2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_default2").get("get").getParameter("formData", "F");
		assertEquals("a\nb", x.getDefault());
	}
	@Test
	public void sb26_FormData_onParameter__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_enum2").get("get").getParameter("formData", "F");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sb28_FormData_onParameter_items2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/items2").get("get").getParameter("formData", "F");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sb30_FormData_onParameter_example2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/example2").get("get").getParameter("formData", "F");
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
}
