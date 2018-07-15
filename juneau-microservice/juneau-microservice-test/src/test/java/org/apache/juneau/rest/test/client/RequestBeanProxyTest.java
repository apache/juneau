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

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RequestBeanProxyTest {

	//=================================================================================================================
	// @Query
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(name=GET, path="/echoQuery")
		public String echoQuery(RestRequest req) throws Exception {
			return req.getQuery().toString(true);
		}
	}
	static MockRest a = MockRest.create(A.class);

	//=================================================================================================================
	// @Query - Simple values
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A01_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A01_BeanImpl rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String serialized(@RequestBean(serializer=XSerializer.class) A01_BeanImpl rb);
	}

	public static interface A01_BeanInterface {
		@Query String getA();
		@Query("b") String getX1();
		@Query(name="c") String getX2();
		@Query @BeanProperty("d") String getX3();
		@Query("e") String getX4();
		@Query("f") String getX5();
		@Query("g") String getX6();
		@Query("h") String getX7();
	}

	public static class A01_BeanImpl implements A01_BeanInterface {
		@Override public String getA() { return "a1"; }
		@Override public String getX1() { return "b1"; }
		@Override public String getX2() { return "c1"; }
		@Override public String getX3() { return "d1"; }
		@Override public String getX4() { return ""; }
		@Override public String getX5() { return null; }
		@Override public String getX6() { return "true"; }
		@Override public String getX7() { return "123"; }
	}

	static A01_Remoteable a01a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A01_Remoteable.class, null);
	static A01_Remoteable a01b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(a).build().getRemoteableProxy(A01_Remoteable.class, null);

	@Test
	public void a01a_query_simpleVals_plainText() throws Exception {
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", a01a.normal(new A01_BeanImpl()));
	}
	@Test
	public void a01b_query_simpleVals_uon() throws Exception {
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'\\'true\\'',h:'\\'123\\''}", a01b.normal(new A01_BeanImpl()));
	}
	@Test
	public void a01c_query_simpleVals_x() throws Exception {
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", a01b.serialized(new A01_BeanImpl()));
	}

	//=================================================================================================================
	// @Query - Maps
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A02_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A02_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String serialized(@RequestBean(serializer=XSerializer.class) A02_Bean rb);
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
		@Query(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Query("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static A02_Remoteable a02a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A02_Remoteable.class, null);
	static A02_Remoteable a02b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(a).build().getRemoteableProxy(A02_Remoteable.class, null);

	@Test
	public void a02a_query_maps_plainText() throws Exception {
		String r = a02a.normal(new A02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a02b_query_maps_uon() throws Exception {
		String r = a02b.normal(new A02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a02c_query_maps_x() throws Exception {
		String r = a02b.serialized(new A02_Bean());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Query - NameValuePairs
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A03_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A03_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String serialized(@RequestBean(serializer=XSerializer.class) A03_Bean rb);
	}

	public static class A03_Bean {
		@Query
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Query("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Query(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Query("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static A03_Remoteable a03a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A03_Remoteable.class, null);
	static A03_Remoteable a03b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(a).build().getRemoteableProxy(A03_Remoteable.class, null);

	@Test
	public void a03a_query_nameValuePairs_plainText() throws Exception {
		String r = a03a.normal(new A03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a03b_query_nameValuePairs_on() throws Exception {
		String r = a03b.normal(new A03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void a03c_query_nameValuePairs_x() throws Exception {
		String r = a03b.serialized(new A03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	//=================================================================================================================
	// @Query - CharSequence
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A04_Remoteable {
		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A04_Bean rb);
	}

	public static class A04_Bean {
		@Query("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	static A04_Remoteable a04a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A04_Remoteable.class, null);

	@Test
	public void a04a_query_charSequence() throws Exception {
		String r = a04a.normal(new A04_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @Query - Reader
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A05_Remoteable {
		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A05_Bean rb);
	}

	public static class A05_Bean {
		@Query("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	static A05_Remoteable a05a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A05_Remoteable.class, null);

	@Test
	public void a05a_query_reader() throws Exception {
		String r = a05a.normal(new A05_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @Query - Collections
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface A06_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String normal(@RequestBean A06_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String serialized(@RequestBean(serializer=XSerializer.class) A06_Bean rb);
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
		@Query("d")
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
		@Query("h")
		public Object[] getX7() {
			return new Object[]{};
		}
		@Query("i")
		public Object[] getX8() {
			return null;
		}
	}

	static A06_Remoteable a06a = RestClient.create().mockHttpConnection(a).build().getRemoteableProxy(A06_Remoteable.class, null);
	static A06_Remoteable a06b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(a).build().getRemoteableProxy(A06_Remoteable.class, null);

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
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}

	//=================================================================================================================
	// @FormData
	//=================================================================================================================

	@RestResource(parsers=UrlEncodingParser.class)
	public static class C {
		@RestMethod(name=POST)
		public String echoFormData(RestRequest req) throws Exception {
			return req.getFormData().toString(true);
		}
	}
	static MockRest c = MockRest.create(C.class);

	//=================================================================================================================
	// @FormData, Simple values
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface C01_Remoteable {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C01_Bean rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String serialized(@RequestBean(serializer=XSerializer.class) C01_Bean rb);
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
		@FormData
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}
		@FormData("e")
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

	static C01_Remoteable c01a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C01_Remoteable.class, null);
	static C01_Remoteable c01b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(c).build().getRemoteableProxy(C01_Remoteable.class, null);

	@Test
	public void c01a_formData_simpleVals_plainText() throws Exception {
		String r = c01a.normal(new C01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}
	@Test
	public void c01b_formData_simpleVals_uon() throws Exception {
		String r = c01b.normal(new C01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}
	@Test
	public void c01c_formData_simpleVals_x() throws Exception {
		String r = c01b.serialized(new C01_Bean());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	//=================================================================================================================
	// @FormData, Maps
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface C02_Remoteable {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C02_Bean rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String serialized(@RequestBean(serializer=XSerializer.class) C02_Bean rb);
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
		@FormData(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@FormData("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static C02_Remoteable c02a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C02_Remoteable.class, null);
	static C02_Remoteable c02b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(c).build().getRemoteableProxy(C02_Remoteable.class, null);

	@Test
	public void c02a_formData_maps_plainText() throws Exception {
		String r = c02a.normal(new C02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c02b_formData_maps_uon() throws Exception {
		String r = c02b.normal(new C02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void c02c_formData_maps_x() throws Exception {
		String r = c02b.serialized(new C02_Bean());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @FormData, NameValuePairs
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface C03_Remoteable {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C03_Bean rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String serialized(@RequestBean(serializer=XSerializer.class) C03_Bean rb);
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

	static C03_Remoteable c03a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C03_Remoteable.class, null);
	static C03_Remoteable c03b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(c).build().getRemoteableProxy(C03_Remoteable.class, null);

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

	@Remoteable(path="/")
	public static interface C04_Remoteable {
		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C04_Bean rb);
	}

	public static class C04_Bean {
		@FormData("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	static C04_Remoteable c04a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C04_Remoteable.class, null);

	@Test
	public void c04a_formDataCharSequence() throws Exception {
		String r = c04a.normal(new C04_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @FormData, Reader
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface C05_Remoteable {
		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C05_Bean rb);
	}

	public static class C05_Bean {
		@FormData("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	static C05_Remoteable c05a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C05_Remoteable.class, null);

	@Test
	public void c05a_formDataReader() throws Exception {
		String r = c05a.normal(new C05_Bean());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	//=================================================================================================================
	// @FormData, Collections
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface C06_Remoteable {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String normal(@RequestBean C06_Bean rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String serialized(@RequestBean(serializer=XSerializer.class) C06_Bean rb);
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
		@FormData("d")
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
		@FormData("h")
		public Object[] getX7() {
			return new Object[]{};
		}
		@FormData("i")
		public Object[] getX8() {
			return null;
		}
	}

	static C06_Remoteable c06a = RestClient.create().mockHttpConnection(c).build().getRemoteableProxy(C06_Remoteable.class, null);
	static C06_Remoteable c06b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(c).build().getRemoteableProxy(C06_Remoteable.class, null);

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
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}


	//=================================================================================================================
	// @Header
	//=================================================================================================================

	@RestResource
	public static class E {
		@RestMethod(name=GET)
		public String echoHeaders(RestRequest req) throws Exception {
			return req.getHeaders().subset("a,b,c,d,e,f,g,h,i,a1,a2,a3,a4,b1,b2,b3,b4,c1,c2,c3,c4").toString(true);
		}
	}
	static MockRest e = MockRest.create(E.class);

	//=================================================================================================================
	// @Header, Simple values
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface E01_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String normal(@RequestBean E01_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String serialized(@RequestBean(serializer=XSerializer.class) E01_Bean rb);
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
		@Header
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}
		@Header("e")
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

	static E01_Remoteable e01a = RestClient.create().mockHttpConnection(e).build().getRemoteableProxy(E01_Remoteable.class, null);
	static E01_Remoteable e01b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(e).build().getRemoteableProxy(E01_Remoteable.class, null);

	@Test
	public void e01a_headerSimpleValsPlainText() throws Exception {
		String r = e01a.normal(new E01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}
	@Test
	public void e01b_headerSimpleValsUon() throws Exception {
		String r = e01b.normal(new E01_Bean());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}
	@Test
	public void e01c_headerSimpleValsX() throws Exception {
		String r = e01b.serialized(new E01_Bean());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	//=================================================================================================================
	// @Header, Maps
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface E02_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String normal(@RequestBean E02_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String serialized(@RequestBean(serializer=XSerializer.class) E02_Bean rb);
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
		@Header(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Header("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static E02_Remoteable e02a = RestClient.create().mockHttpConnection(e).build().getRemoteableProxy(E02_Remoteable.class, null);
	static E02_Remoteable e02b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(e).build().getRemoteableProxy(E02_Remoteable.class, null);

	@Test
	public void e02a_header_maps_plainText() throws Exception {
		String r = e02a.normal(new E02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e02b_header_maps_uon() throws Exception {
		String r = e02b.normal(new E02_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e02c_header_maps_x() throws Exception {
		String r = e02b.serialized(new E02_Bean());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	//=================================================================================================================
	// @Header, NameValuePairs
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface E03_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String normal(@RequestBean E03_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String serialized(@RequestBean(serializer=XSerializer.class) E03_Bean rb);
	}

	public static class E03_Bean {
		@Header
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Header("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Header(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Header("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static E03_Remoteable e03a = RestClient.create().mockHttpConnection(e).build().getRemoteableProxy(E03_Remoteable.class, null);
	static E03_Remoteable e03b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(e).build().getRemoteableProxy(E03_Remoteable.class, null);

	@Test
	public void e03a_header_nameValuePairs_plainText() throws Exception {
		String r = e03a.normal(new E03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e03b_header_nameValuePairs_uon() throws Exception {
		String r = e03b.normal(new E03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}
	@Test
	public void e03c_header_nameValuePairs_x() throws Exception {
		String r = e03b.serialized(new E03_Bean());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	//=================================================================================================================
	// @Header, Collections
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface E04_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String normal(@RequestBean E04_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String serialized(@RequestBean(serializer=XSerializer.class) E04_Bean rb);
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
		@Header("d")
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
		@Header("h")
		public Object[] getX7() {
			return new Object[]{};
		}
		@Header("i")
		public Object[] getX8() {
			return null;
		}
	}

	static E04_Remoteable e04a = RestClient.create().mockHttpConnection(e).build().getRemoteableProxy(E04_Remoteable.class, null);
	static E04_Remoteable e04b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(e).build().getRemoteableProxy(E04_Remoteable.class, null);

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
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}

	//=================================================================================================================
	// @Path
	//=================================================================================================================

	@RestResource
	public static class G  {
		@RestMethod(name=GET)
		public String echoPath(RestRequest req) throws Exception {
			return req.getPathMatch().getRemainder();
		}
	}
	static MockRest g = MockRest.create(G.class);

	//=================================================================================================================
	// @Path, Simple values
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface G01_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}")
		String normal(@RequestBean G01_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}")
		String serialized(@RequestBean(serializer=XSerializer.class) G01_Bean rb);
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
		@Path
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}
		@Path("e")
		public String getX4() {
			return "";
		}
		@Path("f")
		public String getX5() {
			return null;
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

	static G01_Remoteable g01a = RestClient.create().mockHttpConnection(g).build().getRemoteableProxy(G01_Remoteable.class, null);
	static G01_Remoteable g01b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(g).build().getRemoteableProxy(G01_Remoteable.class, null);

	@Test
	public void g01a_pathSimpleValsPlainText() throws Exception {
		String r = g01a.normal(new G01_Bean());
		assertEquals("echoPath/a1/b1/c1/d1//null/true/123", r);
	}
	@Test
	public void g01b_pathSimpleValsUon() throws Exception {
		String r = g01b.normal(new G01_Bean());
		assertEquals("echoPath/a1/b1/c1/d1//null/'true'/'123'", r);
	}
	@Test
	public void g01c_pathSimpleValsX() throws Exception {
		String r = g01b.serialized(new G01_Bean());
		assertEquals("echoPath/xa1x/xb1x/xc1x/xd1x/xx/NULL/xtruex/x123x", r);
	}

	//=================================================================================================================
	// @Path, Maps
	//=================================================================================================================

	@Remoteable(path="/")
	public static interface G02_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String normal(@RequestBean G02_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String serialized(@RequestBean(serializer=XSerializer.class) G02_Bean rb);
	}

	public static class G02_Bean {
		@Path
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Path("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Path(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Path("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	static G02_Remoteable g02a = RestClient.create().mockHttpConnection(g).build().getRemoteableProxy(G02_Remoteable.class, null);
	static G02_Remoteable g02b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(g).build().getRemoteableProxy(G02_Remoteable.class, null);

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

	@Remoteable(path="/")
	public static interface G03_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String normal(@RequestBean G03_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String serialized(@RequestBean(serializer=XSerializer.class) G03_Bean rb);
	}

	public static class G03_Bean {
		@Path
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}
		@Path("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}
		@Path(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}
		@Path("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	static G03_Remoteable g03a = RestClient.create().mockHttpConnection(g).build().getRemoteableProxy(G03_Remoteable.class, null);
	static G03_Remoteable g03b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(g).build().getRemoteableProxy(G03_Remoteable.class, null);

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

	@Remoteable(path="/")
	public static interface G04_Remoteable {

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}/{i}")
		String normal(@RequestBean G04_Bean rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}/{i}")
		String serialized(@RequestBean(serializer=XSerializer.class) G04_Bean rb);
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
		@Path("d")
		public List<Object> getX3() {
			return new AList<>();
		}
		@Path("e")
		public List<Object> getX4() {
			return null;
		}
		@Path("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Path(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}
		@Path("h")
		public Object[] getX7() {
			return new Object[]{};
		}
		@Path("i")
		public Object[] getX8() {
			return null;
		}
	}

	static G04_Remoteable g04a = RestClient.create().mockHttpConnection(g).build().getRemoteableProxy(G04_Remoteable.class, null);
	static G04_Remoteable g04b = RestClient.create().partSerializer(UonPartSerializer.class).mockHttpConnection(g).build().getRemoteableProxy(G04_Remoteable.class, null);

	@Test
	public void g04a_path_collections_plainText() throws Exception {
		String r = g04a.normal(new G04_Bean());
		assertEquals("echoPath/foo,,true,123,null,true,123,null/foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null//null/foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null//null", r);
	}
	@Test
	public void g04b_path_collections_uon() throws Exception {
		String r = g04b.normal(new G04_Bean());
		assertEquals("echoPath/@(foo,'','true','123','null',true,123,null)/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()/null/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()/null", r);
	}
	@Test
	public void g04c_path_collections_x() throws Exception {
		String r = g04b.serialized(new G04_Bean());
		assertEquals("echoPath/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull//NULL/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull//NULL", r);
	}

	//=================================================================================================================
	// Support classes
	//=================================================================================================================

	public static class XSerializer implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession createSession(SerializerSessionArgs args) {
			return new HttpPartSerializerSession() {
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
			return createSession(null).serialize(partType, schema, value);
		}
	}

	public static class ListSerializer implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession createSession(SerializerSessionArgs args) {
			return new HttpPartSerializerSession() {
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
			return createSession(null).serialize(partType, schema, value);
		}
	}
}
