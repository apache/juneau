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

import static org.apache.juneau.assertions.Assertions.*;
import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

class SerializerSetTest extends SimpleTestBase {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void testSerializerGroupMatching() {

		SerializerSet sg = SerializerSet.create().add(SA1.class, SA2.class, SA3.class).build();
		assertObject(sg.getSerializer("text/foo")).isType(SA1.class);
		assertObject(sg.getSerializer("text/foo_a")).isType(SA1.class);
		assertObject(sg.getSerializer("text/xxx+foo_a")).isType(SA1.class);
		assertObject(sg.getSerializer("text/foo_a+xxx")).isType(SA1.class);
		assertObject(sg.getSerializer("text/foo+bar")).isType(SA2.class);
		assertObject(sg.getSerializer("text/foo+bar_a")).isType(SA2.class);
		assertObject(sg.getSerializer("text/bar+foo")).isType(SA2.class);
		assertObject(sg.getSerializer("text/bar_a+foo")).isType(SA2.class);
		assertObject(sg.getSerializer("text/bar+foo+xxx")).isType(SA2.class);
		assertObject(sg.getSerializer("text/bar_a+foo+xxx")).isType(SA2.class);
		assertObject(sg.getSerializer("text/baz")).isType(SA3.class);
		assertObject(sg.getSerializer("text/baz_a")).isType(SA3.class);
		assertObject(sg.getSerializer("text/baz+yyy")).isType(SA3.class);
		assertObject(sg.getSerializer("text/baz_a+yyy")).isType(SA3.class);
		assertObject(sg.getSerializer("text/yyy+baz")).isType(SA3.class);
		assertObject(sg.getSerializer("text/yyy+baz_a")).isType(SA3.class);

		assertObject(sg.getSerializer("text/foo;q=0.9,text/foo+bar;q=0.8")).isType(SA1.class);
		assertObject(sg.getSerializer("text/foo;q=0.8,text/foo+bar;q=0.9")).isType(SA2.class);
	}


	public static class SA1 extends JsonSerializer {
		public SA1(JsonSerializer.Builder builder) {
			super(builder.accept("text/foo+*,text/foo_a+*"));
		}
	}

	public static class SA2 extends JsonSerializer {
		public SA2(JsonSerializer.Builder builder) {
			super(builder.accept("text/foo+bar+*,text/foo+bar_a+*"));
		}
	}

	public static class SA3 extends JsonSerializer {
		public SA3(JsonSerializer.Builder builder) {
			super(builder.accept("text/baz+*,text/baz_a+*"));
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test void testInheritence() {
		SerializerSet.Builder gb = null;
		SerializerSet g = null;

		gb = SerializerSet.create().add(SB1.class, SB2.class);
		g = gb.build();
		assertJson(g.getSupportedMediaTypes(), "['text/1','text/2','text/2a']");

		gb = SerializerSet.create().add(SB1.class, SB2.class).add(SB3.class, SB4.class);
		g = gb.build();
		assertJson(g.getSupportedMediaTypes(), "['text/3','text/4','text/4a','text/1','text/2','text/2a']");

		gb = SerializerSet.create().add(SB1.class, SB2.class).add(SB3.class, SB4.class).add(SB5.class);
		g = gb.build();
		assertJson(g.getSupportedMediaTypes(), "['text/5','text/3','text/4','text/4a','text/1','text/2','text/2a']");
	}

	public static class SB1 extends JsonSerializer {
		public SB1(JsonSerializer.Builder builder) {
			super(builder.accept("text/1"));
		}
	}

	public static class SB2 extends JsonSerializer {
		public SB2(JsonSerializer.Builder builder) {
			super(builder.accept("text/2,text/2a"));
		}
	}

	public static class SB3 extends JsonSerializer {
		public SB3(JsonSerializer.Builder builder) {
			super(builder.accept("text/3"));
		}
	}

	public static class SB4 extends JsonSerializer {
		public SB4(JsonSerializer.Builder builder) {
			super(builder.accept("text/4,text/4a"));
		}
	}

	public static class SB5 extends JsonSerializer {
		public SB5(JsonSerializer.Builder builder) {
			super(builder.accept("text/5"));
		}
	}

	//====================================================================================================
	// Test media type with meta-characters
	//====================================================================================================
	@Test void testMediaTypesWithMetaCharacters() {
		SerializerSet.Builder gb = null;
		SerializerSet g = null;

		gb = SerializerSet.create().add(SC1.class, SC2.class, SC3.class);
		g = gb.build();
		assertObject(g.getSerializer("text/foo")).isType(SC1.class);
		assertObject(g.getSerializer("foo/json")).isType(SC2.class);
		assertObject(g.getSerializer("foo/foo")).isType(SC3.class);
	}

	public static class SC1 extends JsonSerializer {
		public SC1(JsonSerializer.Builder builder) {
			super(builder.accept("text/*"));
		}
	}

	public static class SC2 extends JsonSerializer {
		public SC2(JsonSerializer.Builder builder) {
			super(builder.accept("*/json"));
		}
	}

	public static class SC3 extends JsonSerializer {
		public SC3(JsonSerializer.Builder builder) {
			super(builder.accept("*/*"));
		}
	}
}