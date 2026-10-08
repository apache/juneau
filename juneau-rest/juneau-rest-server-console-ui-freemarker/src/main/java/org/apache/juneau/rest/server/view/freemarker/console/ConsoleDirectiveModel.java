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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@console>} document-shell FreeMarker directive.
 *
 * <p>
 * Renders its body into the per-render {@link PageCapture} (theme, navigation, header/footer slots, the banner region
 * before {@code <@main/>}), then writes the document: the server-rendered {@code <head>}, the {@code <body>} tag, the
 * {@code #juneau-page} contract island, one {@code <template>} per markup body, the {@code juneau-console.js} shell,
 * and the server-emitted scripts. The shell renders the header, nav, cards and footer from the contract.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@console brand="Release Manager" icon="/app/logo.svg" title="Releases"&gt;
 * 	  &lt;@navigation&gt;
 * 	    &lt;@node id="home" label="Home" href="/home"/&gt;
 * 	  &lt;/@navigation&gt;
 * 	  &lt;@main/&gt;
 * 	  &lt;@footer text="ACME"/&gt;
 * 	&lt;/@console&gt;
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
public final class ConsoleDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "console";

	static final Set<String> ATTRS = Set.of("theme", "icon", "favicon", "brand", "title", "chrome");

	private final boolean devMode;

	ConsoleDirectiveModel(boolean devMode) {
		this.devMode = devMode;
	}

	@Override
	@SuppressWarnings({
		"java:S3776", // Attribute validation and document emission read best as one straight-line method.
		"resource", // FreeMarker owns env.getOut(); closing it would close the HTTP response.
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@console> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var req = FreemarkerRenderScope.request();
		if (n(req))
			throw FtlAttrLists.reject("<@console> needs FreemarkerRenderScope.request() (renderer wrap).");

		var cap = PageCapture.of(env);
		if (cap.consoleOpen || cap.consoleDone)
			throw FtlAttrLists.reject("<@console> cannot be nested inside another <@console>.");

		// Resolve (and fail-closed validate) the theme= attribute up front; a nested <@theme> may still win below.
		var themeAttr = FtlAttrLists.scalar(p, "theme");
		var themeName = themeAttr.isEmpty() ? ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0) : themeAttr;
		if (! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(themeName))
			throw FtlAttrLists.reject("<@console> unknown theme name '" + themeName + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");

		var icon = FtlAttrLists.scalar(p, "icon");
		var favicon = FtlAttrLists.scalar(p, "favicon");
		var brand = FtlAttrLists.scalar(p, "brand");
		var titleAttr = FtlAttrLists.scalar(p, "title");
		var chrome = FtlAttrLists.strictBoolean(p, NAME, "chrome", false);

		// Document <title>: the title= attribute wins, else fall back to brand=; omit when neither is authored.
		var docTitle = ! titleAttr.isEmpty() ? titleAttr : brand;
		cap.title(docTitle);
		cap.header(h -> {
			if (! brand.isEmpty())
				h.title(brand);
			if (! icon.isEmpty())
				h.logo(icon, req.getUriResolver().resolve("context:/"), brand.isEmpty() ? null : brand);
			h.chrome(chrome);
		});

		cap.consoleOpen = true;
		try {
			if (nn(body))
				body.render(cap.consoleBuffer());
		} finally {
			cap.consoleOpen = false;
		}
		cap.checkConsoleClose();
		cap.resolveActiveNav();

		if (n(cap.themeCssUrl)) {
			cap.themeCssUrl = ConsoleChromeMixin.themeAssetUrl(req, themeName);
			cap.theme(themeName);
		}

		var token = req.getAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE).asString().orElse(null);
		var csrfHeader = req.getAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE).asString().orElse(null);
		var hasCsrf = inb(token);

		if (devMode) {
			var findings = PageContractSchema.get().validate(cap.toContractJson(), cap.templates().keySet());
			if (! findings.isEmpty())
				throw FtlAttrLists.reject(String.format("Page contract failed schema validation: '%s'.", String.join("; ", findings)));
		}

		var out = env.getOut();
		out.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
		out.write("<meta charset=\"utf-8\">\n");
		out.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		if (! docTitle.isEmpty())
			out.write("<title>" + attrEscape(docTitle) + "</title>\n");
		if (! cap.tab().isEmpty())
			out.write("<meta name=\"page-tab\" content=\"" + attrEscape(cap.tab()) + "\">\n");
		if (hasCsrf)
			out.write("<meta name=\"csrf-token\" content=\"" + attrEscape(token) + "\">\n");
		if (! favicon.isEmpty())
			out.write("<link rel=\"icon\" href=\"" + attrEscape(favicon) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(ConsoleChromeMixin.chromeCssUrl(req)) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(cap.themeCssUrl) + "\">\n");
		if (nn(cap.themeOverrideBlock))
			out.write("<style>" + cap.themeOverrideBlock + "</style>\n");
		// App CSS that must lose to page-local css=: emitted BEFORE the page css so a page rule still wins.
		if (nn(cap.headBeforePageCss))
			out.write(cap.headBeforePageCss);
		for (var href : cap.vendorCss())
			out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(href) + "\" data-toolkit-css>\n");
		for (var href : cap.cssHrefs())
			out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(href) + "\">\n");
		for (var href : cap.runtimeCss())
			out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(href) + "\" data-toolkit-css>\n");
		if (nn(cap.head))
			out.write(cap.head);
		out.write("\n</head>\n");

		out.write("<body");
		if (hasCsrf) {
			out.write(" data-juneau-csrf=\"" + attrEscape(token) + "\"");
			if (inb(csrfHeader))
				out.write(" data-juneau-csrf-header=\"" + attrEscape(csrfHeader) + "\"");
		}
		if (inb(cap.bodyAttrs))
			out.write(" " + cap.bodyAttrs.trim());
		out.write(">\n");

		cap.shellUrl(ConsoleChromeMixin.consoleJsUrl(req));
		cap.writeBody(out);
		out.write("\n");

		for (var src : cap.vendorJs())
			out.write("<script src=\"" + attrEscape(src) + "\" data-toolkit-js></script>\n");
		for (var src : cap.runtimeJs())
			out.write("<script src=\"" + attrEscape(src) + "\" data-toolkit-js></script>\n");
		// App scripts that must run after the toolkit pack but before the page-local init= scripts.
		if (nn(cap.scriptsAfterToolkit))
			out.write(cap.scriptsAfterToolkit);
		for (var src : cap.initScripts())
			out.write("<script src=\"" + attrEscape(src) + "\"></script>\n");
		if (nn(cap.scripts))
			out.write(cap.scripts);
		out.write("\n</body>\n</html>\n");
		cap.consoleDone = true;
	}

	/** Minimal HTML-attribute escape for an author-/request-supplied value placed in a double-quoted attribute. */
	private static String attrEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
