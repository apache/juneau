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
package org.apache.juneau.serializer;

import static org.apache.juneau.BeanTraverseContext.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for BeanTraverseContext properties.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class BeanTraverseComboTest extends ComboRoundTripTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"TestInitialDepth",
					Bean.class,
					Bean.create(),
					/* Json */		"{f:1}",
					/* JsonT */		"{f:1}",
					/* JsonR */		"\t\t{\n\t\t\tf: 1\n\t\t}",
					/* Xml */		"<object><f>1</f></object>",
					/* XmlT */		"<object><f>1</f></object>",
					/* XmlR */		"\t\t<object>\n\t\t\t<f>1</f>\n\t\t</object>\n",
					/* XmlNs */		"<object><f>1</f></object>",
					/* Html */		"<table><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlR */		"\t\t\t\t<table>\n\t\t\t\t\t<tr>\n\t\t\t\t\t\t<td>f</td>\n\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</table>\n",
					/* Uon */		"(f=1)",
					/* UonT */		"(f=1)",
					/* UonR */		"\t\t(\n\t\t\tf=1\n\t\t)",
					/* UrlEnc */	"f=1",
					/* UrlEncT */	"f=1",
					/* UrlEncR */	"\t\tf=1",
					/* MsgPack */	"81A16601",
					/* MsgPackT */	"81A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(BEANTRAVERSE_initialDepth, 2))
			},
		});
	}

	public BeanTraverseComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
}
