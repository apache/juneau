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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({"serial"})
@FixMethodOrder(NAME_ASCENDING)
public class BasicXmlTest {

	private static final XmlSerializer
		s1 = XmlSerializer.DEFAULT_SQ,
		s2 = XmlSerializer.DEFAULT_SQ_READABLE,
		s3 = XmlSerializer.DEFAULT_NS_SQ;
	private static final XmlParser parser = XmlParser.DEFAULT;

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			{	/* 0 */
				"SimpleTypes-1",
				"foo",
				"<string>foo</string>",
				"<string>foo</string>\n",
				"<string>foo</string>",
			},
			{	/* 1 */
				"SimpleTypes-2",
				true,
				"<boolean>true</boolean>",
				"<boolean>true</boolean>\n",
				"<boolean>true</boolean>",
			},
			{	/* 2 */
				"SimpleTypes-3",
				123,
				"<number>123</number>",
				"<number>123</number>\n",
				"<number>123</number>",
			},
			{	/* 3 */
				"SimpleTypes-4",
				1.23f,
				"<number>1.23</number>",
				"<number>1.23</number>\n",
				"<number>1.23</number>",
			},
			{	/* 4 */
				"SimpleTypes-5",
				null,
				"<null/>",
				"<null/>\n",
				"<null/>",
			},
			{	/* 5 */
				"Arrays-1",
				new String[]{"foo"},
				"<array><string>foo</string></array>",
				"<array>\n\t<string>foo</string>\n</array>\n",
				"<array><string>foo</string></array>",
			},
			{	/* 6 */
				"Arrays-2",
				new String[]{null},
				"<array><null/></array>",
				"<array>\n\t<null/>\n</array>\n",
				"<array><null/></array>",
			},
			{	/* 7 */
				"Arrays-3",
				new Object[]{"foo"},
				"<array><string>foo</string></array>",
				"<array>\n\t<string>foo</string>\n</array>\n",
				"<array><string>foo</string></array>",
			},
			{	/* 8 */
				"Arrays-4",
				new int[]{123},
				"<array><number>123</number></array>",
				"<array>\n\t<number>123</number>\n</array>\n",
				"<array><number>123</number></array>",
			},
			{	/* 9 */
				"Arrays-5",
				new boolean[]{true},
				"<array><boolean>true</boolean></array>",
				"<array>\n\t<boolean>true</boolean>\n</array>\n",
				"<array><boolean>true</boolean></array>",
			},
			{	/* 10 */
				"Arrays-6",
				new String[][]{{"foo"}},
				"<array><array><string>foo</string></array></array>",
				"<array>\n\t<array>\n\t\t<string>foo</string>\n\t</array>\n</array>\n",
				"<array><array><string>foo</string></array></array>",
			},
			{	/* 11 */
				"MapWithStrings",
				new MapWithStrings().append("k1", "v1").append("k2", null),
				"<object><k1>v1</k1><k2 _type='null'/></object>",
				"<object>\n\t<k1>v1</k1>\n\t<k2 _type='null'/>\n</object>\n",
				"<object><k1>v1</k1><k2 _type='null'/></object>",
			},
			{	/* 12 */
				"MapsWithNumbers",
				new MapWithNumbers().append("k1", 123).append("k2", 1.23).append("k3", null),
				"<object><k1>123</k1><k2>1.23</k2><k3 _type='null'/></object>",
				"<object>\n\t<k1>123</k1>\n\t<k2>1.23</k2>\n\t<k3 _type='null'/>\n</object>\n",
				"<object><k1>123</k1><k2>1.23</k2><k3 _type='null'/></object>",
			},
			{	/* 13 */
				"MapWithObjects",
				new MapWithObjects().append("k1", "v1").append("k2", 123).append("k3", 1.23).append("k4", true).append("k5", null),
				"<object><k1>v1</k1><k2 _type='number'>123</k2><k3 _type='number'>1.23</k3><k4 _type='boolean'>true</k4><k5 _type='null'/></object>",
				"<object>\n\t<k1>v1</k1>\n\t<k2 _type='number'>123</k2>\n\t<k3 _type='number'>1.23</k3>\n\t<k4 _type='boolean'>true</k4>\n\t<k5 _type='null'/>\n</object>\n",
				"<object><k1>v1</k1><k2 _type='number'>123</k2><k3 _type='number'>1.23</k3><k4 _type='boolean'>true</k4><k5 _type='null'/></object>",
			},
			{	/* 14 */
				"ListWithStrings",
				new ListWithStrings().append("foo").append(null),
				"<array><string>foo</string><null/></array>",
				"<array>\n\t<string>foo</string>\n\t<null/>\n</array>\n",
				"<array><string>foo</string><null/></array>",
			},
			{	/* 15 */
				"ListWithNumbers",
				new ListWithNumbers().append(123).append(1.23).append(null),
				"<array><number>123</number><number>1.23</number><null/></array>",
				"<array>\n\t<number>123</number>\n\t<number>1.23</number>\n\t<null/>\n</array>\n",
				"<array><number>123</number><number>1.23</number><null/></array>",
			},
			{	/* 16 */
				"ListWithObjects",
				new ListWithObjects().append("foo").append(123).append(1.23).append(true).append(null),
				"<array><string>foo</string><number>123</number><number>1.23</number><boolean>true</boolean><null/></array>",
				"<array>\n\t<string>foo</string>\n\t<number>123</number>\n\t<number>1.23</number>\n\t<boolean>true</boolean>\n\t<null/>\n</array>\n",
				"<array><string>foo</string><number>123</number><number>1.23</number><boolean>true</boolean><null/></array>",
			},
			{	/* 17 */
				"BeanWithNormalProperties",
				new BeanWithNormalProperties().init(),
				"<object>"
					+"<a>foo</a>"
					+"<b>123</b>"
					+"<c>bar</c>"
					+"<d _type='number'>456</d>"
					+"<e>"
						+"<h>qux</h>"
					+"</e>"
					+"<f>"
						+"<string>baz</string>"
					+"</f>"
					+"<g>"
						+"<number>789</number>"
					+"</g>"
				+"</object>",
				"<object>"
					+"\n\t<a>foo</a>"
					+"\n\t<b>123</b>"
					+"\n\t<c>bar</c>"
					+"\n\t<d _type='number'>456</d>"
					+"\n\t<e>"
						+"\n\t\t<h>qux</h>"
					+"\n\t</e>"
					+"\n\t<f>"
						+"\n\t\t<string>baz</string>"
					+"\n\t</f>"
					+"\n\t<g>"
						+"\n\t\t<number>789</number>"
					+"\n\t</g>"
				+"\n</object>\n",
				"<object>"
					+"<a>foo</a>"
					+"<b>123</b>"
					+"<c>bar</c>"
					+"<d _type='number'>456</d>"
					+"<e>"
						+"<h>qux</h>"
					+"</e>"
					+"<f>"
						+"<string>baz</string>"
					+"</f>"
					+"<g>"
						+"<number>789</number>"
					+"</g>"
				+"</object>",
			},
			{	/* 18 */
				"BeanWithMapProperties",
				new BeanWithMapProperties().init(),
				"<object>"
					+"<a>"
						+"<k1>foo</k1>"
					+"</a>"
					+"<b>"
						+"<k2>123</k2>"
					+"</b>"
					+"<c>"
						+"<k3>bar</k3>"
						+"<k4 _type='number'>456</k4>"
						+"<k5 _type='boolean'>true</k5>"
						+"<k6 _type='null'/>"
					+"</c>"
				+"</object>",
				"<object>"
					+"\n\t<a>"
						+"\n\t\t<k1>foo</k1>"
					+"\n\t</a>"
					+"\n\t<b>"
						+"\n\t\t<k2>123</k2>"
					+"\n\t</b>"
					+"\n\t<c>"
						+"\n\t\t<k3>bar</k3>"
						+"\n\t\t<k4 _type='number'>456</k4>"
						+"\n\t\t<k5 _type='boolean'>true</k5>"
						+"\n\t\t<k6 _type='null'/>"
					+"\n\t</c>"
				+"\n</object>\n",
				"<object>"
					+"<a>"
						+"<k1>foo</k1>"
					+"</a>"
					+"<b>"
						+"<k2>123</k2>"
					+"</b>"
					+"<c>"
						+"<k3>bar</k3>"
						+"<k4 _type='number'>456</k4>"
						+"<k5 _type='boolean'>true</k5>"
						+"<k6 _type='null'/>"
					+"</c>"
				+"</object>",
			},
			{	/* 19 */
				"BeanWithTypeName",
				new BeanWithTypeName().init(),
				"<X><a>123</a><b>foo</b></X>",
				"<X>\n\t<a>123</a>\n\t<b>foo</b>\n</X>\n",
				"<X><a>123</a><b>foo</b></X>",
			},
			{	/* 20 */
				"BeanWithPropertiesWithTypeNames",
				new BeanWithPropertiesWithTypeNames().init(),
				"<object><B _name='b1'><b>foo</b></B><B _name='b2'><b>foo</b></B></object>",
				"<object>\n	<B _name='b1'>\n		<b>foo</b>\n	</B>\n	<B _name='b2'>\n		<b>foo</b>\n	</B>\n</object>\n",
				"<object><B _name='b1'><b>foo</b></B><B _name='b2'><b>foo</b></B></object>"
			},
			{	/* 21 */
				"BeanWithPropertiesWithArrayTypeNames",
				new BeanWithPropertiesWithArrayTypeNames().init(),
				"<object>"
					+"<b1>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b1>"
					+"<b2>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b2>"
					+"<b3>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b3>"
				+"</object>",
				"<object>\n"
					+"\t<b1>\n"
						+"\t\t<B>\n"
							+"\t\t\t<b>foo</b>\n"
						+"\t\t</B>\n"
					+"\t</b1>\n"
					+"\t<b2>\n"
						+"\t\t<B>\n"
							+"\t\t\t<b>foo</b>\n"
						+"\t\t</B>\n"
					+"\t</b2>\n"
					+"\t<b3>\n"
						+"\t\t<B>\n"
							+"\t\t\t<b>foo</b>\n"
						+"\t\t</B>\n"
					+"\t</b3>\n"
				+"</object>\n",
				"<object>"
					+"<b1>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b1>"
					+"<b2>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b2>"
					+"<b3>"
						+"<B>"
							+"<b>foo</b>"
						+"</B>"
					+"</b3>"
				+"</object>",
			},
			{	/* 22 */
				"BeanWithPropertiesWithArray2dTypeNames",
				new BeanWithPropertiesWith2dArrayTypeNames().init(),
				"<object>"
					+"<b1>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b1>"
					+"<b2>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b2>"
					+"<b3>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b3>"
				+"</object>",
				"<object>\n"
					+"\t<b1>\n"
						+"\t\t<array>\n"
							+"\t\t\t<B>\n"
								+"\t\t\t\t<b>foo</b>\n"
							+"\t\t\t</B>\n"
						+"\t\t</array>\n"
					+"\t</b1>\n"
					+"\t<b2>\n"
						+"\t\t<array>\n"
							+"\t\t\t<B>\n"
								+"\t\t\t\t<b>foo</b>\n"
							+"\t\t\t</B>\n"
						+"\t\t</array>\n"
					+"\t</b2>\n"
					+"\t<b3>\n"
						+"\t\t<array>\n"
							+"\t\t\t<B>\n"
								+"\t\t\t\t<b>foo</b>\n"
							+"\t\t\t</B>\n"
						+"\t\t</array>\n"
					+"\t</b3>\n"
				+"</object>\n",
				"<object>"
					+"<b1>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b1>"
					+"<b2>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b2>"
					+"<b3>"
						+"<array>"
							+"<B>"
								+"<b>foo</b>"
							+"</B>"
						+"</array>"
					+"</b3>"
				+"</object>",
			},
			{	/* 23 */
				"BeanWithPropertiesWithMapTypeNames",
				new BeanWithPropertiesWithMapTypeNames().init(),
				"<object>"
					+"<b1>"
						+"<B _name='k1'>"
							+"<b>foo</b>"
						+"</B>"
					+"</b1>"
					+"<b2>"
						+"<B _name='k2'>"
							+"<b>foo</b>"
						+"</B>"
					+"</b2>"
				+"</object>",
				"<object>\n"
					+"\t<b1>\n"
						+"\t\t<B _name='k1'>\n"
							+"\t\t\t<b>foo</b>\n"
						+"\t\t</B>\n"
					+"\t</b1>\n"
					+"\t<b2>\n"
						+"\t\t<B _name='k2'>\n"
							+"\t\t\t<b>foo</b>\n"
						+"\t\t</B>\n"
					+"\t</b2>\n"
				+"</object>\n",
				"<object>"
					+"<b1>"
						+"<B _name='k1'>"
							+"<b>foo</b>"
						+"</B>"
					+"</b1>"
					+"<b2>"
						+"<B _name='k2'>"
							+"<b>foo</b>"
						+"</B>"
					+"</b2>"
				+"</object>",
			},
			{	/* 24 */
				"BeanWithChildTypeNames",
				new BeanWithChildTypeNames().init(),
				"<object>"
					+"<X _name='a'>"
						+"<fx>fx1</fx>"
					+"</X>"
					+"<X _name='b'>"
						+"<fx>fx1</fx>"
					+"</X>"
					+"<c>"
						+"<X>"
							+"<fx>fx1</fx>"
						+"</X>"
					+"</c>"
					+"<d>"
						+"<X>"
							+"<fx>fx1</fx>"
						+"</X>"
					+"</d>"
				+"</object>",
				"<object>"
					+"\n\t<X _name='a'>"
						+"\n\t\t<fx>fx1</fx>"
					+"\n\t</X>"
					+"\n\t<X _name='b'>"
						+"\n\t\t<fx>fx1</fx>"
					+"\n\t</X>"
					+"\n\t<c>"
						+"\n\t\t<X>"
							+"\n\t\t\t<fx>fx1</fx>"
						+"\n\t\t</X>"
					+"\n\t</c>"
					+"\n\t<d>"
						+"\n\t\t<X>"
							+"\n\t\t\t<fx>fx1</fx>"
						+"\n\t\t</X>"
					+"\n\t</d>"
				+"\n</object>\n",
				"<object>"
					+"<X _name='a'>"
						+"<fx>fx1</fx>"
					+"</X>"
					+"<X _name='b'>"
						+"<fx>fx1</fx>"
					+"</X>"
					+"<c>"
						+"<X>"
							+"<fx>fx1</fx>"
						+"</X>"
					+"</c>"
					+"<d>"
						+"<X>"
							+"<fx>fx1</fx>"
						+"</X>"
					+"</d>"
				+"</object>",
			},
			{	/* 25 */
				"BeanWithChildName",
				new BeanWithChildName().init(),
				"<object><a><X>foo</X><X>bar</X></a><b><Y>123</Y><Y>456</Y></b></object>",
				"<object>\n\t<a>\n\t\t<X>foo</X>\n\t\t<X>bar</X>\n\t</a>\n\t<b>\n\t\t<Y>123</Y>\n\t\t<Y>456</Y>\n\t</b>\n</object>\n",
				"<object><a><X>foo</X><X>bar</X></a><b><Y>123</Y><Y>456</Y></b></object>",
			},
			{	/* 26 */
				"BeanWithXmlFormatAttrProperty",
				new BeanWithXmlFormatAttrProperty().init(),
				"<object a='foo' b='123'/>",
				"<object a='foo' b='123'/>\n",
				"<object a='foo' b='123'/>",
			},
			{	/* 27 */
				"BeanWithXmlFormatAttrs",
				new BeanWithXmlFormatAttrs().init(),
				"<object a='foo' b='123'/>",
				"<object a='foo' b='123'/>\n",
				"<object a='foo' b='123'/>",
			},
			{	/* 28 */
				"BeanWithXmlFormatElementProperty",
				new BeanWithXmlFormatElementProperty().init(),
				"<object a='foo'><b>123</b></object>",
				"<object a='foo'>\n\t<b>123</b>\n</object>\n",
				"<object a='foo'><b>123</b></object>",
			},
			{	/* 29 */
				"BeanWithXmlFormatAttrsProperty",
				new BeanWithXmlFormatAttrsProperty().init(),
				"<object k1='foo' k2='123' b='456'/>",
				"<object k1='foo' k2='123' b='456'/>\n",
				"<object k1='foo' k2='123' b='456'/>",
			},
			{	/* 30 */
				"BeanWithXmlFormatCollapsedProperty",
				new BeanWithXmlFormatCollapsedProperty().init(),
				"<object><A>foo</A><A>bar</A><B>123</B><B>456</B></object>",
				"<object>\n\t<A>foo</A>\n\t<A>bar</A>\n\t<B>123</B>\n\t<B>456</B>\n</object>\n",
				"<object><A>foo</A><A>bar</A><B>123</B><B>456</B></object>",
			},
			{	/* 31 */
				"BeanWithXmlFormatTextProperty",
				new BeanWithXmlFormatTextProperty().init(),
				"<object a='foo'>bar</object>",
				"<object a='foo'>bar</object>\n",
				"<object a='foo'>bar</object>",
			},
			{	/* 32 */
				"BeanWithXmlFormatXmlTextProperty",
				new BeanWithXmlFormatXmlTextProperty().init(),
				"<object a='foo'>bar<b>baz</b>qux</object>",
				"<object a='foo'>bar<b>baz</b>qux</object>\n",
				"<object a='foo'>bar<b>baz</b>qux</object>",
			},
			{	/* 33 */
				"BeanWithXmlFormatElementsPropertyCollection",
				new BeanWithXmlFormatElementsPropertyCollection().init(),
				"<object a='foo'><string>bar</string><string>baz</string><number>123</number><boolean>true</boolean><null/></object>",
				"<object a='foo'>\n\t<string>bar</string>\n\t<string>baz</string>\n\t<number>123</number>\n\t<boolean>true</boolean>\n\t<null/>\n</object>\n",
				"<object a='foo'><string>bar</string><string>baz</string><number>123</number><boolean>true</boolean><null/></object>",
			},
			{	/* 34 */
				"BeanWithMixedContent",
				new BeanWithMixedContent().init(),
				"<object>foo<X fx='fx1'/>bar<Y fy='fy1'/>baz</object>",
				"<object>foo<X fx='fx1'/>bar<Y fy='fy1'/>baz</object>\n",  // Mixed content doesn't use whitespace!
				"<object>foo<X fx='fx1'/>bar<Y fy='fy1'/>baz</object>",
			},
			{	/* 35 */
				"BeanWithSpecialCharacters",
				new BeanWithSpecialCharacters().init(),
				"<object><a>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</a></object>",
				"<object>\n\t<a>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</a>\n</object>\n",
				"<object><a>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</a></object>"
			},
			{	/* 36 */
				"BeanWithSpecialCharacters2",
				new BeanWithSpecialCharacters2().init(),
				"<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_><_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_></_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>",
				"<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>\n\t<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>\n</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>\n",
				"<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_><_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_></_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>"
			},
			{	/* 37 */
				"BeanWithNullProperties",
				new BeanWithNullProperties(),
				"<object/>",
				"<object/>\n",
				"<object/>"
			},
			{	/* 38 */
				"BeanWithAbstractFields",
				new BeanWithAbstractFields().init(),
				"<object>"
					+"<A _name='a'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='ia'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='aa'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='o'>"
						+"<a>foo</a>"
					+"</A>"
				+"</object>",
				"<object>\n"
					+"\t<A _name='a'>\n"
						+"\t\t<a>foo</a>\n"
					+"\t</A>\n"
					+"\t<A _name='ia'>\n"
						+"\t\t<a>foo</a>\n"
					+"\t</A>\n"
					+"\t<A _name='aa'>\n"
						+"\t\t<a>foo</a>\n"
					+"\t</A>\n"
					+"\t<A _name='o'>\n"
						+"\t\t<a>foo</a>\n"
					+"\t</A>\n"
				+"</object>\n",
				"<object>"
					+"<A _name='a'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='ia'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='aa'>"
						+"<a>foo</a>"
					+"</A>"
					+"<A _name='o'>"
						+"<a>foo</a>"
					+"</A>"
				+"</object>",
			},
			{	/* 39 */
				"BeanWithAbstractArrayFields",
				new BeanWithAbstractArrayFields().init(),
				"<object>"
					+"<a>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</a>"
					+"<ia1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</ia1>"
					+"<ia2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</ia2>"
					+"<aa1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</aa1>"
					+"<aa2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</aa2>"
					+"<o1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</o1>"
					+"<o2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</o2>"
				+"</object>",
				"<object>\n"
					+"\t<a>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</a>\n"
					+"\t<ia1>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</ia1>\n"
					+"\t<ia2>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</ia2>\n"
					+"\t<aa1>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</aa1>\n"
					+"\t<aa2>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</aa2>\n"
					+"\t<o1>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</o1>\n"
					+"\t<o2>\n"
						+"\t\t<A>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</o2>\n"
				+"</object>\n",
				"<object>"
					+"<a>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</a>"
					+"<ia1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</ia1>"
					+"<ia2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</ia2>"
					+"<aa1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</aa1>"
					+"<aa2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</aa2>"
					+"<o1>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</o1>"
					+"<o2>"
						+"<A>"
							+"<a>foo</a>"
						+"</A>"
					+"</o2>"
				+"</object>",
			},
			{	/* 40 */
				"BeanWithAbstractMapFields",
				new BeanWithAbstractMapFields().init(),
				"<object>"
					+"<a>"
						+"<A _name='k1'>"
							+"<a>foo</a>"
						+"</A>"
					+"</a>"
					+"<b>"
						+"<A _name='k2'>"
							+"<a>foo</a>"
						+"</A>"
					+"</b>"
					+"<c>"
						+"<A _name='k3'>"
							+"<a>foo</a>"
						+"</A>"
					+"</c>"
				+"</object>",
				"<object>\n"
					+"\t<a>\n"
						+"\t\t<A _name='k1'>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</a>\n"
					+"\t<b>\n"
						+"\t\t<A _name='k2'>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</b>\n"
					+"\t<c>\n"
						+"\t\t<A _name='k3'>\n"
							+"\t\t\t<a>foo</a>\n"
						+"\t\t</A>\n"
					+"\t</c>\n"
				+"</object>\n",
				"<object>"
					+"<a>"
						+"<A _name='k1'>"
							+"<a>foo</a>"
						+"</A>"
					+"</a>"
					+"<b>"
						+"<A _name='k2'>"
							+"<a>foo</a>"
						+"</A>"
					+"</b>"
					+"<c>"
						+"<A _name='k3'>"
							+"<a>foo</a>"
						+"</A>"
					+"</c>"
				+"</object>",
			},
			{	/* 41 */
				"BeanWithAbstractMapArrayFields",
				new BeanWithAbstractMapArrayFields().init(),
				"<object>"
					+"<a>"
						+"<a1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</a1>"
					+"</a>"
					+"<ia>"
						+"<ia1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</ia1>"
						+"<ia2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</ia2>"
					+"</ia>"
					+"<aa>"
						+"<aa1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</aa1>"
						+"<aa2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</aa2>"
					+"</aa>"
					+"<o>"
						+"<o1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</o1>"
						+"<o2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</o2>"
					+"</o>"
				+"</object>",
				"<object>\n"
					+"\t<a>\n"
						+"\t\t<a1>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</a1>\n"
					+"\t</a>\n"
					+"\t<ia>\n"
						+"\t\t<ia1>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</ia1>\n"
						+"\t\t<ia2>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</ia2>\n"
					+"\t</ia>\n"
					+"\t<aa>\n"
						+"\t\t<aa1>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</aa1>\n"
						+"\t\t<aa2>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</aa2>\n"
					+"\t</aa>\n"
					+"\t<o>\n"
						+"\t\t<o1>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</o1>\n"
						+"\t\t<o2>\n"
							+"\t\t\t<A>\n"
								+"\t\t\t\t<a>foo</a>\n"
							+"\t\t\t</A>\n"
						+"\t\t</o2>\n"
					+"\t</o>\n"
				+"</object>\n",
				"<object>"
					+"<a>"
						+"<a1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</a1>"
					+"</a>"
					+"<ia>"
						+"<ia1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</ia1>"
						+"<ia2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</ia2>"
					+"</ia>"
					+"<aa>"
						+"<aa1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</aa1>"
						+"<aa2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</aa2>"
					+"</aa>"
					+"<o>"
						+"<o1>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</o1>"
						+"<o2>"
							+"<A>"
								+"<a>foo</a>"
							+"</A>"
						+"</o2>"
					+"</o>"
				+"</object>",
			},
			{	/* 42 */
				"BeanWithWhitespaceTextFields-1",
				new BeanWithWhitespaceTextFields().init(null),
				"<object nil='true'></object>",
				"<object nil='true'>\n</object>\n",
				"<object nil='true'></object>",
			},
			{	/* 43 */
				"BeanWithWhitespaceTextFields-2",
				new BeanWithWhitespaceTextFields().init(""),
				"<object>_xE000_</object>",
				"<object>_xE000_</object>\n",
				"<object>_xE000_</object>",
			},
			{	/* 44 */
				"BeanWithWhitespaceTextFields-3",
				new BeanWithWhitespaceTextFields().init(" "),
				"<object>_x0020_</object>",
				"<object>_x0020_</object>\n",
				"<object>_x0020_</object>",
			},
			{	/* 45 */
				"BeanWithWhitespaceTextFields-4",
				new BeanWithWhitespaceTextFields().init("  "),
				"<object>_x0020__x0020_</object>",
				"<object>_x0020__x0020_</object>\n",
				"<object>_x0020__x0020_</object>",
			},
			{	/* 46 */
				"BeanWithWhitespaceTextFields-5",
				new BeanWithWhitespaceTextFields().init(" foo\n\tbar "),
				"<object>_x0020_foo&#x000a;&#x0009;bar_x0020_</object>",
				"<object>_x0020_foo&#x000a;&#x0009;bar_x0020_</object>\n",
				"<object>_x0020_foo&#x000a;&#x0009;bar_x0020_</object>",
			},
			{	/* 47 */
				"BeanWithWhitespaceTextPwsFields-1",
				new BeanWithWhitespaceTextPwsFields().init(null),
				"<object nil='true'></object>",
				"<object nil='true'>\n</object>\n",
				"<object nil='true'></object>",
			},
			{	/* 48 */
				"BeanWithWhitespaceTextPwsFields-2",
				new BeanWithWhitespaceTextPwsFields().init(""),
				"<object>_xE000_</object>",
				"<object>_xE000_</object>\n",
				"<object>_xE000_</object>",
			},
			{	/* 49 */
				"BeanWithWhitespaceTextPwsFields-3",
				new BeanWithWhitespaceTextPwsFields().init(" "),
				"<object> </object>",
				"<object> </object>\n",
				"<object> </object>",
			},
			{	/* 50 */
				"BeanWithWhitespaceTextPwsFields-4",
				new BeanWithWhitespaceTextPwsFields().init("  "),
				"<object>  </object>",
				"<object>  </object>\n",
				"<object>  </object>",
			},
			{	/* 51 */
				"BeanWithWhitespaceTextPwsFields-5",
				new BeanWithWhitespaceTextPwsFields().init("  foobar  "),
				"<object>  foobar  </object>",
				"<object>  foobar  </object>\n",
				"<object>  foobar  </object>",
			},
			{	/* 52 */
				"BeanWithWhitespaceMixedFields-1",
				new BeanWithWhitespaceMixedFields().init(null),
				"<object nil='true'></object>",
				"<object nil='true'>\n</object>\n",
				"<object nil='true'></object>",
			},
			{	/* 53 */
				"BeanWithWhitespaceMixedFields-2",
				new BeanWithWhitespaceMixedFields().init(new String[0]),
				"<object></object>",
				"<object></object>\n",
				"<object></object>",
			},
			{	/* 54 */
				"BeanWithWhitespaceMixedFields-3",
				new BeanWithWhitespaceMixedFields().init(new String[]{""}),
				"<object>_xE000_</object>",
				"<object>_xE000_</object>\n",
				"<object>_xE000_</object>",
			},
			{	/* 55 */
				"BeanWithWhitespaceMixedFields-4",
				new BeanWithWhitespaceMixedFields().init(new String[]{" "}),
				"<object>_x0020_</object>",
				"<object>_x0020_</object>\n",
				"<object>_x0020_</object>",
			},
			{	/* 56 */
				"BeanWithWhitespaceMixedFields-5",
				new BeanWithWhitespaceMixedFields().init(new String[]{"  "}),
				"<object>_x0020__x0020_</object>",
				"<object>_x0020__x0020_</object>\n",
				"<object>_x0020__x0020_</object>",
			},
			{	/* 57 */
				"BeanWithWhitespaceMixedFields-6",
				new BeanWithWhitespaceMixedFields().init(new String[]{"  foobar  "}),
				"<object>_x0020_ foobar _x0020_</object>",
				"<object>_x0020_ foobar _x0020_</object>\n",
				"<object>_x0020_ foobar _x0020_</object>",
			},
			{	/* 58 */
				"BeanWithWhitespaceMixedPwsFields-1",
				new BeanWithWhitespaceMixedPwsFields().init(null),
				"<object nil='true'></object>",
				"<object nil='true'>\n</object>\n",
				"<object nil='true'></object>",
			},
			{	/* 59 */
				"BeanWithWhitespaceMixedPwsFields-2",
				new BeanWithWhitespaceMixedPwsFields().init(new String[0]),
				"<object></object>",
				"<object></object>\n",
				"<object></object>",
			},
			{	/* 60 */
				"BeanWithWhitespaceMixedPwsFields-3",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{""}),
				"<object>_xE000_</object>",
				"<object>_xE000_</object>\n",
				"<object>_xE000_</object>",
			},
			{	/* 61 */
				"BeanWithWhitespaceMixedPwsFields-4",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{" "}),
				"<object> </object>",
				"<object> </object>\n",
				"<object> </object>",
			},
			{	/* 62 */
				"BeanWithWhitespaceMixedPwsFields-5",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  "}),
				"<object>  </object>",
				"<object>  </object>\n",
				"<object>  </object>",
			},
			{	/* 63 */
				"BeanWithWhitespaceMixedPwsFields-6",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  foobar  "}),
				"<object>  foobar  </object>",
				"<object>  foobar  </object>\n",
				"<object>  foobar  </object>",
			},
		});
	}

	private String label, e1, e2, e3;
	private Object in;

	public BasicXmlTest(String label, Object in, String e1, String e2, String e3) throws Exception {
		this.label = label;
		this.in = in;
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}

	@Test
	public void serializeNormal() {
		try {
			String r = s1.serialize(in);
			assertEquals(label + " serialize-normal failed", e1, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}

	@Test
	public void parseNormal() {
		try {
			String r = s1.serialize(in);
			Class<?> c = in == null ? Object.class : in.getClass();
			Object o = parser.parse(r, c);
			r = s1.serialize(o);
			assertEquals(label + " parse-normal failed", e1, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}

	@Test
	public void serializeReadable() {
		try {
			String r = s2.serialize(in);
			assertEquals(label + " serialize-readable failed", e2, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}

	@Test
	public void parseReadable() {
		try {
			String r = s2.serialize(in);
			Class<?> c = in == null ? Object.class : in.getClass();
			Object o = parser.parse(r, c);
			r = s2.serialize(o);
			assertEquals(label + " parse-readable failed", e2, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}

	@Test
	public void serializeNsEnabled() {
		try {
			String r = s3.serialize(in);
			assertEquals(label + " serialize-ns-enabled failed", e3, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}

	@Test
	public void parseNsEnabled() {
		try {
			String r = s3.serialize(in);
			Class<?> c = in == null ? Object.class : in.getClass();
			Object o = parser.parse(r, c);
			r = s3.serialize(o);
			assertEquals(label + " parse-ns-enabled failed", e3, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(label + " test failed", e);
		}
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Test beans
	//-----------------------------------------------------------------------------------------------------------------

	public static class MapWithStrings extends LinkedHashMap<String,String> {
		public MapWithStrings append(String key, String value) {
			put(key, value);
			return this;
		}
	}

	public static class MapWithNumbers extends LinkedHashMap<String,Number> {
		public MapWithNumbers append(String key, Number value) {
			put(key, value);
			return this;
		}
	}

	public static class MapWithObjects extends LinkedHashMap<String,Object> {
		public MapWithObjects append(String key, Object value) {
			put(key, value);
			return this;
		}
	}

	public static class ListWithStrings extends ArrayList<String> {
		public ListWithStrings append(String value) {
			this.add(value);
			return this;
		}
	}

	public static class ListWithNumbers extends ArrayList<Number> {
		public ListWithNumbers append(Number value) {
			this.add(value);
			return this;
		}
	}

	public static class ListWithObjects extends ArrayList<Object> {
		public ListWithObjects append(Object value) {
			this.add(value);
			return this;
		}
	}

	public static class BeanWithNormalProperties {
		public String a;
		public int b;
		public Object c;
		public Object d;
		public Bean1a e;
		public String[] f;
		public int[] g;

		BeanWithNormalProperties init() {
			a = "foo";
			b = 123;
			c = "bar";
			d = 456;
			e = new Bean1a().init();
			f = new String[]{ "baz" };
			g = new int[]{ 789 };
			return this;
		}
	}

	public static class Bean1a {
		public String h;

		Bean1a init() {
			h = "qux";
			return this;
		}
	}

	public static class BeanWithMapProperties {
		@Beanp(type=MapWithStrings.class)
		public Map<String,String> a;
		@Beanp(type=MapWithNumbers.class)
		public Map<String,Number> b;
		@Beanp(type=MapWithObjects.class)
		public Map<String,Object> c;

		BeanWithMapProperties init() {
			a = new MapWithStrings().append("k1","foo");
			b = new MapWithNumbers().append("k2",123);
			c = new MapWithObjects().append("k3","bar").append("k4",456).append("k5",true).append("k6",null);
			return this;
		}
	}

	@Bean(typeName="X")
	public static class BeanWithTypeName {
		public int a;
		public String b;

		BeanWithTypeName init() {
			a = 123;
			b = "foo";
			return this;
		}
	}

	@Bean(dictionary={B.class})
	public static class BeanWithPropertiesWithTypeNames {
		public B b1;
		public Object b2;

		BeanWithPropertiesWithTypeNames init() {
			b1 = new B().init();
			b2 = new B().init();
			return this;
		}
	}

	@Bean(dictionary={B.class})
	public static class BeanWithPropertiesWithArrayTypeNames {
		public B[] b1;
		public Object[] b2;
		public Object[] b3;

		BeanWithPropertiesWithArrayTypeNames init() {
			b1 = new B[]{new B().init()};
			b2 = new B[]{new B().init()};
			b3 = new Object[]{new B().init()};
			return this;
		}
	}

	@Bean(dictionary={B.class})
	public static class BeanWithPropertiesWith2dArrayTypeNames {
		public B[][] b1;
		public Object[][] b2;
		public Object[][] b3;

		BeanWithPropertiesWith2dArrayTypeNames init() {
			b1 = new B[][]{{new B().init()}};
			b2 = new B[][]{{new B().init()}};
			b3 = new Object[][]{{new B().init()}};
			return this;
		}
	}

	@Bean(dictionary={B.class})
	public static class BeanWithPropertiesWithMapTypeNames {
		public Map<String,B> b1;
		public Map<String,Object> b2;

		BeanWithPropertiesWithMapTypeNames init() {
			b1 = new HashMap<>();
			b1.put("k1", new B().init());
			b2 = new HashMap<>();
			b2.put("k2", new B().init());
			return this;
		}
	}

	@Bean(typeName="B")
	public static class B {
		public String b;

		B init() {
			b = "foo";
			return this;
		}
	}

	public static class BeanWithChildTypeNames {
		public BeanX a;
		@Beanp(dictionary=BeanX.class)
		public Object b;
		public BeanX[] c;
		@Beanp(dictionary=BeanX.class)
		public Object[] d;
		BeanWithChildTypeNames init() {
			a = new BeanX().init();
			b = new BeanX().init();
			c = new BeanX[]{new BeanX().init()};
			d = new Object[]{new BeanX().init()};
			return this;
		}
	}

	public static class BeanWithChildName {
		@Xml(childName = "X")
		public String[] a;
		@Xml(childName = "Y")
		public int[] b;
		BeanWithChildName init() {
			a = new String[] { "foo", "bar" };
			b = new int[] { 123, 456 };
			return this;
		}
	}

	public static class BeanWithXmlFormatAttrProperty {
		@Xml(format=XmlFormat.ATTR)
		public String a;
		@Xml(format=XmlFormat.ATTR)
		public int b;
		BeanWithXmlFormatAttrProperty init() {
			a = "foo";
			b = 123;
			return this;
		}
	}

	@Xml(format=XmlFormat.ATTRS)
	public static class BeanWithXmlFormatAttrs {
		public String a;
		public int b;
		BeanWithXmlFormatAttrs init() {
			a = "foo";
			b = 123;
			return this;
		}
	}

	@Xml(format=XmlFormat.ATTRS)
	public static class BeanWithXmlFormatElementProperty {
		public String a;
		@Xml(format=XmlFormat.ELEMENT)
		public int b;
		BeanWithXmlFormatElementProperty init() {
			a = "foo";
			b = 123;
			return this;
		}
	}

	public static class BeanWithXmlFormatAttrsProperty {
		@Xml(format=XmlFormat.ATTRS)
		public Map<String,Object> a;
		@Xml(format=XmlFormat.ATTR)
		public int b;
		BeanWithXmlFormatAttrsProperty init() {
			a = JsonMap.of("k1", "foo", "k2", 123);
			b = 456;
			return this;
		}
	}

	public static class BeanWithXmlFormatCollapsedProperty {
		@Xml(childName="A",format=XmlFormat.COLLAPSED)
		public String[] a;
		@Xml(childName="B",format=XmlFormat.COLLAPSED)
		public int[] b;
		BeanWithXmlFormatCollapsedProperty init() {
			a = new String[]{"foo","bar"};
			b = new int[]{123,456};
			return this;
		}
	}

	public static class BeanWithXmlFormatTextProperty {
		@Xml(format=XmlFormat.ATTR)
		public String a;
		@Xml(format=XmlFormat.TEXT)
		public String b;
		BeanWithXmlFormatTextProperty init() {
			a = "foo";
			b = "bar";
			return this;
		}
	}

	public static class BeanWithXmlFormatXmlTextProperty {
		@Xml(format=XmlFormat.ATTR)
		public String a;
		@Xml(format=XmlFormat.XMLTEXT)
		public String b;
		BeanWithXmlFormatXmlTextProperty init() {
			a = "foo";
			b = "bar<b>baz</b>qux";
			return this;
		}
	}

	public static class BeanWithXmlFormatElementsPropertyCollection {
		@Xml(format=XmlFormat.ATTR)
		public String a;
		@Xml(format=XmlFormat.ELEMENTS)
		public Object[] b;
		BeanWithXmlFormatElementsPropertyCollection init() {
			a = "foo";
			b = new Object[]{"bar","baz",123,true,null};
			return this;
		}
	}

	public static class BeanWithMixedContent {
		@Xml(format=XmlFormat.MIXED)
		@Beanp(dictionary={BeanXSimple.class, BeanYSimple.class})
		public Object[] a;
		BeanWithMixedContent init() {
			a = new Object[]{
				"foo",
				new BeanXSimple().init(),
				"bar",
				new BeanYSimple().init(),
				"baz"
			};
			return this;
		}
	}

	@Bean(typeName="X")
	public static class BeanX {
		public String fx;
		BeanX init() {
			fx = "fx1";
			return this;
		}
	}

	@Bean(typeName="X")
	public static class BeanXSimple {
		@Xml(format=XmlFormat.ATTR)
		public String fx;
		BeanXSimple init() {
			fx = "fx1";
			return this;
		}
	}

	@Bean(typeName="Y")
	public static class BeanY {
		public String fy;
		BeanY init() {
			fy = "fy1";
			return this;
		}
	}

	@Bean(typeName="Y")
	public static class BeanYSimple {
		@Xml(format=XmlFormat.ATTR)
		public String fy;
		BeanYSimple init() {
			fy = "fy1";
			return this;
		}
	}

	public static class BeanWithSpecialCharacters {
		public String a;

		BeanWithSpecialCharacters init() {
			a = "  \b\f\n\t\r  ";
			return this;
		}
	}

	@Bean(typeName="  \b\f\n\t\r  ")
	public static class BeanWithSpecialCharacters2 {

		@Beanp(name="  \b\f\n\t\r  ")
		public String a;

		BeanWithSpecialCharacters2 init() {
			a = "  \b\f\n\t\r  ";
			return this;
		}
	}

	public static class BeanWithNullProperties {
		public String a;
		public String[] b;
	}

	@Bean(dictionary={A.class},p="a,ia,aa,o")
	public static class BeanWithAbstractFields {
		public A a;
		public IA ia;
		public AA aa;
		public Object o;

		BeanWithAbstractFields init() {
			ia = new A().init();
			aa = new A().init();
			a = new A().init();
			o = new A().init();
			return this;
		}
	}

	@Bean(dictionary={A.class},p="a,ia1,ia2,aa1,aa2,o1,o2")
	public static class BeanWithAbstractArrayFields {
		public A[] a;
		public IA[] ia1, ia2;
		public AA[] aa1, aa2;
		public Object[] o1, o2;

		BeanWithAbstractArrayFields init() {
			a = new A[]{new A().init()};
			ia1 = new A[]{new A().init()};
			aa1 = new A[]{new A().init()};
			o1 = new A[]{new A().init()};
			ia2 = new IA[]{new A().init()};
			aa2 = new AA[]{new A().init()};
			o2 = new Object[]{new A().init()};
			return this;
		}
	}

	@Bean(dictionary={A.class})
	public static class BeanWithAbstractMapFields {
		public Map<String,A> a;
		public Map<String,AA> b;
		public Map<String,Object> c;

		BeanWithAbstractMapFields init() {
			a = new HashMap<>();
			b = new HashMap<>();
			c = new HashMap<>();
			a.put("k1", new A().init());
			b.put("k2", new A().init());
			c.put("k3", new A().init());
			return this;
		}
	}

	@Bean(dictionary={A.class},p="a,ia,aa,o")
	public static class BeanWithAbstractMapArrayFields {
		public Map<String,A[]> a;
		public Map<String,IA[]> ia;
		public Map<String,AA[]> aa;
		public Map<String,Object[]> o;

		BeanWithAbstractMapArrayFields init() {
			a = new LinkedHashMap<>();
			ia = new LinkedHashMap<>();
			aa = new LinkedHashMap<>();
			o = new LinkedHashMap<>();
			a.put("a1", new A[]{new A().init()});
			ia.put("ia1", new A[]{new A().init()});
			ia.put("ia2", new IA[]{new A().init()});
			aa.put("aa1", new A[]{new A().init()});
			aa.put("aa2", new AA[]{new A().init()});
			o.put("o1", new A[]{new A().init()});
			o.put("o2", new Object[]{new A().init()});
			return this;
		}
	}

	public static interface IA {
		public String getA();
		public void setA(String a);
	}

	public static abstract class AA implements IA {}

	@Bean(typeName="A")
	public static class A extends AA {
		private String a;

		@Override
		public String getA() {
			return a;
		}

		@Override
		public void setA(String a) {
			this.a = a;
		}

		A init() {
			this.a = "foo";
			return this;
		}
	}

	public static class BeanWithWhitespaceTextFields {
		@Xml(format=XmlFormat.TEXT)
		public String a;

		public BeanWithWhitespaceTextFields init(String s) {
			a = s;
			return this;
		}
	}

	public static class BeanWithWhitespaceTextPwsFields {
		@Xml(format=XmlFormat.TEXT_PWS)
		public String a;

		public BeanWithWhitespaceTextPwsFields init(String s) {
			a = s;
			return this;
		}
	}

	public static class BeanWithWhitespaceMixedFields {
		@Xml(format=XmlFormat.MIXED)
		public String[] a;

		public BeanWithWhitespaceMixedFields init(String[] s) {
			a = s;
			return this;
		}
	}

	public static class BeanWithWhitespaceMixedPwsFields {
		@Xml(format=XmlFormat.MIXED_PWS)
		public String[] a;

		public BeanWithWhitespaceMixedPwsFields init(String[] s) {
			a = s;
			return this;
		}
	}
}
