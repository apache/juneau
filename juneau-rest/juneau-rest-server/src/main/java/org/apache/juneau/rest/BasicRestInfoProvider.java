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

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
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

	private final RestContext context;
	private final String
		siteName,
		title,
		description;
	private final ConcurrentHashMap<Locale,ConcurrentHashMap<Integer,Swagger>> swaggers = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param context The resource context.
	 */
	public BasicRestInfoProvider(RestContext context) {
		this.context = context;

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
				if (! r.title().isEmpty())
					title = r.title();
				if (! r.description().isEmpty())
					description = r.description();
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
	@SuppressWarnings("unchecked")
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
		
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		MessageBundle mb = context.getMessages();
		Class<?> c = context.getResource().getClass();
		
		// Load swagger JSON from classpath.
		ObjectMap omSwagger = context.getClasspathResource(ObjectMap.class, MediaType.JSON, getClass().getSimpleName() + ".json", locale);
		if (omSwagger == null)
			omSwagger = new ObjectMap();
		
		// Combine it with @RestResource(swagger)
		for (Map.Entry<Class<?>,RestResource> e : findAnnotationsMapParentFirst(RestResource.class, context.getResource().getClass()).entrySet()) {
			RestResource r = e.getValue();
			if (r.swagger().length > 0) {
				try {
					String json = vr.resolve(join(r.swagger(), '\n').trim());
					if (! isObjectMap(json, true))
						json = "{\n" + json + "\n}";
					omSwagger.putAll(new ObjectMap(json));
				} catch (ParseException x) {
					throw new SwaggerException(x, e.getKey(), "Malformed swagger JSON encountered in @RestResource(swagger).");
				}
			}
		}
		
		ObjectMap 
			info = omSwagger.getObjectMap("info", true),
			externalDocs = omSwagger.getObjectMap("externalDocs", true),
			definitions = omSwagger.getObjectMap("definitions", true);
		ObjectList
			produces = omSwagger.getObjectList("produces", true),
			consumes = omSwagger.getObjectList("consumes", true);
		
		String s = this.title;
		if (s == null)
			s = mb.findFirstString(locale, "title");
		if (s != null) 
			info.put("title", vr.resolve(s));

		s = this.description;
		if (s == null)
			s = mb.findFirstString(locale, "description");
		if (s != null) 
			info.put("description", vr.resolve(s));
		
		s = mb.findFirstString(locale, "version");
		if (s != null) 
			info.put("version", vr.resolve(s));
		
		s = mb.findFirstString(locale, "contact");
		if (s != null) 
			info.put("contact", jp.parse(vr.resolve(s), ObjectMap.class));
		
		s = mb.findFirstString(locale, "license");
		if (s != null) 
			info.put("license", jp.parse(vr.resolve(s), ObjectMap.class));
		
		s = mb.findFirstString(locale, "termsOfService");
		if (s != null) 
			info.put("termsOfService", vr.resolve(s));
		
		if (consumes.isEmpty()) 
			consumes.addAll(req.getContext().getConsumes());

		if (produces.isEmpty()) 
			produces.addAll(req.getContext().getProduces());
		
		Map<String,ObjectMap> tagMap = new LinkedHashMap<>();
		if (omSwagger.containsKey("tags")) {
			for (ObjectMap om : omSwagger.getObjectList("tags").elements(ObjectMap.class)) {
				String name = om.getString("name");
				if (name == null)
					throw new SwaggerException(c, "Tag definition found without name in swagger JSON.");
				tagMap.put(name, om);
			}
		}
		
		s = mb.findFirstString(locale, "tags");
		if (s != null) {
			for (ObjectMap m : jp.parse(vr.resolve(s), ObjectList.class).elements(ObjectMap.class)) {
				String name = m.getString("name");
				if (name == null)
					throw new SwaggerException(c, "Tag definition found without name in resource bundle.");
				if (tagMap.containsKey(name))
					tagMap.get(name).putAll(m);
				else
					tagMap.put(name, m);
			}
		}

		s = mb.findFirstString(locale, "externalDocs");
		if (s != null) 
			externalDocs.putAll(jp.parse(vr.resolve(s), ObjectMap.class));
		
//		JsonSchemaDefinitions defs = new JsonSchemaDefinitions();
//		for (String defId : definitions.keySet()) 
//			defs.put(defId, definitions.getObjectMap(defId));
		
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
			if (rm.swagger().length > 0) {
				try {
					String json = vr.resolve(join(rm.swagger(), '\n').trim());
					if (! isObjectMap(json, true))
						json = "{\n" + json + "\n}";
					op.putAll(new ObjectMap(json));
				} catch (ParseException e) {
					throw new SwaggerException(e, m, "Malformed swagger JSON encountered in @RestMethod(swagger).");
				}
			}

			op.putIfNotExists("operationId", mn);
			
			if (m.getAnnotation(Deprecated.class) != null)
				op.put("deprecated", true);
				
			s = rm.summary();
			if (s.isEmpty())
				s = mb.findFirstString(locale, mn + ".summary");
			if (s != null)
				op.put("summary", vr.resolve(s));

			s = rm.description();
			if (s.isEmpty())
				s = mb.findFirstString(locale, mn + ".description");
			if (s != null)
				op.put("description", vr.resolve(s));

			Set<String> tags = new LinkedHashSet<>();
			if (op.containsKey("tags"))
				for (String tag : op.getObjectList("tags").elements(String.class)) 
					tags.add(tag);
			
			s = mb.findFirstString(locale, mn + ".tags");
			if (s != null) {
				s = vr.resolve(s);
				if (isObjectList(s, true))
					tags.addAll((List<String>)jp.parse(s, ArrayList.class, String.class));
				else
					tags.addAll(Arrays.asList(StringUtils.split(s)));
			}
			
			for (String tag : tags) 
				if (! tagMap.containsKey(tag))
					tagMap.put(tag, new ObjectMap().append("name", tag));
			
			op.put("tags", tags);
			
			s = mb.findFirstString(locale, mn + ".externalDocs");
			if (s != null) {
				ObjectMap eom = jp.parse(vr.resolve(s), ObjectMap.class);
				if (op.containsKey("externalDocs"))
					op.getObjectMap("externalDocs").putAll(eom);
				else
					op.put("externalDocs", eom);
			}
			
			ObjectMap paramMap = new ObjectMap();

			ObjectList ol = op.getObjectList("parameters");
			if (ol != null) 
				for (ObjectMap param : ol.elements(ObjectMap.class)) 
					paramMap.put(param.getString("in") + '.' + param.getString("name"), param);
		
			s = mb.findFirstString(locale, mn + ".parameters");
			if (s != null) {
				ol = jp.parse(vr.resolve(s), ObjectList.class);
				for (ObjectMap param : ol.elements(ObjectMap.class)) {
					String key = param.getString("in") + '.' + param.getString("name");
					if (paramMap.containsKey(key))
						paramMap.getObjectMap(key).putAll(param);
					else
						paramMap.put(key, param);
				}
			}
			
			// Finally, look for parameters defined on method.
			for (RestParam mp : context.getRestParams(m)) {
				
				RestParamType in = mp.getParamType();
				
				if (in == OTHER)
					continue;
				
				String key = in.toString() + '.' + (in == BODY ? null : mp.getName());
				
				ObjectMap param = paramMap.getObjectMap(key, true);
					
				param.append("in", in);
				
				if (in != BODY)
					param.append("name", mp.name);
				
				ObjectMap pm = mp.getMetaData();
				if (pm.containsKeyNotEmpty("required"))
					param.put("required", vr.resolve(pm.getString("required")));
				if (pm.containsKeyNotEmpty("description"))
					param.put("description", vr.resolve(pm.getString("description")));
				if (pm.containsKeyNotEmpty("type"))
					param.put("type", vr.resolve(pm.getString("type")));
				if (pm.containsKeyNotEmpty("format"))
					param.put("format", vr.resolve(pm.getString("format")));
				if (pm.containsKeyNotEmpty("pattern"))
					param.put("pattern", vr.resolve(pm.getString("pattern")));
				if (pm.containsKeyNotEmpty("collectionFormat"))
					param.put("collectionFormat", vr.resolve(pm.getString("collectionFormat")));
				if (pm.containsKeyNotEmpty("maximum"))
					param.put("maximum", vr.resolve(pm.getString("maximum")));
				if (pm.containsKeyNotEmpty("minimum"))
					param.put("minimum", vr.resolve(pm.getString("minimum")));
				if (pm.containsKeyNotEmpty("multipleOf"))
					param.put("multipleOf", vr.resolve(pm.getString("multipleOf")));
				if (pm.containsKeyNotEmpty("maxLength"))
					param.put("maxLength", vr.resolve(pm.getString("maxLength")));
				if (pm.containsKeyNotEmpty("minLength"))
					param.put("minLength", vr.resolve(pm.getString("minLength")));
				if (pm.containsKeyNotEmpty("maxItems"))
					param.put("maxItems", vr.resolve(pm.getString("maxItems")));
				if (pm.containsKeyNotEmpty("minItems"))
					param.put("minItems", vr.resolve(pm.getString("minItems")));
				if (pm.containsKeyNotEmpty("allowEmptyVals"))
					param.put("allowEmptyVals", vr.resolve(pm.getString("allowEmptyVals")));
				if (pm.containsKeyNotEmpty("exclusiveMaximum"))
					param.put("exclusiveMaximum", vr.resolve(pm.getString("exclusiveMaximum")));
				if (pm.containsKeyNotEmpty("exclusiveMimimum"))
					param.put("exclusiveMimimum", vr.resolve(pm.getString("exclusiveMimimum")));
				if (pm.containsKeyNotEmpty("uniqueItems"))
					param.put("uniqueItems", vr.resolve(pm.getString("uniqueItems")));
				if (pm.containsKeyNotEmpty("schema"))
					param.put("schema", new ObjectMap(vr.resolve(pm.getString("schema"))));
				if (pm.containsKeyNotEmpty("default"))
					param.put("default", JsonParser.DEFAULT.parse(vr.resolve(pm.getString("default")), Object.class));
				if (pm.containsKeyNotEmpty("enum"))
					param.put("enum", new ObjectList(vr.resolve(pm.getString("enum"))));
				if (pm.containsKeyNotEmpty("items"))
					param.put("items", new ObjectMap(vr.resolve(pm.getString("items"))));
				
				if ((in == BODY || in == PATH) && ! param.containsKeyNotEmpty("required"))
					param.put("required", true);
				
//				param.put("schema", getSchema(req, param.getObjectMap("schema", true), defs, mp.getType()));
//				addXExamples(req, param, in.toString(), defs, mp.getType());
			}
			
			if (! paramMap.isEmpty())
				op.put("parameters", paramMap.values());

			ObjectMap responses = op.getObjectMap("responses", true);
			
			s = mb.findFirstString(locale, mn + ".responses");
			if (s != null) {
				for (Map.Entry<String,Object> e : jp.parse(vr.resolve(s), ObjectMap.class).entrySet()) {
					String httpCode = e.getKey();
					if (responses.containsKey(httpCode))
						responses.getObjectMap(httpCode).putAll((ObjectMap)e.getValue());
					else
						responses.put(httpCode, e.getValue());
				}
			}
			
			// Gather responses from @RestStatus-annotated exceptions.
			for (Class<?> t : m.getExceptionTypes()) {
				RestStatus rs = t.getAnnotation(RestStatus.class);
				if (rs != null) {
					String httpCode = String.valueOf(rs.value());
					if (! responses.containsKey(httpCode))
						responses.put(httpCode, new ObjectMap().append("description", rs.description()));
				}
			}
			
			if (! responses.containsKey("200"))
				responses.put("200", new ObjectMap().append("description", "Success"));
			
//			ObjectMap okResponse = responses.getObjectMap("200");
//			
//			okResponse.put("schema", getSchema(req, okResponse.getObjectMap("schema", true), defs, m.getGenericReturnType()));
//			addXExamples(req, okResponse, "ok", defs, m.getGenericReturnType());
			
			if (responses.isEmpty())
				op.remove("responses");
			else
				op.put("responses", new TreeMap<>(responses));
			
			if (! op.containsKey("consumes")) {
				List<MediaType> mConsumes = req.getParsers().getSupportedMediaTypes();
				if (! mConsumes.equals(consumes))
					op.put("consumes", mConsumes);
			}

			if (! op.containsKey("produces")) {
				List<MediaType> mProduces = req.getSerializers().getSupportedMediaTypes();
				if (! mProduces.equals(produces))
					op.put("produces", mProduces);
			}
		}
		
