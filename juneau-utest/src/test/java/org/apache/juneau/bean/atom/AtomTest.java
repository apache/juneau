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
package org.apache.juneau.bean.atom;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.atom.AtomBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class AtomTest extends TestBase {

	public Feed createFeed() throws Exception {
		return
			feed("tag:foo.org", "Title", "2016-12-31T05:02:03Z")
			.setSubtitle(text("html").setText("Subtitle"))
			.setLinks(
				link("alternate", "text/html", "http://foo.org/").setHreflang("en"),
				link("self", "application/atom+xml", "http://foo.org/feed.atom")
			)
			.setGenerator(
				generator("Example Toolkit").setUri("http://www.foo.org/").setVersion("1.0")
			)
			.setEntries(
				entry("tag:foo.org", "Title", "2016-12-31T05:02:03Z")
				.setLinks(
					link("alternate", "text/html", "http://foo.org/2005/04/02/atom"),
					link("enclosure", "audio/mpeg", "http://foo.org/audio/foobar.mp3").setLength(1337)
				)
				.setPublished("2016-12-31T05:02:03Z")
				.setAuthors(
					person("John Smith").setUri(new URI("http://foo.org/")).setEmail("foo@foo.org")
				)
				.setContributors(
					person("John Smith"),
					person("Jane Smith")
				)
				.setContent(
					content("xhtml")
					.setLang("en")
					.setBase("http://foo.org/")
					.setText("<div><p><i>[Sample content]</i></p></div>")
				)
			);
	}

	@Test void a01_normal() throws Exception {
		var p = XmlParser.DEFAULT;
		var f = createFeed();

		var expected = """
		<feed>
			<entry>
				<author>
					<email>foo@foo.org</email>
					<name>John Smith</name>
					<uri>http://foo.org/</uri>
				</author>
				<content base='http://foo.org/' lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></content>
				<contributor>
					<name>John Smith</name>
				</contributor>
				<contributor>
					<name>Jane Smith</name>
				</contributor>
				<id>tag:foo.org</id>
				<link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>
				<link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>
				<published>2016-12-31T05:02:03Z</published>
				<title>Title</title>
				<updated>2016-12-31T05:02:03Z</updated>
			</entry>
			<generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</generator>
			<id>tag:foo.org</id>
			<link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>
			<link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>
			<subtitle type='html'>Subtitle</subtitle>
			<title>Title</title>
			<updated>2016-12-31T05:02:03Z</updated>
		</feed>
		""";
		var s = XmlSerializer.create().sq().ws().sortProperties().build();
		var r = s.serialize(f);
		assertEquals(expected, r);
		var f2 = p.parse(r, Feed.class);
		assertEquals(json(f2), json(f));
	}

	@Test void a02_withNamespaces() throws Exception {
		var p = XmlParser.DEFAULT;
		var f = createFeed();

		var expected = """
		<atom:feed xmlns='http://www.apache.org/2013/Juneau' xmlns:atom='http://www.w3.org/2005/Atom/' xmlns:xml='http://www.w3.org/XML/1998/namespace'>
			<atom:entry>
				<atom:author>
					<atom:email>foo@foo.org</atom:email>
					<atom:name>John Smith</atom:name>
					<atom:uri>http://foo.org/</atom:uri>
				</atom:author>
				<atom:content xml:base='http://foo.org/' xml:lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></atom:content>
				<atom:contributor>
					<atom:name>John Smith</atom:name>
				</atom:contributor>
				<atom:contributor>
					<atom:name>Jane Smith</atom:name>
				</atom:contributor>
				<atom:id>tag:foo.org</atom:id>
				<atom:link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>
				<atom:link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>
				<atom:published>2016-12-31T05:02:03Z</atom:published>
				<atom:title>Title</atom:title>
				<atom:updated>2016-12-31T05:02:03Z</atom:updated>
			</atom:entry>
			<atom:generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</atom:generator>
			<atom:id>tag:foo.org</atom:id>
			<atom:link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>
			<atom:link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>
			<atom:subtitle type='html'>Subtitle</atom:subtitle>
			<atom:title>Title</atom:title>
			<atom:updated>2016-12-31T05:02:03Z</atom:updated>
		</atom:feed>
		""";
		var s = XmlSerializer.create().sq().ws().enableNamespaces().addNamespaceUrisToRoot().sortProperties().build();
		var r = s.serialize(f);
		assertEquals(expected, r);
		var f2 = p.parse(r, Feed.class);
		assertEquals(json(f2), json(f));
	}

	@Test void a03_withNamespacesWithAtomAsDefault() throws Exception {
		var p = XmlParser.DEFAULT;
		var f = createFeed();

		var expected = """
		<feed xmlns='http://www.w3.org/2005/Atom/' xmlns:xml='http://www.w3.org/XML/1998/namespace'>
			<entry>
				<author>
					<email>foo@foo.org</email>
					<name>John Smith</name>
					<uri>http://foo.org/</uri>
				</author>
				<content xml:base='http://foo.org/' xml:lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></content>
				<contributor>
					<name>John Smith</name>
				</contributor>
				<contributor>
					<name>Jane Smith</name>
				</contributor>
				<id>tag:foo.org</id>
				<link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>
				<link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>
				<published>2016-12-31T05:02:03Z</published>
				<title>Title</title>
				<updated>2016-12-31T05:02:03Z</updated>
			</entry>
			<generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</generator>
			<id>tag:foo.org</id>
			<link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>
			<link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>
			<subtitle type='html'>Subtitle</subtitle>
			<title>Title</title>
			<updated>2016-12-31T05:02:03Z</updated>
		</feed>
		""";
		var s = XmlSerializer.create().sq().ws().defaultNamespace(Namespace.of("atom")).enableNamespaces().addNamespaceUrisToRoot().sortProperties().build();
		var r = s.serialize(f);
		assertEquals(expected, r);
		var f2 = p.parse(r, Feed.class);
		assertEquals(json(f2), json(f));
	}

	@Test void a04_toString() throws Exception {
		var p = XmlParser.DEFAULT;
		var f = createFeed();
		var r = f.toString();
		var f2 = p.parse(r, Feed.class);
		assertEquals(json(f2), json(f));
	}

	// AtomBuilder factory methods not covered by a01-a04
	@Test void b01_atomBuilderMissingFactoryMethods() {
		assertBean(category("tech"), "term", "tech");
		assertBean(content(), "type", "<null>");
		assertBean(id("tag:example.org"), "text", "tag:example.org");
		assertBean(logo("http://example.org/logo.png"), "uri", "http://example.org/logo.png");
		assertBean(source(), "generator", "<null>");
		assertBean(text(), "text", "<null>");
	}

	@Test void b02_atomBuilderObjectConstructors() {
		var idObj = id("tag:example.org");
		var titleObj = text("text").setText("Title");
		var cal = GregorianCalendar.from(java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z"));

		var a = entry(idObj, titleObj, cal);
		assertBean(a, "id{text}", "{tag:example.org}");

		var b = feed(idObj, titleObj, cal);
		assertBean(b, "id{text}", "{tag:example.org}");
	}

	// Category class - completely uncovered
	@Test void b03_category() {
		var a = category("tech")
			.setLabel("Technology")
			.setScheme("http://example.org/schemes/")
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(a, "term,label,scheme", "tech,Technology,http://example.org/schemes/");
		assertBean(new Category(), "term", "<null>");
	}

	// Logo class - completely uncovered
	@Test void b04_logo() {
		var a = logo("http://example.org/logo.png")
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(a, "uri", "http://example.org/logo.png");
		assertBean(new Logo(), "uri", "<null>");
		a.setUri(null);
		assertBean(a, "uri", "<null>");
	}

	// Source class - completely uncovered
	@Test void b05_source() {
		var gen = generator("Test Gen");
		var ico = icon("http://example.org/icon.png");
		var lgo = logo("http://example.org/logo.png");
		var a = source()
			.setGenerator(gen)
			.setIcon(ico)
			.setLogo(lgo)
			.setId("tag:example.org")
			.setId(id("tag:example.org"))
			.setTitle("My Source")
			.setTitle(text("text").setText("My Source"))
			.setSubtitle("Sub")
			.setSubtitle(text("text").setText("Sub"))
			.setRights("Copyright 2024")
			.setRights(text("text").setText("Copyright 2024"))
			.setUpdated("2024-01-01T00:00:00Z")
			.setUpdated((Calendar) null)
			.setAuthors(person("Author"))
			.setCategories(category("tech"))
			.setContributors(person("Contributor"))
			.setLinks(link("alternate", "text/html", "http://example.org/"))
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(a, "generator{text},icon{uri},logo{uri}", "{Test Gen},{http://example.org/icon.png},{http://example.org/logo.png}");
		assertBean(a, "id{text},title{text},subtitle{text},rights{text},updated", "{tag:example.org},{My Source},{Sub},{Copyright 2024},<null>");
		assertBean(a, "authors{#{name}},categories{#{term}},contributors{#{name}},links{#{href}}", "{[{Author}]},{[{tech}]},{[{Contributor}]},{[{http://example.org/}]}");
	}

	// CommonEntry missing setters
	@Test void b06_commonEntryMissingSetters() {
		var cal = GregorianCalendar.from(java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z"));
		var a = feed("id", "Title", "2024-01-01T00:00:00Z")
			.setBase("http://example.org/")
			.setCategories(category("tech"))
			.setContributors(person("Contrib"))
			.setId("new-id")
			.setLang("fr")
			.setLinks(link("alternate", "text/html", "http://example.org/"))
			.setRights("Copyright 2024")
			.setRights(text("text").setText("Copyright 2024"))
			.setUpdated(cal)
			.setUpdated("");
		assertBean(a, "id{text},rights{text},updated", "{new-id},{Copyright 2024},<null>");
		assertBean(a, "categories{#{term}},contributors{#{name}},links{#{href}}", "{[{tech}]},{[{Contrib}]},{[{http://example.org/}]}");
	}

	// Entry missing constructors and setters
	@Test void b07_entryMissingConstructorsAndSetters() {
		var idObj = id("eid");
		var titleObj = text("text").setText("Entry Title");
		var cal = GregorianCalendar.from(java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z"));
		var a = new Entry(idObj, titleObj, cal);
		assertBean(a, "id{text}", "{eid}");

		var src = source().setTitle("Source Feed").setUpdated("2024-01-01T00:00:00Z");
		a.setBase("http://example.org/")
			.setCategories(category("cat"))
			.setSource(src);
		assertBean(a.getSource(), "title{text}", "{Source Feed}");
	}

	// Link missing setters
	@Test void b08_linkMissingSetters() {
		var a = link("alternate", "text/html", "http://example.org/")
			.setBase("http://example.org/")
			.setLang("en")
			.setTitle("My Link");
		assertBean(a, "title", "My Link");
	}

	// Person missing setters
	@Test void b09_personMissingSetters() {
		var a = person("Jane Doe")
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(a, "name", "Jane Doe");
	}

	// Entry additional missing setters
	@Test void b10_entryAdditionalSetters() {
		var a = entry("eid", "title", "2024-01-01T00:00:00Z")
			.setLang("de")
			.setRights("Copyright")
			.setRights(text("text").setText("Copyright text"))
			.setSummary("Brief summary")
			.setSummary(text("text").setText("Full summary text"))
			.setPublished("");
		assertBean(a, "rights{text},summary{text},published", "{Copyright text},{Full summary text},<null>");
	}

	// Feed additional missing setters
	@Test void b11_feedAdditionalSetters() {
		var a = feed("fid", "Feed Title", "2024-01-01T00:00:00Z")
			.setAuthors(person("Author"))
			.setIcon(icon("http://example.org/icon.png"))
			.setLogo(logo("http://example.org/logo.png"))
			.setSubtitle("Plain subtitle");
		assertBean(a, "icon{uri},logo{uri},subtitle{type}", "{http://example.org/icon.png},{http://example.org/logo.png},{Plain subtitle}");
		assertBean(a, "authors{#{name}}", "{[{Author}]}");
	}

	// Generator, Icon, Id - missing setBase and setLang overrides
	@Test void b12_miscBeanSetters() {
		var gen = generator("Test Generator")
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(gen, "text", "Test Generator");

		var ico = new Icon()
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(ico, "uri", "<null>");

		var idBean = id("tag:example.org")
			.setBase("http://example.org/")
			.setLang("en");
		assertBean(idBean, "text", "tag:example.org");
	}
}