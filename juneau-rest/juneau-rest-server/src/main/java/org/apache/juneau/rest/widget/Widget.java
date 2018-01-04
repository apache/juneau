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
package org.apache.juneau.rest.widget;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Defines an interface for resolvers of <js>"$W{...}"</js> string variables.
 *
 * <p>
 * Widgets are associated with resources through the following
 * <ul>
 * 	<li>{@link HtmlDoc#widgets() @HtmlDoc.widgets}
 * 	<li>{@link RestContextBuilder#widgets(Class...)}
 * 	<li>{@link RestContextBuilder#widgets(Widget...)}
 * </ul>
 *
 * <p>
 * Widgets allow you to add arbitrary HTML, CSS, and Javascript to the page.
 *
 * <p>
 * The HTML content returned by the {@link #getHtml(RestRequest)} method is added where the <js>"$W{...}"</js> is
 * referenced in the page.
 * The Javascript and stylesheet content is added to the header of the page.
 * They allow you to control the look and behavior of your widgets.
 *
 * <p>
 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
 * and aside section of the page:
 *
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		widgets={
 * 			MyWidget.<jk>class</jk>
 * 		}
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			navlinks={
 * 				<js>"$W{MyWidget}"</js>
 * 			},
 * 			aside={
 * 				<js>"Check out this widget:  $W{MyWidget}"</js>
 * 			}
 * 		)
 * 	)
 * </p>
 *
 * <p>
 * The following shows an example of a widget that renders an image located in the <code>htdocs</code> static files
 * directory in your classpath (see {@link RestResource#staticFiles()}):
 * <p class='bcode'>
 * 	<jk>public class</jk> MyWidget <jk>extends</jk> Widget {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getHtml(RestRequest req) <jk>throws</jk> Exception {
 * 			UriResolver r = req.getUriResolver();
 * 			<jk>return</jk> <js>"&lt;img class='myimage' onclick='myalert(this)' src='"</js>+r.resolve(<js>"servlet:/htdocs/myimage.png"</js>)+<js>"'&gt;"</js>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getScript(RestRequest req) <jk>throws</jk> Exception {
 * 			<jk>return</jk> <js>""</js>
 * 				+ <js>"\n function myalert(imageElement) {"</js>
 * 				+ <js>"\n 	alert('cool!');"</js>
 * 				+ <js>"\n }"</js>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getStyle(RestRequest req) <jk>throws</jk> Exception {
 * 			<jk>return</jk> <js>""</js>
 * 				+ <js>"\n .myimage {"</js>
 * 				+ <js>"\n 	border: 10px solid red;"</js>
 * 				+ <js>"\n }"</js>;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Note the {@link #getClasspathResourceAsString(String)} and {@link #getClasspathResourceAsString(String, Locale)} convenience methods
 * provided for quickly loading javascript and css files from the classpath or file system.
 * These are useful if your script or styles are complex and you want them loaded from files.
 *
 * <p>
 * <p class='bcode'>
 * 	<jk>public class</jk> MyWidget <jk>extends</jk> Widget {
 *
 * 		...
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getScript(RestRequest req) <jk>throws</jk> Exception {
 * 			<jk>return</jk> getResourceAsString(<js>"MyWidget.js"</js>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getStyle(RestRequest req) <jk>throws</jk> Exception {
 * 			<jk>return</jk> getResourceAsString(<js>"MyWidget.css"</js>);
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * Widgets must provide one of the following public constructors:
 * <ul>
 * 	<li><code><jk>public</jk> Widget();</code>
 * 	<li><code><jk>public</jk> Widget(PropertyStore);</code>
 * </ul>
 * 
 * <p>
 * Widgets can be defined as inner classes of REST resource classes.
 */
public abstract class Widget {

	private final ClasspathResourceManager rm = new ClasspathResourceManager(getClass());

	/**
	 * The widget key.
	 *
	 * <p>
	 * (i.e. The variable name inside the <js>"$W{...}"</js> variable).
	 *
	 * <p>
	 * The returned value must not be <jk>null</jk>.
	 *
	 * <p>
	 * If not overridden, the default value is the class simple name.
	 *
	 * @return The widget key.
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Resolves the HTML content for this widget.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The HTML content of this widget.
	 * @throws Exception
	 */
	public String getHtml(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Resolves any Javascript that should be added to the <xt>&lt;head&gt;/&lt;script&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The Javascript needed by this widget.
	 * @throws Exception
	 */
	public String getScript(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Resolves any CSS styles that should be added to the <xt>&lt;head&gt;/&lt;style&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The CSS styles needed by this widget.
	 * @throws Exception
	 */
	public String getStyle(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Retrieves the specified classpath resource and returns the contents as a string.
	 *
	 * <p>
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
	 * up the parent hierarchy chain.
	 *
	 * <p>
	 * If the resource cannot be found in the classpath, then an attempt is made to look relative to the JVM working directory.
	 * <br>Path traversals outside the working directory are not allowed for security reasons.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String getClasspathResourceAsString(String name) throws IOException {
		return rm.getString(name);
	}

	/**
	 * Same as {@link #getClasspathResourceAsString(String)} except also looks for localized-versions of the file.
	 *
	 * <p>
	 * If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
	 * <br>For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
	 * files in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String getClasspathResourceAsString(String name, Locale locale) throws IOException {
		return rm.getString(name, locale);
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips Javascript comments from
	 * the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadScript(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips CSS comments from
	 * the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadStyle(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips HTML comments from the
	 * file.
	 *
	 * <p>
	 * Comment are assumed to be <js>"<!-- -->"</js> code blocks.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadHtml(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
		return s;
	}
}