//		definitions.putAll(defs);
		
		if (definitions.isEmpty())
			omSwagger.remove("definitions");		
		if (tagMap.isEmpty())
			omSwagger.remove("tags");
		
		try {
			String swaggerJson = omSwagger.toString(JsonSerializer.DEFAULT_LAX_READABLE);
			swagger = jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new RestServletException("Error detected in swagger.").initCause(e);
		}
		
		swaggers.get(locale).put(hashCode, swagger);
		
		return swagger;
	}
	
//	private ObjectMap getSchema(RestRequest req, ObjectMap schema, JsonSchemaDefinitions defs, Type type) throws Exception {
//		BeanSession bs = req.getBeanSession();
//		ClassMeta<?> cm = bs.getClassMeta(type);
//		
//		if (schema.containsKey("type") || schema.containsKey("$ref")) 
//			return schema;
//		if (cm.isBean()) {
//			schema.put("$ref", defs.getURI(cm));
//		} else if (cm.isMap() || cm.isCollectionOrArray()) {
//			schema.putAll(JsonSchemaUtils.getSchema(bs, defs, cm));
//			Object example = getExample(req, cm, schema.get("example"));
//			if (example != null)
//				schema.put("example", example);
//		} else {
//			schema.putAll(JsonSchemaUtils.getSchema(bs, defs, cm));
//		}
//		return schema;
//	}
//	
//	private void addXExamples(RestRequest req, ObjectMap m, String in, JsonSchemaDefinitions defs, Type type) throws Exception {
//		
//		ClassMeta<?> cm = req.getBeanSession().getClassMeta(type);
//		Object example = defs.getExample(cm);
//		if (example == null)
//			return;
//		
//		List<MediaType> mediaTypes = "ok".equals("in") ? req.getSerializers().getSupportedMediaTypes() : req.getParsers().getSupportedMediaTypes();
//		
//		if ("ok".equals(in) || "body".equals(in)) {
//			ObjectMap examples = new ObjectMap();
//			ObjectMap sprops = new ObjectMap().append(WSERIALIZER_useWhitespace, true).append(OSSERIALIZER_binaryFormat, BinaryFormat.SPACED_HEX);
//			
//			for (MediaType mt : mediaTypes) {
//				if (mt != MediaType.HTML) {
//					Serializer s2 = req.getSerializers().getSerializer(mt);
//					if (s2 != null) {
//						SerializerSessionArgs args = new SerializerSessionArgs(sprops, req.getJavaMethod(), req.getLocale(), null, mt, req.getUriContext());
//						String eVal = s2.createSession(args).serializeToString(example);
//						examples.put(s2.getMediaTypes()[0].toString(), eVal);
//					}
//				}
//			}
//			m.put("x-examples", examples);
//			
//		} else {
//			m.put("example", example);
//		}
//		
//	}
//	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private Object getExample(RestRequest req, ClassMeta<?> cm, Object jsonExample) {
//		try {
//			BeanSession bs = req.getBeanSession();
//			
//			if (cm.isMapOrBean()) {
//				if (jsonExample instanceof ObjectMap)
//					return ((ObjectMap)jsonExample).cast(cm);
//				return cm.getExample(bs);
//			} else if (cm.isCollectionOrArray()) {
//				if (jsonExample instanceof ObjectList)
//					return ((ObjectList)jsonExample).cast(cm);
//				Object e = cm.getExample(bs);
//				if (e != null)
//					return e;
//				e = cm.getElementType().getExample(bs);
//				if (e != null) {
//					if (cm.isCollection()) {
//						Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new ObjectList());
//						c.add(e);
//						return c;
//					}
//					Object array = Array.newInstance(cm.getInnerClass(), 1);
//					Array.set(array, 0, e);
//					return array;
//				}
//			} else {
//				return jsonExample;
//			}
//		} catch (Exception e) { /* Ignore */ }
//		return null;
//	}
	
	private static class SwaggerException extends ParseException {
		private static final long serialVersionUID = 1L;
		
		SwaggerException(Class<?> c, String message, Object...args) {
			this(null, c, message, args);
		}
		SwaggerException(Exception e, Class<?> c, String message, Object...args) {
			super("Swagger exception on class " + c.getName() + ".  " + message, args);
			initCause(e);
		}
		SwaggerException(Exception e, Method m, String message, Object...args) {
			super("Swagger exception on class " + m.getDeclaringClass().getName() + " method "+m.getName()+".  " + message, args);
			initCause(e);
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
		
		String s = method.getAnnotation(RestMethod.class).description();
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
			Map<String,Map<String,Operation>> sp = s.getPaths();
			if (sp != null) {
				Map<String,Operation> spp = sp.get(method.getAnnotation(RestMethod.class).path());
				if (spp != null)
					return spp.get(req.getMethod());
			}
		}
		return null;
	}
}
