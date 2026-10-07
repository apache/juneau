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
package org.apache.juneau.petstore.dto;

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Pet_Test extends TestBase {

	@Test void a01_defaultStatusSetsWhenNull() {
		assertBean(new Pet().defaultStatus(PetStatus.SOLD), "status", "SOLD");
	}

	@Test void a02_defaultStatusKeepsExisting() {
		assertBean(new Pet().setStatus(PetStatus.AVAILABLE).defaultStatus(PetStatus.SOLD), "status", "AVAILABLE");
	}

	@Test void a03_defaultSpeciesSetsWhenNull() {
		assertBean(new Pet().defaultSpecies(Species.CAT), "species", "CAT");
	}

	@Test void a04_defaultSpeciesKeepsExisting() {
		assertBean(new Pet().setSpecies(Species.DOG).defaultSpecies(Species.CAT), "species", "DOG");
	}

	@Test void a05_defaultTagsSetsWhenNull() {
		assertList(new Pet().defaultTags(List.of("a", "b")).getTags(), "a", "b");
	}

	@Test void a06_defaultTagsKeepsExisting() {
		assertList(new Pet().setTags(List.of("x")).defaultTags(List.of("a", "b")).getTags(), "x");
	}

	@Test void a07_defaultPhotoSetsWhenNull() {
		assertBean(new Pet().defaultPhoto("p"), "photo", "p");
	}

	@Test void a08_defaultPhotoKeepsExisting() {
		assertBean(new Pet().setPhoto("old").defaultPhoto("p"), "photo", "old");
	}

	@Test void a09_defaultsChain() {
		var p = new Pet().setStatus(PetStatus.SOLD).defaultStatus(PetStatus.AVAILABLE).defaultSpecies(Species.BIRD).defaultPhoto("p");
		assertBean(p, "status,species,photo", "SOLD,BIRD,p");
	}
}
