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
package org.apache.juneau.config.store;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.annotation.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.utils.*;

/**
 * Filesystem-based storage location for configuration files.
 *
 * <p>
 * Points to a file system directory containing configuration files.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 */
@SuppressWarnings("resource")
public class FileStore extends ConfigStore {
	/**
	 * Builder class.
	 */
	public static class Builder extends ConfigStore.Builder {

		String directory, extensions;
		Charset charset;
		boolean enableWatcher, updateOnWrite;
		WatcherSensitivity watcherSensitivity;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			directory = env("ConfigFileStore.directory", ".");
			charset = env("ConfigFileStore.charset", Charset.defaultCharset());
			enableWatcher = env("ConfigFileStore.enableWatcher", false);
			watcherSensitivity = env("ConfigFileStore.watcherSensitivity", WatcherSensitivity.MEDIUM);
			updateOnWrite = env("ConfigFileStore.updateOnWrite", false);
			extensions = env("ConfigFileStore.extensions", "cfg");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			directory = copyFrom.directory;
			charset = copyFrom.charset;
			enableWatcher = copyFrom.enableWatcher;
			watcherSensitivity = copyFrom.watcherSensitivity;
			updateOnWrite = copyFrom.updateOnWrite;
			extensions = copyFrom.extensions;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(FileStore copyFrom) {
			super(copyFrom);
			type(copyFrom.getClass());
			directory = copyFrom.directory;
			charset = copyFrom.charset;
			enableWatcher = copyFrom.enableWatcher;
			watcherSensitivity = copyFrom.watcherSensitivity;
			updateOnWrite = copyFrom.updateOnWrite;
			extensions = copyFrom.extensions;
		}

