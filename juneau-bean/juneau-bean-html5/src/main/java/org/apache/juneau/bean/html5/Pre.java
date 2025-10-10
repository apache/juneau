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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-pre-element">&lt;pre&gt;</a>
 * element.
 *
 * <p>
 * The pre element represents a block of preformatted text, in which structure is represented
 * by typographic conventions rather than by elements. It is used to display text exactly as
 * it is written, preserving whitespace, line breaks, and formatting. The pre element is
 * typically rendered in a monospace font and is commonly used for displaying code snippets,
 * ASCII art, or any text where formatting and spacing are important.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	// Simple preformatted text
 * 	Pre simple = new Pre()
 * 		.children("This text preserves\n  all formatting\n    and spacing.");
 *
 * 	// Pre with styling
 * 	Pre styled = new Pre()
 * 		._class("code-block")
 * 		.children("function hello() {\n  return 'Hello World';\n}");
 *
 * 	// Pre with complex content
 * 	Pre complex = new Pre()
 * 		.children(
 * 			"  ",
 * 			new Strong().children("Bold text"),
 * 			" in preformatted content\n",
 * 			"  ",
 * 			new Em().children("Italic text"),
 * 			" with preserved formatting"
 * 		);
 *
 * 	// Pre with ID
 * 	Pre withId = new Pre()
 * 		.id("code-example")
 * 		.children("console.log('Hello World');");
 *
 * 	// Pre with styling
 * 	Pre styled2 = new Pre()
 * 		.style("background-color: #f4f4f4; padding: 10px; border: 1px solid #ddd;")
 * 		.children("This is styled preformatted text.");
 *
 * 	// Pre with multiple elements
 * 	Pre multiple = new Pre()
 * 		.children(
 * 			"Line 1: ",
 * 			new Span()._class("keyword").children("function"),
 * 			" ",
 * 			new Span()._class("function-name").children("example"),
 * 			"() {\n",
 * 			"Line 2:   ",
 * 			new Span()._class("keyword").children("return"),
 * 			" ",
 * 			new Span()._class("string").children("'Hello'"),
 * 			";\n",
 * 			"Line 3: }"
 * 		);
 *
 * 	// Pre with code
 * 	Pre withCode = new Pre()
 * 		.children(
 * 			new Code().children("const message = 'Hello World';\nconsole.log(message);")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="pre")
@FluentSetters
public class Pre extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Pre} element.
	 */
	public Pre() {}

	/**
	 * Creates a {@link Pre} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Pre(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Xml(format=MIXED_PWS)
	@Beanp(dictionary=HtmlBeanDictionary.class, name="c")
	@Override
	public List<Object> getChildren() {
		return super.getChildren();
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Pre translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Pre child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementMixed */
	public Pre children(Object...value) {
		super.children(value);
		return this;
	}

	// </FluentSetters>
}