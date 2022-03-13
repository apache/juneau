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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for BeanTraverseContext properties.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class SerializerPropertiesComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"SERIALIZER_addBeanTypes",
					JsonMap.class,
					JsonMap.of("a", T0.create())
				)
				.json("{a:{_type:'BwT',f:1}}")
				.jsonT("{a:{t:'BwT',f:1}}")
				.jsonR("{\n\ta: {\n\t\t_type: 'BwT',\n\t\tf: 1\n\t}\n}")
				.xml("<object><BwT _name='a'><f>1</f></BwT></object>")
				.xmlT("<object><BwT _name='a'><f>1</f></BwT></object>")
				.xmlR("<object>\n\t<BwT _name='a'>\n\t\t<f>1</f>\n\t</BwT>\n</object>\n")
				.xmlNs("<object><BwT _name='a'><f>1</f></BwT></object>")
				.html("<table><tr><td>a</td><td><table _type='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>a</td><td><table t='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<table _type='BwT'>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>f</td>\n\t\t\t\t\t<td>1</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(a=(_type=BwT,f=1))")
				.uonT("(a=(t=BwT,f=1))")
				.uonR("(\n\ta=(\n\t\t_type=BwT,\n\t\tf=1\n\t)\n)")
				.urlEnc("a=(_type=BwT,f=1)")
				.urlEncT("a=(t=BwT,f=1)")
				.urlEncR("a=(\n\t_type=BwT,\n\tf=1\n)")
				.msgPack("81A16182A55F74797065A3427754A16601")
				.msgPackT("81A16182A174A3427754A16601")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:_type>BwT</jp:_type>\n      <jp:f>1</jp:f>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.beanContext(x -> x.beanDictionary(T0.class))
				.apply(Serializer.Builder.class, x -> x.addBeanTypes())
			},
			{ 	/* 1 */
				new ComboInput<>(
					"SERIALIZER_addRootType",
					T0.class,
					T0.create()
				)
				.json("{_type:'BwT',f:1}")
				.jsonT("{t:'BwT',f:1}")
				.jsonR("{\n\t_type: 'BwT',\n\tf: 1\n}")
				.xml("<BwT><f>1</f></BwT>")
				.xmlT("<BwT><f>1</f></BwT>")
				.xmlR("<BwT>\n\t<f>1</f>\n</BwT>\n")
				.xmlNs("<BwT><f>1</f></BwT>")
				.html("<table _type='BwT'><tr><td>f</td><td>1</td></tr></table>")
				.htmlT("<table t='BwT'><tr><td>f</td><td>1</td></tr></table>")
				.htmlR("<table _type='BwT'>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(_type=BwT,f=1)")
				.uonT("(t=BwT,f=1)")
				.uonR("(\n\t_type=BwT,\n\tf=1\n)")
				.urlEnc("_type=BwT&f=1")
				.urlEncT("t=BwT&f=1")
				.urlEncR("_type=BwT\n&f=1")
				.msgPack("82A55F74797065A3427754A16601")
				.msgPackT("82A174A3427754A16601")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>BwT</jp:_type>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.beanContext(x -> x.beanDictionary(T0.class))
				.apply(Serializer.Builder.class, x -> x.addRootType())
			},
			{ 	/* 2 */
				new ComboInput<>(
					"SERIALIZER_sortCollections",
					String[].class,
					new String[]{"c","a","b"}
				)
				.json("['a','b','c']")
				.jsonT("['a','b','c']")
				.jsonR("[\n\t'a',\n\t'b',\n\t'c'\n]")
				.xml("<array><string>a</string><string>b</string><string>c</string></array>")
				.xmlT("<array><string>a</string><string>b</string><string>c</string></array>")
				.xmlR("<array>\n\t<string>a</string>\n\t<string>b</string>\n\t<string>c</string>\n</array>\n")
				.xmlNs("<array><string>a</string><string>b</string><string>c</string></array>")
				.html("<ul><li>a</li><li>b</li><li>c</li></ul>")
				.htmlT("<ul><li>a</li><li>b</li><li>c</li></ul>")
				.htmlR("<ul>\n\t<li>a</li>\n\t<li>b</li>\n\t<li>c</li>\n</ul>\n")
				.uon("@(a,b,c)")
				.uonT("@(a,b,c)")
				.uonR("@(\n\ta,\n\tb,\n\tc\n)")
				.urlEnc("0=c&1=a&2=b")
				.urlEncT("0=c&1=a&2=b")
				.urlEncR("0=c\n&1=a\n&2=b")
				.msgPack("93A161A162A163")
				.msgPackT("93A161A162A163")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>a</rdf:li>\n    <rdf:li>b</rdf:li>\n    <rdf:li>c</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.sortCollections())
			},
			{ 	/* 3 */
				new ComboInput<>(
					"SERIALIZER_sortCollections",
					List.class,
					ulist("c","a","b")
				)
				.json("['a','b','c']")
				.jsonT("['a','b','c']")
				.jsonR("[\n\t'a',\n\t'b',\n\t'c'\n]")
				.xml("<array><string>a</string><string>b</string><string>c</string></array>")
				.xmlT("<array><string>a</string><string>b</string><string>c</string></array>")
				.xmlR("<array>\n\t<string>a</string>\n\t<string>b</string>\n\t<string>c</string>\n</array>\n")
				.xmlNs("<array><string>a</string><string>b</string><string>c</string></array>")
				.html("<ul><li>a</li><li>b</li><li>c</li></ul>")
				.htmlT("<ul><li>a</li><li>b</li><li>c</li></ul>")
				.htmlR("<ul>\n\t<li>a</li>\n\t<li>b</li>\n\t<li>c</li>\n</ul>\n")
				.uon("@(a,b,c)")
				.uonT("@(a,b,c)")
				.uonR("@(\n\ta,\n\tb,\n\tc\n)")
				.urlEnc("0=c&1=a&2=b")
				.urlEncT("0=c&1=a&2=b")
				.urlEncR("0=c\n&1=a\n&2=b")
				.msgPack("93A161A162A163")
				.msgPackT("93A161A162A163")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n<rdf:li>b</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>a</rdf:li>\n    <rdf:li>b</rdf:li>\n    <rdf:li>c</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.sortCollections())
			},
			{ 	/* 4 */
				new ComboInput<>(
					"SERIALIZER_sortMaps",
					Map.class,
					unmodifiable(map("c","3","a","1","b","2"))
				)
				.json("{a:'1',b:'2',c:'3'}")
				.jsonT("{a:'1',b:'2',c:'3'}")
				.jsonR("{\n\ta: '1',\n\tb: '2',\n\tc: '3'\n}")
				.xml("<object><a>1</a><b>2</b><c>3</c></object>")
				.xmlT("<object><a>1</a><b>2</b><c>3</c></object>")
				.xmlR("<object>\n\t<a>1</a>\n\t<b>2</b>\n\t<c>3</c>\n</object>\n")
				.xmlNs("<object><a>1</a><b>2</b><c>3</c></object>")
				.html("<table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>2</td></tr><tr><td>c</td><td>3</td></tr></table>")
				.htmlT("<table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>2</td></tr><tr><td>c</td><td>3</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>2</td>\n\t</tr>\n\t<tr>\n\t\t<td>c</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n")
				.uon("(a='1',b='2',c='3')")
				.uonT("(a='1',b='2',c='3')")
				.uonR("(\n\ta='1',\n\tb='2',\n\tc='3'\n)")
				.urlEnc("a='1'&b='2'&c='3'")
				.urlEncT("a='1'&b='2'&c='3'")
				.urlEncR("a='1'\n&b='2'\n&c='3'")
				.msgPack("83A161A131A162A132A163A133")
				.msgPackT("83A161A131A162A132A163A133")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:b>2</jp:b>\n<jp:c>3</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:b>2</jp:b>\n<jp:c>3</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>1</jp:a>\n    <jp:b>2</jp:b>\n    <jp:c>3</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.sortMaps())
			},
			{ 	/* 5 */
				new ComboInput<>(
					"SERIALIZER_trimEmptyCollections",
					T5.class,
					new T5()
				)
				.json("{}")
				.jsonT("{}")
				.jsonR("{\n}")
				.xml("<object/>")
				.xmlT("<object/>")
				.xmlR("<object/>\n")
				.xmlNs("<object/>")
				.html("<table></table>")
				.htmlT("<table></table>")
				.htmlR("<table>\n</table>\n")
				.uon("()")
				.uonT("()")
				.uonR("(\n)")
				.urlEnc("")
				.urlEncT("")
				.urlEncR("")
				.msgPack("82A2663190A2663290")
				.msgPackT("82A2663190A2663290")
				.rdfXml("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.trimEmptyCollections())
			},
			{ 	/* 6 */
				new ComboInput<>(
					"SERIALIZER_trimEmptyMaps",
					T6.class,
					new T6()
				)
				.json("{}")
				.jsonT("{}")
				.jsonR("{\n}")
				.xml("<object/>")
				.xmlT("<object/>")
				.xmlR("<object/>\n")
				.xmlNs("<object/>")
				.html("<table></table>")
				.htmlT("<table></table>")
				.htmlR("<table>\n</table>\n")
				.uon("()")
				.uonT("()")
				.uonR("(\n)")
				.urlEnc("")
				.urlEncT("")
				.urlEncR("")
				.msgPack("82A2663180A2663280")
				.msgPackT("82A2663180A2663280")
				.rdfXml("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.trimEmptyMaps())
			},
			{ 	/* 7 */
				new ComboInput<>(
					"SERIALIZER_keepNullProperties",
					T7.class,
					new T7()
				)
				.json("{f:null}")
				.jsonT("{f:null}")
				.jsonR("{\n\tf: null\n}")
				.xml("<object><f _type='null'/></object>")
				.xmlT("<object><f t='null'/></object>")
				.xmlR("<object>\n\t<f _type='null'/>\n</object>\n")
				.xmlNs("<object><f _type='null'/></object>")
				.html("<table><tr><td>f</td><td><null/></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><null/></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td><null/></td>\n\t</tr>\n</table>\n")
				.uon("(f=null)")
				.uonT("(f=null)")
				.uonR("(\n\tf=null\n)")
				.urlEnc("f=null")
				.urlEncT("f=null")
				.urlEncR("f=null")
				.msgPack("81A166C0")
				.msgPackT("81A166C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.keepNullProperties())
			},
			{ 	/* 8 */
				new ComboInput<>(
					"SERIALIZER_trimStrings",
					T8.class,
					new T8()
				)
				.json("{f:'foo'}")
				.jsonT("{f:'foo'}")
				.jsonR("{\n\tf: 'foo'\n}")
				.xml("<object><f>foo</f></object>")
				.xmlT("<object><f>foo</f></object>")
				.xmlR("<object>\n\t<f>foo</f>\n</object>\n")
				.xmlNs("<object><f>foo</f></object>")
				.html("<table><tr><td>f</td><td>foo</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>foo</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>foo</td>\n\t</tr>\n</table>\n")
				.uon("(f=foo)")
				.uonT("(f=foo)")
				.uonR("(\n\tf=foo\n)")
				.urlEnc("f=foo")
				.urlEncT("f=foo")
				.urlEncR("f=foo")
				.msgPack("81A166A3666F6F")
				.msgPackT("81A166A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>foo</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>foo</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>foo</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.trimStrings())
			},
			{ 	/* 9 */
				new ComboInput<>(
					"SERIALIZER_uriContext/uriResolution/uriRelativity",
					T9.class,
					new T9()
				)
				.json("{f:'https://localhost:80/context/resource/foo'}")
				.jsonT("{f:'https://localhost:80/context/resource/foo'}")
				.jsonR("{\n\tf: 'https://localhost:80/context/resource/foo'\n}")
				.xml("<object><f>https://localhost:80/context/resource/foo</f></object>")
				.xmlT("<object><f>https://localhost:80/context/resource/foo</f></object>")
				.xmlR("<object>\n\t<f>https://localhost:80/context/resource/foo</f>\n</object>\n")
				.xmlNs("<object><f>https://localhost:80/context/resource/foo</f></object>")
				.html("<table><tr><td>f</td><td><a href='https://localhost:80/context/resource/foo'>foo</a></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><a href='https://localhost:80/context/resource/foo'>foo</a></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td><a href='https://localhost:80/context/resource/foo'>foo</a></td>\n\t</tr>\n</table>\n")
				.uon("(f=https://localhost:80/context/resource/foo)")
				.uonT("(f=https://localhost:80/context/resource/foo)")
				.uonR("(\n\tf=https://localhost:80/context/resource/foo\n)")
				.urlEnc("f=https://localhost:80/context/resource/foo")
				.urlEncT("f=https://localhost:80/context/resource/foo")
				.urlEncR("f=https://localhost:80/context/resource/foo")
				.msgPack("81A166D92968747470733A2F2F6C6F63616C686F73743A38302F636F6E746578742F7265736F757263652F666F6F")
				.msgPackT("81A166D92968747470733A2F2F6C6F63616C686F73743A38302F636F6E746578742F7265736F757263652F666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='https://localhost:80/context/resource/foo'/>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='https://localhost:80/context/resource/foo'/>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:resource='https://localhost:80/context/resource/foo'/>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(Serializer.Builder.class, x -> x.uriContext(UriContext.of("https://localhost:80", "/context", "/resource", "/path")).uriRelativity(UriRelativity.PATH_INFO).uriResolution(UriResolution.ABSOLUTE))
				.convert(x -> new T9())
				.skipTest(x -> x.contains("parseRdf") || x.contains("verifyRdf"))
			},
			{ 	/* 10 */
				new ComboInput<>(
					"WSERIALIZER_maxIndent/WSERIALIZER_useWhitespace",
					T10.class,
					new T10().init()
				)
				.json("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}")
				.jsonT("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}")
				.jsonR("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}")
				.xml("<object>\n\t<f1>1</f1>\n\t<f2>\n\t\t<f1>2</f1>\n\t\t<f2><f1>3</f1></f2>\n\t</f2>\n</object>\n")
				.xmlT("<object>\n\t<f1>1</f1>\n\t<f2>\n\t\t<f1>2</f1>\n\t\t<f2><f1>3</f1></f2>\n\t</f2>\n</object>\n")
				.xmlR("<object>\n\t<f1>1</f1>\n\t<f2>\n\t\t<f1>2</f1>\n\t\t<f2><f1>3</f1></f2>\n\t</f2>\n</object>\n")
				.xmlNs("<object>\n\t<f1>1</f1>\n\t<f2>\n\t\t<f1>2</f1>\n\t\t<f2><f1>3</f1></f2>\n\t</f2>\n</object>\n")
				.html("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2</td>\n\t\t<td><table><tr><td>f1</td><td>2</td></tr><tr><td>f2</td><td><table><tr><td>f1</td><td>3</td></tr></table></td></tr></table>\t\t</td>\n\t</tr>\n</table>\n")
				.htmlT("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2</td>\n\t\t<td><table><tr><td>f1</td><td>2</td></tr><tr><td>f2</td><td><table><tr><td>f1</td><td>3</td></tr></table></td></tr></table>\t\t</td>\n\t</tr>\n</table>\n")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2</td>\n\t\t<td><table><tr><td>f1</td><td>2</td></tr><tr><td>f2</td><td><table><tr><td>f1</td><td>3</td></tr></table></td></tr></table>\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(\n\tf1=1,\n\tf2=(\n\t\tf1=2,\n\t\tf2=(f1=3)\n\t)\n)")
				.uonT("(\n\tf1=1,\n\tf2=(\n\t\tf1=2,\n\t\tf2=(f1=3)\n\t)\n)")
				.uonR("(\n\tf1=1,\n\tf2=(\n\t\tf1=2,\n\t\tf2=(f1=3)\n\t)\n)")
				.urlEnc("f1=1\n&f2=(\n\tf1=2,\n\tf2=(\n\t\tf1=3\n\t)\n)")
				.urlEncT("f1=1\n&f2=(\n\tf1=2,\n\tf2=(\n\t\tf1=3\n\t)\n)")
				.urlEncR("f1=1\n&f2=(\n\tf1=2,\n\tf2=(\n\t\tf1=3\n\t)\n)")
				.msgPack("82A2663101A2663282A2663102A2663281A2663103")
				.msgPackT("82A2663101A2663282A2663102A2663281A2663103")
				.rdfXml("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2 rdf:parseType='Resource'>\n      <jp:f1>2</jp:f1>\n      <jp:f2 rdf:parseType='Resource'>\n        <jp:f1>3</jp:f1>\n      </jp:f2>\n    </jp:f2>\n  </rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2 rdf:parseType='Resource'>\n      <jp:f1>2</jp:f1>\n      <jp:f2 rdf:parseType='Resource'>\n        <jp:f1>3</jp:f1>\n      </jp:f2>\n    </jp:f2>\n  </rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2 rdf:parseType='Resource'>\n      <jp:f1>2</jp:f1>\n      <jp:f2 rdf:parseType='Resource'>\n        <jp:f1>3</jp:f1>\n      </jp:f2>\n    </jp:f2>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(WriterSerializer.Builder.class, x -> x.maxIndent(2).useWhitespace())
			},
			{ 	/* 11 */
				new ComboInput<>(
					"WSERIALIZER_quoteChar",
					T11.class,
					new T11()
				)
				.json("{_type:|T11|,f:[{f:[| foo |]}]}")
				.jsonT("{t:|T11|,f:[{f:[| foo |]}]}")
				.jsonR("{\n\t_type: |T11|,\n\tf: [\n\t\t{\n\t\t\tf: [\n\t\t\t\t| foo |\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<T11><f><T11a><f><string>_x0020_foo_x0020_</string></f></T11a></f></T11>")
				.xmlT("<T11><f><T11a><f><string>_x0020_foo_x0020_</string></f></T11a></f></T11>")
				.xmlR("<T11>\n\t<f>\n\t\t<T11a>\n\t\t\t<f>\n\t\t\t\t<string>_x0020_foo_x0020_</string>\n\t\t\t</f>\n\t\t</T11a>\n\t</f>\n</T11>\n")
				.xmlNs("<T11 xmlns=|http://www.apache.org/2013/Juneau|><f><T11a><f><string>_x0020_foo_x0020_</string></f></T11a></f></T11>")
				.html("<table _type=|T11|><tr><td>f</td><td><table _type=|array|><tr><th>f</th></tr><tr><td><ul><li><sp> </sp>foo<sp> </sp></li></ul></td></tr></table></td></tr></table>")
				.htmlT("<table t=|T11|><tr><td>f</td><td><table t=|array|><tr><th>f</th></tr><tr><td><ul><li><sp> </sp>foo<sp> </sp></li></ul></td></tr></table></td></tr></table>")
				.htmlR("<table _type=|T11|>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table _type=|array|>\n\t\t\t\t<tr>\n\t\t\t\t\t<th>f</th>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li><sp> </sp>foo<sp> </sp></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(_type=T11,f=@((f=@(| foo |))))")
				.uonT("(t=T11,f=@((f=@(| foo |))))")
				.uonR("(\n\t_type=T11,\n\tf=@(\n\t\t(\n\t\t\tf=@(\n\t\t\t\t| foo |\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=T11&f=@((f=@(|+foo+|)))")
				.urlEncT("t=T11&f=@((f=@(|+foo+|)))")
				.urlEncR("_type=T11\n&f=@(\n\t(\n\t\tf=@(\n\t\t\t|+foo+|\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A3543131A1669181A16691A520666F6F20")
				.msgPackT("82A174A3543131A1669181A16691A520666F6F20")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>T11</jp:_type>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f>\n<rdf:Seq>\n<rdf:li>_x0020_foo_x0020_</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>T11</jp:t>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f>\n<rdf:Seq>\n<rdf:li>_x0020_foo_x0020_</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>T11</jp:_type>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f>\n            <rdf:Seq>\n              <rdf:li>_x0020_foo_x0020_</rdf:li>\n            </rdf:Seq>\n          </jp:f>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.apply(XmlSerializer.Builder.class, x -> x.addNamespaceUrisToRoot())
				.apply(WriterSerializer.Builder.class, x -> x.quoteCharOverride('|'))
				.apply(Serializer.Builder.class, x -> x.addBeanTypes().addRootType())
				.skipTest(x -> x.startsWith("parse") || x.startsWith("verify"))
			},
		});
	}

	public SerializerPropertiesComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Bean(typeName="BwT")
	public static class T0 {
		public int f;

		public static T0 create() {
			T0 l = new T0();
			l.f = 1;
			return l;
		}
	}

	public static class T5 {
		public List<String> f1 = alist();
		public String[] f2 = {};
	}

	public static class T6 {
		public Map<String,String> f1 = map();
		public JsonMap f2 = JsonMap.create();
	}

	public static class T7 {
		public String f;
	}

	public static class T8 {
		public String f = " foo ";
	}

	public static class T9 {
		@Uri
		public String f = "foo";
	}

	public static class T10 {
		public int f1;
		public T10 f2;

		public T10 init() {
			T10 x2 = new T10(), x3 = new T10();
			this.f1 = 1;
			x2.f1 = 2;
			x3.f1 = 3;
			this.f2 = x2;
			x2.f2 = x3;
			return this;
		}
	}

	@Bean(typeName="T11")
	public static class T11 {
		public T11a[] f = new T11a[]{new T11a()};
	}

	@Bean(typeName="T11a")
	public static class T11a {
		public String[] f = new String[]{" foo "};
	}
}
