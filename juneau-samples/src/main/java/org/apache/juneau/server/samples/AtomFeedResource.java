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

import static org.apache.juneau.dto.atom.AtomBuilder.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;

import java.net.*;

import org.apache.juneau.dto.atom.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.annotation.Body;

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
			feed = 
				feed("tag:juneau.sample.com,2013:1", "Juneau ATOM specification", "2013-05-08T12:29:29Z")
				.subtitle(text("html").text("A <em>lot</em> of effort went into making this effortless"))
				.links(
					link("alternate", "text/html", "http://www.sample.com/").hreflang("en"),
					link("self", "application/atom+xml", "http://www.sample.com/feed.atom")
				)
				.rights("Copyright (c) 2016, Apache Foundation")
				.generator(
					generator("Juneau").uri("http://juneau.apache.org/").version("1.0")
				)
				.entries(
					entry("tag:juneau.sample.com,2013:1.2345", "Juneau ATOM specification snapshot", "2013-05-08T12:29:29Z")
					.links(
						link("alternate", "text/html", "http://www.sample.com/2012/05/08/juneau.atom"),
						link("enclosure", "audio/mpeg", "http://www.sample.com/audio/juneau_podcast.mp3").length(1337)
					)
					.published("2013-05-08T12:29:29Z")
					.authors(
						person("James Bognar").uri(new URI("http://www.sample.com/")).email("jamesbognar@apache.org")
					)
					.contributors(
						person("Barry M. Caceres")
					)
					.content(
						content("xhtml")
						.lang("en")
						.base("http://www.apache.org/")
						.text("<div><p>[Update: Juneau supports ATOM.]</p></div>")
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
	public Feed setFeed(@Body Feed feed) throws Exception {
		this.feed = feed;
		return feed;
	}
}
