/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-form-element">&lt;form&gt;</a>
 * element.
 *
 * <p>
 * The form element represents a document section containing interactive controls for submitting
 * information to a web server. It groups form controls together and defines how the data should
 * be submitted, including the target URL, HTTP method, and encoding type.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple contact form</jc>
 * 	Form <jv>form1</jv> = <jsm>form</jsm>()
 * 		.action(<js>"/contact"</js>)
 * 		.method(<js>"post"</js>)
 * 		.children(
 * 			<jsm>input</jsm>(<js>"text"</js>).name(<js>"name"</js>).placeholder(<js>"Your Name"</js>),
 * 			<jsm>input</jsm>(<js>"email"</js>).name(<js>"email"</js>).placeholder(<js>"Your Email"</js>),
 * 			<jsm>textarea</jsm>().name(<js>"message"</js>).placeholder(<js>"Your Message"</js>),
 * 			<jsm>button</jsm>().type(<js>"submit"</js>).text(<js>"Send Message"</js>)
 * 		);
 *
 * 	<jc>// File upload form</jc>
 * 	Form <jv>form2</jv> = <jsm>form</jsm>()
 * 		.action(<js>"/upload"</js>)
 * 		.method(<js>"post"</js>)
 * 		.enctype(<js>"multipart/form-data"</js>)
 * 		.children(
 * 			<jsm>input</jsm>(<js>"file"</js>).name(<js>"file"</js>).accept(<js>"image/*"</js>),
 * 			<jsm>button</jsm>().type(<js>"submit"</js>).text(<js>"Upload"</js>)
 * 		);
 *
 * 	<jc>// Form with validation</jc>
 * 	Form <jv>form3</jv> = <jsm>form</jsm>()
 * 		.action(<js>"/register"</js>)
 * 		.method(<js>"post"</js>)
 * 		.novalidate(<jk>false</jk>)
 * 		.children(
 * 			<jsm>input</jsm>(<js>"email"</js>).name(<js>"email"</js>).required(<jk>true</jk>),
 * 			<jsm>input</jsm>(<js>"password"</js>).name(<js>"password"</js>).required(<jk>true</jk>),
 * 			<jsm>button</jsm>().type(<js>"submit"</js>).text(<js>"Register"</js>)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#form() form()}
 * 		<li class='jm'>{@link HtmlBuilder#form(Object, Object...) form(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="form")
public class Form extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Form} element.
	 */
	public Form() {}

	/**
	 * Creates a {@link Form} element with the specified {@link Form#action(String)} attribute.
	 *
	 * @param action The {@link Form#action(String)} attribute.
	 */
	public Form(String action) {
		action(action);
	}

	/**
	 * Creates an {@link Form} element with the specified {@link Form#action(String)} attribute and child nodes.
	 *
	 * @param action The {@link Form#action(String)} attribute.
	 * @param children The child nodes.
	 */
	public Form(String action, Object...children) {
		action(action).children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-accept-charset">accept-charset</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the character encodings that are accepted for form submission. Multiple encodings
	 * can be specified as a space-separated list.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"UTF-8"</js> - Unicode UTF-8 encoding (default)</li>
	 * 	<li><js>"ISO-8859-1"</js> - Latin-1 encoding</li>
	 * 	<li><js>"UTF-8 ISO-8859-1"</js> - Multiple encodings</li>
	 * </ul>
	 *
	 * @param acceptcharset The character encodings accepted for form submission.
	 * @return This object.
	 */
	public Form acceptcharset(String value) {
		attr("accept-charset", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-action">action</a> attribute.
	 *
	 * <p>
	 * URL to use for form submission.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param action The new value for this attribute.
	 * @return This object.
	 */
	public Form action(String value) {
		attrUri("action", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-autocomplete">autocomplete</a>
	 * attribute.
	 *
	 * <p>
	 * Sets the default autocomplete behavior for all form controls within this form.
	 * Individual controls can override this setting.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"on"</js> - Allow autocomplete (default)</li>
	 * 	<li><js>"off"</js> - Disable autocomplete</li>
	 * </ul>
	 *
	 * @param autocomplete The default autocomplete behavior for form controls.
	 * @return This object.
	 */
	public Form autocomplete(String value) {
		attr("autocomplete", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-enctype">enctype</a> attribute.
	 *
	 * <p>
	 * Specifies how form data should be encoded when submitted to the server.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"application/x-www-form-urlencoded"</js> - Default encoding (default)</li>
	 * 	<li><js>"multipart/form-data"</js> - Used for file uploads</li>
	 * 	<li><js>"text/plain"</js> - Plain text encoding</li>
	 * </ul>
	 *
	 * @param enctype The encoding type for form data submission.
	 * @return This object.
	 */
	public Form enctype(String value) {
		attr("enctype", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-method">method</a> attribute.
	 *
	 * <p>
	 * Specifies the HTTP method to use when submitting the form.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"get"</js> - Form data is sent as URL parameters (default)</li>
	 * 	<li><js>"post"</js> - Form data is sent in the request body</li>
	 * 	<li><js>"dialog"</js> - Used for forms within dialog elements</li>
	 * </ul>
	 *
	 * @param method The HTTP method for form submission.
	 * @return This object.
	 */
	public Form method(String value) {
		attr("method", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-form-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the form. This name can be used to access the form via the
	 * document.forms API and for form submission.
	 *
	 * <p>
	 * The name should be unique within the document and should not contain spaces or special characters.
	 *
	 * @param name The name of the form for API access and submission.
	 * @return This object.
	 */
	public Form name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-novalidate">novalidate</a> attribute.
	 *
	 * <p>
	 * Disables form validation, allowing the form to be submitted even if validation fails.
	 *
	 * @param novalidate If <jk>true</jk>, disables form validation.
	 * @return This object.
	 */
	public Form novalidate(Boolean value) {
		attr("novalidate", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-target">target</a> attribute.
	 *
	 * <p>
	 * Specifies where to display the response after form submission.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"_self"</js> - Load in the same frame (default)</li>
	 * 	<li><js>"_blank"</js> - Load in a new window or tab</li>
	 * 	<li><js>"_parent"</js> - Load in the parent frame</li>
	 * 	<li><js>"_top"</js> - Load in the full body of the window</li>
	 * 	<li>Frame name - Load in the named frame</li>
	 * </ul>
	 *
	 * @param target Where to display the form submission response.
	 * @return This object.
	 */
	public Form target(String value) {
		attr("target", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Form _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Form translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Form child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Form children(Object...value) {
		super.children(value);
		return this;
	}
}