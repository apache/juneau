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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply(HtmlDocConfigAnnotation.SerializerApply.class)
public @interface HtmlDocConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// HtmlDocSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Aside section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the aside section on the HTML page.
	 * The aside section floats on the right of the page for providing content supporting the serialized content of
	 * the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#aside(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] aside() default {};

	/**
	 * Float aside section contents.
	 *
	 * <p>
	 * Allows you to position the aside contents of the page around the main contents.
	 *
	 * <p>
	 * By default, the aside section is floated to the right.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *  <ja>@HtmlDocConfig</ja>(
	 * 		aside={
	 * 			<js>"&lt;ul&gt;"</js>,
	 * 			<js>"	&lt;li&gt;Item 1"</js>,
	 * 			<js>"	&lt;li&gt;Item 2"</js>,
	 * 			<js>"	&lt;li&gt;Item 3"</js>,
	 * 			<js>"&lt;/ul&gt;"</js>
	 * 		},
	 * 		asideFloat=<js>"TOP"</js>
	 * 	)
	 * </p>
	 *
	 * <ul class='values'>
	 * 	<li><js>"RIGHT"</js>
	 * 	<li><js>"LEFT"</js>
	 * 	<li><js>"TOP"</js>
	 * 	<li><js>"BOTTOM"</js>
	 * 	<li><js>"DEFAULT"</js> (defaults to <js>"RIGHT"</js>)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#asideFloat(AsideFloat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String asideFloat() default "DEFAULT";

	/**
	 * Footer section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the footer section on the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		footer={
	 * 			<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#footer(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] footer() default {};

	/**
	 * Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		head={
	 * 			<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The head content from the parent can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#head(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] head() default {};

	/**
	 * Header section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the header section on the HTML page.
	 * The header section normally contains the title and description at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;My own header&lt;/h1&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no header.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class if not overridden.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#header(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] header() default {};

	/**
	 * Nav section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the nav section on the HTML page.
	 * The nav section normally contains the page links at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		nav={
	 * 			<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		When {@link #navlinks()} is also specified, this content is placed AFTER the navigation links.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#nav(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] nav() default {};

	/**
	 * Page navigation links.
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
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"api: servlet:/api"</js>,
	 * 			<js>"stats: servlet:/stats"</js>,
	 * 			<js>"doc: doc"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServlet {
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent links can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 		<br>Use the syntax <js>"key[index]: value"</js> or <js>"[index]: value"</js> to specify an index location
	 * 		to place a link inside the list of parent links.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a> (e.g. <js>"servlet:/..."</js>, <js>"request:/..."</js>).
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#navlinks(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] navlinks() default {};

	/**
	 * No-results message.
	 *
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		noResultsMessage=<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: HTML
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to represent no value to differentiate it from an empty string.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#noResultsMessage(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String noResultsMessage() default "";

	/**
	 * Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#nowrap()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String nowrap() default "";

	/**
	 * Javascript code.
	 *
	 * <p>
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		script={
	 * 			<js>"alert('hello!');"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: Javascript
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#script(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] script() default {};

	/**
	 * CSS style code.
	 *
	 * <p>
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		style={
	 * 			<js>"h3 { color: red; }"</js>,
	 * 			<js>"h5 { font-weight: bold; }"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: CSS
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li class='note'>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li class='note'>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#style(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] style() default {};

	/**
	 * Stylesheet import URLs.
	 *
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <p>
	 * Note that this stylesheet is controlled by the <code><ja>@Rest</ja>.stylesheet()</code> annotation.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: URL
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#stylesheet(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] stylesheet() default {};

	/**
	 * HTML document template.
	 *
	 * <p>
	 * Specifies the template to use for serializing the page.
	 *
	 * <p>
	 * By default, the {@link BasicHtmlDocTemplate} class is used to construct the contents of the HTML page, but
	 * can be overridden with your own custom implementation class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		template=MySpecialDocTemplate.<jk>class</jk>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li class='note'>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializer.Builder#template(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends HtmlDocTemplate> template() default HtmlDocTemplate.Void.class;

	/**
	 * HTML Widgets.
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
	 * <p class='bjava'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 * 	<li class='note'>
	 * 		Values are appended to the existing list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends HtmlWidget>[] widgets() default {};
}
