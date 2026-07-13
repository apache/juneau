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
package org.apache.juneau.commons.runtime;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Lean accessor for the contents of a Jar manifest file.
 *
 * <p>
 * Wraps a {@link Manifest} so callers can read main-attribute and per-section entries via an
 * {@link Optional}-returning API without depending on the marshalling stack.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Load the manifest of the jar containing this class.</jc>
 * 	ManifestFile <jv>mf</jv> = <jk>new</jk> ManifestFile(MyClass.<jk>class</jk>);
 *
 * 	<jc>// Pull a main attribute.</jc>
 * 	String <jv>version</jv> = <jv>mf</jv>.get(<js>"Bundle-Version"</js>).orElse(<js>"unknown"</js>);
 *
 * 	<jc>// Pull a section attribute (new capability).</jc>
 * 	String <jv>sectionAttr</jv> = <jv>mf</jv>.get(<js>"my-section"</js>, <js>"some-key"</js>).orElse(<jk>null</jk>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is immutable and thread-safe once constructed.
 * </ul>
 */
public class ManifestFile {

	private final Map<String,String> mainAttributes;
	private final Map<String,Map<String,String>> sections;

	/**
	 * Loads the manifest of the jar containing the given class.
	 *
	 * <p>
	 * If the class isn't packaged inside a Jar (e.g. running from an exploded classpath), the resulting
	 * instance has no main attributes and no sections.
	 *
	 * @param c The anchor class.
	 * @throws IOException If the manifest could not be read.
	 */
	public ManifestFile(Class<?> c) throws IOException {
		var className = c.getSimpleName() + ".class";
		var classPath = c.getResource(className).toString();
		Manifest mf = null;
		if (classPath.startsWith("jar")) {
			var manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
			try (var in = URI.create(manifestPath).toURL().openStream()) {
				mf = new Manifest(in);
			} catch (IllegalArgumentException | MalformedURLException e) {
				throw castException(IOException.class, e);
			}
		}
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	/**
	 * Loads a manifest from a file on disk.
	 *
	 * @param f The manifest file.
	 * @throws IOException If the manifest could not be read.
	 */
	public ManifestFile(File f) throws IOException {
		Manifest mf;
		try (var fis = new FileInputStream(f)) {
			mf = new Manifest();
			mf.read(fis);
		} catch (IOException e) {
			throw ioex(e, "Problem detected in MANIFEST.MF.  Contents below:\n{0}", read(f));
		}
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	/**
	 * Loads a manifest from an input stream.
	 *
	 * <p>
	 * The stream must end in a newline to pick up the last line.
	 *
	 * @param in The manifest stream.
	 * @throws IOException If the manifest could not be read.
	 */
	public ManifestFile(InputStream in) throws IOException {
		var mf = new Manifest(in);
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	/**
	 * Wraps an existing {@link Manifest}.
	 *
	 * @param mf The manifest.
	 */
	public ManifestFile(Manifest mf) {
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	/**
	 * Loads a manifest from a path.
	 *
	 * @param path The manifest path.
	 * @throws IOException If the manifest could not be read.
	 */
	public ManifestFile(Path path) throws IOException {
		Manifest mf;
		try (var in = Files.newInputStream(path)) {
			mf = new Manifest();
			mf.read(in);
		} catch (IOException e) {
			throw ioex(e, "Problem detected in MANIFEST.MF.  Contents below:\n{0}", read(path));
		}
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	/**
	 * Loads a manifest from a reader.
	 *
	 * <p>
	 * The reader's contents must end in a newline to pick up the last line.
	 *
	 * @param r The manifest reader.
	 * @throws IOException If the manifest could not be read.
	 */
	public ManifestFile(Reader r) throws IOException {
		var mf = new Manifest(new ByteArrayInputStream(read(r).getBytes(UTF8)));
		this.mainAttributes = readMain(mf);
		this.sections = readSections(mf);
	}

	private static Map<String,String> readMain(Manifest mf) {
		if (mf == null)
			return Collections.emptyMap();
		var out = new LinkedHashMap<String,String>();
		mf.getMainAttributes().forEach((k,v) -> out.put(k.toString(), v.toString()));
		return Collections.unmodifiableMap(out);
	}

	private static Map<String,Map<String,String>> readSections(Manifest mf) {
		if (mf == null)
			return Collections.emptyMap();
		var out = new LinkedHashMap<String,Map<String,String>>();
		mf.getEntries().forEach((sectionName, attrs) -> {
			var section = new LinkedHashMap<String,String>();
			attrs.forEach((k,v) -> section.put(k.toString(), v.toString()));
			out.put(sectionName, Collections.unmodifiableMap(section));
		});
		return Collections.unmodifiableMap(out);
	}

	/**
	 * Returns the value of a main-attribute key.
	 *
	 * @param key The main-attribute key.
	 * @return The value, or empty if the key is unset.
	 */
	public Optional<String> get(String key) {
		return o(mainAttributes.get(key));
	}

	/**
	 * Returns the value of a key inside the named section.
	 *
	 * @param section The section name.
	 * @param key The attribute key.
	 * @return The value, or empty if the section or key is unset.
	 */
	public Optional<String> get(String section, String key) {
		var s = sections.get(section);
		return s == null ? oe() : o(s.get(key));
	}

	/**
	 * Returns the names of all per-entry sections in the manifest.
	 *
	 * @return An unmodifiable set of section names.
	 */
	public Set<String> sections() {
		return sections.keySet();
	}

	/**
	 * Returns the main attributes as an unmodifiable map.
	 *
	 * @return The main-attribute map.
	 */
	public Map<String,String> asMap() {
		return mainAttributes;
	}

	/**
	 * Returns the attributes of the named section as an unmodifiable map.
	 *
	 * @param section The section name.
	 * @return The section's attribute map, or an empty map if the section is unknown.
	 */
	public Map<String,String> asMap(String section) {
		var s = sections.get(section);
		return s == null ? Collections.emptyMap() : s;
	}

	@Override /* Object */
	public String toString() {
		var sb = new StringBuilder();
		mainAttributes.forEach((k,v) -> sb.append(k).append(": ").append(v).append('\n'));
		return sb.toString();
	}
}
