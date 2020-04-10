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
package org.apache.juneau.transforms;

import static org.apache.juneau.testutils.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.testutils.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for the CalendarSwap class.
 */
@RunWith(Parameterized.class)
@SuppressWarnings("deprecation")
public class CalendarSwapComboTest extends ComboRoundTripTest {

	private static Calendar singleDate = new GregorianCalendar(TimeZone.getTimeZone("PST"));
	static {
		singleDate.setTimeInMillis(0);
		singleDate.set(1901, 2, 3, 10, 11, 12);
	}

	private static Calendar[] dateArray = new Calendar[]{singleDate};

	private static OMap dateMap = OMap.of("foo", singleDate);


	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{	/* 0 */
				new ComboInput<Calendar>(
					"CalendarSwap.ToString/singleDate",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarSwap.ToString.class)
				.json("'Sun Mar 03 10:11:12 PST 1901'")
				.jsonT("'Sun Mar 03 10:11:12 PST 1901'")
				.jsonR("'Sun Mar 03 10:11:12 PST 1901'")
				.xml("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.xmlT("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.xmlR("<string>Sun Mar 03 10:11:12 PST 1901</string>\n")
				.xmlNs("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.html("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.htmlT("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.htmlR("<string>Sun Mar 03 10:11:12 PST 1901</string>")
				.uon("'Sun Mar 03 10:11:12 PST 1901'")
				.uonT("'Sun Mar 03 10:11:12 PST 1901'")
				.uonR("'Sun Mar 03 10:11:12 PST 1901'")
				.urlEnc("_value='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncT("_value='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncR("_value='Sun+Mar+03+10:11:12+PST+1901'")
				.msgPack("BC53756E204D61722030332031303A31313A3132205053542031393031")
				.msgPackT("BC53756E204D61722030332031303A31313A3132205053542031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>Sun Mar 03 10:11:12 PST 1901</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>Sun Mar 03 10:11:12 PST 1901</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>Sun Mar 03 10:11:12 PST 1901</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 1 */
				new ComboInput<Calendar[]>(
					"CalendarSwap.ToString/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarSwap.ToString.class)
				.json("['Sun Mar 03 10:11:12 PST 1901']")
				.jsonT("['Sun Mar 03 10:11:12 PST 1901']")
				.jsonR("[\n\t'Sun Mar 03 10:11:12 PST 1901'\n]")
				.xml("<array><string>Sun Mar 03 10:11:12 PST 1901</string></array>")
				.xmlT("<array><string>Sun Mar 03 10:11:12 PST 1901</string></array>")
				.xmlR("<array>\n\t<string>Sun Mar 03 10:11:12 PST 1901</string>\n</array>\n")
				.xmlNs("<array><string>Sun Mar 03 10:11:12 PST 1901</string></array>")
				.html("<ul><li>Sun Mar 03 10:11:12 PST 1901</li></ul>")
				.htmlT("<ul><li>Sun Mar 03 10:11:12 PST 1901</li></ul>")
				.htmlR("<ul>\n\t<li>Sun Mar 03 10:11:12 PST 1901</li>\n</ul>\n")
				.uon("@('Sun Mar 03 10:11:12 PST 1901')")
				.uonT("@('Sun Mar 03 10:11:12 PST 1901')")
				.uonR("@(\n\t'Sun Mar 03 10:11:12 PST 1901'\n)")
				.urlEnc("0='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncT("0='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncR("0='Sun+Mar+03+10:11:12+PST+1901'")
				.msgPack("91BC53756E204D61722030332031303A31313A3132205053542031393031")
				.msgPackT("91BC53756E204D61722030332031303A31313A3132205053542031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Sun Mar 03 10:11:12 PST 1901</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Sun Mar 03 10:11:12 PST 1901</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>Sun Mar 03 10:11:12 PST 1901</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 2 */
				new ComboInput<OMap>(
					"CalendarSwap.ToString",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarSwap.ToString.class)
				.json("{foo:'Sun Mar 03 10:11:12 PST 1901'}")
				.jsonT("{foo:'Sun Mar 03 10:11:12 PST 1901'}")
				.jsonR("{\n\tfoo: 'Sun Mar 03 10:11:12 PST 1901'\n}")
				.xml("<object><foo>Sun Mar 03 10:11:12 PST 1901</foo></object>")
				.xmlT("<object><foo>Sun Mar 03 10:11:12 PST 1901</foo></object>")
				.xmlR("<object>\n\t<foo>Sun Mar 03 10:11:12 PST 1901</foo>\n</object>\n")
				.xmlNs("<object><foo>Sun Mar 03 10:11:12 PST 1901</foo></object>")
				.html("<table><tr><td>foo</td><td>Sun Mar 03 10:11:12 PST 1901</td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td>Sun Mar 03 10:11:12 PST 1901</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>Sun Mar 03 10:11:12 PST 1901</td>\n\t</tr>\n</table>\n")
				.uon("(foo='Sun Mar 03 10:11:12 PST 1901')")
				.uonT("(foo='Sun Mar 03 10:11:12 PST 1901')")
				.uonR("(\n\tfoo='Sun Mar 03 10:11:12 PST 1901'\n)")
				.urlEnc("foo='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncT("foo='Sun+Mar+03+10:11:12+PST+1901'")
				.urlEncR("foo='Sun+Mar+03+10:11:12+PST+1901'")
				.msgPack("81A3666F6FBC53756E204D61722030332031303A31313A3132205053542031393031")
				.msgPackT("81A3666F6FBC53756E204D61722030332031303A31313A3132205053542031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Sun Mar 03 10:11:12 PST 1901</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Sun Mar 03 10:11:12 PST 1901</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>Sun Mar 03 10:11:12 PST 1901</jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
			{	/* 3 */
				new ComboInput<Calendar>(
					"CalendarSwap.ISO8601DT/singleDate",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarSwap.ISO8601DT.class)
				.json("'1901-03-03T10:11:12-08:00'")
				.jsonT("'1901-03-03T10:11:12-08:00'")
				.jsonR("'1901-03-03T10:11:12-08:00'")
				.xml("<string>1901-03-03T10:11:12-08:00</string>")
				.xmlT("<string>1901-03-03T10:11:12-08:00</string>")
				.xmlR("<string>1901-03-03T10:11:12-08:00</string>\n")
				.xmlNs("<string>1901-03-03T10:11:12-08:00</string>")
				.html("<string>1901-03-03T10:11:12-08:00</string>")
				.htmlT("<string>1901-03-03T10:11:12-08:00</string>")
				.htmlR("<string>1901-03-03T10:11:12-08:00</string>")
				.uon("1901-03-03T10:11:12-08:00")
				.uonT("1901-03-03T10:11:12-08:00")
				.uonR("1901-03-03T10:11:12-08:00")
				.urlEnc("_value=1901-03-03T10:11:12-08:00")
				.urlEncT("_value=1901-03-03T10:11:12-08:00")
				.urlEncR("_value=1901-03-03T10:11:12-08:00")
				.msgPack("B9313930312D30332D30335431303A31313A31322D30383A3030")
				.msgPackT("B9313930312D30332D30335431303A31313A31322D30383A3030")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>1901-03-03T10:11:12-08:00</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>1901-03-03T10:11:12-08:00</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>1901-03-03T10:11:12-08:00</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 4 */
				new ComboInput<Calendar[]>(
					"CalendarSwap.ISO8601DT/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarSwap.ISO8601DT.class)
				.json("['1901-03-03T10:11:12-08:00']")
				.jsonT("['1901-03-03T10:11:12-08:00']")
				.jsonR("[\n\t'1901-03-03T10:11:12-08:00'\n]")
				.xml("<array><string>1901-03-03T10:11:12-08:00</string></array>")
				.xmlT("<array><string>1901-03-03T10:11:12-08:00</string></array>")
				.xmlR("<array>\n\t<string>1901-03-03T10:11:12-08:00</string>\n</array>\n")
				.xmlNs("<array><string>1901-03-03T10:11:12-08:00</string></array>")
				.html("<ul><li>1901-03-03T10:11:12-08:00</li></ul>")
				.htmlT("<ul><li>1901-03-03T10:11:12-08:00</li></ul>")
				.htmlR("<ul>\n\t<li>1901-03-03T10:11:12-08:00</li>\n</ul>\n")
				.uon("@(1901-03-03T10:11:12-08:00)")
				.uonT("@(1901-03-03T10:11:12-08:00)")
				.uonR("@(\n\t1901-03-03T10:11:12-08:00\n)")
				.urlEnc("0=1901-03-03T10:11:12-08:00")
				.urlEncT("0=1901-03-03T10:11:12-08:00")
				.urlEncR("0=1901-03-03T10:11:12-08:00")
				.msgPack("91B9313930312D30332D30335431303A31313A31322D30383A3030")
				.msgPackT("91B9313930312D30332D30335431303A31313A31322D30383A3030")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>1901-03-03T10:11:12-08:00</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>1901-03-03T10:11:12-08:00</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>1901-03-03T10:11:12-08:00</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 5 */
				new ComboInput<OMap>(
					"CalendarSwap.ISO8601DT/dateMap",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarSwap.ISO8601DT.class)
				.json("{foo:'1901-03-03T10:11:12-08:00'}")
				.jsonT("{foo:'1901-03-03T10:11:12-08:00'}")
				.jsonR("{\n\tfoo: '1901-03-03T10:11:12-08:00'\n}")
				.xml("<object><foo>1901-03-03T10:11:12-08:00</foo></object>")
				.xmlT("<object><foo>1901-03-03T10:11:12-08:00</foo></object>")
				.xmlR("<object>\n\t<foo>1901-03-03T10:11:12-08:00</foo>\n</object>\n")
				.xmlNs("<object><foo>1901-03-03T10:11:12-08:00</foo></object>")
				.html("<table><tr><td>foo</td><td>1901-03-03T10:11:12-08:00</td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td>1901-03-03T10:11:12-08:00</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>1901-03-03T10:11:12-08:00</td>\n\t</tr>\n</table>\n")
				.uon("(foo=1901-03-03T10:11:12-08:00)")
				.uonT("(foo=1901-03-03T10:11:12-08:00)")
				.uonR("(\n\tfoo=1901-03-03T10:11:12-08:00\n)")
				.urlEnc("foo=1901-03-03T10:11:12-08:00")
				.urlEncT("foo=1901-03-03T10:11:12-08:00")
				.urlEncR("foo=1901-03-03T10:11:12-08:00")
				.msgPack("81A3666F6FB9313930312D30332D30335431303A31313A31322D30383A3030")
				.msgPackT("81A3666F6FB9313930312D30332D30335431303A31313A31322D30383A3030")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>1901-03-03T10:11:12-08:00</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>1901-03-03T10:11:12-08:00</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>1901-03-03T10:11:12-08:00</jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
			{	/* 6 */
				new ComboInput<Calendar>(
					"CalendarSwap.RFC2822DTZ/singleDate",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarSwap.RFC2822DTZ.class)
				.json("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.jsonT("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.jsonR("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.xml("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.xmlT("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.xmlR("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>\n")
				.xmlNs("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.html("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.htmlT("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.htmlR("<string>Sun, 03 Mar 1901 18:11:12 GMT</string>")
				.uon("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.uonT("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.uonR("'Sun, 03 Mar 1901 18:11:12 GMT'")
				.urlEnc("_value='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncT("_value='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncR("_value='Sun,+03+Mar+1901+18:11:12+GMT'")
				.msgPack("BD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.msgPackT("BD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>Sun, 03 Mar 1901 18:11:12 GMT</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>Sun, 03 Mar 1901 18:11:12 GMT</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>Sun, 03 Mar 1901 18:11:12 GMT</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 7 */
				new ComboInput<Calendar[]>(
					"CalendarSwap.RFC2822DTZ/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarSwap.RFC2822DTZ.class)
				.json("['Sun, 03 Mar 1901 18:11:12 GMT']")
				.jsonT("['Sun, 03 Mar 1901 18:11:12 GMT']")
				.jsonR("[\n\t'Sun, 03 Mar 1901 18:11:12 GMT'\n]")
				.xml("<array><string>Sun, 03 Mar 1901 18:11:12 GMT</string></array>")
				.xmlT("<array><string>Sun, 03 Mar 1901 18:11:12 GMT</string></array>")
				.xmlR("<array>\n\t<string>Sun, 03 Mar 1901 18:11:12 GMT</string>\n</array>\n")
				.xmlNs("<array><string>Sun, 03 Mar 1901 18:11:12 GMT</string></array>")
				.html("<ul><li>Sun, 03 Mar 1901 18:11:12 GMT</li></ul>")
				.htmlT("<ul><li>Sun, 03 Mar 1901 18:11:12 GMT</li></ul>")
				.htmlR("<ul>\n\t<li>Sun, 03 Mar 1901 18:11:12 GMT</li>\n</ul>\n")
				.uon("@('Sun, 03 Mar 1901 18:11:12 GMT')")
				.uonT("@('Sun, 03 Mar 1901 18:11:12 GMT')")
				.uonR("@(\n\t'Sun, 03 Mar 1901 18:11:12 GMT'\n)")
				.urlEnc("0='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncT("0='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncR("0='Sun,+03+Mar+1901+18:11:12+GMT'")
				.msgPack("91BD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.msgPackT("91BD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Sun, 03 Mar 1901 18:11:12 GMT</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Sun, 03 Mar 1901 18:11:12 GMT</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>Sun, 03 Mar 1901 18:11:12 GMT</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 8 */
				new ComboInput<OMap>(
					"CalendarSwap.RFC2822DTZ/dateMap",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarSwap.RFC2822DTZ.class)
				.json("{foo:'Sun, 03 Mar 1901 18:11:12 GMT'}")
				.jsonT("{foo:'Sun, 03 Mar 1901 18:11:12 GMT'}")
				.jsonR("{\n\tfoo: 'Sun, 03 Mar 1901 18:11:12 GMT'\n}")
				.xml("<object><foo>Sun, 03 Mar 1901 18:11:12 GMT</foo></object>")
				.xmlT("<object><foo>Sun, 03 Mar 1901 18:11:12 GMT</foo></object>")
				.xmlR("<object>\n\t<foo>Sun, 03 Mar 1901 18:11:12 GMT</foo>\n</object>\n")
				.xmlNs("<object><foo>Sun, 03 Mar 1901 18:11:12 GMT</foo></object>")
				.html("<table><tr><td>foo</td><td>Sun, 03 Mar 1901 18:11:12 GMT</td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td>Sun, 03 Mar 1901 18:11:12 GMT</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>Sun, 03 Mar 1901 18:11:12 GMT</td>\n\t</tr>\n</table>\n")
				.uon("(foo='Sun, 03 Mar 1901 18:11:12 GMT')")
				.uonT("(foo='Sun, 03 Mar 1901 18:11:12 GMT')")
				.uonR("(\n\tfoo='Sun, 03 Mar 1901 18:11:12 GMT'\n)")
				.urlEnc("foo='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncT("foo='Sun,+03+Mar+1901+18:11:12+GMT'")
				.urlEncR("foo='Sun,+03+Mar+1901+18:11:12+GMT'")
				.msgPack("81A3666F6FBD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.msgPackT("81A3666F6FBD53756E2C203033204D617220313930312031383A31313A313220474D54")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Sun, 03 Mar 1901 18:11:12 GMT</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Sun, 03 Mar 1901 18:11:12 GMT</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>Sun, 03 Mar 1901 18:11:12 GMT</jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
			{	/* 9 */
				new ComboInput<Calendar>(
					"CalendarLongSwap",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarLongSwap.class)
				.json("-2172116928000")
				.jsonT("-2172116928000")
				.jsonR("-2172116928000")
				.xml("<number>-2172116928000</number>")
				.xmlT("<number>-2172116928000</number>")
				.xmlR("<number>-2172116928000</number>\n")
				.xmlNs("<number>-2172116928000</number>")
				.html("<number>-2172116928000</number>")
				.htmlT("<number>-2172116928000</number>")
				.htmlR("<number>-2172116928000</number>")
				.uon("-2172116928000")
				.uonT("-2172116928000")
				.uonR("-2172116928000")
				.urlEnc("_value=-2172116928000")
				.urlEncT("_value=-2172116928000")
				.urlEncR("_value=-2172116928000")
				.msgPack("D3FFFFFE0643BDFA00")
				.msgPackT("D3FFFFFE0643BDFA00")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>-2172116928000</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>-2172116928000</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>-2172116928000</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 10 */
				new ComboInput<Calendar[]>(
					"CalendarLongSwap/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarLongSwap.class)
				.json("[-2172116928000]")
				.jsonT("[-2172116928000]")
				.jsonR("[\n\t-2172116928000\n]")
				.xml("<array><number>-2172116928000</number></array>")
				.xmlT("<array><number>-2172116928000</number></array>")
				.xmlR("<array>\n\t<number>-2172116928000</number>\n</array>\n")
				.xmlNs("<array><number>-2172116928000</number></array>")
				.html("<ul><li><number>-2172116928000</number></li></ul>")
				.htmlT("<ul><li><number>-2172116928000</number></li></ul>")
				.htmlR("<ul>\n\t<li><number>-2172116928000</number></li>\n</ul>\n")
				.uon("@(-2172116928000)")
				.uonT("@(-2172116928000)")
				.uonR("@(\n\t-2172116928000\n)")
				.urlEnc("0=-2172116928000")
				.urlEncT("0=-2172116928000")
				.urlEncR("0=-2172116928000")
				.msgPack("91D3FFFFFE0643BDFA00")
				.msgPackT("91D3FFFFFE0643BDFA00")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>-2172116928000</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>-2172116928000</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>-2172116928000</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 11 */
				new ComboInput<OMap>(
					"CalendarLongSwap/dateMap",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarLongSwap.class)
				.json("{foo:-2172116928000}")
				.jsonT("{foo:-2172116928000}")
				.jsonR("{\n\tfoo: -2172116928000\n}")
				.xml("<object><foo _type='number'>-2172116928000</foo></object>")
				.xmlT("<object><foo t='number'>-2172116928000</foo></object>")
				.xmlR("<object>\n\t<foo _type='number'>-2172116928000</foo>\n</object>\n")
				.xmlNs("<object><foo _type='number'>-2172116928000</foo></object>")
				.html("<table><tr><td>foo</td><td><number>-2172116928000</number></td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td><number>-2172116928000</number></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td><number>-2172116928000</number></td>\n\t</tr>\n</table>\n")
				.uon("(foo=-2172116928000)")
				.uonT("(foo=-2172116928000)")
				.uonR("(\n\tfoo=-2172116928000\n)")
				.urlEnc("foo=-2172116928000")
				.urlEncT("foo=-2172116928000")
				.urlEncR("foo=-2172116928000")
				.msgPack("81A3666F6FD3FFFFFE0643BDFA00")
				.msgPackT("81A3666F6FD3FFFFFE0643BDFA00")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>-2172116928000</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>-2172116928000</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>-2172116928000</jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
			{	/* 12 */
				new ComboInput<Calendar>(
					"CalendarMapSwap/singleDate",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarMapSwap.class)
				.json("{time:-2172116928000,timeZone:'PST'}")
				.jsonT("{time:-2172116928000,timeZone:'PST'}")
				.jsonR("{\n\ttime: -2172116928000,\n\ttimeZone: 'PST'\n}")
				.xml("<object><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></object>")
				.xmlT("<object><time t='number'>-2172116928000</time><timeZone>PST</timeZone></object>")
				.xmlR("<object>\n\t<time _type='number'>-2172116928000</time>\n\t<timeZone>PST</timeZone>\n</object>\n")
				.xmlNs("<object><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></object>")
				.html("<table><tr><td>time</td><td><number>-2172116928000</number></td></tr><tr><td>timeZone</td><td>PST</td></tr></table>")
				.htmlT("<table><tr><td>time</td><td><number>-2172116928000</number></td></tr><tr><td>timeZone</td><td>PST</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>time</td>\n\t\t<td><number>-2172116928000</number></td>\n\t</tr>\n\t<tr>\n\t\t<td>timeZone</td>\n\t\t<td>PST</td>\n\t</tr>\n</table>\n")
				.uon("(time=-2172116928000,timeZone=PST)")
				.uonT("(time=-2172116928000,timeZone=PST)")
				.uonR("(\n\ttime=-2172116928000,\n\ttimeZone=PST\n)")
				.urlEnc("time=-2172116928000&timeZone=PST")
				.urlEncT("time=-2172116928000&timeZone=PST")
				.urlEncR("time=-2172116928000\n&timeZone=PST")
				.msgPack("82A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.msgPackT("82A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:time>-2172116928000</jp:time>\n    <jp:timeZone>PST</jp:timeZone>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 13 */
				new ComboInput<Calendar[]>(
					"CalendarMapSwap/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarMapSwap.class)
				.json("[{time:-2172116928000,timeZone:'PST'}]")
				.jsonT("[{time:-2172116928000,timeZone:'PST'}]")
				.jsonR("[\n\t{\n\t\ttime: -2172116928000,\n\t\ttimeZone: 'PST'\n\t}\n]")
				.xml("<array><object><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></object></array>")
				.xmlT("<array><object><time t='number'>-2172116928000</time><timeZone>PST</timeZone></object></array>")
				.xmlR("<array>\n\t<object>\n\t\t<time _type='number'>-2172116928000</time>\n\t\t<timeZone>PST</timeZone>\n\t</object>\n</array>\n")
				.xmlNs("<array><object><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></object></array>")
				.html("<table _type='array'><tr><th>time</th><th>timeZone</th></tr><tr><td><number>-2172116928000</number></td><td>PST</td></tr></table>")
				.htmlT("<table t='array'><tr><th>time</th><th>timeZone</th></tr><tr><td><number>-2172116928000</number></td><td>PST</td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>time</th>\n\t\t<th>timeZone</th>\n\t</tr>\n\t<tr>\n\t\t<td><number>-2172116928000</number></td>\n\t\t<td>PST</td>\n\t</tr>\n</table>\n")
				.uon("@((time=-2172116928000,timeZone=PST))")
				.uonT("@((time=-2172116928000,timeZone=PST))")
				.uonR("@(\n\t(\n\t\ttime=-2172116928000,\n\t\ttimeZone=PST\n\t)\n)")
				.urlEnc("0=(time=-2172116928000,timeZone=PST)")
				.urlEncT("0=(time=-2172116928000,timeZone=PST)")
				.urlEncR("0=(\n\ttime=-2172116928000,\n\ttimeZone=PST\n)")
				.msgPack("9182A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.msgPackT("9182A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:time>-2172116928000</jp:time>\n      <jp:timeZone>PST</jp:timeZone>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 14 */
				new ComboInput<OMap>(
					"CalendarMapSwap/dateMap",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarMapSwap.class)
				.json("{foo:{time:-2172116928000,timeZone:'PST'}}")
				.jsonT("{foo:{time:-2172116928000,timeZone:'PST'}}")
				.jsonR("{\n\tfoo: {\n\t\ttime: -2172116928000,\n\t\ttimeZone: 'PST'\n\t}\n}")
				.xml("<object><foo _type='object'><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></foo></object>")
				.xmlT("<object><foo t='object'><time t='number'>-2172116928000</time><timeZone>PST</timeZone></foo></object>")
				.xmlR("<object>\n\t<foo _type='object'>\n\t\t<time _type='number'>-2172116928000</time>\n\t\t<timeZone>PST</timeZone>\n\t</foo>\n</object>\n")
				.xmlNs("<object><foo _type='object'><time _type='number'>-2172116928000</time><timeZone>PST</timeZone></foo></object>")
				.html("<table><tr><td>foo</td><td><table><tr><td>time</td><td><number>-2172116928000</number></td></tr><tr><td>timeZone</td><td>PST</td></tr></table></td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td><table><tr><td>time</td><td><number>-2172116928000</number></td></tr><tr><td>timeZone</td><td>PST</td></tr></table></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>time</td>\n\t\t\t\t\t<td><number>-2172116928000</number></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>timeZone</td>\n\t\t\t\t\t<td>PST</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(foo=(time=-2172116928000,timeZone=PST))")
				.uonT("(foo=(time=-2172116928000,timeZone=PST))")
				.uonR("(\n\tfoo=(\n\t\ttime=-2172116928000,\n\t\ttimeZone=PST\n\t)\n)")
				.urlEnc("foo=(time=-2172116928000,timeZone=PST)")
				.urlEncT("foo=(time=-2172116928000,timeZone=PST)")
				.urlEncR("foo=(\n\ttime=-2172116928000,\n\ttimeZone=PST\n)")
				.msgPack("81A3666F6F82A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.msgPackT("81A3666F6F82A474696D65D3FFFFFE0643BDFA00A874696D655A6F6E65A3505354")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo rdf:parseType='Resource'>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo rdf:parseType='Resource'>\n<jp:time>-2172116928000</jp:time>\n<jp:timeZone>PST</jp:timeZone>\n</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo rdf:parseType='Resource'>\n      <jp:time>-2172116928000</jp:time>\n      <jp:timeZone>PST</jp:timeZone>\n    </jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
			{	/* 15 */
				new ComboInput<Calendar>(
					"CalendarSwap.DateMedium/singleDate",
					Calendar.class,
					singleDate
				)
				.swaps(CalendarSwap.DateMedium.class)
				.json("'Mar 3, 1901'")
				.jsonT("'Mar 3, 1901'")
				.jsonR("'Mar 3, 1901'")
				.xml("<string>Mar 3, 1901</string>")
				.xmlT("<string>Mar 3, 1901</string>")
				.xmlR("<string>Mar 3, 1901</string>\n")
				.xmlNs("<string>Mar 3, 1901</string>")
				.html("<string>Mar 3, 1901</string>")
				.htmlT("<string>Mar 3, 1901</string>")
				.htmlR("<string>Mar 3, 1901</string>")
				.uon("'Mar 3, 1901'")
				.uonT("'Mar 3, 1901'")
				.uonR("'Mar 3, 1901'")
				.urlEnc("_value='Mar+3,+1901'")
				.urlEncT("_value='Mar+3,+1901'")
				.urlEncR("_value='Mar+3,+1901'")
				.msgPack("AB4D617220332C2031393031")
				.msgPackT("AB4D617220332C2031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>Mar 3, 1901</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>Mar 3, 1901</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>Mar 3, 1901</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x))
			},
			{	/* 16 */
				new ComboInput<Calendar[]>(
					"CalendarSwap.DateMedium/dateArray",
					Calendar[].class,
					dateArray
				)
				.swaps(CalendarSwap.DateMedium.class)
				.json("['Mar 3, 1901']")
				.jsonT("['Mar 3, 1901']")
				.jsonR("[\n\t'Mar 3, 1901'\n]")
				.xml("<array><string>Mar 3, 1901</string></array>")
				.xmlT("<array><string>Mar 3, 1901</string></array>")
				.xmlR("<array>\n\t<string>Mar 3, 1901</string>\n</array>\n")
				.xmlNs("<array><string>Mar 3, 1901</string></array>")
				.html("<ul><li>Mar 3, 1901</li></ul>")
				.htmlT("<ul><li>Mar 3, 1901</li></ul>")
				.htmlR("<ul>\n\t<li>Mar 3, 1901</li>\n</ul>\n")
				.uon("@('Mar 3, 1901')")
				.uonT("@('Mar 3, 1901')")
				.uonR("@(\n\t'Mar 3, 1901'\n)")
				.urlEnc("0='Mar+3,+1901'")
				.urlEncT("0='Mar+3,+1901'")
				.urlEncR("0='Mar+3,+1901'")
				.msgPack("91AB4D617220332C2031393031")
				.msgPackT("91AB4D617220332C2031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Mar 3, 1901</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>Mar 3, 1901</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>Mar 3, 1901</rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x[0]))
			},
			{	/* 17 */
				new ComboInput<OMap>(
					"CalendarSwap.DateMedium/dateMap",
					getType(Map.class,String.class,Calendar.class),
					dateMap
				)
				.swaps(CalendarSwap.DateMedium.class)
				.json("{foo:'Mar 3, 1901'}")
				.jsonT("{foo:'Mar 3, 1901'}")
				.jsonR("{\n\tfoo: 'Mar 3, 1901'\n}")
				.xml("<object><foo>Mar 3, 1901</foo></object>")
				.xmlT("<object><foo>Mar 3, 1901</foo></object>")
				.xmlR("<object>\n\t<foo>Mar 3, 1901</foo>\n</object>\n")
				.xmlNs("<object><foo>Mar 3, 1901</foo></object>")
				.html("<table><tr><td>foo</td><td>Mar 3, 1901</td></tr></table>")
				.htmlT("<table><tr><td>foo</td><td>Mar 3, 1901</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>Mar 3, 1901</td>\n\t</tr>\n</table>\n")
				.uon("(foo='Mar 3, 1901')")
				.uonT("(foo='Mar 3, 1901')")
				.uonR("(\n\tfoo='Mar 3, 1901'\n)")
				.urlEnc("foo='Mar+3,+1901'")
				.urlEncT("foo='Mar+3,+1901'")
				.urlEncR("foo='Mar+3,+1901'")
				.msgPack("81A3666F6FAB4D617220332C2031393031")
				.msgPackT("81A3666F6FAB4D617220332C2031393031")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Mar 3, 1901</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:foo>Mar 3, 1901</jp:foo>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>Mar 3, 1901</jp:foo>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verifyInstanceOf(Calendar.class, x.get("foo")))
			},
		});
	}

	public CalendarSwapComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@BeforeClass
	public static void beforeClass() {
		TestUtils.setTimeZone("PST");
		TestUtils.setLocale(Locale.US);
	}

	@AfterClass
	public static void afterClass() {
		TestUtils.unsetTimeZone();
		TestUtils.unsetLocale();
	}
}
