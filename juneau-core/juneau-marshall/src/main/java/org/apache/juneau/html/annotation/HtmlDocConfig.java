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
package org.apache.juneau.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.svl.*;

/**
 * Annotation for specifying config properties defined in {@link HtmlSerializer}, {@link HtmlParser}, and {@link HtmlDocSerializer}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(HtmlDocConfigApply.class)
public @interface HtmlDocConfig {

	//-------------------------------------------------------------------------------------------------------------------
	// HtmlDocSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Aside section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the aside section on the HTML page.
	 * The aside section floats on the right of the page for providing content supporting the serialized content of
	 * the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		aside={
	 * 			<js>"&lt;ul&gt;"</js>,
	 * 			<js>"	&lt;li&gt;Item 1"</js>,
	 * 			<js>"	&lt;li&gt;Item 2"</js>,
	 * 			<js>"	&lt;li&gt;Item 3"</js>,
	 * 			<js>"&lt;/ul&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.aside.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_aside}
	 * </ul>
	 */
	String[] aside() default {};

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the footer section on the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		footer={
	 * 			<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.footer.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_footer}
	 * </ul>
	 */
	String[] footer() default {};

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		head={
	 * 			<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The head content from the parent can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.head.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_head}
	 * </ul>
	 */
	String[] head() default {};

	/**
	 * Configuration property:  Header section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the header section on the HTML page.
	 * The header section normally contains the title and description at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;My own header&lt;/h1&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no header.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class if not overridden.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.header.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_header}
	 * </ul>
	 */
	String[] header() default {};

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the nav section on the HTML page.
	 * The nav section normally contains the page links at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		nav={
	 * 			<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		When {@link #navlinks()} is also specified, this content is placed AFTER the navigation links.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.nav.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_nav}
	 * </ul>
	 */
	String[] nav() default {};

	/**
	 * Configuration property:  Page navigation links.
	 *
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
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"options: ?method=OPTIONS"</js>,
	 * 			<js>"doc: doc"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent links can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 		<br>Use the syntax <js>"key[index]: value"</js> or <js>"[index]: value"</js> to specify an index location
	 * 		to place a link inside the list of parent links.
	 * 	<li>
	 * 		Supports {@doc juneau-marshall.URIs} (e.g. <js>"servlet:/..."</js>, <js>"request:/..."</js>).
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.navlinks.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_navlinks}
	 * </ul>
	 */
	String[] navlinks() default {};

	/**
	 * Configuration property:  No-results message.
	 *
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		noResultsMessage=<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to represent no value to differentiate it from an empty string.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.noResultsMessage.s"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_noResultsMessage}
	 * </ul>
	 */
	String noResultsMessage() default "";

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.nowrap.b"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_nowrap}
	 * </ul>
	 */
	String nowrap() default "";

	/**
	 * Configuration property:  Javascript code.
	 *
	 * <p>
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * 	htmldoc=<ja>@HtmlDocConfig</ja>(
	 * 		script={
	 * 			<js>"alert('hello!');"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: Javascript
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.script.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_script}
	 * </ul>
	 */
	String[] script() default {};

	/**
	 * Configuration property:  CSS style code.
	 *
	 * <p>
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		style={
	 * 			<js>"h3 { color: red; }"</js>,
	 * 			<js>"h5 { font-weight: bold; }"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: CSS
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.style.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_style}
	 * </ul>
	 */
	String[] style() default {};

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <p>
	 * Note that this stylesheet is controlled by the <code><ja>@RestResource</ja>.stylesheet()</code> annotation.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Format: URL
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlDocSerializer.stylesheet.ls"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_stylesheet}
	 * </ul>
	 */
	String[] stylesheet() default {};

	/**
	 * Configuration property:  HTML document template.
	 *
	 * <p>
	 * Specifies the template to use for serializing the page.
	 *
	 * <p>
	 * By default, the {@link BasicHtmlDocTemplate} class is used to construct the contents of the HTML page, but
	 * can be overridden with your own custom implementation class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	htmldoc=<ja>@HtmlDocConfig</ja>(
	 * 		template=MySpecialDocTemplate.<jk>class</jk>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_template}
	 * </ul>
	 */
	Class<? extends HtmlDocTemplate> template() default HtmlDocTemplate.Null.class;

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * <p>
	 * Widgets resolve the following variables:
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"$W{name}"</js> - Contents returned by {@link HtmlWidget#getHtml(VarResolverSession)}.
	 * </ul>
	 *
	 * <p>
	 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
	 * and aside section of the page:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		widgets={
	 * 			MyWidget.<jk>class</jk>
	 * 		}
	 * 		navlinks={
	 * 			<js>"$W{MyWidget}"</js>
	 * 		},
	 * 		aside={
	 * 			<js>"Check out this widget:  $W{MyWidget}"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 * 	<li>
	 * 		Values are appended to the existing list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation.Widgets}
	 * </ul>
	 */
	Class<? extends HtmlWidget>[] widgets() default {};
}
