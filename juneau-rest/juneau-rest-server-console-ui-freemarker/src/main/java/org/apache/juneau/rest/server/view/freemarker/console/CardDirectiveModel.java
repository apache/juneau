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

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@card>} shared FreeMarker directive: always emits a {@code .jc-card}. {@code type=} is
 * {@code html | js | datatables | json | calendar}; omit {@code type} = html sugar. Runs inside a
 * {@code <@page>} capture and writes its trusted HTML straight into that capture via
 * {@code env.getOut().write(...)} &mdash; there is exactly one {@code fromMarkup} call, on the
 * captured page body, so a card body is not double-escaped.
 *
 * @since 10.0.0
 */
public final class CardDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "card";

	/**
	 * The page-cards sidecar wire contract version &mdash; the sidecar's own {@code "1"}, deliberately
	 * <b>not</b> aliased to {@code ViewDef.CONTRACT_VERSION} (F3): a VIEW_META bump must not silently
	 * restamp every html/js sidecar.
	 */
	public static final String SIDECAR_CONTRACT_VERSION = "1";

	/**
	 * The html-card override attributes copied verbatim onto the strict-JSON sidecar for the page-cards
	 * runtime, in emit order. When an html card carries any of these, it emits a sidecar (and {@code id=}
	 * becomes required); a markup-only html card carries none and stays sidecar-free (b01).
	 */
	private static final List<String> OVERRIDE_ATTRS = List.of("template", "src", "source", "fn", "renderers", "utils");

	private static final Set<String> ATTRS = Set.of("type", "id", "src", "template", "source", "fn", "renderers", "utils");

	CardDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked", // FreeMarker's raw params Map is String-keyed by contract.
		"resource" // FreeMarker owns env.getOut(); closing it would close the HTTP response.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@card> uses type= only; format= is not a valid attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var type = FtlAttrLists.scalar(p, "type");
		var id = FtlAttrLists.scalar(p, "id");

		var out = env.getOut();
		switch (type) {
			case "", "html" -> out.write(htmlCard(p, id, capture(body)));
			case "js" -> out.write(jsCard(id, capture(body)));
			case "datatables" -> out.write(datatablesCard(id, capture(body)));
			case "calendar" -> out.write(calendarCard(id, capture(body)));
			case "json" -> out.write(jsonCard(id, capture(body)));
			default -> throw FtlAttrLists.reject(
				"<@card> type= must be one of html|js|datatables|json|calendar; got '" + type + "'.");
		}
	}

	/**
	 * Desugars a {@code type="json"} envelope. The body is JSON5 (parsed once via {@link CardEnvelope});
	 * its inner {@code type} selects the emit. {@code type: 'html'} emits the same {@code .jc-card} as the
	 * html sugar, using the envelope's {@code content} string as the trusted markup. A missing inner
	 * {@code type} is an author error.
	 */
	private static String jsonCard(String id, String body) throws TemplateModelException {
		var env = CardEnvelope.parse(body);
		var innerType = env.getString("type");
		if (innerType == null || innerType.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"json\"> json envelope requires type.");
		return switch (innerType) {
			case "html" -> htmlCard(id, nullToEmpty(env.getString("content")));
			case "datatables" -> {
				// Desugar pin: a json envelope carrying type:'datatables' emits the SAME wide markup +
				// lifted SLOT_META sidecar as the type="datatables" sugar (liftTable ignores the envelope's
				// own type key). id= is required because the sidecar needs a mount target.
				if (id.isEmpty())
					throw FtlAttrLists.reject("<@card type=\"datatables\"> requires id=.");
				yield datatablesCardFromCatalog(id, env);
			}
			case "calendar" -> {
				// Desugar pin: a json envelope carrying type:'calendar' emits the SAME calendar mount + sidecar
				// as the type="calendar" sugar (the envelope's own type key is dropped from the lifted catalog).
				if (id.isEmpty())
					throw FtlAttrLists.reject("<@card type=\"calendar\"> requires id=.");
				yield calendarCardFromCatalog(id, env);
			}
			default -> throw FtlAttrLists.reject(
				"<@card type=\"json\"> envelope type must be one of html|js|datatables|calendar; got '" + innerType + "'.");
		};
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	/**
	 * Emits a {@code type="js"} card: an empty {@code .jc-card} carrying the author {@code id=} as its
	 * mount target, plus a strict-JSON sidecar naming the {@code populate} function. The js body is the
	 * populate name (trimmed text), not JSON. {@code id=} is required, and the populate name must be
	 * non-blank.
	 */
	private static String jsCard(String id, String body) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"js\"> requires id=.");
		var populate = body.trim();
		if (populate.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"js\"> requires a non-blank populate name as its body.");
		var m = new JsonMap();
		m.put("contractVersion", SIDECAR_CONTRACT_VERSION);
		m.put("id", id);
		m.put("populate", populate);
		return htmlCard(id, "") + sidecar(id, scriptSafeJson(Json.DEFAULT.write(m)));
	}

	/**
	 * Emits a {@code type="datatables"} card: a {@code .jc-card} whose <b>inner</b> {@code jc-card-body}
	 * carries {@code data-juneau-layout="wide"} and the author {@code id=} as the mount target (Q4: wide
	 * lives on the inner slot, not the outer card, so {@code :has} does not chrome a sibling title card).
	 * The sidecar's {@code table} is the lifted SLOT_META (F2), and any author {@code page} (renderer ids)
	 * rides alongside {@code table}, not inside it.
	 *
	 * <p>
	 * A string body is a table URL (existing fetch path); an object body is an author catalog that
	 * {@link CardEnvelope#liftTable} lifts. {@code id=} is required.
	 */
	private static String datatablesCard(String id, String body) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"datatables\"> requires id=.");
		var trimmed = body.trim();
		if (trimmed.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"datatables\"> requires a table URL or catalog as its body.");

		if (trimmed.startsWith("{"))
			return datatablesCardFromCatalog(id, CardEnvelope.parse(trimmed));

		// A bare string body is an existing table URL (no lift).
		var sidecar = new JsonMap();
		sidecar.put("contractVersion", SIDECAR_CONTRACT_VERSION);
		sidecar.put("id", id);
		sidecar.put("table", trimmed);
		return datatablesMarkup(id) + sidecar(id, scriptSafeJson(Json.DEFAULT.write(sidecar)));
	}

	/**
	 * Emits a datatables card from a parsed author catalog: the shared path for the {@code type="datatables"}
	 * object-body sugar and the {@code type="json"} {@code type:'datatables'} envelope (desugar pin). The
	 * sidecar's {@code table} is the lifted SLOT_META (F2), and any author {@code page} rides alongside it.
	 */
	private static String datatablesCardFromCatalog(String id, JsonMap catalog) {
		var sidecar = new JsonMap();
		sidecar.put("contractVersion", SIDECAR_CONTRACT_VERSION);
		sidecar.put("id", id);
		sidecar.put("table", CardEnvelope.liftTable(id, catalog));
		var page = catalog.get("page");
		if (page != null)
			sidecar.put("page", page);
		return datatablesMarkup(id) + sidecar(id, scriptSafeJson(Json.DEFAULT.write(sidecar)));
	}

	/** The datatables card markup: a {@code .jc-card} wrapping the inner wide {@code jc-card-body} mount. */
	private static String datatablesMarkup(String id) {
		return "<div class=\"jc-card\" data-juneau-card=\"datatables\">"
			+ "<div id=\"" + id + "\" class=\"jc-card-body\" data-juneau-layout=\"wide\"></div></div>";
	}

	/**
	 * Emits a {@code type="calendar"} card (Q8b A): a {@code .jc-card} wrapping the inner
	 * {@code data-juneau-calendar} mount the already-shipping {@code juneau-calendar.js} runtime auto-inits,
	 * plus a page-cards sidecar naming the card id. This is JSON5-catalog sugar like {@code datatables} &mdash;
	 * <b>not</b> a new calendar runtime; the {@code "calendar"} toolkit pack ships the runtime and must be listed
	 * explicitly (e.g. {@code toolkit="views,calendar"}). {@code id=} is required (the mount + sidecar need it).
	 *
	 * <p>
	 * A string body is a bare month-data URL; an object body is a JSON5 catalog whose {@code dataUrl} becomes the
	 * runtime's month-GET endpoint.
	 */
	private static String calendarCard(String id, String body) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"calendar\"> requires id=.");
		var trimmed = body.trim();
		if (trimmed.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"calendar\"> requires a data URL or catalog as its body.");
		if (trimmed.startsWith("{"))
			return calendarCardFromCatalog(id, CardEnvelope.parse(trimmed));
		var catalog = new JsonMap();
		catalog.put("dataUrl", trimmed);
		return calendarCardFromCatalog(id, catalog);
	}

	/**
	 * Emits a calendar card from a parsed author catalog: the shared path for the {@code type="calendar"}
	 * object-body sugar and the {@code type="json"} {@code type:'calendar'} envelope (desugar pin). The mount
	 * carries the runtime contract handshake and, when present, the {@code dataUrl} as the month-GET endpoint;
	 * the page-cards sidecar carries the catalog verbatim under {@code calendar} (its own {@code type} key, if
	 * any, is dropped).
	 */
	private static String calendarCardFromCatalog(String id, JsonMap catalog) {
		var dataUrl = catalog.getString("dataUrl");

		var sidecarCatalog = new JsonMap();
		sidecarCatalog.putAll(catalog);
		sidecarCatalog.remove("type");   // the desugar-pin envelope carries type:'calendar'; drop it from the lift
		var sidecar = new JsonMap();
		sidecar.put("contractVersion", SIDECAR_CONTRACT_VERSION);
		sidecar.put("id", id);
		sidecar.put("calendar", sidecarCatalog);
		return calendarMarkup(id, dataUrl) + sidecar(id, scriptSafeJson(Json.DEFAULT.write(sidecar)));
	}

	/**
	 * The calendar card markup: a {@code .jc-card} wrapping the inner {@code data-juneau-calendar} mount carrying
	 * the baked contract-version handshake ({@link org.apache.juneau.rest.server.widgets.WidgetsMixin#CALENDAR_CONTRACT_VERSION})
	 * the runtime fails-loud on, and &mdash; when the catalog supplied one &mdash; the month-GET endpoint.
	 */
	private static String calendarMarkup(String id, String dataUrl) {
		var sb = new StringBuilder("<div class=\"jc-card\" data-juneau-card=\"calendar\">")
			.append("<div id=\"").append(id).append("\" class=\"jc-card-body\" data-juneau-calendar=\"").append(id)
			.append("\" data-juneau-calendar-contract=\"")
			.append(org.apache.juneau.rest.server.widgets.WidgetsMixin.CALENDAR_CONTRACT_VERSION).append('"');
		if (dataUrl != null && ! dataUrl.isEmpty())
			sb.append(" data-juneau-calendar-endpoint=\"").append(attrEscape(dataUrl)).append('"');
		sb.append("></div></div>");
		return sb.toString();
	}

	/** Minimal HTML-attribute escape for an author-supplied value placed in a double-quoted attribute. */
	private static String attrEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** Wraps a strict-JSON payload in the frozen page-cards sidecar {@code <script>} for {@code id}. */
	private static String sidecar(String id, String json) {
		return "<script type=\"application/json\" class=\"juneau-card-sidecar\" data-juneau-card-sidecar=\""
			+ id + "\">" + json + "</script>";
	}

	/**
	 * Script-escapes strict JSON before embedding it in a {@code <script type="application/json">}
	 * element: every {@code <} is replaced with its JSON unicode escape (backslash-u-003c) so a nested
	 * {@code </script>} or {@code <!--} in a {@code source}/{@code content} value cannot break out of
	 * the element (F4).
	 */
	static String scriptSafeJson(String json) {
		return json.replace("<", "\\u003c");
	}

	/** Renders a nested directive body to a string (empty when there is no body). */
	static String capture(TemplateDirectiveBody body) throws TemplateException, IOException {
		if (body == null)
			return "";
		var sw = new StringWriter();
		body.render(sw);
		return sw.toString();
	}

	/**
	 * Emits an html card, deciding whether it carries a page-cards sidecar. A markup-only html card (no
	 * {@code template}/{@code src}/{@code source}/{@code fn}/{@code renderers}/{@code utils}) is sidecar-free
	 * (b01). When the author supplies any of those override attributes, the card carries a strict-JSON
	 * sidecar that copies them verbatim (script-escaped via {@link #scriptSafeJson}, so a {@code source}
	 * holding a literal {@code </script>} cannot break out &mdash; F4); {@code id=} is then required because
	 * the sidecar needs a mount target.
	 */
	private static String htmlCard(Map<String, TemplateModel> p, String id, String inner) throws TemplateModelException {
		var overrides = new JsonMap();
		for (var key : OVERRIDE_ATTRS) {
			var v = FtlAttrLists.scalar(p, key);
			if (! v.isEmpty())
				overrides.put(key, v);
		}
		if (overrides.isEmpty())
			return htmlCard(id, inner);
		if (id.isEmpty())
			throw FtlAttrLists.reject(
				"<@card> with template=/src=/source=/fn=/renderers=/utils= requires id=.");
		var m = new JsonMap();
		m.put("contractVersion", SIDECAR_CONTRACT_VERSION);
		m.put("id", id);
		m.putAll(overrides);
		return htmlCard(id, inner) + sidecar(id, scriptSafeJson(Json.DEFAULT.write(m)));
	}

	/** Wraps trusted markup in a {@code .jc-card} with an optional {@code id}. */
	private static String htmlCard(String id, String inner) {
		var sb = new StringBuilder("<div class=\"jc-card\"");
		if (! id.isEmpty())
			sb.append(" id=\"").append(id).append('"');
		sb.append('>').append(inner).append("</div>");
		return sb.toString();
	}
}
