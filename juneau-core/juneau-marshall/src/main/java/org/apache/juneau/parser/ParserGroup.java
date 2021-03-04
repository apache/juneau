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
package org.apache.juneau.parser;

import static org.apache.juneau.http.header.StandardHttpHeaders.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;

/**
 * Represents a group of {@link Parser Parsers} that can be looked up by media type.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Finds parsers based on HTTP <c>Content-Type</c> header values.
 * 	<li>
 * 		Sets common properties on all parsers in a single method call.
 * 	<li>
 * 		Locks all parsers in a single method call.
 * 	<li>
 * 		Clones existing groups and all parsers within the group in a single method call.
 * </ul>
 *
 * <h5 class='topic'>Match ordering</h5>
 *
 * Parsers are matched against <c>Content-Type</c> strings in the order they exist in this group.
 *
 * <p>
 * Adding new entries will cause the entries to be prepended to the group.
 * This allows for previous parsers to be overridden through subsequent calls.
 *
 * <p>
 * For example, calling <code>g.append(P1.<jk>class</jk>,P2.<jk>class</jk>).append(P3.<jk>class</jk>,P4.<jk>class</jk>)</code>
 * will result in the order <c>P3, P4, P1, P2</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct a new parser group builder</jc>
 * 	ParserGroupBuilder b = ParserGroup.<jsm>create</jsm>();
 *
 * 	<jc>// Add some parsers to it</jc>
 * 	b.append(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
 *
 * 	<jc>// Change settings on parsers simultaneously</jc>
 * 	b.set(BeanContext.<jsf>BEAN_beansRequireSerializable</jsf>)
 * 		.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>);
 *
 * 	ParserGroup g = b.build();
 *
 * 	<jc>// Find the appropriate parser by Content-Type</jc>
 * 	ReaderParser p = (ReaderParser)g.getParser(<js>"text/json"</js>);
 *
 * 	<jc>// Parse a bean from JSON</jc>
 * 	String json = <js>"{...}"</js>;
 * 	AddressBook addressBook = p.parse(json, AddressBook.<jk>class</jk>);
 * </p>
 */
@ConfigurableContext(nocache=true)
public final class ParserGroup extends BeanContext {

	/**
	 * An unmodifiable empty parser group.
	 */
	public static final ParserGroup EMPTY = create().build();

	// Maps Content-Type headers to matches.
	private final ConcurrentHashMap<String,ParserMatch> cache = new ConcurrentHashMap<>();

	private final List<MediaType> mediaTypes;
	private final List<Parser> mediaTypeParsers;
	private final List<Parser> parsers;

	/**
	 * Instantiates a new clean-slate {@link ParserGroupBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> ParserGroupBuilder()</code>.
	 *
	 * @return A new {@link ParserGroupBuilder} object.
	 */
	public static ParserGroupBuilder create() {
		return new ParserGroupBuilder();
	}

	/**
	 * Returns a builder that's a copy of the settings on this parser group.
	 *
	 * @return A new {@link ParserGroupBuilder} initialized to this group.
	 */
	@Override /* Context */
	public ParserGroupBuilder builder() {
		return new ParserGroupBuilder(this);
	}

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The modifiable properties that were used to initialize the parsers.
	 * 	A snapshot of these will be made so that we can clone and modify this group.
	 * @param parsers
	 * 	The parsers defined in this group.
	 * 	The order is important because they will be tried in reverse order (e.g. newer first) in which they will be
	 * 	tried to match against media types.
	 */
	public ParserGroup(ContextProperties cp, Parser[] parsers) {
		super(cp);
		this.parsers = AList.unmodifiable(parsers);

		AList<MediaType> lmt = AList.create();
		AList<Parser> l = AList.create();
		for (Parser p : parsers) {
			for (MediaType m: p.getMediaTypes()) {
				lmt.add(m);
				l.add(p);
			}
		}

		this.mediaTypes = lmt.unmodifiable();
		this.mediaTypeParsers = l.unmodifiable();
	}

	/**
	 * Searches the group for a parser that can handle the specified <l>Content-Type</l> header value.
	 *
	 * <p>
	 * The returned object includes both the parser and media type that matched.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header value.
	 * @return The parser and media type that matched the content type header, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(String contentTypeHeader) {
		ParserMatch pm = cache.get(contentTypeHeader);
		if (pm != null)
			return pm;

		ContentType ct = contentType(contentTypeHeader);
		int match = ct.match(mediaTypes);

		if (match >= 0) {
			pm = new ParserMatch(mediaTypes.get(match), mediaTypeParsers.get(match));
			cache.putIfAbsent(contentTypeHeader, pm);
		}

		return cache.get(contentTypeHeader);
	}

	/**
	 * Same as {@link #getParserMatch(String)} but matches using a {@link MediaType} instance.
	 *
	 * @param mediaType The HTTP <l>Content-Type</l> header value as a media type.
	 * @return The parser and media type that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(MediaType mediaType) {
		return getParserMatch(mediaType.toString());
	}

	/**
	 * Same as {@link #getParserMatch(String)} but returns just the matched parser.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header string.
	 * @return The parser that matched the content type header, or <jk>null</jk> if no match was made.
	 */
	public Parser getParser(String contentTypeHeader) {
		ParserMatch pm = getParserMatch(contentTypeHeader);
		return pm == null ? null : pm.getParser();
	}

	/**
	 * Same as {@link #getParserMatch(MediaType)} but returns just the matched parser.
	 *
	 * @param mediaType The HTTP media type.
	 * @return The parser that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public Parser getParser(MediaType mediaType) {
		ParserMatch pm = getParserMatch(mediaType);
		return pm == null ? null : pm.getParser();
	}

	/**
	 * Returns the media types that all parsers in this group can handle
	 *
	 * <p>
	 * Entries are ordered in the same order as the parsers in the group.
	 *
	 * @return An unmodifiable list of media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return mediaTypes;
	}

	/**
	 * Returns the parsers in this group.
	 *
	 * @return An unmodifiable list of parsers in this group.
	 */
	public List<Parser> getParsers() {
		return parsers;
	}

	/**
	 * Returns <jk>true</jk> if this group contains no parsers.
	 *
	 * @return <jk>true</jk> if this group contains no parsers.
	 */
	public boolean isEmpty() {
		return parsers.isEmpty();
	}
}
