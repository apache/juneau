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
package org.apache.juneau.dto.swagger.ui;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.util.Map;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Generates a Swagger-UI interface from a Swagger document.
 */
public class SwaggerUI extends PojoSwap<Swagger,Div> {
	
	static final ClasspathResourceManager RESOURCES = new ClasspathResourceManager(SwaggerUI.class);
	
	@Override
	public MediaType[] forMediaTypes() {
		return new MediaType[] {MediaType.HTML};
	}
	
	@Override
	public Div swap(BeanSession session, Swagger s) throws Exception {
		
		s = s.copy().resolveRefs();
		
		Div outer = div(
			style(RESOURCES.getString("SwaggerUI.css")),
			script("text/javascript", RESOURCES.getString("SwaggerUI.js")),
			header(s)
		)._class("swagger-ui");
		
		// Operations without tags are rendered first.
		outer.child(div()._class("tag-block tag-block-open").children(tagBlockContents(s, null)));

		if (s.getTags() != null) {
			for (Tag t : s.getTags()) {
				Div tagBlock = div()._class("tag-block tag-block-open").children(
					tagBlockSummary(t),
					tagBlockContents(s, t)
				);
				outer.child(tagBlock);
			}
		}
		
		return outer;
	}
	
	// Creates the informational summary before the ops.
	private Table header(Swagger s) {
		Table table = table()._class("header");
		
		Info info = s.getInfo();
		if (info != null) {
			
			if (info.hasVersion())
				table.child(tr(th("Version:"),td(info.getVersion())));

			if (info.hasTermsOfService()) {
				String tos = info.getTermsOfService();
				Object child = StringUtils.isUri(tos) ? a(tos, tos) : tos;
				table.child(tr(th("Terms of Service:"),td(child)));
			}
			
			Contact c = info.getContact();
			if (c != null) {
				Table t2 = table();

				if (c.hasName())
					t2.child(tr(th("Name:"),td(c.getName())));
				if (c.hasUrl())
					t2.child(tr(th("URL:"),td(a(c.getUrl(), c.getUrl()))));
				if (c.hasEmail())
					t2.child(tr(th("Email:"),td(a("mailto:"+ c.getEmail(), c.getEmail()))));
					
				table.child(tr(th("Contact:"),td(t2)));
			}
			
			License l = info.getLicense();
			if (l != null) {
				Object child = l.hasUrl() ? a(l.getUrl(), l.hasName() ? l.getName() : l.getUrl()) : l.getName();
				table.child(tr(th("License:"),td(child)));
			}
			
			ExternalDocumentation ed = s.getExternalDocs();
			if (ed != null) {
				Object child = ed.hasUrl() ? a(ed.getUrl(), ed.hasDescription() ? ed.getDescription() : ed.getUrl()) : ed.getDescription();
				table.child(tr(th("Docs:"),td(child)));
			}
		}
		
		return table;
	}
	
	// Creates the "pet  Everything about your Pets  ext-link" header.
	private HtmlElement tagBlockSummary(Tag t) {
		ExternalDocumentation ed = t.getExternalDocs();
		
		return div()._class("tag-block-summary").children(
			span(t.getName())._class("name"),
			span(t.getDescription())._class("description"),
			ed == null ? null : span(a(ed.getUrl(), ed.hasDescription() ? ed.getDescription() : ed.getUrl()))._class("extdocs")
		).onclick("toggleTagBlock(this)");
	}
	
	// Creates the contents under the "pet  Everything about your Pets  ext-link" header.
	private Div tagBlockContents(Swagger s, Tag t) {
		Div tagBlockContents = div()._class("tag-block-contents");
		
		for (Map.Entry<String,Map<String,Operation>> e : s.getPaths().entrySet()) {
			String path = e.getKey();
			for (Map.Entry<String,Operation> e2 : e.getValue().entrySet()) {
				String opName = e2.getKey();
				Operation op = e2.getValue();
				if ((t == null && op.hasNoTags()) || (t != null && op.hasTag(t.getName())))
					tagBlockContents.child(opBlock(s, path, opName, op));
			}
		}
		
		return tagBlockContents;
	}
	
