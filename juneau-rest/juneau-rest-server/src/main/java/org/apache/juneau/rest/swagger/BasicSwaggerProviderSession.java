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
package org.apache.juneau.rest.swagger;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.rest.httppart.RestPartType.*;
import static org.apache.juneau.rest.annotation.RestOpAnnotation.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * A single session of generating a Swagger document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
public class BasicSwaggerProviderSession {

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
	public Swagger getSwagger() throws Exception {

		InputStream is = ff.getStream(rci.getSimpleName() + ".json", locale).orElse(null);

		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;

		// Load swagger JSON from classpath.
		JsonMap omSwagger = Json5.DEFAULT.read(is, JsonMap.class);
		if (omSwagger == null)
			omSwagger = new JsonMap();

		// Combine it with @Rest(swagger)
		for (Rest rr : rci.getAnnotations(context, Rest.class)) {

			JsonMap sInfo = omSwagger.getMap("info", true);

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
				);

			org.apache.juneau.rest.annotation.Swagger r = rr.swagger();

			omSwagger.append(parseMap(r.value(), "@Swagger(value) on class {0}", c));

			if (! SwaggerAnnotation.empty(r)) {
				JsonMap info = omSwagger.getMap("info", true);

				info
					.appendIf(ne, "title", resolve(r.title()))
					.appendIf(ne, "description", resolve(r.description()))
					.appendIf(ne, "version", resolve(r.version()))
					.appendIf(ne, "termsOfService", resolve(r.termsOfService()))
					.appendIf(nem, "contact",
						merge(
							info.getMap("contact"),
							toMap(r.contact(), "@Swagger(contact) on class {0}", c)
						)
					)
					.appendIf(nem, "license",
						merge(
							info.getMap("license"),
							toMap(r.license(), "@Swagger(license) on class {0}", c)
						)
					);
			}

			omSwagger
				.appendIf(nem, "externalDocs",
					merge(
						omSwagger.getMap("externalDocs"),
						toMap(r.externalDocs(), "@Swagger(externalDocs) on class {0}", c)
					)
				)
				.appendIf(nec, "tags",
					merge(
						omSwagger.getList("tags"),
						toList(r.tags(), "@Swagger(tags) on class {0}", c)
					)
				);
		}

		omSwagger.appendIf(nem, "externalDocs", parseMap(mb.findFirstString("externalDocs"), "Messages/externalDocs on class {0}", c));

		JsonMap info = omSwagger.getMap("info", true);

		info
			.appendIf(ne, "title", resolve(mb.findFirstString("title")))
			.appendIf(ne, "description", resolve(mb.findFirstString("description")))
			.appendIf(ne, "version", resolve(mb.findFirstString("version")))
			.appendIf(ne, "termsOfService", resolve(mb.findFirstString("termsOfService")))
			.appendIf(nem, "contact", parseMap(mb.findFirstString("contact"), "Messages/contact on class {0}", c))
			.appendIf(nem, "license", parseMap(mb.findFirstString("license"), "Messages/license on class {0}", c));

		if (info.isEmpty())
			omSwagger.remove("info");

		JsonList
			produces = omSwagger.getList("produces", true),
			consumes = omSwagger.getList("consumes", true);
		if (consumes.isEmpty())
			consumes.addAll(context.getConsumes());
		if (produces.isEmpty())
			produces.addAll(context.getProduces());

