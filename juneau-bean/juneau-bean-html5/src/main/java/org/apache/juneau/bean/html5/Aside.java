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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-aside-element">&lt;aside&gt;</a>
 * element.
 *
 * <p>
 * The aside element represents a section of a page that consists of content that is tangentially
 * related to the content around the aside element, and which could be considered separate from
 * that content. It is commonly used for sidebars, pull quotes, advertisements, navigation links,
 * or other content that is related but not essential to the main content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Sidebar with related links</jc>
 * 	Aside <jv>sidebar</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"Related Articles"</js>),
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article1"</js>, <js>"Article 1"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article2"</js>, <js>"Article 2"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article3"</js>, <js>"Article 3"</js>))
 * 		)
 * 	)._class(<js>"sidebar"</js>);
 *
 * 	<jc>// Pull quote</jc>
 * 	Aside <jv>pullQuote</jv> = <jsm>aside</jsm>(
 * 		<jsm>blockquote</jsm>(
 * 			<js>"The best way to predict the future is to create it."</js>,
 * 			<jsm>footer</jsm>(<js>"— Peter Drucker"</js>)
 * 		)
 * 	)._class(<js>"pull-quote"</js>);
 *
 * 	<jc>// Advertisement</jc>
 * 	Aside <jv>advertisement</jv> = <jsm>aside</jsm>(
 * 		<jsm>h4</jsm>(<js>"Sponsored Content"</js>),
 * 		<jsm>p</jsm>(<js>"Check out our latest product!"</js>),
 * 		<jsm>a</jsm>(<js>"/product"</js>, <js>"Learn More"</js>)
 * 	)._class(<js>"advertisement"</js>);
 *
 * 	<jc>// Author bio</jc>
 * 	Aside <jv>authorBio</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"About the Author"</js>),
 * 		<jsm>p</jsm>(<js>"John Doe is a web developer with 10 years of experience..."</js>),
 * 		<jsm>a</jsm>(<js>"/author/john-doe"</js>, <js>"Read more articles"</js>)
 * 	)._class(<js>"author-bio"</js>);
 *
 * 	<jc>// Navigation menu</jc>
 * 	Aside <jv>navigation</jv> = <jsm>aside</jsm>(
 * 		<jsm>nav</jsm>(
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>))
 * 			)
 * 		)
 * 	)._class(<js>"navigation"</js>);
 *
 * 	<jc>// Glossary or definitions</jc>
 * 	Aside <jv>glossary</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"Key Terms"</js>),
 * 		<jsm>dl</jsm>(
 * 			<jsm>dt</jsm>(<js>"HTML"</js>),
 * 			<jsm>dd</jsm>(<js>"HyperText Markup Language"</js>),
 * 			<jsm>dt</jsm>(<js>"CSS"</js>),
 * 			<jsm>dd</jsm>(<js>"Cascading Style Sheets"</js>)
 * 		)
 * 	)._class(<js>"glossary"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#aside() aside()}
 * 		<li class='jm'>{@link HtmlBuilder#aside(Object...) aside(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="aside")
public class Aside extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Aside} element.
	 */
	public Aside() {}

	/**
	 * Creates an {@link Aside} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Aside(Object...children) {
		children(children);
	}
	@Override /* Overridden from HtmlElement */
	public Aside _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Aside child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Aside children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Aside translate(Object value) {
		super.translate(value);
		return this;
	}
}