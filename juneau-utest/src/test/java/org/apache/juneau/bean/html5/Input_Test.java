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

class Input_Test extends TestBase {

	@Test void a01_basicSetters() {
		Input x = input()
			.accept("a")
			.alt("b")
			.autocomplete("c")
			.autofocus("d")
			.checked("e")
			.dirname("f")
			.disabled("g")
			.form("h")
			.formaction("i")
			.formenctype("j")
			.formmethod("k")
			.formnovalidate("l")
			.formtarget("m")
			.height("n")
			.inputmode("o")
			.list("p")
			.max("q")
			.maxlength("r")
			.min("s")
			.minlength("t")
			.multiple("u")
			.name("v")
			.pattern("w")
			.placeholder("x")
			.readonly("y")
			.readonly(true)
			.required("aa")
			.size("ab")
			.src("ac")
			.step("ad")
			.type("ae")
			.value("af")
			.width("ag")
			._class("ah")
			.accesskey("ai")
			.contenteditable("aj")
			.dir("ak")
			.hidden("al")
			.id("am")
			.lang("an")
			.onabort("ao")
			.onblur("ap")
			.oncancel("aq")
			.oncanplay("ar")
			.oncanplaythrough("as")
			.onchange("at")
			.onclick("au")
			.oncuechange("av")
			.ondblclick("aw")
			.ondurationchange("ax")
			.onemptied("ay")
			.onended("az")
			.onerror("ba")
			.onfocus("bb")
			.oninput("bc")
			.oninvalid("bd")
			.onkeydown("be")
			.onkeypress("bf")
			.onkeyup("bg")
			.onload("bh")
			.onloadeddata("bi")
			.onloadedmetadata("bj")
			.onloadstart("bk")
			.onmousedown("bl")
			.onmouseenter("bm")
			.onmouseleave("bn")
			.onmousemove("bo")
			.onmouseout("bp")
			.onmouseover("bq")
			.onmouseup("br")
			.onmousewheel("bs")
			.onpause("bt")
			.onplay("bu")
			.onplaying("bv")
			.onprogress("bw")
			.onratechange("bx")
			.onreset("by")
			.onresize("bz")
			.onscroll("ca")
			.onseeked("cb")
			.onseeking("cc")
			.onselect("cd")
			.onshow("ce")
			.onstalled("cf")
			.onsubmit("cg")
			.onsuspend("ch")
			.ontimeupdate("ci")
			.ontoggle("cj")
			.onvolumechange("ck")
			.onwaiting("cl")
			.spellcheck("cm")
			.style("cn")
			.tabindex("co")
			.title("cp")
			.translate("cq");

		assertString(
			"<input accept='a' alt='b' autocomplete='c' autofocus='d' checked='e' dirname='f' disabled='g' form='h' formaction='i' formenctype='j' formmethod='k' formnovalidate='l' formtarget='m' height='n' inputmode='o' list='p' max='q' maxlength='r' min='s' minlength='t' multiple='u' name='v' pattern='w' placeholder='x' readonly='y' value='af' required='aa' size='ab' src='ac' step='ad' type='ae' width='ag' class='ah' accesskey='ai' contenteditable='aj' dir='ak' hidden='al' id='am' lang='an' onabort='ao' onblur='ap' oncancel='aq' oncanplay='ar' oncanplaythrough='as' onchange='at' onclick='au' oncuechange='av' ondblclick='aw' ondurationchange='ax' onemptied='ay' onended='az' onerror='ba' onfocus='bb' oninput='bc' oninvalid='bd' onkeydown='be' onkeypress='bf' onkeyup='bg' onload='bh' onloadeddata='bi' onloadedmetadata='bj' onloadstart='bk' onmousedown='bl' onmouseenter='bm' onmouseleave='bn' onmousemove='bo' onmouseout='bp' onmouseover='bq' onmouseup='br' onmousewheel='bs' onpause='bt' onplay='bu' onplaying='bv' onprogress='bw' onratechange='bx' onreset='by' onresize='bz' onscroll='ca' onseeked='cb' onseeking='cc' onselect='cd' onshow='ce' onstalled='cf' onsubmit='cg' onsuspend='ch' ontimeupdate='ci' ontoggle='cj' onvolumechange='ck' onwaiting='cl' spellcheck='cm' style='cn' tabindex='co' title='cp' translate='cq'/>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<input/>", input());
	}

	@Test void a03_otherConstructors() {
		Input x1 = new Input("a");
		assertString("<input type='a'/>", x1);

	}
}