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
package org.apache.juneau.rest;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Programmatic interface for setting properties used by the HtmlDoc serializer.
 *
 * <p>
 * This class is instantiated by calling the {@link RestResponse#getHtmlDocBuilder()} method.
 */
public class HtmlDocBuilder {

	private final ObjectMap properties;

	HtmlDocBuilder(ObjectMap properties) {
		this.properties = properties;
	}

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
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no header.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#header() @HtmlDoc.header()} annotation.
	 *
	 * @param value
	 * 	The HTML header section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder header(Object value) {
		return set(HTMLDOC_header, value);
	}

	/**
	 * Sets the links in the HTML nav section.
	 *
	 * <p>
	 * The format of this value is a lax-JSON map of key/value pairs where the keys are the link text and the values are
	 * relative (to the servlet) or absolute URLs.
	 *
	 * <p>
	 * The page links are positioned immediately under the title and text.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This field can also use URIs of any support type in {@link UriResolver}.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#navlinks() @HtmlDoc.navlinks()} annotation.
	 *
	 * @param value
	 * 	The HTML nav section links links.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder navlinks(Object...value) {
		return set(HTMLDOC_navlinks, value);
	}

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
	 * <p>
	 * When a value is specified, the {@link #navlinks(Object[])} value will be ignored.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#nav() @HtmlDoc.nav()} annotation.
	 *
	 * @param value
	 * 	The HTML nav section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder nav(Object value) {
		return set(HTMLDOC_nav, value);
	}

	/**
	 * Sets the HTML aside section contents.
	 *
	 * <p>
	 * The format of this value is HTML.
	 *
	 * <p>
	 * The aside section typically floats on the right side of the page.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#aside() @HtmlDoc.aside()} annotation.
	 *
	 * @param value
	 * 	The HTML aside section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to waste
	 * 				string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder aside(Object value) {
		return set(HTMLDOC_aside, value);
	}

	/**
	 * Sets the HTML footer section contents.
	 *
	 * <p>
	 * The format of this value is HTML.
	 *
	 * <p>
	 * The footer section typically floats on the bottom of the page.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#footer() @HtmlDoc.footer()} annotation.
	 *
	 * @param value
	 * 	The HTML footer section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder footer(Object value) {
		return set(HTMLDOC_footer, value);
	}

	/**
	 * Sets the HTML CSS style section contents.
	 *
	 * <p>
	 * The format of this value is CSS.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#style() @HtmlDoc.style()} annotation.
	 *
	 * @param value
	 * 	The HTML CSS style section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder style(Object value) {
		return set(HTMLDOC_style, value);
	}

	/**
	 * Sets the CSS URL in the HTML CSS style section.
	 *
	 * <p>
	 * The format of this value is a comma-delimited list of URLs.
	 *
	 * <p>
	 * Specifies the URL to the stylesheet to add as a link in the style tag in the header.
	 *
	 * <p>
	 * The format of this value is CSS.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>) and can use URL protocols defined
	 * by {@link UriResolver}.
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#stylesheet() @HtmlDoc.stylesheet()} annotation.
	 *
	 * @param value
	 * 	The CSS URL in the HTML CSS style section.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder stylesheet(Object value) {
		return set(HTMLDOC_stylesheet, value);
	}

	/**
	 * Sets the HTML script section contents.
	 *
	 * <p>
	 * The format of this value is Javascript.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#script() @HtmlDoc.script()} annotation.
	 *
	 * @param value
	 * 	The HTML script section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder script(Object value) {
		return set(HTMLDOC_script, value);
	}

	/**
	 * Sets the HTML head section contents.
	 *
	 * <p>
	 * The format of this value is HTML.
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#head() @HtmlDoc.head()} annotation.
	 *
	 * @param value
	 * 	The HTML head section contents.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder head(Object...value) {
		return set(HTMLDOC_head, value);
	}

	/**
	 * Shorthand method for forcing the rendered HTML content to be no-wrap.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#nowrap() @HtmlDoc.nowrap()} annotation.
	 *
	 * @param value The new nowrap setting.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder nowrap(boolean value) {
		return set(HTMLDOC_nowrap, value);
	}

	/**
	 * Specifies the text to display when serializing an empty array or collection.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#noResultsMessage() @HtmlDoc.noResultsMessage()}
	 * annotation.
	 *
	 * @param value The text to display when serializing an empty array or collection.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder noResultsMessage(Object value) {
		return set(HTMLDOC_noResultsMessage, value);
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 *
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide your own custom
	 * renderer or subclasses from the basic class to have full control over how the page is rendered.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc.template()} annotation.
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder template(Class<? extends HtmlDocTemplate> value) {
		return set(HTMLDOC_template, value);
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 *
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide your own custom
	 * renderer or subclasses from the basic class to have full control over how the page is rendered.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc.template()} annotation.
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder template(HtmlDocTemplate value) {
		return set(HTMLDOC_template, value);
	}

	private HtmlDocBuilder set(String key, Object value) {
		properties.put(key, value);
		return this;
	}
}
