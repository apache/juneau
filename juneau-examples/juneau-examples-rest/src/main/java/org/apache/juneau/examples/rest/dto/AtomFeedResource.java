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

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.atom.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Sample resource that shows how to generate ATOM feeds.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/atom",
	title="Sample ATOM feed resource",
	description="Sample resource that shows how to render ATOM feeds",
	encoders=GzipEncoder.class,
	swagger=@Swagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"up: request:/..",
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/dto/AtomFeedResource.java"
	}
)
@SerializerConfig(
	quoteChar="'"
)
public class AtomFeedResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private Feed feed;     // The root resource object

	@Override /* Servlet */
	public void init() {
		try {
			feed =
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * [HTTP GET /dto/atom]
	 * Get the sample ATOM feed
	 *
	 * @return The sample ATOM feed.
	 */
	@RestGet(
		summary="Get the sample ATOM feed"
	)
	public Feed get() {
		return feed;
	}

	/**
	 * [HTTP PUT /dto/atom]
	 * Overwrite the sample ATOM feed
	 *
	 * @param feed The new ATOM feed.
	 * @return The updated ATOM feed.
	 */
	@RestPut(
		summary="Overwrite the sample ATOM feed",
		description="Replaces the feed with the specified content, and then mirrors it as the response."
	)
	public Feed put(@Content Feed feed) {
		this.feed = feed;
		return feed;
	}
}
