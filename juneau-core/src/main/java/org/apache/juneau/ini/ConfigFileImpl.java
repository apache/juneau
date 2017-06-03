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
package org.apache.juneau.ini;

import static org.apache.juneau.ini.ConfigUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;

/**
 * Implementation class for {@link ConfigFile}.
 */
public final class ConfigFileImpl extends ConfigFile {

	private final File file;
	private final Encoder encoder;
	private final WriterSerializer serializer;
	private final ReaderParser parser;
	private final BeanSession pBeanSession;
	private final Charset charset;
	final List<ConfigFileListener> listeners = Collections.synchronizedList(new ArrayList<ConfigFileListener>());

	private Map<String,Section> sections;  // The actual data.

	private static final String DEFAULT = "default";

	private final boolean readOnly;

	volatile boolean hasBeenModified = false;
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	long modifiedTimestamp;

	/**
	 * Constructor.
	 * <p>
	 * Loads the contents of the specified file into this config file.
	 * <p>
	 * If file does not initially exist, this object will start off empty.
	 *
	 * @param file The INI file on disk.
	 * If <jk>null</jk>, create an in-memory config file.
	 * @param readOnly Make this configuration file read-only.
	 * Attempting to set any values on this config file will cause {@link UnsupportedOperationException} to be thrown.
	 * @param encoder The encoder to use for encoding sensitive values in this configuration file.
	 * If <jk>null</jk>, defaults to {@link XorEncoder#INSTANCE}.
	 * @param serializer The serializer to use for serializing POJOs in the {@link #put(String, Object)} method.
	 * If <jk>null</jk>, defaults to {@link JsonSerializer#DEFAULT}.
	 * @param parser The parser to use for parsing POJOs in the {@link #getObject(String,Class)} method.
	 * If <jk>null</jk>, defaults to {@link JsonParser#DEFAULT}.
	 * @param charset The charset on the files.
	 * If <jk>null</jk>, defaults to {@link Charset#defaultCharset()}.
	 * @throws IOException
	 */
	public ConfigFileImpl(File file, boolean readOnly, Encoder encoder, WriterSerializer serializer, ReaderParser parser, Charset charset) throws IOException {
		this.file = file;
		this.encoder = encoder == null ? XorEncoder.INSTANCE : encoder;
		this.serializer = serializer == null ? JsonSerializer.DEFAULT : serializer;
		this.parser = parser == null ? JsonParser.DEFAULT : parser;
		this.charset = charset == null ? Charset.defaultCharset() : charset;
		load();
		this.readOnly = readOnly;
		if (readOnly) {
			this.sections = Collections.unmodifiableMap(this.sections);
			for (Section s : sections.values())
				s.setReadOnly();
		}
		this.pBeanSession = this.parser.getBeanContext().createSession();
	}

	/**
	 * Constructor.
	 * Shortcut for calling <code><jk>new</jk> ConfigFileImpl(file, <jk>false</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>);</code>
	 *
	 * @param file The config file.  Does not need to exist.
	 * @throws IOException
	 */
	public ConfigFileImpl(File file) throws IOException {
		this(file, false, null, null, null, null);
	}

	/**
	 * Constructor.
	 * Shortcut for calling <code><jk>new</jk> ConfigFileImpl(<jk>null</jk>, <jk>false</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>);</code>
	 *
	 * @throws IOException
	 */
	public ConfigFileImpl() throws IOException {
		this(null);
	}