		Map<String,JsonMap> tagMap = map();
		if (omSwagger.containsKey("tags")) {
			for (JsonMap om : omSwagger.getList("tags").elements(JsonMap.class)) {
				String name = om.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}

		String s = mb.findFirstString("tags");
		if (s != null) {
			for (JsonMap m : parseListOrCdl(s, "Messages/tags on class {0}", c).elements(JsonMap.class)) {
				String name = m.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in resource bundle on class {0}", c) ;
				if (tagMap.containsKey(name))
					tagMap.get(name).putAll(m);
				else
					tagMap.put(name, m);
			}
		}

		// Load our existing bean definitions into our session.
		JsonMap definitions = omSwagger.getMap("definitions", true);
		for (String defId : definitions.keySet())
			js.addBeanDef(defId, new JsonMap(definitions.getMap(defId)));

		// Iterate through all the @RestOp methods.
		for (RestOpContext sm : context.getRestOperations().getOpContexts()) {

			BeanSession bs = sm.getBeanContext().getSession();

			Method m = sm.getJavaMethod();
			MethodInfo mi = MethodInfo.of(m);
			AnnotationList al = mi.getAnnotationList(REST_OP_GROUP);
			String mn = m.getName();

			// Get the operation from the existing swagger so far.
			JsonMap op = getOperation(omSwagger, sm.getPathPattern(), sm.getHttpMethod().toLowerCase());

			// Add @RestOp(swagger)
			Value<OpSwagger> _ms = Value.empty();
			al.forEachValue(OpSwagger.class, "swagger", OpSwaggerAnnotation::notEmpty, x -> _ms.set(x));
			OpSwagger ms = _ms.orElseGet(()->OpSwaggerAnnotation.create().build());

			op.append(parseMap(ms.value(), "@OpSwagger(value) on class {0} method {1}", c, m));
			op.appendIf(ne, "operationId",
				firstNonEmpty(
					resolve(ms.operationId()),
					op.getString("operationId"),
					mn
				)
			);

			Value<String> _summary = Value.empty();
			al.forEachValue(String.class, "summary", NOT_EMPTY, x -> _summary.set(x));
			op.appendIf(ne, "summary",
				firstNonEmpty(
					resolve(ms.summary()),
					resolve(mb.findFirstString(mn + ".summary")),
					op.getString("summary"),
					resolve(_summary.orElse(null))
				)
			);

			Value<String[]> _description = Value.empty();
			al.forEachValue(String[].class, "description",x -> x.length > 0, x -> _description.set(x));
			op.appendIf(ne, "description",
				firstNonEmpty(
					resolve(ms.description()),
					resolve(mb.findFirstString(mn + ".description")),
					op.getString("description"),
					resolve(_description.orElse(new String[0]))
				)
			);
			op.appendIf(ne, "deprecated",
				firstNonEmpty(
					resolve(ms.deprecated()),
					(m.getAnnotation(Deprecated.class) != null || m.getDeclaringClass().getAnnotation(Deprecated.class) != null) ? "true" : null
				)
			);
			op.appendIf(nec, "tags",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".tags"), "Messages/tags on class {0} method {1}", c, m),
					parseListOrCdl(ms.tags(), "@OpSwagger(tags) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, "schemes",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".schemes"), "Messages/schemes on class {0} method {1}", c, m),
					parseListOrCdl(ms.schemes(), "@OpSwagger(schemes) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, "consumes",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".consumes"), "Messages/consumes on class {0} method {1}", c, m),
					parseListOrCdl(ms.consumes(), "@OpSwagger(consumes) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, "produces",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".produces"), "Messages/produces on class {0} method {1}", c, m),
					parseListOrCdl(ms.produces(), "@OpSwagger(produces) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nec, "parameters",
				merge(
					parseList(mb.findFirstString(mn + ".parameters"), "Messages/parameters on class {0} method {1}", c, m),
					parseList(ms.parameters(), "@OpSwagger(parameters) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nem, "responses",
				merge(
					parseMap(mb.findFirstString(mn + ".responses"), "Messages/responses on class {0} method {1}", c, m),
					parseMap(ms.responses(), "@OpSwagger(responses) on class {0} method {1}", c, m)
				)
			);
			op.appendIf(nem, "externalDocs",
				merge(
					op.getMap("externalDocs"),
					parseMap(mb.findFirstString(mn + ".externalDocs"), "Messages/externalDocs on class {0} method {1}", c, m),
					toMap(ms.externalDocs(), "@OpSwagger(externalDocs) on class {0} method {1}", c, m)
				)
			);

			if (op.containsKey("tags"))
				for (String tag : op.getList("tags").elements(String.class))
					if (! tagMap.containsKey(tag))
						tagMap.put(tag, JsonMap.of("name", tag));

			JsonMap paramMap = new JsonMap();
			if (op.containsKey("parameters"))
				for (JsonMap param : op.getList("parameters").elements(JsonMap.class))
					paramMap.put(param.getString("in") + '.' + ("body".equals(param.getString("in")) ? "body" : param.getString("name")), param);

			// Finally, look for parameters defined on method.
			for (ParamInfo mpi : mi.getParams()) {

				ClassInfo pt = mpi.getParameterType();
				Type type = pt.innerType();

				if (mpi.hasAnnotation(Content.class) || pt.hasAnnotation(Content.class)) {
					JsonMap param = paramMap.getMap(BODY + ".body", true).append("in", BODY);
					JsonMap schema = getSchema(param.getMap("schema"), type, bs);
					mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(schema, x));
					mpi.forEachAnnotation(Content.class, x -> true, x -> merge(schema, x.schema()));
					pushupSchemaFields(BODY, param, schema);
					param.appendIf(nem, "schema", schema);
					param.putIfAbsent("required", true);
					addBodyExamples(sm, param, false, type, locale);

				} else if (mpi.hasAnnotation(Query.class) || pt.hasAnnotation(Query.class)) {
					String name = QueryAnnotation.findName(mpi).orElse(null);
					JsonMap param = paramMap.getMap(QUERY + "." + name, true).append("name", name).append("in", QUERY);
					mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(param, x));
					mpi.forEachAnnotation(Query.class, x -> true, x -> merge(param, x.schema()));
					pushupSchemaFields(QUERY, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, QUERY, type);

				} else if (mpi.hasAnnotation(FormData.class) || pt.hasAnnotation(FormData.class)) {
					String name = FormDataAnnotation.findName(mpi).orElse(null);
					JsonMap param = paramMap.getMap(FORM_DATA + "." + name, true).append("name", name).append("in", FORM_DATA);
					mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(param, x));
					mpi.forEachAnnotation(FormData.class, x -> true, x -> merge(param, x.schema()));
					pushupSchemaFields(FORM_DATA, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, FORM_DATA, type);

				} else if (mpi.hasAnnotation(Header.class) || pt.hasAnnotation(Header.class)) {
					String name = HeaderAnnotation.findName(mpi).orElse(null);
					JsonMap param = paramMap.getMap(HEADER + "." + name, true).append("name", name).append("in", HEADER);
					mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(param, x));
					mpi.forEachAnnotation(Header.class, x -> true, x -> merge(param, x.schema()));
					pushupSchemaFields(HEADER, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, HEADER, type);

				} else if (mpi.hasAnnotation(Path.class) || pt.hasAnnotation(Path.class)) {
					String name = PathAnnotation.findName(mpi).orElse(null);
					JsonMap param = paramMap.getMap(PATH + "." + name, true).append("name", name).append("in", PATH);
					mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(param, x));
					mpi.forEachAnnotation(Path.class, x -> true, x -> merge(param, x.schema()));
					pushupSchemaFields(PATH, param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, PATH, type);
					param.putIfAbsent("required", true);
				}
			}

			if (! paramMap.isEmpty())
				op.put("parameters", paramMap.values());

			JsonMap responses = op.getMap("responses", true);

			for (ClassInfo eci : mi.getExceptionTypes()) {
				if (eci.hasAnnotation(Response.class)) {
					List<Response> la = eci.getAnnotations(context, Response.class);
					List<StatusCode> la2 = eci.getAnnotations(context, StatusCode.class);
					Set<Integer> codes = getCodes(la2, 500);
					for (Response a : la) {
						for (Integer code : codes) {
							JsonMap om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							JsonMap schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
							eci.forEachAnnotation(Schema.class, x -> true, x -> merge(schema, x));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, "schema", schema);
						}
					}
					List<MethodInfo> methods = eci.getMethods();
					for (int i = methods.size()-1; i>=0; i--) {
						MethodInfo ecmi = methods.get(i);
						Header a = ecmi.getAnnotation(Header.class);
						if (a == null)
							a = ecmi.getReturnType().unwrap(Value.class,Optional.class).getAnnotation(Header.class);
						if (a != null && ! isMulti(a)) {
							String ha = a.name();
							for (Integer code : codes) {
								JsonMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
								ecmi.forEachAnnotation(context, Schema.class, x-> true, x -> merge(header, x));
								ecmi.getReturnType().unwrap(Value.class,Optional.class).forEachAnnotation(Schema.class, x -> true, x -> merge(header, x));
								pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header.getMap("schema"), ecmi.getReturnType().unwrap(Value.class,Optional.class).innerType(), bs));
							}
						}
					}
				}
			}

			if (mi.hasAnnotation(Response.class) || mi.getReturnType().unwrap(Value.class,Optional.class).hasAnnotation(Response.class)) {
				List<Response> la = list();
				mi.forEachAnnotation(context, Response.class, x -> true, x -> la.add(x));
				List<StatusCode> la2 = list();
				mi.forEachAnnotation(context, StatusCode.class, x -> true, x -> la2.add(x));
				Set<Integer> codes = getCodes(la2, 200);
				for (Response a : la) {
					for (Integer code : codes) {
						JsonMap om = responses.getMap(String.valueOf(code), true);
						merge(om, a);
						JsonMap schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
						mi.forEachAnnotation(context, Schema.class, x -> true, x -> merge(schema, x));
						pushupSchemaFields(RESPONSE, om, schema);
						om.appendIf(nem, "schema", schema);
						addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
					}
				}
				if (mi.getReturnType().hasAnnotation(Response.class)) {
					List<MethodInfo> methods = mi.getReturnType().getMethods();
					for (int i = methods.size()-1; i>=0; i--) {
						MethodInfo ecmi = methods.get(i);
						if (ecmi.hasAnnotation(Header.class)) {
							Header a = ecmi.getAnnotation(Header.class);
							String ha = a.name();
							if (! isMulti(a)) {
								for (Integer code : codes) {
									JsonMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
									ecmi.forEachAnnotation(context, Schema.class, x -> true, x -> merge(header, x));
									ecmi.getReturnType().unwrap(Value.class,Optional.class).forEachAnnotation(Schema.class, x -> true, x -> merge(header, x));
									merge(header, a.schema());
									pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header, ecmi.getReturnType().innerType(), bs));
								}
							}
						}
					}
				}
			} else if (m.getGenericReturnType() != void.class) {
				JsonMap om = responses.getMap("200", true);
				ClassInfo pt2 = ClassInfo.of(m.getGenericReturnType());
				JsonMap schema = getSchema(om.getMap("schema"), m.getGenericReturnType(), bs);
				pt2.forEachAnnotation(Schema.class, x -> true, x -> merge(schema, x));
				pushupSchemaFields(RESPONSE, om, schema);
				om.appendIf(nem, "schema", schema);
				addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
			}

			// Finally, look for Value @Header parameters defined on method.
			for (ParamInfo mpi : mi.getParams()) {

				ClassInfo pt = mpi.getParameterType();

				if (pt.is(Value.class) && (mpi.hasAnnotation(Header.class) || pt.hasAnnotation(Header.class))) {
					List<Header> la = list();
					mpi.forEachAnnotation(Header.class, x -> true, x -> la.add(x));
					pt.forEachAnnotation(Header.class, x -> true, x -> la.add(x));
					List<StatusCode> la2 = list();
					mpi.forEachAnnotation(StatusCode.class, x -> true, x -> la2.add(x));
					pt.forEachAnnotation(StatusCode.class, x -> true, x -> la2.add(x));
					Set<Integer> codes = getCodes(la2, 200);
					String name = HeaderAnnotation.findName(mpi).orElse(null);
					Type type = Value.unwrap(mpi.getParameterType().innerType());
					for (Header a : la) {
						if (! isMulti(a)) {
							for (Integer code : codes) {
								JsonMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(name, true);
								mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(header, x));
								merge(header, a.schema());
								pushupSchemaFields(RESPONSE_HEADER, header, getSchema(header, type, bs));
							}
						}
					}

				} else if (mpi.hasAnnotation(Response.class) || pt.hasAnnotation(Response.class)) {
					List<Response> la = list();
					mpi.forEachAnnotation(Response.class, x -> true, x -> la.add(x));
					pt.forEachAnnotation(Response.class, x -> true, x -> la.add(x));
					List<StatusCode> la2 = list();
					mpi.forEachAnnotation(StatusCode.class, x -> true, x -> la2.add(x));
					pt.forEachAnnotation(StatusCode.class, x -> true, x -> la2.add(x));
					Set<Integer> codes = getCodes(la2, 200);
					Type type = Value.unwrap(mpi.getParameterType().innerType());
					for (Response a : la) {
						for (Integer code : codes) {
							JsonMap om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							JsonMap schema = getSchema(om.getMap("schema"), type, bs);
							mpi.forEachAnnotation(Schema.class, x -> true, x -> merge(schema, x));
							la.forEach(x -> merge(schema, x.schema()));
							pushupSchemaFields(RESPONSE, om, schema);
							om.appendIf(nem, "schema", schema);
						}
					}
				}
			}

			// Add default response descriptions.
			for (Map.Entry<String,Object> e : responses.entrySet()) {
				String key = e.getKey();
				JsonMap val = responses.getMap(key);
				if (StringUtils.isDecimal(key))
					val.appendIfAbsentIf(ne, "description", RestUtils.getHttpResponseText(Integer.parseInt(key)));
			}

			if (responses.isEmpty())
				op.remove("responses");
			else
				op.put("responses", new TreeMap<>(responses));

			if (! op.containsKey("consumes")) {
				List<MediaType> mConsumes = sm.getSupportedContentTypes();
				if (! mConsumes.equals(consumes))
					op.put("consumes", mConsumes);
			}

			if (! op.containsKey("produces")) {
				List<MediaType> mProduces = sm.getSupportedAcceptTypes();
				if (! mProduces.equals(produces))
					op.put("produces", mProduces);
			}
		}

		if (js.getBeanDefs() != null)
			for (Map.Entry<String,JsonMap> e : js.getBeanDefs().entrySet())
				definitions.put(e.getKey(), fixSwaggerExtensions(e.getValue()));
		if (definitions.isEmpty())
			omSwagger.remove("definitions");

		if (! tagMap.isEmpty())
			omSwagger.put("tags", tagMap.values());

		if (consumes.isEmpty())
			omSwagger.remove("consumes");
		if (produces.isEmpty())
			omSwagger.remove("produces");

