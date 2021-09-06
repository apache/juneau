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

import static java.util.Arrays.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.header.*;

/**
 * Represents the group of {@link Encoder encoders} keyed by codings.
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
 * Encoders are tried in the order they appear in the group.  The {@link Builder#add(Class...)}/{@link Builder#add(Encoder...)}
 * methods prepend the values to the list to allow them the opportunity to override encoders already in the list.
 *
 * <p>
 * For example, calling <code>groupBuilder.add(E1.<jk>class</jk>,E2.<jk>class</jk>).add(E3.<jk>class</jk>,
 * E4.<jk>class</jk>)</code> will result in the order <c>E3, E4, E1, E2</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create an encoder group with support for gzip compression.</jc>
 * 	EncoderGroup <jv>group</jv> = EncoderGroup
 * 		.<jsm>create</jsm>()
 * 		.add(GzipEncoder.<jk>class</jk>)
 * 		.build();
 *
 * 	<jc>// Should return "gzip"</jc>
 * 	String <jv>matchedCoding</jv> = <jv>group</jv>.findMatch(<js>"compress;q=1.0, gzip;q=0.8, identity;q=0.5, *;q=0"</js>);
 *
 * 	<jc>// Get the encoder</jc>
 * 	Encoder <jv>encoder</jv> = <jv>group</jv>.getEncoder(<jv>matchedCoding</jv>);
 * </p>
 */
public final class EncoderGroup {

	/**
	 * An identifier that a group of encoders should inherit from another group.
	 */
	public static abstract class Inherit extends Encoder {}

	/**
	 * A default encoder group consisting of identity and G-Zip encoding.
	 */
	public static final EncoderGroup DEFAULT = create().add(IdentityEncoder.class, GzipEncoder.class).build();

	// Maps Accept-Encoding headers to matching encoders.
	private final ConcurrentHashMap<String,EncoderMatch> cache = new ConcurrentHashMap<>();

	private final List<String> encodings;
	private final Encoder[] encodingsEncoders;
	private final List<Encoder> encoders;

	/**
	 * Instantiates a new clean-slate {@link EncoderGroup.Builder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> EncoderGroupBuilder()</code>.
	 *
	 * @return A new {@link EncoderGroup.Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected EncoderGroup(Builder builder) {
		List<Encoder> x = new ArrayList<>();
		BeanStore bs = builder.beanStore;
		for (Object e : builder.encoders) {
			if (e == EncoderGroup.Inherit.class && builder.inheritFrom != null) {
				for (Object e2 : builder.inheritFrom.encoders)
					x.add(instantiate(bs, e2));
			} else {
				x.add(instantiate(bs, e));
			}
		}
		encoders = Collections.unmodifiableList(x);

		AList<String> lc = AList.create();
		AList<Encoder> l = AList.create();
		for (Encoder e : encoders) {
			for (String c: e.getCodings()) {
				lc.add(c);
				l.add(e);
			}
		}

		this.encodings = lc.unmodifiable();
		this.encodingsEncoders = l.asArrayOf(Encoder.class);
	}

	@SuppressWarnings("unchecked")
	private static Encoder instantiate(BeanStore bs, Object o) {
		if (o instanceof Encoder)
			return (Encoder)o;
		try {
			return bs.createBean(Encoder.class, (Class<? extends Encoder>)o);
		} catch (ExecutableException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builder class for this object.
	 */
	public static class Builder {
		final AList<Object> encoders;
		Builder inheritFrom;
		BeanStore beanStore = BeanStore.create().build();

		Builder() {
			encoders = AList.create();
		}

		/**
		 * Registers the specified encoders with this group.
		 *
		 * <p>
		 * Entries are added to the beginning of the list.
		 *
		 * @param values The encoders to add to this group.
		 * @return This object (for method chaining).
		 * @throws IllegalArgumentException if any class does not extend from {@link Encoder}.
		 */
		public Builder add(Class<?>...values) {
			encoders.addAll(0, asList(assertClassArrayArgIsType("values", Encoder.class, values)));
			return this;
		}

		/**
		 * Registers the specified encoders with this group.
		 *
		 * <p>
		 * Entries are added to the beginning of the list.
		 *
		 * @param values The encoders to add to this group.
		 * @return This object (for method chaining).
		 */
		public Builder add(Encoder...values) {
			encoders.addAll(0, asList(values));
			return this;
		}

		/**
		 * Associates a bean store with this builder.
		 *
		 * <p>
		 * Used for instantiating encoders specified via classes.
		 *
		 * @param value The new value for this setting.
		 * @return This object (for method chaining).
		 */
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}

		/**
		 * Associates a parent builder with this builder.
		 *
		 * <p>
		 * When {@link Inherit} is passed to {@link #add(Class...)}, the entries in the specified
		 * group will be inserted into this group.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// A group being inherited.</jc>
		 * 	EncoderGroup <jv>group1</jv> = EncoderGroup
		 * 		.<jsm>create</jsm>()
		 * 		.add(GzipEncoder.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// A group inheriting another group.</jc>
		 * 	<jc>// The group ends up containing [IdentityEncoder, GZipEncoder] in that order.</jc>
		 * 	EncoderGroup <jv>group2</jv> = EncoderGroup
		 * 		.<jsm>create</jsm>()
		 * 		.inheritFrom(<jv>group1</jv>)
		 * 		.add(IdentityEncoder.<jk>class</jk>, EncoderGroup.Inherit.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * @param value The group to inherit from.
		 * @return This object (for method chaining).
		 */
		public Builder inheritFrom(EncoderGroup.Builder value) {
			inheritFrom = value;
			return this;
		}

		/**
		 * Creates a new {@link EncoderGroup} object using a snapshot of the settings defined in this builder.
		 *
		 * <p>
		 * This method can be called multiple times to produce multiple encoder groups.
		 *
		 * @return A new {@link EncoderGroup} object.
		 */
		public EncoderGroup build() {
			return new EncoderGroup(this);
		}

		/**
		 * Returns <jk>true</jk> if this builder is empty.
		 *
		 * @return <jk>true</jk> if this builder is empty.
		 */
		public boolean isEmpty() {
			return encoders.isEmpty();
		}

		@Override /* Object */
		public String toString() {
			return encoders.stream().map(x -> toString(x)).collect(joining(",","[","]"));
		}

		private static String toString(Object o) {
			if (o == null)
				return "null";
			if (o instanceof Class)
				return "class:" + ((Class<?>)o).getSimpleName();
			return "object:" + o.getClass().getSimpleName();
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

		AcceptEncoding ae = acceptEncoding(acceptEncoding);
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
