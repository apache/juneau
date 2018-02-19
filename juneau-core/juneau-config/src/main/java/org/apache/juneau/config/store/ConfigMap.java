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
package org.apache.juneau.config.store;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.config.event.ChangeEventType.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.internal.*;

/**
 * Represents the parsed contents of a configuration.
 */
public class ConfigMap implements StoreListener {

	private final Store store;               // The store that created this object.
	private volatile String contents;        // The original contents of this object.
	private final String name;               // The name  of this object.

	private final static AsciiSet MOD_CHARS = new AsciiSet("#$%&*+^@~");
	
	// Changes that have been applied since the last load.
	private final List<ChangeEvent> changes = Collections.synchronizedList(new ArrayList<ChangeEvent>());
	
	// Registered listeners listening for changes during saves or reloads.
	private final Set<ChangeEventListener> listeners = Collections.synchronizedSet(new HashSet<ChangeEventListener>());

	// The parsed entries of this map with all changes applied.
	final Map<String,ConfigSection> entries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigSection>());

	// The original entries of this map before any changes were applied.
	final Map<String,ConfigSection> oentries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigSection>());
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	/**
	 * Constructor.
	 * 
	 * @param store The config store.
	 * @param name The config name.
	 * @throws IOException
	 */
	public ConfigMap(Store store, String name) throws IOException {
		this.store = store;
		this.name = name;
		load(store.read(name));
	}

	ConfigMap(String contents) {
		this.store = null;
		this.name = null;
		load(contents);
	}
	
	private ConfigMap load(String contents) {
		if (contents == null)
			contents = "";
		this.contents = contents;
		
		entries.clear();
		oentries.clear();
		
		List<String> lines = new LinkedList<>();
		try (Scanner scanner = new Scanner(contents)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				char c = StringUtils.firstNonWhitespaceChar(line);
				if (c != 0 || c != '#') {
					if (c == '[') {
						int c2 = StringUtils.lastNonWhitespaceChar(line);
						String l = line.trim();
						if (c2 != ']' || ! isValidNewSectionName(l.substring(1, l.length()-1)))
							throw new ConfigException("Invalid section name found in configuration:  {0}", line) ;
					}
				}
				lines.add(line);
			}
		}
		
		// Add [default] section.
		boolean inserted = false;
		boolean foundComment = false;
		for (ListIterator<String> li = lines.listIterator(); li.hasNext();) {
			String l = li.next();
			char c = StringUtils.firstNonWhitespaceChar(l);
			if (c != '#') {
				if (c == 0 && foundComment) {
					li.set("[default]");
					inserted = true;
				}
				break;
			} 
			foundComment = true;
		}
		if (! inserted)
			lines.add(0, "[default]");
		
		// Collapse any multi-lines.
		ListIterator<String> li = lines.listIterator(lines.size());
		String accumulator = null;
		while (li.hasPrevious()) {
			String l = li.previous();
			char c = l.isEmpty() ? 0 : l.charAt(0);
			if (c == '\t') {
				if (accumulator == null)
					accumulator = l.substring(1);
				else
					accumulator = l.substring(1) + "\n" + accumulator;
				li.remove();
			} else if (accumulator != null) {
				li.set(l + "\n" + accumulator);
				accumulator = null;
			}
		}
		
		lines = new ArrayList<>(lines);
		int last = lines.size()-1;
		int S1 = 1; // Looking for section.
		int S2 = 2; // Found section, looking for start.
		int state = S1;
		
		List<ConfigSection> sections = new ArrayList<>();
		
		for (int i = last; i >= 0; i--) {
			String l = lines.get(i);
			char c = StringUtils.firstNonWhitespaceChar(l);
			
			if (state == S1) {
				if (c == '[') {
					state = S2;
				}
			} else {
				if (c != '#' && (c == '[' || l.indexOf('=') != -1)) {
					sections.add(new ConfigSection(lines.subList(i+1, last+1)));
					last = i + 1;// (c == '[' ? i+1 : i);
					state = (c == '[' ? S2 : S1);
				}
			}
		}

		sections.add(new ConfigSection(lines.subList(0, last+1)));
		
		for (int i = sections.size() - 1; i >= 0; i--) {
			ConfigSection cs = sections.get(i);
			if (entries.containsKey(cs.name))
				throw new ConfigException("Duplicate section found in configuration:  [{0}]", cs.name);
			entries.put(cs.name, cs);
		 }

		oentries.putAll(entries);
		return this;
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// Getters
	//-----------------------------------------------------------------------------------------------------------------
	
	/**
	 * Reads an entry from this map.
	 * 
	 * @param section The section name.
	 * @param key The entry key.
	 * @return The entry, or <jk>null</jk> if the entry doesn't exist.
	 */
	public ConfigEntry getEntry(String section, String key) {
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			return cs == null ? null : cs.entries.get(key);
		} finally {
			readUnlock();
		}
	}

	/**
	 * Returns the pre-lines on the specified section.
	 * 
	 * <p>
	 * The pre-lines are all lines such as blank lines and comments that preceed a section.
	 * 
	 * @param section 
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	An unmodifiable list of lines, or <jk>null</jk> if the section doesn't exist.
	 */
	public List<String> getPreLines(String section) {
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			return cs == null ? null : cs.preLines;
		} finally {
			readUnlock();
		}
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// Setters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a new section or replaces the pre-lines on an existing section.
	 * 
	 * @param section The section name.
	 * @param preLines
	 * 	The pre-lines on the section.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setSection(String section, List<String> preLines) {
		if (! isValidSectionName(section))
			throw new ConfigException("Invalid section name: {0}", section);
		return applyChange(true, ChangeEvent.setSection(section, preLines));
	}
	
	/**
	 * Removes a section.
	 * 
	 * <p>
	 * This eliminates all entries in the section as well.
	 * 
	 * @param section The section name.
	 * @return This object (for method chaining).
	 */
	public ConfigMap removeSection(String section) {
		return applyChange(true, ChangeEvent.removeSection(section));
	}
	
	/**
	 * Sets the pre-lines on an entry without modifying any other attributes.
	 * 
	 * @param section The section name.
	 * @param key The entry key.
	 * @param preLines 
	 * 	The new pre-lines.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setPreLines(String section, String key, List<String> preLines) {
		ChangeEvent cv = null;
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			ConfigEntry ce = (cs  == null ? null : cs.entries.get(key));
			if (ce != null)
				cv = ChangeEvent.setEntry(section, key, ce.value, ce.modifiers, ce.comment, preLines);
		} finally {
			readUnlock();
		}
		return applyChange(true, cv);
	}
	
	/**
	 * Sets the value on an entry without modifying any other attributes.
	 * 
	 * @param section The section name.
	 * @param key The entry key.
	 * @param value 
	 * 	The new value.
	 * 	<br>Can be <jk>null</jk> which will delete the entry.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setValue(String section, String key, String value) {
		
		if (! isValidSectionName(section))
			throw new ConfigException("Invalid section name: {0}", section);
		if (! isValidKeyName(key))
			throw new ConfigException("Invalid key name: {0}", key);

		ChangeEvent cv = null;
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			ConfigEntry ce = (cs  == null ? null : cs.entries.get(key));
			if (ce != null)
				cv = ChangeEvent.setEntry(section, key, value, ce.modifiers, ce.comment, ce.preLines);
			else
				cv = ChangeEvent.setEntry(section, key, value, null, null, null);
		} finally {
			readUnlock();
		}
		return applyChange(true, cv);
	}
	
	/**
	 * Sets the comment on an entry without modifying any other attributes.
	 * 
	 * @param section The section name.
	 * @param key The entry key.
	 * @param comment 
	 * 	The new comment.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setComment(String section, String key, String comment) {
		ChangeEvent cv = null;
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			ConfigEntry ce = (cs  == null ? null : cs.entries.get(key));
			if (ce != null)
				cv = ChangeEvent.setEntry(section, key, ce.value, ce.modifiers, comment, ce.preLines);
		} finally {
			readUnlock();
		}
		return applyChange(true, cv);
	}
	
	/**
	 * Adds or overwrites an existing entry.
	 * 
	 * @param section The section name.
	 * @param key The entry key.
	 * @param value The entry value.
	 * @param modifiers 
	 * 	Optional modifiers.
	 * 	<br>Can be <jk>null</jk>.
	 * @param comment 
	 * 	Optional comment.  
	 * 	<br>Can be <jk>null</jk>.
	 * @param preLines 
	 * 	Optional pre-lines.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setEntry(String section, String key, String value, String modifiers, String comment, List<String> preLines) {
		if (! isValidSectionName(section))
			throw new ConfigException("Invalid section name: {0}", section);
		if (! isValidKeyName(key))
			throw new ConfigException("Invalid key name: {0}", key);
		if (modifiers != null && ! MOD_CHARS.containsOnly(modifiers))
			throw new ConfigException("Invalid modifiers: {0}", modifiers);
		return applyChange(true, ChangeEvent.setEntry(section, key, value, modifiers, comment, preLines));
	}
	
	private ConfigMap applyChange(boolean addToChangeList, ChangeEvent ce) {
		if (ce == null)
			return this;
		writeLock();
		try {
			String section = ce.getSection();
			ConfigSection cs = entries.get(section);
			if (ce.getType() == SET_ENTRY) {
				if (cs == null) {
					cs = new ConfigSection(section);
					entries.put(section, cs);
				}
				cs.addEntry(ce.getKey(), ce.getValue(), ce.getModifiers(), ce.getComment(), ce.getPreLines());
			} else if (ce.getType() == SET_SECTION) {
				if (cs == null) {
					cs = new ConfigSection(section);
					entries.put(section, cs);
				}
				cs.setPreLines(ce.getPreLines());
			} else {
				if (cs != null)
					entries.remove(section);
			}
			if (addToChangeList)
				changes.add(ce);
		} finally {
			writeUnlock();
		}
		return this;	
	}	
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle events
	//-----------------------------------------------------------------------------------------------------------------
	
	/**
	 * Persist any changes made to this map and signal all listeners.
	 * 
	 * <p>
	 * If the underlying contents of the file have changed, this will reload it and apply the changes
	 * on top of the modified file.
	 * 
	 * <p>
	 * Subsequent changes made to the underlying file will also be signaled to all listeners.
	 * 
	 * <p>
	 * We try saving the file up to 10 times.
	 * <br>If the file keeps changing on the file system, we throw an exception.
	 * 
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public ConfigMap save() throws IOException {
		writeLock();
		try {
			String newContents = asString();
			for (int i = 0; i <= 10; i++) {
				if (i == 10)
					throw new ConfigException("Unable to store contents of config to store.");
				String currentContents = store.write(name, contents, newContents);
				if (currentContents == null) 
					break;
				onChange(name, currentContents);
			}
			this.changes.clear();
		} finally {
			writeUnlock();
		}
		return this;
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// Listeners
	//-----------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an event listener on this map.
	 * 
	 * @param listener The new listener.
	 * @return This object (for method chaining).
	 */
	public ConfigMap registerListener(ChangeEventListener listener) {
		listeners.add(listener);
		return this;
	}
	
	/**
	 * Unregisters an event listener from this map.
	 * 
	 * @param listener The listener to remove.
	 * @return This object (for method chaining).
	 */
	public ConfigMap unregisterListener(ChangeEventListener listener) {
		listeners.remove(listener);
		return this;
	}
	
	@Override /* StoreListener */
	public void onChange(String name, String newContents) {
		List<ChangeEvent> changes = null;
		writeLock();
		try {
			if (! StringUtils.isEquals(contents, newContents)) {
				changes = findDiffs(newContents);
				load(newContents);
				
				// Reapply our changes on top of the modifications.
				for (ChangeEvent ce : this.changes)
					applyChange(false, ce);
			}
		} finally {
			writeUnlock();
		}
		if (changes != null && ! changes.isEmpty())
			signal(changes);
	}
	
	private void signal(List<ChangeEvent> changes) {
		for (ChangeEventListener l : listeners)
			l.onEvents(changes);
	}

	private List<ChangeEvent> findDiffs(String updatedContents) {
		List<ChangeEvent> changes = new ArrayList<>();
		ConfigMap newMap = new ConfigMap(updatedContents);
		for (ConfigSection ns : newMap.oentries.values()) {
			ConfigSection s = oentries.get(ns.name);
			if (s == null) {
				//changes.add(ChangeEvent.setSection(ns.name, ns.preLines));
				for (ConfigEntry ne : ns.entries.values()) {
					changes.add(ChangeEvent.setEntry(ns.name, ne.key, ne.value, ne.modifiers, ne.comment, ne.preLines));
				}
			} else {
				for (ConfigEntry ne : ns.oentries.values()) {
					ConfigEntry e = s.oentries.get(ne.key);
					if (e == null || ! isEquals(e.value, ne.value)) {
						changes.add(ChangeEvent.setEntry(s.name, ne.key, ne.value, ne.modifiers, ne.comment, ne.preLines));
					}
				}
				for (ConfigEntry e : s.oentries.values()) {
					ConfigEntry ne = ns.oentries.get(e.key);
					if (ne == null) {
						changes.add(ChangeEvent.removeEntry(s.name, e.key));
					}
				}
			}
		}
		for (ConfigSection s : oentries.values()) {
			ConfigSection ns = newMap.oentries.get(s.name);
			if (ns == null) {
				//changes.add(ChangeEvent.removeSection(s.name));
				for (ConfigEntry e : s.oentries.values())
					changes.add(ChangeEvent.removeEntry(s.name, e.key));
			}
		}
		return changes;
	}

	
	//---------------------------------------------------------------------------------------------
	// ConfigSection
	//---------------------------------------------------------------------------------------------
	
	class ConfigSection {

		final String name;   // The config section name, or "default" if the default section.  Never null.

		final List<String> preLines = Collections.synchronizedList(new ArrayList<String>());
		private final String rawLine;
		
		final Map<String,ConfigEntry> oentries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigEntry>());
		final Map<String,ConfigEntry> entries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigEntry>());

		/**
		 * Constructor.
		 */
		ConfigSection(String name) {
			this.name = name;
			this.rawLine = "[" + name + "]";
		}
		
		/**
		 * Constructor.
		 */
		ConfigSection(List<String> lines) {
			
			String name = null, rawLine = null;
			
			int S1 = 1; // Looking for section.
			int S2 = 2; // Found section, looking for end.
			int state = S1;
			int start = 0;
			
			for (int i = 0; i < lines.size(); i++) {
				String l = lines.get(i);
				char c = StringUtils.firstNonWhitespaceChar(l);
				if (state == S1) {
					if (c == '[') {
						int i1 = l.indexOf('['), i2 = l.indexOf(']');
						name = l.substring(i1+1, i2).trim();
						rawLine = l;
						state = S2;
						start = i+1;
					} else {
						preLines.add(l);
					}
				} else {
					if (c != '#' && l.indexOf('=') != -1) {
						ConfigEntry e = new ConfigEntry(l, lines.subList(start, i));
						if (entries.containsKey(e.key))
							throw new ConfigException("Duplicate entry found in section [{0}] of configuration:  {1}", name, e.key);
						entries.put(e.key, e);
						start = i+1;
					} 
				}
			}
			
			this.name = name;
			this.rawLine = rawLine;
			this.oentries.putAll(entries);
		}
		
		ConfigSection addEntry(String key, String value, String modifiers, String comment, List<String> preLines) {
			ConfigEntry e = new ConfigEntry(key, value, modifiers, comment, preLines);
			this.entries.put(e.key, e);
			return this;
		}

		ConfigSection setPreLines(List<String> preLines) {
			this.preLines.clear();
			this.preLines.addAll(preLines);
			return this;
		}
		
		Writer writeTo(Writer out) throws IOException {
			for (String s : preLines)
				out.append(s).append('\n');
			
			if (! name.equals("default"))
				out.append(rawLine).append('\n');
			else {
				// Need separation between default prelines and first-entry prelines.
				if (! preLines.isEmpty())
					out.append('\n');
			}

			for (ConfigEntry e : entries.values()) 
				e.writeTo(out);
			
			return out;
		}
	}

	@Override /* Object */
	public String toString() {
		readLock();
		try {
			return asString();
		} finally {
			readUnlock();
		}
	}

	String asString() {
		try {
			StringWriter sw = new StringWriter();
			for (ConfigSection cs : entries.values())
				cs.writeTo(sw);
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);  // Not possible.
		}
	}
	
	private boolean isValidNewSectionName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		if ("default".equals(s))
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/' || c == '\\' || c == '[' || c == ']')
				return false;
		}
		return true;
	}

	private boolean isValidSectionName(String s) {
		return "default".equals(s) || isValidNewSectionName(s);
	}
	
	private boolean isValidKeyName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/' || c == '\\' || c == '[' || c == ']' || c == '=' || c == '#')
				return false;
		}
		return true;
	}

	//--------------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------------

	void readLock() {
		lock.readLock().lock();
	}

	void readUnlock() {
		lock.readLock().unlock();
	}

	void writeLock() {
		lock.writeLock().lock();
	}

	void writeUnlock() {
		lock.writeLock().unlock();
	}
}
