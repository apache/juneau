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
package org.apache.juneau.xml;

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class XmlParserTest {

	@Test
	public void testGenericAttributes() throws Exception {
		String xml = "<A b='1'><c>2</c></A>";
		ObjectMap m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{b:'1',c:'2'}", m.toString());
	}

	@Test
	public void testGenericWithChildElements() throws Exception {
		String xml;
		ObjectMap m;

		xml = "<A><B><C>c</C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C:'c'}}", m.toString());

		xml = "<A><B><C1>c1</C1><C2>c2</C2></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C1:'c1',C2:'c2'}}", m.toString());

		xml = "<A><B><C><D1>d1</D1><D2>d2</D2></C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C:{D1:'d1',D2:'d2'}}}", m.toString());

		xml = "<A><B><C><D1 d1a='d1av'><E1>e1</E1></D1><D2 d2a='d2av'><E2>e2</E2></D2></C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C:{D1:{d1a:'d1av',E1:'e1'},D2:{d2a:'d2av',E2:'e2'}}}}", m.toString());

		xml = "<A><B b='b'><C>c</C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{b:'b',C:'c'}}", m.toString());

		xml = "<A><B b='b'>c</B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{b:'b',contents:'c'}}", m.toString());

		xml = "<A><B>b1</B><B>b2</B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:['b1','b2']}", m.toString());

		xml = "<A><B><C>c1</C><C>c2</C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C:['c1','c2']}}", m.toString());

		xml = "<A><B v='v1'>b1</B><B v='v2'>b2</B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:[{v:'v1',contents:'b1'},{v:'v2',contents:'b2'}]}", m.toString());

		xml = "<A><B><C v='v1'>c1</C><C v='v2'>c2</C></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{C:[{v:'v1',contents:'c1'},{v:'v2',contents:'c2'}]}}", m.toString());

		xml = "<A><B c='c1'><c>c2</c></B></A>";
		m = XmlParser.DEFAULT.parse(xml, ObjectMap.class);
		assertEquals("{B:{c:['c1','c2']}}", m.toString());
	}

	@Test
	public void testPreserveRootElement() throws Exception {
		String xml;
		ObjectMap m;
		ReaderParser p = XmlParser.create().preserveRootElement(true).build();

		xml = "<A><B><C>c</C></B></A>";
		m = p.parse(xml, ObjectMap.class);
		assertEquals("{A:{B:{C:'c'}}}", m.toString());

		xml = "<A></A>";
		m = p.parse(xml, ObjectMap.class);
		assertEquals("{A:{}}", m.toString());
	}
}