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
package org.apache.juneau.rest;

import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.collections.FluentMap;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.HttpPartSerializer.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.common.utils.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestOp @RestOp}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestOpContext">RestOpContext</a>
 * </ul>
 */
@SuppressWarnings("java:S115")
public class RestOpContext extends Context implements Comparable<RestOpContext> {

	// Property name constants
	private static final String PROP_defaultRequestFormData = "defaultRequestFormData";
	private static final String PROP_defaultRequestHeaders = "defaultRequestHeaders";
	private static final String PROP_defaultRequestQueryData = "defaultRequestQueryData";
	private static final String PROP_httpMethod = "httpMethod";

	// Argument name constants for assertArgNotNull
	private static final String ARG_beanType = "beanType";
	private static final String ARG_value = "value";
	private static final String ARG_values = "values";

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Builder class.
	 */
	public static class Builder extends Context.Builder {

		private BeanContext.Builder beanContext;
		private BasicBeanStore beanStore;
		private boolean dotAll;
		private Charset defaultCharset;
		private EncoderSet.Builder encoders;
		private Enablement debug;
		private HeaderList defaultRequestHeaders;
		private HeaderList defaultResponseHeaders;
		private HttpPartParser.Creator partParser;
		private HttpPartSerializer.Creator partSerializer;
		private JsonSchemaGenerator.Builder jsonSchemaGenerator;
		private List<MediaType> consumes;
		private List<MediaType> produces;
		private List<String> path;
		private Long maxInput;
		private Method restMethod;
		private NamedAttributeMap defaultRequestAttributes;
		private ParserSet.Builder parsers;
		private PartList defaultRequestFormData;
		private PartList defaultRequestQueryData;
		private RestContext restContext;
		private RestContext.Builder parent;
		private RestConverterList.Builder converters;
		private RestGuardList.Builder guards;
		private RestMatcherList.Builder matchers;
		private SerializerSet.Builder serializers;
		private Set<String> roleGuard;
		private Set<String> rolesDeclared;
		private String clientVersion;
		private String httpMethod;

