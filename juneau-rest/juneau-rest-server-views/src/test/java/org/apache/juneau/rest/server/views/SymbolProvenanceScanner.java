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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

/**
 * Test-only, <b>private</b> reader for {@code juneau-symbols.svg} and its provenance manifest
 * ({@code juneau-symbols-provenance.md}), modeled on {@link RawContentSinkScanner}'s
 * {@link RawContentSinkScanner#locateModuleRoot() locateModuleRoot()} / scan-the-module's-own-tree idiom.
 *
 * <h5 class='section'>What it is for</h5>
 * <p>
 * Every glyph in this sprite is Juneau-original, so the manifest incurs no attribution and exists only as a guard:
 * it pins the <i>approved</i> artwork so that a future paste of foreign path data over a glyph fails the build
 * until someone deliberately edits the manifest row. This class does the reading and the hashing;
 * {@link SymbolSprite_Provenance_Test} does the asserting.
 *
 * <h5 class='section'>Be honest about what a fingerprint can and cannot do</h5>
 * <p>
 * It reliably catches <b>drift</b> &mdash; artwork changing without a reviewed manifest edit. It cannot recognise
 * foreign artwork as foreign: an author who pastes a proprietary glyph <i>and</i> updates the row passes. That is
 * not a defect to be engineered around, it is the boundary of the mechanism: the guard's job is to make the paste
 * require a visible, reviewable second act rather than ride along inside an artwork commit. The manifest's
 * <i>Authoring rules</i> section is what that second act is reviewed against.
 *
 * <h5 class='section'>What the fingerprint covers, and why it is the whole element</h5>
 * <p>
 * SHA-256 over the exact UTF-8 bytes of the whole {@code <symbol ...>...</symbol>} element &mdash; opening tag
 * included, so {@code viewBox} is inside the hash. That is deliberate rather than incidental: a glyph's
 * {@code viewBox} sets the lattice its stroke centrelines snap to, because {@code juneau-icons.js} hard-codes the
 * host {@code <svg>} at {@code viewBox="0 0 24 24"} and the grid-fit lattice is defined in <i>host</i> units. A
 * glyph arriving at a foreign modulus is therefore an artwork change in every sense that matters, and hashing only
 * the path data would leave it invisible to every guard in the tree.
 *
 * <h5 class='section'>Why it reads the source tree rather than the classpath</h5>
 * <p>
 * The manifest is not a registered servable resource &mdash; only {@code juneau-symbols.svg} is (see
 * {@link ViewsMixin}) &mdash; so reading it through the resource loader would assert against a copy no consumer
 * ever fetches. Both files are read from the module's own {@code src/main/resources}, the same way
 * {@link SymbolsKey_Staleness_Test} reads the sprite and its authoring key.
 */
final class SymbolProvenanceScanner {

	static final String RESOURCE_DIR = "src/main/resources/org/apache/juneau/views";
	static final String SPRITE = "juneau-symbols.svg";
	static final String MANIFEST = "juneau-symbols-provenance.md";

	/** The id prefix every glyph carries; the stem is what follows it. */
	static final String STEM_PREFIX = "juneau-sym-";

	/** One whole {@code <symbol>} element, non-greedy so consecutive symbols do not collapse into one match. */
	private static final Pattern SYMBOL =
		Pattern.compile("<symbol\\s+id=\"" + Pattern.quote(STEM_PREFIX) + "([A-Za-z0-9_-]+)\".*?</symbol>", Pattern.DOTALL);

	/** One manifest fingerprint row: {@code | `stem` | `origin` | `hex` |}. */
	private static final Pattern MANIFEST_ROW =
		Pattern.compile("^\\|\\s*`([A-Za-z0-9_-]+)`\\s*\\|\\s*`([a-z-]+)`\\s*\\|\\s*`([0-9a-f]{64})`\\s*\\|\\s*$",
			Pattern.MULTILINE);

	/** The three document-family members whose frame path is required to be byte-identical. */
	static final List<String> FRAMED_FAMILY = List.of("csv", "pdf", "spreadsheet");

	/** A {@code d} attribute value. */
	private static final Pattern PATH_D = Pattern.compile("\\bd=\"([^\"]*)\"");

	/** A {@code fill} or {@code stroke} paint value. */
	private static final Pattern PAINT = Pattern.compile("\\b(fill|stroke)=\"([^\"]*)\"");

