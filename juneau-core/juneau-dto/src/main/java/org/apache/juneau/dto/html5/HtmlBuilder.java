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
package org.apache.juneau.dto.html5;

/**
 * Various useful static methods for creating HTML elements.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
public class HtmlBuilder {

	/**
	 * Creates an empty {@link A} element.
	 *
	 * @return The new element.
	 */
	public static final A a() {
		return new A();
	}

	/**
	 * Creates an {@link A} element with the specified {@link A#href(Object)} attribute and {@link A#children(Object[])}
	 * nodes.
	 *
	 * @param href The {@link A#href(Object)} attribute.
	 * @param children The {@link A#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final A a(Object href, Object...children) {
		return new A(href, children);
	}

	/**
	 * Creates an empty {@link Abbr} element.
	 *
	 * @return The new element.
	 */
	public static final Abbr abbr() {
		return new Abbr();
	}

	/**
	 * Creates an {@link Abbr} element with the specified {@link Abbr#title(String)} attribute and
	 * {@link Abbr#children(Object[])} nodes.
	 *
	 * @param title The {@link Abbr#title(String)} attribute.
	 * @param children The {@link Abbr#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Abbr abbr(String title, Object...children) {
		return new Abbr(title, children);
	}

	/**
	 * Creates an empty {@link Address} element.
	 *
	 * @return The new element.
	 */
	public static final Address address() {
		return new Address();
	}

	/**
	 * Creates an {@link Address} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Address address(Object...children) {
		return new Address(children);
	}

	/**
	 * Creates an empty {@link Area} element.
	 *
	 * @return The new element.
	 */
	public static final Area area() {
		return new Area();
	}

	/**
	 * Creates an {@link Area} element with the specified {@link Area#shape(String)}, {@link Area#coords(String)},
	 * and {@link Area#href(Object)} attributes.
	 *
	 * @param shape The {@link Area#shape(String)} attribute.
	 * @param coords The {@link Area#coords(String)} attribute.
	 * @param href The {@link Area#href(Object)} attribute.
	 * @return The new element.
	 */
	public static final Area area(String shape, String coords, Object href) {
		return new Area(shape, coords, href);
	}

	/**
	 * Creates an empty {@link Article} element.
	 *
	 * @return The new element.
	 */
	public static final Article article() {
		return new Article();
	}

	/**
	 * Creates an {@link Article} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Article article(Object...children) {
		return new Article(children);
	}

	/**
	 * Creates an empty {@link Aside} element.
	 *
	 * @return The new element.
	 */
	public static final Aside aside() {
		return new Aside();
	}

	/**
	 * Creates an {@link Aside} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Aside aside(Object...children) {
		return new Aside(children);
	}

	/**
	 * Creates an empty {@link Audio} element.
	 *
	 * @return The new element.
	 */
	public static final Audio audio() {
		return new Audio();
	}

	/**
	 * Creates an {@link Audio} element with the specified {@link Audio#src(Object)} attribute.
	 *
	 * @param src The {@link Audio#src(Object)} attribute.
	 * @return The new element.
	 */
	public static final Audio audio(String src) {
		return new Audio(src);
	}

	/**
	 * Creates an empty {@link B} element.
	 *
	 * @return The new element.
	 */
	public static final B b() {
		return new B();
	}

	/**
	 * Creates a {@link B} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final B b(Object...children) {
		return new B(children);
	}

	/**
	 * Creates an empty {@link Base} element.
	 *
	 * @return The new element.
	 */
	public static final Base base() {
		return new Base();
	}

	/**
	 * Creates a {@link Base} element with the specified {@link Base#href(Object)} attribute.
	 *
	 * @param href The {@link Base#href(Object)} attribute.
	 * @return The new element.
	 */
	public static final Base base(Object href) {
		return new Base(href);
	}

	/**
	 * Creates an empty {@link Bdi} element.
	 *
	 * @return The new element.
	 */
	public static final Bdi bdi() {
		return new Bdi();
	}

	/**
	 * Creates a {@link Bdi} element with the specified {@link Bdi#text(Object)} node.
	 *
	 * @param text The {@link Bdi#text(Object)} node.
	 * @return The new element.
	 */
	public static final Bdi bdi(Object text) {
		return new Bdi(text);
	}

	/**
	 * Creates an empty {@link Bdo} element.
	 *
	 * @return The new element.
	 */
	public static final Bdo bdo() {
		return new Bdo();
	}

	/**
	 * Creates a {@link Bdo} element with the specified {@link Bdo#dir(String)} attribute and child nodes.
	 *
	 * @param dir The {@link Bdo#dir(String)} attribute.
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Bdo bdo(String dir, Object...children) {
		return new Bdo(dir, children);
	}

	/**
	 * Creates an empty {@link Blockquote} element.
	 *
	 * @return The new element.
	 */
	public static final Blockquote blockquote() {
		return new Blockquote();
	}

	/**
	 * Creates a {@link Blockquote} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Blockquote blockquote(Object...children) {
		return new Blockquote(children);
	}

	/**
	 * Creates an empty {@link Body} element.
	 *
	 * @return The new element.
	 */
	public static final Body body() {
		return new Body();
	}

	/**
	 * Creates a {@link Body} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Body body(Object...children) {
		return new Body(children);
	}

	/**
	 * Creates an empty {@link Br} element.
	 *
	 * @return The new element.
	 */
	public static final Br br() {
		return new Br();
	}

	/**
	 * Creates an empty {@link Button} element.
	 *
	 * @return The new element.
	 */
	public static final Button button() {
		return new Button();
	}

	/**
	 * Creates a {@link Button} element with the specified {@link Button#type(String)} attribute.
	 *
	 * @param type The {@link Button#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Button button(String type) {
		return new Button(type);
	}

	/**
	 * Creates a {@link Button} element with the specified {@link Button#type(String)} attribute and
	 * {@link Button#children(Object[])} nodes.
	 *
	 * @param type The {@link Button#type(String)} attribute.
	 * @param children The {@link Button#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Button button(String type, Object...children) {
		return new Button(type, children);
	}

	/**
	 * Creates an empty {@link Canvas} element.
	 * @return The new element.
	 */
	public static final Canvas canvas() {
		return new Canvas();
	}

	/**
	 * Creates a {@link Canvas} element with the specified {@link Canvas#width(Object)} and
	 * {@link Canvas#height(Object)} attributes.
	 *
	 * @param width The {@link Canvas#width(Object)} attribute.
	 * @param height The {@link Canvas#height(Object)} attribute.
	 * @return The new element.
	 */
	public static final Canvas canvas(Number width, Number height) {
		return new Canvas(width, height);
	}

	/**
	 * Creates an empty {@link Caption} element.
	 *
	 * @return The new element.
	 */
	public static final Caption caption() {
		return new Caption();
	}

	/**
	 * Creates a {@link Caption} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Caption caption(Object...children) {
		return new Caption(children);
	}

	/**
	 * Creates an empty {@link Cite} element.
	 *
	 * @return The new element.
	 */
	public static final Cite cite() {
		return new Cite();
	}

	/**
	 * Creates a {@link Cite} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Cite cite(Object...children) {
		return new Cite(children);
	}

	/**
	 * Creates an empty {@link Code} element.
	 *
	 * @return The new element.
	 */
	public static final Code code() {
		return new Code();
	}

	/**
	 * Creates a {@link Code} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Code code(Object...children) {
		return new Code(children);
	}

	/**
	 * Creates an empty {@link Col} element.
	 *
	 * @return The new element.
	 *
	 */
	public static final Col col() {
		return new Col();
	}

	/**
	 * Creates a {@link Col} element with the specified {@link Col#span(Object)} attribute.
	 *
	 * @param span The {@link Col#span(Object)} attribute.
	 * @return The new element.
	 */
	public static final Col col(Number span) {
		return new Col(span);
	}

	/**
	 * Creates an empty {@link Colgroup} element.
	 *
	 * @return The new element.
	 */
	public static final Colgroup colgroup() {
		return new Colgroup();
	}

	/**
	 * Creates a {@link Colgroup} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Colgroup colgroup(Object...children) {
		return new Colgroup(children);
	}

	/**
	 * Creates an empty {@link Data} element.
	 *
	 * @return The new element.
	 */
	public static final Data data() {
		return new Data();
	}

	/**
	 * Creates a {@link Data} element with the specified {@link Data#value(Object)} attribute and child node.
	 *
	 * @param value The {@link Data#value(Object)} attribute.
	 * @param child The child node.
	 * @return The new element.
	 */
	public static final Data data(String value, Object child) {
		return new Data(value, child);
	}

	/**
	 * Creates an empty {@link Datalist} element.
	 *
	 * @return The new element.
	 */
	public static final Datalist datalist() {
		return new Datalist();
	}

	/**
	 * Creates a {@link Datalist} element with the specified {@link Datalist#id(String)} attribute and child nodes.
	 *
	 * @param id The {@link Datalist#id(String)} attribute.
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Datalist datalist(String id, Option...children) {
		return new Datalist(id, children);
	}

	/**
	 * Creates an empty {@link Dd} element.
	 *
	 * @return The new element.
	 */
	public static final Dd dd() {
		return new Dd();
	}

	/**
	 * Creates a {@link Dd} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Dd dd(Object...children) {
		return new Dd(children);
	}

	/**
	 * Creates an empty {@link Del} element.
	 *
	 * @return The new element.
	 */
	public static final Del del() {
		return new Del();
	}

	/**
	 * Creates a {@link Del} element with the specified {@link Del#children(Object[])} node.
	 *
	 * @param children The {@link Del#children(Object[])} node.
	 * @return The new element.
	 */
	public static final Del del(Object...children) {
		return new Del(children);
	}

	/**
	 * Creates an empty {@link Dfn} element.
	 *
	 * @return The new element.
	 */
	public static final Dfn dfn() {
		return new Dfn();
	}

	/**
	 * Creates a {@link Dfn} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Dfn dfn(Object...children) {
		return new Dfn(children);
	}

	/**
	 * Creates an empty {@link Div} element.
	 *
	 * @return The new element.
	 */
	public static final Div div() {
		return new Div();
	}

	/**
	 * Creates a {@link Div} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Div div(Object...children) {
		return new Div(children);
	}

	/**
	 * Creates an empty {@link Dl} element.
	 *
	 * @return The new element.
	 */
	public static final Dl dl() {
		return new Dl();
	}

	/**
	 * Creates a {@link Dl} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Dl dl(Object...children) {
		return new Dl(children);
	}

	/**
	 * Creates an empty {@link Dt} element.
	 *
	 * @return The new element.
	 */
	public static final Dt dt() {
		return new Dt();
	}

	/**
	 * Creates a {@link Dt} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Dt dt(Object...children) {
		return new Dt(children);
	}

	/**
	 * Creates an empty {@link Em} element.
	 *
	 * @return The new element.
	 */
	public static final Em em() {
		return new Em();
	}

	/**
	 * Creates an {@link Em} element with the specified {@link Em#children(Object[])} nodes.
	 *
	 * @param children The {@link Em#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Em em(Object...children) {
		return new Em(children);
	}

	/**
	 * Creates an empty {@link Embed} element.
	 *
	 * @return The new element.
	 */
	public static final Embed embed() {
		return new Embed();
	}

	/**
	 * Creates an {@link Embed} element with the specified {@link Embed#src(Object)} attribute.
	 *
	 * @param src The {@link Embed#src(Object)} attribute.
	 * @return The new element.
	 */
	public static final Embed embed(Object src) {
		return new Embed(src);
	}

	/**
	 * Creates an empty {@link Fieldset} element.
	 *
	 * @return The new element.
	 */
	public static final Fieldset fieldset() {
		return new Fieldset();
	}

	/**
	 * Creates a {@link Fieldset} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Fieldset fieldset(Object...children) {
		return new Fieldset(children);
	}

	/**
	 * Creates an empty {@link Figcaption} element.
	 *
	 * @return The new element.
	 */
	public static final Figcaption figcaption() {
		return new Figcaption();
	}

	/**
	 * Creates a {@link Figcaption} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Figcaption figcaption(Object...children) {
		return new Figcaption(children);
	}

	/**
	 * Creates an empty {@link Figure} element.
	 *
	 * @return The new element.
	 */
	public static final Figure figure() {
		return new Figure();
	}

	/**
	 * Creates a {@link Figure} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Figure figure(Object...children) {
		return new Figure(children);
	}

	/**
	 * Creates an empty {@link Footer} element.
	 *
	 * @return The new element.
	 */
	public static final Footer footer() {
		return new Footer();
	}

	/**
	 * Creates a {@link Footer} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Footer footer(Object...children) {
		return new Footer(children);
	}

	/**
	 * Creates an empty {@link Form} element.
	 *
	 * @return The new element.
	 */
	public static final Form form() {
		return new Form();
	}

	/**
	 * Creates a {@link Form} element with the specified {@link Form#action(String)} attribute.
	 *
	 * @param action The {@link Form#action(String)} attribute.
	 * @return The new element.
	 */
	public static final Form form(String action) {
		return new Form(action);
	}

	/**
	 * Creates an {@link Form} element with the specified {@link Form#action(String)} attribute and child nodes.
	 *
	 * @param action The {@link Form#action(String)} attribute.
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Form form(String action, Object...children) {
		return new Form(action, children);
	}

	/**
	 * Creates an empty {@link H1} element.
	 *
	 * @return The new element.
	 */
	public static final H1 h1() {
		return new H1();
	}

	/**
	 * Creates an {@link H1} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H1 h1(Object...children) {
		return new H1(children);
	}

	/**
	 * Creates an empty {@link H2} element.
	 *
	 * @return The new element.
	 */
	public static final H2 h2() {
		return new H2();
	}

	/**
	 * Creates an {@link H2} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H2 h2(Object...children) {
		return new H2(children);
	}

	/**
	 * Creates an empty {@link H3} element.
	 *
	 * @return The new element.
	 */
	public static final H3 h3() {
		return new H3();
	}

	/**
	 * Creates an {@link H3} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H3 h3(Object...children) {
		return new H3(children);
	}

	/**
	 * Creates an empty {@link H4} element.
	 *
	 * @return The new element.
	 */
	public static final H4 h4() {
		return new H4();
	}

	/**
	 * Creates an {@link H4} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H4 h4(Object...children) {
		return new H4(children);
	}

	/**
	 * Creates an empty {@link H5} element.
	 *
	 * @return The new element.
	 */
	public static final H5 h5() {
		return new H5();
	}

	/**
	 * Creates an {@link H5} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H5 h5(Object...children) {
		return new H5(children);
	}

	/**
	 * Creates an empty {@link H6} element.
	 * @return The new element.
	 */
	public static final H6 h6() {
		return new H6();
	}

	/**
	 * Creates an {@link H6} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final H6 h6(Object...children) {
		return new H6(children);
	}

	/**
	 * Creates an empty {@link Head} element.
	 *
	 * @return The new element.
	 */
	public static final Head head() {
		return new Head();
	}

	/**
	 * Creates a {@link Head} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Head head(Object...children) {
		return new Head(children);
	}

	/**
	 * Creates an empty {@link Header} element.
	 *
	 * @return The new element.
	 */
	public static final Header header() {
		return new Header();
	}

	/**
	 * Creates a {@link Header} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Header header(Object...children) {
		return new Header(children);
	}

	/**
	 * Creates an empty {@link Hr} element.
	 *
	 * @return The new element.
	 */
	public static final Hr hr() {
		return new Hr();
	}

	/**
	 * Creates an empty {@link Html} element.
	 *
	 * @return The new element.
	 */
	public static final Html html() {
		return new Html();
	}

	/**
	 * Creates an {@link Html} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Html html(Object...children) {
		return new Html(children);
	}

	/**
	 * Creates an empty {@link I} element.
	 *
	 * @return The new element.
	 */
	public static final I i() {
		return new I();
	}

	/**
	 * Creates an {@link I} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final I i(Object...children) {
		return new I(children);
	}

	/**
	 * Creates an empty {@link Iframe} element.
	 *
	 * @return The new element.
	 */
	public static final Iframe iframe() {
		return new Iframe();
	}

	/**
	 * Creates an {@link Iframe} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Iframe iframe(Object...children) {
		return new Iframe(children);
	}

	/**
	 * Creates an empty {@link Img} element.
	 *
	 * @return The new element.
	 */
	public static final Img img() {
		return new Img();
	}

	/**
	 * Creates an {@link Img} element with the specified {@link Img#src(Object)} attribute.
	 *
	 * @param src The {@link Img#src(Object)} attribute.
	 * @return The new element.
	 */
	public static final Img img(Object src) {
		return new Img(src);
	}

	/**
	 * Creates an empty {@link Input} element.
	 *
	 * @return The new element.
	 */
	public static final Input input() {
		return new Input();
	}

	/**
	 * Creates an {@link Input} element with the specified {@link Input#type(String)} attribute.
	 *
	 * @param type The {@link Input#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Input input(String type) {
		return new Input(type);
	}

	/**
	 * Creates an empty {@link Ins} element.
	 *
	 * @return The new element.
	 */
	public static final Ins ins() {
		return new Ins();
	}

	/**
	 * Creates an {@link Ins} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Ins ins(Object...children) {
		return new Ins(children);
	}

	/**
	 * Creates an empty {@link Kbd} element.
	 *
	 * @return The new element.
	 */
	public static final Kbd kbd() {
		return new Kbd();
	}

	/**
	 * Creates a {@link Kbd} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Kbd kbd(Object...children) {
		return new Kbd(children);
	}

	/**
	 * Creates an empty {@link Keygen} element.
	 *
	 * @return The new element.
	 */
	public static final Keygen keygen() {
		return new Keygen();
	}

	/**
	 * Creates an empty {@link Label} element.
	 *
	 * @return The new element.
	 */
	public static final Label label() {
		return new Label();
	}

	/**
	 * Creates a {@link Label} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Label label(Object...children) {
		return new Label(children);
	}

	/**
	 * Creates an empty {@link Legend} element.
	 *
	 * @return The new element.
	 */
	public static final Legend legend() {
		return new Legend();
	}

	/**
	 * Creates a {@link Legend} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Legend legend(Object...children) {
		return new Legend(children);
	}

	/**
	 * Creates an empty {@link Li} element.
	 *
	 * @return The new element.
	 */
	public static final Li li() {
		return new Li();
	}

	/**
	 * Creates an {@link Li} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Li li(Object...children) {
		return new Li(children);
	}

	/**
	 * Creates an empty {@link Link} element.
	 *
	 * @return The new element.
	 */
	public static final Link link() {
		return new Link();
	}

	/**
	 * Creates a {@link Link} element with the specified {@link Link#href(Object)} attribute.
	 *
	 * @param href The {@link Link#href(Object)} attribute.
	 * @return The new element.
	 */
	public static final Link link(Object href) {
		return new Link(href);
	}

	/**
	 * Creates an empty {@link Main} element.
	 *
	 * @return The new element.
	 */
	public static final Main main() {
		return new Main();
	}

	/**
	 * Creates a {@link Main} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Main main(Object...children) {
		return new Main(children);
	}

	/**
	 * Creates an empty {@link Map} element.
	 *
	 * @return The new element.
	 */
	public static final Map map() {
		return new Map();
	}

	/**
	 * Creates a {@link Map} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Map map(Object...children) {
		return new Map(children);
	}

	/**
	 * Creates an empty {@link Mark} element.
	 *
	 * @return The new element.
	 */
	public static final Mark mark() {
		return new Mark();
	}

	/**
	 * Creates a {@link Mark} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Mark mark(Object...children) {
		return new Mark(children);
	}

	/**
	 * Creates an empty {@link Meta} element.
	 *
	 * @return The new element.
	 */
	public static final Meta meta() {
		return new Meta();
	}

	/**
	 * Creates an empty {@link Meter} element.
	 *
	 * @return The new element.
	 */
	public static final Meter meter() {
		return new Meter();
	}

	/**
	 * Creates a {@link Meter} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Meter meter(Object...children) {
		return new Meter(children);
	}

	/**
	 * Creates an empty {@link Nav} element.
	 *
	 * @return The new element.
	 */
	public static final Nav nav() {
		return new Nav();
	}

	/**
	 * Creates a {@link Nav} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Nav nav(Object...children) {
		return new Nav(children);
	}

	/**
	 * Creates an empty {@link Noscript} element.
	 *
	 * @return The new element.
	 */
	public static final Noscript noscript() {
		return new Noscript();
	}

	/**
	 * Creates a {@link Noscript} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Noscript noscript(Object...children) {
		return new Noscript(children);
	}

	/**
	 * Creates an empty {@link Object_} element.
	 *
	 * @return The new element.
	 */
	public static final Object_ object() {
		return new Object_();
	}

	/**
	 * Creates an {@link Object_} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Object_ object(Object...children) {
		return new Object_(children);
	}

	/**
	 * Creates an empty {@link Ol} element.
	 *
	 * @return The new element.
	 */
	public static final Ol ol() {
		return new Ol();
	}

	/**
	 * Creates an {@link Ol} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Ol ol(Object...children) {
		return new Ol(children);
	}

	/**
	 * Creates an empty {@link Optgroup} element.
	 *
	 * @return The new element.
	 */
	public static final Optgroup optgroup() {
		return new Optgroup();
	}

	/**
	 * Creates an {@link Optgroup} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Optgroup optgroup(Object...children) {
		return new Optgroup(children);
	}

	/**
	 * Creates an empty {@link Option} element.
	 *
	 * @return The new element.
	 */
	public static final Option option() {
		return new Option();
	}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#text(Object)} attribute.
	 *
	 * @param text The {@link Option#text(Object)} attribute.
	 * @return The new element.
	 */
	public static final Option option(Object text) {
		return new Option(text);
	}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#value(Object)} attribute and
	 * {@link Option#text(Object)} node.
	 *
	 * @param value The {@link Option#value(Object)} attribute.
	 * @param text The {@link Option#text(Object)} node.
	 * @return The new element.
	 */
	public static final Option option(Object value, Object text) {
		return new Option(value, text);
	}

	/**
	 * Creates an empty {@link Output} element.
	 *
	 * @return The new element.
	 */
	public static final Output output() {
		return new Output();
	}

	/**
	 * Creates an {@link Output} element with the specified {@link Output#name(String)} attribute.
	 *
	 * @param name The {@link Output#name(String)} attribute.
	 * @return The new element.
	 */
	public static final Output output(String name) {
		return new Output(name);
	}

	/**
	 * Creates an empty {@link P} element.
	 *
	 * @return The new element.
	 */
	public static final P p() {
		return new P();
	}

	/**
	 * Creates a {@link P} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final P p(Object...children) {
		return new P(children);
	}

	/**
	 * Creates an empty {@link Param} element.
	 *
	 * @return The new element.
	 */
	public static final Param param() {
		return new Param();
	}

	/**
	 * Creates a {@link Param} element with the specified {@link Param#name(String)} and {@link Param#value(Object)}
	 * attributes.
	 *
	 * @param name The {@link Param#name(String)} attribute.
	 * @param value The {@link Param#value(Object)} attribute.
	 * @return The new element.
	 */
	public static final Param param(String name, Object value) {
		return new Param(name, value);
	}

	/**
	 * Creates an empty {@link Pre} element.
	 *
	 * @return The new element.
	 */
	public static final Pre pre() {
		return new Pre();
	}

	/**
	 * Creates a {@link Pre} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Pre pre(Object...children) {
		return new Pre(children);
	}

	/**
	 * Creates an empty {@link Progress} element.
	 *
	 * @return The new element.
	 */
	public static final Progress progress() {
		return new Progress();
	}

	/**
	 * Creates a {@link Progress} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Progress progress(Object...children) {
		return new Progress(children);
	}

	/**
	 * Creates an empty {@link Q} element.
	 *
	 * @return The new element.
	 */
	public static final Q q() {
		return new Q();
	}

	/**
	 * Creates a {@link Q} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Q q(Object...children) {
		return new Q(children);
	}

	/**
	 * Creates an empty {@link Rb} element.
	 *
	 * @return The new element.
	 */
	public static final Rb rb() {
		return new Rb();
	}

	/**
	 * Creates a {@link Rb} element with the specified {@link Rb#children(Object[])} nodes.
	 *
	 * @param children The {@link Rb#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Rb rb(Object...children) {
		return new Rb(children);
	}

	/**
	 * Creates an empty {@link Rp} element.
	 *
	 * @return The new element.
	 */
	public static final Rp rp() {
		return new Rp();
	}

	/**
	 * Creates a {@link Rp} element with the specified {@link Rp#children(Object[])} nodes.
	 *
	 * @param children The {@link Rp#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Rp rp(Object...children) {
		return new Rp(children);
	}

	/**
	 * Creates an empty {@link Rt} element.
	 *
	 * @return The new element.
	 */
	public static final Rt rt() {
		return new Rt();
	}

	/**
	 * Creates a {@link Rt} element with the specified {@link Rt#children(Object[])} nodes.
	 *
	 * @param children The {@link Rt#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Rt rt(Object...children) {
		return new Rt(children);
	}

	/**
	 * Creates an empty {@link Rtc} element.
	 *
	 * @return The new element.
	 */
	public static final Rtc rtc() {
		return new Rtc();
	}

	/**
	 * Creates an {@link Rtc} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Rtc rtc(Object...children) {
		return new Rtc(children);
	}

	/**
	 * Creates an empty {@link Ruby} element.
	 *
	 * @return The new element.
	 */
	public static final Ruby ruby() {
		return new Ruby();
	}

	/**
	 * Creates a {@link Ruby} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Ruby ruby(Object...children) {
		return new Ruby(children);
	}

	/**
	 * Creates an empty {@link S} element.
	 *
	 * @return The new element.
	 */
	public static final S s() {
		return new S();
	}

	/**
	 * Creates an {@link S} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final S s(Object...children) {
		return new S(children);
	}

	/**
	 * Creates an empty {@link Samp} element.
	 *
	 * @return The new element.
	 */
	public static final Samp samp() {
		return new Samp();
	}

	/**
	 * Creates a {@link Samp} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Samp samp(Object...children) {
		return new Samp(children);
	}

	/**
	 * Creates an empty {@link Script} element.
	 *
	 * @return The new element.
	 */
	public static final Script script() {
		return new Script();
	}

	/**
	 * Creates a {@link Script} element with the specified {@link Script#type(String)} attribute and
	 * {@link Script#text(Object)} node.
	 *
	 * @param type The {@link Script#type(String)} attribute.
	 * @param text The child text node.
	 * @return The new element.
	 */
	public static final Script script(String type, String...text) {
		return new Script(type, text);
	}

	/**
	 * Creates an empty {@link Section} element.
	 *
	 * @return The new element.
	 */
	public static final Section section() {
		return new Section();
	}

	/**
	 * Creates a {@link Section} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Section section(Object...children) {
		return new Section(children);
	}

	/**
	 * Creates an empty {@link Select} element.
	 *
	 * @return The new element.
	 */
	public static final Select select() {
		return new Select();
	}

	/**
	 * Creates a {@link Select} element with the specified {@link Select#name(String)} attribute and child nodes.
	 *
	 * @param name The {@link Select#name(String)} attribute.
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Select select(String name, Object...children) {
		return new Select(name, children);
	}

	/**
	 * Creates an empty {@link Small} element.
	 *
	 * @return The new element.
	 */
	public static final Small small() {
		return new Small();
	}

	/**
	 * Creates a {@link Small} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Small small(Object...children) {
		return new Small(children);
	}

	/**
	 * Creates an empty {@link Source} element.
	 *
	 * @return The new element.
	 */
	public static final Source source() {
		return new Source();
	}

	/**
	 * Creates a {@link Source} element with the specified {@link Source#src(Object)} and {@link Source#type(String)}
	 * attributes.
	 *
	 * @param src The {@link Source#src(Object)} attribute.
	 * @param type The {@link Source#type(String)} attribute.
	 * @return The new element.
	 */
	public static final Source source(Object src, String type) {
		return new Source(src, type);
	}

	/**
	 * Creates an empty {@link Span} element.
	 *
	 * @return The new element.
	 */
	public static final Span span() {
		return new Span();
	}

	/**
	 * Creates a {@link Span} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Span span(Object...children) {
		return new Span(children);
	}

	/**
	 * Creates an empty {@link Strong} element.
	 *
	 * @return The new element.
	 */
	public static final Strong strong() {
		return new Strong();
	}

	/**
	 * Creates a {@link Strong} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Strong strong(Object...children) {
		return new Strong(children);
	}

	/**
	 * Creates an empty {@link Style} element.
	 *
	 * @return The new element.
	 */
	public static final Style style() {
		return new Style();
	}

	/**
	 * Creates a {@link Style} element with the specified {@link Style#text(Object)} node.
	 *
	 * @param text The {@link Style#text(Object)} node.
	 * @return The new element.
	 */
	public static final Style style(Object text) {
		return new Style(text);
	}

	/**
	 * Creates a {@link Style} element with the specified inner text.
	 *
	 * @param text
	 * 	The contents of the style element.
	 * 	<br>Values will be concatenated with newlines.
	 * @return The new element.
	 */
	public static final Style style(String...text) {
		return new Style(text);
	}

	/**
	 * Creates an empty {@link Sub} element.
	 *
	 * @return The new element.
	 */
	public static final Sub sub() {
		return new Sub();
	}

	/**
	 * Creates a {@link Sub} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Sub sub(Object...children) {
		return new Sub(children);
	}

	/**
	 * Creates an empty {@link Sup} element.
	 *
	 * @return The new element.
	 */
	public static final Sup sup() {
		return new Sup();
	}

	/**
	 * Creates a {@link Sup} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Sup sup(Object...children) {
		return new Sup(children);
	}

	/**
	 * Creates an empty {@link Table} element.
	 *
	 * @return The new element.
	 */
	public static final Table table() {
		return new Table();
	}

	/**
	 * Creates a {@link Table} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Table table(Object...children) {
		return new Table(children);
	}

	/**
	 * Creates an empty {@link Tbody} element.
	 *
	 * @return The new element.
	 */
	public static final Tbody tbody() {
		return new Tbody();
	}

	/**
	 * Creates a {@link Tbody} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Tbody tbody(Object...children) {
		return new Tbody(children);
	}

	/**
	 * Creates an empty {@link Td} element.
	 *
	 * @return The new element.
	 */
	public static final Td td() {
		return new Td();
	}

	/**
	 * Creates a {@link Td} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Td td(Object...children) {
		return new Td(children);
	}

	/**
	 * Creates an empty {@link Template} element.
	 *
	 * @return The new element.
	 */
	public static final Template template() {
		return new Template();
	}

	/**
	 * Creates a {@link Template} element with the specified {@link Template#id(String)} attribute and child nodes.
	 *
	 * @param id The {@link Template#id(String)} attribute.
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Template template(String id, Object...children) {
		return new Template(id, children);
	}

	/**
	 * Creates an empty {@link Textarea} element.
	 *
	 * @return The new element.
	 */
	public static final Textarea textarea() {
		return new Textarea();
	}

	/**
	 * Creates a {@link Textarea} element with the specified {@link Textarea#name(String)} attribute and
	 * {@link Textarea#text(Object)} node.
	 *
	 * @param name The {@link Textarea#name(String)} attribute.
	 * @param text The {@link Textarea#text(Object)} node.
	 * @return The new element.
	 */
	public static final Textarea textarea(String name, String text) {
		return new Textarea(name, text);
	}

	/**
	 * Creates an empty {@link Tfoot} element.
	 *
	 * @return The new element.
	 */
	public static final Tfoot tfoot() {
		return new Tfoot();
	}

	/**
	 * Creates a {@link Tfoot} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Tfoot tfoot(Object...children) {
		return new Tfoot(children);
	}

	/**
	 * Creates an empty {@link Th} element.
	 *
	 * @return The new element.
	 */
	public static final Th th() {
		return new Th();
	}

	/**
	 * Creates a {@link Th} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Th th(Object...children) {
		return new Th(children);
	}

	/**
	 * Creates an empty {@link Thead} element.
	 *
	 * @return The new element.
	 */
	public static final Thead thead() {
		return new Thead();
	}

	/**
	 * Creates a {@link Thead} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Thead thead(Object...children) {
		return new Thead(children);
	}

	/**
	 * Creates an empty {@link Time} element.
	 *
	 * @return The new element.
	 */
	public static final Time time() {
		return new Time();
	}

	/**
	 * Creates a {@link Time} element with the specified {@link Time#children(Object[])} nodes.
	 *
	 * @param children The {@link Time#children(Object[])} nodes.
	 * @return The new element.
	 */
	public static final Time time(Object...children) {
		return new Time(children);
	}

	/**
	 * Creates an empty {@link Title} element.
	 *
	 * @return The new element.
	 */
	public static final Title title() {
		return new Title();
	}

	/**
	 * Creates a {@link Title} element with the specified {@link Title#text(Object)} node.
	 *
	 * @param text The {@link Title#text(Object)} node.
	 * @return The new element.
	 */
	public static final Title title(String text) {
		return new Title(text);
	}

	/**
	 * Creates an empty {@link Tr} element.
	 *
	 * @return The new element.
	 */
	public static final Tr tr() {
		return new Tr();
	}

	/**
	 * Creates a {@link Tr} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Tr tr(Object...children) {
		return new Tr(children);
	}

	/**
	 * Creates an empty {@link Track} element.
	 *
	 * @return The new element.
	 */
	public static final Track track() {
		return new Track();
	}

	/**
	 * Creates a {@link Track} element with the specified {@link Track#src(Object)} and {@link Track#kind(String)}
	 * attributes.
	 *
	 * @param src The {@link Track#src(Object)} attribute.
	 * @param kind The {@link Track#kind(String)} attribute.
	 * @return The new element.
	 */
	public static final Track track(Object src, String kind) {
		return new Track(src, kind);
	}

	/**
	 * Creates an empty {@link U} element.
	 *
	 * @return The new element.
	 */
	public static final U u() {
		return new U();
	}

	/**
	 * Creates a {@link U} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final U u(Object...children) {
		return new U(children);
	}

	/**
	 * Creates an empty {@link Ul} element.
	 *
	 * @return The new element.
	 */
	public static final Ul ul() {
		return new Ul();
	}

	/**
	 * Creates a {@link Ul} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Ul ul(Object...children) {
		return new Ul(children);
	}

	/**
	 * Creates an empty {@link Var} element.
	 *
	 * @return The new element.
	 */
	public static final Var var() {
		return new Var();
	}

	/**
	 * Creates a {@link Var} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 * @return The new element.
	 */
	public static final Var var(Object...children) {
		return new Var(children);
	}

	/**
	 * Creates an empty {@link Video} element.
	 *
	 * @return The new element.
	 */
	public static final Video video() {
		return new Video();
	}

	/**
	 * Creates a {@link Video} element with the specified {@link Video#src(Object)} attribute.
	 *
	 * @param src The {@link Video#src(Object)} attribute.
	 * @return The new element.
	 */
	public static final Video video(Object src) {
		return new Video(src);
	}

	/**
	 * Creates an empty {@link Wbr} element.
	 *
	 * @return The new element.
	 */
	public static final Wbr wbr() {
		return new Wbr();
	}
}
