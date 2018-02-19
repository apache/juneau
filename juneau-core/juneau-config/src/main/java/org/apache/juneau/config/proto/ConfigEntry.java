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
package org.apache.juneau.config.proto;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Represents a single entry in a configuration.
 * 
 * This is a read-only object.
 */
public class ConfigEntry {
	final String rawLine;
	final String key, value, comment;
	final String modifiers;
	final List<String> preLines;
	
	private final static AsciiSet MOD_CHARS = new AsciiSet("#$%&*+^@~");

	ConfigEntry(String line, List<String> preLines) {
		this.rawLine = line;
		int i = line.indexOf('=');
		String key = line.substring(0, i).trim();
		
		int modIndex = key.length();
		for (int j = key.length()-1; j > 0; j--) 
			if (MOD_CHARS.contains(key.charAt(j)))
				modIndex--;

		this.modifiers = key.substring(modIndex);
		this.key = key.substring(0, modIndex);
		
		line = line.substring(i+1);
	
		i = line.indexOf('#');
		if (i != -1) {
			String[] l2 = StringUtils.split(line, '#', 2);
			line = l2[0];
			if (l2.length == 2)
				this.comment = l2[1].trim();
			else
				this.comment = null;
		} else {
			this.comment = null;
		}
	
		this.value = line.trim();

		this.preLines = immutableList(preLines);
	}
	
	ConfigEntry(String key, String value, String modifiers, String comment, List<String> preLines) {
		this.rawLine = null;
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.modifiers = modifiers;
		this.preLines = immutableList(preLines);
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
	 * Returns whether this entry has the specified modifier.
	 * 
	 * @param m The modifier character.
	 * @return <jk>true</jk> if this entry is encoded.
	 */
	public boolean hasModifier(char m) {
		return modifiers.indexOf(m) != -1;
	}
	
	Writer writeTo(Writer out) throws IOException {
		if (value == null)
			return out;
		for (String pl : preLines)
			out.append(pl).append('\n');
		if (rawLine != null) {
			String l = rawLine;
			if (l.indexOf('\n') != -1)
				l = l.replaceAll("(\\r?\\n)", "$1\t");
			out.append(l).append('\n');
		} else {
			out.append(key);
			out.append(modifiers);
			out.append(" = ");
			
			String val = value;
			if (val.indexOf('\n') != -1)
				val = val.replaceAll("(\\r?\\n)", "$1\t");
			if (val.indexOf('#') != -1)
				val = val.replaceAll("#", "\\\\#");
			out.append(val);
				
			if (comment != null) 
				out.append(" # ").append(comment);

			out.append('\n');
		}
		return out;
	}
}