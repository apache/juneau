/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.core.jena.RdfProperties.*;
import static com.ibm.juno.core.jena.RdfSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static javax.xml.bind.DatatypeConverter.*;

import java.net.*;

import com.ibm.juno.core.dto.atom.*;
import com.ibm.juno.core.dto.atom.Content;
import com.ibm.juno.core.encoders.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample resource that shows how to generate ATOM feeds.
 */
@RestResource(
	path="/atom",
	messages="nls/AtomFeedResource",
	properties={
		@Property(name=SERIALIZER_quoteChar, value="'"),
		@Property(name=RDF_rdfxml_tab, value="5"),
		@Property(name=RDF_addRootProperty, value="true"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.AtomFeedResource)'}")
	},
	encoders=GzipEncoder.class
)
public class AtomFeedResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	private Feed feed;     // The root resource object

	@Override /* Servlet */
	public void init() {

		try {
			feed = new Feed()
				.setTitle(new Text("text", "Juno ATOM specification"))
				.setSubTitle(new Text("html", "A <em>lot</em> of effort went into making this effortless"))
				.setUpdated(parseDateTime("2013-05-08T12:29:29Z"))
				.setId(new Id("tag:juno.sample.com,2013:1"))
				.addLinks(
					new Link("alternate", "text/html", "http://www.sample.com/").setHreflang("en"),
					new Link("self", "application/atom+xml", "http://www.sample.com/feed.atom")
				)
				.setRights(new Text("Copyright (c) 2013, IBM"))
				.setGenerator(new Generator("Juno").setUri(new URI("http://juno.ibm.com/")).setVersion("1.0"))
				.addEntries(
					new Entry()
						.setTitle(new Text("Juno ATOM specification snapshot"))
						.addLinks(
							new Link("alternate", "text/html", "http://www.sample.com/2012/05/08/juno.atom"),
							new Link("enclosure", "audio/mpeg", "http://www.sample.com/audio/juno_podcast.mp3").setLength(12345)
						)
						.setId(new Id("tag:juno.sample.com,2013:1.2345"))
						.setUpdated(parseDateTime("2013-05-08T12:29:29Z"))
						.setPublished(parseDateTime("2013-05-08T12:29:29Z"))
						.addAuthors(new Person("James Bognar").setUri(new URI("http://www.sample.com/")).setEmail("jbognar@us.ibm.com"))
						.addContributors(
							new Person("Barry M. Caceres")
						)
						.setContent(
							new Content()
								.setLang("en")
								.setBase(new URI("http://www.ibm.com/"))
								.setType("xhtml")
								.setText("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><i>[Update: Juno supports ATOM.]</i></p></div>")
						)
				);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** 
	 * GET request handler 
	 */
	@RestMethod(name="GET", path="/")
	public Feed getFeed() throws Exception {
		return feed;
	}

	/**
	 * PUT request handler.
	 * Replaces the feed with the specified content, and then mirrors it as the response.
	 */
	@RestMethod(name="PUT", path="/")
	public Feed setFeed(@com.ibm.juno.server.annotation.Content Feed feed) throws Exception {
		this.feed = feed;
		return feed;
	}
}
