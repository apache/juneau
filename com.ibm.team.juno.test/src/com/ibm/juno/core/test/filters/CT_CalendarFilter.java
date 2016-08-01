/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static com.ibm.juno.core.jena.RdfProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static org.junit.Assert.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.xml.bind.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.xml.*;

public class CT_CalendarFilter {

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
		PojoFilter<Calendar,String> f;
		String s;
		Calendar c;

		//--------------------
		// ISO8601DT
		//--------------------
		f = new CalendarFilter.ISO8601DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.filter(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.filter(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.filter(c));


		//--------------------
		// ISO8601DTZ
		//--------------------
		f = new CalendarFilter.ISO8601DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals(s, f.filter(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.filter(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("2001-01-31T12:34:56Z", f.filter(c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarFilter.RFC2822DT();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 +0000", f.filter(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 09:34:56 -0300", f.filter(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 06:34:56 -0600", f.filter(c));

		//--------------------
		// RFC2822DTZ
		//--------------------
		f = new CalendarFilter.RFC2822DTZ();

		s = "2001-01-31T12:34:56Z";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.filter(c));

		s = "2001-01-31T09:34:56-03:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.filter(c));

		s = "2001-01-31T06:34:56-06:00";
		c = DatatypeConverter.parseDateTime(s);
		assertEquals("Wed, 31 Jan 2001 12:34:56 GMT", f.filter(c));
	}


	//====================================================================================================
	// testDefaultStringJson - DEFAULT_STRING, JSON
	//====================================================================================================
	@Test
	public void testDefaultStringJson() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, true, "'Sun Mar 03 10:11:12 PST 1901'");
	}

	//====================================================================================================
	// testDefaultStringXml - DEFAULT_STRING, XML
	//====================================================================================================
	@Test
	public void testDefaultStringXml() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, true, "<string>Sun Mar 03 10:11:12 PST 1901</string>");
	}

	//====================================================================================================
	// testDefaultStringHtml - DEFAULT_STRING, HTML
	//====================================================================================================
	@Test
	public void testDefaultStringHtml() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = new HtmlSerializer().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, true, "<string>Sun Mar 03 10:11:12 PST 1901</string>");
	}

	//====================================================================================================
	// testDefaultStringUrlParam - DEFAULT_STRING, UON
	//====================================================================================================
	@Test
	public void testDefaultStringUon() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = new UonSerializer.Encoding().addFilters(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addFilters(f);
		doTest(s, p, true, "Sun+Mar+03+10:11:12+PST+1901");
	}

	//====================================================================================================
	// testDefaultStringUrlParam - DEFAULT_STRING, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultStringUrlEncoding() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = new UrlEncodingSerializer().addFilters(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addFilters(f);
		doTest(s, p, true, "_value=Sun+Mar+03+10:11:12+PST+1901");
	}

	//====================================================================================================
	// testDefaultStringRdfXml - DEFAULT_STRING, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultStringRdfXml() throws Exception {
		Class<?> f = CalendarFilter.ToString.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
		doTest(s, p, true, "<rdf:Description><j:value>Sun Mar 03 10:11:12 PST 1901</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testISO8601DTJson - DEFAULT_ISO8601DT, JSON
	//====================================================================================================
	@Test
	public void testISO8601DTJson() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		String x = "'1901-03-03T10:11:12-08:00'";
		doTest(s, p, true, x);
	}

	//====================================================================================================
	// testISO8601DTXml - DEFAULT_ISO8601DT, XML
	//====================================================================================================
	@Test
	public void testISO8601DTXml() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, true, "<string>1901-03-03T10:11:12-08:00</string>");
	}

	//====================================================================================================
	// testISO8601DTHtml - DEFAULT_ISO8601DT, HTML
	//====================================================================================================
	@Test
	public void testISO8601DTHtml() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = new HtmlSerializer().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, true, "<string>1901-03-03T10:11:12-08:00</string>");
	}

	//====================================================================================================
	// testISO8601DTUrlParam - DEFAULT_ISO8601DT, UON
	//====================================================================================================
	@Test
	public void testISO8601DTUon() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = new UonSerializer().addFilters(f);
		ReaderParser p = new UonParser().addFilters(f);
		doTest(s, p, true, "1901-03-03T10:11:12-08:00");
	}

	//====================================================================================================
	// testISO8601DTUrlParam - DEFAULT_ISO8601DT, URL-ENCODING
	//====================================================================================================
	@Test
	public void testISO8601DTUrlEncoding() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = new UrlEncodingSerializer().addFilters(f);
		ReaderParser p = new UrlEncodingParser().addFilters(f);
		doTest(s, p, true, "_value=1901-03-03T10:11:12-08:00");
	}

	//====================================================================================================
	// testISO8601DTRdfXml - DEFAULT_ISO8601DT, RDF/XML
	//====================================================================================================
	@Test
	public void testISO8601DTRdfXml() throws Exception {
		Class<?> f = CalendarFilter.ISO8601DT.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
		doTest(s, p, true, "<rdf:Description><j:value>1901-03-03T10:11:12-08:00</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testRFC2822DTJson - DEFAULT_RFC2822DT, JSON
	//====================================================================================================
	@Test
	public void testRFC2822DTJson() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, true, "'Sun, 03 Mar 1901 18:11:12 GMT'");
	}

	//====================================================================================================
	// testRFC2822DTXml - DEFAULT_RFC2822DT, XML
	//====================================================================================================
	@Test
	public void testRFC2822DTXml() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, true, "<string>Sun, 03 Mar 1901 18:11:12 GMT</string>");
	}

	//====================================================================================================
	// testRFC2822DTHtml - DEFAULT_RFC2822DT, HTML
	//====================================================================================================
	@Test
	public void testRFC2822DTHtml() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = new HtmlSerializer().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, true, "<string>Sun, 03 Mar 1901 18:11:12 GMT</string>");
	}

	//====================================================================================================
	// testRFC2822DTUrlParam - DEFAULT_RFC2822DT, UON
	//====================================================================================================
	@Test
	public void testRFC2822DTUon() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = new UonSerializer.Encoding().addFilters(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addFilters(f);
		doTest(s, p, true, "Sun,+03+Mar+1901+18:11:12+GMT");
	}

	//====================================================================================================
	// testRFC2822DTUrlParam - DEFAULT_RFC2822DT, URL-ENCODING
	//====================================================================================================
	@Test
	public void testRFC2822DTUrlEncoding() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = new UrlEncodingSerializer().addFilters(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addFilters(f);
		doTest(s, p, true, "_value=Sun,+03+Mar+1901+18:11:12+GMT");
	}

	//====================================================================================================
	// testRFC2822DTRdfXml - DEFAULT_RFC2822DT, RDF/XML
	//====================================================================================================
	@Test
	public void testRFC2822DTRdfXml() throws Exception {
		Class<?> f = CalendarFilter.RFC2822DTZ.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
		doTest(s, p, true, "<rdf:Description><j:value>Sun, 03 Mar 1901 18:11:12 GMT</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultLongJson - DEFAULT_LONG, JSON
	//====================================================================================================
	@Test
	public void testDefaultLongJson() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, true, "-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongXml - DEFAULT_LONG, XML
	//====================================================================================================
	@Test
	public void testDefaultLongXml() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, true, "<number>-2172116928000</number>");
	}

	//====================================================================================================
	// testDefaultLongHtml - DEFAULT_LONG, HTML
	//====================================================================================================
	@Test
	public void testDefaultLongHtml() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = new HtmlSerializer().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, true, "<number>-2172116928000</number>");
	}

	//====================================================================================================
	// testDefaultLongUrlParam - DEFAULT_LONG, UON
	//====================================================================================================
	@Test
	public void testDefaultLongUon() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = UonSerializer.DEFAULT_SIMPLE.clone().addFilters(f);
		ReaderParser p = new UonParser().addFilters(f);
		doTest(s, p, true, "-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongUrlParam - DEFAULT_LONG, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultLongUrlEncoding() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = UrlEncodingSerializer.DEFAULT_SIMPLE.clone().addFilters(f);
		ReaderParser p = new UrlEncodingParser().addFilters(f);
		doTest(s, p, true, "_value=-2172116928000");
	}

	//====================================================================================================
	// testDefaultLongRdfXml - DEFAULT_LONG, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultLongRdfXml() throws Exception {
		Class<?> f = CalendarLongFilter.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
		doTest(s, p, true, "<rdf:Description><j:value>-2172116928000</j:value></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultMapJson - DEFAULT_MAP, JSON
	//====================================================================================================
	@Test
	public void testDefaultMapJson() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, true, "{time:-2172116928000,timeZone:'PST'}");
	}

	//====================================================================================================
	// testDefaultMapXml - DEFAULT_MAP, XML
	//====================================================================================================
	@Test
	public void testDefaultMapXml() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = new XmlSerializer.SimpleXmlJsonSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, true, "<object><time type='number'>-2172116928000</time><timeZone>PST</timeZone></object>");
	}

	//====================================================================================================
	// testDefaultMapHtml - DEFAULT_MAP, HTML
	//====================================================================================================
	@Test
	public void testDefaultMapHtml() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = new HtmlSerializer.Sq().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, true, "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>time</string></td><td><number>-2172116928000</number></td></tr><tr><td><string>timeZone</string></td><td><string>PST</string></td></tr></table>");
	}

	//====================================================================================================
	// testDefaultMapUrlParam - DEFAULT_MAP, UON
	//====================================================================================================
	@Test
	public void testDefaultMapUon() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = UonSerializer.DEFAULT_SIMPLE.clone().addFilters(f);
		ReaderParser p = new UonParser().addFilters(f);
		doTest(s, p, true, "(time=-2172116928000,timeZone=PST)");
	}

	//====================================================================================================
	// testDefaultMapUrlParam - DEFAULT_MAP, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultMapUrlEncoding() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = UrlEncodingSerializer.DEFAULT_SIMPLE.clone().addFilters(f);
		ReaderParser p = new UrlEncodingParser().addFilters(f);
		doTest(s, p, true, "time=-2172116928000&timeZone=PST");
	}

	//====================================================================================================
	// testDefaultMapRdfXml - DEFAULT_MAP, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultMapRdfXml() throws Exception {
		Class<?> f = CalendarMapFilter.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
		doTest(s, p, true, "<rdf:Description><jp:time>-2172116928000</jp:time><jp:timeZone>PST</jp:timeZone></rdf:Description>");
	}

	//====================================================================================================
	// testDefaultMediumJson - DEFAULT_MEDIUM, JSON
	//====================================================================================================
	@Test
	public void testDefaultMediumJson() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, false, "'Mar 3, 1901'");
	}

	//====================================================================================================
	// testDefaultMediumXml - DEFAULT_MEDIUM, XML
	//====================================================================================================
	@Test
	public void testDefaultMediumXml() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = new XmlSerializer.SimpleSq().addFilters(f);
		ReaderParser p = new XmlParser().addFilters(f);
		doTest(s, p, false, "<string>Mar 3, 1901</string>");
	}

	//====================================================================================================
	// testDefaultMediumHtml - DEFAULT_MEDIUM, HTML
	//====================================================================================================
	@Test
	public void testDefaultMediumHtml() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = new HtmlSerializer().addFilters(f);
		ReaderParser p = new HtmlParser().addFilters(f);
		doTest(s, p, false, "<string>Mar 3, 1901</string>");
	}

	//====================================================================================================
	// testDefaultMediumUrlParam - DEFAULT_MEDIUM, UON
	//====================================================================================================
	@Test
	public void testDefaultMediumUon() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = new UonSerializer.Encoding().addFilters(f);
		ReaderParser p = UonParser.DEFAULT_DECODING.clone().addFilters(f);
		doTest(s, p, false, "Mar+3,+1901");
	}

	//====================================================================================================
	// testDefaultMediumUrlParam - DEFAULT_MEDIUM, URL-ENCODING
	//====================================================================================================
	@Test
	public void testDefaultMediumUrlEncoding() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = new UrlEncodingSerializer().addFilters(f);
		ReaderParser p = UrlEncodingParser.DEFAULT.clone().addFilters(f);
		doTest(s, p, false, "_value=Mar+3,+1901");
	}

	//====================================================================================================
	// testDefaultMediumRdfXml - DEFAULT_MEDIUM, RDF/XML
	//====================================================================================================
	@Test
	public void testDefaultMediumRdfXml() throws Exception {
		Class<?> f = CalendarFilter.Medium.class;
		WriterSerializer s = getRdfSerializer().addFilters(f);
		ReaderParser p = new RdfParser.Xml().addFilters(f);
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
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>d1</string></td><td><string>1901-03-03T18:11:12Z</string></td></tr><tr><td><string>d2</string></td><td><string>Sun, 03 Mar 1901 18:11:12 GMT</string></td></tr><tr><td><string>d3</string></td><td><number>-2172116928000</number></td></tr></table>", html);
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

	public static class A {
		@BeanProperty(filter=CalendarFilter.ISO8601DTZ.class)
		public Calendar d1;
		private Calendar d2, d3;
		public A(Calendar date) {
			d1 = d2 = d3 = date;
		}

		public A() {}

		@BeanProperty(filter=CalendarFilter.RFC2822DTZ.class)
		public Calendar getD2() {
			return d2;
		}
		public void setD2(Calendar d2) {
			this.d2 = d2;
		}

		public Calendar getD3() {
			return d3;
		}
		@BeanProperty(filter=CalendarLongFilter.class)
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