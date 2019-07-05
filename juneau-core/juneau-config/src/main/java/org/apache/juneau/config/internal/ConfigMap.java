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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.config.event.ConfigEventType.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.internal.*;

/**
 * Represents the parsed contents of a configuration.
 */
public class ConfigMap implements ConfigStoreListener {

	private final ConfigStore store;         // The store that created this object.
	private volatile String contents;        // The original contents of this object.
	final String name;                       // The name  of this object.

	private final static AsciiSet MOD_CHARS = AsciiSet.create("#$%&*+^@~");

	// Changes that have been applied since the last load.
	private final List<ConfigEvent> changes = Collections.synchronizedList(new ConfigEvents());

	// Registered listeners listening for changes during saves or reloads.
	private final Set<ConfigEventListener> listeners = Collections.synchronizedSet(new HashSet<ConfigEventListener>());

	// The parsed entries of this map with all changes applied.
	final Map<String,ConfigSection> entries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigSection>());

	// The original entries of this map before any changes were applied.
	final Map<String,ConfigSection> oentries = Collections.synchronizedMap(new LinkedHashMap<String,ConfigSection>());

	// Import statements in this config.
	final List<Import> imports = new CopyOnWriteArrayList<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Constructor.
	 *
	 * @param store The config store.
	 * @param name The config name.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ConfigMap(ConfigStore store, String name) throws IOException {
		this.store = store;
		this.name = name;
		load(store.read(name));
	}

	ConfigMap(ConfigStore store, String name, String contents) throws IOException {
		this.store = store;
		this.name = name;
		load(contents);
	}

	private ConfigMap load(String contents) throws IOException {
		if (contents == null)
			contents = "";
		this.contents = contents;

		entries.clear();
		oentries.clear();
		for (Import ir : imports)
			ir.unregisterAll();
		imports.clear();

		Map<String,ConfigMap> imports = new LinkedHashMap<>();

		List<String> lines = new LinkedList<>();
		try (Scanner scanner = new Scanner(contents)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				char c = firstChar(line);
				int c2 = StringUtils.lastNonWhitespaceChar(line);
				if (c == '[') {
					String l = line.trim();
					if (c2 != ']' || ! isValidNewSectionName(l.substring(1, l.length()-1)))
						throw new ConfigException("Invalid section name found in configuration:  {0}", line);
				} else if (c == '<') {
					String l = line.trim();
					int i = l.indexOf('>');
					if (i != -1) {
						String l2 = l.substring(1, i);
						if (! isValidConfigName(l2))
							throw new ConfigException("Invalid import config name found in configuration:  {0}", line);
						String l3 = l.substring(i+1);
						if (! (isEmpty(l3) || firstChar(l3) == '#'))
							throw new ConfigException("Invalid import config name found in configuration:  {0}", line);
						String importName = l2.trim();
						try {
							if (! imports.containsKey(importName))
								imports.put(importName, store.getMap(importName));
						} catch (StackOverflowError e) {
							throw new IOException("Import loop detected in configuration '"+name+"'->'"+importName+"'");
						}
					}
				}
				lines.add(line);
			}
		}

		List<Import> irl = new ArrayList<>(imports.size());
		for (ConfigMap ic : iterable(imports.values(), true))
			irl.add(new Import(ic).register(listeners));
		this.imports.addAll(irl);

		// Add [blank] section.
		boolean inserted = false;
		boolean foundComment = false;
		for (ListIterator<String> li = lines.listIterator(); li.hasNext();) {
			String l = li.next();
			char c = firstNonWhitespaceChar(l);
			if (c != '#') {
				if (c == 0 && foundComment) {
					li.set("[]");
					inserted = true;
				}
				break;
			}
			foundComment = true;
		}
		if (! inserted)
			lines.add(0, "[]");

		// Collapse any multi-lines.
		ListIterator<String> li = lines.listIterator(lines.size());
		String accumulator = null;
		while (li.hasPrevious()) {
			String l = li.previous();
			char c = firstChar(l);
			if (c == '\t') {
				c = firstNonWhitespaceChar(l);
				if (c != '#') {
					if (accumulator == null)
						accumulator = l.substring(1);
					else
						accumulator = l.substring(1) + "\n" + accumulator;
					li.remove();
				}
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
			char c = firstChar(l);

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
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @param key
	 * 	The entry key.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The entry, or <jk>null</jk> if the entry doesn't exist.
	 */
	public ConfigEntry getEntry(String section, String key) {
		checkSectionName(section);
		checkKeyName(key);
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			ConfigEntry ce = cs == null ? null : cs.entries.get(key);

			if (ce == null) {
				for (Import i : imports) {
					ce = i.getConfigMap().getEntry(section, key);
					if (ce != null)
						break;
				}
			}

			return ce;
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
	 * 	<br>Use blank to refer to the default section.
	 * @return
	 * 	An unmodifiable list of lines, or <jk>null</jk> if the section doesn't exist.
	 */
	public List<String> getPreLines(String section) {
		checkSectionName(section);
		readLock();
		try {
			ConfigSection cs = entries.get(section);
			return cs == null ? null : cs.preLines;
		} finally {
			readUnlock();
		}
	}

	/**
	 * Returns the keys of the entries in the specified section.
	 *
	 * @return
	 * 	An unmodifiable set of keys.
	 */
	public Set<String> getSections() {
		Set<String> s = null;
		if (imports.isEmpty()) {
			s = entries.keySet();
		} else {
			s = new LinkedHashSet<>();
			for (Import ir : imports)
				s.addAll(ir.getConfigMap().getSections());
			s.addAll(entries.keySet());
		}
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Returns the keys of the entries in the specified section.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @return
	 * 	An unmodifiable set of keys, or an empty set if the section doesn't exist.
	 */
	public Set<String> getKeys(String section) {
		checkSectionName(section);
		Set<String> s = null;
		ConfigSection cs = entries.get(section);
		if (imports.isEmpty()) {
			s = cs == null ? Collections.<String>emptySet() : cs.entries.keySet();
		} else {
			s = new LinkedHashSet<>();
			for (Import i : imports)
				s.addAll(i.getConfigMap().getKeys(section));
			if (cs != null)
				s.addAll(cs.entries.keySet());
		}
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Returns <jk>true</jk> if this config has the specified section.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @return <jk>true</jk> if this config has the specified section.
	 */
	public boolean hasSection(String section) {
		checkSectionName(section);
		for (Import i : imports)
			if (i.getConfigMap().hasSection(section))
				return true;
		return entries.get(section) != null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Setters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a new section or replaces the pre-lines on an existing section.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @param preLines
	 * 	The pre-lines on the section.
	 * 	<br>If <jk>null</jk>, the previous value will not be overwritten.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setSection(String section, List<String> preLines) {
		checkSectionName(section);
		return applyChange(true, ConfigEvent.setSection(name, section, preLines));
	}

	/**
	 * Adds or overwrites an existing entry.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @param key
	 * 	The entry key.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param value
	 * 	The entry value.
	 * 	<br>If <jk>null</jk>, the previous value will not be overwritten.
	 * @param modifiers
	 * 	Optional modifiers.
	 * 	<br>If <jk>null</jk>, the previous value will not be overwritten.
	 * @param comment
	 * 	Optional comment.
	 * 	<br>If <jk>null</jk>, the previous value will not be overwritten.
	 * @param preLines
	 * 	Optional pre-lines.
	 * 	<br>If <jk>null</jk>, the previous value will not be overwritten.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setEntry(String section, String key, String value, String modifiers, String comment, List<String> preLines) {
		checkSectionName(section);
		checkKeyName(key);
		if (modifiers != null && ! MOD_CHARS.containsOnly(modifiers))
			throw new ConfigException("Invalid modifiers: {0}", modifiers);
		return applyChange(true, ConfigEvent.setEntry(name, section, key, value, modifiers, comment, preLines));
	}


	/**
	 * Not implemented.
	 *
	 * @param section
	 * 	The section name where to place the import statement.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param importName
	 * 	The import name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param preLines
	 * 	Optional comment and blank lines to add immediately before the import statement.
	 * 	<br>If <jk>null</jk>, previous pre-lines will not be replaced.
	 * @return This object (for method chaining).
	 */
	public ConfigMap setImport(String section, String importName, List<String> preLines) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Removes a section.
	 *
	 * <p>
	 * This eliminates all entries in the section as well.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @return This object (for method chaining).
	 */
	public ConfigMap removeSection(String section) {
		checkSectionName(section);
		return applyChange(true, ConfigEvent.removeSection(name, section));
	}

	/**
	 * Removes an entry.
	 *
	 * @param section
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank to refer to the default section.
	 * @param key
	 * 	The entry key.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap removeEntry(String section, String key) {
		checkSectionName(section);
		checkKeyName(key);
		return applyChange(true, ConfigEvent.removeEntry(name, section, key));
	}

	/**
	 * Not implemented.
	 *
	 * @param section
	 * 	The section name where to place the import statement.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param importName
	 * 	The import name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigMap removeImport(String section, String importName) {
		throw new UnsupportedOperationException();
	}

	private ConfigMap applyChange(boolean addToChangeList, ConfigEvent ce) {
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
				ConfigEntry oe = cs.entries.get(ce.getKey());
				if (oe == null)
					oe = ConfigEntry.NULL;
				cs.addEntry(
					ce.getKey(),
					ce.getValue() == null ? oe.value : ce.getValue(),
					ce.getModifiers() == null ? oe.modifiers : ce.getModifiers(),
					ce.getComment() == null ? oe.comment : ce.getComment(),
					ce.getPreLines() == null ? oe.preLines : ce.getPreLines()
				);
			} else if (ce.getType() == SET_SECTION) {
				if (cs == null) {
					cs = new ConfigSection(section);
					entries.put(section, cs);
				}
				if (ce.getPreLines() != null)
					cs.setPreLines(ce.getPreLines());
			} else if (ce.getType() == REMOVE_ENTRY) {
				if (cs != null)
					cs.entries.remove(ce.getKey());
			} else if (ce.getType() == REMOVE_SECTION) {
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

	/**
	 * Overwrites the contents of the config file.
	 *
	 * @param contents The new contents of the config file.
	 * @param synchronous Wait until the change has been persisted before returning this map.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 * @throws InterruptedException Thread was interrupted.
	 */
	public ConfigMap load(String contents, boolean synchronous) throws IOException, InterruptedException {

		if (synchronous) {
			final CountDownLatch latch = new CountDownLatch(1);
			ConfigStoreListener l = new ConfigStoreListener() {
				@Override
				public void onChange(String contents) {
					latch.countDown();
				}
			};
			store.register(name, l);
			store.write(name, null, contents);
			latch.await(30, TimeUnit.SECONDS);
			store.unregister(name, l);
		} else {
			store.write(name, null, contents);
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
	 * @throws IOException Thrown by underlying stream.
	 */
	public ConfigMap commit() throws IOException {
		writeLock();
		try {
			String newContents = asString();
			for (int i = 0; i <= 10; i++) {
				if (i == 10)
					throw new ConfigException("Unable to store contents of config to store.");
				String currentContents = store.write(name, contents, newContents);
				if (currentContents == null)
					break;
				onChange(currentContents);
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
	public ConfigMap register(ConfigEventListener listener) {
		listeners.add(listener);
		for (Import ir : imports)
			ir.register(listener);
		return this;
	}

	boolean hasEntry(String section, String key) {
		ConfigSection cs = entries.get(section);
		ConfigEntry ce = cs == null ? null : cs.entries.get(key);
		return ce != null;
	}

	/**
	 * Unregisters an event listener from this map.
	 *
	 * @param listener The listener to remove.
	 * @return This object (for method chaining).
	 */
	public ConfigMap unregister(ConfigEventListener listener) {
		listeners.remove(listener);
		for (Import ir : imports)
			ir.register(listener);
		return this;
	}

	/**
	 * Returns the listeners currently associated with this config map.
	 *
	 * @return The listeners currently associated with this config map.
	 */
	public Set<ConfigEventListener> getListeners() {
		return Collections.unmodifiableSet(listeners);
	}

	@Override /* ConfigStoreListener */
	public void onChange(String newContents) {
		ConfigEvents changes = null;
		writeLock();
		try {
			if (! StringUtils.isEquals(contents, newContents)) {
				changes = findDiffs(newContents);
				load(newContents);

				// Reapply our changes on top of the modifications.
				for (ConfigEvent ce : this.changes)
					applyChange(false, ce);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			writeUnlock();
		}
		if (changes != null && ! changes.isEmpty())
			signal(changes);
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

	/**
	 * Returns the values in this config map as a map of maps.
	 *
	 * <p>
	 * This is considered a snapshot copy of the config map.
	 *
	 * <p>
	 * The returned map is modifiable, but modifications to the returned map are not reflected in the config map.
	 *
	 * @return A copy of this config as a map of maps.
	 */
	public ObjectMap asMap() {
		ObjectMap m = new ObjectMap();
		readLock();
		try {
			for (Import i : imports)
				m.putAll(i.getConfigMap().asMap());
			for (ConfigSection cs : entries.values()) {
				Map<String,String> m2 = new LinkedHashMap<>();
				for (ConfigEntry ce : cs.entries.values())
					m2.put(ce.key, ce.value);
				m.put(cs.name, m2);
			}
		} finally {
			readUnlock();
		}
		return m;
	}

	/**
	 * Serializes this map to the specified writer.
	 *
	 * @param w The writer to serialize to.
	 * @return The same writer passed in.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Writer writeTo(Writer w) throws IOException {
		readLock();
		try {
			for (ConfigSection cs : entries.values())
				cs.writeTo(w);
		} finally {
			readUnlock();
		}
		return w;
	}

	/**
	 * Does a rollback of any changes on this map currently in memory.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigMap rollback() {
		if (changes.size() > 0) {
			writeLock();
			try {
				changes.clear();
				load(contents);
			} catch (IOException e) {
				throw new RuntimeException(e);
		 	} finally {
				writeUnlock();
			}
		}
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Private methods
	//-----------------------------------------------------------------------------------------------------------------

	private void readLock() {
		lock.readLock().lock();
	}

	private void readUnlock() {
		lock.readLock().unlock();
	}

	private void writeLock() {
		lock.writeLock().lock();
	}

	private void writeUnlock() {
		lock.writeLock().unlock();
	}

	private void checkSectionName(String s) {
		if (! ("".equals(s) || isValidNewSectionName(s)))
			throw new IllegalArgumentException("Invalid section name: '" + s + "'");
	}

	private void checkKeyName(String s) {
		if (! isValidKeyName(s))
			throw new IllegalArgumentException("Invalid key name: '" + s + "'");
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

	private boolean isValidNewSectionName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/' || c == '\\' || c == '[' || c == ']')
				return false;
		}
		return true;
	}

	private boolean isValidConfigName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0) {
				if (! Character.isJavaIdentifierStart(c))
					return false;
			} else {
				if (! Character.isJavaIdentifierPart(c))
					return false;
			}
		}
		return true;
	}

	private void signal(ConfigEvents changes) {
		if (changes.size() > 0)
			for (ConfigEventListener l : listeners)
				l.onConfigChange(changes);
	}

	private ConfigEvents findDiffs(String updatedContents) throws IOException {
		ConfigEvents changes = new ConfigEvents();
		ConfigMap newMap = new ConfigMap(store, name, updatedContents);

		// Imports added.
		for (Import i : newMap.imports) {
			if (! imports.contains(i)) {
				for (ConfigSection s : i.getConfigMap().entries.values()) {
					for (ConfigEntry e : s.oentries.values()) {
						if (! newMap.hasEntry(s.name, e.key)) {
							changes.add(ConfigEvent.setEntry(name, s.name, e.key, e.value, e.modifiers, e.comment, e.preLines));
						}
					}
				}
			}
		}

		// Imports removed.
		for (Import i : imports) {
			if (! newMap.imports.contains(i)) {
				for (ConfigSection s : i.getConfigMap().entries.values()) {
					for (ConfigEntry e : s.oentries.values()) {
						if (! newMap.hasEntry(s.name, e.key)) {
							changes.add(ConfigEvent.removeEntry(name, s.name, e.key));
						}
					}
				}
			}
		}

		for (ConfigSection ns : newMap.oentries.values()) {
			ConfigSection s = oentries.get(ns.name);
			if (s == null) {
				//changes.add(ConfigEvent.setSection(ns.name, ns.preLines));
				for (ConfigEntry ne : ns.entries.values()) {
					changes.add(ConfigEvent.setEntry(name, ns.name, ne.key, ne.value, ne.modifiers, ne.comment, ne.preLines));
				}
			} else {
				for (ConfigEntry ne : ns.oentries.values()) {
					ConfigEntry e = s.oentries.get(ne.key);
					if (e == null || ! isEquals(e.value, ne.value)) {
						changes.add(ConfigEvent.setEntry(name, s.name, ne.key, ne.value, ne.modifiers, ne.comment, ne.preLines));
					}
				}
				for (ConfigEntry e : s.oentries.values()) {
					ConfigEntry ne = ns.oentries.get(e.key);
					if (ne == null) {
						changes.add(ConfigEvent.removeEntry(name, s.name, e.key));
					}
				}
			}
		}

		for (ConfigSection s : oentries.values()) {
			ConfigSection ns = newMap.oentries.get(s.name);
			if (ns == null) {
				//changes.add(ConfigEvent.removeSection(s.name));
				for (ConfigEntry e : s.oentries.values())
					changes.add(ConfigEvent.removeEntry(name, s.name, e.key));
			}
		}

		return changes;
	}

	// This method should only be called from behind a lock.
	private String asString() {
		try {
			StringWriter sw = new StringWriter();
			for (ConfigSection cs : entries.values())
				cs.writeTo(sw);
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);  // Not possible.
		}
	}


	//---------------------------------------------------------------------------------------------
	// ConfigSection
	//---------------------------------------------------------------------------------------------

	class ConfigSection {

		final String name;   // The config section name, or blank if the default section.  Never null.

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

		Writer writeTo(Writer w) throws IOException {
			for (String s : preLines)
				w.append(s).append('\n');

			if (! name.equals(""))
				w.append(rawLine).append('\n');
			else {
				// Need separation between default prelines and first-entry prelines.
				if (! preLines.isEmpty())
					w.append('\n');
			}

			for (ConfigEntry e : entries.values())
				e.writeTo(w);

			return w;
		}
	}


	//---------------------------------------------------------------------------------------------
	// Import
	//---------------------------------------------------------------------------------------------

	class Import {

		private final ConfigMap configMap;
		private final Map<ConfigEventListener,ConfigEventListener> listenerMap = Collections.synchronizedMap(new LinkedHashMap<>());

		Import(ConfigMap configMap) {
			this.configMap = configMap;
		}

		synchronized Import register(Collection<ConfigEventListener> listeners) {
			for (ConfigEventListener l : listeners)
				register(l);
			return this;
		}

		synchronized Import register(final ConfigEventListener listener) {
			ConfigEventListener l2 = new ConfigEventListener() {
				@Override
				public void onConfigChange(ConfigEvents events) {
					ConfigEvents events2 = new ConfigEvents();
					for (ConfigEvent cev : events) {
						if (! hasEntry(cev.getSection(), cev.getKey()))
							events2.add(cev);
					}
					if (events2.size() > 0)
						listener.onConfigChange(events2);
				}
			};
			listenerMap.put(listener, l2);
			configMap.register(l2);
			return this;
		}

		synchronized Import unregister(final ConfigEventListener listener) {
			configMap.unregister(listenerMap.remove(listener));
			return this;
		}

		synchronized Import unregisterAll() {
			for (ConfigEventListener l : listenerMap.values())
				configMap.unregister(l);
			listenerMap.clear();
			return this;
		}

		String getConfigName() {
			return configMap.name;
		}

		ConfigMap getConfigMap() {
			return configMap;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Import) {
				Import ir = (Import)o;
				if (ir.getConfigName().equals(getConfigName()))
					return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return getConfigName().hashCode();
		}
	}
}
