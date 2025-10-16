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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-article-element">&lt;article&gt;</a>
 * element.
 *
 * <p>
 * The article element represents a self-contained composition in a document, page, application, or site,
 * which is intended to be independently distributable or reusable. It is used for content that could
 * stand alone, such as blog posts, news articles, forum posts, or other independent pieces of content.
 * Each article should have its own heading structure and can contain other semantic elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple blog post article</jc>
 * 	Article <jv>blogPost</jv> = <jsm>article</jsm>(
 * 		<jsm>header</jsm>(
 * 			<jsm>h1</jsm>(<js>"How to Use HTML5 Semantic Elements"</js>),
 * 			<jsm>p</jsm>(<js>"Published on "</js>, <jsm>time</jsm>(<js>"2024-01-15"</js>, <js>"January 15, 2024"</js>))
 * 		),
 * 		<jsm>p</jsm>(<js>"HTML5 introduced several semantic elements that help structure content..."</js>),
 * 		<jsm>footer</jsm>(
 * 			<jsm>p</jsm>(<js>"Author: John Doe"</js>),
 * 			<jsm>address</jsm>(<js>"Contact: john@example.com"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// News article</jc>
 * 	Article <jv>newsArticle</jv> = <jsm>article</jsm>(
 * 		<jsm>header</jsm>(
 * 			<jsm>h1</jsm>(<js>"Breaking: New Technology Released"</js>),
 * 			<jsm>p</jsm>(<js>"By Jane Smith, Technology Reporter"</js>)
 * 		),
 * 		<jsm>p</jsm>(<js>"A revolutionary new technology was announced today..."</js>),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Technical Details"</js>),
 * 			<jsm>p</jsm>(<js>"The technology works by..."</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Forum post</jc>
 * 	Article <jv>forumPost</jv> = <jsm>article</jsm>(
 * 		<jsm>header</jsm>(
 * 			<jsm>h3</jsm>(<js>"Question about CSS Grid"</js>),
 * 			<jsm>p</jsm>(<js>"Posted by "</js>, <jsm>strong</jsm>(<js>"user123"</js>), <js>" on "</js>, <jsm>time</jsm>(<js>"2024-01-14"</js>, <js>"yesterday"</js>))
 * 		),
 * 		<jsm>p</jsm>(<js>"I'm having trouble with CSS Grid layout..."</js>),
 * 		<jsm>footer</jsm>(
 * 			<jsm>p</jsm>(<js>"Tags: "</js>, <jsm>a</jsm>(<js>"/tag/css"</js>, <js>"CSS"</js>), <js>", "</js>, <jsm>a</jsm>(<js>"/tag/grid"</js>, <js>"Grid"</js>))
 * 		)
 * 	)._class(<js>"forum-post"</js>);
 *
 * 	<jc>// Product review</jc>
 * 	Article <jv>review</jv> = <jsm>article</jsm>(
 * 		<jsm>header</jsm>(
 * 			<jsm>h1</jsm>(<js>"Review: Amazing Widget Pro"</js>),
 * 			<jsm>p</jsm>(<js>"Rating: "</js>, <jsm>strong</jsm>(<js>"5/5 stars"</js>))
 * 		),
 * 		<jsm>p</jsm>(<js>"After using the Amazing Widget Pro for a month..."</js>),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Pros"</js>),
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<js>"Easy to use"</js>),
 * 				<jsm>li</jsm>(<js>"Great performance"</js>),
 * 				<jsm>li</jsm>(<js>"Excellent support"</js>)
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Article with multiple sections</jc>
 * 	Article <jv>multiSection</jv> = <jsm>article</jsm>(
 * 		<jsm>header</jsm>(<jsm>h1</jsm>(<js>"Complete Guide to Web Development"</js>)),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Introduction"</js>),
 * 			<jsm>p</jsm>(<js>"Web development encompasses many technologies..."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Frontend Development"</js>),
 * 			<jsm>p</jsm>(<js>"Frontend development focuses on..."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Backend Development"</js>),
 * 			<jsm>p</jsm>(<js>"Backend development handles..."</js>)
 * 		)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#article() article()}
 * 		<li class='jm'>{@link HtmlBuilder#article(Object...) article(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="article")
public class Article extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Article} element.
	 */
	public Article() {}

	/**
	 * Creates an {@link Article} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Article(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Article _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}
	@Override /* Overridden from HtmlElementMixed */
	public Article child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Article children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article dir(String value) {
		super.dir(value);
		return this;
	}

	/**
	 * Adds a footer node to this element.
	 *
	 * @param children The children inside the footer node.
	 * @return This object.
	 */
	public Article footer(Object...children) {
		super.child(HtmlBuilder.footer(children));
		return this;
	}

	/**
	 * Adds a header node to this element.
	 *
	 * @param children The children inside the header node.
	 * @return This object.
	 */
	public Article header(Object...children) {
		super.child(HtmlBuilder.header(children));
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * Adds a link node to this element.
	 *
	 * @param value The link node to add to this article.
	 * @return This object.
	 */
	public Article link(Link value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * Adds a section node to this element.
	 *
	 * @param value The section node to add to this article.
	 * @return This object.
	 */
	public Article section(Section value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Article translate(Object value) {
		super.translate(value);
		return this;
	}
}