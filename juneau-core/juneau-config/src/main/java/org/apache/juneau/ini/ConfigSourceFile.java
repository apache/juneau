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

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Implementation of a configuration source that's a file on the local file system.
 */
public class ConfigSourceFile extends ConfigSource {

	private ConcurrentHashMap<String,CacheEntry> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param settings
	 * 	The settings for this config source.
	 */
	public ConfigSourceFile(ConfigSourceSettings settings) {
		super(settings);
	}

	@Override /* ConfigSource */
	public synchronized String read(String name) throws Exception {
		CacheEntry fe = cache.get(name);

		if (fe == null || fe.hasBeenModified()) {
			File f = findFile(name);
			try (FileInputStream fis = new FileInputStream(f)) {
				try (FileLock lock = fis.getChannel().lock()) {
					try (Reader r = new InputStreamReader(fis, Charset.defaultCharset())) {
						String contents = IOUtils.read(r);
						long lastModified = f.lastModified();
						fe = new CacheEntry(f, lastModified, contents);
						cache.put(name, fe);
					}
				}
			}
		}

		return fe.contents;
	}

	@Override /* ConfigSource */
	public synchronized boolean write(String name, String contents) throws Exception {
		if (hasBeenModified(name))
			return false;

		CacheEntry fe = cache.get(name);
		File f = fe != null ? fe.file : findFile(name);

		try (FileOutputStream fos = new FileOutputStream(f)) {
			try (FileLock lock = fos.getChannel().lock()) {
				if (hasBeenModified(name))
					return false;
				try (Writer w = new OutputStreamWriter(fos, Charset.defaultCharset())) {
					IOUtils.pipe(contents, w);
				}
				fe = new CacheEntry(f, f.lastModified(), contents);
				cache.put(name, fe);
				return true;
			}
		}
	}

	@Override /* ConfigSource */
	public boolean hasBeenModified(String name) throws Exception {
		CacheEntry fe = cache.get(name);
		return (fe != null && fe.hasBeenModified());
	}

	private static class CacheEntry {
		final File file;
		final long lastModified;
		final String contents;

		CacheEntry(File file, long lastModified, String contents) {
			this.file = file;
			this.lastModified = lastModified;
			this.contents = contents;
		}

		boolean hasBeenModified() {
			return file.lastModified() != lastModified;
		}
	}

	private File findFile(String name) throws IOException {

		List<String> searchPaths = getSettings().getSearchPaths();

		if (searchPaths.isEmpty())
			throw new FileNotFoundException("No search paths specified in ConfigFileBuilder.");

		// Handle paths relative to search paths.
		for (String sp : searchPaths) {
			File pf = new File(sp);
			File f = new File(pf, name);
			if (f.exists())
				return f;
		}

		if (getSettings().isCreateIfNotExists()) {
			for (String sf : searchPaths) {
				File pf = new File(sf);
				if (pf.exists() && pf.isDirectory() && pf.canWrite()) {
					File f = new File(pf, name);
					if (f.createNewFile())
						return f;
				}
			}
		}

		throw new FileNotFoundException("Could not find config file '"+name+"'");
	}
}
