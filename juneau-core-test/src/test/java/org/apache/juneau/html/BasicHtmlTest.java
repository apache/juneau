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

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc","serial"})
public class BasicHtmlTest {

	private static final HtmlSerializer
		s1 = HtmlSerializer.DEFAULT_SQ,
		s2 = HtmlSerializer.DEFAULT_SQ_READABLE;
	private static final HtmlParser parser = HtmlParser.DEFAULT;

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			{
				"SimpleTypes-1",
				"foo",
				"<string>foo</string>",
				"<string>foo</string>",
			},
			{
				"SimpleTypes-2",
				true,
				"<boolean>true</boolean>",
				"<boolean>true</boolean>",
			},
			{
				"SimpleTypes-3",
				123,
				"<number>123</number>",
				"<number>123</number>",
			},
			{
				"SimpleTypes-4",
				1.23f,
				"<number>1.23</number>",
				"<number>1.23</number>",
			},
			{
				"SimpleTypes-5",
				null,
				"<null/>",
				"<null/>",
			},
			{
				"Arrays-1",
				new String[]{"foo"},
				"<ul><li>foo</li></ul>",
				"<ul>\n\t<li>foo</li>\n</ul>\n",
			},
			{
				"Arrays-2",
				new String[]{null},
				"<ul><li><null/></li></ul>",
				"<ul>\n\t<li><null/></li>\n</ul>\n",
			},
			{
				"Arrays-3",
				new Object[]{"foo",123,true},
				"<ul><li>foo</li><li><number>123</number></li><li><boolean>true</boolean></li></ul>",
				"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><boolean>true</boolean></li>\n</ul>\n",
			},
			{
				"Arrays-4",
				new int[]{123},
				"<ul><li>123</li></ul>",
				"<ul>\n\t<li>123</li>\n</ul>\n",
			},
			{
				"Arrays-5",
				new boolean[]{true},
				"<ul><li>true</li></ul>",
				"<ul>\n\t<li>true</li>\n</ul>\n",
			},
			{
				"Arrays-6",
				new String[][]{{"foo"}},
				"<ul><li><ul><li>foo</li></ul></li></ul>",
				"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>foo</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
			},
			{
				"MapWithStrings",
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
			},
			{
				"MapsWithNumbers",
				new MapWithNumbers().append("k1", 123).append("k2", 1.23).append("k3", null),
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
				+"</table>",
				"<table>\n"
					+"\t<tr>\n"
						+"\t\t<td>k1</td>\n"
						+"\t\t<td>123</td>\n"
					+"\t</tr>\n"
					+"\t<tr>\n"
						+"\t\t<td>k2</td>\n"
						+"\t\t<td>1.23</td>\n"
					+"\t</tr>\n"
					+"\t<tr>\n"
						+"\t\t<td>k3</td>\n"
						+"\t\t<td><null/></td>\n"
					+"\t</tr>\n"
				+"</table>\n",
			},
			{
				"MapWithObjects",
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
			},
			{
				"ListWithStrings",
				new ListWithStrings().append("foo").append(null),
				"<ul><li>foo</li><li><null/></li></ul>",
				"<ul>\n\t<li>foo</li>\n\t<li><null/></li>\n</ul>\n",
			},
			{
				"ListWithNumbers",
				new ListWithNumbers().append(123).append(1.23).append(null),
				"<ul><li>123</li><li>1.23</li><li><null/></li></ul>",
				"<ul>\n\t<li>123</li>\n\t<li>1.23</li>\n\t<li><null/></li>\n</ul>\n",
			},
			{
				"ListWithObjects",
				new ListWithObjects().append("foo").append(123).append(1.23).append(true).append(null),
				"<ul><li>foo</li><li><number>123</number></li><li><number>1.23</number></li><li><boolean>true</boolean></li><li><null/></li></ul>",
				"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><number>1.23</number></li>\n\t<li><boolean>true</boolean></li>\n\t<li><null/></li>\n</ul>\n",
			},
			{
				"BeanWithNormalProperties",
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
			},
			{
				"BeanWithMapProperties",
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
				+"</table>\n"
		   },
			{
				"BeanWithTypeName",
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
			},
			{
				"BeanWithPropertiesWithTypeNames",
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
				+"</table>\n"
			},
			{
				"BeanWithPropertiesWithArrayTypeNames",
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
							+"<table _type='B^'>"
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
							+"\t\t\t<table _type='B^'>\n"
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
			},
			{
				"BeanWithPropertiesWithArrayTypeNames",
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
								+"<ul _type='B^^'>"
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
							+"\t\t\t<ul _type='B^^'>\n"
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
				+"</table>\n"
			},
			{
				"BeanWithPropertiesWithMapTypeNames",
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
			},
			{
				"LinkBean-1",
				new LinkBean().init(),
				"<a href='http://apache.org'>foo</a>",
				"<a href='http://apache.org'>foo</a>"
			},
			{
				"LinkBean-2",
				new LinkBean[]{new LinkBean().init(),new LinkBean().init()},
				"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
				"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n"
			},
			{
				"ListWithLinkBeans",
				new ListWithLinkBeans().append(new LinkBean().init()).append(new LinkBean().init()),
				"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
				"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n"
			},
			{
				"BeanWithLinkBeanProperties",
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
				+"</table>\n"
			},
			{
				"BeanWithSpecialCharacters",
				new BeanWithSpecialCharacters().init(),
				"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
				"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>\n\t</tr>\n</table>\n"
			},
			{
				"BeanWithSpecialCharacters2",
				new BeanWithSpecialCharacters().init(),
				"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
				"<table>\n"
				+"	<tr>\n"
				+"		<td>a</td>\n"
				+"		<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>\n"
				+"	</tr>\n"
				+"</table>\n"
			},
			{
				"BeanWithNullProperties",
				new BeanWithNullProperties(),
				"<table></table>",
				"<table>\n</table>\n"
			},
			{
				"BeanWithAbstractFields",
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
				+"</table>\n"
			},
			{
				"BeanWithAbstractArrayFields",
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
							+"<table _type='A^'>"
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
							+"<table _type='A^'>"
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
							+"<table _type='A^'>"
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
							+"\t\t\t<table _type='A^'>\n"
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
							+"\t\t\t<table _type='A^'>\n"
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
							+"\t\t\t<table _type='A^'>\n"
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
			},
			{
				"BeanWithAbstractMapFields",
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
				+"</table>\n"
			},
			{
				"BeanWithWhitespaceTextFields-1",
				new BeanWithWhitespaceTextFields().init(null),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceTextFields-2",
				new BeanWithWhitespaceTextFields().init(""),
				"<object><sp/></object>",
				"<object><sp/></object>\n",
			},
			{
				"BeanWithWhitespaceTextFields-3",
				new BeanWithWhitespaceTextFields().init(" "),
				"<object><sp> </sp></object>",
				"<object><sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceTextFields-4",
				new BeanWithWhitespaceTextFields().init("  "),
				"<object><sp> </sp><sp> </sp></object>",
				"<object><sp> </sp><sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceTextFields-5",
				new BeanWithWhitespaceTextFields().init("  foobar  "),
				"<object><sp> </sp> foobar <sp> </sp></object>",
				"<object><sp> </sp> foobar <sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceTextPwsFields-1",
				new BeanWithWhitespaceTextPwsFields().init(null),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceTextPwsFields-2",
				new BeanWithWhitespaceTextPwsFields().init(""),
				"<object><sp/></object>",
				"<object><sp/></object>\n",
			},
			{
				"BeanWithWhitespaceTextPwsFields-3",
				new BeanWithWhitespaceTextPwsFields().init(" "),
				"<object> </object>",
				"<object> </object>\n",
			},
			{
				"BeanWithWhitespaceTextPwsFields-4",
				new BeanWithWhitespaceTextPwsFields().init("  "),
				"<object>  </object>",
				"<object>  </object>\n",
			},
			{
				"BeanWithWhitespaceTextPwsFields-5",
				new BeanWithWhitespaceTextPwsFields().init("  foobar  "),
				"<object>  foobar  </object>",
				"<object>  foobar  </object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-1",
				new BeanWithWhitespaceMixedFields().init(null),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-2",
				new BeanWithWhitespaceMixedFields().init(new String[0]),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-3",
				new BeanWithWhitespaceMixedFields().init(new String[]{""}),
				"<object><sp/></object>",
				"<object><sp/></object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-4",
				new BeanWithWhitespaceMixedFields().init(new String[]{" "}),
				"<object><sp> </sp></object>",
				"<object><sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-5",
				new BeanWithWhitespaceMixedFields().init(new String[]{"  "}),
				"<object><sp> </sp><sp> </sp></object>",
				"<object><sp> </sp><sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceMixedFields-6",
				new BeanWithWhitespaceMixedFields().init(new String[]{"  foobar  "}),
				"<object><sp> </sp> foobar <sp> </sp></object>",
				"<object><sp> </sp> foobar <sp> </sp></object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-1",
				new BeanWithWhitespaceMixedPwsFields().init(null),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-2",
				new BeanWithWhitespaceMixedPwsFields().init(new String[0]),
				"<object></object>",
				"<object></object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-3",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{""}),
				"<object><sp/></object>",
				"<object><sp/></object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-4",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{" "}),
				"<object> </object>",
				"<object> </object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-5",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  "}),
				"<object>  </object>",
				"<object>  </object>\n",
			},
			{
				"BeanWithWhitespaceMixedPwsFields-6",
				new BeanWithWhitespaceMixedPwsFields().init(new String[]{"  foobar  "}),
				"<object>  foobar  </object>",
				"<object>  foobar  </object>\n",
			},
		});
	}

	private String label, e1, e2;
	private Object in;

	public BasicHtmlTest(String label, Object in, String e1, String e2) throws Exception {
		this.label = label;
		this.in = in;
		this.e1 = e1;
		this.e2 = e2;
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


	//--------------------------------------------------------------------------------
	// Test beans
	//--------------------------------------------------------------------------------

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
			b1 = new HashMap<String,B>();
			b1.put("k1", new B().init());
			b2 = new HashMap<String,Object>();
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

	@HtmlLink(nameProperty="a",hrefProperty="b")
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
			c = new LinkedHashMap<String,LinkBean>();
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

	@Bean(beanDictionary={A.class})
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

	@Bean(beanDictionary={A.class})
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
			a = new HashMap<String,A>();
			b = new HashMap<String,AA>();
			c = new HashMap<String,Object>();
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
	
	@Html(asXml=true)
	public static class BeanWithWhitespaceTextFields {
		@Xml(format=XmlFormat.TEXT)
		public String a;
	
		public BeanWithWhitespaceTextFields init(String s) {
			a = s;
			return this;
		}
	}
	
	@Html(asXml=true)
	public static class BeanWithWhitespaceTextPwsFields {
		@Xml(format=XmlFormat.TEXT_PWS)
		public String a;
	
		public BeanWithWhitespaceTextPwsFields init(String s) {
			a = s;
			return this;
		}
	}

	@Html(asXml=true)
	public static class BeanWithWhitespaceMixedFields {
		@Xml(format=XmlFormat.MIXED)
		public String[] a;
	
		public BeanWithWhitespaceMixedFields init(String[] s) {
			a = s;
			return this;
		}
	}

	@Html(asXml=true)
	public static class BeanWithWhitespaceMixedPwsFields {
		@Xml(format=XmlFormat.MIXED_PWS)
		public String[] a;
	
		public BeanWithWhitespaceMixedPwsFields init(String[] s) {
			a = s;
			return this;
		}
	}
}
