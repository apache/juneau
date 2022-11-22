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
package org.apache.juneau.utils;

import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;
import java.net.*;
import java.util.jar.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;

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
public class ManifestFile extends JsonMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Create an instance of this class from a manifest file on the file system.
	 *
	 * @param f The manifest file.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(File f) throws IOException {
		Manifest mf = new Manifest();
		try (FileInputStream fis = new FileInputStream(f)) {
			mf.read(fis);
			load(mf);
		} catch (IOException e) {
			throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n"+read(f), e);
		}
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
	 * Finds and loads the manifest file of the jar file that the specified class is contained within.
	 *
	 * @param c The class to get the manifest file of.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(Class<?> c) throws IOException {
		String className = c.getSimpleName() + ".class";
		String classPath = c.getResource(className).toString();
		if (! classPath.startsWith("jar")) {
			return;
		}
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +  "/META-INF/MANIFEST.MF";
		try {
			Manifest mf = new Manifest(new URL(manifestPath).openStream());
			load(mf);
		} catch (MalformedURLException e) {
			throw ThrowableUtils.cast(IOException.class, e);
		} catch (IOException e) {
			e.printStackTrace();
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

	private void load(Manifest mf) {
		mf.getMainAttributes().forEach((k,v) -> put(k.toString(), v.toString()));
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		forEach((k,v) -> sb.append(k).append(": ").append(v));
		return sb.toString();
	}
}
