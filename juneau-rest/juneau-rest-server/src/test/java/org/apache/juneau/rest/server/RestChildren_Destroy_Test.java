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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.jupiter.api.*;

/**
 * Tests {@link RestChildren#destroy()} when a child's resource is no longer available from its supplier.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // RestContext instances are owned and destroyed by the test via RestChildren.destroy().
})
class RestChildren_Destroy_Test extends org.apache.juneau.TestBase {

	static final AtomicInteger DESTROYED = new AtomicInteger();

	@Rest(path="/gone")
	public static class Gone {
		@RestDestroy public void destroy() { DESTROYED.addAndGet(100); }
	}

	@Rest(path="/alive")
	public static class Alive {
		@RestDestroy public void destroy() { DESTROYED.incrementAndGet(); }
	}

	private static RestContext ctx(Class<?> type, Supplier<?> supplier) throws Exception {
		return new RestContext(new RestContext.Args(type, null, null, supplier, null, null, null, null, null, null));
	}

	// A child whose bean was removed by its container (e.g. a closed Spring context) must be skipped, not abort the parent's destroy.
	@Test void a01_destroy_childResourceGone_otherChildrenStillDestroyed() throws Exception {
		DESTROYED.set(0);
		var present = new AtomicBoolean(true);
		var gone = ctx(Gone.class, () -> {
			if (! present.get())
				throw new NoSuchElementException("bean removed");
			return new Gone();
		});
		var alive = ctx(Alive.class, Alive::new);
		var children = RestChildren.create(null, null, null).add(gone).add(alive).build();

		present.set(false);
		assertDoesNotThrow(children::destroy);

		assertEquals(1, DESTROYED.get());
	}
}
