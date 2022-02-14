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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that Reader and InputStream objects are serialized correctly.
 * Note that these are one-way serializations and you're not guaranteed to produce parsable output.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class ReaderObjectComboTest extends ComboSerializeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<Reader>(
					"SimpleReader",
					Reader.class,
					() -> reader("foobar")
				)
				.json("foobar")
				.jsonT("foobar")
				.jsonR("foobar")
				.xml("foobar")
				.xmlT("foobar")
				.xmlR("foobar\n")
				.xmlNs("foobar")
				.html("foobar")
				.htmlT("foobar")
				.htmlR("foobar")
				.uon("foobar")
				.uonT("foobar")
				.uonR("foobar")
				.urlEnc("foobar")
				.urlEncT("foobar")
				.urlEncR("foobar")
				.msgPack("666F6F626172")
				.msgPackT("666F6F626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foobar</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 1 */
				new ComboInput<InputStream>(
					"SimpleInputStream",
					InputStream.class,
					() -> inputStream("foobar")
				)
				.json("foobar")
				.jsonT("foobar")
				.jsonR("foobar")
				.xml("foobar")
				.xmlT("foobar")
				.xmlR("foobar\n")
				.xmlNs("foobar")
				.html("foobar")
				.htmlT("foobar")
				.htmlR("foobar")
				.uon("foobar")
				.uonT("foobar")
				.uonR("foobar")
				.urlEnc("foobar")
				.urlEncT("foobar")
				.urlEncR("foobar")
				.msgPack("666F6F626172")
				.msgPackT("666F6F626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foobar</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 2 */
				new ComboInput<BeanWithReaderField>(
					"BeanWithReaderField",
					BeanWithReaderField.class,
					() -> new BeanWithReaderField().init()
				)
				.json("{f:fv}")
				.jsonT("{f:fv}")
				.jsonR("{\n\tf: fv\n}")
				.xml("<object><f>fv</f></object>")
				.xmlT("<object><f>fv</f></object>")
				.xmlR("<object>\n\t<f>fv</f>\n</object>\n")
				.xmlNs("<object><f>fv</f></object>")
				.html("<table><tr><td>f</td><td>fv</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>fv</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>fv</td>\n\t</tr>\n</table>\n")
				.uon("(f=fv)")
				.uonT("(f=fv)")
				.uonR("(\n\tf=fv\n)")
				.urlEnc("f=fv")
				.urlEncT("f=fv")
				.urlEncR("f=fv")
				.msgPack("81A1666676")
				.msgPackT("81A1666676")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>fv</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>fv</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>fv</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 3 */
				new ComboInput<BeanWithReader1dField>(
					"BeanWithReader1dField",
					BeanWithReader1dField.class,
					() -> new BeanWithReader1dField().init()
				)
				.json("{f:[fv1,fv2,null]}")
				.jsonT("{f:[fv1,fv2,null]}")
				.jsonR("{\n\tf: [\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t]\n}")
				.xml("<object><f>fv1fv2<null/></f></object>")
				.xmlT("<object><f>fv1fv2<null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\tfv1\n\t\tfv2\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f>fv1fv2<null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>fv1</li>\n\t\t\t\t<li>fv2</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(fv1,fv2,null))")
				.uonT("(f=@(fv1,fv2,null))")
				.uonR("(\n\tf=@(\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(fv1,fv2,null)")
				.urlEncT("f=@(fv1,fv2,null)")
				.urlEncR("f=@(\n\tfv1,\n\tfv2,\n\tnull\n)")
				.msgPack("81A16693667631667632C0")
				.msgPackT("81A16693667631667632C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>fv1</rdf:li>\n        <rdf:li>fv2</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 4 */
				new ComboInput<BeanWithReaderNullField>(
					"BeanWithReaderNullField",
					BeanWithReaderNullField.class,
					() -> new BeanWithReaderNullField().init()
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
			},
			{ 	/* 5 */
				new ComboInput<BeanWithReaderListField>(
					"BeanWithReaderListField",
					BeanWithReaderListField.class,
					() -> new BeanWithReaderListField().init()
				)
				.json("{f:[fv1,fv2,null]}")
				.jsonT("{f:[fv1,fv2,null]}")
				.jsonR("{\n\tf: [\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t]\n}")
				.xml("<object><f>fv1fv2<null/></f></object>")
				.xmlT("<object><f>fv1fv2<null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\tfv1\n\t\tfv2\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f>fv1fv2<null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>fv1</li>\n\t\t\t\t<li>fv2</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(fv1,fv2,null))")
				.uonT("(f=@(fv1,fv2,null))")
				.uonR("(\n\tf=@(\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(fv1,fv2,null)")
				.urlEncT("f=@(fv1,fv2,null)")
				.urlEncR("f=@(\n\tfv1,\n\tfv2,\n\tnull\n)")
				.msgPack("81A16693667631667632C0")
				.msgPackT("81A16693667631667632C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>fv1</rdf:li>\n        <rdf:li>fv2</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 6 */
				new ComboInput<BeanWithReaderMapField>(
					"BeanWithReaderMapField",
					BeanWithReaderMapField.class,
					() -> new BeanWithReaderMapField().init()
				)
				.json("{f:{foo:fv1,bar:null,null:fv2}}")
				.jsonT("{f:{foo:fv1,bar:null,null:fv2}}")
				.jsonR("{\n\tf: {\n\t\tfoo: fv1,\n\t\tbar: null,\n\t\tnull: fv2\n\t}\n}")
				.xml("<object><f><foo>fv1</foo><bar _type='null'/><_x0000_>fv2</_x0000_></f></object>")
				.xmlT("<object><f><foo>fv1</foo><bar t='null'/><_x0000_>fv2</_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>fv1</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>fv2</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo>fv1</foo><bar _type='null'/><_x0000_>fv2</_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td>fv1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>fv2</td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td>fv1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>fv2</td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>fv1</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>fv2</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=fv1,bar=null,null=fv2))")
				.uonT("(f=(foo=fv1,bar=null,null=fv2))")
				.uonR("(\n\tf=(\n\t\tfoo=fv1,\n\t\tbar=null,\n\t\tnull=fv2\n\t)\n)")
				.urlEnc("f=(foo=fv1,bar=null,null=fv2)")
				.urlEncT("f=(foo=fv1,bar=null,null=fv2)")
				.urlEncR("f=(\n\tfoo=fv1,\n\tbar=null,\n\tnull=fv2\n)")
				.msgPack("81A16683A3666F6F667631A3626172C0C0667632")
				.msgPackT("81A16683A3666F6F667631A3626172C0C0667632")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>fv1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>fv2</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>fv1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>fv2</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo>fv1</jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_>fv2</jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 7 */
				new ComboInput<BeanWithReaderBeanListField>(
					"BeanWithReaderBeanListField",
					BeanWithReaderBeanListField.class,
					() -> new BeanWithReaderBeanListField().init()
				)
				.json("{f:[{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},null]}")
				.jsonT("{f:[{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},null]}")
				.jsonR("{\n\tf: [\n\t\t{\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t},\n\t\tnull\n\t]\n}")
				.xml("<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>")
				.xmlT("<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<object>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</object>\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<table>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t</table>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null))")
				.uonT("(f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null))")
				.uonR("(\n\tf=@(\n\t\t(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null)")
				.urlEncT("f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null)")
				.urlEncR("f=@(\n\t(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t),\n\tnull\n)")
				.msgPack("81A1669285A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632C0")
				.msgPackT("81A1669285A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f1>f1v</jp:f1>\n          <jp:f2>\n            <rdf:Seq>\n              <rdf:li>f2v1</rdf:li>\n              <rdf:li>f2v2</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f2>\n          <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:f4>\n            <rdf:Seq>\n              <rdf:li>f4v1</rdf:li>\n              <rdf:li>f4v2</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f4>\n          <jp:f5 rdf:parseType='Resource'>\n            <jp:foo>f5v1</jp:foo>\n            <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            <jp:_x0000_>f5v2</jp:_x0000_>\n          </jp:f5>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 8 */
				new ComboInput<BeanWithReaderBeanMapField>(
					"BeanWithReaderBeanMapField",
					BeanWithReaderBeanMapField.class,
					() -> new BeanWithReaderBeanMapField().init()
				)
				.json("{f:{foo:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},bar:null,null:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}}}}")
				.jsonT("{f:{foo:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},bar:null,null:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}}}}")
				.jsonR("{\n\tf: {\n\t\tfoo: {\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t},\n\t\tbar: null,\n\t\tnull: {\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t}\n\t}\n}")
				.xml("<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>")
				.xmlT("<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar t='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2))))")
				.uonT("(f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2))))")
				.uonR("(\n\tf=(\n\t\tfoo=(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t),\n\t\tbar=null,\n\t\tnull=(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)))")
				.urlEncT("f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)))")
				.urlEncR("f=(\n\tfoo=(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t),\n\tbar=null,\n\tnull=(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t)\n)")
				.msgPack("81A16683A3666F6F85A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632A3626172C0C085A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632")
				.msgPackT("81A16683A3666F6F85A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632A3626172C0C085A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo rdf:parseType='Resource'>\n        <jp:f1>f1v</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>f2v1</rdf:li>\n            <rdf:li>f2v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>f4v1</rdf:li>\n            <rdf:li>f4v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>f5v1</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>f5v2</jp:_x0000_>\n        </jp:f5>\n      </jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_ rdf:parseType='Resource'>\n        <jp:f1>f1v</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>f2v1</rdf:li>\n            <rdf:li>f2v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>f4v1</rdf:li>\n            <rdf:li>f4v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>f5v1</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>f5v2</jp:_x0000_>\n        </jp:f5>\n      </jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			}
		});
	}

	public ReaderObjectComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.copy().keepNullProperties().build();
	}

	public static class BeanWithReaderField {
		public Reader f;
		public BeanWithReaderField init() {
			f = reader("fv");
			return this;
		}
	}

	public static class BeanWithReader1dField {
		public Reader[] f;
		public BeanWithReader1dField init() {
			f = new Reader[]{reader("fv1"),reader("fv2"),null};
			return this;
		}
	}

	public static class BeanWithReaderNullField {
		public Reader f;
		public BeanWithReaderNullField init() {
			f = null;
			return this;
		}
	}

	public static class BeanWithReaderListField {
		public List<Reader> f;
		public BeanWithReaderListField init() {
			f = list(reader("fv1"),reader("fv2"),null);
			return this;
		}
	}

	public static class BeanWithReaderMapField {
		public Map<String,Reader> f;
		public BeanWithReaderMapField init() {
			f = map("foo",reader("fv1"),"bar",null,null,reader("fv2"));
			return this;
		}
	}

	public static class BeanWithReaderBeanListField {
		public List<B> f;
		public BeanWithReaderBeanListField init() {
			f = list(new B().init(),null);
			return this;
		}
	}

	public static class BeanWithReaderBeanMapField {
		public Map<String,B> f;
		public BeanWithReaderBeanMapField init() {
			f = map("foo",new B().init(),"bar",null,null,new B().init());
			return this;
		}
	}

	public static class B {
		public Reader f1;
		public Reader[] f2;
		public Reader f3;
		public List<Reader> f4;
		public Map<String,Reader> f5;

		public B init() {
			f1 = reader("f1v");
			f2 = new Reader[]{reader("f2v1"),reader("f2v2"),null};
			f3 = null;
			f4 = list(reader("f4v1"),reader("f4v2"),null)
			;
			f5 = map("foo",reader("f5v1"),"bar",null,null,reader("f5v2"));
			return this;
		}
	}
}
