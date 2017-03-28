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
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.test.InterfaceProxy.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class InterfaceProxyTest extends RestTestcase {

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
			//{ /* 7 */ "RdfXml", RdfSerializer.DEFAULT_XMLABBREV, RdfParser.DEFAULT_XML },
		});
	}

	private Serializer serializer;
	private Parser parser;

	public InterfaceProxyTest(String label, Serializer serializer, Parser parser) {
		this.serializer = serializer;
		this.parser = parser;
	}

	private InterfaceProxy getProxy() {
		return getClient(serializer, parser).getRemoteableProxy(InterfaceProxy.class, "/testInterfaceProxyResource/proxy");
	}

	@Test
	public void returnVoid() {
		getProxy().returnVoid();
	}

	@Test
	public void returnInteger() {
		assertEquals((Integer)1, getProxy().returnInteger());
	}

	@Test
	public void returnInt() {
		assertEquals(1, getProxy().returnInt());
	}

	@Test
	public void returnBoolean() {
		assertEquals(true, getProxy().returnBoolean());
	}

	@Test
	public void returnFloat() {
		assertTrue(1f == getProxy().returnFloat());
	}

	@Test
	public void returnFloatObject() {
		assertTrue(1f == getProxy().returnFloatObject());
	}

	@Test
	public void returnString() {
		assertEquals("foobar", getProxy().returnString());
	}

	@Test
	public void returnNullString() {
		assertNull(getProxy().returnNullString());
	}

	@Test
	public void returnIntArray() {
		assertObjectEquals("[1,2]", getProxy().returnIntArray());
	}

	@Test
	public void returnInt2dArray() {
		assertObjectEquals("[[1,2]]", getProxy().returnInt2dArray());
	}

	@Test
	public void returnInt3dArray() {
		assertObjectEquals("[[[1,2]]]", getProxy().returnInt3dArray());
	}

	@Test
	public void returnIntegerArray() {
		assertObjectEquals("[1,null]", getProxy().returnIntegerArray());
	}

	@Test
	public void returnInteger2dArray() {
		assertObjectEquals("[[1,null]]", getProxy().returnInteger2dArray());
	}

	@Test
	public void returnInteger3dArray() {
		assertObjectEquals("[[[1,null]]]", getProxy().returnInteger3dArray());
	}

	@Test
	public void returnStringArray() {
		assertObjectEquals("['foo','bar',null]", getProxy().returnStringArray());
	}

	@Test
	public void returnString2dArray() {
		assertObjectEquals("[['foo','bar',null]]", getProxy().returnString2dArray());
	}

	@Test
	public void returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null]]]", getProxy().returnString3dArray());
	}

	@Test
	public void returnIntegerList() {
		assertObjectEquals("[1,null]", getProxy().returnIntegerList());
		assertEquals(Integer.class, getProxy().returnIntegerList().get(0).getClass());
	}

	@Test
	public void returnInteger2dList() {
		assertObjectEquals("[[1,null]]", getProxy().returnInteger2dList());
		assertEquals(Integer.class, getProxy().returnInteger2dList().get(0).get(0).getClass());
	}

	@Test
	public void returnInteger3dList() {
		assertObjectEquals("[[[1,null]]]", getProxy().returnInteger3dList());
		assertEquals(Integer.class, getProxy().returnInteger3dList().get(0).get(0).get(0).getClass());
	}

	@Test
	public void returnInteger1d1dList() {
		assertObjectEquals("[[1,null],null]", getProxy().returnInteger1d1dList());
		assertEquals(Integer.class, getProxy().returnInteger1d1dList().get(0)[0].getClass());
	}

	@Test
	public void returnInteger1d2dList() {
		assertObjectEquals("[[[1,null],null],null]", getProxy().returnInteger1d2dList());
		assertEquals(Integer.class, getProxy().returnInteger1d2dList().get(0)[0][0].getClass());
	}

	@Test
	public void returnInteger1d3dList() {
		assertObjectEquals("[[[[1,null],null],null],null]", getProxy().returnInteger1d3dList());
		assertEquals(Integer.class, getProxy().returnInteger1d3dList().get(0)[0][0][0].getClass());
	}

	@Test
	public void returnInt1d1dList() {
		assertObjectEquals("[[1,2],null]", getProxy().returnInt1d1dList());
		assertEquals(int[].class, getProxy().returnInt1d1dList().get(0).getClass());
	}

	@Test
	public void returnInt1d2dList() {
		assertObjectEquals("[[[1,2],null],null]", getProxy().returnInt1d2dList());
		assertEquals(int[][].class, getProxy().returnInt1d2dList().get(0).getClass());
	}

	@Test
	public void returnInt1d3dList() {
		assertObjectEquals("[[[[1,2],null],null],null]", getProxy().returnInt1d3dList());
		assertEquals(int[][][].class, getProxy().returnInt1d3dList().get(0).getClass());
	}

	@Test
	public void returnStringList() {
		assertObjectEquals("['foo','bar',null]", getProxy().returnStringList());
		assertTrue(getProxy().returnStringList() instanceof List);
	}

	@Test
	public void returnBean() {
		assertObjectEquals("{a:1,b:'foo'}", getProxy().returnBean());
		assertEquals(InterfaceProxy.Bean.class, getProxy().returnBean().getClass());
	}

	@Test
	public void returnBeanArray() {
		assertObjectEquals("[{a:1,b:'foo'}]", getProxy().returnBeanArray());
		assertEquals(InterfaceProxy.Bean.class, getProxy().returnBeanArray()[0].getClass());
	}

	@Test
	public void returnBeanList() {
		assertObjectEquals("[{a:1,b:'foo'}]", getProxy().returnBeanList());
		assertEquals(InterfaceProxy.Bean.class, getProxy().returnBeanList().get(0).getClass());
	}

	@Test
	public void returnBeanMap() {
		assertObjectEquals("{foo:{a:1,b:'foo'}}", getProxy().returnBeanMap());
		assertEquals(InterfaceProxy.Bean.class, getProxy().returnBeanMap().get("foo").getClass());
	}

	@Test
	public void returnBeanListMap() {
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", getProxy().returnBeanListMap());
		assertEquals(InterfaceProxy.Bean.class, getProxy().returnBeanListMap().get("foo").get(0).getClass());
	}

	@Test
	public void returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", getProxy().returnBeanListMapIntegerKeys());
		assertEquals(Integer.class, getProxy().returnBeanListMapIntegerKeys().keySet().iterator().next().getClass());
	}

	@Test
	public void throwException1() {
		try {
			getProxy().throwException1();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException1 e) {
			assertEquals("foo", e.getMessage());
		}
	}

	@Test
	public void throwException2() {
		try {
			getProxy().throwException2();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException2 e) {
		}
	}

	@Test
	public void setNothing() {
		getProxy().setNothing();
	}

	@Test
	public void setInt() {
		getProxy().setInt(1);
	}

	@Test
	public void setWrongInt() {
		try {
			getProxy().setInt(2);
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected:<1> but was:<2>", e.getMessage());
		}
	}

	@Test
	public void setInteger() {
		getProxy().setInteger(1);
	}

	@Test
	public void setBoolean() {
		getProxy().setBoolean(true);
	}

	@Test
	public void setFloat() {
		getProxy().setFloat(1f);
	}

	@Test
	public void setFloatObject() {
		getProxy().setFloatObject(1f);
	}

	@Test
	public void setString() {
		getProxy().setString("foo");
	}

	@Test
	public void setNullString() {
		getProxy().setNullString(null);
	}

	@Test
	public void setNullStringBad() {
		try {
			getProxy().setNullString("foo");
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected null, but was:<foo>", e.getLocalizedMessage());
		}
	}

	@Test
	public void setIntArray() {
		getProxy().setIntArray(new int[]{1,2});
	}

	@Test
	public void setInt2dArray() {
		getProxy().setInt2dArray(new int[][]{{1,2}});
	}

	@Test
	public void setInt3dArray() {
		getProxy().setInt3dArray(new int[][][]{{{1,2}}});
	}

	@Test
	public void setIntegerArray() {
		getProxy().setIntegerArray(new Integer[]{1,null});
	}

	@Test
	public void setInteger2dArray() {
		getProxy().setInteger2dArray(new Integer[][]{{1,null}});
	}

	@Test
	public void setInteger3dArray() {
		getProxy().setInteger3dArray(new Integer[][][]{{{1,null}}});
	}

	@Test
	public void setStringArray() {
		getProxy().setStringArray(new String[]{"foo","bar",null});
	}

	@Test
	public void setString2dArray() {
		getProxy().setString2dArray(new String[][]{{"foo","bar",null}});
	}

	@Test
	public void setString3dArray() {
		getProxy().setString3dArray(new String[][][]{{{"foo","bar",null}}});
	}

	@Test
	public void setIntegerList() {
		getProxy().setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void setInteger2dList() {
		getProxy().setInteger2dList(
			new AList<List<Integer>>()
			.append(new AList<Integer>().append(1).append(null))
		);
	}

	@Test
	public void setInteger3dList() {
		getProxy().setInteger3dList(
			new AList<List<List<Integer>>>()
			.append(
				new AList<List<Integer>>()
				.append(new AList<Integer>().append(1).append(null))
			)
		);
	}

	@Test
	public void setInteger1d1dList() {
		getProxy().setInteger1d1dList(
			new AList<Integer[]>().append(new Integer[]{1,null}).append(null)
		);
	}

	@Test
	public void setInteger1d2dList() {
		getProxy().setInteger1d2dList(
			new AList<Integer[][]>().append(new Integer[][]{{1,null},null}).append(null)
		);
	}

	@Test
	public void setInteger1d3dList() {
		getProxy().setInteger1d3dList(
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null)
		);
	}

	@Test
	public void setInt1d1dList() {
		getProxy().setInt1d1dList(
			new AList<int[]>().append(new int[]{1,2}).append(null)
		);
	}

	@Test
	public void setInt1d2dList() {
		getProxy().setInt1d2dList(
			new AList<int[][]>().append(new int[][]{{1,2},null}).append(null)
		);
	}

	@Test
	public void setInt1d3dList() {
		getProxy().setInt1d3dList(
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null)
		);
	}

	@Test
	public void setStringList() {
		getProxy().setStringList(Arrays.asList("foo","bar",null));
	}

	@Test
	public void setBean() {
		getProxy().setBean(new Bean().init());
	}

	@Test
	public void setBeanArray() {
		getProxy().setBeanArray(new Bean[]{new Bean().init()});
	}

	@Test
	public void setBeanList() {
		getProxy().setBeanList(Arrays.asList(new Bean().init()));
	}

	@Test
	public void setBeanMap() {
		getProxy().setBeanMap(new AMap<String,Bean>().append("foo",new Bean().init()));
	}

	@Test
	public void setBeanListMap() {
		getProxy().setBeanListMap(new AMap<String,List<Bean>>().append("foo",Arrays.asList(new Bean().init())));
	}

	@Test
	public void setBeanListMapIntegerKeys() {
		getProxy().setBeanListMapIntegerKeys(new AMap<Integer,List<Bean>>().append(1,Arrays.asList(new Bean().init())));
	}
}
