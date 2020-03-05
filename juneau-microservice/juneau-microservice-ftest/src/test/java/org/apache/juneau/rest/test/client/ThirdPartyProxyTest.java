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

import static org.apache.juneau.rest.testutils.Constants.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.html.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Request;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client2.ext.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.test.*;
import org.apache.juneau.rest.test.client.ThirdPartyProxyTest.ThirdPartyProxy.*;
import org.apache.juneau.rest.testutils.*;
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
			{ /* 0 */ "Json", JsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfXmlAbbrevSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), RdfXmlParser.DEFAULT },
		});
	}

	private ThirdPartyProxy proxy;

	public ThirdPartyProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = getCached(label, ThirdPartyProxy.class);
		if (proxy == null) {
			this.proxy = getClient(label, serializer, parser).builder().partSerializer(UonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build()).build().getRemote(ThirdPartyProxy.class, null, serializer, parser);
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
				System.err.println("response="+s);
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
			AList.<Integer>create(1,null),
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

	@Test
	public void a08_mapHeader() throws Exception {
		String r = proxy.mapHeader(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
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
	public void a10_nameValuePairsHeader() throws Exception {
		String r = proxy.nameValuePairsHeader(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
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
			new AMap<String,Object>().append("a", 1).append("b", "foo")
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
	public void b12_nameValuePairsQuery() throws Exception {
		String r = proxy.nameValuePairsQuery(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
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

	@Test
	public void c08_mapFormData() throws Exception {
		String r = proxy.mapFormData(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
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
	public void c10_nameValuePairsFormData() throws Exception {
		String r = proxy.nameValuePairsFormData(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
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
		proxy.setEnum1d3dListMap(new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, AList.<TestEnum[][][]>create(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null)));
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
			new AMap<String,Object>().append("a", 1).append("b", "foo")
		);
		assertEquals("OK", r);
	}

	@Test
	public void f03_pathVars3() {
		String r = proxy.pathVars3(
			new ABean().init()
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
					return new AMap<String,Object>().append("a",1).append("b","foo");
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
					return new ABean().init();
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
					return new AMap<String,Object>().append("a",1).append("b","foo");
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
					return new ABean().init();
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
					return new AMap<String,Object>().append("a",1).append("b","foo");
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
					return new ABean().init();
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
					return new AMap<String,Object>().append("a",1).append("b","foo");
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
					return new ABean().init();
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
	// @RemoteMethod(returns=HTTP_STATUS)
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

		@RemoteMethod(method="GET", path="/primitiveHeaders")
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

		@RemoteMethod(method="GET", path="/primitiveCollectionHeaders")
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

		@RemoteMethod(method="GET", path="/beanHeaders")
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

		@RemoteMethod(method="GET", path="/typedBeanHeaders")
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

		@RemoteMethod(method="GET", path="/swappedPojoHeaders")
		String swappedPojoHeaders(
			@Header("a") SwappedPojo a,
			@Header("b") SwappedPojo[][][] b,
			@Header("c") Map<SwappedPojo,SwappedPojo> c,
			@Header("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(method="GET", path="/implicitSwappedPojoHeaders")
		String implicitSwappedPojoHeaders(
			@Header("a") ImplicitSwappedPojo a,
			@Header("b") ImplicitSwappedPojo[][][] b,
			@Header("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Header("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(method="GET", path="/enumHeaders")
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

		@RemoteMethod(method="GET", path="/mapHeader")
		String mapHeader(
			@Header("*") Map<String,Object> a
		);

		@RemoteMethod(method="GET", path="/beanHeader")
		String beanHeader(
			@Header("*") NeBean a
		);

		@RemoteMethod(method="GET", path="/nameValuePairsHeader")
		String nameValuePairsHeader(
			@Header(value="*", allowEmptyValue=true) NameValuePairs a
		);

		//-------------------------------------------------------------------------------------------------------------
		// Query tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteMethod(method="GET", path="/primitiveQueries")
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

		@RemoteMethod(method="GET", path="/primitiveCollectionQueries")
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

		@RemoteMethod(method="GET", path="/beanQueries")
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

		@RemoteMethod(method="GET", path="/typedBeanQueries")
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

		@RemoteMethod(method="GET", path="/swappedPojoQueries")
		String swappedPojoQueries(
			@Query("a") SwappedPojo a,
			@Query("b") SwappedPojo[][][] b,
			@Query("c") Map<SwappedPojo,SwappedPojo> c,
			@Query("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(method="GET", path="/implicitSwappedPojoQueries")
		String implicitSwappedPojoQueries(
			@Query("a") ImplicitSwappedPojo a,
			@Query("b") ImplicitSwappedPojo[][][] b,
			@Query("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Query("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(method="GET", path="/enumQueries")
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

		@RemoteMethod(method="GET", path="/stringQuery1")
		String stringQuery1(
			@Query String a
		);

		@RemoteMethod(method="GET", path="/stringQuery2")
		String stringQuery2(
			@Query("*") String a
		);

		@RemoteMethod(method="GET", path="/mapQuery")
		String mapQuery(
			@Query("*") Map<String,Object> a
		);

		@RemoteMethod(method="GET", path="/beanQuery")
		String beanQuery(
			@Query("*") NeBean a
		);

		@RemoteMethod(method="GET", path="/nameValuePairsQuery")
		String nameValuePairsQuery(
			@Query("*") NameValuePairs a
		);

		//-------------------------------------------------------------------------------------------------------------
		// FormData tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteMethod(method="POST", path="/primitiveFormData")
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

		@RemoteMethod(method="POST", path="/primitiveCollectionFormData")
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

		@RemoteMethod(method="POST", path="/beanFormData")
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

		@RemoteMethod(method="POST", path="/typedBeanFormData")
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

		@RemoteMethod(method="POST", path="/swappedPojoFormData")
		String swappedPojoFormData(
			@FormData("a") SwappedPojo a,
			@FormData("b") SwappedPojo[][][] b,
			@FormData("c") Map<SwappedPojo,SwappedPojo> c,
			@FormData("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(method="POST", path="/implicitSwappedPojoFormData")
		String implicitSwappedPojoFormData(
			@FormData("a") ImplicitSwappedPojo a,
			@FormData("b") ImplicitSwappedPojo[][][] b,
			@FormData("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@FormData("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(method="POST", path="/enumFormData")
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

		@RemoteMethod(method="POST", path="/mapFormData")
		String mapFormData(
			@FormData("*") Map<String,Object> a
		);

		@RemoteMethod(method="POST", path="/beanFormData2")
		String beanFormData(
			@FormData("*") NeBean a
		);

		@RemoteMethod(method="POST", path="/nameValuePairsFormData")
		String nameValuePairsFormData(
			@FormData("*") NameValuePairs a
		);

		//-------------------------------------------------------------------------------------------------------------
		// Path tests
		//-------------------------------------------------------------------------------------------------------------

		@RemoteMethod(method="POST", path="/pathVars1/{a}/{b}")
		String pathVars1(
			@Path("a") int a,
			@Path("b") String b
		);

		@RemoteMethod(method="POST", path="/pathVars2/{a}/{b}")
		String pathVars2(
			@Path Map<String,Object> a
		);

		@RemoteMethod(method="POST", path="/pathVars3/{a}/{b}")
		String pathVars3(
			@Path ABean a
		);

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Path
		//-------------------------------------------------------------------------------------------------------------

		@RemoteMethod(method="POST", path="/reqBeanPath/{a}/{b}")
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


		@RemoteMethod(method="POST", path="/reqBeanPath/{a}/{b}")
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

		@RemoteMethod(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath3(
			@Request ReqBeanPath3 rb
		);

		public static interface ReqBeanPath3 {
			@Path("a")
			int getX();

			@Path("b")
			String getY();
		}

		@RemoteMethod(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath6(
			@Request ReqBeanPath6 rb
		);

		public static interface ReqBeanPath6 {
			@Path("*")
			Map<String,Object> getX();
		}

		@RemoteMethod(method="POST", path="/reqBeanPath/{a}/{b}")
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

		@RemoteMethod(method="POST", path="/reqBeanQuery")
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


		@RemoteMethod(method="POST", path="/reqBeanQuery")
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

		@RemoteMethod(method="POST", path="/reqBeanQuery")
		String reqBeanQuery3(
			@Request ReqBeanQuery3 rb
		);

		public static interface ReqBeanQuery3 {
			@Query("a")
			int getX();

			@Query("b")
			String getY();
		}

		@RemoteMethod(method="POST", path="/reqBeanQuery")
		String reqBeanQuery6(
			@Request ReqBeanQuery6 rb
		);

		public static interface ReqBeanQuery6 {
			@Query("*")
			Map<String,Object> getX();
		}

		@RemoteMethod(method="POST", path="/reqBeanQuery")
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

		@RemoteMethod(method="POST", path="/reqBeanFormData")
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


		@RemoteMethod(method="POST", path="/reqBeanFormData")
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

		@RemoteMethod(method="POST", path="/reqBeanFormData")
		String reqBeanFormData3(
			@Request ReqBeanFormData3 rb
		);

		public static interface ReqBeanFormData3 {
			@FormData("a")
			int getX();

			@FormData("b")
			String getY();
		}

		@RemoteMethod(method="POST", path="/reqBeanFormData")
		String reqBeanFormData6(
			@Request ReqBeanFormData6 rb
		);

		public static interface ReqBeanFormData6 {
			@FormData("*")
			Map<String,Object> getX();
		}

		@RemoteMethod(method="POST", path="/reqBeanFormData")
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

		@RemoteMethod(method="POST", path="/reqBeanHeader")
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


		@RemoteMethod(method="POST", path="/reqBeanHeader")
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

		@RemoteMethod(method="POST", path="/reqBeanHeader")
		String reqBeanHeader3(
			@Request ReqBeanHeader3 rb
		);

		public static interface ReqBeanHeader3 {
			@Header("a")
			int getX();

			@Header("b")
			String getY();
		}

		@RemoteMethod(method="POST", path="/reqBeanHeader")
		String reqBeanHeader6(
			@Request ReqBeanHeader6 rb
		);

		public static interface ReqBeanHeader6 {
			@Header("*")
			Map<String,Object> getX();
		}

		@RemoteMethod(method="POST", path="/reqBeanHeader")
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

		@RemoteMethod(method="POST", path="/partFormatters/{p1}")
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

		@RemoteMethod(method="GET", path="/returnVoid")
		void returnVoid();

		@RemoteMethod(method="GET", path="/returnInt")
		int returnInt();

		@RemoteMethod(method="GET", path="/returnInteger")
		Integer returnInteger();

		@RemoteMethod(method="GET", path="/returnBoolean")
		boolean returnBoolean();

		@RemoteMethod(method="GET", path="/returnFloat")
		float returnFloat();

		@RemoteMethod(method="GET", path="/returnFloatObject")
		Float returnFloatObject();

		@RemoteMethod(method="GET", path="/returnString")
		String returnString();

		@RemoteMethod(method="GET", path="/returnNullString")
		String returnNullString();

		@RemoteMethod(method="GET", path="/returnInt3dArray")
		int[][][] returnInt3dArray();

		@RemoteMethod(method="GET", path="/returnInteger3dArray")
		Integer[][][] returnInteger3dArray();

		@RemoteMethod(method="GET", path="/returnString3dArray")
		String[][][] returnString3dArray();

		@RemoteMethod(method="GET", path="/returnIntegerList")
		List<Integer> returnIntegerList();

		@RemoteMethod(method="GET", path="/returnInteger3dList")
		List<List<List<Integer>>> returnInteger3dList();

		@RemoteMethod(method="GET", path="/returnInteger1d3dList")
		List<Integer[][][]> returnInteger1d3dList();

		@RemoteMethod(method="GET", path="/returnInt1d3dList")
		List<int[][][]> returnInt1d3dList();

		@RemoteMethod(method="GET", path="/returnStringList")
		List<String> returnStringList();

		// Beans

		@RemoteMethod(method="GET", path="/returnBean")
		ABean returnBean();

		@RemoteMethod(method="GET", path="/returnBean3dArray")
		ABean[][][] returnBean3dArray();

		@RemoteMethod(method="GET", path="/returnBeanList")
		List<ABean> returnBeanList();

		@RemoteMethod(method="GET", path="/returnBean1d3dList")
		List<ABean[][][]> returnBean1d3dList();

		@RemoteMethod(method="GET", path="/returnBeanMap")
		Map<String,ABean> returnBeanMap();

		@RemoteMethod(method="GET", path="/returnBeanListMap")
		Map<String,List<ABean>> returnBeanListMap();

		@RemoteMethod(method="GET", path="/returnBean1d3dListMap")
		Map<String,List<ABean[][][]>> returnBean1d3dListMap();

		@RemoteMethod(method="GET", path="/returnBeanListMapIntegerKeys")
		Map<Integer,List<ABean>> returnBeanListMapIntegerKeys();

		// Typed beans

		@RemoteMethod(method="GET", path="/returnTypedBean")
		TypedBean returnTypedBean();

		@RemoteMethod(method="GET", path="/returnTypedBean3dArray")
		TypedBean[][][] returnTypedBean3dArray();

		@RemoteMethod(method="GET", path="/returnTypedBeanList")
		List<TypedBean> returnTypedBeanList();

		@RemoteMethod(method="GET", path="/returnTypedBean1d3dList")
		List<TypedBean[][][]> returnTypedBean1d3dList();

		@RemoteMethod(method="GET", path="/returnTypedBeanMap")
		Map<String,TypedBean> returnTypedBeanMap();

		@RemoteMethod(method="GET", path="/returnTypedBeanListMap")
		Map<String,List<TypedBean>> returnTypedBeanListMap();

		@RemoteMethod(method="GET", path="/returnTypedBean1d3dListMap")
		Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap();

		@RemoteMethod(method="GET", path="/returnTypedBeanListMapIntegerKeys")
		Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys();

		// Swapped POJOs

		@RemoteMethod(method="GET", path="/returnSwappedPojo")
		SwappedPojo returnSwappedPojo();

		@RemoteMethod(method="GET", path="/returnSwappedPojo3dArray")
		SwappedPojo[][][] returnSwappedPojo3dArray();

		@RemoteMethod(method="GET", path="/returnSwappedPojoMap")
		Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap();

		@RemoteMethod(method="GET", path="/returnSwappedPojo3dMap")
		Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap();

		// Implicit swapped POJOs

		@RemoteMethod(method="GET", path="/returnImplicitSwappedPojo")
		ImplicitSwappedPojo returnImplicitSwappedPojo();

		@RemoteMethod(method="GET", path="/returnImplicitSwappedPojo3dArray")
		ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray();

		@RemoteMethod(method="GET", path="/returnImplicitSwappedPojoMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap();

		@RemoteMethod(method="GET", path="/returnImplicitSwappedPojo3dMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap();

		// Enums

		@RemoteMethod(method="GET", path="/returnEnum")
		TestEnum returnEnum();

		@RemoteMethod(method="GET", path="/returnEnum3d")
		TestEnum[][][] returnEnum3d();

		@RemoteMethod(method="GET", path="/returnEnumList")
		List<TestEnum> returnEnumList();

		@RemoteMethod(method="GET", path="/returnEnum3dList")
		List<List<List<TestEnum>>> returnEnum3dList();

		@RemoteMethod(method="GET", path="/returnEnum1d3dList")
		List<TestEnum[][][]> returnEnum1d3dList();

		@RemoteMethod(method="GET", path="/returnEnumMap")
		Map<TestEnum,TestEnum> returnEnumMap();

		@RemoteMethod(method="GET", path="/returnEnum3dArrayMap")
		Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap();

		@RemoteMethod(method="GET", path="/returnEnum1d3dListMap")
		Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap();

		//-------------------------------------------------------------------------------------------------------------
		// Test parameters
		//-------------------------------------------------------------------------------------------------------------

		// Various primitives

		@RemoteMethod(method="POST", path="/setInt")
		void setInt(@Body int x);

		@RemoteMethod(method="POST", path="/setInteger")
		void setInteger(@Body Integer x);

		@RemoteMethod(method="POST", path="/setBoolean")
		void setBoolean(@Body boolean x);

		@RemoteMethod(method="POST", path="/setFloat")
		void setFloat(@Body float x);

		@RemoteMethod(method="POST", path="/setFloatObject")
		void setFloatObject(@Body Float x);

		@RemoteMethod(method="POST", path="/setString")
		void setString(@Body String x);

		@RemoteMethod(method="POST", path="/setNullString")
		void setNullString(@Body String x);

		@RemoteMethod(method="POST", path="/setInt3dArray")
		String setInt3dArray(@Body int[][][] x, @org.apache.juneau.http.annotation.Query("I") int i);

		@RemoteMethod(method="POST", path="/setInteger3dArray")
		void setInteger3dArray(@Body Integer[][][] x);

		@RemoteMethod(method="POST", path="/setString3dArray")
		void setString3dArray(@Body String[][][] x);

		@RemoteMethod(method="POST", path="/setIntegerList")
		void setIntegerList(@Body List<Integer> x);

		@RemoteMethod(method="POST", path="/setInteger3dList")
		void setInteger3dList(@Body List<List<List<Integer>>> x);

		@RemoteMethod(method="POST", path="/setInteger1d3dList")
		void setInteger1d3dList(@Body List<Integer[][][]> x);

		@RemoteMethod(method="POST", path="/setInt1d3dList")
		void setInt1d3dList(@Body List<int[][][]> x);

		@RemoteMethod(method="POST", path="/setStringList")
		void setStringList(@Body List<String> x);

		// Beans

		@RemoteMethod(method="POST", path="/setBean")
		void setBean(@Body ABean x);

		@RemoteMethod(method="POST", path="/setBean3dArray")
		void setBean3dArray(@Body ABean[][][] x);

		@RemoteMethod(method="POST", path="/setBeanList")
		void setBeanList(@Body List<ABean> x);

		@RemoteMethod(method="POST", path="/setBean1d3dList")
		void setBean1d3dList(@Body List<ABean[][][]> x);

		@RemoteMethod(method="POST", path="/setBeanMap")
		void setBeanMap(@Body Map<String,ABean> x);

		@RemoteMethod(method="POST", path="/setBeanListMap")
		void setBeanListMap(@Body Map<String,List<ABean>> x);

		@RemoteMethod(method="POST", path="/setBean1d3dListMap")
		void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x);

		@RemoteMethod(method="POST", path="/setBeanListMapIntegerKeys")
		void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x);

		// Typed beans

		@RemoteMethod(method="POST", path="/setTypedBean")
		void setTypedBean(@Body TypedBean x);

		@RemoteMethod(method="POST", path="/setTypedBean3dArray")
		void setTypedBean3dArray(@Body TypedBean[][][] x);

		@RemoteMethod(method="POST", path="/setTypedBeanList")
		void setTypedBeanList(@Body List<TypedBean> x);

		@RemoteMethod(method="POST", path="/setTypedBean1d3dList")
		void setTypedBean1d3dList(@Body List<TypedBean[][][]> x);

		@RemoteMethod(method="POST", path="/setTypedBeanMap")
		void setTypedBeanMap(@Body Map<String,TypedBean> x);

		@RemoteMethod(method="POST", path="/setTypedBeanListMap")
		void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x);

		@RemoteMethod(method="POST", path="/setTypedBean1d3dListMap")
		void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x);

		@RemoteMethod(method="POST", path="/setTypedBeanListMapIntegerKeys")
		void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		@RemoteMethod(method="POST", path="/setSwappedPojo")
		void setSwappedPojo(@Body SwappedPojo x);

		@RemoteMethod(method="POST", path="/setSwappedPojo3dArray")
		void setSwappedPojo3dArray(@Body SwappedPojo[][][] x);

		@RemoteMethod(method="POST", path="/setSwappedPojoMap")
		void setSwappedPojoMap(@Body Map<SwappedPojo,SwappedPojo> x);

		@RemoteMethod(method="POST", path="/setSwappedPojo3dMap")
		void setSwappedPojo3dMap(@Body Map<SwappedPojo,SwappedPojo[][][]> x);

		// Implicit swapped POJOs

		@RemoteMethod(method="POST", path="/setImplicitSwappedPojo")
		void setImplicitSwappedPojo(@Body ImplicitSwappedPojo x);

		@RemoteMethod(method="POST", path="/setImplicitSwappedPojo3dArray")
		void setImplicitSwappedPojo3dArray(@Body ImplicitSwappedPojo[][][] x);

		@RemoteMethod(method="POST", path="/setImplicitSwappedPojoMap")
		void setImplicitSwappedPojoMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x);

		@RemoteMethod(method="POST", path="/setImplicitSwappedPojo3dMap")
		void setImplicitSwappedPojo3dMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x);

		// Enums

		@RemoteMethod(method="POST", path="/setEnum")
		void setEnum(@Body TestEnum x);

		@RemoteMethod(method="POST", path="/setEnum3d")
		void setEnum3d(@Body TestEnum[][][] x);

		@RemoteMethod(method="POST", path="/setEnumList")
		void setEnumList(@Body List<TestEnum> x);

		@RemoteMethod(method="POST", path="/setEnum3dList")
		void setEnum3dList(@Body List<List<List<TestEnum>>> x);

		@RemoteMethod(method="POST", path="/setEnum1d3dList")
		void setEnum1d3dList(@Body List<TestEnum[][][]> x);

		@RemoteMethod(method="POST", path="/setEnumMap")
		void setEnumMap(@Body Map<TestEnum,TestEnum> x);

		@RemoteMethod(method="POST", path="/setEnum3dArrayMap")
		void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x);

		@RemoteMethod(method="POST", path="/setEnum1d3dListMap")
		void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x);

		// Method returns status code

		@RemoteMethod(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		int httpStatusReturnInt200();

		@RemoteMethod(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		Integer httpStatusReturnInteger200();

		@RemoteMethod(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		int httpStatusReturnInt404();

		@RemoteMethod(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		Integer httpStatusReturnInteger404();

		@RemoteMethod(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		boolean httpStatusReturnBool200();

		@RemoteMethod(method="GET", path="/httpStatusReturn200", returns=RemoteReturn.STATUS)
		Boolean httpStatusReturnBoolean200();

		@RemoteMethod(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
		boolean httpStatusReturnBool404();

		@RemoteMethod(method="GET", path="/httpStatusReturn404", returns=RemoteReturn.STATUS)
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
		@Override
		public HttpPartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new BaseHttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					return "dummy-"+value;
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
