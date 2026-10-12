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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Every console page's FTL render and its builder twin ({@code ?builder}) produce the same {@code #juneau-page}
 * contract and the same card markup.
 *
 * <p>
 * The comparison is {@link PageContractAssert#assertTwins}: key-sorted JSON (list order is kept, because card, nav and
 * option order is part of the page), then the {@code <template>} ids and their stripped markup.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreTwins_Test extends TestBase {

	/**
	 * A page whose twin cannot equal its FTL yet.
	 *
	 * @param path The console path.
	 * @param reason Why, for the report and the disabled test.
	 * @param dropped What only one side renders, removed from both contracts before {@link #a02_twinsEqualOutsideGaps}
	 * 	compares: a card id, or {@code cardId/key/...} for one key inside a card.
	 */
	record Gap(String path, String reason, List<String> dropped) {
		@Override public String toString() { return path; }
	}

	static final String HTML_SRC = "PageSpec cannot build an html card with src=: PageSpec.html rejects src and PageSpec.card rejects type 'html'.";

	static final List<Gap> GAPS = List.of(
		new Gap("/console/store", HTML_SRC, List.of("hours")),
		new Gap("/console/dev/flavors/html", HTML_SRC, List.of("flavor")),
		new Gap("/console/dev/flavors/freemarker", HTML_SRC, List.of("flavor")),
		new Gap("/console/dev/flavors/mustache", HTML_SRC, List.of("flavor"))
	);

	static Stream<String> twins() {
		var gapped = GAPS.stream().map(Gap::path).toList();
		return PetstorePages_ContractTest.PAGE_PATHS.stream().filter(x -> ! gapped.contains(x));
	}

	static Stream<Gap> gaps() {
		return GAPS.stream();
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("twins")
	void a01_ftlEqualsBuilder(String path) throws Exception {
		var c = client();
		assertSameContract(path, ftl(c, path), builder(c, path));
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("gaps")
	void a02_twinsEqualOutsideGaps(Gap g) throws Exception {
		var c = client();
		var ftl = ftl(c, g.path());
		var contract = ftl.contract();
		var templates = new TreeMap<>(ftl.templates());
		var builder = builder(c, g.path());
		var builderContract = builder.contract();
		var builderTemplates = new TreeMap<>(builder.templates());
		for (var d : g.dropped()) {
			// | not ||: drop from both sides.
			var dropped = drop(contract, templates, d) | drop(builderContract, builderTemplates, d);
			assertTrue(dropped, () -> "nothing to drop at '" + d + "' on either side of '" + g.path() + "'");
		}
		assertTwinsAt(g.path(), contract, templates, builderContract, builderTemplates);
	}

	@Test void a03_mixedEqualsFtl() throws Exception {
		var c = client();
		var ftl = ftl(c, "/console/pets");
		assertSameContract("/console/pets?mixed", ftl, ftl(c, "/console/pets?mixed"));
		assertSameContract("/console/pets?builder&mixed", ftl, builder(c, "/console/pets?mixed"));
	}

	@Test void a04_ftlPathIsNotTheBuilder() throws Exception {
		try (var c = client()) {
			assertFalse(c.get("/console/pets").accept("text/html").run().getHeader("X-Petstore-Page").isPresent());
		}
	}

	@Disabled(HTML_SRC) @Test void b01_store() throws Exception { assertFull("/console/store"); }
	@Disabled(HTML_SRC) @Test void b02_flavorsHtml() throws Exception { assertFull("/console/dev/flavors/html"); }
	@Disabled(HTML_SRC) @Test void b03_flavorsFreemarker() throws Exception { assertFull("/console/dev/flavors/freemarker"); }
	@Disabled(HTML_SRC) @Test void b04_flavorsMustache() throws Exception { assertFull("/console/dev/flavors/mustache"); }
	@Test void b05_petOrders() throws Exception { assertFull("/console/pets/1/orders"); }
	@Test void b06_jobs() throws Exception { assertFull("/console/ops/jobs"); }

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static void assertFull(String path) throws Exception {
		var c = client();
		assertSameContract(path, ftl(c, path), builder(c, path));
	}

	private static PageContractAssert ftl(MockRestClient c, String path) throws Exception {
		return assertPage(page(c, path)).isValid();
	}

	/** Renders the builder twin, asserting it really came from the builder. */
	private static PageContractAssert builder(MockRestClient c, String path) throws Exception {
		var p = path + (path.contains("?") ? "&" : "?") + "builder";
		var rsp = c.get(p).accept("text/html").run().assertStatus(200);
		assertEquals("builder", rsp.getHeader("X-Petstore-Page").orElse(null), () -> p + " did not render through its builder.");
		return assertPage(rsp.getContent().asString()).isValid();
	}

	private static void assertSameContract(String path, PageContractAssert ftl, PageContractAssert builder) {
		assertTwinsAt(path, ftl.contract(), ftl.templates(), builder.contract(), builder.templates());
	}

	private static void assertTwinsAt(String path, JsonMap ftl, Map<String,String> ftlTemplates, JsonMap builder, Map<String,String> builderTemplates) {
		try {
			PageContractAssert.assertTwins(ftl, ftlTemplates, builder, builderTemplates);
		} catch (AssertionError e) {
			throw new AssertionError("'" + path + "' FTL and builder: " + e.getMessage(), e);
		}
	}

	/**
	 * Removes a card ({@code id}) or one key inside it ({@code id/key/...}) from the contract, and the card's markup.
	 *
	 * @return Whether there was anything to remove.
	 */
	private static boolean drop(JsonMap contract, Map<String,String> templates, String spec) {
		var parts = spec.split("/");
		var cards = contract.getList("cards");
		for (var i = 0; i < cards.size(); i++) {
			var card = cards.getMap(i);
			if (! parts[0].equals(card.getString("id")))
				continue;
			if (parts.length == 1) {
				cards.remove(i);
				templates.remove(parts[0]);
				return true;
			}
			var m = card;
			for (var j = 1; j < parts.length - 1 && m != null; j++)
				m = m.getMap(parts[j]);
			return m != null && m.remove(parts[parts.length - 1]) != null;
		}
		return false;
	}
}
