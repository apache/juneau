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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.ReflectionUtils.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Default implementation of {@link RestInfoProvider}.
 * 
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST resources are documented.
 * 
 * 
 * <h5 class='topic'>Additional Information</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
 * </ul>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.OptionsPages">Overview &gt; OPTIONS Pages</a>
 * </ul>
 */
public class RestInfoProviderDefault implements RestInfoProvider {

	private final RestContext context;
	private final String
		siteName,
		title,
		description,
		termsOfService,
		contact,
		license,
		version,
		tags,
		externalDocs;
	private final ConcurrentHashMap<Locale,Swagger> swaggers = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param context The resource context.
	 */
	public RestInfoProviderDefault(RestContext context) {
		this.context = context;

		Builder b = new Builder(context);
		this.siteName = b.siteName;
		this.title = b.title;
		this.description = b.description;
		this.termsOfService = b.termsOfService;
		this.contact = b.contact;
		this.license = b.license;
		this.version = b.version;
		this.tags = b.tags;
		this.externalDocs = b.externalDocs;
	}

	private static final class Builder {
		String
			siteName,
			title,
			description,
			termsOfService,
			contact,
			license,
			version,
			tags,
			externalDocs;

		Builder(RestContext context) {

			LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsParentFirst = findAnnotationsMapParentFirst(RestResource.class, context.getResource().getClass());

			for (RestResource r : restResourceAnnotationsParentFirst.values()) {
				if (! r.siteName().isEmpty())
					siteName = r.siteName();
				if (! r.title().isEmpty())
					title = r.title();
				if (! r.description().isEmpty())
					description = r.description();
				ResourceSwagger sr = r.swagger();
				if (! sr.termsOfService().isEmpty())
					termsOfService = sr.termsOfService();
				if (! sr.contact().isEmpty())
					contact = sr.contact();
				if (! sr.license().isEmpty())
					license = sr.license();
				if (! sr.version().isEmpty())
					version = sr.version();
				if (! sr.tags().isEmpty())
					tags = sr.tags();
				if (! sr.externalDocs().isEmpty())
					externalDocs = sr.externalDocs();
			}
		}
	}

	/**
	 * Returns the localized Swagger from the file system.
	 * 
	 * <p>
	 * Looks for a file called <js>"{ServletClass}_{locale}.json"</js> in the same package as this servlet and returns
	 * it as a parsed {@link Swagger} object.
	 * 
	 * <p>
	 * Returned objects are cached per-locale for later quick-lookup.
	 * 
	 * @param req The incoming HTTP request.
	 * @return The parsed swagger object, or <jk>null</jk> if none could be found.
	 * @throws Exception 
	 * 	If swagger file was not valid JSON.
	 */
	public Swagger getSwaggerFromFile(RestRequest req) throws Exception {
		Locale locale = req.getLocale();
		Swagger s = swaggers.get(locale);
		if (s == null) {
			s = context.getClasspathResource(Swagger.class, MediaType.JSON, getClass().getSimpleName() + ".json", locale);
			swaggers.putIfAbsent(locale, s == null ? Swagger.NULL : s);
		}
		return s == Swagger.NULL ? null : s;
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
		
		// If a file is defined, use that.
		Swagger s = getSwaggerFromFile(req);
		if (s != null)
			return s;

		s = swagger(
			info(getTitle(req), getVersion(req))
				.contact(getContact(req))
				.license(getLicense(req))
				.description(getDescription(req))
				.termsOfService(getTermsOfService(req))
			)
			.consumes(getConsumes(req))
			.produces(getProduces(req))
			.tags(getTags(req))
			.externalDocs(getExternalDocs(req));

		for (RestJavaMethod sm : context.getCallMethods().values()) {
			if (sm.isRequestAllowed(req)) {
				Method m = sm.method;
				Operation o = operation()
					.operationId(getMethodOperationId(m, req))
					.description(getMethodDescription(m, req))
					.tags(getMethodTags(m, req))
					.summary(getMethodSummary(m, req))
					.externalDocs(getMethodExternalDocs(m, req))
					.parameters(getMethodParameters(m, req))
					.responses(getMethodResponses(m, req));

				if (isDeprecated(m, req))
					o.deprecated(true);
				
				o.consumes(getMethodConsumes(m, req));
				o.produces(getMethodProduces(m, req));

				s.path(
					sm.getPathPattern(),
					sm.getHttpMethod().toLowerCase(),
					o
				);
			}
		}
		
		return s;
	}

