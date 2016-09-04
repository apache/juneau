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
import java.util.concurrent.locks.*;

import org.apache.juneau.*;

/**
 * Represents a group of {@link Serializer Serializers} that can be looked up by media type.
 *
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
 *
 * <h6 class='topic'>Examples</h6>
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
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class SerializerGroup extends Lockable {

	// Maps media-types to serializers.
	private final Map<String,Serializer> serializerMap = new ConcurrentHashMap<String,Serializer>();

	// Maps Accept headers to matching media types.
	private final Map<String,String> mediaTypeMappings = new ConcurrentHashMap<String,String>();

	private final CopyOnWriteArrayList<Serializer> serializers = new CopyOnWriteArrayList<Serializer>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock rl = lock.readLock(), wl = lock.writeLock();


	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public SerializerGroup append(Class<? extends Serializer>...s) throws Exception {
		checkLock();
		wl.lock();
		try {
			serializerMap.clear();
			mediaTypeMappings.clear();
			for (Class<? extends Serializer> ss : reverse(s)) {
				try {
					append(ss);
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
	 * @param c The serializer to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public SerializerGroup append(Class<? extends Serializer> c) throws Exception {
		checkLock();
		wl.lock();
		try {
			serializerMap.clear();
			mediaTypeMappings.clear();
			serializers.add(0, c.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		} finally {
			wl.unlock();
		}
		return this;
	}

	/**
	 * Returns the serializer registered to handle the specified media type.
	 * <p>
	 * The media-type string must not contain any parameters or q-values.
	 *
	 * @param mediaType The media-type string (e.g. <js>"text/json"</js>
	 * @return The serializer that handles the specified accept content type, or <jk>null</jk> if
	 * 		no serializer is registered to handle it.
	 */
	public Serializer getSerializer(String mediaType) {
		Serializer s = serializerMap.get(mediaType);
		if (s == null)
			s = serializerMap.get(findMatch(mediaType));
		return s;
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
	 * @return The media type registered by one of the parsers that matches the <code>accept</code> string,
	 * 	or <jk>null</jk> if no media types matched.
	 */
	public String findMatch(String acceptHeader) {
		rl.lock();
		try {
			String mt = mediaTypeMappings.get(acceptHeader);
			if (mt != null)
				return mt;

			MediaRange[] mr = MediaRange.parse(acceptHeader);
			if (mr.length == 0)
				mr = MediaRange.parse("*/*");

			for (MediaRange a : mr) {
				for (Serializer s : serializers) {
					for (MediaRange a2 : s.getMediaRanges()) {
						if (a.matches(a2)) {
							mt = a2.getMediaType();
							mediaTypeMappings.put(acceptHeader, mt);
							serializerMap.put(mt, s);
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
		for (Serializer s : serializers)
			for (String mt : s.getMediaTypes())
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
