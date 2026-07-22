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

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.format.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;

/**
 * Represents the parsed contents of a configuration.
 *
 * <h5 class='section'>Concurrency:</h5>
 * <p>
 * The parsed contents are held in a single immutable {@link State} snapshot published through a {@code volatile}
 * reference.  Reads ({@link #getEntry(String,String)}, {@link #getKeys(String)}, {@link #getSections()},
 * {@link #asMap()}, {@link #toString()}, etc.) are <b>lock-free</b>: they take a single snapshot of the current state
 * and operate entirely on immutable collections, so there is no live-view leak and no
 * {@link ConcurrentModificationException} hazard.  Writes ({@link #setEntry(String,String,String,String,String,List)},
 * {@link #commit()}, {@link #onChange(String)}, etc.) build a fresh immutable state and swap it under a write lock,
 * which is still required to make read-modify-write sequences atomic.
 */
@SuppressWarnings({
	"resource", // Complex nested structure; value equality not practical
	"java:S1206", // equals/hashCode not overridden; value equality not practical for this class
})
public class ConfigMap implements ConfigStoreListener {

	static final class ConfigSection {

		final String name;   // The config section name, or blank if the default section.  Never null.

		final List<String> preLines;
		final String rawLine;

		final Map<String,ConfigMapEntry> oentries;
		final Map<String,ConfigMapEntry> entries;

		private ConfigSection(String name, String rawLine, List<String> preLines, Map<String,ConfigMapEntry> oentries, Map<String,ConfigMapEntry> entries) {
			this.name = name;
			this.rawLine = rawLine;
			this.preLines = preLines;
			this.oentries = oentries;
			this.entries = entries;
		}

		/**
		 * Constructor for an empty (new) section.
		 */
		ConfigSection(String name) {
			this(name, "[" + name + "]", List.of(), Map.of(), Map.of());
		}

		/**
		 * Parses a section from a block of raw lines.
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for config section parsing
		})
		static ConfigSection parse(List<String> lines) {

			String name2 = null;
			String rawLine2 = null;

			var preLines = new ArrayList<String>();
			var entries = new LinkedHashMap<String,ConfigMapEntry>();

			// S1: Looking for section.
			// S2: Found section, looking for end.

			var state = S1;
			var start = 0;

			for (var i = 0; i < lines.size(); i++) {
				var l = lines.get(i);
				var c = StringUtils.firstNonWhitespaceChar(l);
				if (state == S1) {
					if (c == '[') {
						var i1 = l.indexOf('[');
						var i2 = l.indexOf(']');
						name2 = l.substring(i1 + 1, i2).trim();
						rawLine2 = l;
						state = S2;
						start = i + 1;
					} else {
						preLines.add(l);
					}
				} else {
					if (c != '#' && l.indexOf('=') != -1) {
						var e = ConfigMapEntry.parse(l, lines.subList(start, i));
						if (entries.containsKey(e.key()))
							throw new ConfigException("Duplicate entry found in section [%s] of configuration:  %s", name2, e.key());
						entries.put(e.key(), e);
						start = i + 1;
					}
				}
			}

			// oentries and entries start identical; the immutable snapshot is shared safely.
			var entriesU = u(entries);
			return new ConfigSection(name2, rawLine2, u(preLines), entriesU, entriesU);
		}

		/**
		 * Returns a copy of this section with the specified entry added or replaced.
		 */
		ConfigSection withEntry(ConfigMapEntry e) {
			var m = new LinkedHashMap<>(entries);
			m.put(e.key(), e);
			return new ConfigSection(name, rawLine, preLines, oentries, u(m));
		}

		/**
		 * Returns a copy of this section with the specified entry removed.
		 */
		ConfigSection withoutEntry(String key) {
			if (! entries.containsKey(key))
				return this;
			var m = new LinkedHashMap<>(entries);
			m.remove(key);
			return new ConfigSection(name, rawLine, preLines, oentries, u(m));
		}

