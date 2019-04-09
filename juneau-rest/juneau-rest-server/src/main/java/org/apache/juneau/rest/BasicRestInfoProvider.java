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
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Default implementation of {@link RestInfoProvider}.
 *
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST resources are documented.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
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
			ClassInfo ci = getClassInfo(context.getResource().getClass());
			for (RestResource r : ci.getAnnotationsParentFirst(RestResource.class)) {
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
		swagger = new SwaggerGenerator(req).getSwagger();

		swaggers.get(locale).put(hashCode, swagger);

		return swagger;
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
	 * 	<li>{@link RestMethod#summary() @RestMethod(summary)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(summary=<js>"Summary of my method"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(summary=<js>"$L{myLocalizedSummary}"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource(messages)}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].summary</ck>
	 * 			<li><ck>[javaMethodName].summary</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestMethod#summary() @RestMethod(summary)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
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

		String s = getMethodInfo(method).getAnnotation(RestMethod.class).summary();
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
	 * 	<li>{@link RestMethod#description() @RestMethod(description)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<cc>// Direct value</cc>
	 * 	<ja>@RestMethod</ja>(description=<js>"Description of my method"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 *
	 * 	<cc>// Pulled from some other location</cc>
	 * 	<ja>@RestMethod</ja>(description=<js>"$L{myLocalizedDescription}"</js>)
	 * 	<jk>public</jk> Object myMethod() {...}
	 * 		</p>
	 * 	<li>Localized string from resource bundle identified by {@link RestResource#messages() @RestResource(messages)}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].[javaMethodName].description</ck>
	 * 			<li><ck>[javaMethodName].description</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestMethod#description() @RestMethod(description)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
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

		String s = joinnl(getMethodInfo(method).getAnnotation(RestMethod.class).description());
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
	 * 	<li>{@link RestResource#siteName() @RestResource(siteName)} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(siteName=<js>"My Site"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(siteName=<js>"$L{myLocalizedSiteName}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource(messages)}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].siteName</ck>
	 * 			<li><ck>siteName</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#siteName() @RestResource(siteName)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
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
	 * 	<li>{@link RestResource#title() @RestResource(siteName)} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(title=<js>"My Resource"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(title=<js>"$L{myLocalizedTitle}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource(messages)}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].title</ck>
	 * 			<li><ck>title</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#title() @RestResource(title)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
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
	 * 	<li>{@link RestResource#description() @RestResource(description)} annotation on this class, and then any parent classes.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jc>// Direct value</jc>
	 * 	<ja>@RestResource</ja>(description=<js>"My Resource"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 	<jc>// Pulled from some other location</jc>
	 * 	<ja>@RestResource</ja>(description=<js>"$L{myLocalizedDescription}"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 * 		</p>
	 * 	<li>Localized strings from resource bundle identified by {@link RestResource#messages() @RestResource(messages)}
	 * 		on the resource class, then any parent classes.
	 * 		<ol>
	 * 			<li><ck>[ClassName].description</ck>
	 * 			<li><ck>description</ck>
	 * 		</ol>
	 * 		<br>Value can contain any SVL variables defined on the {@link RestResource#description() @RestResource(description)} annotation.
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
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
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private Operation getSwaggerOperation(Method method, RestRequest req) throws Exception {

		Swagger s = getSwagger(req);
		if (s != null) {
			Map<String,OperationMap> sp = s.getPaths();
			if (sp != null) {
				Map<String,Operation> spp = sp.get(fixMethodPath(getMethodInfo(method).getAnnotation(RestMethod.class).path()));
				if (spp != null)
					return spp.get(req.getMethod());
			}
		}
		return null;
	}

	static String joinnl(String[] ss) {
		if (ss.length == 0)
			return "";
		return StringUtils.joinnl(ss).trim();
	}
}