	@Override /* ConfigFile */
	public ConfigFileImpl loadIfModified() throws IOException {
		if (file == null)
			return this;
		writeLock();
		try {
			if (file.lastModified() > modifiedTimestamp)
				load();
		} finally {
			writeUnlock();
		}
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl load() throws IOException {
		Reader r = null;
		if (file != null && file.exists())
			r = new InputStreamReader(new FileInputStream(file), charset);
		else
			r = new StringReader("");
		try {
			load(r);
		} finally {
			r.close();
		}
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl load(Reader r) throws IOException {
		assertFieldNotNull(r, "r");
		writeLock();
		try {
			this.sections = Collections.synchronizedMap(new LinkedHashMap<String,Section>());
			BufferedReader in = new BufferedReader(r);
			try {
				writeLock();
				hasBeenModified = false;
				try {
					sections.clear();
					String line = null;
					Section section = getSection(null, true);
					ArrayList<String> lines = new ArrayList<String>();
					boolean canAppend = false;
					while ((line = in.readLine()) != null) {
						if (isSection(line)) {
							section.addLines(null, lines.toArray(new String[lines.size()]));
							lines.clear();
							canAppend = false;
							String sn = replaceUnicodeSequences(line.substring(line.indexOf('[')+1, line.indexOf(']')).trim());
							section = getSection(sn, true).addHeaderComments(section.removeTrailingComments());
						} else {
							char c = line.isEmpty() ? 0 : line.charAt(0);
							if ((c == ' ' || c == '\t') && canAppend && ! (isComment(line) || isAssignment(line)))
								lines.add(lines.remove(lines.size()-1) + '\n' + line.substring(1));
							else {
								lines.add(line);
								if (isAssignment(line))
									canAppend = true;
								else
									canAppend = canAppend && ! (StringUtils.isEmpty(line) || isComment(line));
							}
						}
					}
					section.addLines(null, lines.toArray(new String[lines.size()]));
					in.close();
					if (hasBeenModified)  // Set when values need to be encoded.
						save();
					if (file != null)
						modifiedTimestamp = file.lastModified();
				} finally {
					writeUnlock();
				}
			} finally {
				in.close();
			}
		} finally {
			writeUnlock();
		}
		for (ConfigFileListener l : listeners)
			l.onLoad(this);
		return this;
	}

	@Override /* ConfigFile */
	protected String serialize(Object value) throws SerializeException {
		if (value == null)
			return "";
		Class<?> c = value.getClass();
		if (isSimpleType(c))
			return value.toString();
		String s = serializer.toString(value);
		if (s.startsWith("'"))
			return s.substring(1, s.length()-1);
		return s;
	}

	@Override /* ConfigFile */
	@SuppressWarnings({ "unchecked" })
	protected <T> T parse(String s, Type type, Type...args) throws ParseException {

		if (StringUtils.isEmpty(s))
			return null;

		if (isSimpleType(type))
			return (T)pBeanSession.convertToType(s, (Class<?>)type);

		char s1 = charAt(s, 0);
		if (s1 != '[' && s1 != '{' && ! "null".equals(s))
			s = '\'' + s + '\'';

		return parser.parse(s, type, args);
	}

	private static boolean isSimpleType(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c == String.class || c.isPrimitive() || c.isAssignableFrom(Number.class) || c == Boolean.class || c.isEnum());
	}


	//--------------------------------------------------------------------------------
	// Map methods
	//--------------------------------------------------------------------------------

	@Override /* Map */
	public Section get(Object key) {
		if (StringUtils.isEmpty(key))
			key = DEFAULT;
		readLock();
		try {
			return sections.get(key);
		} finally {
			readUnlock();
		}
	}

	@Override /* Map */
	public Section put(String key, Section section) {
		Set<String> changes = createChanges();
		Section old = put(key, section, changes);
		signalChanges(changes);
		return old;
	}

	private Section put(String key, Section section, Set<String> changes) {
		if (StringUtils.isEmpty(key))
			key = DEFAULT;
		writeLock();
		try {
			Section prev = sections.put(key, section);
			findChanges(changes, prev, section);
			return prev;
		} finally {
			writeUnlock();
		}
	}

	@Override /* Map */
	public void putAll(Map<? extends String,? extends Section> map) {
		Set<String> changes = createChanges();
		writeLock();
		try {
			for (Map.Entry<? extends String,? extends Section> e : map.entrySet())
				put(e.getKey(), e.getValue(), changes);
		} finally {
			writeUnlock();
		}
		signalChanges(changes);
	}

	@Override /* Map */
	public void clear() {
		Set<String> changes = createChanges();
		writeLock();
		try {
			for (Section s : values())
				findChanges(changes, s, null);
			sections.clear();
		} finally {
			writeUnlock();
		}
		signalChanges(changes);
	}

	@Override /* Map */
	public boolean containsKey(Object key) {
		if (StringUtils.isEmpty(key))
			key = DEFAULT;
		return sections.containsKey(key);
	}

	@Override /* Map */
	public boolean containsValue(Object value) {
		return sections.containsValue(value);
	}

	@Override /* Map */
	public Set<Map.Entry<String,Section>> entrySet() {

		// We need to create our own set so that entries are removed correctly.
		return new AbstractSet<Map.Entry<String,Section>>() {
			@Override /* Map */
			public Iterator<Map.Entry<String,Section>> iterator() {
				return new Iterator<Map.Entry<String,Section>>() {
					Iterator<Map.Entry<String,Section>> i = sections.entrySet().iterator();
					Map.Entry<String,Section> i2;

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public Map.Entry<String,Section> next() {
						i2 = i.next();
						return i2;
					}

					@Override /* Iterator */
					public void remove() {
						Set<String> changes = createChanges();
						findChanges(changes, i2.getValue(), null);
						i.remove();
						signalChanges(changes);
					}
				};
			}

			@Override /* Map */
			public int size() {
				return sections.size();
			}
		};
	}

	@Override /* Map */
	public boolean isEmpty() {
		return sections.isEmpty();
	}

	@Override /* Map */
	public Set<String> keySet() {

		// We need to create our own set so that sections are removed correctly.
		return new AbstractSet<String>() {
			@Override /* Set */
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					Iterator<String> i = sections.keySet().iterator();
					String i2;

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public String next() {
						i2 = i.next();
						return i2;
					}

					@Override /* Iterator */
					public void remove() {
						Set<String> changes = createChanges();
						findChanges(changes, sections.get(i2), null);
						i.remove();
						signalChanges(changes);
					}
				};
			}

			@Override /* Set */
			public int size() {
				return sections.size();
			}
		};
	}

	@Override /* Map */
	public int size() {
		return sections.size();
	}

	@Override /* Map */
	public Collection<Section> values() {
		return new AbstractCollection<Section>() {
			@Override /* Collection */
			public Iterator<Section> iterator() {
				return new Iterator<Section>() {
					Iterator<Section> i = sections.values().iterator();
					Section i2;

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public Section next() {
						i2 = i.next();
						return i2;
					}

					@Override /* Iterator */
					public void remove() {
						Set<String> changes = createChanges();
						findChanges(changes, i2, null);
						i.remove();
						signalChanges(changes);
					}
				};
			}
			@Override /* Collection */
			public int size() {
				return sections.size();
			}
		};
	}

	@Override /* Map */
	public Section remove(Object key) {
		Set<String> changes = createChanges();
		Section prev = remove(key, changes);
		signalChanges(changes);
		return prev;
	}

	private Section remove(Object key, Set<String> changes) {
		writeLock();
		try {
			Section prev = sections.remove(key);
			findChanges(changes, prev, null);
			return prev;
		} finally {
			writeUnlock();
		}
	}


	//--------------------------------------------------------------------------------
	// API methods
	//--------------------------------------------------------------------------------

	@Override /* ConfigFile */
	public String get(String sectionName, String sectionKey) {
		assertFieldNotNull(sectionKey, "sectionKey");
		Section s = get(sectionName);
		if (s == null)
			return null;
		Object s2 = s.get(sectionKey);
		return (s2 == null ? null : s2.toString());
	}

	@Override /* ConfigFile */
	public String put(String sectionName, String sectionKey, Object value, boolean encoded) throws SerializeException {
		assertFieldNotNull(sectionKey, "sectionKey");
		Section s = getSection(sectionName, true);
		return s.put(sectionKey, serialize(value), encoded);
	}

	@Override /* ConfigFile */
	public String put(String sectionName, String sectionKey, String value, boolean encoded) {
		assertFieldNotNull(sectionKey, "sectionKey");
		Section s = getSection(sectionName, true);
		return s.put(sectionKey, value, encoded);
	}

	@Override /* ConfigFile */
	public String remove(String sectionName, String sectionKey) {
		assertFieldNotNull(sectionKey, "sectionKey");
		Section s = getSection(sectionName, false);
		if (s == null)
			return null;
		return s.remove(sectionKey);
	}

	@Override /* ConfigFile */
	public ConfigFileImpl addLines(String section, String...lines) {
		Set<String> changes = createChanges();
		writeLock();
		try {
			getSection(section, true).addLines(changes, lines);
		} finally {
			writeUnlock();
		}
		signalChanges(changes);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl addHeaderComments(String section, String...headerComments) {
		writeLock();
		try {
			if (headerComments == null)
				headerComments = new String[0];
			getSection(section, true).addHeaderComments(Arrays.asList(headerComments));
		} finally {
			writeUnlock();
		}
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl clearHeaderComments(String section) {
		writeLock();
		try {
			Section s = getSection(section, false);
			if (s != null)
				s.clearHeaderComments();
		} finally {
			writeUnlock();
		}
		return this;
	}

	@Override /* ConfigFile */
	public Section getSection(String name) {
		return getSection(name, false);
	}

	@Override /* ConfigFile */
	public Section getSection(String name, boolean create) {
		if (StringUtils.isEmpty(name))
			name = DEFAULT;
		Section s = sections.get(name);
		if (s != null)
			return s;
		if (create) {
			s = new Section().setParent(this).setName(name);
			sections.put(name, s);
			return s;
		}
		return null;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl addSection(String name) {
		writeLock();
		try {
			getSection(name, true);
		} finally {
			writeUnlock();
		}
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile setSection(String name, Map<String,String> contents) {
		writeLock();
		try {
			put(name, new Section(contents).setParent(this).setName(name));
		} finally {
			writeUnlock();
		}
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFileImpl removeSection(String name) {
		Set<String> changes = createChanges();
		writeLock();
		try {
			Section prev = sections.remove(name);
			if (changes != null && prev != null)
				findChanges(changes, prev, null);
		} finally {
			writeUnlock();
		}
		signalChanges(changes);
		return this;
	}

	@Override /* ConfigFile */
	public Set<String> getSectionKeys(String sectionName) {
		Section s = get(sectionName);
		if (s == null)
			return null;
		return s.keySet();
	}

	@Override /* ConfigFile */
	public boolean isEncoded(String key) {
		assertFieldNotNull(key, "key");
		String section = getSectionName(key);
		Section s = getSection(section, false);
		if (s == null)
			return false;
		return s.isEncoded(getSectionKey(key));
	}

	@Override /* ConfigFile */
	public ConfigFileImpl save() throws IOException {
		writeLock();
		try {
			if (file == null)
				throw new UnsupportedOperationException("No backing file specified for config file.");
			Writer out = new OutputStreamWriter(new FileOutputStream(file), charset);
			try {
				serializeTo(out);
				hasBeenModified = false;
				modifiedTimestamp = file.lastModified();
			} finally {
				out.close();
			}
			for (ConfigFileListener l : listeners)
				l.onSave(this);
			return this;
		} finally {
			writeUnlock();
		}
	}

	@Override /* ConfigFile */
	public ConfigFileImpl serializeTo(Writer out, ConfigFileFormat format) throws IOException {
		readLock();
		try {
			PrintWriter pw = (out instanceof PrintWriter ? (PrintWriter)out : new PrintWriter(out));
			for (Section s : sections.values())
				s.writeTo(pw, format);
			pw.flush();
			pw.close();
			out.close();
		} finally {
			readUnlock();
		}
		return this;
	}

	void setHasBeenModified() {
		hasBeenModified = true;
	}

	@Override /* ConfigFile */
	public String toString() {
		try {
			StringWriter sw = new StringWriter();
			toWritable().writeTo(sw);
			return sw.toString();
		} catch (IOException e) {
			return e.getLocalizedMessage();
		}
	}

	@Override /* ConfigFile */
	public ConfigFile addListener(ConfigFileListener listener) {
		assertFieldNotNull(listener, "listener");
		writeLock();
		try {
			this.listeners.add(listener);
			return this;
		} finally {
			writeUnlock();
		}
	}

	List<ConfigFileListener> getListeners() {
		return listeners;
	}

	@Override /* ConfigFile */
	public Writable toWritable() {
		return new ConfigFileWritable(this);
	}

	@Override /* ConfigFile */
	public ConfigFile merge(ConfigFile cf) {
		assertFieldNotNull(cf, "cf");
		Set<String> changes = createChanges();
		writeLock();
		try {
			for (String sectionName : this.keySet())
				if (! cf.containsKey(sectionName))
					remove(sectionName, changes);

			for (Map.Entry<String,Section> e : cf.entrySet())
				put(e.getKey(), e.getValue(), changes);

		} finally {
			writeUnlock();
		}
		signalChanges(changes);
		return this;
	}

	Encoder getEncoder() {
		return encoder;
	}

	@Override /* ConfigFile */
	protected BeanSession getBeanSession() {
		return pBeanSession;
	}

	@Override /* ConfigFile */
	protected void readLock() {
		lock.readLock().lock();
	}

	@Override /* ConfigFile */
	protected void readUnlock() {
		lock.readLock().unlock();
	}

	private void writeLock() {
		if (readOnly)
			throw new UnsupportedOperationException("Cannot modify read-only ConfigFile.");
		lock.writeLock().lock();
		hasBeenModified = true;
	}

	private void writeUnlock() {
		lock.writeLock().unlock();
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving(VarResolver vr) {
		assertFieldNotNull(vr, "vr");
		return new ConfigFileWrapped(this, vr);
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving(VarResolverSession vs) {
		assertFieldNotNull(vs, "vs");
		return new ConfigFileWrapped(this, vs);
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving() {
		return getResolving(
			new VarResolverBuilder()
				.vars(SystemPropertiesVar.class, EnvVariablesVar.class, SwitchVar.class, IfVar.class, ConfigFileVar.class,IfVar.class,SwitchVar.class)
				.contextObject(ConfigFileVar.SESSION_config, this)
				.build()
		);
	}

	/*
	 * Finds the keys that are different between the two sections and adds it to
	 * the specified set.
	 */
	private static void findChanges(Set<String> s, Section a, Section b) {
		if (s == null)
			return;
		String sname = (a == null ? b.name : a.name);
		if (a == null) {
			for (String k : b.keySet())
				s.add(getFullKey(sname, k));
		} else if (b == null) {
			for (String k : a.keySet())
				s.add(getFullKey(sname, k));
		} else {
			for (String k : a.keySet())
				addChange(s, sname, k, a.get(k), b.get(k));
			for (String k : b.keySet())
				addChange(s, sname, k, a.get(k), b.get(k));
		}
	}

	private static void addChange(Set<String> changes, String section, String key, String oldVal, String newVal) {
		if (! isEquals(oldVal, newVal))
			changes.add(getFullKey(section, key));
	}

	private Set<String> createChanges() {
		return (listeners.size() > 0 ? new LinkedHashSet<String>() : null);
	}

	private void signalChanges(Set<String> changes) {
		if (changes != null && ! changes.isEmpty())
			for (ConfigFileListener l : listeners)
				l.onChange(this, changes);
	}
}