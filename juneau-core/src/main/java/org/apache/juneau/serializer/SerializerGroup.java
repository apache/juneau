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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;

/**
 * Represents a group of {@link Serializer Serializers} that can be looked up by media type.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>Finds serializers based on HTTP <code>Accept</code> header values.
 * 	<li>Sets common properties on all serializers in a single method call.
 * 	<li>Locks all serializers in a single method call.
 * 	<li>Clones existing groups and all serializers within the group in a single method call.
 * </ul>
 *
 * <h6 class='topic'>Match ordering</h6>
 * <p>
 * 	Serializers are matched against <code>Accept</code> strings in the order they exist in this group.
 * <p>
 * 	Adding new entries will cause the entries to be prepended to the group.
 *  	This allows for previous serializers to be overridden through subsequent calls.
 * <p>
 * 	For example, calling <code>g.append(S1.<jk>class</jk>,S2.<jk>class</jk>).append(S3.<jk>class</jk>,S4.<jk>class</jk>)</code>
 * 	will result in the order <code>S3, S4, S1, S2</code>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct a new serializer group</jc>
 * 	SerializerGroup g = <jk>new</jk> SerializerGroup();
 *
 * 	<jc>// Add some serializers to it</jc>
 * 	g.append(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
 *
 * 	<jc>// Change settings for all serializers in the group and lock it.</jc>
 * 	g.setProperty(SerializerContext.<jsf>SERIALIZER_useIndentation</jsf>, <jk>true</jk>)
 * 		.addPojoSwaps(CalendarSwap.ISO8601DT.<jk>class</jk>)
 * 		.lock();
 *
 * 	<jc>// Find the appropriate serializer by Accept type</jc>
 * 	String mediaTypeMatch = g.findMatch(<js>"text/foo, text/json;q=0.8, text/*;q:0.6, *\/*;q=0.0"</js>);
 * 	WriterSerializer s = (WriterSerializer)g.getSerializer(mediaTypeMatch);
 *
 * 	<jc>// Serialize a bean to JSON text </jc>
 * 	AddressBook addressBook = <jk>new</jk> AddressBook();  <jc>// Bean to serialize.</jc>
 * 	String json = s.serialize(addressBook);
 * </p>
 */
public final class SerializerGroup extends Lockable {

	// Maps Accept headers to matching serializers.
	private final Map<String,SerializerMatch> cache = new ConcurrentHashMap<String,SerializerMatch>();

	private final CopyOnWriteArrayList<Serializer> serializers = new CopyOnWriteArrayList<Serializer>();

