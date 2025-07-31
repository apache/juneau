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
package org.apache.juneau.dto.html5;

import static org.apache.juneau.assertions.Verify.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.html5.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Tests serialization of HTML5 templates.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class Html5TemplateComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{
				new ComboInput<>(
					"FormTemplate-1",
					FormTemplate.class,
					new FormTemplate("http://myaction", 123, true)
				)
				.json("{a:{action:'http://myaction'},c:[{_type:'input',a:{type:'text',name:'v1',value:123}},{_type:'input',a:{type:'text',name:'v2',value:true}}]}")
				.jsonT("{a:{action:'http://myaction'},c:[{t:'input',a:{type:'text',name:'v1',value:123}},{t:'input',a:{type:'text',name:'v2',value:true}}]}")
				.jsonR("{\n\ta: {\n\t\taction: 'http://myaction'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'text',\n\t\t\t\tname: 'v1',\n\t\t\t\tvalue: 123\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'text',\n\t\t\t\tname: 'v2',\n\t\t\t\tvalue: true\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>")
				.xmlT("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>")
				.xmlR("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>\n")
				.xmlNs("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>")
				.html("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>")
				.htmlT("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>")
				.htmlR("<form action='http://myaction'><input type='text' name='v1' value='123'/><input type='text' name='v2' value='true'/></form>\n")
				.uon("(a=(action=http://myaction),c=@((_type=input,a=(type=text,name=v1,value=123)),(_type=input,a=(type=text,name=v2,value=true))))")
				.uonT("(a=(action=http://myaction),c=@((t=input,a=(type=text,name=v1,value=123)),(t=input,a=(type=text,name=v2,value=true))))")
				.uonR("(\n\ta=(\n\t\taction=http://myaction\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=text,\n\t\t\t\tname=v1,\n\t\t\t\tvalue=123\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=text,\n\t\t\t\tname=v2,\n\t\t\t\tvalue=true\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("a=(action=http://myaction)&c=@((_type=input,a=(type=text,name=v1,value=123)),(_type=input,a=(type=text,name=v2,value=true)))")
				.urlEncT("a=(action=http://myaction)&c=@((t=input,a=(type=text,name=v1,value=123)),(t=input,a=(type=text,name=v2,value=true)))")
				.urlEncR("a=(\n\taction=http://myaction\n)\n&c=@(\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=text,\n\t\t\tname=v1,\n\t\t\tvalue=123\n\t\t)\n\t),\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=text,\n\t\t\tname=v2,\n\t\t\tvalue=true\n\t\t)\n\t)\n)")
				.msgPack("82A16181A6616374696F6EAF687474703A2F2F6D79616374696F6EA1639282A55F74797065A5696E707574A16183A474797065A474657874A46E616D65A27631A576616C75657B82A55F74797065A5696E707574A16183A474797065A474657874A46E616D65A27632A576616C7565C3")
				.msgPackT("82A16181A6616374696F6EAF687474703A2F2F6D79616374696F6EA1639282A174A5696E707574A16183A474797065A474657874A46E616D65A27631A576616C75657B82A174A5696E707574A16183A474797065A474657874A46E616D65A27632A576616C7565C3")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:action rdf:resource='http://myaction'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n<jp:name>v1</jp:name>\n<jp:value>123</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n<jp:name>v2</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:action rdf:resource='http://myaction'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n<jp:name>v1</jp:name>\n<jp:value>123</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n<jp:name>v2</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:action rdf:resource='http://myaction'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>text</jp:type>\n            <jp:name>v1</jp:name>\n            <jp:value>123</jp:value>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>text</jp:type>\n            <jp:name>v2</jp:name>\n            <jp:value>true</jp:value>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(FormTemplate.class))
			},
		});
	}


	@Bean(dictionary=HtmlBeanDictionary.class)
	public static class FormTemplate {

		private String action;
		private int value1;
		private boolean value2;

		public FormTemplate(Form f) {
			this.action = f.getAttr("action");
			this.value1 = f.getChild(Input.class, 0).getAttr(int.class, "value");
			this.value2 = f.getChild(Input.class, 1).getAttr(boolean.class, "value");
		}

		public FormTemplate(String action, int value1, boolean value2) {
			this.action = action;
			this.value1 = value1;
			this.value2 = value2;
		}

		public Form swap(BeanSession session) {
			return form(action,
				input("text").name("v1").value(value1),
				input("text").name("v2").value(value2)
			);
		}
	}

	public Html5TemplateComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}
}