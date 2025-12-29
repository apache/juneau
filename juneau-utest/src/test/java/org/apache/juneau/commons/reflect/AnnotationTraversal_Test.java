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
package org.apache.juneau.commons.reflect;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AnnotationTraversal_Test extends TestBase {

	//====================================================================================================
	// getOrder()
	//====================================================================================================
	@Test
	void a001_getOrder() {
		// Test all enum values and their order values
		assertEquals(10, AnnotationTraversal.SELF.getOrder());
		assertEquals(20, AnnotationTraversal.PARENTS.getOrder());
		assertEquals(20, AnnotationTraversal.MATCHING_METHODS.getOrder());
		assertEquals(20, AnnotationTraversal.MATCHING_PARAMETERS.getOrder());
		assertEquals(30, AnnotationTraversal.RETURN_TYPE.getOrder());
		assertEquals(30, AnnotationTraversal.PARAMETER_TYPE.getOrder());
		assertEquals(35, AnnotationTraversal.DECLARING_CLASS.getOrder());
		assertEquals(40, AnnotationTraversal.PACKAGE.getOrder());
		assertEquals(999, AnnotationTraversal.REVERSE.getOrder());
		
		// Verify order values are as expected (lower values = higher precedence)
		assertTrue(AnnotationTraversal.SELF.getOrder() < AnnotationTraversal.PARENTS.getOrder());
		assertTrue(AnnotationTraversal.PARENTS.getOrder() < AnnotationTraversal.RETURN_TYPE.getOrder());
		assertTrue(AnnotationTraversal.RETURN_TYPE.getOrder() < AnnotationTraversal.DECLARING_CLASS.getOrder());
		assertTrue(AnnotationTraversal.DECLARING_CLASS.getOrder() < AnnotationTraversal.PACKAGE.getOrder());
		assertTrue(AnnotationTraversal.PACKAGE.getOrder() < AnnotationTraversal.REVERSE.getOrder());
	}
}

