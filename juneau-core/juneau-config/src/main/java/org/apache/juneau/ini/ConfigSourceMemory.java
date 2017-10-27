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
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of a configuration source entirely in memory.
 */
public class ConfigSourceMemory extends ConfigSource {

	private static final ConcurrentHashMap<String,MemoryFile> MEMORY = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String,CacheEntry> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param settings
	 * 	The settings for this config source.
	 */
	public ConfigSourceMemory(ConfigSourceSettings settings) {
		super(settings);
	}

	@Override /* ConfigSource */
	public synchronized String read(String name) throws Exception {
		CacheEntry ce = cache.get(name);

		if (ce == null || ce.hasBeenModified()) {
			MemoryFile f = findFile(name);
			synchronized(f) {
				ce = new CacheEntry(f, f.lastModified, f.contents);
				cache.put(name, ce);
			}
		}

		return ce.contents;
	}

	@Override /* ConfigSource */
	public synchronized boolean write(String name, String contents) throws Exception {
		if (hasBeenModified(name))
			return false;

		CacheEntry ce = cache.get(name);
		MemoryFile f = ce != null ? ce.file : findFile(name);

		synchronized(f) {
			if (hasBeenModified(name))
				return false;
			f.contents = contents;
			f.lastModified = System.currentTimeMillis();
			ce = new CacheEntry(f, f.lastModified, f.contents);
			cache.put(name, ce);
		}

		return true;
	}

	@Override /* ConfigSource */
	public boolean hasBeenModified(String name) throws Exception {
		CacheEntry ce = cache.get(name);
		return (ce != null && ce.hasBeenModified());
	}

	private MemoryFile findFile(String name) throws IOException {

		List<String> searchPaths = getSettings().getSearchPaths();

		if (searchPaths.isEmpty())
			throw new FileNotFoundException("No search paths specified in ConfigFileBuilder.");

		// Handle paths relative to search paths.
		for (String sp : searchPaths) {
			String pf = sp + '/' + name;
			MemoryFile mf = MEMORY.get(pf);
			if (mf != null)
				return mf;
		}

		if (getSettings().isCreateIfNotExists()) {
			for (String sf : searchPaths) {
				String path = sf + '/' + name;
				MemoryFile mf = new MemoryFile("");
				MEMORY.putIfAbsent(path, mf);
				return MEMORY.get(path);
			}
		}

		throw new FileNotFoundException("Could not find config file '"+name+"'");
	}

	private static class MemoryFile {
		String contents;
		long lastModified;

		MemoryFile(String contents) {
			this.contents = contents;
			this.lastModified = System.currentTimeMillis();
		}
	}

	private static class CacheEntry {
		final MemoryFile file;
		final long lastModified;
		final String contents;

		CacheEntry(MemoryFile file, long lastModified, String contents) {
			this.file = file;
			this.lastModified = lastModified;
			this.contents = contents;
		}

		boolean hasBeenModified() {
			return file.lastModified != lastModified;
		}
	}
}
