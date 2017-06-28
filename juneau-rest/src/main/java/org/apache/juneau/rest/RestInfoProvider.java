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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Class that provides documentation and other related information about a REST resource.
 *
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST resources are documented.
 * Subclasses MUST implement a public constructor that takes in a {@link RestContext} object.
 *
 * <p>
 * RestInfoProviders are associated with servlets/resources in one of the following ways:
 * <ul>
 * 	<li>The {@link RestResource#infoProvider @RestResource.infoProvider()} annotation.
 * 	<li>The {@link RestConfig#setInfoProvider(Class)}/{@link RestConfig#setInfoProvider(RestInfoProvider)} methods.
 * </ul>
 */
@SuppressWarnings("hiding")
public class RestInfoProvider {

	private final RestContext context;
	private final String
		title,
		description,
		termsOfService,
		contact,
		license,
		version,
		tags,
		externalDocs;
	private final ConcurrentHashMap<Locale,Swagger> swaggers = new ConcurrentHashMap<Locale,Swagger>();

	/**
	 * Constructor.
	 *
	 * @param context The resource context.
	 */
	public RestInfoProvider(RestContext context) {
		this.context = context;

		Builder b = new Builder(context);
		this.title = b.title;
		this.description = b.description;
		this.termsOfService = b.termsOfService;
		this.contact = b.contact;
		this.license = b.license;
		this.version = b.version;
		this.tags = b.tags;
		this.externalDocs = b.externalDocs;
	}

	private static class Builder {
		private String
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
	 * Returns the localized swagger for this REST resource.
	 *
	 * @param req The incoming HTTP request.
	 * @return A new Swagger instance.
	 * @throws RestException
	 */
	protected Swagger getSwagger(RestRequest req) throws RestException {
		try {
			// If a file is defined, use that.
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
				return s;

			s = swagger(
				info(getTitle(req), getVersion(req))
					.contact(getContact(req))
					.license(getLicense(req))
					.description(getDescription(req))
					.termsOfService(getTermsOfService(req))
				)
				.consumes(context.getSupportedAcceptTypes())
				.produces(context.getSupportedContentTypes())
				.tags(getTags(req))
				.externalDocs(getExternalDocs(req));

			for (CallMethod sm : context.getCallMethods().values()) {
				if (sm.isRequestAllowed(req)) {
					Operation o = sm.getSwaggerOperation(req);
					s.path(
						sm.getPathPattern(),
						sm.getHttpMethod().toLowerCase(),
						o
					);
				}
			}
			return s;
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
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
	 * Returned objects are cached for later quick-lookup.
	 *
	 * @param locale The locale of the swagger.
	 * @return The parsed swagger object, or <jk>null</jk> if the swagger file could not be found.
	 * @throws RestException
	 */
	protected Swagger getSwaggerFromFile(Locale locale) throws RestException {
		Swagger s = swaggers.get(locale);
		if (s == null) {
			try {
				s = context.getResource(Swagger.class, MediaType.JSON, getClass().getSimpleName() + ".json", locale);
				swaggers.putIfAbsent(locale, s == null ? Swagger.NULL : s);
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		}
		return s == Swagger.NULL ? null : s;
	}

	/**
	 * Returns the localized summary of the specified java method on this servlet.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own summary.
	 *
	 * <p>
	 * The default implementation returns the summary from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestMethod#summary() @RestMethod.summary()} annotation on the method.
	 * 	<li><ck>[ClassName].[javaMethodName].summary</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>[javaMethodName].summary</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * </ol>
	 *
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized summary of the method, or a blank string if no summary was found.
	 */
	public String getMethodSummary(String javaMethodName, RestRequest req) {
		CallMethod m = context.getCallMethods().get(javaMethodName);
		if (m != null)
			return m.getSummary(req);
		return "";
	}

	/**
	 * Returns the localized description of the specified java method on this servlet.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own description.
	 *
	 * <p>
	 * The default implementation returns the description from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestMethod#description() @RestMethod.description()} annotation on the method.
	 * 	<li><ck>[ClassName].[javaMethodName].description</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>[javaMethodName].description</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * </ol>
	 *
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized description of the method, or a blank string if no description was found.
	 */
	protected String getMethodDescription(String javaMethodName, RestRequest req) {
		CallMethod m = context.getCallMethods().get(javaMethodName);
		if (m != null)
			return m.getDescription(req);
		return "";
	}

	/**
	 * Returns the localized title of this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own title.
	 *
	 * <p>
	 * The default implementation returns the description from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#title() @RestResourcel.title()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].title</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @ResourceBundle.messages()} annotation for this class, then any parent
	 * 		classes.
	 * 	<li><ck>title</ck> in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/title</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getTitle(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (this.title != null)
			return vr.resolve(this.title);
		String title = context.getMessages().findFirstString(req.getLocale(), "title");
		if (title != null)
			return vr.resolve(title);
		Swagger s = req.getSwaggerFromFile();
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
	 * The default implementation returns the description from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#description() @RestResource.description()} annotation on this class, and then any
	 * 		parent classes.
	 * 	<li><ck>[ClassName].description</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>description</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/description</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getDescription(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (this.description != null)
			return vr.resolve(this.description);
		String description = context.getMessages().findFirstString(req.getLocale(), "description");
		if (description != null)
			return vr.resolve(description);
		Swagger s = req.getSwaggerFromFile();
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
	 * The default implementation returns the contact information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#contact() @ResourceSwagger.contact()} annotation on this class, and then any parent
	 * 		classes.
	 * 	<li><ck>[ClassName].contact</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>contact</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/contact</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public Contact getContact(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (this.contact != null)
				return jp.parse(vr.resolve(this.contact), Contact.class);
			String contact = context.getMessages().findFirstString(req.getLocale(), "contact");
			if (contact != null)
				return jp.parse(vr.resolve(contact), Contact.class);
			Swagger s = req.getSwaggerFromFile();
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
	 * The default implementation returns the license information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#license() @ResourceSwagger.license()} annotation on this class, and then any parent
	 * 		classes.
	 * 	<li><ck>[ClassName].license</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>license</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/license</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public License getLicense(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (this.license != null)
				return jp.parse(vr.resolve(this.license), License.class);
			String license = context.getMessages().findFirstString(req.getLocale(), "license");
			if (license != null)
				return jp.parse(vr.resolve(license), License.class);
			Swagger s = req.getSwaggerFromFile();
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
	 * The default implementation returns the terms-of-service information from the following locations (whichever
	 * matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#termsOfService() @ResourceSwagger.termsOfService()} annotation on this class, and
	 * 		then any parent classes.
	 * 	<li><ck>[ClassName].termsOfService</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>termsOfService</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/termsOfService</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getTermsOfService(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (this.termsOfService != null)
			return vr.resolve(this.termsOfService);
		String termsOfService = context.getMessages().findFirstString(req.getLocale(), "termsOfService");
		if (termsOfService != null)
			return vr.resolve(termsOfService);
		Swagger s = req.getSwaggerFromFile();
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
	 * The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#version() @ResourceSwagger.version()} annotation on this class, and then any parent
	 * 		classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getVersion(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (this.version != null)
			return vr.resolve(this.version);
		String version = context.getMessages().findFirstString(req.getLocale(), "version");
		if (version != null)
			return vr.resolve(version);
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getInfo() != null)
			return s.getInfo().getVersion();
		return null;
	}

	/**
	 * Returns the version information of this REST resource.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own version information.
	 *
	 * <p>
	 * The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#version() @ResourceSwagger.version()} annotation on this class, and then any parent
	 * 		classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public List<Tag> getTags(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (this.tags != null)
				return jp.parse(vr.resolve(this.tags), ArrayList.class, Tag.class);
			String tags = context.getMessages().findFirstString(req.getLocale(), "tags");
			if (tags != null)
				return jp.parse(vr.resolve(tags), ArrayList.class, Tag.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
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
	 * The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link ResourceSwagger#version() @ResourceSwagger.version()} annotation on this class, and then any parent
	 * 		classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by
	 * 		{@link RestResource#messages() @RestResource.messages()} annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public ExternalDocumentation getExternalDocs(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (this.externalDocs != null)
				return jp.parse(vr.resolve(this.externalDocs), ExternalDocumentation.class);
			String externalDocs = context.getMessages().findFirstString(req.getLocale(), "externalDocs");
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
				return s.getExternalDocs();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}
}