		/**
		 * Returns a copy of this section with the specified pre-lines.
		 */
		ConfigSection withPreLines(List<String> preLines) {
			return new ConfigSection(name, rawLine, u(cp(preLines)), oentries, entries);
		}

		Writer writeTo(Writer w) throws IOException {
			for (var s : preLines)
				w.append(s).append('\n');

			if (! name.isEmpty())
				w.append(rawLine).append('\n');
			else {
				// Need separation between default prelines and first-entry prelines.
				if (! preLines.isEmpty())
					w.append('\n');
			}

			for (var e : entries.values())
				e.writeTo(w);

			return w;
		}
	}

	class Import {

		private final ConfigMap configMap;
		private final Map<ConfigEventListener,ConfigEventListener> listenerMap = synced(map());

		Import(ConfigMap configMap) {
			this.configMap = configMap;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Import ir && ir.getConfigName().equals(getConfigName()); // HTT - Import.equals() only called via imports.contains() in findDiffs, which requires active import listeners
		}

		@Override
		public int hashCode() {
			return getConfigName().hashCode(); // HTT - Import.hashCode() only called when Import is used as map key, which requires active import functionality
		}

		ConfigMap getConfigMap() { return configMap; } // HTT - only called from import-related paths requiring active import listeners

		String getConfigName() { return configMap.name; } // HTT - only called from import-related paths requiring active import listeners

		synchronized Import register(Collection<ConfigEventListener> listeners) {
			listeners.forEach(this::register);
			return this;
		}

		synchronized Import register(ConfigEventListener listener) { // HTT - only called when registering with non-empty listener collection
			var l2 = (ConfigEventListener)events -> {
				var events2 = events.stream().filter(x -> ! hasEntry(x.getSection(), x.getKey())).collect(Collectors.toCollection(ConfigEvents::new));
				if (! events2.isEmpty())
					listener.onConfigChange(events2);
			};
			listenerMap.put(listener, l2);
			configMap.register(l2);
			return this;
		}

		synchronized Import unregister(ConfigEventListener listener) { // HTT - only called when import listener unregistration is triggered via unsupported import functionality
			configMap.unregister(listenerMap.remove(listener));
			return this;
		}

		synchronized Import unregisterAll() {
			listenerMap.values().forEach(configMap::unregister);
			listenerMap.clear();
			return this;
		}
	}

	/**
	 * Immutable snapshot of the parsed contents of a config.
	 *
	 * <p>
	 * All collections held here are unmodifiable.  A new instance is built for every mutation and published atomically
	 * through the {@code volatile} {@link ConfigMap#state} reference.
	 */
	private record State(
		String contents,
		List<ConfigEvent> changes,
		Map<String,ConfigSection> entries,
		Map<String,ConfigSection> oentries,
		List<Import> imports
	) {
		State withChanges(List<ConfigEvent> changes) {
			return new State(contents, changes, entries, oentries, imports);
		}

		State withImports(List<Import> imports) {
			return new State(contents, changes, entries, oentries, imports);
		}
	}

	private static void checkKeyName(String s) {
		if (! isValidKeyName(s))
			throw iaex("Invalid key name: '%s'", s);
	}

	private static void checkSectionName(String s) {
		if (! ("".equals(s) || isValidNewSectionName(s)))
			throw iaex("Invalid section name: '%s'", s);
	}

