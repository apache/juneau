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

import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * Represents a single entry in a configuration.
 *
 * This is a read-only object.
 */
public class ConfigMapEntry {
	static final ConfigMapEntry NULL = new ConfigMapEntry(null, null, null, null, null);
	private static final AsciiSet REPLACE_CHARS = AsciiSet.of("\\#");
	final String rawLine;
	final String key, value, comment;

	final String modifiers;

	final List<String> preLines;

	ConfigMapEntry(String line, List<String> preLines) {
		this.rawLine = line;
		var i = line.indexOf('=');
		var key2 = line.substring(0, i).trim();

		var m1 = key2.indexOf('<');
		var m2 = key2.indexOf('>');

		modifiers = StringUtils.nullIfEmpty((m1 > -1 && m2 > m1) ? key2.substring(m1 + 1, m2) : null);

		this.key = m1 == -1 ? key2 : key2.substring(0, m1);

		line = line.substring(i + 1);

		i = line.indexOf('#');
		if (i != -1) {
			var l2 = StringUtils.splita(line, '#', 2);
			line = l2[0];
			if (l2.length == 2)
				this.comment = l2[1].trim();
			else
				this.comment = null;
		} else {
			this.comment = null;
		}

		this.value = StringUtils.replaceUnicodeSequences(line.trim());

		this.preLines = preLines == null ? Collections.emptyList() : u(CollectionUtils.copyOf(preLines));
	}

	ConfigMapEntry(String key, String value, String modifiers, String comment, List<String> preLines) {
		this.rawLine = null;
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.modifiers = modifiers;
		this.preLines = preLines == null ? Collections.emptyList() : u(CollectionUtils.copyOf(preLines));
	}

	/**
	 * Returns the same-line comment of this entry.
	 *
	 * @return The same-line comment of this entry.
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

	Writer writeTo(Writer w) throws IOException {
		if (value == null)
			return w;
		for (var pl : preLines)
			w.append(pl).append('\n');
		if (rawLine != null) {
			for (var i = 0; i < rawLine.length(); i++) {
				var c = rawLine.charAt(i);
				if (c == '\n')
					w.append('\n').append('\t');
				else if (c != '\r')
					w.append(c);
			}
			w.append('\n');
		} else {
			w.append(key);
			if (modifiers != null)
				w.append('<').append(modifiers).append('>');
			w.append(" = ");

			var val = value;
			for (var i = 0; i < val.length(); i++) {
				var c = val.charAt(i);
				if (c == '\n')
					w.append('\n').append('\t');
				else if (c != '\r') {
					if (REPLACE_CHARS.contains(c) || (Character.isISOControl(c) && ! (c == '\n' || c == '\r' || c == '\t'))) {
						w.append(StringUtils.unicodeSequence(c));
					} else {
						w.append(c);
					}
				}
			}

			if (isNotEmpty(comment))
				w.append(" # ").append(comment);

			w.append('\n');
		}
		return w;
	}
}