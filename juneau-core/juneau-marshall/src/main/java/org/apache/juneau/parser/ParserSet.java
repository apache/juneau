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
package org.apache.juneau.parser;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Represents a group of {@link Parser Parsers} that can be looked up by media type.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Finds parsers based on HTTP <c>Content-Type</c> header values.
 * 	<li>
 * 		Sets common properties on all parsers in a single method call.
 * 	<li>
 * 		Locks all parsers in a single method call.
 * 	<li>
 * 		Clones existing groups and all parsers within the group in a single method call.
 * </ul>
 *
 * <h5 class='topic'>Match ordering</h5>
 *
 * Parsers are matched against <c>Content-Type</c> strings in the order they exist in this group.
 *
 * <p>
 * Adding new entries will cause the entries to be prepended to the group.
 * This allows for previous parsers to be overridden through subsequent calls.
 *
 * <p>
 * For example, calling <code>g.append(P1.<jk>class</jk>,P2.<jk>class</jk>).append(P3.<jk>class</jk>,P4.<jk>class</jk>)</code>
 * will result in the order <c>P3, P4, P1, P2</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct a new parser group builder</jc>
 * 	ParserSet <jv>parsers</jv> = ParserSet.<jsm>create</jsm>();
 * 		.add(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>); <jc>// Add some parsers to it</jc>
 *		.forEach(<jv>x</jv> -&gt; <jv>x</jv>.swaps(CalendarSwap.IsoLocalDateTime.<jk>class</jk>))
 *		.forEach(<jv>x</jv> -&gt; <jv>x</jv>.beansRequireSerializable())
 * 		.build();
 *
 * 	<jc>// Find the appropriate parser by Content-Type</jc>
 * 	ReaderParser <jv>parser</jv> = (ReaderParser)<jv>parsers</jv>.getParser(<js>"text/json"</js>);
 *
 * 	<jc>// Parse a bean from JSON</jc>
 * 	String <jv>json</jv> = <js>"{...}"</js>;
 * 	AddressBook <jv>addressBook</jv> = <jv>parser</jv>.parse(<jv>json</jv>, AddressBook.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public final class ParserSet {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * An identifier that the previous entries in this group should be inherited.
	 * <p>
	 * Used by {@link ParserSet.Builder#set(Class...)}
	 */
	@SuppressWarnings("javadoc")
	public static abstract class Inherit extends Parser {
		protected Inherit(Parser.Builder builder) {
			super(builder);
		}
	}

	/**
	 * An identifier that the previous entries in this group should not be inherited.
	 * <p>
	 * Used by {@link ParserSet.Builder#add(Class...)}
	 */
	@SuppressWarnings("javadoc")
	public static abstract class NoInherit extends Parser {
		protected NoInherit(Parser.Builder builder) {
			super(builder);
		}
	}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder(BeanStore.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<ParserSet> {

		List<Object> entries;
		private BeanContext.Builder bcBuilder;

		/**
		 * Create an empty parser group builder.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(ParserSet.class, beanStore);
			this.entries = list();
		}

		/**
		 * Clone an existing parser group.
		 *
		 * @param copyFrom The parser group that we're copying settings and parsers from.
		 */
		protected Builder(ParserSet copyFrom) {
			super(copyFrom.getClass(), BeanStore.INSTANCE);
			this.entries = list((Object[])copyFrom.entries);
		}

		/**
		 * Clone an existing parser group builder.
		 *
		 * <p>
		 * Parser builders will be cloned during this process.
		 *
		 * @param copyFrom The parser group that we're copying settings and parsers from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			bcBuilder = copyFrom.bcBuilder == null ? null : copyFrom.bcBuilder.copy();
			entries = list();
			copyFrom.entries.stream().map(x -> copyBuilder(x)).forEach(x -> entries.add(x));
		}

		private Object copyBuilder(Object o) {
			if (o instanceof Parser.Builder) {
				Parser.Builder x = (Parser.Builder)o;
				Parser.Builder x2 = x.copy();
				if (ne(x.getClass(), x2.getClass()))
					throw new BasicRuntimeException("Copy method not implemented on class {0}", x.getClass().getName());
				x = x2;
				if (bcBuilder != null)
					x.beanContext(bcBuilder);
				return x;
			}
			return o;
		}

		@Override /* BeanBuilder */
		protected ParserSet buildDefault() {
			return new ParserSet(this);
		}

		/**
		 * Makes a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Associates an existing bean context builder with all parser builders in this group.
		 *
		 * @param value The bean contest builder to associate.
		 * @return This object.
		 */
		public Builder beanContext(BeanContext.Builder value) {
			bcBuilder = value;
			forEach(x -> x.beanContext(value));
			return this;
		}

		/**
		 * Applies an operation to the bean context builder.
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder beanContext(Consumer<BeanContext.Builder> operation) {
			if (bcBuilder != null)
				operation.accept(bcBuilder);
			return this;
		}

		/**
		 * Adds the specified parsers to this group.
		 *
		 * <p>
		 * Entries are added in-order to the beginning of the list in the group.
		 *
		 * <p>
		 * The {@link NoInherit} class can be used to clear out the existing list of parsers before adding the new entries.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	ParserSet.Builder <jv>builder</jv> = ParserSet.<jsm>create</jsm>();  <jc>// Create an empty builder.</jc>
		 *
		 * 	<jv>builder</jv>.add(FooParser.<jk>class</jk>);  <jc>// Now contains:  [FooParser]</jc>
		 *
		 * 	<jv>builder</jv>.add(BarParser.<jk>class</jk>, BazParser.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazParser,FooParser]</jc>
		 *
		 * 	<jv>builder</jv>.add(NoInherit.<jk>class</jk>, QuxParser.<jk>class</jk>);  <jc>// Now contains:  [QuxParser]</jc>
		 * </p>
		 *
		 * @param values The parsers to add to this group.
		 * @return This object.
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Parser}.
		 */
		public Builder add(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> v : values)
				if (v.getSimpleName().equals("NoInherit"))
					clear();
			for (Class<?> v : values) {
				if (Parser.class.isAssignableFrom(v)) {
					l.add(createBuilder(v));
				} else if (! v.getSimpleName().equals("NoInherit")) {
					throw new BasicRuntimeException("Invalid type passed to ParserSet.Builder.add(): {0}", v.getName());
				}
			}
			entries.addAll(0, l);
			return this;
		}

		/**
		 * Sets the specified parsers for this group.
		 *
		 * <p>
		 * Existing values are overwritten.
		 *
		 * <p>
		 * The {@link Inherit} class can be used to insert existing entries in this group into the position specified.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	ParserSet.Builder <jv>builder</jv> = ParserSet.<jsm>create</jsm>();  <jc>// Create an empty builder.</jc>
		 *
		 * 	<jv>builder</jv>.set(FooParser.<jk>class</jk>);  <jc>// Now contains:  [FooParser]</jc>
		 *
		 * 	<jv>builder</jv>.set(BarParser.<jk>class</jk>, BazParser.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazParser]</jc>
		 *
		 * 	<jv>builder</jv>.set(Inherit.<jk>class</jk>, QuxParser.<jk>class</jk>);  <jc>// Now contains:  [BarParser,BazParser,QuxParser]</jc>
		 * </p>
		 *
		 * @param values The parsers to set in this group.
		 * @return This object.
		 * @throws IllegalArgumentException If one or more values do not extend from {@link Parser} or named <js>"Inherit"</js>.
		 */
		public Builder set(Class<?>...values) {
			List<Object> l = list();
			for (Class<?> v : values) {
				if (v.getSimpleName().equals("Inherit")) {
					l.addAll(entries);
				} else if (Parser.class.isAssignableFrom(v)) {
					l.add(createBuilder(v));
				} else {
					throw new BasicRuntimeException("Invalid type passed to ParserGrouup.Builder.set(): {0}", v.getName());
				}
			}
			entries = l;
			return this;
		}

		private Object createBuilder(Object o) {
			if (o instanceof Class) {

				// Check for no-arg constructor.
				ConstructorInfo ci = ClassInfo.of((Class<?>)o).getPublicConstructor(x -> x.hasNoParams());
				if (ci != null)
					return ci.invoke();

				// Check for builder.
				@SuppressWarnings("unchecked")
				Parser.Builder b = Parser.createParserBuilder((Class<? extends Parser>)o);
				if (bcBuilder != null)
					b.beanContext(bcBuilder);
				o = b;
			}
			return o;
		}

		/**
		 * Registers the specified parsers with this group.
		 *
		 * <p>
		 * When passing in pre-instantiated parsers to this group, applying properties and transforms to the group
		 * do not affect them.
		 *
		 * @param s The parsers to append to this group.
		 * @return This object.
		 */
		public Builder add(Parser...s) {
			prependAll(entries, (Object[])s);
			return this;
		}

		/**
		 * Clears out any existing parsers in this group.
		 *
		 * @return This object.
		 */
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Returns <jk>true</jk> if at least one of the specified annotations can be applied to at least one parser builder in this group.
		 *
		 * @param work The work to check.
		 * @return <jk>true</jk> if at least one of the specified annotations can be applied to at least one parser builder in this group.
		 */
		public boolean canApply(AnnotationWorkList work) {
			for (Object o : entries)
				if (o instanceof Parser.Builder)
					if (((Parser.Builder)o).canApply(work))
						return true;
			return false;
		}

		/**
		 * Applies the specified annotations to all applicable parser builders in this group.
		 *
		 * @param work The annotations to apply.
		 * @return This object.
		 */
		public Builder apply(AnnotationWorkList work) {
			return forEach(x -> x.apply(work));
		}

		/**
		 * Performs an action on all parser builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEach(Consumer<Parser.Builder> action) {
			builders(Parser.Builder.class).forEach(action);
			return this;
		}

		/**
		 * Performs an action on all writer parser builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEachRP(Consumer<ReaderParser.Builder> action) {
			return forEach(ReaderParser.Builder.class, action);
		}

		/**
		 * Performs an action on all output stream parser builders in this group.
		 *
		 * @param action The action to perform.
		 * @return This object.
		 */
		public Builder forEachISP(Consumer<InputStreamParser.Builder> action) {
			return forEach(InputStreamParser.Builder.class, action);
		}

		/**
		 * Performs an action on all parser builders of the specified type in this group.
		 *
		 * @param <B> The parser builder type.
		 * @param type The parser builder type.
		 * @param action The action to perform.
		 * @return This object.
		 */
		public <B extends Parser.Builder> Builder forEach(Class<B> type, Consumer<B> action) {
			builders(type).forEach(action);
			return this;
		}

		/**
		 * Returns direct access to the {@link Parser} and {@link Parser.Builder} objects in this builder.
		 *
		 * <p>
		 * Provided to allow for any extraneous modifications to the list not accomplishable via other methods on this builder such
		 * as re-ordering/adding/removing entries.
		 *
		 * <p>
		 * Note that it is up to the user to ensure that the list only contains {@link Parser} and {@link Parser.Builder} objects.
		 *
		 * @return The inner list of entries in this builder.
		 */
		public List<Object> inner() {
			return entries;
		}

		private <T extends Parser.Builder> Stream<T> builders(Class<T> type) {
			return entries.stream().filter(x -> type.isInstance(x)).map(x -> type.cast(x));
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		@Override /* Object */
		public String toString() {
			return entries.stream().map(x -> toString(x)).collect(joining(",","[","]"));
		}

		private String toString(Object o) {
			if (o == null)
				return "null";
			if (o instanceof Parser.Builder)
				return "builder:" + o.getClass().getName();
			return "parser:" + o.getClass().getName();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	// Maps Content-Type headers to matches.
	private final ConcurrentHashMap<String,ParserMatch> cache = new ConcurrentHashMap<>();

	private final MediaType[] mediaTypes;
	private final Parser[] mediaTypeParsers;

	final Parser[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this bean.
	 */
	public ParserSet(Builder builder) {

		this.entries = builder.entries.stream().map(x -> build(x)).toArray(Parser[]::new);

		List<MediaType> lmt = list();
		List<Parser> l = list();
		for (Parser e : entries) {
			e.getMediaTypes().forEach(x -> {
				lmt.add(x);
				l.add(e);
			});
		}

		this.mediaTypes = array(lmt, MediaType.class);
		this.mediaTypeParsers = array(l, Parser.class);
	}

	private Parser build(Object o) {
		if (o instanceof Parser)
			return (Parser)o;
		return ((Parser.Builder)o).build();
	}

	/**
	 * Creates a copy of this parser group.
	 *
	 * @return A new copy of this parser group.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Searches the group for a parser that can handle the specified <l>Content-Type</l> header value.
	 *
	 * <p>
	 * The returned object includes both the parser and media type that matched.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header value.
	 * @return The parser and media type that matched the content type header, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(String contentTypeHeader) {
		ParserMatch pm = cache.get(contentTypeHeader);
		if (pm != null)
			return pm;

		MediaType ct = MediaType.of(contentTypeHeader);
		int match = ct.match(ulist(mediaTypes));

		if (match >= 0) {
			pm = new ParserMatch(mediaTypes[match], mediaTypeParsers[match]);
			cache.putIfAbsent(contentTypeHeader, pm);
		}

		return cache.get(contentTypeHeader);
	}

	/**
	 * Same as {@link #getParserMatch(String)} but matches using a {@link MediaType} instance.
	 *
	 * @param mediaType The HTTP <l>Content-Type</l> header value as a media type.
	 * @return The parser and media type that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public ParserMatch getParserMatch(MediaType mediaType) {
		return getParserMatch(mediaType.toString());
	}

	/**
	 * Same as {@link #getParserMatch(String)} but returns just the matched parser.
	 *
	 * @param contentTypeHeader The HTTP <l>Content-Type</l> header string.
	 * @return The parser that matched the content type header, or <jk>null</jk> if no match was made.
	 */
	public Parser getParser(String contentTypeHeader) {
		ParserMatch pm = getParserMatch(contentTypeHeader);
		return pm == null ? null : pm.getParser();
	}

	/**
	 * Same as {@link #getParserMatch(MediaType)} but returns just the matched parser.
	 *
	 * @param mediaType The HTTP media type.
	 * @return The parser that matched the media type, or <jk>null</jk> if no match was made.
	 */
	public Parser getParser(MediaType mediaType) {
		ParserMatch pm = getParserMatch(mediaType);
		return pm == null ? null : pm.getParser();
	}

	/**
	 * Returns the media types that all parsers in this group can handle
	 *
	 * <p>
	 * Entries are ordered in the same order as the parsers in the group.
	 *
	 * @return An unmodifiable list of media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return ulist(mediaTypes);
	}

	/**
	 * Returns the parsers in this group.
	 *
	 * @return An unmodifiable list of parsers in this group.
	 */
	public List<Parser> getParsers() {
		return ulist(entries);
	}

	/**
	 * Returns <jk>true</jk> if this group contains no parsers.
	 *
	 * @return <jk>true</jk> if this group contains no parsers.
	 */
	public boolean isEmpty() {
		return entries.length == 0;
	}
}
