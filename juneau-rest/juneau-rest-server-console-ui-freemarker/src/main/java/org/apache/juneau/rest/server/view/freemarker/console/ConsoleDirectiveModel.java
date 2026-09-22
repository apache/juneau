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

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * The {@code <@console>} document-shell FreeMarker directive: the thin chrome element an app {@code base.ftlh}
 * authors instead of hand-writing the {@code <!DOCTYPE html>} document, its head cascade, and its body scaffold.
 * Juneau expands it into the full document.
 *
 * <p>
 * {@code <@page>} captured the child page body and set the {@code pageBody} / {@code pageTab} / {@code pageInit} /
 * {@code pageCss} / {@code pageToolkitCss} / {@code pageToolkitJs} chrome variables, then included the app chrome
 * template. That chrome template's whole job is now {@code <@console>…</@console>}: this directive installs a
 * {@link ConsoleContext}, renders its body into a throwaway buffer so the nested capture slots
 * ({@code <@head>}/{@code <@scripts>}/{@code <@brand>}/{@code <@actions>}/{@code <@title>}/{@code <@footer>}) fold
 * into that context and the positional {@code <@navigation>}/{@code <@main>} directives write their markup into the
 * buffer in author order, then emits the document.
 *
 * <h5 class='section'>Head cascade (Juneau emits; the app does not write these tags):</h5>
 * <ol class='spaced-list'>
 * 	<li>{@code charset} / {@code viewport}
 * 	<li>the document {@code <title>} (the {@code title=} attribute, else {@code brand=}; omitted when neither is set)
 * 	<li>the {@code page-tab} meta when {@code <@page tab=>} set one
 * 	<li>the CSRF {@code <meta name="csrf-token">} (only when a {@link LoopbackBoundaryFilter} published a token)
 * 	<li>the {@code favicon=} {@code <link rel="icon">} (only when the attribute is present)
 * 	<li>{@code chrome.css} ({@link ConsoleChromeMixin#chromeCssUrl(org.apache.juneau.rest.server.RestRequest)})
 * 	<li>the theme pack {@code <link>} (a nested {@code <@theme>} wins over the {@code theme=} attribute; default
 * 		{@code open}), then that pack's FTL-constructed override {@code <style>} block if {@code <@theme>} declared tokens
 * 	<li>the {@code <@head phase="before-page-css">} pass-through (app CSS that must lose to a page-local {@code css=})
 * 	<li>the captured {@code pageCss}
 * 	<li>the toolkit CSS ({@code pageToolkitCss})
 * 	<li>the chrome-level {@code <@head>} pass-through
 * </ol>
 *
 * <h5 class='section'>Body:</h5>
 * <p>
 * A {@code <body>} carrying the CSRF {@code data-juneau-csrf} / {@code data-juneau-csrf-header} attributes (same
 * token gate as the head meta) plus any {@code <@body>}-captured app attributes, then:
 * <ol class='spaced-list'>
 * 	<li>the header (raw {@code <@title>} if present, else the default {@code icon=} + {@code brand=} header composed
 * 		with the {@code <@brand>} / {@code <@actions>} sub-slots; omitted entirely when nothing header-worthy is
 * 		authored) and the pre-main region (nav + author markup above {@code <@main/>}) &mdash; wrapped in a
 * 		{@code .jc-chrome} sticky stack when {@code chrome="true"}
 * 	<li>the {@code <main>} ({@code <@main/>})
 * 	<li>any author markup below {@code <@main/>}, then the {@code <@footer>} slot
 * 	<li>the toolkit JS ({@code pageToolkitJs}), the {@code <@scripts phase="after-toolkit">} pass-through, the
 * 		{@code pageInit} scripts, and the chrome-level {@code <@scripts>} pass-through
 * </ol>
 *
 * @since 10.0.0
 */
public final class ConsoleDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "console";

	private static final Set<String> ATTRS = Set.of("theme", "icon", "favicon", "brand", "title", "chrome");

	ConsoleDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked", // FreeMarker's raw params Map is String-keyed by contract.
		"resource" // FreeMarker owns env.getOut(); closing it would close the HTTP response.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@console> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var req = FreemarkerRenderScope.request();
		if (req == null)
			throw FtlAttrLists.reject("<@console> needs FreemarkerRenderScope.request() (renderer wrap).");

		// Resolve (and fail-closed validate) the theme= attribute up front; a nested <@theme> may still win below.
		var themeAttr = FtlAttrLists.scalar(p, "theme");
		var themeName = themeAttr.isEmpty() ? ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0) : themeAttr;
		if (! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(themeName))
			throw FtlAttrLists.reject("<@console> unknown theme name '" + themeName + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");
		var attrThemeUrl = ConsoleChromeMixin.themeAssetUrl(req, themeName);

		var icon = FtlAttrLists.scalar(p, "icon");
		var favicon = FtlAttrLists.scalar(p, "favicon");
		var brand = FtlAttrLists.scalar(p, "brand");
		var titleAttr = FtlAttrLists.scalar(p, "title");
		var chrome = booleanAttr(p, "chrome");

		// Render the body into a throwaway buffer: the capture slots fold into the context and emit nothing, while
		// <@navigation> / <@main> write their markup into the buffer in author order.  FreeMarker restores env.getOut()
		// to the real chrome output when body.render(...) returns.
		var ctx = new ConsoleContext();
		env.setCustomState(ConsoleContext.KEY, ctx);
		var buffer = new StringWriter();
		try {
			if (body != null)
				body.render(buffer);
		} finally {
			env.setCustomState(ConsoleContext.KEY, null);
		}

		var themePackUrl = ctx.themePackUrl != null ? ctx.themePackUrl : attrThemeUrl;

		var token = req.getAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE).asString().orElse(null);
		var header = req.getAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE).asString().orElse(null);
		var hasCsrf = token != null && ! token.isBlank();

		var out = env.getOut();
		out.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
		out.write("<meta charset=\"utf-8\">\n");
		out.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		// Document <title>: the title= attribute wins, else fall back to brand=; omit when neither is authored.
		var docTitle = ! titleAttr.isEmpty() ? titleAttr : brand;
		if (! docTitle.isEmpty())
			out.write("<title>" + attrEscape(docTitle) + "</title>\n");
		var pageTab = envScalar(env, "pageTab");
		if (! pageTab.isEmpty())
			out.write("<meta name=\"page-tab\" content=\"" + attrEscape(pageTab) + "\">\n");
		if (hasCsrf)
			out.write("<meta name=\"csrf-token\" content=\"" + attrEscape(token) + "\">\n");
		if (! favicon.isEmpty())
			out.write("<link rel=\"icon\" href=\"" + attrEscape(favicon) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(ConsoleChromeMixin.chromeCssUrl(req)) + "\">\n");
		out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(themePackUrl) + "\">\n");
		if (ctx.themeOverrideBlock != null)
			out.write("<style>" + ctx.themeOverrideBlock + "</style>\n");
		// App CSS that must lose to page-local css=: emitted BEFORE pageCss so a page rule still wins.
		if (ctx.headBeforePageCss != null)
			out.write(ctx.headBeforePageCss);
		for (var href : envList(env, "pageCss"))
			out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(href) + "\">\n");
		for (var href : envList(env, "pageToolkitCss"))
			out.write("<link rel=\"stylesheet\" href=\"" + attrEscape(href) + "\" data-toolkit-css>\n");
		if (ctx.head != null)
			out.write(ctx.head);
		out.write("\n</head>\n");

		out.write("<body");
		if (hasCsrf) {
			out.write(" data-juneau-csrf=\"" + attrEscape(token) + "\"");
			if (header != null && ! header.isBlank())
				out.write(" data-juneau-csrf-header=\"" + attrEscape(header) + "\"");
		}
		// App-authored <body> attributes (e.g. a runtime data-attribute switch) captured via the <@body> slot.
		if (ctx.bodyAttrs != null && ! ctx.bodyAttrs.isBlank())
			out.write(" " + ctx.bodyAttrs.trim());
		out.write(">\n");

		// Split the body buffer at the <@main/> placeholder: everything before is the pre-main chrome region
		// (nav + any author markup above <@main/>), everything after is emitted below the <main>.
		var buf = buffer.toString();
		var mainIdx = buf.indexOf(ConsoleContext.MAIN_PLACEHOLDER);
		String preMain, postMain, mainMarkup;
		if (mainIdx >= 0) {
			preMain = buf.substring(0, mainIdx);
			postMain = buf.substring(mainIdx + ConsoleContext.MAIN_PLACEHOLDER.length());
			mainMarkup = ctx.mainMarkup != null ? ctx.mainMarkup : "";
		} else {
			preMain = buf;
			postMain = "";
			mainMarkup = "";
		}

		var headerHtml = header(ctx, icon, brand);
		// Optional .jc-chrome: header + pre-main region as one sticky stack (the CSS contract chrome.css ships).
		if (chrome) {
			out.write("<div class=\"jc-chrome\">\n");
			out.write(headerHtml);
			out.write(preMain);
			out.write("</div>\n");
		} else {
			out.write(headerHtml);
			out.write(preMain);
		}
		out.write(mainMarkup);
		out.write(postMain);
		if (ctx.footer != null)
			out.write("<footer class=\"jc-page-footer\">" + ctx.footer + "</footer>\n");
		for (var src : envList(env, "pageToolkitJs"))
			out.write("<script src=\"" + attrEscape(src) + "\" data-toolkit-js></script>\n");
		// App scripts that must run after the toolkit pack but before pageInit's page-local mount.
		if (ctx.scriptsAfterToolkit != null)
			out.write(ctx.scriptsAfterToolkit);
		for (var src : envList(env, "pageInit"))
			out.write("<script src=\"" + attrEscape(src) + "\"></script>\n");
		if (ctx.scripts != null)
			out.write(ctx.scripts);
		out.write("\n</body>\n</html>\n");
	}

	/**
	 * Composes the header region: a raw {@code <@title>} slot replaces the whole header when present; otherwise the
	 * default {@code icon=} + {@code brand=} header, with the {@code <@brand>} sub-slot replacing only the brand
	 * region's inner markup and the {@code <@actions>} sub-slot supplying the header-actions region.
	 *
	 * <p>
	 * Returns {@code ""} (no {@code <header>} at all) when nothing header-worthy is authored &mdash; no {@code icon=},
	 * no {@code brand=}, and no {@code <@title>}/{@code <@brand>}/{@code <@actions>} slot &mdash; so a masthead-less
	 * console does not get an empty sticky bar.
	 *
	 * <p>
	 * {@code icon=} paints {@code .jc-logo} as a <b>background-image {@code <div>}</b> (an inline
	 * {@code background-image} override on the shipped {@code chrome.css} {@code .jc-logo} contract), not an
	 * {@code <img>}: {@code chrome.css}'s {@code .jc-logo} is a hard-coded background-image element with no
	 * {@code object-fit}, so an {@code <img>} would collide with the shipped art.
	 */
	private static String header(ConsoleContext ctx, String icon, String brand) {
		if (ctx.title != null)
			return ctx.title;
		if (icon.isEmpty() && brand.isEmpty() && ctx.brand == null && ctx.actions == null)
			return "";
		var sb = new StringBuilder("<header class=\"jc-header\">");
		if (ctx.brand != null) {
			sb.append("<div class=\"jc-brand\">").append(ctx.brand).append("</div>");
		} else {
			sb.append("<div class=\"jc-brand\">");
			if (! icon.isEmpty()) {
				sb.append("<div class=\"jc-logo\" role=\"img\" style=\"")
					.append(attrEscape("background-image:url('" + cssUrl(icon) + "')")).append("\"");
				if (! brand.isEmpty())
					sb.append(" aria-label=\"").append(attrEscape(brand)).append("\"");
				sb.append("></div>");
			}
			if (! brand.isEmpty())
				sb.append("<div class=\"jc-brand-title\">").append(attrEscape(brand)).append("</div>");
			sb.append("</div>");
		}
		if (ctx.actions != null)
			sb.append("<div class=\"jc-header-actions\">").append(ctx.actions).append("</div>");
		sb.append("</header>");
		return sb.toString();
	}

	/**
	 * Reads a boolean {@code <@console>} attribute (accepts {@code "true"} / {@code "false"}; absent/blank is
	 * {@code false}); any other value is rejected fail-closed.
	 */
	private static boolean booleanAttr(Map<String, TemplateModel> p, String name) throws TemplateModelException {
		var v = FtlAttrLists.scalar(p, name);
		if (v.isEmpty() || "false".equals(v))
			return false;
		if ("true".equals(v))
			return true;
		throw FtlAttrLists.reject("<@console> " + name + "= must be true or false.");
	}

	/** Reads a scalar chrome variable set by {@code <@page>}, empty when unset. */
	private static String envScalar(Environment env, String name) throws TemplateModelException {
		var m = env.getVariable(name);
		if (m == null)
			return "";
		var u = DeepUnwrap.unwrap(m);
		return u == null ? "" : String.valueOf(u).trim();
	}

	/** Reads a sequence-valued chrome variable set by {@code <@page>} (e.g. {@code pageCss}), empty when unset. */
	private static List<String> envList(Environment env, String name) throws TemplateModelException {
		var m = env.getVariable(name);
		if (m == null)
			return List.of();
		var u = DeepUnwrap.unwrap(m);
		if (u instanceof Collection<?> c) {
			var out = new ArrayList<String>();
			for (var item : c)
				if (item != null)
					out.add(String.valueOf(item));
			return out;
		}
		return List.of();
	}

	/** Minimal HTML-attribute escape for an author-/request-supplied value placed in a double-quoted attribute. */
	private static String attrEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Escapes a URL for a single-quoted CSS {@code url('…')} literal (backslash-escapes {@code \} and {@code '} so it
	 * cannot break out of the string); the surrounding {@code style="…"} attribute is then {@link #attrEscape}d.
	 */
	private static String cssUrl(String s) {
		return s.replace("\\", "\\\\").replace("'", "\\'");
	}
}