		@Override /* Overridden from Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Class<?>...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Object...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public FileStore build() {
			return build(FileStore.class);
		}

		@Override /* Overridden from Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		/**
		 * Charset for external files.
		 *
		 * <p>
		 * Identifies the charset of external files.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.charset"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_CHARSET"
		 * 		<li>{@link Charset#defaultCharset()}
		 * 	</ul>
		 * @return This object.
		 */
		public Builder charset(Charset value) {
			charset = value;
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		/**
		 * Local file system directory.
		 *
		 * <p>
		 * Identifies the path of the directory containing the configuration files.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.directory"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_DIRECTORY"
		 * 		<li><js>"."</js>.
		 * 	</ul>
		 * @return This object.
		 */
		public Builder directory(File value) {
			directory = value.getAbsolutePath();
			return this;
		}

		/**
		 * Local file system directory.
		 *
		 * <p>
		 * Identifies the path of the directory containing the configuration files.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.directory"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_DIRECTORY"
		 * 		<li><js>"."</js>
		 * 	</ul>
		 * @return This object.
		 */
		public Builder directory(String value) {
			directory = value;
			return this;
		}

		/**
		 * Use watcher.
		 *
		 * <p>
		 * Use a file system watcher for file system changes.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Calling {@link FileStore#close()} closes the watcher.
		 * </ul>
		 *
		 *	<p>
		 * 	The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.enableWatcher"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_ENABLEWATCHER"
		 * 		<li><jk>false</jk>.
		 * 	</ul>
		 *
		 * @return This object.
		 */
		public Builder enableWatcher() {
			enableWatcher = true;
			return this;
		}

		/**
		 * File extensions.
		 *
		 * <p>
		 * Defines what file extensions to search for when the config name does not have an extension.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.extensions"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_EXTENSIONS"
		 * 		<li><js>"cfg"</js>
		 * 	</ul>
		 * @return This object.
		 */
		public Builder extensions(String value) {
			extensions = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		/**
		 * Update-on-write.
		 *
		 * <p>
		 * When enabled, the {@link FileStore#update(String, String)} method will be called immediately following
		 * calls to {@link FileStore#write(String, String, String)} when the contents are changing.
		 * <br>This allows for more immediate responses to configuration changes on file systems that use
		 * polling watchers.
		 * <br>This may cause double-triggering of {@link ConfigStoreListener ConfigStoreListeners}.
		 *
		 *	<p>
		 * 	The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.updateOnWrite"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_UPDATEONWRITE"
		 * 		<li><jk>false</jk>.
		 * 	</ul>
		 *
		 * @return This object.
		 */
		public Builder updateOnWrite() {
			updateOnWrite = true;
			return this;
		}

		/**
		 * Watcher sensitivity.
		 *
		 * <p>
		 * Determines how frequently the file system is polled for updates.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This relies on internal Sun packages and may not work on all JVMs.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"ConfigFileStore.watcherSensitivity"
		 * 		<li>Environment variable <js>"CONFIGFILESTORE_WATCHERSENSITIVITY"
		 * 		<li>{@link WatcherSensitivity#MEDIUM}
		 * 	</ul>
		 * @return This object.
		 */
		public Builder watcherSensitivity(WatcherSensitivity value) {
			watcherSensitivity = value;
			return this;
		}
	}

	class WatcherThread extends Thread {
		private final WatchService watchService;

		WatcherThread(File dir, WatcherSensitivity s) throws Exception {
			watchService = FileSystems.getDefault().newWatchService();
			var kinds = a(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			var modifier = lookupModifier(s);
			dir.toPath().register(watchService, kinds, modifier);
		}

		@Override /* Overridden from Thread */
		public void interrupt() {
			try {
				watchService.close();
			} catch (IOException e) {
				throw toRex(e);
			} finally {
				super.interrupt();
			}
		}

		@SuppressWarnings("unchecked")
		@Override /* Overridden from Thread */
		public void run() {
			try {
				WatchKey key;
				while (nn(key = watchService.take())) {
					for (var event : key.pollEvents()) {
						var kind = event.kind();
						if (kind != OVERFLOW)
							FileStore.this.onFileEvent(((WatchEvent<Path>)event));
					}
					if (! key.reset())
						break;
				}
			} catch (Exception e) {
				throw toRex(e);
			}
		}

		private WatchEvent.Modifier lookupModifier(WatcherSensitivity s) {
			try {
				return switch (s) {
					case LOW -> com.sun.nio.file.SensitivityWatchEventModifier.LOW;
					case MEDIUM -> com.sun.nio.file.SensitivityWatchEventModifier.MEDIUM;
					case HIGH -> com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
				};
			} catch (@SuppressWarnings("unused") Exception e) {
				/* Ignore */
			}
			return null;
		}
	}

	/** Default file store, all default values.*/
	public static final FileStore DEFAULT = FileStore.create().build();

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
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
	public FileStore(Builder builder) {
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
			exts = split(extensions).toArray(String[]::new);
			watcher = enableWatcher ? new WatcherThread(dir, watcherSensitivity) : null;
			if (nn(watcher))
				watcher.start();
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from Closeable */
	public synchronized void close() {
		if (nn(watcher))
			watcher.interrupt();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from ConfigStore */
	public synchronized boolean exists(String name) {
		return Files.exists(resolveFile(name));
	}

	@Override /* Overridden from ConfigStore */
	public synchronized String read(String name) throws IOException {
		name = resolveName(name);

		var p = resolveFile(name);
		name = p.getFileName().toString();

		var s = cache.get(name);
		if (nn(s))
			return s;

		dir.mkdirs();

		// If file doesn't exist, don't trigger creation.
		if (! Files.exists(p))
			return "";

		var isWritable = isWritable(p);
		var oo = isWritable ? a(READ, WRITE, CREATE) : a(READ);

		try (var fc = FileChannel.open(p, oo)) {
			try (var lock = isWritable ? fc.lock() : null) {
				var buf = ByteBuffer.allocate(1024);
				var sb = new StringBuilder();
				while (fc.read(buf) != -1) {
					sb.append(charset.decode((buf.flip()))); // Fixes Java 11 issue involving overridden flip method.
					buf.clear();
				}
				s = sb.toString();
				cache.put(name, s);
			}
		}

		return cache.get(name);
	}

	@Override /* Overridden from ConfigStore */
	public synchronized FileStore update(String name, String newContents) {
		cache.put(name, newContents);
		super.update(name, newContents);
		return this;
	}

	@Override /* Overridden from ConfigStore */
	public synchronized String write(String name, String expectedContents, String newContents) throws IOException {
		name = resolveName(name);

		// This is a no-op.
		if (eq(expectedContents, newContents))
			return null;

		dir.mkdirs();

		var p = resolveFile(name);
		name = p.getFileName().toString();

		var exists = Files.exists(p);

		// Don't create the file if we're not going to match.
		if ((! exists) && isNotEmpty(expectedContents))
			return "";

		if (isWritable(p)) {
			if (newContents == null)
				Files.delete(p);
			else {
				try (var fc = FileChannel.open(p, READ, WRITE, CREATE)) {
					try (var lock = fc.lock()) {
						var currentContents = "";
						if (exists) {
							var buf = ByteBuffer.allocate(1024);
							var sb = new StringBuilder();
							while (fc.read(buf) != -1) {
								sb.append(charset.decode(buf.flip()));
								buf.clear();
							}
							currentContents = sb.toString();
						}
						if (nn(expectedContents) && ne(currentContents, expectedContents)) {
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

	private synchronized boolean isWritable(Path p) {
		try {
			if (! Files.exists(p)) {
				Files.createDirectories(p.getParent());
				if (! Files.exists(p) && ! p.toFile().createNewFile()) {
					throw ioex("Could not create file: {0}", p);
				}
			}
		} catch (@SuppressWarnings("unused") IOException e) {
			return false;
		}
		return Files.isWritable(p);
	}

	private Path resolveFile(String name) {
		return dir.toPath().resolve(resolveName(name));
	}

	/**
	 * Gets called when the watcher service on this store is triggered with a file system change.
	 *
	 * @param e The file system event.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected synchronized void onFileEvent(WatchEvent<Path> e) throws IOException {
		var fn = e.context().getFileName().toString();

		var oldContents = cache.get(fn);
		cache.remove(fn);
		var newContents = read(fn);

		if (ne(oldContents, newContents)) {
			update(fn, newContents);
		}
	}

	@Override /* Overridden from Context */
	protected JsonMap properties() {
		return filteredMap("charset", charset, "extensions", extensions, "updateOnWrite", updateOnWrite);
	}

	@Override
	protected String resolveName(String name) {
		if (! nameCache.containsKey(name)) {
			var n = (String)null;

			// Does file exist as-is?
			if (fileExists(dir, name))
				n = name;

			// Does name already have an extension?
			if (n == null) {
				for (var ext : exts) {
					if (hasExtension(name, ext)) {
						n = name;
						break;
					}
				}
			}

			// Find file with the correct extension.
			if (n == null) {
				for (var ext : exts) {
					if (fileExists(dir, name + '.' + ext)) {
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
}