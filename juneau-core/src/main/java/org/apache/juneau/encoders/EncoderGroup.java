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

import org.apache.juneau.*;

/**
 * Represents the group of {@link Encoder encoders} keyed by codings.
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Maintains a set of encoders and the codings that they can handle.
 * <p>
 * 	The {@link #findMatch(String)} and {@link #getEncoder(String)} methods are then
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
 * <h6 class='topic'>Example:</h6>
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
public final class EncoderGroup {

	private Map<String,EncoderEntry> entryMap = new TreeMap<String,EncoderEntry>(String.CASE_INSENSITIVE_ORDER);
	private LinkedList<EncoderEntry> tempEntries = new LinkedList<EncoderEntry>();
	private EncoderEntry[] entries;

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
	public String findMatch(String acceptEncoding) {
		if (getEntries().length == 0)
			return null;

		MediaRange[] ae = MediaRange.parse(acceptEncoding);

		if (ae.length == 0)
			ae = MediaRange.parse("*");

		for (MediaRange a : ae)
			for (EncoderEntry e : getEntries())
				for (MediaRange a2 : e.encodingRanges)
					if (a.matches(a2))
						return a2.getType();

		return null;
	}

	/**
	 * Adds the specified encoders to this group.
	 *
	 * @param e The encoders to instantiate and add to this group.
	 * @return This object (for method chaining).
	 * @throws Exception If an instantiation error occurred.
	 */
	public EncoderGroup append(Class<? extends Encoder>...e) throws Exception {
		for (Class<? extends Encoder> r : reverse(e))
			append(r.newInstance());
		return this;
	}

	/**
	 * Adds the specified encoders to this group.
	 *
	 * @param e The encoder to instantiate and add to this group.
	 * @return This object (for method chaining).
	 * @throws Exception If an instantiation error occurred.
	 */
	public EncoderGroup append(Class<? extends Encoder> e) throws Exception {
		append(e.newInstance());
		return this;
	}

	/**
	 * Adds the specified encoders to this group.
	 *
	 * @param e The encoders to add to this group.
	 * @return This object (for method chaining).
	 */
	public EncoderGroup append(Encoder...e) {
		entries = null;
		for (Encoder r : reverse(e)) {
			EncoderEntry ee = new EncoderEntry(r);
			tempEntries.addFirst(ee);
			for (String s : ee.encodings)
				this.entryMap.put(s, ee);
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
		for (EncoderEntry e : reverse(g.getEntries()))
			append(e.encoder);
		return this;
	}

	/**
	 * Returns the encoder registered with the specified coding (e.g. <js>"gzip"</js>).
	 *
	 * @param coding The coding string.
	 * @return The encoder, or <jk>null</jk> if encoder isn't registered with that coding.
	 */
	public Encoder getEncoder(String coding) {
		EncoderEntry e = entryMap.get(coding);
		return (e == null ? null : e.encoder);
	}

	/**
	 * Returns the set of codings supported by all encoders in this group.
	 *
	 * @return The set of codings supported by all encoders in this group.  Never <jk>null</jk>.
	 */
	public List<String> getSupportedEncodings() {
		List<String> l = new ArrayList<String>();
		for (EncoderEntry e : getEntries())
			for (String enc : e.encodings)
				if (! l.contains(enc))
					l.add(enc);
		return l;
	}

	private EncoderEntry[] getEntries() {
		if (entries == null)
			entries = tempEntries.toArray(new EncoderEntry[tempEntries.size()]);
		return entries;
	}

	static class EncoderEntry {
		Encoder encoder;
		MediaRange[] encodingRanges;
		String[] encodings;

		EncoderEntry(Encoder e) {
			encoder = e;

			encodings = new String[e.getCodings().length];
			int i = 0;
			for (String enc : e.getCodings())
				encodings[i++] = enc;

			List<MediaRange> l = new LinkedList<MediaRange>();
			for (i = 0; i < encodings.length; i++)
				l.addAll(Arrays.asList(MediaRange.parse(encodings[i])));
			encodingRanges = l.toArray(new MediaRange[l.size()]);
		}
	}
}
