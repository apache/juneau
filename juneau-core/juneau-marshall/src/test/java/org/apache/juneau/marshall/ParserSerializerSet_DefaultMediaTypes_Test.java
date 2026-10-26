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
package org.apache.juneau.marshall;

import static org.apache.juneau.bct.BctAssertions.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Guard test: every public concrete parser and serializer class must keep its own media types when it is added to a
 * {@link ParserSet} or {@link SerializerSet} by class.
 *
 * <p>
 * Classes (including public nested variants such as <c>Readable</c>) are discovered by scanning the compiled classes of
 * this module, so new parsers and serializers are covered automatically.  The expected instance is, in order:
 * <ol>
 * 	<li>the class's own <c>DEFAULT</c> field,
 * 	<li>any other public static field on the class or its enclosing class holding an instance of exactly that class
 * 		(for example <c>DEFAULT_READABLE</c>),
 * 	<li>the result of the class's own static <c>create().build()</c>,
 * 	<li>for a nested variant with no <c>create()</c> of its own, the variant constructed from its enclosing class's
 * 		<c>create()</c>.
 * </ol>
 */
class ParserSerializerSet_DefaultMediaTypes_Test extends TestBase {

	/**
	 * Finds all public concrete subclasses of {@code base} (top-level or public static nested) in the code source that
	 * contains {@code anchor} that have a public constructor taking a builder.
	 */
	static <T> List<Class<? extends T>> findClasses(Class<?> anchor, Class<T> base) throws Exception {
		var root = Paths.get(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
		var result = new ArrayList<Class<? extends T>>();
		try (var walk = Files.walk(root)) {
			var names = walk
				.map(p -> root.relativize(p).toString())
				.filter(n -> n.endsWith(".class") && ! n.endsWith("package-info.class") && ! n.endsWith("module-info.class"))
				.map(n -> n.substring(0, n.length() - 6).replace('/', '.').replace('\\', '.'))
				.sorted()
				.toList();
			for (var n : names) {
				var c = Class.forName(n, false, anchor.getClassLoader());
				var m = c.getModifiers();
				if (! base.isAssignableFrom(c) || Modifier.isAbstract(m) || ! Modifier.isPublic(m) || c.isAnonymousClass() || c.isLocalClass() || c.isSynthetic())
					continue;
				if (c.isMemberClass() && ! Modifier.isStatic(m))
					continue;
				// Only classes a set can build from a builder (public constructor taking a builder).
				if (Arrays.stream(c.getConstructors()).noneMatch(x -> x.getParameterCount() == 1 && Context.Builder.class.isAssignableFrom(x.getParameterTypes()[0])))
					continue;
				result.add(c.asSubclass(base));
			}
		}
		return result;
	}

	/**
	 * Returns the instance whose media types the set-built instance of {@code c} must match, or <jk>null</jk> if none of
	 * the rules in the class description apply.
	 */
	static Object expectedInstance(Class<?> c) throws Exception {
		var own = field(c, "DEFAULT");
		if (own != null && own.get(null).getClass() == c)
			return own.get(null);
		for (var holder : c.getEnclosingClass() != null ? List.of(c, c.getEnclosingClass()) : List.of(c))
			for (var f : holder.getDeclaredFields())
				if (isPublicStatic(f.getModifiers()) && f.get(null) != null && f.get(null).getClass() == c)
					return f.get(null);
		var create = ownCreate(c);
		if (create != null) {
			var o = ((Context.Builder<?>)create.invoke(null)).build();
			return o.getClass() == c ? o : null;
		}
		// A nested variant without its own create() (for example XmlDocSerializer.Ns) is built from its enclosing
		// class's builder.
		var outer = c.getEnclosingClass();
		create = outer == null || ! outer.isAssignableFrom(c) ? null : ownCreate(outer);
		if (create != null)
			for (var x : c.getConstructors())
				if (x.getParameterCount() == 1 && x.getParameterTypes()[0].isAssignableFrom(create.getReturnType()))
					return x.newInstance(create.invoke(null));
		return null;
	}

	private static Method ownCreate(Class<?> c) {
		for (var m : c.getDeclaredMethods())
			if (m.getName().equals("create") && m.getParameterCount() == 0 && isPublicStatic(m.getModifiers()))
				return m;
		return null;
	}

	private static Field field(Class<?> c, String name) {
		try {
			var f = c.getDeclaredField(name);
			return isPublicStatic(f.getModifiers()) ? f : null;
		} catch (@SuppressWarnings("unused") NoSuchFieldException e) {
			return null;
		}
	}

	private static boolean isPublicStatic(int m) {
		return Modifier.isPublic(m) && Modifier.isStatic(m);
	}

	/**
	 * Returns the classes that have no expected instance (see {@link #expectedInstance(Class)}).
	 */
	static List<String> unverifiable(List<? extends Class<?>> classes) throws Exception {
		var l = new ArrayList<String>();
		for (var c : classes)
			if (expectedInstance(c) == null)
				l.add(c.getName());
		return l;
	}

	static String parserMediaTypes(Parser p) {
		return p.getClass().getSimpleName() + "=" + p.getMediaTypes();
	}

	static String serializerMediaTypes(Serializer s) {
		return s.getClass().getSimpleName() + "=" + s.getMediaTypeRanges() + ";produces=" + s.getResponseContentType();
	}

	/**
	 * Returns the mismatches between each parser's expected instance and the instance a ParserSet builds from the class.
	 */
	static List<String> parserMismatches(List<Class<? extends Parser>> classes) throws Exception {
		var mismatches = new ArrayList<String>();
		for (var c : classes) {
			var e = expectedInstance(c);
			if (e == null)
				continue;
			var expected = parserMediaTypes((Parser)e);
			var actual = parserMediaTypes(ParserSet.create().add(c).build().getParsers().get(0));
			if (! expected.equals(actual))
				mismatches.add("expected " + c.getName() + "=" + expected + " but set built " + actual);
		}
		return mismatches;
	}

	/**
	 * Returns the mismatches between each serializer's expected instance and the instance a SerializerSet builds from the
	 * class.
	 */
	static List<String> serializerMismatches(List<Class<? extends Serializer>> classes) throws Exception {
		var mismatches = new ArrayList<String>();
		for (var c : classes) {
			var e = expectedInstance(c);
			if (e == null)
				continue;
			var expected = serializerMediaTypes((Serializer)e);
			var actual = serializerMediaTypes(SerializerSet.create().add(c).build().getSerializers().get(0));
			if (! expected.equals(actual))
				mismatches.add("expected " + c.getName() + "=" + expected + " but set built " + actual);
		}
		return mismatches;
	}

	@Test void a01_parsers_discovered() throws Exception {
		var classes = findClasses(Parser.class, Parser.class);
		assertContains("JsonParser", classes.toString());
		assertContains("Json5Parser", classes.toString());
		assertContains("UonParser$Decoding", classes.toString());
		var unverifiable = unverifiable(classes);
		assertEmpty(unverifiable::toString, unverifiable);
	}

	@Test void a02_serializers_discovered() throws Exception {
		var classes = findClasses(Serializer.class, Serializer.class);
		assertContains("JsonSerializer", classes.toString());
		assertContains("Json5Serializer$Readable", classes.toString());
		assertContains("HtmlSchemaDocSerializer", classes.toString());
		var unverifiable = unverifiable(classes);
		assertEmpty(unverifiable::toString, unverifiable);
	}

	@Test void a03_parserSetMembers_matchDefaultMediaTypes() throws Exception {
		var mismatches = parserMismatches(findClasses(Parser.class, Parser.class));
		assertEmpty(mismatches::toString, mismatches);
	}

	@Test void a04_serializerSetMembers_matchDefaultMediaTypes() throws Exception {
		var mismatches = serializerMismatches(findClasses(Serializer.class, Serializer.class));
		assertEmpty(mismatches::toString, mismatches);
	}

	@Test void a05_json5Parser_inParserSet_consumesJson5() {
		var s = ParserSet.create().add(JsonParser.class, Json5Parser.class).build();
		assertString("Json5Parser", s.getParser("application/json5").orElseThrow().getClass().getSimpleName());
	}

	@Test void a06_json5Serializer_inSerializerSet_producesJson5() {
		var s = SerializerSet.create().add(JsonSerializer.class, Json5Serializer.class).build();
		assertString("Json5Serializer", s.getSerializer("application/json5").orElseThrow().getClass().getSimpleName());
	}

	@Test void a07_htmlSchemaDocSerializer_inSerializerSet_producesHtmlSchema() {
		var s = SerializerSet.create().add(HtmlDocSerializer.class, HtmlSchemaDocSerializer.class).build();
		assertString("HtmlSchemaDocSerializer", s.getSerializer("text/html+schema").orElseThrow().getClass().getSimpleName());
	}
}
