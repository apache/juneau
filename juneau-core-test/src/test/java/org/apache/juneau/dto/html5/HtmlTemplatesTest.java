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
package org.apache.juneau.dto.html5;

import static org.junit.Assert.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.BeanSession;
import org.apache.juneau.annotation.Bean;
import org.apache.juneau.html.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;


@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class HtmlTemplatesTest {

	private static final WriterSerializer
		sXmlSq = XmlSerializer.DEFAULT_SQ,
		sXmlNsSq = XmlSerializer.DEFAULT_NS_SQ,
		sXmlSqReadable = XmlSerializer.DEFAULT_SQ_READABLE,
		sHtmlSq = HtmlSerializer.DEFAULT_SQ,
		sHtmlSqReadable = HtmlSerializer.DEFAULT_SQ_READABLE;

	private static final ReaderParser
		pXml = XmlParser.DEFAULT,
		pHtml = HtmlParser.DEFAULT;

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{
				"FormTemplate-1",
				new FormTemplate("myaction", 123, true),
				"<form action='myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>",
				"<form action='myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>\n",
				"<form action='myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>",
				"<form action='myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>\n",
			},
		});
	}

	
	@Bean(beanDictionary=HtmlBeanDictionary.class)
	public static class FormTemplate {
		
		private String action;
		private int value1;
		private boolean value2;
		
		public FormTemplate(Form f) {
			this.action = f.getAttr("action");
			this.value1 = f.getChild(Input.class, 0).getAttr(int.class, "value");
			this.value2 = f.getChild(Input.class, 1).getAttr(boolean.class, "value");
		}
		
		public FormTemplate(String action, int value1, boolean value2) {
			this.action = action;
			this.value1 = value1;
			this.value2 = value2;
		}
		
		public Form swap(BeanSession session) {
			return form(action,
				input("text").name("v1").value(value1),
				input("text").name("v2").value(value2)
			);
		}
	}
	

	private String label, xml, xmlr, html, htmlr;
	private Object in;

	public HtmlTemplatesTest(String label, Object in, String xml, String xmlr, String html, String htmlr) throws Exception {
		this.label = label;
		this.in = in;
		this.xml = xml;
		this.xmlr = xmlr;
		this.html = html;
		this.htmlr = htmlr;
	}

	private void testSerialize(WriterSerializer s, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			assertEquals(label + " serialize-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
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
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	@Test
	public void serializeXmlDefaultSq() throws Exception {
		testSerialize(sXmlSq, xml);
	}

	@Test
	public void parseXmlDefaultSq() throws Exception {
		testParse(sXmlSq, pXml, xml);
	}

	@Test
	public void serializeXmlDefaultNsSq() throws Exception {
		testSerialize(sXmlNsSq, xml);
	}

	@Test
	public void parseXmlDefaultNsSq() throws Exception {
		testParse(sXmlNsSq, pXml, xml);
	}

	@Test
	public void serializeHtmlDefaultSq() throws Exception {
		testSerialize(sHtmlSq, html);
	}

	@Test
	public void parseHtmlDefaultSq() throws Exception {
		testParse(sHtmlSq, pHtml, html);
	}

	@Test
	public void serializeXmlDefaultSqReadable() throws Exception {
		testSerialize(sXmlSqReadable, xmlr);
	}

	@Test
	public void parseXmlDefaultSqReadable() throws Exception {
		testParse(sXmlSqReadable, pXml, xmlr);
	}

	@Test
	public void serializeHtmlDefaultSqReadable() throws Exception {
		testSerialize(sHtmlSqReadable, htmlr);
	}

	@Test
	public void parseHtmlDefaultSqReadable() throws Exception {
		testParse(sHtmlSqReadable, pHtml, htmlr);
	}
}
