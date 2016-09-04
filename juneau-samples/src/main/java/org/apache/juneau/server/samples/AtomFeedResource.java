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
package org.apache.juneau.server.samples;

import static javax.xml.bind.DatatypeConverter.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;

import java.net.*;

import org.apache.juneau.dto.atom.*;
import org.apache.juneau.dto.atom.Content;
import org.apache.juneau.encoders.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.annotation.*;

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
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.server.samples.AtomFeedResource)'}")
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
				.setTitle(new Text("text", "Juneau ATOM specification"))
				.setSubTitle(new Text("html", "A <em>lot</em> of effort went into making this effortless"))
				.setUpdated(parseDateTime("2013-05-08T12:29:29Z"))
				.setId(new Id("tag:juneau.sample.com,2013:1"))
				.addLinks(
					new Link("alternate", "text/html", "http://www.sample.com/").setHreflang("en"),
					new Link("self", "application/atom+xml", "http://www.sample.com/feed.atom")
				)
				.setRights(new Text("Copyright (c) 2013, IBM"))
				.setGenerator(new Generator("Juneau").setUri(new URI("http://juneau.ibm.com/")).setVersion("1.0"))
				.addEntries(
					new Entry()
						.setTitle(new Text("Juneau ATOM specification snapshot"))
						.addLinks(
							new Link("alternate", "text/html", "http://www.sample.com/2012/05/08/juneau.atom"),
							new Link("enclosure", "audio/mpeg", "http://www.sample.com/audio/juneau_podcast.mp3").setLength(12345)
						)
						.setId(new Id("tag:juneau.sample.com,2013:1.2345"))
						.setUpdated(parseDateTime("2013-05-08T12:29:29Z"))
						.setPublished(parseDateTime("2013-05-08T12:29:29Z"))
						.addAuthors(new Person("James Bognar").setUri(new URI("http://www.sample.com/")).setEmail("james.bognar@salesforce.com"))
						.addContributors(
							new Person("Barry M. Caceres")
						)
						.setContent(
							new Content()
								.setLang("en")
								.setBase(new URI("http://www.ibm.com/"))
								.setType("xhtml")
								.setText("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><i>[Update: Juneau supports ATOM.]</i></p></div>")
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
	public Feed setFeed(@org.apache.juneau.server.annotation.Content Feed feed) throws Exception {
		this.feed = feed;
		return feed;
	}
}
