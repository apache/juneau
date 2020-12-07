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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.RestMethodContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestMethod @RestMethod} annotation.
 */
public class RestMethodAnnotation {


	/** Default value */
	public static final RestMethod DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for the {@link RestMethod} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends TargetedAnnotationMBuilder {

		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestMatcher>[] matchers = new Class[0];
		Class<?>[] encoders=new Class<?>[0], parsers=new Class<?>[0], serializers=new Class<?>[0];
		int priority = 0;
		Logging logging = LoggingAnnotation.DEFAULT;
		MethodSwagger swagger = MethodSwaggerAnnotation.DEFAULT;
		Property[] properties = new Property[0];
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", method="", path="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] consumes={}, defaultFormData={}, defaultQuery={}, description={}, flags={}, paths={}, produces={}, reqAttrs={}, reqHeaders={};

		/**
		 * Constructor.
		 */
		public Builder() {
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
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#defaultFormData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultFormData(String...value) {
			this.defaultFormData = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#defaultQuery()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultQuery(String...value) {
			this.defaultQuery = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder encoders(Class<?>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#flags()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder flags(String...value) {
			this.flags = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#logging()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder logging(Logging value) {
			this.logging = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#method()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder method(String value) {
			this.method = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder path(String value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#paths()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder paths(String...value) {
			this.paths = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#priority()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder priority(int value) {
			this.priority = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder properties(Property...value) {
			this.properties = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#reqAttrs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder reqAttrs(String...value) {
			this.reqAttrs = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#reqHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder reqHeaders(String...value) {
			this.reqHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializers(Class<?>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swagger(MethodSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestMethod#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMBuilder */
		public Builder on(java.lang.reflect.Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
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
		private final String clientVersion, debug, defaultAccept, defaultCharset, defaultContentType, maxInput, method, path, rolesDeclared, roleGuard, summary, value;
		private final String[] consumes, defaultFormData, defaultQuery, description, flags, paths, produces, reqAttrs, reqHeaders;

		Impl(Builder b) {
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
			this.value = b.value;
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

		@Override /* RestMethod */
		public String value() {
			return value;
		}
	}

	/**
	 * Applies {@link RestMethod} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<RestMethod> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<RestMethod> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestMethod> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			RestMethod a = ai.getAnnotation();
			MethodInfo mi = ai.getMethodOn();
			String sig = mi == null ? "Unknown" : mi.getSignature();
			String s = null;

			for (Property p1 : a.properties()) {
				psb.set(p1.name(), string(p1.value()));  // >>> DEPRECATED - Remove in 9.0 <<<
				psb.putTo(REST_properties, string(p1.name()), string(p1.value()));
			}

			for (String p1 : a.flags()) {
				psb.set(p1, true);  // >>> DEPRECATED - Remove in 9.0 <<<
				psb.putTo(REST_properties, string(p1), true);
			}

			if (a.serializers().length > 0)
				psb.set(REST_serializers, merge(ConverterUtils.toType(psb.peek(REST_serializers), Object[].class), a.serializers()));

			if (a.parsers().length > 0)
				psb.set(REST_parsers, merge(ConverterUtils.toType(psb.peek(REST_parsers), Object[].class), a.parsers()));

			if (a.encoders().length > 0)
				psb.set(REST_encoders, merge(ConverterUtils.toType(psb.peek(REST_encoders), Object[].class), a.encoders()));

			if (a.produces().length > 0)
				psb.set(REST_produces, strings(a.produces()));

			if (a.consumes().length > 0)
				psb.set(REST_consumes, strings(a.consumes()));

			for (String header : strings(a.reqHeaders())) {
				String[] h = RestUtils.parseHeader(header);
				if (h == null)
					throw new ConfigException("Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''Header-Name: header-value''", sig, header);
				if (isNotEmpty(h[1]))
					psb.putTo(REST_reqHeaders, h[0], h[1]);
			}

			if (a.defaultAccept().length() > 0) {
				s = string(a.defaultAccept());
				if (isNotEmpty(s))
					psb.putTo(REST_reqHeaders, "Accept", s);
			}

			if (a.defaultContentType().length() > 0) {
				s = string(a.defaultContentType());
				if (isNotEmpty(s))
					psb.putTo(REST_reqHeaders, "Content-Type", s);
			}

			psb.prependTo(REST_converters, a.converters());

			psb.prependTo(REST_guards, reverse(a.guards()));

			psb.prependTo(RESTMETHOD_matchers, a.matchers());

			if (! a.clientVersion().isEmpty())
				psb.set(RESTMETHOD_clientVersion, a.clientVersion());

			if (! a.defaultCharset().isEmpty())
				psb.set(REST_defaultCharset, string(a.defaultCharset()));

			if (! a.maxInput().isEmpty())
				psb.set(REST_maxInput, string(a.maxInput()));

			if (! a.maxInput().isEmpty())
				psb.set(REST_maxInput, string(a.maxInput()));

			if (! a.path().isEmpty())
				psb.prependTo(RESTMETHOD_paths, string(a.path()));
			for (String p : a.paths())
				psb.prependTo(RESTMETHOD_paths, string(p));

			if (! a.rolesDeclared().isEmpty())
				psb.addTo(REST_rolesDeclared, strings(a.rolesDeclared()));

			if (! a.roleGuard().isEmpty())
				psb.addTo(REST_roleGuard, string(a.roleGuard()));

			for (String h : a.reqHeaders()) {
				String[] h2 = RestUtils.parseKeyValuePair(string(h));
				if (h2 == null)
					throw new ConfigException(
						"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				psb.putTo(RESTMETHOD_reqHeaders, h2[0], h2[1]);
			}

			for (String ra : a.reqAttrs()) {
				String[] ra2 = RestUtils.parseKeyValuePair(string(ra));
				if (ra2 == null)
					throw new ConfigException(
						"Invalid default request attribute specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				psb.putTo(RESTMETHOD_reqAttrs, ra2[0], ra2[1]);
			}

			if (! a.defaultAccept().isEmpty())
				psb.putTo(RESTMETHOD_reqHeaders, "Accept", string(a.defaultAccept()));

			if (! a.defaultContentType().isEmpty())
				psb.putAllTo(RESTMETHOD_reqHeaders, string(a.defaultContentType()));

			for (String h : a.defaultQuery()) {
				String[] h2 = RestUtils.parseKeyValuePair(string(h));
				if (h == null)
					throw new ConfigException(
						"Invalid default query parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				psb.putTo(RESTMETHOD_defaultQuery, h2[0], h2[1]);
			}

			for (String h : a.defaultFormData()) {
				String[] h2 = RestUtils.parseKeyValuePair(string(h));
				if (h == null)
					throw new ConfigException(
						"Invalid default form data parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				psb.putTo(RESTMETHOD_defaultFormData, h2[0], h2[1]);
			}

			if (! a.method().isEmpty())
				psb.set(RESTMETHOD_httpMethod, string(a.method()));

			if (! a.value().isEmpty()) {
				String v = string(a.value()).trim();
				int i = v.indexOf(' ');
				if (i == -1) {
					psb.set(RESTMETHOD_httpMethod, v);
				} else {
					psb.set(RESTMETHOD_httpMethod, v.substring(0, i).trim());
					psb.prependTo(RESTMETHOD_paths,  v.substring(i).trim());
				}
			}

			if (a.priority() != 0)
				psb.set(RESTMETHOD_priority, a.priority());

			if (! a.debug().isEmpty())
				psb.set(RESTMETHOD_debug, string(a.debug()));

			if (! LoggingAnnotation.empty(a.logging())) {
				Logging al = a.logging();
				OMap m = new OMap(psb.peek(OMap.class, RESTMETHOD_callLoggerConfig));

				if (! al.useStackTraceHashing().isEmpty())
					m.append("useStackTraceHashing", bool(al.useStackTraceHashing()));

				if (! al.stackTraceHashingTimeout().isEmpty())
					m.append("stackTraceHashingTimeout", integer(al.stackTraceHashingTimeout(), "@Logging(stackTraceHashingTimeout)"));

				if (! al.disabled().isEmpty())
					m.append("disabled", enablement(al.disabled()));

				if (! al.level().isEmpty())
					m.append("level", level(al.level(), "@Logging(level)"));

				if (al.rules().length > 0) {
					OList ol = new OList();
					for (LoggingRule a2 : al.rules()) {
						OMap m2 = new OMap();

						if (! a2.codes().isEmpty())
							m2.append("codes", string(a2.codes()));

						if (! a2.exceptions().isEmpty())
							m2.append("exceptions", string(a2.exceptions()));

						if (! a2.debugOnly().isEmpty())
							 m2.append("debugOnly", bool(a2.debugOnly()));

						if (! a2.level().isEmpty())
							m2.append("level", level(a2.level(), "@LoggingRule(level)"));

						if (! a2.req().isEmpty())
							m2.append("req", string(a2.req()));

						if (! a2.res().isEmpty())
							m2.append("res", string(a2.res()));

						if (! a2.verbose().isEmpty())
							m2.append("verbose", bool(a2.verbose()));

						if (! a2.disabled().isEmpty())
							m2.append("disabled", bool(a2.disabled()));

						ol.add(m2);
					}
					m.put("rules", ol.appendAll(m.getList("rules")));
				}

				psb.set(RESTMETHOD_callLoggerConfig, m);
			}
		}

		private Enablement enablement(String in) {
			return Enablement.fromString(string(in));
		}

		private Level level(String in, String loc) {
			try {
				return Level.parse(string(in));
			} catch (Exception e) {
				throw new ConfigException("Invalid syntax for level on annotation @RestMethod({1}): {2}", loc, in);
			}
		}
	}

	/**
	 * A collection of {@link RestMethod @RestMethod annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		RestMethod[] value();
	}
}