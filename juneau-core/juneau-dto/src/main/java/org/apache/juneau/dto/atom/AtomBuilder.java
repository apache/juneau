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

import java.net.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Various useful static methods for creating ATOM elements.
 * 
 * <p>
 * Typically, you'll want to do a static import on this class and then call the methods like so...
 * <p class='bcode'>
 * 	<jk>import static</jk> org.apache.juneau.dto.atom.AtomBuilder.*;
 * 
 * 	Feed feed =
 * 		<jsm>feed</jsm>(<js>"tag:juneau.sample.com,2013:1"</js>, <js>"Juneau ATOM specification"</js>,
 * 			<js>"2013-05-08T12:29:29Z"</js>)
 * 		.subtitle(<jsm>text</jsm>(<js>"html"</js>)
 * 			.children(<js>"A &lt;em&gt;lot&lt;/em&gt; of effort went into making this effortless"</js>))
 * 		.links(
 * 			<jsm>link</jsm>(<js>"alternate"</js>, <js>"text/html"</js>, <js>"http://www.sample.com/"</js>)
 * 				.hreflang(<js>"en"</js>),
 * 			<jsm>link</jsm>(<js>"self"</js>, <js>"application/atom+xml"</js>, <js>"http://www.sample.com/feed.atom"</js>)
 * 		);
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Atom'>Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
public class AtomBuilder {

	/**
	 * Creates a {@link Category} element with the specified {@link Category#term(String)} attribute.
	 * 
	 * @param term The {@link Category#term(String)} attribute.
	 * @return The new element.
	 */
	public static final Category category(String term) {
		return new Category(term);
	}

	/**
	 * Creates a {@link Content} element with the specified {@link Content#type(String)} attribute.
	 * 
	 * @return The new element.
	 */
	public static final Content content() {
		return new Content();
	}

	/**
	 * Creates a {@link Content} element.
	 * 
	 * @param type The {@link Content#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Content content(String type) {
		return new Content(type);
	}

	/**
	 * Creates an {@link Entry} element with the specified {@link Entry#id(Id)}, {@link Entry#title(Text)}, and
	 * {@link Entry#updated(Calendar)} attributes.
	 * 
	 * @param id The {@link Entry#id(Id)} attribute.
	 * @param title The {@link Entry#title(Text)} attribute.
	 * @param updated The {@link Entry#updated(Calendar)} attribute.
	 * @return The new element.
	 */
	public static final Entry entry(Id id, Text title, Calendar updated) {
		return new Entry(id, title, updated);
	}

	/**
	 * Creates an {@link Entry} element with the specified {@link Entry#id(Id)}, {@link Entry#title(Text)}, and
	 * {@link Entry#updated(Calendar)} attributes.
	 * 
	 * @param id The {@link Entry#id(Id)} attribute.
	 * @param title The {@link Entry#title(Text)} attribute.
	 * @param updated The {@link Entry#updated(Calendar)} attribute.
	 * @return The new element.
	 */
	public static final Entry entry(String id, String title, String updated) {
		return new Entry(id, title, updated);
	}

	/**
	 * Creates a {@link Feed} element with the specified {@link Feed#id(Id)}, {@link Entry#title(Text)}, and
	 * {@link Feed#updated(Calendar)} attributes.
	 * 
	 * @param id The {@link Feed#id(Id)} attribute.
	 * @param title The {@link Feed#title(Text)} attribute.
	 * @param updated The {@link Feed#updated(Calendar)} attribute.
	 * @return The new element.
	 */
	public static final Feed feed(Id id, Text title, Calendar updated) {
		return new Feed(id, title, updated);
	}

	/**
	 * Creates a {@link Feed} element with the specified {@link Feed#id(Id)}, {@link Entry#title(Text)}, and
	 * {@link Feed#updated(Calendar)} attributes.
	 * 
	 * @param id The {@link Feed#id(Id)} attribute.
	 * @param title The {@link Feed#title(Text)} attribute.
	 * @param updated The {@link Feed#updated(Calendar)} attribute.
	 * @return The new element.
	 */
	public static final Feed feed(String id, String title, String updated) {
		return new Feed(id, title, updated);
	}

	/**
	 * Creates a {@link Generator} element with the specified {@link Generator#text(String)} child node.
	 * 
	 * @param text The {@link Generator#text(String)} child node.
	 * @return The new element.
	 */
	public static final Generator generator(String text) {
		return new Generator(text);
	}

	/**
	 * Creates an {@link Icon} element with the specified {@link Icon#uri(Object)} attribute.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @param uri The {@link Icon#uri(Object)} attribute.
	 * @return The new element.
	 */
	public static final Icon icon(Object uri) {
		return new Icon(uri);
	}

	/**
	 * Creates an {@link Id} element with the specified {@link Id#text(String)} child node.
	 * 
	 * @param text The {@link Id#text(String)} child node.
	 * @return The new element.
	 */
	public static final Id id(String text) {
		return new Id(text);
	}

	/**
	 * Creates a {@link Link} element with the specified {@link Link#rel(String)}, {@link Link#type(String)}, and
	 * {@link Link#href(String)} attributes.
	 * 
	 * @param rel The {@link Link#rel(String)} attribute.
	 * @param type The {@link Link#type(String)} attribute.
	 * @param href The {@link Link#href(String)} attribute.
	 * @return The new element.
	 */
	public static final Link link(String rel, String type, String href) {
		return new Link(rel, type, href);
	}

	/**
	 * Creates a {@link Logo} element with the specified {@link Logo#uri(Object)} attribute.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @param uri The {@link Logo#uri(Object)} attribute.
	 * @return The new element.
	 */
	public static final Logo logo(Object uri) {
		return new Logo(uri);
	}

	/**
	 * Creates a {@link Person} element with the specified {@link Person#name(String)} attribute.
	 * 
	 * @param name The {@link Person#name(String)} attribute.
	 * @return The new element.
	 */
	public static final Person person(String name) {
		return new Person(name);
	}

	/**
	 * Creates a {@link Source} element.
	 * 
	 * @return The new element.
	 */
	public static final Source source() {
		return new Source();
	}

	/**
	 * Creates a {@link Text} element.
	 * 
	 * @return The new element.
	 */
	public static final Text text() {
		return new Text();
	}

	/**
	 * Creates a {@link Text} element with the specified {@link Text#type(String)} attribute.
	 * 
	 * @param type The {@link Text#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Text text(String type) {
		return new Text(type);
	}
}
