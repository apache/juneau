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

import static org.apache.juneau.assertions.Verify.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swaps.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({})
public class ByteArrayBase64SwapComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<byte[]>(
					"ByteArray1d",
					byte[].class,
					new byte[] {1,2,3}
				)
				.json("'AQID'")
				.jsonT("'AQID'")
				.jsonR("'AQID'")
				.xml("<string>AQID</string>")
				.xmlT("<string>AQID</string>")
				.xmlR("<string>AQID</string>\n")
				.xmlNs("<string>AQID</string>")
				.html("<string>AQID</string>")
				.htmlT("<string>AQID</string>")
				.htmlR("<string>AQID</string>")
				.uon("AQID")
				.uonT("AQID")
				.uonR("AQID")
				.urlEnc("_value=AQID")
				.urlEncT("_value=AQID")
				.urlEncR("_value=AQID")
				.msgPack("A441514944")
				.msgPackT("A441514944")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>AQID</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>AQID</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>AQID</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(byte[].class))
			},
			{ 	/* 1 */
				new ComboInput<byte[][]>(
					"ByteArray2d",
					byte[][].class,
					new byte[][]{{1,2,3},{4,5,6},null}
				)
				.json("['AQID','BAUG',null]")
				.jsonT("['AQID','BAUG',null]")
				.jsonR("[\n\t'AQID',\n\t'BAUG',\n\tnull\n]")
				.xml("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.xmlT("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.xmlR("<array>\n\t<string>AQID</string>\n\t<string>BAUG</string>\n\t<null/>\n</array>\n")
				.xmlNs("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.html("<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>")
				.htmlT("<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>AQID</li>\n\t<li>BAUG</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(AQID,BAUG,null)")
				.uonT("@(AQID,BAUG,null)")
				.uonR("@(\n\tAQID,\n\tBAUG,\n\tnull\n)")
				.urlEnc("0=AQID&1=BAUG&2=null")
				.urlEncT("0=AQID&1=BAUG&2=null")
				.urlEncR("0=AQID\n&1=BAUG\n&2=null")
				.msgPack("93A441514944A442415547C0")
				.msgPackT("93A441514944A442415547C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>AQID</rdf:li>\n    <rdf:li>BAUG</rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(byte[][].class))
			},
			{ 	/* 2 */
				new ComboInput<List<byte[]>>(
					"ListOfByteArrays",
					getType(List.class,byte[].class),
					list(new byte[]{1,2,3},new byte[]{4,5,6},null)
				)
				.json("['AQID','BAUG',null]")
				.jsonT("['AQID','BAUG',null]")
				.jsonR("[\n\t'AQID',\n\t'BAUG',\n\tnull\n]")
				.xml("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.xmlT("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.xmlR("<array>\n\t<string>AQID</string>\n\t<string>BAUG</string>\n\t<null/>\n</array>\n")
				.xmlNs("<array><string>AQID</string><string>BAUG</string><null/></array>")
				.html("<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>")
				.htmlT("<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>AQID</li>\n\t<li>BAUG</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(AQID,BAUG,null)")
				.uonT("@(AQID,BAUG,null)")
				.uonR("@(\n\tAQID,\n\tBAUG,\n\tnull\n)")
				.urlEnc("0=AQID&1=BAUG&2=null")
				.urlEncT("0=AQID&1=BAUG&2=null")
				.urlEncR("0=AQID\n&1=BAUG\n&2=null")
				.msgPack("93A441514944A442415547C0")
				.msgPackT("93A441514944A442415547C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>AQID</rdf:li>\n    <rdf:li>BAUG</rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(List.class))
				.verify(x -> verify(x.get(0)).isType(byte[].class))
			},
			{ 	/* 3 */
				new ComboInput<Map<String,byte[]>>(
					"MapOfByteArrays",
					getType(Map.class,String.class,byte[].class),
					mapBuilder(String.class,byte[].class).add("foo",new byte[]{1,2,3}).add("bar",null).add(null,new byte[]{4,5,6}).add("null",new byte[]{7,8,9}).build()
				)
				.json("{foo:'AQID',bar:null,null:'BAUG','null':'BwgJ'}")
				.jsonT("{foo:'AQID',bar:null,null:'BAUG','null':'BwgJ'}")
				.jsonR("{\n\tfoo: 'AQID',\n\tbar: null,\n\tnull: 'BAUG',\n\t'null': 'BwgJ'\n}")
				.xml("<object><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>")
				.xmlT("<object><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>")
				.xmlR("<object>\n\t<foo>AQID</foo>\n\t<bar _type='null'/>\n\t<_x0000_>BAUG</_x0000_>\n\t<null>BwgJ</null>\n</object>\n")
				.xmlNs("<object><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>")
				.html("<table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr><tr><td>null</td><td>BwgJ</td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr><tr><td>null</td><td>BwgJ</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>AQID</td>\n\t</tr>\n\t<tr>\n\t\t<td>bar</td>\n\t\t<td><null/></td>\n\t</tr>\n\t<tr>\n\t\t<td><null/></td>\n\t\t<td>BAUG</td>\n\t</tr>\n\t<tr>\n\t\t<td>null</td>\n\t\t<td>BwgJ</td>\n\t</tr>\n</table>\n")
				.uon("(foo=AQID,bar=null,null=BAUG,'null'=BwgJ)")
				.uonT("(foo=AQID,bar=null,null=BAUG,'null'=BwgJ)")
				.uonR("(\n\tfoo=AQID,\n\tbar=null,\n\tnull=BAUG,\n\t'null'=BwgJ\n)")
				.urlEnc("foo=AQID&bar=null&null=BAUG&'null'=BwgJ")
				.urlEncT("foo=AQID&bar=null&null=BAUG&'null'=BwgJ")
				.urlEncR("foo=AQID\n&bar=null\n&null=BAUG\n&'null'=BwgJ")
				.msgPack("84A3666F6FA441514944A3626172C0C0A442415547A46E756C6CA44277674A")
				.msgPackT("84A3666F6FA441514944A3626172C0C0A442415547A46E756C6CA44277674A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n<jp:null>BwgJ</jp:null>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n<jp:null>BwgJ</jp:null>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>AQID</jp:foo>\n    <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n    <jp:_x0000_>BAUG</jp:_x0000_>\n    <jp:null>BwgJ</jp:null>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Map.class))
				.verify(x -> verify(x.keySet().iterator().next()).isType(String.class))
				.verify(x -> verify(x.values().iterator().next()).isType(byte[].class))
			},
			{ 	/* 4 */
				new ComboInput<BeanWithByteArrayField>(
					"BeanWithByteArrayField",
					BeanWithByteArrayField.class,
					new BeanWithByteArrayField().init()
				)
				.json("{f:'AQID'}")
				.jsonT("{f:'AQID'}")
				.jsonR("{\n\tf: 'AQID'\n}")
				.xml("<object><f>AQID</f></object>")
				.xmlT("<object><f>AQID</f></object>")
				.xmlR("<object>\n\t<f>AQID</f>\n</object>\n")
				.xmlNs("<object><f>AQID</f></object>")
				.html("<table><tr><td>f</td><td>AQID</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>AQID</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>AQID</td>\n\t</tr>\n</table>\n")
				.uon("(f=AQID)")
				.uonT("(f=AQID)")
				.uonR("(\n\tf=AQID\n)")
				.urlEnc("f=AQID")
				.urlEncT("f=AQID")
				.urlEncR("f=AQID")
				.msgPack("81A166A441514944")
				.msgPackT("81A166A441514944")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>AQID</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>AQID</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>AQID</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArrayField.class))
			},
			{	/* 5 */
				new ComboInput<BeanWithByteArray2dField>(
					"BeanWithByteArray2dField",
					BeanWithByteArray2dField.class,
					new BeanWithByteArray2dField().init()
				)
				.json("{f:['AQID','BAUG',null]}")
				.jsonT("{f:['AQID','BAUG',null]}")
				.jsonR("{\n\tf: [\n\t\t'AQID',\n\t\t'BAUG',\n\t\tnull\n\t]\n}")
				.xml("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.xmlT("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<string>AQID</string>\n\t\t<string>BAUG</string>\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>AQID</li>\n\t\t\t\t<li>BAUG</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(AQID,BAUG,null))")
				.uonT("(f=@(AQID,BAUG,null))")
				.uonR("(\n\tf=@(\n\t\tAQID,\n\t\tBAUG,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(AQID,BAUG,null)")
				.urlEncT("f=@(AQID,BAUG,null)")
				.urlEncR("f=@(\n\tAQID,\n\tBAUG,\n\tnull\n)")
				.msgPack("81A16693A441514944A442415547C0")
				.msgPackT("81A16693A441514944A442415547C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>AQID</rdf:li>\n        <rdf:li>BAUG</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArray2dField.class))
			},
			{	/* 6 */
				new ComboInput<BeanWithByteArrayNullField>(
					"BeanWithByteArrayNullField",
					BeanWithByteArrayNullField.class,
					new BeanWithByteArrayNullField().init()
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
				.verify(x -> verify(x).isType(BeanWithByteArrayNullField.class))
			},
			{	/* 7 */
				new ComboInput<BeanWithByteArrayListField>(
					"BeanWithByteArrayListField",
					BeanWithByteArrayListField.class,
					new BeanWithByteArrayListField().init()
				)
				.json("{f:['AQID','BAUG',null]}")
				.jsonT("{f:['AQID','BAUG',null]}")
				.jsonR("{\n\tf: [\n\t\t'AQID',\n\t\t'BAUG',\n\t\tnull\n\t]\n}")
				.xml("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.xmlT("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<string>AQID</string>\n\t\t<string>BAUG</string>\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><string>AQID</string><string>BAUG</string><null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>AQID</li>\n\t\t\t\t<li>BAUG</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@(AQID,BAUG,null))")
				.uonT("(f=@(AQID,BAUG,null))")
				.uonR("(\n\tf=@(\n\t\tAQID,\n\t\tBAUG,\n\t\tnull\n\t)\n)")
				.urlEnc("f=@(AQID,BAUG,null)")
				.urlEncT("f=@(AQID,BAUG,null)")
				.urlEncR("f=@(\n\tAQID,\n\tBAUG,\n\tnull\n)")
				.msgPack("81A16693A441514944A442415547C0")
				.msgPackT("81A16693A441514944A442415547C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>AQID</rdf:li>\n        <rdf:li>BAUG</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArrayListField.class))
			},
			{	/* 8 */
				new ComboInput<BeanWithByteArrayMapField>(
					"BeanWithByteArrayMapField",
					BeanWithByteArrayMapField.class,
					new BeanWithByteArrayMapField().init()
				)
				.json("{f:{foo:'AQID',bar:null,null:'BAUG'}}")
				.jsonT("{f:{foo:'AQID',bar:null,null:'BAUG'}}")
				.jsonR("{\n\tf: {\n\t\tfoo: 'AQID',\n\t\tbar: null,\n\t\tnull: 'BAUG'\n\t}\n}")
				.xml("<object><f><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f></object>")
				.xmlT("<object><f><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>AQID</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>BAUG</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>AQID</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=AQID,bar=null,null=BAUG))")
				.uonT("(f=(foo=AQID,bar=null,null=BAUG))")
				.uonR("(\n\tf=(\n\t\tfoo=AQID,\n\t\tbar=null,\n\t\tnull=BAUG\n\t)\n)")
				.urlEnc("f=(foo=AQID,bar=null,null=BAUG)")
				.urlEncT("f=(foo=AQID,bar=null,null=BAUG)")
				.urlEncR("f=(\n\tfoo=AQID,\n\tbar=null,\n\tnull=BAUG\n)")
				.msgPack("81A16683A3666F6FA441514944A3626172C0C0A442415547")
				.msgPackT("81A16683A3666F6FA441514944A3626172C0C0A442415547")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo>AQID</jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_>BAUG</jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArrayMapField.class))
			},
			{	/* 9 */
				new ComboInput<BeanWithByteArrayBeanListField>(
					"BeanWithByteArrayBeanListField",
					BeanWithByteArrayBeanListField.class,
					new BeanWithByteArrayBeanListField().init()
				)
				.json("{f:[{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},null]}")
				.jsonT("{f:[{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},null]}")
				.jsonR("{\n\tf: [\n\t\t{\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t},\n\t\tnull\n\t]\n}")
				.xml("<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>")
				.xmlT("<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<object>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</object>\n\t\t<null/>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>")
				.html("<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<table>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t</table>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null))")
				.uonT("(f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null))")
				.uonR("(\n\tf=@(\n\t\t(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null)")
				.urlEncT("f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null)")
				.urlEncR("f=@(\n\t(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t),\n\tnull\n)")
				.msgPack("81A1669285A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547C0")
				.msgPackT("81A1669285A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f1>AQID</jp:f1>\n          <jp:f2>\n            <rdf:Seq>\n              <rdf:li>AQID</rdf:li>\n              <rdf:li>BAUG</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f2>\n          <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:f4>\n            <rdf:Seq>\n              <rdf:li>AQID</rdf:li>\n              <rdf:li>BAUG</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f4>\n          <jp:f5 rdf:parseType='Resource'>\n            <jp:foo>AQID</jp:foo>\n            <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            <jp:_x0000_>BAUG</jp:_x0000_>\n          </jp:f5>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArrayBeanListField.class))
			},
			{	/* 10 */
				new ComboInput<BeanWithByteArrayBeanMapField>(
					"BeanWithByteArrayBeanMapField",
					BeanWithByteArrayBeanMapField.class,
					new BeanWithByteArrayBeanMapField().init()
				)
				.json("{f:{foo:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},bar:null,null:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}}}}")
				.jsonT("{f:{foo:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},bar:null,null:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}}}}")
				.jsonR("{\n\tf: {\n\t\tfoo: {\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t},\n\t\tbar: null,\n\t\tnull: {\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t}\n\t}\n}")
				.xml("<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>")
				.xmlT("<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar t='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>")
				.xmlR("<object>\n\t<f>\n\t\t<foo>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</_x0000_>\n\t</f>\n</object>\n")
				.xmlNs("<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>")
				.html("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG))))")
				.uonT("(f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG))))")
				.uonR("(\n\tf=(\n\t\tfoo=(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t),\n\t\tbar=null,\n\t\tnull=(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)))")
				.urlEncT("f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)))")
				.urlEncR("f=(\n\tfoo=(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t),\n\tbar=null,\n\tnull=(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t)\n)")
				.msgPack("81A16683A3666F6F85A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547A3626172C0C085A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547")
				.msgPackT("81A16683A3666F6F85A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547A3626172C0C085A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo rdf:parseType='Resource'>\n        <jp:f1>AQID</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>AQID</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>BAUG</jp:_x0000_>\n        </jp:f5>\n      </jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_ rdf:parseType='Resource'>\n        <jp:f1>AQID</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>AQID</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>BAUG</jp:_x0000_>\n        </jp:f5>\n      </jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithByteArrayBeanMapField.class))
			},
		});
	}

	public ByteArrayBase64SwapComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.copy().swaps(ByteArraySwap.Base64.class).keepNullProperties().build();
	}

	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p.copy().swaps(ByteArraySwap.Base64.class).build();
	}

	public static class BeanWithByteArrayField {
		public byte[] f;
		public BeanWithByteArrayField init() {
			f = new byte[]{1,2,3};
			return this;
		}
	}

	public static class BeanWithByteArray2dField {
		public byte[][] f;
		public BeanWithByteArray2dField init() {
			f = new byte[][]{{1,2,3},{4,5,6},null};
			return this;
		}
	}

	public static class BeanWithByteArrayNullField {
		public byte[] f;
		public BeanWithByteArrayNullField init() {
			f = null;
			return this;
		}
	}

	public static class BeanWithByteArrayListField {
		public List<byte[]> f;
		public BeanWithByteArrayListField init() {
			f = list(new byte[]{1,2,3},new byte[]{4,5,6},null);
			return this;
		}
	}

	public static class BeanWithByteArrayMapField {
		public Map<String,byte[]> f;
		public BeanWithByteArrayMapField init() {
			f = map("foo",new byte[]{1,2,3},"bar",null,null,new byte[]{4,5,6});
			return this;
		}
	}

	public static class BeanWithByteArrayBeanListField {
		public List<B> f;
		public BeanWithByteArrayBeanListField init() {
			f = list(new B().init(),null);
			return this;
		}
	}

	public static class BeanWithByteArrayBeanMapField {
		public Map<String,B> f;
		public BeanWithByteArrayBeanMapField init() {
			f = map("foo",new B().init(),"bar",null,null,new B().init());
			return this;
		}
	}

	public static class B {
		public byte[] f1;
		public byte[][] f2;
		public byte[] f3;
		public List<byte[]> f4;
		public Map<String,byte[]> f5;

		public B init() {
			f1 = new byte[]{1,2,3};
			f2 = new byte[][]{{1,2,3},{4,5,6},null};
			f3 = null;
			f4 = list(new byte[]{1,2,3},new byte[]{4,5,6},null);
			f5 = map("foo",new byte[]{1,2,3},"bar",null,null,new byte[]{4,5,6});
			return this;
		}
	}
}
