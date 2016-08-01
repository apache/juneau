/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.dto.atom;

import static com.ibm.juno.core.test.TestUtils.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;
import static javax.xml.bind.DatatypeConverter.*;
import static org.junit.Assert.*;

import java.net.*;

import org.junit.*;

import com.ibm.juno.core.dto.atom.*;
import com.ibm.juno.core.xml.*;


public class CT_Atom {

	@Test
	public void testBasic() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String expected, r;
		Feed f2;

		Feed f = new Feed()
			.setTitle(new Text("text", "dive into mark"))
			.setSubTitle(new Text("html", "A <em>lot</em> of effort went into making this effortless"))
			.setUpdated(parseDateTime("2005-07-31T12:29:29Z"))
			.setId(new Id("tag:example.org,2003:3"))
			.addLinks(
				new Link("alternate", "text/html", "http://example.org/").setHreflang("en"),
				new Link("self", "application/atom+xml", "http://example.org/feed.atom")
			)
			.setRights(new Text("Copyright (c) 2003, Mark Pilgrim"))
			.setGenerator(new Generator("Example Toolkit").setUri(new URI("http://www.example.com/")).setVersion("1.0"))
			.addEntries(
				new Entry()
					.setTitle(new Text("Atom draft-07 snapshot"))
					.addLinks(
						new Link("alternate", "text/html", "http://example.org/2005/04/02/atom"),
						new Link("enclosure", "audio/mpeg", "http://example.org/audio/ph34r_my_podcast.mp3").setLength(1337)
					)
					.setId(new Id("tag:example.org,2003:3.2397"))
					.setUpdated(parseDateTime("2005-07-31T12:29:29Z"))
					.setPublished(parseDateTime("2003-12-13T08:29:29-04:00"))
					.addAuthors(new Person("Mark Pilgrim").setUri(new URI("http://example.org/")).setEmail("f8dy@example.com"))
					.addContributors(
						new Person("Sam Ruby"),
						new Person("Joe Gregorio")
					)
					.setContent(
						new Content()
							.setLang("en")
							.setBase(new URI("http://diveintomark.org/"))
							.setType("xhtml")
							.setText("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><i>[Update: The Atom draft is finished.]</i></p></div>")
					)
			)
		;

		//--------------------------------------------------------------------------------
		// Test without namespaces
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable().setProperty(XML_enableNamespaces, false);
		expected = readFile("/com/ibm/juno/core/test/dto/atom/test1.xml");
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);

		//--------------------------------------------------------------------------------
		// Test with namespaces
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable();
		expected = readFile("/com/ibm/juno/core/test/dto/atom/test2.xml");
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);

		//--------------------------------------------------------------------------------
		// Test with namespaces but with atom as the default namespace
		//--------------------------------------------------------------------------------
		s = new XmlSerializer.SqReadable().setProperty(XML_defaultNamespaceUri, "atom");
		expected = readFile("/com/ibm/juno/core/test/dto/atom/test3.xml");
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);
	}
}
