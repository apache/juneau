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

class Video_Test extends TestBase {

	@Test void a01_basicSetters() {
		Video x = video()
			.autoplay("a")
			.controls("b")
			.crossorigin("c")
			.height("d")
			.loop("e")
			.mediagroup("f")
			.muted("g")
			.poster("h")
			.preload("i")
			.src("j")
			.width("k")
			._class("l")
			.accesskey("m")
			.contenteditable("n")
			.dir("o")
			.hidden("p")
			.id("q")
			.lang("r")
			.onabort("s")
			.onblur("t")
			.oncancel("u")
			.oncanplay("v")
			.oncanplaythrough("w")
			.onchange("x")
			.onclick("y")
			.oncuechange("z")
			.ondblclick("aa")
			.ondurationchange("ab")
			.onemptied("ac")
			.onended("ad")
			.onerror("ae")
			.onfocus("af")
			.oninput("ag")
			.oninvalid("ah")
			.onkeydown("ai")
			.onkeypress("aj")
			.onkeyup("ak")
			.onload("al")
			.onloadeddata("am")
			.onloadedmetadata("an")
			.onloadstart("ao")
			.onmousedown("ap")
			.onmouseenter("aq")
			.onmouseleave("ar")
			.onmousemove("as")
			.onmouseout("at")
			.onmouseover("au")
			.onmouseup("av")
			.onmousewheel("aw")
			.onpause("ax")
			.onplay("ay")
			.onplaying("az")
			.onprogress("ba")
			.onratechange("bb")
			.onreset("bc")
			.onresize("bd")
			.onscroll("be")
			.onseeked("bf")
			.onseeking("bg")
			.onselect("bh")
			.onshow("bi")
			.onstalled("bj")
			.onsubmit("bk")
			.onsuspend("bl")
			.ontimeupdate("bm")
			.ontoggle("bn")
			.onvolumechange("bo")
			.onwaiting("bp")
			.spellcheck("bq")
			.style("br")
			.tabindex("bs")
			.title("bt")
			.translate("bu")
			.child("child1")
			.children("bv", strong("bw"));

		assertString(
			"<video autoplay='a' controls='b' crossorigin='c' height='d' loop='e' mediagroup='f' muted='g' poster='h' preload='i' src='j' width='k' class='l' accesskey='m' contenteditable='n' dir='o' hidden='p' id='q' lang='r' onabort='s' onblur='t' oncancel='u' oncanplay='v' oncanplaythrough='w' onchange='x' onclick='y' oncuechange='z' ondblclick='aa' ondurationchange='ab' onemptied='ac' onended='ad' onerror='ae' onfocus='af' oninput='ag' oninvalid='ah' onkeydown='ai' onkeypress='aj' onkeyup='ak' onload='al' onloadeddata='am' onloadedmetadata='an' onloadstart='ao' onmousedown='ap' onmouseenter='aq' onmouseleave='ar' onmousemove='as' onmouseout='at' onmouseover='au' onmouseup='av' onmousewheel='aw' onpause='ax' onplay='ay' onplaying='az' onprogress='ba' onratechange='bb' onreset='bc' onresize='bd' onscroll='be' onseeked='bf' onseeking='bg' onselect='bh' onshow='bi' onstalled='bj' onsubmit='bk' onsuspend='bl' ontimeupdate='bm' ontoggle='bn' onvolumechange='bo' onwaiting='bp' spellcheck='bq' style='br' tabindex='bs' title='bt' translate='bu'>child1bv<strong>bw</strong></video>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<video></video>", video());
	}

	@Test void a03_otherConstructors() {
		Video x1 = new Video("a");
		assertString("<video src='a'></video>", x1);

	}
}