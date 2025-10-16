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

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-ol-element">&lt;ol&gt;</a>
 * element.
 *
 * <p>
 * The ol element represents an ordered list of items. It contains li elements that represent
 * individual list items. The list items are typically numbered automatically by the browser,
 * and the numbering can be customized using the type and start attributes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ordered list</jc>
 * 	Ol <jv>ol1</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"First item"</js>),
 * 		<jsm>li</jsm>(<js>"Second item"</js>),
 * 		<jsm>li</jsm>(<js>"Third item"</js>)
 * 	);
 *
 * 	<jc>// Ordered list with custom numbering</jc>
 * 	Ol <jv>ol2</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Item A"</js>),
 * 		<jsm>li</jsm>(<js>"Item B"</js>),
 * 		<jsm>li</jsm>(<js>"Item C"</js>)
 * 	).type(<js>"A"</js>).start(1);
 *
 * 	<jc>// Reversed ordered list</jc>
 * 	Ol <jv>ol3</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Last item"</js>),
 * 		<jsm>li</jsm>(<js>"Middle item"</js>),
 * 		<jsm>li</jsm>(<js>"First item"</js>)
 * 	).reversed(<jk>true</jk>);
 *
 * 	<jc>// Nested ordered list</jc>
 * 	Ol <jv>ol4</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Main item 1"</js>),
 * 		<jsm>li</jsm>(
 * 			<jsm>ol</jsm>(
 * 				<jsm>li</jsm>(<js>"Sub item 1.1"</js>),
 * 				<jsm>li</jsm>(<js>"Sub item 1.2"</js>)
 * 			)
 * 		),
 * 		<jsm>li</jsm>(<js>"Main item 2"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#ol() ol()}
 * 		<li class='jm'>{@link HtmlBuilder#ol(Object...) ol(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="ol")
public class Ol extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Ol} element.
	 */
	public Ol() {}

	/**
	 * Creates an {@link Ol} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Ol(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Ol _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}
	@Override /* Overridden from HtmlElement */
	public Ol attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Ol child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Ol children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-reversed">reversed</a>
	 * attribute.
	 *
	 * <p>
	 * Number the list backwards..
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"reversed"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Ol reversed(Object value) {
		attr("reversed", deminimize(value, "reversed"));
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Ol setChildren(List<Object> children) {
		super.setChildren(children);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-start">start</a> attribute.
	 *
	 * <p>
	 * Ordinal value of the first item.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Ol start(Object value) {
		attr("start", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Ol translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the type of numbering to use for the ordered list items.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"1"</js> - Decimal numbers (1, 2, 3, ...) - default</li>
	 * 	<li><js>"a"</js> - Lowercase letters (a, b, c, ...)</li>
	 * 	<li><js>"A"</js> - Uppercase letters (A, B, C, ...)</li>
	 * 	<li><js>"i"</js> - Lowercase Roman numerals (i, ii, iii, ...)</li>
	 * 	<li><js>"I"</js> - Uppercase Roman numerals (I, II, III, ...)</li>
	 * </ul>
	 *
	 * @param value The type of numbering for the ordered list.
	 * @return This object.
	 */
	public Ol type(String value) {
		attr("type", value);
		return this;
	}
}