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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Utility classes and methods for the {@link Logging @Logging} annotation.
 */
public class LoggingAnnotation {

	/** Default value */
	public static final Logging DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Logging a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Builder class for the {@link Logging} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		LoggingRule[] rules = new LoggingRule[0];
		String disabled="", level="", stackTraceHashingTimeout="", useStackTraceHashing="";

		/**
		 * Constructor.
		 */
		public Builder() {
			super(Logging.class);
		}

		/**
		 * Instantiates a new {@link Logging @Logging} object initialized with this builder.
		 *
		 * @return A new {@link Logging @Logging} object.
		 */
		public Logging build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Logging#disabled()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder disabled(String value) {
			this.disabled = value;
			return this;
		}

		/**
		 * Sets the {@link Logging#level()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder level(String value) {
			this.level = value;
			return this;
		}

		/**
		 * Sets the {@link Logging#rules()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rules(LoggingRule...value) {
			this.rules = value;
			return this;
		}

		/**
		 * Sets the {@link Logging#stackTraceHashingTimeout()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder stackTraceHashingTimeout(String value) {
			this.stackTraceHashingTimeout = value;
			return this;
		}

		/**
		 * Sets the {@link Logging#useStackTraceHashing()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder useStackTraceHashing(String value) {
			this.useStackTraceHashing = value;
			return this;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	private static class Impl extends AnnotationImpl implements Logging {

		private final LoggingRule[] rules;
		private final String disabled, level, stackTraceHashingTimeout, useStackTraceHashing;

		Impl(Builder b) {
			super(b);
			this.disabled = b.disabled;
			this.level = b.level;
			this.rules = copyOf(b.rules);
			this.stackTraceHashingTimeout = b.stackTraceHashingTimeout;
			this.useStackTraceHashing = b.useStackTraceHashing;
			postConstruct();
		}

		@Override /* Logging */
		public String disabled() {
			return disabled;
		}

		@Override /* Logging */
		public String level() {
			return level;
		}

		@Override /* Logging */
		public LoggingRule[] rules() {
			return rules;
		}

		@Override /* Logging */
		public String stackTraceHashingTimeout() {
			return stackTraceHashingTimeout;
		}

		@Override /* Logging */
		public String useStackTraceHashing() {
			return useStackTraceHashing;
		}
	}
}