	/**
	 * Returns the localized operation ID of the specified java method.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own operation ID.
	 * 
	 * <p>
	 * The default implementation simply returns the Java method name.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized operation ID of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public String getMethodOperationId(Method method, RestRequest req) throws Exception {
		return method.getName();
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
		if (s.isEmpty())
			s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".summary");
		if (s != null)
			return vr.resolve(s);
		
		Operation o = getSwaggerOperationFromFile(method, req);
		return o == null ? null : o.getSummary();
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
		if (s.isEmpty())
			s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".description");
		if (s != null)
			return vr.resolve(s);
		
		Operation o = getSwaggerOperationFromFile(method, req);
		return o == null ? null : o.getDescription();
	}

	/**
	 * Returns the localized Swagger tags for this Java method.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own tags.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link MethodSwagger#tags() @MethodSwagger.tags()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(tags=<js>"foo,bar,baz"</js>)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(tags=<js>"$L{myLocalizedTags}"</js>)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].tags</ck>
	 * 			<li><ck>[javaMethodName].tags</ck>
	 * 		</ol>
	 * 		<br>Value can be a comma-delimited list or JSON array.
	 * 		<br>Value can contain any SVL variables defined on the {@link MethodSwagger#tags() @MethodSwagger.tags()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Comma-delimited list</cc>
	 * 	<ck>MyClass.myMethod.tags</ck> = <cv>foo, bar, baz</cv>
	 * 	
	 * 	<cc>// JSON array</cc>
	 * 	<ck>MyClass.myMethod.tags</ck> = <cv>["foo", "bar", "baz"]</cv>
	 * 
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.description</ck> = <cv>$C{MyStrings/MyClass.myMethod.tags}</cv>
	 * 		</p>
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized tags of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 */
	public List<String> getMethodTags(Method method, RestRequest req) throws Exception {
		JsonParser p = JsonParser.DEFAULT;
		VarResolverSession vr = req.getVarResolverSession();
		
		String s = method.getAnnotation(RestMethod.class).swagger().tags();
		if (s.isEmpty())
			s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".tags");
		if (s != null) {
			s = vr.resolve(s);
			if (StringUtils.isObjectList(s)) 
				return p.parse(s, ArrayList.class, String.class);
			return Arrays.asList(StringUtils.split(s));
		}

