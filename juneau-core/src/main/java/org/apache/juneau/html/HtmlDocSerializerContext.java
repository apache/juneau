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

import java.util.*;

import org.apache.juneau.*;

/**
 * Properties associated with the {@link HtmlDocSerializer} class.
 * <p>
 * 	These are typically specified via <ja>@RestResource.properties()</ja> and <ja>@RestMethod.properties()</ja> annotations,
 * 		although they can also be set programmatically via the <code>RestResponse.setProperty()</code> method.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		messages=<js>"nls/AddressBookResource"</js>,
 * 		properties={
 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_title</jsf>, value=<js>"$L{title}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_description</jsf>, value=<js>"$L{description}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_links</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
 * 		}
 * 	)
 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> RestServletJenaDefault {
 * </p>
 * <p>
 * 	The <code>$L{...}</code> variable represent localized strings pulled from the resource bundle identified by the <code>messages</code> annotation.
 * 	These variables are replaced at runtime based on the HTTP request locale.
 * 	Several built-in runtime variable types are defined, and the API can be extended to include user-defined variables.
 * </p>
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the HTML document serializer</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_title}</td>
 * 		<td>Page title.</td>
 * 		<td><code>String</code></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_description}</td>
 * 		<td>Page description.</td>
 * 		<td><code>String</code></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_links}<br>{@link #HTMLDOC_links_put}</td>
 * 		<td>Page links.</td>
 * 		<td><code>Map&lt;String,String&gt;</code></td>
 * 		<td>empty map</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_cssUrl}</td>
 * 		<td>Stylesheet URL.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>"style.css"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_cssImports}<br>{@link #HTMLDOC_cssImports_add}</td>
 * 		<td>CSS imports.</td>
 * 		<td><code>List&lt;String&gt;</code></td>
 * 		<td>empty list</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTMLDOC_nowrap}</td>
 * 		<td>Prevent word wrap on page.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 		<ul>
 * 			<li class='c'><a class='doclink' href='../html/HtmlSerializerContext.html#ConfigProperties'>HtmlSerializerContext</a> - Configurable properties on the HTML serializer.
 * 		</ul>
 * 	</ul>
 * </ul>
 */
public final class HtmlDocSerializerContext extends HtmlSerializerContext {

	/**
	 * <b>Configuration property:</b>  Page title.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.title"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * </ul>
	 * <p>
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_title</jsf>, value=<js>"$L{title}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> RestServletJenaDefault {
	 * </p>
	 * <p>
	 * 	...with this property in <code>AddressBookResource.properties</code>...
	 * </p>
	 * <p class='bcode'>
	 * 	title = <js>AddressBook sample resource</js>
	 * </p>
	 * <p>
	 * 	...to produce this title on the HTML page...
	 * </p>
	 * <img class='bordered' src='doc-files/HTML_TITLE.png'>
	 */
	public static final String HTMLDOC_title = "HtmlSerializer.title";

	/**
	 * <b>Configuration property:</b>  Page description.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.description"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * </ul>
	 * <p>
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_description</jsf>, value=<js>"description"</js>, type=<jsf>NLS</jsf>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> RestServletJenaDefault {
	 * </p>
	 * <p>
	 * 	...with this property in <code>AddressBookResource.properties</code>...
	 * </p>
	 * <p class='bcode'>
	 * 	description = <js>Simple address book POJO sample resource</js>
	 * </p>
	 * <p>
	 * 	...to produce this description on the HTML page...
	 * </p>
	 *	<img class='bordered' src='doc-files/HTML_DESCRIPTION.png'>
	 */
	public static final String HTMLDOC_description = "HtmlSerializer.description";

	/**
	 * <b>Configuration property:</b>  Page links.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlDocSerializer.links.map"</js>
	 * 	<li><b>Data type:</b> <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b> empty map
	 * </ul>
	 * <p>
	 * Adds a list of hyperlinks immediately under the title and description but above the content of the page.
	 * <p>
	 * 	This can be used to provide convenient hyperlinks when viewing the REST interface from a browser.
	 * <p>
	 * 	The value is a JSON object string where the keys are anchor text and the values are URLs.
	 * <p>
	 * 	Relative URLs are considered relative to the servlet path.
	 * 	For example, if the servlet path is <js>"http://localhost/myContext/myServlet"</js>, and the
	 * 		URL is <js>"foo"</js>, the link becomes <js>"http://localhost/myContext/myServlet/foo"</js>.
	 * 	Absolute (<js>"/myOtherContext/foo"</js>) and fully-qualified (<js>"http://localhost2/foo"</js>) URLs
	 * 		can also be used.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerContext.<jsf>HTMLDOC_links</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> RestServletJenaDefault {
	 * </p>
	 * <p>
	 * 	...to produce this list of links on the HTML page...
	 * </p>
	 * <img class='bordered' src='doc-files/HTML_LINKS.png'>
	 */
	public static final String HTMLDOC_links = "HtmlDocSerializer.links.map";

	/**
	 * <b>Configuration property:</b>  Add to the {@link #HTMLDOC_links} property.
	 */
	public static final String HTMLDOC_links_put = "HtmlDocSerializer.links.map.put";

	/**
	 * <b>Configuration property:</b>  Stylesheet URL.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlDocSerializer.cssUrl"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"style.css"</js>
	 * </ul>
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 * <p>
	 * 	If not specified, defaults to the built-in stylesheet located at <js>"style.css"</js>.
	 * 	Note that this stylesheet is controlled by the <code><ja>@RestResource</ja>.stylesheet()</code> annotation.
	 */
	public static final String HTMLDOC_cssUrl = "HtmlDocSerializer.cssUrl";

	/**
	 * <b>Configuration property:</b>  CSS imports.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlDocSerializer.cssImports.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * </ul>
	 * <p>
	 * Imports the specified CSS page URLs into the page.
	 */
	public static final String HTMLDOC_cssImports = "HtmlDocSerializer.cssImports.list";

	/**
	 * <b>Configuration property:</b>  Add to the {@link #HTMLDOC_cssImports} property.
	 */
	public static final String HTMLDOC_cssImports_add = "HtmlDocSerializer.cssImports.list.add";

	/**
	 * <b>Configuration property:</b>  Prevent word wrap on page.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlDocSerializer.nowrap"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the style header to prevent word wrapping.
	 */
	public static final String HTMLDOC_nowrap = "HtmlDocSerializer.nowrap";

	final String[] cssImports;
	final Map<String,String> links;
	final String title, description, cssUrl;
	final boolean nowrap;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public HtmlDocSerializerContext(ContextFactory cf) {
		super(cf);
		cssImports = cf.getProperty(HTMLDOC_cssImports, String[].class, new String[0]);
		title = cf.getProperty(HTMLDOC_title, String.class, null);
		description = cf.getProperty(HTMLDOC_description, String.class, null);
		cssUrl = cf.getProperty(HTMLDOC_cssUrl, String.class, null);
		nowrap = cf.getProperty(HTMLDOC_nowrap, boolean.class, false);
		links = cf.getMap(HTMLDOC_links, String.class, String.class, Collections.<String,String>emptyMap());
	}
}
