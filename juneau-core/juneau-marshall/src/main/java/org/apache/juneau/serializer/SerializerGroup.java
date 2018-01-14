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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Represents a group of {@link Serializer Serializers} that can be looked up by media type.
 * 
 * 
 * <h5 class='topic'>Description</h5>
 * 
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Finds serializers based on HTTP <code>Accept</code> header values.
 * 	<li>
 * 		Sets common properties on all serializers in a single method call.
 * 	<li>
 * 		Clones existing groups and all serializers within the group in a single method call.
 * </ul>
 * 
 * <h6 class='topic'>Match ordering</h6>
 * 
 * Serializers are matched against <code>Accept</code> strings in the order they exist in this group.
 * 
 * <p>
 * Adding new entries will cause the entries to be prepended to the group.
 * This allows for previous serializers to be overridden through subsequent calls.
 * 
 * <p>
 * For example, calling <code>g.append(S1.<jk>class</jk>,S2.<jk>class</jk>).append(S3.<jk>class</jk>,S4.<jk>class</jk>)</code>
 * will result in the order <code>S3, S4, S1, S2</code>.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct a new serializer group</jc>
 * 	SerializerGroup g = SerializerGroup.<jsm>create</jsm>();
 * 		.append(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>); <jc>// Add some serializers to it</jc>
 * 		.ws().pojoSwaps(CalendarSwap.ISO8601DT.<jk>class</jk>) <jc>// Change settings for all serializers in the group.</jc>
 * 		.build();
 * 
 * 	<jc>// Find the appropriate serializer by Accept type</jc>
 * 	WriterSerializer s = g.getWriterSerializer(<js>"text/foo, text/json;q=0.8, text/*;q:0.6, *\/*;q=0.0"</js>);
 * 
 * 	<jc>// Serialize a bean to JSON text </jc>
 * 	AddressBook addressBook = <jk>new</jk> AddressBook();  <jc>// Bean to serialize.</jc>
 * 	String json = s.serialize(addressBook);
 * </p>
 */
public final class SerializerGroup extends BeanContext {

	// Maps Accept headers to matching serializers.
	private final ConcurrentHashMap<String,SerializerMatch> cache = new ConcurrentHashMap<>();

	private final MediaType[] mediaTypes;
	private final List<MediaType> mediaTypesList;
	private final Serializer[] mediaTypeSerializers;
	private final List<Serializer> serializers;

	/**
	 * Instantiates a new clean-slate {@link SerializerGroupBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> SerializerGroupBuilder()</code>.
	 * 
	 * @return A new {@link SerializerGroupBuilder} object.
	 */
	public static SerializerGroupBuilder create() {
		return new SerializerGroupBuilder();
	}
	
	/**
	 * Returns a builder that's a copy of the settings on this serializer group.
	 * 
	 * @return A new {@link SerializerGroupBuilder} initialized to this group.
	 */
	@Override /* Context */
	public SerializerGroupBuilder builder() {
		return new SerializerGroupBuilder(this);
	}

	/**
	 * Constructor.
	 * 
	 * @param ps
	 * 	The modifiable properties that were used to initialize the serializers.
	 * 	A snapshot of these will be made so that we can clone and modify this group.
	 * @param serializers
	 * 	The serializers defined in this group.
	 * 	The order is important because they will be tried in reverse order (e.g.newer first) in which they will be tried
	 * 	to match against media types.
	 */
	public SerializerGroup(PropertyStore ps, Serializer[] serializers) {
		super(ps);
		this.serializers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(serializers)));

		List<MediaType> lmt = new ArrayList<>();
		List<Serializer> l = new ArrayList<>();
		for (Serializer s : serializers) {
			for (MediaType m: s.getMediaTypes()) {
				lmt.add(m);
				l.add(s);
			}
		}

		this.mediaTypes = lmt.toArray(new MediaType[lmt.size()]);
		this.mediaTypesList = Collections.unmodifiableList(lmt);
		this.mediaTypeSerializers = l.toArray(new Serializer[l.size()]);
	}

	/**
	 * Searches the group for a serializer that can handle the specified <code>Accept</code> value.
	 * 
	 * <p>
	 * The <code>accept</code> value complies with the syntax described in RFC2616, Section 14.1, as described below:
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
	 * 
	 * <p>
	 * The returned object includes both the serializer and media type that matched.
	 * 
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer and media type that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public SerializerMatch getSerializerMatch(String acceptHeader) {
		SerializerMatch sm = cache.get(acceptHeader);
		if (sm != null)
			return sm;

		Accept a = Accept.forString(acceptHeader);
		int match = a.findMatch(mediaTypes);
		if (match >= 0) {
			sm = new SerializerMatch(mediaTypes[match], mediaTypeSerializers[match]);
			cache.putIfAbsent(acceptHeader, sm);
		}

		return cache.get(acceptHeader);
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
		if (mediaType == null)
			return null;
		return getSerializer(mediaType.toString());
	}

	/**
	 * Same as {@link #getSerializer(String)}, but casts it to a {@link WriterSerializer}.
	 * 
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public WriterSerializer getWriterSerializer(String acceptHeader) {
		return (WriterSerializer)getSerializer(acceptHeader);
	}

	/**
	 * Same as {@link #getSerializer(MediaType)}, but casts it to a {@link WriterSerializer}.
	 * 
	 * @param mediaType The HTTP media type.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public WriterSerializer getWriterSerializer(MediaType mediaType) {
		return (WriterSerializer)getSerializer(mediaType);
	}

	/**
	 * Same as {@link #getSerializer(String)}, but casts it to an {@link OutputStreamSerializer}.
	 * 
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public OutputStreamSerializer getStreamSerializer(String acceptHeader) {
		return (OutputStreamSerializer)getSerializer(acceptHeader);
	}

	/**
	 * Same as {@link #getSerializer(MediaType)}, but casts it to a {@link OutputStreamSerializer}.
	 * 
	 * @param mediaType The HTTP media type.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public OutputStreamSerializer getStreamSerializer(MediaType mediaType) {
		return (OutputStreamSerializer)getSerializer(mediaType);
	}

	/**
	 * Returns the media types that all serializers in this group can handle.
	 * 
	 * <p>
	 * Entries are ordered in the same order as the serializers in the group.
	 * 
	 * @return An unmodifiable list of media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return mediaTypesList;
	}

	/**
	 * Returns a copy of the serializers in this group.
	 * 
	 * @return An unmodifiable list of serializers in this group.
	 */
	public List<Serializer> getSerializers() {
		return serializers;
	}
}
