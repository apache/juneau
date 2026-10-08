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

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Every console page renders a valid {@code #juneau-page} contract with the expected nav, cards and order.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstorePages_ContractTest extends TestBase {

	record Page(String id, String path, Consumer<PageContractAssert> expect) {
		@Override public String toString() { return id; }
	}

	static final List<Page> PAGES = List.of(
		new Page("store", "/console/store", p -> p.hasActiveNav("store").hasCard("welcome", "html")),
		new Page("jobs", "/console/ops/jobs", p -> p.hasActiveNav("ops", "jobs").hasCard("jobs", "datatables")
			.hasCard("groom-output", "console-output").hasCard("groom-file", "console-output").hasCard("groom-runs", "datatables")),
		new Page("audit", "/console/ops/audit", p -> p.hasActiveNav("ops", "audit").hasCard("audit", "datatables")),
		new Page("flavors-html", "/console/dev/flavors/html", p -> p.hasActiveNav("dev", "flavors", "html").hasCardOrder("caption", "flavor")),
		new Page("flavors-freemarker", "/console/dev/flavors/freemarker", p -> p.hasActiveNav("dev", "flavors", "freemarker").hasCardOrder("caption", "flavor")),
		new Page("flavors-mustache", "/console/dev/flavors/mustache", p -> p.hasActiveNav("dev", "flavors", "mustache").hasCardOrder("caption", "flavor")),
		new Page("flavors-react", "/console/dev/flavors/react", p -> p.hasActiveNav("dev", "flavors", "react").hasCardOrder("caption", "flavor")),
		new Page("secure", "/console/dev/secure", p -> p.hasActiveNav("dev", "secure").hasCardOrder("about", "try")),
		new Page("about", "/console/about", p -> p.hasActiveNav("about").hasCardOrder("overview", "links", "beans")),
		new Page("themes", "/console/dev/themes", p -> p.hasActiveNav("dev", "themes").hasCardOrder("swatches", "picker", "authoring").hasTheme("open"))
		// Later tasks append rows here.
	);

	/** Page paths for the vocabulary and asset-order gates. */
	static final List<String> PAGE_PATHS = PAGES.stream().map(Page::path).toList();

	static Stream<Page> pages() {
		return PAGES.stream();
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("pages")
	void a01_ftl(Page p) throws Exception {
		var a = assertPage(page(client(), p.path())).isValid().hasContractVersion("1");
		p.expect().accept(a);
	}
}
