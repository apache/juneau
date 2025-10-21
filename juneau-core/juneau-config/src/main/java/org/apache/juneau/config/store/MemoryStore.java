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

import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
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
public class MemoryStore extends ConfigStore {
	/**
	 * Builder class.
	 */
	public static class Builder extends ConfigStore.Builder {

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(MemoryStore copyFrom) {
			super(copyFrom);
			type(copyFrom.getClass());
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
		public MemoryStore build() {
			return build(MemoryStore.class);
		}

		@Override /* Overridden from Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
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
	}

	/** Default memory store, all default values.*/
	public static final MemoryStore DEFAULT = MemoryStore.create().build();

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MemoryStore(Builder builder) {
		super(builder);
	}

	/**
	 * No-op.
	 */
	@Override /* Overridden from Closeable */
	public void close() throws IOException {
		// No-op
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from ConfigStore */
	public synchronized boolean exists(String name) {
		return cache.containsKey(name);
	}

	@Override /* Overridden from ConfigStore */
	public synchronized String read(String name) {
		return emptyIfNull(cache.get(name));
	}

	@Override /* Overridden from ConfigStore */
	public synchronized MemoryStore update(String name, String newContents) {
		if (newContents == null)
			cache.remove(name);
		else
			cache.put(name, newContents);
		super.update(name, newContents);  // Trigger any listeners.
		return this;
	}

	@Override /* Overridden from ConfigStore */
	public synchronized String write(String name, String expectedContents, String newContents) {

		// This is a no-op.
		if (eq(expectedContents, newContents))
			return null;

		var currentContents = read(name);

		if (expectedContents != null && ! eq(currentContents, expectedContents))
			return currentContents;

		update(name, newContents);

		return null;
	}
}