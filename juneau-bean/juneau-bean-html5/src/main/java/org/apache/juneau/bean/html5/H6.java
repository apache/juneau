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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h6&gt;</a>
 * element.
 *
 * <p>
 * The h6 element represents the lowest level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h5 elements. The h6 element is typically
 * used to organize content into the smallest subsections and is important for creating a logical
 * document structure. It is typically rendered in the smallest font size among all heading
 * elements and is often used for fine-grained content organization.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple lowest-level heading</jc>
 * 	H6 <jv>simple</jv> = <jsm>h6</jsm>(<js>"Connection Timeout"</js>);
 *
 * 	<jc>// H6 with styling</jc>
 * 	H6 <jv>styled</jv> = <jsm>h6</jsm>(<js>"Retry Attempts"</js>)
 * 		._class(<js>"micro-heading"</js>);
 *
 * 	<jc>// H6 with complex content</jc>
 * 	H6 <jv>complex</jv> = <jsm>h6</jsm>(
 * 		<js>"6.1 "</js>,
 * 		<jsm>strong</jsm>(<js>"Connection Pool Size"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Default: 5)"</js>)
 * 	);
 *
 * 	<jc>// H6 with ID</jc>
 * 	H6 <jv>withId</jv> = <jsm>h6</jsm>(<js>"Keep-Alive Settings"</js>)
 * 		.id(<js>"keep-alive"</js>);
 *
 * 	<jc>// H6 with styling</jc>
 * 	H6 <jv>styled2</jv> = <jsm>h6</jsm>(<js>"Advanced Options"</js>)
 * 		.style(<js>"color: #bbb; font-size: 0.8em; font-weight: normal;"</js>);
 *
 * 	<jc>// H6 with multiple elements</jc>
 * 	H6 <jv>multiple</jv> = <jsm>h6</jsm>(
 * 		<js>"6.1.1 "</js>,
 * 		<jsm>span</jsm>()._class(<js>"micro-title"</js>).children(<js>"Max Retries"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Range: 1-10)"</js>)
 * 	);
 *
 * 	<jc>// H6 with links</jc>
 * 	H6 <jv>withLinks</jv> = <jsm>h6</jsm>(
 * 		<js>"Ref: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/connection"</js>).children(<js>"Connection Guide"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h6() h6()}
 * 		<li class='jm'>{@link HtmlBuilder#h6(Object...) h6(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="h6")
public class H6 extends HtmlElementMixed {

	/**
	 * Creates an empty {@link H6} element.
	 */
	public H6() {}

	/**
	 * Creates an {@link H6} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H6(Object...children) {
		children(children);
	}
	@Override /* Overridden from HtmlElement */
	public H6 _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public H6 child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public H6 children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public H6 translate(Object value) {
		super.translate(value);
		return this;
	}
}