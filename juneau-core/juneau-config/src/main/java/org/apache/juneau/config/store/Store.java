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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Represents a storage location for configuration files.
 * 
 * <p>
 * Content stores require two methods to be implemented:
 * <ul>
 * 	<li class='jm'>{@link #read(String)} - Retrieve a config file.
 * 	<li class='jm'>{@link #write(String,String,String)} - Store a config file.
 * </ul>
 */
public abstract class Store extends Context implements Closeable {
	
	private final List<StoreListener> listeners = new LinkedList<>();
	
	/**
	 * Constructor.
	 * 
	 * @param ps The settings for this content store.
	 */
	protected Store(PropertyStore ps) {
		super(ps);
	}

	/**
	 * Returns the contents of the configuration file.
	 * 
	 * @param name The config file name.
	 * @return The contents of the configuration file.
	 * @throws Exception
	 */
	public abstract String read(String name) throws Exception;

	/**
	 * Saves the contents of the configuration file if the underlying storage hasn't been modified.
	 * 
	 * @param name The config file name.
	 * @param oldContents The old contents.
	 * @param newContents The new contents.
	 * @return <jk>true</jk> if we successfully saved the new configuration file contents, or <jk>false</jk> if the
	 * 	underlying storage changed since the last time the {@link #read(String)} method was called.
	 * @throws Exception
	 */
	public abstract boolean write(String name, String oldContents, String newContents) throws Exception;

	/**
	 * Registers a new listener on this store.
	 * 
	 * @param l The new listener.
	 * @return This object (for method chaining).
	 */
	public Store register(StoreListener l) {
		this.listeners.add(l);
		return this;
	}
	
	/**
	 * Unregisters a listener from this store.
	 * 
	 * @param l The listener to unregister.
	 * @return This object (for method chaining).
	 */
	public Store unregister(StoreListener l) {
		this.listeners.remove(l);
		return this;
	}

	/**
	 * Called when the physical contents of a config file have changed.
	 * 
	 * <p>
	 * Triggers calls to {@link StoreListener#onChange(String, String)} on all registered listeners.
	 * 
	 * @param name The config name (e.g. the filename without the extension).
	 * @param contents The new contents.
	 * @return This object (for method chaining).
	 */
	protected Store onChange(String name, String contents) {
		for (StoreListener l : listeners)
			l.onChange(name, contents);
		return this;
	}
	
	/**
	 * Unused.
	 */
	@Override /* Context */
	public final Session createSession(SessionArgs args) {
		throw new NoSuchMethodError();
	}

	/**
	 * Unused.
	 */
	@Override /* Context */
	public final SessionArgs createDefaultSessionArgs() {
		throw new NoSuchMethodError();
	}
}
