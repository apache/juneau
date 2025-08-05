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
package org.apache.juneau.dto.atom;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.bean.atom.AtomBuilder.*;
import static org.junit.Assert.*;
import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.atom.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class AtomTest extends SimpleTestBase {

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

	@Test void testNormal() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected = """
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
		s = XmlSerializer.create().sq().ws().sortProperties().build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertObject(f).isSameJsonAs(f2);
	}

	@Test void testWithNamespaces() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected = """
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
		s = XmlSerializer.create().sq().ws().enableNamespaces().addNamespaceUrisToRoot().sortProperties().build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertObject(f).isSameJsonAs(f2);
	}

	@Test void testWithNamespacesWithAtomAsDefault() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected = """
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
		s = XmlSerializer.create().sq().ws().defaultNamespace(Namespace.of("atom")).enableNamespaces().addNamespaceUrisToRoot().sortProperties().build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertObject(f).isSameJsonAs(f2);
	}

	@Test void testToString() throws Exception {
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;
		r = f.toString();
		f2 = p.parse(r, Feed.class);
		assertObject(f).isSameJsonAs(f2);
	}
}