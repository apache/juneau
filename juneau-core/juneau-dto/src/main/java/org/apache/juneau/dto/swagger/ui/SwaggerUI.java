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

import java.util.*;
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

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "SwaggerUI.";

	/**
	 * Configuration property:  Resolve <code>$ref</code> references in schema up to the specified depth.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"SwaggerUI.resolveRefsMaxDepth.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>1</code>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the maximum recursive depth to resolve <code>$ref</code> variables in schema infos.
	 * <br>The default <code>1</code> means only resolve the first reference encountered.
	 * <br>A value of <code>0</code> disables reference resolution altogether.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Resolve schema references up to 5 levels deep.
	 * 	<ja>@RestResource</ja>(
	 * 			properties={
	 * 				<ja>@Property</ja>(name=<jsf>SWAGGERUI_resolveRefsMaxDepth</jsf>, value=<js>"5"</js>)
	 * 			}
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 */
	public static final String SWAGGERUI_resolveRefsMaxDepth = PREFIX + "resolveRefsMaxDepth.i";


	static final ClasspathResourceManager RESOURCES = new ClasspathResourceManager(SwaggerUI.class, ClasspathResourceFinderBasic.INSTANCE, Boolean.getBoolean("RestContext.useClasspathResourceCaching.b"));

	private static final Set<String> STANDARD_METHODS = new ASet<String>().appendAll("get", "put", "post", "delete", "options");

	/**
	 * This UI applies to HTML requests only.
	 */
	@Override
	public MediaType[] forMediaTypes() {
		return new MediaType[] {MediaType.HTML};
	}

	private static final class Session {
		final int resolveRefsMaxDepth;
		final Swagger swagger;

		Session(BeanSession bs, Swagger swagger) {
			this.swagger = swagger.copy();
			this.resolveRefsMaxDepth = bs.getProperty(SWAGGERUI_resolveRefsMaxDepth, Integer.class, 1);
		}
	}

	@Override
	public Div swap(BeanSession beanSession, Swagger swagger) throws Exception {

		Session s = new Session(beanSession, swagger);

		String css = RESOURCES.getString("files/htdocs/styles/SwaggerUI.css");
		if (css == null)
			css = RESOURCES.getString("SwaggerUI.css");

		Div outer = div(
			style(css),
			script("text/javascript", RESOURCES.getString("SwaggerUI.js")),
			header(s)
		)._class("swagger-ui");

		// Operations without tags are rendered first.
		outer.child(div()._class("tag-block tag-block-open").children(tagBlockContents(s, null)));

		if (s.swagger.hasTags()) {
			for (Tag t : s.swagger.getTags()) {
				Div tagBlock = div()._class("tag-block tag-block-open").children(
					tagBlockSummary(t),
					tagBlockContents(s, t)
				);
				outer.child(tagBlock);
			}
		}

		if (s.swagger.hasDefinitions()) {
			Div modelBlock = div()._class("tag-block").children(
				modelsBlockSummary(),
				modelsBlockContents(s)
			);
			outer.child(modelBlock);
		}

		return outer;
	}

	// Creates the informational summary before the ops.
	private Table header(Session s) {
		Table table = table()._class("header");

		Info info = s.swagger.getInfo();
		if (info != null) {

			if (info.hasDescription())
				table.child(tr(th("Description:"),td(toBRL(info.getDescription()))));

			if (info.hasVersion())
				table.child(tr(th("Version:"),td(info.getVersion())));

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

			ExternalDocumentation ed = s.swagger.getExternalDocs();
			if (ed != null) {
				Object child = ed.hasUrl() ? a(ed.getUrl(), ed.hasDescription() ? ed.getDescription() : ed.getUrl()) : ed.getDescription();
				table.child(tr(th("Docs:"),td(child)));
			}

			if (info.hasTermsOfService()) {
				String tos = info.getTermsOfService();
				Object child = StringUtils.isUri(tos) ? a(tos, tos) : tos;
				table.child(tr(th("Terms of Service:"),td(child)));
			}
		}

		return table;
	}

	// Creates the "pet  Everything about your Pets  ext-link" header.
	private HtmlElement tagBlockSummary(Tag t) {
		ExternalDocumentation ed = t.getExternalDocs();

		return div()._class("tag-block-summary").children(
			span(t.getName())._class("name"),
			span(toBRL(t.getDescription()))._class("description"),
			ed == null ? null : span(a(ed.getUrl(), ed.hasDescription() ? ed.getDescription() : ed.getUrl()))._class("extdocs")
		).onclick("toggleTagBlock(this)");
	}

	// Creates the contents under the "pet  Everything about your Pets  ext-link" header.
	private Div tagBlockContents(Session s, Tag t) {
		Div tagBlockContents = div()._class("tag-block-contents");

		for (Map.Entry<String,OperationMap> e : s.swagger.getPaths().entrySet()) {
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

	private Div opBlock(Session s, String path, String opName, Operation op) {

		String opClass = op.isDeprecated() ? "deprecated" : opName.toLowerCase();
		if (! op.isDeprecated() && ! STANDARD_METHODS.contains(opClass))
			opClass = "other";

		return div()._class("op-block op-block-closed " + opClass).children(
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

	private Div tableContainer(Session s, Operation op) {
		Div tableContainer = div()._class("table-container");

		if (op.hasDescription())
			tableContainer.child(div(toBRL(op.getDescription()))._class("op-block-description"));

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
					div(toBRL(pi.getDescription()))._class("description"),
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
					div(toBRL(ri.getDescription()))._class("description"),
					examples(s, ri),
					headers(s, ri)
				)._class("response-value");

				responses.child(tr(code, codeValue));
			}
		}

		return tableContainer;
	}

	private Div headers(Session s, ResponseInfo ri) {
		if (! ri.hasHeaders())
			return null;

		Table sectionTable = table(tr(th("Name"),th("Description"),th("Schema")))._class("section-table");

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
					td(toBRL(hi.getDescription()))._class("description"),
					td(hi.asMap().keepAll("type","format","items","collectionFormat","default","maximum","exclusiveMaximum","minimum","exclusiveMinimum","maxLength","minLength","pattern","maxItems","minItems","uniqueItems","enum","multipleOf"))
				)
			);
		}

		return headers;
	}

	private Div examples(Session s, ParameterInfo pi) {
		boolean isBody = "body".equals(pi.getIn());

		ObjectMap m = new ObjectMap();

		try {
			if (isBody) {
				SchemaInfo si = pi.getSchema();
				if (si != null)
					m.put("model", si.copy().resolveRefs(s.swagger, new ArrayDeque<String>(), s.resolveRefsMaxDepth));
			} else {
				ObjectMap om = pi
					.copy()
					.resolveRefs(s.swagger, new ArrayDeque<String>(), s.resolveRefsMaxDepth)
					.asMap()
					.keepAll("format","pattern","collectionFormat","maximum","minimum","multipleOf","maxLength","minLength","maxItems","minItems","allowEmptyValue","exclusiveMaximum","exclusiveMinimum","uniqueItems","items","default","enum");
				m.put("model", om.isEmpty() ? i("none") : om);
			}

			Map<String,?> examples = pi.getExamples();
			if (examples != null)
				for (Map.Entry<String,?> e : examples.entrySet())
					m.put(e.getKey(), e.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	private Div examples(Session s, ResponseInfo ri) {
		SchemaInfo si = ri.getSchema();

		ObjectMap m = new ObjectMap();
		try {
			if (si != null) {
				si = si.copy().resolveRefs(s.swagger, new ArrayDeque<String>(), s.resolveRefsMaxDepth);
				m.put("model", si);
			}

			Map<String,?> examples = ri.getExamples();
			if (examples != null)
				for (Map.Entry<String,?> e : examples.entrySet())
					m.put(e.getKey(), e.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	private Div examplesDiv(ObjectMap m) {
		if (m.isEmpty())
			return null;

		Select select = (Select)select().disabled(m.size() < 2).onchange("selectExample(this)")._class("example-select");
		Div div = div(select)._class("examples");

		select.child(option("model","model"));
		div.child(div(m.remove("model"))._class("model active").attr("data-name", "model"));

		for (Map.Entry<String,Object> e : m.entrySet()) {
			select.child(option(e.getKey(), e.getKey()));
			div.child(div(e.getValue().toString().replaceAll("\\n", "\n"))._class("example").attr("data-name", e.getKey()));
		}

		return div;
	}

	// Creates the "Model" header.
	private HtmlElement modelsBlockSummary() {
		return div()._class("tag-block-summary").children(span("Models")._class("name")).onclick("toggleTagBlock(this)");
	}

	// Creates the contents under the "Model" header.
	private Div modelsBlockContents(Session s) {
		Div modelBlockContents = div()._class("tag-block-contents");
		for (Map.Entry<String,ObjectMap> e : s.swagger.getDefinitions().entrySet())
			modelBlockContents.child(modelBlock(e.getKey(), e.getValue()));
		return modelBlockContents;
	}

	private Div modelBlock(String modelName, ObjectMap model) {
		return div()._class("op-block op-block-closed model").children(
			modelBlockSummary(modelName, model),
			div(model)._class("op-block-contents")
		);
	}

	private HtmlElement modelBlockSummary(String modelName, ObjectMap model) {
		return div()._class("op-block-summary").children(
			span(modelName)._class("method-button"),
			model.containsKey("description") ? span(toBRL(model.remove("description").toString()))._class("summary") : null
		).onclick("toggleOpBlock(this)");
	}

	/**
	 * Replaces newlines with <br> elements.
	 */
	private static List<Object> toBRL(String s) {
		if (s == null)
			return null;
		if (s.indexOf(',') == -1)
			return Collections.<Object>singletonList(s);
		List<Object> l = new ArrayList<>();
		String[] sa = s.split("\n");
		for (int i = 0; i < sa.length; i++) {
			if (i > 0)
				l.add(br());
			l.add(sa[i]);
		}
		return l;
	}
}

