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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestParamType.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Contact;
import org.apache.juneau.http.annotation.License;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.jsonschema.annotation.Tag;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * A single session of generating a Swagger document.
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

		// Load swagger JSON from classpath.
		OMap omSwagger = SimpleJson.DEFAULT.read(is, OMap.class);
		if (omSwagger == null)
			omSwagger = new OMap();

		// Combine it with @Rest(swagger)
		for (Rest rr : rci.getAnnotations(Rest.class)) {

			OMap sInfo = omSwagger.getMap("info", true);

			sInfo
				.appendSkipEmpty("title",
					firstNonEmpty(
						sInfo.getString("title"),
						resolve(rr.title())
					)
				)
				.appendSkipEmpty("description",
					firstNonEmpty(
						sInfo.getString("description"),
						resolve(rr.description())
					)
				);

			org.apache.juneau.rest.annotation.Swagger r = rr.swagger();

			omSwagger.append(parseMap(r.value(), "@Swagger(value) on class {0}", c));

			if (! SwaggerAnnotation.empty(r)) {
				OMap info = omSwagger.getMap("info", true);

				info
					.appendSkipEmpty("title", resolve(r.title()))
					.appendSkipEmpty("description", resolve(r.description()))
					.appendSkipEmpty("version", resolve(r.version()))
					.appendSkipEmpty("termsOfService", resolve(r.termsOfService()))
					.appendSkipEmpty("contact",
						merge(
							info.getMap("contact"),
							toMap(r.contact(), "@Swagger(contact) on class {0}", c)
						)
					)
					.appendSkipEmpty("license",
						merge(
							info.getMap("license"),
							toMap(r.license(), "@Swagger(license) on class {0}", c)
						)
					);
			}

			omSwagger
				.appendSkipEmpty("externalDocs",
					merge(
						omSwagger.getMap("externalDocs"),
						toMap(r.externalDocs(), "@Swagger(externalDocs) on class {0}", c)
					)
				)
				.appendSkipEmpty("tags",
					merge(
						omSwagger.getList("tags"),
						toList(r.tags(), "@Swagger(tags) on class {0}", c)
					)
				);
		}

		omSwagger.appendSkipEmpty("externalDocs", parseMap(mb.findFirstString("externalDocs"), "Messages/externalDocs on class {0}", c));

		OMap info = omSwagger.getMap("info", true);

		info
			.appendSkipEmpty("title", resolve(mb.findFirstString("title")))
			.appendSkipEmpty("description", resolve(mb.findFirstString("description")))
			.appendSkipEmpty("version", resolve(mb.findFirstString("version")))
			.appendSkipEmpty("termsOfService", resolve(mb.findFirstString("termsOfService")))
			.appendSkipEmpty("contact", parseMap(mb.findFirstString("contact"), "Messages/contact on class {0}", c))
			.appendSkipEmpty("license", parseMap(mb.findFirstString("license"), "Messages/license on class {0}", c));

		if (info.isEmpty())
			omSwagger.remove("info");

		OList
			produces = omSwagger.getList("produces", true),
			consumes = omSwagger.getList("consumes", true);
		if (consumes.isEmpty())
			consumes.addAll(context.getConsumes());
		if (produces.isEmpty())
			produces.addAll(context.getProduces());

		Map<String,OMap> tagMap = new LinkedHashMap<>();
		if (omSwagger.containsKey("tags")) {
			for (OMap om : omSwagger.getList("tags").elements(OMap.class)) {
				String name = om.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}

		String s = mb.findFirstString("tags");
		if (s != null) {
			for (OMap m : parseListOrCdl(s, "Messages/tags on class {0}", c).elements(OMap.class)) {
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
		OMap definitions = omSwagger.getMap("definitions", true);
		for (String defId : definitions.keySet())
			js.addBeanDef(defId, new OMap(definitions.getMap(defId)));

		// Iterate through all the @RestOp methods.
		for (RestOpContext sm : context.getOpContexts()) {

			BeanSession bs = sm.getBeanContext().createBeanSession();

			Method m = sm.getJavaMethod();
			MethodInfo mi = MethodInfo.of(m);
			AnnotationList al = mi.getAnnotationGroupList(RestOp.class);
			String mn = m.getName();

			// Get the operation from the existing swagger so far.
			OMap op = getOperation(omSwagger, sm.getPathPattern(), sm.getHttpMethod().toLowerCase());

			// Add @RestOp(swagger)
			OpSwagger ms = al.getValues(OpSwagger.class, "swagger").stream().filter(x -> ! OpSwaggerAnnotation.empty(x)).findFirst().orElse(OpSwaggerAnnotation.create().build());

			op.append(parseMap(ms.value(), "@OpSwagger(value) on class {0} method {1}", c, m));
			op.appendSkipEmpty("operationId",
				firstNonEmpty(
					resolve(ms.operationId()),
					op.getString("operationId"),
					mn
				)
			);
			op.appendSkipEmpty("summary",
				firstNonEmpty(
					resolve(ms.summary()),
					resolve(mb.findFirstString(mn + ".summary")),
					op.getString("summary"),
					resolve(al.getValues(String.class, "summary").stream().filter(x -> !x.isEmpty()).findFirst().orElse(null))
				)
			);
			op.appendSkipEmpty("description",
				firstNonEmpty(
					resolve(ms.description()),
					resolve(mb.findFirstString(mn + ".description")),
					op.getString("description"),
					resolve(al.getValues(String[].class, "description").stream().filter(x -> x.length > 0).findFirst().orElse(new String[0]))
				)
			);
			op.appendSkipEmpty("deprecated",
				firstNonEmpty(
					resolve(ms.deprecated()),
					(m.getAnnotation(Deprecated.class) != null || m.getDeclaringClass().getAnnotation(Deprecated.class) != null) ? "true" : null
				)
			);
			op.appendSkipEmpty("tags",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".tags"), "Messages/tags on class {0} method {1}", c, m),
					parseListOrCdl(ms.tags(), "@OpSwagger(tags) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("schemes",
				merge(
					parseListOrCdl(mb.findFirstString(mn + ".schemes"), "Messages/schemes on class {0} method {1}", c, m),
					parseListOrCdl(ms.schemes(), "@OpSwagger(schemes) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("consumes",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".consumes"), "Messages/consumes on class {0} method {1}", c, m),
					parseListOrCdl(ms.consumes(), "@OpSwagger(consumes) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("produces",
				firstNonEmpty(
					parseListOrCdl(mb.findFirstString(mn + ".produces"), "Messages/produces on class {0} method {1}", c, m),
					parseListOrCdl(ms.produces(), "@OpSwagger(produces) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("parameters",
				merge(
					parseList(mb.findFirstString(mn + ".parameters"), "Messages/parameters on class {0} method {1}", c, m),
					parseList(ms.parameters(), "@OpSwagger(parameters) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("responses",
				merge(
					parseMap(mb.findFirstString(mn + ".responses"), "Messages/responses on class {0} method {1}", c, m),
					parseMap(ms.responses(), "@OpSwagger(responses) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("externalDocs",
				merge(
					op.getMap("externalDocs"),
					parseMap(mb.findFirstString(mn + ".externalDocs"), "Messages/externalDocs on class {0} method {1}", c, m),
					toMap(ms.externalDocs(), "@OpSwagger(externalDocs) on class {0} method {1}", c, m)
				)
			);

			if (op.containsKey("tags"))
				for (String tag : op.getList("tags").elements(String.class))
					if (! tagMap.containsKey(tag))
						tagMap.put(tag, OMap.of("name", tag));

			OMap paramMap = new OMap();
			if (op.containsKey("parameters"))
				for (OMap param : op.getList("parameters").elements(OMap.class))
					paramMap.put(param.getString("in") + '.' + ("body".equals(param.getString("in")) ? "body" : param.getString("name")), param);

			// Finally, look for parameters defined on method.
			for (ParamInfo mpi : mi.getParams()) {

				ClassInfo pt = mpi.getParameterType();
				Type type = pt.innerType();

				if (mpi.hasAnnotation(Body.class) || pt.hasAnnotation(Body.class)) {
					OMap param = paramMap.getMap(BODY + ".body", true).a("in", BODY);
					for (Body a : mpi.getAnnotations(Body.class))
						merge(param, a);
					for (Body a : pt.getAnnotations(Body.class))
						merge(param, a);
					param.putIfAbsent("required", true);
					param.appendSkipEmpty("schema", getSchema(param.getMap("schema"), type, bs));
					addBodyExamples(sm, param, false, type, locale);

				} else if (mpi.hasAnnotation(Query.class) || pt.hasAnnotation(Query.class)) {
					String name = null;
					for (Query a : mpi.getAnnotations(Query.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					for (Query a : pt.getAnnotations(Query.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					OMap param = paramMap.getMap(QUERY + "." + name, true).a("name", name).a("in", QUERY);
					for (Query a : mpi.getAnnotations(Query.class))
						merge(param, a);
					for (Query a : pt.getAnnotations(Query.class))
						merge(param, a);
					mergePartSchema(param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, QUERY, type);

				} else if (mpi.hasAnnotation(FormData.class) || pt.hasAnnotation(FormData.class)) {
					String name = null;
					for (FormData a : mpi.getAnnotations(FormData.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					for (FormData a : pt.getAnnotations(FormData.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					OMap param = paramMap.getMap(FORM_DATA + "." + name, true).a("name", name).a("in", FORM_DATA);
					for (FormData a : mpi.getAnnotations(FormData.class))
						merge(param, a);
					for (FormData a : pt.getAnnotations(FormData.class))
						merge(param, a);
					mergePartSchema(param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, FORM_DATA, type);

				} else if (mpi.hasAnnotation(Header.class) || pt.hasAnnotation(Header.class)) {
					String name = null;
					for (Header a : mpi.getAnnotations(Header.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					for (Header a : pt.getAnnotations(Header.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					OMap param = paramMap.getMap(HEADER + "." + name, true).a("name", name).a("in", HEADER);
					for (Header a : mpi.getAnnotations(Header.class))
						merge(param, a);
					for (Header a : pt.getAnnotations(Header.class))
						merge(param, a);
					mergePartSchema(param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, HEADER, type);

				} else if (mpi.hasAnnotation(Path.class) || pt.hasAnnotation(Path.class)) {
					String name = null;
					for (Path a : mpi.getAnnotations(Path.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					for (Path a : pt.getAnnotations(Path.class))
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					OMap param = paramMap.getMap(PATH + "." + name, true).a("name", name).a("in", PATH);
					for (Path a : mpi.getAnnotations(Path.class))
						merge(param, a);
					for (Path a : pt.getAnnotations(Path.class))
						merge(param, a);
					mergePartSchema(param, getSchema(param.getMap("schema"), type, bs));
					addParamExample(sm, param, PATH, type);
					param.putIfAbsent("required", true);
				}
			}

			if (! paramMap.isEmpty())
				op.put("parameters", paramMap.values());

			OMap responses = op.getMap("responses", true);

			for (ClassInfo eci : mi.getExceptionTypes()) {
				if (eci.hasAnnotation(Response.class)) {
					List<Response> la = eci.getAnnotations(Response.class);
					Set<Integer> codes = getCodes(la, 500);
					for (Response a : la) {
						for (Integer code : codes) {
							OMap om = responses.getMap(String.valueOf(code), true);
							merge(om, a);
							if (! om.containsKey("schema"))
								om.appendSkipEmpty("schema", getSchema(om.getMap("schema"), eci.inner(), bs));
						}
					}
					for (MethodInfo ecmi : eci.getAllMethodsParentFirst()) {
						ResponseHeader a = ecmi.getLastAnnotation(ResponseHeader.class);
						if (a == null)
							a = ecmi.getReturnType().unwrap(Value.class,Optional.class).getLastAnnotation(ResponseHeader.class);
						if (a != null && ! isMulti(a)) {
							String ha = a.name();
							for (Integer code : codes) {
								OMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
								merge(header, a);
								mergePartSchema(header, getSchema(header, ecmi.getReturnType().innerType(), bs));
							}
						}
					}
				}
			}

			if (mi.hasAnnotation(Response.class) || mi.getReturnType().unwrap(Value.class,Optional.class).hasAnnotation(Response.class)) {
				List<Response> la = mi.getAnnotations(Response.class);
				Set<Integer> codes = getCodes(la, 200);
				for (Response a : la) {
					for (Integer code : codes) {
						OMap om = responses.getMap(String.valueOf(code), true);
						merge(om, a);
						if (! om.containsKey("schema"))
							om.appendSkipEmpty("schema", getSchema(om.getMap("schema"), m.getGenericReturnType(), bs));
						addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
					}
				}
				if (mi.getReturnType().hasAnnotation(Response.class)) {
					for (MethodInfo ecmi : mi.getReturnType().getAllMethodsParentFirst()) {
						if (ecmi.hasAnnotation(ResponseHeader.class)) {
							ResponseHeader a = ecmi.getLastAnnotation(ResponseHeader.class);
							String ha = a.name();
							if (! isMulti(a)) {
								for (Integer code : codes) {
									OMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(ha, true);
									merge(header, a);
									mergePartSchema(header, getSchema(header, ecmi.getReturnType().innerType(), bs));
								}
							}
						}
					}
				}
			} else if (m.getGenericReturnType() != void.class) {
				OMap om = responses.getMap("200", true);
				if (! om.containsKey("schema"))
					om.appendSkipEmpty("schema", getSchema(om.getMap("schema"), m.getGenericReturnType(), bs));
				addBodyExamples(sm, om, true, m.getGenericReturnType(), locale);
			}

			// Finally, look for @ResponseHeader parameters defined on method.
			for (ParamInfo mpi : mi.getParams()) {

				ClassInfo pt = mpi.getParameterType();

				if (mpi.hasAnnotation(ResponseHeader.class) || pt.hasAnnotation(ResponseHeader.class)) {
					List<ResponseHeader> la = AList.of(mpi.getAnnotations(ResponseHeader.class)).a(pt.getAnnotations(ResponseHeader.class));
					Set<Integer> codes = getCodes2(la, 200);
					String name = null;
					for (ResponseHeader a : la)
						name = firstNonEmpty(a.name(), a.n(), a.value(), name);
					Type type = mpi.getParameterType().innerType();
					for (ResponseHeader a : la) {
						if (! isMulti(a)) {
							for (Integer code : codes) {
								OMap header = responses.getMap(String.valueOf(code), true).getMap("headers", true).getMap(name, true);
								merge(header, a);
								mergePartSchema(header, getSchema(header, Value.getParameterType(type), bs));
							}
						}
					}

				} else if (mpi.hasAnnotation(Response.class) || pt.hasAnnotation(Response.class)) {
					List<Response> la = AList.of(mpi.getAnnotations(Response.class)).a(pt.getAnnotations(Response.class));
					Set<Integer> codes = getCodes(la, 200);
					Type type = mpi.getParameterType().innerType();
					for (Response a : la) {
						for (Integer code : codes) {
							OMap response = responses.getMap(String.valueOf(code), true);
							merge(response, a);
						}
					}
					type = Value.getParameterType(type);
					if (type != null) {
						for (String code : responses.keySet()) {
							OMap om = responses.getMap(code);
							if (! om.containsKey("schema"))
								om.appendSkipEmpty("schema", getSchema(om.getMap("schema"), type, bs));
						}
					}
				}
			}

			// Add default response descriptions.
			for (Map.Entry<String,Object> e : responses.entrySet()) {
				String key = e.getKey();
				OMap val = responses.getMap(key);
				if (StringUtils.isDecimal(key))
					val.appendIf(false, true, true, "description", RestUtils.getHttpResponseText(Integer.parseInt(key)));
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
			for (Map.Entry<String,OMap> e : js.getBeanDefs().entrySet())
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
//			System.err.println(omSwagger.toString(SimpleJsonSerializer.DEFAULT_READABLE));
//			throw e1;
//		}

		try {
			String swaggerJson = SimpleJsonSerializer.DEFAULT_READABLE.toString(omSwagger);
//			System.err.println(swaggerJson);
			return jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new RestServletException(e, "Error detected in swagger.");
		}
	}
	//=================================================================================================================
	// Utility methods
	//=================================================================================================================

	private boolean isMulti(ResponseHeader h) {
		if ("*".equals(h.name()) || "*".equals(h.value()))
			return true;
		return false;
	}

	private OMap resolve(OMap om) throws ParseException {
		OMap om2 = null;
		if (om.containsKey("_value")) {
			om = om.modifiable();
			om2 = parseMap(om.remove("_value"));
		} else {
			om2 = new OMap();
		}
		for (Map.Entry<String,Object> e : om.entrySet()) {
			Object val = e.getValue();
			if (val instanceof OMap) {
				val = resolve((OMap)val);
			} else if (val instanceof OList) {
				val = resolve((OList) val);
			} else if (val instanceof String) {
				val = resolve(val.toString());
			}
			om2.put(e.getKey(), val);
		}
		return om2;
	}

	private OList resolve(OList om) throws ParseException {
		OList ol2 = new OList();
		for (Object val : om) {
			if (val instanceof OMap) {
				val = resolve((OMap)val);
			} else if (val instanceof OList) {
				val = resolve((OList) val);
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

	private OMap parseMap(String[] o, String location, Object...args) throws ParseException {
		if (o.length == 0)
			return OMap.EMPTY_MAP;
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private OMap parseMap(String o, String location, Object...args) throws ParseException {
		try {
			return parseMap(o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private OMap parseMap(Object o) throws ParseException {
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
				return OMap.of("ignore", true);
			if (! isJsonObject(s, true))
				s = "{" + s + "}";
			return OMap.ofJson(s);
		}
		if (o instanceof OMap)
			return (OMap)o;
		throw new SwaggerException(null, "Unexpected data type ''{0}''.  Expected OMap or String.", className(o));
	}

	private OList parseList(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			if (! isJsonArray(s, true))
				s = "[" + s + "]";
			return OList.ofJson(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private OList parseListOrCdl(Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = resolve(s);
			return StringUtils.parseListOrCdl(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private OMap newMap(OMap om, String[] value, String location, Object...locationArgs) throws ParseException {
		if (value.length == 0)
			return om == null ? new OMap() : om;
			OMap om2 = parseMap(joinnl(value), location, locationArgs);
		if (om == null)
			return om2;
		return om.append(om2);
	}

	private OMap merge(OMap...maps) {
		OMap m = maps[0];
		for (int i = 1; i < maps.length; i++) {
			if (maps[i] != null) {
				if (m == null)
					m = new OMap();
				m.putAll(maps[i]);
			}
		}
		return m;
	}

	private OList merge(OList...lists) {
		OList l = lists[0];
		for (int i = 1; i < lists.length; i++) {
			if (lists[i] != null) {
				if (l == null)
					l = new OList();
				l.addAll(lists[i]);
			}
		}
		return l;
	}

	@SafeVarargs
	private final <T> T firstNonEmpty(T...t) {
		return ObjectUtils.firstNonEmpty(t);
	}

	private OMap toMap(ExternalDocs a, String location, Object...locationArgs) throws ParseException {
		if (ExternalDocsAnnotation.empty(a))
			return null;
		OMap om = newMap(new OMap(), a.value(), location, locationArgs)
			.appendSkipEmpty("description", resolve(joinnl(a.description())))
			.appendSkipEmpty("url", resolve(a.url()));
		return nullIfEmpty(om);
	}

	private OMap toMap(Contact a, String location, Object...locationArgs) throws ParseException {
		if (ContactAnnotation.empty(a))
			return null;
		OMap om = newMap(new OMap(), a.value(), location, locationArgs)
			.appendSkipEmpty("name", resolve(a.name()))
			.appendSkipEmpty("url", resolve(a.url()))
			.appendSkipEmpty("email", resolve(a.email()));
		return nullIfEmpty(om);
	}

	private OMap toMap(License a, String location, Object...locationArgs) throws ParseException {
		if (LicenseAnnotation.empty(a))
			return null;
		OMap om = newMap(new OMap(), a.value(), location, locationArgs)
			.appendSkipEmpty("name", resolve(a.name()))
			.appendSkipEmpty("url", resolve(a.url()));
		return nullIfEmpty(om);
	}

	private OMap toMap(Tag a, String location, Object...locationArgs) throws ParseException {
		OMap om = newMap(new OMap(), a.value(), location, locationArgs);
		om
			.appendSkipEmpty("name", resolve(a.name()))
			.appendSkipEmpty("description", resolve(joinnl(a.description())))
			.appendSkipNull("externalDocs", merge(om.getMap("externalDocs"), toMap(a.externalDocs(), location, locationArgs)));
		return nullIfEmpty(om);
	}

	private OList toList(Tag[] aa, String location, Object...locationArgs) throws ParseException {
		if (aa.length == 0)
			return null;
		OList ol = new OList();
		for (Tag a : aa)
			ol.add(toMap(a, location, locationArgs));
		return nullIfEmpty(ol);
	}

	private OMap getSchema(OMap schema, Type type, BeanSession bs) throws Exception {

		if (type == Swagger.class)
			return null;

		schema = newMap(schema);

		ClassMeta<?> cm = bs.getClassMeta(type);

		if (schema.getBoolean("ignore", false))
			return null;

		if (schema.containsKey("type") || schema.containsKey("$ref"))
			return schema;

		OMap om = fixSwaggerExtensions(schema.append(js.getSchema(cm)));

		return nullIfEmpty(om);
	}

	/**
	 * Replaces non-standard JSON-Schema attributes with standard Swagger attributes.
	 */
	private OMap fixSwaggerExtensions(OMap om) {
		om
			.appendSkipNull("discriminator", om.remove("x-discriminator"))
			.appendSkipNull("readOnly", om.remove("x-readOnly"))
			.appendSkipNull("xml", om.remove("x-xml"))
			.appendSkipNull("externalDocs", om.remove("x-externalDocs"))
			.appendSkipNull("example", om.remove("x-example"));
		return nullIfEmpty(om);
	}

	private void addBodyExamples(RestOpContext sm, OMap piri, boolean response, Type type, Locale locale) throws Exception {

		String sex = piri.getString("example");

		if (sex == null) {
			OMap schema = resolveRef(piri.getMap("schema"));
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

		OMap examples = piri.getMap(examplesKey);
		if (examples == null)
			examples = new OMap();

		List<MediaType> mediaTypes = response ? sm.getSerializers().getSupportedMediaTypes() : sm.getParsers().getSupportedMediaTypes();

		for (MediaType mt : mediaTypes) {
			if (mt != MediaType.HTML) {
				Serializer s2 = sm.getSerializers().getSerializer(mt);
				if (s2 != null) {
					SerializerSessionArgs args =
						SerializerSessionArgs
							.create()
							.locale(locale)
							.mediaType(mt)
							.useWhitespace(true)
						;
					try {
						String eVal = s2.createSession(args).serializeToString(example);
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

	private void addParamExample(RestOpContext sm, OMap piri, RestParamType in, Type type) throws Exception {

		String s = piri.getString("example");

		if (isEmpty(s))
			return;

		OMap examples = piri.getMap("examples");
		if (examples == null)
			examples = new OMap();

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


	private OMap resolveRef(OMap m) {
		if (m == null)
			return null;
		if (m.containsKey("$ref") && js.getBeanDefs() != null) {
			String ref = m.getString("$ref");
			if (ref.startsWith("#/definitions/"))
				return js.getBeanDefs().get(ref.substring(14));
		}
		return m;
	}

	private OMap getOperation(OMap om, String path, String httpMethod) {
		if (! om.containsKey("paths"))
			om.put("paths", new OMap());
		om = om.getMap("paths");
		if (! om.containsKey(path))
			om.put(path, new OMap());
		om = om.getMap(path);
		if (! om.containsKey(httpMethod))
			om.put(httpMethod, new OMap());
		return om.getMap(httpMethod);
	}

	private static OMap newMap(OMap om) {
		if (om == null)
			return new OMap();
		return om.modifiable();
	}

	private OMap merge(OMap om, Body a) throws ParseException {
		if (BodyAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipEmpty("examples", parseMap(a.examples()), parseMap(a.exs()))
			.appendSkipFalse("required", a.required() || a.r())
			.appendSkipEmpty("schema", merge(om.getMap("schema"), a.schema()))
		;
	}

	private OMap merge(OMap om, Query a) throws ParseException {
		if (QueryAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipFalse("allowEmptyValue", a.allowEmptyValue() || a.aev())
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipFalse("required", a.required() || a.r())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
		;
	}

	private OMap merge(OMap om, FormData a) throws ParseException {
		if (FormDataAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipFalse("allowEmptyValue", a.allowEmptyValue() || a.aev())
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipFalse("required", a.required())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
		;
	}

	private OMap merge(OMap om, Header a) throws ParseException {
		if (HeaderAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipFalse("required", a.required() || a.r())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
		;
	}

	private OMap merge(OMap om, Path a) throws ParseException {
		if (PathAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
		;
	}

	private OMap merge(OMap om, Schema a) throws ParseException {
		if (SchemaAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("additionalProperties", toOMap(a.additionalProperties()))
			.appendSkipEmpty("allOf", joinnl(a.allOf()))
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("discriminator", a.discriminator())
			.appendSkipEmpty("description", resolve(a.description()), resolve(a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example()), resolve(a.ex()))
			.appendSkipEmpty("examples", parseMap(a.examples()), parseMap(a.exs()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("externalDocs", merge(om.getMap("externalDocs"), a.externalDocs()))
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("ignore", a.ignore() ? "true" : null)
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipMinusOne("maxProperties", a.maxProperties(), a.maxp())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipMinusOne("minProperties", a.minProperties(), a.minp())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipEmpty("properties", toOMap(a.properties()))
			.appendSkipFalse("readOnly", a.readOnly() || a.ro())
			.appendSkipFalse("required", a.required() || a.r())
			.appendSkipEmpty("title", a.title())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
			.appendSkipEmpty("xml", joinnl(a.xml()))
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private OMap merge(OMap om, ExternalDocs a) throws ParseException {
		if (ExternalDocsAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("description", resolve(a.description()))
			.appendSkipEmpty("url", a.url())
		;
	}

	private OMap merge(OMap om, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private OMap merge(OMap om, SubItems a) throws ParseException {
		if (SubItemsAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", toOMap(a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private OMap merge(OMap om, Response a) throws ParseException {
		if (ResponseAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipEmpty("examples", parseMap(a.examples()), parseMap(a.exs()))
			.appendSkipEmpty("headers", merge(om.getMap("headers"), a.headers()))
			.appendSkipEmpty("schema", merge(om.getMap("schema"), a.schema()))
		;
	}

	private OMap merge(OMap om, ResponseHeader[] a) throws ParseException {
		if (a.length == 0)
			return om;
		om = newMap(om);
		for (ResponseHeader aa : a) {
			String name = StringUtils.firstNonEmpty(aa.name(), aa.value());
			if (isEmpty(name))
				throw runtimeException("@ResponseHeader used without name or value.");
			om.getMap(name, true).putAll(merge(null, aa));
		}
		return om;
	}

	private OMap merge(OMap om, ResponseHeader a) throws ParseException {
		if (ResponseHeaderAnnotation.empty(a))
			return om;
		om = newMap(om);
		if (a.api().length > 0)
			om.putAll(parseMap(a.api()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat(), a.cf())
			.appendSkipEmpty("default", joinnl(a._default(), a.df()))
			.appendSkipEmpty("description", resolve(a.description(), a.d()))
			.appendSkipEmpty("enum", toSet(a._enum()), toSet(a.e()))
			.appendSkipEmpty("example", resolve(a.example(), a.ex()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendSkipEmpty("format", a.format(), a.f())
			.appendSkipEmpty("items", merge(om.getMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum(), a.max())
			.appendSkipMinusOne("maxItems", a.maxItems(), a.maxi())
			.appendSkipMinusOne("maxLength", a.maxLength(), a.maxl())
			.appendSkipEmpty("minimum", a.minimum(), a.min())
			.appendSkipMinusOne("minItems", a.minItems(), a.mini())
			.appendSkipMinusOne("minLength", a.minLength(), a.minl())
			.appendSkipEmpty("multipleOf", a.multipleOf(), a.mo())
			.appendSkipEmpty("pattern", a.pattern(), a.p())
			.appendSkipEmpty("type", a.type(), a.t())
			.appendSkipFalse("uniqueItems", a.uniqueItems() || a.ui())
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private OMap mergePartSchema(OMap param, OMap schema) {
		if (schema != null) {
			param
				.appendIf(false, true, true, "collectionFormat", schema.remove("collectionFormat"))
				.appendIf(false, true, true, "default", schema.remove("default"))
				.appendIf(false, true, true, "description", schema.remove("enum"))
				.appendIf(false, true, true, "enum", schema.remove("enum"))
				.appendIf(false, true, true, "example", schema.remove("example"))
				.appendIf(false, true, true, "exclusiveMaximum", schema.remove("exclusiveMaximum"))
				.appendIf(false, true, true, "exclusiveMinimum", schema.remove("exclusiveMinimum"))
				.appendIf(false, true, true, "format", schema.remove("format"))
				.appendIf(false, true, true, "items", schema.remove("items"))
				.appendIf(false, true, true, "maximum", schema.remove("maximum"))
				.appendIf(false, true, true, "maxItems", schema.remove("maxItems"))
				.appendIf(false, true, true, "maxLength", schema.remove("maxLength"))
				.appendIf(false, true, true, "minimum", schema.remove("minimum"))
				.appendIf(false, true, true, "minItems", schema.remove("minItems"))
				.appendIf(false, true, true, "minLength", schema.remove("minLength"))
				.appendIf(false, true, true, "multipleOf", schema.remove("multipleOf"))
				.appendIf(false, true, true, "pattern", schema.remove("pattern"))
				.appendIf(false, true, true, "required", schema.remove("required"))
				.appendIf(false, true, true, "type", schema.remove("type"))
				.appendIf(false, true, true, "uniqueItems", schema.remove("uniqueItems"));

			if ("object".equals(param.getString("type")) && ! schema.isEmpty())
				param.put("schema", schema);
		}

		return param;
	}



	private OMap toOMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isJsonObject(s, true))
			s = "{" + s + "}";
		s = resolve(s);
		return OMap.ofJson(s);
	}

	private Set<String> toSet(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		s = resolve(s);
		Set<String> set = ASet.of();
		for (Object o : StringUtils.parseListOrCdl(s))
			set.add(o.toString());
		return set;
	}

	static String joinnl(String[]...s) {
		for (String[] ss : s) {
			if (ss.length != 0)
			return StringUtils.joinnl(ss).trim();
		}
		return "";
	}

	private static Set<Integer> getCodes(List<Response> la, Integer def) {
		Set<Integer> codes = new TreeSet<>();
		for (Response a : la) {
			for (int i : a.value())
				codes.add(i);
			for (int i : a.code())
				codes.add(i);
		}
		if (codes.isEmpty() && def != null)
			codes.add(def);
		return codes;
	}

	private static Set<Integer> getCodes2(List<ResponseHeader> la, Integer def) {
		Set<Integer> codes = new TreeSet<>();
		for (ResponseHeader a : la) {
			for (int i : a.code())
				codes.add(i);
		}
		if (codes.isEmpty() && def != null)
			codes.add(def);
		return codes;
	}

	private static OMap nullIfEmpty(OMap m) {
		return (m == null || m.isEmpty() ? null : m);
	}

	private static OList nullIfEmpty(OList l) {
		return (l == null || l.isEmpty() ? null : l);
	}
}
