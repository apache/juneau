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
package org.apache.juneau.bean.swagger.ui;

import static java.util.Collections.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.a;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.swap.*;

/**
 * Generates a Swagger-UI interface from a Swagger document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class SwaggerUI extends ObjectSwap<Swagger,Div> {

	private static class Session {
		final int resolveRefsMaxDepth;
		final Swagger swagger;

		Session(Swagger swagger) {
			this.swagger = swagger.copy();
			this.resolveRefsMaxDepth = 1;
		}
	}

	// @formatter:off
	static final FileFinder RESOURCES = FileFinder
		.create(BeanStore.INSTANCE)
		.cp(SwaggerUI.class, null, true)
		.dir(",")
		.caching(Boolean.getBoolean("RestContext.disableClasspathResourceCaching.b") ? -1 : 1_000_000)
		.build();
	// @formatter:on

	private static final Set<String> STANDARD_METHODS = set("get", "put", "post", "delete", "options");

	/**
	 * Replaces newlines with <br> elements.
	 */
	private static List<Object> toBRL(String s) {
		if (s == null)
			return null;  // NOSONAR - Intentionally returning null.
		if (s.indexOf(',') == -1)
			return singletonList(s);
		var l = Utils.list();
		var sa = s.split("\n");
		for (var i = 0; i < sa.length; i++) {
			if (i > 0)
				l.add(br());
			l.add(sa[i]);
		}
		return l;
	}

	/**
	 * This UI applies to HTML requests only.
	 */
	@Override
	public MediaType[] forMediaTypes() {
		return new MediaType[] { MediaType.HTML };
	}

	@Override
	public Div swap(BeanSession beanSession, Swagger swagger) throws Exception {
		// @formatter:off
		var s = new Session(swagger);

		var css = RESOURCES.getString("files/htdocs/styles/SwaggerUI.css", null).orElse(null);
		if (css == null)
			css = RESOURCES.getString("SwaggerUI.css", null).orElse(null);

		var outer = div(
			style(css),
			script("text/javascript", RESOURCES.getString("SwaggerUI.js", null).orElse(null)),
			header(s)
		)._class("swagger-ui");

		// Operations without tags are rendered first.
		outer.child(div()._class("tag-block tag-block-open").children(tagBlockContents(s, null)));

		if (s.swagger.getTags() != null) {
			s.swagger.getTags().forEach(x -> {
				var tagBlock = div()._class("tag-block tag-block-open").children(
					tagBlockSummary(x),
					tagBlockContents(s, x)
				);
				outer.child(tagBlock);
			});
		}

		if (s.swagger.getDefinitions() != null) {
			var modelBlock = div()._class("tag-block").children(
				modelsBlockSummary(),
				modelsBlockContents(s)
			);
			outer.child(modelBlock);
		}

		return outer;
		// @formatter:on
	}

	private static Div examples(Session s, ParameterInfo pi) {
		// @formatter:off
		var isBody = "body".equals(pi.getIn());

		var m = new JsonMap();

		try {
			if (isBody) {
				var si = pi.getSchema();
				if (si != null)
					m.put("model", si.copy().resolveRefs(s.swagger, new ArrayDeque<>(), s.resolveRefsMaxDepth));
			} else {
				var m2 = pi
					.copy()
					.resolveRefs(s.swagger, new ArrayDeque<>(), s.resolveRefsMaxDepth)
					.asMap()
					.keepAll("format","pattern","collectionFormat","maximum","minimum","multipleOf","maxLength","minLength","maxItems","minItems","allowEmptyValue","exclusiveMaximum","exclusiveMinimum","uniqueItems","items","default","enum");
				m.put("model", m2.isEmpty() ? i("none") : m2);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
		// @formatter:on
	}

	private static Div examples(Session s, ResponseInfo ri) {
		var si = ri.getSchema();

		var m = new JsonMap();
		try {
			if (si != null) {
				si = si.copy().resolveRefs(s.swagger, new ArrayDeque<>(), s.resolveRefsMaxDepth);
				m.put("model", si);
			}

			var examples = ri.getExamples();
			if (examples != null)
				examples.forEach(m::put);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	private static Div examplesDiv(JsonMap m) {
		if (m.isEmpty())
			return null;

		Select select = null;
		if (m.size() > 1) {
			select = select().onchange("selectExample(this)")._class("example-select");
		}

		var div = div(select)._class("examples");

		if (select != null)
			select.child(option("model", "model"));
		div.child(div(m.remove("model"))._class("model active").attr("data-name", "model"));

		var select2 = select;
		m.forEach((k, v) -> {
			if (select2 != null)
				select2.child(option(k, k));
			div.child(div(v.toString().replace("\\n", "\n"))._class("example").attr("data-name", k));
		});

		return div;
	}

	// Creates the informational summary before the ops.
	private static Table header(Session s) {
		var table = table()._class("header");

		var info = s.swagger.getInfo();
		if (info != null) {

			if (info.getDescription() != null)
				table.child(tr(th("Description:"), td(toBRL(info.getDescription()))));

			if (info.getVersion() != null)
				table.child(tr(th("Version:"), td(info.getVersion())));

			var c = info.getContact();
			if (c != null) {
				var t2 = table();

				if (c.getName() != null)
					t2.child(tr(th("Name:"), td(c.getName())));
				if (c.getUrl() != null)
					t2.child(tr(th("URL:"), td(a(c.getUrl(), c.getUrl()))));
				if (c.getEmail() != null)
					t2.child(tr(th("Email:"), td(a("mailto:" + c.getEmail(), c.getEmail()))));

				table.child(tr(th("Contact:"), td(t2)));
			}

			var l = info.getLicense();
			if (l != null) {
				var content = l.getName() != null ? l.getName() : l.getUrl();
				var child = l.getUrl() != null ? a(l.getUrl(), content) : l.getName();
				table.child(tr(th("License:"), td(child)));
			}

			var ed = s.swagger.getExternalDocs();
			if (ed != null) {
				var content = ed.getDescription() != null ? ed.getDescription() : ed.getUrl();
				var child = ed.getUrl() != null ? a(ed.getUrl(), content) : ed.getDescription();
				table.child(tr(th("Docs:"), td(child)));
			}

			if (info.getTermsOfService() != null) {
				var tos = info.getTermsOfService();
				var child = StringUtils.isUri(tos) ? a(tos, tos) : tos;
				table.child(tr(th("Terms of Service:"), td(child)));
			}
		}

		return table;
	}

	private static Div headers(ResponseInfo ri) {
		// @formatter:off
		if (ri.getHeaders() == null)
			return null;

		var sectionTable = table(tr(th("Name"), th("Description"), th("Schema")))._class("section-table");

		var headers = div(
			div("Headers:")._class("section-name"),
			sectionTable
		)._class("headers");

		ri.getHeaders().forEach((k,v) ->
			sectionTable.child(
				tr(
					td(k)._class("name"),
					td(toBRL(v.getDescription()))._class("description"),
					td(v.asMap().keepAll("type","format","items","collectionFormat","default","maximum","exclusiveMaximum","minimum","exclusiveMinimum","maxLength","minLength","pattern","maxItems","minItems","uniqueItems","enum","multipleOf"))
				)
			)
		);

		return headers;
		// @formatter:on
	}

	private static Div modelBlock(String modelName, JsonMap model) {
		// @formatter:off
		return div()._class("op-block op-block-closed model").children(
			modelBlockSummary(modelName, model),
			div(model)._class("op-block-contents")
		);
		// @formatter:on
	}

	private static HtmlElement modelBlockSummary(String modelName, JsonMap model) {
		// @formatter:off
		return div()._class("op-block-summary").onclick("toggleOpBlock(this)").children(
			span(modelName)._class("method-button"),
			model.containsKey("description") ? span(toBRL(model.remove("description").toString()))._class("summary") : null
		);
		// @formatter:on
	}

	// Creates the contents under the "Model" header.
	private static Div modelsBlockContents(Session s) {
		var modelBlockContents = div()._class("tag-block-contents");
		s.swagger.getDefinitions().forEach((k, v) -> modelBlockContents.child(modelBlock(k, v)));
		return modelBlockContents;
	}

	// Creates the "Model" header.
	private static HtmlElement modelsBlockSummary() {
		return div()._class("tag-block-summary").onclick("toggleTagBlock(this)").children(span("Models")._class("name"));
	}

	private static Div opBlock(Session s, String path, String opName, Operation op) {

		var opClass = op.isDeprecated() ? "deprecated" : opName.toLowerCase();
		if (! op.isDeprecated() && ! STANDARD_METHODS.contains(opClass))
			opClass = "other";

		// @formatter:off
		return div()._class("op-block op-block-closed " + opClass).children(
			opBlockSummary(path, opName, op),
			div(tableContainer(s, op))._class("op-block-contents")
		);
		// @formatter:on
	}

	private static HtmlElement opBlockSummary(String path, String opName, Operation op) {
		// @formatter:off
		return div()._class("op-block-summary").onclick("toggleOpBlock(this)").children(
			span(opName.toUpperCase())._class("method-button"),
			span(path)._class("path"),
			op.getSummary() != null ? span(op.getSummary())._class("summary") : null
		);
		// @formatter:on
	}

	private static Div tableContainer(Session s, Operation op) {
		// @formatter:off
		var tableContainer = div()._class("table-container");

		if (op.getDescription() != null)
			tableContainer.child(div(toBRL(op.getDescription()))._class("op-block-description"));

		if (op.getParameters() != null) {
			tableContainer.child(div(h4("Parameters")._class("title"))._class("op-block-section-header"));

			var parameters = table(tr(th("Name")._class("parameter-key"), th("Description")._class("parameter-key")))._class("parameters");

			op.getParameters().forEach(x -> {
				var piName = "body".equals(x.getIn()) ? "body" : x.getName();
				var required = x.getRequired() != null && x.getRequired();

				var parameterKey = td(
					div(piName)._class("name" + (required ? " required" : "")),
					required ? div("required")._class("requiredlabel") : null,
					div(x.getType())._class("type"),
					div('(' + x.getIn() + ')')._class("in")
				)._class("parameter-key");

				var parameterValue = td(
					div(toBRL(x.getDescription()))._class("description"),
					examples(s, x)
				)._class("parameter-value");

				parameters.child(tr(parameterKey, parameterValue));
			});

			tableContainer.child(parameters);
		}

		if (op.getResponses() != null) {
			tableContainer.child(div(h4("Responses")._class("title"))._class("op-block-section-header"));

			var responses = table(tr(th("Code")._class("response-key"), th("Description")._class("response-key")))._class("responses");
			tableContainer.child(responses);

			op.getResponses().forEach((k, v) -> {
				var code = td(k)._class("response-key");

				var codeValue = td(
					div(toBRL(v.getDescription()))._class("description"),
					examples(s, v),
					headers(v)
				)._class("response-value");

				responses.child(tr(code, codeValue));
			});
		}

		return tableContainer;
		// @formatter:on
	}

	// Creates the contents under the "pet  Everything about your Pets  ext-link" header.
	private static Div tagBlockContents(Session s, Tag t) {
		// @formatter:off
		var tagBlockContents = div()._class("tag-block-contents");

		if (s.swagger.getPaths() != null) {
			s.swagger.getPaths().forEach((path,v) ->
				v.forEach((opName,op) -> {
					if ((t == null && op.getTags() == null) || (t != null && op.getTags() != null && op.getTags() != null && op.getTags().contains(t.getName())))
						tagBlockContents.child(opBlock(s, path, opName, op));
				})
			);
		}

		return tagBlockContents;
		// @formatter:on
	}

	// Creates the "pet  Everything about your Pets  ext-link" header.
	private static HtmlElement tagBlockSummary(Tag t) {
		var ed = t.getExternalDocs();

		var children = new ArrayList<HtmlElement>();
		children.add(span(t.getName())._class("name"));
		children.add(span(toBRL(t.getDescription()))._class("description"));

		if (ed != null) {
			var content = ed.getDescription() != null ? ed.getDescription() : ed.getUrl();
			children.add(span(a(ed.getUrl(), content))._class("extdocs"));
		}

		return div()._class("tag-block-summary").onclick("toggleTagBlock(this)").children(children);
	}
}