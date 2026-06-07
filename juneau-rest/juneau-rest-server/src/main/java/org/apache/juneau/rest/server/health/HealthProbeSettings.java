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
package org.apache.juneau.rest.server.health;

import java.time.*;

/**
 * Settings for the built-in health probe resource.
 *
 * @since 10.0.0
 */
public class HealthProbeSettings {

	private final Duration timeout;

	private HealthProbeSettings(Builder b) {
		this.timeout = b.timeout;
	}

	/**
	 * Builder creator.
	 *
	 * @return New builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * @return Per-indicator timeout.
	 */
	public Duration getTimeout() {
		return timeout;
	}

	/**
	 * Builder for {@link HealthProbeSettings}.
	 */
	public static class Builder {
		private Duration timeout = Duration.ofSeconds(1);

		/**
		 * Sets per-indicator timeout.
		 *
		 * @param value Timeout value.
		 * @return This object.
		 */
		public Builder timeout(Duration value) {
			timeout = value == null ? Duration.ofSeconds(1) : value;
			return this;
		}

		/**
		 * Builds settings.
		 *
		 * @return Settings.
		 */
		public HealthProbeSettings build() {
			return new HealthProbeSettings(this);
		}
	}
}
