/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import static com.ibm.juno.core.utils.ArrayUtils.*;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;

/**
 * Represents a group of {@link Serializer Serializers} that can be looked up by media type.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Provides the following features:
 * <ul>
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
 * 	g.setProperty(SerializerProperties.<jsf>SERIALIZER_useIndentation</jsf>, <jk>true</jk>)
 * 		.addFilters(CalendarFilter.ISO8601DT.<jk>class</jk>)
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
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SerializerGroup extends Lockable {

	// Maps media-types to serializers.
	private transient Map<String,SerializerEntry> entryMap = new HashMap<String,SerializerEntry>();
	private transient LinkedList<SerializerEntry> tempEntries = new LinkedList<SerializerEntry>();
	private transient SerializerEntry[] entries;


	/**
	 * Registers the specified REST serializers with this serializer group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup append(Serializer<?>...s) {
		checkLock();
		entries = null;
		for (Serializer<?> ss : reverse(s))  {
			SerializerEntry e = new SerializerEntry(ss);
			tempEntries.addFirst(e);
			for (String mediaType : e.mediaTypes)
				entryMap.put(mediaType, e);
		}
		return this;
	}

	/**
	 * Same as {@link #append(Serializer[])}, except specify classes instead of class instances
	 * 	 of {@link Serializer}.
	 * <p>
	 * Note that this can only be used on {@link Serializer Serializers} with public no-arg constructors.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public SerializerGroup append(Class<? extends Serializer<?>>...s) throws Exception {
		checkLock();
		for (Class<? extends Serializer<?>> ss : reverse(s))
			try {
			append(ss.newInstance());
			} catch (NoClassDefFoundError e) {
				// Ignore if dependent library not found (e.g. Jena).
				System.err.println(e);
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
	public SerializerGroup append(Class<? extends Serializer<?>> c) throws Exception {
		checkLock();
		try {
		append(c.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
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
	public Serializer<?> getSerializer(String mediaType) {
		SerializerEntry e = entryMap.get(mediaType);
		return (e == null ? null : e.serializer);
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
	 * 	the matched media type can be passed in to the {@link WriterSerializer#doSerialize(Object, Writer, SerializerContext)} method.
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
	 * @param accept The accept string.
	 * @return The media type registered by one of the parsers that matches the <code>accept</code> string,
	 * 	or <jk>null</jk> if no media types matched.
	 */
	public String findMatch(String accept) {
		MediaRange[] mr = MediaRange.parse(accept);
		if (mr.length == 0)
			mr = MediaRange.parse("*/*");

		for (MediaRange a : mr)
			for (SerializerEntry e : getEntries())
				for (MediaRange a2 : e.mediaRanges)
					if (a.matches(a2))
						return a2.getMediaType();

		return null;
	}

	/**
	 * Returns the media types that all serializers in this group can handle
	 * <p>
	 * Entries are ordered in the same order as the serializers in the group.
	 *
	 * @return The list of media types.
	 */
	public List<String> getSupportedMediaTypes() {
		List<String> l = new ArrayList<String>();
		for (SerializerEntry e : getEntries())
			for (String mt : e.mediaTypes)
				if (! l.contains(mt))
					l.add(mt);
		return l;
	}

	private SerializerEntry[] getEntries() {
		if (entries == null)
			entries = tempEntries.toArray(new SerializerEntry[tempEntries.size()]);
		return entries;
	}

	static class SerializerEntry {
		Serializer<?> serializer;
		MediaRange[] mediaRanges;
		String[] mediaTypes;

		SerializerEntry(Serializer<?> s) {
			serializer = s;

			mediaTypes = new String[s.getMediaTypes().length];
			int i = 0;
			for (String mt : s.getMediaTypes())
				mediaTypes[i++] = mt.toLowerCase(Locale.ENGLISH);

			List<MediaRange> l = new LinkedList<MediaRange>();
			for (i = 0; i < mediaTypes.length; i++)
				l.addAll(Arrays.asList(MediaRange.parse(mediaTypes[i])));
			mediaRanges = l.toArray(new MediaRange[l.size()]);
		}
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
		for (SerializerEntry e : getEntries())
			e.serializer.setProperty(property, value);
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
		if (properties != null)
			for (Map.Entry<String,Object> e : properties.entrySet())
				setProperty(e.getKey(), e.getValue());
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
		for (SerializerEntry e : getEntries())
			e.serializer.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Serializer#addFilters(Class[])} on all serializers in this group.
	 *
	 * @param classes The classes to add bean filters for to the underlying bean context of all serializers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public SerializerGroup addFilters(Class<?>...classes) throws LockedException {
		checkLock();
		for (SerializerEntry e : getEntries())
			e.serializer.addFilters(classes);
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
		for (SerializerEntry e : getEntries())
			e.serializer.addImplClass(interfaceClass, implClass);
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
		for (SerializerEntry e : getEntries())
			e.serializer.lock();
		return this;
	}

	/**
	 * Clones this group and all serializers in this group.
	 */
	@Override /* Lockable */
	public SerializerGroup clone() throws CloneNotSupportedException {
		SerializerGroup c = (SerializerGroup)super.clone();
		c.entryMap = new HashMap<String,SerializerEntry>();
		c.tempEntries = new LinkedList<SerializerEntry>();
		c.entries = null;
		SerializerEntry[] e = getEntries();
		for (int i = e.length-1; i >= 0; i--)
			c.append(e[i].serializer.clone());
		return c;
	}
}
