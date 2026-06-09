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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests various error conditions when defining beans.
 */
class BeanMapErrors_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// @MarshalledProp(name) on method not in @Marshalled(properties)
	// JUNEAU-248: Shouldn't be found in keySet()/entrySet()/containsKey() but should be accessible via get()/put()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void beanPropertyMethodNotInBeanProperties() {
		var bc = MarshallingContext.DEFAULT;

		var bm = bc.newBeanMap(A1.class);
		assertFalse(bm.containsKey("f2"));  // JUNEAU-248: Now consistent with keySet()
		assertEquals(-1, bm.get("f2"));      // But get() still works
		bm.put("f2", -2);                    // And put() still works
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@BeanType(p="f1")
	public static class A1 {
		public int f1;

		private int f2 = -1;
		@BeanProp("f2") public int f2() { return f2; }
		public void setF2(int v) { f2 = v; }
	}

	@Test void beanPropertyMethodNotInBeanProperties_usingConfig() {
		var bc = MarshallingContext.create().applyAnnotations(B1Config.class).build();

		var bm = bc.newBeanMap(B1.class);
		assertFalse(bm.containsKey("f2"));  // JUNEAU-248: Now consistent with keySet()
		assertEquals(-1, bm.get("f2"));      // But get() still works
		bm.put("f2", -2);                    // And put() still works
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@BeanTypeApply(on="Dummy",value=@BeanType(p="dummy"))
	@BeanTypeApply(on="B1",value=@BeanType(p="f1"))
	@BeanPropApply(on="Dummy",value=@BeanProp("dummy"))
	@BeanPropApply(on="B1.f2",value=@BeanProp("f2"))
	private static class B1Config {}

	public static class B1 {
		public int f1;

		private int f2 = -1;
		@BeanProp("f2") public int f2() { return f2; }
		public void setF2(int v) { f2 = v; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @MarshalledProp(name) on field not in @Marshalled(properties)
	// JUNEAU-248: Shouldn't be found in keySet()/entrySet()/containsKey() but should be accessible via get()/put()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void beanPropertyFieldNotInBeanProperties() {
		var bc = MarshallingContext.DEFAULT;

		var bm = bc.newBeanMap(A2.class);
		assertFalse(bm.containsKey("f2"));  // JUNEAU-248: Now consistent with keySet()
		assertEquals(-1, bm.get("f2"));      // But get() still works
		bm.put("f2", -2);                    // And put() still works
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@BeanType(p="f1")
	public static class A2 {
		public int f1;

		@BeanProp("f2")
		public int f2 = -1;
	}

	@Test void beanPropertyFieldNotInMarshalledConfig() {
		var bc = MarshallingContext.create().applyAnnotations(B2Config.class).build();

		var bm = bc.newBeanMap(B2.class);
		assertFalse(bm.containsKey("f2"));  // JUNEAU-248: Now consistent with keySet()
		assertEquals(-1, bm.get("f2"));      // But get() still works
		bm.put("f2", -2);                    // And put() still works
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@BeanTypeApply(on="Dummy",value=@BeanType(p="dummy"))
	@BeanTypeApply(on="B2",value=@BeanType(p="f1"))
	@BeanPropApply(on="Dummy",value=@BeanProp("dummy"))
	@BeanPropApply(on="B2.f2",value=@BeanProp("f2"))
	private static class B2Config {}

	public static class B2 {
		public int f1;
		public int f2 = -1;
	}
}