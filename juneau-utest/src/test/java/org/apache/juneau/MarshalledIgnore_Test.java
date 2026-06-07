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

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

class MarshalledIgnore_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// @MarshalledIgnore on TYPE — skip entirely during marshalling: serializers output null, parsers return null.
	//------------------------------------------------------------------------------------------------------------------

	@MarshalledIgnore
	public static class B1 {
		public int f = 1;

		@Override
		public String toString() {
			return "xxx";
		}
	}

	public static class B {
		public int f2 = 2;
		public B1 f3 = new B1();

		public B1 getF4() {
			return new B1();
		}
	}

	@Test void a01_marshalledIgnoreOnType() {
		assertJson("{f2:2,f3:null,f4:null}", new B());
	}

	@MarshalledIgnoreApply(on="B1c",value=@MarshalledIgnore())
	private static class B1cConfig {}

	public static class B1c {
		public int f = 1;

		@Override
		public String toString() {
			return "xxx";
		}
	}

	public static class Bc {
		public int f2 = 2;
		public B1c f3 = new B1c();

		public B1c getF4() {
			return new B1c();
		}
	}

	@Test void a02_marshalledIgnoreOnType_usingConfig() {
		assertSerialized(new Bc(), Json5Serializer.DEFAULT.copy().applyAnnotations(B1cConfig.class).build(), "{f2:2,f3:null,f4:null}");
	}
}
