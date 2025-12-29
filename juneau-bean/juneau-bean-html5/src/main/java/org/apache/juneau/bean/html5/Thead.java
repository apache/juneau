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

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-thead-element">&lt;thead&gt;</a>
 * element.
 *
 * <p>
 * The thead element represents a group of rows that consist of the column labels (headers) for
 * the parent table element. It is used to group header rows of a table, separating them from the
 * body (tbody) and footer (tfoot) sections. The thead element can contain multiple tr elements
 * and is typically used to display column headers and other header information.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table header</jc>
 * 	Thead <jv>simple</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Name"</js>),
 * 			<jsm>th</jsm>(<js>"Age"</js>),
 * 			<jsm>th</jsm>(<js>"City"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Table header with styling</jc>
 * 	Thead <jv>styled</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Product"</js>),
 * 			<jsm>th</jsm>(<js>"Price"</js>),
 * 			<jsm>th</jsm>(<js>"Stock"</js>)
 * 		)
 * 	)._class(<js>"table-header"</js>);
 *
 * 	<jc>// Table header with multiple rows</jc>
 * 	Thead <jv>multiple</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Contact Information"</js>).colspan(2),
 * 			<jsm>th</jsm>(<js>"Address"</js>).colspan(2)
 * 		),
 * 			new Tr()
 * 				.children(
 * 					new Th().children("Name"),
 * 					new Th().children("Phone"),
 * 					new Th().children("Street"),
 * 					new Th().children("City")
 * 				)
 * 		);
 *
 * 	// Table header with sorting
 * 	Thead sortable = new Thead()
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Th().sorted("asc").children("Name"),
 * 					new Th().sorted("desc").children("Date"),
 * 					new Th().children("Status")
 * 				)
 * 		);
 *
 * 	// Table header with accessibility
 * 	Thead accessible = new Thead()
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Th().scope("col").children("ID"),
 * 					new Th().scope("col").children("Description"),
 * 					new Th().scope("col").children("Amount")
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "thead")
public class Thead extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Thead} element.
	 */
	public Thead() {}

	/**
	 * Creates a {@link Thead} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Thead(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Thead _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Thead child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Thead children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Thead setChildren(List<Object> children) {
		super.setChildren(children);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Thead translate(Object value) {
		super.translate(value);
		return this;
	}
}