//		try {
//			if (! omSwagger.isEmpty())
//				assertNoEmpties(omSwagger);
//		} catch (SwaggerException e1) {
//			System.err.println(omSwagger.toString(Json5Serializer.DEFAULT_READABLE));
//			throw e1;
//		}

		try {
			String swaggerJson = Json5Serializer.DEFAULT_READABLE.toString(omSwagger);
//			System.err.println(swaggerJson);
			return jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new ServletException("Error detected in swagger.", e);
		}
	}
	//=================================================================================================================
	// Utility methods
	//=================================================================================================================

	private boolean isMulti(Header h) {
		if ("*".equals(h.name()) || "*".equals(h.value()))
			return true;
		return false;
	}

	private JsonMap resolve(JsonMap om) throws ParseException {
		JsonMap om2 = null;
		if (om.containsKey("_value")) {
			om = om.modifiable();
			om2 = parseMap(om.remove("_value"));
		} else {
			om2 = new JsonMap();
		}
		for (Map.Entry<String,Object> e : om.entrySet()) {
			Object val = e.getValue();
			if (val instanceof JsonMap) {
				val = resolve((JsonMap)val);
			} else if (val instanceof JsonList) {
				val = resolve((JsonList) val);
			} else if (val instanceof String) {
				val = resolve(val.toString());
			}
			om2.put(e.getKey(), val);
		}
		return om2;
	}

	private JsonList resolve(JsonList om) throws ParseException {
		JsonList ol2 = new JsonList();
		for (Object val : om) {
			if (val instanceof JsonMap) {
				val = resolve((JsonMap)val);
			} else if (val instanceof JsonList) {
				val = resolve((JsonList) val);
			} else if (val instanceof String) {
				val = resolve(val.toString());
			}
			ol2.add(val);
		}
		return ol2;
	}

	private String resolve(String[]...s) {
		for (String[] ss : s) {
			if (ss.length != 0)
				return resolve(joinnl(ss));
		}
		return null;
	}

	private String resolve(String s) {
		if (s == null)
			return null;
		return vr.resolve(s.trim());
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

	private JsonMap parseMap(String o, String location, Object...args) throws ParseException {
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private JsonMap parseMap(Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[])
			o = joinnl((String[])o);
		if (o instanceof String) {
			String s = o.toString();
			if (s.isEmpty())
				return null;
			s = resolve(s);
			if ("IGNORE".equalsIgnoreCase(s))
				return JsonMap.of("ignore", true);
			if (! isJsonObject(s, true))
				s = "{" + s + "}";
			return JsonMap.ofJson(s);
		}
		if (o instanceof JsonMap)
			return (JsonMap)o;
		throw new SwaggerException(null, "Unexpected data type ''{0}''.  Expected JsonMap or String.", className(o));
	}

	private JsonList parseList(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			if (! isJsonArray(s, true))
				s = "[" + s + "]";
			return JsonList.ofJson(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private JsonList parseListOrCdl(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			return JsonList.ofJsonOrCdl(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private JsonMap merge(JsonMap...maps) {
		JsonMap m = maps[0];
		for (int i = 1; i < maps.length; i++) {
			if (maps[i] != null) {
				if (m == null)
					m = new JsonMap();
				m.putAll(maps[i]);
			}
		}
		return m;
	}

	private JsonList merge(JsonList...lists) {
		JsonList l = lists[0];
		for (int i = 1; i < lists.length; i++) {
			if (lists[i] != null) {
				if (l == null)
					l = new JsonList();
				l.addAll(lists[i]);
			}
		}
		return l;
	}

	@SafeVarargs
	private final <T> T firstNonEmpty(T...t) {
		for (T oo : t)
			if (! ObjectUtils.isEmpty(oo))
				return oo;
		return null;
	}

	private JsonMap toMap(ExternalDocs a, String location, Object...locationArgs) {
		if (ExternalDocsAnnotation.empty(a))
			return null;
		Predicate<String> ne = StringUtils::isNotEmpty;
		JsonMap om = JsonMap.create()
			.appendIf(ne, "description", resolve(joinnl(a.description())))
			.appendIf(ne, "url", resolve(a.url()));
		return nullIfEmpty(om);
	}

	private JsonMap toMap(Contact a, String location, Object...locationArgs) {
		if (ContactAnnotation.empty(a))
			return null;
		Predicate<String> ne = StringUtils::isNotEmpty;
		JsonMap om = JsonMap.create()
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "url", resolve(a.url()))
			.appendIf(ne, "email", resolve(a.email()));
		return nullIfEmpty(om);
	}

	private JsonMap toMap(License a, String location, Object...locationArgs) {
		if (LicenseAnnotation.empty(a))
			return null;
		Predicate<String> ne = StringUtils::isNotEmpty;
		JsonMap om = JsonMap.create()
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "url", resolve(a.url()));
		return nullIfEmpty(om);
	}

	private JsonMap toMap(Tag a, String location, Object...locationArgs) {
		JsonMap om = JsonMap.create();
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		om
			.appendIf(ne, "name", resolve(a.name()))
			.appendIf(ne, "description", resolve(joinnl(a.description())))
			.appendIf(nem, "externalDocs", merge(om.getMap("externalDocs"), toMap(a.externalDocs(), location, locationArgs)));
		return nullIfEmpty(om);
	}

	private JsonList toList(Tag[] aa, String location, Object...locationArgs) {
		if (aa.length == 0)
			return null;
		JsonList ol = new JsonList();
		for (Tag a : aa)
			ol.add(toMap(a, location, locationArgs));
		return nullIfEmpty(ol);
	}

	private JsonMap getSchema(JsonMap schema, Type type, BeanSession bs) throws Exception {

		if (type == Swagger.class)
			return JsonMap.create();

		schema = newMap(schema);

		ClassMeta<?> cm = bs.getClassMeta(type);

		if (schema.getBoolean("ignore", false))
			return null;

		if (schema.containsKey("type") || schema.containsKey("$ref"))
			return schema;

		JsonMap om = fixSwaggerExtensions(schema.append(js.getSchema(cm)));

		return om;
	}

	/**
	 * Replaces non-standard JSON-Schema attributes with standard Swagger attributes.
	 */
	private JsonMap fixSwaggerExtensions(JsonMap om) {
		Predicate<Object> nn = ObjectUtils::isNotNull;
		om
			.appendIf(nn, "discriminator", om.remove("x-discriminator"))
			.appendIf(nn, "readOnly", om.remove("x-readOnly"))
			.appendIf(nn, "xml", om.remove("x-xml"))
			.appendIf(nn, "externalDocs", om.remove("x-externalDocs"))
			.appendIf(nn, "example", om.remove("x-example"));
		return nullIfEmpty(om);
	}

	private void addBodyExamples(RestOpContext sm, JsonMap piri, boolean response, Type type, Locale locale) throws Exception {

		String sex = piri.getString("example");

		if (sex == null) {
			JsonMap schema = resolveRef(piri.getMap("schema"));
			if (schema != null)
				sex = schema.getString("example", schema.getString("example"));
		}

		if (isEmpty(sex))
			return;

		Object example = null;
		if (isJson(sex)) {
			example = jp.parse(sex, type);
		} else {
			ClassMeta<?> cm = js.getClassMeta(type);
			if (cm.hasStringMutater()) {
				example = cm.getStringMutater().mutate(sex);
			}
		}

		String examplesKey = "examples";  // Parameters don't have an examples attribute.

		JsonMap examples = piri.getMap(examplesKey);
		if (examples == null)
			examples = new JsonMap();

		List<MediaType> mediaTypes = response ? sm.getSerializers().getSupportedMediaTypes() : sm.getParsers().getSupportedMediaTypes();

		for (MediaType mt : mediaTypes) {
			if (mt != MediaType.HTML) {
				Serializer s2 = sm.getSerializers().getSerializer(mt);
				if (s2 != null) {
					try {
						String eVal = s2
							.createSession()
							.locale(locale)
							.mediaType(mt)
							.apply(WriterSerializerSession.Builder.class, x -> x.useWhitespace(true))
							.build()
							.serializeToString(example);
						examples.put(s2.getPrimaryMediaType().toString(), eVal);
					} catch (Exception e) {
						System.err.println("Could not serialize to media type ["+mt+"]: " + e.getLocalizedMessage());  // NOT DEBUG
					}
				}
			}
		}

		if (! examples.isEmpty())
			piri.put(examplesKey, examples);
	}

	private void addParamExample(RestOpContext sm, JsonMap piri, RestPartType in, Type type) throws Exception {

		String s = piri.getString("example");

		if (isEmpty(s))
			return;

		JsonMap examples = piri.getMap("examples");
		if (examples == null)
			examples = new JsonMap();

		String paramName = piri.getString("name");

		if (in == QUERY)
			s = "?" + urlEncodeLax(paramName) + "=" + urlEncodeLax(s);
		else if (in == FORM_DATA)
			s = paramName + "=" + s;
		else if (in == HEADER)
			s = paramName + ": " + s;
		else if (in == PATH)
			s = sm.getPathPattern().replace("{"+paramName+"}", urlEncodeLax(s));

		examples.put("example", s);

		if (! examples.isEmpty())
			piri.put("examples", examples);
	}


	private JsonMap resolveRef(JsonMap m) {
		if (m == null)
			return null;
		if (m.containsKey("$ref") && js.getBeanDefs() != null) {
			String ref = m.getString("$ref");
			if (ref.startsWith("#/definitions/"))
				return js.getBeanDefs().get(ref.substring(14));
		}
		return m;
	}

	private JsonMap getOperation(JsonMap om, String path, String httpMethod) {
		if (! om.containsKey("paths"))
			om.put("paths", new JsonMap());
		om = om.getMap("paths");
		if (! om.containsKey(path))
			om.put(path, new JsonMap());
		om = om.getMap(path);
		if (! om.containsKey(httpMethod))
			om.put(httpMethod, new JsonMap());
		return om.getMap(httpMethod);
	}

	private static JsonMap newMap(JsonMap om) {
		if (om == null)
			return new JsonMap();
		return om.modifiable();
	}

	private JsonMap merge(JsonMap om, Schema a) {
		try {
			if (SchemaAnnotation.empty(a))
				return om;
			om = newMap(om);
			Predicate<String> ne = StringUtils::isNotEmpty;
			Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
			Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
			Predicate<Boolean> nf = ObjectUtils::isTrue;
			Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
			return om
				.appendIf(nem, "additionalProperties", toJsonMap(a.additionalProperties()))
				.appendIf(ne, "allOf", joinnl(a.allOf()))
				.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
				.appendIf(ne, "default", joinnl(a._default(), a.df()))
				.appendIf(ne, "discriminator", a.discriminator())
				.appendIf(ne, "description", resolve(a.description(), a.d()))
				.appendFirst(nec, "enum", toSet(a._enum()), toSet(a.e()))
				.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
				.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
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
				.appendIf(nem, "properties", toJsonMap(a.properties()))
				.appendIf(nf, "readOnly", a.readOnly() || a.ro())
				.appendIf(nf, "required", a.required() || a.r())
				.appendIf(ne, "title", a.title())
				.appendFirst(ne, "type", a.type(), a.t())
				.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
				.appendIf(ne, "xml", joinnl(a.xml()))
				.appendIf(ne, "$ref", a.$ref())
			;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private JsonMap merge(JsonMap om, ExternalDocs a) {
		if (ExternalDocsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = StringUtils::isNotEmpty;
		return om
			.appendIf(ne, "description", resolve(a.description()))
			.appendIf(ne, "url", a.url())
		;
	}

	private JsonMap merge(JsonMap om, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
		return om
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a._default(), a.df()))
			.appendFirst(nec, "enum", toSet(a._enum()), toSet(a.e()))
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
	}

	private JsonMap merge(JsonMap om, SubItems a) throws ParseException {
		if (SubItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
		return om
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a._default(), a.df()))
			.appendFirst(nec, "enum", toSet(a._enum()), toSet(a.e()))
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendFirst(ne, "format", a.format(), a.f())
			.appendIf(nem, "items", toJsonMap(a.items()))
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
	}

	private JsonMap merge(JsonMap om, Response a) throws ParseException {
		if (ResponseAnnotation.empty(a))
			return om;
		om = newMap(om);
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		if (! SchemaAnnotation.empty(a.schema()))
			merge(om, a.schema());
		return om
			.appendIf(nem, "examples", parseMap(a.examples()))
			.appendIf(nem, "headers", merge(om.getMap("headers"), a.headers()))
			.appendIf(nem, "schema", merge(om.getMap("schema"), a.schema()))
		;
	}

	private JsonMap merge(JsonMap om, Header[] a) {
		if (a.length == 0)
			return om;
		om = newMap(om);
		for (Header aa : a) {
			String name = StringUtils.firstNonEmpty(aa.name(), aa.value());
			if (isEmpty(name))
				throw new RuntimeException("@Header used without name or value.");
			merge(om.getMap(name, true), aa.schema());
		}
		return om;
	}

	private JsonMap pushupSchemaFields(RestPartType type, JsonMap param, JsonMap schema) {
		Predicate<Object> ne = ObjectUtils::isNotEmpty;
		if (schema != null && ! schema.isEmpty()) {
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

			if ("object".equals(param.getString("type")) && ! schema.isEmpty())
				param.put("schema", schema);
			}
		}

		return param;
	}

	private JsonMap toJsonMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isJsonObject(s, true))
			s = "{" + s + "}";
		s = resolve(s);
		return JsonMap.ofJson(s);
	}

	private Set<String> toSet(String[] ss) {
		if (ss.length == 0)
			return null;
		Set<String> set = set();
		for (String s : ss)
			split(s, x -> set.add(x));
		return set.isEmpty() ? null : set;
	}

	static String joinnl(String[]...s) {
		for (String[] ss : s) {
			if (ss.length != 0)
			return StringUtils.joinnl(ss).trim();
		}
		return "";
	}

	private static Set<Integer> getCodes(List<StatusCode> la, Integer def) {
		Set<Integer> codes = new TreeSet<>();
		for (StatusCode a : la) {
			for (int i : a.value())
				codes.add(i);
		}
		if (codes.isEmpty() && def != null)
			codes.add(def);
		return codes;
	}

	private static JsonMap nullIfEmpty(JsonMap m) {
		return (m == null || m.isEmpty() ? null : m);
	}

	private static JsonList nullIfEmpty(JsonList l) {
		return (l == null || l.isEmpty() ? null : l);
	}
}
