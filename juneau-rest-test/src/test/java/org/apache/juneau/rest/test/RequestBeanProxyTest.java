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

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RequestBeanProxyTest extends RestTestcase {

	private <T> T getProxyPlainText(Class<T> t) {
		RestClient rc = TestMicroservice.client(PlainTextSerializer.class, PlainTextParser.class).plainTextParts().build();
		addClientToLifecycle(rc);
		return rc.getRemoteableProxy(t, null);
	}

	private <T> T getProxyUon(Class<T> t) {
		RestClient rc = TestMicroservice.client(PlainTextSerializer.class, PlainTextParser.class).build();
		addClientToLifecycle(rc);
		return rc.getRemoteableProxy(t, null);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// @Query
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_querySimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Query.class).querySimpleValsPlainText(new RequestBean_QuerySimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}

	@Test
	public void a02_querySimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).querySimpleValsUon(new RequestBean_QuerySimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}

	@Test
	public void a03_querySimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).querySimpleValsX(new RequestBean_QuerySimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void a04_queryMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Query.class).queryMapsPlainText(new RequestBean_QueryMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void a05_queryMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryMapsUon(new RequestBean_QueryMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void a06_queryMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryMapsX(new RequestBean_QueryMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	@Test
	public void a07_queryNameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Query.class).queryNameValuePairsPlainText(new RequestBean_QueryNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void a08_queryNameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryNameValuePairsUon(new RequestBean_QueryNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void a09_queryNameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryNameValuePairsX(new RequestBean_QueryNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void a10_queryCharSequence() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryCharSequence(new RequestBean_QueryCharSequence());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void a11_queryReader() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryReader(new RequestBean_QueryReader());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void a12_queryCollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Query.class).queryCollectionsPlainText(new RequestBean_QueryCollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}

	@Test
	public void a13_queryCollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryCollectionsUon(new RequestBean_QueryCollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}

	@Test
	public void a14_queryCollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Query.class).queryCollectionsX(new RequestBean_QueryCollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_Query {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsPlainText(@RequestBean RequestBean_QuerySimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsUon(@RequestBean RequestBean_QuerySimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_QuerySimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsPlainText(@RequestBean RequestBean_QueryMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsUon(@RequestBean RequestBean_QueryMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsPlainText(@RequestBean RequestBean_QueryNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsUon(@RequestBean RequestBean_QueryNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCharSequence(@RequestBean RequestBean_QueryCharSequence rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryReader(@RequestBean RequestBean_QueryReader rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsPlainText(@RequestBean RequestBean_QueryCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsUon(@RequestBean RequestBean_QueryCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryCollections rb);
	}

	public static class RequestBean_QuerySimpleVals {

		@Query
		public String getA() {
			return "a1";
		}

		@Query("b")
		public String getX1() {
			return "b1";
		}

		@Query(name="c")
		public String getX2() {
			return "c1";
		}

		@Query
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}

		@Query("e")
		public String getX4() {
			return "";
		}

		@Query("f")
		public String getX5() {
			return null;
		}

		@Query("g")
		public String getX6() {
			return "true";
		}

		@Query("h")
		public String getX7() {
			return "123";
		}
	}

	public static class RequestBean_QueryMaps {

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

	public static class RequestBean_QueryNameValuePairs {

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

	public static class RequestBean_QueryCharSequence {

		@Query("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_QueryReader {

		@Query("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_QueryCollections {

		@Query
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Query("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Query(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Query("d")
		public List<Object> getX3() {
			return new AList<Object>();
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

	//-------------------------------------------------------------------------------------------------------------------
	// @QueryIfNE
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_queryIfNESimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_QueryIfNE.class).querySimpleValsPlainText(new RequestBean_QueryIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'true',h:'123'}", r);
	}

	@Test
	public void b02_queryIfNESimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).querySimpleValsUon(new RequestBean_QueryIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'\\'true\\'',h:'\\'123\\''}", r);
	}

	@Test
	public void b03_queryIfNESimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).querySimpleValsX(new RequestBean_QueryIfNESimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void b04_queryIfNEMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_QueryIfNE.class).queryMapsPlainText(new RequestBean_QueryIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void b05_queryIfNEMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryMapsUon(new RequestBean_QueryIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void b06_queryIfNEMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryMapsX(new RequestBean_QueryIfNEMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x'}", r);
	}

	@Test
	public void b07_queryIfNENameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_QueryIfNE.class).queryNameValuePairsPlainText(new RequestBean_QueryIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void b08_queryIfNENameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryNameValuePairsUon(new RequestBean_QueryIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void b09_queryIfNENameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryNameValuePairsX(new RequestBean_QueryIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void b10_queryIfNECharSequence() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryCharSequence(new RequestBean_QueryIfNECharSequence());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void b11_queryIfNEReader() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryReader(new RequestBean_QueryIfNEReader());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void b12_queryIfNECollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_QueryIfNE.class).queryCollectionsPlainText(new RequestBean_QueryIfNECollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void b13_queryIfNECollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryCollectionsUon(new RequestBean_QueryIfNECollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void b14_queryIfNECollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_QueryIfNE.class).queryCollectionsX(new RequestBean_QueryIfNECollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull'}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_QueryIfNE {

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsPlainText(@RequestBean RequestBean_QueryIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsUon(@RequestBean RequestBean_QueryIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String querySimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsPlainText(@RequestBean RequestBean_QueryIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsUon(@RequestBean RequestBean_QueryIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsPlainText(@RequestBean RequestBean_QueryIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsUon(@RequestBean RequestBean_QueryIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCharSequence(@RequestBean RequestBean_QueryIfNECharSequence rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryReader(@RequestBean RequestBean_QueryIfNEReader rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsPlainText(@RequestBean RequestBean_QueryIfNECollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsUon(@RequestBean RequestBean_QueryIfNECollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoQuery")
		String queryCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_QueryIfNECollections rb);
	}

	public static class RequestBean_QueryIfNESimpleVals {

		@QueryIfNE
		public String getA() {
			return "a1";
		}

		@QueryIfNE("b")
		public String getX1() {
			return "b1";
		}

		@QueryIfNE(name="c")
		public String getX2() {
			return "c1";
		}

		@QueryIfNE
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}

		@QueryIfNE("e")
		public String getX4() {
			return "";
		}

		@QueryIfNE("f")
		public String getX5() {
			return null;
		}

		@QueryIfNE("g")
		public String getX6() {
			return "true";
		}

		@QueryIfNE("h")
		public String getX7() {
			return "123";
		}
	}

	public static class RequestBean_QueryIfNEMaps {

		@QueryIfNE
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@QueryIfNE("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@QueryIfNE(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@QueryIfNE("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	public static class RequestBean_QueryIfNENameValuePairs {

		@QueryIfNE
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@QueryIfNE("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@QueryIfNE(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@QueryIfNE("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	public static class RequestBean_QueryIfNECharSequence {

		@QueryIfNE("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_QueryIfNEReader {

		@QueryIfNE("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_QueryIfNECollections {

		@QueryIfNE
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@QueryIfNE("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@QueryIfNE(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@QueryIfNE("d")
		public List<Object> getX3() {
			return new AList<Object>();
		}

		@QueryIfNE("e")
		public List<Object> getX4() {
			return null;
		}

		@QueryIfNE("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@QueryIfNE(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@QueryIfNE("h")
		public Object[] getX7() {
			return new Object[]{};
		}

		@QueryIfNE("i")
		public Object[] getX8() {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// @FormData
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_formDataSimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormData.class).formDataSimpleValsPlainText(new RequestBean_FormDataSimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}

	@Test
	public void c02_formDataSimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataSimpleValsUon(new RequestBean_FormDataSimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'\\'true\\'',h:'\\'123\\''}", r);
	}

	@Test
	public void c03_formDataSimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataSimpleValsX(new RequestBean_FormDataSimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void c04_formDataMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormData.class).formDataMapsPlainText(new RequestBean_FormDataMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void c05_formDataMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataMapsUon(new RequestBean_FormDataMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void c06_formDataMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataMapsX(new RequestBean_FormDataMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	@Test
	public void c07_formDataNameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormData.class).formDataNameValuePairsPlainText(new RequestBean_FormDataNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void c08_formDataNameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataNameValuePairsUon(new RequestBean_FormDataNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void c09_formDataNameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataNameValuePairsX(new RequestBean_FormDataNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void c10_formDataCharSequence() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataCharSequence(new RequestBean_FormDataCharSequence());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void c11_formDataReader() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataReader(new RequestBean_FormDataReader());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void c12_formDataCollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormData.class).formDataCollectionsPlainText(new RequestBean_FormDataCollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}

	@Test
	public void c13_formDataCollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataCollectionsUon(new RequestBean_FormDataCollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}

	@Test
	public void c14_formDataCollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormData.class).formDataCollectionsX(new RequestBean_FormDataCollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_FormData {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsPlainText(@RequestBean RequestBean_FormDataSimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsUon(@RequestBean RequestBean_FormDataSimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataSimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsPlainText(@RequestBean RequestBean_FormDataMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsUon(@RequestBean RequestBean_FormDataMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsPlainText(@RequestBean RequestBean_FormDataNameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsUon(@RequestBean RequestBean_FormDataNameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataNameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCharSequence(@RequestBean RequestBean_FormDataCharSequence rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataReader(@RequestBean RequestBean_FormDataReader rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsPlainText(@RequestBean RequestBean_FormDataCollections rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsUon(@RequestBean RequestBean_FormDataCollections rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataCollections rb);
	}

	public static class RequestBean_FormDataSimpleVals {

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

	public static class RequestBean_FormDataMaps {

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

	public static class RequestBean_FormDataNameValuePairs {

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

	public static class RequestBean_FormDataCharSequence {

		@FormData("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_FormDataReader {

		@FormData("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_FormDataCollections {

		@FormData
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormData("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormData(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormData("d")
		public List<Object> getX3() {
			return new AList<Object>();
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

	//-------------------------------------------------------------------------------------------------------------------
	// @FormDataIfNE
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_formDataIfNESimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormDataIfNE.class).formDataSimpleValsPlainText(new RequestBean_FormDataIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'true',h:'123'}", r);
	}

	@Test
	public void d02_formDataIfNESimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataSimpleValsUon(new RequestBean_FormDataIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'\\'true\\'',h:'\\'123\\''}", r);
	}

	@Test
	public void d03_formDataIfNESimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataSimpleValsX(new RequestBean_FormDataIfNESimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void d04_formDataIfNEMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormDataIfNE.class).formDataMapsPlainText(new RequestBean_FormDataIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void d05_formDataIfNEMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataMapsUon(new RequestBean_FormDataIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void d06_formDataIfNEMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataMapsX(new RequestBean_FormDataIfNEMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x'}", r);
	}

	@Test
	public void d07_formDataIfNENameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormDataIfNE.class).formDataNameValuePairsPlainText(new RequestBean_FormDataIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void d08_formDataIfNENameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataNameValuePairsUon(new RequestBean_FormDataIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void d09_formDataIfNENameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataNameValuePairsX(new RequestBean_FormDataIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void d10_formDataIfNECharSequence() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataCharSequence(new RequestBean_FormDataIfNECharSequence());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void d11_formDataIfNEReader() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataReader(new RequestBean_FormDataIfNEReader());
		assertEquals("{baz:'qux',foo:'bar'}", r);
	}

	@Test
	public void d12_formDataIfNECollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_FormDataIfNE.class).formDataCollectionsPlainText(new RequestBean_FormDataIfNECollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void d13_formDataIfNECollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataCollectionsUon(new RequestBean_FormDataIfNECollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void d14_formDataIfNECollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_FormDataIfNE.class).formDataCollectionsX(new RequestBean_FormDataIfNECollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull'}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_FormDataIfNE {

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsPlainText(@RequestBean RequestBean_FormDataIfNESimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsUon(@RequestBean RequestBean_FormDataIfNESimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataSimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataIfNESimpleVals rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsPlainText(@RequestBean RequestBean_FormDataIfNEMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsUon(@RequestBean RequestBean_FormDataIfNEMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataIfNEMaps rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsPlainText(@RequestBean RequestBean_FormDataIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsUon(@RequestBean RequestBean_FormDataIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCharSequence(@RequestBean RequestBean_FormDataIfNECharSequence rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataReader(@RequestBean RequestBean_FormDataIfNEReader rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsPlainText(@RequestBean RequestBean_FormDataIfNECollections rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsUon(@RequestBean RequestBean_FormDataIfNECollections rb);

		@RemoteMethod(httpMethod="POST", path="/echoFormData")
		String formDataCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_FormDataIfNECollections rb);
	}

	public static class RequestBean_FormDataIfNESimpleVals {

		@FormDataIfNE
		public String getA() {
			return "a1";
		}

		@FormDataIfNE("b")
		public String getX1() {
			return "b1";
		}

		@FormDataIfNE(name="c")
		public String getX2() {
			return "c1";
		}

		@FormDataIfNE
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}

		@FormDataIfNE("e")
		public String getX4() {
			return "";
		}

		@FormDataIfNE("f")
		public String getX5() {
			return null;
		}

		@FormDataIfNE("g")
		public String getX6() {
			return "true";
		}

		@FormDataIfNE("h")
		public String getX7() {
			return "123";
		}
	}

	public static class RequestBean_FormDataIfNEMaps {

		@FormDataIfNE
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@FormDataIfNE("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@FormDataIfNE(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@FormDataIfNE("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	public static class RequestBean_FormDataIfNENameValuePairs {

		@FormDataIfNE
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@FormDataIfNE("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@FormDataIfNE(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@FormDataIfNE("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	public static class RequestBean_FormDataIfNECharSequence {

		@FormDataIfNE("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_FormDataIfNEReader {

		@FormDataIfNE("*")
		public Reader getA() {
			return new StringReader("foo=bar&baz=qux");
		}
	}

	public static class RequestBean_FormDataIfNECollections {

		@FormDataIfNE
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormDataIfNE("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormDataIfNE(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@FormDataIfNE("d")
		public List<Object> getX3() {
			return new AList<Object>();
		}

		@FormDataIfNE("e")
		public List<Object> getX4() {
			return null;
		}

		@FormDataIfNE("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@FormDataIfNE(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@FormDataIfNE("h")
		public Object[] getX7() {
			return new Object[]{};
		}

		@FormDataIfNE("i")
		public Object[] getX8() {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// @Header
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_headerSimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Header.class).headerSimpleValsPlainText(new RequestBean_HeaderSimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}

	@Test
	public void e02_headerSimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerSimpleValsUon(new RequestBean_HeaderSimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',e:'',g:'true',h:'123'}", r);
	}

	@Test
	public void e03_headerSimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerSimpleValsX(new RequestBean_HeaderSimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',e:'xx',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void e04_headerMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Header.class).headerMapsPlainText(new RequestBean_HeaderMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void e05_headerMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerMapsUon(new RequestBean_HeaderMaps());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void e06_headerMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerMapsX(new RequestBean_HeaderMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',a4:'xx',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}", r);
	}

	@Test
	public void e07_headerNameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Header.class).headerNameValuePairsPlainText(new RequestBean_HeaderNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void e08_headerNameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerNameValuePairsUon(new RequestBean_HeaderNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void e09_headerNameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerNameValuePairsX(new RequestBean_HeaderNameValuePairs());
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}", r);
	}

	@Test
	public void e10_headerCollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Header.class).headerCollectionsPlainText(new RequestBean_HeaderCollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',d:'',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null',h:''}", r);
	}

	@Test
	public void e11_headerCollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerCollectionsUon(new RequestBean_HeaderCollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null',h:'@()'}", r);
	}

	@Test
	public void e12_headerCollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Header.class).headerCollectionsX(new RequestBean_HeaderCollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',d:'',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull',h:''}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_Header {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsPlainText(@RequestBean RequestBean_HeaderSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsUon(@RequestBean RequestBean_HeaderSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsPlainText(@RequestBean RequestBean_HeaderMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsUon(@RequestBean RequestBean_HeaderMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsPlainText(@RequestBean RequestBean_HeaderNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsUon(@RequestBean RequestBean_HeaderNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsPlainText(@RequestBean RequestBean_HeaderCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsUon(@RequestBean RequestBean_HeaderCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderCollections rb);
	}

	public static class RequestBean_HeaderSimpleVals {

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

	public static class RequestBean_HeaderMaps {

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

	public static class RequestBean_HeaderNameValuePairs {

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

	public static class RequestBean_HeaderCollections {

		@Header
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Header("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Header(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Header("d")
		public List<Object> getX3() {
			return new AList<Object>();
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

	//-------------------------------------------------------------------------------------------------------------------
	// @HeaderIfNE
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_headerIfNESimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_HeaderIfNE.class).headerSimpleValsPlainText(new RequestBean_HeaderIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'true',h:'123'}", r);
	}

	@Test
	public void f02_headerIfNESimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerSimpleValsUon(new RequestBean_HeaderIfNESimpleVals());
		assertEquals("{a:'a1',b:'b1',c:'c1',d:'d1',g:'true',h:'123'}", r);
	}

	@Test
	public void f03_headerIfNESimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerSimpleValsX(new RequestBean_HeaderIfNESimpleVals());
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',d:'xd1x',g:'xtruex',h:'x123x'}", r);
	}

	@Test
	public void f04_headerIfNEMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_HeaderIfNE.class).headerMapsPlainText(new RequestBean_HeaderIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void f05_headerIfNEMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerMapsUon(new RequestBean_HeaderIfNEMaps());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void f06_headerIfNEMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerMapsX(new RequestBean_HeaderIfNEMaps());
		assertEquals("{a1:'xv1x',a2:'x123x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x'}", r);
	}

	@Test
	public void f07_headerIfNENameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_HeaderIfNE.class).headerNameValuePairsPlainText(new RequestBean_HeaderIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void f08_headerIfNENameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerNameValuePairsUon(new RequestBean_HeaderIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void f09_headerIfNENameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerNameValuePairsX(new RequestBean_HeaderIfNENameValuePairs());
		assertEquals("{a1:'v1',a2:'123',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123'}", r);
	}

	@Test
	public void f10_headerIfNECollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_HeaderIfNE.class).headerCollectionsPlainText(new RequestBean_HeaderIfNECollections());
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'foo||true|123|null|true|123|null',f:'foo,,true,123,null,true,123,null',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void f11_headerIfNECollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerCollectionsUon(new RequestBean_HeaderIfNECollections());
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'foo||true|123|null|true|123|null',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'foo||true|123|null|true|123|null'}", r);
	}

	@Test
	public void f12_headerIfNECollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_HeaderIfNE.class).headerCollectionsX(new RequestBean_HeaderIfNECollections());
		assertEquals("{a:'fooXXtrueX123XnullXtrueX123Xnull',b:'fooXXtrueX123XnullXtrueX123Xnull',c:'fooXXtrueX123XnullXtrueX123Xnull',f:'fooXXtrueX123XnullXtrueX123Xnull',g:'fooXXtrueX123XnullXtrueX123Xnull'}", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_HeaderIfNE {

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsPlainText(@RequestBean RequestBean_HeaderIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsUon(@RequestBean RequestBean_HeaderIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerSimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderIfNESimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsPlainText(@RequestBean RequestBean_HeaderIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsUon(@RequestBean RequestBean_HeaderIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderIfNEMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsPlainText(@RequestBean RequestBean_HeaderIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsUon(@RequestBean RequestBean_HeaderIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderIfNENameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsPlainText(@RequestBean RequestBean_HeaderIfNECollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsUon(@RequestBean RequestBean_HeaderIfNECollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoHeaders")
		String headerCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_HeaderIfNECollections rb);
	}

	public static class RequestBean_HeaderIfNESimpleVals {

		@HeaderIfNE
		public String getA() {
			return "a1";
		}

		@HeaderIfNE("b")
		public String getX1() {
			return "b1";
		}

		@HeaderIfNE(name="c")
		public String getX2() {
			return "c1";
		}

		@HeaderIfNE
		@BeanProperty("d")
		public String getX3() {
			return "d1";
		}

		@HeaderIfNE("e")
		public String getX4() {
			return "";
		}

		@HeaderIfNE("f")
		public String getX5() {
			return null;
		}

		@HeaderIfNE("g")
		public String getX6() {
			return "true";
		}

		@HeaderIfNE("h")
		public String getX7() {
			return "123";
		}
	}

	public static class RequestBean_HeaderIfNEMaps {

		@HeaderIfNE
		public Map<String,Object> getA() {
			return new AMap<String,Object>().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@HeaderIfNE("*")
		public Map<String,Object> getB() {
			return new AMap<String,Object>().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@HeaderIfNE(name="*")
		public Map<String,Object> getC() {
			return new AMap<String,Object>().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@HeaderIfNE("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	public static class RequestBean_HeaderIfNENameValuePairs {

		@HeaderIfNE
		public NameValuePairs getA() {
			return new NameValuePairs().append("a1","v1").append("a2", 123).append("a3", null).append("a4", "");
		}

		@HeaderIfNE("*")
		public NameValuePairs getB() {
			return new NameValuePairs().append("b1","true").append("b2", "123").append("b3", "null");
		}

		@HeaderIfNE(name="*")
		public NameValuePairs getC() {
			return new NameValuePairs().append("c1","v1").append("c2", 123).append("c3", null).append("c4", "");
		}

		@HeaderIfNE("*")
		public NameValuePairs getD() {
			return null;
		}
	}

	public static class RequestBean_HeaderIfNECollections {

		@HeaderIfNE
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@HeaderIfNE("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@HeaderIfNE(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@HeaderIfNE("d")
		public List<Object> getX3() {
			return new AList<Object>();
		}

		@HeaderIfNE("e")
		public List<Object> getX4() {
			return null;
		}

		@HeaderIfNE("f")
		public Object[] getX5() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@HeaderIfNE(name="g", serializer=ListSerializer.class)
		public Object[] getX6() {
			return new Object[]{"foo", "", "true", "123", "null", true, 123, null};
		}

		@HeaderIfNE("h")
		public Object[] getX7() {
			return new Object[]{};
		}

		@HeaderIfNE("i")
		public Object[] getX8() {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// @Path
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_pathSimpleValsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Path.class).pathSimpleValsPlainText(new RequestBean_PathSimpleVals());
		assertEquals("a1/b1/c1/d1//null/true/123", r);
	}

	@Test
	public void g02_pathSimpleValsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathSimpleValsUon(new RequestBean_PathSimpleVals());
		assertEquals("a1/b1/c1/d1//null/'true'/'123'", r);
	}

	@Test
	public void g03_pathSimpleValsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathSimpleValsX(new RequestBean_PathSimpleVals());
		assertEquals("xa1x/xb1x/xc1x/xd1x/xx/NULL/xtruex/x123x", r);
	}

	@Test
	public void g04_pathMapsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Path.class).pathMapsPlainText(new RequestBean_PathMaps());
		assertEquals("v1/123/null//true/123/null/v1/123/null/", r);
	}

	@Test
	public void g05_pathMapsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathMapsUon(new RequestBean_PathMaps());
		assertEquals("v1/123/null//'true'/'123'/'null'/v1/123/null/", r);
	}

	@Test
	public void g06_pathMapsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathMapsX(new RequestBean_PathMaps());
		assertEquals("xv1x/x123x/NULL/xx/xtruex/x123x/xnullx/xv1x/x123x/NULL/xx", r);
	}

	@Test
	public void g07_pathNameValuePairsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Path.class).pathNameValuePairsPlainText(new RequestBean_PathNameValuePairs());
		assertEquals("plainText/v1/123/null//true/123/null/v1/123/null/", r);
	}

	@Test
	public void g08_pathNameValuePairsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathNameValuePairsUon(new RequestBean_PathNameValuePairs());
		assertEquals("v1/'123'/null//'true'/'123'/'null'/v1/'123'/null/", r);
	}

	@Test
	public void g09_pathNameValuePairsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathNameValuePairsX(new RequestBean_PathNameValuePairs());
		assertEquals("xv1x/x123x/NULL/xx/xtruex/x123x/xnullx/xv1x/x123x/NULL/xx", r);
	}

	@Test
	public void g10_pathCollectionsPlainText() throws Exception {
		String r = getProxyPlainText(RequestBeanProxy_Path.class).pathCollectionsPlainText(new RequestBean_PathCollections());
		assertEquals("foo,,true,123,null,true,123,null/foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null//null/foo,,true,123,null,true,123,null/foo||true|123|null|true|123|null//null", r);
	}

	@Test
	public void g11_pathCollectionsUon() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathCollectionsUon(new RequestBean_PathCollections());
		assertEquals("@(foo,'','true','123','null',true,123,null)/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()/null/@(foo,'','true','123','null',true,123,null)/foo||true|123|null|true|123|null/@()/null", r);
	}

	@Test
	public void g12_pathCollectionsX() throws Exception {
		String r = getProxyUon(RequestBeanProxy_Path.class).pathCollectionsX(new RequestBean_PathCollections());
		assertEquals("fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull//NULL/fooXXtrueX123XnullXtrueX123Xnull/fooXXtrueX123XnullXtrueX123Xnull//NULL", r);
	}

	@Remoteable(path="/testRequestBeanProxy")
	public static interface RequestBeanProxy_Path {

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}")
		String pathSimpleValsPlainText(@RequestBean RequestBean_PathSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}")
		String pathSimpleValsUon(@RequestBean RequestBean_PathSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}")
		String pathSimpleValsX(@RequestBean(serializer=XSerializer.class) RequestBean_PathSimpleVals rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathMapsPlainText(@RequestBean RequestBean_PathMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathMapsUon(@RequestBean RequestBean_PathMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathMapsX(@RequestBean(serializer=XSerializer.class) RequestBean_PathMaps rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/plainText/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathNameValuePairsPlainText(@RequestBean RequestBean_PathNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathNameValuePairsUon(@RequestBean RequestBean_PathNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}")
		String pathNameValuePairsX(@RequestBean(serializer=XSerializer.class) RequestBean_PathNameValuePairs rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}/{i}")
		String pathCollectionsPlainText(@RequestBean RequestBean_PathCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}/{i}")
		String pathCollectionsUon(@RequestBean RequestBean_PathCollections rb);

		@RemoteMethod(httpMethod="GET", path="/echoPath/{a}/{b}/{c}/{d}/{e}/{f}/{g}/{h}/{i}")
		String pathCollectionsX(@RequestBean(serializer=XSerializer.class) RequestBean_PathCollections rb);
	}

	public static class RequestBean_PathSimpleVals {

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

	public static class RequestBean_PathMaps {

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

	public static class RequestBean_PathNameValuePairs {

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

	public static class RequestBean_PathCollections {

		@Path
		public List<Object> getA() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Path("b")
		public List<Object> getX1() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Path(name="c", serializer=ListSerializer.class)
		public List<Object> getX2() {
			return new AList<Object>().append("foo").append("").append("true").append("123").append("null").append(true).append(123).append(null);
		}

		@Path("d")
		public List<Object> getX3() {
			return new AList<Object>();
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

	//-------------------------------------------------------------------------------------------------------------------
	// Support classes
	//-------------------------------------------------------------------------------------------------------------------

	public static class XSerializer implements PartSerializer {
		@Override
		public String serialize(PartType type, Object value) {
			if (value == null)
				return "NULL";
			if (value instanceof Collection)
				return StringUtils.join((Collection<?>)value, "X");
			if (ArrayUtils.isArray(value))
				return StringUtils.join(ArrayUtils.toList(value, Object.class), "X");
			return "x" + value + "x";
		}
	}

	public static class ListSerializer implements PartSerializer {
		@Override
		public String serialize(PartType type, Object value) {
			if (value == null)
				return "NULL";
			if (value instanceof Collection)
				return StringUtils.join((Collection<?>)value, '|');
			if (ArrayUtils.isArray(value))
				return StringUtils.join(ArrayUtils.toList(value, Object.class), "|");
			return "?" + value + "?";
		}
	}
}
