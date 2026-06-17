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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.marshall.parquet.ParquetSchemaElement.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ParquetSchemaElement#isLeaf()} branch combinations.
 */
class ParquetSchemaElement_Test extends TestBase {

	private static ParquetSchemaElement el(Integer type, Integer numChildren) {
		return new ParquetSchemaElement("n", type, null, OPTIONAL, numChildren, null, null, null, null, "n");
	}

	@Test
	void a01_leafWhenTypePresentAndNoChildren() {
		assertTrue(el(TYPE_INT32, null).isLeaf());   // type!=null, numChildren==null
		assertTrue(el(TYPE_INT32, 0).isLeaf());      // type!=null, numChildren==0
	}

	@Test
	void a02_notLeafWhenNoType() {
		assertFalse(el(null, null).isLeaf());        // type==null short-circuits
		assertFalse(el(null, 2).isLeaf());           // type==null short-circuits
	}

	@Test
	void a03_notLeafWhenHasChildren() {
		assertFalse(el(TYPE_INT32, 2).isLeaf());     // type!=null but numChildren>0 (group node)
	}
}
