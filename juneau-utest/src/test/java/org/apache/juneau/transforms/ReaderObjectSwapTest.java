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
package org.apache.juneau.transforms;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that Reader and InputStream objects are serialized correctly.
 * Note that these are one-way serializations and you're not guaranteed to produce parsable output.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class ReaderObjectSwapTest extends ComboSerializeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"PojoToSimpleReader",
					PojoToSimpleReader.class,
					new PojoToSimpleReader()
				)
				.json("foo")
				.jsonT("foo")
				.jsonR("foo")
				.xml("foo")
				.xmlT("foo")
				.xmlR("foo\n")
				.xmlNs("foo")
				.html("foo")
				.htmlT("foo")
				.htmlR("foo")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("foo")
				.urlEncT("foo")
				.urlEncR("foo")
				.msgPack("666F6F")
				.msgPackT("666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 1 */
				new ComboInput<>(
					"PojoToDynamicReader",
					PojoToDynamicReader.class,
					new PojoToDynamicReader("foo")
				)
				.json("foo-json5")
				.jsonT("foo-json5")
				.jsonR("foo-json5")
				.xml("foo-xml")
				.xmlT("foo-xml")
				.xmlR("foo-xml\n")
				.xmlNs("foo-xml")
				.html("foo-html")
				.htmlT("foo-html")
				.htmlR("foo-html")
				.uon("foo-uon")
				.uonT("foo-uon")
				.uonR("foo-uon")
				.urlEnc("foo-x-www-form-urlencoded")
				.urlEncT("foo-x-www-form-urlencoded")
				.urlEncR("foo-x-www-form-urlencoded")
				.msgPack("666F6F2D6D73677061636B")
				.msgPackT("666F6F2D6D73677061636B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foo-xml</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foo-xml</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo-xml</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 2 */
				new ComboInput<>(
					"SometimesSwappedBean1",
					SometimesSwappedBean1.class,
					new SometimesSwappedBean1("foo")
				)
				.json("foo-application/json5")
				.jsonT("foo-application/json5")
				.jsonR("foo-application/json5")
				.xml("foo-text/xml")
				.xmlT("foo-text/xml")
				.xmlR("foo-text/xml\n")
				.xmlNs("foo-text/xml")
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
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foo-text/xml+rdf</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foo-text/xml+rdf</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo-text/xml+rdf</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 3 */
				new ComboInput<>(
					"SometimesSwappedBean2",
					SometimesSwappedBean2.class,
					new SometimesSwappedBean2("foo")
				)
				.json("{f:'foo'}")
				.jsonT("{f:'foo'}")
				.jsonR("{\n\tf: 'foo'\n}")
				.xml("<object><f>foo</f></object>")
				.xmlT("<object><f>foo</f></object>")
				.xmlR("<object>\n\t<f>foo</f>\n</object>\n")
				.xmlNs("<object><f>foo</f></object>")
				.html("foo-text/html")
				.htmlT("foo-text/html")
				.htmlR("foo-text/html")
				.uon("foo-text/uon")
				.uonT("foo-text/uon")
				.uonR("foo-text/uon")
				.urlEnc("foo-application/x-www-form-urlencoded")
				.urlEncT("foo-application/x-www-form-urlencoded")
				.urlEncR("foo-application/x-www-form-urlencoded")
				.msgPack("666F6F2D6F6374616C2F6D73677061636B")
				.msgPackT("666F6F2D6F6374616C2F6D73677061636B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>foo</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>foo</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>foo</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 4 */
				new ComboInput<>(
					"BeanWithSwappedField",
					BeanWithSwappedField.class,
					new BeanWithSwappedField("x")
				)
				.json("{f:x-json5}")
				.jsonT("{f:x-json5}")
				.jsonR("{\n\tf: x-json5\n}")
				.xml("<object><f>x-xml</f></object>")
				.xmlT("<object><f>x-xml</f></object>")
				.xmlR("<object>\n\t<f>x-xml</f>\n</object>\n")
				.xmlNs("<object><f>x-xml</f></object>")
				.html("<table><tr><td>f</td><td>x-html</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>x-html</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>x-html</td>\n\t</tr>\n</table>\n")
				.uon("(f=x-uon)")
				.uonT("(f=x-uon)")
				.uonR("(\n\tf=x-uon\n)")
				.urlEnc("f=x-x-www-form-urlencoded")
				.urlEncT("f=x-x-www-form-urlencoded")
				.urlEncR("f=x-x-www-form-urlencoded")
				.msgPack("81A166782D6D73677061636B")
				.msgPackT("81A166782D6D73677061636B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>x-xml</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>x-xml</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>x-xml</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 5 */
				new ComboInput<>(
					"BeanWithSwapped1dField",
					BeanWithSwapped1dField.class,
					new BeanWithSwapped1dField("x")
				)
				.json("{f:[x1-json5,x2-json5,null]}")
				.jsonT("{f:[x1-json5,x2-json5,null]}")
				.jsonR("{\n\tf: [\n\t\tx1-json5,\n\t\tx2-json5,\n\t\tnull\n\t]\n}")
				.xml("<object><f>x1-xmlx2-xml<null/></f></object>")
				.xmlT("<object><f>x1-xmlx2-xml<null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\tx1-xml\n\t\tx2-xml\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f>x1-xmlx2-xml<null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>x1-html</li><li>x2-html</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>x1-html</li><li>x2-html</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>x1-html</li>\n\t\t\t\t<li>x2-html</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(x1-uon,x2-uon,null))")
				.uonT("(f=@(x1-uon,x2-uon,null))")
				.uonR("(\n\tf=@(\n\t\tx1-uon,\n\t\tx2-uon,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(x1-x-www-form-urlencoded,x2-x-www-form-urlencoded,null)")
				.urlEncT("f=@(x1-x-www-form-urlencoded,x2-x-www-form-urlencoded,null)")
				.urlEncR("f=@(\n\tx1-x-www-form-urlencoded,\n\tx2-x-www-form-urlencoded,\n\tnull\n)")
				.msgPack("81A1669378312D6D73677061636B78322D6D73677061636BC0")
				.msgPackT("81A1669378312D6D73677061636B78322D6D73677061636BC0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>x1-xml</rdf:li>\n<rdf:li>x2-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>x1-xml</rdf:li>\n<rdf:li>x2-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>x1-xml</rdf:li>\n        <rdf:li>x2-xml</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 6 */
				new ComboInput<>(
					"BeanWithSwappedNullField",
					BeanWithSwappedNullField.class,
					new BeanWithSwappedNullField()
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
				.msgPack("80")
				.msgPackT("80")
				.rdfXml("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n</rdf:RDF>\n")
			},
			{ 	/* 7 */
				new ComboInput<>(
					"BeanWithSwappedListField",
					BeanWithSwappedListField.class,
					new BeanWithSwappedListField("x")
				)
				.json("{f:[x1-json5,x2-json5,null]}")
				.jsonT("{f:[x1-json5,x2-json5,null]}")
				.jsonR("{\n\tf: [\n\t\tx1-json5,\n\t\tx2-json5,\n\t\tnull\n\t]\n}")
				.xml("<object><f>x1-xmlx2-xml<null/></f></object>")
				.xmlT("<object><f>x1-xmlx2-xml<null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\tx1-xml\n\t\tx2-xml\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f>x1-xmlx2-xml<null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>x1-html</li><li>x2-html</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>x1-html</li><li>x2-html</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>x1-html</li>\n\t\t\t\t<li>x2-html</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(x1-uon,x2-uon,null))")
				.uonT("(f=@(x1-uon,x2-uon,null))")
				.uonR("(\n\tf=@(\n\t\tx1-uon,\n\t\tx2-uon,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(x1-x-www-form-urlencoded,x2-x-www-form-urlencoded,null)")
				.urlEncT("f=@(x1-x-www-form-urlencoded,x2-x-www-form-urlencoded,null)")
				.urlEncR("f=@(\n\tx1-x-www-form-urlencoded,\n\tx2-x-www-form-urlencoded,\n\tnull\n)")
				.msgPack("81A1669378312D6D73677061636B78322D6D73677061636BC0")
				.msgPackT("81A1669378312D6D73677061636B78322D6D73677061636BC0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>x1-xml</rdf:li>\n<rdf:li>x2-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>x1-xml</rdf:li>\n<rdf:li>x2-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>x1-xml</rdf:li>\n        <rdf:li>x2-xml</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 8 */
				new ComboInput<>(
					"BeanWithSwappedMapField",
					BeanWithSwappedMapField.class,
					new BeanWithSwappedMapField("x")
				)
				.json("{f:{foo:x1-json5,bar:null,null:x2-json5}}")
				.jsonT("{f:{foo:x1-json5,bar:null,null:x2-json5}}")
				.jsonR("{\n\tf: {\n\t\tfoo: x1-json5,\n\t\tbar: null,\n\t\tnull: x2-json5\n\t}\n}")
				.xml("<object><f><foo>x1-xml</foo><bar _type='null'/><_x0000_>x2-xml</_x0000_></f></object>")
				.xmlT("<object><f><foo>x1-xml</foo><bar t='null'/><_x0000_>x2-xml</_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>x1-xml</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>x2-xml</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo>x1-xml</foo><bar _type='null'/><_x0000_>x2-xml</_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td>x1-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x2-html</td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td>x1-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x2-html</td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>x1-html</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>x2-html</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=x1-uon,bar=null,null=x2-uon))")
				.uonT("(f=(foo=x1-uon,bar=null,null=x2-uon))")
				.uonR("(\n\tf=(\n\t\tfoo=x1-uon,\n\t\tbar=null,\n\t\tnull=x2-uon\n\t)\n)")
				.urlEnc("f=(foo=x1-x-www-form-urlencoded,bar=null,null=x2-x-www-form-urlencoded)")
				.urlEncT("f=(foo=x1-x-www-form-urlencoded,bar=null,null=x2-x-www-form-urlencoded)")
				.urlEncR("f=(\n\tfoo=x1-x-www-form-urlencoded,\n\tbar=null,\n\tnull=x2-x-www-form-urlencoded\n)")
				.msgPack("81A16683A3666F6F78312D6D73677061636BA3626172C0C078322D6D73677061636B")
				.msgPackT("81A16683A3666F6F78312D6D73677061636BA3626172C0C078322D6D73677061636B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>x1-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x2-xml</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>x1-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x2-xml</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo>x1-xml</jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_>x2-xml</jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 9 */
				new ComboInput<>(
					"BeanWithListBeanSwappedField",
					BeanWithListBeanSwappedField.class,
					new BeanWithListBeanSwappedField("x")
				)
				.json("{f:[{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}},null]}")
				.jsonT("{f:[{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}},null]}")
				.jsonR("{\n\tf: [\n\t\t{\n\t\t\tf1: x1a-json5,\n\t\t\tf2: [\n\t\t\t\tx2a-json5,\n\t\t\t\tx2b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf4: [\n\t\t\t\tx4a-json5,\n\t\t\t\tx4b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: x5a-json5,\n\t\t\t\tbar: null,\n\t\t\t\tnull: x5c-json5\n\t\t\t}\n\t\t},\n\t\tnull\n\t]\n}")
				.xml("<object><f><object><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></object><null/></f></object>")
				.xmlT("<object><f><object><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar t='null'/><_x0000_>x5c-xml</_x0000_></f5></object><null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<object>\n\t\t\t<f1>x1a-xml</f1>\n\t\t\t<f2>\n\t\t\t\tx2a-xml\n\t\t\t\tx2b-xml\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f4>\n\t\t\t\tx4a-xml\n\t\t\t\tx4b-xml\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>x5a-xml</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>x5c-xml</_x0000_>\n\t\t\t</f5>\n\t\t</object>\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><object><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></object><null/></f></object>")
				.html("<table><tr><td>f</td><td><table _type='array'><tr><th>f1</th><th>f2</th><th>f3</th><th>f4</th><th>f5</th></tr><tr><td>x1a-html</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td><td><null/></td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr><tr><null/></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table t='array'><tr><th>f1</th><th>f2</th><th>f3</th><th>f4</th><th>f5</th></tr><tr><td>x1a-html</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td><td><null/></td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr><tr><null/></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table _type='array'>\n\t\t\t\t<tr>\n\t\t\t\t\t<th>f1</th>\n\t\t\t\t\t<th>f2</th>\n\t\t\t\t\t<th>f3</th>\n\t\t\t\t\t<th>f4</th>\n\t\t\t\t\t<th>f5</th>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>x1a-html</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>x2a-html</li>\n\t\t\t\t\t\t\t<li>x2b-html</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>x4a-html</li>\n\t\t\t\t\t\t\t<li>x4b-html</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t<td>x5a-html</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t<td>x5c-html</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<null/>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@((f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon)),null))")
				.uonT("(f=@((f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon)),null))")
				.uonR("(\n\tf=@(\n\t\t(\n\t\t\tf1=x1a-uon,\n\t\t\tf2=@(\n\t\t\t\tx2a-uon,\n\t\t\t\tx2b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf4=@(\n\t\t\t\tx4a-uon,\n\t\t\t\tx4b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=x5a-uon,\n\t\t\t\tbar=null,\n\t\t\t\tnull=x5c-uon\n\t\t\t)\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("f=@((f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)),null)")
				.urlEncT("f=@((f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)),null)")
				.urlEncR("f=@(\n\t(\n\t\tf1=x1a-x-www-form-urlencoded,\n\t\tf2=@(\n\t\t\tx2a-x-www-form-urlencoded,\n\t\t\tx2b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf4=@(\n\t\t\tx4a-x-www-form-urlencoded,\n\t\t\tx4b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=x5a-x-www-form-urlencoded,\n\t\t\tbar=null,\n\t\t\tnull=x5c-x-www-form-urlencoded\n\t\t)\n\t),\n\tnull\n)")
				.msgPack("81A1669284A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636BC0")
				.msgPackT("81A1669284A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636BC0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f1>x1a-xml</jp:f1>\n          <jp:f2>\n            <rdf:Seq>\n              <rdf:li>x2a-xml</rdf:li>\n              <rdf:li>x2b-xml</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f2>\n          <jp:f4>\n            <rdf:Seq>\n              <rdf:li>x4a-xml</rdf:li>\n              <rdf:li>x4b-xml</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f4>\n          <jp:f5 rdf:parseType='Resource'>\n            <jp:foo>x5a-xml</jp:foo>\n            <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            <jp:_x0000_>x5c-xml</jp:_x0000_>\n          </jp:f5>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 10 */
				new ComboInput<>(
					"BeanWithMapBeanSwappedField",
					BeanWithMapBeanSwappedField.class,
					new BeanWithMapBeanSwappedField("x")
				)
				.json("{f:{foo:{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}},bar:null,null:{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}}}}")
				.jsonT("{f:{foo:{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}},bar:null,null:{f1:x1a-json5,f2:[x2a-json5,x2b-json5,null],f4:[x4a-json5,x4b-json5,null],f5:{foo:x5a-json5,bar:null,null:x5c-json5}}}}")
				.jsonR("{\n\tf: {\n\t\tfoo: {\n\t\t\tf1: x1a-json5,\n\t\t\tf2: [\n\t\t\t\tx2a-json5,\n\t\t\t\tx2b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf4: [\n\t\t\t\tx4a-json5,\n\t\t\t\tx4b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: x5a-json5,\n\t\t\t\tbar: null,\n\t\t\t\tnull: x5c-json5\n\t\t\t}\n\t\t},\n\t\tbar: null,\n\t\tnull: {\n\t\t\tf1: x1a-json5,\n\t\t\tf2: [\n\t\t\t\tx2a-json5,\n\t\t\t\tx2b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf4: [\n\t\t\t\tx4a-json5,\n\t\t\t\tx4b-json5,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: x5a-json5,\n\t\t\t\tbar: null,\n\t\t\t\tnull: x5c-json5\n\t\t\t}\n\t\t}\n\t}\n}")
				.xml("<object><f><foo><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></_x0000_></f></object>")
				.xmlT("<object><f><foo><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar t='null'/><_x0000_>x5c-xml</_x0000_></f5></foo><bar t='null'/><_x0000_><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar t='null'/><_x0000_>x5c-xml</_x0000_></f5></_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>\n\t\t\t<f1>x1a-xml</f1>\n\t\t\t<f2>\n\t\t\t\tx2a-xml\n\t\t\t\tx2b-xml\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f4>\n\t\t\t\tx4a-xml\n\t\t\t\tx4b-xml\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>x5a-xml</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>x5c-xml</_x0000_>\n\t\t\t</f5>\n\t\t</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>\n\t\t\t<f1>x1a-xml</f1>\n\t\t\t<f2>\n\t\t\t\tx2a-xml\n\t\t\t\tx2b-xml\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f4>\n\t\t\t\tx4a-xml\n\t\t\t\tx4b-xml\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>x5a-xml</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>x5c-xml</_x0000_>\n\t\t\t</f5>\n\t\t</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>x1a-xml</f1><f2>x2a-xmlx2b-xml<null/></f2><f4>x4a-xmlx4b-xml<null/></f4><f5><foo>x5a-xml</foo><bar _type='null'/><_x0000_>x5c-xml</_x0000_></f5></_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>x1a-html</td></tr><tr><td>f2</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td></tr><tr><td>f4</td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>x1a-html</td></tr><tr><td>f2</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td></tr><tr><td>f4</td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>x1a-html</td></tr><tr><td>f2</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td></tr><tr><td>f4</td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>x1a-html</td></tr><tr><td>f2</td><td><ul><li>x2a-html</li><li>x2b-html</li><li><null/></li></ul></td></tr><tr><td>f4</td><td><ul><li>x4a-html</li><li>x4b-html</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>x5a-html</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>x5c-html</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>x1a-html</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>x2a-html</li>\n\t\t\t\t\t\t\t\t\t\t<li>x2b-html</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>x4a-html</li>\n\t\t\t\t\t\t\t\t\t\t<li>x4b-html</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>x5a-html</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>x5c-html</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>x1a-html</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>x2a-html</li>\n\t\t\t\t\t\t\t\t\t\t<li>x2b-html</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>x4a-html</li>\n\t\t\t\t\t\t\t\t\t\t<li>x4b-html</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>x5a-html</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>x5c-html</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=(f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon)),bar=null,null=(f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon))))")
				.uonT("(f=(foo=(f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon)),bar=null,null=(f1=x1a-uon,f2=@(x2a-uon,x2b-uon,null),f4=@(x4a-uon,x4b-uon,null),f5=(foo=x5a-uon,bar=null,null=x5c-uon))))")
				.uonR("(\n\tf=(\n\t\tfoo=(\n\t\t\tf1=x1a-uon,\n\t\t\tf2=@(\n\t\t\t\tx2a-uon,\n\t\t\t\tx2b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf4=@(\n\t\t\t\tx4a-uon,\n\t\t\t\tx4b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=x5a-uon,\n\t\t\t\tbar=null,\n\t\t\t\tnull=x5c-uon\n\t\t\t)\n\t\t),\n\t\tbar=null,\n\t\tnull=(\n\t\t\tf1=x1a-uon,\n\t\t\tf2=@(\n\t\t\t\tx2a-uon,\n\t\t\t\tx2b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf4=@(\n\t\t\t\tx4a-uon,\n\t\t\t\tx4b-uon,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=x5a-uon,\n\t\t\t\tbar=null,\n\t\t\t\tnull=x5c-uon\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("f=(foo=(f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)),bar=null,null=(f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)))")
				.urlEncT("f=(foo=(f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)),bar=null,null=(f1=x1a-x-www-form-urlencoded,f2=@(x2a-x-www-form-urlencoded,x2b-x-www-form-urlencoded,null),f4=@(x4a-x-www-form-urlencoded,x4b-x-www-form-urlencoded,null),f5=(foo=x5a-x-www-form-urlencoded,bar=null,null=x5c-x-www-form-urlencoded)))")
				.urlEncR("f=(\n\tfoo=(\n\t\tf1=x1a-x-www-form-urlencoded,\n\t\tf2=@(\n\t\t\tx2a-x-www-form-urlencoded,\n\t\t\tx2b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf4=@(\n\t\t\tx4a-x-www-form-urlencoded,\n\t\t\tx4b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=x5a-x-www-form-urlencoded,\n\t\t\tbar=null,\n\t\t\tnull=x5c-x-www-form-urlencoded\n\t\t)\n\t),\n\tbar=null,\n\tnull=(\n\t\tf1=x1a-x-www-form-urlencoded,\n\t\tf2=@(\n\t\t\tx2a-x-www-form-urlencoded,\n\t\t\tx2b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf4=@(\n\t\t\tx4a-x-www-form-urlencoded,\n\t\t\tx4b-x-www-form-urlencoded,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=x5a-x-www-form-urlencoded,\n\t\t\tbar=null,\n\t\t\tnull=x5c-x-www-form-urlencoded\n\t\t)\n\t)\n)")
				.msgPack("81A16683A3666F6F84A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636BA3626172C0C084A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636B")
				.msgPackT("81A16683A3666F6F84A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636BA3626172C0C084A266317831612D6D73677061636BA26632937832612D6D73677061636B7832622D6D73677061636BC0A26634937834612D6D73677061636B7834622D6D73677061636BC0A2663583A3666F6F7835612D6D73677061636BA3626172C0C07835632D6D73677061636B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>x1a-xml</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>x2a-xml</rdf:li>\n<rdf:li>x2b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>x4a-xml</rdf:li>\n<rdf:li>x4b-xml</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>x5a-xml</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>x5c-xml</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo rdf:parseType='Resource'>\n        <jp:f1>x1a-xml</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>x2a-xml</rdf:li>\n            <rdf:li>x2b-xml</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>x4a-xml</rdf:li>\n            <rdf:li>x4b-xml</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>x5a-xml</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>x5c-xml</jp:_x0000_>\n        </jp:f5>\n      </jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_ rdf:parseType='Resource'>\n        <jp:f1>x1a-xml</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>x2a-xml</rdf:li>\n            <rdf:li>x2b-xml</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>x4a-xml</rdf:li>\n            <rdf:li>x4b-xml</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>x5a-xml</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>x5c-xml</jp:_x0000_>\n        </jp:f5>\n      </jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
		});
	}

	public ReaderObjectSwapTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s;
	}

	@Swap(PojoToSimpleReaderSwap.class)
	public static class PojoToSimpleReader {}

	public static class PojoToSimpleReaderSwap extends ObjectSwap<PojoToSimpleReader,Reader> {
		@Override
		public Reader swap(BeanSession session, PojoToSimpleReader o) throws Exception {
			return reader("foo");
		}
	}

	@Swap(PojoToDynamicReaderSwap.class)
	public static class PojoToDynamicReader {
		private String f;
		public PojoToDynamicReader(String f) {
			this.f = f;
		}
	}

	public static class PojoToDynamicReaderSwap extends ObjectSwap<PojoToDynamicReader,Object> {
		@Override
		public Object swap(BeanSession session, PojoToDynamicReader o) throws Exception {
			return reader(o.f + "-" + session.getMediaType().getSubTypes().get(0));
		}
	}

	@Swap(SometimesSwappedBeanSwap1.class)
	public static class SometimesSwappedBean1 {
		public String f;
		public SometimesSwappedBean1(String f) {
			this.f = f;
		}
	}

	public static class SometimesSwappedBeanSwap1 extends ObjectSwap<SometimesSwappedBean1,Object> {
		@Override
		public Object swap(BeanSession session, SometimesSwappedBean1 o) throws Exception {
			MediaType mt = session.getMediaType();
			if (mt.hasSubType("json5") || mt.hasSubType("xml"))
				return reader(o.f + "-" + mt);
			return o;
		}
	}

	@Swap(SometimesSwappedBeanSwap2.class)
	public static class SometimesSwappedBean2 {
		public String f;
		public SometimesSwappedBean2(String f) {
			this.f = f;
		}
	}

	public static class SometimesSwappedBeanSwap2 extends ObjectSwap<SometimesSwappedBean2,Object> {
		@Override
		public Object swap(BeanSession session, SometimesSwappedBean2 o) throws Exception {
			MediaType mt = session.getMediaType();
			if (mt.hasSubType("json5") || mt.hasSubType("xml"))
				return o;
			return reader(o.f + "-" + mt);
		}
	}


	public static class BeanWithSwappedField {
		public PojoToDynamicReader f;
		public BeanWithSwappedField(String f) {
			this.f = new PojoToDynamicReader(f);
		}
	}

	public static class BeanWithSwapped1dField {
		public PojoToDynamicReader[] f;
		public BeanWithSwapped1dField(String f) {
			this.f = new PojoToDynamicReader[]{new PojoToDynamicReader(f + "1"),new PojoToDynamicReader(f + 2),null};
		}
	}

	public static class BeanWithSwappedNullField {
		public PojoToDynamicReader f;
	}

	public static class BeanWithSwappedListField {
		public List<PojoToDynamicReader> f;
		public BeanWithSwappedListField(String f) {
			this.f = list(new PojoToDynamicReader(f + "1"),new PojoToDynamicReader(f + "2"),null);
		}
	}

	public static class BeanWithSwappedMapField {
		public Map<String,PojoToDynamicReader> f;
		public BeanWithSwappedMapField(String f) {
			this.f = map("foo",new PojoToDynamicReader(f + "1"),"bar",null,null,new PojoToDynamicReader(f + "2"));
		}
	}

	public static class BeanWithListBeanSwappedField {
		public List<B> f;
		public BeanWithListBeanSwappedField(String f) {
			this.f = list(new B(f),null);
		}
	}

	public static class BeanWithMapBeanSwappedField {
		public Map<String,B> f;
		public BeanWithMapBeanSwappedField(String f) {
			this.f = map("foo",new B(f),"bar",null,null,new B(f));
		}
	}

	public static class B {
		public PojoToDynamicReader f1;
		public PojoToDynamicReader[] f2;
		public PojoToDynamicReader f3;
		public List<PojoToDynamicReader> f4;
		public Map<String,PojoToDynamicReader> f5;

		public B(String f) {
			f1 = new PojoToDynamicReader(f + "1a");
			f2 = new PojoToDynamicReader[]{new PojoToDynamicReader(f + "2a"),new PojoToDynamicReader(f + "2b"),null};
			f3 = null;
			f4 = list(new PojoToDynamicReader(f + "4a"),new PojoToDynamicReader(f + "4b"),null);
			f5 = map("foo",new PojoToDynamicReader(f + "5a"),"bar",null,null,new PojoToDynamicReader(f + "5c"));
		}
	}
}
