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

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardOpenOption.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Filesystem-based storage location for configuration files.
 *
 * <p>
 * Points to a file system directory containing configuration files.
 */
@ConfigurableContext
public class ConfigFileStore extends ConfigStore {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default file store, all default values.*/
	public static final ConfigFileStore DEFAULT = ConfigFileStore.create().build();


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static ConfigFileStoreBuilder create() {
		return new ConfigFileStoreBuilder();
	}

	@Override /* Context */
	public ConfigFileStoreBuilder copy() {
		return new ConfigFileStoreBuilder(this);
	}

	final String directory, extensions;
	final Charset charset;
	final boolean enableWatcher, updateOnWrite;
	final WatcherSensitivity watcherSensitivity;

	private final File dir;
	private final WatcherThread watcher;
	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String,String> nameCache = new ConcurrentHashMap<>();
	private final String[] exts;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ConfigFileStore(ConfigFileStoreBuilder builder) {
		super(builder);
		directory = builder.directory;
		extensions = builder.extensions;
		charset = builder.charset;
		enableWatcher = builder.enableWatcher;
		updateOnWrite = builder.updateOnWrite;
		watcherSensitivity = builder.watcherSensitivity;
		try {
			dir = new File(directory).getCanonicalFile();
			dir.mkdirs();
			exts = StringUtils.split(extensions);
			watcher = enableWatcher ? new WatcherThread(dir, watcherSensitivity) : null;
			if (watcher != null)
				watcher.start();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@Override /* ConfigStore */
	public synchronized String read(String name) throws IOException {
		name = resolveName(name);

		Path p = resolveFile(name);
		name = p.getFileName().toString();

		String s = cache.get(name);
		if (s != null)
			return s;

		dir.mkdirs();

		// If file doesn't exist, don't trigger creation.
		if (! Files.exists(p))
			return "";

		boolean isWritable = isWritable(p);
		OpenOption[] oo = isWritable ? new OpenOption[]{READ,WRITE,CREATE} : new OpenOption[]{READ};

		try (FileChannel fc = FileChannel.open(p, oo)) {
			try (FileLock lock = isWritable ? fc.lock() : null) {
				ByteBuffer buf = ByteBuffer.allocate(1024);
				StringBuilder sb = new StringBuilder();
				while (fc.read(buf) != -1) {
					sb.append(charset.decode((ByteBuffer)(((Buffer)buf).flip()))); // Fixes Java 11 issue involving overridden flip method.
					((Buffer)buf).clear();
				}
				s = sb.toString();
				cache.put(name, s);
			}
		}

		return cache.get(name);
	}

	@Override /* ConfigStore */
	public synchronized String write(String name, String expectedContents, String newContents) throws IOException {
		name = resolveName(name);

		// This is a no-op.
		if (eq(expectedContents, newContents))
			return null;

		dir.mkdirs();

		Path p = resolveFile(name);
		name = p.getFileName().toString();

		boolean exists = Files.exists(p);

		// Don't create the file if we're not going to match.
		if ((!exists) && isNotEmpty(expectedContents))
			return "";

		if (isWritable(p)) {
			if (newContents == null)
				Files.delete(p);
			else {
				try (FileChannel fc = FileChannel.open(p, READ, WRITE, CREATE)) {
					try (FileLock lock = fc.lock()) {
						String currentContents = "";
						if (exists) {
							ByteBuffer buf = ByteBuffer.allocate(1024);
							StringBuilder sb = new StringBuilder();
							while (fc.read(buf) != -1) {
								sb.append(charset.decode((ByteBuffer)((Buffer)buf).flip()));
								((Buffer)buf).clear();
							}
							currentContents = sb.toString();
						}
						if (expectedContents != null && ! eq(currentContents, expectedContents)) {
							if (currentContents == null)
								cache.remove(name);
							else
								cache.put(name, currentContents);
							return currentContents;
						}
						fc.position(0);
						fc.write(charset.encode(newContents));
					}
				}
			}
		}

		if (updateOnWrite)
			update(name, newContents);
		else
			cache.remove(name);  // Invalidate the cache.

		return null;
	}

	@Override /* ConfigStore */
	public synchronized boolean exists(String name) {
		return Files.exists(resolveFile(name));
	}

	private Path resolveFile(String name) {
		return dir.toPath().resolve(resolveName(name));
	}

	@Override
	protected String resolveName(String name) {
		if (! nameCache.containsKey(name)) {
			String n = null;

			// Does file exist as-is?
			if (FileUtils.exists(dir, name))
				n = name;

			// Does name already have an extension?
			if (n == null) {
				for (String ext : exts) {
					if (FileUtils.hasExtension(name, ext)) {
						n = name;
						break;
					}
				}
			}

			// Find file with the correct extension.
			if (n == null) {
				for (String ext : exts) {
					if (FileUtils.exists(dir, name + '.' + ext)) {
						n = name + '.' + ext;
						break;
					}
				}
			}

			// If file not found, use the default which is the name with the first extension.
			if (n == null)
				n = exts.length == 0 ? name : (name + "." + exts[0]);

			nameCache.put(name, n);
		}
		return nameCache.get(name);
	}

	private synchronized boolean isWritable(Path p) {
		try {
			if (! Files.exists(p)) {
				Files.createDirectories(p.getParent());
				if (! Files.exists(p))
					p.toFile().createNewFile();
			}
		} catch (IOException e) {
			return false;
		}
		return Files.isWritable(p);
	}

	@Override /* ConfigStore */
	public synchronized ConfigFileStore update(String name, String newContents) {
		cache.put(name, newContents);
		super.update(name, newContents);
		return this;
	}

	@Override /* Closeable */
	public synchronized void close() {
		if (watcher != null)
			watcher.interrupt();
	}


	//---------------------------------------------------------------------------------------------
	// WatcherThread
	//---------------------------------------------------------------------------------------------

	final class WatcherThread extends Thread {
		private final WatchService watchService;

		WatcherThread(File dir, WatcherSensitivity s) throws Exception {
			watchService = FileSystems.getDefault().newWatchService();
			WatchEvent.Kind<?>[] kinds = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};
			WatchEvent.Modifier modifier = lookupModifier(s);
			dir.toPath().register(watchService, kinds, modifier);
		}

		@SuppressWarnings("restriction")
		private WatchEvent.Modifier lookupModifier(WatcherSensitivity s) {
			try {
				switch(s) {
					case LOW: return com.sun.nio.file.SensitivityWatchEventModifier.LOW;
					case MEDIUM: return com.sun.nio.file.SensitivityWatchEventModifier.MEDIUM;
					case HIGH: return com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
				}
			} catch (Exception e) {
				/* Ignore */
			}
			return null;

		}

		@SuppressWarnings("unchecked")
		@Override /* Thread */
		public void run() {
			try {
				WatchKey key;
				while ((key = watchService.take()) != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind != OVERFLOW)
							ConfigFileStore.this.onFileEvent(((WatchEvent<Path>)event));
					}
					if (! key.reset())
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw runtimeException(e);
			}
		};

		@Override /* Thread */
		public void interrupt() {
			try {
				watchService.close();
			} catch (IOException e) {
				throw runtimeException(e);
			} finally {
				super.interrupt();
			}
		}
	}

	/**
	 * Gets called when the watcher service on this store is triggered with a file system change.
	 *
	 * @param e The file system event.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected synchronized void onFileEvent(WatchEvent<Path> e) throws IOException {
		String fn = e.context().getFileName().toString();

		String oldContents = cache.get(fn);
		cache.remove(fn);
		String newContents = read(fn);
		if (! eq(oldContents, newContents)) {
			update(fn, newContents);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"ConfigFileStore",
				OMap
					.create()
					.filtered()
					.a("charset", charset)
					.a("extensions", extensions)
					.a("updateOnWrite", updateOnWrite)
			);
	}
}
