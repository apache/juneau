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
package org.apache.juneau.rest.test.client;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RequestBeanProxyTest {

	//=================================================================================================================
	// @Query
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(name=GET, path="/echoQuery")
		public String echoQuery(RestRequest req) throws Exception {
			return req.getQuery().toString(true);
		}
	}

	//=================================================================================================================
	// @Query - Simple values
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A01_RemoteResource {

		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A01_BeanImpl rb);

		@RemoteMethod(method="GET", path="/echoQuery")
		String serialized(@Request(partSerializer=XSerializer.class) A01_BeanImpl rb);
	}

	public static interface A01_BeanInterface {
		@Query String getA();
		@Query("b") String getX1();
		@Query(name="c") String getX2();
		@Query(name="e",allowEmptyValue=true) String getX4();
		@Query("f") String getX5();
		@Query("g") String getX6();
		@Query("h") String getX7();
	}

	public static class A01_BeanImpl implements A01_BeanInterface {
		@Override public String getA() { return "a1"; }
		@Override public String getX1() { return "b1"; }
		@Override public String getX2() { return "c1"; }
		@Override public String getX4() { return ""; }
		@Override public String getX5() { return null; }
		@Override public String getX6() { return "true"; }
		@Override public String getX7() { return "123"; }
	}

	static A01_RemoteResource a01a = MockRemoteResource.build(A01_RemoteResource.class, A.class, null);
	static A01_RemoteResource a01b = MockRestClient.create(A.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(A01_RemoteResource.class);

	@Test
	public void a01a_query_simpleVals_plainText() throws Exception {
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123'}", a01a.normal(new A01_BeanImpl()));
	}
	@Test
	public void a01b_query_simpleVals_uon() throws Exception {
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\''}", a01b.normal(new A01_BeanImpl()));
	}
	@Test
	public void a01c_query_simpleVals_x() throws Exception {
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x'}", a01b.serialized(new A01_BeanImpl()));
	}

	//=================================================================================================================
	// @Query - Maps
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A02_RemoteResource {

		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A02_Bean rb);

		@RemoteMethod(method="GET", path="/echoQuery")
		String serialized(@Request(partSerializer=XSerializer.class) A02_Bean rb);
	}

	public static class A02_Bean {
		@Query
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Query("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Query(name="*",allowEmptyValue=true)
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Query("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static A02_RemoteResource a02a = MockRemoteResource.build(A02_RemoteResource.class, A.class, null);
	static A02_RemoteResource a02b = MockRestClient.create(A.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(A02_RemoteResource.class);

	@Test
	public void a02a_query_maps_plainText() throws Exception {
		String r = a02a.normal(new A02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a02b_query_maps_uon() throws Exception {
		String r = a02b.normal(new A02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a02c_query_maps_x() throws Exception {
		String r = a02b.serialized(new A02_Bean());
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Query - NameValuePairs
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A03_RemoteResource {

		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A03_Bean rb);

		@RemoteMethod(method="GET", path="/echoQuery")
		String serialized(@Request(partSerializer=XSerializer.class) A03_Bean rb);
	}

	public static class A03_Bean {
		@Query(allowEmptyValue=true)
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Query("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Query(name="*",allowEmptyValue=true)
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Query("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static A03_RemoteResource a03a = MockRemoteResource.build(A03_RemoteResource.class, A.class, null);
	static A03_RemoteResource a03b = MockRestClient.create(A.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(A03_RemoteResource.class);

	@Test
	public void a03a_query_nameValuePairs_plainText() throws Exception {
		String r = a03a.normal(new A03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a03b_query_nameValuePairs_on() throws Exception {
		String r = a03b.normal(new A03_Bean());
		assertEquals("{a1:'v1',a2:'\\'123\\'',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'\\'123\\'',c4:''}", r);
	}
	@Test
	public void a03c_query_nameValuePairs_x() throws Exception {
		String r = a03b.serialized(new A03_Bean());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Query - CharSequence
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A04_RemoteResource {
		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A04_Bean rb);
	}

	public static class A04_Bean {
		@Query("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	static A04_RemoteResource a04a = MockRemoteResource.build(A04_RemoteResource.class, A.class, null);

	@Test
	public void a04a_query_charSequence() throws Exception {
		String r = a04a.normal(new A04_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @Query - Reader
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A05_RemoteResource {
		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A05_Bean rb);
	}

	public static class A05_Bean {
		@Query("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	static A05_RemoteResource a05a = MockRemoteResource.build(A05_RemoteResource.class, A.class, null);

	@Test
	public void a05a_query_reader() throws Exception {
		String r = a05a.normal(new A05_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @Query - Collections
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface A06_RemoteResource {

		@RemoteMethod(method="GET", path="/echoQuery")
		String normal(@Request A06_Bean rb);

		@RemoteMethod(method="GET", path="/echoQuery")
		String serialized(@Request(partSerializer=XSerializer.class) A06_Bean rb);
	}

	public static class A06_Bean {
		@Query
		public List<Object> getA() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Query("b")
		public List<Object> getX1() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Query(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Query(name="d",allowEmptyValue=true)
		public List<Object> getX3() {
			return new AList<>();
		}
		@Query("e")
		public List<Object> getX4() {
			return null;
		}
		@Query("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Query(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Query(name="h",allowEmptyValue=true)
		public Object[] getX7() {
			return new Object[]{};
		}
		@Query("i")
		public Object[] getX8() {
			return null;
		}
	}

	static A06_RemoteResource a06a = MockRemoteResource.build(A06_RemoteResource.class, A.class, null);
	static A06_RemoteResource a06b = MockRestClient.create(A.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(A06_RemoteResource.class);

	@Test
	public void a06a_query_collections_plainText() throws Exception {
		String r = a06a.normal(new A06_Bean());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}
	@Test
	public void a06b_query_collections_uon() throws Exception {
		String r = a06b.normal(new A06_Bean());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}
	@Test
	public void a06c_query_collections_x() throws Exception {
		String r = a06b.serialized(new A06_Bean());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'foo||true|123|null|true|123|null',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'foo||true|123|null|true|123|null',h:''}", r);
	}

	//=================================================================================================================
	// @FormData
	//=================================================================================================================

	@Rest(parsers=UrlEncodingParser.class)
	public static class C {
		@RestMethod(name=POST)
		public String echoFormData(RestRequest req) throws Exception {
			return req.getFormData().toString(true);
		}
	}

	//=================================================================================================================
	// @FormData, Simple values
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C01_RemoteResource {

		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C01_Bean rb);

		@RemoteMethod(method="POST", path="/echoFormData")
		String serialized(@Request(partSerializer=XSerializer.class) C01_Bean rb);
	}

	public static class C01_Bean {
		@FormData
		public String getA() {
			return "a1";
		}
		@FormData("b")
		public String getX1() {
			return "b1";
		}
		@FormData(name="c")
		public String getX2() {
			return "c1";
		}
		@FormData(name="e",allowEmptyValue=true)
		public String getX4() {
			return "";
		}
		@FormData("f")
		public String getX5() {
			return null;
		}
		@FormData("g")
		public String getX6() {
			return "true";
		}
		@FormData("h")
		public String getX7() {
			return "123";
		}
	}

	static C01_RemoteResource c01a = MockRemoteResource.build(C01_RemoteResource.class, C.class, null);
	static C01_RemoteResource c01b = MockRestClient.create(C.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(C01_RemoteResource.class);

	@Test
	public void c01a_formData_simpleVals_plainText() throws Exception {
		String r = c01a.normal(new C01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123'}", r);
	}
	@Test
	public void c01b_formData_simpleVals_uon() throws Exception {
		String r = c01b.normal(new C01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}
	@Test
	public void c01c_formData_simpleVals_x() throws Exception {
		String r = c01b.serialized(new C01_Bean());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	//=================================================================================================================
	// @FormData, Maps
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C02_RemoteResource {

		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C02_Bean rb);

		@RemoteMethod(method="POST", path="/echoFormData")
		String serialized(@Request(partSerializer=XSerializer.class) C02_Bean rb);
	}

	public static class C02_Bean {
		@FormData
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@FormData("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@FormData(name="*",allowEmptyValue=true)
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@FormData("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static C02_RemoteResource c02a = MockRemoteResource.build(C02_RemoteResource.class, C.class, null);
	static C02_RemoteResource c02b = MockRestClient.create(C.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(C02_RemoteResource.class);

	@Test
	public void c02a_formData_maps_plainText() throws Exception {
		String r = c02a.normal(new C02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c02b_formData_maps_uon() throws Exception {
		String r = c02b.normal(new C02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c02c_formData_maps_x() throws Exception {
		String r = c02b.serialized(new C02_Bean());
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @FormData, NameValuePairs
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C03_RemoteResource {

		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C03_Bean rb);

		@RemoteMethod(method="POST", path="/echoFormData")
		String serialized(@Request(partSerializer=XSerializer.class) C03_Bean rb);
	}

	public static class C03_Bean {
		@FormData
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@FormData("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@FormData(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@FormData("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static C03_RemoteResource c03a = MockRemoteResource.build(C03_RemoteResource.class, C.class, null);
	static C03_RemoteResource c03b = MockRestClient.create(C.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(C03_RemoteResource.class);

	@Test
	public void c03a_formData_nameValuePairs_plainText() throws Exception {
		String r = c03a.normal(new C03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c03b_formData_nameValuePairs_uon() throws Exception {
		String r = c03b.normal(new C03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c03c_formData_nameValuePairs_x() throws Exception {
		String r = c03b.serialized(new C03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	//=================================================================================================================
	// @FormData, CharSequence
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C04_RemoteResource {
		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C04_Bean rb);
	}

	public static class C04_Bean {
		@FormData("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	static C04_RemoteResource c04a = MockRemoteResource.build(C04_RemoteResource.class, C.class, null);

	@Test
	public void c04a_formDataCharSequence() throws Exception {
		String r = c04a.normal(new C04_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @FormData, Reader
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C05_RemoteResource {
		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C05_Bean rb);
	}

	public static class C05_Bean {
		@FormData("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	static C05_RemoteResource c05a = MockRemoteResource.build(C05_RemoteResource.class, C.class, null);

	@Test
	public void c05a_formDataReader() throws Exception {
		String r = c05a.normal(new C05_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @FormData, Collections
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface C06_RemoteResource {

		@RemoteMethod(method="POST", path="/echoFormData")
		String normal(@Request C06_Bean rb);

		@RemoteMethod(method="POST", path="/echoFormData")
		String serialized(@Request(partSerializer=XSerializer.class) C06_Bean rb);
	}

	public static class C06_Bean {
		@FormData
		public List<Object> getA() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@FormData("b")
		public List<Object> getX1() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@FormData(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@FormData(name="d",allowEmptyValue=true)
		public List<Object> getX3() {
			return new AList<>();
		}
		@FormData("e")
		public List<Object> getX4() {
			return null;
		}
		@FormData("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@FormData(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@FormData(name="h",allowEmptyValue=true)
		public Object[] getX7() {
			return new Object[]{};
		}
		@FormData("i")
		public Object[] getX8() {
			return null;
		}
	}

	static C06_RemoteResource c06a = MockRemoteResource.build(C06_RemoteResource.class, C.class, null);
	static C06_RemoteResource c06b = MockRestClient.create(C.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(C06_RemoteResource.class);

	@Test
	public void c06a_formData_collections_plainText() throws Exception {
		String r = c06a.normal(new C06_Bean());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}
	@Test
	public void c06b_formData_collections_uon() throws Exception {
		String r = c06b.normal(new C06_Bean());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}
	@Test
	public void c06c_formData_collections_x() throws Exception {
		String r = c06b.serialized(new C06_Bean());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'foo||true|123|null|true|123|null',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'foo||true|123|null|true|123|null',h:''}", r);
	}


	//=================================================================================================================
	// @Header
	//=================================================================================================================

	@Rest
	public static class E {
		@RestMethod(name=GET)
		public String echoHeaders(RestRequest req) throws Exception {
			return req.getHeaders().subset("a,b,c,d,e,f,g,h,i,a1,a2,a3,a4,b1,b2,b3,b4,c1,c2,c3,c4").toString(true);
		}
	}

	//=================================================================================================================
	// @Header, Simple values
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface E01_RemoteResource {

		@RemoteMethod(method="GET", path="/echoHeaders")
		String normal(@Request E01_Bean rb);

		@RemoteMethod(method="GET", path="/echoHeaders")
		String serialized(@Request(partSerializer=XSerializer.class) E01_Bean rb);
	}

	public static class E01_Bean {
		@Header
		public String getA() {
			return "a1";
		}
		@Header("b")
		public String getX1() {
			return "b1";
		}
		@Header(name="c")
		public String getX2() {
			return "c1";
		}
		@Header(name="e",allowEmptyValue=true)
		public String getX4() {
			return "";
		}
		@Header("f")
		public String getX5() {
			return null;
		}
		@Header("g")
		public String getX6() {
			return "true";
		}
		@Header("h")
		public String getX7() {
			return "123";
		}
	}

	static E01_RemoteResource e01a = MockRemoteResource.build(E01_RemoteResource.class, E.class, null);
	static E01_RemoteResource e01b = MockRestClient.create(E.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(E01_RemoteResource.class);

	@Test
	public void e01a_headerSimpleValsPlainText() throws Exception {
		String r = e01a.normal(new E01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123'}", r);
	}
	@Test
	public void e01b_headerSimpleValsUon() throws Exception {
		String r = e01b.normal(new E01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}
	@Test
	public void e01c_headerSimpleValsX() throws Exception {
		String r = e01b.serialized(new E01_Bean());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	//=================================================================================================================
	// @Header, Maps
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface E02_RemoteResource {

		@RemoteMethod(method="GET", path="/echoHeaders")
		String normal(@Request E02_Bean rb);

		@RemoteMethod(method="GET", path="/echoHeaders")
		String serialized(@Request(partSerializer=XSerializer.class) E02_Bean rb);
	}

	public static class E02_Bean {
		@Header
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Header("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Header(name="*",allowEmptyValue=true)
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Header("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static E02_RemoteResource e02a = MockRemoteResource.build(E02_RemoteResource.class, E.class, null);
	static E02_RemoteResource e02b = MockRestClient.create(E.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(E02_RemoteResource.class);

	@Test
	public void e02a_header_maps_plainText() throws Exception {
		String r = e02a.normal(new E02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e02b_header_maps_uon() throws Exception {
		String r = e02b.normal(new E02_Bean());
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e02c_header_maps_x() throws Exception {
		String r = e02b.serialized(new E02_Bean());
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Header, NameValuePairs
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface E03_RemoteResource {

		@RemoteMethod(method="GET", path="/echoHeaders")
		String normal(@Request E03_Bean rb);

		@RemoteMethod(method="GET", path="/echoHeaders")
		String serialized(@Request(partSerializer=XSerializer.class) E03_Bean rb);
	}

	public static class E03_Bean {
		@Header(allowEmptyValue=true)
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Header(value="*",allowEmptyValue=true)
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Header(name="*",allowEmptyValue=true)
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Header(value="*",allowEmptyValue=true)
		public NameValuePairs getD() {
			return null;
		}
	}

	static E03_RemoteResource e03a = MockRemoteResource.build(E03_RemoteResource.class, E.class, null);
	static E03_RemoteResource e03b = MockRestClient.create(E.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(E03_RemoteResource.class);

	@Test
	public void e03a_header_nameValuePairs_plainText() throws Exception {
		String r = e03a.normal(new E03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e03b_header_nameValuePairs_uon() throws Exception {
		String r = e03b.normal(new E03_Bean());
		assertEquals("{a1:'v1',a2:'\\'123\\'',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'\\'123\\'',c4:''}", r);
	}
	@Test
	public void e03c_header_nameValuePairs_x() throws Exception {
		String r = e03b.serialized(new E03_Bean());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Header, Collections
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface E04_RemoteResource {

		@RemoteMethod(method="GET", path="/echoHeaders")
		String normal(@Request E04_Bean rb);

		@RemoteMethod(method="GET", path="/echoHeaders")
		String serialized(@Request(partSerializer=XSerializer.class) E04_Bean rb);
	}

	public static class E04_Bean {
		@Header
		public List<Object> getA() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Header("b")
		public List<Object> getX1() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Header(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Header(name="d",allowEmptyValue=true)
		public List<Object> getX3() {
			return new AList<>();
		}
		@Header("e")
		public List<Object> getX4() {
			return null;
		}
		@Header("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Header(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Header(name="h",allowEmptyValue=true)
		public Object[] getX7() {
			return new Object[]{};
		}
		@Header("i")
		public Object[] getX8() {
			return null;
		}
	}

	static E04_RemoteResource e04a = MockRemoteResource.build(E04_RemoteResource.class, E.class, null);
	static E04_RemoteResource e04b = MockRestClient.create(E.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(E04_RemoteResource.class);

	@Test
	public void e04a_header_collections_plainText() throws Exception {
		String r = e04a.normal(new E04_Bean());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}
	@Test
	public void e04b_header_collections_uon() throws Exception {
		String r = e04b.normal(new E04_Bean());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}
	@Test
	public void e04c_header_collections_x() throws Exception {
		String r = e04b.serialized(new E04_Bean());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'foo||true|123|null|true|123|null',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'foo||true|123|null|true|123|null',h:''}", r);
	}

	//=================================================================================================================
	// @Path
	//=================================================================================================================

	@Rest
	public static class G  {
		@RestMethod(name=GET,path="/*")
		public String echoPath(RestRequest req) throws Exception {
			return req.getPathMatch().getRemainder();
		}
	}

	//=================================================================================================================
	// @Path, Simple values
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface G01_RemoteResource {

		@RemoteMethod(method="GET", path="/echoPath/{a}/{b}/{c}/{e}/{g}/{h}")
		String normal(@Request G01_Bean rb);

		@RemoteMethod(method="GET", path="/echoPath/{a}/{b}/{c}/{e}/{g}/{h}")
		String serialized(@Request(partSerializer=XSerializer.class) G01_Bean rb);
	}

	public static class G01_Bean {
		@Path
		public String getA() {
			return "a1";
		}
		@Path("b")
		public String getX1() {
			return "b1";
		}
		@Path(name="c")
		public String getX2() {
			return "c1";
		}
		@Path(name="e",allowEmptyValue=true)
		public String getX4() {
			return "";
		}
		@Path("g")
		public String getX6() {
			return "true";
		}
		@Path("h")
		public String getX7() {
			return "123";
		}
	}

	static G01_RemoteResource g01a = MockRemoteResource.build(G01_RemoteResource.class, G.class, null);
	static G01_RemoteResource g01b = MockRestClient.create(G.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(G01_RemoteResource.class);

	@Test
	public void g01a_pathSimpleValsPlainText() throws Exception {
		String r = g01a.normal(new G01_Bean());
		assertEquals("echoPath/a1/b1/c1//true/123", r);
	}
	@Test
	public void g01b_pathSimpleValsUon() throws Exception {
		String r = g01b.normal(new G01_Bean());
		assertEquals("echoPath/a1/b1/c1//'true'/'123'", r);
	}
	@Test
	public void g01c_pathSimpleValsX() throws Exception {
		String r = g01b.serialized(new G01_Bean());
		assertEquals("echoPath/xa1x/xb1x/xc1x/xx/xtruex/x123x", r);
	}

	//=================================================================================================================
	// @Path, Maps
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface G02_RemoteResource {

		@RemoteMethod(method="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String normal(@Request G02_Bean rb);

		@RemoteMethod(method="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String serialized(@Request(partSerializer=XSerializer.class) G02_Bean rb);
	}

	public static class G02_Bean {
		@Path(name="*",allowEmptyValue=true)
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Path("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Path(name="*",allowEmptyValue=true)
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Path("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static G02_RemoteResource g02a = MockRemoteResource.build(G02_RemoteResource.class, G.class, null);
	static G02_RemoteResource g02b = MockRestClient.create(G.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(G02_RemoteResource.class);

	@Test
	public void g02a_path_maps_plainText() throws Exception {
		String r = g02a.normal(new G02_Bean());
		assertEquals("echoPath/v1/123/null//true/123/null/v1/123/null/", r);
	}
	@Test
	public void g02b_path_maps_uon() throws Exception {
		String r = g02b.normal(new G02_Bean());
		assertEquals("echoPath/v1/123/null//'true'/'123'/'null'/v1/123/null/", r);
	}
	@Test
	public void g02c_path_maps_x() throws Exception {
		String r = g02b.serialized(new G02_Bean());
		assertEquals("echoPath/xv1x/x123x/NULL/xx/xtruex/x123x/xnullx/xv1x/x123x/NULL/xx", r);
	}

	//=================================================================================================================
	// @Path, NameValuePairs
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface G03_RemoteResource {

		@RemoteMethod(method="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String normal(@Request G03_Bean rb);

		@RemoteMethod(method="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String serialized(@Request(partSerializer=XSerializer.class) G03_Bean rb);
	}

	public static class G03_Bean {
		@Path(name="*",allowEmptyValue=true)
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Path("/*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Path(name="*",allowEmptyValue=true)
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Path("/*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static G03_RemoteResource g03a = MockRemoteResource.build(G03_RemoteResource.class, G.class, null);
	static G03_RemoteResource g03b = MockRestClient.create(G.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(G03_RemoteResource.class);

	@Test
	public void g03a_path_nameValuePairs_plainText() throws Exception {
		String r = g03a.normal(new G03_Bean());
		assertEquals("echoPath/v1/123/null//true/123/null/v1/123/null/", r);
	}
	@Test
	public void g03b_path_nameValuePairs_uon() throws Exception {
		String r = g03b.normal(new G03_Bean());
		assertEquals("echoPath/v1/'123'/null//'true'/'123'/'null'/v1/'123'/null/", r);
	}
	@Test
	public void g03c_path_nameValuePairs_x() throws Exception {
		String r = g03b.serialized(new G03_Bean());
		assertEquals("echoPath/xv1x/x123x/NULL/xx/xtruex/x123x/xnullx/xv1x/x123x/NULL/xx", r);
	}

	//=================================================================================================================
	// @Path, Collections
	//=================================================================================================================

	@RemoteResource(path="/")
	public static interface G04_RemoteResource {

		@RemoteMethod(method="GET", path="/echoPath/{a}/{b}/{c}/{d}/{f}/{g}/{h}")
		String normal(@Request G04_Bean rb);

		@RemoteMethod(method="GET", path="/echoPath/{a}/{b}/{c}/{d}/{f}/{g}/{h}")
		String serialized(@Request(partSerializer=XSerializer.class) G04_Bean rb);
	}

	public static class G04_Bean {
		@Path
		public List<Object> getA() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Path("b")
		public List<Object> getX1() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Path(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}
		@Path(name="d",allowEmptyValue=true)
		public List<Object> getX3() {
			return new AList<>();
		}
		@Path("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Path(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Path(name="h",allowEmptyValue=true)
		public Object[] getX7() {
			return new Object[]{};
		}
	}

	static G04_RemoteResource g04a = MockRemoteResource.build(G04_RemoteResource.class, G.class, null);
	static G04_RemoteResource g04b = MockRestClient.create(G.class, null).partSerializer(UonSerializer.class).build().getRemoteResource(G04_RemoteResource.class);

	@Test
	public void g04a_path_collections_plainText() throws Exception {
		String r = g04a.normal(new G04_Bean());
		assertEquals("echoPath/foo,,true,123,null,true,123,null/foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null//foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null/", r);
	}
	@Test
	public void g04b_path_collections_uon() throws Exception {
		String r = g04b.normal(new G04_Bean());
		assertEquals("echoPath/@(foo,'','true','123','null',true,123,null)/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()", r);
	}
	@Test
	public void g04c_path_collections_x() throws Exception {
		String r = g04b.serialized(new G04_Bean());
		assertEquals("echoPath/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull/foo||true|123|null|true|123|null//fooXXtrueX123XnullXtrueX123Xnull/foo||true|123|null|true|123|null/", r);
	}

	//=================================================================================================================
	// Support classes
	//=================================================================================================================

	public static class XSerializer extends BaseHttpPartSerializer {
		@Override
		public HttpPartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new BaseHttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					if (value == null)
						return "NULL";
					if (value instanceof Collection)
						return join((Collection<?>)value, "X");
					if (isArray(value))
						return join(toList(value, Object.class), "X");
					return "x" + value + "x";
				}
			};
		}

		@Override
		public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createPartSession().serialize(partType, schema, value);
		}

		@Override
		public String serialize(HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createPartSession().serialize(null, schema, value);
		}
	}

	public static class ListSerializer extends BaseHttpPartSerializer {
		@Override
		public HttpPartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new BaseHttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					if (value == null)
						return "NULL";
					if (value instanceof Collection)
						return join((Collection<?>)value, '|');
					if (isArray(value))
						return join(toList(value, Object.class), "|");
					return "?" + value + "?";
				}
			};
		}

		@Override
		public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createPartSession().serialize(partType, schema, value);
		}

		@Override
		public String serialize(HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createPartSession().serialize(null, schema, value);
		}
	}
}
