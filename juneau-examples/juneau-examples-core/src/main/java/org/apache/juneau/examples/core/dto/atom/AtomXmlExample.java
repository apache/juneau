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
import org.apache.juneau.xml.XmlSerializer;

/**
 * Atom feed XML example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class AtomXmlExample {

	/**
	 * XML Atom feed example.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {


		Feed feed = AtomFeed.getAtomFeed();

		// Example with no namespaces
		// Create a serializer with readable output, no namespaces yet.
		XmlSerializer s = XmlSerializer.create().sq().ws().build();

		//Produces
		/**
		 *<feed>
		 *<id> tag:juneau.apache.org</id>
		 *<link href='http://juneau.apache.org/' rel='alternate' type='text/html' hreflang='en'/>
		 *<link href='http://juneau.apache.org/feed.atom' rel='self' type='application/atom+xml'/>
		 *<title type='text'>Juneau ATOM specification</title>
		 *<updated>2016-01-02T03:04:05Z</updated>
		 *<generator uri='http://juneau.apache.org/' version='1.0'>Juneau</generator>
		 *<subtitle type='html'>Describes <em>stuff</em> about Juneau</subtitle>
		 *<entry>
		 *  <author>
		 *      <name>Jane Smith</name>
		 *      <uri>http://juneau.apache.org/</uri>
		 *      <email>janesmith@apache.org</email>
		 *  </author>
		 *  <contributor>
		 *      <name>John Smith</name>
		 *  </contributor>
		 *  <id>tag:juneau.apache.org</id>
		 *  <link href='http://juneau.apache.org/juneau.atom' rel='alternate' type='text/html'/>
		 *  <link href='http://juneau.apache.org/audio/juneau_podcast.mp3' rel='enclosure' type='audio/mpeg' length='12345'/>
		 *  <title>Juneau ATOM specification snapshot</title>
		 *  <updated>2016-01-02T03:04:05Z</updated>
		 *  <content base='http://www.apache.org/' lang='en' type='xhtml'>
		 *      <div xmlns="http://www.w3.org/1999/xhtml"><p><i>[Update: Juneau supports ATOM.]</i></p></div>
		 *  </content>
		 *  <published>2016-01-02T03:04:05Z</published>
		 *</entry>
		 *</feed>
		 */
		//Serialize to ATOM/XML
		String atomXml = s.serialize(feed);

		/**
		 * Produces
		 * <feed>
		 * <link hreflang='en' rel='alternate' href='http://juneau.apache.org' type='text/html'/>
		 * <link rel='self' href='http://juneau.apache.org/feed.atom' type='application/atom+xml'/>
		 * <title>Juneau ATOM specification</title>
		 * <updated>2016-01-02T03:04:05Z</updated>
		 * <id>tag:juneau.apache.org</id>
		 * <subtitle type='html'>Describes <em>stuff</em> about Juneau</subtitle>
		 * <generator version='1.0' uri='http://juneau.apache.org'>Juneau</generator>
		 * <entry>
		 *  <link rel='alternate' href='http://juneau.apache.org/juneau.atom' type='text/html'/>
		 *  <link rel='enclosure' href='http://juneau.apache.org/audio/juneau_podcast.mp3' type='audio/mpeg' length='1337'/>
		 *  <author>
		 *      <uri>http://juneau.apache.org</uri>
		 *      <email>janesmith@apache.org</email>
		 *      <name>Jane Smith</name>
		 *  </author>
		 *  <contributor>
		 *      <name>John Smith</name>
		 *  </contributor>
		 *  <title>Juneau ATOM specification snapshot</title>
		 *  <updated>2016-01-02T03:04:05Z</updated>
		 *  <id>tag:juneau.sample.com,2013:1.2345</id>
		 *  <published>2016-01-02T03:04:05Z</published>
		 *  <content lang='en' base='http://www.apache.org/' type='xhtml'><div><p><i>[Update: Juneau supports ATOM.]</i></p></div></content>
		 * </entry>
		 * </feed>
		 */
		// Create a serializer with readable output, no namespaces yet.
		XmlSerializer ns = XmlSerializer.create().sq().ws().build();

		// Serialize to ATOM/XML
		atomXml = ns.serialize(feed);
	}
}
