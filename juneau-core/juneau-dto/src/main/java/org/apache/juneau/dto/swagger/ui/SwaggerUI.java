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
 * 
 */
public class SwaggerUI extends PojoSwap<Swagger,Div> {
	
	static final ClasspathResourceManager RM = new ClasspathResourceManager(SwaggerUI.class);
	
	@Override
	public MediaType[] forMediaTypes() {
		return new MediaType[] {MediaType.HTML};
	}
	
	@Override
	public Div swap(BeanSession session, Swagger s) throws Exception {
		Div outer = div(
			style(RM.getString("SwaggerUI.css")),
			script("text/javascript", RM.getString("SwaggerUI.js"))
		);
		
		// Operations without tags are rendered first.
		outer.child(div()._class("tag-block tag-block-open").children(tagBlockContents(s, null)));

		for (Tag t : s.getTags()) {
			Div tagBlock = div()._class("tag-block tag-block-open").children(
				tagBlockSummary(t),
				tagBlockContents(s, t)
			);
			outer.child(tagBlock);
		}
		
		return outer;
	}
	
	// Creates the "pet  Everything about your Pets  ext-link" header.
	private HtmlElement tagBlockSummary(Tag t) {
		ExternalDocumentation ed = t.getExternalDocs();
		String edd = ed == null ? null : ed.getDescription();
		Object extDocText = ed == null ? null : (edd != null ? edd : ed.getUrl());
		
		return div()._class("tag-block-summary").children(
			span(t.getName())._class("name"),
			span(t.getDescription())._class("description"),
			ed == null ? null : span(a(ed.getUrl(), extDocText))._class("extdocs")
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
		String opNameLc = opName.toLowerCase();
		
		return div()._class("op-block op-block-closed " + opNameLc).children(
			opBlockSummary(path, opName, op),
			div(tableContainer(s, op))._class("op-block-contents")
		);
	}

	private HtmlElement opBlockSummary(String path, String opName, Operation op) {
		return div()._class("op-block-summary").children(
			span(opName.toUpperCase())._class("method-button"),
			span(path)._class("path"),
			span(op.getSummary())._class("summary")
		).onclick("toggleOpBlock(this)");
	}

	private Div tableContainer(Swagger s, Operation op) {
		Div tableContainer = div()._class("table-container");
		
		String d = op.getDescription();
		if (! StringUtils.isEmpty(d)) 
			tableContainer.child(div(d)._class("op-block-description"));
			
		if (op.hasParameters()) {
			tableContainer.child(div(h4("Parameters")._class("title"))._class("op-block-section-header"));
			
			Table parameters = table(tr(th("Name")._class("parameter-key"), th("Description")._class("parameter-key")))._class("parameters");
			
			for (ParameterInfo pi : op.getParameters()) {
				String piName = "body".equals(pi.getIn()) ? "body" : pi.getName();
				
				Td parameterKey = td(
					div(piName)._class("name" + (pi.getRequired() ? " required" : "")),
					div(pi.getType())._class("type"),
					div('(' + pi.getIn() + ')')._class("in")
				)._class("parameter-key");
				
				Td parameterValue = td(
					div(pi.getDescription())._class("description"),
					examples(s, pi)
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
					examples(s, ri),
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

	private Div examples(Swagger s, ParameterInfo pi) {
		return examples(s, pi.getSchema());
	}
	
	private Div examples(Swagger s, ResponseInfo ri) {
		return examples(s, ri.getSchema());
	}
	
	// If SchemaInfo is a "$ref", resolve it, otherwise a no-op.
	private SchemaInfo resolve(Swagger s, SchemaInfo si) {
		String ref = si.getRef();
		if (ref != null) 
			si = s.findRef(ref, SchemaInfo.class);
		return si;
	}
	
	private Div examples(Swagger s, SchemaInfo si) {
		if (si == null)
			return null;
		
		si = resolve(s, si);
		
		Map<String,String> examples = si.getExamples();
		if (examples == null)
			return null;
		
		Ul ul = ul()._class("tab");
		Div div = div(ul)._class("examples");
		
		ul.child(li("model").onclick("selectExample(this)").attr("data-name", "model")._class("active"));
		div.child(div(getSchemaModel(s, si))._class("model active").attr("data-name", "model"));

		for (Map.Entry<String,String> e : examples.entrySet()) {
			String name = e.getKey();
			String value = e.getValue();
			
			ul.child(li(name).onclick("selectExample(this)").attr("data-name", name));
			div.child(div(value.replaceAll("\\n", "\n"))._class("example").attr("data-name", name));
		}
		
		return div;
	}
	
	// Generates the model contents.
	@SuppressWarnings("rawtypes")
	private ObjectMap getSchemaModel(Swagger s, SchemaInfo si) {

		ObjectMap m = new ObjectMap();

		for (String k1 : si.keySet()) {
			if (k1.equals("properties")) {
				ObjectMap m2 = new ObjectMap();
				for (Map.Entry<String,Map<String,Object>> e : si.getProperties().entrySet()) {
					String pName = e.getKey();
					Map<String,Object> pEntry = e.getValue();
					if (pEntry.containsKey("$ref")) 
						pEntry = getSchemaModel(s, s.findRef(pEntry.get("$ref").toString(), SchemaInfo.class));
					if (pEntry.containsKey("items")) {
						Object items = pEntry.get("items");
						if (items instanceof Map && ((Map)items).containsKey("$ref"))
							pEntry.put("items", getSchemaModel(s, s.findRef(((Map)items).get("$ref").toString(), SchemaInfo.class)));
					}
					m2.put(pName, pEntry);
				}
				m.put(k1, m2);
			} else if (k1.equals("x-examples")) {
				// Ignore these.
			} else {
				m.append(k1, si.get(k1, Object.class));
			}
		}
		
		return m;
	}
}
