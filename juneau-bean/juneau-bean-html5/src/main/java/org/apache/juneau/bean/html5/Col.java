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

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-col-element">&lt;col&gt;</a>
 * element.
 *
 * <p>
 * The col element represents one or more columns in a column group represented by its parent colgroup
 * element. It is used to define the structural columns of a table and can specify attributes that apply
 * to all cells in those columns. The col element is a void element that does not contain any content
 * and is typically used within a colgroup element to define column properties such as width, alignment,
 * and styling.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple column</jc>
 * 	Col <jv>simple</jv> = <jsm>col</jsm>();
 *
 * 	<jc>// Column spanning multiple columns</jc>
 * 	Col <jv>spanning</jv> = <jsm>col</jsm>().span(3);
 *
 * 	<jc>// Column with styling</jc>
 * 	Col <jv>styled</jv> = <jsm>col</jsm>()
 * 		._class(<js>"highlight-column"</js>)
 * 		.style(<js>"background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Column with width</jc>
 * 	Col <jv>withWidth</jv> = <jsm>col</jsm>()
 * 		.style(<js>"width: 200px;"</js>);
 *
 * 	<jc>// Column with alignment</jc>
 * 	Col <jv>aligned</jv> = <jsm>col</jsm>()
 * 		.style(<js>"text-align: center;"</js>);
 *
 * 	<jc>// Column with ID</jc>
 * 	Col <jv>withId</jv> = <jsm>col</jsm>()
 * 		.id(<js>"name-column"</js>)
 * 		.span(2);
 *
 * 	<jc>// Column with multiple attributes</jc>
 * 	Col <jv>complex</jv> = <jsm>col</jsm>()
 * 		.span(2)
 * 		._class(<js>"data-column"</js>)
 * 		.style(<js>"width: 150px; text-align: right;"</js>)
 * 		.title(<js>"Numeric data column"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#col() col()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "col")
public class Col extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Col} element.
	 */
	public Col() {}

	/**
	 * Creates a {@link Col} element with the specified {@link Col#span(Object)} attribute.
	 *
	 * @param span The {@link Col#span(Object)} attribute.
	 */
	public Col(Number span) {
		span(span);
	}

	@Override /* Overridden from HtmlElement */
	public Col _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-col-span">span</a> attribute.
	 *
	 * <p>
	 * Number of columns spanned by the element.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Col span(Object value) {
		attr("span", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Col translate(Object value) {
		super.translate(value);
		return this;
	}
}