	/**
	 * Adds the specified serializer to the beginning of this group.
	 *
	 * @param s The serializer to add to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup append(Serializer s) {
		checkLock();
		synchronized(serializers) {
			cache.clear();
			serializers.add(0, s);
		}
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public SerializerGroup append(Class<? extends Serializer>...s) throws Exception {
		for (Class<? extends Serializer> ss : reverse(s))
			append(ss);
		return this;
	}

	/**
	 * Same as {@link #append(Class[])}, except specify a single class to avoid unchecked compile warnings.
	 *
	 * @param s The serializer to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public SerializerGroup append(Class<? extends Serializer> s) throws Exception {
		try {
			append(s.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		}
		return this;
	}

	/**
	 * Adds the serializers in the specified group to this group.
	 *
	 * @param g The group containing the serializers to add to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup append(SerializerGroup g) {
		for (Serializer s : reverse(g.serializers.toArray(new Serializer[g.serializers.size()])))
			append(s);
		return this;
	}

	/**
	 * Searches the group for a serializer that can handle the specified <code>Accept</code> value.
	 * <p>
	 * 	The <code>accept</code> value complies with the syntax described in RFC2616, Section 14.1, as described below:
	 * <p class='bcode'>
	 * 	Accept         = "Accept" ":"
	 * 	                  #( media-range [ accept-params ] )
	 *
	 * 	media-range    = ( "*\/*"
	 * 	                  | ( type "/" "*" )
	 * 	                  | ( type "/" subtype )
	 * 	                  ) *( ";" parameter )
	 * 	accept-params  = ";" "q" "=" qvalue *( accept-extension )
	 * 	accept-extension = ";" token [ "=" ( token | quoted-string ) ]
	 * </p>
	 * <p>
	 * 	The general idea behind having the serializer resolution be a two-step process is so that
	 * 	the matched media type can be passed in to the {@link WriterSerializer#doSerialize(SerializerSession, Object)} method.
	 * 	For example...
	 * <p class='bcode'>
	 * 	String acceptHeaderValue = request.getHeader(<js>"Accept"</js>);
	 * 	String matchingMediaType = group.findMatch(acceptHeaderValue);
	 * 	if (matchingMediaType == <jk>null</jk>)
	 * 		<jk>throw new</jk> RestException(<jsf>SC_NOT_ACCEPTABLE</jsf>);
	 * 	WriterSerializer s = (WriterSerializer)group.getSerializer(matchingMediaType);
	 *  s.serialize(getPojo(), response.getWriter(), response.getProperties(), matchingMediaType);
	 * </p>
	 *
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer and media type that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public SerializerMatch getSerializerMatch(String acceptHeader) {
		SerializerMatch sm = cache.get(acceptHeader);
		if (sm != null)
			return sm;

		MediaRange[] mr = MediaRange.parse(acceptHeader);
		if (mr.length == 0)
			mr = MediaRange.parse("*/*");

		Map<Float,SerializerMatch> m = null;

		for (MediaRange a : mr) {
			for (Serializer s : serializers) {
				for (MediaType a2 : s.getMediaTypes()) {
					float q = a.matches(a2);
					if (q == 1) {
						sm = new SerializerMatch(a2, s);
						cache.put(acceptHeader, sm);
						return sm;
					} else if (q > 0) {
						if (m == null)
							m = new TreeMap<Float,SerializerMatch>(Collections.reverseOrder());
						m.put(q, new SerializerMatch(a2, s));
					}
				}
			}
		}

