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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.BasicRestObject;
import org.junit.jupiter.api.*;

class NoInherit_Test extends TestBase {

	private static RestContext restContext(Class<? extends BasicRestObject> c) throws Exception {
		var o = c.getDeclaredConstructor().newInstance();
		
		// Check if class has a parent REST resource (not BasicRestObject)
		RestContext parentContext = null;
		var superClass = c.getSuperclass();
		if (superClass != null && superClass != BasicRestObject.class && BasicRestObject.class.isAssignableFrom(superClass)) {
			@SuppressWarnings("unchecked")
			var parentClass = (Class<? extends BasicRestObject>) superClass;
			parentContext = restContext(parentClass);
		}
		
		return RestContext.create(c, parentContext, null).init(() -> o).build().postInit().postInitChildFirst();
	}

	@Rest(allowedSerializerOptions = "parentSer")
	public static class ParentSer extends BasicRestObject {}

	@Rest(allowedSerializerOptions = "childSer", noInherit = "allowedSerializerOptions")
	public static class ChildSer extends ParentSer {}

	@Test
	void a01_classNoInherit_skipsParentSerializerAllowlist() throws Exception {
		var ctx = restContext(ChildSer.class);
		var keys = ctx.getAllowedSerializerOptions();

		assertTrue(keys.contains("childSer"));
		assertFalse(keys.contains("parentSer"));
	}

	@Rest(allowedSerializerOptions = "pBoth")
	public static class ParentBoth extends BasicRestObject {}

	@Rest(allowedSerializerOptions = "cBoth")
	public static class ChildBoth extends ParentBoth {}

	@Test
	void a02_withoutNoInherit_mergesParentSerializerAllowlist() throws Exception {
		var ctx = restContext(ChildBoth.class);
		var keys = ctx.getAllowedSerializerOptions();

		assertTrue(keys.contains("pBoth"));
		assertTrue(keys.contains("cBoth"));
	}

	public static class ParentM extends BasicRestObject {
		@RestGet(allowedSerializerOptions = "parentM")
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	public static class ChildM extends ParentM {
		@RestGet(allowedSerializerOptions = "childM", noInherit = "allowedSerializerOptions")
		@Override
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	@Test
	void a03_methodNoInherit_stillInheritsParentMethodSerializerAllowlist() throws Exception {
		var ctx = restContext(ChildM.class);
		var op = ctx.getRestOperations().getOpContexts().stream()
			.filter(o -> ChildM.class.equals(o.getJavaMethod().getDeclaringClass()) && "get".equals(o.getJavaMethod().getName()))
			.findFirst()
			.orElseThrow();
		var keys = op.getAllowedSerializerOptions();

		// Method annotations ALWAYS inherit from parent methods, regardless of noInherit
		// noInherit only blocks inheritance from the REST class context
		assertTrue(keys.contains("childM"));
		assertTrue(keys.contains("parentM"));
	}
	
	// Test case for aggregated noInherit: parent has noInherit="prop1", child has noInherit="prop2"
	// The aggregated noInherit should be {"prop1", "prop2"}, but allowedSerializerOptions should
	// include values from both parent and child since neither blocks allowedSerializerOptions
	public static class ParentAggregated extends BasicRestObject {
		@RestGet(allowedSerializerOptions = "parentOpt", noInherit = "prop1")
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	public static class ChildAggregated extends ParentAggregated {
		@RestGet(allowedSerializerOptions = "childOpt", noInherit = "prop2")
		@Override
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	@Test
	void a04_aggregatedNoInherit_includesBothParentAndChild() throws Exception {
		var ctx = restContext(ChildAggregated.class);
		var op = ctx.getRestOperations().getOpContexts().stream()
			.filter(o -> ChildAggregated.class.equals(o.getJavaMethod().getDeclaringClass()) && "get".equals(o.getJavaMethod().getName()))
			.findFirst()
			.orElseThrow();
		var keys = op.getAllowedSerializerOptions();

		// Both parent and child options should be present since neither has allowedSerializerOptions in noInherit
		assertTrue(keys.contains("childOpt"));
		assertTrue(keys.contains("parentOpt"));
	}

	// Test that noInherit blocks REST class context inheritance but NOT parent method inheritance
	@Rest(allowedSerializerOptions = "classLevel")
	public static class ParentWithClassLevel extends BasicRestObject {
		@RestGet(allowedSerializerOptions = "parentMethod")
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	public static class ChildBlocksClassInheritance extends ParentWithClassLevel {
		@RestGet(allowedSerializerOptions = "childMethod", noInherit = "allowedSerializerOptions")
		@Override
		public void get() {
			// Intentionally empty - method only used for annotation metadata testing
		}
	}

	@Test
	void a05_methodNoInherit_blocksClassLevelButNotParentMethod() throws Exception {
		var ctx = restContext(ChildBlocksClassInheritance.class);
		var op = ctx.getRestOperations().getOpContexts().stream()
			.filter(o -> ChildBlocksClassInheritance.class.equals(o.getJavaMethod().getDeclaringClass()) && "get".equals(o.getJavaMethod().getName()))
			.findFirst()
			.orElseThrow();
		var keys = op.getAllowedSerializerOptions();

		// Should include method-level keys from both child and parent methods
		assertTrue(keys.contains("childMethod"));
		assertTrue(keys.contains("parentMethod"));
		
		// Should NOT include class-level key because of noInherit
		assertFalse(keys.contains("classLevel"));
	}
}
