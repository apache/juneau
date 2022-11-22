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
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
enum HtmlTag {

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
	xTABLE(-1,"</table>"),
	xTR(-2,"</tr>"),
	xTH(-3,"</th>"),
	xTD(-4,"</td>"),
	xUL(-5,"</ul>"),
	xLI(-6,"</li>"),
	xSTRING(-7,"</string>"),
	xNUMBER(-8,"</number>"),
	xBOOLEAN(-9,"</boolean>"),
	xNULL(-10,"</null>"),
	xA(-11,"</a>"),
	xBR(-12,"</br>"),
	xFF(-13,"</ff>"),
	xBS(-14,"</bs>"),
	xSP(-17, "</sp>"),
	xP(-18, "</p>"),
	xHTML(-19, "</html>");

	private Map<Integer,HtmlTag> cache = new HashMap<>();

	int id;
	String label;

	HtmlTag(int id, String label) {
		this.id = id;
		this.label = label;
		cache.put(id, this);
	}

	static HtmlTag forEvent(ParserSession session, XMLStreamReader r) throws ParseException {
		int et = r.getEventType();
		if (et == START_ELEMENT)
			return forString(r.getLocalName(), false);
		else if (et == END_ELEMENT)
			return forString(r.getLocalName(), true);
		throw new ParseException(session, "Invalid call to HtmlTag.forEvent on event of type ''{0}''", XmlUtils.toReadableEvent(r));
	}

	static HtmlTag forString(String tag, boolean end) {
		char c = tag.charAt(0);
		HtmlTag t = null;
		if (c == 'u')
			t = (end ? xUL : UL);
		else if (c == 'l')
			t = (end ? xLI : LI);
		else if (c == 's') {
			c = tag.charAt(1);
			if (c == 'p')
				t = (end ? xSP : SP);
			else if (c == 't')
				t = (end ? xSTRING : STRING);
		}
		else if (c == 'b') {
			c = tag.charAt(1);
			if (c == 'o')
				t = (end ? xBOOLEAN : BOOLEAN);
			else if (c == 'r')
				t = (end ? xBR : BR);
			else if (c == 's')
				t = (end ? xBS : BS);
		}
		else if (c == 'a')
			t = (end ? xA : A);
		else if (c == 'n') {
			c = tag.charAt(2);
			if (c == 'm')
				t = (end ? xNUMBER : NUMBER);
			else if (c == 'l')
				t = (end ? xNULL : NULL);
		}
		else if (c == 't') {
			c = tag.charAt(1);
			if (c == 'a')
				t = (end ? xTABLE : TABLE);
			else if (c == 'r')
				t = (end ? xTR : TR);
			else if (c == 'h')
				t = (end ? xTH : TH);
			else if (c == 'd')
				t = (end ? xTD : TD);
		}
		else if (c == 'f')
			t = (end ? xFF : FF);
		else if (c == 'p')
			t = (end ? xP : P);
		else if (c == 'h')
			t = (end ? xHTML : HTML);
		return t;
	}

	@Override /* Object */
	public String toString() {
		return label;
	}

	public boolean isOneOf(HtmlTag...tags) {
		for (HtmlTag tag : tags)
			if (tag == this)
				return true;
		return false;
	}
}