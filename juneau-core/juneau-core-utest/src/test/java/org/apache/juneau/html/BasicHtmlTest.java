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
package org.apache.juneau.html;

import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({"serial","rawtypes","unchecked"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicHtmlTest {

	private static final HtmlSerializer
		s1 = HtmlSerializer.DEFAULT_SQ.builder().addRootType().build(),
		s2 = HtmlSerializer.DEFAULT_SQ_READABLE.builder().addRootType().build(),
		s3 = HtmlSerializer.DEFAULT_SQ.builder().build();
	private static final HtmlParser parser = HtmlParser.DEFAULT;

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			{	/* 0 */
				new Input<String>(
					"SimpleTypes-1",
					String.class,
					"foo",
					"<string>foo</string>",
					"<string>foo</string>",
					"<string>foo</string>"
				)
				{
					@Override
					public void verify(String o) {
						assertInstanceOf(String.class, o);
					}
				}
			},
			{	/* 1 */
				new Input<Boolean>(
					"SimpleTypes-2",
					boolean.class,
					true,
					"<boolean>true</boolean>",
					"<boolean>true</boolean>",
					"<boolean>true</boolean>"
				)
				{
					@Override
					public void verify(Boolean o) {
						assertInstanceOf(Boolean.class, o);
					}
				}
			},
			{	/* 2 */
				new Input<Integer>(
					"SimpleTypes-3",
					int.class,
					123,
					"<number>123</number>",
					"<number>123</number>",
					"<number>123</number>"
				)
				{
					@Override
					public void verify(Integer o) {
						assertInstanceOf(Integer.class, o);
					}
				}
			},
			{	/* 3 */
				new Input<Float>(
					"SimpleTypes-4",
					float.class,
					1.23f,
					"<number>1.23</number>",
					"<number>1.23</number>",
					"<number>1.23</number>"
				)
				{
					@Override
					public void verify(Float o) {
						assertInstanceOf(Float.class, o);
					}
				}
			},
			{	/* 4 */
				new Input<String>(
					"SimpleTypes-5",
					String.class,
					null,
					"<null/>",
					"<null/>",
					"<null/>"
				)
			},
			{	/* 5 */
				new Input<String[]>(
					"Arrays-1",
					String[].class,
					new String[]{"foo"},
					"<ul><li>foo</li></ul>",
					"<ul>\n\t<li>foo</li>\n</ul>\n",
					"<ul><li>foo</li></ul>"
				)
				{
					@Override
					public void verify(String[] o) {
						assertInstanceOf(String.class, o[0]);
					}
				}
			},
			{	/* 6 */
				new Input<>(
					"Arrays-2",
					String[].class,
					new String[]{null},
					"<ul><li><null/></li></ul>",
					"<ul>\n\t<li><null/></li>\n</ul>\n",
					"<ul><li><null/></li></ul>"
				)
			},
			{	/* 7 */
				new Input<Object[]>(
					"Arrays-3",
					Object[].class,
					new Object[]{"foo",123,true},
					"<ul><li>foo</li><li><number>123</number></li><li><boolean>true</boolean></li></ul>",
					"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><boolean>true</boolean></li>\n</ul>\n",
					"<ul><li>foo</li><li><number>123</number></li><li><boolean>true</boolean></li></ul>"
				)
				{
					@Override
					public void verify(Object[] o) {
						assertInstanceOf(String.class, o[0]);
						assertInstanceOf(Integer.class, o[1]);
						assertInstanceOf(Boolean.class, o[2]);
					}
				}
			},
			{	/* 8 */
				new Input<int[]>(
					"Arrays-4",
					int[].class,
					new int[]{123},
					"<ul><li><number>123</number></li></ul>",
					"<ul>\n\t<li><number>123</number></li>\n</ul>\n",
					"<ul><li>123</li></ul>"
				)
				{
					@Override
					public void verify(int[] o) {
						assertInstanceOf(int[].class, o);
					}
				}
			},
			{	/* 9 */
				new Input<boolean[]>(
					"Arrays-5",
					boolean[].class,
					new boolean[]{true},
					"<ul><li><boolean>true</boolean></li></ul>",
					"<ul>\n\t<li><boolean>true</boolean></li>\n</ul>\n",
					"<ul><li>true</li></ul>"
				)
				{
					@Override
					public void verify(boolean[] o) {
						assertInstanceOf(boolean[].class, o);
					}
				}
			},
			{	/* 10 */
				new Input<String[][]>(
					"Arrays-6",
					String[][].class,
					new String[][]{{"foo"}},
					"<ul><li><ul><li>foo</li></ul></li></ul>",
					"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>foo</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
					"<ul><li><ul><li>foo</li></ul></li></ul>"
				)
				{
					@Override
					public void verify(String[][] o) {
						assertInstanceOf(String[][].class, o);
					}
				}
			},
			{	/* 11 */
				new Input<Map<String,String>>(
					"MapWithStrings",
					MapWithStrings.class,
					new MapWithStrings().append("k1", "v1").append("k2", null),
					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td>v1</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>k1</td>\n"
							+"\t\t<td>v1</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k2</td>\n"
							+"\t\t<td><null/></td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td>v1</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(Map<String,String> o) {
						assertInstanceOf(String.class, o.get("k1"));
					}
				}
			},
			{	/* 12 */
				new Input<Map<String,Number>>(
					"MapsWithNumbers",
					MapWithNumbers.class,
					new MapWithNumbers().append("k1", 123).append("k2", 1.23).append("k3", null),
					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td><number>123</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td><number>1.23</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k3</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>k1</td>\n"
							+"\t\t<td><number>123</number></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k2</td>\n"
							+"\t\t<td><number>1.23</number></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k3</td>\n"
							+"\t\t<td><null/></td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td>123</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td>1.23</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k3</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(Map<String,Number> o) {
						assertInstanceOf(Number.class, o.get("k1"));
					}
				}
			},
			{	/* 13 */
				new Input<Map<String,Object>>(
					"MapWithObjects",
					getType(Map.class,String.class,Object.class),
					new MapWithObjects().append("k1", "v1").append("k2", 123).append("k3", 1.23).append("k4", true).append("k5", null),
					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td>v1</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td><number>123</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k3</td>"
							+"<td><number>1.23</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k4</td>"
							+"<td><boolean>true</boolean></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k5</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>k1</td>\n"
							+"\t\t<td>v1</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k2</td>\n"
							+"\t\t<td><number>123</number></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k3</td>\n"
							+"\t\t<td><number>1.23</number></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k4</td>\n"
							+"\t\t<td><boolean>true</boolean></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>k5</td>\n"
							+"\t\t<td><null/></td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>k1</td>"
							+"<td>v1</td>"
						+"</tr>"
						+"<tr>"
							+"<td>k2</td>"
							+"<td><number>123</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k3</td>"
							+"<td><number>1.23</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k4</td>"
							+"<td><boolean>true</boolean></td>"
						+"</tr>"
						+"<tr>"
							+"<td>k5</td>"
							+"<td><null/></td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(Map<String,Object> o) {
						assertInstanceOf(String.class, o.get("k1"));
						assertInstanceOf(Integer.class, o.get("k2"));
						assertInstanceOf(Float.class, o.get("k3"));
						assertInstanceOf(Boolean.class, o.get("k4"));
					}
				}
			},
			{	/* 14 */
				new Input<List<String>>(
					"ListWithStrings",
					getType(List.class,String.class),
					new ListWithStrings().append("foo").append(null),
					"<ul><li>foo</li><li><null/></li></ul>",
					"<ul>\n\t<li>foo</li>\n\t<li><null/></li>\n</ul>\n",
					"<ul><li>foo</li><li><null/></li></ul>"
				)
				{
					@Override
					public void verify(List<String> o) {
						assertInstanceOf(String.class, o.get(0));
					}
				}
			},
			{	/* 15 */
				new Input<List<Number>>(
					"ListWithNumbers",
					ListWithNumbers.class,
					new ListWithNumbers().append(123).append(1.23).append(null),
					"<ul><li><number>123</number></li><li><number>1.23</number></li><li><null/></li></ul>",
					"<ul>\n\t<li><number>123</number></li>\n\t<li><number>1.23</number></li>\n\t<li><null/></li>\n</ul>\n",
					"<ul><li>123</li><li>1.23</li><li><null/></li></ul>"
				)
				{
					@Override
					public void verify(List<Number> o) {
						assertInstanceOf(Integer.class, o.get(0));
						assertInstanceOf(Float.class, o.get(1));
					}
				}
			},
			{	/* 16 */
				new Input<List<Object>>(
					"ListWithObjects",
					getType(List.class,Object.class),
					new ListWithObjects().append("foo").append(123).append(1.23).append(true).append(null),
					"<ul><li>foo</li><li><number>123</number></li><li><number>1.23</number></li><li><boolean>true</boolean></li><li><null/></li></ul>",
					"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><number>1.23</number></li>\n\t<li><boolean>true</boolean></li>\n\t<li><null/></li>\n</ul>\n",
					"<ul><li>foo</li><li><number>123</number></li><li><number>1.23</number></li><li><boolean>true</boolean></li><li><null/></li></ul>"
				)
				{
					@Override
					public void verify(List<Object> o) {
						assertInstanceOf(String.class, o.get(0));
						assertInstanceOf(Integer.class, o.get(1));
						assertInstanceOf(Float.class, o.get(2));
						assertInstanceOf(Boolean.class, o.get(3));
					}
				}
			},
			{	/* 17 */
				new Input<BeanWithNormalProperties>(
					"BeanWithNormalProperties",
					BeanWithNormalProperties.class,
					new BeanWithNormalProperties().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>foo</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>123</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>bar</td>"
						+"</tr>"
						+"<tr>"
							+"<td>d</td>"
							+"<td><number>456</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>e</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>h</td>"
										+"<td>qux</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>f</td>"
							+"<td>"
								+"<ul>"
									+"<li>baz</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>g</td>"
							+"<td>"
								+"<ul>"
									+"<li>789</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>foo</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b</td>\n"
							+"\t\t<td>123</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>c</td>\n"
							+"\t\t<td>bar</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>d</td>\n"
							+"\t\t<td><number>456</number></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>e</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>h</td>\n"
										+"\t\t\t\t\t<td>qux</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>f</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li>baz</li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>g</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li>789</li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>foo</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>123</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>bar</td>"
						+"</tr>"
						+"<tr>"
							+"<td>d</td>"
							+"<td><number>456</number></td>"
						+"</tr>"
						+"<tr>"
							+"<td>e</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>h</td>"
										+"<td>qux</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>f</td>"
							+"<td>"
								+"<ul>"
									+"<li>baz</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>g</td>"
							+"<td>"
								+"<ul>"
									+"<li>789</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithNormalProperties o) {
						assertInstanceOf(String.class, o.c);
						assertInstanceOf(Integer.class, o.d);
						assertInstanceOf(Bean1a.class, o.e);
					}
				}
			},
			{	/* 18 */
				new Input<BeanWithMapProperties>(
					"BeanWithMapProperties",
					BeanWithMapProperties.class,
					new BeanWithMapProperties().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>123</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k3</td>"
										+"<td>bar</td>"
									+"</tr>"
									+"<tr>"
										+"<td>k4</td>"
										+"<td><number>456</number></td>"
									+"</tr>"
									+"<tr>"
										+"<td>k5</td>"
										+"<td><boolean>true</boolean></td>"
									+"</tr>"
									+"<tr>"
										+"<td>k6</td>"
										+"<td><null/></td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k1</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k2</td>\n"
										+"\t\t\t\t\t<td>123</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>c</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k3</td>\n"
										+"\t\t\t\t\t<td>bar</td>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k4</td>\n"
										+"\t\t\t\t\t<td><number>456</number></td>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k5</td>\n"
										+"\t\t\t\t\t<td><boolean>true</boolean></td>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k6</td>\n"
										+"\t\t\t\t\t<td><null/></td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>123</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k3</td>"
										+"<td>bar</td>"
									+"</tr>"
									+"<tr>"
										+"<td>k4</td>"
										+"<td><number>456</number></td>"
									+"</tr>"
									+"<tr>"
										+"<td>k5</td>"
										+"<td><boolean>true</boolean></td>"
									+"</tr>"
									+"<tr>"
										+"<td>k6</td>"
										+"<td><null/></td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithMapProperties o) {
						assertInstanceOf(String.class, o.a.get("k1"));
						assertInstanceOf(Integer.class, o.b.get("k2"));
						assertInstanceOf(String.class, o.c.get("k3"));
						assertInstanceOf(Integer.class, o.c.get("k4"));
						assertInstanceOf(Boolean.class, o.c.get("k5"));
					}
				}
			},
			{	/* 19 */
				new Input<BeanWithTypeName>(
					"BeanWithTypeName",
					BeanWithTypeName.class,
					new BeanWithTypeName().init(),
					"<table _type='X'>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>123</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>foo</td>"
						+"</tr>"
					+"</table>",

					"<table _type='X'>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>123</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b</td>\n"
							+"\t\t<td>foo</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>123</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>foo</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithTypeName o) {
						assertInstanceOf(BeanWithTypeName.class, o);
					}
				}
			},
			{	/* 20 */
				new Input<BeanWithPropertiesWithTypeNames>(
					"BeanWithPropertiesWithTypeNames",
					BeanWithPropertiesWithTypeNames.class,
					new BeanWithPropertiesWithTypeNames().init(),
					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>b</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table _type='B'>"
									+"<tr>"
										+"<td>b</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>b1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>b</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='B'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>b</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>b</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table _type='B'>"
									+"<tr>"
										+"<td>b</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithPropertiesWithTypeNames o) {
						assertInstanceOf(B.class, o.b2);
					}
				}
			},
			{	/* 21 */
				new Input<BeanWithPropertiesWithArrayTypeNames>(
					"BeanWithPropertiesWithArrayTypeNames",
					BeanWithPropertiesWithArrayTypeNames.class,
					new BeanWithPropertiesWithArrayTypeNames().init(),
					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr _type='B'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b3</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr _type='B'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>b1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>b</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>b</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='B'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b3</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>b</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='B'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr _type='B'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b3</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>b</th>"
									+"</tr>"
									+"<tr _type='B'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithPropertiesWithArrayTypeNames o) {
						assertInstanceOf(B.class, o.b2[0]);
						assertInstanceOf(B.class, o.b3[0]);
					}
				}
			},
			{	/* 22 */
				new Input<BeanWithPropertiesWith2dArrayTypeNames>(
					"BeanWithPropertiesWith2dArrayTypeNames",
					BeanWithPropertiesWith2dArrayTypeNames.class,
					new BeanWithPropertiesWith2dArrayTypeNames().init(),
					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr _type='B'>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b3</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr _type='B'>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>b1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li>\n"
										+"\t\t\t\t\t<table _type='array'>\n"
											+"\t\t\t\t\t\t<tr>\n"
												+"\t\t\t\t\t\t\t<th>b</th>\n"
											+"\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t<tr>\n"
												+"\t\t\t\t\t\t\t<td>foo</td>\n"
											+"\t\t\t\t\t\t</tr>\n"
										+"\t\t\t\t\t</table>\n"
									+"\t\t\t\t</li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li>\n"
										+"\t\t\t\t\t<table _type='array'>\n"
											+"\t\t\t\t\t\t<tr>\n"
												+"\t\t\t\t\t\t\t<th>b</th>\n"
											+"\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t<tr _type='B'>\n"
												+"\t\t\t\t\t\t\t<td>foo</td>\n"
											+"\t\t\t\t\t\t</tr>\n"
										+"\t\t\t\t\t</table>\n"
									+"\t\t\t\t</li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b3</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li>\n"
										+"\t\t\t\t\t<table _type='array'>\n"
											+"\t\t\t\t\t\t<tr>\n"
												+"\t\t\t\t\t\t\t<th>b</th>\n"
											+"\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t<tr _type='B'>\n"
												+"\t\t\t\t\t\t\t<td>foo</td>\n"
											+"\t\t\t\t\t\t</tr>\n"
										+"\t\t\t\t\t</table>\n"
									+"\t\t\t\t</li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr _type='B'>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b3</td>"
							+"<td>"
								+"<ul>"
									+"<li>"
										+"<table _type='array'>"
											+"<tr>"
												+"<th>b</th>"
											+"</tr>"
											+"<tr _type='B'>"
												+"<td>foo</td>"
											+"</tr>"
										+"</table>"
									+"</li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithPropertiesWith2dArrayTypeNames o) {
						assertInstanceOf(B.class, o.b2[0][0]);
						assertInstanceOf(B.class, o.b3[0][0]);
					}
				}
			},
			{	/* 23 */
				new Input<BeanWithPropertiesWithMapTypeNames>(
					"BeanWithPropertiesWithMapTypeNames",
					BeanWithPropertiesWithMapTypeNames.class,
					new BeanWithPropertiesWithMapTypeNames().init(),
					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>"
											+"<table>"
												+"<tr>"
													+"<td>b</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>"
											+"<table _type='B'>"
												+"<tr>"
													+"<td>b</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>b1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k1</td>\n"
										+"\t\t\t\t\t<td>\n"
											+"\t\t\t\t\t\t<table>\n"
												+"\t\t\t\t\t\t\t<tr>\n"
													+"\t\t\t\t\t\t\t\t<td>b</td>\n"
													+"\t\t\t\t\t\t\t\t<td>foo</td>\n"
												+"\t\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t</table>\n"
										+"\t\t\t\t\t</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k2</td>\n"
										+"\t\t\t\t\t<td>\n"
											+"\t\t\t\t\t\t<table _type='B'>\n"
												+"\t\t\t\t\t\t\t<tr>\n"
													+"\t\t\t\t\t\t\t\t<td>b</td>\n"
													+"\t\t\t\t\t\t\t\t<td>foo</td>\n"
												+"\t\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t</table>\n"
										+"\t\t\t\t\t</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>b1</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>"
											+"<table>"
												+"<tr>"
													+"<td>b</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b2</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>"
											+"<table _type='B'>"
												+"<tr>"
													+"<td>b</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithPropertiesWithMapTypeNames o) {
						assertInstanceOf(B.class, o.b1.get("k1"));
						assertInstanceOf(B.class, o.b2.get("k2"));
					}
				}
			},
			{	/* 24 */
				new Input<LinkBean>(
					"LinkBean-1",
					LinkBean.class,
					new LinkBean().init(),
					"<a href='http://apache.org'>foo</a>",
					"<a href='http://apache.org'>foo</a>",
					"<a href='http://apache.org'>foo</a>"
				)
				{
					@Override
					public void verify(LinkBean o) {
						assertInstanceOf(LinkBean.class, o);
					}
				}
			},
			{	/* 25 */
				new Input<LinkBean[]>(
					"LinkBean-2",
					LinkBean[].class,
					new LinkBean[]{new LinkBean().init(),new LinkBean().init()},
					"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
					"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
					"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>"
				)
				{
					@Override
					public void verify(LinkBean[] o) {
						assertInstanceOf(LinkBean.class, o[0]);
					}
				}
			},
			{	/* 26 */
				new Input<List<LinkBean>>(
					"ListWithLinkBeans",
					ListWithLinkBeans.class,
					new ListWithLinkBeans().append(new LinkBean().init()).append(new LinkBean().init()),
					"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
					"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
					"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>"
				)
				{
					@Override
					public void verify(List<LinkBean> o) {
						assertInstanceOf(LinkBean.class, o.get(0));
					}
				}
			},
			{	/* 27 */
				new Input<BeanWithLinkBeanProperties>(
					"BeanWithLinkBeanProperties",
					BeanWithLinkBeanProperties.class,
					new BeanWithLinkBeanProperties().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td><a href='http://apache.org'>foo</a></td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<ul>"
									+"<li><a href='http://apache.org'>foo</a></li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>c1</td>"
										+"<td><a href='http://apache.org'>foo</a></td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td><a href='http://apache.org'>foo</a></td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<ul>\n"
									+"\t\t\t\t<li><a href='http://apache.org'>foo</a></li>\n"
								+"\t\t\t</ul>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>c</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>c1</td>\n"
										+"\t\t\t\t\t<td><a href='http://apache.org'>foo</a></td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td><a href='http://apache.org'>foo</a></td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<ul>"
									+"<li><a href='http://apache.org'>foo</a></li>"
								+"</ul>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>c1</td>"
										+"<td><a href='http://apache.org'>foo</a></td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithLinkBeanProperties o) {
						assertInstanceOf(LinkBean.class, o.a);
						assertInstanceOf(LinkBean.class, o.b.get(0));
						assertInstanceOf(LinkBean.class, o.c.get("c1"));
					}
				}
			},
			{	/* 28 */
				new Input<BeanWithSpecialCharacters>(
					"BeanWithSpecialCharacters",
					BeanWithSpecialCharacters.class,
					new BeanWithSpecialCharacters().init(),
					"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
					"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>\n\t</tr>\n</table>\n",
					"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>"
				)
				{
					@Override
					public void verify(BeanWithSpecialCharacters o) {
						assertInstanceOf(BeanWithSpecialCharacters.class, o);
					}
				}
			},
			{	/* 29 */
				new Input<BeanWithSpecialCharacters>(
					"BeanWithSpecialCharacters-2",
					BeanWithSpecialCharacters.class,
					new BeanWithSpecialCharacters().init(),
					"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",

					"<table>\n"
					+"	<tr>\n"
					+"		<td>a</td>\n"
					+"		<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>\n"
					+"	</tr>\n"
					+"</table>\n",

					"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>"
				)
				{
					@Override
					public void verify(BeanWithSpecialCharacters o) {
						assertInstanceOf(BeanWithSpecialCharacters.class, o);
					}
				}
			},
			{	/* 30 */
				new Input<BeanWithNullProperties>(
					"BeanWithNullProperties",
					BeanWithNullProperties.class,
					new BeanWithNullProperties(),
					"<table></table>",
					"<table>\n</table>\n",
					"<table></table>"
				)
				{
					@Override
					public void verify(BeanWithNullProperties o) {
						assertInstanceOf(BeanWithNullProperties.class, o);
					}
				}
			},
			{	/* 31 */
				new Input<BeanWithAbstractFields>(
					"BeanWithAbstractFields",
					BeanWithAbstractFields.class,
					new BeanWithAbstractFields().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>a</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>ia</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='A'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>a</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>aa</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='A'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>a</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>o</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='A'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>a</td>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o</td>"
							+"<td>"
								+"<table _type='A'>"
									+"<tr>"
										+"<td>a</td>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithAbstractFields o) {
						assertInstanceOf(A.class, o.a);
						assertInstanceOf(A.class, o.ia);
						assertInstanceOf(A.class, o.aa);
						assertInstanceOf(A.class, o.o);
					}
				}
			},
			{	/* 32 */
				new Input<BeanWithAbstractArrayFields>(
					"BeanWithAbstractArrayFields",
					BeanWithAbstractArrayFields.class,
					new BeanWithAbstractArrayFields().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>ia1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>ia2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>aa1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>aa2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>o1</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>o2</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table _type='array'>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<th>a</th>\n"
									+"\t\t\t\t</tr>\n"
									+"\t\t\t\t<tr _type='A'>\n"
										+"\t\t\t\t\t<td>foo</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>ia2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>aa2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o1</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>o2</td>"
							+"<td>"
								+"<table _type='array'>"
									+"<tr>"
										+"<th>a</th>"
									+"</tr>"
									+"<tr _type='A'>"
										+"<td>foo</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithAbstractArrayFields o) {
						assertInstanceOf(A.class, o.a[0]);
						assertInstanceOf(A.class, o.ia1[0]);
						assertInstanceOf(A.class, o.ia2[0]);
						assertInstanceOf(A.class, o.aa1[0]);
						assertInstanceOf(A.class, o.aa2[0]);
						assertInstanceOf(A.class, o.o1[0]);
						assertInstanceOf(A.class, o.o2[0]);
					}
				}
			},
			{	/* 33 */
				new Input<BeanWithAbstractMapFields>(
					"BeanWithAbstractMapFields",
					BeanWithAbstractMapFields.class,
					new BeanWithAbstractMapFields().init(),
					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>"
											+"<table>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>"
											+"<table _type='A'>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k3</td>"
										+"<td>"
											+"<table _type='A'>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>",

					"<table>\n"
						+"\t<tr>\n"
							+"\t\t<td>a</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k1</td>\n"
										+"\t\t\t\t\t<td>\n"
											+"\t\t\t\t\t\t<table>\n"
												+"\t\t\t\t\t\t\t<tr>\n"
													+"\t\t\t\t\t\t\t\t<td>a</td>\n"
													+"\t\t\t\t\t\t\t\t<td>foo</td>\n"
												+"\t\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t</table>\n"
										+"\t\t\t\t\t</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>b</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k2</td>\n"
										+"\t\t\t\t\t<td>\n"
											+"\t\t\t\t\t\t<table _type='A'>\n"
												+"\t\t\t\t\t\t\t<tr>\n"
													+"\t\t\t\t\t\t\t\t<td>a</td>\n"
													+"\t\t\t\t\t\t\t\t<td>foo</td>\n"
												+"\t\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t</table>\n"
										+"\t\t\t\t\t</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
						+"\t<tr>\n"
							+"\t\t<td>c</td>\n"
							+"\t\t<td>\n"
								+"\t\t\t<table>\n"
									+"\t\t\t\t<tr>\n"
										+"\t\t\t\t\t<td>k3</td>\n"
										+"\t\t\t\t\t<td>\n"
											+"\t\t\t\t\t\t<table _type='A'>\n"
												+"\t\t\t\t\t\t\t<tr>\n"
													+"\t\t\t\t\t\t\t\t<td>a</td>\n"
													+"\t\t\t\t\t\t\t\t<td>foo</td>\n"
												+"\t\t\t\t\t\t\t</tr>\n"
											+"\t\t\t\t\t\t</table>\n"
										+"\t\t\t\t\t</td>\n"
									+"\t\t\t\t</tr>\n"
								+"\t\t\t</table>\n"
							+"\t\t</td>\n"
						+"\t</tr>\n"
					+"</table>\n",

					"<table>"
						+"<tr>"
							+"<td>a</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k1</td>"
										+"<td>"
											+"<table>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>b</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k2</td>"
										+"<td>"
											+"<table _type='A'>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
						+"<tr>"
							+"<td>c</td>"
							+"<td>"
								+"<table>"
									+"<tr>"
										+"<td>k3</td>"
										+"<td>"
											+"<table _type='A'>"
												+"<tr>"
													+"<td>a</td>"
													+"<td>foo</td>"
												+"</tr>"
											+"</table>"
										+"</td>"
									+"</tr>"
								+"</table>"
							+"</td>"
						+"</tr>"
					+"</table>"
				)
				{
					@Override
					public void verify(BeanWithAbstractMapFields o) {
						assertInstanceOf(A.class, o.a.get("k1"));
						assertInstanceOf(A.class, o.b.get("k2"));
						assertInstanceOf(A.class, o.c.get("k3"));
					}
				}
			},
			{	/* 34 */
				new Input<BeanWithWhitespaceTextFields>(
					"BeanWithWhitespaceTextFields-1",
					BeanWithWhitespaceTextFields.class,
					new BeanWithWhitespaceTextFields().init(null),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextFields o) {
						assertInstanceOf(BeanWithWhitespaceTextFields.class, o);
					}
				}
			},
			{	/* 35 */
				new Input<BeanWithWhitespaceTextFields>(
					"BeanWithWhitespaceTextFields-2",
					BeanWithWhitespaceTextFields.class,
					new BeanWithWhitespaceTextFields().init(""),
					"<object><sp/></object>",
					"<object><sp/></object>\n",
					"<object><sp/></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextFields o) {
						assertInstanceOf(BeanWithWhitespaceTextFields.class, o);
					}
				}
			},
			{	/* 36 */
				new Input<BeanWithWhitespaceTextFields>(
					"BeanWithWhitespaceTextFields-3",
					BeanWithWhitespaceTextFields.class,
					new BeanWithWhitespaceTextFields().init(" "),
					"<object><sp> </sp></object>",
					"<object><sp> </sp></object>\n",
					"<object><sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextFields o) {
						assertInstanceOf(BeanWithWhitespaceTextFields.class, o);
					}
				}
			},
			{	/* 37 */
				new Input<BeanWithWhitespaceTextFields>(
					"BeanWithWhitespaceTextFields-4",
					BeanWithWhitespaceTextFields.class,
					new BeanWithWhitespaceTextFields().init("  "),
					"<object><sp> </sp><sp> </sp></object>",
					"<object><sp> </sp><sp> </sp></object>\n",
					"<object><sp> </sp><sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextFields o) {
						assertInstanceOf(BeanWithWhitespaceTextFields.class, o);
					}
				}
			},
			{	/* 38 */
				new Input<BeanWithWhitespaceTextFields>(
					"BeanWithWhitespaceTextFields-5",
					BeanWithWhitespaceTextFields.class,
					new BeanWithWhitespaceTextFields().init("  foobar  "),
					"<object><sp> </sp> foobar <sp> </sp></object>",
					"<object><sp> </sp> foobar <sp> </sp></object>\n",
					"<object><sp> </sp> foobar <sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextFields o) {
						assertInstanceOf(BeanWithWhitespaceTextFields.class, o);
					}
				}
			},
			{	/* 39 */
				new Input<BeanWithWhitespaceTextPwsFields>(
					"BeanWithWhitespaceTextPwsFields-1",
					BeanWithWhitespaceTextPwsFields.class,
					new BeanWithWhitespaceTextPwsFields().init(null),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, o);
					}
				}
			},
			{	/* 40 */
				new Input<BeanWithWhitespaceTextPwsFields>(
					"BeanWithWhitespaceTextPwsFields-2",
					BeanWithWhitespaceTextPwsFields.class,
					new BeanWithWhitespaceTextPwsFields().init(""),
					"<object><sp/></object>",
					"<object><sp/></object>\n",
					"<object><sp/></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, o);
					}
				}
			},
			{	/* 41 */
				new Input<BeanWithWhitespaceTextPwsFields>(
					"BeanWithWhitespaceTextPwsFields-3",
					BeanWithWhitespaceTextPwsFields.class,
					new BeanWithWhitespaceTextPwsFields().init(" "),
					"<object> </object>",
					"<object> </object>\n",
					"<object> </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, o);
					}
				}
			},
			{	/* 42 */
				new Input<BeanWithWhitespaceTextPwsFields>(
					"BeanWithWhitespaceTextPwsFields-4",
					BeanWithWhitespaceTextPwsFields.class,
					new BeanWithWhitespaceTextPwsFields().init("  "),
					"<object>  </object>",
					"<object>  </object>\n",
					"<object>  </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, o);
					}
				}
			},
			{	/* 43 */
				new Input<BeanWithWhitespaceTextPwsFields>(
					"BeanWithWhitespaceTextPwsFields-5",
					BeanWithWhitespaceTextPwsFields.class,
					new BeanWithWhitespaceTextPwsFields().init("  foobar  "),
					"<object>  foobar  </object>",
					"<object>  foobar  </object>\n",
					"<object>  foobar  </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceTextPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, o);
					}
				}
			},
			{	/* 44 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-1",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(null),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 45 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-2",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(new String[0]),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 46 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-3",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(new String[]{""}),
					"<object><sp/></object>",
					"<object><sp/></object>\n",
					"<object><sp/></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 47 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-4",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(new String[]{" "}),
					"<object><sp> </sp></object>",
					"<object><sp> </sp></object>\n",
					"<object><sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 48 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-5",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(new String[]{"  "}),
					"<object><sp> </sp><sp> </sp></object>",
					"<object><sp> </sp><sp> </sp></object>\n",
					"<object><sp> </sp><sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 49 */
				new Input<BeanWithWhitespaceMixedFields>(
					"BeanWithWhitespaceMixedFields-6",
					BeanWithWhitespaceMixedFields.class,
					new BeanWithWhitespaceMixedFields().init(new String[]{"  foobar  "}),
					"<object><sp> </sp> foobar <sp> </sp></object>",
					"<object><sp> </sp> foobar <sp> </sp></object>\n",
					"<object><sp> </sp> foobar <sp> </sp></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedFields.class, o);
					}
				}
			},
			{	/* 50 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-1",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(null),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
			{	/* 51 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-2",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(new String[0]),
					"<object></object>",
					"<object></object>\n",
					"<object></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
			{	/* 52 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-3",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(new String[]{""}),
					"<object><sp/></object>",
					"<object><sp/></object>\n",
					"<object><sp/></object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
			{	/* 53 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-4",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(new String[]{" "}),
					"<object> </object>",
					"<object> </object>\n",
					"<object> </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
			{	/* 54 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-5",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  "}),
					"<object>  </object>",
					"<object>  </object>\n",
					"<object>  </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
			{	/* 55 */
				new Input<BeanWithWhitespaceMixedPwsFields>(
					"BeanWithWhitespaceMixedPwsFields-6",
					BeanWithWhitespaceMixedPwsFields.class,
					new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  foobar  "}),
					"<object>  foobar  </object>",
					"<object>  foobar  </object>\n",
					"<object>  foobar  </object>"
				)
				{
					@Override
					public void verify(BeanWithWhitespaceMixedPwsFields o) {
						assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, o);
					}
				}
			},
		});
	}

	private Input input;

	public BasicHtmlTest(Input input) throws Exception {
		this.input = input;
	}

	public static class Input<T> {
		private final String label, e1, e2, e3;
		private final Type type;
		private final Object in;

		public Input(String label, Type type, T in, String e1, String e2, String e3) {
			this.label = label;
			this.type = type;
			this.in = in;
			this.e1 = e1;
			this.e2 = e2;
			this.e3 = e3;
		}

		public void verify(T o) {}
	}

	@Test
	public void a1_serializeNormal() {
		try {
			String r = s1.serialize(input.in);
			assertEquals(input.label + " serialize-normal failed", input.e1, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void a2_parseNormal() {
		try {
			String r = s1.serialize(input.in);
			Object o = parser.parse(r, input.type);
			r = s1.serialize(o);
			assertEquals(input.label + " parse-normal failed", input.e1, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void a3_verifyNormal() {
		try {
			String r = s1.serialize(input.in);
			Object o = parser.parse(r, input.type);
			input.verify(o);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void b1_serializeReadable() {
		try {
			String r = s2.serialize(input.in);
			assertEquals(input.label + " serialize-readable failed", input.e2, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void b2_parseReadable() {
		try {
			String r = s2.serialize(input.in);
			Object o = parser.parse(r, input.type);
			r = s2.serialize(o);
			assertEquals(input.label + " parse-readable failed", input.e2, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void b3_verifyReadable() {
		try {
			String r = s2.serialize(input.in);
			Object o = parser.parse(r, input.type);
			input.verify(o);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void c1_serializeAbridged() {
		try {
			String r = s3.serialize(input.in);
			assertEquals(input.label + " serialize-addRootType failed", input.e3, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void c2_parseAbridged() {
		try {
			String r = s3.serialize(input.in);
			Object o = parser.parse(r, input.type);
			r = s3.serialize(o);
			assertEquals(input.label + " parse-addRootType failed", input.e3, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@Test
	public void c3_verifyAbridged() {
		try {
			String r = s3.serialize(input.in);
			Object o = parser.parse(r, input.type);
			input.verify(o);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
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
		@BeanProperty(type=MapWithStrings.class)
		public Map<String,String> a;
		@BeanProperty(type=MapWithNumbers.class)
		public Map<String,Number> b;
		@BeanProperty(type=MapWithObjects.class)
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

	@Bean(beanDictionary={B.class})
	public static class BeanWithPropertiesWithTypeNames {
		public B b1;
		public Object b2;

		BeanWithPropertiesWithTypeNames init() {
			b1 = new B().init();
			b2 = new B().init();
			return this;
		}
	}

	@Bean(beanDictionary={B.class})
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

	@Bean(beanDictionary={B.class})
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

	@Bean(beanDictionary={B.class})
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

	@HtmlLink(nameProperty="a",uriProperty="b")
	public static class LinkBean {
		public String a;
		public String b;

		LinkBean init() {
			a = "foo";
			b = "http://apache.org";
			return this;
		}
	}

	public static class ListWithLinkBeans extends ArrayList<LinkBean> {
		public ListWithLinkBeans append(LinkBean value) {
			this.add(value);
			return this;
		}
	}

	public static class BeanWithLinkBeanProperties {
		public LinkBean a;
		public List<LinkBean> b;
		public Map<String,LinkBean> c;

		BeanWithLinkBeanProperties init() {
			a = new LinkBean().init();
			b = new ListWithLinkBeans().append(new LinkBean().init());
			c = new LinkedHashMap<>();
			c.put("c1", new LinkBean().init());
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

		@BeanProperty(name="  \b\f\n\t\r  ")
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

	@Bean(beanDictionary={A.class},properties="a,ia,aa,o")
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

	@Bean(beanDictionary={A.class},properties="a,ia1,ia2,aa1,aa2,o1,o2")
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

	@Bean(beanDictionary={A.class})
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

	@Html(format=XML)
	public static class BeanWithWhitespaceTextFields {
		@Xml(format=XmlFormat.TEXT)
		public String a;

		public BeanWithWhitespaceTextFields init(String s) {
			a = s;
			return this;
		}
	}

	@Html(format=XML)
	public static class BeanWithWhitespaceTextPwsFields {
		@Xml(format=XmlFormat.TEXT_PWS)
		public String a;

		public BeanWithWhitespaceTextPwsFields init(String s) {
			a = s;
			return this;
		}
	}

	@Html(format=XML)
	public static class BeanWithWhitespaceMixedFields {
		@Xml(format=XmlFormat.MIXED)
		public String[] a;

		public BeanWithWhitespaceMixedFields init(String[] s) {
			a = s;
			return this;
		}
	}

	@Html(format=XML)
	public static class BeanWithWhitespaceMixedPwsFields {
		@Xml(format=XmlFormat.MIXED_PWS)
		public String[] a;

		public BeanWithWhitespaceMixedPwsFields init(String[] s) {
			a = s;
			return this;
		}
	}
}
