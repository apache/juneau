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

import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestParamType.*;
import static org.apache.juneau.rest.util.AnnotationUtils.*;

import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Contact;
import org.apache.juneau.http.annotation.License;
import org.apache.juneau.http.annotation.Tag;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Default implementation of {@link RestInfoProvider}.
 *
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST resources are documented.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.OptionsPages">Overview &gt; juneau-rest-server &gt; OPTIONS Pages</a>
 * </ul>
 */
public class BasicRestInfoProvider implements RestInfoProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "BasicRestInfoProvider.";

	/**
	 * Configuration property:  Ignore types from schema definitions.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BasicRestInfoProvider.ignoreTypes.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> (comma-delimited)
	 * 	<li><b>Default:</b>  <jk>null</jk>.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.
	 * 	<ja>@RestResource</ja>(
	 * 			properties={
	 * 				<ja>@Property</ja>(name=<jsf>INFOPROVIDER_ignoreTypes</jsf>, value=<js>"Swagger,*.proto.*"</js>)
	 * 			}
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 */
	public static final String INFOPROVIDER_ignoreTypes = PREFIX + "ignoreTypes.s";


	private final RestContext context;
	private final String
		siteName,
		title,
		description;
	private final Set<Pattern> ignoreTypes;
	private final ConcurrentHashMap<Locale,ConcurrentHashMap<Integer,Swagger>> swaggers = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param context The resource context.
	 */
	public BasicRestInfoProvider(RestContext context) {
		this.context = context;

		PropertyStore ps = context.getPropertyStore();
		this.ignoreTypes = new LinkedHashSet<>();
		for (String s : split(ps.getProperty(INFOPROVIDER_ignoreTypes, String.class, "")))
			ignoreTypes.add(Pattern.compile(s.replace(".", "\\.").replace("*", ".*")));

		Builder b = new Builder(context);
		this.siteName = b.siteName;
		this.title = b.title;
		this.description = b.description;
	}

	private static final class Builder {
		String
			siteName,
			title,
			description;

		Builder(RestContext context) {

			LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsParentFirst = findAnnotationsMapParentFirst(RestResource.class, context.getResource().getClass());

			for (RestResource r : restResourceAnnotationsParentFirst.values()) {
				if (! r.siteName().isEmpty())
					siteName = r.siteName();
				if (r.title().length > 0)
					title = joinnl(r.title());
				if (r.description().length > 0)
					description = joinnl(r.description());
			}
		}
	}

	/**
	 * Returns the localized swagger for this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to customize the Swagger.
	 *
	 * @param req The incoming HTTP request.
	 * @return
	 * 	A new Swagger instance.
	 * 	<br>Never <jk>null</jk>.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public Swagger getSwagger(RestRequest req) throws Exception {

		Locale locale = req.getLocale();

		// Find it in the cache.
		// Swaggers are cached by user locale and an int hash of the @RestMethods they have access to.
		HashCode userHash = HashCode.create();
		for (RestJavaMethod sm : context.getCallMethods().values())
			if (sm.isRequestAllowed(req))
				userHash.add(sm.hashCode());
		int hashCode = userHash.get();

		if (! swaggers.containsKey(locale))
			swaggers.putIfAbsent(locale, new ConcurrentHashMap<Integer,Swagger>());

		Swagger swagger = swaggers.get(locale).get(hashCode);
		if (swagger != null)
			return swagger;

		// Wasn't cached...need to create one.

		Object resource = context.getResource();
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		MessageBundle mb = context.getMessages();
		Class<?> c = context.getResource().getClass();
		JsonSchemaSerializerSession js = req.getContext().getJsonSchemaSerializer().createSession();

		// Load swagger JSON from classpath.
		ObjectMap omSwagger = context.getClasspathResource(ObjectMap.class, MediaType.JSON, ClassUtils.getSimpleName(resource.getClass()) + ".json", locale);
		if (omSwagger == null)
			omSwagger = context.getClasspathResource(ObjectMap.class, MediaType.JSON, resource.getClass().getSimpleName() + ".json", locale);
		if (omSwagger == null)
			omSwagger = new ObjectMap();

		// Combine it with @RestResource(swagger)
		for (Map.Entry<Class<?>,RestResource> e : findAnnotationsMapParentFirst(RestResource.class, resource.getClass()).entrySet()) {
			RestResource rr = e.getValue();

			ObjectMap sInfo = omSwagger.getObjectMap("info", true);
			sInfo.appendSkipEmpty("title",
				firstNonEmpty(
					sInfo.getString("title"),
					resolve(vr, rr.title())
				)
			);
			sInfo.appendSkipEmpty("description",
				firstNonEmpty(
					sInfo.getString("description"),
					resolve(vr, rr.description())
				)
			);

			ResourceSwagger r = rr.swagger();

			omSwagger.appendAll(parseMap(vr, r.value(), "@ResourceSwagger(value) on class {0}", c));

			if (! empty(r)) {
				ObjectMap info = omSwagger.getObjectMap("info", true);
				info.appendSkipEmpty("title", resolve(vr, r.title()));
				info.appendSkipEmpty("description", resolve(vr, r.description()));
				info.appendSkipEmpty("version", resolve(vr, r.version()));
				info.appendSkipEmpty("termsOfService", resolve(vr, r.termsOfService()));
				info.appendSkipEmpty("contact",
					merge(
						info.getObjectMap("contact"),
						toMap(vr, r.contact(), "@ResourceSwagger(contact) on class {0}", c)
					)
				);
				info.appendSkipEmpty("license",
					merge(
						info.getObjectMap("license"),
						toMap(vr, r.license(), "@ResourceSwagger(license) on class {0}", c)
					)
				);
			}

			omSwagger.appendSkipEmpty("externalDocs",
				merge(
					omSwagger.getObjectMap("externalDocs"),
					toMap(vr, r.externalDocs(), "@ResourceSwagger(externalDocs) on class {0}", c)
				)
			);
			omSwagger.appendSkipEmpty("tags",
				merge(
					omSwagger.getObjectList("tags"),
					toList(vr, r.tags(), "@ResourceSwagger(tags) on class {0}", c)
				)
			);
		}

		omSwagger.appendSkipEmpty("externalDocs", parseMap(vr, mb.findFirstString(locale, "externalDocs"), "Messages/externalDocs on class {0}", c));

		ObjectMap info = omSwagger.getObjectMap("info", true);
		info.appendSkipEmpty("title", resolve(vr, mb.findFirstString(locale, "title")));
		info.appendSkipEmpty("description", resolve(vr, mb.findFirstString(locale, "description")));
		info.appendSkipEmpty("version", resolve(vr, mb.findFirstString(locale, "version")));
		info.appendSkipEmpty("termsOfService", resolve(vr, mb.findFirstString(locale, "termsOfService")));
		info.appendSkipEmpty("contact", parseMap(vr, mb.findFirstString(locale, "contact"), "Messages/contact on class {0}", c));
		info.appendSkipEmpty("license", parseMap(vr, mb.findFirstString(locale, "license"), "Messages/license on class {0}", c));
		if (info.isEmpty())
			omSwagger.remove("info");

		ObjectList
			produces = omSwagger.getObjectList("produces", true),
			consumes = omSwagger.getObjectList("consumes", true);
		if (consumes.isEmpty())
			consumes.addAll(req.getContext().getConsumes());
		if (produces.isEmpty())
			produces.addAll(req.getContext().getProduces());

		Map<String,ObjectMap> tagMap = new LinkedHashMap<>();
		if (omSwagger.containsKey("tags")) {
			for (ObjectMap om : omSwagger.getObjectList("tags").elements(ObjectMap.class)) {
				String name = om.getString("name");
				if (name == null)
					throw new SwaggerException(null, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}

		String s = mb.findFirstString(locale, "tags");
		if (s != null) {
			for (ObjectMap m : parseListOrCdl(vr, s, "Messages/tags on class {0}", c).elements(ObjectMap.class)) {
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
		ObjectMap definitions = omSwagger.getObjectMap("definitions", true);
		for (String defId : definitions.keySet())
			js.addBeanDef(defId, definitions.getObjectMap(defId));

		// Iterate through all the @RestMethod methods.
		for (RestJavaMethod sm : context.getCallMethods().values()) {

			// Skip it if user doesn't have access.
			if (! sm.isRequestAllowed(req))
				continue;

			Method m = sm.method;
			RestMethod rm = m.getAnnotation(RestMethod.class);
			String mn = m.getName();

			// Get the operation from the existing swagger so far.
			ObjectMap op = getOperation(omSwagger, sm.getPathPattern(), sm.getHttpMethod().toLowerCase());

			// Add @RestMethod(swagger)
			MethodSwagger ms = rm.swagger();

			op.appendAll(parseMap(vr, ms.value(), "@MethodSwagger(value) on class {0} method {1}", c, m));
			op.appendSkipEmpty("operationId",
				firstNonEmpty(
					resolve(vr, ms.operationId()),
					op.getString("operationId"),
					mn
				)
			);
			op.appendSkipEmpty("summary",
				firstNonEmpty(
					resolve(vr, ms.summary()),
					resolve(vr, mb.findFirstString(locale, mn + ".summary")),
					op.getString("summary"),
					resolve(vr, rm.summary())
				)
			);
			op.appendSkipEmpty("description",
				firstNonEmpty(
					resolve(vr, ms.description()),
					resolve(vr, mb.findFirstString(locale, mn + ".description")),
					op.getString("description"),
					resolve(vr, rm.description())
				)
			);
			op.appendSkipEmpty("deprecated",
				firstNonEmpty(
					resolve(vr, ms.deprecated()),
					(m.getAnnotation(Deprecated.class) != null || m.getDeclaringClass().getAnnotation(Deprecated.class) != null) ? "true" : null
				)
			);
			op.appendSkipEmpty("tags",
				merge(
					parseListOrCdl(vr, mb.findFirstString(locale, mn + ".tags"), "Messages/tags on class {0} method {1}", c, m),
					parseListOrCdl(vr, ms.tags(), "@MethodSwagger(tags) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("schemes",
				merge(
					parseListOrCdl(vr, mb.findFirstString(locale, mn + ".schemes"), "Messages/schemes on class {0} method {1}", c, m),
					parseListOrCdl(vr, ms.schemes(), "@MethodSwagger(schemes) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("consumes",
				firstNonEmpty(
					parseListOrCdl(vr, mb.findFirstString(locale, mn + ".consumes"), "Messages/consumes on class {0} method {1}", c, m),
					parseListOrCdl(vr, ms.consumes(), "@MethodSwagger(consumes) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("produces",
				firstNonEmpty(
					parseListOrCdl(vr, mb.findFirstString(locale, mn + ".produces"), "Messages/produces on class {0} method {1}", c, m),
					parseListOrCdl(vr, ms.produces(), "@MethodSwagger(produces) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("parameters",
				merge(
					parseList(vr, mb.findFirstString(locale, mn + ".parameters"), "Messages/parameters on class {0} method {1}", c, m),
					parseList(vr, ms.parameters(), "@MethodSwagger(parameters) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("responses",
				merge(
					parseMap(vr, mb.findFirstString(locale, mn + ".responses"), "Messages/responses on class {0} method {1}", c, m),
					parseMap(vr, ms.responses(), "@MethodSwagger(responses) on class {0} method {1}", c, m)
				)
			);
			op.appendSkipEmpty("externalDocs",
				merge(
					op.getObjectMap("externalDocs"),
					parseMap(vr, mb.findFirstString(locale, mn + ".externalDocs"), "Messages/externalDocs on class {0} method {1}", c, m),
					toMap(vr, ms.externalDocs(), "@MethodSwagger(externalDocs) on class {0} method {1}", c, m)
				)
			);

			if (op.containsKey("tags"))
				for (String tag : op.getObjectList("tags").elements(String.class))
					if (! tagMap.containsKey(tag))
						tagMap.put(tag, new ObjectMap().append("name", tag));

			ObjectMap paramMap = new ObjectMap();
			if (op.containsKey("parameters"))
				for (ObjectMap param : op.getObjectList("parameters").elements(ObjectMap.class))
					paramMap.put(param.getString("in") + '.' + ("body".equals(param.getString("in")) ? "body" : param.getString("name")), param);

			// Finally, look for parameters defined on method.
			for (RestMethodParam mp : context.getRestMethodParams(m)) {

				RestParamType in = mp.getParamType();

				if (in == OTHER || in == RESPONSE || in == RESPONSE_HEADER || in == RESPONSE_STATUS)
					continue;

				String key = in.toString() + '.' + (in == BODY ? "body" : mp.getName());

				ObjectMap param = paramMap.getObjectMap(key, true);

				param.append("in", in);

				if (in != BODY)
					param.append("name", mp.name);

				ObjectMap pi = resolve(vr, mp.getApi(), "ParameterInfo on class {0} method {1}", c, m);

				// Common to all
				param.appendSkipEmpty("description", resolve(vr, pi.getString("description")));
				param.appendSkipEmpty("required", resolve(vr, pi.getString("required")));

				if (in == BODY) {
					param.put("schema", getSchema(req, param.getObjectMap("schema", true), js, mp.getType()));
					param.appendSkipEmpty("schema", parseMap(vr, pi.get("schema"), "ParameterInfo/schema on class {0} method {1}", c, m));
					param.appendSkipEmpty("x-example", parseAnything(vr, pi.getString("example"), "ParameterInfo/example on class {0} method {1}", c, m));
					param.appendSkipEmpty("x-examples", parseMap(vr, pi.get("examples"), "ParameterInfo/examples on class {0} method {1}", c, m));
				} else {
					param.appendSkipEmpty("type", resolve(vr, pi.getString("type")));
					param.appendSkipEmpty("format", resolve(vr, pi.getString("format")));
					param.appendSkipEmpty("pattern", resolve(vr, pi.getString("pattern")));
					param.appendSkipEmpty("collectionFormat", resolve(vr, pi.getString("collectionFormat")));
					param.appendSkipEmpty("maximum", resolve(vr, pi.getString("maximum")));
					param.appendSkipEmpty("minimum", resolve(vr, pi.getString("minimum")));
					param.appendSkipEmpty("multipleOf", resolve(vr, pi.getString("multipleOf")));
					param.appendSkipEmpty("maxLength", resolve(vr, pi.getString("maxLength")));
					param.appendSkipEmpty("minLength", resolve(vr, pi.getString("minLength")));
					param.appendSkipEmpty("maxItems", resolve(vr, pi.getString("maxItems")));
					param.appendSkipEmpty("minItems", resolve(vr, pi.getString("minItems")));
					param.appendSkipEmpty("allowEmptyValue", resolve(vr, pi.getString("allowEmptyValue")));
					param.appendSkipEmpty("exclusiveMaximum", resolve(vr, pi.getString("exclusiveMaximum")));
					param.appendSkipEmpty("exclusiveMinimum", resolve(vr, pi.getString("exclusiveMinimum")));
					param.appendSkipEmpty("uniqueItems", resolve(vr, pi.getString("uniqueItems")));
					param.appendSkipEmpty("schema", parseMap(vr, pi.get("schema"), "ParameterInfo/schema on class {0} method {1}", c, m));
					param.appendSkipEmpty("default", parseAnything(vr, pi.getString("default"), "ParameterInfo/default on class {0} method {1}", c, m));
					param.appendSkipEmpty("enum", parseListOrCdl(vr, pi.getString("enum"), "ParameterInfo/enum on class {0} method {1}", c, m));
					param.appendSkipEmpty("x-example", parseAnything(vr, pi.getString("example"), "ParameterInfo/example on class {0} method {1}", c, m));
					param.appendSkipEmpty("x-examples", parseMap(vr, pi.get("examples"), "ParameterInfo/examples on class {0} method {1}", c, m));
					param.appendSkipEmpty("items", parseMap(vr, pi.get("items"), "ParameterInfo/items on class {0} method {1}", c, m));

					// Technically Swagger doesn't support schema on non-body parameters, but we do.
					param.appendSkipEmpty("schema", getSchema(req, param.getObjectMap("schema", true), js, mp.getType()));
				}

				if ((in == BODY || in == PATH) && ! param.containsKeyNotEmpty("required"))
					param.put("required", true);

				addXExamples(req, sm, param, in.toString(), js, mp.getType());
			}

			if (! paramMap.isEmpty())
				op.put("parameters", paramMap.values());

			ObjectMap responses = op.getObjectMap("responses", true);

			// Gather responses from @Response-annotated exceptions.
			for (RestMethodThrown rt : context.getRestMethodThrowns(m)) {
				ObjectMap md = resolve(vr, rt.getApi(), "RestMethodThrown on class {0} method {1}", c, m);
				for (String code : md.keySet()) {
					ObjectMap response = md.getObjectMap(code);
					ObjectMap om = responses.getObjectMap(code, true);
					om.appendSkipEmpty("description", resolve(vr, response.getString("description")));
					om.appendSkipEmpty("x-example", parseAnything(vr, response.getString("example"), "RestMethodThrown/example on class {0} method {1}", c, m));
					om.appendSkipEmpty("examples", parseMap(vr, response.get("examples"), "RestMethodThrown/examples on class {0} method {1}", c, m));
					om.appendSkipEmpty("schema", parseMap(vr, response.get("schema"), "RestMethodThrown/schema on class {0} method {1}", c, m));
					om.appendSkipEmpty("headers", parseMap(vr, response.get("headers"), "RestMethodThrown/headers on class {0} method {1}", c, m));
				}
			}

			RestMethodReturn r = context.getRestMethodReturn(m);
			String rStatus = r.getCode() == 0 ? "200" : String.valueOf(r.getCode());

			ObjectMap rom = responses.getObjectMap(rStatus, true);

			if (r.getType() != void.class) {
				ObjectMap rmd = resolve(vr, r.getApi(), "RestMethodReturn on class {0} method {1}", c, m);
				rom.appendSkipEmpty("description", resolve(vr, rmd.getString("description")));
				rom.appendSkipEmpty("x-example", parseAnything(vr, rmd.getString("example"), "RestMethodReturn/example on class {0} method {1}", c, m));
				rom.appendSkipEmpty("examples", parseMap(vr, rmd.get("examples"), "RestMethodReturn/examples on class {0} method {1}", c, m));
				rom.appendSkipEmpty("schema", parseMap(vr, rmd.get("schema"), "RestMethodReturn/schema on class {0} method {1}", c, m));
				rom.appendSkipEmpty("headers", parseMap(vr, rmd.get("headers"), "RestMethodReturn/headers on class {0} method {1}", c, m));
				rom.appendSkipEmpty("schema", getSchema(req, rom.getObjectMap("schema", true), js, m.getGenericReturnType()));
				addXExamples(req, sm, rom, "ok", js, m.getGenericReturnType());
			}

			// Finally, look for @ResponseHeader parameters defined on method.
			for (RestMethodParam mp : context.getRestMethodParams(m)) {

				RestParamType in = mp.getParamType();

				if (in == RESPONSE_HEADER) {
					ObjectMap pi = resolve(vr, mp.getApi(), "@ResponseHeader on class {0} method {1}", c, m);
					for (String code : pi.keySet()) {
						String name = mp.getName();
						ObjectMap pi2 = pi.getObjectMap(code, true);

						ObjectMap header = responses.getObjectMap(code, true).getObjectMap("headers", true).getObjectMap(name, true);

						header.appendSkipEmpty("description", resolve(vr, pi2.getString("description")));
						header.appendSkipEmpty("type", resolve(vr, pi2.getString("type")));
						header.appendSkipEmpty("format", resolve(vr, pi2.getString("format")));
						header.appendSkipEmpty("collectionFormat", resolve(vr, pi2.getString("collectionFormat")));
						header.appendSkipEmpty("maximum", resolve(vr, pi2.getString("maximum")));
						header.appendSkipEmpty("minimum", resolve(vr, pi2.getString("minimum")));
						header.appendSkipEmpty("multipleOf", resolve(vr, pi2.getString("multipleOf")));
						header.appendSkipEmpty("maxLength", resolve(vr, pi2.getString("maxLength")));
						header.appendSkipEmpty("minLength", resolve(vr, pi2.getString("minLength")));
						header.appendSkipEmpty("maxItems", resolve(vr, pi2.getString("maxItems")));
						header.appendSkipEmpty("minItems", resolve(vr, pi2.getString("minItems")));
						header.appendSkipEmpty("exclusiveMaximum", resolve(vr, pi2.getString("exclusiveMaximum")));
						header.appendSkipEmpty("exclusiveMinimum", resolve(vr, pi2.getString("exclusiveMinimum")));
						header.appendSkipEmpty("uniqueItems", resolve(vr, pi2.getString("uniqueItems")));
						header.appendSkipEmpty("default", parseAnything(vr, pi2.getString("default"), "@ResponseHeader/default on class {0} method {1}", c, m));
						header.appendSkipEmpty("enum", parseListOrCdl(vr, pi2.getString("enum"), "@ResponseHeader/enum on class {0} method {1}", c, m));
						header.appendSkipEmpty("x-example", parseAnything(vr, pi2.getString("example"), "@ResponseHeader/example on class {0} method {1}", c, m));
						header.appendSkipEmpty("examples", parseMap(vr, pi2.get("examples"), "@ResponseHeader/examples on class {0} method {1}", c, m));
						header.appendSkipEmpty("items", parseMap(vr, pi2.get("items"), "@ResponseHeader/items on class {0} method {1}", c, m));
					}

				} else if (in == RESPONSE) {
					ObjectMap pi = resolve(vr, mp.getApi(), "@Response on class {0} method {1}", c, m);
					for (String code : pi.keySet()) {
						ObjectMap pi2 = pi.getObjectMap(code, true);

						ObjectMap response = responses.getObjectMap(code, true);

						response.appendSkipEmpty("description", resolve(vr, pi2.getString("description")));
						response.appendSkipEmpty("schema", parseMap(vr, pi2.get("schema"), "@Response/schema on class {0} method {1}", c, m));
						response.appendSkipEmpty("headers", parseMap(vr, pi2.get("headers"), "@Response/headers on class {0} method {1}", c, m));
						response.appendSkipEmpty("x-example", parseAnything(vr, pi2.getString("example"), "@Response/example on class {0} method {1}", c, m));
						response.appendSkipEmpty("examples", parseMap(vr, pi2.get("examples"), "@Response/examples on class {0} method {1}", c, m));

						Type type = mp.getType();
						if (type instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType)type;
							if (pt.getRawType().equals(Value.class))
								type = pt.getActualTypeArguments()[0];
						}

						response.appendSkipEmpty("schema", getSchema(req, response.getObjectMap("schema", true), js, type));
					}

				} else if (in == RESPONSE_STATUS) {
					ObjectMap pi = resolve(vr, mp.getApi(), "@ResponseStatus on class {0} method {1}", c, m);
					for (String code : pi.keySet()) {
						ObjectMap pi2 = pi.getObjectMap(code, true);

						ObjectMap response = responses.getObjectMap(code, true);

						response.appendSkipEmpty("description", resolve(vr, pi2.getString("description")));
					}
				}
			}

			// Add default response descriptions.
			for (Map.Entry<String,Object> e : responses.entrySet()) {
				String key = e.getKey();
				ObjectMap val = responses.getObjectMap(key);
				if (StringUtils.isDecimal(key))
					val.appendIf(false, true, true, "description", RestUtils.getHttpResponseText(Integer.parseInt(key)));
			}

			if (responses.isEmpty())
				op.remove("responses");
			else
				op.put("responses", new TreeMap<>(responses));

			if (! op.containsKey("consumes")) {
				List<MediaType> mConsumes = sm.supportedContentTypes;
				if (! mConsumes.equals(consumes))
					op.put("consumes", mConsumes);
			}

			if (! op.containsKey("produces")) {
				List<MediaType> mProduces = sm.supportedAcceptTypes;
				if (! mProduces.equals(produces))
					op.put("produces", mProduces);
			}
		}

		if (js.getBeanDefs() != null)
			for (Map.Entry<String,ObjectMap> e : js.getBeanDefs().entrySet())
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
//			System.err.println(omSwagger.toString(JsonSerializer.DEFAULT_LAX_READABLE));
//			throw e1;
//		}

		try {
			String swaggerJson = omSwagger.toString(JsonSerializer.DEFAULT_LAX_READABLE);
			swagger = jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new RestServletException("Error detected in swagger.").initCause(e);
		}

		swaggers.get(locale).put(hashCode, swagger);

		return swagger;
	}

//	private static void assertNoEmpties(ObjectMap om) throws SwaggerException {
//		if (om.isEmpty())
//			throw new SwaggerException(null, "Empty map detected.");
//		for (Map.Entry<String,Object> e : om.entrySet()) {
//			Object val = e.getValue();
//			if (val instanceof ObjectMap)
//				assertNoEmpties((ObjectMap)val);
//			if (val instanceof ObjectList)
//				assertNoEmpties((ObjectList)val);
//		}
//	}
//
//	private static void assertNoEmpties(ObjectList ol) throws SwaggerException {
//		if (ol.isEmpty())
//			throw new SwaggerException(null, "Empty list detected.");
//		for (Object val : ol) {
//			if (val instanceof ObjectMap)
//				assertNoEmpties((ObjectMap)val);
//			if (val instanceof ObjectList)
//				assertNoEmpties((ObjectList)val);
//		}
//	}

	//=================================================================================================================
	// Utility methods
	//=================================================================================================================

	private ObjectMap resolve(VarResolverSession vs, ObjectMap om, String location, Object...args) throws ParseException {
		if (om == null)
			return om;
		try {
			return resolve(vs, om.modifiable());
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private ObjectMap resolve(VarResolverSession vs, ObjectMap om) throws ParseException {
		ObjectMap om2 = om.containsKey("_value") ? parseMap(vs, om.remove("_value")) : new ObjectMap();
		for (Map.Entry<String,Object> e : om.entrySet()) {
			Object val = e.getValue();
			if (val instanceof ObjectMap) {
				val = resolve(vs, (ObjectMap)val);
			} else if (val instanceof ObjectList) {
				val = resolve(vs, (ObjectList) val);
			} else if (val instanceof String) {
				val = vs.resolve(val.toString().trim());
			}
			om2.put(e.getKey(), val);
		}
		return om2;
	}

	private ObjectList resolve(VarResolverSession vs, ObjectList om) throws ParseException {
		ObjectList ol2 = new ObjectList();
		for (Object val : om) {
			if (val instanceof ObjectMap) {
				val = resolve(vs, (ObjectMap)val);
			} else if (val instanceof ObjectList) {
				val = resolve(vs, (ObjectList) val);
			} else if (val instanceof String) {
				val = vs.resolve(val.toString().trim());
			}
			ol2.add(val);
		}
		return ol2;
	}

	private String resolve(VarResolverSession vs, String[] s) {
		return resolve(vs, joinnl(s));
	}

	private String resolve(VarResolverSession vs, String s) {
		return vs.resolve(s);
	}

	private ObjectMap parseMap(VarResolverSession vs, Object o, String location, Object...args) throws ParseException {
		try {
			return parseMap(vs, o);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON object encountered in " + location + ".", args);
		}
	}

	private ObjectMap parseMap(VarResolverSession vs, Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[])
			o = joinnl((String[])o);
		if (o instanceof String) {
			String s = o.toString();
			if (s.isEmpty())
				return null;
			s = vs.resolve(s.trim());
			if ("IGNORE".equalsIgnoreCase(s))
				return new ObjectMap().append("ignore", true);
			if (! isObjectMap(s, true))
				s = "{" + s + "}";
			return new ObjectMap(s);
		}
		if (o instanceof ObjectMap)
			return (ObjectMap)o;
		throw new SwaggerException(null, "Unexpected data type ''{0}''.  Expected ObjectMap or String.", o.getClass().getName());
	}

	private ObjectList parseList(VarResolverSession vs, Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = vs.resolve(s.trim());
			if (! isObjectList(s, true))
				s = "[" + s + "]";
			return new ObjectList(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private Object parseAnything(VarResolverSession vs, Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			if (o instanceof String[])
				o = joinnl((String[])o);
			String s = o.toString();
			if (s.isEmpty())
				return null;
			return RestUtils.parseAnything(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON encountered in "+location+".", locationArgs);
		}
	}

	private ObjectList parseListOrCdl(VarResolverSession vs, Object o, String location, Object...locationArgs) throws ParseException {
		try {
			if (o == null)
				return null;
			String s = (o instanceof String[] ? joinnl((String[])o) : o.toString());
			if (s.isEmpty())
				return null;
			s = vs.resolve(s.trim());
			return StringUtils.parseListOrCdl(s);
		} catch (ParseException e) {
			throw new SwaggerException(e, "Malformed swagger JSON array encountered in "+location+".", locationArgs);
		}
	}

	private ObjectMap newMap(VarResolverSession vs, ObjectMap om, Object[] value, String location, Object...locationArgs) throws ParseException {
		if (value.length == 0)
			return om == null ? new ObjectMap() : om;
		ObjectMap om2 = parseMap(vs, joinnl(value), location, locationArgs);
		if (om == null)
			return om2;
		return om.appendAll(om2);
	}

	private ObjectMap merge(ObjectMap...maps) {
		ObjectMap m = maps[0];
		for (int i = 1; i < maps.length; i++) {
			if (maps[i] != null) {
				if (m == null)
					m = new ObjectMap();
				m.putAll(maps[i]);
			}
		}
		return m;
	}

	private ObjectList merge(ObjectList...lists) {
		ObjectList l = lists[0];
		for (int i = 1; i < lists.length; i++) {
			if (lists[i] != null) {
				if (l == null)
					l = new ObjectList();
				l.addAll(lists[i]);
			}
		}
		return l;
	}

	@SafeVarargs
	private final <T> T firstNonEmpty(T...t) {
		return ObjectUtils.firstNonEmpty(t);
	}

	private ObjectMap toMap(VarResolverSession vs, ExternalDocs a, String location, Object...locationArgs) throws ParseException {
		if (empty(a))
			return null;
		ObjectMap om = newMap(vs, new ObjectMap(), a.value(), location, locationArgs);
		om.appendSkipEmpty("description", vs.resolve(joinnl(a.description())));
		om.appendSkipEmpty("url", vs.resolve(a.url()));
		return om.isEmpty() ? null : om;
	}

	private ObjectMap toMap(VarResolverSession vs, Contact a, String location, Object...locationArgs) throws ParseException {
		if (empty(a))
			return null;
		ObjectMap om = newMap(vs, new ObjectMap(), a.value(), location, locationArgs);
		om.appendSkipEmpty("name", vs.resolve(a.name()));
		om.appendSkipEmpty("url", vs.resolve(a.url()));
		om.appendSkipEmpty("email", vs.resolve(a.email()));
		return om.isEmpty() ? null : om;
	}

	private ObjectMap toMap(VarResolverSession vs, License a, String location, Object...locationArgs) throws ParseException {
		if (empty(a))
			return null;
		ObjectMap om = newMap(vs, new ObjectMap(), a.value(), location, locationArgs);
		om.appendSkipEmpty("name", vs.resolve(a.name()));
		om.appendSkipEmpty("url", vs.resolve(a.url()));
		return om.isEmpty() ? null : om;
	}

	private ObjectMap toMap(VarResolverSession vs, Tag a, String location, Object...locationArgs) throws ParseException {
		ObjectMap om = newMap(vs, new ObjectMap(), a.api(), location, locationArgs);
		om.appendSkipEmpty("name", vs.resolve(firstNonEmpty(a.name(), a.value())));
		om.appendSkipEmpty("description", vs.resolve(joinnl(a.description())));
		om.appendSkipNull("externalDocs", merge(om.getObjectMap("externalDocs"), toMap(vs, a.externalDocs(), location, locationArgs)));
		return om.isEmpty() ? null : om;
	}

	private ObjectList toList(VarResolverSession vs, Tag[] aa, String location, Object...locationArgs) throws ParseException {
		if (aa.length == 0)
			return null;
		ObjectList ol = new ObjectList();
		for (Tag a : aa)
			ol.add(toMap(vs, a, location, locationArgs));
		return ol.isEmpty() ? null : ol;
	}

	private ObjectMap getSchema(RestRequest req, ObjectMap schema, JsonSchemaSerializerSession js, Type type) throws Exception {
		BeanSession bs = req.getBeanSession();
		if (bs == null)
			bs = BeanContext.DEFAULT.createBeanSession();

		ClassMeta<?> cm = bs.getClassMeta(type);

		if (schema.getBoolean("ignore", false))
			return null;

		if (schema.containsKey("type") || schema.containsKey("$ref"))
			return schema;

		for (Pattern p : ignoreTypes)
			if (p.matcher(cm.getSimpleName()).matches() || p.matcher(cm.getName()).matches())
				return null;

		return fixSwaggerExtensions(schema.appendAll(js.getSchema(cm)));
	}

	/**
	 * Replaces non-standard JSON-Schema attributes with standard Swagger attributes.
	 */
	private ObjectMap fixSwaggerExtensions(ObjectMap om) {
		om.appendSkipNull("discriminator", om.remove("x-discriminator"));
		om.appendSkipNull("readOnly", om.remove("x-readOnly"));
		om.appendSkipNull("xml", om.remove("x-xml"));
		om.appendSkipNull("externalDocs", om.remove("x-externalDocs"));
		om.appendSkipNull("example", om.remove("x-example"));
		return om;
	}

	private void addXExamples(RestRequest req, RestJavaMethod sm, ObjectMap piri, String in, JsonSchemaSerializerSession js, Type type) throws Exception {

		Object example = piri.get("x-example");

		if (example == null) {
			ObjectMap schema = resolve(js, piri.getObjectMap("schema"));
			if (schema != null)
				example = schema.getWithDefault("example", schema.get("x-example"));
		}

		if (example == null)
			return;

		boolean isOk = "ok".equals(in), isBody = "body".equals(in);

		String sex = example.toString();
		if (isJson(sex)) {
			example = JsonParser.DEFAULT.parse(JsonSerializer.DEFAULT.serialize(example), type);
		} else {
			ClassMeta<?> cm = js.getClassMeta(type);
			if (cm.hasStringTransform()) {
				example = cm.getStringTransform().transform(sex);
			}
		}

		String examplesKey = isOk ? "examples" : "x-examples";  // Parameters don't have an examples attribute.

		ObjectMap examples = piri.getObjectMap(examplesKey);
		if (examples == null)
			examples = new ObjectMap();

		if (isOk || isBody) {
			List<MediaType> mediaTypes = isOk ? sm.getSerializers().getSupportedMediaTypes() : sm.getParsers().getSupportedMediaTypes();

			for (MediaType mt : mediaTypes) {
				if (mt != MediaType.HTML) {
					Serializer s2 = sm.getSerializers().getSerializer(mt);
					if (s2 != null) {
						SerializerSessionArgs args = new SerializerSessionArgs(null, req.getJavaMethod(), req.getLocale(), null, mt, req.isDebug() ? true : null, req.getUriContext(), true);
						try {
							String eVal = s2.createSession(args).serializeToString(example);
							examples.put(s2.getPrimaryMediaType().toString(), eVal);
						} catch (Exception e) {
							System.err.println("Could not serialize to media type ["+mt+"]: " + e.getLocalizedMessage());
						}
					}
				}
			}
		} else {
			String paramName = piri.getString("name");
			String s = sm.partSerializer.createSession(req.getSerializerSessionArgs()).serialize(HttpPartType.valueOf(in.toUpperCase()), null, example);
			if ("query".equals(in))
				s = "?" + urlEncodeLax(paramName) + "=" + urlEncodeLax(s);
			else if ("formData".equals(in))
				s = paramName + "=" + s;
			else if ("header".equals(in))
				s = paramName + ": " + s;
			else if ("path".equals(in))
				s = sm.getPathPattern().replace("{"+paramName+"}", urlEncodeLax(s));
 			examples.put("example", s);
		}

		if (! examples.isEmpty())
			piri.put(examplesKey, examples);
	}

	private ObjectMap resolve(JsonSchemaSerializerSession js, ObjectMap m) {
		if (m == null)
			return null;
		if (m.containsKey("$ref") && js.getBeanDefs() != null) {
			String ref = m.getString("$ref");
			if (ref.startsWith("#/definitions/"))
				return js.getBeanDefs().get(ref.substring(14));
		}
		return m;
	}

	private static class SwaggerException extends ParseException {
		private static final long serialVersionUID = 1L;

		SwaggerException(Exception e, String location, Object...locationArgs) {
			super(e, "Swagger exception:  at " + format(location, locationArgs));
		}
	}

	private ObjectMap getOperation(ObjectMap om, String path, String httpMethod) {
		if (! om.containsKey("paths"))
			om.put("paths", new ObjectMap());
		om = om.getObjectMap("paths");
		if (! om.containsKey(path))
			om.put(path, new ObjectMap());
		om = om.getObjectMap(path);
		if (! om.containsKey(httpMethod))
			om.put(httpMethod, new ObjectMap());
		return om.getObjectMap(httpMethod);
	}

	/**
	 * Returns the localized summary of the specified java method on this servlet.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own summary.
	 *
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestMethod#summary() @RestMethod.summary()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(summary=<js>"Summary of my method"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(summary=<js>"$L{myLocalizedSummary}"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].summary</ck>
	 * 			<li><ck>[javaMethodName].summary</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestMethod#summary() @RestMethod.summary()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.myMethod.summary</ck> = <cv>Summary of my method.</cv>
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.summary</ck> = <cv>$C{MyStrings/MyClass.myMethod.summary}</cv>
	 * 		</p>
	 * </ol>
	 *
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized summary of the method, or <jk>null</jk> if none was found.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public String getMethodSummary(Method method, RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();

		String s = method.getAnnotation(RestMethod.class).summary();
		if (s.isEmpty()) {
			Operation o = getSwaggerOperation(method, req);
			if (o != null)
				s = o.getSummary();
		}

		return isEmpty(s) ? null : vr.resolve(s);
	}

	/**
	 * Returns the localized description of the specified java method on this servlet.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own description.
	 *
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestMethod#description() @RestMethod.description()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(description=<js>"Description of my method"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(description=<js>"$L{myLocalizedDescription}"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].description</ck>
	 * 			<li><ck>[javaMethodName].description</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestMethod#description() @RestMethod.description()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.myMethod.description</ck> = <cv>Description of my method.</cv>
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.description</ck> = <cv>$C{MyStrings/MyClass.myMethod.description}</cv>
	 * 		</p>
	 * </ol>
	 *
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized description of the method, or <jk>null</jk> if none was found.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public String getMethodDescription(Method method, RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();

		String s = joinnl(method.getAnnotation(RestMethod.class).description());
		if (s.isEmpty()) {
			Operation o = getSwaggerOperation(method, req);
			if (o != null)
				s = o.getDescription();
		}

		return isEmpty(s) ? null : vr.resolve(s);
	}

	/**
	 * Returns the localized site name of this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own site name.
	 *
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestResource#siteName() @RestResource.siteName()} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(siteName=<js>"My Site"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(siteName=<js>"$L{myLocalizedSiteName}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].siteName</ck>
	 * 			<li><ck>siteName</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#siteName() @RestResource.siteName()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.siteName</ck> = <cv>My Site</cv>
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.siteName</ck> = <cv>$C{MyStrings/MyClass.siteName}</cv>
	 * 		</p>
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized site name of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public String getSiteName(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		if (siteName != null)
			return vr.resolve(siteName);
		String siteName = context.getMessages().findFirstString(req.getLocale(), "siteName");
		if (siteName != null)
			return vr.resolve(siteName);
		return null;
	}

	/**
	 * Returns the localized title of this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own title.
	 *
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestResource#title() @RestResource.siteName()} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(title=<js>"My Resource"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(title=<js>"$L{myLocalizedTitle}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].title</ck>
	 * 			<li><ck>title</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#title() @RestResource.title()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.title</ck> = <cv>My Resource</cv>
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.title</ck> = <cv>$C{MyStrings/MyClass.title}</cv>
	 * 		</p>
	 * 	<li><ck>/info/title</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized title of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public String getTitle(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		if (title != null)
			return vr.resolve(title);
		String title = context.getMessages().findFirstString(req.getLocale(), "title");
		if (title != null)
			return vr.resolve(title);
		Swagger s = getSwagger(req);
		if (s != null && s.getInfo() != null && s.getInfo().hasTitle())
			return s.getInfo().getTitle();
		return null;
	}

	/**
	 * Returns the localized description of this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own description.
	 *
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestResource#description() @RestResource.description()} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(description=<js>"My Resource"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(description=<js>"$L{myLocalizedDescription}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].description</ck>
	 * 			<li><ck>description</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#description() @RestResource.description()} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.description</ck> = <cv>My Resource</cv>
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.description</ck> = <cv>$C{MyStrings/MyClass.description}</cv>
	 * 		</p>
	 * 	<li><ck>/info/description</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if none was was found.
	 * @throws Exception
	 */
	@Override /* RestInfoProvider */
	public String getDescription(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		if (description != null)
			return vr.resolve(description);
		String description = context.getMessages().findFirstString(req.getLocale(), "description");
		if (description != null)
			return vr.resolve(description);
		Swagger s = getSwagger(req);
		if (s != null && s.getInfo() != null && s.getInfo().hasDescription())
			return s.getInfo().getDescription();
		return null;
	}

	private Operation getSwaggerOperation(Method method, RestRequest req) throws Exception {

		Swagger s = getSwagger(req);
		if (s != null) {
			Map<String,OperationMap> sp = s.getPaths();
			if (sp != null) {
				Map<String,Operation> spp = sp.get(method.getAnnotation(RestMethod.class).path());
				if (spp != null)
					return spp.get(req.getMethod());
			}
		}
		return null;
	}
}
