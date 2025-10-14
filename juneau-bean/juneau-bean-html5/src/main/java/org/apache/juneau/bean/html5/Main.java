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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-main-element">&lt;main&gt;</a>
 * element.
 *
 * <p>
 * The main element represents the main content of the body of a document or application. It is
 * used to identify the primary content of the page, excluding content that is repeated across
 * multiple pages such as navigation, headers, footers, and sidebars. The main element should
 * be used only once per page and contains the central topic or functionality of the page. It
 * is important for accessibility and helps screen readers identify the main content area.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple main content</jc>
 * 	Main <jv>simple</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Welcome to Our Site"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main content of our page."</js>)
 * 	);
 *
 * 	<jc>// Main with styling</jc>
 * 	Main <jv>styled</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"About Us"</js>),
 * 		<jsm>p</jsm>(<js>"Learn more about our company and mission."</js>)
 * 	)._class(<js>"main-content"</js>);
 *
 * 	<jc>// Main with complex content</jc>
 * 	Main <jv>complex</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Product Catalog"</js>),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Featured Products"</js>),
 * 			<jsm>p</jsm>(<js>"Check out our latest offerings."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Categories"</js>),
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<js>"Electronics"</js>),
 * 				<jsm>li</jsm>(<js>"Clothing"</js>),
 * 				<jsm>li</jsm>(<js>"Books"</js>)
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Main with ID</jc>
 * 	Main <jv>withId</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Main Content"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main content area."</js>)
 * 	).id(<js>"page-main"</js>);
 *
 * 	<jc>// Main with styling</jc>
 * 	Main <jv>styled2</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Centered Content"</js>),
 * 		<jsm>p</jsm>(<js>"This content is centered and styled."</js>)
 * 	).style(<js>"max-width: 800px; margin: 0 auto; padding: 20px;"</js>);
 *
 * 	<jc>// Main with multiple sections</jc>
 * 	Main <jv>multiSection</jv> = <jsm>main</jsm>(
 * 		<jsm>section</jsm>(
 * 			<jsm>h1</jsm>(<js>"Page Title"</js>),
 * 			<jsm>p</jsm>(<js>"Introduction to the page content."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Content Section"</js>),
 * 			<jsm>p</jsm>(<js>"Detailed content goes here."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Conclusion"</js>),
 * 			<jsm>p</jsm>(<js>"Summary and closing thoughts."</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Main with article</jc>
 * 	Main <jv>withArticle</jv> = <jsm>main</jsm>(
 * 		<jsm>article</jsm>(
 * 			<jsm>h1</jsm>(<js>"Article Title"</js>),
 * 			<jsm>p</jsm>(<js>"Article content goes here."</js>),
 * 			<jsm>footer</jsm>(<js>"Article footer"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="main")
public class Main extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Main} element.
	 */
	public Main() {}

	/**
	 * Creates a {@link Main} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Main(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Main _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Main translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Main child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Main children(Object...value) {
		super.children(value);
		return this;
	}
}