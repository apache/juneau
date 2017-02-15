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
	 * @param p The parser to add to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroup append(Parser p) {
		checkLock();
		synchronized(this) {
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
			System.err.println(e); // NOT DEBUG
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
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Calls {@link Parser#setTrimStrings(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see ParserContext#PARSER_trimStrings
	 */
	public ParserGroup setTrimStrings(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setTrimStrings(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setStrict(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see ParserContext#PARSER_strict
	 */
	public ParserGroup setStrict(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setStrict(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setInputStreamCharset(String)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see ParserContext#PARSER_inputStreamCharset
	 */
	public ParserGroup setInputStreamCharset(String value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setInputStreamCharset(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setFileCharset(String)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see ParserContext#PARSER_fileCharset
	 */
	public ParserGroup setFileCharset(String value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setFileCharset(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeansRequireDefaultConstructor(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public ParserGroup setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeansRequireDefaultConstructor(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeansRequireSerializable(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public ParserGroup setBeansRequireSerializable(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeansRequireSerializable(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeansRequireSettersForGetters(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public ParserGroup setBeansRequireSettersForGetters(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeansRequireSettersForGetters(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeansRequireSomeProperties(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public ParserGroup setBeansRequireSomeProperties(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeansRequireSomeProperties(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanMapPutReturnsOldValue(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public ParserGroup setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanConstructorVisibility(Visibility)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public ParserGroup setBeanConstructorVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanConstructorVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanClassVisibility(Visibility)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public ParserGroup setBeanClassVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanClassVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanFieldVisibility(Visibility)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public ParserGroup setBeanFieldVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanFieldVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setMethodVisibility(Visibility)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public ParserGroup setMethodVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setMethodVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setUseJavaBeanIntrospector(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public ParserGroup setUseJavaBeanIntrospector(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setUseJavaBeanIntrospector(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setUseInterfaceProxies(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public ParserGroup setUseInterfaceProxies(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setUseInterfaceProxies(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setIgnoreUnknownBeanProperties(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public ParserGroup setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setIgnoreUnknownNullBeanProperties(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public ParserGroup setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setIgnorePropertiesWithoutSetters(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public ParserGroup setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setIgnoreInvocationExceptionsOnGetters(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public ParserGroup setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setIgnoreInvocationExceptionsOnSetters(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public ParserGroup setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setSortProperties(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_sortProperties
	 */
	public ParserGroup setSortProperties(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setSortProperties(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setNotBeanPackages(String...)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public ParserGroup setNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setNotBeanPackages(Collection)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public ParserGroup setNotBeanPackages(Collection<String> value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setNotBeanPackages(value);
		return this;
	}

	/**
	 * Calls {@link Parser#addNotBeanPackages(String...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroup addNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addNotBeanPackages(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroup addNotBeanPackages(Collection<String> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeNotBeanPackages(String...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroup removeNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeNotBeanPackages(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public ParserGroup removeNotBeanPackages(Collection<String> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setNotBeanClasses(Class...)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public ParserGroup setNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setNotBeanClasses(Collection)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public ParserGroup setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addNotBeanClasses(Class...)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public ParserGroup addNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addNotBeanClasses(Collection)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public ParserGroup addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeNotBeanClasses(Class...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public ParserGroup removeNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeNotBeanClasses(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public ParserGroup removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanFilters(Class...)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public ParserGroup setBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanFilters(Collection)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public ParserGroup setBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addBeanFilters(Class...)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public ParserGroup addBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addBeanFilters(Collection)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public ParserGroup addBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeBeanFilters(Class...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public ParserGroup removeBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeBeanFilters(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public ParserGroup removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setPojoSwaps(Class...)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public ParserGroup setPojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setPojoSwaps(Collection)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public ParserGroup setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addPojoSwaps(Class...)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public ParserGroup addPojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addPojoSwaps(Collection)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public ParserGroup addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removePojoSwaps(Class...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public ParserGroup removePojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removePojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removePojoSwaps(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public ParserGroup removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removePojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setImplClasses(Map)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 */
	public ParserGroup setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setImplClasses(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addImplClass(Class,Class)} on all parsers in this group.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 * @see BeanContext#BEAN_implClasses_put
	 */
	public <T> ParserGroup addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addImplClass(interfaceClass, implClass);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanDictionary(Class...)} on all parsers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public ParserGroup setBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanDictionary(Collection)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public ParserGroup setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addToBeanDictionary(Class...)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public ParserGroup addToBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addToBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#addToBeanDictionary(Collection)} on all parsers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public ParserGroup addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addToBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeFromBeanDictionary(Class...)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public ParserGroup removeFromBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeFromBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#removeFromBeanDictionary(Collection)} on all parsers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public ParserGroup removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeFromBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Parser#setBeanTypePropertyName(String)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public ParserGroup setBeanTypePropertyName(String value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setBeanTypePropertyName(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setDefaultParser(Class)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_defaultParser
	 */
	public ParserGroup setDefaultParser(Class<?> value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setDefaultParser(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setLocale(Locale)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_locale
	 */
	public ParserGroup setLocale(Locale value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setLocale(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setTimeZone(TimeZone)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_timeZone
	 */
	public ParserGroup setTimeZone(TimeZone value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setTimeZone(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setMediaType(MediaType)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_mediaType
	 */
	public ParserGroup setMediaType(MediaType value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setMediaType(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setDebug(boolean)} on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_debug
	 */
	public ParserGroup setDebug(boolean value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setDebug(value);
		return this;
	}

	/**
	 * Calls {@link Parser#setProperty(String,Object)} on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object or {@link ContextFactory} object.
	 * @see ContextFactory#setProperty(String, Object)
	 */
	public ParserGroup setProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Parser#setProperties(ObjectMap)} on all parsers in this group.
	 *
	 * @param properties The properties to set on this class.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 * @see ContextFactory#setProperties(java.util.Map)
	 */
	public ParserGroup setProperties(ObjectMap properties) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setProperties(properties);
		return this;
	}

	/**
	 * Calls {@link Parser#addToProperty(String,Object)} on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public ParserGroup addToProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.addToProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Parser#putToProperty(String,Object,Object)} on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public ParserGroup putToProperty(String name, Object key, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.putToProperty(name, key, value);
		return this;
	}

	/**
	 * Calls {@link Parser#putToProperty(String,Object)} on all parsers in this group.
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public ParserGroup putToProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.putToProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Parser#removeFromProperty(String,Object)} on all parsers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public ParserGroup removeFromProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	/**
	 * Calls {@link Parser#setClassLoader(ClassLoader)} on all parsers in this group.
	 *
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#setClassLoader(ClassLoader)
	 */
	public ParserGroup setClassLoader(ClassLoader classLoader) throws LockedException {
		checkLock();
		for (Parser p : parsers)
			p.setClassLoader(classLoader);
		return this;
	}

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
