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
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.json.Json5Serializer;

/**
 * Atom feed JSON example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class AtomJsonExample {

	/**
	 * JSON Atom feed example.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		Feed feed = AtomFeed.getAtomFeed();

		// Get JSON serializer with readable output.
		JsonSerializer s = Json5Serializer.DEFAULT_READABLE;

		// Serialize to ATOM/JSON
		//Produces
		/**
		 * {
		 *  id: {
		 *      text: 'tag:juneau.apache.org'
		 *  },
		 *  links: [
		 *      {
		 *          href: 'http://juneau.apache.org/',
		 *          rel: 'alternate',
		 *          type: 'text/html',
		 *          hreflang: 'en'
		 *      },
		 *      {
		 *          href: 'http://juneau.apache.org/juneau.atom',
		 *          rel: 'self',
		 *          type: 'application/atom+xml'
		 *      }
		 *  ],
		 *  title: {
		 *      type: 'text',
		 *      text: 'Juneau ATOM specification'
		 *  },
		 *  updated: '2016-01-02T03:04:05Z',
		 *  generator: {
		 *      uri: 'http://juneau.apache.org/',
		 *      version: '1.0',
		 *      text: 'Juneau'
		 *  },
		 *  subtitle: {
		 *      type: 'html',
		 *      text: 'Describes <em>stuff</em> about Juneau'
		 *  },
		 *  entries: [
		 *      {
		 *          authors: [
		 *              {
		 *                  name: 'James Bognar',
		 *                  uri: 'http://juneau.apache.org/',
		 *                  email: 'jamesbognar@apache.org'
		 *              }
		 *          ],
		 *  contributors: [
		 *      {
		 *          name: 'Barry M. Caceres'
		 *      }
		 *  ],
		 *  id: {
		 *      text: 'tag:juneau.apache.org'
		 *  },
		 *  links: [
		 *      {
		 *          href: 'http://juneau.apache.org/juneau.atom',
		 *          rel: 'alternate',
		 *          type: 'text/html'
		 *      },
		 *      {
		 *          href: 'http://juneau.apache.org/audio/juneau_podcast.mp3',
		 *          rel: 'enclosure',
		 *          type: 'audio/mpeg',
		 *          length: 12345
		 *      }
		 *  ],
		 *  title: {
		 *      text: 'Juneau ATOM specification snapshot'
		 *  },
		 *  updated: '2016-01-02T03:04:05Z',
		 *  content: {
		 *      base: 'http://www.apache.org/',
		 *      lang: 'en',
		 *      type: 'xhtml',
		 *      text: '<div xmlns="http://www.w3.org/1999/xhtml"><p><i>[Update: Juneau supports ATOM.]</i></p></div>'
		 *  },
		 *  published: '2016-01-02T03:04:05Z'
		 *  }
		 *  ]
		 *  }
		 */
		String atomJson = s.serialize(feed);
	}
}