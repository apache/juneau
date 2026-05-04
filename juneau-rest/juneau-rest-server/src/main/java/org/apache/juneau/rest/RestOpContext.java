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
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
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
	"java:S115"  // Constants use UPPER_snakeCase convention (e.g., PROP_defaultRequestFormData)
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

		private BeanContext.Builder beanContext;
		private BasicBeanStore beanStore;
		private EncoderSet.Builder encoders;
		private HttpPartParser.Creator partParser;
		private HttpPartSerializer.Creator partSerializer;
		private JsonSchemaGenerator.Builder jsonSchemaGenerator;
		private Method restMethod;
		private ParserSet.Builder parsers;
		private RestContext restContext;
		private RestContext.Builder parent;
		private SerializerSet.Builder serializers;

		Builder(Method method, RestContext context) {

			this.restContext = context;
			this.parent = context.builder;
			this.restMethod = method;

			this.beanStore = BasicBeanStore.of(context.getBeanStore()).addBean(Method.class, method);
			var ap = context.getBeanContext().getAnnotationProvider();

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

			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		Builder beanStore(BasicBeanStore value) {
			this.beanStore = value;
			return this;
		}

		BasicBeanStore beanStore() {
			return beanStore;
		}

		BeanContext.Builder beanContext() {
			if (beanContext == null)
				beanContext = createBeanContext(beanStore(), parent, resource());
			return beanContext;
		}

		EncoderSet.Builder encoders() {
			if (encoders == null)
				encoders = createEncoders(parent);
			return encoders;
		}

		JsonSchemaGenerator.Builder jsonSchemaGenerator() {
			if (jsonSchemaGenerator == null)
				jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), parent, resource());
			return jsonSchemaGenerator;
		}

		ParserSet.Builder parsers() {
			if (parsers == null)
				parsers = createParsers(parent);
			return parsers;
		}

		HttpPartParser.Creator partParser() {
			if (partParser == null)
				partParser = createPartParser(beanStore(), parent, resource());
			return partParser;
		}

		HttpPartSerializer.Creator partSerializer() {
			if (partSerializer == null)
				partSerializer = createPartSerializer(beanStore(), parent, resource());
			return partSerializer;
		}

		SerializerSet.Builder serializers() {
			if (serializers == null)
				serializers = createSerializers(parent);
			return serializers;
		}

		@SuppressWarnings({
			"java:S1452" // Wildcard required - Supplier<?> for generic REST resource instance
		})
		Supplier<?> resource() {
			return restContext.builder.resource();
		}

		Optional<BeanContext> getBeanContext() { return opt(beanContext).map(BeanContext.Builder::build); }
		Optional<JsonSchemaGenerator> getJsonSchemaGenerator() { return opt(jsonSchemaGenerator).map(JsonSchemaGenerator.Builder::build); }
		Optional<HttpPartParser> getPartParser() { return opt(partParser).map(org.apache.juneau.httppart.HttpPartParser.Creator::create); }
		Optional<HttpPartSerializer> getPartSerializer() { return opt(partSerializer).map(Creator::create); }

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
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

		private BeanContext.Builder createBeanContext(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {
			Value<BeanContext.Builder> v = Value.of(parent.beanContext().copy());
			var bs = BasicBeanStore.of(beanStore).addBean(BeanContext.Builder.class, v.get());
			new BeanCreateMethodFinder<>(BeanContext.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			return v.get();
		}

		private static EncoderSet.Builder createEncoders(RestContext.Builder parent) {
			return parent.encoders().copy();
		}

		private JsonSchemaGenerator.Builder createJsonSchemaGenerator(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {
			Value<JsonSchemaGenerator.Builder> v = Value.of(parent.jsonSchemaGenerator().copy());
			var bs = BasicBeanStore.of(beanStore).addBean(JsonSchemaGenerator.Builder.class, v.get());
			new BeanCreateMethodFinder<>(JsonSchemaGenerator.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			return v.get();
		}

		private static ParserSet.Builder createParsers(RestContext.Builder parent) {
			return parent.parsers().copy();
		}

		private HttpPartParser.Creator createPartParser(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {
			Value<HttpPartParser.Creator> v = Value.of(parent.partParser().copy());
			var bs = BasicBeanStore.of(beanStore).addBean(HttpPartParser.Creator.class, v.get());
			new BeanCreateMethodFinder<>(HttpPartParser.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			return v.get();
		}

		private HttpPartSerializer.Creator createPartSerializer(BasicBeanStore beanStore, RestContext.Builder parent, Supplier<?> resource) {
			Value<HttpPartSerializer.Creator> v = Value.of(parent.partSerializer().copy());
			var bs = BasicBeanStore.of(beanStore).addBean(HttpPartSerializer.Creator.class, v.get());
			new BeanCreateMethodFinder<>(HttpPartSerializer.class, resource, bs)
				.find(this::matches)
				.run(x -> v.get().impl(x));
			return v.get();
		}

		private static SerializerSet.Builder createSerializers(RestContext.Builder parent) {
			return parent.serializers().copy();
		}
	}

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, HttpPartSerializer defaultSerializer) {
		return BeanCreator.of(HttpPartSerializer.class).type(c).orElse(defaultSerializer);
	}

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

	// Sub-builder fields: annotation-applied copies captured from Builder before it is discarded.
	// Non-null only when op-level annotations actually touched the sub-builder during construction.
	// null means "no op annotations applied" and the memoizer falls through to the parent context value.
	private final BeanContext.Builder beanContextBuilder;
	private final EncoderSet.Builder encodersBuilder;
	private final JsonSchemaGenerator.Builder jsonSchemaGeneratorBuilder;
	private final ParserSet.Builder parsersBuilder;
	private final HttpPartParser.Creator partParserCreator;
	private final HttpPartSerializer.Creator partSerializerCreator;
	private final SerializerSet.Builder serializersBuilder;

	// The annotation work-list produced during construction (replaces builder.getApplied() references).
	private final AnnotationWorkList appliedAnnotations;

	private RestContext restContext() { return context; }
	private Method method() { return method; }
	private BeanContext.Builder beanContextBuilder() { return beanContextBuilder; }
	private EncoderSet.Builder encodersBuilder() { return encodersBuilder; }
	private JsonSchemaGenerator.Builder jsonSchemaGeneratorBuilder() { return jsonSchemaGeneratorBuilder; }
	private ParserSet.Builder parsersBuilder() { return parsersBuilder; }
	private HttpPartParser.Creator partParserCreator() { return partParserCreator; }
	private HttpPartSerializer.Creator partSerializerCreator() { return partSerializerCreator; }
	private SerializerSet.Builder serializersBuilder() { return serializersBuilder; }

	//-----------------------------------------------------------------------------------------------------------------
	// Memoized fields
	//-----------------------------------------------------------------------------------------------------------------

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

	/** The effective {@link DebugEnablement} for this operation. */
	private final Memoizer<DebugEnablement> debugEnablement = memoizer(() -> {
		var v = findOpString(PROPERTY_debug);
		if (v.isPresent())
			return DebugEnablement.create(restContext().getBeanStore()).enable(Enablement.fromString(v.get()), "*").build();
		if (isInherited(PROPERTY_debug))
			return restContext().getDebugEnablement();
		return DebugEnablement.create(restContext().getBeanStore()).build();
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
	 * The HTTP method string for this operation (e.g. {@code "GET"}, {@code "*"}).
	 *
	 * <p>
	 * Walks {@code @RestOp}-group annotations child-to-parent; verb-specific annotations
	 * ({@link RestGet}, etc.) imply their fixed verb. Falls back to
	 * {@link HttpUtils#detectHttpMethod} when no annotation declares a verb.
	 */
	@SuppressWarnings("java:S3776")
	private final Memoizer<String> httpMethod = memoizer(() -> {
		var vr = restContext().getVarResolver();
		for (var ai : getRestOpAnnotations()) {
			var v = httpMethodFromAnnotation(ai.inner(), vr);
			if (v != null && !v.isEmpty())
				return normalizeHttpMethod(v);
		}
		return normalizeHttpMethod(HttpUtils.detectHttpMethod(method(), true, "GET"));
	});

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

	/** Aggregated {@code noInherit} keys from all RestOp-group annotations on this operation. */
	private final Memoizer<SortedSet<String>> noInheritOp = memoizer(() -> {
		var l = getRestOpAnnotations().stream()
			.map(ai -> ai.getStringArray("noInherit").orElse(StringUtils.EMPTY_STRING_ARRAY))
			.flatMap(this::resolveCdl)
			.toList();
		return u(treeSetCi(l));
	});

	/** Effective allowed parser session-option keys for this operation. */
	private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(() -> {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedParserOptions;
		if (isInherited(p))
			l.addAll(restContext().getAllowedParserOptions());
		getRestOpAnnotations().stream()
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(new String[0])))
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
			.flatMap(ai -> resolveCdl(ai.getStringArray(p).orElse(new String[0])))
			.forEach(l::add);
		return u(treeSetCi(removeNegations(l)));
	});

	/** The {@link BeanContext} for this operation (op-level annotations applied on top of the parent context). */
	private final Memoizer<BeanContext> beanContext = memoizer(() ->
		beanContextBuilder() != null ? beanContextBuilder().build() : restContext().getBeanContext()
	);

	/** The call logger for this operation (delegated to the parent {@link RestContext}). */
	private final Memoizer<CallLogger> callLogger = memoizer(() -> restContext().getCallLogger());

	/**
	 * The encoder group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(encoders)} chain (parent-to-child); each non-empty {@code encoders()}
	 * array REPLACES the inherited set (with {@link Inherit} as a sentinel that re-injects the prior
	 * set's entries at the specified position — matches the legacy {@code EncoderSet.Builder.set(...)}
	 * semantics). Falls through to the class-level {@link RestContext#getEncoders()} when no op
	 * annotation declares encoders. An {@code @RestInject EncoderSet} bean (matching this operation's
	 * method scope) REPLACES the result entirely.
	 */
	private final Memoizer<EncoderSet> encoders = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var b = (encodersBuilder() != null ? encodersBuilder() : restContext().builder.encoders()).copy();
		getRestOpAnnotationsForProperty(PROPERTY_encoders).forEach(ai -> {
			var c = ai.getClassArray("encoders", org.apache.juneau.encoders.Encoder.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var v = Value.of(b.build());
		new BeanCreateMethodFinder<>(EncoderSet.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(v::set);
		return v.get();
	});

	/** The JSON-Schema generator for this operation (op-level annotations applied on top of the parent). */
	private final Memoizer<JsonSchemaGenerator> jsonSchemaGenerator = memoizer(() ->
		jsonSchemaGeneratorBuilder() != null ? jsonSchemaGeneratorBuilder().build() : restContext().getJsonSchemaGenerator()
	);

	/**
	 * The parser group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(parsers)} chain (parent-to-child); the most-derived non-empty
	 * {@code parsers()} array REPLACES the entire inherited set. Falls through to the class-level
	 * {@link RestContext#getParsers()} when no op annotation declares parsers. An
	 * {@code @RestInject ParserSet} bean (matching this operation's method scope) REPLACES the result.
	 */
	private final Memoizer<ParserSet> parsers = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var b = (parsersBuilder() != null ? parsersBuilder() : restContext().builder.parsers()).copy();
		getRestOpAnnotationsForProperty(PROPERTY_parsers).forEach(ai -> {
			var c = ai.getClassArray("parsers", java.lang.Object.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var result = Value.of(b.build());
		new BeanCreateMethodFinder<>(ParserSet.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(result::set);
		return result.get();
	});

	/** The HTTP part parser for this operation (op-level creator applied on top of the parent). */
	private final Memoizer<HttpPartParser> partParser = memoizer(() ->
		partParserCreator() != null ? partParserCreator().create() : restContext().getPartParser()
	);

	/** The HTTP part serializer for this operation (op-level creator applied on top of the parent). */
	private final Memoizer<HttpPartSerializer> partSerializer = memoizer(() ->
		partSerializerCreator() != null ? partSerializerCreator().create() : restContext().getPartSerializer()
	);

	/**
	 * The serializer group for this operation.
	 *
	 * <p>
	 * Walks the {@code @RestOp(serializers)} chain (parent-to-child); the most-derived non-empty
	 * {@code serializers()} array REPLACES the entire inherited set. Falls through to the class-level
	 * {@link RestContext#getSerializers()} when no op annotation declares serializers. An
	 * {@code @RestInject SerializerSet} bean (matching this operation's method scope) REPLACES the result.
	 */
	private final Memoizer<SerializerSet> serializers = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var b = (serializersBuilder() != null ? serializersBuilder() : restContext().builder.serializers()).copy();
		getRestOpAnnotationsForProperty(PROPERTY_serializers).forEach(ai -> {
			var c = ai.getClassArray("serializers", org.apache.juneau.serializer.Serializer.class).orElse(null);
			if (nn(c) && c.length > 0)
				b.set(c);
		});
		var result = Value.of(b.build());
		new BeanCreateMethodFinder<>(SerializerSet.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(result::set);
		return result.get();
	});

	/**
	 * The default request attribute map for this operation.
	 *
	 * <p>
	 * Starts with the class-level value (which already incorporates
	 * {@code @Rest(defaultRequestAttributes)}), then walks the {@code @RestOp}/verb chain
	 * (parent-to-child) and adds each entry — {@link NamedAttributeMap#add} uses put-semantics so
	 * child entries override parent entries by name. An
	 * {@code @RestInject(name="defaultRequestAttributes") NamedAttributeMap} bean (matching this
	 * operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<NamedAttributeMap> defaultRequestAttributes = memoizer(() -> {
		var v = Value.of(restContext().getDefaultRequestAttributes().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestAttributes).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestAttributes).orElse(EMPTY_STRING_ARRAY))
				v.get().add(BasicNamedAttribute.ofPair(s));
		});
		new BeanCreateMethodFinder<>(NamedAttributeMap.class, restContext().getResource(), restContext().getBeanStore())
			.find(x -> matchesInjectScope(x, PROPERTY_defaultRequestAttributes))
			.run(v::set);
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
	 * {@code setDefault} = first wins). An {@code @RestInject(name="defaultRequestFormData") PartList}
	 * bean (matching this operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<PartList> defaultRequestFormData = memoizer(() -> {
		var v = Value.of(PartList.create());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestFormData).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestFormData).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(basicPart(s));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof FormData f) {
				try {
					v.get().setDefault(basicPart(firstNonEmpty(f.name(), f.value()), parseIfJson(def)));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @FormData annotation");
				}
			}
		});
		new BeanCreateMethodFinder<>(PartList.class, restContext().getResource(), restContext().getBeanStore())
			.find(x -> matchesInjectScope(x, PROPERTY_defaultRequestFormData))
			.run(v::set);
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
	 * {@code @RestInject(name="defaultRequestHeaders") HeaderList} bean (matching this operation's
	 * method scope) REPLACES the entire result.
	 */
	private final Memoizer<HeaderList> defaultRequestHeaders = memoizer(() -> {
		var v = Value.of(restContext().getDefaultRequestHeaders().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestHeaders).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestHeaders).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(stringHeader(s));
			ai.getString(PROPERTY_defaultAccept).filter(s -> !s.isEmpty()).ifPresent(s -> v.get().setDefault(accept(s)));
			ai.getString(PROPERTY_defaultContentType).filter(s -> !s.isEmpty()).ifPresent(s -> v.get().setDefault(contentType(s)));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof Header h) {
				try {
					v.get().set(basicHeader(firstNonEmpty(h.name(), h.value()), parseIfJson(def)));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @Header annotation");
				}
			}
		});
		new BeanCreateMethodFinder<>(HeaderList.class, restContext().getResource(), restContext().getBeanStore())
			.find(x -> matchesInjectScope(x, PROPERTY_defaultRequestHeaders))
			.run(v::set);
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
	 * {@code setDefault} = first wins). An {@code @RestInject(name="defaultRequestQueryData") PartList}
	 * bean (matching this operation's method scope) REPLACES the entire result.
	 */
	private final Memoizer<PartList> defaultRequestQueryData = memoizer(() -> {
		var v = Value.of(PartList.create());
		getRestOpAnnotationsForProperty(PROPERTY_defaultRequestQueryData).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultRequestQueryData).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(basicPart(s));
		});
		processParameterDefaults((paramAnn, def) -> {
			if (paramAnn instanceof Query q) {
				try {
					v.get().setDefault(basicPart(firstNonEmpty(q.name(), q.value()), parseIfJson(def)));
				} catch (ParseException e) {
					throw new ConfigException(e, "Malformed @Query annotation");
				}
			}
		});
		new BeanCreateMethodFinder<>(PartList.class, restContext().getResource(), restContext().getBeanStore())
			.find(x -> matchesInjectScope(x, PROPERTY_defaultRequestQueryData))
			.run(v::set);
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
	 * {@code @RestInject(name="defaultResponseHeaders") HeaderList} bean (matching this operation's
	 * method scope) REPLACES the entire result.
	 */
	private final Memoizer<HeaderList> defaultResponseHeaders = memoizer(() -> {
		var v = Value.of(restContext().getDefaultResponseHeaders().copy());
		getRestOpAnnotationsForProperty(PROPERTY_defaultResponseHeaders).forEach(ai -> {
			for (var s : ai.getStringArray(PROPERTY_defaultResponseHeaders).orElse(EMPTY_STRING_ARRAY))
				v.get().setDefault(stringHeader(s));
		});
		new BeanCreateMethodFinder<>(HeaderList.class, restContext().getResource(), restContext().getBeanStore())
			.find(x -> matchesInjectScope(x, PROPERTY_defaultResponseHeaders))
			.run(v::set);
		return v.get();
	});

	/**
	 * Folds method-parameter {@link Query @Query} annotations (with a {@link Schema#default_()} /
	 * {@link Schema#df()} default) into the supplied {@link PartList} using {@link PartList#setDefault}
	 * (first wins). Used by the {@link #defaultRequestQueryData} memoizer.
	 */
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
	 * The response-converter array for this operation.
	 *
	 * <p>
	 * Walks the {@code @Rest(converters)} class chain (parent-to-child) followed by the
	 * {@code @RestOp(converters)} method chain (parent-to-child). Op-level
	 * {@code noInherit={"converters"}} cuts off the class-chain contribution. An
	 * {@code @RestInject RestConverterList} bean (either as a name-anonymous bean in the bean store
	 * or as a {@code @RestInject} method whose {@code methodScope} matches this operation's method
	 * name) REPLACES the entire annotation-derived list.
	 */
	private final Memoizer<RestConverter[]> converters = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var v = Value.of(RestConverterList.create(bs));
		if (isInherited(PROPERTY_converters))
			restContext().getRestAnnotationsForProperty(PROPERTY_converters)
				.forEach(ai -> v.get().append(ai.inner().converters()));
		getRestOpAnnotationsForProperty(PROPERTY_converters)
			.forEach(ai -> ai.getClassArray("converters", RestConverter.class).ifPresent(classes -> {
				for (var c : classes)
					v.get().append(classArray(c));
			}));
		bs.getBean(RestConverterList.class).ifPresent(x -> v.get().impl(x));
		new BeanCreateMethodFinder<>(RestConverterList.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(x -> v.get().impl(x));
		return v.get().build().asArray();
	});

	/**
	 * Returns {@code true} if the given method has a {@code @RestInject} annotation whose
	 * {@code methodScope} includes this operation's method name (or {@code "*"}).
	 *
	 * <p>
	 * Used by op-level memoizers when scanning the resource class for {@code @RestInject}-supplied
	 * composite-bean overrides ({@code RestConverterList}, {@code RestGuardList}, etc.). This is the
	 * {@link RestOpContext}-scope peer of {@link Builder#matches(MethodInfo)} — kept in sync with that
	 * one.
	 */
	private boolean matchesInjectScope(MethodInfo annotated) {
		var a = annotated.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null);
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
	 * {@link RestInject#name()} attribute to equal {@code beanName}.
	 *
	 * <p>
	 * Used for named composite-bean overrides ({@code defaultRequestHeaders}, {@code defaultResponseHeaders},
	 * etc.) where the bean type alone is ambiguous (e.g. {@link HeaderList} appears as both request and
	 * response headers).
	 */
	private boolean matchesInjectScope(MethodInfo annotated, String beanName) {
		var a = annotated.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null);
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
	 * An {@code @RestInject RestGuardList} bean (via the bean store or an {@code @RestInject} method
	 * with matching {@code methodScope}) REPLACES the entire annotation-derived list (Decision #1).
	 */
	private final Memoizer<RestGuard[]> guards = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var v = Value.of(RestGuardList.create(bs));
		var rolesDeclaredSet = new java.util.LinkedHashSet<String>();
		var roleGuardStrs = new ArrayList<String>();

		java.util.function.Consumer<AnnotationInfo<?>> walk = ai -> {
			ai.getClassArray("guards", RestGuard.class).ifPresent(classes -> {
				for (var c : classes)
					v.get().append(classArray(c));
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
				v.get().append(new RoleBasedRestGuard(declaredRoles, rg));
			} catch (java.text.ParseException e) {
				throw toRex(e);
			}
		}

		bs.getBean(RestGuardList.class).ifPresent(x -> v.get().impl(x));
		new BeanCreateMethodFinder<>(RestGuardList.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(x -> v.get().impl(x));
		return v.get().build().asArray();
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
	 * An {@code @RestInject RestMatcherList} bean (via the bean store or an {@code @RestInject}
	 * method with matching {@code methodScope}) REPLACES the entire annotation-derived list.
	 */
	private final Memoizer<RestMatcherList> matchersList = memoizer(() -> {
		var bs = restContext().getBeanStore();
		var v = Value.of(RestMatcherList.create(bs));
		var clientVersion = new String[]{null};

		getRestOpAnnotationsForProperty(PROPERTY_matchers).forEach(ai -> {
			ai.getClassArray("matchers", RestMatcher.class).ifPresent(classes -> {
				for (var c : classes)
					v.get().append(classArray(c));
			});
			ai.getString("clientVersion").filter(StringUtils::isNotBlank).ifPresent(s -> clientVersion[0] = s);
		});

		if (nn(clientVersion[0]))
			v.get().append(new ClientVersionMatcher(restContext().getClientVersionHeader(), MethodInfo.of(method())));

		bs.getBean(RestMatcherList.class).ifPresent(x -> v.get().impl(x));
		new BeanCreateMethodFinder<>(RestMatcherList.class, restContext().getResource(), bs)
			.find(this::matchesInjectScope)
			.run(x -> v.get().impl(x));
		return v.get().build();
	});

	/** The optional (non-required) matchers extracted from {@link #matchersList}. */
	private final Memoizer<RestMatcher[]> optionalMatchers = memoizer(() -> matchersList.get().getOptionalEntries());

	/** The required matchers extracted from {@link #matchersList}. */
	private final Memoizer<RestMatcher[]> requiredMatchers = memoizer(() -> matchersList.get().getRequiredEntries());

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
	 * {@code @RestInject UrlPathMatcherList} bean (matching this operation's method scope) REPLACES
	 * the entire result.
	 */
	@SuppressWarnings("java:S3776")
	private final Memoizer<UrlPathMatcher[]> pathMatchers = memoizer(() -> {
		var v = Value.of(UrlPathMatcherList.create());
		getRestOpAnnotationsForProperty(PROPERTY_path).forEach(ai -> {
			for (var p : ai.getStringArray(PROPERTY_path).orElse(StringUtils.EMPTY_STRING_ARRAY))
				v.get().add(UrlPathMatcher.of(p));
			// On verb annotations (@RestGet/@RestPost/etc.) value() is always the path. On @RestOp,
			// value() is "[METHOD] [path]" where the leading method token is optional — only when a
			// space is present does the trailing token represent a path. To keep this loop annotation-
			// agnostic, we apply the @RestOp space-split rule only when an @RestOp annotation is in
			// play (i.e. the annotation type matches), and otherwise treat value() as a plain path.
			ai.getString(PROPERTY_value).filter(StringUtils::isNotBlank).map(String::trim).ifPresent(s -> {
				if (ai.inner() instanceof RestOp) {
					var i = s.indexOf(' ');
					if (i != -1)
						v.get().add(UrlPathMatcher.of(s.substring(i).trim()));
				} else {
					v.get().add(UrlPathMatcher.of(s));
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

		new BeanCreateMethodFinder<>(UrlPathMatcherList.class, restContext().getResource(), restContext().getBeanStore())
			.addBean(UrlPathMatcherList.class, v.get())
			.find(this::matchesInjectScope)
			.run(v::set);

		return v.get().asArray();
	});

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
		var vr = restContext().getVarResolver();
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
		var vr = restContext().getVarResolver();
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
	 * 3-arg positional context constructor (for internal subclass use).
	 *
	 * <p>
	 * Allows a subclass (e.g. {@link org.apache.juneau.rest.rrpc.RrpcRestOpContext}) to override the builder-time
	 * {@link BasicBeanStore} used for constructor-argument resolution. The default, reached via the 2-arg ctor, is
	 * {@link RestContext#getBeanStore()}.
	 *
	 * @param method The Java method this context represents. Must not be <jk>null</jk>.
	 * @param context The owning {@link RestContext}. Must not be <jk>null</jk>.
	 * @param beanStoreOverride An optional bean store to use in place of {@code context.getBeanStore()}. May be
	 * 		<jk>null</jk>, in which case the default store is used.
	 * @throws ServletException If context could not be created.
	 * @since 9.5.0
	 */
	protected RestOpContext(java.lang.reflect.Method method, RestContext context, BasicBeanStore beanStoreOverride) throws ServletException {
		this(nn(beanStoreOverride) ? new Builder(method, context).beanStore(beanStoreOverride) : new Builder(method, context));
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

			// Capture sub-builder fields (raw field access — null means no op annotations touched that slot).
			// The memoizer findXxx() methods check for null and fall through to the parent context value.
			beanContextBuilder = builder.beanContext;
			encodersBuilder = builder.encoders;
			jsonSchemaGeneratorBuilder = builder.jsonSchemaGenerator;
			parsersBuilder = builder.parsers;
			partParserCreator = builder.partParser;
			partSerializerCreator = builder.partSerializer;
			serializersBuilder = builder.serializers;

			context = builder.restContext;
			method = builder.restMethod;

			mi = MethodInfo.of(method).accessible();

			// @formatter:off
			var bs = BasicBeanStore.of(context.getBootstrapBeanStore())
				.addBean(RestOpContext.class, this)
				.addBean(Method.class, method)
				.addBean(AnnotationWorkList.class, appliedAnnotations);
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

			// The 6 formerly-eager scalar fields are now memoized; no eagerness needed here.
			// Pre-warm httpMethod so it is in the memoizer cache for immediate use by compareTo/match.
			httpMethod.get();

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

			responseMeta = ResponseBeanMeta.create(mi, appliedAnnotations);

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
		return RestOpSession.create(this, session).logger(getCallLogger()).debug(debugEnablement.get().isDebug(this, session.getRequest()));
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
	public BeanContext getBeanContext() { return beanContext.get(); }

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
	public PartList getDefaultRequestFormData() { return defaultRequestFormData.get(); }

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() { return defaultRequestHeaders.get(); }

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestQueryData() { return defaultRequestQueryData.get(); }

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() { return defaultResponseHeaders.get(); }

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

	RestOpInvoker getMethodInvoker() { return methodInvoker; }

	RestOpInvoker[] getPostCallMethods() { return postCallMethods; }

	RestOpInvoker[] getPreCallMethods() { return preCallMethods; }
	
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