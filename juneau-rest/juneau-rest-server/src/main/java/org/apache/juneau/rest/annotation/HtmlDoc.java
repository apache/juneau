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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.widget.*;

/**
 * Contains all the configurable annotations for the {@link HtmlDocSerializer}.
 * 
 * <p>
 * Used with {@link RestResource#htmldoc() @RestResource.htmldoc()} and {@link RestMethod#htmldoc() @RestMethod.htmldoc()} 
 * to customize the HTML view of serialized POJOs.
 * 
 * <p>
 * All annotations specified here have no effect on any serializers other than {@link HtmlDocSerializer} and is
 * provided as a shorthand method of for specifying configuration properties.
 * 
 * <p>
 * For example, the following two methods for defining the HTML nav links are considered equivalent:
 * <p class='bcode'>
 * 	<jc>// Defined via properties.</jc>
 * 	<ja>@RestResource</ja>(
 * 		properties={
 * 			<ja>@Property</ja>(name=HtmlDocSerializer.<jsf>HTMLDOC_navlinks</jsf>, value=<js>"{options:'?method=OPTIONS',doc:'doc'}"</js>)
 * 		}
 * 	)
 * 
 * 	<jc>// Defined via annotation.</jc>
 * 	<ja>@RestResource</ja>(
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
 * The purpose of these annotation is to populate the HTML document view which by default consists of the following
 * structure:
 * <p class='bcode'>
 * 	<xt>&lt;html&gt;
 * 		&lt;head&gt;
 * 			&lt;style <xa>type</xa>=<xs>'text/css'</xs>&gt;
 * 				<xv>CSS styles and links to stylesheets</xv>
 * 			&lt;/style&gt;
 * 		&lt;/head&gt;
 * 		&lt;body&gt;
 * 			&lt;header&gt;
 * 				<xv>Page header</xv>
 * 			&lt;/header&gt;
 * 			&lt;nav&gt;
 * 				<xv>Navigation links</xv>
 * 			&lt;/nav&gt;
 * 			&lt;aside&gt;
 * 				<xv>Side-bar text</xv>
 * 			&lt;/aside&gt;
 * 			&lt;article&gt;
 * 				<xv>Contents of serialized object</xv>
 * 			&lt;/article&gt;
 * 			&lt;footer&gt;
 * 				<xv>Footer message</xv>
 * 			&lt;/footer&gt;
 * 		&lt;/body&gt;
 * 	&lt;/html&gt;</xt>
 * </p>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HtmlDoc">Overview &gt; @HtmlDoc</a>
 * </ul>
 */
public @interface HtmlDoc {

	/**
	 * Sets the HTML aside section contents.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <p>
	 * The aside section typically floats on the right side of the page.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			aside={
	 * 				<js>"&lt;p&gt;Custom aside content&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_aside}
	 * 	<li class='jm'>{@link HtmlDocBuilder#aside(Object...)}
	 * </ul>
	 */
	String[] aside() default {};

	/**
	 * Sets the HTML footer section contents.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <p>
	 * The footer section typically floats on the bottom of the page.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			footer={
	 * 				<js>"&lt;p&gt;Custom footer content&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_footer}
	 * 	<li class='jm'>{@link HtmlDocBuilder#footer(Object...)}
	 * </ul>
	 */
	String[] footer() default {};

	/**
	 * Adds arbitrary content to the HTML <xt>&lt;head&gt;</xt> element on the page.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			head={
	 * 				<jc>// Add a shortcut link in the browser tab</jc>
	 * 				<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>,
	 * 
	 * 				<jc>// Reload the page every 5 seconds </jc>
	 * 				<js>"&lt;meta http-equiv='refresh' content='5'&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The head content from the parent can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_head}
	 * 	<li class='jm'>{@link HtmlDocBuilder#head(Object...)}
	 * </ul>
	 */
	String[] head() default {};

	/**
	 * Sets the HTML header section contents.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <p>
	 * The page header normally contains the title and description, but this value can be used to override the contents
	 * to be whatever you want.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			header={
	 * 				<js>"&lt;p&gt;This is my REST interface&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no header.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class if not overridden.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_header}
	 * 	<li class='jm'>{@link HtmlDocBuilder#header(Object...)}
	 * </ul>
	 */
	String[] header() default {};

