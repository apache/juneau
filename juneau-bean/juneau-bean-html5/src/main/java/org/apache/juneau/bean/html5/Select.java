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
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-select-element">&lt;select&gt;</a>
 * element.
 *
 * <p>
 * The select element represents a control that provides a menu of options. It creates a dropdown
 * list that allows users to select one or more options from a list. The select element contains
 * option elements that define the available choices, and can be organized into groups using
 * optgroup elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple select dropdown</jc>
 * 	Select <jv>select1</jv> = <jsm>select</jsm>(<js>"color"</js>,
 * 		<jsm>option</jsm>(<js>"red"</js>, <js>"Red"</js>),
 * 		<jsm>option</jsm>(<js>"green"</js>, <js>"Green"</js>),
 * 		<jsm>option</jsm>(<js>"blue"</js>, <js>"Blue"</js>)
 * 	);
 *
 * 	<jc>// Multiple selection</jc>
 * 	Select <jv>select2</jv> = <jsm>select</jsm>(<js>"hobbies"</js>,
 * 		<jsm>option</jsm>(<js>"reading"</js>, <js>"Reading"</js>),
 * 		<jsm>option</jsm>(<js>"gaming"</js>, <js>"Gaming"</js>),
 * 		<jsm>option</jsm>(<js>"sports"</js>, <js>"Sports"</js>)
 * 	).multiple(<jk>true</jk>).size(4);
 *
 * 	<jc>// Select with option groups</jc>
 * 	Select <jv>select3</jv> = <jsm>select</jsm>(<js>"food"</js>,
 * 		<jsm>optgroup</jsm>(<js>"Fruits"</js>,
 * 			<jsm>option</jsm>(<js>"apple"</js>, <js>"Apple"</js>),
 * 			<jsm>option</jsm>(<js>"banana"</js>, <js>"Banana"</js>)
 * 		),
 * 		<jsm>optgroup</jsm>(<js>"Vegetables"</js>,
 * 			<jsm>option</jsm>(<js>"carrot"</js>, <js>"Carrot"</js>),
 * 			<jsm>option</jsm>(<js>"broccoli"</js>, <js>"Broccoli"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Disabled select</jc>
 * 	Select <jv>select4</jv> = <jsm>select</jsm>(<js>"disabled"</js>)
 * 		.disabled(<jk>true</jk>)
 * 		.children(
 * 			option().value("option1").text("Option 1")
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#select() select()}
 * 		<li class='jm'>{@link HtmlBuilder#select(String, Object...) select(String, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="select")
public class Select extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Select} element.
	 */
	public Select() {}

	/**
	 * Creates a {@link Select} element with the specified {@link Select#name(String)} attribute and child nodes.
	 *
	 * @param name The {@link Select#name(String)} attribute.
	 * @param children The child nodes.
	 */
	public Select(String name, Object...children) {
		name(name).children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Select _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Select autofocus(Object value) {
		attr("autofocus", value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Select child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Select children(Object...value) {
		super.children(value);
		return this;
	}

	/**
	 * Convenience method for selecting a child {@link Option} after the options have already been populated.
	 *
	 * @param optionValue The option value.
	 * @return This object.
	 */
	public Select choose(Object optionValue) {
		if (optionValue != null) {
			getChildren().forEach(x -> {
				if (x instanceof Option o && Utils.eq(optionValue.toString(), o.getAttr(String.class, "value")))
					o.selected(true);
			});
		}
		return this;
	}
	@Override /* Overridden from HtmlElement */
	public Select contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select dir(String value) {
		super.dir(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"disabled"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Select disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the select element with a form element by specifying the form's ID. This allows the select
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this select.
	 * @return This object.
	 */
	public Select form(String value) {
		attr("form", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-select-multiple">multiple</a> attribute.
	 *
	 * <p>
	 * Whether to allow multiple values.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"multiple"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Select multiple(Object value) {
		attr("multiple", deminimize(value, "multiple"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the select element. This name is used when the form is submitted and
	 * can be used to access the element via the form.elements API.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param value The name of the select element for submission and API access.
	 * @return This object.
	 */
	public Select name(String value) {
		attr("name", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-select-required">required</a> attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Select required(Object value) {
		attr("required", value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Select setChildren(List<Object> children) {
		super.setChildren(children);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-select-size">size</a> attribute.
	 *
	 * <p>
	 * Specifies the number of visible options in a select element. If greater than 1,
	 * the select becomes a scrollable list instead of a dropdown.
	 *
	 * @param value The number of visible options (1 for dropdown, >1 for list).
	 * @return This object.
	 */
	public Select size(Object value) {
		attr("size", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Select translate(Object value) {
		super.translate(value);
		return this;
	}
}