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
package org.apache.juneau.config.event;

import static org.apache.juneau.config.event.ConfigEventType.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a change to a config.
 */
@BeanIgnore
public class ConfigEvent {
	
	private final ConfigEventType type;
	private final String section, key, value, comment;
	private final List<String> preLines;
	private final String modifiers;
	
	private ConfigEvent(ConfigEventType type, String section, String key, String value, String modifiers, String comment, List<String> preLines) {
		this.type = type;
		this.section = section;
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.preLines = preLines;
		this.modifiers = modifiers;
	}
	
	//---------------------------------------------------------------------------------------------
	// Static creators.
	//---------------------------------------------------------------------------------------------
	
	/**
	 * Sets or replaces a value in a configuration.
	 * 
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
	public static ConfigEvent setEntry(String section, String key, String value, String modifiers, String comment, List<String> prelines) {
		return new ConfigEvent(SET_ENTRY, section, key, value, modifiers, comment, prelines);
	}
	
	/**
	 * Removes a value from a configuration.
	 * 
	 * @param section 
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param key
	 * 	The entry name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent removeEntry(String section, String key) {
		return new ConfigEvent(REMOVE_ENTRY, section, key, null, null, null, null);
	}
	
	/**
	 * Adds a new empty section to the config.
	 * 
	 * @param section
	 * 	The section name.
	 * @param prelines 
	 * 	Comment lines that occur before this section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent setSection(String section, List<String> prelines) {
		return new ConfigEvent(SET_SECTION, section, null, null, null, null, prelines);
	}
	
	/**
	 * Removes a section from the config.
	 * 
	 * @param section
	 * 	The section name.
	 * @return
	 * 	A new {@link ConfigEvent} object.
	 */
	public static ConfigEvent removeSection(String section) {
		return new ConfigEvent(REMOVE_SECTION, section, null, null, null, null, null);
	}
	
	
	//---------------------------------------------------------------------------------------------
	// Instance methods.
	//---------------------------------------------------------------------------------------------
	
	/**
	 * Returns the event type.
	 *
	 * @return The event type.
	 */
	public ConfigEventType getType() {
		return type;
	}

	/**
	 * Returns the section name.
	 *
	 * @return The section name.
	 */
	public String getSection() {
		return section;
	}

	/**
	 * Returns the entry name.
	 *
	 * @return The entry name.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Returns the entry value.
	 *
	 * @return The entry value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the entry comment.
	 *
	 * @return The entry comment.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns the section or entry lines.
	 *
	 * @return The section or entry lines.
	 */
	public List<String> getPreLines() {
		return preLines;
	}

	/**
	 * Returns the modifiers on this entry.
	 * 
	 * @return 
	 * 	The modifier characters.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getModifiers() {
		return modifiers;
	}
	
	@Override /* Object */
	public String toString() {
		switch(type) {
			case REMOVE_SECTION:
				return "REMOVE_SECTION("+section+")";
			case REMOVE_ENTRY:
				return "REMOVE_ENTRY("+section+(section.isEmpty() ? "" : "/")+key+")";
			case SET_SECTION:
				return "SET_SECTION("+section+", preLines="+StringUtils.join(preLines, '|')+")";
			case SET_ENTRY:
				StringBuilder out = new StringBuilder("SET(");
				out.append(key);
				if (modifiers != null)
					out.append(modifiers);
				out.append(" = ");
				String val = value == null ? "null" : value;
				if (val.indexOf('\n') != -1)
					val = val.replaceAll("(\\r?\\n)", "$1\t");
				if (val.indexOf('#') != -1)
					val = val.replaceAll("#", "\\\\#");
				out.append(val);
				if (! StringUtils.isEmpty(comment)) 
					out.append(" # ").append(comment);
				out.append(')');
				return out.toString();
			default:
				return null;
		}
	}
}
