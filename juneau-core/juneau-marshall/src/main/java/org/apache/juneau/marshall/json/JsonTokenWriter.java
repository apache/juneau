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
package org.apache.juneau.marshall.json;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.math.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenWriter} surface for the JSON format.
 *
 * <p>
 * Emits RFC-8259-compliant JSON.  Output style is driven by a small {@link Settings} bundle that
 * mirrors the structurally-applicable subset of {@link JsonSerializer.Builder}'s formatting flags:
 * <c>useWhitespace</c>, <c>maxIndent</c>, <c>quoteChar</c>, <c>escapeSolidus</c>, and
 * <c>trimStrings</c>.  When opened via {@link JsonSerializer#writeTokens(Object)} the writer
 * inherits the settings from the calling serializer; constructing the writer directly via
 * {@link #JsonTokenWriter(Writer)} uses {@link Settings#DEFAULT} (compact, double-quoted, RFC-8259
 * strict).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The writer tracks just enough state to insert structural separators (<c>,</c> between
 * 		array/object members and <c>:</c> between field names and values).  It does not validate
 * 		event ordering &mdash; emitting <c>field</c> twice in a row, for instance, throws.
 * 	<li>The writer is purely structural; object swaps and {@code @Schema} annotations have no
 * 		effect.  Use the POJO databind path on {@link JsonSerializer} for those.
 * </ul>
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity acceptable for JSON encoder dispatch
	"resource"    // The writer's underlying Writer/OutputStream is owned by the caller via try-with-resources on the writer itself; Eclipse JDT flags the inner stream as unclosed but that's by design.
})
public class JsonTokenWriter implements TokenWriter {

	/**
	 * Output-formatting settings honored by {@link JsonTokenWriter}.
	 *
	 * <p>
	 * This is the structurally-applicable subset of {@link JsonSerializer.Builder}.  Properties
	 * that operate on the POJO path (<c>addBeanTypes</c>, <c>keepNullProperties</c>,
	 * <c>trimEmptyMaps</c>, <c>trimEmptyCollections</c>, <c>sortMaps</c>, <c>sortCollections</c>,
	 * object swaps, and {@code @Schema} annotations) have no effect on the token surface and are
	 * deliberately not represented here.
	 *
	 * @param useWhitespace If <jk>true</jk>, emit indented output with newlines.
	 * @param maxIndent Maximum indentation depth when <c>useWhitespace</c> is enabled.  Levels
	 * 	beyond this depth are emitted compactly.
	 * @param quoteChar The quote character used for strings and field names.  Typically
	 * 	<c>'"'</c> (RFC-8259) but may be <c>'\''</c> for JSON5-style output.
	 * @param escapeSolidus If <jk>true</jk>, forward slashes (<c>/</c>) are escaped as <c>\/</c>.
	 * @param trimStrings If <jk>true</jk>, leading/trailing whitespace is trimmed from emitted
	 * 	{@link TokenType#VALUE_STRING} and {@link TokenType#FIELD_NAME} values.
	 * @param simpleAttrs If <jk>true</jk>, field names are emitted unquoted when they are valid
	 * 	JavaScript identifiers and not reserved words.  This is the JSON5 "lax attribute" mode;
	 * 	strict RFC-8259 JSON keeps this <jk>false</jk>.
	 * @param walk The {@link PojoWalker.Options walk options} used by
	 * 	{@link #object(Object) object(Object)} when walking a value graph.
	 * @param disableObject If <jk>true</jk>, {@link #object(Object) object(Object)} throws
	 * 	{@link UnsupportedOperationException} with a format-specific message.  Used by formats
	 * 	whose {@code serialize()} produces non-standard JSON (e.g. {@link JsonSchemaSerializer},
	 * 	{@link org.apache.juneau.marshall.jcs.JcsSerializer}) where the cursor's POJO walker would
	 * 	emit ordinary JSON instead of the format's special form.
	 */
	@SuppressWarnings("javadoc")
	public record Settings(
			boolean useWhitespace,
			int maxIndent,
			char quoteChar,
			boolean escapeSolidus,
			boolean trimStrings,
			boolean simpleAttrs,
			PojoWalker.Options walk,
			boolean disableObject) {

		/** Default canonical setting: compact RFC-8259 JSON, double-quoted, no trimming. */
		public static final Settings DEFAULT = new Settings(false, 100, '"', false, false, false, PojoWalker.Options.DEFAULT, false);
	}

