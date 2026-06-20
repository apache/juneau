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
package org.apache.juneau.marshall.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Branch coverage tests for {@link MarkdownDocParserSession}.
 *
 * <p>
 * Covers paths not reached by {@link MarkdownDocParser_Test}.
 */
@SuppressWarnings({
	"unchecked",  // Unchecked cast required for generic test utility.
	"java:S5976",  // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class MarkdownDocParserSession_Test {

	//====================================================================================================
	// Shared bean types
	//====================================================================================================

	public static class SimpleBean {
		public String name;
		public int age;
	}

	public static class AddressBean {
		public String city;
		public String state;
	}

	public static class ContainerBean {
		public String name;
		public int age;
		public AddressBean address;
	}

	//====================================================================================================
	// a - parseDocAnything: null eType → falls back to object()
	//====================================================================================================

	@Test void a01_parseDocAnything_nullEType_treatsAsObject() {
		// When parsed to Object.class the document-mode parser dispatches through parseAnything.
		var md = """
			# Root

			| Property | Value |
			|---|---|
			| k | v |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, Object.class);
		assertNotNull(r);
	}

	//====================================================================================================
	// b - parseDocAnything: builder swap path (line 128 / 146)
	//====================================================================================================

	// Class detected as having a builder via static create() + build() pattern.
	public static class BBean {
		public String name;
		boolean builtByBuilder;

		public BBean(BBeanBuilder b) {
			if (b != null) {
				name = b.name;
				builtByBuilder = true;
			}
		}

		public static BBeanBuilder create() {
			return new BBeanBuilder();
		}
	}

	public static class BBeanBuilder {
		public String name;

		public BBean build() {
			return new BBean(this);
		}
	}

	@Test void b01_builderSwap_beanParsedViaBuilder() {
		var md = """
			# BBean

			| Property | Value |
			|---|---|
			| name | Alice |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, BBean.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertTrue(r.builtByBuilder);
	}

	//====================================================================================================
	// c - parseDocAnything: ObjectSwap path (line 130 / 149)
	//====================================================================================================

	public static class CWrapped {
		public String val;
	}

	public static class CSwap extends ObjectSwap<CWrapped, String> {
		@Override
		public String swap(MarshallingSession session, CWrapped o) {
			return o == null ? null : o.val;
		}

		@Override
		public CWrapped unswap(MarshallingSession session, String s, ClassMeta<?> hint, String attrName) {
			if (s == null) return null;
			var w = new CWrapped();
			w.val = s;
			return w;
		}
	}

	@Test void c01_objectSwapPath_topLevelType() {
		// CWrapped has a swap registered → parseDocAnything takes the swap path.
		// The swap maps CWrapped → String, so sType becomes String.
		var p = MarkdownDocParser.create().swaps(CSwap.class).build();
		var r = p.parse("hello", CWrapped.class);
		assertNotNull(r);
		assertEquals("hello", r.val);
	}

	//====================================================================================================
	// d - parseBeanFromDoc: empty root section (ne(rootLines) == false)
	//====================================================================================================

	@Test void d01_emptyRootSection_onlySubHeadings() {
		// Document has no content before the first sub-heading → root section is an empty list.
		// ne(rootLines) is false; parsing still succeeds via the sub-heading.
		var md = """
			## address

			| Property | Value |
			|---|---|
			| city | Portland |
			| state | OR |
			""";
		// headingLevel(1) → sub-headings are ## → address section maps to property.
		var p = MarkdownDocParser.create().headingLevel(1).build();
		var r = p.parse(md, ContainerBean.class);
		assertNotNull(r);
		assertNotNull(r.address);
		assertEquals("Portland", r.address.city);
		assertEquals("OR", r.address.state);
	}

	//====================================================================================================
	// e - parseBeanFromDoc: root section with no table rows (tableRows.isEmpty())
	//====================================================================================================

	@Test void e01_rootSectionNoTableRows_plainText() {
		// Root section has a heading and plain text (no | rows) → tableRows.isEmpty() is true.
		var md = """
			# Person

			Just some narrative text with no table.
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, SimpleBean.class);
		// No table rows → no properties set → defaults.
		assertNotNull(r);
		assertNull(r.name);
		assertEquals(0, r.age);
	}

	//====================================================================================================
	// f - parseBeanFromDoc: isKeyValue == false in root section (non-key-value table header)
	//====================================================================================================

	@Test void f01_rootSection_nonKeyValueTable_ignored() {
		// Root section table has headers that are NOT "Property"/"Key" and "Value".
		// isKeyValue == false → table is not processed, bean remains at defaults.
		var md = """
			# Data

			| col1 | col2 |
			|---|---|
			| foo | bar |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, SimpleBean.class);
		assertNotNull(r);
		assertNull(r.name);
	}

	//====================================================================================================
	// g - parseBeanFromDoc: pm == null (unknown key in key/value table) → onUnknownProperty
	//====================================================================================================

	@Test void g01_unknownPropertyInRootTable_ignored() {
		// Row has a key that does not match any bean property → onUnknownProperty path.
		// Use ignoreUnknownBeanProperties() so the parser silently skips the unknown key.
		var md = """
			# Data

			| Property | Value |
			|---|---|
			| name | Alice |
			| unknownProp | something |
			| age | 30 |
			""";
		var p = MarkdownDocParser.create().ignoreUnknownBeanProperties().build();
		var r = p.parse(md, SimpleBean.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// h - parseBeanFromDoc: malformed row (cells.size() < 2) in root key/value table
	//====================================================================================================

	@Test void h01_malformedRowInRootTable_skipped() {
		// A row with only one cell → cells.size() < 2 → continue.
		var md = """
			# Data

			| Property | Value |
			|---|---|
			| name | Alice |
			| badrow |
			| age | 30 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, SimpleBean.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// i - parseBeanFromDoc: unknown sub-section (pm == null for section name)
	//====================================================================================================

	@Test void i01_unknownSubSection_skipped() {
		// A sub-section whose name does not match any bean property → pm == null → skipped.
		var md = """
			# Data

			| Property | Value |
			|---|---|
			| name | Bob |
			| age | 25 |

			## unknownSection

			| Property | Value |
			|---|---|
			| irrelevant | value |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, SimpleBean.class);
		assertEquals("Bob", r.name);
		assertEquals(25, r.age);
	}

	//====================================================================================================
	// j - parseBeanFromDoc: sub-section with non-key-value table → parseTable path
	//====================================================================================================

	public static class JBean {
		public String name;
		public List<SimpleBean> items;
	}

	@Test void j01_subSection_nonKeyValueTable_toList() {
		// The sub-section "items" has a multi-column table (not key/value) → parseTable.
		var md = """
			# Container

			| Property | Value |
			|---|---|
			| name | TestJ |

			## items

			| name | age |
			|---|---|
			| Alice | 30 |
			| Bob | 25 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, JBean.class);
		assertEquals("TestJ", r.name);
		assertNotNull(r.items);
		assertEquals(2, r.items.size());
		assertEquals("Alice", r.items.get(0).name);
		assertEquals(30, r.items.get(0).age);
	}

	//====================================================================================================
	// k - parseBeanFromDoc: sub-section with bullet list content (line 256)
	//====================================================================================================

	public static class KBean {
		public String name;
		public List<String> tags;
	}

	@Test void k01_subSection_bulletList_asteriskMarker() {
		// Bullet list using * marker — exercises isBulletLine * branch in sub-section dispatch.
		var md = """
			# Report

			| Property | Value |
			|---|---|
			| name | Summary |

			## tags

			* alpha
			* beta
			* gamma
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, KBean.class);
		assertEquals("Summary", r.name);
		assertNotNull(r.tags);
		assertEquals(List.of("alpha", "beta", "gamma"), r.tags);
	}

	@Test void k02_subSection_bulletList_plusMarker() {
		// Bullet list using + marker.
		var md = """
			# Report

			| Property | Value |
			|---|---|
			| name | Summary |

			## tags

			+ one
			+ two
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, KBean.class);
		assertEquals("Summary", r.name);
		assertNotNull(r.tags);
		assertEquals(List.of("one", "two"), r.tags);
	}

	//====================================================================================================
	// l - parseBeanFromDoc: sub-section else-branch, non-bean simple type (line 260 false)
	//====================================================================================================

	public static class LBean {
		public String name;
		public String description;
	}

	@Test void l01_subSection_elseBranch_simpleType() {
		// Sub-section content is plain text (not table, not bullet list) and property type is String.
		// → propCm.isBean() == false → parseAnything path.
		var md = """
			# LBean

			| Property | Value |
			|---|---|
			| name | Test |

			## description

			Some plain text description here.
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, LBean.class);
		assertEquals("Test", r.name);
		assertNotNull(r.description);
	}

	//====================================================================================================
	// m - parseBeanFromDoc: sub-section else-branch with empty content and nested bean (line 260 true)
	//====================================================================================================

	@Test void m01_subSection_emptyContent_nestedBean() {
		// Sub-section content is empty (only blank lines).
		// contentLines.isEmpty() → else branch → propCm.isBean() → parseDocAnything.
		var md = """
			# Container

			| Property | Value |
			|---|---|
			| name | Test |

			## address

			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, ContainerBean.class);
		assertEquals("Test", r.name);
		// address is a bean, empty section → parseDocAnything with empty lines → empty Address
	}

	//====================================================================================================
	// n - parseMapFromDoc: canCreateNewInstance false → newGenericMap() (line 287)
	//====================================================================================================

	@Test void n01_parseMapFromDoc_rawMapType_usesGenericMap() {
		// Parsing to raw Map.class — canCreateNewInstance is false → newGenericMap().
		var md = """
			# Data

			| Key | Value |
			|---|---|
			| a | 1 |
			| b | 2 |
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertNotNull(r);
		assertEquals(2, r.size());
	}

	//====================================================================================================
	// o - parseMapFromDoc: keyType and valueType not null (TreeMap with typed params)
	//====================================================================================================

	@Test void o01_parseMapFromDoc_typedKeyAndValue() {
		// TreeMap<String,Integer> — getKeyType() != null and getValueType() != null.
		var md = """
			# Data

			| Property | Value |
			|---|---|
			| x | 10 |
			| y | 20 |
			""";
		var r = (TreeMap<String, Integer>) MarkdownDocParser.DEFAULT.parse(md, TreeMap.class, String.class, Integer.class);
		assertNotNull(r);
		assertEquals(10, (int) r.get("x"));
		assertEquals(20, (int) r.get("y"));
	}

	//====================================================================================================
	// p - parseMapFromDoc: no table rows in root section (line 295 false)
	//====================================================================================================

	@Test void p01_parseMapFromDoc_noTableRows_plainText() {
		// Map parsing with root section that has no | rows → tableRows.isEmpty() is true.
		var md = """
			# Data

			Just text, no table.
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	//====================================================================================================
	// q - parseMapFromDoc: non-key-value table header in root (line 301 false)
	//====================================================================================================

	@Test void q01_parseMapFromDoc_nonKeyValueHeader_ignored() {
		// Root section table with non-key-value headers → isKeyValue = false → table not processed.
		var md = """
			# Data

			| col1 | col2 |
			|---|---|
			| a | b |
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	//====================================================================================================
	// r - parseMapFromDoc: malformed row in key/value table (cells.size() < 2)
	//====================================================================================================

	@Test void r01_parseMapFromDoc_malformedRow_skipped() {
		// Row with single cell in map key/value table → cells.size() < 2 → continue.
		var md = """
			# Data

			| Key | Value |
			|---|---|
			| k1 | v1 |
			| badrow |
			| k2 | v2 |
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertEquals("v1", r.get("k1"));
		assertEquals("v2", r.get("k2"));
		assertFalse(r.containsKey("badrow"));
	}

	//====================================================================================================
	// s - parseMapFromDoc: sub-section adds entry to map (line 315-321)
	//====================================================================================================

	@Test void s01_parseMapFromDoc_withSubSection() {
		// Map parsing where sub-sections also get added as entries.
		// headingLevel(1) → sub-sections are ##.
		var md = """
			# Data

			| Key | Value |
			|---|---|
			| k1 | v1 |

			## section1

			some text
			""";
		var p = MarkdownDocParser.create().headingLevel(1).build();
		var r = (Map<String, Object>) p.parse(md, Map.class);
		assertNotNull(r);
		assertEquals("v1", r.get("k1"));
		assertTrue(r.containsKey("section1"));
	}

	//====================================================================================================
	// t - getHeadingText: line at a wrong level / extra-hash discrimination
	//====================================================================================================

	@Test void t01_getHeadingText_normalHeading_returnsText() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertEquals("Heading", session.getHeadingText("# Heading", 1));
		assertEquals("Heading", session.getHeadingText("## Heading", 2));
	}

	@Test void t02_getHeadingText_wrongLevel_returnsNull() {
		// Level-2 prefix on a level-1 heading → startsWith fails → null.
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertNull(session.getHeadingText("# Heading", 2));
	}

	@Test void t03_getHeadingText_extraHashes_returnsNull() {
		// "## Heading" tested as level 1: trimmed starts with "# " but next char is '#' → null.
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertNull(session.getHeadingText("## Heading", 1));
	}

	@Test void t04_getHeadingText_notAHeading_returnsNull() {
		// Plain text does not start with the prefix.
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertNull(session.getHeadingText("plain text", 1));
		assertNull(session.getHeadingText("", 1));
	}

	//====================================================================================================
	// u - isBulletLine: all three marker types
	//====================================================================================================

	@Test void u01_isBulletLine_dash() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertTrue(session.isBulletLine("- item"));
		assertTrue(session.isBulletLine("  - item"));
	}

	@Test void u02_isBulletLine_asterisk() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertTrue(session.isBulletLine("* item"));
		assertTrue(session.isBulletLine("  * item"));
	}

	@Test void u03_isBulletLine_plus() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertTrue(session.isBulletLine("+ item"));
		assertTrue(session.isBulletLine("  + item"));
	}

	@Test void u04_isBulletLine_notABullet() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertFalse(session.isBulletLine("not a bullet"));
		assertFalse(session.isBulletLine(""));
		assertFalse(session.isBulletLine("-nospace"));
	}

	//====================================================================================================
	// v - parseDocAnything: builder swap — builder is null skip (o == null, no builder call)
	//====================================================================================================

	// A builder whose build() returns null when name is not set.
	public static class VBean {
		public String name;

		public VBean(VBeanBuilder b) {
			if (b != null)
				name = b.name;
		}

		public static VBeanBuilder create() {
			return new VBeanBuilder();
		}
	}

	public static class VBeanBuilder {
		public String name;

		public VBean build() {
			if (name == null)
				return null;
			return new VBean(this);
		}
	}

	@Test void v01_builderSwap_withName_succeeds() {
		// Exercises builder != null && o != null branch at line 146.
		var md = """
			# VBean

			| Property | Value |
			|---|---|
			| name | Carol |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, VBean.class);
		assertNotNull(r);
		assertEquals("Carol", r.name);
	}

	//====================================================================================================
	// w - parseDocAnything: object swap with non-null result (line 149 true)
	//====================================================================================================

	public static class WWrapped {
		public String val;
	}

	public static class WSwap extends ObjectSwap<WWrapped, String> {
		@Override
		public String swap(MarshallingSession session, WWrapped o) {
			return o == null ? null : o.val;
		}

		@Override
		public WWrapped unswap(MarshallingSession session, String s, ClassMeta<?> hint, String attrName) {
			if (s == null) return null;
			var w = new WWrapped();
			w.val = s;
			return w;
		}
	}

	@Test void w01_objectSwap_nonNullResult() {
		// swap != null && o != null → unswap is called at line 150.
		var p = MarkdownDocParser.create().swaps(WSwap.class).build();
		var r = p.parse("hello", WWrapped.class);
		assertNotNull(r);
		assertEquals("hello", r.val);
	}

	//====================================================================================================
	// x - parseMapFromDoc: empty rootLines (sections.get("") returns empty list)
	//====================================================================================================

	@Test void x01_parseMapFromDoc_emptyRoot_onlySubSections() {
		// Map document where the root section has no content (no lines before sub-headings).
		// headingLevel(1) → sub-sections are ##.
		var md = """
			## key1

			val1

			## key2

			val2
			""";
		var p = MarkdownDocParser.create().headingLevel(1).build();
		var r = (Map<String, Object>) p.parse(md, Map.class);
		assertNotNull(r);
		assertTrue(r.containsKey("key1"));
		assertTrue(r.containsKey("key2"));
	}

	//====================================================================================================
	// y - segmentBySections edge cases
	//====================================================================================================

	@Test void y01_segmentBySections_noHeadings() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		var lines = List.of("line1", "line2", "line3");
		var sections = session.segmentBySections(lines, 2);
		assertEquals(1, sections.size());
		assertTrue(sections.containsKey(""));
		assertEquals(List.of("line1", "line2", "line3"), sections.get(""));
	}

	@Test void y02_segmentBySections_multipleSubHeadings() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		var lines = List.of("intro", "## sec1", "s1line", "## sec2", "s2line");
		var sections = session.segmentBySections(lines, 2);
		assertEquals(3, sections.size());
		assertTrue(sections.containsKey(""));
		assertTrue(sections.containsKey("sec1"));
		assertTrue(sections.containsKey("sec2"));
		assertEquals(List.of("intro"), sections.get(""));
		assertEquals(List.of("s1line"), sections.get("sec1"));
		assertEquals(List.of("s2line"), sections.get("sec2"));
	}

	//====================================================================================================
	// z - isHeadingLine
	//====================================================================================================

	@Test void z01_isHeadingLine_matchingLevel() {
		var session = MarkdownDocParser.DEFAULT.getSession();
		assertTrue(session.isHeadingLine("# H1", 1));
		assertTrue(session.isHeadingLine("## H2", 2));
		assertFalse(session.isHeadingLine("## H2", 1));
		assertFalse(session.isHeadingLine("plain text", 1));
	}

	//====================================================================================================
	// aa - parseMapFromDoc: 3-column table header (headers.size() != 2) → isKeyValue false
	//====================================================================================================

	@Test void aa01_parseMapFromDoc_threeColumnHeader_isKeyValueFalse() {
		// A 3-column header → headers.size() == 2 is false → isKeyValue is false → table skipped.
		var md = """
			# Data

			| col1 | col2 | col3 |
			|---|---|---|
			| a | b | c |
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	//====================================================================================================
	// ab - parseMapFromDoc: "Value" header mismatch (second header not "Value")
	//====================================================================================================

	@Test void ab01_parseMapFromDoc_secondHeaderNotValue_isKeyValueFalse() {
		// "Key | Desc" — second header is not "Value" → isKeyValue false.
		var md = """
			# Data

			| Key | Desc |
			|---|---|
			| k1 | v1 |
			""";
		var r = (Map<String, Object>) MarkdownDocParser.DEFAULT.parse(md, Map.class);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	//====================================================================================================
	// ac - parseMapFromDoc: canCreateNewInstance true (TreeMap — concrete map)
	//====================================================================================================

	@Test void ac01_parseMapFromDoc_concreteMap_canCreateNewInstance() {
		// TreeMap.canCreateNewInstance() is true → takes the first branch at line 287.
		// No type parameters → getKeyType() is null and getValueType() is null → string()/object() defaults.
		var md = """
			# Data

			| Key | Value |
			|---|---|
			| b | 2 |
			| a | 1 |
			""";
		var r = (TreeMap<?, ?>) MarkdownDocParser.DEFAULT.parse(md, TreeMap.class);
		assertNotNull(r);
		assertInstanceOf(TreeMap.class, r);
		assertEquals(2, r.size());
	}

	//====================================================================================================
	// ad - parseBeanFromDoc: sub-section content has heading lines (isHeadingLine filter)
	//====================================================================================================

	public static class ADBean {
		public String name;
		public AddressBean address;
	}

	@Test void ad01_subSection_withHeadingLine_filtered() {
		// The sub-section "address" starts with a level-2 heading and then has content.
		// The heading line is filtered out by the isHeadingLine check → parsed correctly.
		var md = """
			# ADBean

			| Property | Value |
			|---|---|
			| name | Test |

			## address

			| Property | Value |
			|---|---|
			| city | Denver |
			| state | CO |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, ADBean.class);
		assertEquals("Test", r.name);
		assertNotNull(r.address);
		assertEquals("Denver", r.address.city);
	}

	//====================================================================================================
	// ae - parseBeanFromDoc: sub-section where heading line is filtered from contentLines (line 233)
	//====================================================================================================

	@Test void ae01_subSection_headingLineFilteredFromContentLines() {
		// The section lines include the sub-heading itself (level+1 = ##).
		// That heading line is filtered by !isHeadingLine(l, level+1) → exercises the false branch at line 233.
		// The remaining content lines determine dispatch: here it is a key/value table.
		var md = """
			# ADBean

			| Property | Value |
			|---|---|
			| name | Test |

			## address

			| Property | Value |
			|---|---|
			| city | Denver |
			| state | CO |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, ADBean.class);
		assertEquals("Test", r.name);
		assertNotNull(r.address);
		assertEquals("Denver", r.address.city);
		assertEquals("CO", r.address.state);
	}

	//====================================================================================================
	// af - parseBeanFromDoc: sub-section key/value table with non-bean property → parseTable
	//====================================================================================================

	public static class AEBean {
		public String name;
		public Map<String, String> props;
	}

	@Test void af01_subSection_keyValueTable_nonBeanProp_parseTable() {
		// Sub-section "props" has a key/value table but propCm is Map (not bean) → parseTable path.
		var md = """
			# AEBean

			| Property | Value |
			|---|---|
			| name | Test |

			## props

			| Property | Value |
			|---|---|
			| p1 | v1 |
			| p2 | v2 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, AEBean.class);
		assertEquals("Test", r.name);
		// props is a Map (non-bean) → parseTable path
		assertNotNull(r.props);
	}
}
