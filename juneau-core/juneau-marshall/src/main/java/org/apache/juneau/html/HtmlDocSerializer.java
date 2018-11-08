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

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Accept</code> types:  <code><b>text/html</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>text/html</b></code>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>,
 * <code><xt>&lt;head&gt;</code>, and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <p>
 * Configurable properties are typically specified via <ja>@RestResource(properties)</ja> and <ja>@RestMethod(properties)</ja>
 * annotations, although they can also be set programmatically via the <code>RestResponse.setProperty()</code> method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestResource</ja>(
 * 		messages=<js>"nls/AddressBookResource"</js>,
 * 		properties={
 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_title</jsf>, value=<js>"$L{title}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_description</jsf>, value=<js>"$L{description}"</js>),
 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_navlinks</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
 * 		}
 * 	)
 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
 * </p>
 *
 * <p>
 * Note that shortcut annotations are also provided for these particular settings:
 * <p class='bcode w800'>
 * 	<ja>@RestResource</ja>(
 * 		messages=<js>"nls/AddressBookResource"</js>,
 * 		title=<js>"$L{title}"</js>,
 * 		description=<js>"$L{description}"</js>,
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			navlinks={
 * 				<js>"options: ?method=OPTIONS"</js>,
 * 				<js>"doc: doc"</js>
 * 			}
 * 		)
 * 	)
 * </p>
 *
 * <p>
 * The <code>$L{...}</code> variable represent localized strings pulled from the resource bundle identified by the
 * <code>messages</code> annotation.
 * <br>These variables are replaced at runtime based on the HTTP request locale.
 * <br>Several built-in runtime variable types are defined, and the API can be extended to include user-defined variables.
 */
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "HtmlDocSerializer.";

	/**
	 * Configuration property:  Aside section contents.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.aside.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to specify the contents of the aside section on the HTML page.
	 * The aside section floats on the right of the page for providing content supporting the serialized content of
	 * the page.
	 *
	 * <p>
	 * By default, the aside section is empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			aside={
	 * 				<js>"&lt;ul&gt;"</js>,
	 * 				<js>"	&lt;li&gt;Item 1"</js>,
	 * 				<js>"	&lt;li&gt;Item 2"</js>,
	 * 				<js>"	&lt;li&gt;Item 3"</js>,
	 * 				<js>"&lt;/ul&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_aside = PREFIX + "aside.ls";

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.footer.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to specify the contents of the footer section on the HTML page.
	 *
	 * <p>
	 * By default, the footer section is empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			footer={
	 * 				<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_footer = PREFIX + "footer.ls";

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.head.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_links</jsf>,
	 * 				value=<js>"['&lt;link rel=\"icon\" href=\"htdocs/mypageicon.ico\"&gt;']"</js>)
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * A shortcut on <ja>@RestResource</ja> is also provided for this setting:
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=@HtmlDoc(
	 * 			head={
	 * 				<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_head = PREFIX + "head.ls";

	/**
	 * Configuration property:  Header section contents.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to override the contents of the header section on the HTML page.
	 * The header section normally contains the title and description at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			header={
	 * 				<js>"&lt;h1&gt;My own header&lt;/h1&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_header = PREFIX + "header.ls";

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.nav.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to override the contents of the nav section on the HTML page.
	 * The nav section normally contains the page links at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			nav={
	 * 				<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <p>
	 * When this property is specified, the {@link #HTMLDOC_navlinks} property is ignored.
	 */
	public static final String HTMLDOC_nav = PREFIX + "nav.ls";

	/**
	 * Configuration property:  Page navigation links.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.navlinks.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds a list of hyperlinks immediately under the title and description but above the content of the page.
	 *
	 * <p>
	 * This can be used to provide convenient hyperlinks when viewing the REST interface from a browser.
	 *
	 * <p>
	 * The value is an array of strings with two possible values:
	 * <ul>
	 * 	<li>A key-value pair representing a hyperlink label and href:
	 * 		<br><js>"google: http://google.com"</js>
	 * 	<li>Arbitrary HTML.
	 * </ul>
	 *
	 * <p>
	 * Relative URLs are considered relative to the servlet path.
	 * For example, if the servlet path is <js>"http://localhost/myContext/myServlet"</js>, and the
	 * URL is <js>"foo"</js>, the link becomes <js>"http://localhost/myContext/myServlet/foo"</js>.
	 * Absolute (<js>"/myOtherContext/foo"</js>) and fully-qualified (<js>"http://localhost2/foo"</js>) URLs
	 * can also be used in addition to various other protocols specified by {@link UriResolver} such as
	 * <js>"servlet:/..."</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p>
	 * The <code>AddressBookResource</code> sample class uses this property...
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_navlinks</jsf>,
	 * 				value=<js>"['options: ?method=OPTIONS', 'doc: doc']"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
	 * </p>
	 *
	 * <p>
	 * A shortcut on <ja>@RestResource</ja> is also provided for this setting:
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=@HtmlDoc(
	 * 			navlinks={
	 * 				<js>"options: ?method=OPTIONS"</js>,
	 * 				<js>"doc: doc"</js>
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
	 * </p>
	 */
	public static final String HTMLDOC_navlinks = PREFIX + "navlinks.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_navlinks} property.
	 */
	public static final String HTMLDOC_navlinks_add = PREFIX + "navlinks.ls/add";

	/**
	 * Configuration property:  No-results message.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.noResultsMessage.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"&lt;p&gt;no results&lt;/p&gt;"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			noResultsMessage=<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to represent no value to differentiate it from an empty string.
	 */
	public static final String HTMLDOC_noResultsMessage = PREFIX + "noResultsMessage.s";

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.nowrap.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 */
	public static final String HTMLDOC_nowrap = PREFIX + "nowrap.b";

	/**
	 * Configuration property:  Javascript code.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.script.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_script</jsf>,
	 * 				value=<js>"alert('hello!');"</js>)
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * A shortcut on <ja>@RestResource</ja> is also provided for this setting:
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=@HtmlDoc(
	 * 			script={
	 * 				<js>"alert('hello!');"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_script = PREFIX + "script.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_script} property.
	 */
	public static final String HTMLDOC_script_add = PREFIX + "script.ls/add";

	/**
	 * Configuration property:  CSS style code.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.style.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_style</jsf>,
	 * 				value=<js>"h3 { color: red; }\nh5 { font-weight: bold; }"</js>)
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * A shortcut on <ja>@RestResource</ja> is also provided for this setting:
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=@HtmlDoc(
	 * 			style={
	 * 				<js>"h3 { color: red; }"</js>,
	 * 				<js>"h5 { font-weight: bold; }"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_style = PREFIX + "style.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_style} property.
	 */
	public static final String HTMLDOC_style_add = PREFIX + "style.ls/add";

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.stylesheet.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <p>
	 * Note that this stylesheet is controlled by the <code><ja>@RestResource</ja>.stylesheet()</code> annotation.
	 */
	public static final String HTMLDOC_stylesheet = PREFIX + "stylesheet.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_stylesheet} property.
	 */
	public static final String HTMLDOC_stylesheet_add = PREFIX + "stylesheet.ls/add";

	/**
	 * Configuration property:  HTML document template.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.template.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> HtmlDocTemplate&gt;</code>
	 * 	<li><b>Default:</b>  <code>HtmlDocTemplateBasic.<jk>class</jk></code>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the template to use for serializing the page.
	 *
	 * <p>
	 * By default, the {@link BasicHtmlDocTemplate} class is used to construct the contents of the HTML page, but
	 * can be overridden with your own custom implementation class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=@HtmlDoc(
	 * 			template=MySpecialDocTemplate.<jk>class</jk>
	 * 		)
	 * 	)
	 * </p>
	 */
	public static final String HTMLDOC_template = PREFIX + "template.c";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String[] style, stylesheet, script, navlinks, head, header, nav, aside, footer;
	private final String noResultsMessage;
	private final boolean nowrap;
	private final HtmlDocTemplate template;

	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public HtmlDocSerializer(PropertyStore ps) {
		this(ps, "text/html", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	{@doc RFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json",text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public HtmlDocSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
		style = getArrayProperty(HTMLDOC_style, String.class);
		stylesheet = getArrayProperty(HTMLDOC_stylesheet, String.class);
		script = getArrayProperty(HTMLDOC_script, String.class);
		head = getArrayProperty(HTMLDOC_head, String.class);
		header = getArrayProperty(HTMLDOC_header, String.class);
		nav = getArrayProperty(HTMLDOC_nav, String.class);
		aside = getArrayProperty(HTMLDOC_aside, String.class);
		footer = getArrayProperty(HTMLDOC_footer, String.class);
		nowrap = getBooleanProperty(HTMLDOC_nowrap, false);
		navlinks = getArrayProperty(HTMLDOC_navlinks, String.class);
		noResultsMessage = getStringProperty(HTMLDOC_noResultsMessage, "<p>no results</p>");
		template = getInstanceProperty(HTMLDOC_template, HtmlDocTemplate.class, BasicHtmlDocTemplate.class);
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlDocSerializerSession(this, args);
	}

	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = builder().build(HtmlSchemaDocSerializer.class);
		return schemaSerializer;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  CSS style code.
	 *
	 * @see #HTMLDOC_style
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return style;
	}

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * @see #HTMLDOC_stylesheet
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return stylesheet;
	}

	/**
	 * Configuration property:  Javascript code.
	 *
	 * @see #HTMLDOC_script
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return script;
	}

	/**
	 * Configuration property:  Page navigation links.
	 *
	 * @see #HTMLDOC_navlinks
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return navlinks;
	}

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * @see #HTMLDOC_head
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return head;
	}

	/**
	 * Configuration property:  Header section contents.
	 *
	 * @see #HTMLDOC_header
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return header;
	}

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * @see #HTMLDOC_nav
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return nav;
	}

	/**
	 * Configuration property:  Aside section contents.
	 *
	 * @see #HTMLDOC_aside
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return aside;
	}

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * @see #HTMLDOC_footer
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return footer;
	}

	/**
	 * Configuration property:  No-results message.
	 *
	 * @see #HTMLDOC_noResultsMessage
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return noResultsMessage;
	}

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * @see #HTMLDOC_nowrap
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return nowrap;
	}

	/**
	 * Configuration property:  HTML document template.
	 *
	 * @see #HTMLDOC_template
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return template;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlDocSerializer", new ObjectMap()
				.append("header", header)
				.append("nav", nav)
				.append("navlinks", navlinks)
				.append("aside", aside)
				.append("footer", footer)
				.append("style", style)
				.append("head", head)
				.append("stylesheet", stylesheet)
				.append("nowrap", nowrap)
				.append("template", template)
				.append("noResultsMessage", noResultsMessage)
			);
	}
}
