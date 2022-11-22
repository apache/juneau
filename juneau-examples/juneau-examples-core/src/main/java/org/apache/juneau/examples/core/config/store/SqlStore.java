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
package org.apache.juneau.examples.core.config.store;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.config.store.*;

/**
 * Example of a {@link ConfigStore} that uses a relational database as a backend.
 *
 * <p>
 * This class provides a basic framework but requires implementing the following methods:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #getDatabaseValue(String)}
 * 	<li class='jm'>{@link #exists(String)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings({"resource","unused","javadoc"})
public class SqlStore extends ConfigStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	static final String
		SQLSTORE_jdbcUrl = "SqlStore.jdbcUrl",
		SQLSTORE_tableName = "SqlStore.tableName",
		SQLSTORE_nameColumn = "SqlStore.nameColumn",
		SQLSTORE_valueColumn = "SqlStore.valueColumn",
		SQLSTORE_pollInterval = "SqlStore.pollInterval";


	/**
	 * Instantiates a builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends ConfigStore.Builder {

		String jdbcUrl, tableName, nameColumn, valueColumn;
		int pollInterval;

		Builder() {
			this.jdbcUrl = env(SQLSTORE_jdbcUrl, "jdbc:derby:mydb");
			this.tableName = env(SQLSTORE_tableName, "config");
			this.nameColumn = env(SQLSTORE_nameColumn, "name");
			this.valueColumn = env(SQLSTORE_valueColumn, "value");
			this.pollInterval = env(SQLSTORE_pollInterval, 600);  // Time in seconds.
		}

		public Builder jdbcUrl(String value) {
			this.jdbcUrl = value;
			return this;
		}

		public Builder tableName(String value) {
			this.tableName = value;
			return this;
		}

		public Builder nameColumn(String value) {
			this.nameColumn = value;
			return this;
		}

		public Builder valueColumn(String value) {
			this.valueColumn = value;
			return this;
		}

		public Builder pollInterval(int value) {
			this.pollInterval = value;
			return this;
		}

		@Override
		public Builder copy() {
			return null;
		}

		@Override
		public SqlStore build() {
			return build(SqlStore.class);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String jdbcUrl;
	private final String tableName, nameColumn, valueColumn;
	private final Timer watcher;
	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected SqlStore(Builder builder) {
		super(builder);
		this.jdbcUrl = builder.jdbcUrl;
		this.tableName = builder.tableName;
		this.nameColumn = builder.nameColumn;
		this.valueColumn = builder.valueColumn;

		int pollInterval = builder.pollInterval;

		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				SqlStore.this.poll();
			}
		};

		this.watcher = new Timer("MyTimer");
		watcher.scheduleAtFixedRate(timerTask, 0, pollInterval * 1000);
	}

	synchronized void poll() {

		// Loop through all our entries and find the latest values.
		cache.forEach((name,cacheContents) -> {
			String newContents = getDatabaseValue(name);

			// Change detected!
			if (! cacheContents.equals(newContents))
				update(name, newContents);
		});
	}

	// Reads the value from the database.
	protected String getDatabaseValue(String name) {
		// Implement me!
		return null;
	}

	@Override /* ConfigStore */
	public boolean exists(String name) {
		// Implement me!
		return false;
	}

	@Override /* ConfigStore */
	public synchronized String read(String name) {
		String contents = cache.get(name);
		if (contents == null) {
			contents = getDatabaseValue(name);
			update(name, contents);
		}
		return contents;
	}

	@Override /* ConfigStore */
	public synchronized String write(String name, String expectedContents, String newContents) {

		// This is a no-op.
		if (StringUtils.eq(expectedContents, newContents))
			return null;

		String currentContents = read(name);

		if (expectedContents != null && StringUtils.ne(currentContents, expectedContents))
			return currentContents;

		update(name, newContents);

		// Success!
		return null;
	}

	@Override /* ConfigStore */
	public synchronized SqlStore update(String name, String newContents) {
		cache.put(name, newContents);
		super.update(name, newContents);  // Trigger any listeners.
		return this;
	}

	@Override /* Closeable */
	public synchronized void close() {
		if (watcher != null)
			watcher.cancel();
	}
}
