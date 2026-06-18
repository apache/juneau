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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.processor.*;

/**
 * Utility classes and methods for the {@link Mixin @Mixin} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Mixin}
 * 	<li class='ja'>{@link Rest#mixinDefs()}
 * </ul>
 *
 * @since 10.0.0
 */
public class MixinAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MixinAnnotation() {}

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for generic class-array fields.
	})
	public static class Builder extends AnnotationObject.Builder {

		Class<?> type = Object.class;
		Class<? extends RestGuard>[] guards = new Class[0];
		String roleGuard = "";
		String rolesDeclared = "";
		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends Encoder>[] encoders = new Class[0];
		Class<? extends Serializer>[] serializers = new Class[0];
		Class<?>[] parsers = {};
		Class<? extends ResponseProcessor>[] responseProcessors = new Class[0];
		Class<? extends RestOpArg>[] restOpArgs = new Class[0];
		Class<? extends CallLogger> callLogger = CallLogger.Void.class;
		Class<? extends HttpPartSerializer> partSerializer = HttpPartSerializer.Void.class;
		Class<? extends HttpPartParser> partParser = HttpPartParser.Void.class;
		Debug debug = DebugAnnotation.DEFAULT;
		String messages = "";
		String[] defaultRequestHeaders = {};
		String[] defaultResponseHeaders = {};
		String[] defaultRequestAttributes = {};
		String[] produces = {};
		String[] consumes = {};
		String defaultAccept = "";
		String defaultContentType = "";
		String defaultCharset = "";
		String maxInput = "";
		String path = "";
		String[] paths = {};
		String[] noInherit = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Mixin.class);
		}

		/**
		 * Sets the {@link Mixin#type()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(Class<?> value) { type = value; return this; }

		/**
		 * Sets the {@link Mixin#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder guards(Class<? extends RestGuard>... value) { guards = value; return this; }

		/**
		 * Sets the {@link Mixin#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) { roleGuard = value; return this; }

		/**
		 * Sets the {@link Mixin#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) { rolesDeclared = value; return this; }

		/**
		 * Sets the {@link Mixin#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder converters(Class<? extends RestConverter>... value) { converters = value; return this; }

		/**
		 * Sets the {@link Mixin#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder encoders(Class<? extends Encoder>... value) { encoders = value; return this; }

		/**
		 * Sets the {@link Mixin#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializers(Class<? extends Serializer>... value) { serializers = value; return this; }

		/**
		 * Sets the {@link Mixin#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parsers(Class<?>... value) { parsers = value; return this; }

		/**
		 * Sets the {@link Mixin#responseProcessors()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder responseProcessors(Class<? extends ResponseProcessor>... value) { responseProcessors = value; return this; }

		/**
		 * Sets the {@link Mixin#restOpArgs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder restOpArgs(Class<? extends RestOpArg>... value) { restOpArgs = value; return this; }

		/**
		 * Sets the {@link Mixin#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder callLogger(Class<? extends CallLogger> value) { callLogger = value; return this; }

		/**
		 * Sets the {@link Mixin#partSerializer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) { partSerializer = value; return this; }

		/**
		 * Sets the {@link Mixin#partParser()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) { partParser = value; return this; }

		/**
		 * Sets the {@link Mixin#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(Debug value) { debug = value == null ? DebugAnnotation.DEFAULT : value; return this; }

		/**
		 * Sets the {@link Mixin#messages()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder messages(String value) { messages = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String... value) { defaultRequestHeaders = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String... value) { defaultResponseHeaders = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String... value) { defaultRequestAttributes = value; return this; }

		/**
		 * Sets the {@link Mixin#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String... value) { produces = value; return this; }

		/**
		 * Sets the {@link Mixin#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String... value) { consumes = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) { defaultAccept = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultContentType(String value) { defaultContentType = value; return this; }

		/**
		 * Sets the {@link Mixin#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) { defaultCharset = value; return this; }

		/**
		 * Sets the {@link Mixin#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) { maxInput = value; return this; }

		/**
		 * Sets the {@link Mixin#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String value) { path = value; return this; }

		/**
		 * Sets the {@link Mixin#paths()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder paths(String... value) { paths = value; return this; }

		/**
		 * Sets the {@link Mixin#noInherit()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noInherit(String... value) { noInherit = value; return this; }

		/**
		 * Instantiates a new {@link Mixin @Mixin} object initialized with this builder.
		 *
		 * @return A new {@link Mixin @Mixin} object.
		 */
		public Mixin build() {
			return new Impl(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods.
	})
	private static class Impl extends AnnotationObject implements Mixin {

		private final Class<?> type;
		private final Class<? extends RestGuard>[] guards;
		private final String roleGuard;
		private final String rolesDeclared;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] parsers;
		private final Class<? extends ResponseProcessor>[] responseProcessors;
		private final Class<? extends RestOpArg>[] restOpArgs;
		private final Class<? extends CallLogger> callLogger;
		private final Class<? extends HttpPartSerializer> partSerializer;
		private final Class<? extends HttpPartParser> partParser;
		private final Debug debug;
		private final String messages;
		private final String[] defaultRequestHeaders;
		private final String[] defaultResponseHeaders;
		private final String[] defaultRequestAttributes;
		private final String[] produces;
		private final String[] consumes;
		private final String defaultAccept;
		private final String defaultContentType;
		private final String defaultCharset;
		private final String maxInput;
		private final String path;
		private final String[] paths;
		private final String[] noInherit;

		Impl(MixinAnnotation.Builder b) {
			super(b);
			type = b.type;
			guards = copyOf(b.guards);
			roleGuard = b.roleGuard;
			rolesDeclared = b.rolesDeclared;
			converters = copyOf(b.converters);
			encoders = copyOf(b.encoders);
			serializers = copyOf(b.serializers);
			parsers = copyOf(b.parsers);
			responseProcessors = copyOf(b.responseProcessors);
			restOpArgs = copyOf(b.restOpArgs);
			callLogger = b.callLogger;
			partSerializer = b.partSerializer;
			partParser = b.partParser;
			debug = b.debug;
			messages = b.messages;
			defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			defaultResponseHeaders = copyOf(b.defaultResponseHeaders);
			defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			produces = copyOf(b.produces);
			consumes = copyOf(b.consumes);
			defaultAccept = b.defaultAccept;
			defaultContentType = b.defaultContentType;
			defaultCharset = b.defaultCharset;
			maxInput = b.maxInput;
			path = b.path;
			paths = copyOf(b.paths);
			noInherit = copyOf(b.noInherit);
		}

		@Override /* Overridden from Mixin */ public Class<?> type() { return type; }
		@Override /* Overridden from Mixin */ public Class<? extends RestGuard>[] guards() { return guards; }
		@Override /* Overridden from Mixin */ public String roleGuard() { return roleGuard; }
		@Override /* Overridden from Mixin */ public String rolesDeclared() { return rolesDeclared; }
		@Override /* Overridden from Mixin */ public Class<? extends RestConverter>[] converters() { return converters; }
		@Override /* Overridden from Mixin */ public Class<? extends Encoder>[] encoders() { return encoders; }
		@Override /* Overridden from Mixin */ public Class<? extends Serializer>[] serializers() { return serializers; }
		@Override /* Overridden from Mixin */ public Class<?>[] parsers() { return parsers; }
		@Override /* Overridden from Mixin */ public Class<? extends ResponseProcessor>[] responseProcessors() { return responseProcessors; }
		@Override /* Overridden from Mixin */ public Class<? extends RestOpArg>[] restOpArgs() { return restOpArgs; }
		@Override /* Overridden from Mixin */ public Class<? extends CallLogger> callLogger() { return callLogger; }
		@Override /* Overridden from Mixin */ public Class<? extends HttpPartSerializer> partSerializer() { return partSerializer; }
		@Override /* Overridden from Mixin */ public Class<? extends HttpPartParser> partParser() { return partParser; }
		@Override /* Overridden from Mixin */ public Debug debug() { return debug; }
		@Override /* Overridden from Mixin */ public String messages() { return messages; }
		@Override /* Overridden from Mixin */ public String[] defaultRequestHeaders() { return defaultRequestHeaders; }
		@Override /* Overridden from Mixin */ public String[] defaultResponseHeaders() { return defaultResponseHeaders; }
		@Override /* Overridden from Mixin */ public String[] defaultRequestAttributes() { return defaultRequestAttributes; }
		@Override /* Overridden from Mixin */ public String[] produces() { return produces; }
		@Override /* Overridden from Mixin */ public String[] consumes() { return consumes; }
		@Override /* Overridden from Mixin */ public String defaultAccept() { return defaultAccept; }
		@Override /* Overridden from Mixin */ public String defaultContentType() { return defaultContentType; }
		@Override /* Overridden from Mixin */ public String defaultCharset() { return defaultCharset; }
		@Override /* Overridden from Mixin */ public String maxInput() { return maxInput; }
		@Override /* Overridden from Mixin */ public String path() { return path; }
		@Override /* Overridden from Mixin */ public String[] paths() { return paths; }
		@Override /* Overridden from Mixin */ public String[] noInherit() { return noInherit; }
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/** Default {@link Mixin} instance (empty overrides; {@code type=Object.class}). */
	public static final Mixin DEFAULT = create().build();
}
