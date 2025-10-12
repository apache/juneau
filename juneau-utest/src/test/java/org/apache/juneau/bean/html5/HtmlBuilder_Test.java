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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HtmlBuilder_Test extends TestBase {

	@Test void a01_allStaticMethods() {
		assertString("<a></a>", a());
		assertString("<abbr></abbr>", abbr());
		assertString("<address></address>", address());
		assertString("<area/>", area());
		assertString("<article></article>", article());
		assertString("<aside></aside>", aside());
		assertString("<audio></audio>", audio());
		assertString("<b></b>", b());
		assertString("<base/>", base());
		assertString("<bdi></bdi>", bdi());
		assertString("<bdo></bdo>", bdo());
		assertString("<blockquote></blockquote>", blockquote());
		assertString("<body></body>", body());
		assertString("<br/>", br());
		assertString("<button></button>", button());
		assertString("<canvas></canvas>", canvas());
		assertString("<caption></caption>", caption());
		assertString("<cite></cite>", cite());
		assertString("<code></code>", code());
		assertString("<col/>", col());
		assertString("<colgroup></colgroup>", colgroup());
		assertString("<data></data>", data());
		assertString("<datalist></datalist>", datalist());
		assertString("<dd></dd>", dd());
		assertString("<del></del>", del());
		assertString("<dfn></dfn>", dfn());
		assertString("<div></div>", div());
		assertString("<dl></dl>", dl());
		assertString("<dt></dt>", dt());
		assertString("<em></em>", em());
		assertString("<embed/>", embed());
		assertString("<fieldset></fieldset>", fieldset());
		assertString("<figcaption></figcaption>", figcaption());
		assertString("<figure></figure>", figure());
		assertString("<footer></footer>", footer());
		assertString("<form></form>", form());
		assertString("<h1></h1>", h1());
		assertString("<h2></h2>", h2());
		assertString("<h3></h3>", h3());
		assertString("<h4></h4>", h4());
		assertString("<h5></h5>", h5());
		assertString("<h6></h6>", h6());
		assertString("<head></head>", head());
		assertString("<header></header>", header());
		assertString("<hr/>", hr());
		assertString("<html></html>", html());
		assertString("<i></i>", i());
		assertString("<iframe></iframe>", iframe());
		assertString("<img/>", img());
		assertString("<input/>", input());
		assertString("<ins></ins>", ins());
		assertString("<kbd></kbd>", kbd());
		assertString("<keygen/>", keygen());
		assertString("<label></label>", label());
		assertString("<legend></legend>", legend());
		assertString("<li></li>", li());
		assertString("<link/>", link());
		assertString("<main></main>", main());
		assertString("<map></map>", map());
		assertString("<mark></mark>", mark());
		assertString("<meta/>", meta());
		assertString("<meter></meter>", meter());
		assertString("<nav></nav>", nav());
		assertString("<noscript></noscript>", noscript());
		assertString("<object></object>", object());
		assertString("<ol></ol>", ol());
		assertString("<optgroup></optgroup>", optgroup());
		assertString("<option></option>", option());
		assertString("<output></output>", output());
		assertString("<p></p>", p());
		assertString("<param/>", param());
		assertString("<pre></pre>", pre());
		assertString("<progress></progress>", progress());
		assertString("<q></q>", q());
		assertString("<rb></rb>", rb());
		assertString("<rp></rp>", rp());
		assertString("<rt></rt>", rt());
		assertString("<rtc></rtc>", rtc());
		assertString("<ruby></ruby>", ruby());
		assertString("<s></s>", s());
		assertString("<samp></samp>", samp());
		assertString("<script></script>", script());
		assertString("<section></section>", section());
		assertString("<select></select>", select());
		assertString("<small></small>", small());
		assertString("<source/>", source());
		assertString("<span></span>", span());
		assertString("<strong></strong>", strong());
		assertString("<style></style>", style());
		assertString("<sub></sub>", sub());
		assertString("<sup></sup>", sup());
		assertString("<table></table>", table());
		assertString("<tbody></tbody>", tbody());
		assertString("<td></td>", td());
		assertString("<template></template>", template());
		assertString("<textarea></textarea>", textarea());
		assertString("<tfoot></tfoot>", tfoot());
		assertString("<th></th>", th());
		assertString("<thead></thead>", thead());
		assertString("<time></time>", time());
		assertString("<title></title>", title());
		assertString("<tr></tr>", tr());
		assertString("<track/>", track());
		assertString("<u></u>", u());
		assertString("<ul></ul>", ul());
		assertString("<var></var>", var());
		assertString("<video></video>", video());
		assertString("<wbr/>", wbr());
		assertString("<a href='url'>text</a>", a("url", "text"));
		assertString("<form action='submit'><input/></form>", form("submit", input()));
		assertString("<button type='submit'>Click</button>", button("submit", "Click"));
		assertString("<input type='text'/>", input("text"));
		assertString("<option value='v'>text</option>", option("v", "text"));
		assertString("<select name='test'><option></option></select>", select("test", option()));
		assertString("<textarea name='test'>content</textarea>", textarea("test", "content"));
		assertString("<canvas width='100' height='100'></canvas>", canvas(100, 100));
		assertString("<audio src='test.mp3'></audio>", audio("test.mp3"));
		assertString("<colgroup><col/></colgroup>", colgroup(col()));
		assertString("<datalist id='test'><option></option></datalist>", datalist("test", option()));
		assertString("<dl><dt></dt></dl>", dl(dt()));
		assertString("<figure><img/></figure>", figure(img()));
		assertString("<html>en</html>", html("en"));
		assertString("<main><p></p></main>", main(p()));
		assertString("<map>test</map>", map("test"));
		assertString("<ol><li></li></ol>", ol(li()));
		assertString("<optgroup>test<option></option></optgroup>", optgroup("test", option()));
		assertString("<tbody><tr></tr></tbody>", tbody(tr()));
		assertString("<tfoot><tr></tr></tfoot>", tfoot(tr()));
		assertString("<thead><tr></tr></thead>", thead(tr()));
		assertString("<tr><td></td></tr>", tr(td()));
		assertString("<ul><li></li></ul>", ul(li()));
		assertString("<video src='test.mp4'></video>", video("test.mp4"));
		assertString("<abbr title='test'>content</abbr>", abbr("test", "content"));
		assertString("<area shape='rect' coords='0,0,100,100' href='url'/>", area("rect", "0,0,100,100", "url"));
		assertString("<bdo dir='rtl'>text</bdo>", bdo("rtl", "text"));
		assertString("<data value='12345'>Product</data>", data("12345", "Product"));
		assertString("<param name='autoplay' value='true'/>", param("autoplay", "true"));
		assertString("<script type='text/javascript'>alert('test');</script>", script("text/javascript", "alert('test');"));
		assertString("<source src='video.mp4' type='video/mp4'/>", source("video.mp4", "video/mp4"));
		assertString("<template id='test'><p></p></template>", template("test", p()));
		assertString("<track src='captions.vtt' kind='captions'/>", track("captions.vtt", "captions"));
		assertString("<address>text<strong>bold</strong></address>", address("text", strong("bold")));
		assertString("<time>10:00<span>AM</span></time>", time("10:00", span("AM")));
		assertString("<bdi>text</bdi>", bdi("text"));
		assertString("<button type='submit'></button>", button("submit"));
		assertString("<col span='2'/>", col(2));
		assertString("<form action='submit'></form>", form("submit"));
		assertString("<embed src='file.swf'/>", embed("file.swf"));
		assertString("<img src='image.jpg'/>", img("image.jpg"));
		assertString("<link href='style.css'/>", link("style.css"));
		assertString("<option>text</option>", option("text"));
		assertString("<output name='result'></output>", output("result"));
		assertString("<style>body { color: red; }</style>", style("body { color: red; }"));
		assertString("<style>body { color: red; }\ndiv { color: blue; }</style>", style("body { color: red; }", "div { color: blue; }"));
		assertString("<title>Page Title</title>", title("Page Title"));
		assertString("<h1>Heading 1</h1>", h1("Heading 1"));
		assertString("<h2>Heading 2</h2>", h2("Heading 2"));
		assertString("<h3>Heading 3</h3>", h3("Heading 3"));
		assertString("<h4>Heading 4</h4>", h4("Heading 4"));
		assertString("<h5>Heading 5</h5>", h5("Heading 5"));
		assertString("<h6>Heading 6</h6>", h6("Heading 6"));
	}
}