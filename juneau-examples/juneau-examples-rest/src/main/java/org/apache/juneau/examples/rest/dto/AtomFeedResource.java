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
package org.apache.juneau.examples.rest.dto;

import static org.apache.juneau.dto.atom.AtomBuilder.*;

import java.net.URI;

import org.apache.juneau.jsonschema.annotation.ExternalDocs;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.atom.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Sample resource that shows how to generate ATOM feeds.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@RestResource(
	path="/atom",
	title="Sample ATOM feed resource",
	description="Sample resource that shows how to render ATOM feeds",
	encoders=GzipEncoder.class,
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class,
		ThemeMenuItem.class
	},
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"$W{ContentTypeMenuItem}",
		"$W{ThemeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/dto/$R{servletClassSimple}.java"
	}
)
@SerializerConfig(
	quoteChar="'"
)
@RdfConfig(
	rdfxml_tab="5",
	addRootProperty="true"
)
@BeanConfig(
	examples="Feed: $F{AtomFeedResource_example.json}"
)
public class AtomFeedResource extends BasicRestServletJena {
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
	 * Get the sample ATOM feed
	 *
	 * @return The sample ATOM feed.
	 */
	@RestMethod(
		summary="Get the sample ATOM feed"
	)
	public Feed get() {
		return feed;
	}

	/**
	 * Overwrite the sample ATOM feed
	 *
	 * @param feed The new ATOM feed.
	 * @return The updated ATOM feed.
	 */
	@RestMethod(
		summary="Overwrite the sample ATOM feed",
		description="Replaces the feed with the specified content, and then mirrors it as the response."
	)
	public Feed put(@Body Feed feed) {
		this.feed = feed;
		return feed;
	}
}
