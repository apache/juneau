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
package org.apache.juneau.utils;

import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.jar.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;

/**
 * Utility class for working with Jar manifest files.
 *
 * <p>
 * Copies the contents of a {@link Manifest} into an {@link JsonMap} so that the various convenience methods on that
 * class can be used to retrieve values.
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings("resource")
public class ManifestFile extends JsonMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Finds and loads the manifest file of the jar file that the specified class is contained within.
	 *
	 * @param c The class to get the manifest file of.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(Class<?> c) throws IOException {
		var className = c.getSimpleName() + ".class";
		var classPath = c.getResource(className).toString();
		if (! classPath.startsWith("jar")) {
			return;
		}
		var manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
		try {
			var mf = new Manifest(new URL(manifestPath).openStream());
			load(mf);
		} catch (MalformedURLException e) {
			throw castException(IOException.class, e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create an instance of this class from a manifest file on the file system.
	 *
	 * @param f The manifest file.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(File f) throws IOException {
		var mf = new Manifest();
		try (var fis = new FileInputStream(f)) {
			mf.read(fis);
			load(mf);
		} catch (IOException e) {
			throw ioex(e, "Problem detected in MANIFEST.MF.  Contents below:\n{0}", read(f));
		}
	}

	/**
	 * Create an instance of this class loaded from the contents of an input stream.
	 *
	 * <p>
	 * Note that the input must end in a newline to pick up the last line!
	 *
	 * @param is The manifest file contents.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(InputStream is) throws IOException {
		load(new Manifest(is));
	}

	/**
	 * Create an instance of this class from a {@link Manifest} object.
	 *
	 * @param f The manifest to read from.
	 */
	public ManifestFile(Manifest f) {
		load(f);
	}

	/**
	 * Create an instance of this class from a manifest path on the file system.
	 *
	 * @param path The manifest path.
	 * @throws IOException If a problem occurred while trying to read the manifest path.
	 */
	public ManifestFile(Path path) throws IOException {
		var mf = new Manifest();
		try (var fis = Files.newInputStream(path)) {
			mf.read(fis);
			load(mf);
		} catch (IOException e) {
			throw ioex(e, "Problem detected in MANIFEST.MF.  Contents below:\n{0}", read(path), e);
		}
	}

	/**
	 * Create an instance of this class loaded from the contents of a reader.
	 *
	 * <p>
	 * Note that the input must end in a newline to pick up the last line!
	 *
	 * @param r The manifest file contents.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(Reader r) throws IOException {
		load(new Manifest(new ByteArrayInputStream(read(r).getBytes(UTF8))));
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile append(Map<String,Object> values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile append(String key, Object value) {
		super.append(key, value);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile appendIf(boolean flag, String key, Object value) {
		super.appendIf(flag, key, value);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile filtered(Predicate<Object> value) {
		super.filtered(value);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile inner(Map<String,Object> inner) {
		super.inner(inner);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile keepAll(String...keys) {
		super.keepAll(keys);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile modifiable() {
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile session(BeanSession session) {
		super.session(session);
		return this;
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile setBeanSession(BeanSession value) {
		super.setBeanSession(value);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		var sb = new StringBuilder();
		forEach((k, v) -> sb.append(k).append(": ").append(v));
		return sb.toString();
	}

	@Override /* Overridden from JsonMap */
	public ManifestFile unmodifiable() {
		return this;
	}

	private void load(Manifest mf) {
		mf.getMainAttributes().forEach((k, v) -> put(k.toString(), v.toString()));
	}
}