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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-head-element">&lt;head&gt;</a>
 * element.
 *
 * <p>
 * The head element represents a collection of metadata for the document. It is used to contain
 * information about the document that is not displayed as part of the document's content, such
 * as the title, links to stylesheets, scripts, and other metadata. The head element is typically
 * placed immediately after the opening html tag and before the body element. It can contain
 * elements like title, meta, link, style, script, and base.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple head with title</jc>
 * 	Head <jv>simple</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"My Website"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	);
 *
 * 	<jc>// Head with styling</jc>
 * 	Head <jv>styled</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Styled Page"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/style.css"</js>)
 * 	);
 *
 * 	<jc>// Head with complex content</jc>
 * 	Head <jv>complex</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Complete Page"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>meta</jsm>().name(<js>"viewport"</js>).content(<js>"width=device-width, initial-scale=1.0"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/main.css"</js>),
 * 		<jsm>link</jsm>().rel(<js>"icon"</js>).href(<js>"/favicon.ico"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/main.js"</js>)
 * 	);
 *
 * 	<jc>// Head with ID</jc>
 * 	Head <jv>withId</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Page with ID"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	).id(<js>"page-head"</js>);
 *
 * 	<jc>// Head with styling</jc>
 * 	Head <jv>styled2</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Styled Head"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	).style(<js>"background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Head with multiple elements</jc>
 * 	Head <jv>multiple</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Multi-Element Head"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>meta</jsm>().name(<js>"description"</js>).content(<js>"A comprehensive page"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/reset.css"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/layout.css"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/jquery.js"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/app.js"</js>)
 * 	);
 *
 * 	<jc>// Head with base element</jc>
 * 	Head <jv>withBase</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Page with Base"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>base</jsm>().href(<js>"https://example.com/"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"css/style.css"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#head() head()}
 * 		<li class='jm'>{@link HtmlBuilder#head(Object, Object...) head(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="head")
public class Head extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Head} element.
	 */
	public Head() {}

	/**
	 * Creates a {@link Head} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Head(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Head _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Head translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Head child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Head children(Object...value) {
		super.children(value);
		return this;
	}
}