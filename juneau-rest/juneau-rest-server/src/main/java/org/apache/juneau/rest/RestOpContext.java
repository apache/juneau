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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.UTF8;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.rest.RestServerConstants.*;
import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.collections.FluentMap;
import org.apache.juneau.commons.function.Memoizer;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.lang.Value;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
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
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.commons.svl.*;
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
	"java:S115",  // Constants use UPPER_snakeCase convention (e.g., PROP_defaultRequestFormData)
	"java:S1200", // High coupling is intentional in this central operation context aggregator.
	"java:S6539", // RestOpContext intentionally centralizes op wiring and annotation resolution.
	"resource"   // op-level BasicBeanStores are short-lived scratch stores; the long-lived opBeanStore field is owned and closed by the parent RestContext via its bean-store hierarchy.
})
public class RestOpContext extends Context implements Comparable<RestOpContext> {

	// Property name constants
	private static final String PROP_defaultRequestFormData = "defaultRequestFormData";
	private static final String PROP_defaultRequestHeaders = "defaultRequestHeaders";
	private static final String PROP_defaultRequestQueryData = "defaultRequestQueryData";
	private static final String PROP_httpMethod = "httpMethod";

	// Argument name constants for assertArgNotNull

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
	 * Internal construction-time state holder.
	 */
	static class Builder extends Context.Builder {

		private Method restMethod;
		private RestContext restContext;
		private Supplier<Object> restResourceSupplier;

		Builder(Method method, RestContext context) {
			this(method, context, null);
		}

		Builder(Method method, RestContext context, Supplier<Object> resourceSupplier) {

			this.restContext = context;
			this.restMethod = method;
			this.restResourceSupplier = resourceSupplier == null ? context::getResource : resourceSupplier;

			var ap = context.getMarshallingContext().getAnnotationProvider();
			var mi = MethodInfo.of(context.getResourceClass(), method);
			var resourceClass = ClassInfo.of(context.getResourceClass());

			try {
				var vr = context.getVarResolver();
				var vrs = vr.createSession();

				// Parent-to-child merge: class-level annotations (parent class chain) applied first, then method-level
				// (matching-methods chain, return type, package). Method annotations thus override class annotations
				// for the same property. LinkedHashSet deduplicates while preserving order.
				var declaringClassAnnotations = rstream(ap.find(resourceClass, SELF, PARENTS));
				var methodAnnotations = rstream(ap.find(mi, SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE));
				var allAnnotationsSet = new java.util.LinkedHashSet<AnnotationInfo<?>>();
				declaringClassAnnotations.forEach(allAnnotationsSet::add);
				methodAnnotations.forEach(allAnnotationsSet::add);
				var work = AnnotationWorkList.of(vrs, allAnnotationsSet.stream().filter(CONTEXT_APPLY_FILTER));

				apply(work);

			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}
	}

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, HttpPartSerializer defaultSerializer) {
		return c == null
			? defaultSerializer
			: BeanInstantiator.of(HttpPartSerializer.class).type(c).fallback(() -> defaultSerializer).run();
	}

	private final WritableBeanStore opBeanStore;
	protected final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	protected final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	protected final Method method;
	protected final MethodInfo mi;
	protected final RestContext context;
	private final Supplier<Object> resourceSupplier;

	// The annotation work-list produced during construction.
	private final AnnotationWorkList appliedAnnotations;

	private RestContext restContext() { return context; }

	/**
	 * Returns the {@link RestContext} that owns this operation context.
	 *
	 * <p>
	 * For operations declared directly on the host resource class, returns the host context. For operations
	 * registered through a {@code @Rest(mixins=...)} mixin, returns the per-mixin sub-context (parent-linked
	 * to the host's context via {@link RestContext#getParentContext()}). Callers can use this to identify
	 * whether the operation was contributed by a mixin (via {@link RestContext#isMixinContext()}).
	 *
	 * @return The owning {@link RestContext} for this operation.
	 * @since 9.5.0
	 */
	public RestContext getContext() { return context; }

	private Method method() { return method; }
	private MethodInfo methodInfo() { return mi; }
	private AnnotationWorkList appliedAnnotations() { return appliedAnnotations; }
	private BeanStore beanStore() { return context.getBeanStore(); }
	private WritableBeanStore opBeanStore() { return opBeanStore; }
	private Object resource() { return resourceSupplier.get(); }
	private VarResolver varResolver() { return context.getVarResolver(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Memoized fields
	//-----------------------------------------------------------------------------------------------------------------

	/** Effective allowed parser session-option keys for this operation. */
	private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(() -> {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedParserOptions;
		if (isInherited(p))
			l.addAll(restContext().getAllowedParserOptions());
		getRestOpAnnotations().stream()
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(EMPTY_STRING_ARRAY)))
			.forEach(l::add);
		return u(treeSetCi(removeNegations(l)));
	});

