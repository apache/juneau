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
package org.apache.juneau.encoders;

import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Represents the set of {@link Encoder encoders} keyed by codings.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Maintains a set of encoders and the codings that they can handle.
 *
 * <p>
 * The {@link #getEncoderMatch(String)} and {@link #getEncoder(String)} methods are then used to find appropriate
 * encoders for specific <c>Accept-Encoding</c> and <c>Content-Encoding</c> header values.
 *
 * <h5 class='topic'>Match ordering</h5>
 *
 * Encoders are matched against <c>Accept-Encoding</c> strings in the order they exist in this group.
 *
 * <p>
 * Encoders are tried in the order they appear in the set.  The {@link Builder#add(Class...)}/{@link Builder#add(Encoder...)}
 * methods prepend the values to the list to allow them the opportunity to override encoders already in the list.
 *
 * <p>
 * For example, calling <code>groupBuilder.add(E1.<jk>class</jk>,E2.<jk>class</jk>).add(E3.<jk>class</jk>,
 * E4.<jk>class</jk>)</code> will result in the order <c>E3, E4, E1, E2</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an encoder group with support for gzip compression.</jc>
 * 	EncoderSet <jv>encoders</jv> = EncoderSet
 * 		.<jsm>create</jsm>()
 * 		.add(GzipEncoder.<jk>class</jk>)
 * 		.build();
 *
 * 	<jc>// Should return "gzip"</jc>
 * 	String <jv>matchedCoding</jv> = <jv>encoders</jv>.findMatch(<js>"compress;q=1.0, gzip;q=0.8, identity;q=0.5, *;q=0"</js>);
 *
 * 	<jc>// Get the encoder</jc>
 * 	Encoder <jv>encoder</jv> = <jv>encoders</jv>.getEncoder(<jv>matchedCoding</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Encoders">Encoders</a>
 * </ul>
 */
public final class EncoderSet {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * An identifier that the previous encoders in this group should be inherited.
	 * <p>
	 * Used by {@link Builder#set(Class...)}
	 */
	public static abstract class Inherit extends Encoder {}

	/**
	 * An identifier that the previous encoders in this group should not be inherited.
	 * <p>
	 * Used by {@link Builder#add(Class...)}
	 */
	public static abstract class NoInherit extends Encoder {}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder(BeanStore.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<EncoderSet> {
		List<Object> entries;
		Builder inheritFrom;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(EncoderSet.class, beanStore);
			entries = list();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder being copied.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			entries = copyOf(copyFrom.entries);
		}

		@Override /* BeanBuilder */
		protected EncoderSet buildDefault() {
			return new EncoderSet(this);
		}

		/**
		 * Makes a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Registers the specified encoders with this group.
		 *
		 * <p>
		 * Entries are added in-order to the beginning of the list.
		 *
		 * @param values The encoders to add to this group.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link Encoder}.
		 */
		public Builder add(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> v : values)
				if (v.getSimpleName().equals("NoInherit"))
					clear();
			for (Class<?> v : values) {
				if (Encoder.class.isAssignableFrom(v)) {
					l.add(v);
				} else if (! v.getSimpleName().equals("NoInherit")) {
					throw new IllegalArgumentException("Invalid type passed to EncoderSet.Builder.add(): " + v.getName());
				}
			}
			entries.addAll(0, l);
			return this;
		}

		/**
		 * Sets the encoders in this group.
		 *
		 * <p>
		 * All encoders in this group are replaced with the specified values.
		 *
		 * <p>
		 * If {@link Inherit} is specified (or any other class whose simple name is <js>"Inherit"</js>, the existing values are preserved
		 * and inserted into the position in the values array.
		 *
		 * @param values The encoders to add to this group.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link Encoder}.
		 */
		public Builder set(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> v : values) {
				if (v.getSimpleName().equals("Inherit")) {
					l.addAll(entries);
				} else if (Encoder.class.isAssignableFrom(v)) {
					l.add(v);
				} else {
					throw new IllegalArgumentException("Invalid type passed to EncoderSet.Builder.set(): " + v.getName());
				}
			}
			entries = l;
			return this;
		}

		/**
		 * Registers the specified encoders with this group.
		 *
		 * <p>
		 * Entries are added to the beginning of the list.
		 *
		 * @param values The encoders to add to this group.
		 * @return This object.
		 */
		public Builder add(Encoder...values) {
			prependAll(entries, (Object[])values);
			return this;
		}

		/**
		 * Clears out any existing encoders in this group.
		 *
		 * @return This object.
		 */
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Returns <jk>true</jk> if this builder is empty.
		 *
		 * @return <jk>true</jk> if this builder is empty.
		 */
		public boolean isEmpty() {
			return entries.isEmpty();
		}

		/**
		 * Returns direct access to the {@link Encoder} objects and classes in this builder.
		 *
		 * <p>
		 * Provided to allow for any extraneous modifications to the list not accomplishable via other methods on this builder such
		 * as re-ordering/adding/removing entries.
		 *
		 * <p>
		 * Note that it is up to the user to ensure that the list only contains {@link Encoder} objects and classes.
		 *
		 * @return The inner list of entries in this builder.
		 */
		public List<Object> inner() {
			return entries;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//-------------------------------------------------------------------------------------------------------------
		// Other methods
		//-------------------------------------------------------------------------------------------------------------

		@Override /* Object */
		public String toString() {
			return entries.stream().map(x -> toString(x)).collect(joining(",","[","]"));
		}

		private static String toString(Object o) {
			if (o == null)
				return "null";
			if (o instanceof Class)
				return "class:" + ((Class<?>)o).getSimpleName();
			return "object:" + o.getClass().getSimpleName();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	// Maps Accept-Encoding headers to matching encoders.
	private final ConcurrentHashMap<String,EncoderMatch> cache = new ConcurrentHashMap<>();

	private final List<String> encodings;
	private final Encoder[] encodingsEncoders;
	private final Encoder[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected EncoderSet(Builder builder) {
		entries = builder.entries.stream().map(x -> instantiate(builder.beanStore(), x)).toArray(Encoder[]::new);

		List<String> lc = list();
		List<Encoder> l = list();
		for (Encoder e : entries) {
			for (String c: e.getCodings()) {
				lc.add(c);
				l.add(e);
			}
		}

		this.encodings = unmodifiable(lc);
		this.encodingsEncoders = l.toArray(new Encoder[l.size()]);
	}

	private static Encoder instantiate(BeanStore bs, Object o) {
		if (o instanceof Encoder)
			return (Encoder)o;
		try {
			return bs.createBean(Encoder.class).type((Class<?>)o).run();
		} catch (ExecutableException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Returns the coding string for the matching encoder that can handle the specified <c>Accept-Encoding</c>
	 * or <c>Content-Encoding</c> header value.
	 *
	 * <p>
	 * Returns <jk>null</jk> if no encoders can handle it.
	 *
	 * <p>
	 * This method is fully compliant with the RFC2616/14.3 and 14.11 specifications.
	 *
	 * @param acceptEncoding The <c>Accept-Encoding</c> or <c>Content-Encoding</c> value.
	 * @return The coding value (e.g. <js>"gzip"</js>).
	 */
	public EncoderMatch getEncoderMatch(String acceptEncoding) {
		EncoderMatch em = cache.get(acceptEncoding);
		if (em != null)
			return em;

		StringRanges ae = StringRanges.of(acceptEncoding);
		int match = ae.match(encodings);

		if (match >= 0) {
			em = new EncoderMatch(encodings.get(match), encodingsEncoders[match]);
			cache.putIfAbsent(acceptEncoding, em);
		}

		return cache.get(acceptEncoding);
	}

	/**
	 * Returns the encoder registered with the specified coding (e.g. <js>"gzip"</js>).
	 *
	 * @param encoding The coding string.
	 * @return The encoder, or <jk>null</jk> if encoder isn't registered with that coding.
	 */
	public Encoder getEncoder(String encoding) {
		EncoderMatch em = getEncoderMatch(encoding);
		return (em == null ? null : em.getEncoder());
	}

	/**
	 * Returns the set of codings supported by all encoders in this group.
	 *
	 * @return An unmodifiable list of codings supported by all encoders in this group.  Never <jk>null</jk>.
	 */
	public List<String> getSupportedEncodings() {
		return encodings;
	}
}
