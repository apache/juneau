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
			ext = getStringProperty(FILESTORE_ext, "cfg");
			charset = getProperty(FILESTORE_charset, Charset.class, Charset.defaultCharset());
			watcher = getBooleanProperty(FILESTORE_useWatcher, false) ? new WatcherThread(dir) : null;
			if (watcher != null)
				watcher.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

		WatcherThread(File dir) throws IOException {
			watchService = FileSystems.getDefault().newWatchService();
			dir.toPath().register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		}
		
		@SuppressWarnings("unchecked")
		@Override /* Thread */
		public void run() {
		    try {
				while (true) {
				    WatchKey key = watchService.take();
				    
				    for (WatchEvent<?> event: key.pollEvents()) {
				        WatchEvent.Kind<?> kind = event.kind();

				        if (kind != OVERFLOW) 
				        		FileStore.this.onFileEvent(((WatchEvent<Path>)event));
				    }  
				    
				    if (! key.reset())
				    		break;
				}
			} catch (Exception e) {
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
	
	synchronized void onFileEvent(WatchEvent<Path> e) throws IOException {
		File f = e.context().toFile();
		String fn = f.getName();
		String bn = FileUtils.getBaseName(fn);
		String ext = FileUtils.getExtension(fn);
		if (ext.equals(ext)) {
			String newContents = IOUtils.read(f);
			String oldContents = cache.get(bn);
			if (! StringUtils.isEquals(oldContents, newContents)) {
				onChange(bn, newContents);
				cache.put(bn, newContents);
			}
		}
	}
		
	@Override
	public synchronized String read(String name) throws Exception {
		String s = cache.get(name);
		if (s != null)
			return s;
		
		File f = new File(dir, name + '.' + ext);
		if (f.exists()) {
			try (FileInputStream fis = new FileInputStream(f)) {
				try (FileLock lock = fis.getChannel().lock()) {
					try (Reader r = new InputStreamReader(fis, charset)) {
						String contents = IOUtils.read(r);
						cache.put(name, contents);
					}
				}
			}
		}
		
		return cache.get(name);
	}

	@Override
	public synchronized boolean write(String name, String oldContents, String newContents) throws Exception {
		File f = new File(dir, name + '.' + ext);
		try (FileChannel fc = FileChannel.open(f.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			try (FileLock lock = fc.lock()) {
				ByteBuffer buf = ByteBuffer.allocate(1024);
				StringBuilder sb = new StringBuilder();
				while (fc.read(buf) != -1) {
					sb.append(charset.decode(buf));
					sb.append(charset.decode((ByteBuffer)(buf.flip())));
					buf.clear();
				}
				String s = sb.toString();
				if (! StringUtils.isEquals(oldContents, sb.toString())) {
					cache.put(name, s);
					return false;
				}
				fc.position(0);
				fc.write(charset.encode(newContents));
				cache.put(name, newContents);
				return true;
			}
			
		}
	}
}
