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
package org.apache.juneau.bean.openapi3.ui;

import static java.util.Collections.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.bean.html5.HtmlBuilder.a;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.swap.*;

/**
 * Generates an OpenAPI-UI interface from an OpenAPI document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class OpenApiUI extends ObjectSwap<OpenApi,Div> {

	static final FileFinder RESOURCES = FileFinder
		.create(BeanStore.INSTANCE)
		.cp(OpenApiUI.class, null, true)
		.dir(",")
		.caching(Boolean.getBoolean("RestContext.disableClasspathResourceCaching.b") ? -1 : 1_000_000)
		.build();

	private static final Set<String> STANDARD_METHODS = set("get", "put", "post", "delete", "options", "head", "patch", "trace");

	/**
	 * This UI applies to HTML requests only.
	 */
	@Override
	public org.apache.juneau.MediaType[] forMediaTypes() {
		return new org.apache.juneau.MediaType[] {org.apache.juneau.MediaType.HTML};
	}

	private static class Session {
		final int resolveRefsMaxDepth;
		final OpenApi openApi;

		Session(OpenApi openApi) {
			this.openApi = openApi.copy();
			this.resolveRefsMaxDepth = 1;
		}
	}

	@Override
	public Div swap(BeanSession beanSession, OpenApi openApi) throws Exception {

		var s = new Session(openApi);

		var css = RESOURCES.getString("files/htdocs/styles/OpenApiUI.css", null).orElse(null);
		if (css == null)
			css = RESOURCES.getString("OpenApiUI.css", null).orElse(null);

		var outer = div(
			style(css),
			script("text/javascript", RESOURCES.getString("OpenApiUI.js", null).orElse(null)),
			header(s)
		)._class("openapi-ui");

		// Operations without tags are rendered first.
		outer.child(div()._class("tag-block tag-block-open").children(tagBlockContents(s, null)));

		if (s.openApi.getTags() != null) {
			s.openApi.getTags().forEach(x -> {
				var tagBlock = div()._class("tag-block tag-block-open").children(
					tagBlockSummary(x),
					tagBlockContents(s, x)
				);
				outer.child(tagBlock);
			});
		}

		if (s.openApi.getComponents() != null && s.openApi.getComponents().getSchemas() != null) {
			var modelBlock = div()._class("tag-block").children(
				modelsBlockSummary(),
				modelsBlockContents(s)
			);
			outer.child(modelBlock);
		}

		return outer;
	}

	// Creates the informational summary before the ops.
	private Table header(Session s) {
		var table = table()._class("header");

		var info = s.openApi.getInfo();
		if (info != null) {

			if (info.getDescription() != null)
				table.child(tr(th("Description:"),td(toBRL(info.getDescription()))));

			if (info.getVersion() != null)
				table.child(tr(th("Version:"),td(info.getVersion())));

			var c = info.getContact();
			if (c != null) {
				var t2 = table();

				if (c.getName() != null)
					t2.child(tr(th("Name:"),td(c.getName())));
				if (c.getUrl() != null)
					t2.child(tr(th("URL:"),td(a(c.getUrl(), c.getUrl()))));
				if (c.getEmail() != null)
					t2.child(tr(th("Email:"),td(a("mailto:"+ c.getEmail(), c.getEmail()))));

				table.child(tr(th("Contact:"),td(t2)));
			}

			var l = info.getLicense();
			if (l != null) {
				var content = l.getName() != null ? l.getName() : l.getUrl();
				var child = l.getUrl() != null ? a(l.getUrl(), content) : l.getName();
				table.child(tr(th("License:"),td(child)));
			}

			ExternalDocumentation ed = s.openApi.getExternalDocs();
			if (ed != null) {
				var content = ed.getDescription() != null ? ed.getDescription() : ed.getUrl();
				var child = ed.getUrl() != null ? a(ed.getUrl(), content) : ed.getDescription();
				table.child(tr(th("Docs:"),td(child)));
			}

			if (info.getTermsOfService() != null) {
				var tos = info.getTermsOfService();
				var child = StringUtils.isUri(tos) ? a(tos, tos) : tos;
				table.child(tr(th("Terms of Service:"),td(child)));
			}
		}

		return table;
	}

	// Creates the "pet  Everything about your Pets  ext-link" header.
	private HtmlElement tagBlockSummary(Tag t) {
		var ed = t.getExternalDocs();

		var content = ed != null && ed.getDescription() != null ? ed.getDescription() : (ed != null ? ed.getUrl() : null);
		return div()._class("tag-block-summary").children(
			span(t.getName())._class("name"),
			span(toBRL(t.getDescription()))._class("description"),
			ed != null && ed.getUrl() != null ? span(a(ed.getUrl(), content))._class("extdocs") : null
		).onclick("toggleTagBlock(this)");
	}

	// Creates the contents under the "pet  Everything about your Pets  ext-link" header.
	private Div tagBlockContents(Session s, Tag t) {
		var tagBlockContents = div()._class("tag-block-contents");

		if (s.openApi.getPaths() != null) {
			s.openApi.getPaths().forEach((path, pathItem) -> {
				// Check each HTTP method in the path item
				if (pathItem.getGet() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "get", pathItem.getGet(), t);
				if (pathItem.getPut() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "put", pathItem.getPut(), t);
				if (pathItem.getPost() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "post", pathItem.getPost(), t);
				if (pathItem.getDelete() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "delete", pathItem.getDelete(), t);
				if (pathItem.getOptions() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "options", pathItem.getOptions(), t);
				if (pathItem.getHead() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "head", pathItem.getHead(), t);
				if (pathItem.getPatch() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "patch", pathItem.getPatch(), t);
				if (pathItem.getTrace() != null)
					addOperationIfTagMatches(tagBlockContents, s, path, "trace", pathItem.getTrace(), t);
			});
		}

		return tagBlockContents;
	}

	private void addOperationIfTagMatches(Div tagBlockContents, Session s, String path, String method, Operation op, Tag t) {
		if ((t == null && (op.getTags() == null || op.getTags().isEmpty())) ||
			(t != null && op.getTags() != null && op.getTags().contains(t.getName()))) {
			tagBlockContents.child(opBlock(s, path, method, op));
		}
	}

	private Div opBlock(Session s, String path, String opName, Operation op) {

		var opClass = op.getDeprecated() != null && op.getDeprecated() ? "deprecated" : opName.toLowerCase();
		if (!(op.getDeprecated() != null && op.getDeprecated()) && !STANDARD_METHODS.contains(opClass))
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
			op.getSummary() != null ? span(op.getSummary())._class("summary") : null
		).onclick("toggleOpBlock(this)");
	}

	private Div tableContainer(Session s, Operation op) {
		var tableContainer = div()._class("table-container");

		if (op.getDescription() != null)
			tableContainer.child(div(toBRL(op.getDescription()))._class("op-block-description"));

		if (op.getParameters() != null) {
			tableContainer.child(div(h4("Parameters")._class("title"))._class("op-block-section-header"));

			var parameters = table(tr(th("Name")._class("parameter-key"), th("Description")._class("parameter-key")))._class("parameters");

			op.getParameters().forEach(x -> {
				var piName = x.getName();
				var required = x.getRequired() != null && x.getRequired();

				var parameterKey = td(
					div(piName)._class("name" + (required ? " required" : "")),
					required ? div("required")._class("requiredlabel") : null,
					x.getSchema() != null ? div(x.getSchema().getType())._class("type") : null,
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

			op.getResponses().forEach((k,v) -> {
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
	}

	private Div headers(Response ri) {
		if (ri.getHeaders() == null)
			return null;

		var sectionTable = table(tr(th("Name"),th("Description"),th("Schema")))._class("section-table");

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
	}

	private Div examples(Session s, Parameter pi) {
		var m = new JsonMap();

		try {
			var si = pi.getSchema();
			if (si != null)
				m.put("model", si.copy().resolveRefs(s.openApi, new ArrayDeque<>(), s.resolveRefsMaxDepth));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	private Div examples(Session s, Response ri) {
		var m = new JsonMap();
		try {
			var content = ri.getContent();
			if (content != null) {
				// For OpenAPI 3.0, content is a map of media types to MediaType objects
				content.forEach((mediaType, mediaTypeObj) -> {
					if (mediaTypeObj.getSchema() != null) {
						try {
							var schema = mediaTypeObj.getSchema().copy().resolveRefs(s.openApi, new ArrayDeque<>(), s.resolveRefsMaxDepth);
							m.put(mediaType, schema);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	private Div examplesDiv(JsonMap m) {
		if (m.isEmpty())
			return null;

		Select select = null;
		if (m.size() > 1) {
			select = select().onchange("selectExample(this)")._class("example-select");
		}

		var div = div(select)._class("examples");

		if (select != null)
			select.child(option("model","model"));
		div.child(div(m.remove("model"))._class("model active").attr("data-name", "model"));

		var select2 = select;
		m.forEach((k,v) -> {
			if (select2 != null)
				select2.child(option(k, k));
			div.child(div(v.toString().replace("\\n", "\n"))._class("example").attr("data-name", k));
		});

		return div;
	}

	// Creates the "Model" header.
	private HtmlElement modelsBlockSummary() {
		return div()._class("tag-block-summary").children(span("Models")._class("name")).onclick("toggleTagBlock(this)");
	}

	// Creates the contents under the "Model" header.
	private Div modelsBlockContents(Session s) {
		var modelBlockContents = div()._class("tag-block-contents");
		if (s.openApi.getComponents() != null && s.openApi.getComponents().getSchemas() != null) {
			s.openApi.getComponents().getSchemas().forEach((k,v) -> modelBlockContents.child(modelBlock(k,v)));
		}
		return modelBlockContents;
	}

	private Div modelBlock(String modelName, SchemaInfo model) {
		return div()._class("op-block op-block-closed model").children(
			modelBlockSummary(modelName, model),
			div(model)._class("op-block-contents")
		);
	}

	private HtmlElement modelBlockSummary(String modelName, SchemaInfo model) {
		return div()._class("op-block-summary").children(
			span(modelName)._class("method-button"),
			model.getDescription() != null ? span(toBRL(model.getDescription()))._class("summary") : null
		).onclick("toggleOpBlock(this)");
	}

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
}
