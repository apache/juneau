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
package org.apache.juneau.examples.core.dto.atom;

import org.apache.juneau.dto.atom.Feed;

import static org.apache.juneau.dto.atom.AtomBuilder.*;

import java.net.*;

/**
 * Atom feed example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class AtomFeed {

	/**
	 * @return A sample Atom feed.
	 * @throws URISyntaxException Won't happen
	 */
	public static Feed getAtomFeed() throws URISyntaxException{

		Feed feed =
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

		return feed;

	}
}
