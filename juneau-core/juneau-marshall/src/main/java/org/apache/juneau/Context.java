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
package org.apache.juneau;

import static org.apache.juneau.Visibility.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.SystemEnv.*;
import static org.apache.juneau.reflect.ReflectionFilters.*;
import static java.util.Arrays.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
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
 * Context beans follow the convention of having the following parts:
 * <ul>
 * 	<li>A {@link Builder} class for configuring the context bean.
 * 	<ul>
 * 		<li>This bean is non-thread-safe and meant for one-time use.
 * 	</ul>
 * 	<li>A {@link Context#Context(Builder)} constructor that takes in a builder object.
 * 	<ul>
 * 		<li>This bean is thread-safe and cacheable/reusable.
 * 	</ul>
 * 	<li>A {@link Session} class for doing work.
 * 	<ul>
 * 		<li>This bean is non-thread-safe and meant for one-time use.
 * 	</ul>
 */
public abstract class Context implements MetaProvider {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Map<Class<?>,MethodInfo> BUILDER_CREATE_METHODS = new ConcurrentHashMap<>();

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
				mi = ClassInfo.of(type).getBuilderCreateMethod();
				if (mi == null)
					throw runtimeException("Could not find builder create method on class {0}", type);
				BUILDER_CREATE_METHODS.put(type, mi);
			}
			Builder b = (Builder)mi.invoke(null);
			b.type(type);
			return b;
		} catch (ExecutableException e) {
			throw new RuntimeException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static abstract class Builder {

		boolean debug;
		Class<?> type;
		Context impl;
		List<Annotation> annotations;

		private final List<Object> builders = new ArrayList<>();
		private final AnnotationWorkList applied = new AnnotationWorkList();

		/**
		 * Constructor.
		 * Default settings.
		 */
		protected Builder() {
			debug = env("Context.debug", false);
			annotations = null;
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
			annotations = copyFrom.annotations.isEmpty() ? null : new ArrayList<>(copyFrom.annotations);
			registerBuilders(this);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			debug = copyFrom.debug;
			type = copyFrom.type;
			annotations = copyFrom.annotations == null ? null : new ArrayList<>(copyFrom.annotations);
			registerBuilders(this);
		}

		/**
		 * Copy creator.
		 *
		 * @return A new mutable copy of this builder.
		 */
		public abstract Builder copy();

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
		 * Returns the {@link #impl(Context)} bean if it's the specified type.
		 *
		 * @param c The expected bean type.
		 * @return The impl bean, or <jk>null</jk> if an impl bean wasn't specified.
		 */
		protected <T extends Context> T impl(Class<T> c) {
			if (impl != null && c.isInstance(impl))
				return c.cast(impl);
			return null;
		}

		/**
		 * Build the object.
		 *
		 * @return
		 * 	The built object.
		 * 	<br>Subsequent calls to this method will create new instances (unless context object is cacheable).
		 */
		public Context build() {
			if (impl != null)
				return impl;
			if (type == null)
				throw runtimeException("Context class not specified.");
			try {
				ClassInfo ci = ClassInfo.of(type);
				ConstructorInfo cc = ci.getConstructor(isVisible(PROTECTED).and(hasParentArgs(this))).map(x -> x.accessible()).orElse(null);
				if (cc != null)
					return cc.invoke(this);
				throw runtimeException("Constructor not found for class {0}", type);
			} catch (ExecutableException e) {
				throw runtimeException(e, "Error occurred trying to create context.");
			}
		}

		/**
		 * Apply a consumer to this builder.
		 *
		 * @param subtype The builder subtype that this consumer can be applied to.
		 * @param consumer The consumer.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public <T extends Builder> Builder apply(Class<T> subtype, Consumer<T> consumer) {
			if (subtype.isInstance(this))
				consumer.accept((T)this);
			return this;
		}

		/**
		 * Associates a context class with this builder.
		 *
		 * @param value The context class that this builder should create.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder type(Class<? extends Context> value) {
			this.type = value;
			return this;
		}

		/**
		 * Returns the context class that this builder should create.
		 *
		 * @return The context class if it was specified.
		 */
		public Optional<Class<?>> getType() {
			return Optional.ofNullable(type);
		}

		/**
		 * Specifies a pre-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder impl(Context value) {
			impl = value;
			return this;
		}

		/**
		 * Returns <jk>true</jk> if any of the annotations/appliers can be applied to this builder.
		 *
		 * @param work The work to check.
		 * @return <jk>true</jk> if any of the annotations/appliers can be applied to this builder.
		 */
		public boolean canApply(AnnotationWorkList work) {
			for (AnnotationWork w : work)
				for (Object b : builders)
					if (w.canApply(b))
						return true;
			return false;
		}

		/**
		 * Applies a set of applied to this builder.
		 *
		 * <p>
		 * An {@link AnnotationWork} consists of a single pair of {@link AnnotationInfo} that represents an annotation instance,
		 * and {@link AnnotationApplier} which represents the code used to apply the values in that annotation to a specific builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// A class annotated with a config annotation.</jc>
		 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
		 * 	<jk>public class</jk> MyClass {...}
		 *
		 * 	<jc>// Find all annotations that themselves are annotated with @ContextPropertiesApply.</jc>
		 * 	List&lt;AnnotationWork&gt; <jv>work</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>)
		 * 		.getAnnotationList(ConfigAnnotationFilter.<jsf>INSTANCE</jsf>)
		 * 		.getWork(VarResolver.<jsf>DEFAULT</jsf>.createSession());
		 *
		 * 	<jc>// Apply any settings found on the annotations.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.apply(<jv>work</jv>)
		 * 		.build();
		 * </p>
		 *
		 * @param work The list of annotations and appliers to apply to this builder.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder apply(AnnotationWorkList work) {
			applied.addAll(work);
			for (AnnotationWork w : work)
				for (Object b : builders)
					w.apply(b);
			return this;
		}

		/**
		 * Returns all the annotations that have been applied to this builder.
		 *
		 * @return All the annotations that have been applied to this builder.
		 */
		public AnnotationWorkList getApplied() {
			return applied;
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
			for (Object b : builders) {
				if (b == this)
					this.builders.add(b);
				else if (b instanceof Builder)
					this.builders.addAll(((Builder)b).builders);
				else
					this.builders.add(b);
			}
		}

		/**
		 * Applies any of the various <ja>@XConfig</ja> annotations on the specified class to this context.
		 *
		 * <p>
		 * Any annotations found that themselves are annotated with {@link ContextApply} will be resolved and
		 * applied as properties to this builder.  These annotations include:
		 * <ul class='javatree'>
		 * 	<li class ='ja'>{@link BeanConfig}
		 * 	<li class ='ja'>{@link CsvConfig}
		 * 	<li class ='ja'>{@link HtmlConfig}
		 * 	<li class ='ja'>{@link HtmlDocConfig}
		 * 	<li class ='ja'>{@link JsoConfig}
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
		 * The default var resolver {@link VarResolver#DEFAULT} is used to resolve any variables in annotation field values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// A class annotated with a config annotation.</jc>
		 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
		 * 	<jk>public class</jk> MyClass {...}
		 *
		 * 	<jc>// Apply any settings found on the annotations.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.applyAnnotations(MyClass.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * @param fromClasses The classes on which the annotations are defined.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder applyAnnotations(Class<?>...fromClasses) {
			VarResolverSession vrs = VarResolver.DEFAULT.createSession();
			AnnotationWorkList work = stream(fromClasses)
				.map(ClassInfo::of)
				.map(x -> x.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs))
				.flatMap(Collection::stream)
				.collect(toCollection(AnnotationWorkList::new));
			return apply(work);
		}

		/**
		 * Applies any of the various <ja>@XConfig</ja> annotations on the specified method to this context.
		 *
		 * <p>
		 * Any annotations found that themselves are annotated with {@link ContextApply} will be resolved and
		 * applied as properties to this builder.  These annotations include:
		 * <ul class='javatree'>
		 * 	<li class ='ja'>{@link BeanConfig}
		 * 	<li class ='ja'>{@link CsvConfig}
		 * 	<li class ='ja'>{@link HtmlConfig}
		 * 	<li class ='ja'>{@link HtmlDocConfig}
		 * 	<li class ='ja'>{@link JsoConfig}
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
		 * <p class='bcode w800'>
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
		 * @param fromMethods The methods on which the annotations are defined.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder applyAnnotations(Method...fromMethods) {
			VarResolverSession vrs = VarResolver.DEFAULT.createSession();
			AnnotationWorkList work = stream(fromMethods)
				.map(MethodInfo::of)
				.map(x -> x.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs))
				.flatMap(Collection::stream)
				.collect(toCollection(AnnotationWorkList::new));
			return apply(work);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

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
		 * <p class='bcode w800'>
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
		 * <p class='bcode w800'>
		 * 	<jc>// Create a concrete @Bean annotation.</jc>
		 * 	BeanAnnotation <jv>annotation</jv> = BeanAnnotation.<jsm>create</jsm>(B.<jk>class</jk>).properties(<js>"street,city,state"</js>);
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
		 * <ul class='javatree'>
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
		 * 	<li class='ja'>{@link org.apache.juneau.jso.annotation.JsoAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.json.annotation.JsonAnnotation}
		 * 	<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.SchemaAnnotation}
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
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link BeanConfig}
		 * </ul>
		 *
		 * @param values
		 * 	The annotations to register with the context.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder annotations(Annotation...values) {
			if (annotations == null)
				annotations = new ArrayList<>();
			Collections.addAll(annotations, values);
			return this;
		}

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
		 * 		Enables {@link BeanTraverseBuilder#detectRecursions()}.
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
		 * <p class='bcode w800'>
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
		 * 	MyBean <jv>myBean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>myBean</jv>.<jf>f</jf> = <jv>myBean</jv>;
		 *
		 * 	<jc>// Throws a SerializeException and not a StackOverflowError</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>myBean</jv>);
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
		 * 	<li class='jm'>{@link org.apache.juneau.Context.Args#debug(Boolean)}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder debug() {
			return debug(true);
		}

		/**
		 * Same as {@link #debug()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debug(boolean value) {
			debug = value;
			return this;
		}

		/**
		 * Returns <jk>true</jk> if debug is enabled.
		 *
		 * @return <jk>true</jk> if debug is enabled.
		 */
		public boolean isDebug() {
			return debug;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Session
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
	 */
	public static abstract class Session {

		private final OMap properties;
		private Map<String,Object> cache;
		private List<String> warnings;	// Any warnings encountered.

		private final Context ctx;
		private final boolean debug;
		private final boolean unmodifiable;


		/**
		 * Default constructor.
		 *
		 * @param ctx The context object.
		 * @param args
		 * 	Runtime arguments.
		 */
		protected Session(Context ctx, Args args) {
			this.ctx = ctx;
			this.unmodifiable = args.unmodifiable;
			OMap sp = args.properties;
			if (args.unmodifiable)
				sp = sp.unmodifiable();
			properties = sp;
			debug = ofNullable(args.debug).orElse(ctx.isDebug());
		}

		/**
		 * Returns the session properties on this session.
		 *
		 * @return The session properties on this session.  Never <jk>null</jk>.
		 */
		public final OMap getSessionProperties() {
			return properties;
		}

		/**
		 * Returns the context that created this session.
		 *
		 * @return The context that created this session.
		 */
		public Context getContext() {
			return ctx;
		}

		/**
		 * Adds an arbitrary object to this session's cache.
		 *
		 * <p>
		 * Can be used to store objects for reuse during a session.
		 *
		 * @param key The key.  Can be any string.
		 * @param val The cached object.
		 */
		public final void addToCache(String key, Object val) {
			if (unmodifiable)
				return;
			if (cache == null)
				cache = new TreeMap<>();
			cache.put(key, val);
		}

		/**
		 * Adds arbitrary objects to this session's cache.
		 *
		 * <p>
		 * Can be used to store objects for reuse during a session.
		 *
		 * @param cacheObjects
		 * 	The objects to add to this session's cache.
		 * 	No-op if <jk>null</jk>.
		 */
		public final void addToCache(Map<String,Object> cacheObjects) {
			if (unmodifiable)
				return;
			if (cacheObjects != null) {
				if (cache == null)
					cache = new TreeMap<>();
				cache.putAll(cacheObjects);
			}
		}

		/**
		 * Returns an object stored in the session cache.
		 *
		 * @param c The class type of the object.
		 * @param key The session object key.
		 * @return The cached object, or <jk>null</jk> if it doesn't exist.
		 */
		@SuppressWarnings("unchecked")
		public final <T> T getFromCache(Class<T> c, String key) {
			return cache == null ? null : (T)cache.get(key);
		}

		/**
		 * Logs a warning message.
		 *
		 * @param msg The warning message.
		 * @param args Optional {@link MessageFormat}-style arguments.
		 */
		public void addWarning(String msg, Object... args) {
			if (unmodifiable)
				return;
			if (warnings == null)
				warnings = new LinkedList<>();
			warnings.add((warnings.size() + 1) + ": " + format(msg, args));
		}

		/**
		 * Returns <jk>true</jk> if warnings occurred in this session.
		 *
		 * @return <jk>true</jk> if warnings occurred in this session.
		 */
		public final boolean hasWarnings() {
			return warnings != null && warnings.size() > 0;
		}

		/**
		 * Returns the warnings that occurred in this session.
		 *
		 * @return The warnings that occurred in this session, or <jk>null</jk> if no warnings occurred.
		 */
		public final List<String> getWarnings() {
			return warnings;
		}

		/**
		 * Throws a {@link BeanRuntimeException} if any warnings occurred in this session.
		 */
		public void checkForWarnings() {
			if (warnings != null && ! warnings.isEmpty())
				throw new BeanRuntimeException("Warnings occurred in session: \n" + join(getWarnings(), "\n"));
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Configuration properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Configuration property:  Debug mode.
		 *
		 * @see Context.Builder#debug()
		 * @return
		 * 	<jk>true</jk> if debug mode is enabled.
		 */
		public boolean isDebug() {
			return debug;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Other methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the properties defined on this bean as a simple map for debugging purposes.
		 *
		 * <p>
		 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
		 *
		 * @return A new map containing this bean's properties.
		 */
		public OMap toMap() {
			return OMap.create().filtered();
		}

		@Override /* Object */
		public String toString() {
			return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Args
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Runtime arguments bean.
	 *
	 * <p>
	 * These are passed to {@link Session} beans to modify select settings defined on the {@link Context} bean.
	 */
	@FluentSetters
	public static class Args {

		OMap properties = OMap.create();
		boolean unmodifiable;
		Boolean debug;

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Debug mode.
		 *
		 * <p>
		 * Enables the following additional information during parsing:
		 * <ul>
		 * 	<li> When bean setters throws exceptions, the exception includes the object stack information in order to determine how that method was invoked.
		 * </ul>
		 *
		 * <p>
		 * If not specified, defaults to {@link Context.Builder#debug()}.
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
		 * 	<li class='jm'>{@link org.apache.juneau.Context.Builder#debug()}
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Args debug(Boolean value) {
			debug = value;
			return this;
		}

		/**
		 * Create an unmodifiable session.
		 *
		 * <p>
		 * The created Session object will be unmodifiable which makes it suitable for caching and reuse.
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Args unmodifiable() {
			this.unmodifiable = true;
			return this;
		}

		/**
		 * Session-level properties.
		 *
		 * <p>
		 * Overrides context-level properties.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Args properties(Map<String,Object> value) {
			this.properties = OMap.of(value);
			return this;
		}

		/**
		 * Adds a property to this session.
		 *
		 * @param key The property key.
		 * @param value The property value.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Args property(String key, Object value) {
			if (value == null) {
				properties.remove(key);
			} else {
				properties.put(key, value);
			}
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Other methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the properties defined on this bean as a simple map for debugging purposes.
		 *
		 * <p>
		 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
		 *
		 * @return A new map containing this bean's properties.
		 */
		public OMap toMap() {
			return OMap
				.create()
				.filtered()
				.append("Args", OMap.create().filtered()
					.append("properties", properties)
				);
		}

		@Override /* Object */
		public String toString() {
			return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final List<Annotation> annotations;
	final boolean debug;

	private final ReflectionMap<Annotation> annotationMap;

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The context to copy from.
	 */
	protected Context(Context copyFrom) {
		annotationMap = copyFrom.annotationMap;
		annotations = copyFrom.annotations;
		debug = copyFrom.debug;
	}

	/**
	 * Constructor for this class.
	 *
	 * @param builder The builder for this class.
	 */
	protected Context(Builder builder) {
		debug = builder.debug;
		annotations = ofNullable(builder.annotations).orElse(emptyList());

		ReflectionMap.Builder<Annotation> rmb = ReflectionMap.create(Annotation.class);

		for (Annotation a : annotations) {
			try {
				ClassInfo ci = ClassInfo.of(a.getClass());

				MethodInfo mi = ci.getMethod("onClass");
				if (mi != null) {
					if (! mi.getReturnType().is(Class[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an onClass() method that returns a Class array.", a.getClass().getSimpleName());
					for (Class<?> c : (Class<?>[])mi.accessible().invoke(a))
						rmb.append(c.getName(), a);
				}

				mi = ci.getMethod("on");
				if (mi != null) {
					if (! mi.getReturnType().is(String[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an on() method that returns a String array.", a.getClass().getSimpleName());
					for (String s : (String[])mi.accessible().invoke(a))
						rmb.append(s, a);
				}

			} catch (Exception e) {
				throw new ConfigException(e, "Invalid annotation @{0} used in BEAN_annotations property.", className(a));
			}
		}
		this.annotationMap = rmb.build();
	}

	/**
	 * Creates a builder from this context object.
	 *
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 *
	 * @return A new ContextBuilder object.
	 */
	public abstract Builder copy();

	/**
	 * Creates an {@link Args} bean to pass to the {@link #createSession(Args)} method.
	 *
	 * <p>
	 * The {@link Args} bean can be used to override select settings defined on this context when creating session beans.
	 *
	 * @return A new Args bean.
	 */
	public Args createArgs() {
		return new Args();
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public Session createSession() {
		return createSession(defaultArgs());
	}

	/**
	 * Create a new session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public abstract Session createSession(Args args);

	/**
	 * Defines default session arguments used when calling the {@link #createSession()} method.
	 *
	 * @return A SessionArgs object, possibly a read-only reusable instance.
	 */
	public abstract Args defaultArgs();

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode.
	 *
	 * @see Context.Builder#debug()
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return debug;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MetaProvider methods
	//-----------------------------------------------------------------------------------------------------------------

	private static final boolean DISABLE_ANNOTATION_CACHING = ! Boolean.getBoolean("juneau.disableAnnotationCaching");

	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> classAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> declaredClassAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Method,Class<? extends Annotation>,List<Annotation>> methodAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Field,Class<? extends Annotation>,List<Annotation>> fieldAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Constructor<?>,Class<? extends Annotation>,List<Annotation>> constructorAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = classAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotationMap.appendAll(c, a, l);
			aa = l.unmodifiable();
			classAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getDeclaredAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = declaredClassAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getDeclaredAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotationMap.appendAll(c, a, l);
			aa = l.unmodifiable();
			declaredClassAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Method m) {
		if (a == null || m == null)
			return emptyList();
		List<Annotation> aa = methodAnnotationCache.get(m, a);
		if (aa == null) {
			A[] x = m.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotationMap.appendAll(m, a, l);
			aa = l.unmodifiable();
			methodAnnotationCache.put(m, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner());
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Method m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, MethodInfo m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Field f) {
		if (a == null || f == null)
			return emptyList();
		List<Annotation> aa = fieldAnnotationCache.get(f, a);
		if (aa == null) {
			A[] x = f.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotationMap.appendAll(f, a, l);
			aa = l.unmodifiable();
			fieldAnnotationCache.put(f, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null: f.inner());
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Constructor<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = constructorAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotationMap.appendAll(c, a, l);
			aa = l.unmodifiable();
			constructorAnnotationCache.put(c, a, l);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner());
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Class<?> c) {
		return getAnnotations(a, c).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ClassInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Method m) {
		return getAnnotations(a, m).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,f)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param f The field being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified field.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null : f.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The constructor being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified constructor.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		if (SimpleJsonSerializer.DEFAULT_READABLE == null)
			return super.toString();
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a(
				"Context",
				OMap
					.create()
					.filtered()
					.a("annotations", annotations)
					.a("debug", debug)
			);
	}
}