	private Div opBlock(Swagger s, String path, String opName, Operation op) {
		String opNameLc = op.isDeprecated() ? "deprecated" : opName.toLowerCase();
		
		return div()._class("op-block op-block-closed " + opNameLc).children(
			opBlockSummary(path, opName, op),
			div(tableContainer(s, op))._class("op-block-contents")
		);
	}

	private HtmlElement opBlockSummary(String path, String opName, Operation op) {
		return div()._class("op-block-summary").children(
			span(opName.toUpperCase())._class("method-button"),
			span(path)._class("path"),
			op.hasSummary() ? span(op.getSummary())._class("summary") : null
		).onclick("toggleOpBlock(this)");
	}

	private Div tableContainer(Swagger s, Operation op) {
		Div tableContainer = div()._class("table-container");
		
		if (op.hasDescription()) 
			tableContainer.child(div(op.getDescription())._class("op-block-description"));
			
		if (op.hasParameters()) {
			tableContainer.child(div(h4("Parameters")._class("title"))._class("op-block-section-header"));
			
			Table parameters = table(tr(th("Name")._class("parameter-key"), th("Description")._class("parameter-key")))._class("parameters");
			
			for (ParameterInfo pi : op.getParameters()) {
				String piName = "body".equals(pi.getIn()) ? "body" : pi.getName();
				boolean required = pi.getRequired() == null ? false : pi.getRequired();
				
				Td parameterKey = td(
					div(piName)._class("name" + (required ? " required" : "")),
					required ? div("required")._class("requiredlabel") : null,
					div(pi.getType())._class("type"),
					div('(' + pi.getIn() + ')')._class("in")
				)._class("parameter-key");
				
				Td parameterValue = td(
					div(pi.getDescription())._class("description"),
					examples(pi.getSchema(), pi.getExamples())
				)._class("parameter-value");
				
				parameters.child(tr(parameterKey, parameterValue));
			}
			
			tableContainer.child(parameters);
		}
		
		if (op.hasResponses()) {
			tableContainer.child(div(h4("Responses")._class("title"))._class("op-block-section-header"));
			
			Table responses = table(tr(th("Code")._class("response-key"), th("Description")._class("response-key")))._class("responses");
			tableContainer.child(responses);
			
			for (Map.Entry<String,ResponseInfo> e3 : op.getResponses().entrySet()) {
				ResponseInfo ri = e3.getValue();
				
				Td code = td(e3.getKey())._class("response-key");

				Td codeValue = td(
					div(ri.getDescription())._class("description"),
					examples(ri.getSchema(), ri.getExamples()),
					headers(s, ri)
				)._class("response-value");
				
				responses.child(tr(code, codeValue));
			}
		}
		
		return tableContainer;
	}
	
	private Div headers(Swagger s, ResponseInfo ri) {
		if (! ri.hasHeaders())
			return null;
		
		Table sectionTable = table(tr(th("Name"),th("Description"),th("Type")))._class("section-table");
		
		Div headers = div(
			div("Headers:")._class("section-name"),
			sectionTable
		)._class("headers");
		
		for (Map.Entry<String,HeaderInfo> e : ri.getHeaders().entrySet()) {
			String name = e.getKey();
			HeaderInfo hi = e.getValue();
			sectionTable.child(
				tr(
					td(name)._class("name"),
					td(hi.getDescription())._class("description"),
					td(hi.getType())._class("type")
				)
			);
		}
		
		return headers;
	}

	private Div examples(SchemaInfo si, Map<String,?> examples) {
		if (examples == null || si == null)
			return null;
		
		Select select = (Select)select().onchange("selectExample(this)")._class("example-select");
		select.child(option("model","model"));
		Div div = div(select)._class("examples");
		
		div.child(div(si.copy().setExample(null))._class("model active").attr("data-name", "model"));

		for (Map.Entry<String,?> e : examples.entrySet()) {
			String name = e.getKey();
			String value = e.getValue().toString();
			select.child(option(name, name));
			div.child(div(value.replaceAll("\\n", "\n"))._class("example").attr("data-name", name));
		}
		
		return div;
	}
}
