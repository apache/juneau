/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.html;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@SuppressWarnings({"serial","rawtypes"})
class BasicHtml_Test extends TestBase {

	private static final Class<?>[] ANNOTATED_CLASSES = {
		BeanWithWhitespaceTextFields2Config.class, BeanWithWhitespaceTextPwsFields2Config.class, BeanWithWhitespaceMixedFields2Config.class, BeanWithWhitespaceMixedPwsFields2Config.class, LinkBeanCConfig.class
	};
	private static final HtmlSerializer
		s1 = HtmlSerializer.DEFAULT_SQ.copy().addRootType().applyAnnotations(ANNOTATED_CLASSES).build(),
		s2 = HtmlSerializer.DEFAULT_SQ_READABLE.copy().addRootType().applyAnnotations(ANNOTATED_CLASSES).build(),
		s3 = HtmlSerializer.DEFAULT_SQ.copy().applyAnnotations(ANNOTATED_CLASSES).build();
	private static final HtmlParser parser = HtmlParser.DEFAULT.copy().applyAnnotations(ANNOTATED_CLASSES).build();

	private static Input[] INPUT = {
		input(
			"SimpleTypes-1",
			String.class,
			"foo",
			"<string>foo</string>",
			"<string>foo</string>",
			"<string>foo</string>",
			x -> assertInstanceOf(String.class, x)
		),
		input(
			"SimpleTypes-2",
			Boolean.class,
			true,
			"<boolean>true</boolean>",
			"<boolean>true</boolean>",
			"<boolean>true</boolean>",
			x -> assertInstanceOf(Boolean.class, x)
		),
		input(
			"SimpleTypes-3",
			Integer.class,
			123,
			"<number>123</number>",
			"<number>123</number>",
			"<number>123</number>",
			x -> assertInstanceOf(Integer.class, x)
		),
		input(
			"SimpleTypes-4",
			Float.class,
			1.23f,
			"<number>1.23</number>",
			"<number>1.23</number>",
			"<number>1.23</number>",
			x -> assertInstanceOf(Float.class, x)
		),
		input(
			"SimpleTypes-5",
			String.class,
			null,
			"<null/>",
			"<null/>",
			"<null/>"
		),
		input(
			"Arrays-1",
			String[].class,
			a("foo"),
			"<ul><li>foo</li></ul>",
			"<ul>\n\t<li>foo</li>\n</ul>\n",
			"<ul><li>foo</li></ul>",
			x -> assertInstanceOf(String.class, x[0])
		),
		input(
			"Arrays-2",
			String[].class,
			a((String)null),
			"<ul><li><null/></li></ul>",
			"<ul>\n\t<li><null/></li>\n</ul>\n",
			"<ul><li><null/></li></ul>"
		),
		input(
			"Arrays-3",
			Object[].class,
			a("foo",123,true),
			"<ul><li>foo</li><li><number>123</number></li><li><boolean>true</boolean></li></ul>",
			"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><boolean>true</boolean></li>\n</ul>\n",
			"<ul><li>foo</li><li><number>123</number></li><li><boolean>true</boolean></li></ul>",
			x -> { assertInstanceOf(String.class, x[0]); assertInstanceOf(Integer.class, x[1]); assertInstanceOf(Boolean.class, x[2]); }
		),
		input(
			"Arrays-4",
			int[].class,
			ints(123),
			"<ul><li><number>123</number></li></ul>",
			"<ul>\n\t<li><number>123</number></li>\n</ul>\n",
			"<ul><li>123</li></ul>",
			x -> assertInstanceOf(int[].class, x)
		),
		input(
			"Arrays-5",
			boolean[].class,
			booleans(true),
			"<ul><li><boolean>true</boolean></li></ul>",
			"<ul>\n\t<li><boolean>true</boolean></li>\n</ul>\n",
			"<ul><li>true</li></ul>",
			x -> assertInstanceOf(boolean[].class, x)
		),
		input(
			"Arrays-6",
			String[][].class,
			a2(a("foo")),
			"<ul><li><ul><li>foo</li></ul></li></ul>",
			"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>foo</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
			"<ul><li><ul><li>foo</li></ul></li></ul>",
			x -> assertInstanceOf(String[][].class, x)
		),
		input(
			"MapWithStrings",
			MapWithStrings.class,
			new MapWithStrings().append("k1", "v1").append("k2", null),
			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><null/></td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstanceOf(String.class, x.get("k1"))
		),
		input(
			"MapsWithNumbers",
			MapWithNumbers.class,
			new MapWithNumbers().append("k1", 123).append("k2", 1.23).append("k3", null),
			"""
			<table>
				<tr>
					<td>k1</td>
					<td><number>123</number></td>
				</tr>
				<tr>
					<td>k2</td>
					<td><number>1.23</number></td>
				</tr>
				<tr>
					<td>k3</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>k1</td>
					<td><number>123</number></td>
				</tr>
				<tr>
					<td>k2</td>
					<td><number>1.23</number></td>
				</tr>
				<tr>
					<td>k3</td>
					<td><null/></td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>k1</td>
					<td>123</td>
				</tr>
				<tr>
					<td>k2</td>
					<td>1.23</td>
				</tr>
				<tr>
					<td>k3</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstanceOf(Number.class, x.get("k1"))
		),
		input(
			"MapWithObjects",
			MapWithObjects.class,
			new MapWithObjects().append("k1", "v1").append("k2", 123).append("k3", 1.23).append("k4", true).append("k5", null),
			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><number>123</number></td>
				</tr>
				<tr>
					<td>k3</td>
					<td><number>1.23</number></td>
				</tr>
				<tr>
					<td>k4</td>
					<td><boolean>true</boolean></td>
				</tr>
				<tr>
					<td>k5</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><number>123</number></td>
				</tr>
				<tr>
					<td>k3</td>
					<td><number>1.23</number></td>
				</tr>
				<tr>
					<td>k4</td>
					<td><boolean>true</boolean></td>
				</tr>
				<tr>
					<td>k5</td>
					<td><null/></td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>k1</td>
					<td>v1</td>
				</tr>
				<tr>
					<td>k2</td>
					<td><number>123</number></td>
				</tr>
				<tr>
					<td>k3</td>
					<td><number>1.23</number></td>
				</tr>
				<tr>
					<td>k4</td>
					<td><boolean>true</boolean></td>
				</tr>
				<tr>
					<td>k5</td>
					<td><null/></td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(String.class, x.get("k1")); assertInstanceOf(Integer.class, x.get("k2")); assertInstanceOf(Float.class, x.get("k3")); assertInstanceOf(Boolean.class, x.get("k4")); }
		),
		input(
			"ListWithStrings",
			ListWithStrings.class,
			new ListWithStrings().append("foo").append(null),
			"<ul><li>foo</li><li><null/></li></ul>",
			"<ul>\n\t<li>foo</li>\n\t<li><null/></li>\n</ul>\n",
			"<ul><li>foo</li><li><null/></li></ul>",
			x -> assertInstanceOf(String.class, x.get(0))
		),
		input(
			"ListWithNumbers",
			ListWithNumbers.class,
			new ListWithNumbers().append(123).append(1.23).append(null),
			"<ul><li><number>123</number></li><li><number>1.23</number></li><li><null/></li></ul>",
			"<ul>\n\t<li><number>123</number></li>\n\t<li><number>1.23</number></li>\n\t<li><null/></li>\n</ul>\n",
			"<ul><li>123</li><li>1.23</li><li><null/></li></ul>",
			x -> { assertInstanceOf(Integer.class, x.get(0)); assertInstanceOf(Float.class, x.get(1)); }
		),
		input(
			"ListWithObjects",
			ListWithObjects.class,
			new ListWithObjects().append("foo").append(123).append(1.23).append(true).append(null),
			"<ul><li>foo</li><li><number>123</number></li><li><number>1.23</number></li><li><boolean>true</boolean></li><li><null/></li></ul>",
			"<ul>\n\t<li>foo</li>\n\t<li><number>123</number></li>\n\t<li><number>1.23</number></li>\n\t<li><boolean>true</boolean></li>\n\t<li><null/></li>\n</ul>\n",
			"<ul><li>foo</li><li><number>123</number></li><li><number>1.23</number></li><li><boolean>true</boolean></li><li><null/></li></ul>",
			x -> { assertInstanceOf(String.class, x.get(0)); assertInstanceOf(Integer.class, x.get(1)); assertInstanceOf(Float.class, x.get(2)); assertInstanceOf(Boolean.class, x.get(3)); }
		),
		input(
			"BeanWithNormalProperties",
			BeanWithNormalProperties.class,
			new BeanWithNormalProperties().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td>foo</td>
				</tr>
				<tr>
					<td>b</td>
					<td>123</td>
				</tr>
				<tr>
					<td>c</td>
					<td>bar</td>
				</tr>
				<tr>
					<td>d</td>
					<td><number>456</number></td>
				</tr>
				<tr>
					<td>e</td>
					<td>
						<table>
							<tr>
								<td>h</td>
								<td>qux</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>f</td>
					<td>
						<ul>
							<li>baz</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>g</td>
					<td>
						<ul>
							<li>789</li>
						</ul>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td>foo</td>
				</tr>
				<tr>
					<td>b</td>
					<td>123</td>
				</tr>
				<tr>
					<td>c</td>
					<td>bar</td>
				</tr>
				<tr>
					<td>d</td>
					<td><number>456</number></td>
				</tr>
				<tr>
					<td>e</td>
					<td>
						<table>
							<tr>
								<td>h</td>
								<td>qux</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>f</td>
					<td>
						<ul>
							<li>baz</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>g</td>
					<td>
						<ul>
							<li>789</li>
						</ul>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>foo</td>
				</tr>
				<tr>
					<td>b</td>
					<td>123</td>
				</tr>
				<tr>
					<td>c</td>
					<td>bar</td>
				</tr>
				<tr>
					<td>d</td>
					<td><number>456</number></td>
				</tr>
				<tr>
					<td>e</td>
					<td>
						<table>
							<tr>
								<td>h</td>
								<td>qux</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>f</td>
					<td>
						<ul>
							<li>baz</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>g</td>
					<td>
						<ul>
							<li>789</li>
						</ul>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(String.class, x.c); assertInstanceOf(Integer.class, x.d); assertInstanceOf(Bean1a.class, x.e); }
		),
		input(
			"BeanWithMapProperties",
			BeanWithMapProperties.class,
			new BeanWithMapProperties().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>123</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>bar</td>
							</tr>
							<tr>
								<td>k4</td>
								<td><number>456</number></td>
							</tr>
							<tr>
								<td>k5</td>
								<td><boolean>true</boolean></td>
							</tr>
							<tr>
								<td>k6</td>
								<td><null/></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>123</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>bar</td>
							</tr>
							<tr>
								<td>k4</td>
								<td><number>456</number></td>
							</tr>
							<tr>
								<td>k5</td>
								<td><boolean>true</boolean></td>
							</tr>
							<tr>
								<td>k6</td>
								<td><null/></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>123</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>bar</td>
							</tr>
							<tr>
								<td>k4</td>
								<td><number>456</number></td>
							</tr>
							<tr>
								<td>k5</td>
								<td><boolean>true</boolean></td>
							</tr>
							<tr>
								<td>k6</td>
								<td><null/></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(String.class, x.a.get("k1")); assertInstanceOf(Integer.class, x.b.get("k2")); assertInstanceOf(String.class, x.c.get("k3")); assertInstanceOf(Integer.class, x.c.get("k4")); assertInstanceOf(Boolean.class, x.c.get("k5")); }
		),
		input(
			"BeanWithTypeName",
			BeanWithTypeName.class,
			new BeanWithTypeName().init(),
			"""
			<table _type='X'>
				<tr>
					<td>a</td>
					<td>123</td>
				</tr>
				<tr>
					<td>b</td>
					<td>foo</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table _type='X'>
				<tr>
					<td>a</td>
					<td>123</td>
				</tr>
				<tr>
					<td>b</td>
					<td>foo</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>123</td>
				</tr>
				<tr>
					<td>b</td>
					<td>foo</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstanceOf(BeanWithTypeName.class, x)
		),
		input(
			"BeanWithPropertiesWithTypeNames",
			BeanWithPropertiesWithTypeNames.class,
			new BeanWithPropertiesWithTypeNames().init(),
			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='B'>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='B'>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='B'>
							<tr>
								<td>b</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstanceOf(B.class, x.b2)
		),
		input(
			"BeanWithPropertiesWithArrayTypeNames",
			BeanWithPropertiesWithArrayTypeNames.class,
			new BeanWithPropertiesWithArrayTypeNames().init(),
			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<table _type='array'>
							<tr>
								<th>b</th>
							</tr>
							<tr _type='B'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstancesOf(B.class, x.b2[0], x.b3[0])
		),
		input(
			"BeanWithPropertiesWith2dArrayTypeNames",
			BeanWithPropertiesWith2dArrayTypeNames.class,
			new BeanWithPropertiesWith2dArrayTypeNames().init(),
			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>b3</td>
					<td>
						<ul>
							<li>
								<table _type='array'>
									<tr>
										<th>b</th>
									</tr>
									<tr _type='B'>
										<td>foo</td>
									</tr>
								</table>
							</li>
						</ul>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(B.class, x.b2[0][0]); assertInstanceOf(B.class, x.b3[0][0]); }
		),
		input(
			"BeanWithPropertiesWithMapTypeNames",
			BeanWithPropertiesWithMapTypeNames.class,
			new BeanWithPropertiesWithMapTypeNames().init(),
			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='B'>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='B'>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>b1</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b2</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='B'>
										<tr>
											<td>b</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(B.class, x.b1.get("k1")); assertInstanceOf(B.class, x.b2.get("k2")); }
		),
		input(
			"LinkBean-1",
			LinkBean.class,
			new LinkBean().init(),
			"<a href='http://apache.org'>foo</a>",
			"<a href='http://apache.org'>foo</a>",
			"<a href='http://apache.org'>foo</a>",
			x -> assertInstanceOf(LinkBean.class, x)
		),
		input(
			"LinkBean-2",
			LinkBean[].class,
			a(new LinkBean().init(),new LinkBean().init()),
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			x -> assertInstanceOf(LinkBean.class, x[0])
		),
		input(
			"ListWithLinkBeans",
			ListWithLinkBeans.class,
			new ListWithLinkBeans().append(new LinkBean().init()).append(new LinkBean().init()),
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			x -> assertInstanceOf(LinkBean.class, x.get(0))
		),
		input(
			"BeanWithLinkBeanProperties",
			BeanWithLinkBeanProperties.class,
			new BeanWithLinkBeanProperties().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(LinkBean.class, x.a); assertInstanceOf(LinkBean.class, x.b.get(0)); assertInstanceOf(LinkBean.class, x.c.get("c1")); }
		),
		input(
			"LinkBeanC-1",
			LinkBeanC.class,
			new LinkBeanC().init(),
			"<a href='http://apache.org'>foo</a>",
			"<a href='http://apache.org'>foo</a>",
			"<a href='http://apache.org'>foo</a>",
			x -> assertInstanceOf(LinkBeanC.class, x)
		),
		input(
			"LinkBeanC-2",
			LinkBeanC[].class,
			a(new LinkBeanC().init(),new LinkBeanC().init()),
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			x -> assertInstanceOf(LinkBeanC.class, x[0])
		),
		input(
			"ListWithLinkBeansC",
			ListWithLinkBeansC.class,
			new ListWithLinkBeansC().append(new LinkBeanC().init()).append(new LinkBeanC().init()),
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			"<ul>\n\t<li><a href='http://apache.org'>foo</a></li>\n\t<li><a href='http://apache.org'>foo</a></li>\n</ul>\n",
			"<ul><li><a href='http://apache.org'>foo</a></li><li><a href='http://apache.org'>foo</a></li></ul>",
			x -> assertInstanceOf(LinkBeanC.class, x.get(0))
		),
		input(
			"BeanWithLinkBeanPropertiesC",
			BeanWithLinkBeanPropertiesC.class,
			new BeanWithLinkBeanPropertiesC().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td><a href='http://apache.org'>foo</a></td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<ul>
							<li><a href='http://apache.org'>foo</a></li>
						</ul>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>c1</td>
								<td><a href='http://apache.org'>foo</a></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> { assertInstanceOf(LinkBeanC.class, x.a); assertInstanceOf(LinkBeanC.class, x.b.get(0)); assertInstanceOf(LinkBeanC.class, x.c.get("c1")); }
		),
		input(
			"BeanWithSpecialCharacters",
			BeanWithSpecialCharacters.class,
			new BeanWithSpecialCharacters().init(),
			"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
			"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>\n\t</tr>\n</table>\n",
			"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
			x -> assertInstanceOf(BeanWithSpecialCharacters.class, x)
		),
		input(
			"BeanWithSpecialCharacters-2",
			BeanWithSpecialCharacters.class,
			new BeanWithSpecialCharacters().init(),
			"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",

			"""
			<table>
				<tr>
					<td>a</td>
					<td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td>
				</tr>
			</table>
			""",

			"<table><tr><td>a</td><td><sp> </sp> <bs/><ff/><br/><sp>&#x2003;</sp>&#13; <sp> </sp></td></tr></table>",
			x -> assertInstanceOf(BeanWithSpecialCharacters.class, x)
		),
		input(
			"BeanWithNullProperties",
			BeanWithNullProperties.class,
			new BeanWithNullProperties(),
			"<table></table>",
			"<table>\n</table>\n",
			"<table></table>",
			x -> assertInstanceOf(BeanWithNullProperties.class, x)
		),
		input(
			"BeanWithAbstractFields",
			BeanWithAbstractFields.class,
			new BeanWithAbstractFields().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o</td>
					<td>
						<table _type='A'>
							<tr>
								<td>a</td>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstancesOf(A.class, x.a, x.ia, x.aa, x.o)
		),
		input(
			"BeanWithAbstractArrayFields",
			BeanWithAbstractArrayFields.class,
			new BeanWithAbstractArrayFields().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>ia2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>aa2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o1</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>o2</td>
					<td>
						<table _type='array'>
							<tr>
								<th>a</th>
							</tr>
							<tr _type='A'>
								<td>foo</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstancesOf(A.class, x.a[0], x.ia1[0], x.ia2[0], x.aa1[0], x.aa2[0], x.o1[0], x.o2[0])
		),
		input(
			"BeanWithAbstractMapFields",
			BeanWithAbstractMapFields.class,
			new BeanWithAbstractMapFields().init(),
			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""",

			"""
			<table>
				<tr>
					<td>a</td>
					<td>
						<table>
							<tr>
								<td>k1</td>
								<td>
									<table>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>b</td>
					<td>
						<table>
							<tr>
								<td>k2</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>c</td>
					<td>
						<table>
							<tr>
								<td>k3</td>
								<td>
									<table _type='A'>
										<tr>
											<td>a</td>
											<td>foo</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			""".replaceAll("(?m)^\\s+|\\R", ""),
			x -> assertInstancesOf(A.class, x.a.get("k1"), x.b.get("k2"), x.c.get("k3"))
		),
		input(
			"BeanWithWhitespaceTextFields-1",
			BeanWithWhitespaceTextFields.class,
			new BeanWithWhitespaceTextFields().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields-2",
			BeanWithWhitespaceTextFields.class,
			new BeanWithWhitespaceTextFields().init(""),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields-3",
			BeanWithWhitespaceTextFields.class,
			new BeanWithWhitespaceTextFields().init(" "),
			"<object><sp> </sp></object>",
			"<object><sp> </sp></object>\n",
			"<object><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields-4",
			BeanWithWhitespaceTextFields.class,
			new BeanWithWhitespaceTextFields().init("  "),
			"<object><sp> </sp><sp> </sp></object>",
			"<object><sp> </sp><sp> </sp></object>\n",
			"<object><sp> </sp><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields-5",
			BeanWithWhitespaceTextFields.class,
			new BeanWithWhitespaceTextFields().init("  foobar  "),
			"<object><sp> </sp> foobar <sp> </sp></object>",
			"<object><sp> </sp> foobar <sp> </sp></object>\n",
			"<object><sp> </sp> foobar <sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields-1",
			BeanWithWhitespaceTextPwsFields.class,
			new BeanWithWhitespaceTextPwsFields().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields-2",
			BeanWithWhitespaceTextPwsFields.class,
			new BeanWithWhitespaceTextPwsFields().init(""),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields-3",
			BeanWithWhitespaceTextPwsFields.class,
			new BeanWithWhitespaceTextPwsFields().init(" "),
			"<object> </object>",
			"<object> </object>\n",
			"<object> </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields-4",
			BeanWithWhitespaceTextPwsFields.class,
			new BeanWithWhitespaceTextPwsFields().init("  "),
			"<object>  </object>",
			"<object>  </object>\n",
			"<object>  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields-5",
			BeanWithWhitespaceTextPwsFields.class,
			new BeanWithWhitespaceTextPwsFields().init("  foobar  "),
			"<object>  foobar  </object>",
			"<object>  foobar  </object>\n",
			"<object>  foobar  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-1",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-2",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(new String[0]),
			"<object></object>",
			"<object></object>\n",
			"<object></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-3",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(a("")),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-4",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(a(" ")),
			"<object><sp> </sp></object>",
			"<object><sp> </sp></object>\n",
			"<object><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-5",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(a("  ")),
			"<object><sp> </sp><sp> </sp></object>",
			"<object><sp> </sp><sp> </sp></object>\n",
			"<object><sp> </sp><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields-6",
			BeanWithWhitespaceMixedFields.class,
			new BeanWithWhitespaceMixedFields().init(a("  foobar  ")),
			"<object><sp> </sp> foobar <sp> </sp></object>",
			"<object><sp> </sp> foobar <sp> </sp></object>\n",
			"<object><sp> </sp> foobar <sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-1",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-2",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(new String[0]),
			"<object></object>",
			"<object></object>\n",
			"<object></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-3",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(a("")),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-4",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(a(" ")),
			"<object> </object>",
			"<object> </object>\n",
			"<object> </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-5",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(a("  ")),
			"<object>  </object>",
			"<object>  </object>\n",
			"<object>  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields-6",
			BeanWithWhitespaceMixedPwsFields.class,
			new BeanWithWhitespaceMixedPwsFields().init(a("  foobar  ")),
			"<object>  foobar  </object>",
			"<object>  foobar  </object>\n",
			"<object>  foobar  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields2-1",
			BeanWithWhitespaceTextFields2.class,
			new BeanWithWhitespaceTextFields2().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields2-2",
			BeanWithWhitespaceTextFields2.class,
			new BeanWithWhitespaceTextFields2().init(""),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields2-3",
			BeanWithWhitespaceTextFields2.class,
			new BeanWithWhitespaceTextFields2().init(" "),
			"<object><sp> </sp></object>",
			"<object><sp> </sp></object>\n",
			"<object><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields2-4",
			BeanWithWhitespaceTextFields2.class,
			new BeanWithWhitespaceTextFields2().init("  "),
			"<object><sp> </sp><sp> </sp></object>",
			"<object><sp> </sp><sp> </sp></object>\n",
			"<object><sp> </sp><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextFields2-5",
			BeanWithWhitespaceTextFields2.class,
			new BeanWithWhitespaceTextFields2().init("  foobar  "),
			"<object><sp> </sp> foobar <sp> </sp></object>",
			"<object><sp> </sp> foobar <sp> </sp></object>\n",
			"<object><sp> </sp> foobar <sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields2-1",
			BeanWithWhitespaceTextPwsFields2.class,
			new BeanWithWhitespaceTextPwsFields2().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields2-2",
			BeanWithWhitespaceTextPwsFields2.class,
			new BeanWithWhitespaceTextPwsFields2().init(""),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields2-3",
			BeanWithWhitespaceTextPwsFields2.class,
			new BeanWithWhitespaceTextPwsFields2().init(" "),
			"<object> </object>",
			"<object> </object>\n",
			"<object> </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields2-4",
			BeanWithWhitespaceTextPwsFields2.class,
			new BeanWithWhitespaceTextPwsFields2().init("  "),
			"<object>  </object>",
			"<object>  </object>\n",
			"<object>  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceTextPwsFields2-5",
			BeanWithWhitespaceTextPwsFields2.class,
			new BeanWithWhitespaceTextPwsFields2().init("  foobar  "),
			"<object>  foobar  </object>",
			"<object>  foobar  </object>\n",
			"<object>  foobar  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceTextPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-1",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-2",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(new String[0]),
			"<object></object>",
			"<object></object>\n",
			"<object></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-3",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(a("")),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-4",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(a(" ")),
			"<object><sp> </sp></object>",
			"<object><sp> </sp></object>\n",
			"<object><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-5",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(a("  ")),
			"<object><sp> </sp><sp> </sp></object>",
			"<object><sp> </sp><sp> </sp></object>\n",
			"<object><sp> </sp><sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedFields2-6",
			BeanWithWhitespaceMixedFields2.class,
			new BeanWithWhitespaceMixedFields2().init(a("  foobar  ")),
			"<object><sp> </sp> foobar <sp> </sp></object>",
			"<object><sp> </sp> foobar <sp> </sp></object>\n",
			"<object><sp> </sp> foobar <sp> </sp></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-1",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(null),
			"<object nil='true'></object>",
			"<object nil='true'>\n</object>\n",
			"<object nil='true'></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-2",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(new String[0]),
			"<object></object>",
			"<object></object>\n",
			"<object></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-3",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(a("")),
			"<object><sp/></object>",
			"<object><sp/></object>\n",
			"<object><sp/></object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-4",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(a(" ")),
			"<object> </object>",
			"<object> </object>\n",
			"<object> </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-5",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(a("  ")),
			"<object>  </object>",
			"<object>  </object>\n",
			"<object>  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		),
		input(
			"BeanWithWhitespaceMixedPwsFields2-6",
			BeanWithWhitespaceMixedPwsFields2.class,
			new BeanWithWhitespaceMixedPwsFields2().init(a("  foobar  ")),
			"<object>  foobar  </object>",
			"<object>  foobar  </object>\n",
			"<object>  foobar  </object>",
			x -> assertInstanceOf(BeanWithWhitespaceMixedPwsFields2.class, x)
		)
	};

	static Input[] input() {
		return INPUT;
	}

	private static <T> Input<T> input(String label, Class<T> type, T in, String e1, String e2, String e3) {
		return input(label, type, in, e1, e2, e3, null);
	}

	private static <T> Input<T> input(String label, Class<T> type, T in, String e1, String e2, String e3, Consumer<T> verifier) {
		return new Input<>(label, type, in, e1, e2, e3, verifier);
	}

	public static class Input<T> {
		private final String label, e1, e2, e3;
		private final Class<T> type;
		private final Object in;
		private final Consumer<T> verifier;

		public Input(String label, Class<T> type, T in, String e1, String e2, String e3, Consumer<T> verifier) {
			this.label = label;
			this.type = type;
			this.in = in;
			this.e1 = e1;
			this.e2 = e2;
			this.e3 = e3;
			this.verifier = verifier;
		}

		public void verify(T o) {
			if (verifier != null) verifier.accept(o);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	void a1_serializeNormal(Input input) {
		try {
			var r = s1.serialize(input.in);
			assertEquals(input.e1, r, fs("{0} serialize-normal failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	<T> void a2_parseNormal(Input<T> input) {
		try {
			var r = s1.serialize(input.in);
			var o = parser.parse(r, input.type);
			r = s1.serialize(o);
			assertEquals(input.e1, r, fs("{0} parse-normal failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	<T> void a3_verifyNormal(Input<T> input) {
		try {
			var r = s1.serialize(input.in);
			var o = parser.parse(r, input.type);
			input.verify(input.type.cast(o));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	void b1_serializeReadable(Input input) {
		try {
			var r = s2.serialize(input.in);
			assertEquals(input.e2, r, fs("{0} serialize-readable failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	<T> void b2_parseReadable(Input<T> input) {
		try {
			var r = s2.serialize(input.in);
			var o = parser.parse(r, input.type);
			r = s2.serialize(o);
			assertEquals(input.e2, r, fs("{0} parse-readable failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	<T> void b3_verifyReadable(Input<T> input) {
		try {
			var r = s2.serialize(input.in);
			var o = parser.parse(r, input.type);
			input.verify(o);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	void c1_serializeAbridged(Input input) {
		try {
			var r = s3.serialize(input.in);
			assertEquals(input.e3, r, fs("{0} serialize-addRootType failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	<T> void c2_parseAbridged(Input<T> input) {
		try {
			var r = s3.serialize(input.in);
			var o = parser.parse(r, input.type);
			r = s3.serialize(o);
			assertEquals(input.e3, r, fs("{0} parse-addRootType failed", input.label));
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(input.label + " test failed", e);
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	void c3_verifyAbridged(Input<Object> input) {
		try {
			var r = s3.serialize(input.in);
			var o = parser.parse(r, input.type);
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
			add(value);
			return this;
		}
	}

	public static class ListWithNumbers extends ArrayList<Number> {
		public ListWithNumbers append(Number value) {
			add(value);
			return this;
		}
	}

	public static class ListWithObjects extends ArrayList<Object> {
		public ListWithObjects append(Object value) {
			add(value);
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
			f = a("baz");
			g = ints( 789 );
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
			b1 = a(new B().init());
			b2 = a(new B().init());
			b3 = ao(new B().init());
			return this;
		}
	}

	@Bean(dictionary={B.class})
	public static class BeanWithPropertiesWith2dArrayTypeNames {
		public B[][] b1;
		public Object[][] b2;
		public Object[][] b3;

		BeanWithPropertiesWith2dArrayTypeNames init() {
			b1 = a2(a(new B().init()));
			b2 = a2(a(new B().init()));
			b3 = a2(ao(new B().init()));
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

	@HtmlLink(on="Dummy1", nameProperty="a", uriProperty="b")
	@HtmlLink(on="LinkBeanC", nameProperty="a", uriProperty="b")
	@HtmlLink(on="Dummy2", nameProperty="a", uriProperty="b")
	private static class LinkBeanCConfig {}

	public static class LinkBeanC {
		public String a;
		public String b;

		LinkBeanC init() {
			a = "foo";
			b = "http://apache.org";
			return this;
		}
	}

	public static class ListWithLinkBeansC extends ArrayList<LinkBeanC> {
		public ListWithLinkBeansC append(LinkBeanC value) {
			this.add(value);
			return this;
		}
	}

	public static class BeanWithLinkBeanPropertiesC {
		public LinkBeanC a;
		public List<LinkBeanC> b;
		public Map<String,LinkBeanC> c;

		BeanWithLinkBeanPropertiesC init() {
			a = new LinkBeanC().init();
			b = new ListWithLinkBeansC().append(new LinkBeanC().init());
			c = new LinkedHashMap<>();
			c.put("c1", new LinkBeanC().init());
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
			a = a(new A().init());
			ia1 = a(new A().init());
			aa1 = a(new A().init());
			o1 = a(new A().init());
			ia2 = a(new A().init());
			aa2 = a(new A().init());
			o2 = ao(new A().init());
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

	public interface IA {
		String getA();
		void setA(String a);
	}

	public abstract static class AA implements IA {}

	@Bean(typeName="A")
	public static class A extends AA {

		private String a;
		@Override public String getA() { return a; }
		@Override public void setA(String v) { a = v; }

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

	@Html(on="Dummy1",format=XML)
	@Html(on="BeanWithWhitespaceTextFields2",format=XML)
	@Html(on="Dummy2",format=XML)
	@Xml(on="Dummy1.a",format=XmlFormat.TEXT)
	@Xml(on="BeanWithWhitespaceTextFields2.a",format=XmlFormat.TEXT)
	@Xml(on="Dumuy2.a",format=XmlFormat.TEXT)
	private static class BeanWithWhitespaceTextFields2Config {}

	public static class BeanWithWhitespaceTextFields2 {
		public String a;

		public BeanWithWhitespaceTextFields2 init(String s) {
			a = s;
			return this;
		}
	}

	@Html(on="BeanWithWhitespaceTextPwsFields2",format=XML)
	@Xml(on="BeanWithWhitespaceTextPwsFields2.a",format=XmlFormat.TEXT_PWS)
	private static class BeanWithWhitespaceTextPwsFields2Config {}

	public static class BeanWithWhitespaceTextPwsFields2 {
		public String a;

		public BeanWithWhitespaceTextPwsFields2 init(String s) {
			a = s;
			return this;
		}
	}

	@Html(on="BeanWithWhitespaceMixedFields2",format=XML)
	@Xml(on="BeanWithWhitespaceMixedFields2.a",format=XmlFormat.MIXED)
	public static class BeanWithWhitespaceMixedFields2Config {}

	public static class BeanWithWhitespaceMixedFields2 {
		public String[] a;

		public BeanWithWhitespaceMixedFields2 init(String[] s) {
			a = s;
			return this;
		}
	}

	@Html(on="BeanWithWhitespaceMixedPwsFields2",format=XML)
	@Xml(on="BeanWithWhitespaceMixedPwsFields2.a",format=XmlFormat.MIXED_PWS)
	public static class BeanWithWhitespaceMixedPwsFields2Config {}

	@Html(format=XML)
	public static class BeanWithWhitespaceMixedPwsFields2 {
		public String[] a;

		public BeanWithWhitespaceMixedPwsFields2 init(String[] s) {
			a = s;
			return this;
		}
	}

	private static void assertInstancesOf(Class<?> c, Object...values) {
		for (int i = 0; i < values.length; i++) {
			assertInstanceOf(c, values[i], fs("Object {0} not expected type.  Expected={1}, Actual={2}", i, c, Utils.scn(values[i])));
		}
	}
}