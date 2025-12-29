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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h3&gt;</a>
 * element.
 *
 * <p>
 * The h3 element represents a third-level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h2 elements. The h3 element is typically
 * used to organize content into smaller subsections and is important for creating a logical
 * document structure. It is typically rendered in a smaller font size than h2 but larger
 * than h4 elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple subsection heading</jc>
 * 	H3 <jv>simple</jv> = <jsm>h3</jsm>(<js>"Basic Configuration"</js>);
 *
 * 	<jc>// H3 with styling</jc>
 * 	H3 <jv>styled</jv> = <jsm>h3</jsm>(<js>"Advanced Settings"</js>)
 * 		._class(<js>"subsection-title"</js>);
 *
 * 	<jc>// H3 with complex content</jc>
 * 	H3 <jv>complex</jv> = <jsm>h3</jsm>(
 * 		<js>"Step 3: "</js>,
 * 		<jsm>strong</jsm>(<js>"Database Setup"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Optional)"</js>)
 * 	);
 *
 * 	<jc>// H3 with ID</jc>
 * 	H3 <jv>withId</jv> = <jsm>h3</jsm>(<js>"Troubleshooting"</js>)
 * 		.id(<js>"troubleshooting"</js>);
 *
 * 	<jc>// H3 with styling</jc>
 * 	H3 <jv>styled2</jv> = <jsm>h3</jsm>(<js>"Common Issues"</js>)
 * 		.style(<js>"color: #888; margin-top: 20px;"</js>);
 *
 * 	<jc>// H3 with multiple elements</jc>
 * 	H3 <jv>multiple</jv> = <jsm>h3</jsm>(
 * 		<js>"3.1 "</js>,
 * 		<jsm>span</jsm>()._class(<js>"step-title"</js>).children(<js>"Installation"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(5 minutes)"</js>)
 * 	);
 *
 * 	<jc>// H3 with links</jc>
 * 	H3 <jv>withLinks</jv> = <jsm>h3</jsm>(
 * 		<js>"See also: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/faq"</js>).children(<js>"Frequently Asked Questions"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h3() h3()}
 * 		<li class='jm'>{@link HtmlBuilder#h3(Object...) h3(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "h3")
public class H3 extends HtmlElementMixed {

	/**
	 * Creates an empty {@link H3} element.
	 */
	public H3() {}

	/**
	 * Creates an {@link H3} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H3(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public H3 _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public H3 child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public H3 children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H3 translate(Object value) {
		super.translate(value);
		return this;
	}
}