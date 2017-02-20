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

import static org.junit.Assert.*;

import java.net.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class CalendarSwapTest {

	static Calendar testDate = new GregorianCalendar(TimeZone.getTimeZone("PST"));
	static {
		testDate.setTimeInMillis(0);
		testDate.set(1901, 2, 3, 10, 11, 12);
	}

	@Before
	public void beforeTest() {
		TestUtils.setLocale(Locale.US);
	}

	@After
	public void afterTest() {
		TestUtils.unsetLocale();
	}

	private RdfSerializer getRdfSerializer() {
		return new RdfSerializer()
			.setQuoteChar('\'')
			.setUseWhitespace(false)
			.setProperty(RdfCommonContext.RDF_rdfxml_allowBadUris, true)
			.setProperty(RdfCommonContext.RDF_rdfxml_showDoctypeDeclaration, false)
			.setProperty(RdfCommonContext.RDF_rdfxml_showXmlDeclaration, false);
	}

	private String stripRdf(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	//====================================================================================================
	// testTimezone - Make sure timezone on calendar is preserved.
	//====================================================================================================
	@Test
	public void testTimezone() throws Exception {
		PojoSwap<Calendar,String> f;
		String s;
		Calendar c;
		BeanSession session = BeanContext.DEFAULT.createSession();

		//--------------------
		// ISO8601DT
		//--------------------
		f = new CalendarSwap.ISO8601DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(session, c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(session, c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(session, c));


		//--------------------
		// ISO8601DTZ
		//--------------------
		f = new CalendarSwap.ISO8601DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(session, c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.swap(session, c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.swap(session, c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarSwap.RFC2822DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 +0000", f.swap(session, c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 09:34:56 -0300", f.swap(session, c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 06:34:56 -0600", f.swap(session, c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarSwap.RFC2822DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(session, c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(session, c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(session, c));
	}

	//====================================================================================================
	// Test 7a - BeanProperty.filter, JSON
	//====================================================================================================
	@Test
	public void testBeanProperyFilterJson() throws Exception {
		WriterSerializer s = JsonSerializer.DEFAULT_LAX;
		ReaderParser p = JsonParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String json = s.serialize(t);
		assertEquals("{d1:'1901-03-03T18:11:12Z',d2:'Sun, 03 Mar 1901 18:11:12 GMT',d3:-2172116928000}", json);
		t = p.parse(json, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
	}

	//====================================================================================================
	// Test 7b - BeanProperty.filter, XML
	//====================================================================================================
	@Test
	public void testBeanProperyFilterXml() throws Exception {
		WriterSerializer s = XmlSerializer.DEFAULT_SQ;
		ReaderParser p = XmlParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String xml = s.serialize(t);
		assertEquals("<object><d1>1901-03-03T18:11:12Z</d1><d2>Sun, 03 Mar 1901 18:11:12 GMT</d2><d3>-2172116928000</d3></object>", xml);
		t = p.parse(xml, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
	}

	//====================================================================================================
	// Test 7c - BeanProperty.filter, HTML
	//====================================================================================================
	@Test
	public void testBeanProperyFilterHtml() throws Exception {
		WriterSerializer s = HtmlSerializer.DEFAULT_SQ.clone().setAddKeyValueTableHeaders(true);
		ReaderParser p = HtmlParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String html = s.serialize(t);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>d1</td><td>1901-03-03T18:11:12Z</td></tr><tr><td>d2</td><td>Sun, 03 Mar 1901 18:11:12 GMT</td></tr><tr><td>d3</td><td>-2172116928000</td></tr></table>", html);
		t = p.parse(html, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
	}

	//====================================================================================================
	// Test 7d - BeanProperty.filter, UON
	//====================================================================================================
	@Test
	public void testBeanProperyFilterUon() throws Exception {
		WriterSerializer s = UonSerializer.DEFAULT_ENCODING;
		ReaderParser p = UonParser.DEFAULT;
		ReaderParser pe = UonParser.DEFAULT_DECODING;

		Calendar c = testDate;
		A t = new A(c);
		String url = s.serialize(t);
		assertEquals("(d1=1901-03-03T18:11:12Z,d2='Sun,+03+Mar+1901+18:11:12+GMT',d3=-2172116928000)", url);
		t = pe.parse(url, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
		url = URLDecoder.decode(url, "UTF-8");
		t = p.parse(url, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
	}

	//====================================================================================================
	// Test 7d - BeanProperty.filter, URL-ENCODING
	//====================================================================================================
	@Test
	public void testBeanProperyFilterUrlEncoding() throws Exception {
		WriterSerializer s = UrlEncodingSerializer.DEFAULT;
		ReaderParser p = UrlEncodingParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String url = s.serialize(t);
		assertEquals("d1=1901-03-03T18:11:12Z&d2='Sun,+03+Mar+1901+18:11:12+GMT'&d3=-2172116928000", url);
		t = p.parse(url, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
		url = URLDecoder.decode(url, "UTF-8");
		t = p.parse(url, A.class);
		assertEquals(t.d1.getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD2().getTime().getTime(), c.getTime().getTime());
		assertEquals(t.getD3().getTime().getTime(), c.getTime().getTime());
	}

	//====================================================================================================
	// Test 7f - BeanProperty.filter, RDF/XML
	//====================================================================================================
	@Test
	public void testBeanProperyFilterRdfXmlWithNs() throws Exception {
		WriterSerializer s = getRdfSerializer();

		Calendar c = testDate;
		A t = new A(c);
		String rdf = stripRdf(s.serialize(t));
		assertEquals("<rdf:Description><jp:d1>1901-03-03T18:11:12Z</jp:d1><jp:d2>Sun, 03 Mar 1901 18:11:12 GMT</jp:d2><jp:d3>-2172116928000</jp:d3></rdf:Description>", rdf);
	}

	@Bean(sort=true)
	public static class A {
		@BeanProperty(swap=CalendarSwap.ISO8601DTZ.class)
		public Calendar d1;
		private Calendar d2, d3;
		public A(Calendar date) {
			d1 = d2 = d3 = date;
		}

		public A() {}

		@BeanProperty(swap=CalendarSwap.RFC2822DTZ.class)
		public Calendar getD2() {
			return d2;
		}
		public void setD2(Calendar d2) {
			this.d2 = d2;
		}

		public Calendar getD3() {
			return d3;
		}
		@BeanProperty(swap=CalendarLongSwap.class)
		public void setD3(Calendar d3) {
			this.d3 = d3;
		}
	}
}