		return (m == null ? null : m.values().iterator().next());
	}

	/**
	 * Same as {@link #getSerializerMatch(String)} but matches using a {@link MediaType} instance.
	 *
	 * @param mediaType The HTTP media type.
	 * @return The serializer and media type that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public SerializerMatch getSerializerMatch(MediaType mediaType) {
		return getSerializerMatch(mediaType.toString());
	}

	/**
	 * Same as {@link #getSerializerMatch(String)} but returns just the matched serializer.
	 *
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public Serializer getSerializer(String acceptHeader) {
		SerializerMatch sm = getSerializerMatch(acceptHeader);
		return sm == null ? null : sm.getSerializer();
	}

	/**
	 * Same as {@link #getSerializerMatch(MediaType)} but returns just the matched serializer.
	 *
	 * @param mediaType The HTTP media type.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public Serializer getSerializer(MediaType mediaType) {
		return getSerializer(mediaType == null ? null : mediaType.toString());
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
		for (Serializer s : serializers)
			for (MediaType mt : s.getMediaTypes())
				if (! l.contains(mt))
					l.add(mt);
		return l;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Calls {@link Serializer#setMaxDepth(int)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_maxDepth
	 */
	public SerializerGroup setMaxDepth(int value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setMaxDepth(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setInitialDepth(int)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_initialDepth
	 */
	public SerializerGroup setInitialDepth(int value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setInitialDepth(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setDetectRecursions(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_detectRecursions
	 */
	public SerializerGroup setDetectRecursions(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setDetectRecursions(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnoreRecursions(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_ignoreRecursions
	 */
	public SerializerGroup setIgnoreRecursions(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnoreRecursions(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setUseIndentation(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_useIndentation
	 */
	public SerializerGroup setUseIndentation(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setUseIndentation(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setAddBeanTypeProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_addBeanTypeProperties
	 */
	public SerializerGroup setAddBeanTypeProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setAddBeanTypeProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setQuoteChar(char)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_quoteChar
	 */
	public SerializerGroup setQuoteChar(char value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setQuoteChar(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setTrimNullProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimNullProperties
	 */
	public SerializerGroup setTrimNullProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setTrimNullProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setTrimEmptyCollections(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimEmptyCollections
	 */
	public SerializerGroup setTrimEmptyCollections(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setTrimEmptyCollections(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setTrimEmptyMaps(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimEmptyMaps
	 */
	public SerializerGroup setTrimEmptyMaps(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setTrimEmptyMaps(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setTrimStrings(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimStrings
	 */
	public SerializerGroup setTrimStrings(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setTrimStrings(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setRelativeUriBase(String)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_relativeUriBase
	 */
	public SerializerGroup setRelativeUriBase(String value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setRelativeUriBase(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setAbsolutePathUriBase(String)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_absolutePathUriBase
	 */
	public SerializerGroup setAbsolutePathUriBase(String value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setAbsolutePathUriBase(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setSortCollections(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_sortCollections
	 */
	public SerializerGroup setSortCollections(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setSortCollections(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setSortMaps(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public SerializerGroup setSortMaps(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setSortMaps(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeansRequireDefaultConstructor(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public SerializerGroup setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeansRequireDefaultConstructor(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeansRequireSerializable(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public SerializerGroup setBeansRequireSerializable(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeansRequireSerializable(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeansRequireSettersForGetters(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public SerializerGroup setBeansRequireSettersForGetters(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeansRequireSettersForGetters(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeansRequireSomeProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public SerializerGroup setBeansRequireSomeProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeansRequireSomeProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanMapPutReturnsOldValue(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public SerializerGroup setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanConstructorVisibility(Visibility)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public SerializerGroup setBeanConstructorVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanConstructorVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanClassVisibility(Visibility)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public SerializerGroup setBeanClassVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanClassVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanFieldVisibility(Visibility)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public SerializerGroup setBeanFieldVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanFieldVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setMethodVisibility(Visibility)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public SerializerGroup setMethodVisibility(Visibility value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setMethodVisibility(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setUseJavaBeanIntrospector(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public SerializerGroup setUseJavaBeanIntrospector(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setUseJavaBeanIntrospector(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setUseInterfaceProxies(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public SerializerGroup setUseInterfaceProxies(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setUseInterfaceProxies(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnoreUnknownBeanProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public SerializerGroup setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnoreUnknownNullBeanProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public SerializerGroup setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnorePropertiesWithoutSetters(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public SerializerGroup setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnoreInvocationExceptionsOnGetters(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public SerializerGroup setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setIgnoreInvocationExceptionsOnSetters(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public SerializerGroup setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setSortProperties(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_sortProperties
	 */
	public SerializerGroup setSortProperties(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setSortProperties(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setNotBeanPackages(String...)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroup setNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setNotBeanPackages(Collection)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroup setNotBeanPackages(Collection<String> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addNotBeanPackages(String...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroup addNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addNotBeanPackages(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroup addNotBeanPackages(Collection<String> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeNotBeanPackages(String...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroup removeNotBeanPackages(String...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeNotBeanPackages(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public SerializerGroup removeNotBeanPackages(Collection<String> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeNotBeanPackages(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setNotBeanClasses(Class...)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public SerializerGroup setNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setNotBeanClasses(Collection)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public SerializerGroup setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addNotBeanClasses(Class...)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public SerializerGroup addNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addNotBeanClasses(Collection)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public SerializerGroup addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeNotBeanClasses(Class...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroup removeNotBeanClasses(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeNotBeanClasses(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public SerializerGroup removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeNotBeanClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanFilters(Class...)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroup setBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanFilters(Collection)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public SerializerGroup setBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addBeanFilters(Class...)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroup addBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addBeanFilters(Collection)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public SerializerGroup addBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeBeanFilters(Class...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroup removeBeanFilters(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeBeanFilters(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public SerializerGroup removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeBeanFilters(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setPojoSwaps(Class...)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroup setPojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setPojoSwaps(Collection)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public SerializerGroup setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addPojoSwaps(Class...)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroup addPojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addPojoSwaps(Collection)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public SerializerGroup addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addPojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removePojoSwaps(Class...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroup removePojoSwaps(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removePojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removePojoSwaps(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public SerializerGroup removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removePojoSwaps(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setImplClasses(Map)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 */
	public SerializerGroup setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setImplClasses(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addImplClass(Class,Class)} on all serializers in this group.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 * @see BeanContext#BEAN_implClasses_put
	 */
	public <T> SerializerGroup addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addImplClass(interfaceClass, implClass);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanDictionary(Class...)} on all serializers in this group.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public SerializerGroup setBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanDictionary(Collection)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroup setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addToBeanDictionary(Class...)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroup addToBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addToBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#addToBeanDictionary(Collection)} on all serializers in this group.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public SerializerGroup addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addToBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeFromBeanDictionary(Class...)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroup removeFromBeanDictionary(Class<?>...values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeFromBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeFromBeanDictionary(Collection)} on all serializers in this group.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public SerializerGroup removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeFromBeanDictionary(values);
		return this;
	}

	/**
	 * Calls {@link Serializer#setBeanTypePropertyName(String)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public SerializerGroup setBeanTypePropertyName(String value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setBeanTypePropertyName(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setDefaultParser(Class)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_defaultParser
	 */
	public SerializerGroup setDefaultParser(Class<?> value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setDefaultParser(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setLocale(Locale)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_locale
	 */
	public SerializerGroup setLocale(Locale value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setLocale(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setTimeZone(TimeZone)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_timeZone
	 */
	public SerializerGroup setTimeZone(TimeZone value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setTimeZone(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setMediaType(MediaType)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_mediaType
	 */
	public SerializerGroup setMediaType(MediaType value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setMediaType(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setDebug(boolean)} on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_debug
	 */
	public SerializerGroup setDebug(boolean value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setDebug(value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setProperty(String,Object)} on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object or {@link ContextFactory} object.
	 * @see ContextFactory#setProperty(String, Object)
	 */
	public SerializerGroup setProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Serializer#setProperties(ObjectMap)} on all serializers in this group.
	 *
	 * @param properties The properties to set on this class.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 * @see ContextFactory#setProperties(java.util.Map)
	 */
	public SerializerGroup setProperties(ObjectMap properties) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setProperties(properties);
		return this;
	}

	/**
	 * Calls {@link Serializer#addToProperty(String,Object)} on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public SerializerGroup addToProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addToProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Serializer#putToProperty(String,Object,Object)} on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public SerializerGroup putToProperty(String name, Object key, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.putToProperty(name, key, value);
		return this;
	}

	/**
	 * Calls {@link Serializer#putToProperty(String,Object)} on all serializers in this group.
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public SerializerGroup putToProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.putToProperty(name, value);
		return this;
	}

	/**
	 * Calls {@link Serializer#removeFromProperty(String,Object)} on all serializers in this group.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public SerializerGroup removeFromProperty(String name, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	/**
	 * Calls {@link Serializer#setClassLoader(ClassLoader)} on all serializers in this group.
	 *
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#setClassLoader(ClassLoader)
	 */
	public SerializerGroup setClassLoader(ClassLoader classLoader) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setClassLoader(classLoader);
		return this;
	}

	/**
	 * Locks this group and all serializers in this group.
	 */
	@Override /* Lockable */
	public SerializerGroup lock() {
		super.lock();
		for (Serializer s : serializers)
			s.lock();
		return this;
	}

	/**
	 * Clones this group and all serializers in this group.
	 */
	@Override /* Lockable */
	public SerializerGroup clone() throws CloneNotSupportedException {
		SerializerGroup g = new SerializerGroup();

		List<Serializer> l = new ArrayList<Serializer>(serializers.size());
		for (Serializer s : serializers)
			l.add(s.clone());

		g.serializers.addAll(l);

		return g;
	}
}
