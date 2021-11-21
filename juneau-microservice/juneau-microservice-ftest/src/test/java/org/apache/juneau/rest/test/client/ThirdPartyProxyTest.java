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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.testutils.Constants.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Request;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.test.*;
import org.apache.juneau.rest.test.client.ThirdPartyProxyTest.ThirdPartyProxy.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Parameterized.class)
public class ThirdPartyProxyTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfXmlAbbrevSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), RdfXmlParser.DEFAULT },
		});
	}

	private ThirdPartyProxy proxy;

	public ThirdPartyProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = getCached(label, ThirdPartyProxy.class);
		if (proxy == null) {
			proxy = TestMicroservice.client(serializer, parser).ignoreErrors().partSerializer(UonSerializer.create().addBeanTypes().addRootType().build()).build().getRemote(ThirdPartyProxy.class, null, serializer, parser);
			cache(label, proxy);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Temporary exhaustive test.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	@Ignore
	public void a00_lotsOfSetInt3dArray() {
		final AtomicLong time = new AtomicLong(System.currentTimeMillis());
		final AtomicInteger iteration = new AtomicInteger(0);
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - time.get() > 10000) {
					try {
						System.err.println("Failed at iteration " + iteration.get());  // NOT DEBUG
						TestMicroservice.jettyDump(null, null);
						System.exit(2);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000);
		for (int i = 0; i < 100000; i++) {
			iteration.set(i);
			String s = proxy.setInt3dArray(new int[][][]{{{i},null},null}, i);
			if (i % 1000 == 0)
				System.err.println("response="+s);  // NOT DEBUG
			time.set(System.currentTimeMillis());
		}
		timer.cancel();
	}



	//-----------------------------------------------------------------------------------------------------------------
	// Header tests
	//-----------------------------------------------------------------------------------------------------------------

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
			AList.of(1,null),
			AList.of(AList.of(AList.of(1,null),null),null),
			AList.of(new Integer[][][]{{{1,null},null},null},null),
			AList.of(new int[][][]{{{1,2},null},null},null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a03_beanHeaders() throws Exception {
		String r = proxy.beanHeaders(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			AList.of(ABean.get(),null),
			AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null),
			AMap.of("foo",ABean.get()),
			AMap.of("foo",Arrays.asList(ABean.get())),
			AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList(ABean.get()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void a04_typedBeanHeaders() throws Exception {
		String r = proxy.typedBeanHeaders(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			AList.of(TypedBeanImpl.get(),null),
			AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			AMap.of("foo",TypedBeanImpl.get()),
			AMap.of("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())),
			AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void a05_swappedObjectHeaders() throws Exception {
		String r = proxy.swappedObjectHeaders(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			AMap.of(new SwappedObject(),new SwappedObject()),
			AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a06_implicitSwappedObjectHeaders() throws Exception {
		String r = proxy.implicitSwappedObjectHeaders(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a07_enumHeaders() throws Exception {
		String r = proxy.enumHeaders(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			AList.of(TestEnum.TWO,null),
			AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null),
			AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			AMap.of(TestEnum.ONE,TestEnum.TWO),
			AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void a08_mapHeader() throws Exception {
		String r = proxy.mapHeader(
			AMap.of("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a09_beanHeader() throws Exception {
		String r = proxy.beanHeader(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void a10_headerList() throws Exception {
		String r = proxy.headerList(
			headerList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query tests
	//-----------------------------------------------------------------------------------------------------------------

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
			AList.of(1,null),
			AList.of(AList.of(AList.of(1,null),null),null),
			AList.of(new Integer[][][]{{{1,null},null},null},null),
			AList.of(new int[][][]{{{1,2},null},null},null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void b03_beanQueries() throws Exception {
		String r = proxy.beanQueries(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			AList.of(ABean.get(),null),
			AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null),
			AMap.of("foo",ABean.get()),
			AMap.of("foo",Arrays.asList(ABean.get())),
			AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList(ABean.get()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void b04_typedBeanQueries() throws Exception {
		String r = proxy.typedBeanQueries(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			AList.of(TypedBeanImpl.get(),null),
			AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			AMap.of("foo",TypedBeanImpl.get()),
			AMap.of("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())),
			AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void b05_swappedObjectQueries() throws Exception {
		String r = proxy.swappedObjectQueries(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			AMap.of(new SwappedObject(),new SwappedObject()),
			AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b06_implicitSwappedObjectQueries() throws Exception {
		String r = proxy.implicitSwappedObjectQueries(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b07_enumQueries() throws Exception {
		String r = proxy.enumQueries(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			AList.of(TestEnum.TWO,null),
			AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null),
			AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			AMap.of(TestEnum.ONE,TestEnum.TWO),
			AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void b08_stringQuery1() throws Exception {
		String r = proxy.stringQuery1("a=1&b=foo");
		assertEquals("OK", r);
	}

	@Test
	public void b09_stringQuery2() throws Exception {
		String r = proxy.stringQuery2("a=1&b=foo");
		assertEquals("OK", r);
	}

	@Test
	public void b10_mapQuery() throws Exception {
		String r = proxy.mapQuery(
			AMap.of("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@Test
	public void b11_beanQuery() throws Exception {
		String r = proxy.beanQuery(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void b12_partListQuery() throws Exception {
		String r = proxy.partListQuery(
			partList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// FormData tests
	//-----------------------------------------------------------------------------------------------------------------

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
			AList.of(1,null),
			AList.of(AList.of(AList.of(1,null),null),null),
			AList.of(new Integer[][][]{{{1,null},null},null},null),
			AList.of(new int[][][]{{{1,2},null},null},null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c03_beanFormData() throws Exception {
		String r = proxy.beanFormData(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			AList.of(ABean.get(),null),
			AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null),
			AMap.of("foo",ABean.get()),
			AMap.of("foo",Arrays.asList(ABean.get())),
			AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList(ABean.get()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void c04_typedBeanFormData() throws Exception {
		String r = proxy.typedBeanFormData(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			AList.of(TypedBeanImpl.get(),null),
			AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			AMap.of("foo",TypedBeanImpl.get()),
			AMap.of("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())),
			AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			AMap.of(1,Arrays.asList((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void c05_swappedObjectFormData() throws Exception {
		String r = proxy.swappedObjectFormData(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			AMap.of(new SwappedObject(),new SwappedObject()),
			AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c06_implicitSwappedObjectFormData() throws Exception {
		String r = proxy.implicitSwappedObjectFormData(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c07_enumFormData() throws Exception {
		String r = proxy.enumFormData(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			AList.of(TestEnum.TWO,null),
			AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null),
			AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			AMap.of(TestEnum.ONE,TestEnum.TWO),
			AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void c08_mapFormData() throws Exception {
		String r = proxy.mapFormData(
			AMap.of("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c09_beanFormData() throws Exception {
		String r = proxy.beanFormData(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void c10_partListFormData() throws Exception {
		String r = proxy.partListFormData(
			partList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

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
		assertObject(proxy.returnInt3dArray()).asJson().is("[[[1,2],null],null]");
	}

	@Test
	public void da10_returnInteger3dArray() {
		assertObject(proxy.returnInteger3dArray()).asJson().is("[[[1,null],null],null]");
	}

	@Test
	public void da11_returnString3dArray() {
		assertObject(proxy.returnString3dArray()).asJson().is("[[['foo','bar',null],null],null]");
	}

	@Test
	public void da12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertObject(x).asJson().is("[1,null]");
		assertObject(x.get(0)).isType(Integer.class);
	}

	@Test
	public void da13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertObject(x).asJson().is("[[[1,null],null],null]");
		assertObject(x.get(0).get(0).get(0)).isType(Integer.class);
	}

	@Test
	public void da14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertObject(x).asJson().is("[[[[1,null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(Integer.class);
	}

	@Test
	public void da15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertObject(x).asJson().is("[[[[1,2],null],null],null]");
		assertObject(x.get(0)).isType(int[][][].class);
	}

	@Test
	public void da16_returnStringList() {
		assertObject(proxy.returnStringList()).asJson().is("['foo','bar',null]");
	}

	// Beans

	@Test
	public void db01_returnBean() {
		ABean x = proxy.returnBean();
		assertObject(x).asJson().is("{a:1,b:'foo'}");
		assertObject(x).isType(ABean.class);
	}

	@Test
	public void db02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertObject(x[0][0][0]).isType(ABean.class);
	}

	@Test
	public void db03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertObject(x).asJson().is("[{a:1,b:'foo'}]");
		assertObject(x.get(0)).isType(ABean.class);
	}

	@Test
	public void db04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void db05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
		assertObject(x.get("foo")).isType(ABean.class);
	}

	@Test
	public void db06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertObject(x.get("foo").get(0)).isType(ABean.class);
	}

	@Test
	public void db07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertObject(x.get("foo").get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void db08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertObject(x.keySet().iterator().next()).isType(Integer.class);
	}

	// Typed beans

	@Test
	public void dc01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertObject(x).asJson().is("{a:1,b:'foo'}");
		assertObject(x).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertObject(x[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertObject(x).asJson().is("[{a:1,b:'foo'}]");
		assertObject(x.get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
		assertObject(x.get("foo")).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertObject(x.get("foo").get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertObject(x.get("foo").get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void dc08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertObject(x.get(1).get(0)).isType(TypedBeanImpl.class);
	}

	// Swapped POJOs

	@Test
	public void dd01_returnSwappedObject() {
		SwappedObject x = proxy.returnSwappedObject();
		assertObject(x).asJson().is("'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void dd02_returnSwappedObject3dArray() {
		SwappedObject[][][] x = proxy.returnSwappedObject3dArray();
		assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void dd03_returnSwappedObjectMap() {
		Map<SwappedObject,SwappedObject> x = proxy.returnSwappedObjectMap();
		assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<SwappedObject,SwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void dd04_returnSwappedObject3dMap() {
		Map<SwappedObject,SwappedObject[][][]> x = proxy.returnSwappedObject3dMap();
		assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<SwappedObject,SwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void de01_returnImplicitSwappedObject() {
		ImplicitSwappedObject x = proxy.returnImplicitSwappedObject();
		assertObject(x).asJson().is("'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void de02_returnImplicitSwappedObject3dArray() {
		ImplicitSwappedObject[][][] x = proxy.returnImplicitSwappedObject3dArray();
		assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void de03_returnImplicitSwappedObjectMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x = proxy.returnImplicitSwappedObjectMap();
		assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void de04_returnImplicitSwappedObject3dMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x = proxy.returnImplicitSwappedObject3dMap();
		assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void df01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertObject(x).asJson().is("'TWO'");
	}

	@Test
	public void df02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertObject(x).asJson().is("[[['TWO',null],null],null]");
		assertObject(x[0][0][0]).isType(TestEnum.class);
	}

	@Test
	public void df03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertObject(x).asJson().is("['TWO',null]");
		assertObject(x.get(0)).isType(TestEnum.class);
	}

	@Test
	public void df04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertObject(x).asJson().is("[[['TWO',null],null],null]");
		assertObject(x.get(0).get(0).get(0)).isType(TestEnum.class);
	}

	@Test
	public void df05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertObject(x).asJson().is("[[[['TWO',null],null],null],null]");
		assertObject(x.get(0)).isType(TestEnum[][][].class);
	}

	@Test
	public void df06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertObject(x).asJson().is("{ONE:'TWO'}");
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertObject(e.getKey()).isType(TestEnum.class);
		assertObject(e.getValue()).isType(TestEnum.class);
	}

	@Test
	public void df07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertObject(x).asJson().is("{ONE:[[['TWO',null],null],null]}");
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertObject(e.getKey()).isType(TestEnum.class);
		assertObject(e.getValue()).isType(TestEnum[][][].class);
	}

	@Test
	public void df08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertObject(x).asJson().is("{ONE:[[[['TWO',null],null],null],null]}");
		assertObject(x.get(TestEnum.ONE).get(0)).isType(TestEnum[][][].class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test Body
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@Test
	public void ea01_setInt() {
		proxy.setInt(1);
	}

	@Test
	public void ea02_setWrongInt() {
		assertThrown(()->proxy.setInt(2)).messages().any(contains("expected:<1> but was:<2>"));
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
		assertThrown(()->proxy.setNullString("foo")).messages().any(contains("expected null, but was:<foo>"));
	}

	@Test
	public void ea10_setInt3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1},null},null}, 1);
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
		proxy.setIntegerList(AList.of(1,null));
	}

	@Test
	public void ea14_setInteger3dList() {
		proxy.setInteger3dList(AList.of(AList.of(AList.of(1,null),null),null));
	}

	@Test
	public void ea15_setInteger1d3dList() {
		proxy.setInteger1d3dList(AList.of(new Integer[][][]{{{1,null},null},null},null));
	}

	@Test
	public void ea16_setInt1d3dList() {
		proxy.setInt1d3dList(AList.of(new int[][][]{{{1,2},null},null},null));
	}

	@Test
	public void ea17_setStringList() {
		proxy.setStringList(Arrays.asList("foo","bar",null));
	}

	// Beans
	@Test
	public void eb01_setBean() {
		proxy.setBean(ABean.get());
	}

	@Test
	public void eb02_setBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null});
	}

	@Test
	public void eb03_setBeanList() {
		proxy.setBeanList(Arrays.asList(ABean.get()));
	}

	@Test
	public void eb04_setBean1d3dList() {
		proxy.setBean1d3dList(AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null));
	}

	@Test
	public void eb05_setBeanMap() {
		proxy.setBeanMap(AMap.of("foo",ABean.get()));
	}

	@Test
	public void eb06_setBeanListMap() {
		proxy.setBeanListMap(AMap.of("foo",Arrays.asList(ABean.get())));
	}

	@Test
	public void eb07_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null)));
	}

	@Test
	public void eb08_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(AMap.of(1,Arrays.asList(ABean.get())));
	}

	// Typed beans

	@Test
	public void ec01_setTypedBean() {
		proxy.setTypedBean(TypedBeanImpl.get());
	}

	@Test
	public void ec02_setTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null});
	}

	@Test
	public void ec03_setTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)TypedBeanImpl.get()));
	}

	@Test
	public void ec04_setTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
	}

	@Test
	public void ec05_setTypedBeanMap() {
		proxy.setTypedBeanMap(AMap.of("foo",TypedBeanImpl.get()));
	}

	@Test
	public void ec06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(AMap.of("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	@Test
	public void ec07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
	}

	@Test
	public void ec08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(AMap.of(1,Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	// Swapped POJOs

	@Test
	public void ed01_setSwappedObject() {
		proxy.setSwappedObject(new SwappedObject());
	}

	@Test
	public void ed02_setSwappedObject3dArray() {
		proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
	}

	@Test
	public void ed03_setSwappedObjectMap() {
		proxy.setSwappedObjectMap(AMap.of(new SwappedObject(),new SwappedObject()));
	}

	@Test
	public void ed04_setSwappedObject3dMap() {
		proxy.setSwappedObject3dMap(AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void ee01_setImplicitSwappedObject() {
		proxy.setImplicitSwappedObject(new ImplicitSwappedObject());
	}

	@Test
	public void ee02_setImplicitSwappedObject3dArray() {
		proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
	}

	@Test
	public void ee03_setImplicitSwappedObjectMap() {
		proxy.setImplicitSwappedObjectMap(AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject()));
	}

	@Test
	public void ee04_setImplicitSwappedObject3dMap() {
		proxy.setImplicitSwappedObject3dMap(AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
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
		proxy.setEnumList(AList.of(TestEnum.TWO,null));
	}

	@Test
	public void ef04_setEnum3dList() {
		proxy.setEnum3dList(AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null));
	}

	@Test
	public void ef05_setEnum1d3dList() {
		proxy.setEnum1d3dList(AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
	}

	@Test
	public void ef06_setEnumMap() {
		proxy.setEnumMap(AMap.of(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void ef07_setEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void ef08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path variables
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_pathVars1() {
		String r = proxy.pathVars1(1, "foo");
		assertEquals("OK", r);
	}

	@Test
	public void f02_pathVars2() {
		String r = proxy.pathVars2(
			AMap.of("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@Test
	public void f03_pathVars3() {
		String r = proxy.pathVars3(
			ABean.get()
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Path
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ga01_reqBeanPath1() throws Exception {
		String r = proxy.reqBeanPath1(
			new ReqBeanPath1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga01_reqBeanPath1a() throws Exception {
		String r = proxy.reqBeanPath1(
			new ReqBeanPath1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga02_reqBeanPath2() throws Exception {
		String r = proxy.reqBeanPath2(
			new ReqBeanPath2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga03_reqBeanPath3() throws Exception {
		String r = proxy.reqBeanPath3(
			new ReqBeanPath3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga06_reqBeanPath6() throws Exception {
		String r = proxy.reqBeanPath6(
			new ReqBeanPath6() {
				@Override
				public Map<String,Object> getX() {
					return AMap.of("a",1,"b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga07_reqBeanPath7() throws Exception {
		String r = proxy.reqBeanPath7(
			new ReqBeanPath7() {
				@Override
				public ABean getX() {
					return ABean.get();
				}
			}
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Query
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void gb01_reqBeanQuery1() throws Exception {
		String r = proxy.reqBeanQuery1(
			new ReqBeanQuery1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb01_reqBeanQuery1a() throws Exception {
		String r = proxy.reqBeanQuery1(
			new ReqBeanQuery1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb02_reqBeanQuery2() throws Exception {
		String r = proxy.reqBeanQuery2(
			new ReqBeanQuery2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb03_reqBeanQuery3() throws Exception {
		String r = proxy.reqBeanQuery3(
			new ReqBeanQuery3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb06_reqBeanQuery6() throws Exception {
		String r = proxy.reqBeanQuery6(
			new ReqBeanQuery6() {
				@Override
				public Map<String,Object> getX() {
					return AMap.of("a",1,"b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb07_reqBeanQuery7() throws Exception {
		String r = proxy.reqBeanQuery7(
			new ReqBeanQuery7() {
				@Override
				public ABean getX() {
					return ABean.get();
				}
			}
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - FormData
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void gd01_reqBeanFormData1() throws Exception {
		String r = proxy.reqBeanFormData1(
			new ReqBeanFormData1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd01_reqBeanFormData1a() throws Exception {
		String r = proxy.reqBeanFormData1(
			new ReqBeanFormData1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd02_reqBeanFormData2() throws Exception {
		String r = proxy.reqBeanFormData2(
			new ReqBeanFormData2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd03_reqBeanFormData3() throws Exception {
		String r = proxy.reqBeanFormData3(
			new ReqBeanFormData3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd06_reqBeanFormData6() throws Exception {
		String r = proxy.reqBeanFormData6(
			new ReqBeanFormData6() {
				@Override
				public Map<String,Object> getX() {
					return AMap.of("a",1,"b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd07_reqBeanFormData7() throws Exception {
		String r = proxy.reqBeanFormData7(
			new ReqBeanFormData7() {
				@Override
				public ABean getX() {
					return ABean.get();
				}
			}
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Header
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void gf01_reqBeanHeader1() throws Exception {
		String r = proxy.reqBeanHeader1(
			new ReqBeanHeader1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf01_reqBeanHeader1a() throws Exception {
		String r = proxy.reqBeanHeader1(
			new ReqBeanHeader1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf02_reqBeanHeader2() throws Exception {
		String r = proxy.reqBeanHeader2(
			new ReqBeanHeader2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf03_reqBeanHeader3() throws Exception {
		String r = proxy.reqBeanHeader3(
			new ReqBeanHeader3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf06_reqBeanHeader6() throws Exception {
		String r = proxy.reqBeanHeader6(
			new ReqBeanHeader6() {
				@Override
				public Map<String,Object> getX() {
					return AMap.of("a",1,"b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf07_reqBeanHeader7() throws Exception {
		String r = proxy.reqBeanHeader7(
			new ReqBeanHeader7() {
				@Override
				public ABean getX() {
					return ABean.get();
				}
			}
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// PartFormatters
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void h01() throws Exception {
		String r = proxy.partFormatters("1", "2", "3", "4");
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RemoteOp(returns=HTTP_STATUS)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void i01a() throws Exception {
		int r = proxy.httpStatusReturnInt200();
		assertEquals(200, r);
	}

	@Test
	public void i01b() throws Exception {
		Integer r = proxy.httpStatusReturnInteger200();
		assertEquals(200, r.intValue());
	}

	@Test
	public void i01c() throws Exception {
		int r = proxy.httpStatusReturnInt404();
		assertEquals(404, r);
	}

	@Test
	public void i01d() throws Exception {
		Integer r = proxy.httpStatusReturnInteger404();
		assertEquals(404, r.intValue());
	}

	@Test
	public void i02a() throws Exception {
		boolean r = proxy.httpStatusReturnBool200();
		assertEquals(true, r);
	}

	@Test
	public void i02b() throws Exception {
		Boolean r = proxy.httpStatusReturnBoolean200();
		assertEquals(true, r);
	}

	@Test
	public void i02c() throws Exception {
		boolean r = proxy.httpStatusReturnBool404();
		assertEquals(false, r);
	}

	public void i02d() throws Exception {
		Boolean r = proxy.httpStatusReturnBoolean404();
		assertEquals(false, r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Proxy class
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/testThirdPartyProxy")
	public static interface ThirdPartyProxy {

		//-------------------------------------------------------------------------------------------------------------
		// Header tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="GET", path="/primitiveHeaders")
		String primitiveHeaders(
			@Header("a") String a,
			@Header("an") String an,
			@Header("b") int b,
			@Header("c") Integer c,
			@Header("cn") Integer cn,
			@Header("d") Boolean d,
			@Header("e") float e,
			@Header("f") Float f
		);

		@RemoteOp(method="GET", path="/primitiveCollectionHeaders")
		String primitiveCollectionHeaders(
			@Header("a") int[][][] a,
			@Header("b") Integer[][][] b,
			@Header("c") String[][][] c,
			@Header("d") List<Integer> d,
			@Header("e") List<List<List<Integer>>> e,
			@Header("f") List<Integer[][][]> f,
			@Header("g") List<int[][][]> g,
			@Header("h") List<String> h
		);

		@RemoteOp(method="GET", path="/beanHeaders")
		String beanHeaders(
			@Header("a") ABean a,
			@Header("an") ABean an,
			@Header("b") ABean[][][] b,
			@Header("c") List<ABean> c,
			@Header("d") List<ABean[][][]> d,
			@Header("e") Map<String,ABean> e,
			@Header("f") Map<String,List<ABean>> f,
			@Header("g") Map<String,List<ABean[][][]>> g,
			@Header("h") Map<Integer,List<ABean>> h
		);

		@RemoteOp(method="GET", path="/typedBeanHeaders")
		String typedBeanHeaders(
			@Header("a") TypedBean a,
			@Header("an") TypedBean an,
			@Header("b") TypedBean[][][] b,
			@Header("c") List<TypedBean> c,
			@Header("d") List<TypedBean[][][]> d,
			@Header("e") Map<String,TypedBean> e,
			@Header("f") Map<String,List<TypedBean>> f,
			@Header("g") Map<String,List<TypedBean[][][]>> g,
			@Header("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteOp(method="GET", path="/swappedObjectHeaders")
		String swappedObjectHeaders(
			@Header("a") SwappedObject a,
			@Header("b") SwappedObject[][][] b,
			@Header("c") Map<SwappedObject,SwappedObject> c,
			@Header("d") Map<SwappedObject,SwappedObject[][][]> d
		);

		@RemoteOp(method="GET", path="/implicitSwappedObjectHeaders")
		String implicitSwappedObjectHeaders(
			@Header("a") ImplicitSwappedObject a,
			@Header("b") ImplicitSwappedObject[][][] b,
			@Header("c") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@Header("d") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		);

		@RemoteOp(method="GET", path="/enumHeaders")
		String enumHeaders(
			@Header("a") TestEnum a,
			@Header("an") TestEnum an,
			@Header("b") TestEnum[][][] b,
			@Header("c") List<TestEnum> c,
			@Header("d") List<List<List<TestEnum>>> d,
			@Header("e") List<TestEnum[][][]> e,
			@Header("f") Map<TestEnum,TestEnum> f,
			@Header("g") Map<TestEnum,TestEnum[][][]> g,
			@Header("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteOp(method="GET", path="/mapHeader")
		String mapHeader(
			@Header("*") Map<String,Object> a
		);

		@RemoteOp(method="GET", path="/beanHeader")
		String beanHeader(
			@Header("*") NeBean a
		);

		@RemoteOp(method="GET", path="/headerList")
		String headerList(
			@Header(value="*") @Schema(allowEmptyValue=true) HeaderList a
		);

		//-------------------------------------------------------------------------------------------------------------
		// Query tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="GET", path="/primitiveQueries")
		String primitiveQueries(
			@Query("a") String a,
			@Query("an") String an,
			@Query("b") int b,
			@Query("c") Integer c,
			@Query("cn") Integer cn,
			@Query("d") Boolean d,
			@Query("e") float e,
			@Query("f") Float f
		);

		@RemoteOp(method="GET", path="/primitiveCollectionQueries")
		String primitiveCollectionQueries(
			@Query("a") int[][][] a,
			@Query("b") Integer[][][] b,
			@Query("c") String[][][] c,
			@Query("d") List<Integer> d,
			@Query("e") List<List<List<Integer>>> e,
			@Query("f") List<Integer[][][]> f,
			@Query("g") List<int[][][]> g,
			@Query("h") List<String> h
		);

		@RemoteOp(method="GET", path="/beanQueries")
		String beanQueries(
			@Query("a") ABean a,
			@Query("an") ABean an,
			@Query("b") ABean[][][] b,
			@Query("c") List<ABean> c,
			@Query("d") List<ABean[][][]> d,
			@Query("e") Map<String,ABean> e,
			@Query("f") Map<String,List<ABean>> f,
			@Query("g") Map<String,List<ABean[][][]>> g,
			@Query("h") Map<Integer,List<ABean>> h
		);

		@RemoteOp(method="GET", path="/typedBeanQueries")
		String typedBeanQueries(
			@Query("a") TypedBean a,
			@Query("an") TypedBean an,
			@Query("b") TypedBean[][][] b,
			@Query("c") List<TypedBean> c,
			@Query("d") List<TypedBean[][][]> d,
			@Query("e") Map<String,TypedBean> e,
			@Query("f") Map<String,List<TypedBean>> f,
			@Query("g") Map<String,List<TypedBean[][][]>> g,
			@Query("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteOp(method="GET", path="/swappedObjectQueries")
		String swappedObjectQueries(
			@Query("a") SwappedObject a,
			@Query("b") SwappedObject[][][] b,
			@Query("c") Map<SwappedObject,SwappedObject> c,
			@Query("d") Map<SwappedObject,SwappedObject[][][]> d
		);

		@RemoteOp(method="GET", path="/implicitSwappedObjectQueries")
		String implicitSwappedObjectQueries(
			@Query("a") ImplicitSwappedObject a,
			@Query("b") ImplicitSwappedObject[][][] b,
			@Query("c") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@Query("d") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		);

		@RemoteOp(method="GET", path="/enumQueries")
		String enumQueries(
			@Query("a") TestEnum a,
			@Query("an") TestEnum an,
			@Query("b") TestEnum[][][] b,
			@Query("c") List<TestEnum> c,
			@Query("d") List<List<List<TestEnum>>> d,
			@Query("e") List<TestEnum[][][]> e,
			@Query("f") Map<TestEnum,TestEnum> f,
			@Query("g") Map<TestEnum,TestEnum[][][]> g,
			@Query("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteOp(method="GET", path="/stringQuery1")
		String stringQuery1(
			@Query String a
		);

		@RemoteOp(method="GET", path="/stringQuery2")
		String stringQuery2(
			@Query("*") String a
		);

		@RemoteOp(method="GET", path="/mapQuery")
		String mapQuery(
			@Query("*") Map<String,Object> a
		);

		@RemoteOp(method="GET", path="/beanQuery")
		String beanQuery(
			@Query("*") NeBean a
		);

		@RemoteOp(method="GET", path="/partListQuery")
		String partListQuery(
			@Query("*") PartList a
		);

		//-------------------------------------------------------------------------------------------------------------
		// FormData tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/primitiveFormData")
		String primitiveFormData(
			@FormData("a") String a,
			@FormData("an") String an,
			@FormData("b") int b,
			@FormData("c") Integer c,
			@FormData("cn") Integer cn,
			@FormData("d") Boolean d,
			@FormData("e") float e,
			@FormData("f") Float f
		);

		@RemoteOp(method="POST", path="/primitiveCollectionFormData")
		String primitiveCollectionFormData(
			@FormData("a") int[][][] a,
			@FormData("b") Integer[][][] b,
			@FormData("c") String[][][] c,
			@FormData("d") List<Integer> d,
			@FormData("e") List<List<List<Integer>>> e,
			@FormData("f") List<Integer[][][]> f,
			@FormData("g") List<int[][][]> g,
			@FormData("h") List<String> h
		);

		@RemoteOp(method="POST", path="/beanFormData")
		String beanFormData(
			@FormData("a") ABean a,
			@FormData("an") ABean an,
			@FormData("b") ABean[][][] b,
			@FormData("c") List<ABean> c,
			@FormData("d") List<ABean[][][]> d,
			@FormData("e") Map<String,ABean> e,
			@FormData("f") Map<String,List<ABean>> f,
			@FormData("g") Map<String,List<ABean[][][]>> g,
			@FormData("h") Map<Integer,List<ABean>> h
		);

		@RemoteOp(method="POST", path="/typedBeanFormData")
		String typedBeanFormData(
			@FormData("a") TypedBean a,
			@FormData("an") TypedBean an,
			@FormData("b") TypedBean[][][] b,
			@FormData("c") List<TypedBean> c,
			@FormData("d") List<TypedBean[][][]> d,
			@FormData("e") Map<String,TypedBean> e,
			@FormData("f") Map<String,List<TypedBean>> f,
			@FormData("g") Map<String,List<TypedBean[][][]>> g,
			@FormData("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteOp(method="POST", path="/swappedObjectFormData")
		String swappedObjectFormData(
			@FormData("a") SwappedObject a,
			@FormData("b") SwappedObject[][][] b,
			@FormData("c") Map<SwappedObject,SwappedObject> c,
			@FormData("d") Map<SwappedObject,SwappedObject[][][]> d
		);

		@RemoteOp(method="POST", path="/implicitSwappedObjectFormData")
		String implicitSwappedObjectFormData(
			@FormData("a") ImplicitSwappedObject a,
			@FormData("b") ImplicitSwappedObject[][][] b,
			@FormData("c") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@FormData("d") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		);

		@RemoteOp(method="POST", path="/enumFormData")
		String enumFormData(
			@FormData("a") TestEnum a,
			@FormData("an") TestEnum an,
			@FormData("b") TestEnum[][][] b,
			@FormData("c") List<TestEnum> c,
			@FormData("d") List<List<List<TestEnum>>> d,
			@FormData("e") List<TestEnum[][][]> e,
			@FormData("f") Map<TestEnum,TestEnum> f,
			@FormData("g") Map<TestEnum,TestEnum[][][]> g,
			@FormData("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteOp(method="POST", path="/mapFormData")
		String mapFormData(
			@FormData("*") Map<String,Object> a
		);

		@RemoteOp(method="POST", path="/beanFormData2")
		String beanFormData(
			@FormData("*") NeBean a
		);

		@RemoteOp(method="POST", path="/partListFormData")
		String partListFormData(
			@FormData("*") PartList a
		);

		//-------------------------------------------------------------------------------------------------------------
		// Path tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/pathVars1/{a}/{b}")
		String pathVars1(
			@Path("a") int a,
			@Path("b") String b
		);

		@RemoteOp(method="POST", path="/pathVars2/{a}/{b}")
		String pathVars2(
			@Path Map<String,Object> a
		);

		@RemoteOp(method="POST", path="/pathVars3/{a}/{b}")
		String pathVars3(
			@Path ABean a
		);

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Path
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath1(
			@Request ReqBeanPath1 rb
		);

		public static interface ReqBeanPath1 {
			@Path
			int getA();

			@Path
			String getB();
		}

		public static class ReqBeanPath1Impl implements ReqBeanPath1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath2(
			@Request ReqBeanPath2 rb
		);

		public static class ReqBeanPath2 {
			@Path
			public int getA() {
				return 1;
			};

			@Path
			public String getB() {
				return "foo";
			}
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath3(
			@Request ReqBeanPath3 rb
		);

		public static interface ReqBeanPath3 {
			@Path("a")
			int getX();

			@Path("b")
			String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath6(
			@Request ReqBeanPath6 rb
		);

		public static interface ReqBeanPath6 {
			@Path("*")
			Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath7(
			@Request ReqBeanPath7 rb
		);

		public static interface ReqBeanPath7 {
			@Path("*")
			ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Query
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery1(
			@Request ReqBeanQuery1 rb
		);

		public static interface ReqBeanQuery1 {
			@Query
			int getA();

			@Query
			String getB();
		}

		public static class ReqBeanQuery1Impl implements ReqBeanQuery1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery2(
			@Request ReqBeanQuery2 rb
		);

		public static class ReqBeanQuery2 {
			@Query
			public int getA() {
				return 1;
			};

			@Query
			public String getB() {
				return "foo";
			}
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery3(
			@Request ReqBeanQuery3 rb
		);

		public static interface ReqBeanQuery3 {
			@Query("a")
			int getX();

			@Query("b")
			String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery6(
			@Request ReqBeanQuery6 rb
		);

		public static interface ReqBeanQuery6 {
			@Query("*")
			Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery7(
			@Request ReqBeanQuery7 rb
		);

		public static interface ReqBeanQuery7 {
			@Query("*")
			ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - FormData
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData1(
			@Request ReqBeanFormData1 rb
		);

		public static interface ReqBeanFormData1 {
			@FormData
			int getA();

			@FormData
			String getB();
		}

		public static class ReqBeanFormData1Impl implements ReqBeanFormData1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData2(
			@Request ReqBeanFormData2 rb
		);

		public static class ReqBeanFormData2 {
			@FormData
			public int getA() {
				return 1;
			};

			@FormData
			public String getB() {
				return "foo";
			}
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData3(
			@Request ReqBeanFormData3 rb
		);

		public static interface ReqBeanFormData3 {
			@FormData("a")
			int getX();

			@FormData("b")
			String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData6(
			@Request ReqBeanFormData6 rb
		);

		public static interface ReqBeanFormData6 {
			@FormData("*")
			Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData7(
			@Request ReqBeanFormData7 rb
		);

		public static interface ReqBeanFormData7 {
			@FormData("*")
			ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Header
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader1(
			@Request ReqBeanHeader1 rb
		);

		public static interface ReqBeanHeader1 {
			@Header
			int getA();

			@Header
			String getB();
		}

		public static class ReqBeanHeader1Impl implements ReqBeanHeader1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader2(
			@Request ReqBeanHeader2 rb
		);

		public static class ReqBeanHeader2 {
			@Header
			public int getA() {
				return 1;
			};

			@Header
			public String getB() {
				return "foo";
			}
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader3(
			@Request ReqBeanHeader3 rb
		);

		public static interface ReqBeanHeader3 {
			@Header("a")
			int getX();

			@Header("b")
			String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader6(
			@Request ReqBeanHeader6 rb
		);

		public static interface ReqBeanHeader6 {
			@Header("*")
			Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader7(
			@Request ReqBeanHeader7 rb
		);

		public static interface ReqBeanHeader7 {
			@Header("*")
			ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// PartFormatters
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/partFormatters/{p1}")
		String partFormatters(
			@Path(value="p1", serializer=DummyPartSerializer.class) String p1,
			@Header(value="h1", serializer=DummyPartSerializer.class) String h1,
			@Query(value="q1", serializer=DummyPartSerializer.class) String q1,
			@FormData(value="f1", serializer=DummyPartSerializer.class) String f1
		);

		//-------------------------------------------------------------------------------------------------------------
		// Test return types.
		//-------------------------------------------------------------------------------------------------------------

		// Various primitives

		@RemoteOp(method="GET", path="/returnVoid")
		void returnVoid();

		@RemoteOp(method="GET", path="/returnInt")
		int returnInt();

		@RemoteOp(method="GET", path="/returnInteger")
		Integer returnInteger();

		@RemoteOp(method="GET", path="/returnBoolean")
		boolean returnBoolean();

		@RemoteOp(method="GET", path="/returnFloat")
		float returnFloat();

		@RemoteOp(method="GET", path="/returnFloatObject")
		Float returnFloatObject();

		@RemoteOp(method="GET", path="/returnString")
		String returnString();

		@RemoteOp(method="GET", path="/returnNullString")
		String returnNullString();

		@RemoteOp(method="GET", path="/returnInt3dArray")
		int[][][] returnInt3dArray();

		@RemoteOp(method="GET", path="/returnInteger3dArray")
		Integer[][][] returnInteger3dArray();

		@RemoteOp(method="GET", path="/returnString3dArray")
		String[][][] returnString3dArray();

		@RemoteOp(method="GET", path="/returnIntegerList")
		List<Integer> returnIntegerList();

		@RemoteOp(method="GET", path="/returnInteger3dList")
		List<List<List<Integer>>> returnInteger3dList();

		@RemoteOp(method="GET", path="/returnInteger1d3dList")
		List<Integer[][][]> returnInteger1d3dList();

		@RemoteOp(method="GET", path="/returnInt1d3dList")
		List<int[][][]> returnInt1d3dList();

		@RemoteOp(method="GET", path="/returnStringList")
		List<String> returnStringList();

		// Beans

		@RemoteOp(method="GET", path="/returnBean")
		ABean returnBean();

		@RemoteOp(method="GET", path="/returnBean3dArray")
		ABean[][][] returnBean3dArray();

		@RemoteOp(method="GET", path="/returnBeanList")
		List<ABean> returnBeanList();

		@RemoteOp(method="GET", path="/returnBean1d3dList")
		List<ABean[][][]> returnBean1d3dList();

		@RemoteOp(method="GET", path="/returnBeanMap")
		Map<String,ABean> returnBeanMap();

		@RemoteOp(method="GET", path="/returnBeanListMap")
		Map<String,List<ABean>> returnBeanListMap();

		@RemoteOp(method="GET", path="/returnBean1d3dListMap")
		Map<String,List<ABean[][][]>> returnBean1d3dListMap();

		@RemoteOp(method="GET", path="/returnBeanListMapIntegerKeys")
		Map<Integer,List<ABean>> returnBeanListMapIntegerKeys();

		// Typed beans

		@RemoteOp(method="GET", path="/returnTypedBean")
		TypedBean returnTypedBean();

		@RemoteOp(method="GET", path="/returnTypedBean3dArray")
		TypedBean[][][] returnTypedBean3dArray();

		@RemoteOp(method="GET", path="/returnTypedBeanList")
		List<TypedBean> returnTypedBeanList();

		@RemoteOp(method="GET", path="/returnTypedBean1d3dList")
		List<TypedBean[][][]> returnTypedBean1d3dList();

		@RemoteOp(method="GET", path="/returnTypedBeanMap")
		Map<String,TypedBean> returnTypedBeanMap();

		@RemoteOp(method="GET", path="/returnTypedBeanListMap")
		Map<String,List<TypedBean>> returnTypedBeanListMap();

		@RemoteOp(method="GET", path="/returnTypedBean1d3dListMap")
		Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap();

		@RemoteOp(method="GET", path="/returnTypedBeanListMapIntegerKeys")
		Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys();

		// Swapped POJOs

		@RemoteOp(method="GET", path="/returnSwappedObject")
		SwappedObject returnSwappedObject();

		@RemoteOp(method="GET", path="/returnSwappedObject3dArray")
		SwappedObject[][][] returnSwappedObject3dArray();

		@RemoteOp(method="GET", path="/returnSwappedObjectMap")
		Map<SwappedObject,SwappedObject> returnSwappedObjectMap();

		@RemoteOp(method="GET", path="/returnSwappedObject3dMap")
		Map<SwappedObject,SwappedObject[][][]> returnSwappedObject3dMap();

		// Implicit swapped POJOs

		@RemoteOp(method="GET", path="/returnImplicitSwappedObject")
		ImplicitSwappedObject returnImplicitSwappedObject();

		@RemoteOp(method="GET", path="/returnImplicitSwappedObject3dArray")
		ImplicitSwappedObject[][][] returnImplicitSwappedObject3dArray();

		@RemoteOp(method="GET", path="/returnImplicitSwappedObjectMap")
		Map<ImplicitSwappedObject,ImplicitSwappedObject> returnImplicitSwappedObjectMap();

		@RemoteOp(method="GET", path="/returnImplicitSwappedObject3dMap")
		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> returnImplicitSwappedObject3dMap();

		// Enums

		@RemoteOp(method="GET", path="/returnEnum")
		TestEnum returnEnum();

		@RemoteOp(method="GET", path="/returnEnum3d")
		TestEnum[][][] returnEnum3d();

		@RemoteOp(method="GET", path="/returnEnumList")
		List<TestEnum> returnEnumList();

		@RemoteOp(method="GET", path="/returnEnum3dList")
		List<List<List<TestEnum>>> returnEnum3dList();

		@RemoteOp(method="GET", path="/returnEnum1d3dList")
		List<TestEnum[][][]> returnEnum1d3dList();

		@RemoteOp(method="GET", path="/returnEnumMap")
		Map<TestEnum,TestEnum> returnEnumMap();

		@RemoteOp(method="GET", path="/returnEnum3dArrayMap")
		Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap();

		@RemoteOp(method="GET", path="/returnEnum1d3dListMap")
		Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap();

		//-------------------------------------------------------------------------------------------------------------
		// Test parameters
		//-------------------------------------------------------------------------------------------------------------

		// Various primitives

		@RemoteOp(method="POST", path="/setInt")
		void setInt(@Body int x) throws AssertionError;

		@RemoteOp(method="POST", path="/setInteger")
		void setInteger(@Body Integer x);

		@RemoteOp(method="POST", path="/setBoolean")
		void setBoolean(@Body boolean x);

		@RemoteOp(method="POST", path="/setFloat")
		void setFloat(@Body float x);

		@RemoteOp(method="POST", path="/setFloatObject")
		void setFloatObject(@Body Float x);

		@RemoteOp(method="POST", path="/setString")
		void setString(@Body String x);

		@RemoteOp(method="POST", path="/setNullString")
		void setNullString(@Body String x) throws AssertionError;

		@RemoteOp(method="POST", path="/setInt3dArray")
		String setInt3dArray(@Body int[][][] x, @org.apache.juneau.http.annotation.Query("I") int i);

		@RemoteOp(method="POST", path="/setInteger3dArray")
		void setInteger3dArray(@Body Integer[][][] x);

		@RemoteOp(method="POST", path="/setString3dArray")
		void setString3dArray(@Body String[][][] x);

		@RemoteOp(method="POST", path="/setIntegerList")
		void setIntegerList(@Body List<Integer> x);

		@RemoteOp(method="POST", path="/setInteger3dList")
		void setInteger3dList(@Body List<List<List<Integer>>> x);

		@RemoteOp(method="POST", path="/setInteger1d3dList")
		void setInteger1d3dList(@Body List<Integer[][][]> x);

		@RemoteOp(method="POST", path="/setInt1d3dList")
		void setInt1d3dList(@Body List<int[][][]> x);

		@RemoteOp(method="POST", path="/setStringList")
		void setStringList(@Body List<String> x);

		// Beans

		@RemoteOp(method="POST", path="/setBean")
		void setBean(@Body ABean x);

		@RemoteOp(method="POST", path="/setBean3dArray")
		void setBean3dArray(@Body ABean[][][] x);

		@RemoteOp(method="POST", path="/setBeanList")
		void setBeanList(@Body List<ABean> x);

		@RemoteOp(method="POST", path="/setBean1d3dList")
		void setBean1d3dList(@Body List<ABean[][][]> x);

		@RemoteOp(method="POST", path="/setBeanMap")
		void setBeanMap(@Body Map<String,ABean> x);

		@RemoteOp(method="POST", path="/setBeanListMap")
		void setBeanListMap(@Body Map<String,List<ABean>> x);

		@RemoteOp(method="POST", path="/setBean1d3dListMap")
		void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x);

		@RemoteOp(method="POST", path="/setBeanListMapIntegerKeys")
		void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x);

		// Typed beans

		@RemoteOp(method="POST", path="/setTypedBean")
		void setTypedBean(@Body TypedBean x);

		@RemoteOp(method="POST", path="/setTypedBean3dArray")
		void setTypedBean3dArray(@Body TypedBean[][][] x);

		@RemoteOp(method="POST", path="/setTypedBeanList")
		void setTypedBeanList(@Body List<TypedBean> x);

		@RemoteOp(method="POST", path="/setTypedBean1d3dList")
		void setTypedBean1d3dList(@Body List<TypedBean[][][]> x);

		@RemoteOp(method="POST", path="/setTypedBeanMap")
		void setTypedBeanMap(@Body Map<String,TypedBean> x);

		@RemoteOp(method="POST", path="/setTypedBeanListMap")
		void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x);

		@RemoteOp(method="POST", path="/setTypedBean1d3dListMap")
		void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x);

		@RemoteOp(method="POST", path="/setTypedBeanListMapIntegerKeys")
		void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		@RemoteOp(method="POST", path="/setSwappedObject")
		void setSwappedObject(@Body SwappedObject x);

		@RemoteOp(method="POST", path="/setSwappedObject3dArray")
		void setSwappedObject3dArray(@Body SwappedObject[][][] x);

		@RemoteOp(method="POST", path="/setSwappedObjectMap")
		void setSwappedObjectMap(@Body Map<SwappedObject,SwappedObject> x);

		@RemoteOp(method="POST", path="/setSwappedObject3dMap")
		void setSwappedObject3dMap(@Body Map<SwappedObject,SwappedObject[][][]> x);

		// Implicit swapped POJOs

		@RemoteOp(method="POST", path="/setImplicitSwappedObject")
		void setImplicitSwappedObject(@Body ImplicitSwappedObject x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObject3dArray")
		void setImplicitSwappedObject3dArray(@Body ImplicitSwappedObject[][][] x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObjectMap")
		void setImplicitSwappedObjectMap(@Body Map<ImplicitSwappedObject,ImplicitSwappedObject> x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObject3dMap")
		void setImplicitSwappedObject3dMap(@Body Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x);

		// Enums

		@RemoteOp(method="POST", path="/setEnum")
		void setEnum(@Body TestEnum x);

		@RemoteOp(method="POST", path="/setEnum3d")
		void setEnum3d(@Body TestEnum[][][] x);

		@RemoteOp(method="POST", path="/setEnumList")
		void setEnumList(@Body List<TestEnum> x);

		@RemoteOp(method="POST", path="/setEnum3dList")
		void setEnum3dList(@Body List<List<List<TestEnum>>> x);

		@RemoteOp(method="POST", path="/setEnum1d3dList")
		void setEnum1d3dList(@Body List<TestEnum[][][]> x);

		@RemoteOp(method="POST", path="/setEnumMap")
		void setEnumMap(@Body Map<TestEnum,TestEnum> x);

		@RemoteOp(method="POST", path="/setEnum3dArrayMap")
		void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x);

		@RemoteOp(method="POST", path="/setEnum1d3dListMap")
		void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x);

		// Method returns status code

		@RemoteOp(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		int httpStatusReturnInt200();

		@RemoteOp(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		Integer httpStatusReturnInteger200();

		@RemoteOp(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		int httpStatusReturnInt404();

		@RemoteOp(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		Integer httpStatusReturnInteger404();

		@RemoteOp(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		boolean httpStatusReturnBool200();

		@RemoteOp(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		Boolean httpStatusReturnBoolean200();

		@RemoteOp(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		boolean httpStatusReturnBool404();

		@RemoteOp(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		Boolean httpStatusReturnBoolean404();
	}

	// Bean for testing NE annotations.
	public static class NeBean {
		public String a, b, c;

		public NeBean init() {
			this.a = "foo";
			this.b = "";
			this.c = null;
			return this;
		}
	}

	public static class DummyPartSerializer extends BaseHttpPartSerializer {
		public DummyPartSerializer(Builder builder) {
			super(builder);
		}

		public static Builder create() {
			return new Builder();
		}

		public static class Builder extends BaseHttpPartSerializer.Builder {

			Builder() {
				super();
			}

			Builder(Builder builder) {
				super(builder);
			}

			@Override
			public Builder copy() {
				return new Builder(this);
			}

			@Override
			public DummyPartSerializer build() {
				return new DummyPartSerializer(this);
			}
		}

		@Override
		public HttpPartSerializerSession getPartSession() {
			return new BaseHttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					return "dummy-"+value;
				}
			};
		}
	}
}
