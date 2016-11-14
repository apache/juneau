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

import static javax.xml.bind.DatatypeConverter.*;
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;

import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class AtomTest {

	@Test
	public void testBasic() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String expected, r;
		Feed f2;

		Feed f = new Feed()
			.setTitle(new Text("text", "Title"))
			.setSubTitle(new Text("html", "Subtitle"))
			.setUpdated(parseDateTime("2016-12-31T01:02:03-04:00"))
			.setId(new Id("tag:foo.org"))
			.addLinks(
				new Link("alternate", "text/html", "http://foo.org/").setHreflang("en"),
				new Link("self", "application/atom+xml", "http://foo.org/feed.atom")
			)
			.setRights(new Text("Copyright (c) 2016, Apache Foundation"))
			.setGenerator(new Generator("Example Toolkit").setUri(new URI("http://www.foo.org/")).setVersion("1.0"))
			.addEntries(
				new Entry()
					.setTitle(new Text("Title"))
					.addLinks(
						new Link("alternate", "text/html", "http://foo.org/2005/04/02/atom"),
						new Link("enclosure", "audio/mpeg", "http://foo.org/audio/foobar.mp3").setLength(1337)
					)
					.setId(new Id("tag:foo.org"))
					.setUpdated(parseDateTime("2016-12-31T01:02:03-04:00"))
					.setPublished(parseDateTime("2016-12-31T01:02:03-04:00"))
					.addAuthors(new Person("John Smith").setUri(new URI("http://foo.org/")).setEmail("foo@foo.org"))
					.addContributors(
						new Person("John Smith"),
						new Person("Jane Smith")
					)
					.setContent(
						new Content()
							.setLang("en")
							.setBase(new URI("http://foo.org/"))
							.setType("xhtml")
							.setText("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><i>[Sample content]</i></p></div>")
					)
			)
		;

		//--------------------------------------------------------------------------------
		// Test without namespaces
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable().setProperty(XML_enableNamespaces, false).setProperty(BEAN_sortProperties, true);
		expected = readFile(getClass().getResource("/dto/atom/test1.xml").getPath());
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);

		//--------------------------------------------------------------------------------
		// Test with namespaces
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable().setProperty(BEAN_sortProperties, true);
		expected = readFile(getClass().getResource("/dto/atom/test2.xml").getPath());
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);

		//--------------------------------------------------------------------------------
		// Test with namespaces but with atom as the default namespace
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable().setProperty(XML_defaultNamespaceUri, "atom").setProperty(BEAN_sortProperties, true);
		expected = readFile(getClass().getResource("/dto/atom/test3.xml").getPath());
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);
	}
}
