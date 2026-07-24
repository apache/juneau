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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;

/**
 * Utility classes and methods for the {@link Child @Child} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Child}
 * 	<li class='ja'>{@link Rest#childrenDefs()}
 * </ul>
 *
 * @since 10.0.0
 */
public class ChildAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ChildAnnotation() {}

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for generic class-array fields.
	})
	public static class Builder extends AnnotationObject.Builder {

		Class<?> type = Object.class;
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestConverter>[] converters = new Class[0];
		String roleGuard = "";
		String rolesDeclared = "";
		Class<? extends CallLogger> callLogger = CallLogger.Void.class;
		Class<? extends HttpPartSerializer> partSerializer = HttpPartSerializer.Void.class;
		Class<? extends HttpPartParser> partParser = HttpPartParser.Void.class;
		Debug debug = DebugAnnotation.DEFAULT;
		String defaultCharset = "";
		String maxInput = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Child.class);
		}

		/**
		 * Sets the {@link Child#type()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(Class<?> value) { type = value; return this; }

		/**
		 * Sets the {@link Child#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder guards(Class<? extends RestGuard>... value) { guards = value; return this; }

		/**
		 * Sets the {@link Child#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder converters(Class<? extends RestConverter>... value) { converters = value; return this; }

		/**
		 * Sets the {@link Child#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) { roleGuard = value; return this; }

		/**
		 * Sets the {@link Child#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) { rolesDeclared = value; return this; }

		/**
		 * Sets the {@link Child#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder callLogger(Class<? extends CallLogger> value) { callLogger = value; return this; }

		/**
		 * Sets the {@link Child#partSerializer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) { partSerializer = value; return this; }

		/**
		 * Sets the {@link Child#partParser()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) { partParser = value; return this; }

		/**
		 * Sets the {@link Child#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(Debug value) { debug = value == null ? DebugAnnotation.DEFAULT : value; return this; }

		/**
		 * Sets the {@link Child#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) { defaultCharset = value; return this; }

		/**
		 * Sets the {@link Child#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) { maxInput = value; return this; }

		/**
		 * Instantiates a new {@link Child @Child} object initialized with this builder.
		 *
		 * @return A new {@link Child @Child} object.
		 */
		public Child build() {
			return new Impl(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods.
	})
	private static class Impl extends AnnotationObject implements Child {

		private final Class<?> type;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestConverter>[] converters;
		private final String roleGuard;
		private final String rolesDeclared;
		private final Class<? extends CallLogger> callLogger;
		private final Class<? extends HttpPartSerializer> partSerializer;
		private final Class<? extends HttpPartParser> partParser;
		private final Debug debug;
		private final String defaultCharset;
		private final String maxInput;

		Impl(ChildAnnotation.Builder b) {
			super(b);
			type = b.type;
			guards = cp(b.guards);
			converters = cp(b.converters);
			roleGuard = b.roleGuard;
			rolesDeclared = b.rolesDeclared;
			callLogger = b.callLogger;
			partSerializer = b.partSerializer;
			partParser = b.partParser;
			debug = b.debug;
			defaultCharset = b.defaultCharset;
			maxInput = b.maxInput;
		}

		@Override /* Overridden from Child */ public Class<?> type() { return type; }
		@Override /* Overridden from Child */ public Class<? extends RestGuard>[] guards() { return cp(guards); }
		@Override /* Overridden from Child */ public Class<? extends RestConverter>[] converters() { return cp(converters); }
		@Override /* Overridden from Child */ public String roleGuard() { return roleGuard; }
		@Override /* Overridden from Child */ public String rolesDeclared() { return rolesDeclared; }
		@Override /* Overridden from Child */ public Class<? extends CallLogger> callLogger() { return callLogger; }
		@Override /* Overridden from Child */ public Class<? extends HttpPartSerializer> partSerializer() { return partSerializer; }
		@Override /* Overridden from Child */ public Class<? extends HttpPartParser> partParser() { return partParser; }
		@Override /* Overridden from Child */ public Debug debug() { return debug; }
		@Override /* Overridden from Child */ public String defaultCharset() { return defaultCharset; }
		@Override /* Overridden from Child */ public String maxInput() { return maxInput; }
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/** Default {@link Child} instance (empty seed slots; {@code type=Object.class}). */
	public static final Child DEFAULT = create().build();
}
