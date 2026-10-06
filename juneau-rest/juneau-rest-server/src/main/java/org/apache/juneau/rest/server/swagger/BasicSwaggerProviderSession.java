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
package org.apache.juneau.rest.server.swagger;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.rest.server.RestOpAnnotation.*;
import static org.apache.juneau.rest.server.httppart.RestPartType.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.*;
import org.apache.juneau.commons.Items;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.reflect.ParameterInfo;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Contact;
import org.apache.juneau.http.License;
import org.apache.juneau.http.Tag;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.util.*;

import jakarta.servlet.*;

/**
 * A single session of generating a Swagger document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S112", // getSwagger(), addBodyExamples(), addRequestBeanParams() and getSchema() declare throws Exception because example generation and parsing surface arbitrary checked exceptions
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., SWAGGER_paths)
	"java:S1168", // Swagger/OpenAPI spec: null = omit field (e.g. parseList, parseMap, nullIfEmpty, toMap, firstNonEmpty)
	"java:S1192", // Swagger schema keys read more clearly inline than as constants.
	"java:S3776", // getSwagger() (plus merge(Schema) and addBodyExamples()) walks every Swagger section in one pass; splitting it would obscure the document structure
	"java:S9391", // Loops populate the mutable Swagger model in document order; stream pipelines would obscure that sequencing.
	"resource" // Resource management handled externally
})
public class BasicSwaggerProviderSession {

	private static final RichLogger LOG = RichLogger.getLogger(BasicSwaggerProviderSession.class);

	private static Set<Integer> getCodes(List<StatusCode> la, Integer def) {
		var codes = new TreeSet<Integer>();
		la.stream().flatMapToInt(a -> Arrays.stream(a.value())).forEach(codes::add);
		if (codes.isEmpty() && nn(def))
			codes.add(def);
		return codes;
	}

	private static MarshalledMap newMap(MarshalledMap om) {
		if (om == null)
			return new Json5Map();
		return om.modifiable();
	}

	private static MarshalledList nullIfEmpty(MarshalledList l) {
		return ie(l) ? null : l;
	}

	private static MarshalledMap nullIfEmpty(MarshalledMap m) {
		return ie(m) ? null : m;
	}

	static String joinnl(String[]...s) {
		for (var ss : s) {
			if (ss.length != 0)
				return StringUtils.joinnl(ss).trim();
		}
		return "";
	}

	private final RestContext context;
	private final Class<?> c;
	private final ClassInfo rci;
	private final FileFinder ff;

	private final Messages mb;

	private final VarResolverSession vr;
	private final JsonParser jp = Json5Parser.create().ignoreUnknownBeanProperties().build();

	private final JsonSchemaGeneratorSession js;

	private final Locale locale;

	/**
	 * Constructor.
	 *
	 * @param context The context of the REST object we're generating Swagger about.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param locale The language of the swagger we're asking for.
	 * @param ff The file finder to use for finding JSON files.
	 * @param messages The messages to use for finding localized strings.
	 * @param vr The variable resolver to use for resolving variables in the swagger.
	 * @param js The JSON-schema generator to use for stuff like examples.
	 */
	public BasicSwaggerProviderSession(RestContext context, Locale locale, FileFinder ff, Messages messages, VarResolverSession vr, JsonSchemaGeneratorSession js) {
		this.context = context;
		this.c = context.getResourceClass();
		this.rci = ClassInfo.of(c);
		this.ff = ff;
		this.mb = messages;
		this.vr = vr;
		this.js = js;
		this.locale = locale;
	}

	/**
	 * Generates the swagger.
	 *
	 * @return A new {@link Swagger} object.
	 * @throws Exception If an error occurred producing the Swagger.
	 */
	@SuppressWarnings({
		"java:S6541" // Single-threaded context; synchronization unnecessary
	})
	public Swagger getSwagger() throws Exception {
		// @formatter:off

		var is = ff.getStream(rci.getNameSimple() + ".json", locale).orElse(null);

		var ap = this.context.getMarshallingContext().getAnnotationProvider();

		Predicate<String> ne = Shorts::ine;
		Predicate<Collection<?>> nec = Shorts::ine;
		Predicate<Map<?,?>> nem = Shorts::ine;

		// Load swagger JSON from classpath.
		var omSwagger = Json5.DEFAULT.read(is, Json5Map.class);
		if (omSwagger == null)
			omSwagger = new Json5Map();

		// Combine it with @Rest(swagger)
		var restAnnotations = rstream(ap.find(Rest.class, rci)).map(AnnotationInfo::inner).toList();

		for (var rr : restAnnotations) {

			var sInfo = omSwagger.getMap("info", true);

			sInfo
				.appendIf(ne, "title",
					firstNonEmpty(
						sInfo.getString("title"),
						resolve(rr.title())
					)
				)
				.appendIf(ne, "description",
					firstNonEmpty(
						sInfo.getString("description"),
						resolve(rr.description())
					)
				)
				.appendIf(ne, "siteName",
					firstNonEmpty(
						sInfo.getString("siteName"),
						resolve(rr.siteName())
					)
				);

			var r = rr.swagger();

			omSwagger.append(parseMap(r.value(), "@Swagger(value) on class %s", c));

			if (! SwaggerAnnotation.empty(r)) {
				var info = omSwagger.getMap("info", true);

				info
					.appendIf(ne, "title", resolve(r.title()))
					.appendIf(ne, "description", resolve(r.description()))
					.appendIf(ne, "version", resolve(r.version()))
					.appendIf(ne, "termsOfService", resolve(r.termsOfService()))
					.appendIf(nem, "contact",
						merge(
							info.getMap("contact"),
							toMap(r.contact())
						)
					)
					.appendIf(nem, "license",
						merge(
							info.getMap("license"),
							toMap(r.license())
						)
					);
			}

			omSwagger
				.appendIf(nem, "externalDocs",
					merge(
						omSwagger.getMap("externalDocs"),
						toMap(r.externalDocs())
					)
				)
				.appendIf(nec, "tags",
					merge(
						omSwagger.getList("tags"),
						toList(r.tags())
					)
				);
		}

		omSwagger.appendIf(nem, "externalDocs", parseMap(mb.findFirstString("externalDocs"), "Messages/externalDocs on class %s", c));

		var info = omSwagger.getMap("info", true);

		info
			.appendIf(ne, "title", resolve(mb.findFirstString("title")))
			.appendIf(ne, "description", resolve(mb.findFirstString("description")))
			.appendIf(ne, "version", resolve(mb.findFirstString("version")))
			.appendIf(ne, "termsOfService", resolve(mb.findFirstString("termsOfService")))
			.appendIf(nem, "contact", parseMap(mb.findFirstString("contact"), "Messages/contact on class %s", c))
			.appendIf(nem, "license", parseMap(mb.findFirstString("license"), "Messages/license on class %s", c));

		if (info.isEmpty())
			omSwagger.remove("info");

		var produces = omSwagger.getList("produces", true);
		var consumes = omSwagger.getList("consumes", true);

		if (consumes.isEmpty())
			consumes.addAll(context.getConsumes());
		if (produces.isEmpty())
			produces.addAll(context.getProduces());

		Map<String,Json5Map> tagMap = map();
		if (omSwagger.containsKey("tags")) {
			for (var om : omSwagger.getList("tags").elements(Json5Map.class)) {
				String name = om.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}

		var s = mb.findFirstString("tags");
		if (nn(s)) {
			for (var m : parseListOrCdl(s, "Messages/tags on class %s", c).elements(Json5Map.class)) {
				var name = m.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in resource bundle on class %s", c);
				if (tagMap.containsKey(name))
					tagMap.get(name).putAll(m);
				else
					tagMap.put(name, m);
			}
		}

		// Load our existing bean definitions into our session.
		var definitions = omSwagger.getMap("definitions", true);
		for (var defId : definitions.keySet())
			js.addBeanDef(defId, new JsonMap(definitions.getMap(defId)));

		// Iterate through all the @RestOp methods.
		for (var sm : context.getRestOperations().getOpContexts()) {

			var bs = sm.getMarshallingContext().getSession();

			var m = sm.getJavaMethod();
			var mi = MethodInfo.of(m);
			var al = rstream(ap.find(mi)).filter(REST_OP_GROUP).toList();
			var mn = m.getName();

			// Honor @OpSwagger(ignore=true) — skip the operation entirely so neither the path nor the
			// HTTP-method entry shows up in the generated spec. Done before getOperation(...) is called
			// because that method has the side effect of inserting empty entries via computeIfAbsent.
			var msValue = Holder.<OpSwagger>empty();
			al.forEach(ai -> ai.getValue(OpSwagger.class, "swagger").filter(OpSwaggerAnnotation::notEmpty).ifPresent(msValue::set));
			var ms = msValue.orElseGet(() -> OpSwaggerAnnotation.create().build());
			if (ms.ignore())
				continue;

			// Get the operation from the existing swagger so far.
			var op = getOperation(omSwagger, sm.getPathPattern(), lcr(sm.getHttpMethod()));

			op.append(parseMap(ms.value(), "@OpSwagger(value) on class %s method %s", c, m));
			op.appendIf(ne, "operationId",
				firstNonEmpty(
					resolve(ms.operationId()),
					op.getString("operationId"),
					mn
				)
			);

		var summaryValue = Holder.<String>empty();
		al.forEach(ai -> ai.getValue(String.class, "summary").filter(NOT_EMPTY).ifPresent(summaryValue::set));
		op.appendIf(ne, "summary",
			firstNonEmpty(
				resolve(ms.summary()),
				resolve(mb.findFirstString(mn + ".summary")),
				op.getString("summary"),
				resolve(summaryValue.orElse(null))
			)
		);

		var descriptionValue = Holder.<String[]>empty();
		al.forEach(ai -> ai.getValue(String[].class, "description").filter(x -> x.length > 0).ifPresent(descriptionValue::set));
		op.appendIf(ne, "description",
			firstNonEmpty(
				resolve(ms.description()),
				resolve(mb.findFirstString(mn + ".description")),
				op.getString("description"),
				resolve(descriptionValue.orElse(new String[0]))
			)
		);
			op.appendIf(ne, "deprecated",
				firstNonEmpty(
					resolve(ms.deprecated()),
					(nn(m.getAnnotation(Deprecated.class)) || nn(ClassInfo.of(m.getDeclaringClass()).getAnnotations(Deprecated.class).findFirst().map(AnnotationInfo::inner).orElse(null))) ? "true" : null
				)
			);
			op.appendIf(nec, "tags",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".tags"), "Messages/tags on class %s method %s", c, m),
					parseListOrCdl(ms.tags(), "@OpSwagger(tags) on class %s method %s", c, m)
				)
			);
			op.appendIf(nec, "schemes",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".schemes"), "Messages/schemes on class %s method %s", c, m),
					parseListOrCdl(ms.schemes(), "@OpSwagger(schemes) on class %s method %s", c, m)
				)
			);
			op.appendIf(nec, "consumes",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".consumes"), "Messages/consumes on class %s method %s", c, m),
					parseListOrCdl(ms.consumes(), "@OpSwagger(consumes) on class %s method %s", c, m)
				)
			);
			op.appendIf(nec, "produces",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".produces"), "Messages/produces on class %s method %s", c, m),
					parseListOrCdl(ms.produces(), "@OpSwagger(produces) on class %s method %s", c, m)
				)
			);
			op.appendIf(nec, "parameters",
				merge(
					parseList(mb.findFirstString(mn + ".parameters"), "Messages/parameters on class %s method %s", c, m),
					parseList(ms.parameters(), "@OpSwagger(parameters) on class %s method %s", c, m)
				)
			);
			op.appendIf(nem, "responses",
				merge(
					parseMap(mb.findFirstString(mn + ".responses"), "Messages/responses on class %s method %s", c, m),
					parseMap(ms.responses(), "@OpSwagger(responses) on class %s method %s", c, m)
				)
			);
			op.appendIf(nem, "externalDocs",
				merge(
					op.getMap("externalDocs"),
					parseMap(mb.findFirstString(mn + ".externalDocs"), "Messages/externalDocs on class %s method %s", c, m),
					toMap(ms.externalDocs())
				)
			);

			if (op.containsKey("tags"))
				for (var tag : op.getList("tags").elements(String.class))
					if (! tagMap.containsKey(tag))
						tagMap.put(tag, Json5Map.of("name", tag));

			var paramMap = new Json5Map();
			if (op.containsKey("parameters"))
				for (var param : op.getList("parameters").elements(Json5Map.class))
					paramMap.put(param.getString("in") + '.' + (eq(param.getString("in"), "body") ? "body" : param.getString("name")), param);

			// Finally, look for parameters defined on method.
			for (var mpi : mi.getParameters()) {

				var pt = mpi.getParameterType();
				var type = pt.innerType();

				if (ap.has(Content.class, mpi)) {
					var param = paramMap.getMap(BODY + ".body", true).append("in", BODY);
					var schema = getSchema(param.getMap("schema"), type, bs);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(schema, x.inner()));
					rstream(ap.find(Content.class, mpi)).forEach(x -> merge(schema, x.inner().schema()));
					pushupSchemaFields(BODY, param, schema);
					param.appendIf(nem, "schema", schema);
					param.putIfAbsent("required", "true");
					addBodyExamples(sm, param, false, type, locale);

				} else if (ap.has(Query.class, mpi)) {
					var name = findAnnotationName(ap, Query.class, mpi).orElse(null);
					var param = paramMap.getMap(QUERY + "." + name, true).append("name", name).append("in", QUERY);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Query.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(QUERY, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, QUERY);

				} else if (ap.has(FormData.class, mpi)) {
					var name = findAnnotationName(ap, FormData.class, mpi).orElse(null);
					var param = paramMap.getMap(FORM_DATA + "." + name, true).append("name", name).append("in", FORM_DATA);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(FormData.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(FORM_DATA, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, FORM_DATA);

				} else if (ap.has(Header.class, mpi)) {
					var name = findAnnotationName(ap, Header.class, mpi).orElse(null);
					var param = paramMap.getMap(HEADER + "." + name, true).append("name", name).append("in", HEADER);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Header.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(HEADER, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, HEADER);

				} else if (ap.has(Path.class, mpi)) {
					var name = findAnnotationName(ap, Path.class, mpi).orElse(null);
					var param = paramMap.getMap(PATH + "." + name, true).append("name", name).append("in", PATH);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Path.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(PATH, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, PATH);
					param.putIfAbsent("required", "true");

				} else if (ap.has(Request.class, mpi)) {
					addRequestBeanParams(ap, sm, paramMap, mpi, bs);
				}
			}

			if (! paramMap.isEmpty())
				op.put("parameters", paramMap.values());

			var responses = op.getMap("responses", true);

			for (var eci : mi.getExceptionTypes()) {
				if (eci.hasAnnotation(Response.class)) {
					var la = rstream(ap.find(Response.class, eci)).map(AnnotationInfo::inner).toList();
					var la2 = rstream(ap.find(StatusCode.class, eci)).map(x -> x.inner()).toList();
					var codes = getCodes(la2, 500);
					for (var a : la) {
						for (var code : codes) {
							var om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							var schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
							rstream(ap.find(Schema.class, eci)).forEach(x -> merge(schema, x.inner()));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, "schema", schema);
					}
				}
				var methods = eci.getAllMethods();
				for (var i = methods.size() - 1; i >= 0; i--) {
					var ecmi = methods.get(i);
					var a = ecmi.getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
						if (a == null)
							a = ecmi.getReturnType().unwrap(Holder.class, Optional.class).getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
						if (nn(a) && ! isMulti(a)) {
							var ha = a.name();
							for (var code : codes) {
								var header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
								rstream(ap.find(Schema.class, ecmi)).forEach(x -> merge(header, x.inner()));
								rstream(ap.find(Schema.class, ecmi.getReturnType().unwrap(Holder.class, Optional.class))).forEach(x -> merge(header, x.inner()));
								pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header.getMap("schema"), ecmi.getReturnType().unwrap(Holder.class, Optional.class).innerType(), bs));
							}
						}
					}
				}
			}

			if (mi.hasAnnotation(Response.class) || mi.getReturnType().unwrap(Holder.class, Optional.class).hasAnnotation(Response.class)) {
				var la = rstream(ap.find(Response.class, mi)).map(x -> x.inner()).toList();
				var la2 = rstream(ap.find(StatusCode.class, mi)).map(x -> x.inner()).toList();
				var codes = getCodes(la2, 200);
				for (var a : la) {
					for (var code : codes) {
						var om = responses.getMap(String.valueOf(code), true);
						merge(om, a);
						var schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
						rstream(ap.find(Schema.class, mi)).forEach(x -> merge(schema, x.inner()));
						pushupSchemaFields(RESPONSE, om, schema);
						om.appendIf(nem, "schema", schema);
						addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
					}
			}
			if (mi.getReturnType().hasAnnotation(Response.class)) {
				var methods = mi.getReturnType().getAllMethods();
				for (var i = methods.size() - 1; i >= 0; i--) {
					var ecmi = methods.get(i);
						if (ecmi.hasAnnotation(Header.class)) {
							var a = ecmi.getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
							var ha = a.name();
							if (! isMulti(a)) {
								for (var code : codes) {
									var header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
									rstream(ap.find(Schema.class, ecmi)).forEach(x -> merge(header, x.inner()));
									rstream(ap.find(Schema.class, ecmi.getReturnType().unwrap(Holder.class, Optional.class))).forEach(x -> merge(header, x.inner()));
									merge(header, a.schema());
									pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header, ecmi.getReturnType().innerType(), bs));
								}
							}
						}
					}
				}
			} else if (m.getGenericReturnType() != void.class) {
				var om = responses.getMap("200", true);
				var pt2 = ClassInfo.of(m.getGenericReturnType());
				var schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
				rstream(ap.find(Schema.class, pt2)).forEach(x -> merge(schema, x.inner()));
				pushupSchemaFields(RESPONSE, om, schema);
				om.appendIf(nem, "schema", schema);
				addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
			}

			// Finally, look for Value @Header parameters defined on method.
			for (var mpi : mi.getParameters()) {

				var pt = mpi.getParameterType();

				if (pt.is(Holder.class) && (ap.has(Header.class, mpi))) {
					var la = rstream(ap.find(Header.class, mpi)).map(AnnotationInfo::inner).toList();
					var la2 = rstream(ap.find(StatusCode.class, mpi)).map(AnnotationInfo::inner).toList();
					var codes = getCodes(la2, 200);
					var name = findAnnotationName(ap, Header.class, mpi).orElse(null);
					var type = Holder.unwrap(mpi.getParameterType().innerType());
					for (var a : la) {
						if (! isMulti(a)) {
							for (var code : codes) {
								var header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(name, true);
								rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(header, x.inner()));
								merge(header, a.schema());
								pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header, type, bs));
							}
						}
					}

				} else if (ap.has(Response.class, mpi)) {
					var la = rstream(ap.find(Response.class, mpi)).map(AnnotationInfo::inner).toList();
					var la2 = rstream(ap.find(StatusCode.class, mpi)).map(AnnotationInfo::inner).toList();
					var codes = getCodes(la2, 200);
					var type = Holder.unwrap(mpi.getParameterType().innerType());
					for (var a : la) {
						for (var code : codes) {
							var om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							var schema = getSchema(om.getMap("schema"), type, bs);
							rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(schema, x.inner()));
							la.forEach(x -> merge(schema, x.schema()));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, "schema", schema);
						}
					}
				}
			}

			// Add default response descriptions.
			for (var e : responses.entrySet()) {
				var key = e.getKey();
				var val = responses.getMap(key);
				if (isDecimal(key))
					val.appendIfAbsentIf(ne, "description", RestUtils.getHttpResponseText(Integer.parseInt(key)));
			}

			if (responses.isEmpty())
				op.remove("responses");
			else
				op.put("responses", new TreeMap<>(responses));

			if (! op.containsKey("consumes")) {
				var mConsumes = sm.getSupportedContentTypes();
				if (! mConsumes.equals(consumes))
					op.put("consumes", mConsumes);
			}

			if (! op.containsKey("produces")) {
				var mProduces = sm.getSupportedAcceptTypes();
				if (! mProduces.equals(produces))
					op.put("produces", mProduces);
			}
		}

		if (nn(js.getBeanDefs()))
			for (var e : js.getBeanDefs().entrySet())
				definitions.put(e.getKey(), fixSwaggerExtensions(e.getValue()));

		if (definitions.isEmpty())
			omSwagger.remove("definitions");

		if (! tagMap.isEmpty())
			omSwagger.put("tags", tagMap.values());

		if (consumes.isEmpty())
			omSwagger.remove("consumes");
		if (produces.isEmpty())
			omSwagger.remove("produces");

		try {
			var swaggerJson = Json5R.of(omSwagger);
			return jp.read(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new ServletException("Error detected in swagger.", e);
		}
		// @formatter:on
	}

	private void addBodyExamples(RestOpContext sm, MarshalledMap piri, boolean response, Type type, Locale locale) throws Exception {

		var sex = piri.getString("example");

		if (sex == null) {
			var schema = resolveRef(piri.getMap("schema"));
			if (nn(schema))
				sex = schema.getString("example", schema.getString("example"));
		}

		if (isEmpty(sex))
			return;

		Object example = null;
		if (isProbablyJson(sex)) {
			example = jp.read(sex, type);
		} else {
			var cm = js.getClassMeta(type);
			if (cm.hasStringMutater()) {
				example = BasicConverter.INSTANCE.to(sex, cm.inner());
			}
		}

		var examplesKey = "examples";  // Parameters don't have an examples attribute.

		var examples = piri.getMap(examplesKey);
		if (examples == null)
			examples = new Json5Map();

		var mediaTypes = response ? sm.getSerializers().getSupportedMediaTypes() : sm.getParsers().getSupportedMediaTypes();

		for (var mt : mediaTypes) {
			if (mt != MediaType.HTML) {
				var s2 = sm.getSerializers().getSerializer(mt).orElse(null);
				if (nn(s2)) {
					try {
						// @formatter:off
						var eVal = s2
							.createSession()
							.locale(locale)
							.mediaType(mt)
							.apply(WriterSerializerSession.Builder.class, x -> x.useWhitespace(true))
							.build()
							.writeToString(example);
						// @formatter:on
						examples.put(s2.getPrimaryMediaType().toString(), eVal);
					} catch (Exception e) {
						LOG.warning("Could not serialize to media type [{}]: {}", mt, localizedMessage(e));  // NOT DEBUG
					}
				}
			}
		}

		if (! examples.isEmpty())
			piri.put(examplesKey, examples);
	}

	@SuppressWarnings({
		"java:S1172", // Parameter kept to match interface contract
		"unused" // Eclipse: type parameter unused but kept for API consistency
	})
	/**
	 * Documents each annotated getter of a {@link Request @Request} bean parameter as its own part parameter, merging
	 * the getter's {@link Schema @Schema} and part-annotation {@code schema} the same way as for a plain part argument.
	 *
	 * <p>
	 * {@link Content @Content} getters are skipped: a request bean's body is documented by the bean's own schema, not
	 * by a part parameter.
	 */
	private void addRequestBeanParams(AnnotationProvider ap, RestOpContext sm, Json5Map paramMap, ParameterInfo mpi, MarshallingSession bs) throws Exception {
		var rbm = RequestBeanMeta.create(mpi, AnnotationWorkList.create());
		if (rbm == null)
			return;
		for (var pm : rbm.getProperties()) {
			var mi = MethodInfo.of(pm.getGetter());
			var type = pm.getGetter().getGenericReturnType();
			var schemas = new ArrayList<Schema>();
			rstream(ap.find(Schema.class, mi)).forEach(x -> schemas.add(x.inner()));
			RestPartType in;
			switch (pm.getPartType()) {
				case QUERY -> {
					in = QUERY;
					rstream(ap.find(Query.class, mi)).forEach(x -> schemas.add(x.inner().schema()));
				}
				case FORMDATA -> {
					in = FORM_DATA;
					rstream(ap.find(FormData.class, mi)).forEach(x -> schemas.add(x.inner().schema()));
				}
				case HEADER -> {
					in = HEADER;
					rstream(ap.find(Header.class, mi)).forEach(x -> schemas.add(x.inner().schema()));
				}
				case PATH -> {
					in = PATH;
					rstream(ap.find(Path.class, mi)).forEach(x -> schemas.add(x.inner().schema()));
				}
				default -> {
					continue;
				}
			}
			var name = pm.getPartName();
			var param = paramMap.getMap(in + "." + name, true).append("name", name).append("in", in);
			schemas.forEach(x -> merge(param, x));
			pushupSchemaFields(in, param, getSchema(param.getMap("schema"), type, bs));
			addParamExample(sm, param, in);
			if (in == PATH)
				param.putIfAbsent("required", "true");
		}
	}

	private static void addParamExample(RestOpContext sm, MarshalledMap piri, RestPartType in) {

		var s = piri.getString("example");

		if (isEmpty(s))
			return;

		var examples = piri.getMap("examples");
		if (examples == null)
			examples = new Json5Map();

		var paramName = piri.getString("name");

		if (in == QUERY)
			s = "?" + urlEncodeLax(paramName) + "=" + urlEncodeLax(s);
		else if (in == FORM_DATA)
			s = paramName + "=" + s;
		else if (in == HEADER)
			s = paramName + ": " + s;
		else if (in == PATH)
			s = sm.getPathPattern().replace("{" + paramName + "}", urlEncodeLax(s));

		examples.put("example", s);

		if (! examples.isEmpty())
			piri.put("examples", examples);
	}

	@SafeVarargs
	private static final <T> T firstNonEmpty(T...t) {
		for (var oo : t)
			if (ine(oo))
				return oo;
		return null;
	}

	/**
	 * Replaces non-standard JSON-Schema attributes with standard Swagger attributes.
	 */
	private static MarshalledMap fixSwaggerExtensions(MarshalledMap om) {
		Predicate<Object> nn = Shorts::nn;
		// @formatter:off
		om
			.appendIf(nn, "discriminator", om.remove("x-discriminator"))
			.appendIf(nn, "readOnly", om.remove("x-readOnly"))
			.appendIf(nn, "xml", om.remove("x-xml"))
			.appendIf(nn, "externalDocs", om.remove("x-externalDocs"))
			.appendIf(nn, "example", om.remove("x-example"));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private static MarshalledMap getOperation(MarshalledMap om, String path, String httpMethod) {
		om = (MarshalledMap) om.computeIfAbsent("paths", k -> new Json5Map());
		om = (MarshalledMap) om.computeIfAbsent(path, k -> new Json5Map());
		om.computeIfAbsent(httpMethod, k -> new Json5Map());
		return (MarshalledMap) om.get(httpMethod);
	}

	private MarshalledMap getSchema(MarshalledMap schema, Type type, MarshallingSession bs) throws Exception {

		if (type == Swagger.class)
			return Json5Map.create();

		schema = newMap(schema);

		var cm = bs.getClassMeta(type);

		if (schema.is("ignore", false))
			return null;

		if (schema.containsKey("type") || schema.containsKey("$ref"))
			return schema;

		return fixSwaggerExtensions(schema.append(js.getSchema(cm)));
	}

	private static boolean isMulti(Header h) {
		return eq(h.name(), "*") || eq(h.value(), "*");
	}

	private static MarshalledList merge(MarshalledList...lists) {
		var l = lists[0];
		for (var i = 1; i < lists.length; i++) {
			if (nn(lists[i])) {
				if (l == null)
					l = new Json5List();
				l.addAll(lists[i]);
			}
		}
		return l;
	}

	private static MarshalledMap merge(MarshalledMap...maps) {
		var m = maps[0];
		for (var i = 1; i < maps.length; i++) {
			if (nn(maps[i])) {
				if (m == null)
					m = new Json5Map();
				m.putAll(maps[i]);
			}
		}
		return m;
	}

	private MarshalledMap merge(MarshalledMap om, ExternalDocs a) {
		if (ExternalDocsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Shorts::ine;
		// @formatter:off
		return om
			.appendIf(ne, "description", resolve(a.description()))
			.appendIf(ne, "url", a.url())
		;
		// @formatter:on
	}

	private MarshalledMap merge(MarshalledMap om, Header[] a) {
		if (a.length == 0)
			return om;
		om = newMap(om);
		for (var aa : a) {
			var name = StringUtils.firstNonEmpty(aa.name(), aa.value());
			if (isEmpty(name))
				throw iaex("@Header used without name or value.");
			merge(om.getMap(name, true), aa.schema());
		}
		return om;
	}

	private MarshalledMap merge(MarshalledMap om, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Shorts::ine;
		Predicate<Collection<?>> nec = Shorts::ine;
		Predicate<Map<?,?>> nem = Shorts::ine;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = Shorts::nm1;
		// @formatter:off
		return om
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a.default_(), a.df()))
			.appendFirst(nec, "enum", toSet(a.enum_()), toSet(a.e()))
			.appendFirst(ne, "format", a.format(), a.f())
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendIf(nem, "items", merge(om.getMap("items"), a.items()))
			.appendFirst(ne, "maximum", a.maximum(), a.max())
			.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
			.appendFirst(ne, "minimum", a.minimum(), a.min())
			.appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl())
			.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
			.appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendFirst(ne, "type", a.type(), a.t())
			.appendIf(ne, "$ref", a.$ref())
		;
		// @formatter:on
	}

	private MarshalledMap merge(MarshalledMap om, Response a) throws ParseException {
		if (ResponseAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<Map<?,?>> nem = Shorts::ine;
		if (! SchemaAnnotation.empty(a.schema()))
			merge(om, a.schema());
		// @formatter:off
		return om
			.appendIf(nem, "examples", parseMap(a.examples()))
			.appendIf(nem, "headers", merge(om.getMap("headers"), a.headers()))
			.appendIf(nem, "schema", merge(om.getMap("schema"), a.schema()))
		;
		// @formatter:on
	}

	private MarshalledMap merge(MarshalledMap om, Schema a) {
		try {
			if (SchemaAnnotation.empty(a))
				return om;
			om = newMap(om);
			Predicate<String> ne = Shorts::ine;
			Predicate<Collection<?>> nec = Shorts::ine;
			Predicate<Map<?,?>> nem = Shorts::ine;
			Predicate<Boolean> nf = ObjectUtils::isTrue;
			Predicate<Long> nm1 = Shorts::nm1;
			// @formatter:off
			return om
				.appendIf(ne, "allOf", joinnl(a.allOf()))
				.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
				.appendIf(ne, "default", joinnl(a.default_(), a.df()))
				.appendIf(ne, "discriminator", a.discriminator())
				.appendIf(ne, "description", resolve(a.description(), a.d()))
				.appendFirst(nec, "enum", toSet(a.enum_()), toSet(a.e()))
				.appendIf(nf, "exclusiveMaximum", a.emax())
				.appendIf(nf, "exclusiveMinimum", a.emin())
				.appendIf(nem, "externalDocs", merge(om.getMap("externalDocs"), a.externalDocs()))
				.appendFirst(ne, "format", a.format(), a.f())
				.appendIf(ne, "ignore", a.ignore() ? "true" : null)
				.appendIf(nem, "items", merge(om.getMap("items"), a.items()))
				.appendFirst(ne, "maximum", a.maximum(), a.max())
				.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
				.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
				.appendFirst(nm1, "maxProperties", a.maxProperties(), a.maxp())
				.appendFirst(ne, "minimum", a.minimum(), a.min())
				.appendFirst(nm1, "minItems", a.minItems(), a.mini())
				.appendFirst(nm1, "minLength", a.minLength(), a.minl())
				.appendFirst(nm1, "minProperties", a.minProperties(), a.minp())
				.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
				.appendFirst(ne, "pattern", a.pattern(), a.p())
				.appendIf(nf, "readOnly", a.readOnly() || a.ro())
				.appendIf(nf, "required", a.required() || a.r())
				.appendIf(ne, "summary", resolve(firstNonEmpty(a.summary(), a.su())))
				.appendIf(ne, "title", a.title())
				.appendFirst(ne, "type", a.type(), a.t())
				.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
				.appendIf(ne, "xml", joinnl(a.xml()))
				.appendIf(ne, "$ref", a.$ref())
			;
			// @formatter:on
		} catch (ParseException e) {
			throw iaex(e);
		}
	}

	private MarshalledMap merge(MarshalledMap om, SubItems a) throws ParseException {
		if (SubItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Shorts::ine;
		Predicate<Collection<?>> nec = Shorts::ine;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = Shorts::nm1;
		// @formatter:off
		return om
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a.default_(), a.df()))
			.appendFirst(nec, "enum", toSet(a.enum_()), toSet(a.e()))
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendFirst(ne, "format", a.format(), a.f())
			.appendFirst(ne, "maximum", a.maximum(), a.max())
			.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
			.appendFirst(ne, "minimum", a.minimum(), a.min())
			.appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl())
			.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
			.appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendFirst(ne, "type", a.type(), a.t())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendIf(ne, "$ref", a.$ref())
		;
		// @formatter:on
	}

	private MarshalledList parseList(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			var s = (o instanceof String[] o2 ? joinnl(o2) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			if (! isProbablyJsonArray(s, true))
				s = "[" + s + "]";
			return Json5List.ofString(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in " + location + ".", locationArgs);
		}
	}

	private MarshalledList parseListOrCdl(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			var s = (o instanceof String[] o2 ? joinnl(o2) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			return Json5List.ofJson5OrCdl(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in " + location + ".", locationArgs);
		}
	}

	private MarshalledMap parseMap(Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[] o2)
			o = joinnl(o2);
		if (o instanceof String o2) {
			if (o2.isEmpty())
				return null;
			o2 = resolve(o2);
			if (eqic("IGNORE", o2))
				return Json5Map.of("ignore", true);
			if (! isProbablyJsonObject(o2, true))
				o2 = "{" + o2 + "}";
			return Json5Map.ofString(o2);
		}
		if (o instanceof Json5Map o2)
			return o2;
		if (o instanceof MarshalledMap o2)
			return new Json5Map(o2);
		throw new SwaggerException(null, "Unexpected data type '%s'.  Expected Map or String.", cn(o));
	}

	private MarshalledMap parseMap(String o, String location, Object...args) throws ParseException {
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private MarshalledMap parseMap(String[] o, String location, Object...args) throws ParseException {
		if (o.length == 0)
			return Json5Map.EMPTY_MAP;
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private static MarshalledMap pushupSchemaFields(RestPartType type, MarshalledMap param, MarshalledMap schema) {
		// @formatter:off
		Predicate<Object> ne = Shorts::ine;
		if (nn(schema) && ! schema.isEmpty()) {
			if (type == BODY || type == RESPONSE) {
				param
					.appendIf(ne, "description", schema.remove("description"));
			} else {
				param
					.appendIfAbsentIf(ne, "collectionFormat", schema.remove("collectionFormat"))
					.appendIfAbsentIf(ne, "default", schema.remove("default"))
					.appendIfAbsentIf(ne, "description", schema.remove("description"))
					.appendIfAbsentIf(ne, "enum", schema.remove("enum"))
					.appendIfAbsentIf(ne, "example", schema.remove("example"))
					.appendIfAbsentIf(ne, "exclusiveMaximum", schema.remove("exclusiveMaximum"))
					.appendIfAbsentIf(ne, "exclusiveMinimum", schema.remove("exclusiveMinimum"))
					.appendIfAbsentIf(ne, "format", schema.remove("format"))
					.appendIfAbsentIf(ne, "items", schema.remove("items"))
					.appendIfAbsentIf(ne, "maximum", schema.remove("maximum"))
					.appendIfAbsentIf(ne, "maxItems", schema.remove("maxItems"))
					.appendIfAbsentIf(ne, "maxLength", schema.remove("maxLength"))
					.appendIfAbsentIf(ne, "minimum", schema.remove("minimum"))
					.appendIfAbsentIf(ne, "minItems", schema.remove("minItems"))
					.appendIfAbsentIf(ne, "minLength", schema.remove("minLength"))
					.appendIfAbsentIf(ne, "multipleOf", schema.remove("multipleOf"))
					.appendIfAbsentIf(ne, "pattern", schema.remove("pattern"))
					.appendIfAbsentIf(ne, "required", schema.remove("required"))
					.appendIfAbsentIf(ne, "type", schema.remove("type"))
					.appendIfAbsentIf(ne, "uniqueItems", schema.remove("uniqueItems"));

				if (eq(param.getString("type"), "object") && ! schema.isEmpty())
					param.put("schema", schema);
			}
		}

		return param;
		// @formatter:on
	}

	private MarshalledList resolve(MarshalledList om) throws ParseException {
		var ol2 = new Json5List();
		for (var val : om) {
			if (val instanceof MarshalledMap val2) {
				val = resolve(val2);
			} else if (val instanceof MarshalledList val2) {
				val = resolve(val2);
			} else if (val instanceof String val2) {
				val = resolve(val2);
			}
			ol2.add(val);
		}
		return ol2;
	}

	private MarshalledMap resolve(MarshalledMap om) throws ParseException {
		MarshalledMap om2 = null;
		if (om.containsKey("_value")) {
			om = om.modifiable();
			om2 = parseMap(om.remove("_value"));
		} else {
			om2 = new Json5Map();
		}
		for (var e : om.entrySet()) {
			var val = e.getValue();
			if (val instanceof MarshalledMap val2) {
				val = resolve(val2);
			} else if (val instanceof MarshalledList val2) {
				val = resolve(val2);
			} else if (val instanceof String val2) {
				val = resolve(val2);
			}
			om2.put(e.getKey(), val);
		}
		return om2;
	}

	private String resolve(String s) {
		if (s == null)
			return null;
		return vr.resolve(s.trim());
	}

	private String resolve(String[]...s) {
		for (var ss : s) {
			if (ss.length != 0)
				return resolve(joinnl(ss));
		}
		return null;
	}

	private MarshalledMap resolveRef(MarshalledMap m) {
		if (m == null)
			return null;
		if (m.containsKey("$ref") && nn(js.getBeanDefs())) {
			var ref = m.getString("$ref");
			if (ref.startsWith("#/definitions/"))
				return js.getBeanDefs().get(ref.substring(14));
		}
		return m;
	}

	private MarshalledList toList(Tag[] aa) {
		if (aa.length == 0)
			return null;
		var ol = new Json5List();
		for (var a : aa)
			ol.add(toMap(a));
		return nullIfEmpty(ol);
	}

	private MarshalledMap toMap(Contact a) {
		if (ContactAnnotation.empty(a))
			return null;
		Predicate<String> ne = Shorts::ine;
		// @formatter:off
		var om = Json5Map.create()
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "url", resolve(a.url()))
			.appendIf(ne, "email", resolve(a.email()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private MarshalledMap toMap(ExternalDocs a) {
		if (ExternalDocsAnnotation.empty(a))
			return null;
		Predicate<String> ne = Shorts::ine;
		// @formatter:off
		var om = Json5Map.create()
			.appendIf(ne, "description", resolve(joinnl(a.description())))
			.appendIf(ne, "url", resolve(a.url()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private MarshalledMap toMap(License a) {
		if (LicenseAnnotation.empty(a))
			return null;
		Predicate<String> ne = Shorts::ine;
		// @formatter:off
		var om = Json5Map.create()
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "url", resolve(a.url()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private MarshalledMap toMap(Tag a) {
		var om = Json5Map.create();
		Predicate<String> ne = Shorts::ine;
		Predicate<Map<?,?>> nem = Shorts::ine;
		// @formatter:off
		om
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "description", resolve(joinnl(a.description())))
			.appendIf(nem, "externalDocs", merge(om.getMap("externalDocs"), toMap(a.externalDocs())));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private static Optional<String> findAnnotationName(AnnotationProvider ap, Class<? extends java.lang.annotation.Annotation> type, ParameterInfo pi) {
		// @formatter:off
		return ap.find(type, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> {
				var nv = getNameValue(x);
				return isAnyNotBlank(nv[0], nv[1]);
			})
			.findFirst()
			.map(x -> {
				var nv = getNameValue(x);
				return firstNonBlank(nv[0], nv[1]);
			});
		// @formatter:on
	}

	private static String[] getNameValue(java.lang.annotation.Annotation a) {
		if (a instanceof Query a2) return new String[]{a2.name(), a2.value()};
		if (a instanceof Header a2) return new String[]{a2.name(), a2.value()};
		if (a instanceof FormData a2) return new String[]{a2.name(), a2.value()};
		if (a instanceof Path a2) return new String[]{a2.name(), a2.value()};
		return new String[]{"", ""};
	}

	private static Set<String> toSet(String[] ss) {
		if (ss.length == 0)
			return null;
		Set<String> set = set();
		for (var s : ss)
			split(s, set::add);
		return set.isEmpty() ? null : set;
	}
}