	/** One element of a symbol body, with its attributes. */
	private static final Pattern ELEMENT = Pattern.compile("<(\\w+)([^>]*?)/?>");

	private SymbolProvenanceScanner() {}

	//------------------------------------------------------------------------------------------------------------------
	// Reading
	//------------------------------------------------------------------------------------------------------------------

	/** Reads one file out of the module's own {@code src/main/resources}; {@code null} if the module cannot be located. */
	static String read(String fileName) throws IOException {
		var root = RawContentSinkScanner.locateModuleRoot();
		if (root == null)
			return null;
		var f = root.resolve(RESOURCE_DIR).resolve(fileName);
		return Files.isRegularFile(f) ? Files.readString(f, UTF_8) : null;
	}

	/** Stem to the verbatim bytes of its whole {@code <symbol>} element, in document order. */
	static Map<String,String> symbols(String svg) {
		var out = new LinkedHashMap<String,String>();
		var m = SYMBOL.matcher(svg);
		while (m.find())
			out.put(m.group(1), m.group());
		return out;
	}

	/** Stem to its declared {@code origin} in the manifest, in manifest order. */
	static Map<String,String> manifestOrigins(String md) {
		var out = new LinkedHashMap<String,String>();
		var m = MANIFEST_ROW.matcher(md);
		while (m.find())
			out.put(m.group(1), m.group(2));
		return out;
	}

	/** Stem to its declared fingerprint in the manifest, in manifest order. */
	static Map<String,String> manifestFingerprints(String md) {
		var out = new LinkedHashMap<String,String>();
		var m = MANIFEST_ROW.matcher(md);
		while (m.find())
			out.put(m.group(1), m.group(3));
		return out;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Derived facts
	//------------------------------------------------------------------------------------------------------------------

	/** SHA-256, lower-case hex, over the UTF-8 bytes of {@code s}. */
	static String fingerprint(String s) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(UTF_8));
			var sb = new StringBuilder(digest.length * 2);
			for (var b : digest)
				sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required of every JRE", e);
		}
	}

	/** Every {@code d} attribute value in a symbol element, in document order. */
	static List<String> pathData(String symbol) {
		var out = new ArrayList<String>();
		var m = PATH_D.matcher(symbol);
		while (m.find())
			out.add(m.group(1));
		return out;
	}

	/**
	 * The document-family frame path of a symbol: its <b>first</b> {@code d} attribute.
	 *
	 * <p>
	 * Positional rather than pattern-matched, and that is the point. The family spec declares the frame as the
	 * first path of each member, so "the first {@code d}" is the spec being read back rather than a heuristic that
	 * might latch onto an interior mark. A member that reordered its paths so the frame stopped being first would
	 * fail the byte-identity assertion, which is the correct outcome: the spec was not followed.
	 */
	static String framePath(String symbol) {
		var d = pathData(symbol);
		return d.isEmpty() ? null : d.get(0);
	}

	/** Every {@code fill}/{@code stroke} value in the sprite that is neither {@code none} nor {@code currentColor}. */
	static List<String> offContractPaints(String svg) {
		var out = new ArrayList<String>();
		var m = PAINT.matcher(svg);
		while (m.find()) {
			var v = m.group(2);
			if (!"none".equals(v) && !"currentColor".equals(v))
				out.add(m.group(1) + "=\"" + v + "\"");
		}
		return out;
	}

	/**
	 * Elements of a symbol that paint a stroke without declaring its width.
	 *
	 * <p>
	 * Returned as {@code tag attributes} strings so a failure names the offending element rather than only its
	 * glyph.
	 */
	static List<String> strokedWithoutWidth(String symbol) {
		var out = new ArrayList<String>();
		var m = ELEMENT.matcher(symbol);
		while (m.find()) {
			var tag = m.group(1);
			var attrs = m.group(2);
			if ("symbol".equals(tag))
				continue;
			if (attrs.contains("stroke=\"currentColor\"") && !attrs.contains("stroke-width=\""))
				out.add(tag + attrs.stripTrailing());
		}
		return out;
	}

	/** The {@code viewBox} an element declares, or {@code null}. */
	static String viewBox(String element) {
		var m = Pattern.compile("\\bviewBox=\"([^\"]*)\"").matcher(element);
		return m.find() ? m.group(1) : null;
	}
}
