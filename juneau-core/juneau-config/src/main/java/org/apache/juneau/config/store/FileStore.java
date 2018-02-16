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
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Filesystem-based storage location for configuration files.
 * 
 * <p>
 * Points to a file system directory containing configuration files.
 */
public class FileStore extends Store {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "FileStore.";

	/**
	 * Configuration property:  Local file system directory.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"FileStore.directory.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"."</js>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link FileStoreBuilder#directory(String)}
	 * 			<li class='jm'>{@link FileStoreBuilder#directory(File)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 */
	public static final String FILESTORE_directory = PREFIX + "directory.s";

	/**
	 * Configuration property:  Charset.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"FileStore.charset.s"</js>
	 * 	<li><b>Data type:</b>  {@link Charset}
	 * 	<li><b>Default:</b>  {@link Charset#defaultCharset()}
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link FileStoreBuilder#charset(String)}
	 * 			<li class='jm'>{@link FileStoreBuilder#charset(Charset)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the charset of external files.
	 */
	public static final String FILESTORE_charset = PREFIX + "charset.s";
	
	/**
	 * Configuration property:  Use watcher.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"FileStore.useWatcher.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link FileStoreBuilder#useWatcher()}
	 * 			<li class='jm'>{@link FileStoreBuilder#useWatcher(boolean)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Use a file system watcher for file system changes.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Calling {@link #close()} on this object closes the watcher.
	 * </ul>
	 */
	public static final String FILESTORE_useWatcher = PREFIX + "useWatcher.s";
	
	/**
	 * Configuration property:  Watcher sensitivity.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"FileStore.watcherSensitivity.s"</js>
	 * 	<li><b>Data type:</b>  {@link WatcherSensitivity}
	 * 	<li><b>Default:</b>  {@link WatcherSensitivity#MEDIUM}
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link FileStoreBuilder#watcherSensitivity(WatcherSensitivity)}
	 * 			<li class='jm'>{@link FileStoreBuilder#watcherSensitivity(String)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>This relies on internal Sun packages and may not work on all JVMs.
	 * </ul>
	 */
	public static final String FILESTORE_watcherSensitivity = PREFIX + "watcherSensitivity.s";
	
	/**
	 * Configuration property:  Config file extension.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"FileStore.ext.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"cfg"</js>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link FileStoreBuilder#ext(String)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * File extension identifier for config files.
	 */
	public static final String FILESTORE_ext = PREFIX + "ext.s";

	
	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default file store, all default values.*/
	public static final FileStore DEFAULT = FileStore.create().build();


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Create a new builder for this object.
	 * 
	 * @return A new builder for this object.
	 */
	public static FileStoreBuilder create() {
		return new FileStoreBuilder();
	}
	
	@Override /* Context */
	public FileStoreBuilder builder() {
		return new FileStoreBuilder(getPropertyStore());
	}

	private final File dir;
	private final String ext;
	private final Charset charset;
	private final WatcherThread watcher;
	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();
	
	/**
	 * Constructor.
	 * 
	 * @param ps The settings for this content store.
	 */
	protected FileStore(PropertyStore ps) {
		super(ps);
		try {
			dir = new File(getStringProperty(FILESTORE_directory, ".")).getCanonicalFile();
			dir.mkdirs();
			ext = getStringProperty(FILESTORE_ext, "cfg");
			charset = getProperty(FILESTORE_charset, Charset.class, Charset.defaultCharset());
			WatcherSensitivity ws = getProperty(FILESTORE_watcherSensitivity, WatcherSensitivity.class, WatcherSensitivity.MEDIUM);
			watcher = getBooleanProperty(FILESTORE_useWatcher, false) ? new WatcherThread(dir, ws) : null;
			if (watcher != null)
				watcher.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override /* Store */
	public synchronized String read(String name) throws Exception {
		String s = cache.get(name);
		if (s != null)
			return s;
		
		dir.mkdirs();
		Path p = dir.toPath().resolve(name + '.' + ext);
		if (! Files.exists(p)) 
			return null;
		try (FileChannel fc = FileChannel.open(p, READ, WRITE, CREATE)) {
			try (FileLock lock = fc.lock()) {
				ByteBuffer buf = ByteBuffer.allocate(1024);
				StringBuilder sb = new StringBuilder();
				while (fc.read(buf) != -1) {
					sb.append(charset.decode((ByteBuffer)(buf.flip())));
					buf.clear();
				}
				s = sb.toString();
				cache.put(name, s);
			}
		}
		
		return cache.get(name);
	}

	@Override /* Store */
	public synchronized boolean write(String name, String oldContents, String newContents) throws Exception {
		dir.mkdirs();
		Path p = dir.toPath().resolve(name + '.' + ext);
		boolean exists = Files.exists(p);
		if (oldContents != null && ! exists)
			return false;
		try (FileChannel fc = FileChannel.open(p, READ, WRITE, CREATE)) {
			try (FileLock lock = fc.lock()) {
				String currentContents = null;
				if (exists) {
					ByteBuffer buf = ByteBuffer.allocate(1024);
					StringBuilder sb = new StringBuilder();
					while (fc.read(buf) != -1) {
						sb.append(charset.decode((ByteBuffer)(buf.flip())));
						buf.clear();
					}
					currentContents = sb.toString();
				}
				if (! isEquals(oldContents, currentContents)) {
					if (currentContents == null)
						cache.remove(name);
					else
						cache.put(name, currentContents);
					return false;
				}
				fc.position(0);
				fc.write(charset.encode(newContents));
				cache.put(name, newContents);
			}
		}
		return true;
	}
		
	@Override /* Store */
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
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		};
		
		@Override /* Thread */
		public void interrupt() {
			try {
				watchService.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				super.interrupt();
			}
		}
	}
	
	/**
	 * Gets called when the watcher service on this store is triggered with a file system change.
	 * 
	 * @param e The file system event.
	 * @throws Exception
	 */
	protected synchronized void onFileEvent(WatchEvent<Path> e) throws Exception {
		String fn = e.context().getFileName().toString();
		String bn = FileUtils.getBaseName(fn);
		String ext = FileUtils.getExtension(fn);
		
		if (isEquals(this.ext, ext)) {
			String oldContents = cache.get(bn);
			cache.remove(bn);
			String newContents = read(bn);
			if (! isEquals(oldContents, newContents)) {
				update(bn, newContents);
			}
		}
	}
		
}
