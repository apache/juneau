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
import java.util.concurrent.locks.*;

import org.apache.juneau.*;

/**
 * Represents a group of {@link Parser Parsers} that can be looked up by media type.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>Finds parsers based on HTTP <code>Content-Type</code> header values.
 * 	<li>Sets common properties on all parsers in a single method call.
 * 	<li>Locks all parsers in a single method call.
 * 	<li>Clones existing groups and all parsers within the group in a single method call.
 * </ul>
 *
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
 *
 * <h6 class='topic'>Examples</h6>
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
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class ParserGroup extends Lockable {

	// Maps media-types to parsers.
	private final Map<String,Parser> parserMap = new ConcurrentHashMap<String,Parser>();

	// Maps Content-Type headers to matching media types.
	private final Map<String,String> mediaTypeMappings = new ConcurrentHashMap<String,String>();

	private final CopyOnWriteArrayList<Parser> parsers = new CopyOnWriteArrayList<Parser>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock rl = lock.readLock(), wl = lock.writeLock();


	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Parser} could not be constructed.
	 */
	public ParserGroup append(Class<? extends Parser>...p) throws Exception {
		checkLock();
		wl.lock();
		try {
			for (Class<? extends Parser> c : reverse(p)) {
				parserMap.clear();
				mediaTypeMappings.clear();
				try {
					append(c);
				} catch (NoClassDefFoundError e) {
					// Ignore if dependent library not found (e.g. Jena).
					System.err.println(e);
				}
			}
		} finally {
			wl.unlock();
		}
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
		checkLock();
		wl.lock();
		try {
			parserMap.clear();
			mediaTypeMappings.clear();
			parsers.add(0, p.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Returns the parser registered to handle the specified media type.
	 * <p>
	 * The media-type string must not contain any parameters such as <js>";charset=X"</js>.
	 *
	 * @param mediaType The media-type string (e.g. <js>"text/json"</js>).
	 * @return The REST parser that handles the specified request content type, or <jk>null</jk> if
	 * 		no parser is registered to handle it.
	 */
	public Parser getParser(String mediaType) {
		Parser p = parserMap.get(mediaType);
		if (p == null) {
			String mt = findMatch(mediaType);
			if (mt != null)
				p = parserMap.get(mt);
		}
		return p;
	}

	/**
	 * Searches the group for a parser that can handle the specified <l>Content-Type</l> header value.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header value.
	 * @return The media type registered by one of the parsers that matches the <code>mediaType</code> string,
	 * 	or <jk>null</jk> if no media types matched.
	 */
	public String findMatch(String contentTypeHeader) {
		rl.lock();
		try {
			String mt = mediaTypeMappings.get(contentTypeHeader);
			if (mt != null)
				return mt;

			MediaRange[] mr = MediaRange.parse(contentTypeHeader);
			if (mr.length == 0)
				mr = MediaRange.parse("*/*");

			for (MediaRange a : mr) {
				for (Parser p : parsers) {
					for (MediaRange a2 : p.getMediaRanges()) {
						if (a.matches(a2)) {
							mt = a2.getMediaType();
							mediaTypeMappings.put(contentTypeHeader, mt);
							parserMap.put(mt, p);
							return mt;
						}
					}
				}
			}
			return null;
		} finally {
			rl.unlock();
		}
	}

	/**
	 * Returns the media types that all parsers in this group can handle
	 * <p>
	 * Entries are ordered in the same order as the parsers in the group.
	 *
	 * @return The list of media types.
	 */
	public List<String> getSupportedMediaTypes() {
		List<String> l = new ArrayList<String>();
		for (Parser p : parsers)
			for (String mt : p.getMediaTypes())
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
