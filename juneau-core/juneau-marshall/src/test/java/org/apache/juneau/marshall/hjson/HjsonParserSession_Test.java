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
package org.apache.juneau.marshall.hjson;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link HjsonParserSession} focusing on uncovered gaps reported by JaCoCo.
 *
 * <p>Each test exercises a specific branch in {@code HjsonParserSession} not already covered by the
 * existing {@code Hjson*_Test} suite ({@link HjsonParser_Test}, {@link HjsonEdgeCases_Test}, etc.).
 */
@SuppressWarnings({
	"unchecked", // Parser returns Object; cast to Map/List in tests
	"resource"   // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class HjsonParserSession_Test extends TestBase {

	// ============================================================
	// a01-a07: doRead top-level branches
	// ============================================================

	@Test
	void a01_ioExceptionFromReader() {
		// pipe.asString() throws IOException -> wrapped in ParseException.
		var bad = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException {
				throw new IOException("boom");
			}
			@Override public void close() { /* no-op */ }
		};
		assertThrows(Exception.class, () -> HjsonParser.DEFAULT.read(bad, Map.class, String.class, Object.class));
	}

	@Test
	void a02_nullInputReturnsNull() throws Exception {
		// pipe.asString() returns null path: doRead returns null.
		var m = HjsonParser.DEFAULT.read((Object) null, Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void a03_whitespaceOnlyInputReturnsNull() throws Exception {
		// trimmed.isEmpty() branch.
		var m = HjsonParser.DEFAULT.read("   \n\t  \n", Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void a04_eofValueAtRootBracelessReturnsNull() throws Exception {
		// readValue's EOF arm: value position hits end-of-input immediately after the colon.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("a:", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertTrue(m.containsKey("a"));
		assertNull(m.get("a"));
	}

	@Test
	void a05_singleRootlessPairNoTrailingNewline() throws Exception {
		// readRootBraceless's first-pair NEWLINE check (false arm): only one key, no trailing newline.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("name: Bob", Map.class, String.class, Object.class);
		assertBean(m, "name", "Bob");
	}

	@Test
	void a06_threeRootlessPairsExercisesLoopNewlineBothArms() throws Exception {
		// readRootBraceless's loop NEWLINE check: true arm between b/c, false arm at EOF after c.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("a: 1\nb: 2\nc: 3", Map.class, String.class, Object.class);
		assertBean(m, "a,b,c", "1,2,3");
	}

	@Test
	void a07_missingColonAfterSubsequentRootlessKeyThrows() {
		// readRootBraceless's "Expected : after key" inside the loop, for the SECOND pair (line 145/146).
		assertThrowsWithMessage(ParseException.class, "Expected : after key", ()->HjsonParser.DEFAULT.read("a: 1\nb 2", Map.class, String.class, Object.class));
	}

	@Test
	void a08_rootBracelessIntoBeanType() throws Exception {
		// readRootBraceless's beanMeta ternary: type.isBean()==true (a03/a05 above only exercise Map targets).
		var bean = HjsonParser.DEFAULT.read("name: Alice\nage: 30", SimpleBean.class);
		assertNotNull(bean);
		assertEquals("Alice", bean.name);
		assertEquals(30, bean.age);
	}

	// ============================================================
	// b01-b06: readKey branches -- NULL/TRUE/FALSE/NUMBER as key tokens
	// ============================================================

	@Test
	void b01_nullKeyword_asKey() throws Exception {
		// readKey's NULL case -> preserved as a literal null map key.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{null: 1}", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertTrue(m.containsKey(null));
		assertEquals(1, m.get(null));
	}

	@Test
	void b02_trueKeyword_asKey() throws Exception {
		// readKey's TRUE case -> "true" string key.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{true: 1}", Map.class, String.class, Object.class);
		assertEquals(1, m.get("true"));
	}

	@Test
	void b03_falseKeyword_asKey() throws Exception {
		// readKey's FALSE case -> "false" string key.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{false: 1}", Map.class, String.class, Object.class);
		assertEquals(1, m.get("false"));
	}

	@Test
	void b04_numberToken_asKey() throws Exception {
		// readKey's NUMBER case -> numberValue().toString() key.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{42: answer}", Map.class, String.class, Object.class);
		assertEquals("answer", m.get("42"));
	}

	@Test
	void b05_invalidKeyTokenThrows() {
		// readKey's default arm (already covered by HjsonEdgeCases_Test.g03, retained here for locality).
		assertThrowsWithMessage(ParseException.class, "Expected key", ()->HjsonParser.DEFAULT.read("{[1]:1}", Map.class, String.class, Object.class));
	}

	@Test
	void b06_trimStringsOnQuotelessKey() throws Exception {
		// readKey's isTrimStrings() true-arm for a QUOTELESS key.
		var p = HjsonParser.create().trimStrings().build();
		var m = (Map<String,Object>) p.read("{  a  : 1}", Map.class, String.class, Object.class);
		assertEquals(1, m.get("a"));
	}

	// ============================================================
	// c01-c04: readArray branches
	// ============================================================

	@Test
	void c01_arrayWithNewlineSeparators() throws Exception {
		// readArray's separator check: NEWLINE arm (b07 in HjsonParser_Test only exercises COMMA).
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{\"arr\":[1\n2\n3]}", Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("arr");
		assertEquals(3, arr.size());
	}

	@Test
	void c02_emptyArray() throws Exception {
		// readArray's immediate-RBRACKET branch.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{\"arr\":[]}", Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("arr");
		assertTrue(arr.isEmpty());
	}

	@Test
	void c03_arrayOfObjects() throws Exception {
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{\"arr\":[{a:1},{a:2}]}", Map.class, String.class, Object.class);
		var arr = (List<Map<String,Object>>) m.get("arr");
		assertEquals(2, arr.size());
		assertEquals(1, arr.get(0).get("a"));
	}

	@Test
	void c04_beanWithListOfBeans() throws Exception {
		// readArray's element-type threading via propertyType -- List<SimpleBean> property.
		var hjson = "items: [ {name: A, age: 1}, {name: B, age: 2} ]";
		var bean = HjsonParser.DEFAULT.read(hjson, BeanWithList.class);
		assertNotNull(bean);
		assertEquals(2, bean.items.size());
		assertEquals("A", bean.items.get(0).name);
		assertEquals(2, bean.items.get(1).age);
	}

	@Test
	void c05_arrayTargetType() throws Exception {
		// convertToCollection's "type.inner().isArray()" true-arm -- an actual Java array target
		// (c01-c04 above all target List, taking the false arm of that same guard).
		var arr = HjsonParser.DEFAULT.read("[1,2,3]", int[].class);
		assertArrayEquals(new int[] { 1, 2, 3 }, arr);
	}

	// ============================================================
	// d01-d02: readObject branches
	// ============================================================

	@Test
	void d01_emptyObject() throws Exception {
		// readObject's immediate-RBRACE branch.
		var m = (Map<String,Object>) HjsonParser.DEFAULT.read("{}", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test
	void d02_beanPropertyIsNestedBeanNotMapOrCollection() throws Exception {
		// propertyType's cm != null but neither isMap() nor isCollectionOrArray() -- falls through to object().
		var bean = HjsonParser.DEFAULT.read("{inner: {x: hi}}", BeanWithBeanProp.class);
		assertNotNull(bean);
		assertNotNull(bean.inner);
		assertEquals("hi", bean.inner.x);
	}

	// ============================================================
	// e01-e04: byte[] / binary swap coercion (coerceMemberValue)
	// ============================================================

	@Test
	void e01_byteArrayInListDefaultFormatUsesUtf8Fallback() throws Exception {
		// List<byte[]> at default BinaryFormat (NOT_SET): type.getSwap(this) returns null (no swap
		// configured for NOT_SET), so coerceMemberValue falls through to convertToMemberType's plain
		// BasicConverter default String->byte[] coercion, which is raw UTF-8 bytes -- NOT Base64 decode.
		// (This is a real behavioral gap called out in coerceMemberValue's own javadoc: only an
		// EXPLICITLY configured BinaryFormat installs a swap; the implicit default does not.)
		var hjson = "{data: [\"abc\"]}";
		var bean = HjsonParser.DEFAULT.read(hjson, BeanWithByteList.class);
		assertNotNull(bean);
		assertEquals(1, bean.data.size());
		assertArrayEquals("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8), bean.data.get(0));
	}

	@Test
	void e02_byteArrayInListHex() throws Exception {
		// List<byte[]> with BinaryFormat.HEX: swap != null -> explicit unswap() call.
		var p = HjsonParser.create().binaryFormat(BinaryFormat.HEX).build();
		var hjson = "{data: [\"010203\"]}";
		var bean = p.read(hjson, BeanWithByteList.class);
		assertNotNull(bean);
		assertArrayEquals(new byte[] { 1, 2, 3 }, bean.data.get(0));
	}

	@Test
	void e03_beanPropertyIsTypedMap() throws Exception {
		// propertyType's cm.isMap() arm -- threads Map<Integer,String> key/value types into readObject.
		var bean = HjsonParser.DEFAULT.read("{counts: {1: a, 2: b}}", BeanWithIntMap.class);
		assertNotNull(bean);
		assertEquals("a", bean.counts.get(1));
		assertEquals("b", bean.counts.get(2));
	}

	@Test
	void e05_nullKeyInBeanObjectFallsBackToObjectType() {
		// propertyType's "key != null" arm: a NULL-keyword key inside a bean-typed object -- the
		// beanMeta != null guard is true, but key == null short-circuits to object() rather than a
		// property lookup (e01-e04 above only exercise non-null keys against a non-null beanMeta).
		// propertyType() itself resolves cleanly (object() fallback); the resulting MarshalledMap
		// still fails to load into a BeanMap because of the null key downstream, which is an
		// orthogonal (and pre-existing) BeanMap.load() limitation, not something propertyType causes.
		assertThrows(Exception.class, () -> HjsonParser.DEFAULT.read("{null: 1, name: hi}", BeanWithBeanProp.class));
	}

	@Test
	void e06_typePropertyKeyFallsBackToObjectType() throws Exception {
		// propertyType's "!key.equals(getBeanTypePropertyName(...))" false arm: the "_type" key itself
		// is excluded from the property-meta lookup (it's consumed by cast()/convertToBean instead).
		var p = HjsonParser.create().beanDictionary(M23_Sub.class).build();
		var bean = p.read("{_type: M23_Sub, name: a, extra: 5}", M23_Base.class);
		assertInstanceOf(M23_Sub.class, bean);
		assertEquals("a", bean.name);
		assertEquals(5, ((M23_Sub) bean).extra);
	}

	@Test
	void e04_swapPathOnBeanBytesProperty() throws Exception {
		// convertToBean's "if (nn(swap))" branch via a scalar byte[] bean property (not inside a List).
		var p = HjsonParser.create().binaryFormat(BinaryFormat.HEX).build();
		var bean = p.read("{data: \"010203\"}", BeanWithBytes.class);
		assertNotNull(bean);
		assertArrayEquals(new byte[] { 1, 2, 3 }, bean.data);
	}

	// ============================================================
	// f01-f08: convertToBean / injectAnnotations / injectParentAnnotations branches
	// ============================================================

	@Test
	void f01_readEmptyToBean() throws Exception {
		// convertToBean(null,...) returns null branch.
		var bean = HjsonParser.DEFAULT.read("", SimpleBean.class);
		assertNull(bean);
	}

	@Test
	void f02_readBeanWithNameProperty() throws Exception {
		// injectAnnotations: cm.getNameProperty() != null path.
		var hjson = "{items: {foo: {value: aa}, bar: {value: bb}}}";
		var bean = HjsonParser.DEFAULT.read(hjson, OuterWithNamedMap.class);
		assertNotNull(bean);
		assertNotNull(bean.items);
		var foo = bean.items.get("foo");
		assertNotNull(foo);
		assertEquals("foo", foo.name);
		assertEquals("aa", foo.value);
	}

	@Test
	void f03_unknownPropertyKeyHasNoPropertyMeta() throws Exception {
		// injectAnnotations' "pm == null" arm -- a map key with no corresponding declared bean property.
		var p = HjsonParser.create().ignoreUnknownBeanProperties().build();
		var bean = p.read("{inner: {x: hi}, bogus: {y: 1}}", BeanWithBeanProp.class);
		assertNotNull(bean);
		assertEquals("hi", bean.inner.x);
	}

	@Test
	void f04_nullValuedPropertySkipsFurtherInjection() throws Exception {
		// injectAnnotations' "val == null" arm reached via getBeanValueSafely returning a legitimately
		// null property value (as opposed to f05's exception-driven null).
		var bean = HjsonParser.DEFAULT.read("{name: null, value: x}", NamedChild.class);
		assertNotNull(bean);
		assertNull(bean.name);
		assertEquals("x", bean.value);
	}

	@Test
	void f05_getterThrowsIsCaughtAndTreatedAsNull() throws Exception {
		// getBeanValueSafely's catch block: BeanMap.get() throwing is swallowed and treated as null,
		// which in turn short-circuits injectAnnotations' "val == null" arm.
		var bean = HjsonParser.DEFAULT.read("{bad: x, other: y}", ThrowingGetterBean.class);
		assertNotNull(bean);
		assertEquals("y", bean.other);
	}

	@Test
	void f06_parentPropertyInjectedOnListElements() throws Exception {
		// injectParentAnnotations' collection branch: List<Child> elements get @ParentProperty back-filled.
		var bean = HjsonParser.DEFAULT.read("{children: [{name: a}, {name: b}]}", BeanWithParent.class);
		assertNotNull(bean);
		assertEquals(2, bean.children.size());
		assertSame(bean, bean.children.get(0).parent);
		assertSame(bean, bean.children.get(1).parent);
	}

	@Test
	void f07_nullElementInParentAnnotatedList() throws Exception {
		// injectParentAnnotations' "val == null" true-arm, reached via per-element recursion.
		var bean = HjsonParser.DEFAULT.read("{children: [null, {name: a}]}", BeanWithParent.class);
		assertNotNull(bean);
		assertEquals(2, bean.children.size());
		assertNull(bean.children.get(0));
		assertSame(bean, bean.children.get(1).parent);
	}

	@Test
	void f08_objectTypedListElementSkipsRecursion() throws Exception {
		// injectParentAnnotations' collection branch: et.isObject() == true short-circuits before recursing.
		var bean = HjsonParser.DEFAULT.read("{items: [1, \"a\"]}", BeanWithObjectList.class);
		assertNotNull(bean);
		assertEquals(2, bean.items.size());
	}

	@Test
	void f09_parentInjectedOnArrayTypedProperty() throws Exception {
		// injectParentAnnotations' collection branch backed by a real Java array -- covers toIterable's
		// "val instanceof Object[]" arm (f06 uses a List, taking the Collection arm instead).
		var bean = HjsonParser.DEFAULT.read("{children: [{name: a}, {name: b}]}", BeanWithParentArray.class);
		assertNotNull(bean);
		assertEquals(2, bean.children.length);
		assertSame(bean, bean.children[0].parent);
		assertSame(bean, bean.children[1].parent);
	}

	@Test
	void f10_mapOfPlainBeansWithoutNameProperty() throws Exception {
		// injectParentAnnotations' map branch with vt.getNameProperty() == null.
		var bean = HjsonParser.DEFAULT.read("{items: {a: {x: 1}}}", BeanWithMapOfBean.class);
		assertNotNull(bean);
		assertEquals("1", bean.items.get("a").x);
	}

	@Test
	void f11_nullValueInParentAnnotatedMap() throws Exception {
		// injectParentAnnotations' map branch: a null map value.
		var bean = HjsonParser.DEFAULT.read("{items: {a: null}}", BeanWithMapOfBean.class);
		assertNotNull(bean);
		assertTrue(bean.items.containsKey("a"));
		assertNull(bean.items.get("a"));
	}

	@Test
	void f12_objectTypedMapValueSkipsRecursion() throws Exception {
		// injectParentAnnotations' map branch: vt.isObject() == true short-circuits before recursing.
		var bean = HjsonParser.DEFAULT.read("{items: {a: 1, b: hi}}", BeanWithObjectMap.class);
		assertNotNull(bean);
		assertEquals(2, bean.items.size());
	}

	@Test
	void f13_nestedBeanPropertyRecursesIntoInjectAnnotations() throws Exception {
		// injectParentAnnotations' bean branch: cm.isBean() && !(val instanceof Map) -> recurse into
		// injectAnnotations for the nested bean's own @NameProperty/@ParentProperty fields.
		var bean = HjsonParser.DEFAULT.read("{child: {name: a}}", BeanWithNamedChildProp.class);
		assertNotNull(bean);
		assertNotNull(bean.child);
		assertSame(bean, bean.child.parent);
	}

	// ============================================================
	// g01-g02: builder coverage
	// ============================================================

	@Test
	void g01_builderCreate() {
		HjsonParser parser = HjsonParser.DEFAULT;
		var b = HjsonParserSession.create(parser);
		assertNotNull(b);
		var s = b.build();
		assertNotNull(s);
	}

	@Test
	void g02_builderRequiresContext() {
		assertThrows(Exception.class, () -> HjsonParserSession.create((HjsonParser) null));
	}

	@Test
	void g03_isRecordStreamingFalse() {
		assertFalse(HjsonParser.DEFAULT.createSession().build().isRecordStreaming());
	}

	@Test
	void g04_readRecordsDelegatesToRecordAdapter() throws Exception {
		try (var reader = HjsonParser.DEFAULT.createSession().build().readRecords("{a:1}\n{a:2}")) {
			assertNotNull(reader);
		}
	}

	// ============================================================
	// Helper bean classes
	// ============================================================

	public static class SimpleBean {
		public String name;
		public int age;
	}

	public static class NamedChild {
		@NameProperty
		public String name;
		public String value;
	}

	public static class OuterWithNamedMap {
		public Map<String,NamedChild> items;
	}

	public static class BeanWithByteList {
		public List<byte[]> data;
	}

	public static class BeanWithIntMap {
		public Map<Integer,String> counts;
	}

	public static class BeanWithList {
		public List<SimpleBean> items;
	}

	public static class BeanWithBytes {
		public byte[] data;
	}

	public static class Inner {
		public String x;
	}

	public static class BeanWithBeanProp {
		public Inner inner;
	}

	public static class BeanWithParent {
		public List<Child> children;
	}

	public static class Child {
		@ParentProperty
		public BeanWithParent parent;
		public String name;
	}

	public static class BeanWithMapOfBean {
		public Map<String,Inner> items;
	}

	public static class BeanWithObjectList {
		public List<Object> items;
	}

	public static class BeanWithParentArray {
		public ArrayChild[] children;
	}

	public static class ArrayChild {
		@ParentProperty
		public BeanWithParentArray parent;
		public String name;
	}

	public static class BeanWithObjectMap {
		public Map<String,Object> items;
	}

	public static class NamedChildWithParent {
		@ParentProperty
		public BeanWithNamedChildProp parent;
		public String name;
	}

	public static class BeanWithNamedChildProp {
		public NamedChildWithParent child;
	}

	@Marshalled(typeName="M23_Sub")
	public static class M23_Sub extends M23_Base {
		public int extra;
	}

	public static class M23_Base {
		public String name;
	}

	public static class ThrowingGetterBean {
		public String other;
		@SuppressWarnings({
			"unused" // Set by the parser but never read; the getter (not the field) is what's under test.
		})
		private String bad;
		public String getBad() { throw new RuntimeException("boom"); }
		public void setBad(String s) { this.bad = s; }
	}
}
