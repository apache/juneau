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
package org.apache.juneau.config.internal;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.common.internal.*;

/**
 * Represents a single entry in a configuration.
 *
 * This is a read-only object.
 */
public class ConfigMapEntry {
	final String rawLine;
	final String key, value, comment;
	final String modifiers;
	final List<String> preLines;

	static final ConfigMapEntry NULL = new ConfigMapEntry(null, null, null, null, null);

//	private final static AsciiSet MOD_CHARS = AsciiSet.create("#$%&*+^@~");

	ConfigMapEntry(String line, List<String> preLines) {
		this.rawLine = line;
		int i = line.indexOf('=');
		String key = line.substring(0, i).trim();

		int m1 = key.indexOf('<'), m2 = key.indexOf('>');
		modifiers = nullIfEmpty((m1 > -1 && m2 > m1) ? key.substring(m1+1, m2) : null);

		this.key = m1 == -1 ? key : key.substring(0, m1);

		line = line.substring(i+1);

		i = line.indexOf('#');
		if (i != -1) {
			String[] l2 = split(line, '#', 2);
			line = l2[0];
			if (l2.length == 2)
				this.comment = l2[1].trim();
			else
				this.comment = null;
		} else {
			this.comment = null;
		}

		this.value = StringUtils.replaceUnicodeSequences(line.trim());

		this.preLines = preLines == null ? emptyList() : unmodifiable(copyOf(preLines));
	}

	ConfigMapEntry(String key, String value, String modifiers, String comment, List<String> preLines) {
		this.rawLine = null;
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.modifiers = modifiers;
		this.preLines = preLines == null ? emptyList() : unmodifiable(copyOf(preLines));
	}

	/**
	 * Returns the name of this entry.
	 *
	 * @return The name of this entry.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Returns the raw value of this entry.
	 *
	 * @return The raw value of this entry.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the same-line comment of this entry.
	 *
	 * @return The same-line comment of this entry.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns the pre-lines of this entry.
	 *
	 * @return The pre-lines of this entry as an unmodifiable list.
	 */
	public List<String> getPreLines() {
		return preLines;
	}

	/**
	 * Returns the modifiers for this entry.
	 *
	 * @return The modifiers for this entry, or <jk>null</jk> if it has no modifiers.
	 */
	public String getModifiers() {
		return modifiers;
	}

	Writer writeTo(Writer w) throws IOException {
		if (value == null)
			return w;
		for (String pl : preLines)
			w.append(pl).append('\n');
		if (rawLine != null) {
			for (int i = 0; i < rawLine.length(); i++) {
				char c = rawLine.charAt(i);
				if (c == '\n')
					w.append('\n').append('\t');
				else if (c != '\r')
					w.append(c);
			}
			w.append('\n');
		} else {
			w.append(key);
			if (modifiers != null)
				w.append('<').append(new String(modifiers)).append('>');
			w.append(" = ");

			String val = value;
			for (int i = 0; i < val.length(); i++) {
				char c = val.charAt(i);
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

	private static final AsciiSet REPLACE_CHARS = AsciiSet.create("\\#");
}