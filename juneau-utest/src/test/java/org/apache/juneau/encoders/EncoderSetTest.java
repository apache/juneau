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
package org.apache.juneau.encoders;

import static org.apache.juneau.assertions.Assertions.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class EncoderSetTest extends SimpleTestBase {

	//====================================================================================================
	// Test matching
	//====================================================================================================
	@Test void testEncoderGroupMatching() {

		EncoderSet s = EncoderSet.create().add(Encoder1.class, Encoder2.class, Encoder3.class).build();
		assertObject(s.getEncoder("gzip1")).isType(Encoder1.class);
		assertObject(s.getEncoder("gzip2")).isType(Encoder2.class);
		assertObject(s.getEncoder("gzip2a")).isType(Encoder2.class);
		assertObject(s.getEncoder("gzip3")).isType(Encoder3.class);
		assertObject(s.getEncoder("gzip3a")).isType(Encoder3.class);
		assertObject(s.getEncoder("gzip3,gzip2,gzip1")).isType(Encoder3.class);
		assertObject(s.getEncoder("gzip3;q=0.9,gzip2;q=0.1,gzip1")).isType(Encoder1.class);
		assertObject(s.getEncoder("gzip2;q=0.9,gzip1;q=0.1,gzip3")).isType(Encoder3.class);
		assertObject(s.getEncoder("gzip1;q=0.9,gzip3;q=0.1,gzip2")).isType(Encoder2.class);
	}

	public static class Encoder1 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"gzip1"};
		}
	}

	public static class Encoder2 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"gzip2","gzip2a"};
		}
	}

	public static class Encoder3 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"gzip3","gzip3a"};
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test void testInheritence() {
		EncoderSet.Builder sb = null;
		EncoderSet s = null;

		sb = EncoderSet.create().add(E1.class, E2.class);
		s = sb.build();
		assertObject(s.getSupportedEncodings()).asJson().is("['E1','E2','E2a']");

		sb.add(E3.class, E4.class);
		s = sb.build();
		assertObject(s.getSupportedEncodings()).asJson().is("['E3','E4','E4a','E1','E2','E2a']");

		sb.add(E5.class);
		s = sb.build();
		assertObject(s.getSupportedEncodings()).asJson().is("['E5','E3','E4','E4a','E1','E2','E2a']");
	}

	public static class E1 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"E1"};
		}
	}

	public static class E2 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"E2","E2a"};
		}
	}

	public static class E3 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"E3"};
		}
	}

	public static class E4 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"E4","E4a"};
		}
	}

	public static class E5 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"E5"};
		}
	}
}