		Builder(java.lang.reflect.Method method, RestContext context) {

			this.restContext = context;
			this.parent = context.builder;
			this.restMethod = method;

			this.beanStore = BasicBeanStore.of(context.getBeanStore()).addBean(java.lang.reflect.Method.class, method);
			var ap = context.getBeanContext().getAnnotationProvider();

			var mi = MethodInfo.of(context.getResourceClass(), method);
			var resourceClass = ClassInfo.of(context.getResourceClass());

			try {
				var vr = context.getVarResolver();
				var vrs = vr.createSession();

				// For DECLARING_CLASS traversal, we need to search from the resource class hierarchy,
				// not the method's declaring class hierarchy, to ensure we find annotations on the
				// resource class even when the method is declared in a parent class.
				// We search class annotations first (parent-to-child), then method annotations (parent-to-child),
				// so that method-level annotations override class-level ones.
				// Use LinkedHashSet to deduplicate while preserving order (parent-to-child).
				var declaringClassAnnotations = rstream(ap.find(resourceClass, SELF, PARENTS));
				var methodAnnotations = rstream(ap.find(mi, SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE));
				var allAnnotationsSet = new java.util.LinkedHashSet<AnnotationInfo<?>>();
				declaringClassAnnotations.forEach(allAnnotationsSet::add);
				methodAnnotations.forEach(allAnnotationsSet::add);
				var allAnnotations = allAnnotationsSet.stream();

				var work = AnnotationWorkList.of(vrs, allAnnotations.filter(CONTEXT_APPLY_FILTER));

				apply(work);

				if (context.builder.beanContext().canApply(work))
					beanContext().apply(work);
				if (context.builder.serializers().canApply(work))
					serializers().apply(work);
				if (context.builder.parsers().canApply(work))
					parsers().apply(work);
				if (context.builder.partSerializer().canApply(work))
					partSerializer().apply(work);
				if (context.builder.partParser().canApply(work))
					partParser().apply(work);
				if (context.builder.jsonSchemaGenerator().canApply(work))
					jsonSchemaGenerator().apply(work);

				processParameterAnnotations();

			} catch (Exception e) {
				throw new InternalServerError(e);
			}
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

		/**
		 * Returns the bean context sub-builder.
		 *
		 * @return The bean context sub-builder.
		 */
		public BeanContext.Builder beanContext() {
			if (beanContext == null)
				beanContext = createBeanContext(beanStore(), parent, resource());
			return beanContext;
		}

		/**
		 * Returns access to the bean store being used by this builder.
		 *
		 * <p>
		 * Can be used to add more beans to the bean store.
		 *
		 * @return The bean store being used by this builder.
		 */
		public BasicBeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Adds a bean to the bean store of this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.beanStore().add(<jv>beanType</jv>, <jv>bean</jv>);
		 * </p>
		 *
		 * @param <T> The class to associate this bean with.
		 * @param beanType The class to associate this bean with.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @param bean The bean.
		 * 	<br>Can be <jk>null</jk> (a null bean will be stored in the bean store).
		 * @return This object.
		 */
		public <T> Builder beanStore(Class<T> beanType, T bean) {
			beanStore().addBean(assertArgNotNull(ARG_beanType, beanType), bean);
			return this;
		}

		/**
		 * Adds a bean to the bean store of this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.beanStore().add(<jv>beanType</jv>, <jv>bean</jv>, <jv>name</jv>);
		 * </p>
		 *
		 * @param <T> The class to associate this bean with.
		 * @param beanType The class to associate this bean with.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @param bean The bean.
		 * 	<br>Can be <jk>null</jk> (a null bean will be stored in the bean store).
		 * @param name The bean name if this is a named bean.
		 * 	<br>Can be <jk>null</jk> (bean will be stored as an unnamed bean).
		 * @return This object.
		 */
		public <T> Builder beanStore(Class<T> beanType, T bean, String name) {
			beanStore().addBean(assertArgNotNull(ARG_beanType, beanType), bean, name);
			return this;
		}

		@Override /* Overridden from BeanContext.Builder */
		public RestOpContext build() {
			try {
				return BeanCreator.of(RestOpContext.class, beanStore).type(getType().orElse(getDefaultImplClass())).builder(RestOpContext.Builder.class, this).run();
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		@Override /* Overridden from Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		/**
		 * Client version pattern matcher.
		 *
		 * <p>
		 * Specifies whether this method can be called based on the client version.
		 *
		 * <p>
		 * The client version is identified via the HTTP request header identified by
		 * {@link Rest#clientVersionHeader() @Rest(clientVersionHeader)} which by default is <js>"Client-Version"</js>.
		 *
		 * <p>
		 * This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same
		 * method/path based on the client version.
		 *
		 * <p>
		 * The format of the client version range is similar to that of OSGi versions.
		 *
		 * <p>
		 * In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
		 * <p class='bjava'>
		 * 	<jc>// Call this method if Client-Version is at least 2.0.
		 * 	// Note that this also matches 2.0.1.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
		 * 	<jk>public</jk> Object method1()  {...}
		 *
		 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
		 * 	<jk>public</jk> Object method2()  {...}
		 *
		 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
		 * 	<jk>public</jk> Object method3()  {...}
		 * </p>
		 *
		 * <p>
		 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
		 * backwards compatibility.
		 * <p class='bjava'>
		 * 	<jc>// Call this method if Client-Version is at least 2.0.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
		 * 	<jk>public</jk> NewPojo newMethod()  {...}
		 *
		 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
		 * 	<ja>@BeanConfig(swaps=NewToOldSwap.<jk>class</jk>)
		 * 	<jk>public</jk> NewPojo oldMethod() {
		 * 		<jk>return</jk> newMethod();
		 * 	}
		 *
		 * <p>
		 * Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into
		 * an older form.
		 * The old method could also just return back a completely different object.
		 * The range can be any of the following:
		 * <ul>
		 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
		 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
		 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link RestOp#clientVersion}
		 * 	<li class='ja'>{@link RestGet#clientVersion}
		 * 	<li class='ja'>{@link RestPut#clientVersion}
		 * 	<li class='ja'>{@link RestPost#clientVersion}
		 * 	<li class='ja'>{@link RestDelete#clientVersion}
		 * 	<li class='jm'>{@link RestContext.Builder#clientVersionHeader(String)}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			clientVersion = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Supported content media types.
		 *
		 * <p>
		 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
		 * <br>An example where this might be useful if you have parsers registered that handle media types that you
		 * don't want exposed in the Swagger documentation.
		 *
		 * <p>
		 * This affects the returned values from the following:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getConsumes() RestContext.getConsumes()}
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#consumes}
		 * 	<li class='ja'>{@link RestOp#consumes}
		 * 	<li class='ja'>{@link RestPut#consumes}
		 * 	<li class='ja'>{@link RestPost#consumes}
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder consumes(MediaType...values) {
			assertArgNoNulls(ARG_values, values);
			consumes = addAll(consumes, values);
			return this;
		}

		/**
		 * Returns the response converter list sub-builder.
		 *
		 * @return The response converter list sub-builder.
		 */
		public RestConverterList.Builder converters() {
			if (converters == null)
				converters = createConverters(beanStore(), resource());
			return converters;
		}

		/**
		 * Adds one or more converters to use to convert response objects for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.converters().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder converters(Class<? extends RestConverter>...value) {
			assertArgNoNulls(ARG_value, value);
			converters().append(value);
			return this;
		}

		/**
		 * Adds one or more converters to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.converters().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder converters(RestConverter...value) {
			assertArgNoNulls(ARG_value, value);
			converters().append(value);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
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

		/**
		 * Debug mode.
		 *
		 * <p>
		 * Enables the following:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		HTTP request/response bodies are cached in memory for logging purposes.
		 * </ul>
		 *
		 * <p>
		 * If not sppecified, the debug enablement is inherited from the class context.
		 *
		 * @param value The new value for this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder debug(Enablement value) {
			debug = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Default character encoding.
		 *
		 * <p>
		 * The default character encoding for the request and response if not specified on the request.
		 *
		 * <p>
		 * This overrides the value defined on the {@link RestContext}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link RestContext.Builder#defaultCharset(Charset)}
		 * 	<li class='ja'>{@link Rest#defaultCharset}
		 * 	<li class='ja'>{@link RestOp#defaultCharset}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.defaultCharset"
		 * 		<li>Environment variable <js>"RESTCONTEXT_defaultCharset"
		 * 		<li><js>"utf-8"</js>
		 * 	</ul>
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder defaultCharset(Charset value) {
			defaultCharset = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Returns the default classes list.
		 *
		 * <p>
		 * This defines the implementation classes for a variety of bean types.
		 *
		 * <p>
		 * Default classes are inherited from the parent REST object.
		 * Typically used on the top-level {@link RestContext.Builder} to affect class types for that REST object and all children.
		 *
		 * <p>
		 * Modifying the default class list on this builder does not affect the default class list on the parent builder, but changes made
		 * here are inherited by child builders.
		 *
		 * @return The default classes list for this builder.
		 */
		public DefaultClassList defaultClasses() {
			return restContext.builder.defaultClasses();
		}

		/**
		 * Returns the default request attributes sub-builder.
		 *
		 * @return The default request attributes sub-builder.
		 */
		public NamedAttributeMap defaultRequestAttributes() {
			if (defaultRequestAttributes == null)
				defaultRequestAttributes = createDefaultRequestAttributes(beanStore(), parent, resource());
			return defaultRequestAttributes;
		}

		/**
		 * Adds one or more default request attributes to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.defaultRequestAttributes().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(NamedAttribute...value) {
			assertArgNoNulls(ARG_value, value);
			defaultRequestAttributes().add(value);
			return this;
		}

		/**
		 * Returns the default request form data.
		 *
		 * @return The default request form data.
		 */
		public PartList defaultRequestFormData() {
			if (defaultRequestFormData == null)
				defaultRequestFormData = createDefaultRequestFormData(beanStore(), parent, resource());
			return defaultRequestFormData;
		}

		/**
		 * Adds one or more default request form data to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.defaultRequestFormData().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder defaultRequestFormData(NameValuePair...value) {
			assertArgNoNulls(ARG_value, value);
			defaultRequestFormData().append(value);
			return this;
		}

		/**
		 * Returns the default request headers.
		 *
		 * @return The default request headers.
		 */
		public HeaderList defaultRequestHeaders() {
			if (defaultRequestHeaders == null)
				defaultRequestHeaders = createDefaultRequestHeaders(beanStore(), parent, resource());
			return defaultRequestHeaders;
		}

		/**
		 * Adds one or more default request headers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.defaultRequestHeaders().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(org.apache.http.Header...value) {
			assertArgNoNulls(ARG_value, value);
			defaultRequestHeaders().append(value);
			return this;
		}

		/**
		 * Returns the default request query data.
		 *
		 * @return The default request query data.
		 */
		public PartList defaultRequestQueryData() {
			if (defaultRequestQueryData == null)
				defaultRequestQueryData = createDefaultRequestQueryData(beanStore(), parent, resource());
			return defaultRequestQueryData;
		}

		/**
		 * Adds one or more default request query data to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.defaultRequestQueryData().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(NameValuePair...value) {
			assertArgNoNulls(ARG_value, value);
			defaultRequestQueryData().append(value);
			return this;
		}

		/**
		 * Returns the default response headers.
		 *
		 * @return The default response headers.
		 */
		public HeaderList defaultResponseHeaders() {
			if (defaultResponseHeaders == null)
				defaultResponseHeaders = createDefaultResponseHeaders(beanStore(), parent, resource());
			return defaultResponseHeaders;
		}

		/**
		 * Adds one or more default response headers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.defaultResponseHeaders().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(org.apache.http.Header...value) {
			assertArgNoNulls(ARG_value, value);
			defaultResponseHeaders().append(value);
			return this;
		}

		/**
		 * When enabled, append <js>"/*"</js> to path patterns if not already present.
		 *
		 * @return This object.
		 */
		public Builder dotAll() {
			dotAll = true;
			return this;
		}

		/**
		 * Returns the encoder group sub-builder.
		 *
		 * @return The encoder group sub-builder.
		 */
		public EncoderSet.Builder encoders() {
			if (encoders == null)
				encoders = createEncoders(beanStore(), parent, resource());
			return encoders;
		}

		/**
		 * Adds one or more encoders to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.encoders().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder encoders(Class<? extends Encoder>...value) {
			assertArgNoNulls(ARG_value, value);
			encoders().add(value);
			return this;
		}

		/**
		 * Adds one or more encoders to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.encoders().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder encoders(Encoder...value) {
			assertArgNoNulls(ARG_value, value);
			encoders().add(value);
			return this;
		}

		/**
		 * Returns the guard list sub-builder.
		 *
		 * @return The guard list sub-builder.
		 */
		public RestGuardList.Builder guards() {
			if (guards == null)
				guards = createGuards(beanStore(), resource());
			return guards;
		}

		/**
		 * Adds one or more guards to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.guards().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder guards(Class<? extends RestGuard>...value) {
			assertArgNoNulls(ARG_value, value);
			guards().append(value);
			return this;
		}

		/**
		 * Adds one or more guards to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.guards().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder guards(RestGuard...value) {
			assertArgNoNulls(ARG_value, value);
			guards().append(value);
			return this;
		}

		/**
		 * HTTP method name.
		 *
		 * <p>
		 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
		 *
		 * <p>
		 * Method names are case-insensitive (always folded to upper-case).
		 *
		 * <p>
		 * Note that you can use {@link org.apache.juneau.http.HttpMethod} for constant values.
		 *
		 * <p>
		 * Besides the standard HTTP method names, the following can also be specified:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"*"</js>
		 * 		- Denotes any method.
		 * 		<br>Use this if you want to capture any HTTP methods in a single Java method.
		 * 		<br>The {@link org.apache.juneau.rest.annotation.Method @Method} annotation and/or {@link RestRequest#getMethod()} method can be used to
		 * 		distinguish the actual HTTP method name.
		 * 	<li>
		 * 		<js>""</js>
		 * 		- Auto-detect.
		 * 		<br>The method name is determined based on the Java method name.
		 * 		<br>For example, if the method is <c>doPost(...)</c>, then the method name is automatically detected
		 * 		as <js>"POST"</js>.
		 * 		<br>Otherwise, defaults to <js>"GET"</js>.
		 * 	<li>
		 * 		<js>"RRPC"</js>
		 * 		- Remote-proxy interface.
		 * 		<br>This denotes a Java method that returns an object (usually an interface, often annotated with the
		 * 		{@link Remote @Remote} annotation) to be used as a remote proxy using
		 * 		<c>RestClient.getRemoteInterface(Class&lt;T&gt; interfaceClass, String url)</c>.
		 * 		<br>This allows you to construct client-side interface proxies using REST as a transport medium.
		 * 		<br>Conceptually, this is simply a fancy <c>POST</c> against the url <js>"/{path}/{javaMethodName}"</js>
		 * 		where the arguments are marshalled from the client to the server as an HTTP content containing an array of
		 * 		objects, passed to the method as arguments, and then the resulting object is marshalled back to the client.
		 * 	<li>
		 * 		Anything else
		 * 		- Overloaded non-HTTP-standard names that are passed in through a <c>&amp;method=methodName</c> URL
		 * 		parameter.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link RestOp#method()}
		 * 	<li class='ja'>{@link RestGet}
		 * 	<li class='ja'>{@link RestPut}
		 * 	<li class='ja'>{@link RestPost}
		 * 	<li class='ja'>{@link RestDelete}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpMethod(String value) {
			httpMethod = assertArgNotNull(ARG_value, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		/**
		 * Returns the JSON schema generator sub-builder.
		 *
		 * @return The JSON schema generator sub-builder.
		 */
		public JsonSchemaGenerator.Builder jsonSchemaGenerator() {
			if (jsonSchemaGenerator == null)
				jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), parent, resource());
			return jsonSchemaGenerator;
		}

		/**
		 * Specifies the JSON schema generator for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.jsonSchemaGenerator().type(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jsonSchemaGenerator(Class<? extends JsonSchemaGenerator> value) {
			jsonSchemaGenerator().type(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Specifies the JSON schema generator for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.jsonSchemaGenerator().impl(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jsonSchemaGenerator(JsonSchemaGenerator value) {
			jsonSchemaGenerator().impl(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Returns the matcher list sub-builder.
		 *
		 * @return The matcher list sub-builder.
		 */
		public RestMatcherList.Builder matchers() {
			if (matchers == null)
				matchers = createMatchers(beanStore(), resource());
			return matchers;
		}

		/**
		 * Adds one or more matchers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.matchers().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder matchers(Class<? extends RestMatcher>...value) {
			assertArgNoNulls(ARG_value, value);
			matchers().append(value);
			return this;
		}

		/**
		 * Adds one or more matchers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.matchers().append(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder matchers(RestMatcher...value) {
			assertArgNoNulls(ARG_value, value);
			matchers().append(value);
			return this;
		}

		/**
		 * The maximum allowed input size (in bytes) on HTTP requests.
		 *
		 * <p>
		 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
		 * in out-of-memory errors which could affect system stability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(maxInput=<js>"$C{REST/maxInput,10M}"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(RestContext.Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestInit</ja>
		 * 		<jk>public void</jk> init(RestContext.Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
		 * 		}
		 *
		 * 		<jc>// Override at the method level.</jc>
		 * 		<ja>@RestPost</ja>(maxInput=<js>"10M"</js>)
		 * 		<jk>public</jk> Object myMethod() {...}
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		String value that gets resolved to a <jk>long</jk>.
		 * 	<li class='note'>
		 * 		Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:
		 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
		 * 	<li class='note'>
		 * 		A value of <js>"-1"</js> can be used to represent no limit.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#maxInput}
		 * 	<li class='ja'>{@link RestOp#maxInput}
		 * 	<li class='jm'>{@link RestOpContext.Builder#maxInput(String)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.maxInput"
		 * 		<li>Environment variable <js>"RESTCONTEXT_MAXINPUT"
		 * 		<li><js>"100M"</js>
		 * 	</ul>
		 * 	<br>The default is <js>"100M"</js>.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder maxInput(String value) {
			maxInput = parseLongWithSuffix(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Returns the parser group sub-builder.
		 *
		 * @return The parser group sub-builder.
		 */
		public ParserSet.Builder parsers() {
			if (parsers == null)
				parsers = createParsers(beanStore(), parent, resource());
			return parsers;
		}

		/**
		 * Adds one or more parsers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.parsers().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder parsers(Class<? extends Parser>...value) {
			assertArgNoNulls(ARG_value, value);
			parsers().add(value);
			return this;
		}

		/**
		 * Adds one or more parsers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.parsers().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder parsers(Parser...value) {
			assertArgNoNulls(ARG_value, value);
			parsers().add(value);
			return this;
		}

		/**
		 * Returns the part parser sub-builder.
		 *
		 * @return The part parser sub-builder.
		 */
		public HttpPartParser.Creator partParser() {
			if (partParser == null)
				partParser = createPartParser(beanStore(), parent, resource());
			return partParser;
		}

		/**
		 * Specifies the part parser to use for parsing HTTP parts for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.partParser().type(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) {
			partParser().type(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Specifies the part parser to use for parsing HTTP parts for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.partParser().impl(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder partParser(HttpPartParser value) {
			partParser().impl(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Returns the part serializer sub-builder.
		 *
		 * @return The part serializer sub-builder.
		 */
		public HttpPartSerializer.Creator partSerializer() {
			if (partSerializer == null)
				partSerializer = createPartSerializer(beanStore(), parent, resource());
			return partSerializer;
		}

		/**
		 * Specifies the part serializer to use for serializing HTTP parts for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.partSerializer().type(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) {
			partSerializer().type(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Specifies the part serializer to use for serializing HTTP parts for this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.partSerializer().impl(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The new value.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder partSerializer(HttpPartSerializer value) {
			partSerializer().impl(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Resource method paths.
		 *
		 * <p>
		 * Identifies the URL subpath relative to the servlet class.
		 *
		 * <p>
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		This method is only applicable for Java methods.
		 * 	<li class='note'>
		 * 		Slashes are trimmed from the path ends.
		 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
		 * </ul>
		 *
		 * @param values The new values for this setting.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder path(String...values) {
			assertArgNoNulls(ARG_values, values);
			path = prependAll(path, values);
			return this;
		}

		/**
		 * Supported accept media types.
		 *
		 * <p>
		 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
		 * <br>An example where this might be useful if you have serializers registered that handle media types that you
		 * don't want exposed in the Swagger documentation.
		 *
		 * <p>
		 * This affects the returned values from the following:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getProduces() RestContext.getProduces()}
		 * 	<li class='jm'>{@link SwaggerProvider#getSwagger(RestContext,Locale)} - Affects produces field.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#produces}
		 * 	<li class='ja'>{@link RestOp#produces}
		 * 	<li class='ja'>{@link RestGet#produces}
		 * 	<li class='ja'>{@link RestPut#produces}
		 * 	<li class='ja'>{@link RestPost#produces}
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder produces(MediaType...values) {
			assertArgNoNulls(ARG_values, values);
			produces = addAll(produces, values);
			return this;
		}

		/**
		 * Returns the REST servlet/bean instance that this context is defined against.
		 *
		 * @return The REST servlet/bean instance that this context is defined against.
		 */
		public Supplier<?> resource() {
			return restContext.builder.resource();
		}

		/**
		 * Role guard.
		 *
		 * <p>
		 * An expression defining if a user with the specified roles are allowed to access methods on this class.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/foo"</js>,
		 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
		 * 		...
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Supports any of the following expression constructs:
		 * 		<ul>
		 * 			<li><js>"foo"</js> - Single arguments.
		 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
		 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
		 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
		 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
		 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
		 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
		 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
		 * 		</ul>
		 * 	<li class='note'>
		 * 		AND operations take precedence over OR operations (as expected).
		 * 	<li class='note'>
		 * 		Whitespace is ignored.
		 * 	<li class='note'>
		 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
		 * 	<li class='note'>
		 * 		If patterns are used, you must specify the list of declared roles using {@link Rest#rolesDeclared()} or {@link RestOpContext.Builder#rolesDeclared(String...)}.
		 * 	<li class='note'>
		 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
		 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
		 * </ul>
		 *
		 * @param value The values to add to this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			if (roleGuard == null)
				roleGuard = set(assertArgNotNull(ARG_value, value));
			else
				roleGuard.add(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Declared roles.
		 *
		 * <p>
		 * A comma-delimited list of all possible user roles.
		 *
		 * <p>
		 * Used in conjunction with {@link RestOpContext.Builder#roleGuard(String)} is used with patterns.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<ja>@Rest</ja>(
		 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
		 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
		 * 		...
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#rolesDeclared}
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder rolesDeclared(String...values) {
			assertArgNoNulls(ARG_values, values);
			rolesDeclared = addAll(rolesDeclared, values);
			return this;
		}

		/**
		 * Returns the serializer group sub-builder.
		 *
		 * @return The serializer group sub-builder.
		 */
		public SerializerSet.Builder serializers() {
			if (serializers == null)
				serializers = createSerializers(beanStore(), parent, resource());
			return serializers;
		}

		/**
		 * Adds one or more serializers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.serializers().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder serializers(Class<? extends Serializer>...value) {
			assertArgNoNulls(ARG_value, value);
			serializers().add(value);
			return this;
		}

		/**
		 * Adds one or more serializers to this operation.
		 *
		 * <p>
		 * Equivalent to calling:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.serializers().add(<jv>value</jv>);
		 * </p>
		 *
		 * @param value The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder serializers(Serializer...value) {
			assertArgNoNulls(ARG_value, value);
			serializers().add(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		private static String joinnlFirstNonEmptyArray(String[]...s) {
			for (var ss : s)
				if (ss.length > 0)
					return joinnl(ss);
			return null;
		}

		private boolean matches(MethodInfo annotated) {
			var a = annotated.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null);
			if (nn(a)) {
				for (var n : a.methodScope()) {
					if ("*".equals(n) || restMethod.getName().equals(n))
						return true;
				}
			}
			return false;
		}

		private boolean matches(MethodInfo annotated, String beanName) {
			var a = annotated.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null);
			if (nn(a)) {
				if (! a.name().equals(beanName))
					return false;
				for (var n : a.methodScope()) {
					if ("*".equals(n) || restMethod.getName().equals(n))
						return true;
				}
			}
			return false;
		}

		/**
		 * Specifies a {@link BasicBeanStore} to use when resolving constructor arguments.
		 *
		 * @param beanStore The bean store to use for resolving constructor arguments.
		 * @return This object.
		 */
		protected Builder beanStore(BasicBeanStore beanStore) {
			this.beanStore = beanStore;
			return this;
		}

		/**
		 * Instantiates the bean context sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new bean context sub-builder.
		 */
		protected BeanContext.Builder createBeanContext(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<BeanContext.Builder> v = Value.of(parent.beanContext().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] BeanContext xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(BeanContext.Builder.class, v.get());
			new BeanCreateMethodFinder<>(BeanContext.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the response converter list sub-builder.
		 *
		 * <p>
		 * Associates one or more {@link RestConverter converters} with a resource class.
		 * <br>These converters get called immediately after execution of the REST method in the same order specified in the
		 * annotation.
		 * <br>The object passed into this converter is the object returned from the Java method or passed into
		 * the {@link RestResponse#setContent(Object)} method.
		 *
		 * <p>
		 * Can be used for performing post-processing on the response object before serialization.
		 *
		 * <p>
		 * 	When multiple converters are specified, they're executed in the order they're specified in the annotation
		 * 	(e.g. first the results will be traversed, then the resulting node will be searched/sorted).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Our converter.</jc>
		 * 	<jk>public class</jk> MyConverter <jk>implements</jk> RestConverter {
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> Object convert(RestRequest <jv>req</jv>, Object <jv>object</jv>) {
		 * 			<jc>// Do something with object and return another object.</jc>
		 * 			<jc>// Or just return the same object for a no-op.</jc>
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(converters={MyConverter.<jk>class</jk>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(RestContext.Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.converters(MyConverter.<jk>class</jk>);
		 *
		 * 			<jc>// Pass in an instance instead.</jc>
		 * 			<jv>builder</jv>.converters(<jk>new</jk> MyConverter());
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestInit</ja>
		 * 		<jk>public void</jk> init(RestContext.Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.converters(MyConverter.<jk>class</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		When defined as a class, the implementation must have one of the following constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(BeanContext)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
		 * 		</ul>
		 * 	<li class='note'>
		 * 		Inner classes of the REST resource class are allowed.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jc'>{@link Traversable} - Allows URL additional path info to address individual elements in a POJO tree.
		 * 	<li class='jc'>{@link Queryable} - Allows query/view/sort functions to be performed on POJOs.
		 * 	<li class='jc'>{@link Introspectable} - Allows Java public methods to be invoked on the returned POJOs.
		 * 	<li class='ja'>{@link Rest#converters()}
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new response converter list sub-builder.
		 */
		protected RestConverterList.Builder createConverters(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestConverterList.Builder> v = Value.of(RestConverterList.create(beanStore));

			// Specify the implementation class if its set as a default.
			defaultClasses().get(RestConverterList.class).ifPresent(x -> v.get().type(x));

			// Replace with bean from bean store.
			beanStore.getBean(RestConverterList.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] RestConverterList xxx(<args>)
			new BeanCreateMethodFinder<>(RestConverterList.class, resource.get(), beanStore).addBean(RestConverterList.Builder.class, v.get()).find(this::matches).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the default request attributes sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request attributes sub-builder.
		 */
		protected NamedAttributeMap createDefaultRequestAttributes(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			var v = Value.of(parent.defaultRequestAttributes().copy());

			// Replace with bean from:  @RestInject(name="defaultRequestAttributes",methodScope="foo") public [static] NamedAttributeMap xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(NamedAttributeMap.class, v.get());
			new BeanCreateMethodFinder<>(NamedAttributeMap.class, resource, bs)
				.find(x -> matches(x, "defaultRequestAttributes"))
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the default request form data.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request form data sub-builder.
		 */
		protected PartList createDefaultRequestFormData(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			var v = Value.of(PartList.create());

			// Replace with bean from:  @RestInject(name="defaultRequestFormData",methodScope="foo") public [static] PartList xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(PartList.class, v.get());
			new BeanCreateMethodFinder<>(PartList.class, resource, bs)
				.find(x -> matches(x, "defaultRequestFormData"))
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the default request headers.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request headers sub-builder.
		 */
		protected HeaderList createDefaultRequestHeaders(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			var v = Value.of(parent.defaultRequestHeaders().copy());

			// Replace with bean from:  @RestInject(name="defaultRequestHeaders",methodScope="foo") public [static] HeaderList xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(HeaderList.class, v.get());
			new BeanCreateMethodFinder<>(HeaderList.class, resource, bs)
				.find(x -> matches(x, "defaultRequestHeaders"))
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the default request query data.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request query data sub-builder.
		 */
		protected PartList createDefaultRequestQueryData(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			var v = Value.of(PartList.create());

			// Replace with bean from:  @RestInject(name="defaultRequestQueryData",methodScope="foo") public [static] PartList xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(PartList.class, v.get());
			new BeanCreateMethodFinder<>(PartList.class, resource, bs)
				.find(x -> matches(x, "defaultRequestQueryData"))
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the default response headers.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default response headers sub-builder.
		 */
		protected HeaderList createDefaultResponseHeaders(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			var v = Value.of(parent.defaultResponseHeaders().copy());

			// Replace with bean from:  @RestInject(name="defaultResponseHeaders",methodScope="foo") public [static] HeaderList xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(HeaderList.class, v.get());
			new BeanCreateMethodFinder<>(HeaderList.class, resource, bs)
				.find(x -> matches(x, "defaultResponseHeaders"))
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the encoder group sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new encoder group sub-builder.
		 */
		protected EncoderSet.Builder createEncoders(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<EncoderSet.Builder> v = Value.of(parent.encoders().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] EncoderSet xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(EncoderSet.Builder.class, v.get());
			new BeanCreateMethodFinder<>(EncoderSet.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the guard list sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for guards set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestOpContext.Builder#guards()}}
		 * 			<li>{@link RestOp#guards()}.
		 * 			<li>{@link Rest#guards()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createGuards()</c> method that returns <c>{@link RestGuard}[]</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link Method} - The Java method this context belongs to.
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BasicBeanStore}
		 * 			<li>Any <a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a <c>RestGuard[0]</c>.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new guard list sub-builder.
		 */
		protected RestGuardList.Builder createGuards(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestGuardList.Builder> v = Value.of(RestGuardList.create(beanStore));

			// Specify the implementation class if its set as a default.
			defaultClasses().get(RestGuardList.class).ifPresent(x -> v.get().type(x));

			// Replace with bean from bean store.
			beanStore.getBean(RestGuardList.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] RestGuardList xxx(<args>)
			// @formatter:off
			new BeanCreateMethodFinder<>(RestGuardList.class, resource.get(), beanStore)
				.addBean(RestGuardList.Builder.class, v.get())
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the JSON schema generator sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new JSON schema generator sub-builder.
		 */
		protected JsonSchemaGenerator.Builder createJsonSchemaGenerator(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<JsonSchemaGenerator.Builder> v = Value.of(parent.jsonSchemaGenerator().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] JsonSchemaGenerator xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(JsonSchemaGenerator.Builder.class, v.get());
			new BeanCreateMethodFinder<>(JsonSchemaGenerator.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the matcher list sub-builder.
		 *
		 * <p>
		 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
		 *
		 * <p>
		 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
		 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		When defined as a class, the implementation must have one of the following constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(RestContext)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
		 * 		</ul>
		 * 	<li class='note'>
		 * 		Inner classes of the REST resource class are allowed.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link RestOp#matchers()}
		 * 	<li class='ja'>{@link RestGet#matchers()}
		 * 	<li class='ja'>{@link RestPut#matchers()}
		 * 	<li class='ja'>{@link RestPost#matchers()}
		 * 	<li class='ja'>{@link RestDelete#matchers()}
		 * </ul>
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for matchers set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestOp#matchers()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createMatchers()</c> method that returns <c>{@link RestMatcher}[]</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link java.lang.reflect.Method} - The Java method this context belongs to.
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BasicBeanStore}
		 * 			<li>Any <a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a <c>RestMatcher[0]</c>.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new matcher list sub-builder.
		 */
		protected RestMatcherList.Builder createMatchers(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestMatcherList.Builder> v = Value.of(RestMatcherList.create(beanStore));

			// Specify the implementation class if its set as a default.
			defaultClasses().get(RestMatcherList.class).ifPresent(x -> v.get().type(x));

			// Replace with bean from bean store.
			beanStore.getBean(RestMatcherList.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] RestMatcherList xxx(<args>)
			// @formatter:off
			new BeanCreateMethodFinder<>(RestMatcherList.class, resource.get(), beanStore)
				.addBean(RestMatcherList.Builder.class, v.get())
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the parser group sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new parser group sub-builder.
		 */
		protected ParserSet.Builder createParsers(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<ParserSet.Builder> v = Value.of(parent.parsers().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] ParserSet xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(ParserSet.Builder.class, v.get());
			new BeanCreateMethodFinder<>(ParserSet.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the part parser sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part parser sub-builder.
		 */
		protected HttpPartParser.Creator createPartParser(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<HttpPartParser.Creator> v = Value.of(parent.partParser().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] HttpPartParser xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(HttpPartParser.Creator.class, v.get());
			new BeanCreateMethodFinder<>(HttpPartParser.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the part serializer sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part serializer sub-builder.
		 */
		protected HttpPartSerializer.Creator createPartSerializer(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<HttpPartSerializer.Creator> v = Value.of(parent.partSerializer().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] HttpPartSerializer xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(HttpPartSerializer.Creator.class, v.get());
			new BeanCreateMethodFinder<>(HttpPartSerializer.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Instantiates the serializer group sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param parent
		 * 	The builder for the REST resource class.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new serializer group sub-builder.
		 */
		protected SerializerSet.Builder createSerializers(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {

			// Default value.
			Value<SerializerSet.Builder> v = Value.of(parent.serializers().copy());

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] SerializerSet xxx(<args>)
			// @formatter:off
			var bs = BasicBeanStore.of(beanStore).addBean(SerializerSet.Builder.class, v.get());
			new BeanCreateMethodFinder<>(SerializerSet.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Specifies the default implementation class if not specified via {@link #type(Class)}.
		 *
		 * @return The default implementation class if not specified via {@link #type(Class)}.
		 */
		protected Class<? extends RestOpContext> getDefaultImplClass() { return RestOpContext.class; }

		/**
		 * Instantiates the path matchers for this method.
		 *
		 * @return The path matchers for this method.
		 */
		@SuppressWarnings("java:S3776")
		protected UrlPathMatcherList getPathMatchers() {

			var v = Value.of(UrlPathMatcherList.create());

			if (nn(path)) {
				for (var p : path) {
					if (dotAll && ! p.endsWith("/*"))
						p += "/*";
					v.get().add(UrlPathMatcher.of(p));
				}
			}

			if (v.get().isEmpty()) {
				var mi = MethodInfo.of(restMethod);
				String p = null;
				String httpMethod2 = null;
				if (mi.hasAnnotation(RestGet.class))
					httpMethod2 = "get";
				else if (mi.hasAnnotation(RestPut.class))
					httpMethod2 = "put";
				else if (mi.hasAnnotation(RestPost.class))
					httpMethod2 = "post";
				else if (mi.hasAnnotation(RestDelete.class))
					httpMethod2 = "delete";
				else if (mi.hasAnnotation(RestOp.class)) {
					// @formatter:off
					httpMethod2 = AP.find(RestOp.class, mi)
						.stream()
						.map(x -> x.inner().method())
						.filter(Utils::ne)
						.findFirst()
						.orElse(null);
					// @formatter:on
				}

				p = HttpUtils.detectHttpPath(restMethod, httpMethod2);

				if (dotAll && ! p.endsWith("/*"))
					p += "/*";

				v.get().add(UrlPathMatcher.of(p));
			}

			// Replace with bean from:  @RestInject(methodScope="foo") public [static] UrlPathMatcherList xxx(<args>)
			// @formatter:off
			new BeanCreateMethodFinder<>(UrlPathMatcherList.class, resource().get(), beanStore())
				.addBean(UrlPathMatcherList.class, v.get())
				.find(this::matches)
				.run(v::set);
			// @formatter:on

			return v.get();
		}

		/**
		 * Handles processing of any annotations on parameters.
		 *
		 * <p>
		 * This includes: {@link Header}, {@link Query}, {@link FormData}.
		 */
		@SuppressWarnings("java:S3776")
		protected void processParameterAnnotations() {
			for (var aa : restMethod.getParameterAnnotations()) {

				String def = null;
				for (var a : aa) {
					if (a instanceof Schema a2) {
						def = joinnlFirstNonEmptyArray(a2.default_(), a2.df());
					}
				}

				for (var a : aa) {
					if (a instanceof Header a2 && nn(def)) {
						try {
							defaultRequestHeaders().set(basicHeader(firstNonEmpty(a2.name(), a2.value()), parseIfJson(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Header annotation");
						}
					}
					if (a instanceof Query a2 && nn(def)) {
						try {
							defaultRequestQueryData().setDefault(basicPart(firstNonEmpty(a2.name(), a2.value()), parseIfJson(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Query annotation");
						}
					}
					if (a instanceof FormData a2 && nn(def)) {
						try {
							defaultRequestFormData().setDefault(basicPart(firstNonEmpty(a2.name(), a2.value()), parseIfJson(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @FormData annotation");
						}
					}
				}
			}
		}

		Optional<BeanContext> getBeanContext() { return opt(beanContext).map(BeanContext.Builder::build); }

		Optional<EncoderSet> getEncoders() { return opt(encoders).map(EncoderSet.Builder::build); }

		RestGuardList getGuards() {
			var b = guards();
			var roleGuard2 = opt(this.roleGuard).orElseGet(CollectionUtils::set);

			for (var rg : roleGuard2) {
				try {
					b.append(new RoleBasedRestGuard(rolesDeclared, rg));
				} catch (java.text.ParseException e1) {
					throw toRex(e1);
				}
			}

			return guards.build();
		}

		Optional<JsonSchemaGenerator> getJsonSchemaGenerator() { return opt(jsonSchemaGenerator).map(JsonSchemaGenerator.Builder::build); }

		RestMatcherList getMatchers(RestContext restContext) {
			RestMatcherList.Builder b = matchers();
			if (nn(clientVersion))
				b.append(new ClientVersionMatcher(restContext.getClientVersionHeader(), MethodInfo.of(restMethod)));

			return b.build();
		}

		Optional<ParserSet> getParsers() { return opt(parsers).map(ParserSet.Builder::build); }

		Optional<HttpPartParser> getPartParser() { return opt(partParser).map(org.apache.juneau.httppart.HttpPartParser.Creator::create); }

		Optional<HttpPartSerializer> getPartSerializer() { return opt(partSerializer).map(Creator::create); }

		Optional<SerializerSet> getSerializers() { return opt(serializers).map(SerializerSet.Builder::build); }

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param method The Java method this context belongs to.
	 * @param context The Java class context.
	 * @return A new builder.
	 */
	public static Builder create(java.lang.reflect.Method method, RestContext context) {
		return new Builder(method, context);
	}

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, HttpPartSerializer _default) {
		return BeanCreator.of(HttpPartSerializer.class).type(c).orElse(_default);
	}

	protected final boolean dotAll;
	protected final int hierarchyDepth;
	protected final long maxInput;
	protected final BeanContext beanContext;
	protected final CallLogger callLogger;
	protected final Charset defaultCharset;
	protected final DebugEnablement debug;
	protected final EncoderSet encoders;
	protected final HeaderList defaultRequestHeaders;
	protected final HeaderList defaultResponseHeaders;
	protected final HttpPartParser partParser;
	protected final HttpPartSerializer partSerializer;
	protected final JsonSchemaGenerator jsonSchemaGenerator;
	protected final List<MediaType> supportedAcceptTypes;
	protected final List<MediaType> supportedContentTypes;
	protected final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	protected final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	protected final Method method;
	protected final MethodInfo mi;
	protected final NamedAttributeMap defaultRequestAttributes;
	protected final PartList defaultRequestFormData;
	protected final PartList defaultRequestQueryData;
	protected final ParserSet parsers;
	protected final RestContext context;
	protected final RestConverter[] converters;
	protected final RestGuard[] guards;
	protected final RestMatcher[] optionalMatchers;
	protected final RestMatcher[] requiredMatchers;
	protected final RestOpInvoker methodInvoker;
	protected final RestOpInvoker[] postCallMethods;
	protected final RestOpInvoker[] preCallMethods;
	protected final ResponseBeanMeta responseMeta;
	protected final SerializerSet serializers;
	protected final String httpMethod;
	protected final UrlPathMatcher[] pathMatchers;

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	protected RestOpContext(Builder builder) throws ServletException {
		super(builder);

		try {
			context = builder.restContext;
			method = builder.restMethod;

			if (builder.debug == null)
				debug = context.getDebugEnablement();
			else
				debug = DebugEnablement.create(context.getBeanStore()).enable(builder.debug, "*").build();

			mi = MethodInfo.of(method).accessible();

			// @formatter:off
			var bs = BasicBeanStore.of(context.getRootBeanStore())
				.addBean(RestOpContext.class, this)
				.addBean(Method.class, method)
				.addBean(AnnotationWorkList.class, builder.getApplied());
			// @formatter:on
			bs.addBean(BasicBeanStore.class, bs);

			beanContext = bs.add(BeanContext.class, builder.getBeanContext().orElse(context.getBeanContext()));
			converters = bs.add(RestConverter[].class, builder.converters().build().asArray());
			encoders = bs.add(EncoderSet.class, builder.getEncoders().orElse(context.getEncoders()));
			guards = bs.add(RestGuard[].class, builder.getGuards().asArray());
			jsonSchemaGenerator = bs.add(JsonSchemaGenerator.class, builder.getJsonSchemaGenerator().orElse(context.getJsonSchemaGenerator()));
			parsers = bs.add(ParserSet.class, builder.getParsers().orElse(context.getParsers()));
			partParser = bs.add(HttpPartParser.class, builder.getPartParser().orElse(context.getPartParser()));
			partSerializer = bs.add(HttpPartSerializer.class, builder.getPartSerializer().orElse(context.getPartSerializer()));
			serializers = bs.add(SerializerSet.class, builder.getSerializers().orElse(context.getSerializers()));

			var matchers = builder.getMatchers(context);
			optionalMatchers = matchers.getOptionalEntries();
			requiredMatchers = matchers.getRequiredEntries();

			pathMatchers = bs.add(UrlPathMatcher[].class, builder.getPathMatchers().asArray());
			bs.addBean(UrlPathMatcher.class, pathMatchers.length > 0 ? pathMatchers[0] : null);

			supportedAcceptTypes = u(nn(builder.produces) ? builder.produces : serializers.getSupportedMediaTypes());
			supportedContentTypes = u(nn(builder.consumes) ? builder.consumes : parsers.getSupportedMediaTypes());

			defaultRequestAttributes = builder.defaultRequestAttributes();
			defaultRequestFormData = builder.defaultRequestFormData();
			defaultRequestHeaders = builder.defaultRequestHeaders();
			defaultRequestQueryData = builder.defaultRequestQueryData();
			defaultResponseHeaders = builder.defaultResponseHeaders();

			int _hierarchyDepth = 0;
			var sc = method.getDeclaringClass().getSuperclass();
			while (nn(sc)) {
				_hierarchyDepth++;
				sc = sc.getSuperclass();
			}
			hierarchyDepth = _hierarchyDepth;

			var _httpMethod = builder.httpMethod;
			if (_httpMethod == null)
				_httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(_httpMethod))
				_httpMethod = "*";
			httpMethod = _httpMethod.toUpperCase(Locale.ENGLISH);

			defaultCharset = nn(builder.defaultCharset) ? builder.defaultCharset : context.defaultCharset;
			dotAll = builder.dotAll;
			maxInput = nn(builder.maxInput) ? builder.maxInput : context.maxInput;

			responseMeta = ResponseBeanMeta.create(mi, builder.getApplied());

			preCallMethods = context.getPreCallMethods().stream().map(x -> new RestOpInvoker(x, context.findRestOperationArgs(x, bs), context.getMethodExecStats(x))).toArray(RestOpInvoker[]::new);
			postCallMethods = context.getPostCallMethods().stream().map(x -> new RestOpInvoker(x, context.findRestOperationArgs(x, bs), context.getMethodExecStats(x))).toArray(RestOpInvoker[]::new);
			methodInvoker = new RestOpInvoker(method, context.findRestOperationArgs(method, bs), context.getMethodExecStats(method));

			this.callLogger = context.getCallLogger();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Overridden from Comparable */
	public int compareTo(RestOpContext o) {
		int c;

		for (int i = 0; i < Math.min(pathMatchers.length, o.pathMatchers.length); i++) {
			c = pathMatchers[i].compareTo(o.pathMatchers[i]);
			if (c != 0)
				return c;
		}

		c = cmp(o.hierarchyDepth, hierarchyDepth);
		if (c != 0)
			return c;

		c = cmp(o.requiredMatchers.length, requiredMatchers.length);
		if (c != 0)
			return c;

		c = cmp(o.optionalMatchers.length, optionalMatchers.length);
		if (c != 0)
			return c;

		c = cmp(o.guards.length, guards.length);

		if (c != 0)
			return c;

		c = cmp(method.getName(), o.method.getName());
		if (c != 0)
			return c;

		c = cmp(method.getParameterCount(), o.method.getParameterCount());
		if (c != 0)
			return c;

		for (var i = 0; i < method.getParameterCount(); i++) {
			c = cmp(method.getParameterTypes()[i].getName(), o.method.getParameterTypes()[i].getName());
			if (c != 0)
				return c;
		}

		c = cmp(method.getReturnType().getName(), o.method.getReturnType().getName());
		if (c != 0)
			return c;

		return 0;
	}

	@Override /* Overridden from Context */
	public Context.Builder copy() {
		throw unsupportedOp();
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * @param session The current REST call.
	 * @return The wrapped request object.
	 * @throws Exception If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(RestSession session) throws Exception {
		return new RestRequest(this, session);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(RestSession)}.
	 *
	 * @param session The current REST call.
	 * @param req The REST request.
	 * @return The wrapped response object.
	 * @throws Exception If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestSession session, RestRequest req) throws Exception {
		return new RestResponse(this, session, req);
	}

	/**
	 * Creates a new REST operation session.
	 *
	 * @param session The REST session.
	 * @return A new REST operation session.
	 * @throws Exception If op session could not be created.
	 */
	public RestOpSession.Builder createSession(RestSession session) throws Exception {
		return RestOpSession.create(this, session).logger(callLogger).debug(debug.isDebug(this, session.getRequest()));
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof RestOpContext o2) && eq(this, o2, (x, y) -> eq(x.method, y.method));
	}

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean context associated with this context.
	 */
	public BeanContext getBeanContext() { return beanContext; }

	/**
	 * Returns the default charset.
	 *
	 * @return The default charset.  Never <jk>null</jk>.
	 */
	public Charset getDefaultCharset() { return defaultCharset; }

	/**
	 * Returns the default request attributes.
	 *
	 * @return The default request attributes.  Never <jk>null</jk>.
	 */
	public NamedAttributeMap getDefaultRequestAttributes() { return defaultRequestAttributes; }

	/**
	 * Returns the default form data parameters.
	 *
	 * @return The default form data parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestFormData() { return defaultRequestFormData; }

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() { return defaultRequestHeaders; }

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestQueryData() { return defaultRequestQueryData; }

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() { return defaultResponseHeaders; }

	/**
	 * Returns the compression encoders to use for this method.
	 *
	 * @return The compression encoders to use for this method.
	 */
	public EncoderSet getEncoders() { return encoders; }

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 *
	 * @return The HTTP method name.
	 */
	public String getHttpMethod() { return httpMethod; }

	/**
	 * Returns the underlying Java method that this context belongs to.
	 *
	 * @return The underlying Java method that this context belongs to.
	 */
	public Method getJavaMethod() { return method; }

	/**
	 * Returns the JSON-Schema generator applicable to this Java method.
	 *
	 * @return The JSON-Schema generator applicable to this Java method.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() { return jsonSchemaGenerator; }

	/**
	 * Returns the max number of bytes to process in the input content.
	 *
	 * @return The max number of bytes to process in the input content.
	 */
	public long getMaxInput() { return maxInput; }

	/**
	 * Returns the parsers to use for this method.
	 *
	 * @return The parsers to use for this method.
	 */
	public ParserSet getParsers() { return parsers; }

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() { return partParser; }

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() { return partSerializer; }

	/**
	 * Returns the path pattern for this method.
	 *
	 * @return The path pattern.
	 */
	public String getPathPattern() { return pathMatchers[0].toString(); }

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link Response @Response}.
	 *
	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link Response @Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		var c = o.getClass();
		var rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, AnnotationWorkList.create());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link Header @Header}.
	 *
	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link Header @Header}.
	 */
	public ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		var c = o.getClass();
		var pm = headerPartMetas.get(c);
		if (pm == null) {
			var a = ClassInfo.of(c).getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
			if (nn(a)) {
				var schema = HttpPartSchema.create(a);
				var serializer = createPartSerializer(schema.getSerializer(), partSerializer);
				pm = new ResponsePartMeta(HEADER, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			headerPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	/**
	 * Returns the response bean meta if this method returns a {@link Response}-annotated bean.
	 *
	 * @return The response bean meta or <jk>null</jk> if it's not a {@link Response}-annotated bean.
	 */
	public ResponseBeanMeta getResponseMeta() { return responseMeta; }

	/**
	 * Returns the serializers to use for this method.
	 *
	 * @return The serializers to use for this method.
	 */
	public SerializerSet getSerializers() { return serializers; }

	/**
	 * Returns a list of supported accept types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedAcceptTypes() { return supportedAcceptTypes; }

	/**
	 * Returns the list of supported content types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedContentTypes() { return supportedContentTypes; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return method.hashCode();
	}

	private UrlPathMatch matchPattern(RestSession call) {
		UrlPathMatch pm = null;
		for (var pp : pathMatchers)
			if (pm == null)
				pm = pp.match(call.getUrlPath());
		return pm;
	}

	/**
	 * Identifies if this method can process the specified call.
	 *
	 * <p>
	 * To process the call, the following must be true:
	 * <ul>
	 * 	<li>Path pattern must match.
	 * 	<li>Matchers (if any) must match.
	 * </ul>
	 *
	 * @param session The call to check.
	 * @return
	 * 	One of the following values:
	 * 	<ul>
	 * 		<li><c>0</c> - Path doesn't match.
	 * 		<li><c>1</c> - Path matched but matchers did not.
	 * 		<li><c>2</c> - Matches.
	 * 	</ul>
	 */
	protected int match(RestSession session) {

		var pm = matchPattern(session);

		if (pm == null)
			return 0;

		if (requiredMatchers.length == 0 && optionalMatchers.length == 0) {
			session.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		}

		try {
			var req = session.getRequest();

			// If the method implements matchers, test them.
			for (var m : requiredMatchers)
				if (! m.matches(req))
					return 1;
			if (optionalMatchers.length > 0) {
				var matches = false;
				for (var m : optionalMatchers)
					matches |= m.matches(req);
				if (! matches)
					return 1;
			}

			session.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	@Override /* Overridden from Context */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_defaultRequestFormData, defaultRequestFormData)
			.a(PROP_defaultRequestHeaders, defaultRequestHeaders)
			.a(PROP_defaultRequestQueryData, defaultRequestQueryData)
			.a(PROP_httpMethod, httpMethod);
	}

	RestConverter[] getConverters() { return converters; }

	RestGuard[] getGuards() { return guards; }

	RestOpInvoker getMethodInvoker() { return methodInvoker; }

	RestOpInvoker[] getPostCallMethods() { return postCallMethods; }

	RestOpInvoker[] getPreCallMethods() { return preCallMethods; }
}