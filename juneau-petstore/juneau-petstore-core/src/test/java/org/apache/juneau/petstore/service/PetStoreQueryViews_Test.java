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
package org.apache.juneau.petstore.service;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.petstore.dto.*;
import org.junit.jupiter.api.*;

class PetStoreQueryViews_Test extends TestBase {

	@Test void a01_petsByStatus() {
		var s = new PetStore();
		try (var session = s.queryPets()) {
			var page = session.find(BeanQuery.create().eq("status", "SOLD").sort("id").build());
			assertNotEmpty(page.rows());
			page.rows().forEach(p -> assertBean(p, "status", "SOLD"));
		}
	}

	@Test void a02_petColumnsExcludeTagsAndPhoto() {
		assertList(PetStore.PET_QUERY.columns().stream().filter(c -> eqa(c, "tags", "photo")).toList());
	}

	@Test void a03_userColumnsExcludePassword() {
		assertList(PetStore.USER_QUERY.columns().stream().filter("password"::equals).toList());
	}

	@Test void a04_ordersById() {
		try (var session = new PetStore().queryOrders()) {
			assertBeans(session.find(BeanQuery.create().eq("id", 101).build()).rows(), "id,petId", "101,101");
		}
	}

	@Test void a05_auditAndPendingViews() {
		var s = new PetStore();
		s.createPet(new Pet().setName("Q"), "console:alice");
		s.pendingChanges().stage(1, "name", "Z", "alice");
		try (var a = s.queryAudit(); var p = s.queryPendingChanges()) {
			assertBeans(a.find(BeanQuery.create().eq("actor", "console:alice").build()).rows(), "detail", "Q");
			assertBeans(p.find(BeanQuery.create().eq("state", "PENDING").build()).rows(), "petId", "1");
		}
	}

	@Test void a06_sessionIsSnapshot() {
		var s = new PetStore();
		try (var session = s.queryPets()) {
			s.createPet(new Pet().setName("Late"));
			assertEmpty(session.find(BeanQuery.create().eq("name", "Late").build()).rows());
		}
	}
}
