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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;

/**
 * Represents a group of {@link Parser Parsers} that can be looked up by media type.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>Finds parsers based on HTTP <code>Content-Type</code> header values.
 * 	<li>Sets common properties on all parsers in a single method call.
 * 	<li>Locks all parsers in a single method call.
 * 	<li>Clones existing groups and all parsers within the group in a single method call.
 * </ul>
 *
 * <h6 class='topic'>Match ordering</h6>
 * <p>
 * 	Parsers are matched against <code>Content-Type</code> strings in the order they exist in this group.
 * <p>
 * 	Adding new entries will cause the entries to be prepended to the group.
 *  	This allows for previous parsers to be overridden through subsequent calls.
 * <p>
 * 	For example, calling <code>g.append(P1.<jk>class</jk>,P2.<jk>class</jk>).append(P3.<jk>class</jk>,P4.<jk>class</jk>)</code>
 * 	will result in the order <code>P3, P4, P1, P2</code>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct a new parser group</jc>
 * 	ParserGroup g = <jk>new</jk> ParserGroup();
 *
 * 	<jc>// Add some parsers to it</jc>
 * 	g.append(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
 *
 * 	<jc>// Change settings on parsers simultaneously</jc>
 * 	g.setProperty(BeanContext.<jsf>BEAN_beansRequireSerializable</jsf>, <jk>true</jk>)
 * 		.addPojoSwaps(CalendarSwap.ISO8601DT.<jk>class</jk>)
 * 		.lock();
 *
 * 	<jc>// Find the appropriate parser by Content-Type</jc>
 * 	ReaderParser p = (ReaderParser)g.getParser(<js>"text/json"</js>);
 *
 * 	<jc>// Parse a bean from JSON</jc>
 * 	String json = <js>"{...}"</js>;
 * 	AddressBook addressBook = p.parse(json, AddressBook.<jk>class</jk>);
 * </p>
 */
public final class ParserGroup extends Lockable {

	// Maps Content-Type headers to matches.
	private final Map<String,ParserMatch> cache = new ConcurrentHashMap<String,ParserMatch>();

	private final CopyOnWriteArrayList<Parser> parsers = new CopyOnWriteArrayList<Parser>();

	/**
	 * Adds the specified parser to the beginning of this group.
	 *
	 * @param p - The parser to add to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroup append(Parser p) {
		checkLock();
		synchronized(parsers) {
			cache.clear();
			parsers.add(0, p);
		}
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Parser} could not be constructed.
	 */
	public ParserGroup append(Class<? extends Parser>...p) throws Exception {
		for (Class<? extends Parser> pp : reverse(p))
			append(pp);
		return this;
	}

	/**
	 * Same as {@link #append(Class[])}, except specify a single class to avoid unchecked compile warnings.
	 *
	 * @param p The parser to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Parser} could not be constructed.
	 */
	public ParserGroup append(Class<? extends Parser> p) throws Exception {
		try {
			append(p.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		}
		return this;
	}

	/**
	 * Adds the parsers in the specified group to this group.
	 *
	 * @param g The group containing the parsers to add to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroup append(ParserGroup g) {
		for (Parser p : reverse(g.parsers.toArray(new Parser[g.parsers.size()])))
			append(p);
		return this;
	}

	/**
	 * Searches the group for a parser that can handle the specified <l>Content-Type</l> header value.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header value.
	 * @return The parser and media type that matched the content type header, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(String contentTypeHeader) {
		ParserMatch pm = cache.get(contentTypeHeader);
		if (pm != null)
			return pm;

		MediaType mt = MediaType.forString(contentTypeHeader);
		return getParserMatch(mt);
	}

	/**
	 * Same as {@link #getParserMatch(String)} but matches using a {@link MediaType} instance.
	 *
	 * @param mediaType The HTTP <l>Content-Type</l> header value as a media type.
	 * @return The parser and media type that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(MediaType mediaType) {
		ParserMatch pm = cache.get(mediaType.toString());
		if (pm != null)
			return pm;

		for (Parser p : parsers) {
			for (MediaType a2 : p.getMediaTypes()) {
				if (mediaType.matches(a2)) {
					pm = new ParserMatch(a2, p);
					cache.put(mediaType.toString(), pm);
					return pm;
				}
			}
		}
		return null;
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
	 * <p>
	 * Entries are ordered in the same order as the parsers in the group.
	 *
	 * @return The list of media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		List<MediaType> l = new ArrayList<MediaType>();
		for (Parser p : parsers)
			for (MediaType mt : p.getMediaTypes())
				if (! l.contains(mt))
					l.add(mt);
		return l;
	}

	//--------------------------------------------------------------------------------
	// Convenience methods for setting properties on all parsers.
	//--------------------------------------------------------------------------------

	/**
	 * Shortcut for calling {@link Parser#setProperty(String, Object)} on all parsers in this group.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup setProperty(String property, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setProperty(property, value);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#setProperties(ObjectMap)} on all parsers in this group.
	 *
	 * @param properties The properties to set.  Ignored if <jk>null</jk>.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup setProperties(ObjectMap properties) {
		checkLock();
		for (Parser p : parsers)
			p.setProperties(properties);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addNotBeanClasses(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to specify as not-beans to the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addNotBeanClasses(Class<?>...classes) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addBeanFilters(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to add bean filters for to the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addBeanFilters(Class<?>...classes) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addBeanFilters(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addPojoSwaps(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to add POJO swaps for to the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addPojoSwaps(Class<?>...classes) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addPojoSwaps(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addToDictionary(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to add to the bean dictionary on the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addToDictionary(Class<?>...classes) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addToDictionary(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addImplClass(Class, Class)} on all parsers in this group.
	 *
	 * @param <T> The interface or abstract class type.
	 * @param interfaceClass The interface or abstract class.
	 * @param implClass The implementation class.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public <T> ParserGroup addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addImplClass(interfaceClass, implClass);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	/**
	 * Locks this group and all parsers in this group.
	 */
	@Override /* Lockable */
	public ParserGroup lock() {
		super.lock();
		for (Parser p : parsers)
			p.lock();
		return this;
	}

	/**
	 * Clones this group and all parsers in this group.
	 */
	@Override /* Lockable */
	public ParserGroup clone() throws CloneNotSupportedException {
		ParserGroup g = new ParserGroup();

		List<Parser> l = new ArrayList<Parser>(parsers.size());
		for (Parser p : parsers)
			l.add(p.clone());

		g.parsers.addAll(l);

		return g;
	}
}
