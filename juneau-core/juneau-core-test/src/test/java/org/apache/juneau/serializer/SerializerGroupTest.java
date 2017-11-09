// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.serializer;

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;

@SuppressWarnings({"javadoc"})
public class SerializerGroupTest {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test
	public void testSerializerGroupMatching() throws Exception {

		SerializerGroup sg = SerializerGroup.create().append(SA1.class, SA2.class, SA3.class).build();
		assertType(SA1.class, sg.getSerializer("text/foo"));
		assertType(SA1.class, sg.getSerializer("text/foo_a"));
		assertType(SA1.class, sg.getSerializer("text/xxx+foo_a"));
		assertType(SA1.class, sg.getSerializer("text/foo_a+xxx"));
		assertType(SA2.class, sg.getSerializer("text/foo+bar"));
		assertType(SA2.class, sg.getSerializer("text/foo+bar_a"));
		assertType(SA2.class, sg.getSerializer("text/bar+foo"));
		assertType(SA2.class, sg.getSerializer("text/bar_a+foo"));
		assertType(SA2.class, sg.getSerializer("text/bar+foo+xxx"));
		assertType(SA2.class, sg.getSerializer("text/bar_a+foo+xxx"));
		assertType(SA3.class, sg.getSerializer("text/baz"));
		assertType(SA3.class, sg.getSerializer("text/baz_a"));
		assertType(SA3.class, sg.getSerializer("text/baz+yyy"));
		assertType(SA3.class, sg.getSerializer("text/baz_a+yyy"));
		assertType(SA3.class, sg.getSerializer("text/yyy+baz"));
		assertType(SA3.class, sg.getSerializer("text/yyy+baz_a"));

		assertType(SA1.class, sg.getSerializer("text/foo;q=0.9,text/foo+bar;q=0.8"));
		assertType(SA2.class, sg.getSerializer("text/foo;q=0.8,text/foo+bar;q=0.9"));
	}


	public static class SA1 extends JsonSerializer {
		public SA1(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/foo+*", "text/foo_a+*");
		}
	}

	public static class SA2 extends JsonSerializer {
		public SA2(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/foo+bar+*", "text/foo+bar_a+*");
		}
	}

	public static class SA3 extends JsonSerializer {
		public SA3(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/baz+*", "text/baz_a+*");
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test
	public void testInheritence() throws Exception {
		SerializerGroupBuilder gb = null;
		SerializerGroup g = null;

		gb = SerializerGroup.create().append(SB1.class, SB2.class);
		g = gb.build();
		assertObjectEquals("['text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = g.builder().append(SB3.class, SB4.class);
		g = gb.build();
		assertObjectEquals("['text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = g.builder().append(SB5.class);
		g = gb.build();
		assertObjectEquals("['text/5','text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());
	}

	public static class SB1 extends JsonSerializer {
		public SB1(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/1");
		}
	}

	public static class SB2 extends JsonSerializer {
		public SB2(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/2", "text/2a");
		}
	}

	public static class SB3 extends JsonSerializer {
		public SB3(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/3");
		}
	}

	public static class SB4 extends JsonSerializer {
		public SB4(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/4", "text/4a");
		}
	}

	public static class SB5 extends JsonSerializer {
		public SB5(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/5");
		}
	}

	//====================================================================================================
	// Test media type with meta-characters
	//====================================================================================================
	@Test
	public void testMediaTypesWithMetaCharacters() throws Exception {
		SerializerGroupBuilder gb = null;
		SerializerGroup g = null;

		gb = SerializerGroup.create().append(SC1.class, SC2.class, SC3.class);
		g = gb.build();
		assertType(SC1.class, g.getSerializer("text/foo"));
		assertType(SC2.class, g.getSerializer("foo/json"));
		assertType(SC3.class, g.getSerializer("foo/foo"));
	}

	public static class SC1 extends JsonSerializer {
		public SC1(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "text/*");
		}
	}

	public static class SC2 extends JsonSerializer {
		public SC2(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "*/json");
		}
	}

	public static class SC3 extends JsonSerializer {
		public SC3(PropertyStore propertyStore) {
			super(propertyStore, "application/json", "*/*");
		}
	}
}
