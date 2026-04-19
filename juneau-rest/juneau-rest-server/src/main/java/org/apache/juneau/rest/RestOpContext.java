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

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.UTF8;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.rest.RestServerConstants.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.function.*;
import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.collections.FluentMap;
import org.apache.juneau.commons.function.Memoizer;
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
import org.apache.juneau.svl.*;
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
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., PROP_defaultRequestFormData)
})
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

	private static Charset envDefaultRestCharset() {
		return env("RestContext.defaultCharset").map(Charset::forName).orElse(UTF8);
	}

	private static long envDefaultRestMaxInput() {
		return env("RestContext.maxInput").map(RestOpContext::parseMaxInputEnv).orElse(100_000_000L);
	}

	private static long parseMaxInputEnv(String value) {
		return parseLongWithSuffix(value);
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends Context.Builder {

		private BeanContext.Builder beanContext;
		private BasicBeanStore beanStore;
		private EncoderSet.Builder encoders;
		private HeaderList defaultRequestHeaders;
		private HeaderList defaultResponseHeaders;
		private HttpPartParser.Creator partParser;
		private HttpPartSerializer.Creator partSerializer;
		private JsonSchemaGenerator.Builder jsonSchemaGenerator;
		private List<String> path;
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

		/**
		 * Constructor.
		 *
		 * <p>
		 * Was previously package-private and reached via the static {@code RestOpContext.create(method, context)} factory.
		 * Promoted to <jk>public</jk> in 9.5 (TODO-16 Phase C-3 Route B) so that internal subclasses living outside this
		 * package (e.g. {@link org.apache.juneau.rest.rrpc.RrpcRestOpContext}) can construct a builder directly without
		 * the now-deleted factory method.
		 *
		 * @param method The Java method this builder is being created for.
		 * @param context The owning REST context.
		 */
		public Builder(java.lang.reflect.Method method, RestContext context) {

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
		 * Returns the REST servlet/bean instance that this context is defined against.
		 *
		 * @return The REST servlet/bean instance that this context is defined against.
		 */
		@SuppressWarnings({
			"java:S1452"  // Wildcard required - Supplier<?> for generic REST resource instance
		})
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
		 * <p>
		 * Promoted from {@code protected} to <jk>public</jk> in 9.5 (TODO-16 Phase C-3 Route B) so that subclasses
		 * living outside this package (e.g. {@link org.apache.juneau.rest.rrpc.RrpcRestOpContext}) can override the
		 * builder-time bean store without going through the now-deleted {@code RestOpContext.create(...)} factory.
		 *
		 * @param beanStore The bean store to use for resolving constructor arguments.
		 * @return This object.
		 */
		public Builder beanStore(BasicBeanStore beanStore) {
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
		 * 	<jc>// Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(converters={MyConverter.<jk>class</jk>})
		 * 	<jk>public class</jk> MyResource { ... }
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
			beanStore.getBeanType(RestConverterList.class).ifPresent(x -> v.get().type(x));

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
				.find(x -> matches(x, PROP_defaultRequestFormData))
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
				.find(x -> matches(x, PROP_defaultRequestHeaders))
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
				.find(x -> matches(x, PROP_defaultRequestQueryData))
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
			beanStore.getBeanType(RestGuardList.class).ifPresent(x -> v.get().type(x));

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
			beanStore.getBeanType(RestMatcherList.class).ifPresent(x -> v.get().type(x));

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
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for path matcher creation
		})
		protected UrlPathMatcherList getPathMatchers() {

			var v = Value.of(UrlPathMatcherList.create());

			if (nn(path)) {
				for (var p : path)
					v.get().add(UrlPathMatcher.of(p));
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

				// RRPC operations match anything below the method's URL when no explicit
				// path is supplied. The legacy `Builder.dotAll()` flag was removed per
				// TODO-16 Decision #17 — RRPC's "match anything below" convention is
				// now baked into auto-detection.
				if ("RRPC".equalsIgnoreCase(httpMethod2) && ! p.endsWith("/*"))
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
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for HTTP parts configuration
		})
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

	// `public static Builder create(Method, RestContext)` removed in 9.5 (TODO-16 Phase C-3 Route B).
	// Direct callers were Site 1 / Site 2 in `RestContext.Builder.createRestOperations`, both migrated to the
	// new 2-arg ctors `new RestOpContext(method, context)` / `new RrpcRestOpContext(method, context)`. Internal
	// subclasses still needing builder-shaped construction (e.g. `RrpcRestOpContext`'s 2-arg ctor delegating to
	// the protected `RestOpContext(Builder)` ctor) instantiate the now-public `Builder(Method, RestContext)`
	// directly.

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, HttpPartSerializer defaultSerializer) {
		return BeanCreator.of(HttpPartSerializer.class).type(c).orElse(defaultSerializer);
	}

	protected final Builder builder;
	protected final int hierarchyDepth;
	protected final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	protected final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	protected final Method method;
	protected final MethodInfo mi;
	protected final RestContext context;
	protected final RestOpInvoker methodInvoker;
	protected final RestOpInvoker[] postCallMethods;
	protected final RestOpInvoker[] preCallMethods;
	protected final ResponseBeanMeta responseMeta;

	/** Resolved once at construction from {@link Builder} annotation apply and context fallbacks (TODO-16). */
	private final Charset resolvedDefaultCharset;
	private final long resolvedMaxInput;
	private final DebugEnablement resolvedDebugEnablement;
	private final List<MediaType> resolvedSupportedAcceptTypes;
	private final List<MediaType> resolvedSupportedContentTypes;
	private final String resolvedHttpMethod;

	//-----------------------------------------------------------------------------------------------------------------
	// Memoized allowlist fields
	//-----------------------------------------------------------------------------------------------------------------

	/** Memoized all {@link RestOp}-group annotations on this method, child-to-parent order. */
	private final Memoizer<List<AnnotationInfo<?>>> restOpAnnotations = memoizer(this::findRestOpAnnotations);

	private List<AnnotationInfo<?>> findRestOpAnnotations() {
		// Use the same MethodInfo as {@link Builder}'s annotation traversal (resource class + method), not
		// {@link MethodInfo#of(Method)} alone, so method-level {@code @RestGet}/@RestOp metadata (e.g. noInherit)
		// resolves consistently when the implementation class differs from the method's declaring class.
		var methodInfo = MethodInfo.of(context.getResourceClass(), method).accessible();
		return context.getAnnotationProvider().find(methodInfo, SELF, MATCHING_METHODS).stream()
			.filter(ai -> ai.isInGroup(RestOp.class))
			.toList();
	}

	/** Memoized aggregated {@code noInherit} keys from all RestOp-group annotations on this operation. */
	private final Memoizer<SortedSet<String>> noInheritOp = memoizer(this::findNoInheritOp);

	private SortedSet<String> findNoInheritOp() {
		var l = getRestOpAnnotations().stream()
			.map(ai -> ai.getStringArray("noInherit").orElse(StringUtils.EMPTY_STRING_ARRAY))
			.flatMap(this::resolveCdl)
			.toList();
		return Collections.unmodifiableSortedSet(treeSet(String.CASE_INSENSITIVE_ORDER, l));
	}

	/** Memoized effective allowed parser option keys for this operation. */
	private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(this::findAllowedParserOptions);

	private SortedSet<String> findAllowedParserOptions() {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedParserOptions;
		if (isInherited(p))
			l.addAll(context.getAllowedParserOptions());
		getRestOpAnnotations().stream()
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(new String[0])))
			.forEach(l::add);
		return Collections.unmodifiableSortedSet(treeSet(String.CASE_INSENSITIVE_ORDER, removeNegations(l)));
	}

	/** Memoized effective allowed serializer option keys for this operation. */
	private final Memoizer<SortedSet<String>> allowedSerializerOptions = memoizer(this::findAllowedSerializerOptions);

	private SortedSet<String> findAllowedSerializerOptions() {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedSerializerOptions;
		if (isInherited(p))
			l.addAll(context.getAllowedSerializerOptions());
		getRestOpAnnotations().stream()
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(new String[0])))
			.forEach(l::add);
		return Collections.unmodifiableSortedSet(treeSet(String.CASE_INSENSITIVE_ORDER, removeNegations(l)));
	}

	private final Memoizer<BeanContext> beanContextMemo = memoizer(this::findBeanContext);

	private BeanContext findBeanContext() {
		return builder.getBeanContext().orElse(context.getBeanContext());
	}

	private final Memoizer<CallLogger> callLoggerMemo = memoizer(this::findCallLogger);

	private CallLogger findCallLogger() {
		return context.getCallLogger();
	}

	private final Memoizer<EncoderSet> encodersMemo = memoizer(this::findEncoders);

	private EncoderSet findEncoders() {
		return builder.getEncoders().orElse(context.getEncoders());
	}

	private final Memoizer<JsonSchemaGenerator> jsonSchemaGeneratorMemo = memoizer(this::findJsonSchemaGenerator);

	private JsonSchemaGenerator findJsonSchemaGenerator() {
		return builder.getJsonSchemaGenerator().orElse(context.getJsonSchemaGenerator());
	}

	private final Memoizer<ParserSet> parsersMemo = memoizer(this::findParsers);

	private ParserSet findParsers() {
		return builder.getParsers().orElse(context.getParsers());
	}

	private final Memoizer<HttpPartParser> partParserMemo = memoizer(this::findPartParser);

	private HttpPartParser findPartParser() {
		return builder.getPartParser().orElse(context.getPartParser());
	}

	private final Memoizer<HttpPartSerializer> partSerializerMemo = memoizer(this::findPartSerializer);

	private HttpPartSerializer findPartSerializer() {
		return builder.getPartSerializer().orElse(context.getPartSerializer());
	}

	private final Memoizer<SerializerSet> serializersMemo = memoizer(this::findSerializers);

	private SerializerSet findSerializers() {
		return builder.getSerializers().orElse(context.getSerializers());
	}

	private final Memoizer<NamedAttributeMap> defaultRequestAttributesMemo = memoizer(this::findDefaultRequestAttributes);

	private NamedAttributeMap findDefaultRequestAttributes() {
		return builder.defaultRequestAttributes();
	}

	private final Memoizer<PartList> defaultRequestFormDataMemo = memoizer(this::findDefaultRequestFormData);

	private PartList findDefaultRequestFormData() {
		return builder.defaultRequestFormData();
	}

	private final Memoizer<HeaderList> defaultRequestHeadersMemo = memoizer(this::findDefaultRequestHeaders);

	private HeaderList findDefaultRequestHeaders() {
		return builder.defaultRequestHeaders();
	}

	private final Memoizer<PartList> defaultRequestQueryDataMemo = memoizer(this::findDefaultRequestQueryData);

	private PartList findDefaultRequestQueryData() {
		return builder.defaultRequestQueryData();
	}

	private final Memoizer<HeaderList> defaultResponseHeadersMemo = memoizer(this::findDefaultResponseHeaders);

	private HeaderList findDefaultResponseHeaders() {
		return builder.defaultResponseHeaders();
	}

	private final Memoizer<RestConverter[]> convertersMemo = memoizer(this::findConverters);

	private RestConverter[] findConverters() {
		return builder.converters().build().asArray();
	}

	private final Memoizer<RestGuard[]> guardsMemo = memoizer(this::findGuards);

	private RestGuard[] findGuards() {
		return builder.getGuards().asArray();
	}

	private final Memoizer<RestMatcherList> matchersListMemo = memoizer(this::findMatchersList);

	private RestMatcherList findMatchersList() {
		return builder.getMatchers(context);
	}

	private final Memoizer<RestMatcher[]> optionalMatchersMemo = memoizer(this::findOptionalMatchers);

	private RestMatcher[] findOptionalMatchers() {
		return matchersListMemo.get().getOptionalEntries();
	}

	private final Memoizer<RestMatcher[]> requiredMatchersMemo = memoizer(this::findRequiredMatchers);

	private RestMatcher[] findRequiredMatchers() {
		return matchersListMemo.get().getRequiredEntries();
	}

	private final Memoizer<UrlPathMatcher[]> pathMatchersMemo = memoizer(this::findPathMatchers);

	private UrlPathMatcher[] findPathMatchers() {
		return builder.getPathMatchers().asArray();
	}

	private Stream<String> resolveCdl(String...values) {
		if (values == null || values.length == 0)
			return Stream.empty();
		return Arrays.stream(values)
			.filter(Objects::nonNull)
			.map(s -> RestOpContext.this.context.getVarResolver().resolve(s))
			.map(StringUtils::split)
			.flatMap(Collection::stream)
			.map(String::trim)
			.filter(StringUtils::isNotBlank);
	}

	/**
	 * Returns all {@link RestOp}-group annotations on this operation method, in child-to-parent order.
	 *
	 * @return An unmodifiable list, never {@code null}.
	 */
	public List<AnnotationInfo<?>> getRestOpAnnotations() {
		return restOpAnnotations.get();
	}

	/**
	 * Returns {@code true} if context-level values for the given property should be merged.
	 *
	 * @param property The annotation attribute name (e.g. {@link RestServerConstants#PROPERTY_allowedParserOptions},
	 * 	{@link RestServerConstants#PROPERTY_defaultCharset}, {@link RestServerConstants#PROPERTY_maxInput},
	 * 	{@link RestServerConstants#PROPERTY_debug}).
	 * @return {@code true} if {@code noInherit} does not contain this property.
	 */
	protected boolean isInherited(String property) {
		return !noInheritOp.get().contains(property);
	}

	/**
	 * Returns the parser session-option keys allowed for this operation.
	 *
	 * @return An unmodifiable case-insensitive sorted set, never {@code null}.
	 */
	public SortedSet<String> getAllowedParserOptions() {
		return allowedParserOptions.get();
	}

	/**
	 * Returns the serializer session-option keys allowed for this operation.
	 *
	 * @return An unmodifiable case-insensitive sorted set, never {@code null}.
	 */
	public SortedSet<String> getAllowedSerializerOptions() {
		return allowedSerializerOptions.get();
	}

	/**
	 * Returns the first non-blank, SVL-resolved value of the given attribute across all
	 * {@code @RestOp}-group annotations on this method (child-to-parent order).
	 *
	 * @param attr The annotation attribute name (e.g. {@code "defaultCharset"}).
	 * @return The resolved string, or empty if no annotation defines it.
	 */
	private Optional<String> findOpString(String attr) {
		var vr = context.getVarResolver();
		for (var ai : getRestOpAnnotations()) {
			var s = ai.getString(attr).orElse("");
			if (!s.isEmpty()) {
				var resolved = vr.resolve(s);
				if (!resolved.isEmpty())
					return Optional.of(resolved);
			}
		}
		return Optional.empty();
	}

	private Charset findDefaultCharset() {
		var v = findOpString(PROPERTY_defaultCharset);
		if (v.isPresent())
			return Charset.forName(v.get());
		if (isInherited(PROPERTY_defaultCharset)) {
			var rv = context.mergeReplacedStringAttribute(PROPERTY_defaultCharset, null);
			if (rv != null && !rv.isEmpty())
				return Charset.forName(rv);
		}
		return envDefaultRestCharset();
	}

	private long findMaxInput() {
		var v = findOpString(PROPERTY_maxInput);
		if (v.isPresent())
			return parseLongWithSuffix(v.get());
		if (isInherited(PROPERTY_maxInput)) {
			var rv = context.mergeReplacedStringAttribute(PROPERTY_maxInput, null);
			if (rv != null && !rv.isEmpty())
				return parseLongWithSuffix(rv);
		}
		return envDefaultRestMaxInput();
	}

	private DebugEnablement findDebugEnablement() {
		var v = findOpString(PROPERTY_debug);
		if (v.isPresent())
			return DebugEnablement.create(context.getBeanStore()).enable(Enablement.fromString(v.get()), "*").build();
		if (isInherited(PROPERTY_debug))
			return context.getDebugEnablement();
		return DebugEnablement.create(context.getBeanStore()).build();
	}

	/**
	 * Resolves the supported request content types for this operation.
	 *
	 * <p>
	 * Walks the op-level {@code @RestOp}-group annotations for {@code consumes} attributes and (when
	 * {@code noInherit} does not block it) the class-level {@code @Rest(consumes)} hierarchy. Each
	 * value is SVL-resolved. If no explicit values are declared, falls back to the supported content
	 * types of the operation's {@link ParserSet}.
	 *
	 * @return An unmodifiable list of media types, never {@code null}.
	 */
	private List<MediaType> findSupportedContentTypes() {
		var result = collectAnnotationMediaTypes(PROPERTY_consumes);
		if (result.isEmpty())
			return u(getParsers().getSupportedMediaTypes());
		return u(result);
	}

	/**
	 * Resolves the supported response accept types for this operation.
	 *
	 * <p>
	 * Walks the op-level {@code @RestOp}-group annotations for {@code produces} attributes and (when
	 * {@code noInherit} does not block it) the class-level {@code @Rest(produces)} hierarchy. Each
	 * value is SVL-resolved. If no explicit values are declared, falls back to the supported media
	 * types of the operation's {@link SerializerSet}.
	 *
	 * @return An unmodifiable list of media types, never {@code null}.
	 */
	private List<MediaType> findSupportedAcceptTypes() {
		var result = collectAnnotationMediaTypes(PROPERTY_produces);
		if (result.isEmpty())
			return u(getSerializers().getSupportedMediaTypes());
		return u(result);
	}

	private List<MediaType> collectAnnotationMediaTypes(String attr) {
		var result = new ArrayList<MediaType>();
		var vr = context.getVarResolver();
		// Class-level @Rest(consumes|produces) first (when inheritance is allowed), then op-level overrides append.
		if (isInherited(attr)) {
			for (var ai : context.getRestAnnotations())
				appendResolvedMediaTypes(ai, attr, vr, result);
		}
		for (var ai : getRestOpAnnotations())
			appendResolvedMediaTypes(ai, attr, vr, result);
		return result;
	}

	private static void appendResolvedMediaTypes(AnnotationInfo<?> ai, String attr, VarResolver vr, List<MediaType> result) {
		var arr = ai.getStringArray(attr).orElse(null);
		if (arr == null)
			return;
		for (var s : arr) {
			if (s == null || s.isEmpty())
				continue;
			var resolved = vr.resolve(s);
			if (resolved.isEmpty())
				continue;
			result.add(MediaType.of(resolved));
		}
	}

	/**
	 * Resolves the HTTP method for this operation from {@code @RestOp}-group annotations.
	 *
	 * <p>
	 * Walks the op-level annotations in child-to-parent order:
	 * <ul>
	 *   <li>{@code @RestGet}/{@code @RestPut}/{@code @RestPost}/{@code @RestDelete}/{@code @RestPatch}/{@code @RestOptions}
	 *       imply their fixed verb.
	 *   <li>{@code @RestOp(method)} is SVL-resolved; if blank, {@code @RestOp(value)} is parsed for a leading verb token.
	 * </ul>
	 * <p>
	 * If no annotation supplies a value, the verb is inferred from the Java method name via
	 * {@link HttpUtils#detectHttpMethod(Method, boolean, String)}. The literal {@code "METHOD"} is
	 * normalized to the wildcard {@code "*"}, and the result is upper-cased.
	 *
	 * @return The resolved HTTP method, never {@code null}.
	 */
	private String findHttpMethod() {
		var vr = context.getVarResolver();
		for (var ai : getRestOpAnnotations()) {
			var v = httpMethodFromAnnotation(ai.inner(), vr);
			if (v != null && !v.isEmpty())
				return normalizeHttpMethod(v);
		}
		return normalizeHttpMethod(HttpUtils.detectHttpMethod(method, true, "GET"));
	}

	private static String httpMethodFromAnnotation(Annotation a, VarResolver vr) {
		if (a instanceof RestGet)
			return "get";
		if (a instanceof RestPut)
			return "put";
		if (a instanceof RestPost)
			return "post";
		if (a instanceof RestDelete)
			return "delete";
		if (a instanceof RestPatch)
			return "patch";
		if (a instanceof RestOptions)
			return "options";
		if (a instanceof RestOp r) {
			var m = vr.resolve(r.method());
			if (m != null && !m.isEmpty())
				return m;
			var s = vr.resolve(r.value());
			if (s != null) {
				s = s.trim();
				if (!s.isEmpty()) {
					var i = s.indexOf(' ');
					return i == -1 ? s : s.substring(0, i).trim();
				}
			}
		}
		return null;
	}

	private static String normalizeHttpMethod(String v) {
		if ("METHOD".equalsIgnoreCase(v))
			return "*";
		return v.toUpperCase(Locale.ENGLISH);
	}

	/**
	 * 2-arg positional context constructor.
	 *
	 * <p>
	 * Equivalent to the legacy {@code RestOpContext.create(method, context).build()} entry point but without exposing
	 * the {@link Builder} to the caller. All operation-level configuration is resolved from {@link RestOp}-group
	 * annotations on the method and inherited from the parent {@link RestContext}.
	 *
	 * @param method The Java method this context represents. Must not be <jk>null</jk>.
	 * @param context The owning {@link RestContext}. Must not be <jk>null</jk>.
	 * @throws ServletException If context could not be created.
	 * @since 9.2.1
	 */
	public RestOpContext(java.lang.reflect.Method method, RestContext context) throws ServletException {
		this(new Builder(method, context));
	}

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	protected RestOpContext(Builder builder) throws ServletException {
		super(builder);

		try {
			this.builder = builder;
			context = builder.restContext;
			method = builder.restMethod;

			mi = MethodInfo.of(method).accessible();

			// @formatter:off
			var bs = BasicBeanStore.of(context.getRootBeanStore())
				.addBean(RestOpContext.class, this)
				.addBean(Method.class, method)
				.addBean(AnnotationWorkList.class, builder.getApplied());
			// @formatter:on
			bs.addBean(BasicBeanStore.class, bs);

			bs.add(BeanContext.class, getBeanContext());
			bs.add(RestConverter[].class, getConverters());
			bs.add(EncoderSet.class, getEncoders());
			bs.add(RestGuard[].class, getGuards());
			bs.add(JsonSchemaGenerator.class, getJsonSchemaGenerator());
			bs.add(ParserSet.class, getParsers());
			bs.add(HttpPartParser.class, getPartParser());
			bs.add(HttpPartSerializer.class, getPartSerializer());
			bs.add(SerializerSet.class, getSerializers());

			resolvedDefaultCharset = findDefaultCharset();
			resolvedMaxInput = findMaxInput();
			resolvedDebugEnablement = findDebugEnablement();
			resolvedSupportedAcceptTypes = findSupportedAcceptTypes();
			resolvedSupportedContentTypes = findSupportedContentTypes();
			resolvedHttpMethod = findHttpMethod();

			var pm = getPathMatchers();
			bs.add(UrlPathMatcher[].class, pm);
			bs.addBean(UrlPathMatcher.class, pm.length > 0 ? pm[0] : null);

		int hierarchyDepthTemp = 0;
		var sc = method.getDeclaringClass().getSuperclass();
		while (nn(sc)) {
			hierarchyDepthTemp++;
			sc = sc.getSuperclass();
		}
		hierarchyDepth = hierarchyDepthTemp;

			responseMeta = ResponseBeanMeta.create(mi, builder.getApplied());

			preCallMethods = context.getPreCallMethods().stream().map(x -> new RestOpInvoker(x, context.findRestOperationArgs(x, bs), context.getMethodExecStats(x))).toArray(RestOpInvoker[]::new);
			postCallMethods = context.getPostCallMethods().stream().map(x -> new RestOpInvoker(x, context.findRestOperationArgs(x, bs), context.getMethodExecStats(x))).toArray(RestOpInvoker[]::new);
			methodInvoker = new RestOpInvoker(method, context.findRestOperationArgs(method, bs), context.getMethodExecStats(method));
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

		var pm = getPathMatchers();
		var opm = o.getPathMatchers();
		for (int i = 0; i < Math.min(pm.length, opm.length); i++) {
			c = pm[i].compareTo(opm[i]);
			if (c != 0)
				return c;
		}

		c = cmp(o.hierarchyDepth, hierarchyDepth);
		if (c != 0)
			return c;

		c = cmp(o.getRequiredMatchers().length, getRequiredMatchers().length);
		if (c != 0)
			return c;

		c = cmp(o.getOptionalMatchers().length, getOptionalMatchers().length);
		if (c != 0)
			return c;

		c = cmp(o.getGuards().length, getGuards().length);

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
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
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
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
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
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	public RestOpSession.Builder createSession(RestSession session) throws Exception {
		return RestOpSession.create(this, session).logger(getCallLogger()).debug(resolvedDebugEnablement.isDebug(this, session.getRequest()));
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
	public BeanContext getBeanContext() { return beanContextMemo.get(); }

	/**
	 * Returns the default charset.
	 *
	 * @return The default charset.  Never <jk>null</jk>.
	 */
	public Charset getDefaultCharset() { return resolvedDefaultCharset; }

	/**
	 * Returns the default request attributes.
	 *
	 * @return The default request attributes.  Never <jk>null</jk>.
	 */
	public NamedAttributeMap getDefaultRequestAttributes() { return defaultRequestAttributesMemo.get(); }

	/**
	 * Returns the default form data parameters.
	 *
	 * @return The default form data parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestFormData() { return defaultRequestFormDataMemo.get(); }

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() { return defaultRequestHeadersMemo.get(); }

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestQueryData() { return defaultRequestQueryDataMemo.get(); }

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() { return defaultResponseHeadersMemo.get(); }

	/**
	 * Returns the compression encoders to use for this method.
	 *
	 * @return The compression encoders to use for this method.
	 */
	public EncoderSet getEncoders() { return encodersMemo.get(); }

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 *
	 * @return The HTTP method name.
	 */
	public String getHttpMethod() { return resolvedHttpMethod; }

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
	public JsonSchemaGenerator getJsonSchemaGenerator() { return jsonSchemaGeneratorMemo.get(); }

	/**
	 * Returns the max number of bytes to process in the input content.
	 *
	 * @return The max number of bytes to process in the input content.
	 */
	public long getMaxInput() { return resolvedMaxInput; }

	/**
	 * Returns the parsers to use for this method.
	 *
	 * @return The parsers to use for this method.
	 */
	public ParserSet getParsers() { return parsersMemo.get(); }

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() { return partParserMemo.get(); }

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() { return partSerializerMemo.get(); }

	/**
	 * Returns the path pattern for this method.
	 *
	 * @return The path pattern.
	 */
	public String getPathPattern() { return getPathMatchers()[0].toString(); }

	/**
	 * Returns the URL path matchers for this operation.
	 *
	 * @return The URL path matchers for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UrlPathMatcher[] getPathMatchers() { return pathMatchersMemo.get(); }

	/**
	 * Returns the optional matchers for this operation.
	 *
	 * @return The optional matchers for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestMatcher[] getOptionalMatchers() { return optionalMatchersMemo.get(); }

	/**
	 * Returns the required matchers for this operation.
	 *
	 * @return The required matchers for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestMatcher[] getRequiredMatchers() { return requiredMatchersMemo.get(); }

	/**
	 * Returns the call logger for this operation.
	 *
	 * @return The call logger for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public CallLogger getCallLogger() { return callLoggerMemo.get(); }

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
				@SuppressWarnings("unchecked")
				var serializer = createPartSerializer((Class<? extends HttpPartSerializer>)schema.getSerializer(), getPartSerializer());
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
	public SerializerSet getSerializers() { return serializersMemo.get(); }

	/**
	 * Returns a list of supported accept types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedAcceptTypes() { return resolvedSupportedAcceptTypes; }

	/**
	 * Returns the list of supported content types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedContentTypes() { return resolvedSupportedContentTypes; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return method.hashCode();
	}

	private UrlPathMatch matchPattern(RestSession call) {
		UrlPathMatch pm = null;
		for (var pp : getPathMatchers())
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

		var rm = getRequiredMatchers();
		var om = getOptionalMatchers();
		if (rm.length == 0 && om.length == 0) {
			session.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		}

		try {
			var req = session.getRequest();

			// If the method implements matchers, test them.
			for (var m : rm)
				if (! m.matches(req))
					return 1;
			if (om.length > 0) {
				var matches = false;
				for (var m : om)
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
			.a(PROP_defaultRequestFormData, getDefaultRequestFormData())
			.a(PROP_defaultRequestHeaders, getDefaultRequestHeaders())
			.a(PROP_defaultRequestQueryData, getDefaultRequestQueryData())
			.a(PROP_httpMethod, getHttpMethod());
	}

	RestConverter[] getConverters() { return convertersMemo.get(); }

	RestGuard[] getGuards() { return guardsMemo.get(); }

	RestOpInvoker getMethodInvoker() { return methodInvoker; }

	RestOpInvoker[] getPostCallMethods() { return postCallMethods; }

	RestOpInvoker[] getPreCallMethods() { return preCallMethods; }
}