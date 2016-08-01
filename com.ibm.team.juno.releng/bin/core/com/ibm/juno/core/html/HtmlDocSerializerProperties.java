/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;


/**
 * Properties associated with the {@link HtmlDocSerializer} class.
 * <p>
 * 	These are typically specified via <ja>@RestResource.properties()</ja> and <ja>@RestMethod.properties()</ja> annotations,
 * 		although they can also be set programmatically via the <code>RestREsponse.setProperty()</code> method.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		messages=<js>"nls/AddressBookResource"</js>,
 * 		properties={
 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_title</jsf>, value=<js>"$L{title}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_description</jsf>, value=<js>"$L{description}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_links</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
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
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class HtmlDocSerializerProperties {

	/**
	 * Adds a title at the top of a page.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_title</jsf>, value=<js>"$L{title}"</js>)
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
	 * 		<img class='bordered' src='doc-files/HTML_TITLE.png'>
	 * 	</dd>
	 * </dl>
	 */
	public static final String HTMLDOC_title = "HtmlSerializer.title";

	/**
	 * Adds a description right below the title of a page.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_description</jsf>, value=<js>"description"</js>, type=<jsf>NLS</jsf>)
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
	 * 		<img class='bordered' src='doc-files/HTML_DESCRIPTION.png'>
	 * 	</dd>
	 * </dl>
	 */
	public static final String HTMLDOC_description = "HtmlSerializer.description";

	/**
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
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p>
	 * 	The <code>AddressBookResource</code> sample class uses this property...
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/AddressBookResource"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializerProperties.<jsf>HTMLDOC_links</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> RestServletJenaDefault {
	 * </p>
	 * <p>
	 * 	...to produce this list of links on the HTML page...
	 * </p>
	 * 		<img class='bordered' src='doc-files/HTML_LINKS.png'>
	 * 	</dd>
	 * </dl>
	 */
	public static final String HTMLDOC_links = "HtmlDocSerializer.links";

	/**
	 * Similar to {@link #HTMLDOC_links} except appends on to the existing list of links.
	 */
	public static final String HTMLDOC_addLinks = "HtmlDocSerializer.addLinks";

	/**
	 * Adds a link to the specified stylesheet URL.
	 * <p>
	 * 	If not specified, defaults to the built-in stylesheet located at <js>"/servletPath/style.css"</js>.
	 * 	Note that this stylesheet is controlled by the <code><ja>@RestResource</ja>.style()</code> annotation.
	 */
	public static final String HTMLDOC_cssUrl = "HtmlDocSerializer.cssUrl";

	/**
	 * Imports the specified CSS page URLs into the page.
	 */
	public static final String HTMLDOC_cssImports = "HtmlDocSerializer.cssImports";

	/**
	 * Adds <js>"* {white-space:nowrap}"</js> to the style header to prevent word wrapping.
	 */
	public static final String HTMLDOC_nowrap = "HtmlDocSerializer.nowrap";
}
