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
package org.apache.juneau.rest.beans;

import org.apache.juneau.bean.html5.*;

/**
 * Defines a simple hyperlink class.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Produces &lt;a href=&quot;/foo&quot;&gt;bar&lt;/a&gt;</jc>
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> Hyperlink a01() {
 * 			<jk>return new</jk> Hyperlink(<js>"foo"</js>, <js>"bar"</js>);
 * 		}
 *
 * 		<jc>// Produces &lt;ul&gt;&lt;li&gt;&lt;a href=&quot;/foo&quot;&gt;bar&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;</jc>
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> Hyperlink[] a02() {
 * 			<jk>return new</jk> Hyperlink[]{a01()};
 * 		}
 *
 * 		<jc>// Produces &lt;ul&gt;&lt;li&gt;&lt;a href=&quot;/foo&quot;&gt;bar&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;</jc>
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> Collection&lt;Hyperlink&gt; a03() {
 * 			<jk>return</jk> Arrays.<jsm>asList</jsm>(a02());
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * </ul>
 */
public class Hyperlink extends A {
	/**
	 * Static creator.
	 *
	 * @param href The {@link A#href(Object)} attribute.
	 * @param children The {@link A#children(Object[])} nodes.
	 * @return A new {@link Hyperlink} object.
	 */
	public static Hyperlink create(Object href, Object...children) {
		return new Hyperlink(href, children);
	}

	/**
	 * Creates an empty {@link A} element.
	 */
	public Hyperlink() {}

	/**
	 * Creates an {@link A} element with the specified {@link A#href(Object)} attribute and {@link A#children(Object[])}
	 * nodes.
	 *
	 * @param href The {@link A#href(Object)} attribute.
	 * @param children The {@link A#children(Object[])} nodes.
	 */
	public Hyperlink(Object href, Object...children) {
		super(href, children);
	}
	// A-specific attributes

	@Override /* Overridden from A */
	public Hyperlink _class(String value) {
		super._class(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink children(Object...value) {
		super.children(value);
		return this;
	}

	// Global HTML attributes

	@Override /* Overridden from A */
	public Hyperlink contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink download(Object value) {
		super.download(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink href(Object value) {
		super.href(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink hreflang(String value) {
		super.hreflang(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	// Event handler attributes

	@Override /* Overridden from A */
	public Hyperlink oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink rel(String value) {
		super.rel(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	// Child/attribute methods

	@Override /* Overridden from A */
	public Hyperlink target(String value) {
		super.target(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from A */
	public Hyperlink type(String value) {
		super.type(value);
		return this;
	}
}