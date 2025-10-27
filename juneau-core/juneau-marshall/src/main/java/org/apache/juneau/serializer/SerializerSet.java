/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.serializer;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;

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
 * <p class='bjava'>
 * 	<jc>// Construct a new serializer group</jc>
 * 	SerializerSet <jv>serializers</jv> = SerializerSet.<jsm>create</jsm>();
 * 		.add(JsonSerializer.<jk>class</jk>, UrlEncodingSerializer.<jk>class</jk>) <jc>// Add some serializers to it</jc>
 * 		.forEach(<jv>x</jv> -&gt; <jv>x</jv>.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>))
 * 		.forEachWS(<jv>x</jv> -&gt; <jv>x</jv>.ws())
 * 		.build();
 *
 * 	<jc>// Find the appropriate serializer by Accept type</jc>
 * 	WriterSerializer <jv>serializer</jv> = <jv>serializers</jv>
 * 		.getWriterSerializer(<js>"text/foo, text/json;q=0.8, text/*;q:0.6, *\/*;q=0.0"</js>);
 *
 * 	<jc>// Serialize a bean to JSON text </jc>
 * 	AddressBook <jv>addressBook</jv> = <jk>new</jk> AddressBook();  <jc>// Bean to serialize.</jc>
 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>addressBook</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>

 * </ul>
 */
public class SerializerSet {
	/**
	 * Builder class.
	 */
	public static class Builder extends BeanBuilder<SerializerSet> {

		List<Object> entries;
		private BeanContext.Builder bcBuilder;

		/**
		 * Create an empty serializer group builder.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(SerializerSet.class, beanStore);
			this.entries = list();
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
			super(copyFrom);
			bcBuilder = copyFrom.bcBuilder == null ? null : copyFrom.bcBuilder.copy();
			entries = list();
			copyFrom.entries.stream().map(this::copyBuilder).forEach(x -> entries.add(x));
		}

		/**
		 * Clone an existing serializer group.
		 *
		 * @param copyFrom The serializer group that we're copying settings and serializers from.
		 */
		protected Builder(SerializerSet copyFrom) {
			super(copyFrom.getClass());
			this.entries = list((Object[])copyFrom.entries);
		}

		/**
		 * Adds the specified serializers to this group.
		 *
		 * <p>
		 * Entries are added in-order to the beginning of the list in the group.
		 *
		 * <p>
		 * The {@link NoInherit} class can be used to clear out the existing list of serializers before adding the new entries.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	SerializerSet.Builder <jv>builder</jv> = SerializerSet.<jsm>create</jsm>();  <jc>// Create an empty builder.</jc>
		 *
		 * 	<jv>builder</jv>.add(FooSerializer.<jk>class</jk>);  <jc>// Now contains:  [FooSerializer]</jc>
		 *
		 * 	<jv>builder</jv>.add(BarSerializer.<jk>class</jk>, BazSerializer.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazSerializer,FooSerializer]</jc>
		 *
		 * 	<jv>builder</jv>.add(NoInherit.<jk>class</jk>, QuxSerializer.<jk>class</jk>);  <jc>// Now contains:  [QuxSerializer]</jc>
		 * </p>
		 *
		 * @param values The serializers to add to this group.
		 * @return This object.
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Serializer}.
		 */
		public Builder add(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> e : values) {
				if (Serializer.class.isAssignableFrom(e)) {
					l.add(createBuilder(e));
				} else {
					throw new BasicRuntimeException("Invalid type passed to SerializeGroup.Builder.add(): {0}", e.getName());
				}
			}
			entries.addAll(0, l);
			return this;
		}

		/**
		 * Registers the specified serializers with this group.
		 *
		 * <p>
		 * When passing in pre-instantiated serializers to this group, applying properties and transforms to the group
		 * do not affect them.
		 *
		 * @param s The serializers to append to this group.
		 * @return This object.
		 */
		public Builder add(Serializer...s) {
			prependAll(entries, (Object[])s);
			return this;
		}

		/**
		 * Applies the specified annotations to all applicable serializer builders in this group.
		 *
		 * @param work The annotations to apply.
		 * @return This object.
		 */
		public Builder apply(AnnotationWorkList work) {
			return forEach(x -> x.apply(work));
		}

		/**
		 * Associates an existing bean context builder with all serializer builders in this group.
		 *
		 * @param value The bean contest builder to associate.
		 * @return This object.
		 */
		public Builder beanContext(BeanContext.Builder value) {
			bcBuilder = value;
			forEach(x -> x.beanContext(value));
			return this;
		}

		/**
		 * Applies an operation to the bean context builder.
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder beanContext(Consumer<BeanContext.Builder> operation) {
			if (nn(bcBuilder))
				operation.accept(bcBuilder);
			return this;
		}

		/**
		 * Returns <jk>true</jk> if at least one of the specified annotations can be applied to at least one serializer builder in this group.
		 *
		 * @param work The work to check.
		 * @return <jk>true</jk> if at least one of the specified annotations can be applied to at least one serializer builder in this group.
		 */
		public boolean canApply(AnnotationWorkList work) {
			for (Object o : entries)
				if (o instanceof Serializer.Builder)
					if (((Serializer.Builder)o).canApply(work))
						return true;
			return false;
		}

