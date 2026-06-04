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
package org.apache.juneau.rest.health;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

/**
 * Component health result returned from a {@link HealthIndicator}.
 *
 * @since 10.0.0
 */
public final class Health {

	private final String name;
	private final HealthStatus status;
	private final Map<String,Object> details;
	private final Throwable error;

	private Health(Builder b) {
		this.name = b.name;
		this.status = b.status;
		this.details = Collections.unmodifiableMap(new LinkedHashMap<>(b.details));
		this.error = b.error;
	}

	/**
	 * Creates an {@link HealthStatus#UP} builder.
	 *
	 * @param name Component name.
	 * @return A new builder.
	 */
	public static Builder up(String name) {
		return new Builder(name, HealthStatus.UP, null);
	}

	/**
	 * Creates an {@link HealthStatus#DOWN} builder.
	 *
	 * @param name Component name.
	 * @param error Optional error causing the down state.
	 * @return A new builder.
	 */
	public static Builder down(String name, Throwable error) {
		return new Builder(name, HealthStatus.DOWN, error);
	}

	/**
	 * Creates an {@link HealthStatus#UNKNOWN} builder.
	 *
	 * @param name Component name.
	 * @return A new builder.
	 */
	public static Builder unknown(String name) {
		return new Builder(name, HealthStatus.UNKNOWN, null);
	}

	/** @return Component name. */
	public String getName() { return name; }
	/** @return Health status. */
	public HealthStatus getStatus() { return status; }
	/** @return Structured details map. */
	public Map<String,Object> getDetails() { return details; }
	/** @return Optional error. */
	public Throwable getError() { return error; }

	/**
	 * Builder for {@link Health}.
	 */
	public static final class Builder {
		private final String name;
		private final HealthStatus status;
		private final Map<String,Object> details = new LinkedHashMap<>();
		private Throwable error;

		private Builder(String name, HealthStatus status, Throwable error) {
			assertArgNotNull("name", name);
			if (name.isEmpty())
				throw new IllegalArgumentException("Argument 'name' cannot be empty.");
			this.name = name;
			this.status = Objects.requireNonNull(status, "status");
			this.error = error;
		}

		/**
		 * Adds a structured detail.
		 *
		 * @param key Detail key.
		 * @param value Detail value.
		 * @return This builder.
		 */
		public Builder detail(String key, Object value) {
			assertArgNotNull("key", key);
			if (key.isEmpty())
				throw new IllegalArgumentException("Argument 'key' cannot be empty.");
			details.put(key, value);
			return this;
		}

		/**
		 * Overrides the error attached to this result.
		 *
		 * @param value The throwable.
		 * @return This builder.
		 */
		public Builder error(Throwable value) {
			error = value;
			return this;
		}

		/**
		 * Builds a health result.
		 *
		 * @return A new health result.
		 */
		public Health build() {
			return new Health(this);
		}
	}
}
