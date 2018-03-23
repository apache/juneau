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
import static org.apache.juneau.serializer.WriterSerializer.*;

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

	private final RestContext context;
	private final String
		siteName,
		title,
		description;
	private final ConcurrentHashMap<Locale,Swagger> swaggers = new ConcurrentHashMap<>();

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
	@Override /* RestInfoProvider */
	public Swagger getSwagger(RestRequest req) throws Exception {
		
		Locale locale = req.getLocale();
		BeanSession bs = req.getBeanSession();
		
		Swagger s = swaggers.get(locale);
		if (s != null)
			return s;

		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		MessageBundle mb = context.getMessages();
		
		ObjectMap om = context.getClasspathResource(ObjectMap.class, MediaType.JSON, getClass().getSimpleName() + ".json", locale);
		if (om == null)
			om = new ObjectMap();
		
		LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsParentFirst = findAnnotationsMapParentFirst(RestResource.class, context.getResource().getClass());

		for (RestResource r : restResourceAnnotationsParentFirst.values()) {
			if (r.swagger().length > 0) {
				try {
					String json = vr.resolve(StringUtils.join(r.swagger(), '\n').trim());
					if (! StringUtils.isObjectMap(json, true))
						json = "{\n" + json + "\n}";
					om.putAll(new ObjectMap(json));
				} catch (ParseException e) {
					throw new ParseException("Malformed swagger JSON encountered in @RestResource(swagger) on class "+context.getResource().getClass().getName()+".").initCause(e);
				}
			}
		}

		String title = this.title;
		if (title == null)
			title = mb.findFirstString(locale, "title");
		if (title != null) 
			getInfo(om).put("title", vr.resolve(title));

		String description = this.description;
		if (description == null)
			description = mb.findFirstString(locale, "description");
		if (description != null) 
			getInfo(om).put("description", vr.resolve(description));
		
		String version = mb.findFirstString(locale, "version");
		if (version != null) 
			getInfo(om).put("version", vr.resolve(version));
		
		String contact = mb.findFirstString(locale, "contact");
		if (contact != null) 
			getInfo(om).put("contact", jp.parse(vr.resolve(contact), ObjectMap.class));
		
		String license = mb.findFirstString(locale, "license");
		if (license != null) 
			getInfo(om).put("license", jp.parse(vr.resolve(license), ObjectMap.class));
		
		String termsOfService = mb.findFirstString(locale, "termsOfService");
		if (termsOfService != null) 
			getInfo(om).put("termsOfService", vr.resolve(termsOfService));
		
		if (! om.containsKey("consumes")) {
			List<MediaType> consumes = req.getContext().getConsumes();
			if (! consumes.isEmpty())
				om.put("consumes", consumes);
		}

		if (! om.containsKey("produces")) {
			List<MediaType> produces = req.getContext().getProduces();
			if (! produces.isEmpty())
				om.put("produces", produces);
		}
			
		String tags = mb.findFirstString(locale, "tags");
		if (tags != null)
			om.put("tags", jp.parse(vr.resolve(tags), ObjectList.class));

		String externalDocs = mb.findFirstString(locale, "externalDocs");
		if (externalDocs != null)
			om.put("externalDocs", jp.parse(vr.resolve(externalDocs), ObjectMap.class));
		
		ObjectMap definitions = om.getObjectMap("definitions", new ObjectMap());
		
		for (RestJavaMethod sm : context.getCallMethods().values()) {
			if (sm.isRequestAllowed(req)) {
				Method m = sm.method;
				RestMethod rm = m.getAnnotation(RestMethod.class);
				String mn = m.getName(), cn = m.getClass().getName();
				
				ObjectMap mom = getOperation(om, sm.getPathPattern(), sm.getHttpMethod().toLowerCase());
				
				if (rm.swagger().length > 0) {
					try {
						String json = vr.resolve(StringUtils.join(rm.swagger(), '\n').trim());
						if (! (json.startsWith("{") && json.endsWith("}")))
							json = "{\n" + json + "\n}";
						mom.putAll(new ObjectMap(json));
					} catch (ParseException e) {
						throw new ParseException("Malformed swagger JSON encountered in @RestMethod(swagger) on method "+mn+" on class "+cn+".").initCause(e);
					}
				}

				mom.put("operationId", mn);
				
				String mDescription = rm.description();
				if (mDescription.isEmpty())
					mDescription = mb.findFirstString(locale, mn + ".description");
				if (mDescription != null)
					mom.put("description", vr.resolve(mDescription));
				
				String mTags = mb.findFirstString(locale, mn + ".tags");
				if (mTags != null) {
					mTags = vr.resolve(mTags);
					if (StringUtils.isObjectList(mTags, true)) 
						mom.put("tags", jp.parse(mTags, ArrayList.class, String.class));
					else
						mom.put("tags", Arrays.asList(StringUtils.split(mTags)));
				}
				
				String mSummary = mb.findFirstString(locale, mn + ".summary");
				if (mSummary != null)
					mom.put("summary", vr.resolve(mSummary));

				String mExternalDocs = mb.findFirstString(locale, mn + ".externalDocs");
				if (mExternalDocs != null) 
					mom.put("externalDocs", jp.parse(vr.resolve(s), ObjectMap.class));
				
				Map<String,ObjectMap> paramMap = new LinkedHashMap<>();

				ObjectList parameters = mom.getObjectList("parameters");
				if (parameters != null) {
					for (ObjectMap param : parameters.elements(ObjectMap.class)) {
						String key = param.getString("in") + '.' + param.getString("name");
						paramMap.put(key, param);
					}
				}
			
				String mParameters = mb.findFirstString(locale, mn + ".parameters");
				if (mParameters != null) {
					ObjectList ol = jp.parse(vr.resolve(mParameters), ObjectList.class);
					for (ObjectMap param : ol.elements(ObjectMap.class)) {
						String key = param.getString("in") + '.' + param.getString("name");
						if (paramMap.containsKey(key))
							paramMap.get(key).putAll(param);
						else
							paramMap.put(key, param);
					}
				}
				
				// Finally, look for parameters defined on method.
				for (RestParam mp : context.getRestParams(m)) {
					RestParamType in = mp.getParamType();
					if (in != RestParamType.OTHER) {
						String key = in.toString() + '.' + (in == RestParamType.BODY ? null : mp.getName());
						
						if (! paramMap.containsKey(key))
							paramMap.put(key, new ObjectMap());
						ObjectMap param = paramMap.get(key);
							
						param.append("in", in);
						
						if (in != RestParamType.BODY)
							param.append("name", mp.name);
						
						if (! param.containsKey("schema")) {
							ClassMeta<?> cm = bs.getClassMeta(mp.getType());
							
							if (cm.isBean()) {
								String name = mp.forClass().getSimpleName();
								if (! definitions.containsKey(name)) {
									ObjectMap definition = JsonSchemaUtils.getSchema(bs, cm);
									
									Object example = cm.getExample(bs);
									if (example != null) {
										ObjectMap examples = new ObjectMap();
										ObjectMap sprops = new ObjectMap().append(WSERIALIZER_useWhitespace, true);
										for (MediaType mt : req.getParsers().getSupportedMediaTypes()) {
											if (mt != MediaType.HTML) {
												Serializer s2 = req.getSerializers().getSerializer(mt);
												if (s2 != null) {
													SerializerSessionArgs args = new SerializerSessionArgs(sprops, req.getJavaMethod(), req.getLocale(), null, mt, req.getUriContext());
													String eVal = s2.createSession(args).serializeToString(example);
													examples.put(s2.getMediaTypes()[0].toString(), eVal);
												}
											}
										}
										definition.put("x-examples", examples);
									}
									
									definitions.put(name, definition);
								}
								param.put("schema", new ObjectMap().append("$ref", "#/definitions/" + mp.forClass().getSimpleName()));
							}
						}
					}
				}
				
				if (! paramMap.isEmpty())
					mom.put("parameters", paramMap.values());
				
				String mResponses = mb.findFirstString(locale, mn + ".responses");
				if (mResponses != null) 
					mom.put("responses", jp.parse(vr.resolve(mResponses), ObjectMap.class));

				if (! mom.containsKey("consumes")) {
					List<MediaType> mConsumes = req.getParsers().getSupportedMediaTypes();
					if (! mConsumes.equals(om.get("consumes")))
						mom.put("consumes", mConsumes);
				}
	
				if (! mom.containsKey("produces")) {
					List<MediaType> mProduces = req.getSerializers().getSupportedMediaTypes();
					if (! mProduces.equals(om.get("produces")))
						mom.put("produces", mProduces);
				}
			}
		}
		
		if (! definitions.isEmpty())
			om.put("definitions", definitions);		
		
		String swaggerJson = om.toString(JsonSerializer.DEFAULT_LAX_READABLE);
		try {
			s = jp.parse(swaggerJson, Swagger.class);
		} catch (Exception e) {
			throw new RestServletException("Error detected in swagger: \n{0}", StringUtils.addLineNumbers(swaggerJson)).initCause(e);
		}
		swaggers.put(locale, s);
		
		return s;
	}
	
	private ObjectMap getInfo(ObjectMap om) {
		if (! om.containsKey("info"))
			om.put("info", new ObjectMap());
		return om.getObjectMap("info");
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
		if (s != null && s.getInfo() != null)
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
		if (s != null && s.getInfo() != null)
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
