/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.dto.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>, <code><xt>&lt;head&gt;</code>,
 * 	and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link HtmlDocSerializerProperties}
 * 	<li>{@link HtmlSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces("text/html")
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	// Properties defined in RestServletProperties
	private static final String
		REST_method = "RestServlet.method",
		REST_relativeServletURI = "RestServlet.relativeServletURI";


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {

		HtmlSerializerContext hctx = (HtmlSerializerContext)ctx;
		HtmlSerializerWriter w = hctx.getWriter(out);

		ObjectMap properties = hctx.getProperties();

		boolean isOptionsPage = properties.containsKey(REST_method) && properties.getString(REST_method).equalsIgnoreCase("OPTIONS");

		// Render the header.
		w.sTag("html").nl();
		w.sTag("head").nl();

		String cssUrl = hctx.getCssUrl();
		if (cssUrl == null)
			cssUrl = properties.getString(REST_relativeServletURI) + "/style.css";

		w.oTag(1, "style")
			.attr("type", "text/css")
			.appendln(">")
			.append(2, "@import ").q().append(cssUrl).q().appendln(";");
		if (hctx.isNoWrap())
			w.appendln("\n* {white-space:nowrap;}");
		if (hctx.getCssImports() != null)
			for (String cssImport : hctx.getCssImports())
				w.append(2, "@import ").q().append(cssImport).q().appendln(";");
		w.eTag(1, "style").nl();
		w.eTag("head").nl();
		w.sTag("body").nl();
		// Write the title of the page.
		String title = hctx.getTitle();
		if (title == null && isOptionsPage)
			title = "Options";
		String description = hctx.getDescription();
		if (title != null)
			w.oTag(1, "h3").attr("class", "title").append('>').encodeText(title).eTag("h3").nl();
		if (description != null)
			w.oTag(1, "h5").attr("class", "description").append('>').encodeText(description).eTag("h5").nl();

		// Write the action links that render above the results.
		List<Link> actions = new LinkedList<Link>();

		// If this is an OPTIONS request, provide a 'back' link to return to the GET request page.
		if (! isOptionsPage) {
			ObjectMap htmlLinks = hctx.getLinks();
			if (htmlLinks != null) {
				for (Map.Entry<String,Object> e : htmlLinks.entrySet()) {
					String uri = e.getValue().toString();
					if (uri.indexOf("://") == -1 && ! StringUtils.startsWith(uri, '/')) {
						StringBuilder sb = new StringBuilder(properties.getString(REST_relativeServletURI));
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

		hctx.indent = 3;

		// To allow for page formatting using CSS, we encapsulate the data inside two div tags:
		// <div class='outerdata'><div class='data' id='data'>...</div></div>
		w.oTag(1, "div").attr("class","outerdata").append('>').nl();
		w.oTag(2, "div").attr("class","data").attr("id", "data").append('>').nl();
		if (isEmptyList(o))
			w.oTag(3, "p").append('>').append("no results").eTag("p");
		else
			super.doSerialize(o, w, hctx);
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
}
