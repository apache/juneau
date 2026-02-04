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
package org.apache.juneau.rest.swagger;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.rest.annotation.RestOpAnnotation.*;
import static org.apache.juneau.rest.httppart.RestPartType.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Contact;
import org.apache.juneau.http.annotation.License;
import org.apache.juneau.http.annotation.Tag;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

import jakarta.servlet.*;

/**
 * A single session of generating a Swagger document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
@SuppressWarnings({"resource","java:S1168","java:S115"})
public class BasicSwaggerProviderSession {

	// Swagger JSON property name constants
	private static final String SWAGGER_additionalProperties = "additionalProperties";
	private static final String SWAGGER_allOf = "allOf";
	private static final String SWAGGER_body = "body";
	private static final String SWAGGER_collectionFormat = "collectionFormat";
	private static final String SWAGGER_consumes = "consumes";
	private static final String SWAGGER_contact = "contact";
	private static final String SWAGGER_default = "default";
	private static final String SWAGGER_definitions = "definitions";
	private static final String SWAGGER_deprecated = "deprecated";
	private static final String SWAGGER_description = "description";
	private static final String SWAGGER_discriminator = "discriminator";
	private static final String SWAGGER_email = "email";
	private static final String SWAGGER_enum = "enum";
	private static final String SWAGGER_example = "example";
	private static final String SWAGGER_examples = "examples";
	private static final String SWAGGER_exclusiveMaximum = "exclusiveMaximum";
	private static final String SWAGGER_exclusiveMinimum = "exclusiveMinimum";
	private static final String SWAGGER_externalDocs = "externalDocs";
	private static final String SWAGGER_format = "format";
	private static final String SWAGGER_headers = "headers";
	private static final String SWAGGER_ignore = "ignore";
	private static final String SWAGGER_in = "in";
	private static final String SWAGGER_info = "info";
	private static final String SWAGGER_items = "items";
	private static final String SWAGGER_license = "license";
	private static final String SWAGGER_maxItems = "maxItems";
	private static final String SWAGGER_maxLength = "maxLength";
	private static final String SWAGGER_maxProperties = "maxProperties";
	private static final String SWAGGER_maximum = "maximum";
	private static final String SWAGGER_minItems = "minItems";
	private static final String SWAGGER_minLength = "minLength";
	private static final String SWAGGER_minProperties = "minProperties";
	private static final String SWAGGER_minimum = "minimum";
	private static final String SWAGGER_multipleOf = "multipleOf";
	private static final String SWAGGER_name = "name";
	private static final String SWAGGER_object = "object";
	private static final String SWAGGER_operationId = "operationId";
	private static final String SWAGGER_parameters = "parameters";
	private static final String SWAGGER_paths = "paths";
	private static final String SWAGGER_pattern = "pattern";
	private static final String SWAGGER_produces = "produces";
	private static final String SWAGGER_properties = "properties";
	private static final String SWAGGER_readOnly = "readOnly";
	private static final String SWAGGER_required = "required";
	private static final String SWAGGER_responses = "responses";
	private static final String SWAGGER_schema = "schema";
	private static final String SWAGGER_schemes = "schemes";
	private static final String SWAGGER_summary = "summary";
	private static final String SWAGGER_tags = "tags";
	private static final String SWAGGER_termsOfService = "termsOfService";
	private static final String SWAGGER_title = "title";
	private static final String SWAGGER_true = "true";
	private static final String SWAGGER_type = "type";
	private static final String SWAGGER_uniqueItems = "uniqueItems";
	private static final String SWAGGER_url = "url";
	private static final String SWAGGER_version = "version";
	private static final String SWAGGER_xml = "xml";
	private static final String SWAGGER_$ref = "$ref";

	// JSON-Schema extension property name constants (x-prefixed)
	private static final String JSONSCHEMA_x_discriminator = "x-discriminator";
	private static final String JSONSCHEMA_x_example = "x-example";
	private static final String JSONSCHEMA_x_externalDocs = "x-externalDocs";
	private static final String JSONSCHEMA_x_readOnly = "x-readOnly";
	private static final String JSONSCHEMA_x_xml = "x-xml";

	// Other constant values
	private static final String CONST_200 = "200";
	private static final String CONST_IGNORE = "IGNORE";
	private static final String CONST_value = "_value";

	private static Set<Integer> getCodes(List<StatusCode> la, Integer def) {
		var codes = new TreeSet<Integer>();
		for (var a : la) {
			for (var i : a.value())
				codes.add(i);
		}
		if (codes.isEmpty() && nn(def))
			codes.add(def);
		return codes;
	}

	private static JsonMap newMap(JsonMap om) {
		if (om == null)
			return new JsonMap();
		return om.modifiable();
	}

	private static JsonList nullIfEmpty(JsonList l) {
		return (l == null || l.isEmpty() ? null : l);
	}

	private static JsonMap nullIfEmpty(JsonMap m) {
		return (m == null || m.isEmpty() ? null : m);
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
	private final JsonParser jp = JsonParser.create().ignoreUnknownBeanProperties().build();

	private final JsonSchemaGeneratorSession js;

	private final Locale locale;

	/**
	 * Constructor.
	 *
	 * @param context The context of the REST object we're generating Swagger about.
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
	@SuppressWarnings({ "java:S3776", "java:S6541" })
	public Swagger getSwagger() throws Exception {
		// @formatter:off

		var is = ff.getStream(rci.getNameSimple() + ".json", locale).orElse(null);

		var ap = this.context.getBeanContext().getAnnotationProvider();

		Predicate<String> ne = Utils::ne;
		Predicate<Collection<?>> nec = Utils::ne;
		Predicate<Map<?,?>> nem = Utils::ne;

		// Load swagger JSON from classpath.
		var omSwagger = Json5.DEFAULT.read(is, JsonMap.class);
		if (omSwagger == null)
			omSwagger = new JsonMap();

		// Combine it with @Rest(swagger)
		var restAnnotations = rstream(ap.find(Rest.class, rci)).map(AnnotationInfo::inner).toList();

		for (var rr : restAnnotations) {

			var sInfo = omSwagger.getMap(SWAGGER_info, true);

			sInfo
				.appendIf(ne, SWAGGER_title,
					firstNonEmpty(
						sInfo.getString(SWAGGER_title),
						resolve(rr.title())
					)
				)
				.appendIf(ne, SWAGGER_description,
					firstNonEmpty(
						sInfo.getString(SWAGGER_description),
						resolve(rr.description())
					)
				);

			var r = rr.swagger();

			omSwagger.append(parseMap(r.value(), "@Swagger(value) on class {0}", c));

			if (! SwaggerAnnotation.empty(r)) {
				var info = omSwagger.getMap(SWAGGER_info, true);

				info
					.appendIf(ne, SWAGGER_title, resolve(r.title()))
					.appendIf(ne, SWAGGER_description, resolve(r.description()))
					.appendIf(ne, SWAGGER_version, resolve(r.version()))
					.appendIf(ne, SWAGGER_termsOfService, resolve(r.termsOfService()))
					.appendIf(nem, SWAGGER_contact,
						merge(
							info.getMap(SWAGGER_contact),
							toMap(r.contact(), "@Swagger(contact) on class {0}", c)
						)
					)
					.appendIf(nem, SWAGGER_license,
						merge(
							info.getMap(SWAGGER_license),
							toMap(r.license(), "@Swagger(license) on class {0}", c)
						)
					);
			}

			omSwagger
				.appendIf(nem, SWAGGER_externalDocs,
					merge(
						omSwagger.getMap(SWAGGER_externalDocs),
						toMap(r.externalDocs(), "@Swagger(externalDocs) on class {0}", c)
					)
				)
				.appendIf(nec, SWAGGER_tags,
					merge(
						omSwagger.getList(SWAGGER_tags),
						toList(r.tags(), "@Swagger(tags) on class {0}", c)
					)
				);
		}

		omSwagger.appendIf(nem, SWAGGER_externalDocs, parseMap(mb.findFirstString("externalDocs"), "Messages/externalDocs on class {0}", c));

		var info = omSwagger.getMap(SWAGGER_info, true);

		info
			.appendIf(ne, SWAGGER_title, resolve(mb.findFirstString("title")))
			.appendIf(ne, SWAGGER_description, resolve(mb.findFirstString("description")))
			.appendIf(ne, SWAGGER_version, resolve(mb.findFirstString("version")))
			.appendIf(ne, SWAGGER_termsOfService, resolve(mb.findFirstString("termsOfService")))
			.appendIf(nem, SWAGGER_contact, parseMap(mb.findFirstString("contact"), "Messages/contact on class {0}", c))
			.appendIf(nem, SWAGGER_license, parseMap(mb.findFirstString("license"), "Messages/license on class {0}", c));

		if (info.isEmpty())
			omSwagger.remove(SWAGGER_info);

		var produces = omSwagger.getList(SWAGGER_produces, true);
		var consumes = omSwagger.getList(SWAGGER_consumes, true);

		if (consumes.isEmpty())
			consumes.addAll(context.getConsumes());
		if (produces.isEmpty())
			produces.addAll(context.getProduces());

		Map<String,JsonMap> tagMap = map();
		if (omSwagger.containsKey(SWAGGER_tags)) {
			for (var om : omSwagger.getList(SWAGGER_tags).elements(JsonMap.class)) {
				String name = om.getString(SWAGGER_name);
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}

		var s = mb.findFirstString("tags");
		if (nn(s)) {
			for (var m : parseListOrCdl(s, "Messages/tags on class {0}", c).elements(JsonMap.class)) {
				var name = m.getString(SWAGGER_name);
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in resource bundle on class {0}", c);
				if (tagMap.containsKey(name))
					tagMap.get(name).putAll(m);
				else
					tagMap.put(name, m);
			}
		}

		// Load our existing bean definitions into our session.
		var definitions = omSwagger.getMap(SWAGGER_definitions, true);
		for (var defId : definitions.keySet())
			js.addBeanDef(defId, new JsonMap(definitions.getMap(defId)));

		// Iterate through all the @RestOp methods.
		for (var sm : context.getRestOperations().getOpContexts()) {

			var bs = sm.getBeanContext().getSession();

			var m = sm.getJavaMethod();
			var mi = MethodInfo.of(m);
			var al = rstream(ap.find(mi)).filter(REST_OP_GROUP).toList();
			var mn = m.getName();

			// Get the operation from the existing swagger so far.
			var op = getOperation(omSwagger, sm.getPathPattern(), sm.getHttpMethod().toLowerCase());

			// Add @RestOp(swagger)
			var _ms = Value.<OpSwagger>empty();
			al.forEach(ai -> ai.getValue(OpSwagger.class, "swagger").filter(OpSwaggerAnnotation::notEmpty).ifPresent(_ms::set));
			var ms = _ms.orElseGet(() -> OpSwaggerAnnotation.create().build());

			op.append(parseMap(ms.value(), "@OpSwagger(value) on class {0} method {1}", c, m));
			op.appendIf(ne, SWAGGER_operationId,
				firstNonEmpty(
					resolve(ms.operationId()),
					op.getString(SWAGGER_operationId),
					mn
				)
			);

			var _summary = Value.<String>empty();
			al.forEach(ai -> ai.getValue(String.class, "summary").filter(NOT_EMPTY).ifPresent(_summary::set));
			op.appendIf(ne, SWAGGER_summary,
				firstNonEmpty(
					resolve(ms.summary()),
					resolve(mb.findFirstString(mn + ".summary")),
					op.getString(SWAGGER_summary),
					resolve(_summary.orElse(null))
				)
			);

			var _description = Value.<String[]>empty();
			al.forEach(ai -> ai.getValue(String[].class, "description").filter(x -> x.length > 0).ifPresent(_description::set));
			op.appendIf(ne, SWAGGER_description,
				firstNonEmpty(
					resolve(ms.description()),
					resolve(mb.findFirstString(mn + ".description")),
					op.getString(SWAGGER_description),
					resolve(_description.orElse(new String[0]))
				)
			);
			op.appendIf(ne, SWAGGER_deprecated,
				firstNonEmpty(
					resolve(ms.deprecated()),
					(nn(m.getAnnotation(Deprecated.class)) || nn(ClassInfo.of(m.getDeclaringClass()).getAnnotations(Deprecated.class).findFirst().map(AnnotationInfo::inner).orElse(null))) ? SWAGGER_true : null
				)
			);
			op.appendIf(nec, SWAGGER_tags,
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".tags"), "Messages/tags on class {0} method {1}", c, m),
					parseListOrCdl(ms.tags(), "@OpSwagger(tags) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, SWAGGER_schemes,
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".schemes"), "Messages/schemes on class {0} method {1}", c, m),
					parseListOrCdl(ms.schemes(), "@OpSwagger(schemes) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, SWAGGER_consumes,
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".consumes"), "Messages/consumes on class {0} method {1}", c, m),
					parseListOrCdl(ms.consumes(), "@OpSwagger(consumes) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, SWAGGER_produces,
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".produces"), "Messages/produces on class {0} method {1}", c, m),
					parseListOrCdl(ms.produces(), "@OpSwagger(produces) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, SWAGGER_parameters,
				merge(
					parseList(mb.findFirstString(mn + ".parameters"), "Messages/parameters on class {0} method {1}", c, m),
					parseList(ms.parameters(), "@OpSwagger(parameters) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nem, SWAGGER_responses,
				merge(
					parseMap(mb.findFirstString(mn + ".responses"), "Messages/responses on class {0} method {1}", c, m),
					parseMap(ms.responses(), "@OpSwagger(responses) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nem, SWAGGER_externalDocs,
				merge(
					op.getMap(SWAGGER_externalDocs),
					parseMap(mb.findFirstString(mn + ".externalDocs"), "Messages/externalDocs on class {0} method {1}", c, m),
					toMap(ms.externalDocs(), "@OpSwagger(externalDocs) on class {0} method {1}", c, m)
				)
			);

			if (op.containsKey(SWAGGER_tags))
				for (var tag : op.getList(SWAGGER_tags).elements(String.class))
					if (! tagMap.containsKey(tag))
						tagMap.put(tag, JsonMap.of(SWAGGER_name, tag));

			var paramMap = new JsonMap();
			if (op.containsKey(SWAGGER_parameters))
				for (var param : op.getList(SWAGGER_parameters).elements(JsonMap.class))
					paramMap.put(param.getString(SWAGGER_in) + '.' + (SWAGGER_body.equals(param.getString(SWAGGER_in)) ? SWAGGER_body : param.getString(SWAGGER_name)), param);

			// Finally, look for parameters defined on method.
			for (var mpi : mi.getParameters()) {

				var pt = mpi.getParameterType();
				var type = pt.innerType();

				if (ap.has(Content.class, mpi)) {
					var param = paramMap.getMap(BODY + ".body", true).append(SWAGGER_in, BODY);
					var schema = getSchema(param.getMap(SWAGGER_schema), type, bs);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(schema, x.inner()));
					rstream(ap.find(Content.class, mpi)).forEach(x -> merge(schema, x.inner().schema()));
					pushupSchemaFields(BODY, param, schema);
					param.appendIf(nem, SWAGGER_schema, schema);
					param.putIfAbsent(SWAGGER_required, SWAGGER_true);
					addBodyExamples(sm, param, false, type, locale);

				} else if (ap.has(Query.class, mpi)) {
					var name = QueryAnnotation.findName(mpi).orElse(null);
					var param = paramMap.getMap(QUERY + "." + name, true).append(SWAGGER_name, name).append(SWAGGER_in, QUERY);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Query.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(QUERY, param, getSchema(param.getMap(SWAGGER_schema), type, bs));
					addParamExample(sm, param, QUERY, type);

				} else if (ap.has(FormData.class, mpi)) {
					var name = FormDataAnnotation.findName(mpi).orElse(null);
					var param = paramMap.getMap(FORM_DATA + "." + name, true).append(SWAGGER_name, name).append(SWAGGER_in, FORM_DATA);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(FormData.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(FORM_DATA, param, getSchema(param.getMap(SWAGGER_schema), type, bs));
					addParamExample(sm, param, FORM_DATA, type);

				} else if (ap.has(Header.class, mpi)) {
					var name = HeaderAnnotation.findName(mpi).orElse(null);
					var param = paramMap.getMap(HEADER + "." + name, true).append(SWAGGER_name, name).append(SWAGGER_in, HEADER);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Header.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(HEADER, param, getSchema(param.getMap(SWAGGER_schema), type, bs));
					addParamExample(sm, param, HEADER, type);

				} else if (ap.has(Path.class, mpi)) {
					var name = PathAnnotation.findName(mpi).orElse(null);
					var param = paramMap.getMap(PATH + "." + name, true).append(SWAGGER_name, name).append(SWAGGER_in, PATH);
					rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(param, x.inner()));
					rstream(ap.find(Path.class, mpi)).forEach(x -> merge(param, x.inner().schema()));
					pushupSchemaFields(PATH, param, getSchema(param.getMap(SWAGGER_schema), type, bs));
					addParamExample(sm, param, PATH, type);
					param.putIfAbsent(SWAGGER_required, SWAGGER_true);
				}
			}

			if (! paramMap.isEmpty())
				op.put(SWAGGER_parameters, paramMap.values());

			var responses = op.getMap(SWAGGER_responses, true);

			for (var eci : mi.getExceptionTypes()) {
				if (eci.hasAnnotation(Response.class)) {
					var la = rstream(ap.find(Response.class, eci)).map(AnnotationInfo::inner).toList();
					var la2 = rstream(ap.find(StatusCode.class, eci)).map(x -> x.inner()).toList();
					var codes = getCodes(la2, 500);
					for (var a : la) {
						for (var code : codes) {
							var om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							var schema = getSchema(om.getMap(SWAGGER_schema), m.getGenericReturnType(), bs);
							rstream(ap.find(Schema.class, eci)).forEach(x -> merge(schema, x.inner()));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, SWAGGER_schema, schema);
					}
				}
				var methods = eci.getAllMethods();
				for (var i = methods.size() - 1; i >= 0; i--) {
					var ecmi = methods.get(i);
					var a = ecmi.getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
						if (a == null)
							a = ecmi.getReturnType().unwrap(Value.class, Optional.class).getAnnotations(Header.class).findFirst().map(AnnotationInfo::inner).orElse(null);
						if (nn(a) && ! isMulti(a)) {
							var ha = a.name();
							for (var code : codes) {
								var header = responses.getMap(String.valueOf(code), true).getMap(SWAGGER_headers, true).getMap(ha, true);
								rstream(ap.find(Schema.class, ecmi)).forEach(x -> merge(header, x.inner()));
								rstream(ap.find(Schema.class, ecmi.getReturnType().unwrap(Value.class, Optional.class))).forEach(x -> merge(header, x.inner()));
								pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header.getMap(SWAGGER_schema), ecmi.getReturnType().unwrap(Value.class, Optional.class).innerType(), bs));
							}
						}
					}
				}
			}

			if (mi.hasAnnotation(Response.class) || mi.getReturnType().unwrap(Value.class, Optional.class).hasAnnotation(Response.class)) {
				var la = rstream(ap.find(Response.class, mi)).map(x -> x.inner()).toList();
				var la2 = rstream(ap.find(StatusCode.class, mi)).map(x -> x.inner()).toList();
				var codes = getCodes(la2, 200);
				for (var a : la) {
					for (var code : codes) {
						var om = responses.getMap(String.valueOf(code), true);
						merge(om, a);
						var schema = getSchema(om.getMap(SWAGGER_schema), m.getGenericReturnType(), bs);
						rstream(ap.find(Schema.class, mi)).forEach(x -> merge(schema, x.inner()));
						pushupSchemaFields(RESPONSE, om, schema);
						om.appendIf(nem, SWAGGER_schema, schema);
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
									var header = responses.getMap(String.valueOf(code), true).getMap(SWAGGER_headers, true).getMap(ha, true);
									rstream(ap.find(Schema.class, ecmi)).forEach(x -> merge(header, x.inner()));
									rstream(ap.find(Schema.class, ecmi.getReturnType().unwrap(Value.class, Optional.class))).forEach(x -> merge(header, x.inner()));
									merge(header, a.schema());
									pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header, ecmi.getReturnType().innerType(), bs));
								}
							}
						}
					}
				}
			} else if (m.getGenericReturnType() != void.class) {
				var om = responses.getMap(CONST_200, true);
				var pt2 = ClassInfo.of(m.getGenericReturnType());
				var schema = getSchema(om.getMap(SWAGGER_schema), m.getGenericReturnType(), bs);
				rstream(ap.find(Schema.class, pt2)).forEach(x -> merge(schema, x.inner()));
				pushupSchemaFields(RESPONSE, om, schema);
				om.appendIf(nem, SWAGGER_schema, schema);
				addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
			}

			// Finally, look for Value @Header parameters defined on method.
			for (var mpi : mi.getParameters()) {

				var pt = mpi.getParameterType();

				if (pt.is(Value.class) && (ap.has(Header.class, mpi))) {
					var la = rstream(ap.find(Header.class, mpi)).map(AnnotationInfo::inner).toList();
					var la2 = rstream(ap.find(StatusCode.class, mpi)).map(AnnotationInfo::inner).toList();
					var codes = getCodes(la2, 200);
					var name = HeaderAnnotation.findName(mpi).orElse(null);
					var type = Value.unwrap(mpi.getParameterType().innerType());
					for (var a : la) {
						if (! isMulti(a)) {
							for (var code : codes) {
								var header = responses.getMap(String.valueOf(code), true).getMap(SWAGGER_headers, true).getMap(name, true);
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
					var type = Value.unwrap(mpi.getParameterType().innerType());
					for (var a : la) {
						for (var code : codes) {
							var om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							var schema = getSchema(om.getMap(SWAGGER_schema), type, bs);
							rstream(ap.find(Schema.class, mpi)).forEach(x -> merge(schema, x.inner()));
							la.forEach(x -> merge(schema, x.schema()));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, SWAGGER_schema, schema);
						}
					}
				}
			}

			// Add default response descriptions.
			for (var e : responses.entrySet()) {
				var key = e.getKey();
				var val = responses.getMap(key);
				if (isDecimal(key))
					val.appendIfAbsentIf(ne, SWAGGER_description, RestUtils.getHttpResponseText(Integer.parseInt(key)));
			}

			if (responses.isEmpty())
				op.remove(SWAGGER_responses);
			else
				op.put(SWAGGER_responses, new TreeMap<>(responses));

			if (! op.containsKey(SWAGGER_consumes)) {
				var mConsumes = sm.getSupportedContentTypes();
				if (! mConsumes.equals(consumes))
					op.put(SWAGGER_consumes, mConsumes);
			}

			if (! op.containsKey(SWAGGER_produces)) {
				var mProduces = sm.getSupportedAcceptTypes();
				if (! mProduces.equals(produces))
					op.put(SWAGGER_produces, mProduces);
			}
		}

		if (nn(js.getBeanDefs()))
			for (var e : js.getBeanDefs().entrySet())
				definitions.put(e.getKey(), fixSwaggerExtensions(e.getValue()));

		if (definitions.isEmpty())
			omSwagger.remove(SWAGGER_definitions);

		if (! tagMap.isEmpty())
			omSwagger.put(SWAGGER_tags, tagMap.values());

		if (consumes.isEmpty())
			omSwagger.remove(SWAGGER_consumes);
		if (produces.isEmpty())
			omSwagger.remove(SWAGGER_produces);

		try {
			var swaggerJson = Json5Serializer.DEFAULT_READABLE.toString(omSwagger);
			return jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new ServletException("Error detected in swagger.", e);
		}
		// @formatter:on
	}

	@SuppressWarnings("java:S3776")
	private void addBodyExamples(RestOpContext sm, JsonMap piri, boolean response, Type type, Locale locale) throws Exception {

		var sex = piri.getString(SWAGGER_example);

		if (sex == null) {
			var schema = resolveRef(piri.getMap(SWAGGER_schema));
			if (nn(schema))
				sex = schema.getString(SWAGGER_example, schema.getString(SWAGGER_example));
		}

		if (isEmpty(sex))
			return;

		Object example = null;
		if (isProbablyJson(sex)) {
			example = jp.parse(sex, type);
		} else {
			var cm = js.getClassMeta(type);
			if (cm.hasStringMutater()) {
				example = cm.getStringMutater().mutate(sex);
			}
		}

		var examplesKey = SWAGGER_examples;  // Parameters don't have an examples attribute.

		var examples = piri.getMap(examplesKey);
		if (examples == null)
			examples = new JsonMap();

		var mediaTypes = response ? sm.getSerializers().getSupportedMediaTypes() : sm.getParsers().getSupportedMediaTypes();

		for (var mt : mediaTypes) {
			if (mt != MediaType.HTML) {
				var s2 = sm.getSerializers().getSerializer(mt);
				if (nn(s2)) {
					try {
						// @formatter:off
						var eVal = s2
							.createSession()
							.locale(locale)
							.mediaType(mt)
							.apply(WriterSerializerSession.Builder.class, x -> x.useWhitespace(true))
							.build()
							.serializeToString(example);
						// @formatter:on
						examples.put(s2.getPrimaryMediaType().toString(), eVal);
					} catch (Exception e) {
						System.err.println("Could not serialize to media type [" + mt + "]: " + lm(e));  // NOT DEBUG
					}
				}
			}
		}

		if (! examples.isEmpty())
			piri.put(examplesKey, examples);
	}

	private static void addParamExample(RestOpContext sm, JsonMap piri, RestPartType in, Type type) throws Exception {

		var s = piri.getString(SWAGGER_example);

		if (isEmpty(s))
			return;

		var examples = piri.getMap(SWAGGER_examples);
		if (examples == null)
			examples = new JsonMap();

		var paramName = piri.getString(SWAGGER_name);

		if (in == QUERY)
			s = "?" + urlEncodeLax(paramName) + "=" + urlEncodeLax(s);
		else if (in == FORM_DATA)
			s = paramName + "=" + s;
		else if (in == HEADER)
			s = paramName + ": " + s;
		else if (in == PATH)
			s = sm.getPathPattern().replace("{" + paramName + "}", urlEncodeLax(s));

		examples.put(SWAGGER_example, s);

		if (! examples.isEmpty())
			piri.put(SWAGGER_examples, examples);
	}

	@SafeVarargs
	private static final <T> T firstNonEmpty(T...t) {
		for (var oo : t)
			if (ne(oo))
				return oo;
		return null;
	}

	/**
	 * Replaces non-standard JSON-Schema attributes with standard Swagger attributes.
	 */
	private static JsonMap fixSwaggerExtensions(JsonMap om) {
		Predicate<Object> nn = Utils::nn;
		// @formatter:off
		om
			.appendIf(nn, SWAGGER_discriminator, om.remove(JSONSCHEMA_x_discriminator))
			.appendIf(nn, SWAGGER_readOnly, om.remove(JSONSCHEMA_x_readOnly))
			.appendIf(nn, SWAGGER_xml, om.remove(JSONSCHEMA_x_xml))
			.appendIf(nn, SWAGGER_externalDocs, om.remove(JSONSCHEMA_x_externalDocs))
			.appendIf(nn, SWAGGER_example, om.remove(JSONSCHEMA_x_example));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private static JsonMap getOperation(JsonMap om, String path, String httpMethod) {
		om = (JsonMap) om.computeIfAbsent(SWAGGER_paths, k -> new JsonMap());
		om = (JsonMap) om.computeIfAbsent(path, k -> new JsonMap());
		om.computeIfAbsent(httpMethod, k -> new JsonMap());
		return (JsonMap) om.get(httpMethod);
	}

	private JsonMap getSchema(JsonMap schema, Type type, BeanSession bs) throws Exception {

		if (type == Swagger.class)
			return JsonMap.create();

		schema = newMap(schema);

		var cm = bs.getClassMeta(type);

		if (schema.is(SWAGGER_ignore, false))
			return null;

		if (schema.containsKey(SWAGGER_type) || schema.containsKey(SWAGGER_$ref))
			return schema;

		var om = fixSwaggerExtensions(schema.append(js.getSchema(cm)));

		return om;
	}

	private static boolean isMulti(Header h) {
		return "*".equals(h.name()) || "*".equals(h.value());
	}

	private static JsonList merge(JsonList...lists) {
		var l = lists[0];
		for (var i = 1; i < lists.length; i++) {
			if (nn(lists[i])) {
				if (l == null)
					l = new JsonList();
				l.addAll(lists[i]);
			}
		}
		return l;
	}

	private static JsonMap merge(JsonMap...maps) {
		var m = maps[0];
		for (var i = 1; i < maps.length; i++) {
			if (nn(maps[i])) {
				if (m == null)
					m = new JsonMap();
				m.putAll(maps[i]);
			}
		}
		return m;
	}

	private JsonMap merge(JsonMap om, ExternalDocs a) {
		if (ExternalDocsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Utils::ne;
		// @formatter:off
		return om
			.appendIf(ne, SWAGGER_description, resolve(a.description()))
			.appendIf(ne, SWAGGER_url, a.url())
		;
		// @formatter:on
	}

	private JsonMap merge(JsonMap om, Header[] a) {
		if (a.length == 0)
			return om;
		om = newMap(om);
		for (var aa : a) {
			var name = StringUtils.firstNonEmpty(aa.name(), aa.value());
			if (isEmpty(name))
				throw illegalArg("@Header used without name or value.");
			merge(om.getMap(name, true), aa.schema());
		}
		return om;
	}

	private JsonMap merge(JsonMap om, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Utils::ne;
		Predicate<Collection<?>> nec = Utils::ne;
		Predicate<Map<?,?>> nem = Utils::ne;
		Predicate<Boolean> nf = Utils::isTrue;
		Predicate<Long> nm1 = Utils::nm1;
		// @formatter:off
		return om
			.appendFirst(ne, SWAGGER_collectionFormat, a.collectionFormat(), a.cf())
			.appendIf(ne, SWAGGER_default, joinnl(a.default_(), a.df()))
			.appendFirst(nec, SWAGGER_enum, toSet(a.enum_()), toSet(a.e()))
			.appendFirst(ne, SWAGGER_format, a.format(), a.f())
			.appendIf(nf, SWAGGER_exclusiveMaximum, a.exclusiveMaximum() || a.emax())
			.appendIf(nf, SWAGGER_exclusiveMinimum, a.exclusiveMinimum() || a.emin())
			.appendIf(nem, SWAGGER_items, merge(om.getMap(SWAGGER_items), a.items()))
			.appendFirst(ne, SWAGGER_maximum, a.maximum(), a.max())
			.appendFirst(nm1, SWAGGER_maxItems, a.maxItems(), a.maxi())
			.appendFirst(nm1, SWAGGER_maxLength, a.maxLength(), a.maxl())
			.appendFirst(ne, SWAGGER_minimum, a.minimum(), a.min())
			.appendFirst(nm1, SWAGGER_minItems, a.minItems(), a.mini())
			.appendFirst(nm1, SWAGGER_minLength, a.minLength(), a.minl())
			.appendFirst(ne, SWAGGER_multipleOf, a.multipleOf(), a.mo())
			.appendFirst(ne, SWAGGER_pattern, a.pattern(), a.p())
			.appendIf(nf, SWAGGER_uniqueItems, a.uniqueItems() || a.ui())
			.appendFirst(ne, SWAGGER_type, a.type(), a.t())
			.appendIf(ne, SWAGGER_$ref, a.$ref())
		;
		// @formatter:on
	}

	private JsonMap merge(JsonMap om, Response a) throws ParseException {
		if (ResponseAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<Map<?,?>> nem = Utils::ne;
		if (! SchemaAnnotation.empty(a.schema()))
			merge(om, a.schema());
		// @formatter:off
		return om
			.appendIf(nem, SWAGGER_examples, parseMap(a.examples()))
			.appendIf(nem, SWAGGER_headers, merge(om.getMap(SWAGGER_headers), a.headers()))
			.appendIf(nem, SWAGGER_schema, merge(om.getMap(SWAGGER_schema), a.schema()))
		;
		// @formatter:on
	}

	@SuppressWarnings("deprecation")
	private JsonMap merge(JsonMap om, Schema a) {
		try {
			if (SchemaAnnotation.empty(a))
				return om;
			om = newMap(om);
			Predicate<String> ne = Utils::ne;
			Predicate<Collection<?>> nec = Utils::ne;
			Predicate<Map<?,?>> nem = Utils::ne;
			Predicate<Boolean> nf = Utils::isTrue;
			Predicate<Long> nm1 = Utils::nm1;
			// @formatter:off
			return om
				.appendIf(nem, SWAGGER_additionalProperties, toJsonMap(a.additionalProperties()))
				.appendIf(ne, SWAGGER_allOf, joinnl(a.allOf()))
				.appendFirst(ne, SWAGGER_collectionFormat, a.collectionFormat(), a.cf())
				.appendIf(ne, SWAGGER_default, joinnl(a.default_(), a.df()))
				.appendIf(ne, SWAGGER_discriminator, a.discriminator())
				.appendIf(ne, SWAGGER_description, resolve(a.description(), a.d()))
				.appendFirst(nec, SWAGGER_enum, toSet(a.enum_()), toSet(a.e()))
				.appendIf(nf, SWAGGER_exclusiveMaximum, a.exclusiveMaximum() || a.emax())
				.appendIf(nf, SWAGGER_exclusiveMinimum, a.exclusiveMinimum() || a.emin())
				.appendIf(nem, SWAGGER_externalDocs, merge(om.getMap(SWAGGER_externalDocs), a.externalDocs()))
				.appendFirst(ne, SWAGGER_format, a.format(), a.f())
				.appendIf(ne, SWAGGER_ignore, a.ignore() ? SWAGGER_true : null)
				.appendIf(nem, SWAGGER_items, merge(om.getMap(SWAGGER_items), a.items()))
				.appendFirst(ne, SWAGGER_maximum, a.maximum(), a.max())
				.appendFirst(nm1, SWAGGER_maxItems, a.maxItems(), a.maxi())
				.appendFirst(nm1, SWAGGER_maxLength, a.maxLength(), a.maxl())
				.appendFirst(nm1, SWAGGER_maxProperties, a.maxProperties(), a.maxp())
				.appendFirst(ne, SWAGGER_minimum, a.minimum(), a.min())
				.appendFirst(nm1, SWAGGER_minItems, a.minItems(), a.mini())
				.appendFirst(nm1, SWAGGER_minLength, a.minLength(), a.minl())
				.appendFirst(nm1, SWAGGER_minProperties, a.minProperties(), a.minp())
				.appendFirst(ne, SWAGGER_multipleOf, a.multipleOf(), a.mo())
				.appendFirst(ne, SWAGGER_pattern, a.pattern(), a.p())
				.appendIf(nem, SWAGGER_properties, toJsonMap(a.properties()))
				.appendIf(nf, SWAGGER_readOnly, a.readOnly() || a.ro())
				.appendIf(nf, SWAGGER_required, a.required() || a.r())
				.appendIf(ne, SWAGGER_title, a.title())
				.appendFirst(ne, SWAGGER_type, a.type(), a.t())
				.appendIf(nf, SWAGGER_uniqueItems, a.uniqueItems() || a.ui())
				.appendIf(ne, SWAGGER_xml, joinnl(a.xml()))
				.appendIf(ne, SWAGGER_$ref, a.$ref())
			;
			// @formatter:on
		} catch (ParseException e) {
			throw illegalArg(e);
		}
	}

	private JsonMap merge(JsonMap om, SubItems a) throws ParseException {
		if (SubItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = Utils::ne;
		Predicate<Collection<?>> nec = Utils::ne;
		Predicate<Map<?,?>> nem = Utils::ne;
		Predicate<Boolean> nf = Utils::isTrue;
		Predicate<Long> nm1 = Utils::nm1;
		// @formatter:off
		return om
			.appendFirst(ne, SWAGGER_collectionFormat, a.collectionFormat(), a.cf())
			.appendIf(ne, SWAGGER_default, joinnl(a.default_(), a.df()))
			.appendFirst(nec, SWAGGER_enum, toSet(a.enum_()), toSet(a.e()))
			.appendIf(nf, SWAGGER_exclusiveMaximum, a.exclusiveMaximum() || a.emax())
			.appendIf(nf, SWAGGER_exclusiveMinimum, a.exclusiveMinimum() || a.emin())
			.appendFirst(ne, SWAGGER_format, a.format(), a.f())
			.appendIf(nem, SWAGGER_items, toJsonMap(a.items()))
			.appendFirst(ne, SWAGGER_maximum, a.maximum(), a.max())
			.appendFirst(nm1, SWAGGER_maxItems, a.maxItems(), a.maxi())
			.appendFirst(nm1, SWAGGER_maxLength, a.maxLength(), a.maxl())
			.appendFirst(ne, SWAGGER_minimum, a.minimum(), a.min())
			.appendFirst(nm1, SWAGGER_minItems, a.minItems(), a.mini())
			.appendFirst(nm1, SWAGGER_minLength, a.minLength(), a.minl())
			.appendFirst(ne, SWAGGER_multipleOf, a.multipleOf(), a.mo())
			.appendFirst(ne, SWAGGER_pattern, a.pattern(), a.p())
			.appendFirst(ne, SWAGGER_type, a.type(), a.t())
			.appendIf(nf, SWAGGER_uniqueItems, a.uniqueItems() || a.ui())
			.appendIf(ne, SWAGGER_$ref, a.$ref())
		;
		// @formatter:on
	}

	private JsonList parseList(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			var s = (o instanceof String[] stringArray ? joinnl(stringArray) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			if (! isProbablyJsonArray(s, true))
				s = "[" + s + "]";
			return JsonList.ofJson(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in " + location + ".", locationArgs);
		}
	}

	private JsonList parseListOrCdl(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			var s = (o instanceof String[] stringArray ? joinnl(stringArray) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			return JsonList.ofJsonOrCdl(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in " + location + ".", locationArgs);
		}
	}

	private JsonMap parseMap(Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[] stringArray)
			o = joinnl(stringArray);
		if (o instanceof String o2) {
			if (o2.isEmpty())
				return null;
			o2 = resolve(o2);
			if (CONST_IGNORE.equalsIgnoreCase(o2))
				return JsonMap.of(SWAGGER_ignore, true);
			if (! isProbablyJsonObject(o2, true))
				o2 = "{" + o2 + "}";
			return JsonMap.ofJson(o2);
		}
		if (o instanceof JsonMap o2)
			return o2;
		throw new SwaggerException(null, "Unexpected data type ''{0}''.  Expected JsonMap or String.", cn(o));
	}

	private JsonMap parseMap(String o, String location, Object...args) throws ParseException {
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private JsonMap parseMap(String[] o, String location, Object...args) throws ParseException {
		if (o.length == 0)
			return JsonMap.EMPTY_MAP;
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private static JsonMap pushupSchemaFields(RestPartType type, JsonMap param, JsonMap schema) {
		// @formatter:off
		Predicate<Object> ne = Utils::ne;
		if (nn(schema) && ! schema.isEmpty()) {
			if (type == BODY || type == RESPONSE) {
				param
					.appendIf(ne, SWAGGER_description, schema.remove(SWAGGER_description));
			} else {
				param
					.appendIfAbsentIf(ne, SWAGGER_collectionFormat, schema.remove(SWAGGER_collectionFormat))
					.appendIfAbsentIf(ne, SWAGGER_default, schema.remove(SWAGGER_default))
					.appendIfAbsentIf(ne, SWAGGER_description, schema.remove(SWAGGER_description))
					.appendIfAbsentIf(ne, SWAGGER_enum, schema.remove(SWAGGER_enum))
					.appendIfAbsentIf(ne, SWAGGER_example, schema.remove(SWAGGER_example))
					.appendIfAbsentIf(ne, SWAGGER_exclusiveMaximum, schema.remove(SWAGGER_exclusiveMaximum))
					.appendIfAbsentIf(ne, SWAGGER_exclusiveMinimum, schema.remove(SWAGGER_exclusiveMinimum))
					.appendIfAbsentIf(ne, SWAGGER_format, schema.remove(SWAGGER_format))
					.appendIfAbsentIf(ne, SWAGGER_items, schema.remove(SWAGGER_items))
					.appendIfAbsentIf(ne, SWAGGER_maximum, schema.remove(SWAGGER_maximum))
					.appendIfAbsentIf(ne, SWAGGER_maxItems, schema.remove(SWAGGER_maxItems))
					.appendIfAbsentIf(ne, SWAGGER_maxLength, schema.remove(SWAGGER_maxLength))
					.appendIfAbsentIf(ne, SWAGGER_minimum, schema.remove(SWAGGER_minimum))
					.appendIfAbsentIf(ne, SWAGGER_minItems, schema.remove(SWAGGER_minItems))
					.appendIfAbsentIf(ne, SWAGGER_minLength, schema.remove(SWAGGER_minLength))
					.appendIfAbsentIf(ne, SWAGGER_multipleOf, schema.remove(SWAGGER_multipleOf))
					.appendIfAbsentIf(ne, SWAGGER_pattern, schema.remove(SWAGGER_pattern))
					.appendIfAbsentIf(ne, SWAGGER_required, schema.remove(SWAGGER_required))
					.appendIfAbsentIf(ne, SWAGGER_type, schema.remove(SWAGGER_type))
					.appendIfAbsentIf(ne, SWAGGER_uniqueItems, schema.remove(SWAGGER_uniqueItems));

				if (SWAGGER_object.equals(param.getString(SWAGGER_type)) && ! schema.isEmpty())
					param.put(SWAGGER_schema, schema);
			}
		}

		return param;
		// @formatter:on
	}

	private JsonList resolve(JsonList om) throws ParseException {
		var ol2 = new JsonList();
		for (var val : om) {
			if (val instanceof JsonMap val2) {
				val = resolve(val2);
			} else if (val instanceof JsonList val2) {
				val = resolve(val2);
			} else if (val instanceof String val2) {
				val = resolve(val2);
			}
			ol2.add(val);
		}
		return ol2;
	}

	private JsonMap resolve(JsonMap om) throws ParseException {
		JsonMap om2 = null;
		if (om.containsKey(CONST_value)) {
			om = om.modifiable();
			om2 = parseMap(om.remove(CONST_value));
		} else {
			om2 = new JsonMap();
		}
		for (var e : om.entrySet()) {
			var val = e.getValue();
			if (val instanceof JsonMap val2) {
				val = resolve(val2);
			} else if (val instanceof JsonList val2) {
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

	private JsonMap resolveRef(JsonMap m) {
		if (m == null)
			return null;
		if (m.containsKey(SWAGGER_$ref) && nn(js.getBeanDefs())) {
			var ref = m.getString(SWAGGER_$ref);
			if (ref.startsWith("#/definitions/"))
				return js.getBeanDefs().get(ref.substring(14));
		}
		return m;
	}

	private JsonMap toJsonMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		var s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isProbablyJsonObject(s, true))
			s = "{" + s + "}";
		s = resolve(s);
		return JsonMap.ofJson(s);
	}

	private JsonList toList(Tag[] aa, String location, Object...locationArgs) {
		if (aa.length == 0)
			return null;
		var ol = new JsonList();
		for (var a : aa)
			ol.add(toMap(a, location, locationArgs));
		return nullIfEmpty(ol);
	}

	private JsonMap toMap(Contact a, String location, Object...locationArgs) {
		if (ContactAnnotation.empty(a))
			return null;
		Predicate<String> ne = Utils::ne;
		// @formatter:off
		var om = JsonMap.create()
			.appendIf(ne, SWAGGER_name, resolve(a.name()))
			.appendIf(ne, SWAGGER_url, resolve(a.url()))
			.appendIf(ne, SWAGGER_email, resolve(a.email()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private JsonMap toMap(ExternalDocs a, String location, Object...locationArgs) {
		if (ExternalDocsAnnotation.empty(a))
			return null;
		Predicate<String> ne = Utils::ne;
		// @formatter:off
		var om = JsonMap.create()
			.appendIf(ne, SWAGGER_description, resolve(joinnl(a.description())))
			.appendIf(ne, SWAGGER_url, resolve(a.url()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private JsonMap toMap(License a, String location, Object...locationArgs) {
		if (LicenseAnnotation.empty(a))
			return null;
		Predicate<String> ne = Utils::ne;
		// @formatter:off
		var om = JsonMap.create()
			.appendIf(ne, SWAGGER_name, resolve(a.name()))
			.appendIf(ne, SWAGGER_url, resolve(a.url()));
		// @formatter:on
		return nullIfEmpty(om);
	}

	private JsonMap toMap(Tag a, String location, Object...locationArgs) {
		var om = JsonMap.create();
		Predicate<String> ne = Utils::ne;
		Predicate<Map<?,?>> nem = Utils::ne;
		// @formatter:off
		om
			.appendIf(ne, SWAGGER_name, resolve(a.name()))
			.appendIf(ne, SWAGGER_description, resolve(joinnl(a.description())))
			.appendIf(nem, SWAGGER_externalDocs, merge(om.getMap(SWAGGER_externalDocs), toMap(a.externalDocs(), location, locationArgs)));
		// @formatter:on
		return nullIfEmpty(om);
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