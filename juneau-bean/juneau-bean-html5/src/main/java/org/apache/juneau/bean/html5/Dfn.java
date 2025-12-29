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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-dfn-element">&lt;dfn&gt;</a>
 * element.
 *
 * <p>
 * The dfn element represents the defining instance of a term. It is used to mark up the first occurrence
 * of a term that is being defined in the document, making it clear to both users and search engines
 * that this is the definition of the term. The dfn element can contain the term being defined, and
 * the definition is typically provided in the surrounding context or in a related dd element.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple definition</jc>
 * 	Dfn <jv>simple</jv> = <jsm>dfn</jsm>(<js>"HTML"</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dfn <jv>styled</jv> = <jsm>dfn</jsm>(<js>"CSS"</js>)
 * 		._class(<js>"term"</js>);
 *
 * 	<jc>// Definition in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>dfn</jsm>(<js>"DOM"</js>),
 * 		<js>" represents the structure of a web page."</js>
 * 	);
 *
 * 	<jc>// Definition with complex content</jc>
 * 	Dfn <jv>complex</jv> = <jsm>dfn</jsm>(
 * 		<js>"JavaScript"</js>,
 * 		<jsm>span</jsm>()._class(<js>"abbrev"</js>).children(<js>" (JS)"</js>)
 * 	);
 *
 * 	<jc>// Definition with title</jc>
 * 	Dfn <jv>withTitle</jv> = <jsm>dfn</jsm>(<js>"HTML"</js>)
 * 		.title(<js>"HyperText Markup Language"</js>);
 *
 * 	<jc>// Definition with ID</jc>
 * 	Dfn <jv>withId</jv> = <jsm>dfn</jsm>(<js>"CSS"</js>)
 * 		.id(<js>"css-term"</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dfn <jv>styled2</jv> = <jsm>dfn</jsm>(<js>"API"</js>)
 * 		.style(<js>"font-style: italic; color: blue;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#dfn() dfn()}
 * 		<li class='jm'>{@link HtmlBuilder#dfn(Object...) dfn(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "dfn")
public class Dfn extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Dfn} element.
	 */
	public Dfn() {}

	/**
	 * Creates a {@link Dfn} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Dfn(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Dfn _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Dfn child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Dfn children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Dfn translate(Object value) {
		super.translate(value);
		return this;
	}
}