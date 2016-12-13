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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for working with Jar manifest files.
 * <p>
 * Copies the contents of a {@link Manifest} into an {@link ObjectMap} so that the various
 * 	convenience methods on that class can be used to retrieve values.
 */
public class ManifestFile extends ObjectMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Create an instance of this class from a manifest file on the file system.
	 *
	 * @param f The manifest file.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(File f) throws IOException {
		Manifest mf = new Manifest();
		FileInputStream fis = new FileInputStream(f);
		try {
			mf.read(fis);
			load(mf);
		} catch (IOException e) {
			throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n" + IOUtils.read(f), e);
		} finally {
			IOUtils.closeQuietly(fis);
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
			throw new IOException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void load(Manifest mf) {
		for (Map.Entry<Object,Object> e : mf.getMainAttributes().entrySet())
			put(e.getKey().toString(), e.getValue().toString());
	}
}
