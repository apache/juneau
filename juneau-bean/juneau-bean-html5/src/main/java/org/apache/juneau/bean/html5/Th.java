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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-th-element">&lt;th&gt;</a>
 * element.
 *
 * <p>
 * The th element represents a header cell in a table. It is used to contain header information
 * for a column or row, providing context and meaning to the data cells (td) in the table. The th
 * element supports various attributes for accessibility, spanning multiple columns or rows, and
 * defining the relationship between header and data cells.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple header cell</jc>
 * 	Th <jv>simple</jv> = <jsm>th</jsm>(<js>"Name"</js>);
 *
 * 	<jc>// Header cell with scope</jc>
 * 	Th <jv>scoped</jv> = <jsm>th</jsm>(<js>"Age"</js>)
 * 		.scope(<js>"col"</js>);
 *
 * 	<jc>// Header cell spanning multiple columns</jc>
 * 	Th <jv>colspan</jv> = <jsm>th</jsm>(<js>"Contact Information"</js>)
 * 		.colspan(2);
 *
 * 	<jc>// Header cell with abbreviation</jc>
 * 	Th <jv>abbreviated</jv> = <jsm>th</jsm>(<js>"Quantity"</js>)
 * 		.abbr(<js>"Qty"</js>);
 *
 * 	<jc>// Header cell with sorting</jc>
 * 	Th <jv>sorted</jv> = <jsm>th</jsm>(<js>"Price"</js>)
 * 		.sorted(<js>"asc"</js>);
 *
 * 	<jc>// Header cell with styling</jc>
 * 	Th <jv>styled</jv> = <jsm>th</jsm>(<js>"Status"</js>)
 * 		._class(<js>"header-cell"</js>);
 *
 * 	<jc>// Header cell with complex content</jc>
 * 	Th <jv>complex</jv> = <jsm>th</jsm>(
 * 		.children(
 * 			new Strong().children("Total"),
 * 			" ",
 * 			new Small().children("(USD)")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="th")
public class Th extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Th} element.
	 */
	public Th() {}

	/**
	 * Creates a {@link Th} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Th(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-abbr">abbr</a> attribute.
	 *
	 * <p>
	 * Specifies an alternative, abbreviated label for the header cell. This is used by screen readers
	 * and other assistive technologies when referencing the cell in other contexts.
	 *
	 * <p>
	 * The abbreviation should be shorter than the full header text but still meaningful.
	 *
	 * @param abbr The abbreviated label for the header cell.
	 * @return This object.
	 */
	public Th abbr(String value) {
		attr("abbr", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-colspan">colspan</a> attribute.
	 *
	 * <p>
	 * Number of columns that the cell is to span.
	 *
	 * @param colspan
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Th colspan(Object value) {
		attr("colspan", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-headers">headers</a> attribute.
	 *
	 * <p>
	 * Specifies the IDs of header cells that apply to this table cell. This creates a programmatic
	 * relationship between the cell and its headers for accessibility purposes.
	 *
	 * <p>
	 * Multiple IDs can be specified as a space-separated list.
	 *
	 * @param headers The IDs of header cells that apply to this cell.
	 * @return This object.
	 */
	public Th headers(String value) {
		attr("headers", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-rowspan">rowspan</a> attribute.
	 *
	 * <p>
	 * Number of rows that the cell is to span.
	 *
	 * @param rowspan
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Th rowspan(Object value) {
		attr("rowspan", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-scope">scope</a> attribute.
	 *
	 * <p>
	 * Specifies which cells the header cell applies to. This helps define the relationship
	 * between header and data cells for accessibility.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 *  	<li><js>"row"</js> - Header applies to all cells in the same row</li>
	 *  	<li><js>"col"</js> - Header applies to all cells in the same column</li>
	 *  	<li><js>"rowgroup"</js> - Header applies to all cells in the same row group</li>
	 *  	<li><js>"colgroup"</js> - Header applies to all cells in the same column group</li>
	 * </ul>
	 *
	 * @param scope Which cells the header cell applies to.
	 * @return This object.
	 */
	public Th scope(String value) {
		attr("scope", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-sorted">sorted</a> attribute.
	 *
	 * <p>
	 * Specifies the sort direction and ordinality of the column. This indicates how the table
	 * is currently sorted and which column is the primary sort key.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 *  	<li><js>"asc"</js> - Column is sorted in ascending order</li>
	 *  	<li><js>"desc"</js> - Column is sorted in descending order</li>
	 *  	<li><js>"asc 1"</js> - Column is the primary sort key in ascending order</li>
	 *  	<li><js>"desc 1"</js> - Column is the primary sort key in descending order</li>
	 * </ul>
	 *
	 * @param sorted The sort direction and ordinality of the column.
	 * @return This object.
	 */
	public Th sorted(String value) {
		attr("sorted", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Th _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Th translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Th child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Th children(Object...value) {
		super.children(value);
		return this;
	}
}