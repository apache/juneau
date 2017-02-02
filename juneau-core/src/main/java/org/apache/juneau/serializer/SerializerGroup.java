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
 * <h6 class='topic'>Description</h6>
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
 * <h6 class='topic'>Example:</h6>
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
	 * @param s - The serializer to add to this group.
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
	// Convenience methods for setting properties on all serializers.
	//--------------------------------------------------------------------------------

	/**
	 * Shortcut for calling {@link Serializer#setProperty(String, Object)} on all serializers in this group.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup setProperty(String property, Object value) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.setProperty(property, value);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#setProperties(ObjectMap)} on all serializers in this group.
	 *
	 * @param properties The properties to set.  Ignored if <jk>null</jk>.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup setProperties(ObjectMap properties) {
		checkLock();
		for (Serializer s : serializers)
			s.setProperties(properties);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addNotBeanClasses(Class[])} on all serializers in this group.
	 *
	 * @param classes The classes to specify as not-beans to the underlying bean context of all serializers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup addNotBeanClasses(Class<?>...classes) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addBeanFilters(Class[])} on all serializers in this group.
	 *
	 * @param classes The classes to add bean filters for to the underlying bean context of all serializers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup addBeanFilters(Class<?>...classes) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addBeanFilters(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addPojoSwaps(Class[])} on all serializers in this group.
	 *
	 * @param classes The classes to add POJO swaps for to the underlying bean context of all serializers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup addPojoSwaps(Class<?>...classes) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addPojoSwaps(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addToDictionary(Class[])} on all serializers in this group.
	 *
	 * @param classes The classes to add to the bean dictionary on the underlying bean context of all serializers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup addToDictionary(Class<?>...classes) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addToDictionary(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addImplClass(Class, Class)} on all serializers in this group.
	 *
	 * @param <T> The interface or abstract class type.
	 * @param interfaceClass The interface or abstract class.
	 * @param implClass The implementation class.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public <T> SerializerGroup addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		for (Serializer s : serializers)
			s.addImplClass(interfaceClass, implClass);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
