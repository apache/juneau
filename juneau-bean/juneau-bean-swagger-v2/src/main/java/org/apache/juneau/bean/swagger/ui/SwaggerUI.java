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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.swap.*;

/**
 * Generates a Swagger-UI interface from a Swagger document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S1192" // String literals repeated for clarity in UI generation
})
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
		.create(BasicBeanStore.INSTANCE)
		.cp(SwaggerUI.class, null, true)
		.dir(",")
		.caching(Boolean.getBoolean("RestContext.disableClasspathResourceCaching.b") ? -1 : 1_000_000)
		.build();
	// @formatter:on

	private static final Set<String> STANDARD_METHODS = set("get", "put", "post", "delete", "options");

	private static Div examples(Session s, ParameterInfo pi) {
		// @formatter:off
		var isBody = "body".equals(pi.getIn());

		var m = new JsonMap();

		try {
			if (isBody) {
				var si = pi.getSchema();
				if (nn(si))
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
			if (nn(si)) {
				si = si.copy().resolveRefs(s.swagger, new ArrayDeque<>(), s.resolveRefsMaxDepth);
				m.put("model", si);
			}

			var examples = ri.getExamples();
			if (nn(examples))
				examples.forEach(m::put);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (m.isEmpty())
			return null;

		return examplesDiv(m);
	}

	@SuppressWarnings({
		"null" // Null analysis not applicable for optional values
	})
	private static Div examplesDiv(JsonMap m) {
		if (m.isEmpty())
			return null;

		Select select = null;
		if (m.size() > 1) {
			select = select().onchange("selectExample(this)").class_("example-select");
		}

		var div = div(select).class_("examples");

		if (nn(select))
			select.child(option("model", "model"));
		var modelContent = m.remove("model");
		div.child(div(nn(modelContent) ? modelContent : "").class_("model active").attr("data-name", "model"));

		var select2 = select;
		m.forEach((k, v) -> {
			if (nn(select2))
				select2.child(option(k, k));
			div.child(div(v.toString().replace("\\n", "\n")).class_("example").attr("data-name", k));
		});

		return div;
	}

	// Creates the informational summary before the ops.
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for this logic
	})
	private static Table header(Session s) {
		var table = table().class_("header");

		var info = s.swagger.getInfo();
		if (nn(info)) {

			if (nn(info.getDescription()))
				table.child(tr(th("Description:"), td(toBRL(info.getDescription()))));

			if (nn(info.getVersion()))
				table.child(tr(th("Version:"), td(info.getVersion())));

			var c = info.getContact();
			if (nn(c)) {
				var t2 = table();

				if (nn(c.getName()))
					t2.child(tr(th("Name:"), td(c.getName())));
				if (nn(c.getUrl()))
					t2.child(tr(th("URL:"), td(a(c.getUrl(), c.getUrl()))));
				if (nn(c.getEmail()))
					t2.child(tr(th("Email:"), td(a("mailto:" + c.getEmail(), c.getEmail()))));

				table.child(tr(th("Contact:"), td(t2)));
			}

			var l = info.getLicense();
			if (nn(l)) {
				var content = nn(l.getName()) ? l.getName() : l.getUrl();
				var child = nn(l.getUrl()) ? a(l.getUrl(), content) : l.getName();
				table.child(tr(th("License:"), td(child)));
			}

			var ed = s.swagger.getExternalDocs();
			if (nn(ed)) {
				var content = nn(ed.getDescription()) ? ed.getDescription() : ed.getUrl();
				var child = nn(ed.getUrl()) ? a(ed.getUrl(), content) : ed.getDescription();
				table.child(tr(th("Docs:"), td(child)));
			}

			if (nn(info.getTermsOfService())) {
				var tos = info.getTermsOfService();
				var child = isUri(tos) ? a(tos, tos) : tos;
				table.child(tr(th("Terms of Service:"), td(child)));
			}
		}

		return table;
	}

	private static Div headers(ResponseInfo ri) {
		// @formatter:off
		if (ri.getHeaders() == null)
			return null;

		var sectionTable = table(tr(th("Name"), th("Description"), th("Schema"))).class_("section-table");

		var headers = div(
			div("Headers:").class_("section-name"),
			sectionTable
		).class_("headers");

		ri.getHeaders().forEach((k,v) ->
			sectionTable.child(
				tr(
					td(k).class_("name"),
					td(toBRL(v.getDescription())).class_("description"),
					td(v.asMap().keepAll("type","format","items","collectionFormat","default","maximum","exclusiveMaximum","minimum","exclusiveMinimum","maxLength","minLength","pattern","maxItems","minItems","uniqueItems","enum","multipleOf"))
				)
			)
		);

		return headers;
		// @formatter:on
	}

	private static Div modelBlock(String modelName, JsonMap model) {
		// @formatter:off
		return div().class_("op-block op-block-closed model").children(
			modelBlockSummary(modelName, model),
			div(model).class_("op-block-contents")
		);
		// @formatter:on
	}

	private static HtmlElement modelBlockSummary(String modelName, JsonMap model) {
		// @formatter:off
		return div().class_("op-block-summary").onclick("toggleOpBlock(this)").children(
			span(modelName).class_("method-button"),
			model.containsKey("description") ? span(toBRL(model.remove("description").toString())).class_("summary") : null
		);
		// @formatter:on
	}

	// Creates the contents under the "Model" header.
	private static Div modelsBlockContents(Session s) {
		var modelBlockContents = div().class_("tag-block-contents");
		s.swagger.getDefinitions().forEach((k, v) -> modelBlockContents.child(modelBlock(k, v)));
		return modelBlockContents;
	}

	// Creates the "Model" header.
	private static HtmlElement modelsBlockSummary() {
		return div().class_("tag-block-summary").onclick("toggleTagBlock(this)").children(span("Models").class_("name"));
	}

	private static Div opBlock(Session s, String path, String opName, Operation op) {

		var opClass = op.isDeprecated() ? "deprecated" : opName.toLowerCase();
		if (! op.isDeprecated() && ! STANDARD_METHODS.contains(opClass))
			opClass = "other";

		// @formatter:off
		return div().class_("op-block op-block-closed " + opClass).children(
			opBlockSummary(path, opName, op),
			div(tableContainer(s, op)).class_("op-block-contents")
		);
		// @formatter:on
	}

	private static HtmlElement opBlockSummary(String path, String opName, Operation op) {
		// @formatter:off
		return div().class_("op-block-summary").onclick("toggleOpBlock(this)").children(
			span(opName.toUpperCase()).class_("method-button"),
			span(path).class_("path"),
			nn(op.getSummary()) ? span(op.getSummary()).class_("summary") : null
		);
		// @formatter:on
	}

	private static Div tableContainer(Session s, Operation op) {
		// @formatter:off
		var tableContainer = div().class_("table-container");

		if (nn(op.getDescription()))
			tableContainer.child(div(toBRL(op.getDescription())).class_("op-block-description"));

		if (nn(op.getParameters())) {
			tableContainer.child(div(h4("Parameters").class_("title")).class_("op-block-section-header"));

			var parameters = table(tr(th("Name").class_("parameter-key"), th("Description").class_("parameter-key"))).class_("parameters");

			op.getParameters().forEach(x -> {
				var piName = "body".equals(x.getIn()) ? "body" : x.getName();
				var required = nn(x.getRequired()) && x.getRequired();

				var parameterKey = td(
					div(piName).class_("name" + (required ? " required" : "")),
					required ? div("required").class_("requiredlabel") : null,
					div(x.getType()).class_("type"),
					div('(' + x.getIn() + ')').class_("in")
				).class_("parameter-key");

				var parameterValue = td(
					div(toBRL(x.getDescription())).class_("description"),
					examples(s, x)
				).class_("parameter-value");

				parameters.child(tr(parameterKey, parameterValue));
			});

			tableContainer.child(parameters);
		}

		if (nn(op.getResponses())) {
			tableContainer.child(div(h4("Responses").class_("title")).class_("op-block-section-header"));

			var responses = table(tr(th("Code").class_("response-key"), th("Description").class_("response-key"))).class_("responses");
			tableContainer.child(responses);

			op.getResponses().forEach((k, v) -> {
				var code = td(k).class_("response-key");

				var codeValue = td(
					div(toBRL(v.getDescription())).class_("description"),
					examples(s, v),
					headers(v)
				).class_("response-value");

				responses.child(tr(code, codeValue));
			});
		}

		return tableContainer;
		// @formatter:on
	}

	// Creates the contents under the "pet  Everything about your Pets  ext-link" header.
	@SuppressWarnings({
		"null" // Null analysis not applicable for optional values
	})
	private static Div tagBlockContents(Session s, Tag t) {
		// @formatter:off
		var tagBlockContents = div().class_("tag-block-contents");

		if (nn(s.swagger.getPaths())) {
			s.swagger.getPaths().forEach((path,v) ->
				v.forEach((opName,op) -> {
					if ((t == null && op.getTags() == null) || (nn(t) && nn(op.getTags()) && op.getTags().contains(t.getName())))
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
		children.add(span(t.getName()).class_("name"));
		children.add(span(toBRL(t.getDescription())).class_("description"));

		if (nn(ed)) {
			var content = nn(ed.getDescription()) ? ed.getDescription() : ed.getUrl();
			children.add(span(a(ed.getUrl(), content)).class_("extdocs"));
		}

		return div().class_("tag-block-summary").onclick("toggleTagBlock(this)").children(children);
	}

	/**
	 * Replaces newlines with <br> elements.
	 */
	private static List<Object> toBRL(String s) {
		if (s == null)
			return emptyList();
		if (s.indexOf(',') == -1)
			return singletonList(s);
		var l = list();
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
		return CollectionUtils.a(MediaType.HTML);
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
		).class_("swagger-ui");

		// Operations without tags are rendered first.
		outer.child(div().class_("tag-block tag-block-open").children(tagBlockContents(s, null)));

		if (nn(s.swagger.getTags())) {
			s.swagger.getTags().forEach(x -> {
				var tagBlock = div().class_("tag-block tag-block-open").children(
					tagBlockSummary(x),
					tagBlockContents(s, x)
				);
				outer.child(tagBlock);
			});
		}

		if (nn(s.swagger.getDefinitions())) {
			var modelBlock = div().class_("tag-block").children(
				modelsBlockSummary(),
				modelsBlockContents(s)
			);
			outer.child(modelBlock);
		}

		return outer;
		// @formatter:on
	}
}