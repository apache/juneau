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
package org.apache.juneau.config.event;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.config.event.ConfigEventType.*;

import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * Represents a change to a config.
 */
public class ConfigEvent {

	/**
	 * Removes a value from a configuration.
	 *
	 * @param config
	 * 	The configuration name.
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param key
	 * 	The entry name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent removeEntry(String config, String section, String key) {
		return new ConfigEvent(REMOVE_ENTRY, config, section, key, null, null, null, null);
	}

	/**
	 * Removes a section from the config.
	 *
	 * @param config
	 * 	The configuration name.
	 * @param section
	 * 	The section name.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent removeSection(String config, String section) {
		return new ConfigEvent(REMOVE_SECTION, config, section, null, null, null, null, null);
	}

	/**
	 * Sets or replaces a value in a configuration.
	 *
	 * @param config
	 * 	The configuration name.
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param key
	 * 	The entry name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param value
	 * 	The entry value.
	 * 	<br>Can be <jk>null</jk> to remove an entry.
	 * @param comment
	 * 	Optional comment string to add on the same line as the entry.
	 * @param modifiers
	 * 	Optional entry modifiers.
	 * @param prelines
	 * 	Comment lines that occur before this entry.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent setEntry(String config, String section, String key, String value, String modifiers, String comment, List<String> prelines) {
		return new ConfigEvent(SET_ENTRY, config, section, key, value, modifiers, comment, prelines);
	}

	/**
	 * Adds a new empty section to the config.
	 *
	 * @param config
	 * 	The configuration name.
	 * @param section
	 * 	The section name.
	 * @param prelines
	 * 	Comment lines that occur before this section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent setSection(String config, String section, List<String> prelines) {
		return new ConfigEvent(SET_SECTION, config, section, null, null, null, null, prelines);
	}

	private final ConfigEventType type;
	private final String config, section, key, value, comment;

	private final List<String> preLines;

	private final String modifiers;

	/**
	 * Constructor.
	 * @param type - The event type.
	 * @param config - The configuration name.
	 * @param section - The section name.
	 * @param key - The entry name.
	 * @param value - The entry value.
	 * @param modifiers - The entry modifiers.
	 * @param comment - Optional comment string to add on the same line as the entry.
	 * @param preLines - Optional comment lines that occur before this entry.
	 */
	protected ConfigEvent(ConfigEventType type, String config, String section, String key, String value, String modifiers, String comment, List<String> preLines) {
		this.type = type;
		this.config = config;
		this.section = section;
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.preLines = preLines;
		this.modifiers = modifiers;
	}

	/**
	 * Returns the entry comment.
	 *
	 * @return The entry comment.
	 */
	public String getComment() { return comment; }

	/**
	 * Returns the configuration name.
	 *
	 * @return The configuration name.
	 */
	public String getConfig() { return config; }

	/**
	 * Returns the entry name.
	 *
	 * @return The entry name.
	 */
	public String getKey() { return key; }

	/**
	 * Returns the modifiers on this entry.
	 *
	 * @return
	 * 	The modifier characters.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getModifiers() { return modifiers; }

	/**
	 * Returns the section or entry lines.
	 *
	 * @return The section or entry lines.
	 */
	public List<String> getPreLines() { return preLines; }

	/**
	 * Returns the section name.
	 *
	 * @return The section name.
	 */
	public String getSection() { return section; }

	/**
	 * Returns the event type.
	 *
	 * @return The event type.
	 */
	public ConfigEventType getType() { return type; }

	/**
	 * Returns the entry value.
	 *
	 * @return The entry value.
	 */
	public String getValue() { return value; }

	@Override /* Overridden from Object */
	public String toString() {
		return switch (type) {
			case REMOVE_SECTION -> "REMOVE_SECTION(" + section + ")";
			case REMOVE_ENTRY -> "REMOVE_ENTRY(" + section + (section.isEmpty() ? "" : "/") + key + ")";
			case SET_SECTION -> "SET_SECTION(" + section + ", preLines=" + StringUtils.join(preLines, '|') + ")";
			case SET_ENTRY -> {
				var out = new StringBuilder("SET(");
				out.append(section + (section.isEmpty() ? "" : "/") + key);
				if (modifiers != null)
					out.append(modifiers);
				out.append(" = ");
				var val = value == null ? "null" : value;
				if (val.indexOf('\n') != -1)
					val = val.replaceAll("(\\r?\\n)", "$1\t");
				if (val.indexOf('#') != -1)
					val = val.replace("#", "\\#");
				out.append(val);
				if (isNotEmpty(comment))
					out.append(" # ").append(comment);
				out.append(')');
				yield out.toString();
			}
			default -> null; // NOSONAR - Intentional.
		};
	}
}