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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-script-element">&lt;script&gt;</a>
 * element.
 *
 * <p>
 * The script element is used to embed or reference executable code, typically JavaScript. It can
 * contain inline script code or reference external script files. The script element is commonly
 * used to add interactivity and dynamic behavior to web pages.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Inline JavaScript</jc>
 * 	Script <jv>script1</jv> = <jsm>script</jsm>()
 * 		.text(<js>"console.log('Hello, World!');"</js>);
 *
 * 	<jc>// External JavaScript file</jc>
 * 	Script <jv>script2</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/script.js"</js>)
 * 		.type(<js>"text/javascript"</js>);
 *
 * 	<jc>// Async script loading</jc>
 * 	Script <jv>script3</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/analytics.js"</js>)
 * 		.async(<jk>true</jk>)
 * 		.defer(<jk>true</jk>);
 *
 * 	<jc>// Script with integrity check</jc>
 * 	Script <jv>script4</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/library.js"</js>)
 * 		.integrity(<js>"sha384-..."</js>)
 * 		.crossorigin(<js>"anonymous"</js>);
 *
 * 	<jc>// Module script</jc>
 * 	Script <jv>script5</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/module.js"</js>)
 * 		.type(<js>"module"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#script() script()}
 * 		<li class='jm'>{@link HtmlBuilder#script(String, String...) script(String, String...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "script")
public class Script extends HtmlElementRawText {

	/**
	 * Creates an empty {@link Script} element.
	 */
	public Script() {}

	/**
	 * Creates a {@link Script} element with the specified {@link Script#type(String)} attribute and
	 * {@link Script#text(Object)} node.
	 *
	 * @param type The {@link Script#type(String)} attribute.
	 * @param text The child text node.
	 */
	public Script(String type, String...text) {
		type(type).text(Utils.joinnl(text));
	}

	@Override /* Overridden from HtmlElement */
	public Script _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-async">async</a> attribute.
	 *
	 * <p>
	 * Execute script asynchronously.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"async"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Script async(Object value) {
		attr("async", deminimize(value, "async"));
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-charset">charset</a> attribute.
	 *
	 * <p>
	 * Specifies the character encoding of the external script resource. This is used when
	 * the script is loaded from an external source.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"utf-8"</js> - UTF-8 encoding (default)</li>
	 * 	<li><js>"iso-8859-1"</js> - Latin-1 encoding</li>
	 * 	<li><js>"windows-1252"</js> - Windows-1252 encoding</li>
	 * </ul>
	 *
	 * @param value The character encoding of the external script resource.
	 * @return This object.
	 */
	public Script charset(String value) {
		attr("charset", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-crossorigin">crossorigin</a>
	 * attribute.
	 *
	 * <p>
	 * How the element handles cross-origin requests.
	 *
	 * @param value The new value for this attribute.
	 * @return This object.
	 */
	public Script crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-defer">defer</a> attribute.
	 *
	 * <p>
	 * Defer script execution.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"defer"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Script defer(Object value) {
		attr("defer", deminimize(value, "defer"));
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-src">src</a> attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object.
	 */
	public Script src(Object value) {
		attrUri("src", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElementRawText */
	public Script text(Object value) {
		super.text(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Script translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-type">type</a> attribute.
	 *
	 * <p>
	 * Type of embedded resource.
	 *
	 * @param value The new value for this attribute.
	 * @return This object.
	 */
	public Script type(String value) {
		attr("type", value);
		return this;
	}
}