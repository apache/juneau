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
package org.apache.juneau.html;

import static javax.xml.stream.XMLStreamConstants.*;

import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Predefined tags that occur in the serialized output of the HTML serializer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
enum HtmlTag {

	// @formatter:off
	TABLE(1,"<table>"),
	TR(2,"<tr>"),
	TH(3,"<th>"),
	TD(4,"<td>"),
	UL(5,"<ul>"),
	LI(6,"<li>"),
	STRING(7,"<string>"),
	NUMBER(8,"<number>"),
	BOOLEAN(9,"<boolean>"),
	NULL(10,"<null>"),
	A(11,"<a>"),
	BR(12,"<br>"),		// newline
	FF(13,"<ff>"),		// formfeed
	BS(14,"<bs>"),		// backspace
	SP(17, "<sp>"),   // space
	P(18, "<p>"),
	HTML(19, "<html>"),
	X_TABLE(-1,"</table>"),
	X_TR(-2,"</tr>"),
	X_TH(-3,"</th>"),
	X_TD(-4,"</td>"),
	X_UL(-5,"</ul>"),
	X_LI(-6,"</li>"),
	X_STRING(-7,"</string>"),
	X_NUMBER(-8,"</number>"),
	X_BOOLEAN(-9,"</boolean>"),
	X_NULL(-10,"</null>"),
	X_A(-11,"</a>"),
	X_BR(-12,"</br>"),
	X_FF(-13,"</ff>"),
	X_BS(-14,"</bs>"),
	X_SP(-17, "</sp>"),
	X_P(-18, "</p>"),
	X_HTML(-19, "</html>");
	// @formatter:on

	static HtmlTag forEvent(ParserSession session, XMLStreamReader r) throws ParseException {
		int et = r.getEventType();
		if (et == START_ELEMENT)
			return forString(r.getLocalName(), false);
		else if (et == END_ELEMENT)
			return forString(r.getLocalName(), true);
		throw new ParseException(session, "Invalid call to HtmlTag.forEvent on event of type ''{0}''", XmlUtils.toReadableEvent(r));
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for HTML tag string parsing
	})
	static HtmlTag forString(String tag, boolean end) {
		var c = tag.charAt(0);
		HtmlTag t = null;
		if (c == 'u')
			t = (end ? X_UL : UL);
		else if (c == 'l')
			t = (end ? X_LI : LI);
		else if (c == 's') {
			c = tag.charAt(1);
			if (c == 'p')
				t = (end ? X_SP : SP);
			else if (c == 't')
				t = (end ? X_STRING : STRING);
		} else if (c == 'b') {
			c = tag.charAt(1);
			if (c == 'o')
				t = (end ? X_BOOLEAN : BOOLEAN);
			else if (c == 'r')
				t = (end ? X_BR : BR);
			else if (c == 's')
				t = (end ? X_BS : BS);
		} else if (c == 'a')
			t = (end ? X_A : A);
		else if (c == 'n') {
			c = tag.charAt(2);
			if (c == 'm')
				t = (end ? X_NUMBER : NUMBER);
			else if (c == 'l')
				t = (end ? X_NULL : NULL);
		} else if (c == 't') {
			c = tag.charAt(1);
			if (c == 'a')
				t = (end ? X_TABLE : TABLE);
			else if (c == 'r')
				t = (end ? X_TR : TR);
			else if (c == 'h')
				t = (end ? X_TH : TH);
			else if (c == 'd')
				t = (end ? X_TD : TD);
		} else if (c == 'f')
			t = (end ? X_FF : FF);
		else if (c == 'p')
			t = (end ? X_P : P);
		else if (c == 'h')
			t = (end ? X_HTML : HTML);
		return t;
	}

	private Map<Integer,HtmlTag> cache = new HashMap<>();

	int id;

	String label;

	HtmlTag(int id, String label) {
		this.id = id;
		this.label = label;
		cache.put(id, this);
	}

	public boolean isOneOf(HtmlTag...tags) {
		for (var tag : tags)
			if (tag == this)
				return true;
		return false;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return label;
	}
}