		/**
		 * Clears out any existing serializers in this group.
		 *
		 * @return This object.
		 */
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Makes a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Performs an action on all serializer builders of the specified type in this group.
		 *
		 * @param <T> The serializer builder type.
		 * @param type The serializer builder type.
		 * @param action The action to perform.
		 * @return This object.
		 */
		public <T extends Serializer.Builder> Builder forEach(Class<T> type, Consumer<T> action) {
			builders(type).forEach(action);
			return this;
		}

		/**
		 * Performs an action on all serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEach(Consumer<Serializer.Builder> action) {
			builders(Serializer.Builder.class).forEach(action);
			return this;
		}

		/**
		 * Performs an action on all output stream serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEachOSS(Consumer<OutputStreamSerializer.Builder> action) {
			return forEach(OutputStreamSerializer.Builder.class, action);
		}

		/**
		 * Performs an action on all writer serializer builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEachWS(Consumer<WriterSerializer.Builder> action) {
			return forEach(WriterSerializer.Builder.class, action);
		}

		@Override /* Overridden from BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		/**
		 * Returns direct access to the {@link Serializer} and {@link Serializer.Builder} objects in this builder.
		 *
		 * <p>
		 * Provided to allow for any extraneous modifications to the list not accomplishable via other methods on this builder such
		 * as re-ordering/adding/removing entries.
		 *
		 * <p>
		 * Note that it is up to the user to ensure that the list only contains {@link Serializer} and {@link Serializer.Builder} objects.
		 *
		 * @return The inner list of entries in this builder.
		 */
		public List<Object> inner() {
			return entries;
		}