	private static final int CTX_ROOT = 0;
	private static final int CTX_OBJECT = 1;
	private static final int CTX_ARRAY = 2;

	private final Writer out;
	private final Closeable owned;
	private final boolean autoFlush;
	private final Settings settings;

	// Container stack (root, object, array): bit-packed so 64 levels are allocation-free.
	private long ctxBits;
	private int[] ctxOverflow;
	private int depth;

	// True if the next emit inside the current container is the first member (no leading comma).
	private boolean firstInContainer = true;

	// True if the previous structural event was field(), meaning the next emit MUST be a
	// value (no leading comma) and must NOT trigger the first/comma logic.
	private boolean awaitingFieldValue;

	private boolean closed;

	/**
	 * Constructor for a {@link Writer} target with default {@link Settings}.
	 *
	 * @param out The writer to emit JSON to.  Must not be <jk>null</jk>.
	 */
	public JsonTokenWriter(Writer out) {
		this(out, null, false, Settings.DEFAULT);
	}

	/**
	 * Constructor for a {@link Writer} target with explicit {@link Settings}.
	 *
	 * @param out The writer to emit JSON to.  Must not be <jk>null</jk>.
	 * @param settings The output-formatting settings.  Must not be <jk>null</jk>.
	 */
	public JsonTokenWriter(Writer out, Settings settings) {
		this(out, null, false, settings);
	}

