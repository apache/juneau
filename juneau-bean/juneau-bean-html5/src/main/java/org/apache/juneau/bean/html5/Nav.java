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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-nav-element">&lt;nav&gt;</a>
 * element.
 *
 * <p>
 * The nav element represents a section of a page that links to other pages or to parts within
 * the same page. It is used to contain navigation links and is typically used for site navigation,
 * table of contents, or pagination. The nav element should contain a list of links and is important
 * for accessibility as it helps screen readers identify the main navigation areas of a page.
 * It is commonly used with ul and li elements to create structured navigation menus.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	// Simple navigation
 * 	Nav simple = new Nav()
 * 		.children(
 * 			new Ul().children(
 * 				new Li().children(new A().href("/home").children("Home")),
 * 				new Li().children(new A().href("/about").children("About")),
 * 				new Li().children(new A().href("/contact").children("Contact"))
 * 			)
 * 		);
 *
 * 	// Nav with styling
 * 	Nav styled = new Nav()
 * 		._class("main-navigation")
 * 		.children(
 * 			new Ul().children(
 * 				new Li().children(new A().href("/products").children("Products")),
 * 				new Li().children(new A().href("/services").children("Services")),
 * 				new Li().children(new A().href("/support").children("Support"))
 * 			)
 * 		);
 *
 * 	// Nav with complex content
 * 	Nav complex = new Nav()
 * 		.children(
 * 			new H3().children("Site Navigation"),
 * 			new Ul().children(
 * 				new Li().children(new A().href("/home").children("Home")),
 * 				new Li().children(new A().href("/about").children("About")),
 * 				new Li().children(new A().href("/contact").children("Contact"))
 * 			)
 * 		);
 *
 * 	// Nav with ID
 * 	Nav withId = new Nav()
 * 		.id("main-nav")
 * 		.children(
 * 			new Ul().children(
 * 				new Li().children(new A().href("/home").children("Home")),
 * 				new Li().children(new A().href("/about").children("About"))
 * 			)
 * 		);
 *
 * 	// Nav with styling
 * 	Nav styled2 = new Nav()
 * 		.style("background-color: #333; padding: 10px;")
 * 		.children(
 * 			new Ul().children(
 * 				new Li().children(new A().href("/home").children("Home")),
 * 				new Li().children(new A().href("/about").children("About"))
 * 			)
 * 		);
 *
 * 	// Nav with multiple elements
 * 	Nav multiple = new Nav()
 * 		.children(
 * 			new H3().children("Navigation"),
 * 			new Ul().children(
 * 				new Li().children(new A().href("/home").children("Home")),
 * 				new Li().children(new A().href("/about").children("About"))
 * 			),
 * 			new P().children("Use the links above to navigate the site.")
 * 		);
 *
 * 	// Nav with breadcrumbs
 * 	Nav breadcrumbs = new Nav()
 * 		.children(
 * 			new Ul().children(
 * 				new Li().children(new A().href("/").children("Home")),
 * 				new Li().children(" > "),
 * 				new Li().children(new A().href("/products").children("Products")),
 * 				new Li().children(" > "),
 * 				new Li().children("Current Page")
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="nav")
@FluentSetters
public class Nav extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Nav} element.
	 */
	public Nav() {}

	/**
	 * Creates a {@link Nav} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Nav(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Nav translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Nav child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Nav children(Object...value) {
		super.children(value);
		return this;
	}

	// </FluentSetters>
}