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
package org.apache.juneau.dto.html5;

import java.net.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-input-element">&lt;input&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="input")
@FluentSetters
public class Input extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Input} element.
	 */
	public Input() {}

	/**
	 * Creates an {@link Input} element with the specified {@link Input#type(String)} attribute.
	 *
	 * @param type The {@link Input#type(String)} attribute.
	 */
	public Input(String type) {
		type(type);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-accept">accept</a> attribute.
	 *
	 * <p>
	 * Hint for expected file type in file upload controls.
	 *
	 * @param accept The new value for this attribute.
	 * @return This object.
	 */
	public final Input accept(String accept) {
		attr("accept", accept);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-alt">alt</a> attribute.
	 *
	 * <p>
	 * Replacement text for use when images are not available.
	 *
	 * @param alt The new value for this attribute.
	 * @return This object.
	 */
	public final Input alt(String alt) {
		attr("alt", alt);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autocomplete">autocomplete</a> attribute.
	 *
	 * <p>
	 * Hint for form auto-fill feature.
	 *
	 * @param autocomplete The new value for this attribute.
	 * @return This object.
	 */
	public final Input autocomplete(String autocomplete) {
		attr("autocomplete", autocomplete);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param autofocus The new value for this attribute.
	 * @return This object.
	 */
	public final Input autofocus(String autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-checked">checked</a> attribute.
	 *
	 * <p>
	 * Whether the command or control is checked.
	 *
	 * @param checked
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Input checked(Object checked) {
		attr("checked", deminimize(checked, "checked"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-dirname">dirname</a> attribute.
	 *
	 * <p>
	 * Name of form field to use for sending the element's directionality in form submission.
	 *
	 * @param dirname The new value for this attribute.
	 * @return This object.
	 */
	public final Input dirname(String dirname) {
		attr("dirname", dirname);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Input disabled(Object disabled) {
		attr("disabled", deminimize(disabled, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the control with a form element.
	 *
	 * @param form The new value for this attribute.
	 * @return This object.
	 */
	public final Input form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formaction">formaction</a> attribute.
	 *
	 * <p>
	 * URL to use for form submission.
	 *
	 * @param formaction The new value for this attribute.
	 * @return This object.
	 */
	public final Input formaction(String formaction) {
		attr("formaction", formaction);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formenctype">formenctype</a> attribute.
	 *
	 * <p>
	 * Form data set encoding type to use for form submission.
	 *
	 * @param formenctype The new value for this attribute.
	 * @return This object.
	 */
	public final Input formenctype(String formenctype) {
		attr("formenctype", formenctype);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formmethod">formmethod</a> attribute.
	 *
	 * <p>
	 * HTTP method to use for form submission.
	 *
	 * @param formmethod The new value for this attribute.
	 * @return This object.
	 */
	public final Input formmethod(String formmethod) {
		attr("formmethod", formmethod);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formnovalidate">formnovalidate</a>
	 * attribute.
	 *
	 * <p>
	 * Bypass form control validation for form submission.
	 *
	 * @param formnovalidate The new value for this attribute.
	 * @return This object.
	 */
	public final Input formnovalidate(String formnovalidate) {
		attr("formnovalidate", formnovalidate);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formtarget">formtarget</a> attribute.
	 *
	 * <p>
	 * Browsing context for form submission.
	 *
	 * @param formtarget The new value for this attribute.
	 * @return This object.
	 */
	public final Input formtarget(String formtarget) {
		attr("formtarget", formtarget);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height">height</a>
	 * attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param height
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-input-inputmode">inputmode</a>
	 * attribute.
	 * Hint for selecting an input modality.
	 *
	 * @param inputmode The new value for this attribute.
	 * @return This object.
	 */
	public final Input inputmode(String inputmode) {
		attr("inputmode", inputmode);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-list">list</a> attribute.
	 *
	 * <p>
	 * List of auto-complete options.
	 *
	 * @param list The new value for this attribute.
	 * @return This object.
	 */
	public final Input list(String list) {
		attr("list", list);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-max">max</a> attribute.
	 *
	 * <p>
	 * Maximum value.
	 *
	 * @param max
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input max(Object max) {
		attr("max", max);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-maxlength">maxlength</a> attribute.
	 * Maximum length of value.
	 *
	 * @param maxlength The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input maxlength(Object maxlength) {
		attr("maxlength", maxlength);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-min">min</a> attribute.
	 *
	 * <p>
	 * Minimum value.
	 *
	 * @param min
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input min(Object min) {
		attr("min", min);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-minlength">minlength</a> attribute.
	 *
	 * <p>
	 * Minimum length of value.
	 *
	 * @param minlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input minlength(Object minlength) {
		attr("minlength", minlength);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-multiple">multiple</a> attribute.
	 *
	 * <p>
	 * Whether to allow multiple values.
	 *
	 * @param multiple
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Input multiple(Object multiple) {
		attr("multiple", deminimize(multiple, "multiple"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Name of form control to use for form submission and in the form.elements API.
	 *
	 * @param name The new value for this attribute.
	 * @return This object.
	 */
	public final Input name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-pattern">pattern</a> attribute.
	 *
	 * <p>
	 * Pattern to be matched by the form control's value.
	 *
	 * @param pattern The new value for this attribute.
	 * @return This object.
	 */
	public final Input pattern(String pattern) {
		attr("pattern", pattern);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-placeholder">placeholder</a> attribute.
	 *
	 * <p>
	 * User-visible label to be placed within the form control.
	 *
	 * @param placeholder The new value for this attribute.
	 * @return This object.
	 */
	public final Input placeholder(String placeholder) {
		attr("placeholder", placeholder);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">readonly</a> attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Input readonly(Object readonly) {
		attr("readonly", readonly);
		return this;
	}


	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">readonly</a> attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly If <jk>true</jk>, adds <c>readonly="readonly"</c>.
	 * @return This object.
	 */
	public final Input readonly(boolean readonly) {
		if (readonly)
			readonly("readonly");
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">required</a> attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param required
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Input required(Object required) {
		attr("required", required);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-size">size</a> attribute.
	 *
	 * <p>
	 * Size of the control.
	 *
	 * @param size
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input size(Object size) {
		attr("size", size);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-src">src</a> attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * @param src
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object.
	 */
	public final Input src(Object src) {
		attr("src", src);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-step">step</a> attribute.
	 *
	 * <p>
	 * Granularity to be matched by the form control's value.
	 *
	 * @param step The new value for this attribute.
	 * @return This object.
	 */
	public final Input step(String step) {
		attr("step", step);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-type">type</a> attribute.
	 *
	 * <p>
	 * Type of form control.
	 *
	 * @param type The new value for this attribute.
	 * @return This object.
	 */
	public final Input type(String type) {
		attr("type", type);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-value">value</a> attribute.
	 *
	 * <p>
	 * Value of the form control.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input value(Object value) {
		attr("value", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width">width</a> attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Input width(Object width) {
		attr("width", width);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input accesskey(String accesskey) {
		super.accesskey(accesskey);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input contenteditable(Object contenteditable) {
		super.contenteditable(contenteditable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input hidden(Object hidden) {
		super.hidden(hidden);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input id(String id) {
		super.id(id);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input lang(String lang) {
		super.lang(lang);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onabort(String onabort) {
		super.onabort(onabort);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onblur(String onblur) {
		super.onblur(onblur);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oncancel(String oncancel) {
		super.oncancel(oncancel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oncanplay(String oncanplay) {
		super.oncanplay(oncanplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oncanplaythrough(String oncanplaythrough) {
		super.oncanplaythrough(oncanplaythrough);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onchange(String onchange) {
		super.onchange(onchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onclick(String onclick) {
		super.onclick(onclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oncuechange(String oncuechange) {
		super.oncuechange(oncuechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input ondblclick(String ondblclick) {
		super.ondblclick(ondblclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input ondurationchange(String ondurationchange) {
		super.ondurationchange(ondurationchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onemptied(String onemptied) {
		super.onemptied(onemptied);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onended(String onended) {
		super.onended(onended);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onerror(String onerror) {
		super.onerror(onerror);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onfocus(String onfocus) {
		super.onfocus(onfocus);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oninput(String oninput) {
		super.oninput(oninput);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input oninvalid(String oninvalid) {
		super.oninvalid(oninvalid);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onkeydown(String onkeydown) {
		super.onkeydown(onkeydown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onkeypress(String onkeypress) {
		super.onkeypress(onkeypress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onkeyup(String onkeyup) {
		super.onkeyup(onkeyup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onload(String onload) {
		super.onload(onload);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onloadeddata(String onloadeddata) {
		super.onloadeddata(onloadeddata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onloadedmetadata(String onloadedmetadata) {
		super.onloadedmetadata(onloadedmetadata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onloadstart(String onloadstart) {
		super.onloadstart(onloadstart);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmousedown(String onmousedown) {
		super.onmousedown(onmousedown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmouseenter(String onmouseenter) {
		super.onmouseenter(onmouseenter);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmouseleave(String onmouseleave) {
		super.onmouseleave(onmouseleave);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmousemove(String onmousemove) {
		super.onmousemove(onmousemove);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmouseout(String onmouseout) {
		super.onmouseout(onmouseout);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmouseover(String onmouseover) {
		super.onmouseover(onmouseover);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmouseup(String onmouseup) {
		super.onmouseup(onmouseup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onmousewheel(String onmousewheel) {
		super.onmousewheel(onmousewheel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onpause(String onpause) {
		super.onpause(onpause);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onplay(String onplay) {
		super.onplay(onplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onplaying(String onplaying) {
		super.onplaying(onplaying);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onprogress(String onprogress) {
		super.onprogress(onprogress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onratechange(String onratechange) {
		super.onratechange(onratechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onreset(String onreset) {
		super.onreset(onreset);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onresize(String onresize) {
		super.onresize(onresize);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onscroll(String onscroll) {
		super.onscroll(onscroll);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onseeked(String onseeked) {
		super.onseeked(onseeked);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onseeking(String onseeking) {
		super.onseeking(onseeking);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onselect(String onselect) {
		super.onselect(onselect);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onshow(String onshow) {
		super.onshow(onshow);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onstalled(String onstalled) {
		super.onstalled(onstalled);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onsubmit(String onsubmit) {
		super.onsubmit(onsubmit);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onsuspend(String onsuspend) {
		super.onsuspend(onsuspend);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input ontimeupdate(String ontimeupdate) {
		super.ontimeupdate(ontimeupdate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input ontoggle(String ontoggle) {
		super.ontoggle(ontoggle);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onvolumechange(String onvolumechange) {
		super.onvolumechange(onvolumechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input onwaiting(String onwaiting) {
		super.onwaiting(onwaiting);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input spellcheck(Object spellcheck) {
		super.spellcheck(spellcheck);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input style(String style) {
		super.style(style);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input tabindex(Object tabindex) {
		super.tabindex(tabindex);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input title(String title) {
		super.title(title);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Input translate(Object translate) {
		super.translate(translate);
		return this;
	}

	// </FluentSetters>
}
