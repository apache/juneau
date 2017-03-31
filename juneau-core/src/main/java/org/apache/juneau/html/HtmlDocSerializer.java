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
 * Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>, <code><xt>&lt;head&gt;</code>,
 * 	and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
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
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(PropertyStore.create());


	final HtmlDocSerializerContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public HtmlDocSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(HtmlDocSerializerContext.class);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new HtmlDocSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType);
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
		String description = s.getText();
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

	private static boolean isEmptyList(Object o) {
		if (o == null)
			return false;
		if (o instanceof Collection && ((Collection<?>)o).size() == 0)
			return true;
		if (o.getClass().isArray() && Array.getLength(o) == 0)
			return true;
		return false;
	}
}
