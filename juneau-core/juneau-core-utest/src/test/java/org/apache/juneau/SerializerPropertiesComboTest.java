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
package org.apache.juneau;

import static org.apache.juneau.serializer.Serializer.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for BeanTraverseContext properties.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class SerializerPropertiesComboTest extends ComboRoundTripTest {

	@Bean(typeName="BwT")
	public static class BeanWithType {
		public int f;

		public static BeanWithType create() {
			BeanWithType l = new BeanWithType();
			l.f = 1;
			return l;
		}
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
					"SERIALIZER_addBeanTypes",
					OMap.class,
					OMap.of("a", BeanWithType.create()),
					/* Json */		"{a:{_type:'BwT',f:1}}",
					/* JsonT */		"{a:{t:'BwT',f:1}}",
					/* JsonR */		"{\n\ta: {\n\t\t_type: 'BwT',\n\t\tf: 1\n\t}\n}",
					/* Xml */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* XmlT */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* XmlR */		"<object>\n\t<BwT _name='a'>\n\t\t<f>1</f>\n\t</BwT>\n</object>\n",
					/* XmlNs */		"<object><BwT _name='a'><f>1</f></BwT></object>",
					/* Html */		"<table><tr><td>a</td><td><table _type='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>a</td><td><table t='BwT'><tr><td>f</td><td>1</td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<table _type='BwT'>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>f</td>\n\t\t\t\t\t<td>1</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(a=(_type=BwT,f=1))",
					/* UonT */		"(a=(t=BwT,f=1))",
					/* UonR */		"(\n\ta=(\n\t\t_type=BwT,\n\t\tf=1\n\t)\n)",
					/* UrlEnc */	"a=(_type=BwT,f=1)",
					/* UrlEncT */	"a=(t=BwT,f=1)",
					/* UrlEncR */	"a=(\n\t_type=BwT,\n\tf=1\n)",
					/* MsgPack */	"81A16182A55F74797065A3427754A16601",
					/* MsgPackT */	"81A16182A174A3427754A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:_type>BwT</jp:_type>\n      <jp:f>1</jp:f>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_addBeanTypes, true, BEAN_beanDictionary, BeanWithType.class))
			},
			{ 	/* 1 */
				new ComboInput<>(
					"SERIALIZER_addRootType",
					BeanWithType.class,
					BeanWithType.create(),
					/* Json */		"{_type:'BwT',f:1}",
					/* JsonT */		"{t:'BwT',f:1}",
					/* JsonR */		"{\n\t_type: 'BwT',\n\tf: 1\n}",
					/* Xml */		"<BwT><f>1</f></BwT>",
					/* XmlT */		"<BwT><f>1</f></BwT>",
					/* XmlR */		"<BwT>\n\t<f>1</f>\n</BwT>\n",
					/* XmlNs */		"<BwT><f>1</f></BwT>",
					/* Html */		"<table _type='BwT'><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlT */		"<table t='BwT'><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='BwT'>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(_type=BwT,f=1)",
					/* UonT */		"(t=BwT,f=1)",
					/* UonR */		"(\n\t_type=BwT,\n\tf=1\n)",
					/* UrlEnc */	"_type=BwT&f=1",
					/* UrlEncT */	"t=BwT&f=1",
					/* UrlEncR */	"_type=BwT\n&f=1",
					/* MsgPack */	"82A55F74797065A3427754A16601",
					/* MsgPackT */	"82A174A3427754A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>BwT</jp:_type>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>BwT</jp:t>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>BwT</jp:_type>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.properties(OMap.of(SERIALIZER_addRootType, true, BEAN_beanDictionary, BeanWithType.class))
			},
		});
	}

	public SerializerPropertiesComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
}
