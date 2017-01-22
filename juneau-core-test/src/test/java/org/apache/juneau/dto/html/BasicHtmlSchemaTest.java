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
package org.apache.juneau.dto.html;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class BasicHtmlSchemaTest {

	private static final WriterSerializer
		sXmlSq = XmlSerializer.DEFAULT_SQ,
		sXmlSqReadable = XmlSerializer.DEFAULT_SQ_READABLE,
		sXmlNsSq = XmlSerializer.DEFAULT_NS_SQ,
		sHtmlSq = HtmlSerializer.DEFAULT_SQ,
		sHtmlSqReadable = HtmlSerializer.DEFAULT_SQ_READABLE;

	private static final ReaderParser
		pXml = XmlParser.DEFAULT,
		pHtml = HtmlParser.DEFAULT;

	private static final B btag = HtmlBuilder.b("bbb");

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{
				"A-1",
				HtmlBuilder.a("http://foo", "bar"),
				"<a href='http://foo'>bar</a>",
				"<a href='http://foo'>bar</a>\n",
				"<a href='http://foo'>bar</a>",
				"<a href='http://foo'>bar</a>",
				"<a href='http://foo'>bar</a>\n",
			},
			{
				"A-2",
				HtmlBuilder.a("http://foo", "bar", btag, "baz"),
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
			},
		});
	}


	private String label, e1, e2, e3, e4, e5;
	private Object in;

	public BasicHtmlSchemaTest(String label, Object in, String e1, String e2, String e3, String e4, String e5) throws Exception {
		this.label = label;
		this.in = in;
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
		this.e4 = e4;
		this.e5 = e5;
	}

	private void testSerialize(WriterSerializer s, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			assertEquals(label + " serialize-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed", e);
		}
	}

	private void testParse(WriterSerializer s, ReaderParser p, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = s.serialize(o);
			assertEquals(label + " parse-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed", e);
		}
	}

	@Test
	public void serializeXmlDefaultSq() throws Exception {
		testSerialize(sXmlSq, e1);
	}

	@Test
	public void parseXmlDefaultSq() throws Exception {
		testParse(sXmlSq, pXml, e1);
	}

	@Test
	public void serializeXmlDefaultSqReadable() throws Exception {
		testSerialize(sXmlSqReadable, e2);
	}

	@Test
	public void parseXmlDefaultSqReadable() throws Exception {
		testParse(sXmlSqReadable, pXml, e2);
	}

	@Test
	public void serializeXmlDefaultNsSq() throws Exception {
		testSerialize(sXmlNsSq, e3);
	}

	@Test
	public void parseXmlDefaultNsSq() throws Exception {
		testParse(sXmlNsSq, pXml, e3);
	}

	@Test
	public void serializeHtmlDefaultSq() throws Exception {
		testSerialize(sHtmlSq, e4);
	}

	@Test
	public void parseHtmlDefaultSq() throws Exception {
		testParse(sHtmlSq, pHtml, e4);
	}

	@Test
	public void serializeHtmlDefaultSqReadable() throws Exception {
		testSerialize(sHtmlSqReadable, e5);
	}

	@Test
	public void parseHtmlDefaultSqReadable() throws Exception {
		testParse(sHtmlSqReadable, pHtml, e5);
	}
}