		Operation o = getSwaggerOperationFromFile(method, req);
		return o == null ? null : o.getTags();
	}

	/**
	 * Returns the localized external documentation of the specified java method on this servlet.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own external documentation.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link MethodSwagger#externalDocs() @MethodSwagger.externalDocs()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(externalDocs=<js>"{description:'Find more info here',url:'https://swagger.io'}"</js>)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(externalDocs=<js>"$L{myLocalizedExternalDocs}"</js>)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].externalDocs</ck>
	 * 			<li><ck>[javaMethodName].externalDocs</ck>
	 * 		</ol>
	 * 		<br>Value is a JSON representation of a {@link ExternalDocumentation} object.
	 * 		<br>Value can contain any SVL variables defined on the {@link MethodSwagger#externalDocs() @MethodSwagger.externalDocs()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.myMethod.externalDocs</ck> = <cv>{description:"Find more info here",url:"https://swagger.io"}</js>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.externalDocs</ck> = <cv>$C{MyStrings/MyClass.myMethod.externalDocs}</cv>
	 * 		</p>
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized external documentation of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public ExternalDocumentation getMethodExternalDocs(Method method, RestRequest req) throws Exception {
		JsonParser p = JsonParser.DEFAULT;
		VarResolverSession vr = req.getVarResolverSession();
		
		String s = method.getAnnotation(RestMethod.class).swagger().externalDocs();
		if (s.isEmpty())
			s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".externalDocs");
		if (s != null) 
			return p.parse(vr.resolve(s), ExternalDocumentation.class);

		Operation o = getSwaggerOperationFromFile(method, req);
		return o == null ? null : o.getExternalDocs();
	}
	
	/**
	 * Returns the localized parameter info for the specified java method.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own parameter info.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>Operation information from swagger file.
	 * 	<li>{@link MethodSwagger#parameters() @MethodSwagger.parameters()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			parameters={
	 * 				<ja>@Parameter</ja>(in=<js>"path"</js>, name=<js>"a"</js>, description=<js>"The 'a' attribute"</js>)
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			parameters={
	 * 				<ja>@Parameter</ja>(in=<js>"path"</js>, name=<js>"a"</js>, description=<js>"$L{myLocalizedParamADescription}"</js>)
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].parameters</ck>
	 * 			<li><ck>[javaMethodName].parameters</ck>
	 * 		</ol>
	 * 		<br>Value is a JSON representation of a <code>{@link ParameterInfo}[]</code> object.
	 * 		<br>Value can contain any SVL variables defined on the {@link MethodSwagger#parameters() @MethodSwagger.parameters()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.myMethod.parameters</ck> = <cv>[{name:"a",in:"path",description:"The ''a'' attribute"}]</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.parameters</ck> = <cv>$C{MyStrings/MyClass.myMethod.parameters}</cv>
	 * 		</p>
	 * 	<li>Information gathered directly from the parameters on the Java method.
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized parameter info of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public List<ParameterInfo> getMethodParameters(Method method, RestRequest req) throws Exception {
		
		Operation o = getSwaggerOperationFromFile(method, req);
		if (o != null && o.getParameters() != null)
			return o.getParameters();

		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		Map<String,ParameterInfo> m = new TreeMap<>();

		// First parse @RestMethod.parameters() annotation.
		for (org.apache.juneau.rest.annotation.Parameter v : method.getAnnotation(RestMethod.class).swagger().parameters()) {
			String in = vr.resolve(v.in());
			ParameterInfo p = parameterInfo(in, vr.resolve(v.name()));

			if (! v.description().isEmpty())
				p.description(vr.resolve(v.description()));
			if (v.required())
				p.required(v.required());

			if ("body".equals(in)) {
				if (! v.schema().isEmpty())
					p.schema(jp.parse(vr.resolve(v.schema()), SchemaInfo.class));
			} else {
				if (v.allowEmptyValue())
					p.allowEmptyValue(v.allowEmptyValue());
				if (! v.collectionFormat().isEmpty())
					p.collectionFormat(vr.resolve(v.collectionFormat()));
				if (! v._default().isEmpty())
					p._default(vr.resolve(v._default()));
				if (! v.format().isEmpty())
					p.format(vr.resolve(v.format()));
				if (! v.items().isEmpty())
					p.items(jp.parse(vr.resolve(v.items()), Items.class));
				p.type(vr.resolve(v.type()));
			}
			m.put(p.getIn() + '.' + p.getName(), p);
		}

		// Next, look in resource bundle.
		String s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".parameters");
		if (s != null) {
			for (ParameterInfo pi : jp.parse(vr.resolve(s), ParameterInfo[].class)) {
				String key = pi.getIn() + '.' + pi.getName();
				ParameterInfo p = m.get(key);
				if (p == null)
					m.put(key, pi);
				else 
					p.copyFrom(pi);
			}
		}

		// Finally, look for parameters defined on method.
		for (RestParam mp : context.getRestParams(method)) {
			RestParamType in = mp.getParamType();
			if (in != RestParamType.OTHER) {
				String k2 = in.toString() + '.' + (in == RestParamType.BODY ? null : mp.getName());
				ParameterInfo p = m.get(k2);
				if (p == null) {
					p = parameterInfoStrict(in.toString(), mp.getName());
					m.put(k2, p);
				}
			}
		}
		
		return m.isEmpty() ? null : new ArrayList<>(m.values());
	}

	/**
	 * Returns the localized response info for the specified java method.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own parameter info.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>Operation information from swagger file.
	 * 	<li>{@link MethodSwagger#responses() @MethodSwagger.responses()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			responses={
	 * 				<ja>@Response</ja>(
	 * 					value=302,
	 * 					description=<js>"Thing wasn't found here"</js>,
	 * 					headers={
	 * 						<ja>@Parameter</ja>(name=<js>"Location"</js>, description=<js>"The place to find the thing"</js>)
	 * 					}
	 * 				)
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			responses={
	 * 				<ja>@Response</ja>(
	 * 					value=302,
	 * 					description=<js>"Thing wasn't found here"</js>,
	 * 					headers={
	 * 						<ja>@Parameter</ja>(name=<js>"Location"</js>, description=<js>"$L{myLocalizedResponseDescription}"</js>)
	 * 					}
	 * 				)
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].responses</ck>
	 * 			<li><ck>[javaMethodName].responses</ck>
	 * 		</ol>
	 * 		<br>Value is a JSON representation of a <code>Map&lt;Integer,{@link ResponseInfo}&gt;</code> object.
	 * 		<br>Value can contain any SVL variables defined on the {@link MethodSwagger#responses() @MethodSwagger.responses()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.myMethod.responses</ck> = <cv>{302:{description:'Thing wasn''t found here',headers={Location:{description:"The place to find the thing"}}}</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.myMethod.responses</ck> = <cv>$C{MyStrings/MyClass.myMethod.responses}</cv>
	 * 		</p>
	 * 	<li>Information gathered directly from the parameters on the Java method.
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized response info of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer,ResponseInfo> getMethodResponses(Method method, RestRequest req) throws Exception {
		
		Operation o = getSwaggerOperationFromFile(method, req);
		if (o != null && o.getResponses() != null)
			return o.getResponses();

		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		Map<Integer,ResponseInfo> m = new TreeMap<>();
		Map<String,HeaderInfo> m2 = new TreeMap<>();

		// First parse @RestMethod.parameters() annotation.
		for (Response r : method.getAnnotation(RestMethod.class).swagger().responses()) {
			int httpCode = r.value();
			String description = r.description().isEmpty() ? RestUtils.getHttpResponseText(r.value()) : vr.resolve(r.description());
			ResponseInfo r2 = responseInfo(description);

			if (r.headers().length > 0) {
				for (org.apache.juneau.rest.annotation.Parameter v : r.headers()) {
					HeaderInfo h = headerInfoStrict(vr.resolve(v.type()));
					if (! v.collectionFormat().isEmpty())
						h.collectionFormat(vr.resolve(v.collectionFormat()));
					if (! v._default().isEmpty())
						h._default(vr.resolve(v._default()));
					if (! v.description().isEmpty())
						h.description(vr.resolve(v.description()));
					if (! v.format().isEmpty())
						h.format(vr.resolve(v.format()));
					if (! v.items().isEmpty())
						h.items(jp.parse(vr.resolve(v.items()), Items.class));
					r2.header(v.name(), h);
					m2.put(httpCode + '.' + v.name(), h);
				}
			}
			m.put(httpCode, r2);
		}

		// Next, look in resource bundle.
		String s = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".responses");
		if (s != null) {
			for (Map.Entry<Integer,ResponseInfo> e : ((Map<Integer,ResponseInfo>)jp.parse(vr.resolve(s), Map.class, Integer.class, ResponseInfo.class)).entrySet()) {
				Integer httpCode = e.getKey();
				ResponseInfo ri = e.getValue();

				ResponseInfo r = m.get(httpCode);
				if (r == null)
					m.put(httpCode, ri);
				else
					r.copyFrom(ri);
			}
		}

		return m.isEmpty() ? null : m;
	}

	/**
	 * Returns the supported <code>Accept</code> types the specified Java method.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own produces info.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestMethod#produces() @RestMethod.supportedAcceptTypes()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(supportedAcceptTypes={<js>"text/json"</js>})
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(supportedAcceptTypes={<js>"$C{mySupportedProduces}"</js>})
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Media types defined on the parsers associated with the method.
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The supported <code>Accept</code> types of the method, or <jk>null</jk> if none was found 
	 * 	or the list of media types match those of the parent resource class.
	 * @throws Exception 
	 */
	public List<MediaType> getMethodProduces(Method method, RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		String[] s = method.getAnnotation(RestMethod.class).produces();
		if (s.length > 0)
			return Arrays.asList(MediaType.forStrings(vr.resolve(s)));
		List<MediaType> l = req.getSerializers().getSupportedMediaTypes();
		return (l.equals(context.getProduces())  ? null : l);
	}
	
	/**
	 * Returns the supported <code>Content-Type</code> types the specified Java method.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link RestMethod#consumes() @RestMethod.supportedContentTypes()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(supportedContentTypes={<js>"text/json"</js>})
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(supportedContentTypes={<js>"$C{mySupportedConsumes}"</js>})
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Media types defined on the serializers associated with the method.
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The supported <code>Content-Type</code> types of the method, or <jk>null</jk> if none was found 
	 * 	or the list of media types match those of the parent resource class.
	 * @throws Exception 
	 */
	public List<MediaType> getMethodConsumes(Method method, RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		String[] s = method.getAnnotation(RestMethod.class).consumes();
		if (s.length > 0)
			return Arrays.asList(MediaType.forStrings(vr.resolve(s)));
		List<MediaType> l = req.getParsers().getSupportedMediaTypes();
		return (l.equals(context.getConsumes())  ? null : l);
	}

	/**
	 * Returns whether the specified method is deprecated
	 * 
	 * <p>
	 * The default implementation returns the value from the following location:
	 * <ol class='spaced-list'>
	 * 	<li>{@link MethodSwagger#deprecated() @MethodSwagger.deprecated()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			deprecated=<jk>true</jk>
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * </ol>
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return <jk>true</jk> if the method is deprecated.
	 * @throws Exception 
	 */
	public boolean isDeprecated(Method method, RestRequest req) throws Exception {
		return method.getAnnotation(RestMethod.class).swagger().deprecated();
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
		Swagger s = getSwaggerFromFile(req);
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
	 * 		<h6 class='figure'>Examples:</h6>
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
	 * 		<h6 class='figure'>Examples:</h6>
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
		Swagger s = getSwaggerFromFile(req);
		if (s != null && s.getInfo() != null)
			return s.getInfo().getDescription();
		return null;
	}

	/**
	 * Returns the localized contact information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own contact information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#contact() @ResourceSwagger.contact()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(contact=<js>"{name:'John Smith',email:'john.smith@foo.bar'}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(contact=<js>"$C{MyStrings/MyClass.myContactInfo}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].contact</ck>
	 * 			<li><ck>contact</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#contact() @ResourceSwagger.contact()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.contact</ck> = <cv>{name:"John Smith",email:"john.smith@foo.bar"}</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.contact</ck> = <cv>$C{MyStrings/MyClass.myContactInfo}</cv>
	 * 		</p>
	 * 	<li><ck>/info/contact</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public Contact getContact(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (contact != null)
				return jp.parse(vr.resolve(contact), Contact.class);
			String contact = context.getMessages().findFirstString(req.getLocale(), "contact");
			if (contact != null)
				return jp.parse(vr.resolve(contact), Contact.class);
			Swagger s = getSwaggerFromFile(req);
			if (s != null && s.getInfo() != null)
				return s.getInfo().getContact();
			return null;
		} catch (ParseException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized license information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own license information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#license() @ResourceSwagger.license()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(license=<js>"$C{MyStrings/MyClass.myLicenseInfo}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].license</ck>
	 * 			<li><ck>license</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#license() @ResourceSwagger.license()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.license</ck> = <cv>{name:"Apache 2.0",url:"http://www.apache.org/licenses/LICENSE-2.0.html"}</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.license</ck> = <cv>$C{MyStrings/MyClass.myLicenseInfo}</cv>
	 * 		</p>
	 * 	<li><ck>/info/license</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized license information of this REST resource, or <jk>null</jk> if none was found found.
	 * @throws Exception 
	 */
	public License getLicense(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (license != null)
				return jp.parse(vr.resolve(license), License.class);
			String license = context.getMessages().findFirstString(req.getLocale(), "license");
			if (license != null)
				return jp.parse(vr.resolve(license), License.class);
			Swagger s = getSwaggerFromFile(req);
			if (s != null && s.getInfo() != null)
				return s.getInfo().getLicense();
			return null;
		} catch (ParseException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the terms-of-service information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own terms-of-service information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#termsOfService() @ResourceSwagger.termsOfService()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(termsOfService=<js>"You're on your own"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(termsOfService=<js>"$C{MyStrings/MyClass.myTermsOfService}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].termsOfService</ck>
	 * 			<li><ck>termsOfService</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#termsOfService() @ResourceSwagger.termsOfService()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.termsOfService</ck> = <cv>You''re on your own</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.termsOfService</ck> = <cv>$C{MyStrings/MyClass.myTermsOfService}</cv>
	 * 		</p>
	 * 	<li><ck>/info/termsOfService</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized terms-of-service of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public String getTermsOfService(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		if (termsOfService != null)
			return vr.resolve(termsOfService);
		String termsOfService = context.getMessages().findFirstString(req.getLocale(), "termsOfService");
		if (termsOfService != null)
			return vr.resolve(termsOfService);
		Swagger s = getSwaggerFromFile(req);
		if (s != null && s.getInfo() != null)
			return s.getInfo().getTermsOfService();
		return null;
	}

	/**
	 * Returns the version information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own version information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#version() @ResourceSwagger.version()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(version=<js>"2.0"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(version=<js>"$C{MyStrings/MyClass.myVersion}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].version</ck>
	 * 			<li><ck>version</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#version() @ResourceSwagger.version()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.version</ck> = <cv>2.0</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.version</ck> = <cv>$C{MyStrings/MyClass.myVersion}</cv>
	 * 		</p>
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized version of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public String getVersion(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		if (version != null)
			return vr.resolve(version);
		String version = context.getMessages().findFirstString(req.getLocale(), "version");
		if (version != null)
			return vr.resolve(version);
		Swagger s = getSwaggerFromFile(req);
		if (s != null && s.getInfo() != null)
			return s.getInfo().getVersion();
		return null;
	}

	/**
	 * Returns the supported <code>Content-Type</code> request headers for the REST resource.
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#version() @ResourceSwagger.version()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(version=<js>"2.0"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(version=<js>"$C{MyStrings/MyClass.myVersion}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].version</ck>
	 * 			<li><ck>version</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#version() @ResourceSwagger.version()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.version</ck> = <cv>2.0</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.version</ck> = <cv>$C{MyStrings/MyClass.myVersion}</cv>
	 * 		</p>
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The supported <code>Content-Type</code> request headers of the REST resource, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 */
	public List<MediaType> getConsumes(RestRequest req) throws Exception {
		List<MediaType> l = req.getContext().getConsumes();
		return l.isEmpty() ? null : l;
	}
	
	/**
	 * Returns the supported <code>Accept</code> request headers for the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The supported <code>Accept</code> request headers of the REST resource, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 */
	public List<MediaType> getProduces(RestRequest req) throws Exception {
		List<MediaType> l = req.getContext().getProduces();
		return l.isEmpty() ? null : l;
	}

	/**
	 * Returns the version information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own version information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#tags() @ResourceSwagger.tags()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(tags=<js>"foo,bar,baz"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(tags=<js>"$C{MyStrings/MyClass.myTags}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].tags</ck>
	 * 			<li><ck>tags</ck>
	 * 		</ol>
	 * 		<br>Value is either a comma-delimited list or a JSON array.
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#tags() @ResourceSwagger.tags()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Comma-delimited list</cc>
	 * 	<ck>MyClass.tags</ck> = <cv>foo,bar,baz</cv>
	 * 	
	 * 	<cc>// JSON array</cc>
	 * 	<ck>MyClass.tags</ck> = <cv>["foo","bar","baz"]</cv>
	 * 
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.tags</ck> = <cv>$C{MyStrings/MyClass.myTags}</cv>
	 * 		</p>
	 * 	<li><ck>tags</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized tags of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public List<Tag> getTags(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (tags != null)
				return jp.parse(vr.resolve(tags), ArrayList.class, Tag.class);
			String tags = context.getMessages().findFirstString(req.getLocale(), "tags");
			if (tags != null)
				return jp.parse(vr.resolve(tags), ArrayList.class, Tag.class);
			Swagger s = getSwaggerFromFile(req);
			if (s != null && s.getTags() != null)
				return s.getTags();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the version information of this REST resource.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own version information.
	 * 
	 * <p>
	 * The default implementation returns the value from the following locations (whichever matches first):
	 * <ol class='spaced-list'>
	 * 	<li>{@link ResourceSwagger#externalDocs() @ResourceSwagger.externalDocs()} annotation on this class, and then any parent classes.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(externalDocs=<js>"{url:'http://juneau.apache.org'}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 	
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@ResourceSwagger</ja>(externalDocs=<js>"$C{MyStrings/MyClass.myExternalDocs}"</js>)
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].externalDocs</ck>
	 * 			<li><ck>externalDocs</ck>
	 * 		</ol>
	 * 		<br>Value is a JSON objec representation of a {@link ExternalDocumentation} object.
	 * 		<br>Value can contain any SVL variables defined on the {@link ResourceSwagger#externalDocs() @ResourceSwagger.externalDocs()} annotation.
	 * 		<h6 class='figure'>Examples:</h6>
	 * 		<p class='bcode'>
	 * 	<cc>// Direct value</cc>
	 * 	<ck>MyClass.externalDocs</ck> = <cv>{url:"http://juneau.apache.org"}</cv>
	 * 	
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ck>MyClass.externalDocs</ck> = <cv>$C{MyStrings/MyClass.myExternalDocs}</cv>
	 * 		</p>
	 * 	<li><ck>externalDocs</ck> entry in swagger file.
	 * </ol>
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized external documentation of this REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 */
	public ExternalDocumentation getExternalDocs(RestRequest req) throws Exception {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			String externalDocs = context.getMessages().findFirstString(req.getLocale(), "externalDocs");
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			Swagger s = getSwaggerFromFile(req);
			if (s != null)
				return s.getExternalDocs();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}
	
	private Operation getSwaggerOperationFromFile(Method method, RestRequest req) throws Exception {

		Swagger s = getSwaggerFromFile(req);
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
