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
package org.apache.juneau.rest.server.debug;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.function.*;
import java.util.logging.*;

import jakarta.servlet.http.*;

/**
 * Debug rule carrying enablement and formatting settings.
 */
public class DebugRule {

	/**
	 * Builder class.
	 */
	public static class Builder {

		boolean enabled;
		Predicate<HttpServletRequest> enabledTest;
		DebugFormat format;
		Level level;
		Boolean cacheBodies;

		/**
		 * Constructor.
		 */
		protected Builder() {
			enabled = false;
			enabledTest = x -> false;
		}

		/**
		 * Builds this object.
		 *
		 * @return A new object.
		 */
		public DebugRule build() {
			return new DebugRule(this);
		}

		/**
		 * Shortcut for always enabled.
		 *
		 * @return This object.
		 */
		public Builder always() {
			enabled = true;
			enabledTest = x -> true;
			return this;
		}

		/**
		 * Shortcut for never enabled.
		 *
		 * @return This object.
		 */
		public Builder never() {
			enabled = false;
			enabledTest = x -> false;
			return this;
		}

		/**
		 * Shortcut for conditional enablement.
		 *
		 * @param value The enablement test.
		 * @return This object.
		 */
		public Builder conditional(Predicate<HttpServletRequest> value) {
			enabled = true;
			enabledTest = value == null ? x -> false : value;
			return this;
		}

		/**
		 * Sets format.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder format(DebugFormat value) {
			format = value;
			return this;
		}

		/**
		 * Sets level.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder level(Level value) {
			level = value;
			return this;
		}

		/**
		 * Sets cache-bodies override.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder cacheBodies(Boolean value) {
			cacheBodies = value;
			return this;
		}
	}

	/**
	 * Creates a builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final boolean enabled;
	private final Predicate<HttpServletRequest> enabledTest;
	private final DebugFormat format;
	private final Level level;
	private final Boolean cacheBodies;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	protected DebugRule(Builder builder) {
		enabled = builder.enabled;
		enabledTest = builder.enabledTest;
		format = builder.format;
		level = builder.level;
		cacheBodies = builder.cacheBodies;
	}

	/**
	 * Returns whether this rule enables debug for this request.
	 *
	 * @param req The request.
	 * @return Whether debug is enabled.
	 */
	public boolean isEnabled(HttpServletRequest req) {
		return enabled && enabledTest.test(req);
	}

	/** Returns format override. */
	public DebugFormat getFormat() { return format; }

	/** Returns level override. */
	public Level getLevel() { return level; }

	/** Returns cache-body override. */
	public Boolean getCacheBodies() { return cacheBodies; }

	@Override
	public String toString() {
		return "enabled=" + enabled + ",level=" + level + ",format=" + (format == null ? null : cn(format)) + ",cacheBodies=" + cacheBodies;
	}
}
