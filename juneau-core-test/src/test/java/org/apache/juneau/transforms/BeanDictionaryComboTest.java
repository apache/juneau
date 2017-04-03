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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for the CalendarSwap class.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class BeanDictionaryComboTest extends ComboTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				"A",
				new A().init(),
				/* Json */		"{_type:'A',a:1}",
				/* JsonT */		"{t:'A',a:1}",
				/* JsonR */		"{\n\t_type: 'A',\n\ta: 1\n}",
				/* Xml */		"<A><a>1</a></A>",
				/* XmlT */		"<A><a>1</a></A>",
				/* XmlR */		"<A>\n\t<a>1</a>\n</A>\n",
				/* XmlNs */		"<A><a>1</a></A>",
				/* Html */		"<table _type='A'><tr><td>a</td><td>1</td></tr></table>",
				/* HtmlT */		"<table t='A'><tr><td>a</td><td>1</td></tr></table>",
				/* HtmlR */		"<table _type='A'>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"(_type=A,a=1)",
				/* UonT */		"(t=A,a=1)",
				/* UonR */		"(\n\t_type=A,\n\ta=1\n)",
				/* UrlEnc */	"_type=A&a=1",
				/* UrlEncT */	"t=A&a=1",
				/* UrlEncR */	"_type=A\n&a=1",
				/* MsgPack */	"82A55F74797065A141A16101",
				/* MsgPackT */	"82A174A141A16101",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>A</jp:_type>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n",
			},
			{
				"A[]",
				new A[]{new A().init()},
				/* Json */		"[{_type:'A',a:1}]",
				/* JsonT */		"[{t:'A',a:1}]",
				/* JsonR */		"[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]",
				/* Xml */		"<array><A><a>1</a></A></array>",
				/* XmlT */		"<array><A><a>1</a></A></array>",
				/* XmlR */		"<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n",
				/* XmlNs */		"<array><A><a>1</a></A></array>",
				/* Html */		"<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>",
				/* HtmlT */		"<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>",
				/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"@((_type=A,a=1))",
				/* UonT */		"@((t=A,a=1))",
				/* UonR */		"@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)",
				/* UrlEnc */	"0=(_type=A,a=1)",
				/* UrlEncT */	"0=(t=A,a=1)",
				/* UrlEncR */	"0=(\n\t_type=A,\n\ta=1\n)",
				/* MsgPack */	"9182A55F74797065A141A16101",
				/* MsgPackT */	"9182A174A141A16101",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n",
			},
			{
				"IA[]",
				new IA[]{new A().init()},
				/* Json */		"[{_type:'A',a:1}]",
				/* JsonT */		"[{t:'A',a:1}]",
				/* JsonR */		"[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]",
				/* Xml */		"<array><A><a>1</a></A></array>",
				/* XmlT */		"<array><A><a>1</a></A></array>",
				/* XmlR */		"<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n",
				/* XmlNs */		"<array><A><a>1</a></A></array>",
				/* Html */		"<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>",
				/* HtmlT */		"<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>",
				/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"@((_type=A,a=1))",
				/* UonT */		"@((t=A,a=1))",
				/* UonR */		"@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)",
				/* UrlEnc */	"0=(_type=A,a=1)",
				/* UrlEncT */	"0=(t=A,a=1)",
				/* UrlEncR */	"0=(\n\t_type=A,\n\ta=1\n)",
				/* MsgPack */	"9182A55F74797065A141A16101",
				/* MsgPackT */	"9182A174A141A16101",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n",
			},
			{
				"B",
				new B().init(),
				/* Json */		"{z:'B',b:1}",
				/* JsonT */		"{z:'B',b:1}",
				/* JsonR */		"{\n\tz: 'B',\n\tb: 1\n}",
				/* Xml */		"<B><b>1</b></B>",
				/* XmlT */		"<B><b>1</b></B>",
				/* XmlR */		"<B>\n\t<b>1</b>\n</B>\n",
				/* XmlNs */		"<B><b>1</b></B>",
				/* Html */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
				/* HtmlT */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
				/* HtmlR */		"<table z='B'>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"(z=B,b=1)",
				/* UonT */		"(z=B,b=1)",
				/* UonR */		"(\n\tz=B,\n\tb=1\n)",
				/* UrlEnc */	"z=B&b=1",
				/* UrlEncT */	"z=B&b=1",
				/* UrlEncR */	"z=B\n&b=1",
				/* MsgPack */	"82A17AA142A16201",
				/* MsgPackT */	"82A17AA142A16201",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:z>B</jp:z>\n    <jp:b>1</jp:b>\n  </rdf:Description>\n</rdf:RDF>\n",
			},
			{
				"B[]",
				new B[]{new B().init()},
				/* Json */		"[{z:'B',b:1}]",
				/* JsonT */		"[{z:'B',b:1}]",
				/* JsonR */		"[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]",
				/* Xml */		"<array><B><b>1</b></B></array>",
				/* XmlT */		"<array><B><b>1</b></B></array>",
				/* XmlR */		"<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n",
				/* XmlNs */		"<array><B><b>1</b></B></array>",
				/* Html */		"<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
				/* HtmlT */		"<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
				/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"@((z=B,b=1))",
				/* UonT */		"@((z=B,b=1))",
				/* UonR */		"@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)",
				/* UrlEnc */	"0=(z=B,b=1)",
				/* UrlEncT */	"0=(z=B,b=1)",
				/* UrlEncR */	"0=(\n\tz=B,\n\tb=1\n)",
				/* MsgPack */	"9182A17AA142A16201",
				/* MsgPackT */	"9182A17AA142A16201",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n",
			},
			{
				"IB[]",
				new IB[]{new B().init()},
				/* Json */		"[{z:'B',b:1}]",
				/* JsonT */		"[{z:'B',b:1}]",
				/* JsonR */		"[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]",
				/* Xml */		"<array><B><b>1</b></B></array>",
				/* XmlT */		"<array><B><b>1</b></B></array>",
				/* XmlR */		"<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n",
				/* XmlNs */		"<array><B><b>1</b></B></array>",
				/* Html */		"<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
				/* HtmlT */		"<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
				/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
				/* Uon */		"@((z=B,b=1))",
				/* UonT */		"@((z=B,b=1))",
				/* UonR */		"@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)",
				/* UrlEnc */	"0=(z=B,b=1)",
				/* UrlEncT */	"0=(z=B,b=1)",
				/* UrlEncR */	"0=(\n\tz=B,\n\tb=1\n)",
				/* MsgPack */	"9182A17AA142A16201",
				/* MsgPackT */	"9182A17AA142A16201",
				/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
				/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n",
			},
		});
	}
	
	public BeanDictionaryComboTest(
			String label, 
			Object in,
			String oJson, String oJsonT, String oJsonR,
			String oXml, String oXmlT, String oXmlR, String oXmlNs,
			String oHtml, String oHtmlT, String oHtmlR,
			String oUon, String oUonT, String oUonR,
			String oUrlEncoding, String oUrlEncodingT, String oUrlEncodingR,
			String oMsgPack, String oMsgPackT,
			String oRdfXml, String oRdfXmlT, String oRdfXmlR
		) {
		super(
			label,
			in, 
			oJson, oJsonT, oJsonR,
			oXml, oXmlT, oXmlR, oXmlNs,
			oHtml, oHtmlT, oHtmlR,
			oUon, oUonT, oUonR,
			oUrlEncoding, oUrlEncodingT, oUrlEncodingR,
			oMsgPack, oMsgPackT,
			oRdfXml, oRdfXmlT, oRdfXmlR
		);
	}
	
	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().trimNullProperties(false).build();
	}
	
	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p.builder().build();
	}
	
	@Bean(beanDictionary={A.class})
	public static interface IA {}
	
	@Bean(typeName="A")
	public static class A implements IA {
		public int a;
		
		public A init() {
			a = 1;
			return this;
		}
	}

	@Bean(beanDictionary={B.class}, typePropertyName="z")
	public static interface IB {}
	
	@Bean(typeName="B")
	public static class B implements IB {
		public int b;
		
		public B init() {
			b = 1;
			return this;
		}
	}
}
