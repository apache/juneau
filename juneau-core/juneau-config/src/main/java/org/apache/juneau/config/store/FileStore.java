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
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
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
public class FileStore extends ConfigStore {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

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

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ConfigStore.Builder {

		String directory, extensions;
		Charset charset;
		boolean enableWatcher, updateOnWrite;
		WatcherSensitivity watcherSensitivity;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
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

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public FileStore build() {
			return build(FileStore.class);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

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

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
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
			exts = split(extensions);
			watcher = enableWatcher ? new WatcherThread(dir, watcherSensitivity) : null;
			if (watcher != null)
				watcher.start();
		} catch (Exception e) {
			throw asRuntimeException(e);
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
	public synchronized FileStore update(String name, String newContents) {
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
							FileStore.this.onFileEvent(((WatchEvent<Path>)event));
					}
					if (! key.reset())
						break;
				}
			} catch (Exception e) {
				throw asRuntimeException(e);
			}
		};

		@Override /* Thread */
		public void interrupt() {
			try {
				watchService.close();
			} catch (IOException e) {
				throw asRuntimeException(e);
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
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap("charset", charset, "extensions", extensions, "updateOnWrite", updateOnWrite);
	}
}
