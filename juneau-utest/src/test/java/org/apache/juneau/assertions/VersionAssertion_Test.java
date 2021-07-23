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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.Version.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class VersionAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private VersionAssertion test(Version value) {
		return assertVersion(value).silent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
	}

	@Test
	public void a02_stdout() throws Exception {
		test(null).stdout();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		Version x = of("1"), nil = null;
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Version x = of("1"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("{maintenance:null,major:1,minor:null}");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		Version x1 = of("1");
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		Version x = of("1"), nil = null;
		test(x).asJson().is("{maintenance:null,major:1,minor:null}");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Version x = of("1"), nil = null;
		test(x).asJsonSorted().is("{maintenance:null,major:1,minor:null}");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Version x1 = of("1"), x2 = of("2");
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bc01_part() throws Exception {
		Version x = of("1.2.3"), nil = null;
		test(x).part(-1).isNull();
		test(x).part(0).is(1);
		test(x).part(1).is(2);
		test(x).part(2).is(3);
		test(x).part(3).isNull();
		test(nil).part(0).isNull();
	}

	@Test
	public void bc02_major() throws Exception {
		Version x = of("1.2.3"), nil = null;
		test(x).major().is(1);
		test(nil).major().isNull();
	}

	@Test
	public void bc03_minor() throws Exception {
		Version x = of("1.2.3"), nil = null;
		test(x).minor().is(2);
		test(nil).minor().isNull();
	}

	@Test
	public void bc04_maintenance() throws Exception {
		Version x = of("1.2.3"), nil = null;
		test(x).maintenance().is(3);
		test(nil).maintenance().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Version x = of("1"), nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Version x = of("1"), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Version x = of("1"), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.  Expect='2'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Version x1 = of("1");
		test(x1).is(x->x.getMajor().orElse(2) == 1);
		assertThrown(()->test(x1).is(x->x.getMajor().orElse(2)==2)).message().oneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.  Did not expect='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Version x1 = of("1"), x1a = of("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='1(Version@*)'.  Actual='1(Version@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='1(Version@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.  Expect='null(null)'.  Actual='1(Version@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Version x1 = of("1"), x1a = of("1"), x2 = of("2"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Version x = of("1"), nil = null;
		test(x).isType(Version.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.Version'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Version x = of("1"), nil = null;
		test(x).isExactType(Version.class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='org.apache.juneau.Version'.");
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.Version'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Version x = of("1"), nil = null;
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Version x = of("1"), nil = null;
		test(x).isJson("{maintenance:null,major:1,minor:null}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isGt() throws Exception {
		Version x1 = of("1"), x2 = of("2"), nil = null;
		test(x2).isGt(x1);
		assertThrown(()->test(x1).isGt(x1)).message().oneLine().is("Value was not greater than expected.  Expect='1'.  Actual='1'.");
		assertThrown(()->test(x1).isGt(x2)).message().oneLine().is("Value was not greater than expected.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).isGt(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGt(x2)).message().is("Value was null.");
	}

	@Test
	public void cb02_isGte() throws Exception {
		Version x1 = of("1"), x2 = of("2"), nil = null;
		test(x2).isGte(x1);
		test(x1).isGte(x1);
		assertThrown(()->test(x1).isGte(x2)).message().oneLine().is("Value was not greater than or equals to expected.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).isGte(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGte(x2)).message().is("Value was null.");
	}

	@Test
	public void cb03_isLt() throws Exception {
		Version x1 = of("1"), x2 = of("2"), nil = null;
		test(x1).isLt(x2);
		assertThrown(()->test(x1).isLt(x1)).message().oneLine().is("Value was not less than expected.  Expect='1'.  Actual='1'.");
		assertThrown(()->test(x2).isLt(x1)).message().oneLine().is("Value was not less than expected.  Expect='1'.  Actual='2'.");
		assertThrown(()->test(x2).isLt(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLt(x1)).message().is("Value was null.");
	}

	@Test
	public void cb04_isLte() throws Exception {
		Version x1 = of("1"), x2 = of("2"), nil = null;
		test(x1).isLte(x2);
		test(x1).isLte(x1);
		assertThrown(()->test(x2).isLte(x1)).message().oneLine().is("Value was not less than or equals to expected.  Expect='1'.  Actual='2'.");
		assertThrown(()->test(x2).isLte(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLte(x1)).message().is("Value was null.");
	}

	@Test
	public void cb05_isBetween() throws Exception {
		Version x1 = of("1"), x2 = of("2"), x3 = of("3"), x4 = of("4"), nil = null;
		test(x1).isBetween(x1, x3);
		test(x2).isBetween(x1, x3);
		test(x3).isBetween(x1, x3);
		assertThrown(()->test(x4).isBetween(x1, x3)).message().oneLine().is("Value was not less than or equals to expected.  Expect='3'.  Actual='4'.");
		assertThrown(()->test(nil).isBetween(x1, x3)).message().is("Value was null.");
		assertThrown(()->test(x1).isBetween(nil, x3)).message().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(x1).isBetween(x1, nil)).message().oneLine().is("Argument 'upper' cannot be null.");
	}
}
