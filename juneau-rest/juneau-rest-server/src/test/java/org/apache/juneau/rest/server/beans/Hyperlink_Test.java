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
package org.apache.juneau.rest.server.beans;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link Hyperlink}.
 *
 * <p>
 * {@link Hyperlink} is a real, reachable public API bean (see e.g. {@code juneau-petstore}'s
 * {@code PetInfoResource} and {@code Hyperlink_Test} in {@code juneau-integration-tests}, which exercises it
 * end-to-end via {@code MockRestClient}). This module ({@code juneau-rest-server}) had no test of its own
 * exercising the ~60 covariant-override fluent setters declared directly on {@link Hyperlink}, so this class's
 * own coverage was near-0% even though the type is actively used.
 */
class Hyperlink_Test extends TestBase {

	@Test void a01_emptyBean() {
		assertEquals("<a></a>", new Hyperlink().toString());
	}

	@Test void a02_hrefAndChildrenConstructor() {
		assertEquals("<a href='foo'>bar</a>", new Hyperlink("foo", "bar").toString());
	}

	@Test void a03_staticCreator() {
		assertEquals("<a href='foo'>bar</a>", Hyperlink.create("foo", "bar").toString());
	}

	@Test void a04_allFluentSetters() {
		Hyperlink x = new Hyperlink()
			.class_("a")
			.accesskey("b")
			.attr("data-foo", "c")
			.attrUri("data-bar", "d")
			.child("e1")
			.children("e2", "e3")
			.contenteditable("f")
			.dir("g")
			.download("h")
			.hidden("i")
			.href("j")
			.hreflang("k")
			.id("l")
			.lang("m")
			.onabort("n")
			.onblur("o")
			.oncancel("p")
			.oncanplay("q")
			.oncanplaythrough("r")
			.onchange("s")
			.onclick("t")
			.oncuechange("u")
			.ondblclick("v")
			.ondurationchange("w")
			.onemptied("x")
			.onended("y")
			.onerror("z")
			.onfocus("aa")
			.oninput("ab")
			.oninvalid("ac")
			.onkeydown("ad")
			.onkeypress("ae")
			.onkeyup("af")
			.onload("ag")
			.onloadeddata("ah")
			.onloadedmetadata("ai")
			.onloadstart("aj")
			.onmousedown("ak")
			.onmouseenter("al")
			.onmouseleave("am")
			.onmousemove("an")
			.onmouseout("ao")
			.onmouseover("ap")
			.onmouseup("aq")
			.onmousewheel("ar")
			.onpause("as")
			.onplay("at")
			.onplaying("au")
			.onprogress("av")
			.onratechange("aw")
			.onreset("ax")
			.onresize("ay")
			.onscroll("az")
			.onseeked("ba")
			.onseeking("bb")
			.onselect("bc")
			.onshow("bd")
			.onstalled("be")
			.onsubmit("bf")
			.onsuspend("bg")
			.ontimeupdate("bh")
			.ontoggle("bi")
			.onvolumechange("bj")
			.onwaiting("bk")
			.rel("bl")
			.spellcheck("bm")
			.style("bn")
			.tabindex("bo")
			.target("bp")
			.title("bq")
			.translate("br")
			.type("bs");

		assertEquals(
			"<a class='a' accesskey='b' data-foo='c' data-bar='d' contenteditable='f' dir='g' download='h' hidden='i' href='j' hreflang='k' id='l' lang='m' onabort='n' onblur='o' oncancel='p' oncanplay='q' oncanplaythrough='r' onchange='s' onclick='t' oncuechange='u' ondblclick='v' ondurationchange='w' onemptied='x' onended='y' onerror='z' onfocus='aa' oninput='ab' oninvalid='ac' onkeydown='ad' onkeypress='ae' onkeyup='af' onload='ag' onloadeddata='ah' onloadedmetadata='ai' onloadstart='aj' onmousedown='ak' onmouseenter='al' onmouseleave='am' onmousemove='an' onmouseout='ao' onmouseover='ap' onmouseup='aq' onmousewheel='ar' onpause='as' onplay='at' onplaying='au' onprogress='av' onratechange='aw' onreset='ax' onresize='ay' onscroll='az' onseeked='ba' onseeking='bb' onselect='bc' onshow='bd' onstalled='be' onsubmit='bf' onsuspend='bg' ontimeupdate='bh' ontoggle='bi' onvolumechange='bj' onwaiting='bk' rel='bl' spellcheck='bm' style='bn' tabindex='bo' target='bp' title='bq' translate='br' type='bs'>e1e2e3</a>",
			x.toString()
		);
	}
}
