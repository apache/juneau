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

import static org.apache.juneau.rest.RestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.utils.*;

/**
 * Programmatic interface for setting properties used by the HtmlDoc serializer.
 */
public class HtmlDocConfig {

	String header, nav, aside, footer, style, stylesheet, script, noResultsMessage;
	String[] navlinks, head;
	boolean nowrap;
	Object template = HtmlDocTemplateBasic.class;
	List<Class<? extends Widget>> widgets = new ArrayList<Class<? extends Widget>>();

	HtmlDocConfig process(HtmlDoc hd) {
		for (Class<? extends Widget> cw : hd.widgets())
			widget(cw);
		header(resolveNewlineSeparatedAnnotation(hd.header(), header));
		nav(resolveNewlineSeparatedAnnotation(hd.nav(), nav));
		aside(resolveNewlineSeparatedAnnotation(hd.aside(), aside));
		footer(resolveNewlineSeparatedAnnotation(hd.footer(), footer));
		style(resolveNewlineSeparatedAnnotation(hd.style(), style));
		script(resolveNewlineSeparatedAnnotation(hd.script(), script));
		navlinks((Object[])resolveLinks(hd.navlinks(), navlinks));
		head((Object[])resolveContent(hd.head(), head));

		if (! hd.stylesheet().isEmpty())
			stylesheet(hd.stylesheet());
		if (! hd.noResultsMessage().isEmpty())
			noResultsMessage(hd.noResultsMessage());
		if (hd.nowrap())
			nowrap(true);
		if (hd.template() != HtmlDocTemplate.class)
			template(hd.template());

		return this;
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
	public HtmlDocConfig header(Object value) {
		header = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig navlinks(Object...value) {
		navlinks = StringUtils.toStrings(value);
		return this;
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
	public HtmlDocConfig nav(Object value) {
		this.nav = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig aside(Object value) {
		this.aside = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig footer(Object value) {
		this.footer = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig style(Object value) {
		this.style = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig stylesheet(Object value) {
		this.stylesheet = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig script(Object value) {
		this.script = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig head(Object...value) {
		this.head = StringUtils.toStrings(value);
		return this;
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
	public HtmlDocConfig nowrap(boolean value) {
		this.nowrap = value;
		return this;
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
	public HtmlDocConfig noResultsMessage(Object value) {
		this.noResultsMessage = StringUtils.toString(value);
		return this;
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
	public HtmlDocConfig template(Class<? extends HtmlDocTemplate> value) {
		this.template = value;
		return this;
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
	public HtmlDocConfig template(HtmlDocTemplate value) {
		this.template = value;
		return this;
	}

	/**
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * <p>
	 * Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 *
	 * @param value The widget class to add.
	 * @return This object (for method chaining).
	 */
	public HtmlDocConfig widget(Class<? extends Widget> value) {
		this.widgets.add(value);
		return this;
	}
}
