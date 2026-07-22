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
package org.apache.juneau.config.internal;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.lang.*;

/**
 * Represents a single entry in a configuration.
 *
 * This is a read-only object.
 *
 * @param rawLine The raw line this entry was parsed from, or <jk>null</jk> if the entry was created programmatically.
 * @param key The name of this entry.
 * @param value The raw value of this entry.
 * @param comment The same-line comment of this entry, or <jk>null</jk> if it has no comment.
 * @param modifiers The modifiers for this entry, or <jk>null</jk> if it has no modifiers.
 * @param preLines The pre-lines of this entry as an unmodifiable list.
 */
public record ConfigMapEntry(String rawLine, String key, String value, String comment, String modifiers, List<String> preLines) {

	static final ConfigMapEntry NULL = new ConfigMapEntry(null, null, null, null, null, null);
	private static final AsciiSet REPLACE_CHARS = AsciiSet.of("\\#");

	/**
	 * Canonical constructor.
	 *
	 * <p>
	 * Normalizes {@code preLines} to an unmodifiable copy (empty when <jk>null</jk>) so this record is truly immutable.
	 */
	public ConfigMapEntry {
		preLines = preLines == null ? Collections.emptyList() : u(cp(preLines));
	}

	/**
	 * Constructor for a programmatically-created entry (no raw line).
	 *
	 * @param key The name of this entry.
	 * @param value The raw value of this entry.
	 * @param modifiers The modifiers for this entry, or <jk>null</jk> if it has no modifiers.
	 * @param comment The same-line comment of this entry, or <jk>null</jk> if it has no comment.
	 * @param preLines The pre-lines of this entry.  Can be <jk>null</jk> (treated as an empty list).
	 */
	ConfigMapEntry(String key, String value, String modifiers, String comment, List<String> preLines) {
		this(null, key, value, comment, modifiers, preLines);
	}

	/**
	 * Parses a raw config line into an entry.
	 *
	 * @param line The raw line to parse.
	 * @param preLines The pre-lines preceding this entry.
	 * @return A new immutable entry.
	 */
	static ConfigMapEntry parse(String line, List<String> preLines) {
		var i = line.indexOf('=');
		var key2 = line.substring(0, i).trim();

		var m1 = key2.indexOf('<');
		var m2 = key2.indexOf('>');

		var modifiers = nie((m1 > -1 && m2 > m1) ? key2.substring(m1 + 1, m2) : null);
		var key = m1 == -1 ? key2 : key2.substring(0, m1);

		var rest = line.substring(i + 1);

		String comment = null;
		if (rest.indexOf('#') != -1) {
			var l2 = splita(rest, '#', 2);
			rest = l2[0];
			if (l2.length == 2)
				comment = l2[1].trim();
		}

		var value = replaceUnicodeSequences(rest.trim());

		return new ConfigMapEntry(line, key, value, comment, modifiers, preLines);
	}

	/**
	 * Returns the same-line comment of this entry.
	 *
	 * @return The same-line comment of this entry, or <jk>null</jk> if it has no comment.
	 */
	public String getComment() { return comment; }

	/**
	 * Returns the name of this entry.
	 *
	 * @return The name of this entry.
	 */
	public String getKey() { return key; }

	/**
	 * Returns the modifiers for this entry.
	 *
	 * @return The modifiers for this entry, or <jk>null</jk> if it has no modifiers.
	 */
	public String getModifiers() { return modifiers; }

	/**
	 * Returns the pre-lines of this entry.
	 *
	 * @return The pre-lines of this entry as an unmodifiable list.
	 */
	public List<String> getPreLines() { return preLines; }

	/**
	 * Returns the raw value of this entry.
	 *
	 * @return The raw value of this entry.
	 */
	public String getValue() { return value; }

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for config entry writer
	})
	Writer writeTo(Writer w) throws IOException {
		if (value == null)
			return w;
		for (var pl : preLines)
			w.append(pl).append('\n');
		if (nn(rawLine)) {
			for (var i = 0; i < rawLine.length(); i++) {
				var c = rawLine.charAt(i);
				if (c == '\n')
					w.append('\n').append('\t');
				else if (c != '\r') // HTT - '\r' in raw line requires OS-level carriage return in stored config content
					w.append(c);
			}
			w.append('\n');
		} else {
			w.append(key);
			if (nn(modifiers))
				w.append('<').append(modifiers).append('>');
			w.append(" = ");

			var val = value;
			for (var i = 0; i < val.length(); i++) {
				var c = val.charAt(i);
				if (c == '\n')
					w.append('\n').append('\t');
				else if (c != '\r') { // HTT - '\r' in value requires OS-level carriage return injection
					if (REPLACE_CHARS.contains(c) || (Character.isISOControl(c) && ! (c == '\n' || c == '\r' || c == '\t'))) { // HTT - some branch combinations (e.g. '\r' in isISOControl) are unreachable due to the outer '\r' check
						w.append(unicodeSequence(c));
					} else {
						w.append(c);
					}
				}
			}

			if (ine(comment))
				w.append(" # ").append(comment);

			w.append('\n');
		}
		return w;
	}
}
