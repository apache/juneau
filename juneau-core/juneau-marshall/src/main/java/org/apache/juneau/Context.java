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
package org.apache.juneau;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Base class for all Context beans.
 *
 * <p>
 * Context beans follow the convention of havinTg the following parts:
 * <ul>
 * 	<li>A {@link Builder} class for configuring the context bean.
 * 	<ul>
 * 		<li>This bean is non-thread-safe and meant for one-time use.
 * 	</ul>
 * 	<li>A {@link Context#Context(Builder)} constructor that takes in a builder object.
 * 	<ul>
 * 		<li>This bean is thread-safe and cacheable/reusable.
 * 	</ul>
 * 	<li>A {@link ContextSession} class for doing work.
 * 	<ul>
 * 		<li>This bean is non-thread-safe and meant for one-time use.
 * 	</ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public abstract class Context {
	/**
	 * Builder class.
	 */
	public abstract static class Builder {

		private static final Map<Class<?>,ConstructorInfo> CONTEXT_CONSTRUCTORS = new ConcurrentHashMap<>();

		boolean debug;
		Class<? extends Context> type;
		Context impl;
		List<Annotation> annotations;
		Cache<HashKey,? extends Context> cache;

		private final List<Object> builders = list();
		private final AnnotationWorkList applied = AnnotationWorkList.create();

		/**
		 * Constructor.
		 * Default settings.
		 */
		@SuppressWarnings("unchecked")
		protected Builder() {
			debug = env("Context.debug", false);
			annotations = null;
			registerBuilders(this);

			// By default, the type being created should be the class declaring the builder.
			Class<?> dc = getClass().getDeclaringClass();
			if (Context.class.isAssignableFrom(dc))
				type((Class<? extends Context>)dc);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			debug = copyFrom.debug;
			type = copyFrom.type;
			annotations = toList(copyFrom.annotations, true);
			registerBuilders(this);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(Context copyFrom) {
			debug = copyFrom.debug;
			type = copyFrom.getClass();
			annotations = toList(copyFrom.annotations, true);
			registerBuilders(this);
		}

		/**
		 * Defines annotations to apply to specific classes and methods.
		 *
		 * <p>
		 * Allows you to dynamically apply Juneau annotations typically applied directly to classes and methods.
		 * Useful in cases where you want to use the functionality of the annotation on beans and bean properties but
		 * do not have access to the code to do so.
		 *
		 * <p>
		 * As a rule, any Juneau annotation with an <l>on()</l> method can be used with this setting.
		 *
		 * <p>
		 * The following example shows the equivalent methods for applying the {@link Bean @Bean} annotation:
		 * <p class='bjava'>
		 * 	<jc>// Class with explicit annotation.</jc>
		 * 	<ja>@Bean</ja>(properties=<js>"street,city,state"</js>)
		 * 	<jk>public class</jk> A {...}
		 *
		 * 	<jc>// Class with annotation applied via @BeanConfig</jc>
		 * 	<jk>public class</jk> B {...}
		 *
		 * 	<jc>// Java REST method with @BeanConfig annotation.</jc>
		 * 	<ja>@RestGet</ja>(...)
		 * 	<ja>@Bean</ja>(on=<js>"B"</js>, properties=<js>"street,city,state"</js>)
		 * 	<jk>public void</jk> doFoo() {...}
		 * </p>
		 *
		 * <p>
		 * In general, the underlying framework uses this method when it finds dynamically applied annotations on
		 * config annotations.  However, concrete implementations of annotations are also provided that can be passed
		 * directly into builder classes like so:
		 * <p class='bjava'>
		 * 	<jc>// Create a concrete @Bean annotation.</jc>
		 * 	<ja>Bean</ja> <jv>annotation</jv> = BeanAnnotation.<jsm>create</jsm>(B.<jk>class</jk>).properties(<js>"street,city,state"</js>).build();
		 *
		 * 	<jc>// Apply it to a serializer.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().annotations(<jv>annotation</jv>).build();
		 *
		 * 	<jc>// Serialize a bean with the dynamically applied annotation.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> B());
		 * </p>
		 *
		 * <p>
		 * The following is the list of annotations builders provided that can be constructed
		 * and passed into the builder class:
		 * <ul class='javatreec'>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeancAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanIgnoreAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanpAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.ExampleAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.NamePropertyAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.ParentPropertyAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.SwapAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.UriAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.csv.annotation.CsvAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.json.annotation.JsonAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.SchemaAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.msgpack.annotation.MsgPackAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.oapi.annotation.OpenApiAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.plaintext.annotation.PlainTextAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.soap.annotation.SoapXmlAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.uon.annotation.UonAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.urlencoding.annotation.UrlEncodingAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlAnnotation}
		 * </ul>
		 *
		 * <p>
		 * The syntax for the <l>on()</l> pattern match parameter depends on whether it applies to a class, method, field, or constructor.
		 * The valid pattern matches are:
		 * <ul class='spaced-list'>
		 *  <li>Classes:
		 * 		<ul>
		 * 			<li>Fully qualified:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass"</js>
		 * 				</ul>
		 * 			<li>Fully qualified inner class:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
		 * 				</ul>
		 * 			<li>Simple:
		 * 				<ul>
		 * 					<li><js>"MyClass"</js>
		 * 				</ul>
		 * 			<li>Simple inner:
		 * 				<ul>
		 * 					<li><js>"MyClass$Inner1$Inner2"</js>
		 * 					<li><js>"Inner1$Inner2"</js>
		 * 					<li><js>"Inner2"</js>
		 * 				</ul>
		 * 		</ul>
		 * 	<li>Methods:
		 * 		<ul>
		 * 			<li>Fully qualified with args:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
		 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
		 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
		 * 				</ul>
		 * 			<li>Fully qualified:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass.myMethod"</js>
		 * 				</ul>
		 * 			<li>Simple with args:
		 * 				<ul>
		 * 					<li><js>"MyClass.myMethod(String,int)"</js>
		 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
		 * 					<li><js>"MyClass.myMethod()"</js>
		 * 				</ul>
		 * 			<li>Simple:
		 * 				<ul>
		 * 					<li><js>"MyClass.myMethod"</js>
		 * 				</ul>
		 * 			<li>Simple inner class:
		 * 				<ul>
		 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
		 * 					<li><js>"Inner1$Inner2.myMethod"</js>
		 * 					<li><js>"Inner2.myMethod"</js>
		 * 				</ul>
		 * 		</ul>
		 * 	<li>Fields:
		 * 		<ul>
		 * 			<li>Fully qualified:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass.myField"</js>
		 * 				</ul>
		 * 			<li>Simple:
		 * 				<ul>
		 * 					<li><js>"MyClass.myField"</js>
		 * 				</ul>
		 * 			<li>Simple inner class:
		 * 				<ul>
		 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
		 * 					<li><js>"Inner1$Inner2.myField"</js>
		 * 					<li><js>"Inner2.myField"</js>
		 * 				</ul>
		 * 		</ul>
		 * 	<li>Constructors:
		 * 		<ul>
		 * 			<li>Fully qualified with args:
		 * 				<ul>
		 * 					<li><js>"com.foo.MyClass(String,int)"</js>
		 * 					<li><js>"com.foo.MyClass(java.lang.String,int)"</js>
		 * 					<li><js>"com.foo.MyClass()"</js>
		 * 				</ul>
		 * 			<li>Simple with args:
		 * 				<ul>
		 * 					<li><js>"MyClass(String,int)"</js>
		 * 					<li><js>"MyClass(java.lang.String,int)"</js>
		 * 					<li><js>"MyClass()"</js>
		 * 				</ul>
		 * 			<li>Simple inner class:
		 * 				<ul>
		 * 					<li><js>"MyClass$Inner1$Inner2()"</js>
		 * 					<li><js>"Inner1$Inner2()"</js>
		 * 					<li><js>"Inner2()"</js>
		 * 				</ul>
		 * 		</ul>
		 * 	<li>A comma-delimited list of anything on this list.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link BeanConfig}
		 * </ul>
		 *
		 * @param values
		 * 	The annotations to register with the context.
		 * @return This object.
		 */
		public Builder annotations(Annotation...values) {
			annotations = addAll(annotations, values);
			return this;
		}

		/**
		 * Same as {@link #annotations(Annotation...)} but uses a list as input.
		 *
		 * @param values
		 * 	The annotations to register with the context.
		 * @return This object.
		 */
		public Builder annotations(List<Annotation> values) {
			annotations = addAll(annotations, values);
			return this;
		}

		/**
		 * Applies a set of applied to this builder.
		 *
		 * <p>
		 * An {@link AnnotationWork} consists of a single pair of {@link AnnotationInfo} that represents an annotation instance,
		 * and {@link AnnotationApplier} which represents the code used to apply the values in that annotation to a specific builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A class annotated with a config annotation.</jc>
		 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
		 * 	<jk>public class</jk> MyClass {...}
		 *
		 * 	<jc>// Find all annotations that themselves are annotated with @ContextPropertiesApply.</jc>
		 * 	AnnotationList <jv>annotations</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getAnnotationList(<jsf>CONTEXT_APPLY_FILTER</jsf>);
		 * 	VarResolverSession <jv>vrs</jv> = VarResolver.<jsf>DEFAULT</jsf>.createSession();
		 * 	AnnotationWorkList <jv>work</jv> = AnnotationWorkList.of(<jv>vrs</jv>, <jv>annotations</jv>);
		 *
		 * 	<jc>// Apply any settings found on the annotations.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.apply(<jv>work</jv>)
		 * 		.build();
		 * </p>
		 *
		 * @param work The list of annotations and appliers to apply to this builder.
		 * @return This object.
		 */
		public Builder apply(AnnotationWorkList work) {
			applied.addAll(work);
			work.forEach(x -> builders.forEach(x::apply));
			return this;
		}

		/**
		 * Apply a consumer to this builder.
		 *
		 * @param <T> The builder subtype that this consumer can be applied to.
		 * @param subtype The builder subtype that this consumer can be applied to.
		 * @param consumer The consumer.
		 * @return This object.
		 */
		public <T extends Builder> Builder apply(Class<T> subtype, Consumer<T> consumer) {
			if (subtype.isInstance(this))
				consumer.accept(subtype.cast(this));
			return this;
		}

		/**
		 * Same as {@link #applyAnnotations(Object...)} but explicitly specifies a class varargs to avoid compilation warnings.
		 *
		 * @param from The classes or methods on which the annotations are defined.
		 * @return This object.
		 */
		public Builder applyAnnotations(Class<?>...from) {
			return applyAnnotations((Object[])from);
		}

		/**
		 * Applies any of the various <ja>@XConfig</ja> annotations on the specified classes or methods to this context.
		 *
		 * <p>
		 * Any annotations found that themselves are annotated with {@link ContextApply} will be resolved and
		 * applied as properties to this builder.  These annotations include:
		 * <ul class='javatreec'>
		 * 	<li class ='ja'>{@link BeanConfig}
		 * 	<li class ='ja'>{@link CsvConfig}
		 * 	<li class ='ja'>{@link HtmlConfig}
		 * 	<li class ='ja'>{@link HtmlDocConfig}
		 * 	<li class ='ja'>{@link JsonConfig}
		 * 	<li class ='ja'>{@link JsonSchemaConfig}
		 * 	<li class ='ja'>{@link MsgPackConfig}
		 * 	<li class ='ja'>{@link OpenApiConfig}
		 * 	<li class ='ja'>{@link ParserConfig}
		 * 	<li class ='ja'>{@link PlainTextConfig}
		 * 	<li class ='ja'>{@link SerializerConfig}
		 * 	<li class ='ja'>{@link SoapXmlConfig}
		 * 	<li class ='ja'>{@link UonConfig}
		 * 	<li class ='ja'>{@link UrlEncodingConfig}
		 * 	<li class ='ja'>{@link XmlConfig}
		 * 	<li class ='ja'><c>RdfConfig</c>
		 * </ul>
		 *
		 * <p>
		 * Annotations on classes are appended in the following order:
		 * <ol>
		 * 	<li>On the package of this class.
		 * 	<li>On interfaces ordered parent-to-child.
		 * 	<li>On parent classes ordered parent-to-child.
		 * 	<li>On this class.
		 * </ol>
		 *
		 * <p>
		 * Annotations on methods are appended in the following order:
		 * <ol>
		 * 	<li>On the package of the method class.
		 * 	<li>On interfaces ordered parent-to-child.
		 * 	<li>On parent classes ordered parent-to-child.
		 * 	<li>On the method class.
		 * 	<li>On this method and matching methods ordered parent-to-child.
		 * </ol>
		 *
		 * <p>
		 * The default var resolver {@link VarResolver#DEFAULT} is used to resolve any variables in annotation field values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A class annotated with a config annotation.</jc>
		 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
		 * 	<jk>public class</jk> MyClass {...}
		 *
		 * 	<jc>// Apply any settings found on the annotations.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.applyAnnotations(MyClass.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// A method annotated with a config annotation.</jc>
		 * 	<jk>public class</jk> MyClass {
		 * 		<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
		 * 		<jk>public void</jk> myMethod() {...}
		 * 	}
		 *
		 * 	<jc>// Apply any settings found on the annotations.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.applyAnnotations(MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>))
		 * 		.build();
		 * </p>
		 *
		 * @param from The classes or methods on which the annotations are defined.
		 * 	Can be any of the following types:
		 * 	<ul>
		 * 		<li>{@link Class}
		 * 		<li>{@link ClassInfo}
		 * 		<li>{@link Method}
		 * 		<li>{@link MethodInfo}
		 *		<li>A collection/stream/array of anything on this list.
		 * @return This object.
		 */
		public Builder applyAnnotations(Object...from) {
			var work = AnnotationWorkList.create();
			Arrays.stream(from).forEach(x -> traverse(work, x));
			return apply(work);
		}

		/**
		 * Build the object.
		 *
		 * @return The built object.
		 */
		public Context build() {
			return innerBuild();
		}

		/**
		 * Convenience method for calling {@link #build()} while avoiding a cast.
		 *
		 * @param <T> The type to cast the built object to.
		 * @param c The type to cast the built object to.
		 * @return The built context bean.
		 */
		@SuppressWarnings("unchecked")
		public final <T extends Context> T build(Class<T> c) {
			if (type == null || ! c.isAssignableFrom(type))
				type = c;
			return (T)innerBuild();
		}

		/**
		 * Specifies a cache to use for hashkey-based caching.
		 *
		 * @param value The cache.
		 * @return This object.
		 */
		public Builder cache(Cache<HashKey,? extends Context> value) {
			this.cache = value;
			return this;
		}

		/**
		 * Returns <jk>true</jk> if any of the annotations/appliers can be applied to this builder.
		 *
		 * @param work The work to check.
		 * @return <jk>true</jk> if any of the annotations/appliers can be applied to this builder.
		 */
		public boolean canApply(AnnotationWorkList work) {
			var f = Flag.create();
			work.forEach(x -> builders.forEach(b -> f.setIf(x.canApply(b))));
			return f.isSet();
		}

		/**
		 * Copy creator.
		 *
		 * @return A new mutable copy of this builder.
		 */
		public abstract Builder copy();

		/**
		 * <i><l>Context</l> configuration property:&emsp;</i>  Debug mode.
		 *
		 * <p>
		 * Enables the following additional information during serialization:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		When bean getters throws exceptions, the exception includes the object stack information
		 * 		in order to determine how that method was invoked.
		 * 	<li>
		 * 		Enables {@link BeanTraverseContext.Builder#detectRecursions()}.
		 * </ul>
		 *
		 * <p>
		 * Enables the following additional information during parsing:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		When bean setters throws exceptions, the exception includes the object stack information
		 * 		in order to determine how that method was invoked.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with debug enabled.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.debug()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>bean</jv>.<jf>f</jf> = <jv>bean</jv>;
		 *
		 * 	<jc>// Throws a SerializeException and not a StackOverflowError</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>bean</jv>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
		 * 	<li class='jm'>{@link org.apache.juneau.ContextSession.Builder#debug(Boolean)}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder debug() {
			return debug(true);
		}

		/**
		 * Same as {@link #debug()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder debug(boolean value) {
			debug = value;
			return this;
		}

		/**
		 * Returns all the annotations that have been applied to this builder.
		 *
		 * @return All the annotations that have been applied to this builder.
		 */
		public AnnotationWorkList getApplied() { return applied; }

		/**
		 * Returns the context class that this builder should create.
		 *
		 * @return The context class if it was specified.
		 */
		public Optional<Class<?>> getType() { return opt(type); }

		/**
		 * Returns the hashkey of this builder.
		 *
		 * <p>
		 * Used to return previously instantiated context beans that have matching hashkeys.
		 * The {@link HashKey} object is suitable for use as a hashmap key of a map of context beans.
		 * A context bean is considered equivalent if the {@link HashKey#equals(Object)} method is the same.
		 *
		 * @return The hashkey of this builder.
		 */
		public HashKey hashKey() {
			return HashKey.of(debug, type, annotations);
		}

		/**
		 * Specifies a pre-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder impl(Context value) {
			impl = value;
			return this;
		}

		/**
		 * Returns <jk>true</jk> if debug is enabled.
		 *
		 * @return <jk>true</jk> if debug is enabled.
		 */
		public boolean isDebug() { return debug; }

		/**
		 * Associates a context class with this builder.
		 *
		 * <p>
		 * This is the type of object that this builder creates when the {@link #build()} method is called.
		 *
		 * <p>
		 * By default, it's the outer class of where the builder class is defined.
		 *
		 * @param value The context class that this builder should create.
		 * @return This object.
		 */
		public Builder type(Class<? extends Context> value) {
			this.type = value;
			return this;
		}

		private ConstructorInfo getContextConstructor() {
			ConstructorInfo cci = CONTEXT_CONSTRUCTORS.get(type);
			if (cci == null) {
				// @formatter:off
				cci = ClassInfo.of(type).getPublicConstructor(
					x -> x.hasNumParameters(1)
					&& x.getParameter(0).canAccept(this)
				);
				// @formatter:on
				if (cci == null)
					throw runtimeException("Public constructor not found: {0}({1})", cn(type), cn(this));
				CONTEXT_CONSTRUCTORS.put(type, cci);
			}
			return cci;
		}

		private Context innerBuild() {
			if (type == null)
				throw runtimeException("Type not specified for context builder {0}", cn(getClass()));
			if (nn(impl) && type.isInstance(impl))
				return type.cast(impl);
			if (nn(cache))
				return cache.get(hashKey(), () -> getContextConstructor().newInstance(this));
			return getContextConstructor().newInstance(this);
		}

		private static AnnotationWorkList traverse(AnnotationWorkList work, Object x) {
			CollectionUtils.traverse(x, y -> {
				if (x instanceof Class<?> x2)
					work.add(rstream(ClassInfo.of(x2).getAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toCollection(AnnotationList::new)));
				else if (x instanceof ClassInfo x2)
					work.add(rstream(x2.getAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toCollection(AnnotationList::new)));
				else if (x instanceof Method x2)
					work.add(rstream(MethodInfo.of(x2).getAllAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toCollection(AnnotationList::new)));
				else if (x instanceof MethodInfo x2)
					work.add(rstream(x2.getAllAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toCollection(AnnotationList::new)));
				else
					illegalArg("Invalid type passed to applyAnnotations:  {0}", cn(x));
			});
			return work;
		}

		/**
		 * Registers the specified secondary builders with this context builder.
		 *
		 * <p>
		 * When {@link #apply(AnnotationWorkList)} is called, it gets called on all registered builders.
		 *
		 * @param builders The builders to add to the list of builders.
		 */
		protected void registerBuilders(Object...builders) {
			for (var b : builders) {
				if (b == this)
					this.builders.add(b);
				else if (b instanceof Builder b2)
					this.builders.addAll(b2.builders);
				else
					this.builders.add(b);
			}
		}
	}

	private static final Map<Class<?>,MethodInfo> BUILDER_CREATE_METHODS = new ConcurrentHashMap<>();

	/**
	 * Predicate for annotations that themselves are annotated with {@link ContextApply}.
	 */
	public static final Predicate<AnnotationInfo<?>> CONTEXT_APPLY_FILTER = x -> x.hasAnnotation(ContextApply.class);

	/**
	 * Instantiates a builder of the specified context class.
	 *
	 * <p>
	 * Looks for a public static method called <c>create</c> that returns an object that can be passed into a public
	 * or protected constructor of the class.
	 *
	 * @param type The builder to create.
	 * @return A new builder.
	 */
	public static Builder createBuilder(Class<? extends Context> type) {
		try {
			MethodInfo mi = BUILDER_CREATE_METHODS.get(type);
			if (mi == null) {
				var c = ClassInfo.of(type);
				for (var ci : c.getPublicConstructors()) {
					if (ci.hasNumParameters(1) && ! ci.getParameter(0).getParameterType().is(type)) {
						// @formatter:off
						mi = c.getPublicMethod(
							x -> x.isStatic()
							&& x.isNotDeprecated()
							&& x.hasName("create")
							&& x.hasReturnType(ci.getParameter(0).getParameterType())
						);
						// @formatter:on
						if (nn(mi))
							break;
					}
				}
				if (mi == null)
					throw runtimeException("Could not find builder create method on class {0}", cn(type));
				BUILDER_CREATE_METHODS.put(type, mi);
			}
			var b = (Builder)mi.invoke(null);
			b.type(type);
			return b;
		} catch (ExecutableException e) {
			throw toRuntimeException(e);
		}
	}

	final List<Annotation> annotations;
	final boolean debug;

	private final AnnotationProvider annotationProvider;

	/**
	 * Constructor for this class.
	 *
	 * @param builder The builder for this class.
	 */
	protected Context(Builder builder) {
		init(builder);
		debug = builder.debug;
		annotations = opt(builder.annotations).orElseGet(Collections::emptyList);
		annotationProvider = AnnotationProvider.create().addRuntimeAnnotations(annotations).build();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The context to copy from.
	 */
	protected Context(Context copyFrom) {
		annotations = copyFrom.annotations;
		debug = copyFrom.debug;
		annotationProvider = copyFrom.annotationProvider;
	}

	public AnnotationProvider getAnnotationProvider() {
		return annotationProvider;
	}

	/**
	 * Creates a builder from this context object.
	 *
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 *
	 * @return A new Builder object.
	 */
	public Builder copy() {
		throw unsupportedOp();
	}

	/**
	 * Create a session builder based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions where you want to override basic settings.
	 * Otherwise, use {@link #getSession()} directly.
	 *
	 * @return A new session builder.
	 */
	public ContextSession.Builder createSession() {
		throw unsupportedOp();
	}

	/**
	 * Returns a session to use for this context.
	 *
	 * <p>
	 * Note that subclasses may opt to return a reusable non-modifiable session.
	 *
	 * @return A new session object.
	 */
	public ContextSession getSession() { return createSession().build(); }

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param <A> The annotation being checked for.
	 * @param type The annotation being checked for.
	 * @param onClass The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type, Class<?> onClass) {
		return getAnnotationProvider().find(type, onClass).map(x -> x.inner()).findFirst().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param <A> The annotation being checked for.
	 * @param type The annotation being checked for.
	 * @param onConstructor The constructor being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified field.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type, Constructor<?> onConstructor) {
		return getAnnotationProvider().find(type, onConstructor).map(x -> x.inner()).findFirst().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,f)</c> returns a non-null value.
	 *
	 * @param <A> The annotation being checked for.
	 * @param type The annotation being checked for.
	 * @param onField The field being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified field.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type, Field onField) {
		return getAnnotationProvider().find(type, onField).map(x -> x.inner()).findFirst().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param <A> The annotation being checked for.
	 * @param type The annotation being checked for.
	 * @param onMethod The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type, Method onMethod) {
		return getAnnotationProvider().find(type, onMethod).map(x -> x.inner()).findFirst().isPresent();
	}

	/**
	 * Debug mode.
	 *
	 * @see Context.Builder#debug()
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() { return debug; }

	@Override /* Overridden from Object */
	public String toString() {
		return Utils2.toPropertyMap(this).asReadableString();
	}

	/**
	 * Perform optional initialization on builder before it is used.
	 *
	 * <p>
	 * Default behavior is a no-op.
	 *
	 * @param builder The builder to initialize.
	 */
	protected void init(Builder builder) {}

	/**
	 * Returns the properties on this bean as a map for debugging.
	 *
	 * @return The properties on this bean as a map for debugging.
	 */
	protected JsonMap properties() {
		return filteredMap("annotations", annotations, "debug", debug);
	}
}