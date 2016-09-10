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

import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.xml.bind.*;

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

	private static Calendar testDate = new GregorianCalendar(TimeZone.getTimeZone("PST"));
	static {
		testDate.setTimeInMillis(0);
		testDate.set(1901, 2, 3, 10, 11, 12);
	}

	private RdfSerializer getRdfSerializer() {
		return new RdfSerializer()
			.setProperty(SERIALIZER_quoteChar, '\'')
			.setProperty(SERIALIZER_useIndentation, false)
			.setProperty(RDF_rdfxml_allowBadUris, true)
			.setProperty(RDF_rdfxml_showDoctypeDeclaration, false)
			.setProperty(RDF_rdfxml_showXmlDeclaration, false);
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

		//--------------------
		// ISO8601DT
		//--------------------
		f = new CalendarSwap.ISO8601DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(c));


		//--------------------
		// ISO8601DTZ
		//--------------------
		f = new CalendarSwap.ISO8601DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.swap(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.swap(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.swap(c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarSwap.RFC2822DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 +0000", f.swap(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 09:34:56 -0300", f.swap(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 06:34:56 -0600", f.swap(c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarSwap.RFC2822DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.swap(c));
	}


	//====================================================================================================
	// testDefaultStringJson - DEFAULT_STRING, JSON
	//====================================================================================================
	@Test
	public void testDefaultStringJson() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		doTest(s, p, true, "'Sun Mar 03 10:11:12 PST 1901'");
	}

	//====================================================================================================
	// testDefaultStringXml - DEFAULT_STRING, XML
	//====================================================================================================
	@Test
	public void testDefaultStringXml() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>Sun Mar 03 10:11:12 PST 1901</string>");
	}

	//====================================================================================================
	// testDefaultStringHtml - DEFAULT_STRING, HTML
	//====================================================================================================
	@Test
	public void testDefaultStringHtml() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = new HtmlSerializer().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>Sun Mar 03 10:11:12 PST 1901</string>");
	}

	//====================================================================================================
	// testDefaultStringUrlParam - DEFAULT_STRING, UON
	//====================================================================================================
	@Test
	public void testDefaultStringUon() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = new UonSerializer.Encoding().addPojoSwaps(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addPojoSwaps(f);
		doTest(s, p, true, "Sun+Mar+03+10:11:12+PST+1901");
	}

	//====================================================================================================
	// testDefaultStringUrlParam - DEFAULT_STRING, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultStringUrlEncoding() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = new UrlEncodingSerializer().addPojoSwaps(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addPojoSwaps(f);
		doTest(s, p, true, "_value=Sun+Mar+03+10:11:12+PST+1901");
	}

	//====================================================================================================
	// testDefaultStringRdfXml - DEFAULT_STRING, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultStringRdfXml() throws Exception {
		Class<?> f = CalendarSwap.ToString.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, true, "<rdf:Description><j:value>Sun Mar 03 10:11:12 PST 1901</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testISO8601DTJson - DEFAULT_ISO8601DT, JSON
	//====================================================================================================
	@Test
	public void testISO8601DTJson() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		String x = "'1901-03-03T10:11:12-08:00'";
		doTest(s, p, true, x);
	}

	//====================================================================================================
	// testISO8601DTXml - DEFAULT_ISO8601DT, XML
	//====================================================================================================
	@Test
	public void testISO8601DTXml() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>1901-03-03T10:11:12-08:00</string>");
	}

	//====================================================================================================
	// testISO8601DTHtml - DEFAULT_ISO8601DT, HTML
	//====================================================================================================
	@Test
	public void testISO8601DTHtml() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = new HtmlSerializer().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>1901-03-03T10:11:12-08:00</string>");
	}

	//====================================================================================================
	// testISO8601DTUrlParam - DEFAULT_ISO8601DT, UON
	//====================================================================================================
	@Test
	public void testISO8601DTUon() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = new UonSerializer().addPojoSwaps(f);
		ReaderParser p = new UonParser().addPojoSwaps(f);
		doTest(s, p, true, "1901-03-03T10:11:12-08:00");
	}

	//====================================================================================================
	// testISO8601DTUrlParam - DEFAULT_ISO8601DT, URL-ENCODING
	//====================================================================================================
	@Test
	public void testISO8601DTUrlEncoding() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = new UrlEncodingSerializer().addPojoSwaps(f);
		ReaderParser p = new UrlEncodingParser().addPojoSwaps(f);
		doTest(s, p, true, "_value=1901-03-03T10:11:12-08:00");
	}

	//====================================================================================================
	// testISO8601DTRdfXml - DEFAULT_ISO8601DT, RDF/XML
	//====================================================================================================
	@Test
	public void testISO8601DTRdfXml() throws Exception {
		Class<?> f = CalendarSwap.ISO8601DT.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, true, "<rdf:Description><j:value>1901-03-03T10:11:12-08:00</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testRFC2822DTJson - DEFAULT_RFC2822DT, JSON
	//====================================================================================================
	@Test
	public void testRFC2822DTJson() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		doTest(s, p, true, "'Sun, 03 Mar 1901 18:11:12 GMT'");
	}

	//====================================================================================================
	// testRFC2822DTXml - DEFAULT_RFC2822DT, XML
	//====================================================================================================
	@Test
	public void testRFC2822DTXml() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>Sun, 03 Mar 1901 18:11:12 GMT</string>");
	}

	//====================================================================================================
	// testRFC2822DTHtml - DEFAULT_RFC2822DT, HTML
	//====================================================================================================
	@Test
	public void testRFC2822DTHtml() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = new HtmlSerializer().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<string>Sun, 03 Mar 1901 18:11:12 GMT</string>");
	}

	//====================================================================================================
	// testRFC2822DTUrlParam - DEFAULT_RFC2822DT, UON
	//====================================================================================================
	@Test
	public void testRFC2822DTUon() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = new UonSerializer.Encoding().addPojoSwaps(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addPojoSwaps(f);
		doTest(s, p, true, "Sun,+03+Mar+1901+18:11:12+GMT");
	}

	//====================================================================================================
	// testRFC2822DTUrlParam - DEFAULT_RFC2822DT, URL-ENCODING
	//====================================================================================================
	@Test
	public void testRFC2822DTUrlEncoding() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = new UrlEncodingSerializer().addPojoSwaps(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addPojoSwaps(f);
		doTest(s, p, true, "_value=Sun,+03+Mar+1901+18:11:12+GMT");
	}

	//====================================================================================================
	// testRFC2822DTRdfXml - DEFAULT_RFC2822DT, RDF/XML
	//====================================================================================================
	@Test
	public void testRFC2822DTRdfXml() throws Exception {
		Class<?> f = CalendarSwap.RFC2822DTZ.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, true, "<rdf:Description><j:value>Sun, 03 Mar 1901 18:11:12 GMT</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultLongJson - DEFAULT_LONG, JSON
	//====================================================================================================
	@Test
	public void testDefaultLongJson() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		doTest(s, p, true, "-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongXml - DEFAULT_LONG, XML
	//====================================================================================================
	@Test
	public void testDefaultLongXml() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<number>-2172116928000</number>");
	}

	//====================================================================================================
	// testDefaultLongHtml - DEFAULT_LONG, HTML
	//====================================================================================================
	@Test
	public void testDefaultLongHtml() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = new HtmlSerializer().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<number>-2172116928000</number>");
	}

	//====================================================================================================
	// testDefaultLongUrlParam - DEFAULT_LONG, UON
	//====================================================================================================
	@Test
	public void testDefaultLongUon() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = UonSerializer.DEFAULT_SIMPLE.clone().addPojoSwaps(f);
		ReaderParser p = new UonParser().addPojoSwaps(f);
		doTest(s, p, true, "-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongUrlParam - DEFAULT_LONG, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultLongUrlEncoding() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = UrlEncodingSerializer.DEFAULT_SIMPLE.clone().addPojoSwaps(f);
		ReaderParser p = new UrlEncodingParser().addPojoSwaps(f);
		doTest(s, p, true, "_value=-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongRdfXml - DEFAULT_LONG, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultLongRdfXml() throws Exception {
		Class<?> f = CalendarLongSwap.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, true, "<rdf:Description><j:value>-2172116928000</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultMapJson - DEFAULT_MAP, JSON
	//====================================================================================================
	@Test
	public void testDefaultMapJson() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		doTest(s, p, true, "{time:-2172116928000,timeZone:'PST'}");
	}

	//====================================================================================================
	// testDefaultMapXml - DEFAULT_MAP, XML
	//====================================================================================================
	@Test
	public void testDefaultMapXml() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = new XmlSerializer.SimpleXmlJsonSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<object><time type='number'>-2172116928000</time><timeZone>PST</timeZone></object>");
	}

	//====================================================================================================
	// testDefaultMapHtml - DEFAULT_MAP, HTML
	//====================================================================================================
	@Test
	public void testDefaultMapHtml() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = new HtmlSerializer.Sq().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, true, "<table _type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>time</string></td><td><number>-2172116928000</number></td></tr><tr><td><string>timeZone</string></td><td><string>PST</string></td></tr></table>");
	}

	//====================================================================================================
	// testDefaultMapUrlParam - DEFAULT_MAP, UON
	//====================================================================================================
	@Test
	public void testDefaultMapUon() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = UonSerializer.DEFAULT_SIMPLE.clone().addPojoSwaps(f);
		ReaderParser p = new UonParser().addPojoSwaps(f);
		doTest(s, p, true, "(time=-2172116928000,timeZone=PST)");
	}

	//====================================================================================================
	// testDefaultMapUrlParam - DEFAULT_MAP, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultMapUrlEncoding() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = UrlEncodingSerializer.DEFAULT_SIMPLE.clone().addPojoSwaps(f);
		ReaderParser p = new UrlEncodingParser().addPojoSwaps(f);
		doTest(s, p, true, "time=-2172116928000&timeZone=PST");
	}

	//====================================================================================================
	// testDefaultMapRdfXml - DEFAULT_MAP, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultMapRdfXml() throws Exception {
		Class<?> f = CalendarMapSwap.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, true, "<rdf:Description><jp:time>-2172116928000</jp:time><jp:timeZone>PST</jp:timeZone></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultMediumJson - DEFAULT_MEDIUM, JSON
	//====================================================================================================
	@Test
	public void testDefaultMediumJson() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(f);
		ReaderParser p = new JsonParser().addPojoSwaps(f);
		doTest(s, p, false, "'Mar 3, 1901'");
	}

	//====================================================================================================
	// testDefaultMediumXml - DEFAULT_MEDIUM, XML
	//====================================================================================================
	@Test
	public void testDefaultMediumXml() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addPojoSwaps(f);
		ReaderParser p = new XmlParser().addPojoSwaps(f);
		doTest(s, p, false, "<string>Mar 3, 1901</string>");
	}

	//====================================================================================================
	// testDefaultMediumHtml - DEFAULT_MEDIUM, HTML
	//====================================================================================================
	@Test
	public void testDefaultMediumHtml() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = new HtmlSerializer().addPojoSwaps(f);
		ReaderParser p = new HtmlParser().addPojoSwaps(f);
		doTest(s, p, false, "<string>Mar 3, 1901</string>");
	}

	//====================================================================================================
	// testDefaultMediumUrlParam - DEFAULT_MEDIUM, UON
	//====================================================================================================
	@Test
	public void testDefaultMediumUon() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = new UonSerializer.Encoding().addPojoSwaps(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addPojoSwaps(f);
		doTest(s, p, false, "Mar+3,+1901");
	}

	//====================================================================================================
	// testDefaultMediumUrlParam - DEFAULT_MEDIUM, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultMediumUrlEncoding() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = new UrlEncodingSerializer().addPojoSwaps(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addPojoSwaps(f);
		doTest(s, p, false, "_value=Mar+3,+1901");
	}

	//====================================================================================================
	// testDefaultMediumRdfXml - DEFAULT_MEDIUM, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultMediumRdfXml() throws Exception {
		Class<?> f = CalendarSwap.Medium.class;
		WriterSerializer s = getRdfSerializer().addPojoSwaps(f);
		ReaderParser p = new RdfParser.Xml().addPojoSwaps(f);
		doTest(s, p, false, "<rdf:Description><j:value>Mar 3, 1901</j:value></rdf:Description>");
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
		WriterSerializer s = XmlSerializer.DEFAULT_SIMPLE_SQ;
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
		WriterSerializer s = HtmlSerializer.DEFAULT_SQ;
		ReaderParser p = HtmlParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String html = s.serialize(t);
		assertEquals("<table _type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>d1</string></td><td><string>1901-03-03T18:11:12Z</string></td></tr><tr><td><string>d2</string></td><td><string>Sun, 03 Mar 1901 18:11:12 GMT</string></td></tr><tr><td><string>d3</string></td><td><number>-2172116928000</number></td></tr></table>", html);
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
		WriterSerializer s = UonSerializer.DEFAULT_SIMPLE_ENCODING;
		ReaderParser p = UonParser.DEFAULT;
		ReaderParser pe = UonParser.DEFAULT_DECODING;

		Calendar c = testDate;
		A t = new A(c);
		String url = s.serialize(t);
		assertEquals("(d1=1901-03-03T18:11:12Z,d2=Sun~,+03+Mar+1901+18:11:12+GMT,d3=-2172116928000)", url);
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
		WriterSerializer s = UrlEncodingSerializer.DEFAULT_SIMPLE;
		ReaderParser p = UrlEncodingParser.DEFAULT;

		Calendar c = testDate;
		A t = new A(c);
		String url = s.serialize(t);
		assertEquals("d1=1901-03-03T18:11:12Z&d2=Sun,+03+Mar+1901+18:11:12+GMT&d3=-2172116928000", url);
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

	private void doTest(WriterSerializer s, ReaderParser p, boolean doTestTimeToo, String expected) throws Exception {
		Calendar d;
		String actual;

		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("PST"));
		actual = s.serialize(testDate);
		String a = (s instanceof RdfSerializer ? stripRdf(actual) : actual);
		assertEquals(expected, a);
		d = p.parse(actual, GregorianCalendar.class);
		d.setTimeZone(TimeZone.getTimeZone("PST"));
		assertEquals(1901, d.get(Calendar.YEAR));
		assertEquals(2, d.get(Calendar.MONTH));
//		assertEquals(3, d.get(Calendar.DATE));
		if (doTestTimeToo) {
			assertEquals(10, d.get(Calendar.HOUR));
			assertEquals(11, d.get(Calendar.MINUTE));
			assertEquals(12, d.get(Calendar.SECOND));
		}
	}
}