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
import org.apache.juneau.rest.*;

/**
 * Builder class for the {@link RestMethod} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class RestMethodBuilder extends TargetedAnnotationMBuilder {

	/** Default value */
	public static final RestMethod DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static RestMethodBuilder create() {
		return new RestMethodBuilder();
	}

	private static class Impl extends TargetedAnnotationImpl implements RestMethod {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<?>[] encoders, parsers, serializers;
		private final int priority;
		private final Logging logging;
		private final MethodSwagger swagger;
		private final Property[] properties;
		private final String clientVersion, debug, defaultAccept, defaultCharset, defaultContentType, maxInput, name, method, path, rolesDeclared, roleGuard, summary;
		private final String[] consumes, defaultFormData, defaultQuery, description, flags, paths, produces, reqAttrs, reqHeaders;

		Impl(RestMethodBuilder b) {
			super(b);
			this.clientVersion = b.clientVersion;
			this.consumes = copyOf(b.consumes);
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultContentType = b.defaultContentType;
			this.defaultFormData = copyOf(b.defaultFormData);
			this.defaultQuery = copyOf(b.defaultQuery);
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.flags = copyOf(b.flags);
			this.guards = copyOf(b.guards);
			this.logging = b.logging;
			this.matchers = copyOf(b.matchers);
			this.maxInput = b.maxInput;
			this.method = b.method;
			this.name = b.name;
			this.parsers = copyOf(b.parsers);
			this.path = b.path;
			this.paths = copyOf(b.paths);
			this.priority = b.priority;
			this.produces = copyOf(b.produces);
			this.properties = copyOf(b.properties);
			this.reqAttrs = copyOf(b.reqAttrs);
			this.reqHeaders = copyOf(b.reqHeaders);
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.serializers = copyOf(b.serializers);
			this.summary = b.summary;
			this.swagger = b.swagger;
			postConstruct();
		}

		@Override /* RestMethod */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestMethod */
		public String[] consumes() {
			return consumes;
		}

		@Override /* RestMethod */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestMethod */
		public String debug() {
			return debug;
		}

		@Override /* RestMethod */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestMethod */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestMethod */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* RestMethod */
		public String[] defaultFormData() {
			return defaultFormData;
		}

		@Override /* RestMethod */
		public String[] defaultQuery() {
			return defaultQuery;
		}

		@Override /* RestMethod */
		public String[] description() {
			return description;
		}

		@Override /* RestMethod */
		public Class<?>[] encoders() {
			return encoders;
		}

		@Override /* RestMethod */
		public String[] flags() {
			return flags;
		}

		@Override /* RestMethod */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestMethod */
		public Logging logging() {
			return logging;
		}

		@Override /* RestMethod */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestMethod */
		public String maxInput() {
			return maxInput;
		}

		@Override /* RestMethod */
		public String method() {
			return method;
		}

		@Override /* RestMethod */
		public String name() {
			return name;
		}

		@Override /* RestMethod */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* RestMethod */
		public String path() {
			return path;
		}

		@Override /* RestMethod */
		public String[] paths() {
			return paths;
		}

		@Override /* RestMethod */
		public int priority() {
			return priority;
		}

		@Override /* RestMethod */
		public String[] produces() {
			return produces;
		}

		@Override /* RestMethod */
		public Property[] properties() {
			return properties;
		}

		@Override /* RestMethod */
		public String[] reqAttrs() {
			return reqAttrs;
		}

		@Override /* RestMethod */
		public String[] reqHeaders() {
			return reqHeaders;
		}

		@Override /* RestMethod */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestMethod */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestMethod */
		public Class<?>[] serializers() {
			return serializers;
		}

		@Override /* RestMethod */
		public String summary() {
			return summary;
		}

		@Override /* RestMethod */
		public MethodSwagger swagger() {
			return swagger;
		}
	}


	Class<? extends RestConverter>[] converters = new Class[0];
	Class<? extends RestGuard>[] guards = new Class[0];
	Class<? extends RestMatcher>[] matchers = new Class[0];
	Class<?>[] encoders=new Class<?>[0], parsers=new Class<?>[0], serializers=new Class<?>[0];
	int priority = 0;
	Logging logging = LoggingBuilder.DEFAULT;
	MethodSwagger swagger = MethodSwaggerBuilder.DEFAULT;
	Property[] properties = new Property[0];
	String clientVersion="", debug="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", name="", method="", path="", rolesDeclared="", roleGuard="", summary="";
	String[] consumes={}, defaultFormData={}, defaultQuery={}, description={}, flags={}, paths={}, produces={}, reqAttrs={}, reqHeaders={};

	/**
	 * Constructor.
	 */
	public RestMethodBuilder() {
		super(RestMethod.class);
	}

	/**
	 * Instantiates a new {@link RestMethod @RestMethod} object initialized with this builder.
	 *
	 * @return A new {@link RestMethod @RestMethod} object.
	 */
	public RestMethod build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link RestMethod#clientVersion()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder clientVersion(String value) {
		this.clientVersion = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#consumes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder consumes(String...value) {
		this.consumes = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#converters()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder converters(Class<? extends RestConverter>...value) {
		this.converters = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#debug()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder debug(String value) {
		this.debug = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#defaultAccept()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder defaultAccept(String value) {
		this.defaultAccept = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#defaultCharset()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder defaultCharset(String value) {
		this.defaultCharset = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#defaultContentType()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder defaultContentType(String value) {
		this.defaultContentType = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#defaultFormData()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder defaultFormData(String...value) {
		this.defaultFormData = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#defaultQuery()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder defaultQuery(String...value) {
		this.defaultQuery = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#description()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#encoders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder encoders(Class<?>...value) {
		this.encoders = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#flags()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder flags(String...value) {
		this.flags = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#guards()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder guards(Class<? extends RestGuard>...value) {
		this.guards = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#logging()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder logging(Logging value) {
		this.logging = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#matchers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder matchers(Class<? extends RestMatcher>...value) {
		this.matchers = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#maxInput()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder maxInput(String value) {
		this.maxInput = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#method()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder method(String value) {
		this.method = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#name()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#parsers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder parsers(Class<?>...value) {
		this.parsers = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#path()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder path(String value) {
		this.path = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#paths()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder paths(String...value) {
		this.paths = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#priority()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder priority(int value) {
		this.priority = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#produces()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder produces(String...value) {
		this.produces = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#properties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder properties(Property...value) {
		this.properties = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#reqAttrs()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder reqAttrs(String...value) {
		this.reqAttrs = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#reqHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder reqHeaders(String...value) {
		this.reqHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#roleGuard()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder roleGuard(String value) {
		this.roleGuard = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#rolesDeclared()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder rolesDeclared(String value) {
		this.rolesDeclared = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#serializers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder serializers(Class<?>...value) {
		this.serializers = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#summary()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder summary(String value) {
		this.summary = value;
		return this;
	}

	/**
	 * Sets the {@link RestMethod#swagger()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestMethodBuilder swagger(MethodSwagger value) {
		this.swagger = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public RestMethodBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMBuilder */
	public RestMethodBuilder on(java.lang.reflect.Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}