	/**
	 * Sets the HTML nav section contents.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <p>
	 * The nav section of the page contains the links.
	 * 
	 * <p>
	 * The format of this value is HTML.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			nav={
	 * 				<js>"&lt;p&gt;Custom nav content&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When a value is specified, the {@link #navlinks()} value will be ignored.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_nav}
	 * 	<li class='jm'>{@link HtmlDocBuilder#nav(Object...)}
	 * </ul>
	 */
	String[] nav() default {};

	/**
	 * Sets the links in the HTML nav section.
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
	 * The page links are positioned immediately under the title and text.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			navlinks={
	 * 				<js>"up: request:/.."</js>,
	 * 				<js>"options: servlet:/?method=OPTIONS"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This field can also use URIs of any support type in {@link UriResolver}.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent links can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 		<br>Use the syntax <js>"key[index]: value"</js> or <js>"[index]: value"</js> to specify an index location
	 * 		to place a link inside the list of parent links.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_navlinks}
	 * 	<li class='jm'>{@link HtmlDocBuilder#navlinks(Object...)}
	 * </ul>
	 */
	String[] navlinks() default {};

	/**
	 * Specifies the text to display when serializing an empty array or collection.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_noResultsMessage}
	 * 	<li class='jm'>{@link HtmlDocBuilder#noResultsMessage(Object)}
	 * </ul>
	 */
	String noResultsMessage() default "no results";

	/**
	 * Shorthand method for forcing the rendered HTML content to be no-wrap.
	 * 
	 * <p>
	 * This only applies to the rendered data portion of the page.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_nowrap}
	 * 	<li class='jm'>{@link HtmlDocBuilder#nowrap(boolean)}
	 * </ul>
	 */
	boolean nowrap() default false;

	/**
	 * Sets the HTML script section contents.
	 * 
	 * <p>
	 * The format of this value is Javascript.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			script={
	 * 				<js>"alert('Hello!')"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_script}
	 * 	<li class='jm'>{@link HtmlDocBuilder#script(Object[])}
	 * </ul>
	 */
	String[] script() default {};

	/**
	 * Sets the HTML CSS style section contents.
	 * 
	 * <p>
	 * The format of this value is CSS.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			style={
	 * 				<js>".red{color:red;}"</js>,
	 * 				<js>".blue{color:blue;}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_style}
	 * 	<li class='jm'>{@link HtmlDocBuilder#style(Object[])}
	 * </ul>
	 */
	String[] style() default {};

	/**
	 * Sets the CSS URL in the HTML CSS style section.
	 * 
	 * <p>
	 * The format of this value is a URL.
	 * 
	 * <p>
	 * Specifies the URL to the stylesheet to add as a link in the style tag in the header.
	 * 
	 * <p>
	 * The format of this value is CSS.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			stylesheet=<js>"http://someOtherHost/stealTheir.css"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_stylesheet}
	 * 	<li class='jm'>{@link HtmlDocBuilder#stylesheet(Object[])}
	 * </ul>
	 */
	String[] stylesheet() default {};

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 * 
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide your own custom
	 * renderer or subclasses from the basic class to have full control over how the page is rendered.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDoc</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_template}
	 * 	<li class='jm'>{@link HtmlDocBuilder#template(Class)}
	 * 	<li class='jm'>{@link HtmlDocBuilder#template(HtmlDocTemplate)}
	 * </ul>
	 */
	Class<? extends HtmlDocTemplate> template() default HtmlDocTemplate.class;

	/**
	 * Configuration property:  HTML Widgets. 
	 * 
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 * 
	 * Widgets resolve the following variables:
	 * <ul>
	 * 	<li><js>"$W{name}"</js> - Contents returned by {@link Widget#getHtml(RestRequest)}.
	 * 	<li><js>"$W{name.script}"</js> - Contents returned by {@link Widget#getScript(RestRequest)}.
	 * 		<br>The script contents are automatically inserted into the <xt>&lt;head/script&gt;</xt> section
	 * 			 in the HTML page.
	 * 	<li><js>"$W{name.style}"</js> - Contents returned by {@link Widget#getStyle(RestRequest)}.
	 * 		<br>The styles contents are automatically inserted into the <xt>&lt;head/style&gt;</xt> section
	 * 			 in the HTML page.
	 * </ul>
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
	 * 	<li class='jf'>{@link RestContext#REST_widgets}
	 * 	<li class='jm'>{@link RestContextBuilder#widgets(Class...)}
	 * 	<li class='jm'>{@link RestContextBuilder#widgets(Widget...)}
	 * 	<li class='jm'>{@link RestContextBuilder#widgets(boolean,Widget...)}
	 * </ul>
	 */
	Class<? extends Widget>[] widgets() default {};
}
