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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.http.MediaType.*;
import static org.apache.juneau.commons.utils.Utils.*;

import org.apache.juneau.MarshallingSession;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.swap.*;

/**
 * Generates a Redoc-style documentation interface from an OpenAPI 3.1 document.
 *
 * <p>
 * Renders a two-column layout — a left sidebar containing the operation table-of-contents and a right
 * content panel with full operation descriptions, parameters, request/response schemas, and components.
 *
 * <p>
 * This is a sibling of {@link OpenApiUI} that provides a documentation-page-style rendering rather
 * than the interactive collapsing-block layout. Both are server-rendered HTML — neither bundles
 * external JavaScript packages.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S1192" // String literals repeated for clarity in UI generation
})
public class RedocUI extends ObjectSwap<OpenApi,Div> {

	// @formatter:off
	static final FileFinder RESOURCES = FileFinder
		.create(BasicBeanStore.INSTANCE)
		.cp(RedocUI.class, null, true)
		.dir(",")
		.caching(Boolean.getBoolean("RestContext.disableClasspathResourceCaching.b") ? -1 : 1_000_000)
		.build();
	// @formatter:on

	@Override
	public MediaType[] forMediaTypes() {
		return new MediaType[] { HTML };
	}

	@Override
	public Div swap(MarshallingSession session, OpenApi openApi) throws Exception {
		var css = RESOURCES.getString("files/htdocs/styles/RedocUI.css", null)
			.orElse(RESOURCES.getString("RedocUI.css", null).orElse(DEFAULT_CSS));

		var sidebar = sidebar(openApi);
		var content = content(openApi);

		return div(style(css), div(sidebar, content).class_("redoc-container")).class_("redoc-ui");
	}

	private static Div sidebar(OpenApi openApi) {
		var nav = div().class_("redoc-sidebar");
		var info = openApi.getInfo();
		if (nn(info) && nn(info.getTitle()))
			nav.child(div(info.getTitle()).class_("redoc-title"));

		var paths = openApi.getPaths();
		if (nn(paths)) {
			var ul = ul().class_("redoc-toc");
			paths.forEach((path, pathItem) -> appendOperations(ul, path, pathItem));
			nav.child(ul);
		}
		if (nn(openApi.getComponents()) && nn(openApi.getComponents().getSchemas())) {
			var ul = ul().class_("redoc-toc redoc-models-toc");
			ul.child(li(div("Models").class_("redoc-section-title")));
			openApi.getComponents().getSchemas().forEach((name, schema) -> ul.child(li(a("#model-" + name, name))));
			nav.child(ul);
		}
		return nav;
	}

	private static void appendOperations(Ul ul, String path, PathItem item) {
		appendOperation(ul, path, "GET", item.getGet());
		appendOperation(ul, path, "PUT", item.getPut());
		appendOperation(ul, path, "POST", item.getPost());
		appendOperation(ul, path, "DELETE", item.getDelete());
		appendOperation(ul, path, "PATCH", item.getPatch());
		appendOperation(ul, path, "HEAD", item.getHead());
		appendOperation(ul, path, "OPTIONS", item.getOptions());
		appendOperation(ul, path, "TRACE", item.getTrace());
	}

	private static void appendOperation(Ul ul, String path, String verb, Operation op) {
		if (op == null)
			return;
		var anchor = "op-" + verb.toLowerCase() + "-" + path.replaceAll("[^A-Za-z0-9]", "_");
		var label = nn(op.getSummary()) ? op.getSummary() : verb + " " + path;
		ul.child(li(span(verb).class_("redoc-method redoc-method-" + verb.toLowerCase()), a("#" + anchor, label)));
	}

	private static Div content(OpenApi openApi) {
		var contentDiv = div().class_("redoc-content");
		var info = openApi.getInfo();
		if (nn(info)) {
			if (nn(info.getTitle()))
				contentDiv.child(h1(info.getTitle()));
			if (nn(info.getVersion()))
				contentDiv.child(div("Version: " + info.getVersion()).class_("redoc-version"));
			if (nn(info.getDescription()))
				contentDiv.child(div(info.getDescription()).class_("redoc-description"));
		}

		var paths = openApi.getPaths();
		if (nn(paths))
			paths.forEach((path, pathItem) -> renderPathOperations(contentDiv, path, pathItem));

		if (nn(openApi.getComponents()) && nn(openApi.getComponents().getSchemas())) {
			contentDiv.child(h2("Models"));
			openApi.getComponents().getSchemas().forEach((name, schema) -> {
				contentDiv.child(h3(name).id("model-" + name));
				if (nn(schema.getDescription()))
					contentDiv.child(div(schema.getDescription()).class_("redoc-model-desc"));
			});
		}
		return contentDiv;
	}

	private static void renderPathOperations(Div contentDiv, String path, PathItem item) {
		renderOperation(contentDiv, path, "GET", item.getGet());
		renderOperation(contentDiv, path, "PUT", item.getPut());
		renderOperation(contentDiv, path, "POST", item.getPost());
		renderOperation(contentDiv, path, "DELETE", item.getDelete());
		renderOperation(contentDiv, path, "PATCH", item.getPatch());
		renderOperation(contentDiv, path, "HEAD", item.getHead());
		renderOperation(contentDiv, path, "OPTIONS", item.getOptions());
		renderOperation(contentDiv, path, "TRACE", item.getTrace());
	}

	private static void renderOperation(Div contentDiv, String path, String verb, Operation op) {
		if (op == null)
			return;
		var anchor = "op-" + verb.toLowerCase() + "-" + path.replaceAll("[^A-Za-z0-9]", "_");
		contentDiv.child(div(
			h2(verb + " " + path).id(anchor).class_("redoc-op-title redoc-op-title-" + verb.toLowerCase()),
			nn(op.getSummary()) ? div(op.getSummary()).class_("redoc-op-summary") : null,
			nn(op.getDescription()) ? div(op.getDescription()).class_("redoc-op-desc") : null
		).class_("redoc-op-block"));
	}

	private static final String DEFAULT_CSS =
		".redoc-ui{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#333;}" +
		".redoc-container{display:flex;min-height:100vh;}" +
		".redoc-sidebar{width:260px;background:#f4f5f7;padding:20px;border-right:1px solid #e1e4e8;overflow-y:auto;}" +
		".redoc-content{flex:1;padding:30px 40px;background:#fff;}" +
		".redoc-title{font-size:1.4em;font-weight:bold;margin-bottom:15px;color:#222;}" +
		".redoc-toc{list-style:none;padding:0;margin:0 0 20px 0;}" +
		".redoc-toc li{margin:6px 0;display:flex;align-items:center;}" +
		".redoc-toc a{color:#0366d6;text-decoration:none;font-size:0.9em;margin-left:8px;}" +
		".redoc-toc a:hover{text-decoration:underline;}" +
		".redoc-section-title{font-weight:bold;margin-top:15px;color:#666;text-transform:uppercase;font-size:0.8em;}" +
		".redoc-method{display:inline-block;font-size:0.7em;font-weight:bold;padding:2px 6px;border-radius:3px;color:#fff;text-transform:uppercase;}" +
		".redoc-method-get{background:#61affe;}" +
		".redoc-method-post{background:#49cc90;}" +
		".redoc-method-put{background:#fca130;}" +
		".redoc-method-delete{background:#f93e3e;}" +
		".redoc-method-patch{background:#50e3c2;}" +
		".redoc-method-head{background:#9012fe;}" +
		".redoc-method-options{background:#0d5aa7;}" +
		".redoc-method-trace{background:#ebebeb;color:#333;}" +
		".redoc-op-block{border-bottom:1px solid #e1e4e8;padding:20px 0;}" +
		".redoc-op-title{margin:0 0 10px 0;font-size:1.2em;}" +
		".redoc-op-summary{font-weight:bold;margin-bottom:8px;}" +
		".redoc-op-desc,.redoc-description,.redoc-model-desc{color:#555;line-height:1.5;}" +
		".redoc-version{color:#666;font-size:0.9em;margin-bottom:15px;}";
}
