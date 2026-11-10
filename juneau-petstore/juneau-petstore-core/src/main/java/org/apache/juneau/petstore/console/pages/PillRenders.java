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
package org.apache.juneau.petstore.console.pages;

import org.apache.juneau.console.views.*;
import org.apache.juneau.console.widgets.*;

/**
 * One pill renderer per enum column, so a value has the same colour on every page.
 */
final class PillRenders {

	/** Pet status: AVAILABLE / PENDING / SOLD. */
	static Render petStatus() {
		return Render.pill()
			.tone("AVAILABLE", StatusTone.SUCCESS)
			.tone("PENDING", StatusTone.WARNING)
			.tone("SOLD", StatusTone.NEUTRAL);
	}

	/** Order status: PLACED / APPROVED / DELIVERED (the colours the Store stats strip already uses). */
	static Render orderStatus() {
		return Render.pill()
			.tone("PLACED", StatusTone.INFO)
			.tone("APPROVED", StatusTone.WARNING)
			.tone("DELIVERED", StatusTone.SUCCESS);
	}

	/** User status: ACTIVE / INACTIVE. */
	static Render userStatus() {
		return Render.pill()
			.tone("ACTIVE", StatusTone.SUCCESS)
			.tone("INACTIVE", StatusTone.NEUTRAL);
	}

	/** Pending change state: PENDING / APPLIED / FAILED. */
	static Render changeState() {
		return Render.pill()
			.tone("PENDING", StatusTone.WARNING)
			.tone("APPLIED", StatusTone.SUCCESS)
			.tone("FAILED", StatusTone.ERROR);
	}

	/** Ops job state: RUNNING / DONE / EXPIRED. */
	static Render jobState() {
		return Render.pill()
			.tone("RUNNING", StatusTone.INFO)
			.tone("DONE", StatusTone.SUCCESS)
			.tone("EXPIRED", StatusTone.NEUTRAL);
	}

	/** Audit action: CREATE / UPDATE / DELETE, plus the change-discard and restock-job actions the store records. */
	static Render auditAction() {
		return Render.pill()
			.tone("CREATE", StatusTone.SUCCESS)
			.tone("UPDATE", StatusTone.INFO)
			.tone("DELETE", StatusTone.ERROR)
			.tone("DISCARD", StatusTone.WARNING)
			.tone("RESTOCK", StatusTone.INFO);
	}

	/** Species: categorical, pinned to collision-free palette slots (values are case-sensitive). */
	static Render species() {
		return Render.pill()
			.categorical()
			.category("DOG", 1)
			.category("CAT", 2)
			.category("BIRD", 4)
			.category("FISH", 6)
			.category("SNAKE", 7)
			.category("RABBIT", 9)
			.category("MOUSE", 10);
	}

	/** Audit entity: categorical, pinned. */
	static Render entity() {
		return Render.pill()
			.categorical()
			.category("Pet", 1)
			.category("Order", 2)
			.category("User", 4)
			.category("Change", 6)
			.category("Job", 7);
	}

	private PillRenders() {}
}
