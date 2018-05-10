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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class BpiTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{	/* 0 */
				"a1",
				"{a:1,_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a><_b>foo</_b></object>",
				"<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>",
				"(a=1,_b=foo)",
				"a=1&_b=foo",
				"{a:1,_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 1 */
				"a2",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 2 */
				"a3",
				"{_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><_b>foo</_b></object>",
				"<table><tr><td>_b</td><td>foo</td></tr></table>",
				"(_b=foo)",
				"_b=foo",
				"{_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 3 */
				"a4",
				"{_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><_b>foo</_b></object>",
				"<table><tr><td>_b</td><td>foo</td></tr></table>",
				"(_b=foo)",
				"_b=foo",
				"{_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 4 */
				"a5",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 5 */
				"a6",
				"{}",
				"<?xml version='1.0' encoding='UTF-8'?><object/>",
				"<table></table>",
				"()",
				"",
				"{}",
				"<rdf:RDF>\n</rdf:RDF>"
			},
			{	/* 6 */
				"b1",
				"{_b:'foo',a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><_b>foo</_b><a>1</a></object>",
				"<table><tr><td>_b</td><td>foo</td></tr><tr><td>a</td><td>1</td></tr></table>",
				"(_b=foo,a=1)",
				"_b=foo&a=1",
				"{_b:'foo',a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 7 */
				"b2",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 8 */
				"b3",
				"{_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><_b>foo</_b></object>",
				"<table><tr><td>_b</td><td>foo</td></tr></table>",
				"(_b=foo)",
				"_b=foo",
				"{_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 9 */
				"b4",
				"{_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><_b>foo</_b></object>",
				"<table><tr><td>_b</td><td>foo</td></tr></table>",
				"(_b=foo)",
				"_b=foo",
				"{_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 10 */
				"b5",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 11 */
				"b6",
				"{}",
				"<?xml version='1.0' encoding='UTF-8'?><object/>",
				"<table></table>",
				"()",
				"",
				"{}",
				"<rdf:RDF>\n</rdf:RDF>"
			},
			{	/* 12 */
				"c1",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 13 */
				"c2",
				"{a:1}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a></object>",
				"<table><tr><td>a</td><td>1</td></tr></table>",
				"(a=1)",
				"a=1",
				"{a:1}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 14 */
				"d1",
				"{a:1,_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a><_b>foo</_b></object>",
				"<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>",
				"(a=1,_b=foo)",
				"a=1&_b=foo",
				"{a:1,_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			},
			{	/* 15 */
				"d2",
				"{a:1,_b:'foo'}",
				"<?xml version='1.0' encoding='UTF-8'?><object><a>1</a><_b>foo</_b></object>",
				"<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>",
				"(a=1,_b=foo)",
				"a=1&_b=foo",
				"{a:1,_b:'foo'}",
				"<rdf:RDF>\n<rdf:Description>\n<jp:a>1</jp:a>\n<jp:_b>foo</jp:_b>\n</rdf:Description>\n</rdf:RDF>"
			}
		});
	}

	private String label;
	private String json, xml, html, uon, urlEnc, msgPack, rdfXml;
	private RestClient client = TestMicroservice.DEFAULT_CLIENT;

	public BpiTest(String label, String json, String xml, String html, String uon, String urlEnc, String msgPack, String rdfXml) {
		this.label = label;
		this.json = json;
		this.xml = xml;
		this.html = html;
		this.uon = uon;
		this.urlEnc = urlEnc;
		this.msgPack = msgPack;
		this.rdfXml = rdfXml;
	}

	@Test
	public void a01_json() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("text/json+simple").getResponseAsString();
		assertEquals(json, r);
	}

	@Test
	public void a02_xml() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("text/xml").getResponseAsString().replace('"', '\'');
		assertEquals(xml, r);
	}

	@Test
	public void a03_html() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("text/html+stripped").getResponseAsString();
		assertEquals(html, r);
	}

	@Test
	public void a04_uon() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("text/uon").getResponseAsString();
		assertEquals(uon, r);
	}

	@Test
	public void a05_urlEnc() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("application/x-www-form-urlencoded").getResponseAsString();
		assertEquals(urlEnc, r);
	}

	@Test
	public void a06_msgPack() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("octal/msgpack").parser(MsgPackParser.DEFAULT).getResponse(ObjectMap.class).toString();
		assertEquals(msgPack, r);
	}

	@Test
	public void a07_rdfXml() throws Exception {
		String r = client.doGet("/testBpi/test/" + label).accept("text/xml+rdf+abbrev").getResponseAsString();
		r = r.replaceAll("<rdf:RDF[^>]*>", "<rdf:RDF>").replace('"', '\'');
		TestUtils.assertEqualsAfterSort(rdfXml, r, "a07_rdfXml failed");
	}
}
