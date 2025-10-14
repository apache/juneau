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
import org.apache.juneau.internal.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-figure-element">&lt;figure&gt;</a>
 * element.
 *
 * <p>
 * The figure element represents self-contained content, potentially with a caption, that is
 * typically referenced as a single unit from the main flow of the document. It is used to
 * group related content such as images, diagrams, code snippets, or other media that can
 * be moved away from the main flow of the document without affecting the document's meaning.
 * The figure element can contain a figcaption element to provide a caption for the content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple figure with image</jc>
 * 	Figure <jv>simple</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/sunset.jpg"</js>, <js>"Sunset"</js>),
 * 		<jsm>figcaption</jsm>(<js>"A beautiful sunset over the mountains."</js>)
 * 	);
 *
 * 	<jc>// Figure with code snippet</jc>
 * 	Figure <jv>codeFigure</jv> = <jsm>figure</jsm>(
 * 		<jsm>pre</jsm>(<jsm>code</jsm>(<js>"function hello() {\n  return 'Hello World';\n}"</js>)),
 * 		<jsm>figcaption</jsm>(<js>"A simple JavaScript function."</js>)
 * 	);
 *
 * 	<jc>// Figure with styling</jc>
 * 	Figure <jv>styled</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/charts/sales.png"</js>, <js>"Sales Chart"</js>),
 * 		<jsm>figcaption</jsm>(<js>"Monthly sales data for 2024."</js>)
 * 	)._class(<js>"chart-figure"</js>);
 *
 * 	<jc>// Figure with multiple elements</jc>
 * 	Figure <jv>complex</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/diagram.png"</js>, <js>"Process Diagram"</js>),
 * 		<jsm>p</jsm>(<js>"This diagram shows the complete workflow."</js>),
 * 		<jsm>figcaption</jsm>(<js>"Figure 1: System Architecture Overview."</js>)
 * 	);
 *
 * 	<jc>// Figure with ID</jc>
 * 	Figure <jv>withId</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/charts/main.png"</js>, <js>"Main Chart"</js>),
 * 		<jsm>figcaption</jsm>(<js>"Primary performance metrics."</js>)
 * 	).id(<js>"main-chart"</js>);
 *
 * 	<jc>// Figure with styling</jc>
 * 	Figure <jv>styled2</jv> = <jsm>figure</jsm>(
 * 		<jsm>img</jsm>(<js>"/images/example.png"</js>, <js>"Example"</js>),
 * 		<jsm>figcaption</jsm>(<js>"An example of the new feature."</js>)
 * 	).style(<js>"border: 1px solid #ccc; padding: 10px; margin: 20px 0;"</js>);
 *
 * 	<jc>// Figure with table</jc>
 * 	Figure <jv>tableFigure</jv> = <jsm>figure</jsm>(
 * 		<jsm>table</jsm>(
 * 			<jsm>tr</jsm>(
 * 				<jsm>th</jsm>(<js>"Name"</js>),
 * 				<jsm>th</jsm>(<js>"Value"</js>)
 * 			),
 * 			<jsm>tr</jsm>(
 * 				<jsm>td</jsm>(<js>"Item 1"</js>),
 * 				<jsm>td</jsm>(<js>"100"</js>)
 * 			)
 * 		),
 * 		<jsm>figcaption</jsm>(<js>"Data summary table."</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#figure() figure()}
 * 		<li class='jm'>{@link HtmlBuilder#figure(Object, Object...) figure(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="figure")
public class Figure extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Figure} element.
	 */
	public Figure() {}

	/**
	 * Creates a {@link Figure} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Figure(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Figure _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Figure translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Figure child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Figure children(Object...value) {
		super.children(value);
		return this;
	}
}