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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link ConsoleTemplateValidator}: one negative and at least one positive case per rule, plus line/column.
 *
 * @since 10.0.0
 */
class ConsoleTemplateValidator_Test extends TestBase {

	private static List<ConsoleTemplateValidator.Finding> lint(String source) {
		return ConsoleTemplateValidator.create().validateSource("t.ftlh", source);
	}

	private static void assertOne(String source, int line, int column, String rule, String message) {
		var f = lint(source);
		assertEquals(1, f.size(), () -> "findings: " + f);
		assertEquals(new ConsoleTemplateValidator.Finding("t.ftlh", line, column, rule, message), f.get(0));
	}

	private static void assertClean(String source) {
		var f = lint(source);
		assertTrue(f.isEmpty(), () -> "findings: " + f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// unknown-directive
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_unknownDirective() {
		assertOne("<@pgae tab=\"x\"/>", 1, 1, "unknown-directive",
			"<@pgae> is not a console directive, a macro in scope, or allow-listed.");
	}

	@Test void a02_macroInSameTemplate_isKnown() {
		assertClean("<#macro box></#macro><@box/>");
	}

	@Test void a03_allowDirective() {
		var f = ConsoleTemplateValidator.create().allowDirective("pageHero").validateSource("t.ftlh", "<@pageHero/>");
		assertTrue(f.isEmpty(), () -> "findings: " + f);
	}

	@Test void a04_macroFromInclude_isKnown(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("inc.ftlh"), "<#macro shout></#macro>");
		var f = ConsoleTemplateValidator.create().templateRoot(dir).validateSource("t.ftlh", "<#include \"/inc.ftlh\"><@shout/>");
		assertTrue(f.isEmpty(), () -> "findings: " + f);
	}

	@Test void a05_macroFromClasspathInclude_isKnown() {
		assertClean("<#include \"/org/apache/juneau/console/base.ftlh\"><@tag domain=\"status\" value=release/>");
	}

	@Test void a06_importedNamespace_isKnown() {
		assertClean("<#import \"/lib.ftlh\" as lib><@lib.anything/>");
	}

	@Test void a07_chromeTemplateMacros_areInScope(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("chrome.ftlh"), "<#macro hero></#macro><@console><@main/></@console>");
		var f = ConsoleTemplateValidator.create().templateRoot(dir).chromeTemplate("chrome.ftlh").validateSource("t.ftlh", "<@page><@hero/></@page>");
		assertTrue(f.isEmpty(), () -> "findings: " + f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// unknown-attribute
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_unknownAttribute() {
		assertOne("<@navigation><@node id=\"a\" lable=\"A\" href=\"/a\"/></@navigation>", 1, 14, "unknown-attribute",
			"<@node> unknown attribute 'lable'.");
	}

	@Test void b03_underInsideNavigation_isUnknownAttribute() {
		assertOne("<@navigation><@node id=\"a\" label=\"A\" href=\"/a\" under=\"fleet\"/></@navigation>", 1, 14, "unknown-attribute",
			"<@node> unknown attribute 'under'.");
	}

	@Test void b04_underOutsideNavigation_isKnown() {
		assertClean("<@page><@node id=\"a\" label=\"A\" href=\"/a\" under=\"fleet\"/></@page>");
	}

	@Test void b05_underAfterClosedNavigation_isKnown() {
		assertClean("<@console><@navigation><@node id=\"a\" label=\"A\" href=\"/a\"/></@navigation><@node id=\"b\" label=\"B\" href=\"/b\" under=\"a\"/><@main/></@console>");
	}

	@Test void b02_slotAttributes() {
		assertClean("<@console><@head phase=\"before-page-css\"></@head><@footer text=\"x\"/><@main/></@console>");
		assertOne("<@console><@brand phase=\"x\">B</@brand><@main/></@console>", 1, 11, "unknown-attribute",
			"<@brand> unknown attribute 'phase'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// strict-boolean
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_strictBoolean() {
		assertOne("<@navigation><@node id=\"a\" href=\"/a\" visible=\"tru\"/></@navigation>", 1, 14, "strict-boolean",
			"<@node> visible= must be true or false; got 'tru'.");
	}

	@Test void c02_strictBoolean_literalsAndExpressions() {
		assertClean("<@navigation>"
			+ "<@node id=\"a\" href=\"/a\" visible=\"false\"/>"
			+ "<@node id=\"b\" href=\"/b\" visible=false selected=true/>"
			+ "<@node id=\"c\" href=\"/c\" visible=\"${show?c}\"/>"
			+ "<@node id=\"d\" href=\"/d\" visible=flag/>"
			+ "</@navigation>");
	}

	@Test void c03_strictBoolean_consoleChrome() {
		assertOne("<@console chrome=\"yes\"><@main/></@console>", 1, 1, "strict-boolean",
			"<@console> chrome= must be true or false; got 'yes'.");
	}

	@Test void c04_strictBoolean_consoleSchemeToggle() {
		assertOne("<@console schemeToggle=\"yes\"><@main/></@console>", 1, 1, "strict-boolean",
			"<@console> schemeToggle= must be true or false; got 'yes'.");
		assertClean("<@console schemeToggle=true><@main/></@console>");
	}

	@Test void c05_consoleScheme_isKnown() {
		assertClean("<@console scheme=\"dark\" schemeToggle=\"false\"><@main/></@console>");
		assertClean("<@console><@theme name=\"open\"><@token name=\"--jc-link\" value=\"#6cb4ff\" scheme=\"dark\"/></@theme><@main/></@console>");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// dangling-template
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_danglingTemplate() {
		assertOne("<@page><@card id=\"a\" template=\"b\"/></@page>", 1, 8, "dangling-template",
			"<@card> template='b' names no <@card id> or slot in this template.");
	}

	@Test void d02_templateNamesALaterCard() {
		assertClean("<@page><@card id=\"a\" template=\"b\"/><@card id=\"b\">x</@card></@page>");
	}

	@Test void d03_templateNamesASlot() {
		assertClean("<@page><@card id=\"a\" template=\"header.actions\"/></@page>");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// duplicate-id
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_duplicateSiblingNode() {
		assertOne("<@navigation><@node id=\"a\" href=\"/a\"/>\n<@node id=\"a\" href=\"/b\"/></@navigation>", 2, 1, "duplicate-id",
			"<@node id='a'> duplicates a sibling id.");
	}

	@Test void e02_sameIdUnderDifferentParents() {
		assertClean("<@navigation>"
			+ "<@node id=\"x\" label=\"X\"><@node id=\"a\" href=\"/x/a\"/></@node>"
			+ "<@node id=\"y\" label=\"Y\"><@node id=\"a\" href=\"/y/a\"/></@node>"
			+ "</@navigation>");
	}

	@Test void e03_duplicateCard() {
		assertOne("<@page><@card id=\"c\">1</@card><@card id=\"c\">2</@card></@page>", 1, 31, "duplicate-id",
			"<@card id='c'> duplicates an existing card id.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// card-src-and-body, card-reserved-key (C2)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void cb01_cardSrcAndBody() {
		assertOne("<@page><@card id=\"t\" type=\"datatables\" src=\"/rest/t\">{dataUrl:'/rest/t'}</@card></@page>",
			1, 8, "card-src-and-body", "<@card id='t'> has both src='/rest/t' and a body; use exactly one.");
	}

	@Test void cb02_cardSrcAndBody_blankBodyIsClean() {
		assertClean("<@page><@card id=\"t\" type=\"datatables\" src=\"/rest/t\"> </@card></@page>");
		assertClean("<@page><@card id=\"t\" type=\"datatables\" src=\"/rest/t\"/></@page>");
	}

	@Test void cb02b_cardSrcAndBody_chartAllowsSrcWithBody() {
		assertClean("<@page><@card id=\"t\" type=\"chart\" src=\"/rest/t\">{kind:'line'}</@card></@page>");
	}

	@Test void cb03_prebuiltSlotMeta_reportsOnlyViewContractVersionAfterGc1() {
		// inline-slot-meta is gone at GC-1 (E-28 at render); C1's view-contract-version rule stays (Auto-decision 4).
		var f = lint("<@page><@card id=\"t\" type=\"datatables\">{view:{contractVersion:'0',id:'t'}}</@card></@page>");
		assertEquals(List.of("view-contract-version"), f.stream().map(ConsoleTemplateValidator.Finding::rule).toList());
	}

	@Test void cb05_cardReservedKey() {
		assertOne("<@page><@card id=\"t\" type=\"kpi\">{id:'x',value:42}</@card></@page>",
			1, 8, "card-reserved-key", "<@card id='t'> body key 'id' is reserved; set it as an attribute.");
	}

	@Test void cb06_cardReservedKey_visibleWhenIsAdmitted() {
		assertClean("<@page><@card id=\"t\" type=\"kpi\">{visibleWhen:{field:'x',op:'eq',value:1},value:1}</@card></@page>");
	}

	@Test void cb07_literalBodyWithInterpolation_isSkipped() {
		assertClean("<@page><@card id=\"t\" type=\"datatables\">{dataUrl:'${url}'}</@card></@page>");
	}

	@Test void cb08_plainHtmlCard_isClean() {
		assertClean("<@page><@card id=\"t\">{value:1}</@card></@page>");
	}

	@Test void cb09_classAttr_isAcceptedOnCard() {
		assertClean("<@page><@card id=\"t\" class=\"ssc-skills x\"><p>x</p></@card></@page>");
	}

	@Test void cb10_escapedQuoteInSingleQuotedString_isNotAReservedKey() {
		assertClean("<@page><@card id=\"t\" type=\"kpi\">{ label: 'item\\'s', note: 'id: 1, type: 2', value: 1 }</@card></@page>");
	}

	@Test void cb11_escapedQuoteInDoubleQuotedString_isNotAReservedKey() {
		assertClean("<@page><@card id=\"t\" type=\"kpi\">{ \"say\": \"say \\\"hi\\\" id: x\", value: 1 }</@card></@page>");
	}

	@Test void cb12_reservedKeyAfterEscapedQuote_isStillCaught() {
		assertOne("<@page><@card id=\"t\" type=\"kpi\">{ label: 'item\\'s', src: 'x', value: 1 }</@card></@page>",
			1, 8, "card-reserved-key", "<@card id='t'> body key 'src' is reserved; set it as an attribute.");
		assertOne("<@page><@card id=\"t\" type=\"kpi\">{ label: \"say \\\"hi\\\"\", id: 'x' }</@card></@page>",
			1, 8, "card-reserved-key", "<@card id='t'> body key 'id' is reserved; set it as an attribute.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// removed-card-type, removed-global
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_removedCardTypes() {
		for (var t : List.of("js", "json", "calendar"))
			assertOne("<@page><@card id=\"p\" type=\"" + t + "\">x</@card></@page>", 1, 8, "removed-card-type",
				"<@card id='p'> type='" + t + "' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.");
		assertClean("<@page><@card id=\"t\" type=\"datatables\">/rest/t</@card></@page>");
	}

	@Test void f02_removedGlobal() {
		assertOne("<p>\n  ${pageBody}</p>", 2, 5, "removed-global",
			"'pageBody' was removed in 10.0.0; <@page> captures the page into the contract.");
		assertOne("<#if (pageToolkit![])?seq_contains(\"views\")>x</#if>", 1, 7, "removed-global",
			"'pageToolkit' was removed in 10.0.0; use jcHasToolkit(\"views\").");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// main-count, parse-error, comments, line/column
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_mainCount() {
		assertOne("<@console><@main/><@main/></@console>", 1, 1, "main-count",
			"<@console> requires exactly one <@main/>; found '2'.");
		assertOne("<@console></@console>", 1, 1, "main-count",
			"<@console> requires exactly one <@main/>; found '0'.");
		assertClean("<@console><@main/></@console>");
		assertClean("<p>no console here</p>");
	}

	@Test void g02_parseError() {
		var f = lint("<p>\n<#if></#if>");
		assertBean(f, "size,0{rule,line}", "1,{parse-error,2}");
	}

	@Test void g03_commentsAreIgnored() {
		assertClean("<#-- <@pgae/> ${pageBody} <@card id=\"x\" type=\"js\"/> -->");
	}

	@Test void g04_lineAndColumn() {
		assertOne("\n\n  <@pgae/>", 3, 3, "unknown-directive",
			"<@pgae> is not a console directive, a macro in scope, or allow-listed.");
	}

	@Test void g05_findingToString() {
		assertEquals("t.ftlh:3:3: [unknown-directive] m", new ConsoleTemplateValidator.Finding("t.ftlh", 3, 3, "unknown-directive", "m").toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// hardcoded-asset-url
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_hardcodedJuneauAssets_flagged() {
		var f = lint("""
			<link rel="stylesheet" href="/rest/home/juneau-views.css">
			<script src='/juneau-console/juneau-console.js?v=1'></script>
			<link href="/juneau-console/chrome.css">
			<link href="themes/juneau-theme-red.css">""");
		assertBean(f, "size", "4");
		assertBeans(f, "line,rule", "1,hardcoded-asset-url", "2,hardcoded-asset-url", "3,hardcoded-asset-url", "4,hardcoded-asset-url");
		assertTrue(f.get(0).message().contains("'/rest/home/juneau-views.css'"), f.get(0).message());
	}

	@Test void i02_helperGeneratedAndAdopterAssets_clean() {
		assertClean("<link rel=\"stylesheet\" href=\"${viewsCssUrl}\">");
		assertClean("<script src=\"${viewAssetUrl(req, '/juneau-views.js')}\"></script>");
		assertClean("<link href=\"/css/chrome.css\"><script src=\"/js/renderers.js\"></script>");
		assertClean("<a href=\"/docs/juneau-views.html\">docs</a>");
		assertClean("<script src=\"${assetUrl('/js/juneau-app.js')}\"></script>");
	}

	@Test void i03_commentedOut_isIgnored() {
		assertClean("<#-- <script src=\"/juneau-views.js\"></script> -->");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// roots
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_validateAll_walksTheRoot(@TempDir Path dir) throws Exception {
		Files.createDirectories(dir.resolve("sub"));
		Files.writeString(dir.resolve("a.ftlh"), "<@pgae/>");
		Files.writeString(dir.resolve("sub/b.ftlh"), "${pageCss}");
		Files.writeString(dir.resolve("ignored.txt"), "<@pgae/>");
		var f = ConsoleTemplateValidator.create().templateRoot(dir).validateAll();
		assertBeans(f, "template", "a.ftlh", "sub/b.ftlh");
	}

	@Test void h02_validateAll_needsATemplateRoot() {
		var v = ConsoleTemplateValidator.create().classpathRoot("/templates/");
		var e = assertThrows(IllegalStateException.class, v::validateAll);
		assertEquals("validateAll() requires templateRoot(Path); a classpath root cannot be listed.", e.getMessage());
	}

	@Test void h03_validate_byClasspathName() {
		var f = ConsoleTemplateValidator.create().classpathRoot("/templates/").validate("c1/console-min.ftlh");
		assertTrue(f.isEmpty(), () -> "findings: " + f);
	}

	@Test void h04_validate_missingTemplate() {
		var validator = ConsoleTemplateValidator.create().classpathRoot("/templates/");
		var e = assertThrows(IllegalArgumentException.class, () -> validator.validate("nope.ftlh"));
		assertEquals("Template 'nope.ftlh' not found under templateRoot or classpathRoot.", e.getMessage());
	}

	@Test void z01_factsDirective_rejectsAnyAttribute() {
		assertOne("<@console><@facts foo=1>{}</@facts><@main/></@console>", 1, 11, "unknown-attribute", "<@facts> unknown attribute 'foo'.");
	}

	@Test void w01_chartCard_absentWebJar_isMissingWebjar() {
		var f = ConsoleTemplateValidator.create().webJarProbe(a -> { throw new IllegalStateException("x"); })
			.validateSource("t.ftlh", "<@card type=\"chart\" id=\"c\">{kind:'line'}</@card>");
		assertSize(1, f);
		assertString("missing-webjar", f.get(0).rule());
		assertContains("E-70: chart cards need the ECharts WebJar", f.get(0).message());
	}

	@Test void w02_chartCard_presentWebJar_clean() {
		assertEmpty(ConsoleTemplateValidator.create().validateSource("t.ftlh",
			"<@card type=\"chart\" id=\"c\">{kind:'line',src:'/x',x:{field:'d'},series:['v']}</@card>")
			.stream().filter(x -> x.rule().equals("missing-webjar")).toList());
	}

	private static final String CHART_CARD = "<@card type=\"chart\" id=\"c\">{kind:'line'}</@card>";

	private static List<ConsoleTemplateValidator.Finding> missingWebJar(List<ConsoleTemplateValidator.Finding> f) {
		return f.stream().filter(x -> x.rule().equals("missing-webjar")).toList();
	}

	@Test void w03_chartCard_echartsProvided_noMissingWebjar() {
		var f = ConsoleTemplateValidator.create().providedPacks("echarts")
			.webJarProbe(a -> { throw new IllegalStateException("x"); })
			.validateSource("t.ftlh", CHART_CARD);
		assertEmpty(missingWebJar(f));
	}

	@Test void w04_chartCard_echartsOverridden_noMissingWebjar() {
		var cdn = ToolkitPack.create("echarts").kind(ToolkitPack.Kind.VENDOR)
			.resolver((req, path) -> path).js("https://cdn.example.com/echarts.min.js").build();
		var f = ConsoleTemplateValidator.create().registerToolkitPack(cdn)
			.webJarProbe(a -> { throw new IllegalStateException("x"); })
			.validateSource("t.ftlh", CHART_CARD);
		assertEmpty(missingWebJar(f));
	}

	@Test void w05_twoChartCards_probesOnce() {
		var calls = new java.util.concurrent.atomic.AtomicInteger();
		var v = ConsoleTemplateValidator.create().webJarProbe(a -> { calls.incrementAndGet(); throw new IllegalStateException("x"); });
		var f = v.validateSource("t.ftlh", CHART_CARD + "<@card type=\"chart\" id=\"d\">{kind:'line'}</@card>");
		assertSize(2, missingWebJar(f));
		v.validateSource("u.ftlh", CHART_CARD);
		assertEquals(1, calls.get());
	}

	@Test void w06_nullProbe_rejected() {
		var target = ConsoleTemplateValidator.create();
		assertThrows(NullPointerException.class, () -> target.webJarProbe(null));
	}

	@Test void w07_cacheResets_afterProvidedPacks_andRegisterToolkitPack() {
		var v = ConsoleTemplateValidator.create().webJarProbe(a -> { throw new IllegalStateException("x"); });
		assertSize(1, missingWebJar(v.validateSource("t.ftlh", CHART_CARD)));
		v.providedPacks("echarts");
		assertEmpty(missingWebJar(v.validateSource("t.ftlh", CHART_CARD)));

		var v2 = ConsoleTemplateValidator.create().webJarProbe(a -> { throw new IllegalStateException("x"); });
		assertSize(1, missingWebJar(v2.validateSource("t.ftlh", CHART_CARD)));
		v2.registerToolkitPack(ToolkitPack.create("echarts").kind(ToolkitPack.Kind.VENDOR)
			.resolver((req, path) -> path).js("https://cdn.example.com/echarts.min.js").build());
		assertEmpty(missingWebJar(v2.validateSource("t.ftlh", CHART_CARD)));
	}

	@Test void w08_chartsPackReplaced_withoutEchartsDependency_noFinding() {
		var plain = ToolkitPack.create("charts").kind(ToolkitPack.Kind.RUNTIME)
			.resolver((req, path) -> path).js("/my-charts.js").build();
		var f = ConsoleTemplateValidator.create().registerToolkitPack(plain)
			.webJarProbe(a -> { throw new IllegalStateException("x"); })
			.validateSource("t.ftlh", CHART_CARD);
		assertEmpty(missingWebJar(f));
	}

	@Test void w09_brokenPackGraph_isAFinding_notACrash() {
		var bad = ToolkitPack.create("charts").kind(ToolkitPack.Kind.RUNTIME)
			.resolver((req, path) -> path).js("/my-charts.js").dependsOn("nope").build();
		var f = ConsoleTemplateValidator.create().registerToolkitPack(bad).validateSource("t.ftlh", CHART_CARD);
		assertSize(1, f);
		assertString("invalid-pack-graph", f.get(0).rule());
		assertContains("nope", f.get(0).message());
	}

	@Test void w10_providedPacks_unknownName_rejected() {
		var v = ConsoleTemplateValidator.create();
		var e = assertThrows(IllegalArgumentException.class, () -> v.providedPacks("nope"));
		assertContains("Unknown toolkit pack 'nope'", e.getMessage());
	}
}
