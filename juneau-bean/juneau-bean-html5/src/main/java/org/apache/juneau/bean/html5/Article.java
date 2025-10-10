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
package org.apache.juneau.bean.html5;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 	// Simple blog post article
 * 	Article blogPost = new Article()
 * 		.children(
 * 			new Header()
 * 				.children(
 * 					new H1().children("How to Use HTML5 Semantic Elements"),
 * 					new P().children("Published on ", new Time().datetime("2024-01-15").children("January 15, 2024"))
 * 				),
 * 			new P().children("HTML5 introduced several semantic elements that help structure content..."),
 * 			new Footer()
 * 				.children(
 * 					new P().children("Author: John Doe"),
 * 					new Address().children("Contact: john@example.com")
 * 				)
 * 		);
 * 
 * 	// News article
 * 	Article newsArticle = new Article()
 * 		.children(
 * 			new Header()
 * 				.children(
 * 					new H1().children("Breaking: New Technology Released"),
 * 					new P().children("By Jane Smith, Technology Reporter")
 * 				),
 * 			new P().children("A revolutionary new technology was announced today..."),
 * 			new Section()
 * 				.children(
 * 					new H2().children("Technical Details"),
 * 					new P().children("The technology works by...")
 * 				)
 * 		);
 * 
 * 	// Forum post
 * 	Article forumPost = new Article()
 * 		._class("forum-post")
 * 		.children(
 * 			new Header()
 * 				.children(
 * 					new H3().children("Question about CSS Grid"),
 * 					new P().children("Posted by ", new Strong().children("user123"), " on ", new Time().datetime("2024-01-14").children("yesterday"))
 * 				),
 * 			new P().children("I'm having trouble with CSS Grid layout..."),
 * 			new Footer()
 * 				.children(
 * 					new P().children("Tags: ", new A().href("/tag/css").children("CSS"), ", ", new A().href("/tag/grid").children("Grid"))
 * 				)
 * 		);
 * 
 * 	// Product review
 * 	Article review = new Article()
 * 		.children(
 * 			new Header()
 * 				.children(
 * 					new H1().children("Review: Amazing Widget Pro"),
 * 					new P().children("Rating: ", new Strong().children("5/5 stars"))
 * 				),
 * 			new P().children("After using the Amazing Widget Pro for a month..."),
 * 			new Section()
 * 				.children(
 * 					new H2().children("Pros"),
 * 					new Ul().children(
 * 						new Li().children("Easy to use"),
 * 						new Li().children("Great performance"),
 * 						new Li().children("Excellent support")
 * 					)
 * 				)
 * 		);
 * 
 * 	// Article with multiple sections
 * 	Article multiSection = new Article()
 * 		.children(
 * 			new Header()
 * 				.children(new H1().children("Complete Guide to Web Development")),
 * 			new Section()
 * 				.children(
 * 					new H2().children("Introduction"),
 * 					new P().children("Web development encompasses many technologies...")
 * 				),
 * 			new Section()
 * 				.children(
 * 					new H2().children("Frontend Development"),
 * 					new P().children("Frontend development focuses on...")
 * 				),
 * 			new Section()
 * 				.children(
 * 					new H2().children("Backend Development"),
 * 					new P().children("Backend development handles...")
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="article")
@FluentSetters
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
	 * Adds a link node to this element.
	 *
	 * @param link The link node to add to this article.
	 * @return This object.
	 */
	public Article link(Link value) {
		super.child(value);
		return this;
	}

	/**
	 * Adds a section node to this element.
	 *
	 * @param section The section node to add to this article.
	 * @return This object.
	 */
	public Article section(Section value) {
		super.child(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Article translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Article child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Article children(Object...value) {
		super.children(value);
		return this;
	}

	// </FluentSetters>
}