		/**
		 * Sets the specified serializers for this group.
		 *
		 * <p>
		 * Existing values are overwritten.
		 *
		 * <p>
		 * The {@link Inherit} class can be used to insert existing entries in this group into the position specified.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	SerializerSet.Builder <jv>builder</jv> = SerializerSet.<jsm>create</jsm>();  <jc>// Create an empty builder.</jc>
		 *
		 * 	<jv>builder</jv>.set(FooSerializer.<jk>class</jk>);  <jc>// Now contains:  [FooSerializer]</jc>
		 *
		 * 	<jv>builder</jv>.set(BarSerializer.<jk>class</jk>, BazSerializer.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazSerializer]</jc>
		 *
		 * 	<jv>builder</jv>.set(Inherit.<jk>class</jk>, QuxSerializer.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazSerializer,QuxSerializer]</jc>
		 * </p>
		 *
		 * @param values The serializers to set in this group.
		 * @return This object.
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Serializer} or named <js>"Inherit"</js>.
		 */
		public Builder set(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> e : values) {
				if (e.getSimpleName().equals("Inherit")) {
					l.addAll(entries);
				} else if (Serializer.class.isAssignableFrom(e)) {
					l.add(createBuilder(e));
				} else {
					throw new BasicRuntimeException("Invalid type passed to SerializeGroup.Builder.set(): {0}", e.getName());
				}
			}
			entries = l;
			return this;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return entries.stream().map(this::toString).collect(joining(",", "[", "]"));
		}

		@Override /* Overridden from BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		private <T extends Serializer.Builder> Stream<T> builders(Class<T> type) {
			return entries.stream().filter(x -> type.isInstance(x)).map(x -> type.cast(x));
		}

		private Object copyBuilder(Object o) {
			if (o instanceof Serializer.Builder) {
				Serializer.Builder x = (Serializer.Builder)o;
				Serializer.Builder x2 = x.copy();
				if (ne(x.getClass(), x2.getClass()))
					throw new BasicRuntimeException("Copy method not implemented on class {0}", x.getClass().getName());
				x = x2;
				if (nn(bcBuilder))
					x.beanContext(bcBuilder);
				return x;
			}
			return o;
		}

		private Object createBuilder(Object o) {
			if (o instanceof Class) {

				// Check for no-arg constructor.
				ConstructorInfo ci = ClassInfo.of((Class<?>)o).getPublicConstructor(ConstructorInfo::hasNoParams);
				if (nn(ci))
					return ci.invoke();

				// Check for builder create method.
				@SuppressWarnings("unchecked")
				Serializer.Builder b = Serializer.createSerializerBuilder((Class<? extends Serializer>)o);
				if (nn(bcBuilder))
					b.beanContext(bcBuilder);
				o = b;
			}
			return o;
		}

		private String toString(Object o) {
			if (o == null)
				return "null";
			if (o instanceof Serializer.Builder)
				return "builder:" + o.getClass().getName();
			return "serializer:" + o.getClass().getName();
		}

		@Override /* Overridden from BeanBuilder */
		protected SerializerSet buildDefault() {
			return new SerializerSet(this);
		}
	}

	/**
	 * An identifier that the previous entries in this group should be inherited.
	 * <p>
	 * Used by {@link SerializerSet.Builder#set(Class...)}
	 */
	@SuppressWarnings("javadoc")
	public static abstract class Inherit extends Serializer {
		public Inherit(Serializer.Builder builder) {
			super(builder);
		}
	}

	/**
	 * An identifier that the previous entries in this group should not be inherited.
	 * <p>
	 * Used by {@link SerializerSet.Builder#add(Class...)}
	 */
	@SuppressWarnings("javadoc")
	public static abstract class NoInherit extends Serializer {
		public NoInherit(Serializer.Builder builder) {
			super(builder);
		}
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder(BeanStore.INSTANCE);
	}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	// Maps Accept headers to matching serializers.
	private final ConcurrentHashMap<String,SerializerMatch> cache = new ConcurrentHashMap<>();

	private final MediaRange[] mediaRanges;
	private final List<MediaRange> mediaRangesList;
	private final Serializer[] mediaTypeRangeSerializers;

	private final MediaType[] mediaTypes;
	private final List<MediaType> mediaTypesList;
	final Serializer[] entries;
	private final List<Serializer> entriesList;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this bean.
	 */
	protected SerializerSet(Builder builder) {

		this.entries = builder.entries.stream().map(this::build).toArray(Serializer[]::new);
		this.entriesList = u(alist(entries));

		List<MediaRange> lmtr = list();
		Set<MediaType> lmt = set();
		List<Serializer> l = list();
		for (Serializer e : entries) {
			e.getMediaTypeRanges().forEachRange(x -> {
				lmtr.add(x);
				l.add(e);
			});
			e.forEachAcceptMediaType(x -> lmt.add(x));
		}

		this.mediaRanges = lmtr.toArray(new MediaRange[lmtr.size()]);
		this.mediaRangesList = u(alist(mediaRanges));
		this.mediaTypes = lmt.toArray(new MediaType[lmt.size()]);
		this.mediaTypesList = u(alist(mediaTypes));
		this.mediaTypeRangeSerializers = l.toArray(new Serializer[l.size()]);
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
	 * Same as {@link #getSerializerMatch(String)} but matches using a {@link MediaType} instance.
	 *
	 * @param mediaType The HTTP media type.
	 * @return The serializer and media type that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public SerializerMatch getSerializerMatch(MediaType mediaType) {
		return getSerializerMatch(mediaType.toString());
	}

	/**
	 * Searches the group for a serializer that can handle the specified <c>Accept</c> value.
	 *
	 * <p>
	 * The <c>accept</c> value complies with the syntax described in RFC2616, Section 14.1, as described below:
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
		if (acceptHeader == null)
			return null;
		SerializerMatch sm = cache.get(acceptHeader);
		if (nn(sm))
			return sm;

		MediaRanges a = MediaRanges.of(acceptHeader);
		int match = a.match(mediaRangesList);
		if (match >= 0) {
			sm = new SerializerMatch(mediaRanges[match], mediaTypeRangeSerializers[match]);
			cache.putIfAbsent(acceptHeader, sm);
		}

		return cache.get(acceptHeader);
	}

	/**
	 * Returns a copy of the serializers in this group.
	 *
	 * @return An unmodifiable list of serializers in this group.
	 */
	public List<Serializer> getSerializers() { return entriesList; }

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
	 * Same as {@link #getSerializer(String)}, but casts it to an {@link OutputStreamSerializer}.
	 *
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public OutputStreamSerializer getStreamSerializer(String acceptHeader) {
		return (OutputStreamSerializer)getSerializer(acceptHeader);
	}

	/**
	 * Returns the media types that all serializers in this group can handle.
	 *
	 * <p>
	 * Entries are ordered in the same order as the serializers in the group.
	 *
	 * @return An unmodifiable list of media types.
	 */
	public List<MediaType> getSupportedMediaTypes() { return mediaTypesList; }

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
	 * Same as {@link #getSerializer(String)}, but casts it to a {@link WriterSerializer}.
	 *
	 * @param acceptHeader The HTTP <l>Accept</l> header string.
	 * @return The serializer that matched the accept header, or <jk>null</jk> if no match was made.
	 */
	public WriterSerializer getWriterSerializer(String acceptHeader) {
		return (WriterSerializer)getSerializer(acceptHeader);
	}

	/**
	 * Returns <jk>true</jk> if this group contains no serializers.
	 *
	 * @return <jk>true</jk> if this group contains no serializers.
	 */
	public boolean isEmpty() { return entries.length == 0; }

	private Serializer build(Object o) {
		if (o instanceof Serializer)
			return (Serializer)o;
		return ((Serializer.Builder)o).build();
	}
}