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
package org.apache.juneau.svl.vars;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.svl.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestrictedVarsTest {

	@Test
	public void testNoNest() throws Exception {
		VarResolver vr = VarResolver.create().vars(NoNestVar.class).build();

		test(vr, "$NoNest{foo}", "foo");
		test(vr, "$NoNest{$NoNest{foo}}", "$NoNest{foo}");
		test(vr, "$NoNest{foo $NoNest{foo} bar}", "foo $NoNest{foo} bar");
	}

	public static class XVar extends SimpleVar {

		public XVar() {
			super("X");
		}

		@Override
		public String resolve(VarResolverSession session, String arg) throws Exception {
			return 'x' + arg + 'x';
		}
	}

	public static class NoNestVar extends SimpleVar {

		public NoNestVar() {
			super("NoNest");
		}

		@Override
		public String resolve(VarResolverSession session, String arg) throws Exception {
			return arg.replaceAll("\\$", "\\\\\\$");
		}

		@Override
		protected boolean allowNested() {
			return false;
		}
	}

	@Test
	public void testNoRecurse() throws Exception {
		VarResolver vr = VarResolver.create().vars(XVar.class, NoRecurseVar.class).build();

		test(vr, "$NoRecurse{foo}", "$X{foo}");
		test(vr, "$NoRecurse{$NoRecurse{foo}}", "$X{$X{foo}}");
		test(vr, "$NoRecurse{foo $NoRecurse{foo} bar}", "$X{foo $X{foo} bar}");
	}

	public static class NoRecurseVar extends SimpleVar {

		public NoRecurseVar() {
			super("NoRecurse");
		}

		@Override
		public String resolve(VarResolverSession session, String arg) throws Exception {
			return "$X{"+arg+"}";
		}

		@Override
		protected boolean allowRecurse() {
			return false;
		}
	}

	@Test
	public void testNoNestOrRecurse() throws Exception {
		VarResolver vr = VarResolver.create().vars(XVar.class, NoEitherVar.class).build();

		test(vr, "$NoEither{foo}", "$X{foo}");
		test(vr, "$NoEither{$NoEither{foo}}", "$X{$NoEither{foo}}");
		test(vr, "$NoEither{foo $NoEither{foo} bar}", "$X{foo $NoEither{foo} bar}");
	}

	public static class NoEitherVar extends SimpleVar {

		public NoEitherVar() {
			super("NoEither");
		}

		@Override
		public String resolve(VarResolverSession session, String arg) throws Exception {
			return "$X{" + arg + "}";
		}

		@Override
		protected boolean allowNested() {
			return false;
		}

		@Override
		protected boolean allowRecurse() {
			return false;
		}
	}

	private void test(VarResolver vr, String s, String expected) throws IOException {
		StringWriter sw = new StringWriter();
		vr.resolveTo(s, sw);
		assertEquals(expected, sw.toString());
		assertEquals(expected, vr.resolve(s));
	}
}
