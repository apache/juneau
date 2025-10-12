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
import org.apache.juneau.html.*;
import org.junit.jupiter.api.*;

class Audio_Test extends TestBase {

	@Test void a01_basicSetters() {
		Audio x = audio()
			.autoplay("a")
			.controls("b")
			.crossorigin("c")
			.loop("d")
			.mediagroup("e")
			.muted("f")
			.preload("g")
			.src("h")
			._class("i")
			.accesskey("j")
			.contenteditable("k")
			.dir("l")
			.hidden("m")
			.id("n")
			.lang("o")
			.onabort("p")
			.onblur("q")
			.oncancel("r")
			.oncanplay("s")
			.oncanplaythrough("t")
			.onchange("u")
			.onclick("v")
			.oncuechange("w")
			.ondblclick("x")
			.ondurationchange("y")
			.onemptied("z")
			.onended("aa")
			.onerror("ab")
			.onfocus("ac")
			.oninput("ad")
			.oninvalid("ae")
			.onkeydown("af")
			.onkeypress("ag")
			.onkeyup("ah")
			.onload("ai")
			.onloadeddata("aj")
			.onloadedmetadata("ak")
			.onloadstart("al")
			.onmousedown("am")
			.onmouseenter("an")
			.onmouseleave("ao")
			.onmousemove("ap")
			.onmouseout("aq")
			.onmouseover("ar")
			.onmouseup("as")
			.onmousewheel("at")
			.onpause("au")
			.onplay("av")
			.onplaying("aw")
			.onprogress("ax")
			.onratechange("ay")
			.onreset("az")
			.onresize("ba")
			.onscroll("bb")
			.onseeked("bc")
			.onseeking("bd")
			.onselect("be")
			.onshow("bf")
			.onstalled("bg")
			.onsubmit("bh")
			.onsuspend("bi")
			.ontimeupdate("bj")
			.ontoggle("bk")
			.onvolumechange("bl")
			.onwaiting("bm")
			.spellcheck("bn")
			.style("bo")
			.tabindex("bp")
			.title("bq")
			.translate("br")
			.child("child1")
			.children("bs", strong("bt"));

		assertString(
			"<audio autoplay='a' controls='b' crossorigin='c' loop='d' mediagroup='e' muted='f' preload='g' src='h' class='i' accesskey='j' contenteditable='k' dir='l' hidden='m' id='n' lang='o' onabort='p' onblur='q' oncancel='r' oncanplay='s' oncanplaythrough='t' onchange='u' onclick='v' oncuechange='w' ondblclick='x' ondurationchange='y' onemptied='z' onended='aa' onerror='ab' onfocus='ac' oninput='ad' oninvalid='ae' onkeydown='af' onkeypress='ag' onkeyup='ah' onload='ai' onloadeddata='aj' onloadedmetadata='ak' onloadstart='al' onmousedown='am' onmouseenter='an' onmouseleave='ao' onmousemove='ap' onmouseout='aq' onmouseover='ar' onmouseup='as' onmousewheel='at' onpause='au' onplay='av' onplaying='aw' onprogress='ax' onratechange='ay' onreset='az' onresize='ba' onscroll='bb' onseeked='bc' onseeking='bd' onselect='be' onshow='bf' onstalled='bg' onsubmit='bh' onsuspend='bi' ontimeupdate='bj' ontoggle='bk' onvolumechange='bl' onwaiting='bm' spellcheck='bn' style='bo' tabindex='bp' title='bq' translate='br'>child1bs<strong>bt</strong></audio>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<audio></audio>", audio());
	}

	@Test void a03_otherConstructors() {
		Audio x1 = new Audio("a");
		assertString("<audio src='a'></audio>", x1);

	}

	@Test void a04_textNodeDelimiter() {
		// Test default behavior (no delimiter)
		Audio x1 = audio().children("a", "b", strong("c"));
		assertString("<audio>ab<strong>c</strong></audio>", x1);

		// Test with space delimiter
		HtmlSerializer serializer = HtmlSerializer.create()
			.textNodeDelimiter(" ")
			.disableJsonTags()
			.build();
		Audio x2 = audio().children("a", "b", strong("c"));
		assertString("<audio>a b<strong>c</strong></audio>", serializer.toString(x2));

		// Test with custom delimiter
		HtmlSerializer serializer2 = HtmlSerializer.create()
			.textNodeDelimiter(" | ")
			.disableJsonTags()
			.build();
		Audio x3 = audio().children("a", "b", strong("c"));
		assertString("<audio>a | b<strong>c</strong></audio>", serializer2.toString(x3));

		// Test that delimiter is not added between text and element
		Audio x4 = audio().children("text", strong("bold"));
		assertString("<audio>text<strong>bold</strong></audio>", serializer.toString(x4));

		// Test with element between text nodes
		Audio x5 = audio().children("before", strong("middle"), "after");
		assertString("<audio>before<strong>middle</strong>after</audio>", serializer.toString(x5));
	}
}
