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
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 	// Simple contact form
 * 	Form form1 = new Form()
 * 		.action("/contact")
 * 		.method("post")
 * 		.children(
 * 			new Input().type("text").name("name").placeholder("Your Name"),
 * 			new Input().type("email").name("email").placeholder("Your Email"),
 * 			new Textarea().name("message").placeholder("Your Message"),
 * 			new Button().type("submit").text("Send Message")
 * 		);
 * 
 * 	// File upload form
 * 	Form form2 = new Form()
 * 		.action("/upload")
 * 		.method("post")
 * 		.enctype("multipart/form-data")
 * 		.children(
 * 			new Input().type("file").name("file").accept("image/*"),
 * 			new Button().type("submit").text("Upload")
 * 		);
 * 
 * 	// Form with validation
 * 	Form form3 = new Form()
 * 		.action("/register")
 * 		.method("post")
 * 		.novalidate(false)
 * 		.children(
 * 			new Input().type("email").name("email").required(true),
 * 			new Input().type("password").name("password").required(true),
 * 			new Button().type("submit").text("Register")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="form")
@FluentSetters
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

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Form translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Form child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Form children(Object...value) {
		super.children(value);
		return this;
	}

	// </FluentSetters>
}