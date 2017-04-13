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

import static org.apache.juneau.rest.test.TestUtils.*;
import static org.apache.juneau.rest.test.pojos.Constants.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.test.pojos.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class ThirdPartyProxyTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT, JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT, HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT, UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfSerializer.DEFAULT_XMLABBREV, RdfParser.DEFAULT_XML },
		});
	}

	private ThirdPartyProxy proxy;

	public ThirdPartyProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = getCached(label, ThirdPartyProxy.class);
		if (proxy == null) {
			this.proxy = getClient(label, serializer, parser).getRemoteableProxy(ThirdPartyProxy.class, null, serializer, parser);
			cache(label, proxy);
		}
	}

	//--------------------------------------------------------------------------------
	// Header tests
	//--------------------------------------------------------------------------------

	@Test
	public void a01_primitiveHeaders() throws Exception {
		String r = proxy.primitiveHeaders(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void a02_primitiveCollectionHeaders() throws Exception {
		String r = proxy.primitiveCollectionHeaders(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a03_beanHeaders() throws Exception {
		String r = proxy.beanHeaders(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void a04_typedBeanHeaders() throws Exception {
		String r = proxy.typedBeanHeaders(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void a05_swappedPojoHeaders() throws Exception {
		String r = proxy.swappedPojoHeaders(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a06_implicitSwappedPojoHeaders() throws Exception {
		String r = proxy.implicitSwappedPojoHeaders(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a07_enumHeaders() throws Exception {
		String r = proxy.enumHeaders(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// Query tests
	//--------------------------------------------------------------------------------

	@Test
	public void b01_primitiveQueries() throws Exception {
		String r = proxy.primitiveQueries(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void b02_primitiveCollectionQueries() throws Exception {
		String r = proxy.primitiveCollectionQueries(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void b03_beanQueries() throws Exception {
		String r = proxy.beanQueries(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void b04_typedBeanQueries() throws Exception {
		String r = proxy.typedBeanQueries(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void b05_swappedPojoQueries() throws Exception {
		String r = proxy.swappedPojoQueries(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b06_implicitSwappedPojoQueries() throws Exception {
		String r = proxy.implicitSwappedPojoQueries(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b07_enumQueries() throws Exception {
		String r = proxy.enumQueries(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// FormData tests
	//--------------------------------------------------------------------------------

	@Test
	public void c01_primitiveFormData() throws Exception {
		String r = proxy.primitiveFormData(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void c02_primitiveCollectionFormData() throws Exception {
		String r = proxy.primitiveCollectionFormData(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c03_beanFormData() throws Exception {
		String r = proxy.beanFormData(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void c04_typedBeanFormData() throws Exception {
		String r = proxy.typedBeanFormData(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void c05_swappedPojoFormData() throws Exception {
		String r = proxy.swappedPojoFormData(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c06_implicitSwappedPojoFormData() throws Exception {
		String r = proxy.implicitSwappedPojoFormData(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c07_enumFormData() throws Exception {
		String r = proxy.enumFormData(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void da01_returnVoid() {
		proxy.returnVoid();
	}

	@Test
	public void da02_returnInteger() {
		assertEquals((Integer)1, proxy.returnInteger());
	}

	@Test
	public void da03_returnInt() {
		assertEquals(1, proxy.returnInt());
	}

	@Test
	public void da04_returnBoolean() {
		assertEquals(true, proxy.returnBoolean());
	}

	@Test
	public void da05_returnFloat() {
		assertTrue(1f == proxy.returnFloat());
	}

	@Test
	public void da06_returnFloatObject() {
		assertTrue(1f == proxy.returnFloatObject());
	}

	@Test
	public void da07_returnString() {
		assertEquals("foobar", proxy.returnString());
	}

	@Test
	public void da08_returnNullString() {
		assertNull(proxy.returnNullString());
	}

	@Test
	public void da09_returnInt3dArray() {
		assertObjectEquals("[[[1,2],null],null]", proxy.returnInt3dArray());
	}

	@Test
	public void da10_returnInteger3dArray() {
		assertObjectEquals("[[[1,null],null],null]", proxy.returnInteger3dArray());
	}

	@Test
	public void da11_returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null],null],null]", proxy.returnString3dArray());
	}

	@Test
	public void da12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertObjectEquals("[1,null]", x);
		assertClass(Integer.class, x.get(0));
	}

	@Test
	public void da13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertObjectEquals("[[[1,null],null],null]", x);
		assertClass(Integer.class, x.get(0).get(0).get(0));
	}

	@Test
	public void da14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertClass(Integer.class, x.get(0)[0][0][0]);
	}

	@Test
	public void da15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertClass(int[][][].class, x.get(0));
	}

	@Test
	public void da16_returnStringList() {
		assertObjectEquals("['foo','bar',null]", proxy.returnStringList());
	}

	// Beans

	@Test
	public void db01_returnBean() {
		ABean x = proxy.returnBean();
		assertObjectEquals("{a:1,b:'foo'}", x);
		assertClass(ABean.class, x);
	}

	@Test
	public void db02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
		assertClass(ABean.class, x[0][0][0]);
	}

	@Test
	public void db03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertObjectEquals("[{a:1,b:'foo'}]", x);
		assertClass(ABean.class, x.get(0));
	}

	@Test
	public void db04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
		assertClass(ABean.class, x.get(0)[0][0][0]);
	}

	@Test
	public void db05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
		assertClass(ABean.class, x.get("foo"));
	}

	@Test
	public void db06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
		assertClass(ABean.class, x.get("foo").get(0));
	}

	@Test
	public void db07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void db08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);
		assertClass(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@Test
	public void dc01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
		assertClass(TypedBeanImpl.class, x);
	}

	@Test
	public void dc02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
		assertClass(TypedBeanImpl.class, x[0][0][0]);
	}

	@Test
	public void dc03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
		assertClass(TypedBeanImpl.class, x.get(0));
	}

	@Test
	public void dc04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
		assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@Test
	public void dc05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
		assertClass(TypedBeanImpl.class, x.get("foo"));
	}

	@Test
	public void dc06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@Test
	public void dc07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void dc08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@Test
	public void dd01_returnSwappedPojo() {
		SwappedPojo x = proxy.returnSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void dd02_returnSwappedPojo3dArray() {
		SwappedPojo[][][] x = proxy.returnSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void dd03_returnSwappedPojoMap() {
		Map<SwappedPojo,SwappedPojo> x = proxy.returnSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void dd04_returnSwappedPojo3dMap() {
		Map<SwappedPojo,SwappedPojo[][][]> x = proxy.returnSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void de01_returnImplicitSwappedPojo() {
		ImplicitSwappedPojo x = proxy.returnImplicitSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void de02_returnImplicitSwappedPojo3dArray() {
		ImplicitSwappedPojo[][][] x = proxy.returnImplicitSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void de03_returnImplicitSwappedPojoMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x = proxy.returnImplicitSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void de04_returnImplicitSwappedPojo3dMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x = proxy.returnImplicitSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void df01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertObjectEquals("'TWO'", x);
	}

	@Test
	public void df02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertObjectEquals("[[['TWO',null],null],null]", x);
		assertClass(TestEnum.class, x[0][0][0]);
	}

	@Test
	public void df03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertObjectEquals("['TWO',null]", x);
		assertClass(TestEnum.class, x.get(0));
	}

	@Test
	public void df04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertObjectEquals("[[['TWO',null],null,null]]", x);
		assertClass(TestEnum.class, x.get(0).get(0).get(0));
	}

	@Test
	public void df05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertClass(TestEnum[][][].class, x.get(0));
	}

	@Test
	public void df06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum.class, e.getValue());
	}

	@Test
	public void df07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue());
	}

	@Test
	public void df08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		assertClass(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
	}

	//--------------------------------------------------------------------------------
	// Test Body
	//--------------------------------------------------------------------------------

	// Various primitives

	@Test
	public void ea01_setInt() {
		proxy.setInt(1);
	}

	@Test
	public void ea02_setWrongInt() {
		try {
			proxy.setInt(2);
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected:<1> but was:<2>", e.getMessage());
		}
	}

	@Test
	public void ea03_setInteger() {
		proxy.setInteger(1);
	}

	@Test
	public void ea04_setBoolean() {
		proxy.setBoolean(true);
	}

	@Test
	public void ea05_setFloat() {
		proxy.setFloat(1f);
	}

	@Test
	public void ea06_setFloatObject() {
		proxy.setFloatObject(1f);
	}

	@Test
	public void ea07_setString() {
		proxy.setString("foo");
	}

	@Test
	public void ea08_setNullString() {
		proxy.setNullString(null);
	}

	@Test
	public void ea09_setNullStringBad() {
		try {
			proxy.setNullString("foo");
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected null, but was:<foo>", e.getLocalizedMessage());
		}
	}

	@Test
	public void ea10_setInt3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1,2},null},null});
	}

	@Test
	public void ea11_setInteger3dArray() {
		proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null});
	}

	@Test
	public void ea12_setString3dArray() {
		proxy.setString3dArray(new String[][][]{{{"foo",null},null},null});
	}

	@Test
	public void ea13_setIntegerList() {
		proxy.setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void ea14_setInteger3dList() {
		proxy.setInteger3dList(
			new AList<List<List<Integer>>>()
			.append(
				new AList<List<Integer>>()
				.append(new AList<Integer>().append(1).append(null))
				.append(null)
			)
			.append(null)
		);
	}

	@Test
	public void ea15_setInteger1d3dList() {
		proxy.setInteger1d3dList(
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null)
		);
	}

	@Test
	public void ea16_setInt1d3dList() {
		proxy.setInt1d3dList(
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null)
		);
	}

	@Test
	public void ea17_setStringList() {
		proxy.setStringList(Arrays.asList("foo","bar",null));
	}

	// Beans
	@Test
	public void eb01_setBean() {
		proxy.setBean(new ABean().init());
	}

	@Test
	public void eb02_setBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{new ABean().init(),null},null},null});
	}

	@Test
	public void eb03_setBeanList() {
		proxy.setBeanList(Arrays.asList(new ABean().init()));
	}

	@Test
	public void eb04_setBean1d3dList() {
		proxy.setBean1d3dList(new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null));
	}

	@Test
	public void eb05_setBeanMap() {
		proxy.setBeanMap(new AMap<String,ABean>().append("foo",new ABean().init()));
	}

	@Test
	public void eb06_setBeanListMap() {
		proxy.setBeanListMap(new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())));
	}

	@Test
	public void eb07_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)));
	}

	@Test
	public void eb08_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init())));
	}

	// Typed beans

	@Test
	public void ec01_setTypedBean() {
		proxy.setTypedBean(new TypedBeanImpl().init());
	}

	@Test
	public void ec02_setTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null});
	}

	@Test
	public void ec03_setTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)new TypedBeanImpl().init()));
	}

	@Test
	public void ec04_setTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null));
	}

	@Test
	public void ec05_setTypedBeanMap() {
		proxy.setTypedBeanMap(new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()));
	}

	@Test
	public void ec06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	@Test
	public void ec07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)));
	}

	@Test
	public void ec08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	// Swapped POJOs

	@Test
	public void ed01_setSwappedPojo() {
		proxy.setSwappedPojo(new SwappedPojo());
	}

	@Test
	public void ed02_setSwappedPojo3dArray() {
		proxy.setSwappedPojo3dArray(new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	@Test
	public void ed03_setSwappedPojoMap() {
		proxy.setSwappedPojoMap(new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()));
	}

	@Test
	public void ed04_setSwappedPojo3dMap() {
		proxy.setSwappedPojo3dMap(new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void ee01_setImplicitSwappedPojo() {
		proxy.setImplicitSwappedPojo(new ImplicitSwappedPojo());
	}

	@Test
	public void ee02_setImplicitSwappedPojo3dArray() {
		proxy.setImplicitSwappedPojo3dArray(new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	@Test
	public void ee03_setImplicitSwappedPojoMap() {
		proxy.setImplicitSwappedPojoMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()));
	}

	@Test
	public void ee04_setImplicitSwappedPojo3dMap() {
		proxy.setImplicitSwappedPojo3dMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null}));
	}

	// Enums

	@Test
	public void ef01_setEnum() {
		proxy.setEnum(TestEnum.TWO);
	}

	@Test
	public void ef02_setEnum3d() {
		proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@Test
	public void ef03_setEnumList() {
		proxy.setEnumList(new AList<TestEnum>().append(TestEnum.TWO).append(null));
	}

	@Test
	public void ef04_setEnum3dList() {
		proxy.setEnum3dList(
			new AList<List<List<TestEnum>>>()
			.append(
				new AList<List<TestEnum>>()
				.append(
					new AList<TestEnum>().append(TestEnum.TWO).append(null)
				)
				.append(null)
			.append(null)
			)
		);
	}

	@Test
	public void ef05_setEnum1d3dList() {
		proxy.setEnum1d3dList(new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
	}

	@Test
	public void ef06_setEnumMap() {
		proxy.setEnumMap(new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void ef07_setEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void ef08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null)));
	}


	//--------------------------------------------------------------------------------
	// Proxy class
	//--------------------------------------------------------------------------------

	@Remoteable(path="/testThirdPartyProxy")
	public static interface ThirdPartyProxy {

		//--------------------------------------------------------------------------------
		// Header tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="GET", path="/primitiveHeaders")
		String primitiveHeaders(
			@Header("h1") String h1,
			@Header("h1n") String h1n,
			@Header("h2") int h2,
			@Header("h3") Integer h3,
			@Header("h3n") Integer h3n,
			@Header("h4") Boolean h4,
			@Header("h5") float h5,
			@Header("h6") Float h6
		);

		@RemoteMethod(httpMethod="GET", path="/primitiveCollectionHeaders")
		String primitiveCollectionHeaders(
			@Header("h1") int[][][] h1,
			@Header("h2") Integer[][][] h2,
			@Header("h3") String[][][] h3,
			@Header("h4") List<Integer> h4,
			@Header("h5") List<List<List<Integer>>> h5,
			@Header("h6") List<Integer[][][]> h6,
			@Header("h7") List<int[][][]> h7,
			@Header("h8") List<String> h8
		);

		@RemoteMethod(httpMethod="GET", path="/beanHeaders")
		String beanHeaders(
			@Header("h1") ABean h1,
			@Header("h1n") ABean h1n,
			@Header("h2") ABean[][][] h2,
			@Header("h3") List<ABean> h3,
			@Header("h4") List<ABean[][][]> h4,
			@Header("h5") Map<String,ABean> h5,
			@Header("h6") Map<String,List<ABean>> h6,
			@Header("h7") Map<String,List<ABean[][][]>> h7,
			@Header("h8") Map<Integer,List<ABean>> h8
		);

		@RemoteMethod(httpMethod="GET", path="/typedBeanHeaders")
		String typedBeanHeaders(
			@Header("h1") TypedBean h1,
			@Header("h1n") TypedBean h1n,
			@Header("h2") TypedBean[][][] h2,
			@Header("h3") List<TypedBean> h3,
			@Header("h4") List<TypedBean[][][]> h4,
			@Header("h5") Map<String,TypedBean> h5,
			@Header("h6") Map<String,List<TypedBean>> h6,
			@Header("h7") Map<String,List<TypedBean[][][]>> h7,
			@Header("h8") Map<Integer,List<TypedBean>> h8
		);

		@RemoteMethod(httpMethod="GET", path="/swappedPojoHeaders")
		String swappedPojoHeaders(
			@Header("h1") SwappedPojo h1,
			@Header("h2") SwappedPojo[][][] h2,
			@Header("h3") Map<SwappedPojo,SwappedPojo> h3,
			@Header("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="GET", path="/implicitSwappedPojoHeaders")
		String implicitSwappedPojoHeaders(
			@Header("h1") ImplicitSwappedPojo h1,
			@Header("h2") ImplicitSwappedPojo[][][] h2,
			@Header("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@Header("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="GET", path="/enumHeaders")
		String enumHeaders(
			@Header("h1") TestEnum h1,
			@Header("h1n") TestEnum h1n,
			@Header("h2") TestEnum[][][] h2,
			@Header("h3") List<TestEnum> h3,
			@Header("h4") List<List<List<TestEnum>>> h4,
			@Header("h5") List<TestEnum[][][]> h5,
			@Header("h6") Map<TestEnum,TestEnum> h6,
			@Header("h7") Map<TestEnum,TestEnum[][][]> h7,
			@Header("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		);

		//--------------------------------------------------------------------------------
		// Query tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="GET", path="/primitiveQueries")
		String primitiveQueries(
			@Query("h1") String h1,
			@Query("h1n") String h1n,
			@Query("h2") int h2,
			@Query("h3") Integer h3,
			@Query("h3n") Integer h3n,
			@Query("h4") Boolean h4,
			@Query("h5") float h5,
			@Query("h6") Float h6
		);

		@RemoteMethod(httpMethod="GET", path="/primitiveCollectionQueries")
		String primitiveCollectionQueries(
			@Query("h1") int[][][] h1,
			@Query("h2") Integer[][][] h2,
			@Query("h3") String[][][] h3,
			@Query("h4") List<Integer> h4,
			@Query("h5") List<List<List<Integer>>> h5,
			@Query("h6") List<Integer[][][]> h6,
			@Query("h7") List<int[][][]> h7,
			@Query("h8") List<String> h8
		);

		@RemoteMethod(httpMethod="GET", path="/beanQueries")
		String beanQueries(
			@Query("h1") ABean h1,
			@Query("h1n") ABean h1n,
			@Query("h2") ABean[][][] h2,
			@Query("h3") List<ABean> h3,
			@Query("h4") List<ABean[][][]> h4,
			@Query("h5") Map<String,ABean> h5,
			@Query("h6") Map<String,List<ABean>> h6,
			@Query("h7") Map<String,List<ABean[][][]>> h7,
			@Query("h8") Map<Integer,List<ABean>> h8
		);

		@RemoteMethod(httpMethod="GET", path="/typedBeanQueries")
		String typedBeanQueries(
			@Query("h1") TypedBean h1,
			@Query("h1n") TypedBean h1n,
			@Query("h2") TypedBean[][][] h2,
			@Query("h3") List<TypedBean> h3,
			@Query("h4") List<TypedBean[][][]> h4,
			@Query("h5") Map<String,TypedBean> h5,
			@Query("h6") Map<String,List<TypedBean>> h6,
			@Query("h7") Map<String,List<TypedBean[][][]>> h7,
			@Query("h8") Map<Integer,List<TypedBean>> h8
		);

		@RemoteMethod(httpMethod="GET", path="/swappedPojoQueries")
		String swappedPojoQueries(
			@Query("h1") SwappedPojo h1,
			@Query("h2") SwappedPojo[][][] h2,
			@Query("h3") Map<SwappedPojo,SwappedPojo> h3,
			@Query("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="GET", path="/implicitSwappedPojoQueries")
		String implicitSwappedPojoQueries(
			@Query("h1") ImplicitSwappedPojo h1,
			@Query("h2") ImplicitSwappedPojo[][][] h2,
			@Query("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@Query("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="GET", path="/enumQueries")
		String enumQueries(
			@Query("h1") TestEnum h1,
			@Query("h1n") TestEnum h1n,
			@Query("h2") TestEnum[][][] h2,
			@Query("h3") List<TestEnum> h3,
			@Query("h4") List<List<List<TestEnum>>> h4,
			@Query("h5") List<TestEnum[][][]> h5,
			@Query("h6") Map<TestEnum,TestEnum> h6,
			@Query("h7") Map<TestEnum,TestEnum[][][]> h7,
			@Query("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		);

		//--------------------------------------------------------------------------------
		// FormData tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/primitiveFormData")
		String primitiveFormData(
			@FormData("h1") String h1,
			@FormData("h1n") String h1n,
			@FormData("h2") int h2,
			@FormData("h3") Integer h3,
			@FormData("h3n") Integer h3n,
			@FormData("h4") Boolean h4,
			@FormData("h5") float h5,
			@FormData("h6") Float h6
		);

		@RemoteMethod(httpMethod="POST", path="/primitiveCollectionFormData")
		String primitiveCollectionFormData(
			@FormData("h1") int[][][] h1,
			@FormData("h2") Integer[][][] h2,
			@FormData("h3") String[][][] h3,
			@FormData("h4") List<Integer> h4,
			@FormData("h5") List<List<List<Integer>>> h5,
			@FormData("h6") List<Integer[][][]> h6,
			@FormData("h7") List<int[][][]> h7,
			@FormData("h8") List<String> h8
		);

		@RemoteMethod(httpMethod="POST", path="/beanFormData")
		String beanFormData(
			@FormData("h1") ABean h1,
			@FormData("h1n") ABean h1n,
			@FormData("h2") ABean[][][] h2,
			@FormData("h3") List<ABean> h3,
			@FormData("h4") List<ABean[][][]> h4,
			@FormData("h5") Map<String,ABean> h5,
			@FormData("h6") Map<String,List<ABean>> h6,
			@FormData("h7") Map<String,List<ABean[][][]>> h7,
			@FormData("h8") Map<Integer,List<ABean>> h8
		);

		@RemoteMethod(httpMethod="POST", path="/typedBeanFormData")
		String typedBeanFormData(
			@FormData("h1") TypedBean h1,
			@FormData("h1n") TypedBean h1n,
			@FormData("h2") TypedBean[][][] h2,
			@FormData("h3") List<TypedBean> h3,
			@FormData("h4") List<TypedBean[][][]> h4,
			@FormData("h5") Map<String,TypedBean> h5,
			@FormData("h6") Map<String,List<TypedBean>> h6,
			@FormData("h7") Map<String,List<TypedBean[][][]>> h7,
			@FormData("h8") Map<Integer,List<TypedBean>> h8
		);

		@RemoteMethod(httpMethod="POST", path="/swappedPojoFormData")
		String swappedPojoFormData(
			@FormData("h1") SwappedPojo h1,
			@FormData("h2") SwappedPojo[][][] h2,
			@FormData("h3") Map<SwappedPojo,SwappedPojo> h3,
			@FormData("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="POST", path="/implicitSwappedPojoFormData")
		String implicitSwappedPojoFormData(
			@FormData("h1") ImplicitSwappedPojo h1,
			@FormData("h2") ImplicitSwappedPojo[][][] h2,
			@FormData("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@FormData("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		);

		@RemoteMethod(httpMethod="POST", path="/enumFormData")
		String enumFormData(
			@FormData("h1") TestEnum h1,
			@FormData("h1n") TestEnum h1n,
			@FormData("h2") TestEnum[][][] h2,
			@FormData("h3") List<TestEnum> h3,
			@FormData("h4") List<List<List<TestEnum>>> h4,
			@FormData("h5") List<TestEnum[][][]> h5,
			@FormData("h6") Map<TestEnum,TestEnum> h6,
			@FormData("h7") Map<TestEnum,TestEnum[][][]> h7,
			@FormData("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		);

		//--------------------------------------------------------------------------------
		// Test return types.
		//--------------------------------------------------------------------------------

		// Various primitives

		@RemoteMethod(httpMethod="GET", path="/returnVoid")
		void returnVoid();

		@RemoteMethod(httpMethod="GET", path="/returnInt")
		int returnInt();

		@RemoteMethod(httpMethod="GET", path="/returnInteger")
		Integer returnInteger();

		@RemoteMethod(httpMethod="GET", path="/returnBoolean")
		boolean returnBoolean();

		@RemoteMethod(httpMethod="GET", path="/returnFloat")
		float returnFloat();

		@RemoteMethod(httpMethod="GET", path="/returnFloatObject")
		Float returnFloatObject();

		@RemoteMethod(httpMethod="GET", path="/returnString")
		String returnString();

		@RemoteMethod(httpMethod="GET", path="/returnNullString")
		String returnNullString();

		@RemoteMethod(httpMethod="GET", path="/returnInt3dArray")
		int[][][] returnInt3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnInteger3dArray")
		Integer[][][] returnInteger3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnString3dArray")
		String[][][] returnString3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnIntegerList")
		List<Integer> returnIntegerList();

		@RemoteMethod(httpMethod="GET", path="/returnInteger3dList")
		List<List<List<Integer>>> returnInteger3dList();

		@RemoteMethod(httpMethod="GET", path="/returnInteger1d3dList")
		List<Integer[][][]> returnInteger1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnInt1d3dList")
		List<int[][][]> returnInt1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnStringList")
		List<String> returnStringList();

		// Beans

		@RemoteMethod(httpMethod="GET", path="/returnBean")
		ABean returnBean();

		@RemoteMethod(httpMethod="GET", path="/returnBean3dArray")
		ABean[][][] returnBean3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnBeanList")
		List<ABean> returnBeanList();

		@RemoteMethod(httpMethod="GET", path="/returnBean1d3dList")
		List<ABean[][][]> returnBean1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnBeanMap")
		Map<String,ABean> returnBeanMap();

		@RemoteMethod(httpMethod="GET", path="/returnBeanListMap")
		Map<String,List<ABean>> returnBeanListMap();

		@RemoteMethod(httpMethod="GET", path="/returnBean1d3dListMap")
		Map<String,List<ABean[][][]>> returnBean1d3dListMap();

		@RemoteMethod(httpMethod="GET", path="/returnBeanListMapIntegerKeys")
		Map<Integer,List<ABean>> returnBeanListMapIntegerKeys();

		// Typed beans

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean")
		TypedBean returnTypedBean();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean3dArray")
		TypedBean[][][] returnTypedBean3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanList")
		List<TypedBean> returnTypedBeanList();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean1d3dList")
		List<TypedBean[][][]> returnTypedBean1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanMap")
		Map<String,TypedBean> returnTypedBeanMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanListMap")
		Map<String,List<TypedBean>> returnTypedBeanListMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean1d3dListMap")
		Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanListMapIntegerKeys")
		Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys();

		// Swapped POJOs

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo")
		SwappedPojo returnSwappedPojo();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo3dArray")
		SwappedPojo[][][] returnSwappedPojo3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojoMap")
		Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo3dMap")
		Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap();

		// Implicit swapped POJOs

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo")
		ImplicitSwappedPojo returnImplicitSwappedPojo();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo3dArray")
		ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojoMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo3dMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap();

		// Enums

		@RemoteMethod(httpMethod="GET", path="/returnEnum")
		TestEnum returnEnum();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3d")
		TestEnum[][][] returnEnum3d();

		@RemoteMethod(httpMethod="GET", path="/returnEnumList")
		List<TestEnum> returnEnumList();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3dList")
		List<List<List<TestEnum>>> returnEnum3dList();

		@RemoteMethod(httpMethod="GET", path="/returnEnum1d3dList")
		List<TestEnum[][][]> returnEnum1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnEnumMap")
		Map<TestEnum,TestEnum> returnEnumMap();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3dArrayMap")
		Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap();

		@RemoteMethod(httpMethod="GET", path="/returnEnum1d3dListMap")
		Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap();

		//--------------------------------------------------------------------------------
		// Test parameters
		//--------------------------------------------------------------------------------

		// Various primitives

		@RemoteMethod(httpMethod="POST", path="/setInt")
		void setInt(@Body int x);

		@RemoteMethod(httpMethod="POST", path="/setInteger")
		void setInteger(@Body Integer x);

		@RemoteMethod(httpMethod="POST", path="/setBoolean")
		void setBoolean(@Body boolean x);

		@RemoteMethod(httpMethod="POST", path="/setFloat")
		void setFloat(@Body float x);

		@RemoteMethod(httpMethod="POST", path="/setFloatObject")
		void setFloatObject(@Body Float x);

		@RemoteMethod(httpMethod="POST", path="/setString")
		void setString(@Body String x);

		@RemoteMethod(httpMethod="POST", path="/setNullString")
		void setNullString(@Body String x);

		@RemoteMethod(httpMethod="POST", path="/setInt3dArray")
		void setInt3dArray(@Body int[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setInteger3dArray")
		void setInteger3dArray(@Body Integer[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setString3dArray")
		void setString3dArray(@Body String[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setIntegerList")
		void setIntegerList(@Body List<Integer> x);

		@RemoteMethod(httpMethod="POST", path="/setInteger3dList")
		void setInteger3dList(@Body List<List<List<Integer>>> x);

		@RemoteMethod(httpMethod="POST", path="/setInteger1d3dList")
		void setInteger1d3dList(@Body List<Integer[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setInt1d3dList")
		void setInt1d3dList(@Body List<int[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setStringList")
		void setStringList(@Body List<String> x);

		// Beans

		@RemoteMethod(httpMethod="POST", path="/setBean")
		void setBean(@Body ABean x);

		@RemoteMethod(httpMethod="POST", path="/setBean3dArray")
		void setBean3dArray(@Body ABean[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setBeanList")
		void setBeanList(@Body List<ABean> x);

		@RemoteMethod(httpMethod="POST", path="/setBean1d3dList")
		void setBean1d3dList(@Body List<ABean[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanMap")
		void setBeanMap(@Body Map<String,ABean> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanListMap")
		void setBeanListMap(@Body Map<String,List<ABean>> x);

		@RemoteMethod(httpMethod="POST", path="/setBean1d3dListMap")
		void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanListMapIntegerKeys")
		void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x);

		// Typed beans

		@RemoteMethod(httpMethod="POST", path="/setTypedBean")
		void setTypedBean(@Body TypedBean x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean3dArray")
		void setTypedBean3dArray(@Body TypedBean[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanList")
		void setTypedBeanList(@Body List<TypedBean> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean1d3dList")
		void setTypedBean1d3dList(@Body List<TypedBean[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanMap")
		void setTypedBeanMap(@Body Map<String,TypedBean> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanListMap")
		void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean1d3dListMap")
		void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanListMapIntegerKeys")
		void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo")
		void setSwappedPojo(@Body SwappedPojo x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo3dArray")
		void setSwappedPojo3dArray(@Body SwappedPojo[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojoMap")
		void setSwappedPojoMap(@Body Map<SwappedPojo,SwappedPojo> x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo3dMap")
		void setSwappedPojo3dMap(@Body Map<SwappedPojo,SwappedPojo[][][]> x);

		// Implicit swapped POJOs

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo")
		void setImplicitSwappedPojo(@Body ImplicitSwappedPojo x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo3dArray")
		void setImplicitSwappedPojo3dArray(@Body ImplicitSwappedPojo[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojoMap")
		void setImplicitSwappedPojoMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo3dMap")
		void setImplicitSwappedPojo3dMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x);

		// Enums

		@RemoteMethod(httpMethod="POST", path="/setEnum")
		void setEnum(@Body TestEnum x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3d")
		void setEnum3d(@Body TestEnum[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setEnumList")
		void setEnumList(@Body List<TestEnum> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3dList")
		void setEnum3dList(@Body List<List<List<TestEnum>>> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum1d3dList")
		void setEnum1d3dList(@Body List<TestEnum[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setEnumMap")
		void setEnumMap(@Body Map<TestEnum,TestEnum> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3dArrayMap")
		void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum1d3dListMap")
		void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x);
	}
}
