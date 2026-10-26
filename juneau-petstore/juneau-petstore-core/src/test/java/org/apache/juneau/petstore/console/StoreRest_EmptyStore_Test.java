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

import static org.apache.juneau.console.test.PageContractAssert.*;
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.test.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * The Store page over a store with no pets and no orders: the inventory strip item falls back to a plain tile, since a meter needs a maximum above zero.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class StoreRest_EmptyStore_Test extends TestBase {

	/** A host over a store with no pets and no orders; its own class, so its REST context is not shared. */
	@Rest(children={PetStoreResource.class, PetstoreConsoleResource.class})
	public static class EmptyHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		/** @return An empty store. */
		@Bean public PetStore petStore() {
			var store = new PetStore(PetstoreSeed.DEFAULT_CLOCK); // the constructor loads the bundled seed, so empty it
			for (var o : List.copyOf(store.getOrders()))
				store.deleteOrder(o.getId());
			for (var p : List.copyOf(store.getPets()))
				store.deletePet(p.getId());
			return store;
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(EmptyHost.class).noTrace().ignoreErrors().build();
	}

	private static PageContractAssert render(MockRestClient c, String path) throws Exception {
		return assertPage(c.get(path).accept("text/html").run().assertStatus(200).getContent().asString()).isValid();
	}

	private static List<?> items(PageContractAssert page) {
		return (List<?>)((Map<?,?>)((Map<?,?>)page.card("stats").get("table")).get("quickStats")).get("items");
	}

	private static void assertEmptyStrip(List<?> items) {
		var orders = (Map<?,?>)items.get(3);
		assertEquals("orders", orders.get("id"));
		assertEquals("segmented", orders.get("type"));
		var segs = (List<?>)orders.get("segments");
		assertSize(3, segs);
		var labels = new ArrayList<String>();
		for (var o : segs) {
			var seg = (Map<?,?>)o;
			labels.add(seg.get("label") + "=" + seg.get("count"));
		}
		assertEquals(List.of("Placed=0", "Approved=0", "Delivered=0"), labels);
		var inv = (Map<?,?>)items.get(4);
		assertEquals("inventory", inv.get("id"));
		assertFalse(inv.containsKey("type")); // a plain tile, not a meter
		assertEquals("0 of 0", inv.get("value"));
	}

	@Test void a00_hostStoreIsReallyEmpty() throws Exception {
		var c = client();
		assertSize(0, new JsonList(c.get("/petstore/pets").accept("application/json").run().assertStatus(200).getContent().asString()));
		assertSize(0, new JsonList(c.get("/petstore/orders").accept("application/json").run().assertStatus(200).getContent().asString()));
	}

	@Test void a01_emptyStore_ftlRendersInventoryAsTile() throws Exception {
		assertEmptyStrip(items(render(client(), "/console/store")));
	}

	@Test void a02_emptyStore_builderEqualsFtl() throws Exception {
		var c = client();
		var ftl = render(c, "/console/store");
		var builder = render(c, "/console/store?builder");
		assertEmptyStrip(items(builder));
		assertTwins(ftl.contract(), ftl.templates(), builder.contract(), builder.templates());
	}
}
