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

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Filesystem-based storage location for configuration files.
 * 
 * <p>
 * Points to a file system directory containing configuration files.
 */
public class MemoryStore extends Store {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default memory store, all default values.*/
	public static final MemoryStore DEFAULT = MemoryStore.create().build();


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Create a new builder for this object.
	 * 
	 * @return A new builder for this object.
	 */
	public static MemoryStoreBuilder create() {
		return new MemoryStoreBuilder();
	}
	
	@Override /* Context */
	public MemoryStoreBuilder builder() {
		return new MemoryStoreBuilder(getPropertyStore());
	}

	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();
	
	/**
	 * Constructor.
	 * 
	 * @param ps The settings for this content store.
	 */
	protected MemoryStore(PropertyStore ps) {
		super(ps);
	}
	
	@Override /* Store */
	public synchronized String read(String name) {
		return cache.get(name);
	}

	@Override /* Store */
	public synchronized String write(String name, String expectedContents, String newContents) {
		String currentContents = read(name);
		
		if (! isEquals(currentContents, expectedContents)) 
			return StringUtils.emptyIfNull(currentContents);
		
		if (! isEquals(currentContents, newContents)) {
			cache.put(name, newContents);
			update(name, newContents);
		}
		
		return null;
	}

	
	@Override /* Store */
	public synchronized MemoryStore update(String name, String newContents) {
		cache.put(name, newContents);
		super.update(name, newContents);
		return this;
	}

	/**
	 * No-op.
	 */
	@Override /* Closeable */
	public void close() throws IOException {
		// No-op
	}
}