	private static boolean isValidConfigName(String s) {
		s = s.trim();
		if (s.isEmpty())
			return false;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
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

	private static boolean isValidKeyName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '/' || c == '\\' || c == '[' || c == ']' || c == '=' || c == '#')
				return false;
		}
		return true;
	}

	private static boolean isValidNewSectionName(String s) {
		if (s == null)
			return false;
		s = s.trim();
		if (s.isEmpty())
			return false;
		if (s.startsWith("/") || s.endsWith("/") || s.contains("//"))
			return false;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\\' || c == '[' || c == ']')
				return false;
		}
		return true;
	}

	private final ConfigStore store;         // The store that created this object.
	private final ConfigFormat format;       // Persistence format strategy.

	final String name;                       // The name  of this object.

	// Registered listeners listening for changes during saves or reloads.
	private final Set<ConfigEventListener> listeners = new CopyOnWriteArraySet<>();

	// The immutable copy-on-write snapshot of this map's contents.  Published atomically; read lock-free.
	@SuppressWarnings("java:S3077") // Copy-on-write snapshot: State is a deeply-immutable graph (unmodifiable collections of immutable ConfigSection/ConfigMapEntry records) replaced wholesale under writeLock and never compound-mutated, so volatile safe-publication is sufficient for lock-free reads.
	private volatile State state = new State("", List.of(), Map.of(), Map.of(), List.of());

	private final SimpleReadWriteLock lock = new SimpleReadWriteLock();

	/**
	 * Constructor.
	 *
	 * @param store The config store.
	 * @param name The config name.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ConfigMap(ConfigStore store, String name) throws IOException {
		this(store, name, IniConfigFormat.INSTANCE);
	}

	public ConfigMap(ConfigStore store, String name, ConfigFormat format) throws IOException {
		this.store = store;
		this.name = name;
		this.format = format == null ? IniConfigFormat.INSTANCE : format;
		load(store.read(name));
	}

	ConfigMap(ConfigStore store, String name, String contents) throws IOException {
		this(store, name, contents, IniConfigFormat.INSTANCE);
	}

	ConfigMap(ConfigStore store, String name, String contents, ConfigFormat format) throws IOException {
		this.store = store;
		this.name = name;
		this.format = format == null ? IniConfigFormat.INSTANCE : format;
		load(contents);
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
	public JsonMap asMap() {
		var st = state;
		var m = new JsonMap();
		st.imports().forEach(y -> m.putAll(y.getConfigMap().asMap()));
		st.entries().values().forEach(z -> {
			var m2 = mapOfType(String.class, String.class);
			z.entries.values().forEach(y -> m2.put(y.key(), y.value()));
			m.put(z.name, m2);
		});
		return m;
	}

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
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ConfigMap commit() throws IOException {
		try (var x = lock.write()) {
			var newContents = asString();
			for (var i = 0; i <= 10; i++) {
				if (i == 10)
					throw new ConfigException("Unable to store contents of config to store.");
			var currentContents = store.write(name, state.contents(), newContents);
			if (currentContents == null)
				break;
			onChange(currentContents); // HTT - retry loop requires concurrent write conflict causing store.write() to return non-null
			}
			state = state.withChanges(List.of());
		}
		return this;
	}

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
	public ConfigMapEntry getEntry(String section, String key) {
		checkSectionName(section);
		checkKeyName(key);
		var st = state;
		var cs = st.entries().get(section);
		var ce = cs == null ? null : cs.entries.get(key);

		if (ce == null)
			ce = st.imports().stream().map(y -> y.getConfigMap().getEntry(section, key)).filter(Shorts::nn).findFirst().orElse(null);

		return ce;
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
		var st = state;
		var cs = st.entries().get(section);
		Set<String> s = st.imports().isEmpty() && nn(cs) ? cs.entries.keySet() : set();
		if (! st.imports().isEmpty()) {
			st.imports().forEach(y -> s.addAll(y.getConfigMap().getKeys(section)));
			if (nn(cs)) // HTT - requires imports where the section doesn't exist locally (cs==null), which needs active import listeners
				s.addAll(cs.entries.keySet());
		}
		// Order-preserving immutable snapshot merging local + imported keys.
		return u(cp(s));
	}

	/**
	 * Returns the listeners currently associated with this config map.
	 *
	 * @return The listeners currently associated with this config map.
	 */
	public Set<ConfigEventListener> getListeners() { return u(cp(listeners)); }

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
		var cs = state.entries().get(section);
		return cs == null ? null : cs.preLines;
	}

	/**
	 * Returns the keys of the entries in the specified section.
	 *
	 * @return
	 * 	An unmodifiable set of keys.
	 */
	public Set<String> getSections() {
		var st = state;
		var s = st.imports().isEmpty() ? st.entries().keySet() : setOfType(String.class);
		if (! st.imports().isEmpty()) {
			st.imports().forEach(y -> s.addAll(y.getConfigMap().getSections()));
			s.addAll(st.entries().keySet());
		}
		// Order-preserving immutable snapshot merging local + imported sections.
		return u(cp(s));
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
		var st = state;
		return nn(st.entries().get(section)) || st.imports().stream().anyMatch(x -> x.getConfigMap().hasSection(section)); // HTT - anyMatch() on imports requires active import listeners
	}

	/**
	 * Overwrites the contents of the config file.
	 *
	 * @param contents The new contents of the config file.
	 * @param synchronous Wait until the change has been persisted before returning this map.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws InterruptedException Thread was interrupted.
	 */
	public ConfigMap load(String contents, boolean synchronous) throws IOException, InterruptedException {

		if (synchronous) {
			final var latch = new CountDownLatch(1);
			var listener = (ConfigStoreListener)x -> latch.countDown();
			store.register(name, listener);
			store.write(name, null, contents);
		if (latch.await(30, TimeUnit.SECONDS)) {
			store.unregister(name, listener);
		} else {
			throw new ConfigException("Unable to store contents of config to store."); // HTT - timeout requires a store that doesn't fire change events within 30 seconds
		}
		} else {
			store.write(name, null, contents);
		}
		return this;
	}

	@Override /* Overridden from ConfigStoreListener */
	public void onChange(String newContents) {
		ConfigEvents changes2 = null;
		try (var x = lock.write()) {
			if (neq(state.contents(), newContents)) {
				changes2 = findDiffs(newContents);
				load(newContents);

				// Reapply our changes on top of the modifications.
				state.changes().forEach(y -> applyChange(false, y));
			}
		} catch (IOException e) {
			throw toRex(e); // HTT - IOException from findDiffs/load requires a failing store operation during onChange
		}
		if (changes2 != null && ! changes2.isEmpty())
			signal(changes2);
	}

	/**
	 * Registers an event listener on this map.
	 *
	 * @param listener The new listener.
	 * @return This object.
	 */
	public ConfigMap register(ConfigEventListener listener) {
		listeners.add(listener);
		state.imports().forEach(x -> x.register(listener));
		return this;
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
	 * @return This object.
	 */
	public ConfigMap removeEntry(String section, String key) {
		checkSectionName(section);
		checkKeyName(key);
		return applyChange(true, ConfigEvent.removeEntry(name, section, key));
	}

	/**
	 * Removes the import statement with the specified name.
	 *
	 * <p>
	 * The imported config is detached from this map: its entries are no longer visible through this map and its
	 * change-propagation listeners are unregistered.  No-op if no matching import exists.
	 *
	 * @param section
	 * 	The section name where the import statement was placed.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param importName
	 * 	The import name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ConfigMap removeImport(String section, String importName) {
		checkSectionName(section);
		try (var x = lock.write()) {
			var st = state;
			st.imports().stream().filter(y -> y.getConfigName().equals(importName)).findFirst().ifPresent(y -> {
				y.unregisterAll();
				var newImports = new ArrayList<>(st.imports());
				newImports.remove(y);
				state = st.withImports(u(newImports));
			});
		}
		return this;
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
	 * @return This object.
	 */
	public ConfigMap removeSection(String section) {
		checkSectionName(section);
		return applyChange(true, ConfigEvent.removeSection(name, section));
	}

	/**
	 * Does a rollback of any changes on this map currently in memory.
	 *
	 * @return This object.
	 */
	public ConfigMap rollback() {
		if (! state.changes().isEmpty()) {
			try (var x = lock.write()) {
				state = state.withChanges(List.of());
				load(state.contents());
		} catch (IOException e) {
			throw toRex(e); // HTT - IOException from private load() is not possible in normal operation (scanner/section parsing doesn't throw)
		}
	}
	return this;
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
	 * @return This object.
	 */
	public ConfigMap setEntry(String section, String key, String value, String modifiers, String comment, List<String> preLines) {
		checkSectionName(section);
		checkKeyName(key);
		return applyChange(true, ConfigEvent.setEntry(name, section, key, value, modifiers, comment, preLines));
	}

	/**
	 * Creates the specified import statement if it doesn't already exist.
	 *
	 * <p>
	 * The named config is resolved through this map's {@link ConfigStore store} and attached to this map so that its
	 * entries become visible through this map and subsequent changes to it are propagated to this map's listeners.
	 * No-op if an import with the same name already exists.
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
	 * @return This object.
	 */
	public ConfigMap setImport(String section, String importName, List<String> preLines) {
		checkSectionName(section);
		if (! isValidConfigName(importName))
			throw iaex("Invalid import config name: '%s'", importName);
		try (var x = lock.write()) {
			var st = state;
			if (st.imports().stream().noneMatch(y -> y.getConfigName().equals(importName))) {
				var newImports = new ArrayList<>(st.imports());
				newImports.add(new Import(store.getMap(importName, format)).register(listeners));
				state = st.withImports(u(newImports));
			}
		} catch (IOException e) {
			throw toRex(e);
		}
		return this;
	}

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
	 * @return This object.
	 */
	public ConfigMap setSection(String section, List<String> preLines) {
		checkSectionName(section);
		return applyChange(true, ConfigEvent.setSection(name, section, preLines));
	}

	@Override /* Overridden from Object */
	public String toString() {
		return asString();
	}

	/**
	 * Unregisters an event listener from this map.
	 *
	 * @param listener The listener to remove.
	 * @return This object.
	 */
	public ConfigMap unregister(ConfigEventListener listener) {
		listeners.remove(listener);
		state.imports().forEach(x -> x.unregister(listener));
		return this;
	}

	/**
	 * Serializes this map to the specified writer.
	 *
	 * @param w
	 * 	The writer to serialize to.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The same writer passed in.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Writer writeTo(Writer w) throws IOException {
		w.append(toString());
		return w;
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for config change application
	})
	private ConfigMap applyChange(boolean addToChangeList, ConfigEvent ce) {
		try (var x = lock.write()) {
			var st = state;
			var section = ce.getSection();
			var cs = st.entries().get(section);
			var newEntries = new LinkedHashMap<>(st.entries());
			switch (ce.getType()) {
				case SET_ENTRY -> {
					var cs2 = cs == null ? new ConfigSection(section) : cs;
					var oe = cs2.entries.get(ce.getKey());
					if (oe == null)
						oe = ConfigMapEntry.NULL;
					// @formatter:off
					cs2 = cs2.withEntry(new ConfigMapEntry(
						ce.getKey(),
						ce.getValue() == null ? oe.value() : ce.getValue(),
						ce.getModifiers() == null ? oe.modifiers() : ce.getModifiers(),
						ce.getComment() == null ? oe.comment() : ce.getComment(),
						ce.getPreLines() == null ? oe.preLines() : ce.getPreLines()
					));
					// @formatter:on
					newEntries.put(section, cs2);
				}
				case SET_SECTION -> {
					var cs2 = cs == null ? new ConfigSection(section) : cs;
					if (nn(ce.getPreLines()))
						cs2 = cs2.withPreLines(ce.getPreLines());
					newEntries.put(section, cs2);
				}
				case REMOVE_ENTRY -> {
					if (nn(cs))
						newEntries.put(section, cs.withoutEntry(ce.getKey()));
				}
				case REMOVE_SECTION -> {
					if (nn(cs))
						newEntries.remove(section);
				}
			}
			List<ConfigEvent> newChanges = st.changes();
			if (addToChangeList) {
				var c = new ArrayList<>(st.changes());
				c.add(ce);
				newChanges = u(c);
			}
			state = new State(st.contents(), newChanges, u(newEntries), st.oentries(), st.imports());
		}
		return this;
	}

	/**
	 * Serializes the current entries snapshot to INI form.
	 *
	 * @return The INI-formatted string.
	 */
	public String asIniString() {
		try {
			var st = state;
			var sw = new StringWriter();
			for (var cs : st.entries().values())
				cs.writeTo(sw);
			return sw.toString();
		} catch (IOException e) {
			throw toRex(e);  // HTT - StringWriter.append() never throws IOException
		}
	}

	// Serializes the current snapshot using the configured format.
	private String asString() {
		try {
			return format.fromInternal(this);
		} catch (IOException e) {
			throw toRex(e); // HTT - in-memory format conversion failures are unexpected in normal operation
		}
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for config diff detection
	})
	private ConfigEvents findDiffs(String updatedContents) throws IOException {
		var changes2 = new ConfigEvents();
		var newMap = new ConfigMap(store, name, updatedContents, format);

		// Imports added.
		for (var i : newMap.state.imports()) { // HTT - requires active import listeners with actual ConfigMap imports registered
			if (! state.imports().contains(i)) {
				for (var s : i.getConfigMap().state.entries().values()) {
					for (var e : s.oentries.values()) {
						if (! newMap.hasEntry(s.name, e.key())) {
							changes2.add(ConfigEvent.setEntry(name, s.name, e.key(), e.value(), e.modifiers(), e.comment(), e.preLines()));
						}
					}
				}
			}
		}

		// Imports removed.
		for (var i : state.imports()) { // HTT - requires active import listeners with actual ConfigMap imports registered
			if (! newMap.state.imports().contains(i)) {
				for (var s : i.getConfigMap().state.entries().values()) {
					for (var e : s.oentries.values()) {
						if (! newMap.hasEntry(s.name, e.key())) {
							changes2.add(ConfigEvent.removeEntry(name, s.name, e.key()));
						}
					}
				}
			}
		}

		for (var ns : newMap.state.oentries().values()) {
			var s = state.oentries().get(ns.name);
			if (s == null) {
				for (var ne : ns.entries.values()) {
					changes2.add(ConfigEvent.setEntry(name, ns.name, ne.key(), ne.value(), ne.modifiers(), ne.comment(), ne.preLines()));
				}
			} else {
				for (var ne : ns.oentries.values()) {
					var e = s.oentries.get(ne.key());
					if (e == null || neq(e.value(), ne.value())) {
						changes2.add(ConfigEvent.setEntry(name, s.name, ne.key(), ne.value(), ne.modifiers(), ne.comment(), ne.preLines()));
					}
				}
				for (var e : s.oentries.values()) {
					var ne = ns.oentries.get(e.key());
					if (ne == null) {
						changes2.add(ConfigEvent.removeEntry(name, s.name, e.key()));
					}
				}
			}
		}

		for (var s : state.oentries().values()) {
			var ns = newMap.state.oentries().get(s.name);
			if (ns == null) {
				for (var e : s.oentries.values())
					changes2.add(ConfigEvent.removeEntry(name, s.name, e.key()));
			}
		}

		return changes2;
	}

	@SuppressWarnings("javabugs:S2259") // False positive: 'format' is a final field assigned a non-null value in every constructor (defaults to IniConfigFormat.INSTANCE); Sonar's symbolic execution can't follow the ternary default across the constructor-to-load() call chain.
	private ConfigMap load(String contents) throws IOException {
		var internalContents = format.toInternal(contents);
		return loadIni(internalContents, contents);
	}

	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541", // Single-threaded context; synchronization unnecessary
	})
	private ConfigMap loadIni(String contents, String originalContents) throws IOException {
		if (contents == null)
			contents = "";
		var newContents = originalContents == null ? "" : originalContents;

		// Detach any imports carried by the previous snapshot before rebuilding.
		state.imports().forEach(Import::unregisterAll);

		var imports2 = mapOfType(String.class, ConfigMap.class);

		List<String> lines = new LinkedList<>();
		try (var scanner = new Scanner(contents)) {
			while (scanner.hasNextLine()) {
				var line = scanner.nextLine();
				var c = firstChar(line);
				var c2 = StringUtils.lastNonWhitespaceChar(line);
				if (c == '[') {
					var l = line.trim();
					if (c2 != ']' || ! isValidNewSectionName(l.substring(1, l.length() - 1)))
						throw new ConfigException("Invalid section name found in configuration:  %s", line);
				} else if (c == '<') {
					var l = line.trim();
					var i = l.indexOf('>');
					if (i != -1) {
						var l2 = l.substring(1, i);
						if (! isValidConfigName(l2))
							throw new ConfigException("Invalid import config name found in configuration:  %s", line);
						var l3 = l.substring(i + 1);
						if (! (isEmpty(l3) || firstChar(l3) == '#'))
							throw new ConfigException("Invalid import config name found in configuration:  %s", line);
						var importName = l2.trim();
						// Circular imports are detected in ConfigStore.getMap() which throws a clean ConfigException.
						if (! imports2.containsKey(importName))
							imports2.put(importName, store.getMap(importName, format));
					}
				}
				lines.add(line);
			}
		}

		List<Import> irl = listOfSize(imports2.size());
		forEachReverse(toList(imports2.values()), x -> irl.add(new Import(x).register(listeners)));

		// Add [blank] section.
		var inserted = false;
		var foundComment = false;
		for (var li = lines.listIterator(); li.hasNext();) {
			var l = li.next();
			var c = firstNonWhitespaceChar(l);
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
		var li = lines.listIterator(lines.size());
		StringBuilder accumulator = null;
		while (li.hasPrevious()) {
			var l = li.previous();
			var c = firstChar(l);
			if (c == '\t') {
				c = firstNonWhitespaceChar(l);
				if (c != '#') {
					if (accumulator == null)
						accumulator = new StringBuilder(l.substring(1));
					else
						accumulator.insert(0, l.substring(1) + "\n");
					li.remove();
				}
			} else if (accumulator != null) {
				li.set(l + "\n" + accumulator.toString());
				accumulator = null;
			}
		}

		lines = cp(lines);
		var last = lines.size() - 1;

		// S1: Looking for section.
		// S2: Found section, looking for start.

		var state2 = S1;

		var sections = listOfType(ConfigSection.class);

		for (var i = last; i >= 0; i--) {
			var l = lines.get(i);
			var c = firstChar(l);

			if (state2 == S1) {
				if (c == '[') {
					state2 = S2;
				}
			} else {
				if (c != '#' && (c == '[' || l.indexOf('=') != -1)) {
					sections.add(ConfigSection.parse(lines.subList(i + 1, last + 1)));
					last = i + 1;
					state2 = (c == '[' ? S2 : S1);
				}
			}
		}

		sections.add(ConfigSection.parse(lines.subList(0, last + 1)));

		var newEntries = new LinkedHashMap<String,ConfigSection>();
		for (var i = sections.size() - 1; i >= 0; i--) {
			var cs = sections.get(i);
			if (newEntries.containsKey(cs.name))
				throw new ConfigException("Duplicate section found in configuration:  [%s]", cs.name);
			newEntries.put(cs.name, cs);
		}

		// entries and oentries start identical; the immutable snapshot is shared safely.  Changes carry over a reload.
		var entriesU = u(newEntries);
		state = new State(newContents, state.changes(), entriesU, entriesU, u(irl));
		return this;
	}

	private void signal(ConfigEvents changes) {
		listeners.forEach(x -> x.onConfigChange(changes));
	}

	boolean hasEntry(String section, String key) {
		var cs = state.entries().get(section);
		var ce = cs == null ? null : cs.entries.get(key);
		return nn(ce);
	}
}