	/** Effective allowed serializer session-option keys for this operation. */
	private final Memoizer<SortedSet<String>> allowedSerializerOptions = memoizer(() -> {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedSerializerOptions;
		if (isInherited(p))
			l.addAll(restContext().getAllowedSerializerOptions());
		getRestOpAnnotations().stream()
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(EMPTY_STRING_ARRAY)))
			.forEach(l::add);
		return u(treeSetCi(removeNegations(l)));
	});

	/** The {@link MarshallingContext} for this operation (op-level annotations applied on top of the parent context). */
	private final Memoizer<MarshallingContext> marshallingContext = memoizer(() -> {
		var aa = appliedAnnotations();
		var parent = restContext().getBeanContextBuilder();
		if (!parent.canApply(aa))
			return restContext().getMarshallingContext();
		Value<MarshallingContext.Builder> v = Value.of(parent.copy());
		v.get().apply(aa);
		var bs = new BasicBeanStore(beanStore())
			.addBean(Method.class, method())
			.addBean(MarshallingContext.Builder.class, v.get());
		bs.createBeanFromMethod(MarshallingContext.class, resource(), this::matchesInjectScope)
			.ifPresent(x -> v.get().impl(x));
		return v.get().build();
	});

	/** The call logger for this operation (delegated to the parent {@link RestContext}). */
	private final Memoizer<CallLogger> callLogger = memoizer(() -> restContext().getCallLogger());

	/**
	 * The response-converter array for this operation.
	 *
	 * <p>
	 * Walks the {@code @Rest(converters)} class chain (parent-to-child) followed by the
	 * {@code @RestOp(converters)} method chain (parent-to-child). Op-level
	 * {@code noInherit={"converters"}} cuts off the class-chain contribution. An
	 * {@code @Bean RestConverterList} bean (either as a name-anonymous bean in the bean store
	 * or as a {@code @Bean} method whose {@code methodScope} matches this operation's method
	 * name) REPLACES the entire annotation-derived list.
	 */
	private final Memoizer<RestConverter[]> converters = memoizer(() -> {
		var bs = beanStore();
		var b = RestConverterList.create(bs);
		if (isInherited(PROPERTY_converters))
			restContext().getRestAnnotationsForProperty(PROPERTY_converters)
				.forEach(ai -> b.append(ai.inner().converters()));
		getRestOpAnnotationsForProperty(PROPERTY_converters)
			.forEach(ai -> ai.getClassArray("converters", RestConverter.class).ifPresent(classes -> {
				for (var c : classes)
					b.append(classArray(c));
			}));
		var override = bs.createBeanFromMethod(RestConverterList.class, resource(), this::matchesInjectScope).orElse(null);
		if (override == null)
			override = bs.getBean(RestConverterList.class).orElse(null);
		return (nn(override) ? override : b.build()).asArray();
	});

	/** The effective {@link DebugConfig} for this operation. */
	private final Memoizer<DebugConfig> debugConfig = memoizer(() -> restContext().getDebugConfig());

	/** The effective default {@link Charset} for this operation, resolved from op annotations, context, or env. */
	private final Memoizer<Charset> defaultCharset = memoizer(() -> {
		var v = findOpString(PROPERTY_defaultCharset);
		if (v.isPresent())
			return Charset.forName(v.get());
		if (isInherited(PROPERTY_defaultCharset)) {
			var rv = restContext().mergeReplacedStringAttribute(PROPERTY_defaultCharset, null);
			if (rv != null && !rv.isEmpty())
				return Charset.forName(rv);
		}
		return envDefaultRestCharset();
	});

	/**
	 * The default request attribute map for this operation.
	 *
	 * <p>
	 * Starts with the class-level value (which already incorporates
	 * {@code @Rest(defaultRequestAttributes)}), then walks the {@code @RestOp}/verb chain
	 * (parent-to-child) and adds each entry — {@link NamedAttributeMap#add} uses put-semantics so
	 * child entries override parent entries by name. An
	 * {@code @Bean(name="defaultRequestAttributes") NamedAttributeMap} bean (matching this
	 * operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<NamedAttributeMap> defaultRequestAttributes = memoizer(() -> {
		var v = Value.of(restContext().getDefaultRequestAttributes().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestAttributes).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestAttributes).orElse(EMPTY_STRING_ARRAY))
				v.get().add(BasicNamedAttribute.ofPair(s));
		});
		beanStore().createBeanFromMethod(NamedAttributeMap.class, resource(), x -> matchesInjectScope(x, PROPERTY_defaultRequestAttributes))
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default request form-data parts for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp}/verb chain (parent-to-child); each {@code defaultRequestFormData}
	 * entry is applied with {@link PartList#setDefault} (first-in-chain wins per name).
	 * Method-parameter {@link FormData @FormData} annotations with a
	 * {@link Schema#default_()}/{@link Schema#df()} default are folded in last (also
	 * {@code setDefault} = first wins). An {@code @Bean(name="defaultRequestFormData") PartList}
	 * bean (matching this operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<HttpPartList> defaultRequestFormData = memoizer(() -> {
		var v = Value.of(HttpPartList.create());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestFormData).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestFormData).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(HttpStringPart.ofPair(s));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof FormData f) {
				try {
					v.get().setDefault(HttpStringPart.of(firstNonEmpty(f.name(), f.value()), toPartValue(parseIfJson(def))));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @FormData annotation");
				}
			}
		});
		beanStore().createBeanFromMethod(HttpPartList.class, resource(), x -> matchesInjectScope(x, PROPERTY_defaultRequestFormData))
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default request headers for this operation.
	 *
	 * <p>
	 * Starts with the class-level value (which already incorporates
	 * {@code @Rest(defaultRequestHeaders|defaultAccept|defaultContentType)}), then walks the
	 * {@code @RestOp}/verb chain (parent-to-child); each annotation's {@code defaultRequestHeaders}
	 * entries plus its {@code defaultAccept} / {@code defaultContentType} (folded into
	 * {@code Accept} / {@code Content-Type} headers) are applied with {@link HeaderList#setDefault}
	 * (first-in-chain wins per name — class-level beats op-level beats later op-level entries).
	 *
	 * <p>
	 * Method-parameter {@link Header @Header} annotations with a
	 * {@link Schema#default_()}/{@link Schema#df()} default are folded in last via
	 * {@link HeaderList#set} (overrides any prior entry with the same name). An
	 * {@code @Bean(name="defaultRequestHeaders") HeaderList} bean (matching this operation's
	 * method scope) REPLACES the entire result.
	 */
	private final Memoizer<HttpHeaderList> defaultRequestHeaders = memoizer(() -> {
		var v = Value.of(restContext().getDefaultRequestHeaders().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestHeaders).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestHeaders).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(HttpStringHeader.ofPair(s));
			ai.getString(PROPERTY_defaultAccept).filter(s -> !s.isEmpty()).ifPresent(s -> v.get().setDefault(Accept.of(s)));
			ai.getString(PROPERTY_defaultContentType).filter(s -> !s.isEmpty()).ifPresent(s -> v.get().setDefault(ContentType.of(s)));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof Header h) {
				try {
					v.get().set(HttpStringHeader.of(firstNonEmpty(h.name(), h.value()), toPartValue(parseIfJson(def))));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @Header annotation");
				}
			}
		});
		beanStore().createBeanFromMethod(HttpHeaderList.class, resource(), x -> matchesInjectScope(x, PROPERTY_defaultRequestHeaders))
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default request query-data parts for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp}/verb chain (parent-to-child); each {@code defaultRequestQueryData}
	 * entry is applied with {@link PartList#setDefault} (first-in-chain wins per name).
	 * Method-parameter {@link Query @Query} annotations with a
	 * {@link Schema#default_()}/{@link Schema#df()} default are folded in last (also
	 * {@code setDefault} = first wins). An {@code @Bean(name="defaultRequestQueryData") PartList}
	 * bean (matching this operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<HttpPartList> defaultRequestQueryData = memoizer(() -> {
		var v = Value.of(HttpPartList.create());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestQueryData).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestQueryData).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(HttpStringPart.ofPair(s));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof Query q) {
				try {
					v.get().setDefault(HttpStringPart.of(firstNonEmpty(q.name(), q.value()), toPartValue(parseIfJson(def))));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @Query annotation");
				}
			}
		});
		beanStore().createBeanFromMethod(HttpPartList.class, resource(), x -> matchesInjectScope(x, PROPERTY_defaultRequestQueryData))
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default response headers for this operation.
	 *
	 * <p>
	 * Starts with the class-level value (which already incorporates
	 * {@code @Rest(defaultResponseHeaders)}), then walks the {@code @RestOp}/verb chain
	 * (parent-to-child); each annotation's {@code defaultResponseHeaders} entries are applied with
	 * {@link HeaderList#setDefault} (first-in-chain wins per name). An
	 * {@code @Bean(name="defaultResponseHeaders") HeaderList} bean (matching this operation's
	 * method scope) REPLACES the entire result.
	 */
	private final Memoizer<HttpHeaderList> defaultResponseHeaders = memoizer(() -> {
		var v = Value.of(restContext().getDefaultResponseHeaders().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultResponseHeaders).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultResponseHeaders).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(HttpStringHeader.ofPair(s));
		});
		beanStore().createBeanFromMethod(HttpHeaderList.class, resource(), x -> matchesInjectScope(x, PROPERTY_defaultResponseHeaders))
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The encoder group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(encoders)} chain (parent-to-child); each non-empty {@code encoders()}
	 * array REPLACES the inherited set (with {@link Inherit} as a sentinel that re-injects the prior
	 * set's entries at the specified position — matches the legacy {@code EncoderSet.Builder.set(...)}
	 * semantics). Falls through to the class-level {@link RestContext#getEncoders()} when no op
	 * annotation declares encoders. An {@code @Bean EncoderSet} bean (matching this operation's
	 * method scope) REPLACES the result entirely.
	 */
	private final Memoizer<EncoderSet> encoders = memoizer(() -> {
		var bs = beanStore();
		var b = restContext().getEncodersBuilder().copy();
		getRestOpAnnotationsForProperty(PROPERTY_encoders).forEach(ai -> {
			var c = ai.getClassArray("encoders", Encoder.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var v = Value.of(b.build());
		bs.createBeanFromMethod(EncoderSet.class, resource(), this::matchesInjectScope)
			.ifPresent(v::set);
		return v.get();
	});

	/**
	 * The request-guard array for this operation. <b>Cross-bucket memoizer</b> — folds three
	 * annotation attributes ({@code guards}, {@code roleGuard}, {@code rolesDeclared}) into a
	 * single effective {@link RestGuardList}.
	 *
	 * <p>
	 * Walk order: class {@code @Rest} chain (parent-to-child, gated by op-level
	 * {@code noInherit={"guards"}}), then op {@code @RestOp} chain (parent-to-child). Concretely:
	 * <ul>
	 * 	<li>Each annotation contributes its {@code guards()} classes (appended in chain order).
	 * 	<li>Each annotation contributes its {@code rolesDeclared()} CDL into a single role-name set.
	 * 	<li>Each annotation contributes its {@code roleGuard()} string (when non-blank). Each
	 * 		non-blank {@code roleGuard} becomes a {@link RoleBasedRestGuard} appended to the list,
	 * 		using the accumulated {@code rolesDeclared} set as its allow-list.
	 * </ul>
	 *
	 * <p>
	 * An {@code @Bean RestGuardList} bean (via the bean store or an {@code @Bean} method
	 * with matching {@code methodScope}) REPLACES the entire annotation-derived list (Decision #1).
	 */
	private final Memoizer<RestGuard[]> guards = memoizer(() -> {
		var bs = beanStore();
		var b = RestGuardList.create(bs);
		var rolesDeclaredSet = new java.util.LinkedHashSet<String>();
		var roleGuardStrs = new ArrayList<String>();

		Consumer<AnnotationInfo<?>> walk = ai -> {
			ai.getClassArray("guards", RestGuard.class).ifPresent(classes -> {
				for (var c : classes)
					b.append(classArray(c));
			});
			// rolesDeclared is a single CDL string (not an array) on every annotation in the chain.
			ai.getString("rolesDeclared").filter(StringUtils::isNotBlank)
				.ifPresent(s -> resolveCdl(s).forEach(rolesDeclaredSet::add));
			ai.getString("roleGuard").filter(StringUtils::isNotBlank).ifPresent(roleGuardStrs::add);
		};

		if (isInherited(PROPERTY_guards))
			restContext().getRestAnnotationsForProperty(PROPERTY_guards).forEach(walk::accept);
		getRestOpAnnotationsForProperty(PROPERTY_guards).forEach(walk);

		// When no @Rest/@RestOp(rolesDeclared) is set, pass null so RoleBasedRestGuard
		// infers role names from the expression itself (legacy semantics — an empty
		// declared-role set would silently never match).
		var declaredRoles = rolesDeclaredSet.isEmpty() ? null : rolesDeclaredSet;
		for (var rg : roleGuardStrs) {
			try {
				b.append(new RoleBasedRestGuard(declaredRoles, rg));
			} catch (java.text.ParseException e) {
				throw toRex(e);
			}
		}

		var override = bs.createBeanFromMethod(RestGuardList.class, resource(), this::matchesInjectScope).orElse(null);
		if (override == null)
			override = bs.getBean(RestGuardList.class).orElse(null);
		return (nn(override) ? override : b.build()).asArray();
	});

	/** The depth of the declaring class in the inheritance chain. */
	private final Memoizer<Integer> hierarchyDepth = memoizer(() -> {
		int hierarchyDepthTemp = 0;
		var sc = method().getDeclaringClass().getSuperclass();
		while (nn(sc)) {
			hierarchyDepthTemp++;
			sc = sc.getSuperclass();
		}
		return hierarchyDepthTemp;
	});

	/**
	 * The HTTP method string for this operation (e.g. {@code "GET"}, {@code "*"}).
	 *
	 * <p>
	 * Walks {@code @RestOp}-group annotations child-to-parent; verb-specific annotations
	 * ({@link RestGet}, etc.) imply their fixed verb. Falls back to
	 * {@link HttpUtils#detectHttpMethod} when no annotation declares a verb.
	 */
	@SuppressWarnings("java:S3776")
	private final Memoizer<String> httpMethod = memoizer(() -> {
		var vr = varResolver();
		for (var ai : getRestOpAnnotations()) {
			var v = httpMethodFromAnnotation(ai.inner(), vr);
			if (v != null && !v.isEmpty())
				return normalizeHttpMethod(v);
		}
		return normalizeHttpMethod(HttpUtils.detectHttpMethod(method(), true, "GET"));
	});

	/** The JSON-Schema generator for this operation (op-level annotations applied on top of the parent). */
	private final Memoizer<JsonSchemaGenerator> jsonSchemaGenerator = memoizer(() -> {
		var aa = appliedAnnotations();
		var parent = restContext().getJsonSchemaGeneratorBuilder();
		if (!parent.canApply(aa))
			return restContext().getJsonSchemaGenerator();
		Value<JsonSchemaGenerator.Builder> v = Value.of(parent.copy());
		v.get().apply(aa);
		var bs = new BasicBeanStore(beanStore())
			.addBean(Method.class, method())
			.addBean(JsonSchemaGenerator.Builder.class, v.get());
		bs.createBeanFromMethod(JsonSchemaGenerator.class, resource(), this::matchesInjectScope)
			.ifPresent(x -> v.get().impl(x));
		return v.get().build();
	});

	/**
	 * The request-matcher list for this operation. <b>Cross-bucket memoizer</b> — folds
	 * {@code matchers} and {@code clientVersion} (both op-level only) into a single effective
	 * {@link RestMatcherList}.
	 *
	 * <p>
	 * Walks the {@code @RestOp}/verb annotation chain (parent-to-child, gated by op-level
	 * {@code noInherit={"matchers"}}). Each annotation contributes its {@code matchers()} classes
	 * (appended in chain order). The final non-blank {@code clientVersion()} (most-derived wins)
	 * appends a single {@link ClientVersionMatcher} keyed off the resource's client-version header.
	 * An {@code @Bean RestMatcherList} bean (via the bean store or an {@code @Bean}
	 * method with matching {@code methodScope}) REPLACES the entire annotation-derived list.
	 */
	private final Memoizer<RestMatcherList> matchersList = memoizer(() -> {
		var bs = beanStore();
		var b = RestMatcherList.create(bs);
		var clientVersion = new String[]{null};

		getRestOpAnnotationsForProperty(PROPERTY_matchers).forEach(ai -> {
			ai.getClassArray("matchers", RestMatcher.class).ifPresent(classes -> {
				for (var c : classes)
					b.append(classArray(c));
			});
			ai.getString("clientVersion").filter(StringUtils::isNotBlank).ifPresent(s -> clientVersion[0] = s);
		});

		if (nn(clientVersion[0]))
			b.append(new ClientVersionMatcher(restContext().getClientVersionHeader(), MethodInfo.of(method())));

		var override = bs.createBeanFromMethod(RestMatcherList.class, resource(), this::matchesInjectScope).orElse(null);
		if (override == null)
			override = bs.getBean(RestMatcherList.class).orElse(null);
		return nn(override) ? override : b.build();
	});

	/** The invoker for the operation method itself. */
	private final Memoizer<RestOpInvoker> methodInvoker = memoizer(() ->
		new RestOpInvoker(method(), restContext().findRestOperationArgs(method(), opBeanStore()), restContext().getMethodExecStats(method()), this::resource)
	);

	/** The effective max-input byte limit for this operation. */
	private final Memoizer<Long> maxInput = memoizer(() -> {
		var v = findOpString(PROPERTY_maxInput);
		if (v.isPresent())
			return parseLongWithSuffix(v.get());
		if (isInherited(PROPERTY_maxInput)) {
			var rv = restContext().mergeReplacedStringAttribute(PROPERTY_maxInput, null);
			if (rv != null && !rv.isEmpty())
				return parseLongWithSuffix(rv);
		}
		return envDefaultRestMaxInput();
	});

	/**
	 * Whether this operation emits RFC 7807 {@code application/problem+json} responses.
	 *
	 * <p>
	 * Tri-state semantics on the {@code @RestOp}-group annotations:
	 * <ul>
	 * 	<li>{@code "true"} &mdash; opt this operation in.
	 * 	<li>{@code "false"} &mdash; opt this operation out (overrides an opted-in resource).
	 * 	<li>{@code ""} (default) &mdash; inherit from the resource-level {@code @Rest(problemDetails)}.
	 * </ul>
	 */
	private final Memoizer<Boolean> problemDetails = memoizer(() -> {
		var v = findOpString(PROPERTY_problemDetails);
		if (v.isPresent())
			return Boolean.parseBoolean(v.get());
		if (isInherited(PROPERTY_problemDetails))
			return restContext().isProblemDetails();
		return false;
	});

	/**
	 * Whether this operation opts into per-request virtual-thread dispatch on Java 21+.
	 *
	 * <p>
	 * Tri-state semantics on {@code @RestOp}-group annotations: {@code "true"} enables, {@code "false"} disables
	 * (overrides an opted-in resource), and {@code ""} (default) inherits from {@code @Rest(virtualThreads)}. Honored
	 * only when the resource-level {@link RestContext#getVirtualThreadExecutor()} is non-{@code null} (Java 21+).
	 */
	private final Memoizer<Boolean> virtualThreadsEnabled = memoizer(() -> {
		var v = findOpString(PROPERTY_virtualThreads);
		if (v.isPresent())
			return Boolean.parseBoolean(v.get());
		if (isInherited(PROPERTY_virtualThreads))
			return restContext().isVirtualThreadsEnabled();
		return false;
	});

	/**
	 * Configurable async-response timeout (milliseconds) for this operation; {@code -1} when neither the op
	 * annotation nor the resource declares a value (so {@code AsyncResponseProcessor}'s 30-second default applies).
	 */
	private final Memoizer<Long> asyncTimeoutMillis = memoizer(() -> {
		var v = findOpString(PROPERTY_asyncTimeoutMillis);
		if (v.isPresent()) {
			try {
				return Long.parseLong(v.get().trim());
			} catch (NumberFormatException nfe) {
				return -1L;
			}
		}
		if (isInherited(PROPERTY_asyncTimeoutMillis))
			return restContext().getAsyncTimeoutMillis();
		return -1L;
	});

	/** Aggregated {@code noInherit} keys from all RestOp-group annotations on this operation. */
	private final Memoizer<SortedSet<String>> noInheritOp = memoizer(() -> {
		var l = getRestOpAnnotations().stream()
			.map(ai -> ai.getStringArray("noInherit").orElse(StringUtils.EMPTY_STRING_ARRAY))
			.flatMap(this::resolveCdl)
			.toList();
		return u(treeSetCi(l));
	});

	/** The optional (non-required) matchers extracted from {@link #matchersList}. */
	private final Memoizer<RestMatcher[]> optionalMatchers = memoizer(() -> matchersList.get().getOptionalEntries());

	/** The invokers for context-level post-call lifecycle methods. */
	private final Memoizer<RestOpInvoker[]> postCallMethods = memoizer(() ->
		restContext().getPostCallMethods().stream().map(x -> new RestOpInvoker(x, restContext().findRestOperationArgs(x, opBeanStore()), restContext().getMethodExecStats(x), this::resource)).toArray(RestOpInvoker[]::new)
	);

	/** The invokers for context-level pre-call lifecycle methods. */
	private final Memoizer<RestOpInvoker[]> preCallMethods = memoizer(() ->
		restContext().getPreCallMethods().stream().map(x -> new RestOpInvoker(x, restContext().findRestOperationArgs(x, opBeanStore()), restContext().getMethodExecStats(x), this::resource)).toArray(RestOpInvoker[]::new)
	);

	/**
	 * The parser group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(parsers)} chain (parent-to-child); the most-derived non-empty
	 * {@code parsers()} array REPLACES the entire inherited set. Falls through to the class-level
	 * {@link RestContext#getParsers()} when no op annotation declares parsers. An
	 * {@code @Bean ParserSet} bean (matching this operation's method scope) REPLACES the result.
	 */
	private final Memoizer<ParserSet> parsers = memoizer(() -> {
		var aa = appliedAnnotations();
		var bs = beanStore();
		var pb = restContext().getParsersBuilder();
		var b = pb.copy();
		if (pb.canApply(aa))
			b.apply(aa);
		getRestOpAnnotationsForProperty(PROPERTY_parsers).forEach(ai -> {
			var c = ai.getClassArray("parsers", java.lang.Object.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var result = Value.of(b.build());
		bs.createBeanFromMethod(ParserSet.class, resource(), this::matchesInjectScope)
			.ifPresent(result::set);
		return result.get();
	});

	/** The HTTP part parser for this operation (op-level creator applied on top of the parent). */
	private final Memoizer<HttpPartParser> partParser = memoizer(() -> {
		var aa = appliedAnnotations();
		var parent = restContext().getPartParserCreator();
		if (!parent.canApply(aa))
			return restContext().getPartParser();
		Value<HttpPartParser.Creator> v = Value.of(parent.copy());
		v.get().apply(aa);
		var bs = new BasicBeanStore(beanStore())
			.addBean(Method.class, method())
			.addBean(HttpPartParser.Creator.class, v.get());
		bs.createBeanFromMethod(HttpPartParser.class, resource(), this::matchesInjectScope)
			.ifPresent(x -> v.get().impl(x));
		return v.get().create();
	});

	/** The HTTP part serializer for this operation (op-level creator applied on top of the parent). */
	private final Memoizer<HttpPartSerializer> partSerializer = memoizer(() -> {
		var aa = appliedAnnotations();
		var parent = restContext().getPartSerializerCreator();
		if (!parent.canApply(aa))
			return restContext().getPartSerializer();
		Value<HttpPartSerializer.Creator> v = Value.of(parent.copy());
		v.get().apply(aa);
		var bs = new BasicBeanStore(beanStore())
			.addBean(Method.class, method())
			.addBean(HttpPartSerializer.Creator.class, v.get());
		bs.createBeanFromMethod(HttpPartSerializer.class, resource(), this::matchesInjectScope)
			.ifPresent(x -> v.get().impl(x));
		return v.get().create();
	});

	/**
	 * The URL path matchers for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp}/verb annotation chain (parent-to-child) and collects each
	 * annotation's {@code path[]} array plus its {@code value()} (the conventional shortcut form on
	 * {@link RestGet @RestGet}/{@link RestPost @RestPost}/etc., or the {@code "METHOD path"} pair on
	 * {@link RestOp @RestOp}). When no explicit paths are declared, the operation method name (with
	 * the verb prefix stripped where applicable) is auto-detected via {@link HttpUtils#detectHttpPath}.
	 * For RRPC operations with no explicit path, a trailing {@code "/*"} is appended so the matcher
	 * matches anything below the method's URL.
	 * {@code noInherit={"path"}} cuts off any further parent-chain contribution. A
	 * {@code @Bean UrlPathMatcherList} bean (matching this operation's method scope) REPLACES
	 * the entire result.
	 *
	 * <p>
	 * SVL variables (e.g. {@code $S{key,default}} or the shorthand {@code ${key:default}}) in path
	 * strings are resolved via the host {@link RestContext}'s {@link VarResolver} before being
	 * compiled into {@link UrlPathMatcher} patterns &mdash; this closes the asymmetry with class-level
	 * {@link Rest#path() @Rest(path)} / {@link Rest#paths() @Rest(paths)}, which are also
	 * SVL-resolved. Strings that contain no SVL markers pass through unchanged, so this is a strict
	 * superset of the previous behavior. Path strings whose entire SVL resolution yields an empty
	 * string are skipped (matching how empty pieces are dropped from the post-comma-split pipeline at
	 * the class level &mdash; see {@code RestContext#splitPathsValue}). Unresolved variables (no
	 * registered {@link Var}) pass through to {@link UrlPathMatcher#of} as the literal {@code ${...}}
	 * placeholder, which then fails predictably at routing time. The auto-detected fallback path
	 * (derived from the method name via {@link HttpUtils#detectHttpPath}) is intentionally
	 * <i>not</i> SVL-resolved &mdash; it is framework-derived, not user input.
	 */
	@SuppressWarnings("java:S3776")
	private final Memoizer<UrlPathMatcher[]> pathMatchers = memoizer(() -> {
		var v = Value.of(UrlPathMatcherList.create());
		var vr = varResolver();
		// Use a single VarResolverSession + explicit compile(...) / .resolve(session) per path
		// so the framework hot path exercises the compiled-template seam explicitly.
		// Each distinct path string is tokenized once into a VarTemplate, then resolved against
		// this session. If we ever switch to per-request dynamic path resolution, the seam is
		// already in place — only the .resolve(session) call moves to the request handler.
		var session = vr.createSession();
		getRestOpAnnotationsForProperty(PROPERTY_path).forEach(ai -> {
			for (var p : ai.getStringArray(PROPERTY_path).orElse(StringUtils.EMPTY_STRING_ARRAY)) {
				var resolved = vr.compile(p).resolve(session);
				if (!resolved.isEmpty())
					v.get().add(UrlPathMatcher.of(resolved));
			}
			// On verb annotations (@RestGet/@RestPost/etc.) value() is always the path. On @RestOp,
			// value() is "[METHOD] [path]" where the leading method token is optional — only when a
			// space is present does the trailing token represent a path. To keep this loop annotation-
			// agnostic, we apply the @RestOp space-split rule only when an @RestOp annotation is in
			// play (i.e. the annotation type matches), and otherwise treat value() as a plain path.
			// SVL is applied AFTER the space-split rule so a resolved path can never be misinterpreted
			// as a "METHOD path" pair (the method token is structural, not user-overridable via SVL).
			ai.getString(PROPERTY_value).filter(StringUtils::isNotBlank).map(String::trim).ifPresent(s -> {
				if (ai.inner() instanceof RestOp) {
					var i = s.indexOf(' ');
					if (i != -1) {
						var resolved = vr.compile(s.substring(i).trim()).resolve(session);
						if (!resolved.isEmpty())
							v.get().add(UrlPathMatcher.of(resolved));
					}
				} else {
					var resolved = vr.compile(s).resolve(session);
					if (!resolved.isEmpty())
						v.get().add(UrlPathMatcher.of(resolved));
				}
			});
		});

		if (v.get().isEmpty()) {
			var methodInfo2 = MethodInfo.of(method());
			String httpMethod2 = null;
			if (methodInfo2.hasAnnotation(RestGet.class))
				httpMethod2 = "get";
			else if (methodInfo2.hasAnnotation(RestPut.class))
				httpMethod2 = "put";
			else if (methodInfo2.hasAnnotation(RestPost.class))
				httpMethod2 = "post";
			else if (methodInfo2.hasAnnotation(RestDelete.class))
				httpMethod2 = "delete";
			else if (methodInfo2.hasAnnotation(RestOp.class)) {
				// @formatter:off
				httpMethod2 = AP.find(RestOp.class, methodInfo2)
					.stream()
					.map(x -> x.inner().method())
					.filter(Utils::ne)
					.findFirst()
					.orElse(null);
				// @formatter:on
			}

			var p = HttpUtils.detectHttpPath(method(), httpMethod2);

			// RRPC operations match anything below the method's URL when no explicit path is supplied
			if ("RRPC".equalsIgnoreCase(httpMethod2) && ! p.endsWith("/*"))
				p += "/*";

			v.get().add(UrlPathMatcher.of(p));
		}

		beanStore().createBeanFromMethod(UrlPathMatcherList.class, resource(), this::matchesInjectScope, v.get())
			.ifPresent(v::set);

		return v.get().asArray();
	});

	/** The required matchers extracted from {@link #matchersList}. */
	private final Memoizer<RestMatcher[]> requiredMatchers = memoizer(() -> matchersList.get().getRequiredEntries());

	/** The computed response metadata for this operation method. */
	private final Memoizer<ResponseBeanMeta> responseMeta = memoizer(() -> ResponseBeanMeta.create(methodInfo(), appliedAnnotations()));

	/**
	 * All {@link RestOp}-group annotations on this method, in child-to-parent order.
	 *
	 * <p>
	 * Uses the same {@link MethodInfo} as the annotation traversal in the {@link Builder}, so
	 * method-level metadata (e.g. {@code noInherit}) resolves consistently when the implementation
	 * class differs from the method's declaring class.
	 */
	private final Memoizer<List<AnnotationInfo<?>>> restOpAnnotations = memoizer(() -> {
		var methodInfo = MethodInfo.of(restContext().getResourceClass(), method()).accessible();
		return restContext().getAnnotationProvider().find(methodInfo, SELF, MATCHING_METHODS).stream()
			.filter(ai -> ai.isInGroup(RestOp.class))
			.toList();
	});

	/**
	 * The serializer group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(serializers)} chain (parent-to-child); the most-derived non-empty
	 * {@code serializers()} array REPLACES the entire inherited set. Falls through to the class-level
	 * {@link RestContext#getSerializers()} when no op annotation declares serializers. An
	 * {@code @Bean SerializerSet} bean (matching this operation's method scope) REPLACES the result.
	 */
	private final Memoizer<SerializerSet> serializers = memoizer(() -> {
		var aa = appliedAnnotations();
		var bs = beanStore();
		var sb = restContext().getSerializersBuilder();
		var b = sb.copy();
		if (sb.canApply(aa))
			b.apply(aa);
		getRestOpAnnotationsForProperty(PROPERTY_serializers).forEach(ai -> {
			var c = ai.getClassArray("serializers", Serializer.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var result = Value.of(b.build());
		bs.createBeanFromMethod(SerializerSet.class, resource(), this::matchesInjectScope)
			.ifPresent(result::set);
		return result.get();
	});

	/**
	 * The supported response accept types for this operation.
	 *
	 * <p>
	 * Walks op-level {@code @RestOp}-group annotations for {@code produces} (and, when
	 * {@code noInherit} does not block it, the class-level {@code @Rest(produces)} hierarchy).
	 * Falls back to the supported media types of the operation's {@link SerializerSet}.
	 */
	private final Memoizer<List<MediaType>> supportedAcceptTypes = memoizer(() -> {
		var result = collectAnnotationMediaTypes(PROPERTY_produces);
		if (result.isEmpty())
			return u(getSerializers().getSupportedMediaTypes());
		return u(result);
	});

	/**
	 * The supported request content types for this operation.
	 *
	 * <p>
	 * Walks op-level {@code @RestOp}-group annotations for {@code consumes} (and, when
	 * {@code noInherit} does not block it, the class-level {@code @Rest(consumes)} hierarchy).
	 * Falls back to the supported content types of the operation's {@link ParserSet}.
	 */
	private final Memoizer<List<MediaType>> supportedContentTypes = memoizer(() -> {
		var result = collectAnnotationMediaTypes(PROPERTY_consumes);
		if (result.isEmpty())
			return u(getParsers().getSupportedMediaTypes());
		return u(result);
	});

	/**
	 * Iterates over each parameter annotation on the operation method, computing the parameter's
	 * {@link Schema#default_()}/{@link Schema#df()} string (joined-non-blank-first) and dispatching
	 * each annotation+default pair to the supplied callback.
	 */
	private void processParameterDefaults(java.util.function.BiConsumer<Annotation,String> callback) {
		for (var aa : method.getParameterAnnotations()) {
			String def = null;
			for (var a : aa) {
				if (a instanceof Schema s)
					def = joinnlFirstNonEmptyArray(s.default_(), s.df());
			}
			if (def == null)
				continue;
			for (var a : aa)
				callback.accept(a, def);
		}
	}

	/**
	 * Returns {@code true} if the given method has a {@code @Bean} annotation whose
	 * {@code methodScope} includes this operation's method name (or {@code "*"}).
	 *
	 * <p>
	 * Used by op-level memoizers when scanning the resource class for {@code @Bean}-supplied
	 * composite-bean overrides ({@code RestConverterList}, {@code RestGuardList}, etc.). This is the
	 * {@link RestOpContext}-scope peer of {@link Builder#matches(MethodInfo)} — kept in sync with that
	 * one.
	 */
	private boolean matchesInjectScope(MethodInfo annotated) {
		var a = annotated.getAnnotations(Bean.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (a != null) {
			for (var n : a.methodScope()) {
				if ("*".equals(n) || method.getName().equals(n))
					return true;
			}
		}
		return false;
	}

	/**
	 * Same as {@link #matchesInjectScope(MethodInfo)} but additionally requires the
	 * {@link Bean#name()} attribute to equal {@code beanName}.
	 *
	 * <p>
	 * Used for named composite-bean overrides ({@code defaultRequestHeaders}, {@code defaultResponseHeaders},
	 * etc.) where the bean type alone is ambiguous (e.g. {@link HeaderList} appears as both request and
	 * response headers).
	 */
	private boolean matchesInjectScope(MethodInfo annotated, String beanName) {
		var a = annotated.getAnnotations(Bean.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (a != null) {
			if (! a.name().equals(beanName))
				return false;
			for (var n : a.methodScope()) {
				if ("*".equals(n) || method.getName().equals(n))
					return true;
			}
		}
		return false;
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
	 * Returns the {@link RestOp}-group annotations on this method for the specified property,
	 * in <b>parent-to-child</b> order, with op-level {@code noInherit} cutoff applied.
	 *
	 * <p>
	 * Mirrors {@link RestContext#getRestAnnotationsForProperty(String)} but for the op-level chain. Used by
	 * op-level memoizers ({@link #converters}, {@link #guards}, {@link #matchersList},
	 * {@link #encoders}, etc.) when accumulating values from each {@code @RestOp} / {@code @RestGet} /
	 * {@code @RestPut} / {@code @RestPost} / {@code @RestDelete} / {@code @RestPatch} / {@code @RestOptions}
	 * annotation in the method-override chain.
	 *
	 * @param name The annotation property name (e.g. {@code "converters"}, {@code "guards"}).
	 * @return A stream of {@link AnnotationInfo} entries in parent-to-child order, never {@code null}.
	 */
	Stream<AnnotationInfo<?>> getRestOpAnnotationsForProperty(String name) {
		var annotations = getRestOpAnnotations();
		var cutoff = annotations.size();
		for (var i = 0; i < annotations.size(); i++) {
			if (resolveCdl(annotations.get(i).getStringArray(PROPERTY_noInherit).orElse(StringUtils.EMPTY_STRING_ARRAY)).anyMatch(name::equalsIgnoreCase)) {
				cutoff = i + 1;
				break;
			}
		}
		return rstream(annotations.subList(0, cutoff));
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
		var vr = varResolver();
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

	private List<MediaType> collectAnnotationMediaTypes(String attr) {
		var result = new ArrayList<MediaType>();
		var vr = varResolver();
		// Class-level @Rest(consumes|produces) first (when inheritance is allowed), then op-level overrides append.
		if (isInherited(attr)) {
			for (var ai : restContext().getRestAnnotations())
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
			if (isNotEmpty(s)) {
				var resolved = vr.resolve(s);
				if (!resolved.isEmpty())
					result.add(MediaType.of(resolved));
			}
		}
	}

	@SuppressWarnings("java:S3776")
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
	 * All operation-level configuration is resolved from {@link RestOp}-group annotations on the method and
	 * inherited from the parent {@link RestContext}.
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
	 * 3-arg positional context constructor.
	 *
	 * @param method The Java method this context represents. Must not be <jk>null</jk>.
	 * @param context The owning {@link RestContext}. Must not be <jk>null</jk>.
	 * @param resourceSupplier Supplier that returns the invocation target for this operation.
	 * @throws ServletException If context could not be created.
	 */
	public RestOpContext(java.lang.reflect.Method method, RestContext context, Supplier<Object> resourceSupplier) throws ServletException {
		this(new Builder(method, context, resourceSupplier));
	}

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	private RestOpContext(Builder builder) throws ServletException {
		super(builder);

		try {
			// Capture the annotation work-list before the builder becomes eligible for GC.
			appliedAnnotations = builder.getApplied();

			context = builder.restContext;
			method = builder.restMethod;
			resourceSupplier = builder.restResourceSupplier;

			mi = MethodInfo.of(method).accessible();

			var bs = new BasicBeanStore(context.getBootstrapBeanStore());
			bs.addBean(RestOpContext.class, this);
			bs.addBean(Method.class, method);
			bs.addBean(AnnotationWorkList.class, appliedAnnotations);
			opBeanStore = bs;

			bs.add(MarshallingContext.class, getMarshallingContext());
			bs.add(RestConverter[].class, getConverters());
			bs.add(EncoderSet.class, getEncoders());
			bs.add(RestGuard[].class, getGuards());
			bs.add(JsonSchemaGenerator.class, getJsonSchemaGenerator());
			bs.add(ParserSet.class, getParsers());
			bs.add(HttpPartParser.class, getPartParser());
			bs.add(HttpPartSerializer.class, getPartSerializer());
			bs.add(SerializerSet.class, getSerializers());

			// The 6 formerly-eager scalar fields are now memoized; no eagerness needed here.
			// Pre-warm httpMethod so it is in the memoizer cache for immediate use by compareTo/match.
			httpMethod.get();

			var pm = getPathMatchers();
			bs.add(UrlPathMatcher[].class, pm);
			bs.addBean(UrlPathMatcher.class, pm.length > 0 ? pm[0] : null);

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

		c = cmp(o.hierarchyDepth.get(), hierarchyDepth.get());
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
		return RestOpSession.create(this, session).logger(getCallLogger()).debug(debugConfig.get().resolve(this, session.getRequest()).enabled());
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
	public MarshallingContext getMarshallingContext() { return marshallingContext.get(); }

	/**
	 * Returns the default charset.
	 *
	 * @return The default charset.  Never <jk>null</jk>.
	 */
	public Charset getDefaultCharset() { return defaultCharset.get(); }

	/**
	 * Returns the default request attributes.
	 *
	 * @return The default request attributes.  Never <jk>null</jk>.
	 */
	public NamedAttributeMap getDefaultRequestAttributes() { return defaultRequestAttributes.get(); }

	/**
	 * Returns the default form data parameters.
	 *
	 * @return The default form data parameters.  Never <jk>null</jk>.
	 */
	public HttpPartList getDefaultRequestFormData() { return defaultRequestFormData.get(); }

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HttpHeaderList getDefaultRequestHeaders() { return defaultRequestHeaders.get(); }

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public HttpPartList getDefaultRequestQueryData() { return defaultRequestQueryData.get(); }

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HttpHeaderList getDefaultResponseHeaders() { return defaultResponseHeaders.get(); }

	/**
	 * Returns the compression encoders to use for this method.
	 *
	 * @return The compression encoders to use for this method.
	 */
	public EncoderSet getEncoders() { return encoders.get(); }

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 *
	 * @return The HTTP method name.
	 */
	public String getHttpMethod() { return httpMethod.get(); }

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
	public JsonSchemaGenerator getJsonSchemaGenerator() { return jsonSchemaGenerator.get(); }

	/**
	 * Returns the max number of bytes to process in the input content.
	 *
	 * @return The max number of bytes to process in the input content.
	 */
	public long getMaxInput() { return maxInput.get(); }

	/**
	 * Returns whether this operation emits RFC 7807 {@code application/problem+json} responses.
	 *
	 * <p>
	 * Tri-state inheritance semantics: an explicit {@code "true"} or {@code "false"} on the operation annotation
	 * wins; otherwise the value is inherited from {@link RestContext#isProblemDetails()} (resource-level
	 * {@code @Rest(problemDetails)}).
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#problemDetails()}
	 * 	<li class='ja'>{@link RestOp#problemDetails()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if RFC 7807 problem-details responses are enabled on this operation.
	 */
	public boolean isProblemDetails() { return problemDetails.get(); }

	/**
	 * Returns whether this operation opts into per-request virtual-thread dispatch on Java 21+.
	 *
	 * <p>
	 * The op-level {@code @RestOp(virtualThreads)} value (when non-blank) wins; otherwise the value is inherited
	 * from {@link RestContext#isVirtualThreadsEnabled()} (resource-level {@code @Rest(virtualThreads)}). Honored
	 * only when the resource-level {@link RestContext#getVirtualThreadExecutor()} is non-{@code null} (Java 21+).
	 *
	 * @return <jk>true</jk> if virtual-thread dispatch is configured on this operation.
	 */
	public boolean isVirtualThreadsEnabled() { return virtualThreadsEnabled.get(); }

	/**
	 * Returns the configured async-response timeout (milliseconds) for this operation, or {@code -1} when no
	 * value was supplied at the op or resource level — in which case the default 30-second fallback applies in
	 * {@link org.apache.juneau.rest.processor.AsyncResponseProcessor}.
	 *
	 * @return The async timeout in milliseconds, or {@code -1} when unset.
	 */
	public long getAsyncTimeoutMillis() { return asyncTimeoutMillis.get(); }

	/**
	 * Returns the parsers to use for this method.
	 *
	 * @return The parsers to use for this method.
	 */
	public ParserSet getParsers() { return parsers.get(); }

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() { return partParser.get(); }

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() { return partSerializer.get(); }

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
	public UrlPathMatcher[] getPathMatchers() { return pathMatchers.get(); }

	/**
	 * Returns the optional matchers for this operation.
	 *
	 * @return The optional matchers for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestMatcher[] getOptionalMatchers() { return optionalMatchers.get(); }

	/**
	 * Returns the required matchers for this operation.
	 *
	 * @return The required matchers for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestMatcher[] getRequiredMatchers() { return requiredMatchers.get(); }

	/**
	 * Returns the call logger for this operation.
	 *
	 * @return The call logger for this operation.
	 * 	<br>Never <jk>null</jk>.
	 */
	public CallLogger getCallLogger() { return callLogger.get(); }

	/**
	 * Returns the debug config for this operation.
	 *
	 * @return The debug config for this operation.
	 */
	public DebugConfig getDebugConfig() { return debugConfig.get(); }

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
	public ResponseBeanMeta getResponseMeta() { return responseMeta.get(); }

	/**
	 * Returns the serializers to use for this method.
	 *
	 * @return The serializers to use for this method.
	 */
	public SerializerSet getSerializers() { return serializers.get(); }

	/**
	 * Returns a list of supported accept types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedAcceptTypes() { return supportedAcceptTypes.get(); }

	/**
	 * Returns the list of supported content types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedContentTypes() { return supportedContentTypes.get(); }

	@Override /* Overridden from Object */
	public int hashCode() {
		return method.hashCode();
	}

	private static String toPartValue(Object value) {
		return value == null ? null : value.toString();
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

	RestConverter[] getConverters() { return converters.get(); }

	RestGuard[] getGuards() { return guards.get(); }

	RestOpInvoker getMethodInvoker() { return methodInvoker.get(); }

	RestOpInvoker[] getPostCallMethods() { return postCallMethods.get(); }

	RestOpInvoker[] getPreCallMethods() { return preCallMethods.get(); }
	
	private static String joinnlFirstNonEmptyArray(String[]...s) {
		for (var ss : s)
			if (ss.length > 0)
				return joinnl(ss);
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T>[] classArray(Class<? extends T> value) {
		return (Class<? extends T>[])new Class<?>[] { value };
	}


}