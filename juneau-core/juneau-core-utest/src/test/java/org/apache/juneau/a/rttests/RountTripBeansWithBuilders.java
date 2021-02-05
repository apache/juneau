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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;
import static java.util.Collections.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RountTripBeansWithBuilders extends RoundTripTest {

	public RountTripBeansWithBuilders(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
		applyAnnotations(AcConfig.class);
	}

	//====================================================================================================
	// simple
	//====================================================================================================

	@Test
	public void simple() throws Exception {
		A a = A.create().f1(1).build();
		a = roundTrip(a, A.class);
		assertObject(a).asJson().is("{f1:1}");
	}

	public static class A {
		private final int f1;

		public A(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {
			private int f1;

			public Builder f1(int f1) {
				this.f1 = f1;
				return this;
			}

			public A build() {
				return new A(this);
			}
		}

		public int getF1() {
			return f1;
		}
	}

	@Test
	public void simple_usingConfig() throws Exception {
		Ac a = Ac.create().f1(1).build();
		a = roundTrip(a, Ac.class);
		assertObject(a).asJson().is("{f1:1}");
	}

	@Bean(on="Dummy1", findFluentSetters=true)
	@Bean(on="Builder", findFluentSetters=true)
	@Bean(on="Dummy2", findFluentSetters=true)
	private static class AcConfig {}

	public static class Ac {
		private final int f1;

		public Ac(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		public static class Builder {
			private int f1;

			public Builder f1(int f1) {
				this.f1 = f1;
				return this;
			}

			public Ac build() {
				return new Ac(this);
			}
		}

		public int getF1() {
			return f1;
		}
	}

	//====================================================================================================
	// Bean property builder, simple
	//====================================================================================================

	@Test
	public void beanPropertyBuilder_simple() throws Exception {
		A2 a = A2.create().f1(A.create().f1(1).build()).build();
		a = roundTrip(a, A2.class);
		assertObject(a).asJson().is("{f1:{f1:1}}");
	}

	public static class A2 {
		private final A f1;

		public A2(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {
			private A f1;

			public Builder f1(A f1) {
				this.f1 = f1;
				return this;
			}

			public A2 build() {
				return new A2(this);
			}
		}

		public A getF1() {
			return f1;
		}
	}

	//====================================================================================================
	// Bean property builder, collections
	//====================================================================================================

	@Test
	public void beanPropertyBuilder_collections() throws Exception {
		// It's simply not possible to allow for expanded parameters with a builder-based approach
		// since the value on the builder can only be set once.
		if (label.equals("UrlEncoding - expanded params"))
			return;
		A3 a = A3.create()
			.f1(new A[]{A.create().f1(1).build()})
			.f2(singletonList(A.create().f1(2).build()))
			.f3(singletonList((List<A>)singletonList(A.create().f1(3).build())))
			.f4(singletonList(new A[]{A.create().f1(4).build()}))
			.f5(singletonList(singletonList(new A[]{A.create().f1(5).build()})))
			.f6(singletonMap("foo", A.create().f1(6).build()))
			.f7(singletonMap("foo", singletonMap("bar", A.create().f1(7).build())))
			.f8(singletonMap("foo", new A[]{A.create().f1(8).build()}))
			.f9(singletonMap("foo", singletonList(new A[]{A.create().f1(9).build()})))
			.build();
		a = roundTrip(a, A3.class);
		assertObject(a).asJson().is("{f1:[{f1:1}],f2:[{f1:2}],f3:[[{f1:3}]],f4:[[{f1:4}]],f5:[[[{f1:5}]]],f6:{foo:{f1:6}},f7:{foo:{bar:{f1:7}}},f8:{foo:[{f1:8}]},f9:{foo:[[{f1:9}]]}}");
	}

	@Bean(sort=true)
	public static class A3 {
		private final A[] f1;

		private final List<A> f2;
		private final List<List<A>> f3;
		private final List<A[]> f4;
		private final List<List<A[]>> f5;

		private final Map<String,A> f6;
		private final Map<String,Map<String,A>> f7;
		private final Map<String,A[]> f8;
		private final Map<String,List<A[]>> f9;

		public A3(Builder b) {
			this.f1 = b.f1;
			this.f2 = b.f2;
			this.f3 = b.f3;
			this.f4 = b.f4;
			this.f5 = b.f5;
			this.f6 = b.f6;
			this.f7 = b.f7;
			this.f8 = b.f8;
			this.f9 = b.f9;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {
			private A[] f1;

			private List<A> f2;
			private List<List<A>> f3;
			private List<A[]> f4;
			private List<List<A[]>> f5;

			private Map<String,A> f6;
			private Map<String,Map<String,A>> f7;
			private Map<String,A[]> f8;
			private Map<String,List<A[]>> f9;

			public Builder f1(A[] f1) {
				this.f1 = f1;
				return this;
			}

			public Builder f2(List<A> f2) {
				this.f2 = f2;
				return this;
			}

			public Builder f3(List<List<A>> f3) {
				this.f3 = f3;
				return this;
			}

			public Builder f4(List<A[]> f4) {
				this.f4 = f4;
				return this;
			}

			public Builder f5(List<List<A[]>> f5) {
				this.f5 = f5;
				return this;
			}

			public Builder f6(Map<String,A> f6) {
				this.f6 = f6;
				return this;
			}

			public Builder f7(Map<String,Map<String,A>> f7) {
				this.f7 = f7;
				return this;
			}

			public Builder f8(Map<String,A[]> f8) {
				this.f8 = f8;
				return this;
			}

			public Builder f9(Map<String,List<A[]>> f9) {
				this.f9 = f9;
				return this;
			}

			public A3 build() {
				return new A3(this);
			}
		}

		public A[] getF1() {
			return f1;
		}
		public List<A> getF2() {
			return f2;
		}
		public List<List<A>> getF3() {
			return f3;
		}
		public List<A[]> getF4() {
			return f4;
		}
		public List<List<A[]>> getF5() {
			return f5;
		}
		public Map<String,A> getF6() {
			return f6;
		}
		public Map<String,Map<String,A>> getF7() {
			return f7;
		}
		public Map<String,A[]> getF8() {
			return f8;
		}
		public Map<String,List<A[]>> getF9() {
			return f9;
		}
	}
}
