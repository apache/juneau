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
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings("serial")
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
	public void returnStringArray() {
		assertObjectEquals("['foo','bar',null]", getProxy().returnStringArray());
	}

	@Test
	public void returnIntegerList() {
		assertObjectEquals("[1,2]", getProxy().returnIntegerList());
		assertTrue(getProxy().returnIntegerList().get(0) instanceof Integer);
	}

	@Test
	public void returnStringList() {
		assertObjectEquals("['foo','bar',null]", getProxy().returnStringList());
		assertTrue(getProxy().returnStringList() instanceof List);
	}

	@Test
	public void returnBean() {
		assertObjectEquals("{a:1,b:'foo'}", getProxy().returnBean());
		assertClass(InterfaceProxy.Bean.class, getProxy().returnBean());
	}

	@Test
	public void returnBeanArray() {
		assertObjectEquals("[{a:1,b:'foo'}]", getProxy().returnBeanArray());
		assertClass(InterfaceProxy.Bean.class, getProxy().returnBeanArray()[0]);
	}

	@Test
	public void returnBeanList() {
		assertObjectEquals("[{a:1,b:'foo'}]", getProxy().returnBeanList());
		assertClass(InterfaceProxy.Bean.class, getProxy().returnBeanList().get(0));
	}

	@Test
	public void returnBeanMap() {
		assertObjectEquals("{foo:{a:1,b:'foo'}}", getProxy().returnBeanMap());
		assertClass(InterfaceProxy.Bean.class, getProxy().returnBeanMap().get("foo"));
	}

	@Test
	public void returnBeanListMap() {
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", getProxy().returnBeanListMap());
		assertClass(InterfaceProxy.Bean.class, getProxy().returnBeanListMap().get("foo").get(0));
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
		} catch (Exception e) {
			// Good.
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
		} catch (Exception e) {
			// Good.
		}
	}

	@Test
	public void setIntArray() {
		getProxy().setIntArray(new int[]{1,2});
	}

	@Test
	public void setStringArray() {
		getProxy().setStringArray(new String[]{"foo","bar",null});
	}

	@Test
	public void setIntegerList() {
		getProxy().setIntegerList(Arrays.asList(new Integer[]{1,2,null}));
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
		getProxy().setBeanMap(new HashMap<String,Bean>(){{put("foo",new Bean().init());}});
	}

	@Test
	public void setBeanListMap() {
		getProxy().setBeanListMap(new HashMap<String,List<Bean>>(){{put("foo",Arrays.asList(new Bean().init()));}});
	}
}
