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
package org.apache.juneau.html;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>, <code><xt>&lt;head&gt;</code>,
 * 	and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link HtmlDocSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces("text/html")
@SuppressWarnings("hiding")
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	// Properties defined in RestServletProperties
	private static final String
		REST_method = "RestServlet.method",
		REST_relativeServletURI = "RestServlet.relativeServletURI";


	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer().lock();


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new HtmlDocSerializerSession(getContext(HtmlDocSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {

		HtmlDocSerializerSession s = (HtmlDocSerializerSession)session;
		HtmlWriter w = s.getWriter();

		ObjectMap op = s.getProperties();

		boolean isOptionsPage = op.containsKey(REST_method) && op.getString(REST_method).equalsIgnoreCase("OPTIONS");

		// Render the header.
		w.sTag("html").nl();
		w.sTag("head").nl();

		String cssUrl = s.getCssUrl();
		if (cssUrl == null)
			cssUrl = op.getString(REST_relativeServletURI) + "/style.css";

		w.oTag(1, "style")
			.attr("type", "text/css")
			.appendln(">")
			.append(2, "@import ").q().append(cssUrl).q().appendln(";");
		if (s.isNoWrap())
			w.appendln("\n* {white-space:nowrap;}");
		if (s.getCssImports() != null)
			for (String cssImport : s.getCssImports())
				w.append(2, "@import ").q().append(cssImport).q().appendln(";");
		w.eTag(1, "style").nl();
		w.eTag("head").nl();
		w.sTag("body").nl();
		// Write the title of the page.
		String title = s.getTitle();
		if (title == null && isOptionsPage)
			title = "Options";
		String description = s.getDescription();
		if (title != null)
			w.oTag(1, "h3").attr("class", "title").append('>').text(title).eTag("h3").nl();
		if (description != null)
			w.oTag(1, "h5").attr("class", "description").append('>').text(description).eTag("h5").nl();

		// Write the action links that render above the results.
		List<Link> actions = new LinkedList<Link>();

		// If this is an OPTIONS request, provide a 'back' link to return to the GET request page.
		if (! isOptionsPage) {
			Map<String,String> htmlLinks = s.getLinks();
			if (htmlLinks != null) {
				for (Map.Entry<String,String> e : htmlLinks.entrySet()) {
					String uri = e.getValue();
					if (uri.indexOf("://") == -1 && ! StringUtils.startsWith(uri, '/')) {
						StringBuilder sb = new StringBuilder(op.getString(REST_relativeServletURI));
						if (! (uri.isEmpty() || uri.charAt(0) == '?' || uri.charAt(0) == '/'))
							sb.append('/');
						sb.append(uri);
						uri = sb.toString();
					}

					actions.add(new Link(e.getKey(), uri));
				}
			}
		}

		if (actions.size() > 0) {
			w.oTag(1, "p").attr("class", "links").append('>').nl();
			for (Iterator<Link> i = actions.iterator(); i.hasNext();) {
				Link h = i.next();
				w.oTag(2, "a").attr("class", "link").attr("href", h.getHref(), true).append('>').append(h.getName()).eTag("a").nl();
				if (i.hasNext())
					w.append(3, " - ").nl();
			}
			w.eTag(1, "p").nl();
		}

		s.indent = 3;

		// To allow for page formatting using CSS, we encapsulate the data inside two div tags:
		// <div class='outerdata'><div class='data' id='data'>...</div></div>
		w.oTag(1, "div").attr("class","outerdata").append('>').nl();
		w.oTag(2, "div").attr("class","data").attr("id", "data").append('>').nl();
		if (isEmptyList(o))
			w.oTag(3, "p").append('>').append("no results").eTag("p");
		else
			super.doSerialize(s, o);
		w.eTag(2, "div").nl();
		w.eTag(1, "div").nl();

		w.eTag("body").nl().eTag("html").nl();
	}

	private boolean isEmptyList(Object o) {
		if (o == null)
			return false;
		if (o instanceof Collection && ((Collection<?>)o).size() == 0)
			return true;
		if (o.getClass().isArray() && Array.getLength(o) == 0)
			return true;
		return false;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	@Override /* HtmlSerializer */
	public HtmlDocSerializer setUriAnchorText(String value) throws LockedException {
		super.setUriAnchorText(value);
		return this;
	}

	@Override /* HtmlSerializer */
	public HtmlDocSerializer setDetectLinksInStrings(boolean value) throws LockedException {
		super.setDetectLinksInStrings(value);
		return this;
	}

	@Override /* HtmlSerializer */
	public HtmlDocSerializer setLookForLabelParameters(boolean value) throws LockedException {
		super.setLookForLabelParameters(value);
		return this;
	}

	@Override /* HtmlSerializer */
	public HtmlDocSerializer setLabelParameter(String value) throws LockedException {
		super.setLabelParameter(value);
		return this;
	}

	@Override /* HtmlSerializer */
	public HtmlDocSerializer setAddKeyValueTableHeaders(boolean value) throws LockedException {
		super.setAddKeyValueTableHeaders(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setUseWhitespace(boolean value) throws LockedException {
		super.setUseWhitespace(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public HtmlDocSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlDocSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}

	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public HtmlDocSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public HtmlDocSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public HtmlDocSerializer clone() {
		HtmlDocSerializer c = (HtmlDocSerializer)super.clone();
		return c;
	}
}
