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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Represents the group of {@link Encoder encoders} keyed by codings.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	Maintains a set of encoders and the codings that they can handle.
 * <p>
 * 	The {@link #getEncoderMatch(String)} and {@link #getEncoder(String)} methods are then
 * 		used to find appropriate encoders for specific <code>Accept-Encoding</code>
 * 		and <code>Content-Encoding</code> header values.
 *
 * <h6 class='topic'>Match ordering</h6>
 * <p>
 * 	Encoders are matched against <code>Accept-Encoding</code> strings in the order they exist in this group.
 * <p>
 * 	Adding new entries will cause the entries to be prepended to the group.
 *  	This allows for previous encoders to be overridden through subsequent calls.
 * <p>
 * 	For example, calling <code>g.append(E1.<jk>class</jk>,E2.<jk>class</jk>).append(E3.<jk>class</jk>,E4.<jk>class</jk>)</code>
 * 	will result in the order <code>E3, E4, E1, E2</code>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create an encoder group with support for gzip compression.</jc>
 * 	EncoderGroup g = <jk>new</jk> EncoderGroup().append(GzipEncoder.<jk>class</jk>);
 *
 * 	<jc>// Should return "gzip"</jc>
 * 	String matchedCoding = g.findMatch(<js>"compress;q=1.0, gzip;q=0.8, identity;q=0.5, *;q=0"</js>);
 *
 * 	<jc>// Get the encoder</jc>
 * 	IEncoder encoder = g.getEncoder(matchedCoding);
 * </p>
 */
public final class EncoderGroup extends Lockable {

	// Maps Accept-Encoding headers to matching encoders.
	private final Map<String,EncoderMatch> cache = new ConcurrentHashMap<String,EncoderMatch>();

	private final CopyOnWriteArrayList<Encoder> encoders = new CopyOnWriteArrayList<Encoder>();

	/**
	 * Adds the specified encoder to the beginning of this group.
	 *
	 * @param e - The encoder to add to this group.
	 * @return This object (for method chaining).
	 */
	public EncoderGroup append(Encoder e) {
		checkLock();
		synchronized(encoders) {
			cache.clear();
			encoders.add(0, e);
		}
		return this;
	}

	/**
	 * Registers the specified encoders with this group.
	 *
	 * @param e The encoders to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Encoder} could not be constructed.
	 */
	public EncoderGroup append(Class<? extends Encoder>...e) throws Exception {
		for (Class<? extends Encoder> ee : ArrayUtils.reverse(e))
			append(ee);
		return this;
	}

	/**
	 * Same as {@link #append(Class[])}, except specify a single class to avoid unchecked compile warnings.
	 *
	 * @param e The encoder to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Serializer} could not be constructed.
	 */
	public EncoderGroup append(Class<? extends Encoder> e) throws Exception {
		try {
			append(e.newInstance());
		} catch (NoClassDefFoundError x) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		}
		return this;
	}

	/**
	 * Adds the encoders in the specified group to this group.
	 *
	 * @param g The group containing the encoders to add to this group.
	 * @return This object (for method chaining).
	 */
	public EncoderGroup append(EncoderGroup g) {
		for (Encoder e : reverse(g.encoders.toArray(new Encoder[g.encoders.size()])))
			append(e);
		return this;
	}

	/**
	 * Returns the coding string for the matching encoder that can handle the specified <code>Accept-Encoding</code>
	 * 	or <code>Content-Encoding</code> header value.
	 * <p>
	 * 	Returns <jk>null</jk> if no encoders can handle it.
	 * <p>
	 * 	This method is fully compliant with the RFC2616/14.3 and 14.11 specifications.
	 *
	 * @param acceptEncoding The <code>Accept-Encoding</code> or <code>Content-Encoding</code> value.
	 * @return The coding value (e.g. <js>"gzip"</js>).
	 */
	public EncoderMatch getEncoderMatch(String acceptEncoding) {
		if (encoders.size() == 0)
			return null;

		EncoderMatch em = cache.get(acceptEncoding);
		if (em != null)
			return em;

		MediaRange[] ae = MediaRange.parse(acceptEncoding);

		if (ae.length == 0)
			ae = MediaRange.parse("*/*");

		Map<Float,EncoderMatch> m = null;

		for (MediaRange a : ae) {
			for (Encoder e : encoders) {
				for (String c : e.getCodings()) {
					MediaType mt = MediaType.forString(c);
					float q = a.matches(mt);
					if (q == 1) {
						em = new EncoderMatch(mt, e);
						cache.put(acceptEncoding, em);
						return em;
					} else if (q > 0) {
						if (m == null)
							m = new TreeMap<Float,EncoderMatch>(Collections.reverseOrder());
						m.put(q, new EncoderMatch(mt, e));
					}
				}
			}
		}
		return (m == null ? null : m.values().iterator().next());
	}

	/**
	 * Returns the encoder registered with the specified coding (e.g. <js>"gzip"</js>).
	 *
	 * @param coding The coding string.
	 * @return The encoder, or <jk>null</jk> if encoder isn't registered with that coding.
	 */
	public Encoder getEncoder(String coding) {
		EncoderMatch em = getEncoderMatch(coding);
		return (em == null ? null : em.getEncoder());
	}

	/**
	 * Returns the set of codings supported by all encoders in this group.
	 *
	 * @return The set of codings supported by all encoders in this group.  Never <jk>null</jk>.
	 */
	public List<String> getSupportedEncodings() {
		List<String> l = new ArrayList<String>();
		for (Encoder e : encoders)
			for (String enc : e.getCodings())
				if (! l.contains(enc))
					l.add(enc);
		return l;
	}
}