	/**
	 * Internal constructor used by {@link JsonSerializer#writeTokens(Object)} factory paths to
	 * record an underlying {@link Closeable} the writer should close on shutdown (e.g. a file
	 * stream).
	 *
	 * @param out The writer to emit JSON to.
	 * @param owned A resource owned by this writer that should be closed on close, or
	 * 	<jk>null</jk>.
	 * @param autoFlush If <jk>true</jk>, flushes after every top-level emit.
	 * @param settings The output-formatting settings.
	 */
	JsonTokenWriter(Writer out, Closeable owned, boolean autoFlush, Settings settings) {
		assertArgNotNull("out", out);
		assertArgNotNull("settings", settings);
		this.out = out;
		this.owned = owned;
		this.autoFlush = autoFlush;
		this.settings = settings;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter startObject() throws IOException {
		assertOpen();
		preValue();
		out.write('{');
		pushContext(CTX_OBJECT);
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter endObject() throws IOException {
		assertOpen();
		var wasEmpty = firstInContainer;
		popContext(CTX_OBJECT);
		if (settings.useWhitespace && !wasEmpty)
			writeNewlineAndIndent();
		out.write('}');
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter startArray() throws IOException {
		assertOpen();
		preValue();
		out.write('[');
		pushContext(CTX_ARRAY);
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter endArray() throws IOException {
		assertOpen();
		var wasEmpty = firstInContainer;
		popContext(CTX_ARRAY);
		if (settings.useWhitespace && !wasEmpty)
			writeNewlineAndIndent();
		out.write(']');
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter fieldName(String name) throws IOException {
		assertOpen();
		assertArgNotNull("name", name);
		if (currentContext() != CTX_OBJECT)
			throw new IllegalStateException("field called outside an object");
		if (awaitingFieldValue)
			throw new IllegalStateException("field called twice without an intervening value");
		if (!firstInContainer)
			out.write(',');
		if (settings.useWhitespace)
			writeNewlineAndIndent();
		var trimmed = maybeTrim(name);
		if (settings.simpleAttrs && isSafeBareIdentifier(trimmed))
			out.write(trimmed);
		else
			writeQuotedString(trimmed);
		out.write(':');
		if (settings.useWhitespace)
			out.write(' ');
		firstInContainer = false;
		awaitingFieldValue = true;
		return this;
	}

	/**
	 * Returns whether the given string is safe to emit as a bare (unquoted) JSON5 field name.
	 * Conservative: requires letters / digits / underscore only, must not start with a digit,
	 * and must not be one of a small set of JavaScript reserved words.
	 */
	private static boolean isSafeBareIdentifier(String s) {
		if (ie(s))
			return false;
		var first = s.charAt(0);
		if (!(Character.isLetter(first) || first == '_'))
			return false;
		for (var i = 1; i < s.length(); i++) {
			var c = s.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '_'))
				return false;
		}
		return !RESERVED_WORDS.contains(s);
	}

	private static final java.util.Set<String> RESERVED_WORDS = java.util.Set.of(
		"true", "false", "null", "undefined",
		"break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
		"do", "else", "enum", "export", "extends", "finally", "for", "function", "if", "import",
		"in", "instanceof", "new", "return", "super", "switch", "this", "throw", "try", "typeof",
		"var", "void", "while", "with", "yield", "let", "static", "implements", "interface",
		"package", "private", "protected", "public");

	@Override /* Overridden from TokenWriter */
	public TokenWriter string(String value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValue();
		writeQuotedString(maybeTrim(value));
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter number(Number value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValue();
		out.write(value.toString());
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter number(long value) throws IOException {
		assertOpen();
		preValue();
		out.write(Long.toString(value));
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter number(double value) throws IOException {
		assertOpen();
		if (Double.isNaN(value) || Double.isInfinite(value))
			throw new IOException("Cannot serialize non-finite double as JSON: " + value);
		preValue();
		out.write(Double.toString(value));
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter number(BigDecimal value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValue();
		out.write(value.toString());
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter number(BigInteger value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		preValue();
		out.write(value.toString());
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter bool(boolean value) throws IOException {
		assertOpen();
		preValue();
		out.write(value ? "true" : "false");
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter nil() throws IOException {
		assertOpen();
		preValue();
		out.write("null");
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter object(Object value) throws IOException {
		assertOpen();
		if (settings.disableObject)
			throw new UnsupportedOperationException(
				"object(Object) is not supported on this writer because the format's serialize() " +
				"produces non-standard JSON (e.g. schema generation, canonical form).  Use the raw " +
				"structural methods (startObject / field / string / ...) directly, or call the " +
				"format's serialize() method.");
		PojoWalker.walk(this, value, settings.walk);
		return this;
	}

	/**
	 * Returns the {@link Settings} this writer was configured with.
	 *
	 * <p>
	 * Exposed for delegating wrappers (e.g. {@link org.apache.juneau.marshall.jsonl.JsonlTokenWriter})
	 * that need to read the underlying writer's walk options when implementing {@link #object(Object)}.
	 *
	 * @return The settings.  Never <jk>null</jk>.
	 */
	public Settings settingsAccess() {
		return settings;
	}

	@Override /* Overridden from TokenWriter */
	public TokenWriter binary(byte[] value) throws IOException {
		assertOpen();
		if (value == null)
			return nil();
		// JSON has no native binary type.  Encode as base64-encoded string per common convention.
		preValue();
		writeQuotedString(java.util.Base64.getEncoder().encodeToString(value));
		postValue();
		return this;
	}

	@Override /* Overridden from TokenWriter */
	public void flush() throws IOException {
		out.flush();
	}

	@Override /* Overridden from TokenWriter */
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		try {
			out.flush();
		} finally {
			if (owned != null)
				owned.close();
		}
	}

	// =================================================================================
	// Helpers
	// =================================================================================

	/**
	 * Throws an {@link IOException} if this writer has been {@link #close() closed}.
	 *
	 * @throws IOException If this writer is closed.
	 */
	private void assertOpen() throws IOException {
		if (closed)
			throw new IOException("Token writer is closed.");
	}

	/**
	 * Called immediately before emitting any scalar / container-start.  Inserts a leading comma
	 * if this is not the first member of an array, and clears the field-name-awaiting flag if a
	 * field name preceded the value.
	 */
	private void preValue() throws IOException {
		if (awaitingFieldValue) {
			awaitingFieldValue = false;
			return;
		}
		if (currentContext() == CTX_ARRAY) {
			if (!firstInContainer)
				out.write(',');
			if (settings.useWhitespace)
				writeNewlineAndIndent();
		}
		firstInContainer = false;
	}

	/** Called after emitting any scalar / container-end so subsequent emits see correct state. */
	private void postValue() throws IOException {
		if (autoFlush && currentContext() == CTX_ROOT)
			out.flush();
	}

	private void writeNewlineAndIndent() throws IOException {
		out.write('\n');
		// Indent two spaces per level up to maxIndent; deeper levels emit no extra indent so the
		// output stays usable on pathologically deep documents.
		var levels = Math.min(depth, settings.maxIndent);
		for (var i = 0; i < levels; i++)
			out.write("  ");
	}

	private String maybeTrim(String s) {
		if (!settings.trimStrings || s == null)
			return s;
		return s.trim();
	}

	private void pushContext(int kind) {
		if (depth < 32) {
			// Two bits per level: 00 = root, 01 = object, 10 = array.
			ctxBits |= (kind & 0x3L) << (depth * 2);
		} else {
			if (ctxOverflow == null || depth - 32 >= ctxOverflow.length)
				growOverflow();
			ctxOverflow[depth - 32] = kind;
		}
		depth++;
		firstInContainer = true;
	}

	private void popContext(int expectedKind) {
		if (depth == 0)
			throw new IllegalStateException("end-container called with no matching start-container");
		var actual = currentContext();
		if (actual != expectedKind)
			throw new IllegalStateException(
				"end-container kind mismatch: expected " + nameOfContext(expectedKind) +
				", got " + nameOfContext(actual));
		// Clear the bits at the popped level so nested counts stay correct.
		var top = depth - 1;
		if (top < 32)
			ctxBits &= ~(0x3L << (top * 2));
		depth--;
		// After closing a container, the next emit at the new depth is no longer the first member.
		firstInContainer = false;
		awaitingFieldValue = false;
	}

	private int currentContext() {
		if (depth == 0)
			return CTX_ROOT;
		var top = depth - 1;
		if (top < 32)
			return (int) ((ctxBits >>> (top * 2)) & 0x3L);
		return ctxOverflow[top - 32];
	}

	private static String nameOfContext(int kind) {
		switch (kind) {
			case CTX_ROOT:   return "root";
			case CTX_OBJECT: return "object";
			case CTX_ARRAY:  return "array";
			default:         return "unknown";
		}
	}

	private void growOverflow() {
		var n = ctxOverflow == null ? 16 : ctxOverflow.length << 1;
		var bigger = new int[n];
		if (ctxOverflow != null)
			System.arraycopy(ctxOverflow, 0, bigger, 0, ctxOverflow.length);
		ctxOverflow = bigger;
	}

	private void writeQuotedString(String s) throws IOException {
		var q = settings.quoteChar;
		out.write(q);
		// Escape: the active quote char, backslash, and control chars 0x00-0x1F.  Optionally
		// escape forward slashes when settings.escapeSolidus is set.
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == q) {
				out.write('\\');
				out.write(q);
				continue;
			}
			switch (c) {
				case '\\': out.write("\\\\"); break;
				case '\b': out.write("\\b"); break;
				case '\f': out.write("\\f"); break;
				case '\n': out.write("\\n"); break;
				case '\r': out.write("\\r"); break;
				case '\t': out.write("\\t"); break;
				case '/':
					if (settings.escapeSolidus)
						out.write("\\/");
					else
						out.write(c);
					break;
				default:
					if (c < 0x20)
						out.write(String.format("\\u%04x", (int) c));
					else
						out.write(c);
			}
		}
		out.write(q);
	}

	// =================================================================================
	// Static factory used by JsonSerializer.writeTokens(Object)
	// =================================================================================

	/**
	 * Internal factory used by {@link JsonSerializer#writeTokens(Object)}.  Coerces the supported
	 * output types ({@link Writer} / {@link OutputStream}) into a {@link Writer} the JSON encoder can
	 * target.
	 *
	 * @param output The output object.
	 * @param settings The output-formatting settings.
	 * @return A new {@link JsonTokenWriter} writing to the resolved {@link Writer}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	public static JsonTokenWriter forOutput(Object output, Settings settings) throws IOException {
		if (output == null)
			throw new IOException("Output cannot be null.");
		if (output instanceof Writer w)
			return new JsonTokenWriter(w, null, false, settings);
		if (output instanceof OutputStream os)
			return new JsonTokenWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), null, true, settings);
		throw new IOException("Cannot convert object of type " + output.getClass().getName() + " to a Writer.");
	}
}
