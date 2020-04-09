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
package org.apache.juneau;

import static org.apache.juneau.serializer.Serializer.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for BeanTraverseContext properties.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class SerializerPropertiesComboTest extends ComboRoundTripTest {

	@Bean(typeName="BwT")
	public static class BeanWithType {
		public int f;

		public static BeanWithType create() {
			BeanWithType l = new BeanWithType();
			l.f = 1;
			return l;
		}
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"SERIALIZER_addBeanTypes",
					OMap.class,
					OMap.of("a", BeanWithType.create()),
					/* Json */		"{a:{_type:'BwT',f:1}}",
					/* JsonT */		"{a:{t:'BwT',f:1}}",
					/* JsonR */		"{\n\ta: {\n\t\t_type: 'BwT',\n\t\tf: 1\n\t}\n}",
					/* Xml */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* XmlT */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* XmlR */		"<object>\n\t<BwT _name='a'>\n\t\t<f>1</f>\n\t</BwT>\n</object>\n",
					/* XmlNs */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* Html */		"<table><tr><td>a</td><td><table _type='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>a</td><td><table t='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<table _type='BwT'>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>f</td>\n\t\t\t\t\t<td>1</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(a=(_type=BwT,f=1))",
					/* UonT */		"(a=(t=BwT,f=1))",
					/* UonR */		"(\n\ta=(\n\t\t_type=BwT,\n\t\tf=1\n\t)\n)",
					/* UrlEnc */	"a=(_type=BwT,f=1)",
					/* UrlEncT */	"a=(t=BwT,f=1)",
					/* UrlEncR */	"a=(\n\t_type=BwT,\n\tf=1\n)",
					/* MsgPack */	"81A16182A55F74797065A3427754A16601",
					/* MsgPackT */	"81A16182A174A3427754A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:_type>BwT</jp:_type>\n      <jp:f>1</jp:f>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_addBeanTypes, true, BEAN_beanDictionary, BeanWithType.class))
			},
			{ 	/* 1 */
				new ComboInput<>(
					"SERIALIZER_addRootType",
					BeanWithType.class,
					BeanWithType.create(),
					/* Json */		"{_type:'BwT',f:1}",
					/* JsonT */		"{t:'BwT',f:1}",
					/* JsonR */		"{\n\t_type: 'BwT',\n\tf: 1\n}",
					/* Xml */		"<BwT><f>1</f></BwT>",
					/* XmlT */		"<BwT><f>1</f></BwT>",
					/* XmlR */		"<BwT>\n\t<f>1</f>\n</BwT>\n",
					/* XmlNs */		"<BwT><f>1</f></BwT>",
					/* Html */		"<table _type='BwT'><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlT */		"<table t='BwT'><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='BwT'>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(_type=BwT,f=1)",
					/* UonT */		"(t=BwT,f=1)",
					/* UonR */		"(\n\t_type=BwT,\n\tf=1\n)",
					/* UrlEnc */	"_type=BwT&f=1",
					/* UrlEncT */	"t=BwT&f=1",
					/* UrlEncR */	"_type=BwT\n&f=1",
					/* MsgPack */	"82A55F74797065A3427754A16601",
					/* MsgPackT */	"82A174A3427754A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>BwT</jp:_type>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_addRootType, true, BEAN_beanDictionary, BeanWithType.class))
			},
			{ 	/* 2 */
				new ComboInput<>(
					"SERIALIZER_sortCollections",
					String[].class,
					new String[]{"c","a","b"},
					/* Json */		"['a','b','c']",
					/* JsonT */		"['a','b','c']",
					/* JsonR */		"[\n\t'a',\n\t'b',\n\t'c'\n]",
					/* Xml */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* XmlT */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* XmlR */		"<array>\n\t<string>a</string>\n\t<string>b</string>\n\t<string>c</string>\n</array>\n",
					/* XmlNs */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* Html */		"<ul><li>a</li><li>b</li><li>c</li></ul>",
					/* HtmlT */		"<ul><li>a</li><li>b</li><li>c</li></ul>",
					/* HtmlR */		"<ul>\n\t<li>a</li>\n\t<li>b</li>\n\t<li>c</li>\n</ul>\n",
					/* Uon */		"@(a,b,c)",
					/* UonT */		"@(a,b,c)",
					/* UonR */		"@(\n\ta,\n\tb,\n\tc\n)",
					/* UrlEnc */	"0=c&1=a&2=b",
					/* UrlEncT */	"0=c&1=a&2=b",
					/* UrlEncR */	"0=c\n&1=a\n&2=b",
					/* MsgPack */	"93A161A162A163",
					/* MsgPackT */	"93A161A162A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>a</rdf:li>\n    <rdf:li>b</rdf:li>\n    <rdf:li>c</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_sortCollections, true))
			},
			{ 	/* 3 */
				new ComboInput<>(
					"SERIALIZER_sortCollections",
					List.class,
					Collections.unmodifiableList(AList.of("c","a","b")),
					/* Json */		"['a','b','c']",
					/* JsonT */		"['a','b','c']",
					/* JsonR */		"[\n\t'a',\n\t'b',\n\t'c'\n]",
					/* Xml */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* XmlT */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* XmlR */		"<array>\n\t<string>a</string>\n\t<string>b</string>\n\t<string>c</string>\n</array>\n",
					/* XmlNs */		"<array><string>a</string><string>b</string><string>c</string></array>",
					/* Html */		"<ul><li>a</li><li>b</li><li>c</li></ul>",
					/* HtmlT */		"<ul><li>a</li><li>b</li><li>c</li></ul>",
					/* HtmlR */		"<ul>\n\t<li>a</li>\n\t<li>b</li>\n\t<li>c</li>\n</ul>\n",
					/* Uon */		"@(a,b,c)",
					/* UonT */		"@(a,b,c)",
					/* UonR */		"@(\n\ta,\n\tb,\n\tc\n)",
					/* UrlEnc */	"0=c&1=a&2=b",
					/* UrlEncT */	"0=c&1=a&2=b",
					/* UrlEncR */	"0=c\n&1=a\n&2=b",
					/* MsgPack */	"93A161A162A163",
					/* MsgPackT */	"93A161A162A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>a</rdf:li>\n    <rdf:li>b</rdf:li>\n    <rdf:li>c</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_sortCollections, true))
			},
			{ 	/* 4 */
				new ComboInput<>(
					"SERIALIZER_sortMaps",
					Map.class,
					Collections.unmodifiableMap(AMap.<String,String>of("c","3","a","1","b","2")),
					/* Json */		"{a:'1',b:'2',c:'3'}",
					/* JsonT */		"{a:'1',b:'2',c:'3'}",
					/* JsonR */		"{\n\ta: '1',\n\tb: '2',\n\tc: '3'\n}",
					/* Xml */		"<object><a>1</a><b>2</b><c>3</c></object>",
					/* XmlT */		"<object><a>1</a><b>2</b><c>3</c></object>",
					/* XmlR */		"<object>\n\t<a>1</a>\n\t<b>2</b>\n\t<c>3</c>\n</object>\n",
					/* XmlNs */		"<object><a>1</a><b>2</b><c>3</c></object>",
					/* Html */		"<table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>2</td></tr><tr><td>c</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>2</td></tr><tr><td>c</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>2</td>\n\t</tr>\n\t<tr>\n\t\t<td>c</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(a='1',b='2',c='3')",
					/* UonT */		"(a='1',b='2',c='3')",
					/* UonR */		"(\n\ta='1',\n\tb='2',\n\tc='3'\n)",
					/* UrlEnc */	"a='1'&b='2'&c='3'",
					/* UrlEncT */	"a='1'&b='2'&c='3'",
					/* UrlEncR */	"a='1'\n&b='2'\n&c='3'",
					/* MsgPack */	"83A161A131A162A132A163A133",
					/* MsgPackT */	"83A161A131A162A132A163A133",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:b>2</jp:b>\n<jp:c>3</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:b>2</jp:b>\n<jp:c>3</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n    <jp:b>2</jp:b>\n    <jp:c>3</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_sortMaps, true))
			},
		});
	}

	public SerializerPropertiesComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
}
