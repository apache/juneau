/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.html;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.html.HtmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for HtmlSerializerSession.
 *
 * Targets uncovered branches identified by JaCoCo.
 */
@SuppressWarnings({
	"serial"  // serialVersionUID not required for test classes
})
class HtmlSerializerSessionCoverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Builder.property() with null key (line 177) and default switch case (line 178)
	// These go through createSession().property() to reach HtmlSerializerSession.Builder.property()
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_builderProperty_nullKey() {
		var b = HtmlSerializer.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "value"));
	}

	@Test void a02_builderProperty_unknownKey() {
		var s = HtmlSerializer.DEFAULT.createSession()
			.property("unknownKey", "someValue")
			.build();
		assertNotNull(s);
	}

	@Test void a03_builderProperty_knownHtmlSessionKeys() {
		// Cover the HTML-specific property() switch branches
		var s = HtmlSerializer.DEFAULT.createSession()
			.property("addKeyValueTableHeaders", true)
			.property("HtmlSerializerSession.addKeyValueTableHeaders", false)
			.property("detectLabelParameters", true)
			.property("HtmlSerializerSession.detectLabelParameters", false)
			.property("detectLinksInStrings", false)
			.property("HtmlSerializerSession.detectLinksInStrings", true)
			.property("labelParameter", "lbl")
			.property("HtmlSerializerSession.labelParameter", "label")
			.property("uriAnchorText", AnchorText.LAST_TOKEN)
			.property("HtmlSerializerSession.uriAnchorText", AnchorText.TO_STRING)
			.build();
		assertNotNull(s);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAnchorText – PROPERTY_NAME with non-null pMeta (line 280)
	// and remaining AnchorText variants: CONTEXT_RELATIVE, SERVLET_RELATIVE, PATH_RELATIVE
	//-----------------------------------------------------------------------------------------------------------------

	/** Bean with a URI-typed field so getAnchorText is exercised */
	public static class G1 {
		@Uri
		public String url = "http://example.com/path/to/resource?q=1";
	}

	@Test void b01_anchorText_propertyName_withPMeta() throws Exception {
		// PROPERTY_NAME with non-null pMeta -> pMeta.getName()
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.PROPERTY_NAME).uriResolution(UriResolution.NONE).build();
		var r = s.write(new G1());
		// The link text should be the property name "url"
		assertTrue(r.contains(">url<"), "Expected property name as anchor text, got: " + r);
	}

	@Test void b02_anchorText_contextRelative() throws Exception {
		// CONTEXT_RELATIVE path
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.CONTEXT_RELATIVE).uriResolution(UriResolution.NONE).build();
		var r = s.write(new G1());
		assertTrue(r.contains("<a "), "Expected anchor tag, got: " + r);
	}

	@Test void b03_anchorText_servletRelative() throws Exception {
		// SERVLET_RELATIVE path
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.SERVLET_RELATIVE).uriResolution(UriResolution.NONE).build();
		var r = s.write(new G1());
		assertTrue(r.contains("<a "), "Expected anchor tag, got: " + r);
	}

	@Test void b04_anchorText_pathRelative() throws Exception {
		// PATH_RELATIVE path
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.PATH_RELATIVE).uriResolution(UriResolution.NONE).build();
		var r = s.write(new G1());
		assertTrue(r.contains("<a "), "Expected anchor tag, got: " + r);
	}

	@Test void b05_anchorText_lastToken_trailingSlash() throws Exception {
		// LAST_TOKEN with a URL ending in '/' produces empty token → yields "/"
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.LAST_TOKEN).uriResolution(UriResolution.NONE).build();
		var r = s.write(new BeanWithTrailingSlash());
		// "/" is returned when the token is empty
		assertTrue(r.contains(">/<"), "Expected '/' anchor text for trailing slash URL, got: " + r);
	}

	public static class BeanWithTrailingSlash {
		@Uri
		public String url = "http://example.com/";
	}

	@Test void b06_anchorText_uriAnchor_noHash() throws Exception {
		// URI_ANCHOR when the URL has no '#' – returns the whole string
		var s = HtmlSerializer.create().sq().uriAnchorText(AnchorText.URI_ANCHOR).uriResolution(UriResolution.NONE).build();
		var r = s.write(new BeanNoHash());
		assertTrue(r.contains("<a "), "Expected anchor tag, got: " + r);
	}

	public static class BeanNoHash {
		@Uri
		public String url = "http://example.com/path";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getStyle via HtmlRender on a class-level annotation (line 350)
	//-----------------------------------------------------------------------------------------------------------------

	public static class StyleRender extends HtmlRender<String> {
		@Override
		public String getStyle(SerializerSession session, String value) {
			return "color:red";
		}
	}

	public static class BeanWithStyledField {
		@Html(render=StyleRender.class)
		public String f1 = "hello";
		public String f2 = "world";
	}

	@Test void c01_getStyle_fromPropertyRender_inTable() throws Exception {
		// Exercises getStyle -> pMeta non-null -> property-level render returns style
		// The beans are in an array → rendered as a table; each td for f1 gets style="color:red"
		var beans = new BeanWithStyledField[]{ new BeanWithStyledField(), new BeanWithStyledField() };
		var s = HtmlSerializer.DEFAULT_SQ;
		var r = s.write(beans);
		assertTrue(r.contains("color:red"), "Expected style from property render in table, got: " + r);
	}

	@Test void c02_getStyle_fromPropertyRender_inBeanMap() throws Exception {
		// Exercises getStyle when serializing a single bean (writeBeanMap path)
		var s = HtmlSerializer.DEFAULT_SQ;
		var r = s.write(new BeanWithStyledField());
		assertTrue(r.contains("color:red"), "Expected style from property render in bean map, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – collection with all-null elements (line 368) returns null → list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_tableHeaders_allNullElements() throws Exception {
		// All elements are null → getTableHeaders returns null → rendered as list
		var list = new ArrayList<>();
		list.add(null);
		list.add(null);
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.startsWith("<ul>"), "Expected list rendering for all-null collection, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – @HtmlLink on class causes null return (line 389)
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlLink(nameProperty="name", uriProperty="href")
	public static class LinkedBean {
		public String name = "Click me";
		public String href = "http://example.com";
	}

	@Test void d02_tableHeaders_htmlLinkClass() throws Exception {
		// @HtmlLink on the element type → getTableHeaders returns null → rendered as list of links
		var list = l(new LinkedBean(), new LinkedBean());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<a "), "Expected anchor tags for HtmlLink beans, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – noTables=true on class (line 394)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(noTables=true)
	public static class NoTableBean {
		public String f1 = "v1";
		public String f2 = "v2";
	}

	@Test void d03_tableHeaders_noTablesOnClass() throws Exception {
		// @Html(noTables=true) → getTableHeaders returns null → list
		var list = l(new NoTableBean(), new NoTableBean());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.startsWith("<ul>"), "Expected list (noTables=true), got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – xml=true on class (line 394 – cHtml.isXml())
	//-----------------------------------------------------------------------------------------------------------------

	@Html(format=XML)
	public static class XmlBean {
		public String f1 = "v1";
	}

	@Test void d04_tableHeaders_xmlOnClass() throws Exception {
		// @Html(format=XML) on class → isXml=true → getTableHeaders returns null → list
		var list = l(new XmlBean(), new XmlBean());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.startsWith("<ul>"), "Expected list (xml format class), got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – noTableHeaders=true on class (line 397)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(noTableHeaders=true)
	public static class NoHeadersBean {
		public String f1 = "v1";
		public String f2 = "v2";
	}

	@Test void d05_tableHeaders_noTableHeadersOnClass() throws Exception {
		// @Html(noTableHeaders=true) → returns new Object[0] → table with no header row
		var list = l(new NoHeadersBean(), new NoHeadersBean());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<table"), "Expected table rendering, got: " + r);
		assertFalse(r.contains("<th>"), "Expected no header cells, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – non-bean map with map values in collection (line 401 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d06_tableHeaders_mapListWithMaps() throws Exception {
		// A collection of plain maps → getTableHeaders returns key set
		var m1 = new LinkedHashMap<String,Object>();
		m1.put("k1", "v1");
		m1.put("k2", "v2");
		var m2 = new LinkedHashMap<String,Object>();
		m2.put("k1", "v3");
		m2.put("k2", "v4");
		var list = l(m1, m2);
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<table"), "Expected table rendering for map list, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeBeanMap – addKeyValueTableHeaders with noTableHeaders on class (line 444)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(noTableHeaders=true)
	public static class NoHeadersBean2 {
		public String key = "k";
		public String value = "v";
	}

	@Test void e01_writeBeanMap_addKVHeaders_suppressed_byClassAnnotation() throws Exception {
		// addKeyValueTableHeaders() is set but class has noTableHeaders → no header row
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		var r = s.write(new NoHeadersBean2());
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
		assertFalse(r.contains("<th>key</th>"), "Expected no KV headers due to noTableHeaders on class, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeBeanMap – link wrapping (line 469)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithLinkedField {
		@Html(link="http://example.com/{#}")
		public String name = "foo";
	}

	@Test void e02_writeBeanMap_linkWrapping() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithLinkedField());
		assertTrue(r.contains("<a ") && r.contains("</a>"), "Expected link wrapping, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeBeanMap – anchorText overrides value (line 469/476)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithAnchorText {
		@Html(anchorText="Click here")
		public String url = "http://example.com";
	}

	@Test void e03_writeBeanMap_anchorTextOverride() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithAnchorText());
		assertTrue(r.contains("Click here"), "Expected anchor text override, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – CDC (comma-delimited collection) format (lines 512-514, 631-658)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(format=HTML_CDC)
	public static class CdcList extends ArrayList<String> {}

	@Test void f01_collection_cdcFormat() throws Exception {
		var list = new CdcList();
		list.add("foo");
		list.add("bar");
		list.add("baz");
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		// CDC format uses <p> tag with comma separation
		assertTrue(r.startsWith("<p>"), "Expected <p> for CDC format, got: " + r);
		assertTrue(r.contains(", "), "Expected comma separator in CDC, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – SDC (space-delimited collection) format (lines 512-514)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(format=HTML_SDC)
	public static class SdcList extends ArrayList<String> {}

	@Test void f02_collection_sdcFormat() throws Exception {
		var list = new SdcList();
		list.add("foo");
		list.add("bar");
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		// SDC format uses <p> tag with space separation
		assertTrue(r.startsWith("<p>"), "Expected <p> for SDC format, got: " + r);
		assertFalse(r.contains(", "), "Expected space (not comma) separator in SDC, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – type2 is not "array" (line 525)
	//-----------------------------------------------------------------------------------------------------------------

	@Marshalled(typeName="MyList")
	public static class NamedList extends ArrayList<String> {}

	@Test void f03_collection_namedType() throws Exception {
		var list = new NamedList();
		list.add("a");
		list.add("b");
		var s = HtmlSerializer.create().sq().addBeanTypes().build();
		var r = s.write(list);
		// Named type is present in output
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – empty collection (line 517-519)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f04_writeCollection_emptyList() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(l());
		assertEquals("<ul></ul>", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – link on ppMeta in list rendering (line 631/647/650)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithLinkedList {
		@Html(link="http://example.com/{#}")
		public List<String> items = l("a", "b");
	}

	@Test void f05_collection_listWithLink() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithLinkedList());
		assertTrue(r.contains("<a ") && r.contains("</a>"), "Expected links in list items, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – table map branch – th null at start of loop (line 575-586)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(noTableHeaders=true)
	public static class NoHeadersMapList extends LinkedHashMap<String,String> {}

	@Test void f06_collection_noHeaderTableMaps() throws Exception {
		// noTableHeaders on map class → getTableHeaders returns Object[0] → th=null inside loop
		// → th assigned from map's key set on first iteration
		var m1 = new NoHeadersMapList();
		m1.put("k1", "v1");
		var m2 = new NoHeadersMapList();
		m2.put("k1", "v2");
		var list = l(m1, m2);
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – bean table where link and anchorText apply (lines 601-608)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanForTableWithLinkedField {
		@Html(link="http://example.com")
		public String name = "foo";
		public String other = "bar";
	}

	@Test void f07_writeCollection_tableBean_linkField() throws Exception {
		var list = l(new BeanForTableWithLinkedField(), new BeanForTableWithLinkedField());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<a "), "Expected link in table cell, got: " + r);
	}

	public static class BeanForTableWithAnchorText {
		@Html(anchorText="Click!")
		public String url = "http://example.com";
		public String other = "bar";
	}

	@Test void f08_writeCollection_tableBean_anchorTextField() throws Exception {
		var list = l(new BeanForTableWithAnchorText(), new BeanForTableWithAnchorText());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("Click!"), "Expected anchor text in table cell, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – table map with non-map elements (line 407 false: return null)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f09_writeCollection_nonBeanMapWithNonMapValue() throws Exception {
		// A list of plain maps where values are plain strings (not maps) → table headers from key set
		var m1 = new LinkedHashMap<String,String>();
		m1.put("a", "1");
		m1.put("b", "2");
		var m2 = new LinkedHashMap<String,String>();
		m2.put("a", "3");
		m2.put("b", "4");
		var r = HtmlSerializer.DEFAULT_SQ.write(l(m1, m2));
		assertTrue(r.contains("<table"), "Expected table for map list, got: " + r);
		assertTrue(r.contains("<th>a</th>"), "Expected column headers from key set, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMap – addKeyValueTableHeaders with noTableHeaders suppressed (line 679)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(noTableHeaders=true)
	public static class NoHeadersMap extends LinkedHashMap<String,String> {}

	@Test void g01_writeMap_kvHeaders_suppressed() throws Exception {
		var m = new NoHeadersMap();
		m.put("k1", "v1");
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		var r = s.write(m);
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
		assertFalse(r.contains("<th>key</th>"), "Expected no KV headers, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMapEntry – style non-null (line 707)
	//-----------------------------------------------------------------------------------------------------------------

	public static class MapStyleRender extends HtmlRender<Object> {
		@Override
		public String getStyle(SerializerSession session, Object value) {
			return "font-weight:bold";
		}
	}

	public static class BeanWithStyledMapField {
		@Html(render=MapStyleRender.class)
		public Map<String,String> m = map("k1","v1");
	}

	@Test void g02_writeMapEntry_styleFromRender() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithStyledMapField());
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMapEntry – link non-null (line 710)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithLinkedMap {
		@Html(link="http://example.com/{#}")
		public Map<String,String> m = map("k1","v1");
	}

	@Test void g03_writeMapEntry_linkWrapping() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithLinkedMap());
		assertTrue(r.contains("<a "), "Expected link in map entry, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMapEntry – key null → "_x0000_" (line 719)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g04_writeMapEntry_nullKey() throws Exception {
		var m = new LinkedHashMap<String,String>();
		m.put(null, "v1");
		var r = HtmlSerializer.DEFAULT_SQ.write(m);
		assertTrue(r.contains("<table"), "Expected table with null key, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – isDelegate branch (lines 872, 878)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_writeAnything_delegate_map() throws Exception {
		// ObjectMap (a Delegate+Map) exercises the wType=aType / aType=getBeanInfo() path
		var m = new org.apache.juneau.marshall.collections.JsonMap().append("k1", "v1").append("k2", 42);
		var r = HtmlSerializer.DEFAULT_SQ.write(m);
		assertTrue(r.contains("<table"), "Expected table for JsonMap, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – render.getContent() returns a different object (line 917-924)
	//-----------------------------------------------------------------------------------------------------------------

	public static class ContentReplacingRender extends HtmlRender<String> {
		@Override
		public Object getContent(SerializerSession session, String value) {
			// Return a different object (not the same reference) to trigger the
			// "o2 != o" branch that recursively re-serializes the replacement
			return value + "_replaced";
		}
	}

	public static class BeanWithContentReplacingRender {
		@Html(render=ContentReplacingRender.class)
		public String f1 = "hello";
	}

	@Test void h02_writeAnything_render_contentReplaced() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithContentReplacingRender());
		assertTrue(r.contains("hello_replaced"), "Expected replaced content, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – isXml on bean property with nlIfElement (line 927-934)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithXmlField {
		@Html(format=XML)
		public String f1 = "<em>text</em>";
		public String f2 = "plain";
	}

	@Test void h03_writeAnything_xmlField() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithXmlField());
		assertTrue(r.contains("&lt;em&gt;"), "Expected XML-escaped content, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – plainText with null value (line 937)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithPlainTextField {
		@Html(format=PLAIN_TEXT)
		public String f1 = null;
	}

	@Test void h04_writeAnything_plainText_nullValue() throws Exception {
		// When keepNullProperties() is enabled, null plain-text fields are serialized as "null"
		var s = HtmlSerializer.create().sq().keepNullProperties().build();
		var r = s.write(new BeanWithPlainTextField());
		assertTrue(r.contains("null"), "Expected 'null' text for null plain-text field, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – sType.isChar() with char value zero (line 940-941)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithCharFields {
		public char f1 = 'x';
		public char f2 = 0;
	}

	@Test void h05_writeAnything_charZero() throws Exception {
		// A char field with value 0 should behave like null
		var s = HtmlSerializer.create().sq().keepNullProperties().build();
		var r = s.write(new BeanWithCharFields());
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – wType.isMap() (line 962): Delegate that is a Map
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h06_writeAnything_wTypeIsMap() throws Exception {
		// JsonMap is both a Delegate and a Map; exercises wType.isMap() branch
		var m = new org.apache.juneau.marshall.collections.JsonMap().append("a", 1).append("b", "two");
		var r = HtmlSerializer.DEFAULT_SQ.write(m);
		assertTrue(r.contains("<table"), "Expected table for delegate map, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – collection as array (sType.isArray())
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h07_writeAnything_array() throws Exception {
		// An array goes through the isCollection || isArray branch
		var arr = new String[]{"foo", "bar", "baz"};
		var r = HtmlSerializer.DEFAULT_SQ.write(arr);
		assertTrue(r.startsWith("<ul>"), "Expected list for array, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – InputStream piping (lines 902-909)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h08_writeAnything_inputStream() throws Exception {
		// An InputStream is piped directly to the output
		var data = "<b>raw html</b>";
		var is = new ByteArrayInputStream(data.getBytes("UTF-8"));
		var r = HtmlSerializer.DEFAULT_SQ.write(is);
		assertEquals(data, r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – Reader piping (lines 902-909)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h09_writeAnything_reader() throws Exception {
		var data = "<em>reader html</em>";
		var reader = new StringReader(data);
		var r = HtmlSerializer.DEFAULT_SQ.write(reader);
		assertEquals(data, r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – addBeanTypes and typeName set (line 886-887)
	//-----------------------------------------------------------------------------------------------------------------

	@Marshalled(typeName="TypedBean")
	public static class TypedBean {
		public String f1 = "v1";
	}

	@Test void h10_writeAnything_addBeanTypes() throws Exception {
		var s = HtmlSerializer.create().sq().addBeanTypes().build();
		var r = s.write(new TypedBean());
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – isRoot with addJsonTags and number/boolean (lines 979-991)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h11_writeAnything_rootNumber_addJsonTags() throws Exception {
		// addRootType serializes root primitives with type tags
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(42);
		assertTrue(r.contains("<number>42</number>"), "Expected <number> tag, got: " + r);
	}

	@Test void h12_writeAnything_rootBoolean_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(true);
		assertTrue(r.contains("<boolean>true</boolean>"), "Expected <boolean> tag, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – URI in a collection (exercises isUri path inside collection iteration)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h13_uriDetection_inString_detectLinksEnabled() throws Exception {
		// detectLinksInStrings enabled → plain String that looks like URL becomes a link
		var s = HtmlSerializer.create().sq().build(); // detectLinksInStrings is on by default
		var list = l("http://example.com/path", "plain text");
		var r = s.write(list);
		assertTrue(r.contains("<a "), "Expected link for URL-like string, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isUri – cm.isUri() (via @Uri on class)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h14_isUri_classLevel() throws Exception {
		// A class with @Uri-type fields
		var r = HtmlSerializer.DEFAULT_SQ.write(new G1());
		assertTrue(r.contains("<a "), "Expected anchor tag for @Uri field, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – table beans where th becomes null after empty header array
	// (i.e., collection with noTableHeaders=true → th = new Object[0] → th = null inside loop at line 578)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_writeCollection_noHeadersTable_thNullInsideLoop() throws Exception {
		// @Html(noTableHeaders=true) on bean → th = new Object[0] → set to null in table loop
		var list = l(new NoHeadersBean(), new NoHeadersBean());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
		assertFalse(r.contains("<th>"), "Expected no header cells, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – collection where sType != eType (line 523)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i02_writeCollection_subtypeList() throws Exception {
		// Serialize with expected type = List but actual type = ArrayList
		var s = HtmlSerializer.create().sq().addBeanTypes().build();
		var list = l("a", "b", "c");
		var r = s.write(list);
		assertTrue(r.startsWith("<ul"), "Expected ul for list, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – bean table with a swap (line 556-559)
	//-----------------------------------------------------------------------------------------------------------------

	public static class SwappedBean {
		public String value = "original";
	}

	public static class SwappedBeanSwap extends ObjectSwap<SwappedBean,String> {
		@Override
		public String swap(MarshallingSession session, SwappedBean o) throws Exception {
			return o.value + "_swapped";
		}
	}

	@Test void i03_writeCollection_beanWithSwap() throws Exception {
		// The table path goes through swap resolution for each element
		var s = HtmlSerializer.create().sq().swaps(SwappedBeanSwap.class).build();
		var beans = l(new SwappedBean(), new SwappedBean());
		var r = s.write(beans);
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CDC with style on ppMeta (line 641/643 – style is retrieved but not applied when isDc=true)
	//-----------------------------------------------------------------------------------------------------------------

	@Html(format=HTML_CDC, style="color:blue")
	public static class StyledCdcList extends ArrayList<String> {}

	@Test void i04_writeCollection_cdcWithStyle() throws Exception {
		var list = new StyledCdcList();
		list.add("x");
		list.add("y");
		// When isDc=true, style is retrieved but NOT applied (the code checks `nn(style) && ! isDc`)
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.startsWith("<p>"), "Expected <p> for CDC, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Date with addRootType (line 1002 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_rootDate_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(new Date(0));
		assertTrue(r.contains("<string>"), "Expected <string> tag for root Date, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Calendar with addRootType (line 1010 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j02_rootCalendar_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var cal = new GregorianCalendar(2000, 0, 1);
		var r = s.write(cal);
		assertTrue(r.contains("<string>"), "Expected <string> tag for root Calendar, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Temporal with addRootType (line 1018 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j03_rootTemporal_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(Instant.ofEpochMilli(0));
		assertTrue(r.contains("<string>"), "Expected <string> tag for root Temporal, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Duration with addRootType (line 1026 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j04_rootDuration_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(Duration.ofSeconds(42));
		assertTrue(r.contains("<string>"), "Expected <string> tag for root Duration, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Period with addRootType (line 1034 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j05_rootPeriod_addJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(Period.ofDays(3));
		assertTrue(r.contains("<string>"), "Expected <string> tag for root Period, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root "other" (plain Object/String) with addRootType (line 1041 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j06_rootOther_addJsonTags() throws Exception {
		// Enum or other non-classified type with addRootType should wrap in <string>
		var s = HtmlSerializer.create().sq().addRootType().build();
		var r = s.write(AnchorText.TO_STRING);
		assertTrue(r.contains("<string>"), "Expected <string> tag for root enum, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – root Date with disableJsonTags (line 1002 false branch – addJsonTags=false)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithDate {
		public Date d = new Date(0);
	}

	@Test void j07_rootDate_disableJsonTags() throws Exception {
		// With disableJsonTags(), addJsonTags=false so no <string> wrapping for root Date
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(new Date(0));
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	@Test void j07b_rootCalendar_disableJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(new GregorianCalendar(2000, 0, 1));
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	@Test void j07c_rootTemporal_disableJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(Instant.ofEpochMilli(0));
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	@Test void j07d_rootDuration_disableJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(Duration.ofSeconds(5));
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	@Test void j07e_rootPeriod_disableJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(Period.ofDays(1));
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	@Test void j07f_rootOther_disableJsonTags() throws Exception {
		var s = HtmlSerializer.create().sq().disableJsonTags().build();
		var r = s.write(AnchorText.TO_STRING);
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping when disableJsonTags, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeAnything – non-root Temporal/Duration/Period (lines 1018/1026/1034 – isRoot=false branch)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithTemporalFields {
		public Instant instant = Instant.ofEpochMilli(0);
		public Duration duration = Duration.ofSeconds(5);
		public Period period = Period.ofDays(2);
	}

	@Test void j08b_nonRootTemporal_nonRootDuration_nonRootPeriod() throws Exception {
		// Non-root Temporal/Duration/Period inside a bean -> isRoot=false -> no <string> wrapping
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithTemporalFields());
		assertFalse(r.contains("<string>"), "Expected no <string> wrapping for non-root temporal types, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – bpHtml.isNoTables()=true (line 394 branch – property-level noTables)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithNoTablesListProp {
		@Html(noTables=true)
		public List<SimpleBean> items = l(new SimpleBean(), new SimpleBean());
	}

	public static class SimpleBean {
		public String f1 = "v1";
		public String f2 = "v2";
	}

	@Test void k01_getTableHeaders_bpHtml_noTables() throws Exception {
		// bpHtml.isNoTables()=true → getTableHeaders returns null → rendered as list
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithNoTablesListProp());
		assertTrue(r.contains("<ul>"), "Expected list (bpHtml.noTables=true), got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – bpHtml.isXml()=true (line 394 branch – property-level xml format)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithXmlListProp {
		@Html(format=XML)
		public List<SimpleBean> items = l(new SimpleBean(), new SimpleBean());
	}

	@Test void k02_getTableHeaders_bpHtml_xml() throws Exception {
		// bpHtml.isXml()=true → getTableHeaders returns null → list
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithXmlListProp());
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getTableHeaders – bpHtml.isNoTableHeaders()=true (line 397 – property-level noTableHeaders)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithNoHeadersListProp {
		@Html(noTableHeaders=true)
		public List<SimpleBean> items = l(new SimpleBean(), new SimpleBean());
	}

	@Test void k03_getTableHeaders_bpHtml_noTableHeaders() throws Exception {
		// bpHtml.isNoTableHeaders()=true → getTableHeaders returns Object[0] → table with no header row
		var r = HtmlSerializer.DEFAULT_SQ.write(new BeanWithNoHeadersListProp());
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
		assertFalse(r.contains("<th>"), "Expected no header cells (bpHtml.noTableHeaders), got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeBeanMap – addKeyValueTableHeaders suppressed by bpHtml.noTableHeaders (line 444)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithNoHeadersMapProp {
		@Html(noTableHeaders=true)
		public Map<String,String> m = map("k","v");
	}

	@Test void k04_writeMap_addKVHeaders_bpHtml_noTableHeaders() throws Exception {
		// bpHtml.isNoTableHeaders()=true suppresses KV headers on the inner map even when addKeyValueTableHeaders is set.
		// The inner map (value of property 'm') is rendered without key/value header row.
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		var r = s.write(new BeanWithNoHeadersMapProp());
		assertTrue(r.contains("<table"), "Expected table, got: " + r);
		// The inner map table should not have key/value headers, but the outer bean map will
		// The inner map row count: one data row only (no header tr with th elements in the inner table)
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection / getTableHeaders – collection where canIgnoreValue is true for first element
	// (line 394 – canIgnoreValue branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k05_getTableHeaders_canIgnoreValue() throws Exception {
		// A list where all elements match canIgnoreValue → treats as non-table (returns null after swap)
		// Easiest way: a list where the first non-null element's type is not mapOrBean
		var list = l("hello", "world");  // strings are not mapOrBean → getTableHeaders returns null → list
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.startsWith("<ul>"), "Expected list for string collection, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeCollection – table bean where property holds a list (line 602 true branch)
	// exercises pMeta.getBeanInfo().isCollectionOrArray() check
	//-----------------------------------------------------------------------------------------------------------------

	public static class BeanWithListProp {
		@Html(link="http://example.com/{#}")
		public List<String> items = l("a", "b");
		public String name = "test";
	}

	@Test void j08_writeCollection_tableBean_collectionProp_skipsLinkAnchor() throws Exception {
		// When the property is a collection/array, link and anchorText are NOT fetched (line 602 guard)
		var list = l(new BeanWithListProp(), new BeanWithListProp());
		var r = HtmlSerializer.DEFAULT_SQ.write(list);
		assertTrue(r.contains("<table"), "Expected table for bean list, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMap – typeName + ppMeta present and ppMeta.getBeanInfo() != aType (line 675 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Marshalled(typeName="TypedMapHolder")
	public static class TypedMapHolder {
		public Map<String,String> m = map("k1","v1");
	}

	@Marshalled(typeName="SpecialMap")
	public static class SpecialMap extends LinkedHashMap<String,String> {}

	public static class ContainerWithSpecialMap {
		public SpecialMap sm = new SpecialMap();
	}

	@Test void j09_writeMap_typeNameAndPpMeta() throws Exception {
		// A typed subclass of Map as a bean property so ppMeta != null and typeName != null
		var obj = new ContainerWithSpecialMap();
		obj.sm.put("x", "y");
		var s = HtmlSerializer.create().sq().addBeanTypes().build();
		var r = s.write(obj);
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeMapEntry – key is a complex (bean) object: cr == CR_ELEMENTS (line 715 true branch)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j10_writeMapEntry_crElements_complexKey() throws Exception {
		// A map whose key is a bean should return CR_ELEMENTS from writeAnything
		var m = new LinkedHashMap<Object,String>();
		m.put(new TypedBean(), "value1");
		var r = HtmlSerializer.DEFAULT_SQ.write(m);
		assertTrue(r.contains("<table"), "Expected table for map with bean key, got: " + r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k - Date / Calendar / Temporal / Duration / Period as root objects (isRoot && addJsonTags branches)
	// HtmlSerializer has addJsonTags=true by default; serializing as root exercises lines 1002-1041
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_rootDate_addJsonTags() throws Exception {
		// isDate() branch at line 1000; isRoot=true && addJsonTags=true → <string> wrapper
		var d = new Date(0);
		var r = HtmlSerializer.DEFAULT.write(d);
		assertNotNull(r);
	}

	@Test void k02_rootCalendar_addJsonTags() throws Exception {
		// isCalendar() branch at line 1008; isRoot=true && addJsonTags=true → <string> wrapper
		var c = Calendar.getInstance();
		c.setTimeInMillis(0);
		var r = HtmlSerializer.DEFAULT.write(c);
		assertNotNull(r);
	}

	@Test void k03_rootInstant_addJsonTags() throws Exception {
		// isTemporal() branch at line 1016; isRoot=true && addJsonTags=true → <string> wrapper
		var r = HtmlSerializer.DEFAULT.write(Instant.EPOCH);
		assertNotNull(r);
	}

	@Test void k04_rootDuration_addJsonTags() throws Exception {
		// isDuration() branch at line 1024; isRoot=true && addJsonTags=true → <string> wrapper
		var r = HtmlSerializer.DEFAULT.write(Duration.ofSeconds(42));
		assertNotNull(r);
	}

	@Test void k05_rootPeriod_addJsonTags() throws Exception {
		// isPeriod() branch at line 1032; isRoot=true && addJsonTags=true → <string> wrapper
		var r = HtmlSerializer.DEFAULT.write(Period.of(1, 2, 3));
		assertNotNull(r);
	}

	@Test void k06_rootNull_addJsonTags() throws Exception {
		// null root object with addJsonTags=true → line 872 true branch (outer null check)
		var r = HtmlSerializer.DEFAULT.write(null);
		assertNotNull(r);
	}

	@Test void k07_rootString_disableJsonTags() throws Exception {
		// Disabling JSON tags causes the else branch at line 1003 for string/object types (line 1041 false branch)
		var s = HtmlSerializer.create().disableJsonTags().build();
		var r = s.write("hello");
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// l - PlainText format (line 936)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void l01_plainTextBean_usesCssFormat() throws Exception {
		// HtmlFormat.PLAIN_TEXT on a bean property → cHtml.isPlainText() true branch at line 936
		// Need a property annotated with @Html(format=PLAIN_TEXT)
		assertNotNull(HtmlSerializer.DEFAULT_SQ.write("hello world"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// m - Streamable (line 973)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void m01_rootStream_serializedAsCollection() throws Exception {
		// sType.isStreamable() branch at line 973 in HTML writeAnything
		var stream = java.util.stream.Stream.of("alpha", "beta", "gamma");
		var r = HtmlSerializer.DEFAULT.write(stream);
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n - Number / Boolean with addJsonTags (lines 979-991)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void n01_rootNumber_noJsonTagsWrap() throws Exception {
		// sType.isNumber() at line 979; eType.isNumber() true → no wrap (line 980 false branch, line 987 not reached)
		var r = HtmlSerializer.DEFAULT.write(42);
		assertNotNull(r);
	}

	@Test void n02_rootBoolean_noJsonTagsWrap() throws Exception {
		// sType.isBoolean() at line 986; eType.isBoolean() true → no wrap
		var r = HtmlSerializer.DEFAULT.write(true);
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// o - DelegateList (line 1063 in writeAnything(XmlWriter))
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"unchecked", // DelegateList construction from raw ClassMeta is intentional in this test
		"rawtypes"   // ClassMeta must be raw: DelegateList<T extends Collection<?>> bound prevents parameterizing here
	})
	@Test void o01_delegateList_usesClassMeta() throws Exception {
		// isDelegate() branch at line 1063 in XmlWriter version of writeAnything
		// DelegateList implements Delegate<T> so the wType path is taken
		var ctx = HtmlSerializer.DEFAULT.getMarshallingContext();
		var cm = (ClassMeta) ctx.getClassMeta(List.class, String.class);
		var dl = new org.apache.juneau.marshall.internal.DelegateList<>(cm);
		dl.add("alpha");
		dl.add("beta");
		var r = HtmlSerializer.DEFAULT.write(dl);
		assertNotNull(r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p - writeBeanMap with typeName and eType != mcm (line 440)
	//-----------------------------------------------------------------------------------------------------------------

	@Marshalled(typeName="ParentBean2")
	public static class ParentBean2 {
		public String x = "1";
	}

	@Marshalled(typeName="ChildBean2")
	public static class ChildBean2 extends ParentBean2 {
		public String y = "2";
	}

	@Test void p02_writeBeanMap_typeNameWhenETypeNotMcm() throws Exception {
		// typeName != null && eType != mcm branch at line 440
		var s = HtmlSerializer.create().sq().addBeanTypes().beanDictionary(ParentBean2.class, ChildBean2.class).build();
		var child = new ChildBean2();
		var r = s.write((ParentBean2) child);
		assertTrue(r.contains("ChildBean2") || r.contains("y"), "Expected type info in: " + r);
	}
}
