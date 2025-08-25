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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.utest.utils.Constants.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class ThirdPartyProxy_Test extends SimpleTestBase {

	private static final Input[] INPUT = {
		input("Json", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), JsonParser.DEFAULT),
		input("Xml", XmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT),
		input("Mixed", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT),
		input("Html", HtmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT),
		input("MessagePack", MsgPackSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT),
		input("UrlEncoding", UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT),
		input("Uon", UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UonParser.DEFAULT)
	};

	private static Input input(String label, Serializer serializer, Parser parser) {
		return new Input(label, serializer, parser);
	}

	private static class Input {
		ThirdPartyProxy proxy;

		Input(String label, Serializer serializer, Parser parser) {
			proxy = MockRestClient.create(ThirdPartyProxyResource.class).ignoreErrors().serializer(serializer).parser(parser).partSerializer(UonSerializer.create().addBeanTypes().addRootType().build()).build().getRemote(ThirdPartyProxy.class, null, serializer, parser);
		}
	}

	static Stream<Arguments> input() {
		return Stream.of(INPUT).map(x -> args(x));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Header tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void a01_primitiveHeaders(Input input) {
		var r = input.proxy.primitiveHeaders(
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

	@ParameterizedTest
	@MethodSource("input")
	void a02_primitiveCollectionHeaders(Input input) {
		var r = input.proxy.primitiveCollectionHeaders(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			ulist(1,null),
			ulist(ulist(ulist(1,null),null),null),
			ulist(new Integer[][][]{{{1,null},null},null},null),
			ulist(new int[][][]{{{1,2},null},null},null),
			ulist("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a03_beanHeaders(Input input) {
		var r = input.proxy.beanHeaders(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			ulist(ABean.get(),null),
			ulist(new ABean[][][]{{{ABean.get(),null},null},null},null),
			map("foo",ABean.get()),
			map("foo",ulist(ABean.get())),
			map("foo",ulist(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			map(1,ulist(ABean.get()))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a04_typedBeanHeaders(Input input) {
		var r = input.proxy.typedBeanHeaders(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			ulist(TypedBeanImpl.get(),null),
			ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			map("foo",TypedBeanImpl.get()),
			map("foo",ulist((TypedBean)TypedBeanImpl.get())),
			map("foo",ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			map(1,ulist((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a05_swappedObjectHeaders(Input input) {
		var r = input.proxy.swappedObjectHeaders(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			map(new SwappedObject(),new SwappedObject()),
			map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a06_implicitSwappedObjectHeaders(Input input) {
		var r = input.proxy.implicitSwappedObjectHeaders(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a07_enumHeaders(Input input) {
		var r = input.proxy.enumHeaders(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			ulist(TestEnum.TWO,null),
			ulist(ulist(ulist(TestEnum.TWO,null),null),null),
			ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			map(TestEnum.ONE,TestEnum.TWO),
			map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			map(TestEnum.ONE,ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a08_mapHeader(Input input) {
		var r = input.proxy.mapHeader(
			map("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a09_beanHeader(Input input) {
		var r = input.proxy.beanHeader(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a10_headerList(Input input) {
		var r = input.proxy.headerList(
			headerList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void b01_primitiveQueries(Input input) {
		var r = input.proxy.primitiveQueries(
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

	@ParameterizedTest
	@MethodSource("input")
	void b02_primitiveCollectionQueries(Input input) {
		var r = input.proxy.primitiveCollectionQueries(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			ulist(1,null),
			ulist(ulist(ulist(1,null),null),null),
			ulist(new Integer[][][]{{{1,null},null},null},null),
			ulist(new int[][][]{{{1,2},null},null},null),
			ulist("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b03_beanQueries(Input input) {
		var r = input.proxy.beanQueries(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			ulist(ABean.get(),null),
			ulist(new ABean[][][]{{{ABean.get(),null},null},null},null),
			map("foo",ABean.get()),
			map("foo",ulist(ABean.get())),
			map("foo",ulist(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			map(1,ulist(ABean.get()))
		);
		assertEquals("OK", r);
	}


	@ParameterizedTest
	@MethodSource("input")
	void b04_typedBeanQueries(Input input) {
		var r = input.proxy.typedBeanQueries(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			ulist(TypedBeanImpl.get(),null),
			ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			map("foo",TypedBeanImpl.get()),
			map("foo",ulist((TypedBean)TypedBeanImpl.get())),
			map("foo",ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			map(1,ulist((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b05_swappedObjectQueries(Input input) {
		var r = input.proxy.swappedObjectQueries(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			map(new SwappedObject(),new SwappedObject()),
			map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b06_implicitSwappedObjectQueries(Input input) {
		var r = input.proxy.implicitSwappedObjectQueries(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b07_enumQueries(Input input) {
		var r = input.proxy.enumQueries(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			ulist(TestEnum.TWO,null),
			ulist(ulist(ulist(TestEnum.TWO,null),null),null),
			ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			map(TestEnum.ONE,TestEnum.TWO),
			map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			map(TestEnum.ONE,ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b08_stringQuery1(Input input) {
		var r = input.proxy.stringQuery1("a=1&b=foo");
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b09_stringQuery2(Input input) {
		var r = input.proxy.stringQuery2("a=1&b=foo");
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b10_mapQuery(Input input) {
		var r = input.proxy.mapQuery(
			map("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b11_beanQuery(Input input) {
		var r = input.proxy.beanQuery(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b12_partListQuery(Input input) {
		var r = input.proxy.partListQuery(
			partList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// FormData tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void c01_primitiveFormData(Input input) {
		var r = input.proxy.primitiveFormData(
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

	@ParameterizedTest
	@MethodSource("input")
	void c02_primitiveCollectionFormData(Input input) {
		var r = input.proxy.primitiveCollectionFormData(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			ulist(1,null),
			ulist(ulist(ulist(1,null),null),null),
			ulist(new Integer[][][]{{{1,null},null},null},null),
			ulist(new int[][][]{{{1,2},null},null},null),
			ulist("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c03_beanFormData(Input input) {
		var r = input.proxy.beanFormData(
			ABean.get(),
			null,
			new ABean[][][]{{{ABean.get(),null},null},null},
			ulist(ABean.get(),null),
			ulist(new ABean[][][]{{{ABean.get(),null},null},null},null),
			map("foo",ABean.get()),
			map("foo",ulist(ABean.get())),
			map("foo",ulist(new ABean[][][]{{{ABean.get(),null},null},null},null)),
			map(1,ulist(ABean.get()))
		);
		assertEquals("OK", r);
	}


	@ParameterizedTest
	@MethodSource("input")
	void c04_typedBeanFormData(Input input) {
		var r = input.proxy.typedBeanFormData(
			TypedBeanImpl.get(),
			null,
			new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},
			ulist(TypedBeanImpl.get(),null),
			ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null),
			map("foo",TypedBeanImpl.get()),
			map("foo",ulist((TypedBean)TypedBeanImpl.get())),
			map("foo",ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)),
			map(1,ulist((TypedBean)TypedBeanImpl.get()))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c05_swappedObjectFormData(Input input) {
		var r = input.proxy.swappedObjectFormData(
			new SwappedObject(),
			new SwappedObject[][][]{{{new SwappedObject(),null},null},null},
			map(new SwappedObject(),new SwappedObject()),
			map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c06_implicitSwappedObjectFormData(Input input) {
		var r = input.proxy.implicitSwappedObjectFormData(
			new ImplicitSwappedObject(),
			new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null},
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject()),
			map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c07_enumFormData(Input input) {
		var r = input.proxy.enumFormData(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			ulist(TestEnum.TWO,null),
			ulist(ulist(ulist(TestEnum.TWO,null),null),null),
			ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null),
			map(TestEnum.ONE,TestEnum.TWO),
			map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			map(TestEnum.ONE,ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c08_mapFormData(Input input) {
		var r = input.proxy.mapFormData(
			map("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c09_beanFormData(Input input) {
		var r = input.proxy.beanFormData(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c10_partListFormData(Input input) {
		var r = input.proxy.partListFormData(
			partList("a","foo","b","","c",null)
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives
	@ParameterizedTest
	@MethodSource("input")
	void da01_returnVoid(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.returnVoid());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da02_returnInteger(Input input) {
		assertEquals((Integer)1, input.proxy.returnInteger());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da03_returnInt(Input input) {
		assertEquals(1, input.proxy.returnInt());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da04_returnBoolean(Input input) {
		assertEquals(true, input.proxy.returnBoolean());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da05_returnFloat(Input input) {
		assertEquals(1f, input.proxy.returnFloat(), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("input")
	void da06_returnFloatObject(Input input) {
		assertEquals(1f, input.proxy.returnFloatObject(), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("input")
	void da07_returnString(Input input) {
		assertEquals("foobar", input.proxy.returnString());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da08_returnNullString(Input input) {
		assertNull(input.proxy.returnNullString());
	}

	@ParameterizedTest
	@MethodSource("input")
	void da09_returnInt3dArray(Input input) {
		assertJson(input.proxy.returnInt3dArray(), "[[[1,2],null],null]");
	}

	@ParameterizedTest
	@MethodSource("input")
	void da10_returnInteger3dArray(Input input) {
		assertJson(input.proxy.returnInteger3dArray(), "[[[1,null],null],null]");
	}

	@ParameterizedTest
	@MethodSource("input")
	void da11_returnString3dArray(Input input) {
		assertJson(input.proxy.returnString3dArray(), "[[['foo','bar',null],null],null]");
	}

	@ParameterizedTest
	@MethodSource("input")
	void da12_returnIntegerList(Input input) {
		var x = input.proxy.returnIntegerList();
		assertJson(x, "[1,null]");
		assertType(Integer.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void da13_returnInteger3dList(Input input) {
		var x = input.proxy.returnInteger3dList();
		assertJson(x, "[[[1,null],null],null]");
		assertType(Integer.class, x.get(0).get(0).get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void da14_returnInteger1d3dList(Input input) {
		var x = input.proxy.returnInteger1d3dList();
		assertJson(x, "[[[[1,null],null],null],null]");
		assertType(Integer.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void da15_returnInt1d3dList(Input input) {
		var x = input.proxy.returnInt1d3dList();
		assertJson(x, "[[[[1,2],null],null],null]");
		assertType(int[][][].class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void da16_returnStringList(Input input) {
		assertJson(input.proxy.returnStringList(), "['foo','bar',null]");
	}

	// Beans

	@ParameterizedTest
	@MethodSource("input")
	void db01_returnBean(Input input) {
		var x = input.proxy.returnBean();
		assertJson(x, "{a:1,b:'foo'}");
		assertType(ABean.class, x);
	}

	@ParameterizedTest
	@MethodSource("input")
	void db02_returnBean3dArray(Input input) {
		var x = input.proxy.returnBean3dArray();
		assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
		assertType(ABean.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void db03_returnBeanList(Input input) {
		var x = input.proxy.returnBeanList();
		assertJson(x, "[{a:1,b:'foo'}]");
		assertType(ABean.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void db04_returnBean1d3dList(Input input) {
		var x = input.proxy.returnBean1d3dList();
		assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
		assertType(ABean.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void db05_returnBeanMap(Input input) {
		var x = input.proxy.returnBeanMap();
		assertJson(x, "{foo:{a:1,b:'foo'}}");
		assertType(ABean.class, x.get("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void db06_returnBeanListMap(Input input) {
		var x = input.proxy.returnBeanListMap();
		assertJson(x, "{foo:[{a:1,b:'foo'}]}");
		assertType(ABean.class, x.get("foo").get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void db07_returnBean1d3dListMap(Input input) {
		var x = input.proxy.returnBean1d3dListMap();
		assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertType(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void db08_returnBeanListMapIntegerKeys(Input input) {
		// Note: JsonSerializer serializes key as string.
		var x = input.proxy.returnBeanListMapIntegerKeys();
		assertJson(x, "{'1':[{a:1,b:'foo'}]}");
		assertType(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@ParameterizedTest
	@MethodSource("input")
	void dc01_returnTypedBean(Input input) {
		var x = input.proxy.returnTypedBean();
		assertJson(x, "{a:1,b:'foo'}");
		assertType(TypedBeanImpl.class, x);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc02_returnTypedBean3dArray(Input input) {
		var x = input.proxy.returnTypedBean3dArray();
		assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
		assertType(TypedBeanImpl.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc03_returnTypedBeanList(Input input) {
		var x = input.proxy.returnTypedBeanList();
		assertJson(x, "[{a:1,b:'foo'}]");
		assertType(TypedBeanImpl.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc04_returnTypedBean1d3dList(Input input) {
		var x = input.proxy.returnTypedBean1d3dList();
		assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
		assertType(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc05_returnTypedBeanMap(Input input) {
		var x = input.proxy.returnTypedBeanMap();
		assertJson(x, "{foo:{a:1,b:'foo'}}");
		assertType(TypedBeanImpl.class, x.get("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc06_returnTypedBeanListMap(Input input) {
		var x = input.proxy.returnTypedBeanListMap();
		assertJson(x, "{foo:[{a:1,b:'foo'}]}");
		assertType(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc07_returnTypedBean1d3dListMap(Input input) {
		var x = input.proxy.returnTypedBean1d3dListMap();
		assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertType(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dc08_returnTypedBeanListMapIntegerKeys(Input input) {
		// Note: JsonSerializer serializes key as string.
		var x = input.proxy.returnTypedBeanListMapIntegerKeys();
		assertJson(x, "{'1':[{a:1,b:'foo'}]}");
		assertType(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void dd01_returnSwappedObject(Input input) {
		var x = input.proxy.returnSwappedObject();
		assertJson(x, "'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dd02_returnSwappedObject3dArray(Input input) {
		var x = input.proxy.returnSwappedObject3dArray();
		assertJson(x, "[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dd03_returnSwappedObjectMap(Input input) {
		var x = input.proxy.returnSwappedObjectMap();
		assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void dd04_returnSwappedObject3dMap(Input input) {
		var x = input.proxy.returnSwappedObject3dMap();
		assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void de01_returnImplicitSwappedObject(Input input) {
		var x = input.proxy.returnImplicitSwappedObject();
		assertJson(x, "'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void de02_returnImplicitSwappedObject3dArray(Input input) {
		var x = input.proxy.returnImplicitSwappedObject3dArray();
		assertJson(x, "[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void de03_returnImplicitSwappedObjectMap(Input input) {
		var x = input.proxy.returnImplicitSwappedObjectMap();
		assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void de04_returnImplicitSwappedObject3dMap(Input input) {
		var x = input.proxy.returnImplicitSwappedObject3dMap();
		assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@ParameterizedTest
	@MethodSource("input")
	void df01_returnEnum(Input input) {
		var x = input.proxy.returnEnum();
		assertJson(x, "'TWO'");
	}

	@ParameterizedTest
	@MethodSource("input")
	void df02_returnEnum3d(Input input) {
		var x = input.proxy.returnEnum3d();
		assertJson(x, "[[['TWO',null],null],null]");
		assertType(TestEnum.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void df03_returnEnumList(Input input) {
		var x = input.proxy.returnEnumList();
		assertJson(x, "['TWO',null]");
		assertType(TestEnum.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void df04_returnEnum3dList(Input input) {
		var x = input.proxy.returnEnum3dList();
		assertJson(x, "[[['TWO',null],null],null]");
		assertType(TestEnum.class, x.get(0).get(0).get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void df05_returnEnum1d3dList(Input input) {
		var x = input.proxy.returnEnum1d3dList();
		assertJson(x, "[[[['TWO',null],null],null],null]");
		assertType(TestEnum[][][].class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void df06_returnEnumMap(Input input) {
		var x = input.proxy.returnEnumMap();
		assertJson(x, "{ONE:'TWO'}");
		var e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum.class, e.getValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void df07_returnEnum3dArrayMap(Input input) {
		var x = input.proxy.returnEnum3dArrayMap();
		assertJson(x, "{ONE:[[['TWO',null],null],null]}");
		var e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum[][][].class, e.getValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void df08_returnEnum1d3dListMap(Input input) {
		var x = input.proxy.returnEnum1d3dListMap();
		assertJson(x, "{ONE:[[[['TWO',null],null],null],null]}");
		assertType(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test Body
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@ParameterizedTest
	@MethodSource("input")
	void ea01_setInt(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInt(1));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea02_setWrongInt(Input input) {
		assertThrowsWithMessage(AssertionError.class, "expected:<1> but was:<2>", ()->input.proxy.setInt(2));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea03_setInteger(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInteger(1));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea04_setBoolean(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBoolean(true));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea05_setFloat(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setFloat(1f));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea06_setFloatObject(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setFloatObject(1f));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea07_setString(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setString("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea08_setNullString(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setNullString(null));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea09_setNullStringBad(Input input) {
		assertThrowsWithMessage(AssertionError.class, "expected null, but was:<foo>", ()->input.proxy.setNullString("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea10_setInt3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInt3dArray(new int[][][]{{{1},null},null}, 1));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea11_setInteger3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea12_setString3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setString3dArray(new String[][][]{{{"foo",null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea13_setIntegerList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setIntegerList(ulist(1,null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea14_setInteger3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInteger3dList(ulist(ulist(ulist(1,null),null),null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea15_setInteger1d3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInteger1d3dList(ulist(new Integer[][][]{{{1,null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea16_setInt1d3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setInt1d3dList(ulist(new int[][][]{{{1,2},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ea17_setStringList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setStringList(ulist("foo","bar",null)));
	}

	// Beans
	@ParameterizedTest
	@MethodSource("input")
	void eb01_setBean(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBean(ABean.get()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb02_setBean3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb03_setBeanList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBeanList(ulist(ABean.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb04_setBean1d3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBean1d3dList(ulist(new ABean[][][]{{{ABean.get(),null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb05_setBeanMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBeanMap(map("foo",ABean.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb06_setBeanListMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBeanListMap(map("foo",ulist(ABean.get()))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb07_setBean1d3dListMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBean1d3dListMap(map("foo",ulist(new ABean[][][]{{{ABean.get(),null},null},null},null))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void eb08_setBeanListMapIntegerKeys(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setBeanListMapIntegerKeys(map(1,ulist(ABean.get()))));
	}

	// Typed beans

	@ParameterizedTest
	@MethodSource("input")
	void ec01_setTypedBean(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBean(TypedBeanImpl.get()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec02_setTypedBean3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec03_setTypedBeanList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBeanList(ulist((TypedBean)TypedBeanImpl.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec04_setTypedBean1d3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBean1d3dList(ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec05_setTypedBeanMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBeanMap(map("foo",TypedBeanImpl.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec06_setTypedBeanListMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBeanListMap(map("foo",ulist((TypedBean)TypedBeanImpl.get()))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec07_setTypedBean1d3dListMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBean1d3dListMap(map("foo",ulist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ec08_setTypedBeanListMapIntegerKeys(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setTypedBeanListMapIntegerKeys(map(1,ulist((TypedBean)TypedBeanImpl.get()))));
	}

	// Swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void ed01_setSwappedObject(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setSwappedObject(new SwappedObject()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ed02_setSwappedObject3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ed03_setSwappedObjectMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setSwappedObjectMap(map(new SwappedObject(),new SwappedObject())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ed04_setSwappedObject3dMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setSwappedObject3dMap(map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})));
	}

	// Implicit swapped POJOs
	@ParameterizedTest
	@MethodSource("input")
	void ee01_setImplicitSwappedObject(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setImplicitSwappedObject(new ImplicitSwappedObject()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ee02_setImplicitSwappedObject3dArray(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ee03_setImplicitSwappedObjectMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setImplicitSwappedObjectMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ee04_setImplicitSwappedObject3dMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setImplicitSwappedObject3dMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})));
	}

	// Enums

	@ParameterizedTest
	@MethodSource("input")
	void ef01_setEnum(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum(TestEnum.TWO));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef02_setEnum3d(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef03_setEnumList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnumList(ulist(TestEnum.TWO,null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef04_setEnum3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum3dList(ulist(ulist(ulist(TestEnum.TWO,null),null),null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef05_setEnum1d3dList(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum1d3dList(ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef06_setEnumMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnumMap(map(TestEnum.ONE,TestEnum.TWO)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef07_setEnum3dArrayMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum3dArrayMap(map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null})));
	}

	@ParameterizedTest
	@MethodSource("input")
	void ef08_setEnum1d3dListMap(Input input) {
		TestUtils.assertNotThrown(()->input.proxy.setEnum1d3dListMap(map(TestEnum.ONE,ulist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path variables
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void f01_pathVars1(Input input) {
		var r = input.proxy.pathVars1(1, "foo");
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void f02_pathVars2(Input input) {
		var r = input.proxy.pathVars2(
			map("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void f03_pathVars3(Input input) {
		var r = input.proxy.pathVars3(
			ABean.get()
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Path
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void ga01_reqBeanPath1(Input input) {
		var r = input.proxy.reqBeanPath1(
			new ThirdPartyProxy.ReqBeanPath1() {
						@Override public int getA() { return 1; }
						@Override public String getB() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void ga01_reqBeanPath1a(Input input) {
		var r = input.proxy.reqBeanPath1(
			new ThirdPartyProxy.ReqBeanPath1Impl()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void ga02_reqBeanPath2(Input input) {
		var r = input.proxy.reqBeanPath2(
			new ThirdPartyProxy.ReqBeanPath2()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void ga03_reqBeanPath3(Input input) {
		var r = input.proxy.reqBeanPath3(
			new ThirdPartyProxy.ReqBeanPath3() {
				@Override public int getX() { return 1; }
				@Override public String getY() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga06_reqBeanPath6(Input input) {
		String r = input.proxy.reqBeanPath6(() -> map("a", 1, "b", "foo"));
		assertEquals("OK", r);
	}

	@Test
	public void ga07_reqBeanPath7(Input input) {
		String r = input.proxy.reqBeanPath7(ABean::get);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Query
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void gb01_reqBeanQuery1(Input input) {
		var r = input.proxy.reqBeanQuery1(
			new ThirdPartyProxy.ReqBeanQuery1() {
				@Override public int getA() { return 1; }
				@Override public String getB() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gb01_reqBeanQuery1a(Input input) {
		var r = input.proxy.reqBeanQuery1(
			new ThirdPartyProxy.ReqBeanQuery1Impl()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gb02_reqBeanQuery2(Input input) {
		var r = input.proxy.reqBeanQuery2(
			new ThirdPartyProxy.ReqBeanQuery2()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gb03_reqBeanQuery3(Input input) {
		var r = input.proxy.reqBeanQuery3(
			new ThirdPartyProxy.ReqBeanQuery3() {
				@Override public int getX() { return 1; }
				@Override public String getY() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gb06_reqBeanQuery6(Input input) {
		var r = input.proxy.reqBeanQuery6(
			() -> map("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gb07_reqBeanQuery7(Input input) {
		var r = input.proxy.reqBeanQuery7(
			ABean::get
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - FormData
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void gd01_reqBeanFormData1(Input input) {
		var r = input.proxy.reqBeanFormData1(
			new ThirdPartyProxy.ReqBeanFormData1() {
				@Override public int getA() { return 1; }
				@Override public String getB() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gd01_reqBeanFormData1a(Input input) {
		var r = input.proxy.reqBeanFormData1(
			new ThirdPartyProxy.ReqBeanFormData1Impl()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gd02_reqBeanFormData2(Input input) {
		var r = input.proxy.reqBeanFormData2(
			new ThirdPartyProxy.ReqBeanFormData2()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gd03_reqBeanFormData3(Input input) {
		var r = input.proxy.reqBeanFormData3(
			new ThirdPartyProxy.ReqBeanFormData3() {
				@Override public int getX() { return 1; }
				@Override public String getY() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gd06_reqBeanFormData6(Input input) {
		var r = input.proxy.reqBeanFormData6(
			() -> map("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gd07_reqBeanFormData7(Input input) {
		var r = input.proxy.reqBeanFormData7(
			ABean::get
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests - Header
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void gf01_reqBeanHeader1(Input input) {
		var r = input.proxy.reqBeanHeader1(
			new ThirdPartyProxy.ReqBeanHeader1() {
				@Override public int getA() { return 1; }
				@Override public String getB() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gf01_reqBeanHeader1a(Input input) {
		var r = input.proxy.reqBeanHeader1(
			new ThirdPartyProxy.ReqBeanHeader1Impl()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gf02_reqBeanHeader2(Input input) {
		var r = input.proxy.reqBeanHeader2(
			new ThirdPartyProxy.ReqBeanHeader2()
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gf03_reqBeanHeader3(Input input) {
		var r = input.proxy.reqBeanHeader3(
			new ThirdPartyProxy.ReqBeanHeader3() {
				@Override public int getX() { return 1; }
				@Override public String getY() { return "foo"; }
			}
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gf06_reqBeanHeader6(Input input) {
		var r = input.proxy.reqBeanHeader6(
			() -> map("a",1,"b","foo")
		);
		assertEquals("OK", r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void gf07_reqBeanHeader7(Input input) {
		var r = input.proxy.reqBeanHeader7(
			ABean::get
		);
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// PartFormatters
	//-----------------------------------------------------------------------------------------------------------------
	@ParameterizedTest
	@MethodSource("input")
	void h01(Input input) {
		var r = input.proxy.partFormatters("1", "2", "3", "4");
		assertEquals("OK", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RemoteOp(returns=HTTP_STATUS)
	//-----------------------------------------------------------------------------------------------------------------
	@ParameterizedTest
	@MethodSource("input")
	void i01a(Input input) {
		var r = input.proxy.httpStatusReturnInt200();
		assertEquals(200, r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void i01b(Input input) {
		var r = input.proxy.httpStatusReturnInteger200();
		assertEquals(200, r.intValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void i01c(Input input) {
		var r = input.proxy.httpStatusReturnInt404();
		assertEquals(404, r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void i01d(Input input) {
		var r = input.proxy.httpStatusReturnInteger404();
		assertEquals(404, r.intValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void i02a(Input input) {
		var r = input.proxy.httpStatusReturnBool200();
		assertEquals(true, r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void i02b(Input input) {
		var r = input.proxy.httpStatusReturnBoolean200();
		assertEquals(true, r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void i02c(Input input) {
		var r = input.proxy.httpStatusReturnBool404();
		assertEquals(false, r);
	}

	@ParameterizedTest
	@MethodSource("input")
	void i02d(Input input) {
		var r = input.proxy.httpStatusReturnBoolean404();
		assertEquals(false, r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Proxy class
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	public interface ThirdPartyProxy {

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

		public interface ReqBeanPath1 {
			@Path int getA();
			@Path String getB();
		}

		public static class ReqBeanPath1Impl implements ReqBeanPath1 {
			@Override public int getA() { return 1; }
			@Override public String getB() { return "foo"; }
		}


		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath2(
			@Request ReqBeanPath2 rb
		);

		public static class ReqBeanPath2 {
			@Path public int getA() { return 1; }
			@Path public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath3(
			@Request ReqBeanPath3 rb
		);

		public interface ReqBeanPath3 {
			@Path("a") int getX();
			@Path("b") String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath6(
			@Request ReqBeanPath6 rb
		);

		public interface ReqBeanPath6 {
			@Path("*") Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath7(
			@Request ReqBeanPath7 rb
		);

		public interface ReqBeanPath7 {
			@Path("*") ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Query
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery1(
			@Request ReqBeanQuery1 rb
		);

		public interface ReqBeanQuery1 {
			@Query int getA();
			@Query String getB();
		}

		public static class ReqBeanQuery1Impl implements ReqBeanQuery1 {
			@Override public int getA() { return 1; }
			@Override public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery2(
			@Request ReqBeanQuery2 rb
		);

		public static class ReqBeanQuery2 {
			@Query public int getA() { return 1; }
			@Query public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery3(
			@Request ReqBeanQuery3 rb
		);

		public interface ReqBeanQuery3 {
			@Query("a") int getX();
			@Query("b") String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery6(
			@Request ReqBeanQuery6 rb
		);

		public interface ReqBeanQuery6 {
			@Query("*") Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanQuery")
		String reqBeanQuery7(
			@Request ReqBeanQuery7 rb
		);

		public interface ReqBeanQuery7 {
			@Query("*") ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - FormData
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData1(
			@Request ReqBeanFormData1 rb
		);

		public interface ReqBeanFormData1 {
			@FormData int getA();
			@FormData String getB();
		}

		public static class ReqBeanFormData1Impl implements ReqBeanFormData1 {
			@Override public int getA() { return 1; }
			@Override public String getB() { return "foo"; }
		}


		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData2(
			@Request ReqBeanFormData2 rb
		);

		public static class ReqBeanFormData2 {
			@FormData public int getA() { return 1; }
			@FormData public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData3(
			@Request ReqBeanFormData3 rb
		);

		public interface ReqBeanFormData3 {
			@FormData("a") int getX();
			@FormData("b") String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData6(
			@Request ReqBeanFormData6 rb
		);

		public interface ReqBeanFormData6 {
			@FormData("*") Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanFormData")
		String reqBeanFormData7(
			@Request ReqBeanFormData7 rb
		);

		public interface ReqBeanFormData7 {
			@FormData("*") ABean getX();
		}

		//-------------------------------------------------------------------------------------------------------------
		// @Request tests - Header
		//-------------------------------------------------------------------------------------------------------------

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader1(
			@Request ReqBeanHeader1 rb
		);

		public interface ReqBeanHeader1 {
			@Header int getA();
			@Header String getB();
		}

		public static class ReqBeanHeader1Impl implements ReqBeanHeader1 {
			@Override public int getA() { return 1; }
			@Override public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader2(
			@Request ReqBeanHeader2 rb
		);

		public static class ReqBeanHeader2 {
			@Header public int getA() { return 1; }
			@Header public String getB() { return "foo"; }
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader3(
			@Request ReqBeanHeader3 rb
		);

		public interface ReqBeanHeader3 {
			@Header("a") int getX();
			@Header("b") String getY();
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader6(
			@Request ReqBeanHeader6 rb
		);

		public interface ReqBeanHeader6 {
			@Header("*") Map<String,Object> getX();
		}

		@RemoteOp(method="POST", path="/reqBeanHeader")
		String reqBeanHeader7(
			@Request ReqBeanHeader7 rb
		);

		public interface ReqBeanHeader7 {
			@Header("*") ABean getX();
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
		void setInt(@Content int x) throws AssertionError;

		@RemoteOp(method="POST", path="/setInteger")
		void setInteger(@Content Integer x);

		@RemoteOp(method="POST", path="/setBoolean")
		void setBoolean(@Content boolean x);

		@RemoteOp(method="POST", path="/setFloat")
		void setFloat(@Content float x);

		@RemoteOp(method="POST", path="/setFloatObject")
		void setFloatObject(@Content Float x);

		@RemoteOp(method="POST", path="/setString")
		void setString(@Content String x);

		@RemoteOp(method="POST", path="/setNullString")
		void setNullString(@Content String x) throws AssertionError;

		@RemoteOp(method="POST", path="/setInt3dArray")
		String setInt3dArray(@Content int[][][] x, @org.apache.juneau.http.annotation.Query("I") int i);

		@RemoteOp(method="POST", path="/setInteger3dArray")
		void setInteger3dArray(@Content Integer[][][] x);

		@RemoteOp(method="POST", path="/setString3dArray")
		void setString3dArray(@Content String[][][] x);

		@RemoteOp(method="POST", path="/setIntegerList")
		void setIntegerList(@Content List<Integer> x);

		@RemoteOp(method="POST", path="/setInteger3dList")
		void setInteger3dList(@Content List<List<List<Integer>>> x);

		@RemoteOp(method="POST", path="/setInteger1d3dList")
		void setInteger1d3dList(@Content List<Integer[][][]> x);

		@RemoteOp(method="POST", path="/setInt1d3dList")
		void setInt1d3dList(@Content List<int[][][]> x);

		@RemoteOp(method="POST", path="/setStringList")
		void setStringList(@Content List<String> x);

		// Beans

		@RemoteOp(method="POST", path="/setBean")
		void setBean(@Content ABean x);

		@RemoteOp(method="POST", path="/setBean3dArray")
		void setBean3dArray(@Content ABean[][][] x);

		@RemoteOp(method="POST", path="/setBeanList")
		void setBeanList(@Content List<ABean> x);

		@RemoteOp(method="POST", path="/setBean1d3dList")
		void setBean1d3dList(@Content List<ABean[][][]> x);

		@RemoteOp(method="POST", path="/setBeanMap")
		void setBeanMap(@Content Map<String,ABean> x);

		@RemoteOp(method="POST", path="/setBeanListMap")
		void setBeanListMap(@Content Map<String,List<ABean>> x);

		@RemoteOp(method="POST", path="/setBean1d3dListMap")
		void setBean1d3dListMap(@Content Map<String,List<ABean[][][]>> x);

		@RemoteOp(method="POST", path="/setBeanListMapIntegerKeys")
		void setBeanListMapIntegerKeys(@Content Map<Integer,List<ABean>> x);

		// Typed beans

		@RemoteOp(method="POST", path="/setTypedBean")
		void setTypedBean(@Content TypedBean x);

		@RemoteOp(method="POST", path="/setTypedBean3dArray")
		void setTypedBean3dArray(@Content TypedBean[][][] x);

		@RemoteOp(method="POST", path="/setTypedBeanList")
		void setTypedBeanList(@Content List<TypedBean> x);

		@RemoteOp(method="POST", path="/setTypedBean1d3dList")
		void setTypedBean1d3dList(@Content List<TypedBean[][][]> x);

		@RemoteOp(method="POST", path="/setTypedBeanMap")
		void setTypedBeanMap(@Content Map<String,TypedBean> x);

		@RemoteOp(method="POST", path="/setTypedBeanListMap")
		void setTypedBeanListMap(@Content Map<String,List<TypedBean>> x);

		@RemoteOp(method="POST", path="/setTypedBean1d3dListMap")
		void setTypedBean1d3dListMap(@Content Map<String,List<TypedBean[][][]>> x);

		@RemoteOp(method="POST", path="/setTypedBeanListMapIntegerKeys")
		void setTypedBeanListMapIntegerKeys(@Content Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		@RemoteOp(method="POST", path="/setSwappedObject")
		void setSwappedObject(@Content SwappedObject x);

		@RemoteOp(method="POST", path="/setSwappedObject3dArray")
		void setSwappedObject3dArray(@Content SwappedObject[][][] x);

		@RemoteOp(method="POST", path="/setSwappedObjectMap")
		void setSwappedObjectMap(@Content Map<SwappedObject,SwappedObject> x);

		@RemoteOp(method="POST", path="/setSwappedObject3dMap")
		void setSwappedObject3dMap(@Content Map<SwappedObject,SwappedObject[][][]> x);

		// Implicit swapped POJOs

		@RemoteOp(method="POST", path="/setImplicitSwappedObject")
		void setImplicitSwappedObject(@Content ImplicitSwappedObject x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObject3dArray")
		void setImplicitSwappedObject3dArray(@Content ImplicitSwappedObject[][][] x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObjectMap")
		void setImplicitSwappedObjectMap(@Content Map<ImplicitSwappedObject,ImplicitSwappedObject> x);

		@RemoteOp(method="POST", path="/setImplicitSwappedObject3dMap")
		void setImplicitSwappedObject3dMap(@Content Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x);

		// Enums

		@RemoteOp(method="POST", path="/setEnum")
		void setEnum(@Content TestEnum x);

		@RemoteOp(method="POST", path="/setEnum3d")
		void setEnum3d(@Content TestEnum[][][] x);

		@RemoteOp(method="POST", path="/setEnumList")
		void setEnumList(@Content List<TestEnum> x);

		@RemoteOp(method="POST", path="/setEnum3dList")
		void setEnum3dList(@Content List<List<List<TestEnum>>> x);

		@RemoteOp(method="POST", path="/setEnum1d3dList")
		void setEnum1d3dList(@Content List<TestEnum[][][]> x);

		@RemoteOp(method="POST", path="/setEnumMap")
		void setEnumMap(@Content Map<TestEnum,TestEnum> x);

		@RemoteOp(method="POST", path="/setEnum3dArrayMap")
		void setEnum3dArrayMap(@Content Map<TestEnum,TestEnum[][][]> x);

		@RemoteOp(method="POST", path="/setEnum1d3dListMap")
		void setEnum1d3dListMap(@Content Map<TestEnum,List<TestEnum[][][]>> x);

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
				return build(DummyPartSerializer.class);
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
