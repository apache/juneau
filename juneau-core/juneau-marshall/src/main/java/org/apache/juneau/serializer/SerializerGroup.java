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

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;

/**
 * Represents a group of {@link Serializer Serializers} that can be looked up by media type.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Finds serializers based on HTTP <c>Accept</c> header values.
 * 	<li>
 * 		Sets common properties on all serializers in a single method call.
 * 	<li>
 * 		Clones existing groups and all serializers within the group in a single method call.
 * </ul>
 *
 * <h5 class='topic'>Match ordering</h5>
 *
 * Serializers are matched against <c>Accept</c> strings in the order they exist in this group.
 *
 * <p>
 * Adding new entries will cause the entries to be prepended to the group.
 * This allows for previous serializers to be overridden through subsequent calls.
 *
 * <p>
 * For example, calling <code>g.append(S1.<jk>class</jk>,S2.<jk>class</jk>).append(S3.<jk>class</jk>,S4.<jk>class</jk>)</code>
 * will result in the order <c>S3, S4, S1, S2</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct a new serializer group</jc>
 * 	SerializerGroup g = SerializerGroup.<jsm>create</jsm>();
 * 		.append(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>); <jc>// Add some serializers to it</jc>
 * 		.ws().swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>) <jc>// Change settings for all serializers in the group.</jc>
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
@ConfigurableContext(nocache=true)
public final class SerializerGroup {

	/**
	 * An identifier that a group of serializers should be inherited.
	 */
	@SuppressWarnings("javadoc")
	public static abstract class Inherit extends Serializer {
		protected Inherit(SerializerBuilder builder) {
			super(builder);
		}
	}

	/**
	 * An identifier that a group of serializers should not be inherited.
	 */
	@SuppressWarnings("javadoc")
	public static abstract class None extends Serializer {
		protected None(SerializerBuilder builder) {
			super(builder);
		}
	}

	// Maps Accept headers to matching serializers.
	private final ConcurrentHashMap<String,SerializerMatch> cache = new ConcurrentHashMap<>();

	private final List<MediaRange> mediaRanges;
	private final List<Serializer> mediaTypeRangeSerializers;

	private final List<MediaType> mediaTypesList;
	final Serializer[] serializers;

	/**
	 * Instantiates a new clean-slate {@link Builder} object.
	 *
	 * @return A new {@link Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this bean.
	 */
	protected SerializerGroup(Builder builder) {

		this.serializers = builder.serializers.stream().map(x -> build(x)).toArray(Serializer[]::new);

		AList<MediaRange> lmtr = AList.create();
		ASet<MediaType> lmt = ASet.of();
		AList<Serializer> l = AList.create();
		for (Serializer s : serializers) {
			for (MediaRange m: s.getMediaTypeRanges().getRanges()) {
				lmtr.add(m);
				l.add(s);
			}
			for (MediaType mt : s.getAcceptMediaTypes())
				lmt.add(mt);
		}

		this.mediaRanges = lmtr.unmodifiable();
		this.mediaTypesList = AList.of(lmt).unmodifiable();
		this.mediaTypeRangeSerializers = l.unmodifiable();
	}

	private Serializer build(Object o) {
		if (o instanceof Serializer)
			return (Serializer)o;
		return ((SerializerBuilder)o).build();
	}

	/**
	 * Creates a copy of this serializer group.
	 *
	 * @return A new copy of this serializer group.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Builder class for creating instances of {@link SerializerGroup}.
	 */
	public static class Builder {

		List<Object> serializers;
		private BeanContextBuilder bcBuilder;

		/**
		 * Create an empty serializer group builder.
		 */
		protected Builder() {
			this.serializers = AList.create();
		}

		/**
		 * Clone an existing serializer group.
		 *
		 * @param copyFrom The serializer group that we're copying settings and serializers from.
		 */
		protected Builder(SerializerGroup copyFrom) {
			this.serializers = AList.create().append(asList(copyFrom.serializers));
		}

		/**
		 * Clone an existing serializer group builder.
		 *
		 * <p>
		 * Serializer builders will be cloned during this process.
		 *
		 * @param copyFrom The serializer group that we're copying settings and serializers from.
		 */
		protected Builder(Builder copyFrom) {
			bcBuilder = copyFrom.bcBuilder == null ? null : copyFrom.bcBuilder.copy();
			serializers = AList.create();
			copyFrom.serializers.stream().map(x -> copyBuilder(x)).forEach(x -> serializers.add(x));
		}

		private Object copyBuilder(Object o) {
			if (o instanceof SerializerBuilder) {
				SerializerBuilder x = (SerializerBuilder)o;
				x = x.copy();
				if (bcBuilder != null)
					x.beanContextBuilder(bcBuilder);
				return x;
			}
			return o;
		}

		/**
		 * Copy creator.
		 *
		 * @return A new mutable copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Creates a new {@link SerializerGroup} object using a snapshot of the settings defined in this builder.
		 *
		 * <p>
		 * This method can be called multiple times to produce multiple serializer groups.
		 *
		 * @return A new {@link SerializerGroup} object.
		 */
		public SerializerGroup build() {
			return new SerializerGroup(this);
		}

		/**
		 * Associates an existing bean context builder with all serializer builders in this group.
		 *
		 * @param value The bean contest builder to associate.
		 * @return This object (for method chaining).
		 */
		public Builder beanContextBuilder(BeanContextBuilder value) {
			bcBuilder = value;
			forEach(x -> x.beanContextBuilder(value));
			return this;
		}

		/**
		 * Adds the specified serializers to this group.
		 *
		 * <p>
		 * Entries are added in-order to the beginning of the list in the group.
		 *
		 * @param values The serializers to add to this group.
		 * @return This object (for method chaining).
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Serializer}.
		 */
		public Builder add(Class<?>...values) {
			List<Object> l = new ArrayList<>();
			for (Class<?> e : values) {
				if (Serializer.class.isAssignableFrom(e)) {
					l.add(createBuilder(e));
				} else {
					throw runtimeException("Invalid type passed to SerializeGroup.Builder.add(): " + e.getName());
				}
			}
			serializers.addAll(0, l);
			return this;
		}

		/**
		 * Sets the specified serializers for this group.
		 *
		 * <p>
		 * Existing values are overwritten.
		 *
		 * <p>
		 * If {@link Inherit} (or any other class whose simple name is <js>"Inherit"</js>) is specified, then the existing values are copied
		 * into the final list in the position they appear in the values.
		 *
		 * @param values The serializers to set in this group.
		 * @return This object (for method chaining).
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Serializer} or named <js>"Inherit"</js>.
		 */
		public Builder set(Class<?>...values) {
			List<Object> l = new ArrayList<>();
			for (Class<?> e : values) {
				if (e.getSimpleName().equals("Inherit")) {
					l.addAll(serializers);
				} else if (Serializer.class.isAssignableFrom(e)) {
					l.add(createBuilder(e));
				} else {
					throw runtimeException("Invalid type passed to SerializeGroup.Builder.set(): " + e.getName());
				}
			}
			serializers = l;
			return this;
		}

		private Object createBuilder(Object o) {
			if (o instanceof Class) {
				@SuppressWarnings("unchecked")
				SerializerBuilder b = Serializer.createSerializerBuilder((Class<? extends Serializer>)o);
				if (bcBuilder != null)
					b.beanContextBuilder(bcBuilder);
				o = b;
			}
			return o;
		}

		/**
		 * Registers the specified serializers with this group.
		 *
		 * <p>
		 * When passing in pre-instantiated serializers to this group, applying properties and transforms to the group
		 * do not affect them.
		 *
		 * @param s The serializers to append to this group.
		 * @return This object (for method chaining).
		 */
		public Builder add(Serializer...s) {
			serializers.addAll(0, asList(s));
			return this;
		}

		/**
		 * Clears out any existing serializers in this group.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder clear() {
			serializers.clear();
			return this;
		}

		/**
		 * Returns <jk>true</jk> if at least one of the specified annotations can be applied to at least one serializer builder in this group.
		 *
		 * @param work The work to check.
		 * @return <jk>true</jk> if at least one of the specified annotations can be applied to at least one serializer builder in this group.
		 */
		public boolean canApply(List<AnnotationWork> work) {
			for (Object o : serializers)
				if (o instanceof SerializerBuilder)
					if (((SerializerBuilder)o).canApply(work))
						return true;
			return false;
		}

		/**
		 * Applies the specified annotations to all applicable serializer builders in this group.
		 *
		 * @param work The annotations to apply.
		 * @return This object (for method chaining).
		 */
		public Builder apply(List<AnnotationWork> work) {
			return forEach(x -> x.apply(work));
		}

		/**
		 * Performs an action on all serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object (for method chaining).
		 */
		public Builder forEach(Consumer<SerializerBuilder> action) {
			builders(SerializerBuilder.class).forEach(action);
			return this;
		}

		/**
		 * Performs an action on all writer serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object (for method chaining).
		 */
		public Builder forEachWS(Consumer<WriterSerializerBuilder> action) {
			return forEach(WriterSerializerBuilder.class, action);
		}

		/**
		 * Performs an action on all output stream serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object (for method chaining).
		 */
		public Builder forEachOSS(Consumer<OutputStreamSerializerBuilder> action) {
			return forEach(OutputStreamSerializerBuilder.class, action);
		}

		/**
		 * Performs an action on all serializer builders of the specified type in this group.
		 *
		 * @param type The serializer builder type.
		 * @param action The action to perform.
		 * @return This object (for method chaining).
		 */
		public <T extends SerializerBuilder> Builder forEach(Class<T> type, Consumer<T> action) {
			builders(type).forEach(action);
			return this;
		}

		@SuppressWarnings("unchecked")
		private <T extends SerializerBuilder> Stream<T> builders(Class<T> type) {
			return serializers.stream().filter(x -> type.isInstance(x)).map(x -> (T)x);
		}

		@Override /* Object */
		public String toString() {
			return serializers.stream().map(x -> toString(x)).collect(joining(",","[","]"));
		}

		private String toString(Object o) {
			if (o == null)
				return "null";
			if (o instanceof SerializerBuilder)
				return "builder:" + o.getClass().getName();
			return "serializer:" + o.getClass().getName();
		}
	}

	/**
	 * Searches the group for a serializer that can handle the specified <c>Accept</c> value.
	 *
	 * <p>
	 * The <c>accept</c> value complies with the syntax described in RFC2616, Section 14.1, as described below:
	 * <p class='bcode w800'>
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
		if (acceptHeader == null)
			return null;
		SerializerMatch sm = cache.get(acceptHeader);
		if (sm != null)
			return sm;

		Accept a = accept(acceptHeader);
		int match = a.match(mediaRanges);
		if (match >= 0) {
			sm = new SerializerMatch(mediaRanges.get(match), mediaTypeRangeSerializers.get(match));
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
		return unmodifiableList(asList(serializers));
	}

	/**
	 * Returns <jk>true</jk> if this group contains no serializers.
	 *
	 * @return <jk>true</jk> if this group contains no serializers.
	 */
	public boolean isEmpty() {
		return serializers.length == 0;
	}
}
