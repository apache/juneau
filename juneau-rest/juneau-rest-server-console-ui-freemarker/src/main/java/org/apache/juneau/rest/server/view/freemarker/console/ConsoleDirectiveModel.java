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
import java.util.logging.*;

import org.apache.juneau.rest.server.*;
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

	private static final Logger LOG = Logger.getLogger(ConsoleDirectiveModel.class.getName());

	static final Set<String> ATTRS = Set.of("theme", "icon", "favicon", "brand", "title", "chrome", "facts", "scheme", "schemeToggle");

	private final boolean devMode;
	private final CardTypeRegistry cardTypes;
	private final ToolkitPackRegistry packs;

	ConsoleDirectiveModel(boolean devMode, CardTypeRegistry cardTypes, ToolkitPackRegistry packs) {
		this.devMode = devMode;
		this.cardTypes = cardTypes;
		this.packs = packs;
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

		// Fail-closed validate an explicit theme= attribute up front; a nested <@theme> (or, absent both, a theme seeded
		// by a page spec) decides the final name below.
		var themeAttr = FtlAttrLists.scalar(p, "theme");
		if (! themeAttr.isEmpty() && ! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(themeAttr))
			throw FtlAttrLists.reject("<@console> unknown theme name '" + themeAttr + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");

		// Colour scheme: an explicit attribute wins over a page-spec value, which wins over the default.
		var schemeAttr = FtlAttrLists.scalar(p, "scheme");
		if (! schemeAttr.isEmpty() && ! ConsoleChromeMixin.SCHEMES.contains(schemeAttr))
			throw FtlAttrLists.reject("<@console> unknown scheme '" + schemeAttr + "'.  Schemes: "
				+ String.join(", ", ConsoleChromeMixin.SCHEMES) + ".");
		var scheme = ! schemeAttr.isEmpty() ? schemeAttr : nn(cap.scheme()) ? cap.scheme() : ConsoleChromeMixin.SCHEMES.get(0);
		var schemeToggle = FtlAttrLists.strictBoolean(p, NAME, "schemeToggle", cap.schemeToggle());

		var icon = FtlAttrLists.scalar(p, "icon");
		var favicon = FtlAttrLists.scalar(p, "favicon");
		var brand = FtlAttrLists.scalar(p, "brand");
		var titleAttr = FtlAttrLists.scalar(p, "title");
		var chrome = FtlAttrLists.strictBoolean(p, NAME, "chrome", false);
		var factsAttr = FtlAttrLists.map(p, NAME, "facts");

		// Document <title>: a title seeded by a page spec wins; else the title= attribute, else brand=; omitted when none.
		var docTitle = ! titleAttr.isEmpty() ? titleAttr : brand;
		cap.titleDefault(docTitle);
		cap.header(h -> {
			if (! brand.isEmpty())
				h.title(brand);
			if (! icon.isEmpty())
				h.logo(icon, req.getUriResolver().resolve("context:/"), brand.isEmpty() ? null : brand);
			h.chrome(chrome);
		});

		// Chrome-side facts: last-wins against <@facts>; the one strict check against PageSpec.facts runs at </@console>.
		if (! factsAttr.isEmpty())
			cap.mergeChromeFacts(factsAttr);

		cap.consoleTheme = themeAttr.isEmpty() ? null : themeAttr;
		cap.consoleOpen = true;
		try {
			if (nn(body))
				body.render(cap.consoleBuffer());
		} finally {
			cap.consoleOpen = false;
		}
		cap.checkConsoleClose();
		try {
			cap.checkWiring(cardTypes);
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
		cap.resolveActiveNav();

		// A page that never reached <@page> (a PageSpec rendered straight through the chrome) has not had its seeded
		// toolkits and card-required packs resolved yet; <@page> does it itself, so never twice.
		// The guard is a pure optimisation: toolkitAssets overwrites, and nothing after <@page> can change its inputs
		// (<@card> is only legal inside <@page>), so a second resolve would be identical and no test can observe it.
		if (! cap.assetsResolved())
			PageDirectiveModel.resolveAssets(env, cap, packs);

		if (n(cap.themeCssUrl)) {
			// No <@theme> ran: an explicit theme= wins, else a theme seeded by a page spec is inherited, else the default.
			var themeName = ! themeAttr.isEmpty() ? themeAttr : cap.themeOrNull();
			if (themeName == null)
				themeName = ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0);
			cap.themeCssUrl = ConsoleChromeMixin.themeAssetUrl(req, themeName);
			cap.theme(themeName);
		}

		// Each embed asks for its own value, so a masking boundary never repeats the same masked token in one page.
		var metaToken = csrfToken(req);
		var bodyToken = csrfToken(req);
		var csrfHeader = req.getAttribute(RequestBoundaryFilter.HEADER_ATTRIBUTE).asString().orElse(null);
		var hasCsrf = inb(metaToken) && inb(bodyToken);

		// A page that carries a token must not be cached, and the header has to be set before any of the body is
		// written.  A response that is already committed can no longer be marked, so it never carries the token: both
		// automatic embeds and any spec- or <@body>-authored csrf attributes are dropped, and the page's own writes then
		// fail visibly instead of leaking the secret.
		var bodyAttrs = cap.bodyAttrs;
		if (req.getResponse().getHttpServletResponse().isCommitted()) {
			var omitted = hasCsrf;
			hasCsrf = false;
			bodyAttrs = cap.bodyAttrsWithout("data-juneau-csrf", "data-juneau-csrf-header");
			if (bodyAttrs != null && ! bodyAttrs.equals(cap.bodyAttrs.strip()))
				omitted = true;
			if (omitted)
				LOG.fine("Response already committed; the CSRF token is not embedded in the page.");
		} else if (hasCsrf || cap.hasBodyAttr("data-juneau-csrf")) {
			RequestBoundaryFilter.markTokenBearing(req.getResponse().getHttpServletResponse());
		}

		if (devMode) {
			var findings = PageContractSchema.get().validate(cap.toContractJson(), cap.templates().keySet());
			if (! findings.isEmpty())
				throw FtlAttrLists.reject(f("Page contract failed schema validation: '%s'.", String.join("; ", findings)));
		}

		var out = env.getOut();
		var nonce = req.getAttribute(RestRequest.CSP_NONCE_ATTR).as(String.class).orElse(null);
		var htmlAttrs = ConsoleChromeMixin.schemeHtmlAttributes(scheme, schemeToggle);
		out.write("<!DOCTYPE html>\n<html lang=\"en\"" + (htmlAttrs.isEmpty() ? "" : " " + htmlAttrs) + ">\n<head>\n");
		// The pre-paint scheme script runs before any stylesheet so the first paint already has the right scheme.
		out.write(ConsoleChromeMixin.schemeScriptElement(nonce) + "\n");
		out.write("<meta charset=\"utf-8\">\n");
		out.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		if (! cap.title().isEmpty())
			out.write("<title>" + attrEscape(cap.title()) + "</title>\n");
		if (! cap.tab().isEmpty())
			out.write("<meta name=\"page-tab\" content=\"" + attrEscape(cap.tab()) + "\">\n");
		if (hasCsrf)
			out.write("<meta name=\"csrf-token\" content=\"" + attrEscape(metaToken) + "\">\n");
		if (! favicon.isEmpty())
			out.write("<link rel=\"icon\" href=\"" + attrEscape(favicon) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(ConsoleChromeMixin.chromeCssUrl(req)) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(ConsoleChromeMixin.themeAssetUrl(req, "dark")) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(cap.themeCssUrl) + "\">\n");
		if (nn(cap.themeOverrideBlock))
			out.write("<style" + ConsoleChromeMixin.nonceAttr(nonce) + ">" + cap.themeOverrideBlock + "</style>\n");
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
		// A spec- or <@body>-authored csrf attribute (merged by checkConsoleClose above) wins over the auto-detection.
		// Either name suppresses both auto attributes, so a token and its header never come from different sources.
		if (hasCsrf && ! cap.hasBodyAttr("data-juneau-csrf") && ! cap.hasBodyAttr("data-juneau-csrf-header")) {
			out.write(" data-juneau-csrf=\"" + attrEscape(bodyToken) + "\"");
			if (inb(csrfHeader))
				out.write(" data-juneau-csrf-header=\"" + attrEscape(csrfHeader) + "\"");
		}
		if (inb(bodyAttrs))
			out.write(" " + bodyAttrs.trim());
		out.write(">\n");

		cap.shellUrl(ConsoleChromeMixin.consoleJsUrl(req));
		cap.writeBody(out);
		out.write("\n");

		for (var src : cap.vendorJs())
			out.write("<script src=\"" + attrEscape(src) + "\" data-toolkit-js></script>\n");
		for (var src : cap.runtimeJs())
			out.write("<script src=\"" + attrEscape(src) + "\" data-toolkit-js></script>\n");
		// The badge runtime mounts header.badges from the contract on load.  Not a toolkit pack: a chrome-template
		// <@badge> is only known here, after the page's packs were resolved.
		if (cap.hasBadges())
			out.write("<script src=\"" + attrEscape(ConsoleChromeMixin.badgesJsUrl(req)) + "\" data-toolkit-js></script>\n");
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

	/**
	 * One CSRF token value for one embed, or <jk>null</jk> when the request carries none.
	 *
	 * <p>
	 * Prefers the filter's token supplier, read straight from the servlet request: converting it through the REST
	 * attribute view would stringify the supplier itself.  Only the filter's own {@link RequestBoundaryFilter.TokenSupplier}
	 * is trusted; a value of any other type under that name was set by request code, so no token is embedded.  Falls
	 * back to the single string value for a filter that publishes only that.
	 */
	private static String csrfToken(RestRequest req) {
		var supplier = req.getHttpServletRequest().getAttribute(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE);
		if (supplier instanceof RequestBoundaryFilter.TokenSupplier s)
			return s.get();
		if (supplier != null)
			return null;
		return req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE).asString().orElse(null);
	}

	/** Minimal HTML-attribute escape for an author-/request-supplied value placed in a double-quoted attribute. */
	private static String attrEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
