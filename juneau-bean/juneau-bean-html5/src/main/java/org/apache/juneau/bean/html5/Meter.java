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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-meter-element">&lt;meter&gt;</a>
 * element.
 *
 * <p>
 * The meter element represents a scalar measurement within a known range, or a fractional value.
 * It is used to display a gauge or meter showing a value within a defined range, such as disk
 * usage, memory usage, or progress. The meter element is not suitable for representing a range
 * of values (use the input element with type="range" for that). It is typically rendered as a
 * visual gauge or bar that shows the current value relative to the minimum and maximum values.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple meter</jc>
 * 	Meter <jv>simple</jv> = <jsm>meter</jsm>()
 * 		.value(50)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with styling</jc>
 * 	Meter <jv>styled</jv> = <jsm>meter</jsm>()
 * 		._class(<js>"progress-meter"</js>)
 * 		.value(75)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with complex content</jc>
 * 	Meter <jv>complex</jv> = <jsm>meter</jsm>()
 * 		.value(60)
 * 		.min(0)
 * 		.max(100)
 * 		.low(25)
 * 		.high(75)
 * 		.optimum(50);
 *
 * 	<jc>// Meter with ID</jc>
 * 	Meter <jv>withId</jv> = <jsm>meter</jsm>()
 * 		.id(<js>"disk-usage"</js>)
 * 		.value(80)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with styling</jc>
 * 	Meter <jv>styled2</jv> = <jsm>meter</jsm>()
 * 		.style(<js>"width: 200px; height: 20px;"</js>)
 * 		.value(40)
 * 		.min(0)
 * 		.max(100);
 *
 * 	<jc>// Meter with multiple attributes</jc>
 * 	Meter <jv>multiple</jv> = <jsm>meter</jsm>()
 * 		.value(85)
 * 		.min(0)
 * 		.max(100)
 * 		.low(20)
 * 		.high(80)
 * 		.optimum(50)
 * 		.title(<js>"Disk Usage: 85%"</js>);
 *
 * 	// Meter with form
 * 	Meter withForm = new Meter()
 * 		.form("usage-form")
 * 		.value(30)
 * 		.min(0)
 * 		.max(100);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="meter")
public class Meter extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Meter} element.
	 */
	public Meter() {}

	/**
	 * Creates a {@link Meter} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Meter(Object...children) {
		children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Meter _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Meter child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Meter children(Object...value) {
		super.children(value);
		return this;
	}
	@Override /* Overridden from HtmlElement */
	public Meter contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter hidden(Object value) {
		super.hidden(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-high">high</a> attribute.
	 *
	 * <p>
	 * Low limit of high range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter high(Object value) {
		attr("high", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-low">low</a> attribute.
	 *
	 * <p>
	 * High limit of low range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter low(Object value) {
		attr("low", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-max">max</a> attribute.
	 *
	 * <p>
	 * Upper bound of range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter max(Object value) {
		attr("max", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-min">min</a> attribute.
	 *
	 * <p>
	 * Lower bound of range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter min(Object value) {
		attr("min", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-optimum">optimum</a> attribute.
	 *
	 * <p>
	 * Optimum value in gauge.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter optimum(Object value) {
		attr("optimum", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Meter translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-meter-value">value</a> attribute.
	 *
	 * <p>
	 * Current value of the element.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Meter value(Object value) {
		attr("value", value);
		return this;
	}
}