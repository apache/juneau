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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.ExecutableException;
import org.apache.juneau.parser.*;
import org.apache.juneau.commons.bean.BeanMap;

/**
 * Session for parsing HOCON format into POJOs.
 */
@SuppressWarnings({
	"java:S110", "java:S115", "java:S135", "java:S3776", "java:S6541", "unchecked"
	// java:S135: parseValueOrConcat uses two distinct breaks — one for standard HOCON terminators, one for unquoted keys after arrays/objects; merging them would obscure intent
})
public class HoconParserSession extends ReaderParserSession {

	private static final String ARG_ctx = "ctx";
	private static final String[] EMPTY_PATH = new String[0];

	/**
	 * Builder for HOCON parser session.
	 */
	public static class Builder extends ReaderParserSession.Builder {

		private HoconParser hoconParser;

		protected Builder(HoconParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.hoconParser = ctx;
		}

		@Override
		public HoconParserSession build() {
			return new HoconParserSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The parser context.
	 * @return The builder.
	 */
	public static Builder create(HoconParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final HoconParser hoconParser;

	protected HoconParserSession(Builder builder) {
		super(builder);
		hoconParser = builder.hoconParser;
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		String inputStr;
		try {
			inputStr = pipe.asString();
		} catch (IOException e) {
			throw new ParseException(e);
		}
		if (inputStr == null)
			return null;
		var trimmed = inputStr.trim();
		if (trimmed.isEmpty())
			return null;
		var tokenizer = new HoconTokenizer(new StringReader(inputStr));
		var root = parseRoot(tokenizer);
		if (root == null)
			return null;
		if (hoconParser.resolveSubstitutions) {
			var resolver = new HoconResolver(root);
			resolver.resolve();
		}
		var map = hoconToMap(root, type);
		return (T) convertToBean(map, type);
	}

	private HoconValue.HoconObject parseRoot(HoconTokenizer t) throws IOException, ParseException {
		t.skipWhitespaceAndComments();
		var peek = t.peek();
		if (peek.type() == HoconTokenizer.TokenType.EOF)
			return null;
		if (peek.type() == HoconTokenizer.TokenType.LBRACE) {
			t.read();
			return parseObject(t, null, new String[0]);
		}
		return parseRootBraceless(t);
	}

	private HoconValue.HoconObject parseRootBraceless(HoconTokenizer t) throws IOException, ParseException {
		var root = new HoconValue.HoconObject();
		while (t.peek().type() != HoconTokenizer.TokenType.EOF) {
			var path = readPath(t);
			if (path == null || path.length == 0)
				break;
			t.skipWhitespaceAndComments();
			var next = t.peek();
			if (next.type() == HoconTokenizer.TokenType.LBRACE) {
				t.read();
				var nested = parseObject(t, root, path);
				var existing = root.getPath(path);
				if (existing instanceof HoconValue.HoconObject existingObj)
					existingObj.merge(nested);
				else
					root.setPath(path, nested);
			} else if (next.type() == HoconTokenizer.TokenType.PLUS_EQUALS) {
				t.read();
				t.skipWhitespaceAndComments();
				var value = parseValueOrConcat(t);
				var existing = root.getPath(path);
				if (existing instanceof HoconValue.HoconArray arr)
					arr.getElements().add(value);
				else {
					var newArr = new HoconValue.HoconArray();
					if (existing != null)
						newArr.getElements().add(existing);
					newArr.getElements().add(value);
					root.setPath(path, newArr);
				}
			} else if (next.type() == HoconTokenizer.TokenType.EQUALS || next.type() == HoconTokenizer.TokenType.COLON) {
				t.read();
				t.skipWhitespaceAndComments();
				var value = parseValueOrConcat(t);
				value = resolveSelfRefConcatIfNeeded(value, path, root);
				root.setPath(path, value);
			} else {
				throw new ParseException(this, "Expected =, : or brace at line {0}", t.getLine());
			}
			t.skipWhitespaceAndComments();
			if (t.peek().type() == HoconTokenizer.TokenType.NEWLINE)
				t.read();
		}
		return root;
	}

	private String[] readPath(HoconTokenizer t) throws IOException, ParseException {
		var tok = t.read();
		String first = switch (tok.type()) {
			case EOF -> null;
			case UNQUOTED_STRING -> isTrimStrings() ? tok.stringValue().trim() : tok.stringValue();
			case QUOTED_STRING, TRIPLE_QUOTED -> isTrimStrings() ? tok.stringValue().trim() : tok.stringValue();
			case NULL -> null;
			case TRUE -> "true";
			case FALSE -> "false";
			case NUMBER -> tok.numberValue().toString();
			default -> throw new ParseException(this, "Expected key at line {0}", t.getLine());
		};
		if (first == null)
			return EMPTY_PATH;
		var path = new ArrayList<String>();
		if (tok.type() == HoconTokenizer.TokenType.UNQUOTED_STRING && first.contains(".") && !first.startsWith("."))
			path.addAll(Arrays.asList(first.split("\\.", -1)));
		else {
			var comp = first.startsWith(".") ? first.substring(1) : first;
			if (!comp.isEmpty())
				path.add(comp);
			t.skipWhitespaceAndComments();
			while (t.peek().type() == HoconTokenizer.TokenType.UNQUOTED_STRING
					|| t.peek().type() == HoconTokenizer.TokenType.QUOTED_STRING
					|| t.peek().type() == HoconTokenizer.TokenType.TRIPLE_QUOTED) {
				var next = t.read().stringValue();
				var part = next.startsWith(".") ? next.substring(1) : next;
				if (!part.isEmpty())
					path.add(part);
				t.skipWhitespaceAndComments();
			}
		}
		return path.toArray(String[]::new);
	}

	/**
	 * Parses a value, supporting HOCON concatenation: adjacent values on the same line
	 * are concatenated (strings) or merged (objects/arrays).
	 */
	private HoconValue parseValueOrConcat(HoconTokenizer t) throws IOException, ParseException {
		var values = new ArrayList<HoconValue>();
		values.add(parseValue(t));
		while (true) {
			t.skipWhitespaceAndCommentsExceptNewlines();
			var peek = t.peekNoSkip();
			if (peek.type() == HoconTokenizer.TokenType.NEWLINE
					|| peek.type() == HoconTokenizer.TokenType.COMMA
					|| peek.type() == HoconTokenizer.TokenType.RBRACE
					|| peek.type() == HoconTokenizer.TokenType.RBRACKET
					|| peek.type() == HoconTokenizer.TokenType.EQUALS
					|| peek.type() == HoconTokenizer.TokenType.PLUS_EQUALS
					|| peek.type() == HoconTokenizer.TokenType.EOF)
				break;
			// HOCON: newline ends concatenation; unquoted string after array/object is next key, not concat
			var last = values.isEmpty() ? null : values.get(values.size() - 1);
			if (peek.type() == HoconTokenizer.TokenType.UNQUOTED_STRING
					&& (last instanceof HoconValue.HoconArray || last instanceof HoconValue.HoconObject))
				break;
			values.add(parseValue(t));
		}
		if (values.size() == 1)
			return values.get(0);
		return combineConcat(values);
	}

	private static HoconValue combineConcat(List<HoconValue> values) {
		if (values.size() == 1)
			return values.get(0);
		var allArrays = values.stream().allMatch(v -> v instanceof HoconValue.HoconArray);
		var allObjects = values.stream().allMatch(v -> v instanceof HoconValue.HoconObject);
		if (allArrays) {
			var first = (HoconValue.HoconArray) values.get(0);
			for (var i = 1; i < values.size(); i++)
				first.concat((HoconValue.HoconArray) values.get(i));
			return first;
		}
		if (allObjects) {
			var first = (HoconValue.HoconObject) values.get(0);
			for (var i = 1; i < values.size(); i++)
				first.merge((HoconValue.HoconObject) values.get(i));
			return first;
		}
		var concat = new HoconValue.HoconConcat();
		for (var v : values)
			concat.add(v);
		return concat;
	}

	private static String[] concatenatePaths(String[] prefix, String[] path) {
		var result = new String[prefix.length + path.length];
		System.arraycopy(prefix, 0, result, 0, prefix.length);
		System.arraycopy(path, 0, result, prefix.length, path.length);
		return result;
	}

	private static HoconValue resolveSelfRefConcatIfNeeded(HoconValue value, String[] fullPath,
			HoconValue.HoconObject root) throws ParseException {
		if (!(value instanceof HoconValue.HoconConcat concat))
			return value;
		var pathStr = String.join(".", fullPath);
		if (!concat.referencesPath(pathStr))
			return value;
		var existing = root.getPath(fullPath);
		if (existing == null)
			return value;
		var resolver = new HoconResolver(root);
		java.util.function.Function<String, HoconValue> lookup = p -> p.equals(pathStr) ? existing : resolver.lookup(p);
		return resolver.resolveConcatWithLookup(concat, lookup);
	}

	private HoconValue parseValue(HoconTokenizer t) throws IOException, ParseException {
		var tok = t.peek();
		return switch (tok.type()) {
			case UNQUOTED_STRING, QUOTED_STRING, TRIPLE_QUOTED -> {
				t.read();
				var s = isTrimStrings() ? tok.stringValue().trim() : tok.stringValue();
				yield new HoconValue.HoconString(s);
			}
			case NUMBER -> {
				t.read();
				yield new HoconValue.HoconNumber(tok.numberValue());
			}
			case TRUE -> {
				t.read();
				yield new HoconValue.HoconBoolean(true);
			}
			case FALSE -> {
				t.read();
				yield new HoconValue.HoconBoolean(false);
			}
			case NULL -> {
				t.read();
				yield new HoconValue.HoconNull();
			}
			case LBRACE -> {
				t.read();
				yield parseObject(t, null, new String[0]);
			}
			case LBRACKET -> {
				t.read();
				yield parseArray(t);
			}
			case SUBSTITUTION, OPT_SUBSTITUTION -> {
				var st = t.read();
				yield new HoconValue.HoconSubstitution(st.stringValue(), tok.type() == HoconTokenizer.TokenType.OPT_SUBSTITUTION);
			}
			case EOF -> null;
			default -> throw new ParseException(this, "Unexpected token {0} at line {1}", tok.type(), t.getLine());
		};
	}

	private HoconValue.HoconObject parseObject(HoconTokenizer t, HoconValue.HoconObject root, String[] pathPrefix)
			throws IOException, ParseException {
		var obj = new HoconValue.HoconObject();
		var effectiveRoot = root != null ? root : obj;
		t.skipWhitespaceAndComments();
		while (t.peek().type() != HoconTokenizer.TokenType.RBRACE && t.peek().type() != HoconTokenizer.TokenType.EOF) {
			var path = readPath(t);
			if (path == null || path.length == 0)
				break;
			t.skipWhitespaceAndComments();
			var next = t.peek();
			if (next.type() == HoconTokenizer.TokenType.LBRACE) {
				t.read();
				var nested = parseObject(t, effectiveRoot, concatenatePaths(pathPrefix, path));
				var existing = obj.getPath(path);
				if (existing instanceof HoconValue.HoconObject existingObj)
					existingObj.merge(nested);
				else
					obj.setPath(path, nested);
			} else if (next.type() == HoconTokenizer.TokenType.PLUS_EQUALS) {
				t.read();
				t.skipWhitespaceAndComments();
				var value = parseValueOrConcat(t);
				var existing = obj.getPath(path);
				if (existing instanceof HoconValue.HoconArray arr)
					arr.getElements().add(value);
				else {
					var newArr = new HoconValue.HoconArray();
					if (existing != null)
						newArr.getElements().add(existing);
					newArr.getElements().add(value);
					obj.setPath(path, newArr);
				}
			} else if (next.type() == HoconTokenizer.TokenType.EQUALS || next.type() == HoconTokenizer.TokenType.COLON) {
				t.read();
				t.skipWhitespaceAndComments();
				var value = parseValueOrConcat(t);
				var fullPath = concatenatePaths(pathPrefix, path);
				value = resolveSelfRefConcatIfNeeded(value, fullPath, effectiveRoot);
				obj.setPath(path, value);
			} else {
				throw new ParseException(this, "Expected =, : or brace at line {0}", t.getLine());
			}
			// skipWhitespaceAndComments is a no-op while a token is cached (see HoconTokenizer), so a
			// closing `}` already peeked by parseValueOrConcat's concat loop is preserved here and the
			// loop terminates naturally via the while-condition above rather than chewing through a
			// newline that belongs to an enclosing scope.
			t.skipWhitespaceAndComments();
			if (t.peek().type() == HoconTokenizer.TokenType.COMMA || t.peek().type() == HoconTokenizer.TokenType.NEWLINE)
				t.read();
			t.skipWhitespaceAndComments();
		}
		if (t.peek().type() == HoconTokenizer.TokenType.RBRACE)
			t.read();
		return obj;
	}

	private HoconValue.HoconArray parseArray(HoconTokenizer t) throws IOException, ParseException {
		var arr = new HoconValue.HoconArray();
		t.skipWhitespaceAndComments();
		while (t.peek().type() != HoconTokenizer.TokenType.RBRACKET && t.peek().type() != HoconTokenizer.TokenType.EOF) {
			// Elements inside [ ... ] are separated by COMMA or NEWLINE (handled below), so each
			// parseValueOrConcat call returns a single element.  HOCON array-concatenation
			// (`[a,b] [c,d]` ≡ `[a,b,c,d]`) is performed inside parseValueOrConcat when adjacent
			// arrays appear without a separator; the result is already a flattened HoconArray at
			// that point.  Here we must add the element as-is so nested arrays like
			// `[[1,2,3], [4,5,6]]` (with separators) stay nested rather than flattening.
			arr.getElements().add(parseValueOrConcat(t));
			// parseValueOrConcat's internal concat loop calls peekNoSkip(), which eagerly consumes the
			// closing-bracket char from the underlying reader and stashes it as peeked=RBRACKET.
			// skipWhitespaceAndComments is a no-op while a token is cached (see HoconTokenizer), so it
			// will NOT read past the cached `]` and eat a following newline — a newline that is either
			// the in-array separator for the NEXT element of an enclosing array or a meaningful boundary
			// for an outer scope.  This prevents adjacent newline-separated inner arrays like
			// `[[1,2]\n[3,4]]` from being silently re-merged via HOCON array-concatenation above us.
			// The loop terminates on the cached `]` via the while-condition above.
			t.skipWhitespaceAndComments();
			if (t.peek().type() == HoconTokenizer.TokenType.COMMA || t.peek().type() == HoconTokenizer.TokenType.NEWLINE)
				t.read();
			t.skipWhitespaceAndComments();
		}
		if (t.peek().type() == HoconTokenizer.TokenType.RBRACKET)
			t.read();
		return arr;
	}

	private Object hoconToMap(HoconValue val, ClassMeta<?> type) throws ParseException {
		if (val == null)
			return null;
		// Bug #12: STRING values targeting byte[] consult the configured BinaryFormat's variant
		// BinarySwap before falling back to the default String → byte[] UTF-8 coercion that the bean
		// binder would otherwise apply at the collection-element / top-level dispatch site.  The
		// bean-property path is unaffected because the per-property MPP install hides the type-level
		// default swap behind the per-property swap on the BeanPropertyMeta.
		//
		// Bug #11/#12 a04 residual: at BinaryFormat.NOT_SET BinarySwap.match returns 0 and no swap
		// fires, but HoconSerializerSession emits byte[] at NOT_SET as a base64 string (to sidestep
		// HOCON's array-concatenation flattening for nested int-array wire forms).  Decode that
		// here as the symmetric fallback so List<byte[]> and top-level byte[] round-trip at NOT_SET.
		if (val.getType() == HoconValue.Type.STRING && type != null && type.inner() == byte[].class) {
			var s = ((HoconValue.HoconString) val).getValue();
			var swap = type.getSwap(this);
			if (swap != null)
				return unswap(swap, s, type);
			return Base64.getDecoder().decode(s);
		}
		// Look up bean property metas so nested typed-map properties (Map<K,V> with non-String K) can have
		// their keys coerced via the converter's Map→Map path before the bean binds them (Bug #7b).
		// Extended in Bug #12 to also surface Collection/Array property types so the ARRAY branch can
		// thread the parent's element type into recursion and the byte[] swap dispatch above can fire
		// at the right depth.  Other property shapes (Optional, primitive, bean) still pass through as
		// object() so the lazy-typed conversion path stays in place.
		var beanMeta = (type != null && type.isBean()) ? type.getBeanMeta() : null;
		var result = switch (val.getType()) {
			case STRING -> ((HoconValue.HoconString) val).getValue();
			case NUMBER -> ((HoconValue.HoconNumber) val).getValue();
			case BOOLEAN -> ((HoconValue.HoconBoolean) val).getValue();
			case NULL -> null;
			case OBJECT -> {
				var obj = (HoconValue.HoconObject) val;
				var map = newGenericMap();
				for (var e : obj.getMembers().entrySet()) {
					var key = e.getKey();
					ClassMeta<?> childType = object();
					if (beanMeta != null && key != null && !key.equals(getBeanTypePropertyName(type))) {
						var pMeta = beanMeta.getPropertyMeta(key);
						if (pMeta != null) {
							var cm = (ClassMeta<?>) pMeta.getBeanInfo();
							if (cm != null && (cm.isMap() || cm.isCollectionOrArray()))
								childType = cm;
						}
					}
					map.put(key, hoconToMap(e.getValue(), childType));
				}
				yield map;
			}
			case ARRAY -> {
				var arr = (HoconValue.HoconArray) val;
				var list = newGenericList();
				var elType = (type != null && type.isCollectionOrArray()) ? def(type.getElementType(), object()) : object();
				for (var el : arr.getElements()) {
					list.add(hoconToMap(el, elType));
				}
				yield list;
			}
			default -> null;
		};
		// If the enclosing target is a typed Map (e.g. Map<TestEnum,String>), coerce keys / values
		// via the converter's findMapConversion so the bean property binder gets a properly-keyed map.
		if (result instanceof Map<?,?> m && type != null && type.isMap()
				&& type.getKeyType() != null && !type.getKeyType().isObject())
			return convertToMemberType(null, m, type);
		return result;
	}

	private Object convertToBean(Object map, ClassMeta<?> type) throws ExecutableException, ParseException {
		if (map == null)
			return null;
		if (type == null)
			type = object();
		var casted = cast((MarshalledMap) map, null, type);
		if (casted != map)
			return casted;
		if (!type.isObject()) {
			var swap = type.getSwap(this);
			if (nn(swap)) {
				var sType = swap.getSwapClassMeta(this);
				var swapped = convertToMemberType(null, map, sType);
				return unswap(swap, swapped, type);
			}
			var result = convertToMemberType(null, map, type);
			if (type.isBean() && result != null)
				injectAnnotations((MarshalledMap) map, result);
			return result;
		}
		return map;
	}

	private void injectAnnotations(MarshalledMap map, Object bean) throws ExecutableException {
		var bm = toBeanMap(bean);
		for (var entry : map.entrySet()) {
			var key = entry.getKey();
			var pm = bm.getPropertyMeta(key);
			var val = pm == null ? null : getBeanValueSafely(bm, key);
			if (pm == null || val == null)
				continue;
			var cm = (ClassMeta<?>) pm.getBeanInfo();
			if (cm.getNameProperty() != null)
				setName(cm, val, key);
			if (cm.getParentProperty() != null)
				setParent(cm, val, bean);
			if (cm.isMap() && val instanceof Map<?, ?> val2 && !cm.getValueType().isObject() && cm.getValueType().getNameProperty() != null) {
				var valueType = cm.getValueType();
				for (Map.Entry<?, ?> e : ((Map<?, ?>) val2).entrySet())
					setName(valueType, e.getValue(), e.getKey());
			}
		}
	}

	private static Object getBeanValueSafely(BeanMap<?> bm, String key) {
		try {
			return bm.get(key);
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}
}
