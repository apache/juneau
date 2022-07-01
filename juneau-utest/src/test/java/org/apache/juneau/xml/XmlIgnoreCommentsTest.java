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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Validates that comments in XML files are ignored.
 */
@RunWith(Parameterized.class)
@SuppressWarnings("serial")
@FixMethodOrder(NAME_ASCENDING)
public class XmlIgnoreCommentsTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {

		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				"SimpleTypes-1",
				String.class,
				"foo",
				"|<string>|foo|</string>|",
				false
			},
			{ 	/* 1 */
				"SimpleTypes-2",
				Boolean.class,
				true,
				"|<boolean>|true|</boolean>|",
				false
			},
			{ 	/* 2 */
				"SimpleTypes-3",
				int.class,
				123,
				"|<number>|123|</number>|",
				false
			},
			{ 	/* 3 */
				"SimpleTypes-4",
				float.class,
				1.23f,
				"|<number>|1.23|</number>|",
				false
			},
			{ 	/* 4 */
				"SimpleTypes-5",
				String.class,
				null,
				"|<null/>|",
				false
			},
			{ 	/* 5 */
				"Arrays-1",
				String[].class,
				new String[]{"foo"},
				"|<array>|<string>|foo|</string>|</array>|",
				false
			},
			{ 	/* 6 */
				"Arrays-2",
				String[].class,
				new String[]{null},
				"|<array>|<null/>|</array>|",
				false
			},
			{ 	/* 7 */
				"Arrays-3",
				Object[].class,
				new Object[]{"foo"},
				"|<array>|<string>|foo|</string>|</array>|",
				false
			},
			{ 	/* 8 */
				"Arrays-4",
				int[].class,
				new int[]{123},
				"|<array>|<number>|123|</number>|</array>|",
				false
			},
			{ 	/* 9 */
				"Arrays-5",
				boolean[].class,
				new boolean[]{true},
				"|<array>|<boolean>|true|</boolean>|</array>|",
				false
			},
			{ 	/* 10 */
				"Arrays-6",
				String[][].class,
				new String[][]{{"foo"}},
				"|<array>|<array>|<string>|foo|</string>|</array>|</array>|",
				false
			},
			{ 	/* 11 */
				"MapWithStrings",
				MapWithStrings.class,
				new MapWithStrings().append("k1", "v1").append("k2", null),
				"|<object>|<k1>|v1|</k1>|<k2 _type='null'/>|</object>|",
				false
			},
			{ 	/* 12 */
				"MapsWithNumbers",
				MapWithNumbers.class,
				new MapWithNumbers().append("k1", 123).append("k2", 1.23).append("k3", null),
				"|<object>|<k1>|123|</k1>|<k2>|1.23|</k2>|<k3 _type='null'/>|</object>|",
				false
			},
			{ 	/* 13 */
				"MapWithObjects",
				MapWithObjects.class,
				new MapWithObjects().append("k1", "v1").append("k2", 123).append("k3", 1.23).append("k4", true).append("k5", null),
				"|<object>|<k1>|v1|</k1>|<k2 _type='number'>|123|</k2>|<k3 _type='number'>|1.23|</k3>|<k4 _type='boolean'>|true|</k4>|<k5 _type='null'/>|</object>|",
				false
			},
			{ 	/* 14 */
				"ListWithStrings",
				ListWithStrings.class,
				new ListWithStrings().append("foo").append(null),
				"|<array>|<string>|foo|</string>|<null/>|</array>|",
				false
			},
			{ 	/* 15 */
				"ListWithNumbers",
				ListWithNumbers.class,
				new ListWithNumbers().append(123).append(1.23).append(null),
				"|<array>|<number>|123|</number>|<number>|1.23|</number>|<null/>|</array>|",
				false
			},
			{ 	/* 16 */
				"ListWithObjects",
				ListWithObjects.class,
				new ListWithObjects().append("foo").append(123).append(1.23).append(true).append(null),
				"|<array>|<string>|foo|</string>|<number>|123|</number>|<number>|1.23|</number>|<boolean>|true|</boolean>|<null/>|</array>|",
				false
			},
			{ 	/* 17 */
				"BeanWithNormalProperties",
				BeanWithNormalProperties.class,
				new BeanWithNormalProperties().init(),
				"|<object>"
					+"|<a>|foo|</a>"
					+"|<b>|123|</b>"
					+"|<c>|bar|</c>"
					+"|<d _type='number'>|456|</d>"
					+"|<e>"
						+"|<h>|qux|</h>"
					+"|</e>"
					+"|<f>"
						+"|<string>|baz|</string>"
					+"|</f>"
					+"|<g>"
						+"|<number>|789|</number>"
					+"|</g>"
				+"|</object>|",
				false
			},
			{ 	/* 18 */
				"BeanWithMapProperties",
				BeanWithMapProperties.class,
				new BeanWithMapProperties().init(),
				"|<object>"
					+"|<a>"
						+"|<k1>|foo|</k1>"
					+"|</a>"
					+"|<b>"
						+"|<k2>|123|</k2>"
					+"|</b>"
					+"|<c>"
						+"|<k3>|bar|</k3>"
						+"|<k4 _type='number'>|456|</k4>"
						+"|<k5 _type='boolean'>|true|</k5>"
						+"|<k6 _type='null'/>"
					+"|</c>"
				+"|</object>|",
				false
			},
			{ 	/* 19 */
				"BeanWithTypeName",
				BeanWithTypeName.class,
				new BeanWithTypeName().init(),
				"|<X>|<a>|123|</a>|<b>|foo|</b>|</X>|",
				false
			},
			{ 	/* 20 */
				"BeanWithPropertiesWithTypeNames",
				BeanWithPropertiesWithTypeNames.class,
				new BeanWithPropertiesWithTypeNames().init(),
				"|<object>|<b1>|<b>|foo|</b>|</b1>|<b2 _type='B'>|<b>|foo|</b>|</b2>|</object>|",
				false
			},
			{ 	/* 21 */
				"BeanWithPropertiesWithArrayTypeNames",
				BeanWithPropertiesWithArrayTypeNames.class,
				new BeanWithPropertiesWithArrayTypeNames().init(),
				"|<object>"
					+"|<b1>"
						+"|<B>"
							+"|<b>|foo|</b>"
						+"|</B>"
					+"|</b1>"
					+"|<b2>"
						+"|<B>"
							+"|<b>|foo|</b>"
						+"|</B>"
					+"|</b2>"
					+"|<b3>"
						+"|<B>"
							+"|<b>|foo|</b>"
						+"|</B>"
					+"|</b3>"
				+"|</object>|",
				false
			},
			{ 	/* 22 */
				"BeanWithPropertiesWithArray2dTypeNames",
				BeanWithPropertiesWith2dArrayTypeNames.class,
				new BeanWithPropertiesWith2dArrayTypeNames().init(),
				"|<object>"
					+"|<b1>"
						+"|<array>"
							+"|<B>"
								+"|<b>|foo|</b>"
							+"|</B>"
						+"|</array>"
					+"|</b1>"
					+"|<b2>"
						+"|<array>"
							+"|<B>"
								+"|<b>|foo|</b>"
							+"|</B>"
						+"|</array>"
					+"|</b2>"
					+"|<b3>"
						+"|<array>"
							+"|<B>"
								+"|<b>|foo|</b>"
							+"|</B>"
						+"|</array>"
					+"|</b3>"
				+"|</object>|",
				false
			},
			{ 	/* 23 */
				"BeanWithPropertiesWithMapTypeNames",
				BeanWithPropertiesWithMapTypeNames.class,
				new BeanWithPropertiesWithMapTypeNames().init(),
				"|<object>"
					+"|<b1>"
						+"|<k1>"
							+"|<b>|foo|</b>"
						+"|</k1>"
					+"|</b1>"
					+"|<b2>"
						+"|<k2 _type='B'>"
							+"|<b>|foo|</b>"
						+"|</k2>"
					+"|</b2>"
				+"|</object>|",
				false
			},
			{ 	/* 24 */
				"BeanWithChildTypeNames",
				BeanWithChildTypeNames.class,
				new BeanWithChildTypeNames().init(),
				"|<object>"
					+"|<a>"
						+"|<fx>|fx1|</fx>"
					+"|</a>"
					+"|<b _type='X'>"
						+"|<fx>|fx1|</fx>"
					+"|</b>"
					+"|<c>"
						+"|<X>"
							+"|<fx>|fx1|</fx>"
						+"|</X>"
					+"|</c>"
					+"|<d>"
						+"|<X>"
							+"|<fx>|fx1|</fx>"
						+"|</X>"
					+"|</d>"
				+"|</object>|",
				false
			},
			{ 	/* 25 */
				"BeanWithChildName",
				BeanWithChildName.class,
				new BeanWithChildName().init(),
				"|<object>|<a>|<X>|foo|</X>|<X>|bar|</X>|</a>|<b>|<Y>|123|</Y>|<Y>|456|</Y>|</b>|</object>|",
				false
			},
			{ 	/* 26 */
				"BeanWithXmlFormatAttrProperty",
				BeanWithXmlFormatAttrProperty.class,
				new BeanWithXmlFormatAttrProperty().init(),
				"|<object a='foo' b='123'/>|",
				false
			},
			{ 	/* 27 */
				"BeanWithXmlFormatAttrs",
				BeanWithXmlFormatAttrs.class,
				new BeanWithXmlFormatAttrs().init(),
				"|<object a='foo' b='123'/>|",
				false
			},
			{ 	/* 28 */
				"BeanWithXmlFormatElementProperty",
				BeanWithXmlFormatElementProperty.class,
				new BeanWithXmlFormatElementProperty().init(),
				"|<object a='foo'><b>123</b></object>|",
				false
			},
			{ 	/* 29 */
				"BeanWithXmlFormatAttrsProperty",
				BeanWithXmlFormatAttrsProperty.class,
				new BeanWithXmlFormatAttrsProperty().init(),
				"|<object k1='foo' k2='123' b='456'/>|",
				false
			},
			{ 	/* 30 */
				"BeanWithXmlFormatCollapsedProperty",
				BeanWithXmlFormatCollapsedProperty.class,
				new BeanWithXmlFormatCollapsedProperty().init(),
				"|<object>|<A>|foo|</A>|<A>|bar|</A>|<B>|123|</B>|<B>|456|</B>|</object>|",
				false
			},
			{ 	/* 31 */
				"BeanWithXmlFormatTextProperty",
				BeanWithXmlFormatTextProperty.class,
				new BeanWithXmlFormatTextProperty().init(),
				"|<object a='foo'>|bar|</object>|",
				false
			},
			{ 	/* 32 */
				"BeanWithXmlFormatXmlTextProperty",
				BeanWithXmlFormatXmlTextProperty.class,
				new BeanWithXmlFormatXmlTextProperty().init(),
				"|<object a='foo'>|bar|<b>|baz|</b>|qux|</object>|",
				false
			},
			{ 	/* 33 */
				"BeanWithXmlFormatElementsPropertyCollection",
				BeanWithXmlFormatElementsPropertyCollection.class,
				new BeanWithXmlFormatElementsPropertyCollection().init(),
				"|<object a='foo'>|<string>|bar|</string>|<string>|baz|</string>|<number>|123|</number>|<boolean>|true|</boolean>|<null/>|</object>|",
				false
			},
			{ 	/* 34 */
				"BeanWithMixedContent",
				BeanWithMixedContent.class,
				new BeanWithMixedContent().init(),
				"|<object>|foo|<X fx='fx1'/>|bar|<Y fy='fy1'/>|baz|</object>|",
				true
			},
			{ 	/* 35 */
				"BeanWithSpecialCharacters",
				BeanWithSpecialCharacters.class,
				new BeanWithSpecialCharacters().init(),
				"|<object>|<a>|_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_|</a>|</object>|",
				false
			},
			{ 	/* 36 */
				"BeanWithSpecialCharacters2",
				BeanWithSpecialCharacters2.class,
				new BeanWithSpecialCharacters2().init(),
				"|<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>|<_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>|_x0020_ _x0008__x000C_&#x000a;&#x0009;&#x000d; _x0020_|</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>|</_x0020__x0020__x0008__x000C__x000A__x0009__x000D__x0020__x0020_>|",
				false
			},
			{ 	/* 37 */
				"BeanWithNullProperties",
				BeanWithNullProperties.class,
				new BeanWithNullProperties(),
				"|<object/>|",
				false
			},
			{ 	/* 38 */
				"BeanWithAbstractFields",
				BeanWithAbstractFields.class,
				new BeanWithAbstractFields().init(),
				"|<object>"
					+"|<a>"
						+"|<a>|foo|</a>"
					+"|</a>"
					+"|<ia _type='A'>"
						+"|<a>|foo|</a>"
					+"|</ia>"
					+"|<aa _type='A'>"
						+"|<a>|foo|</a>"
					+"|</aa>"
					+"|<o _type='A'>"
						+"|<a>|foo|</a>"
					+"|</o>"
				+"|</object>|",
				false
			},
			{ 	/* 39 */
				"BeanWithAbstractArrayFields",
				BeanWithAbstractArrayFields.class,
				new BeanWithAbstractArrayFields().init(),
				"|<object>"
					+"|<a>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</a>"
					+"|<ia1>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</ia1>"
					+"|<ia2>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</ia2>"
					+"|<aa1>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</aa1>"
					+"|<aa2>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</aa2>"
					+"|<o1>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</o1>"
					+"|<o2>"
						+"|<A>"
							+"|<a>|foo|</a>"
						+"|</A>"
					+"|</o2>"
				+"|</object>",
				false
			},
			{ 	/* 40 */
				"BeanWithAbstractMapFields",
				BeanWithAbstractMapFields.class,
				new BeanWithAbstractMapFields().init(),
				"|<object>"
					+"|<a>"
						+"|<k1>"
							+"|<a>|foo|</a>"
						+"|</k1>"
					+"|</a>"
					+"|<b>"
						+"|<k2 _type='A'>"
							+"|<a>|foo|</a>"
						+"|</k2>"
					+"|</b>"
					+"|<c>"
						+"|<k3 _type='A'>"
							+"|<a>|foo|</a>"
						+"|</k3>"
					+"|</c>"
				+"|</object>",
				false
			},
			{ 	/* 41 */
				"BeanWithAbstractMapArrayFields",
				BeanWithAbstractMapArrayFields.class,
				new BeanWithAbstractMapArrayFields().init(),
				"|<object>"
					+"|<a>"
						+"|<a1>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</a1>"
					+"|</a>"
					+"|<ia>"
						+"|<ia1>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</ia1>"
						+"|<ia2>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</ia2>"
					+"|</ia>"
					+"|<aa>"
						+"|<aa1>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</aa1>"
						+"|<aa2>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</aa2>"
					+"|</aa>"
					+"|<o>"
						+"|<o1>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</o1>"
						+"|<o2>"
							+"|<A>"
								+"|<a>|foo|</a>"
							+"|</A>"
						+"|</o2>"
					+"|</o>"
				+"|</object>",
				false
			},
			{ 	/* 42 */
				"BeanWithWhitespaceTextFields-1",
				BeanWithWhitespaceTextFields.class,
				new BeanWithWhitespaceTextFields().init(null),
				"|<object/>|",
				false
			},
			{ 	/* 43 */
				"BeanWithWhitespaceTextFields-2",
				BeanWithWhitespaceTextFields.class,
				new BeanWithWhitespaceTextFields().init(""),
				"|<object>|_xE000_|</object>|",
				false
			},
			{ 	/* 44 */
				"BeanWithWhitespaceTextFields-3",
				BeanWithWhitespaceTextFields.class,
				new BeanWithWhitespaceTextFields().init(" "),
				"|<object>|_x0020_|</object>|",
				false
			},
			{ 	/* 45 */
				"BeanWithWhitespaceTextFields-4",
				BeanWithWhitespaceTextFields.class,
				new BeanWithWhitespaceTextFields().init("  "),
				"|<object>|_x0020__x0020_|</object>|",
				false
			},
			{ 	/* 46 */
				"BeanWithWhitespaceTextFields-5",
				BeanWithWhitespaceTextFields.class,
				new BeanWithWhitespaceTextFields().init(" foo\n\tbar "),
				"|<object>|_x0020_foo&#x000a;&#x0009;bar_x0020_|</object>|",
				false
			},
			{ 	/* 47 */
				"BeanWithWhitespaceTextPwsFields-1",
				BeanWithWhitespaceTextPwsFields.class,
				new BeanWithWhitespaceTextPwsFields().init(null),
				"|<object/>|",
				false
			},
			{ 	/* 48 */
				"BeanWithWhitespaceTextPwsFields-2",
				BeanWithWhitespaceTextPwsFields.class,
				new BeanWithWhitespaceTextPwsFields().init(""),
				"|<object>|_xE000_|</object>|",
				true
			},
			{ 	/* 49 */
				"BeanWithWhitespaceTextPwsFields-3",
				BeanWithWhitespaceTextPwsFields.class,
				new BeanWithWhitespaceTextPwsFields().init(" "),
				"|<object>| |</object>|",
				true
			},
			{ 	/* 50 */
				"BeanWithWhitespaceTextPwsFields-4",
				BeanWithWhitespaceTextPwsFields.class,
				new BeanWithWhitespaceTextPwsFields().init("  "),
				"|<object>|  |</object>|",
				true
			},
			{ 	/* 51 */
				"BeanWithWhitespaceTextPwsFields-5",
				BeanWithWhitespaceTextPwsFields.class,
				new BeanWithWhitespaceTextPwsFields().init("  foobar  "),
				"|<object>|  foobar  |</object>|",
				true
			},
			{ 	/* 52 */
				"BeanWithWhitespaceMixedFields-1",
				BeanWithWhitespaceMixedFields.class,
				new BeanWithWhitespaceMixedFields().init(null),
				"|<object nil='true'></object>|",
				false
			},
			{ 	/* 53 */
				"BeanWithWhitespaceMixedFields-3",
				BeanWithWhitespaceMixedFields.class,
				new BeanWithWhitespaceMixedFields().init(new String[]{""}),
				"|<object>|_xE000_|</object>|",
				true
			},
			{ 	/* 54 */
				"BeanWithWhitespaceMixedFields-4",
				BeanWithWhitespaceMixedFields.class,
				new BeanWithWhitespaceMixedFields().init(new String[]{" "}),
				"|<object>|_x0020_|</object>|",
				true
			},
			{ 	/* 55 */
				"BeanWithWhitespaceMixedFields-5",
				BeanWithWhitespaceMixedFields.class,
				new BeanWithWhitespaceMixedFields().init(new String[]{"  "}),
				"|<object>|_x0020__x0020_|</object>|",
				true
			},
			{ 	/* 56 */
				"BeanWithWhitespaceMixedFields-6",
				BeanWithWhitespaceMixedFields.class,
				new BeanWithWhitespaceMixedFields().init(new String[]{"  foobar  "}),
				"|<object>|_x0020_ foobar _x0020_|</object>|",
				true
			},
			{ 	/* 57 */
				"BeanWithWhitespaceMixedPwsFields-1",
				BeanWithWhitespaceMixedPwsFields.class,
				new BeanWithWhitespaceMixedPwsFields().init(null),
				"|<object nil='true'></object>|",
				false
			},
			{ 	/* 58 */
				"BeanWithWhitespaceMixedPwsFields-3",
				BeanWithWhitespaceMixedPwsFields.class,
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{""}),
				"|<object>|_xE000_|</object>|",
				true
			},
			{ 	/* 59 */
				"BeanWithWhitespaceMixedPwsFields-4",
				BeanWithWhitespaceMixedPwsFields.class,
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{" "}),
				"|<object>| |</object>|",
				true
			},
			{ 	/* 60 */
				"BeanWithWhitespaceMixedPwsFields-5",
				BeanWithWhitespaceMixedPwsFields.class,
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  "}),
				"|<object>|  |</object>|",
				true
			},
			{ 	/* 61 */
				"BeanWithWhitespaceMixedPwsFields-6",
				BeanWithWhitespaceMixedPwsFields.class,
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  foobar  "}),
				"|<object>|  foobar  |</object>|",
				true
			},
		});
	}

	private String label;
	private Class<?> type;
	private Object expected;
	private String input;
	private boolean skipWsTests;

	public XmlIgnoreCommentsTest(String label, Class<?> type, Object expected, String input, boolean skipWsTests) throws Exception {
		this.label = label;
		this.type = type;
		this.expected = expected;
		this.input = input;
		this.skipWsTests = skipWsTests;
	}

	@Test
	public void testNoComment() throws Exception {
		try {
			Object actual = XmlParser.DEFAULT.parse(input.replace("|", ""), type);
			assertObject(expected).isSameJsonAs(actual);
		} catch (ComparisonFailure e) {
			throw new ComparisonFailure(MessageFormat.format("Test ''{0}'' failed with comparison error", label), e.getExpected(), e.getActual());
		} catch (Exception e) {
			throw new BasicRuntimeException(e, "Test ''{0}'' failed with error ''{1}''", label, e.getMessage());
		}
	}

	@Test
	public void testNormalComment() throws Exception {
		try {
			Object actual = XmlParser.DEFAULT.parse(input.replace("|", "<!--x-->"), type);
			assertObject(expected).isSameJsonAs(actual);
		} catch (ComparisonFailure e) {
			throw new ComparisonFailure(MessageFormat.format("Test ''{0}'' failed with comparison error", label), e.getExpected(), e.getActual());
		} catch (Exception e) {
			throw new BasicRuntimeException(e, "Test ''{0}'' failed with error ''{1}''", label, e.getMessage());
		}
	}

	@Test
	public void testCommentWithWhitespace() throws Exception {
		try {
			Object actual = XmlParser.DEFAULT.parse(input.replace("|", " \n <!-- \n x \n --> \n "), type);
			if (! skipWsTests)
				assertObject(expected).isSameJsonAs(actual);
		} catch (ComparisonFailure e) {
			throw new ComparisonFailure(MessageFormat.format("Test ''{0}'' failed with comparison error", label), e.getExpected(), e.getActual());
		} catch (Exception e) {
			throw new BasicRuntimeException(e, "Test ''{0}'' failed with error ''{1}''", label, e.getMessage());
		}
	}

	@Test
	public void testDoubleCommentsWithWhitespace() throws Exception {
		try {
			Object actual = XmlParser.DEFAULT.parse(input.replace("|", " \n <!-- \n x \n --> \n \n <!-- \n x \n --> \n "), type);
			if (! skipWsTests)
				assertObject(expected).isSameJsonAs(actual);
		} catch (ComparisonFailure e) {
			throw new ComparisonFailure(MessageFormat.format("Test ''{0}'' failed with comparison error", label), e.getExpected(), e.getActual());
		} catch (Exception e) {
			throw new BasicRuntimeException(e, "Test ''{0}'' failed with error ''{1}''", label, e.getMessage());
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
			a = JsonMap.of("k1", "foo", "k2", "123");
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

	@Bean(dictionary={A.class})
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

	@Bean(dictionary={A.class})
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

	@Bean(dictionary={A.class})
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
