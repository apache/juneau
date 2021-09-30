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

import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.reflect.ReflectionFilters.*;
import static org.apache.juneau.Visibility.*;
import static java.util.stream.Collectors.*;
import static java.util.Arrays.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jso.annotation.*;
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
 * Base builder class for building instances of any context objects configured through property stores.
 * {@review}
 */
@FluentSetters
public abstract class ContextBuilder {

	/** Contains all the modifiable settings for the implementation class. */
	private final ContextPropertiesBuilder cpb;

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
	protected ContextBuilder() {
		cpb = ContextProperties.create();
		debug = env("Context.debug", false);
		annotations = null;
		registerBuilders(this, cpb);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected ContextBuilder(Context copyFrom) {
		cpb = copyFrom.properties.copy();
		debug = copyFrom.debug;
		type = copyFrom.getClass();
		annotations = copyFrom.annotations.isEmpty() ? null : new ArrayList<>(copyFrom.annotations);
		registerBuilders(this, cpb);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected ContextBuilder(ContextBuilder copyFrom) {
		cpb =  new ContextPropertiesBuilder(copyFrom.cpb);
		debug = copyFrom.debug;
		type = copyFrom.type;
		annotations = copyFrom.annotations == null ? null : new ArrayList<>(copyFrom.annotations);
		registerBuilders(this, cpb);
	}

	/**
	 * Copy creator.
	 *
	 * @return A new mutable copy of this builder.
	 */
	public abstract ContextBuilder copy();

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
			cc = ci.getPublicConstructor(cpb);
			if (cc != null)
				return cc.invoke(cpb);
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
	public <T extends ContextBuilder> ContextBuilder apply(Class<T> subtype, Consumer<T> consumer) {
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
	public ContextBuilder type(Class<? extends Context> value) {
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
	public ContextBuilder impl(Context value) {
		impl = value;
		return this;
	}

	/**
	 * Copies the settings from the specified property store into this builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a free-form set of properties.</jc>
	 * 	ContextProperties <jv>properties</jv> = ContextProperties
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Create a serializer that uses those settings.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.apply(<jv>properties</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param copyFrom The property store whose settings are being copied.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder apply(ContextProperties copyFrom) {
		this.cpb.apply(copyFrom);
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
	public ContextBuilder apply(AnnotationWorkList work) {
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
			else if (b instanceof ContextBuilder)
				this.builders.addAll(((ContextBuilder)b).builders);
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
	public ContextBuilder applyAnnotations(Class<?>...fromClasses) {
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
	public ContextBuilder applyAnnotations(Method...fromMethods) {
		VarResolverSession vrs = VarResolver.DEFAULT.createSession();
		AnnotationWorkList work = stream(fromMethods)
			.map(MethodInfo::of)
			.map(x -> x.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs))
			.flatMap(Collection::stream)
			.collect(toCollection(AnnotationWorkList::new));
		return apply(work);
	}

	/**
	 * Build a new instance of the specified object.
	 *
	 * <p>
	 * Creates a new instance of the specified context-based class, or an existing instance if one with the equivalent
	 * property store was already created.
	 *
	 * @param c The subclass of {@link Context} to instantiate.
	 * @return A new object using the settings defined in this builder.
	 */

	public <T extends Context> T build(Class<T> c) {
		return ContextCache.INSTANCE.create(c, getContextProperties());
	}

	/**
	 * Returns a read-only snapshot of the current properties on this builder.
	 *
	 * @return A property store object.
	 */
	public ContextProperties getContextProperties() {
		return cpb.build();
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
	public ContextBuilder annotations(Annotation...values) {
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
	 * 	<li class='jm'>{@link org.apache.juneau.SessionArgs#debug(Boolean)}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder debug() {
		return debug(true);
	}

	/**
	 * Same as {@link #debug()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public ContextBuilder debug(boolean value) {
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

	/**
	 * Sets a free-form configuration property on this object.
	 *
	 * <p>
	 * Provides the ability to specify configuration property values in a generic fashion.
	 *
	 * <p>
	 * Property names must have the following format that identify their datatype...
	 * <p class='bcode w800'>
	 * 	<js>"{class}.{name}.{type}"</js>
	 * </p>
	 * <p>
	 * ...where the parts consist of the following...
	 * <ul>
	 * 	<li><js>"{class}"</js> - The group name of the property (e.g. <js>"JsonSerializer"</js>).
	 * 		<br>It's always going to be the simple class name of the class it's associated with.
	 * 	<li><js>"{name}"</js> - The property name (e.g. <js>"useWhitespace"</js>).
	 * 	<li><js>"{type}"</js> - The property data type.
	 * 		<br>A 1 or 2 character string that identifies the data type of the property.
	 * 		<br>Valid values are:
	 * 		<ul>
	 * 			<li><js>"s"</js> - <c>String</c>
	 * 			<li><js>"b"</js> - <c>Boolean</c>
	 * 			<li><js>"i"</js> - <c>Integer</c>
	 * 			<li><js>"c"</js> - <c>Class</c>
	 * 			<li><js>"o"</js> - <c>Object</c>
	 * 			<li><js>"ss"</js> - <c>TreeSet&lt;String&gt;</c>
	 * 			<li><js>"si"</js> - <c>TreeSet&lt;Integer&gt;</c>
	 * 			<li><js>"sc"</js> - <c>TreeSet&lt;Class&gt;</c>
	 * 			<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 			<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 			<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 			<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * 			<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 			<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 			<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 			<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 			<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 			<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 			<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 			<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * 		</ul>
	 * </ul>
	 *
	 * <p>
	 * For example, <js>"BeanContext.swaps.lc"</js> refers to a property on the <l>BeanContext</l> class
	 * called <l>swaps</l> that has a data type of <l>List&lt;Class&gt;</l>.
	 *
	 * <p>
	 * Property values get 'normalized' when they get set.
	 * For example, calling <c>set(<js>"BeanContext.debug.b"</js>, <js>"true"</js>)</c> will cause the property
	 * value to be converted to a boolean.  This allows the underlying {@link ContextProperties} class to be comparable
	 * and useful in determining whether a cached instance of a context object can be returned.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts maps and bean properties.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * As a general rule, builders don't typically have "unsetter" methods.  For example, once you've set strict
	 * mode on the <l>ParserBuilder</l> class, a method does not exist for unsetting it.
	 * This method can be used in these rare cases where you need to unset a value by setting it to <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Clone an existing serializer and unset the sort settings.</jc>
	 * 	<jv>serializer</jv> = <jv>serializer</jv>.<jsm>builder</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>, <jk>null</jk>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>, <jk>null</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The property value.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(String name, Object value) {
		cpb.set(name, value);
		return this;
	}

	/**
	 * Convenience method for setting a boolean property to <jk>true</jk>.
	 *
	 * @param name The property name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(String name) {
		Assertions.assertString(name).msg("Property ''{0}'' is not boolean.", name).endsWith(".b");
		cpb.set(name);
		return this;
	}

	/**
	 * Convenience method for setting a property to <jk>null</jk>.
	 *
	 * @param name The property name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder unset(String name) {
		cpb.unset(name);
		return this;
	}

	/**
	 * Peeks at a free-form configuration property on this object.
	 *
	 * <p>
	 * Allows you to look at the raw value of a configuration property while it's still in the builder.
	 *
	 * @param key The property name.
	 * @return
	 * 	The raw value of the configuration property as it was passed in through this API.
	 * 	<br><jk>null</jk> if not set.
	 */
	public Object peek(String key) {
		return cpb.peek(key);
	}

	/**
	 * Peeks at a configuration property on this object.
	 *
	 * <p>
	 * Allows you to look at the raw value of a configuration property while it's still in the builder.
	 *
	 * <p>
	 * Converts the value from the current raw form into the specified data type.
	 *
	 * @param c The type to convert to.
	 * @param key The property name.
	 * @return
	 * 	The converted value of the configuration property after conversion from the raw value.
	 * 	<br><jk>null</jk> if not set.
	 * @param <T> The type to convert to.
	 */
	public <T> T peek(Class<T> c, String key) {
		return cpb.peek(c, key);
	}

	/**
	 * Sets multiple free-form configuration properties on this object replacing all previous values.
	 *
	 * <p>
	 * Identical in function to {@link #set(String, Object)} but allows you to specify multiple values at once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts maps and bean properties.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(
	 * 			AMap.<jsm>of</jsm>(
	 * 				<jsf>BEAN_sortMaps</jsf>, <jk>true</jk>,
	 * 				<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>
	 * 			)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param properties
	 * 	The properties to set on this class.
	 * 	<br>The keys must be strings.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(Map<String,Object> properties) {
		cpb.set(properties);
		return this;
	}

	/**
	 * Adds multiple free-form configuration properties on this object without first clearing out any previous values.
	 *
	 * <p>
	 * Identical in function to {@link #set(String, Object)} but allows you to specify multiple values at once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts bean properties.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.add(
	 * 			AMap.<jsm>of</jsm>(
	 * 				<jsf>BEAN_sortMaps</jsf>, <jk>true</jk>,
	 * 				<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>
	 * 			)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param properties
	 * 	The properties to set on this class.
	 * 	<br>The keys must be strings.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder add(Map<String,Object> properties) {
		cpb.add(properties);
		return this;
	}

	/**
	 * Adds a free-form value to a SET property.
	 *
	 * <p>
	 * SET properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ss"</js> - <c>TreeSet&lt;String&gt;</c>
	 * 	<li><js>"si"</js> - <c>TreeSet&lt;Integer&gt;</c>
	 * 	<li><js>"sc"</js> - <c>TreeSet&lt;Class&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContextBuilder#notBeanClasses(Class...)} property which has the value <js>"BeanContext.notBeanClasses.sc"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that forces MyNotBean classes to be converted to strings.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_notBeanClasses</jsf>, MyNotBean.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The new value to add to the SET property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ss"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"si"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"sc"</js> - Only <l>Class</l> objects are allowed.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	@FluentSetter
	public ContextBuilder addTo(String name, Object value) {
		cpb.addTo(name, value);
		return this;
	}

	/**
	 * Adds a free-form value to the end of a LIST property.
	 *
	 * <p>
	 * LIST properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 	<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 	<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 	<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContextBuilder#swaps(Class...)} setting which has the value <js>"BeanContext.swaps.lo"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that converts Temporal objects to Basic ISO date strings.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.appendTo(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * The new value to add to the LIST property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ls"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"li"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"lc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"lo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a LIST property.
	 */
	@FluentSetter
	public ContextBuilder appendTo(String name, Object value) {
		cpb.appendTo(name, value);
		return this;
	}

	/**
	 * Adds a free-form value to the beginning of a LIST property.
	 *
	 * <p>
	 * LIST properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 	<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 	<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 	<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContextBuilder#swaps(Class...)} property which has the value <js>"BeanContext.swaps.lo"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that converts Temporal objects to Basic ISO date strings.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.prependTo(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The new value to add to the LIST property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ls"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"li"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"lc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"lo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a LIST property.
	 */
	@FluentSetter
	public ContextBuilder prependTo(String name, Object value) {
		cpb.prependTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a free-form entry in a MAP property.
	 *
	 * <p>
	 * MAP properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 	<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 	<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 	<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 	<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 	<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value
	 * 	The property value map value.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"sms"</js>,<js>"oms"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"smi"</js>,<js>"omi"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"smc"</js>,<js>"omc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"smo"</js>,<js>"omo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	@FluentSetter
	public ContextBuilder putTo(String name, String key, Object value) {
		cpb.putTo(name, key, value);
		return this;
	}

	/**
	 * Adds or overwrites multiple free-form entries in a MAP property.
	 *
	 * <p>
	 * MAP properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 	<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 	<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 	<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 	<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 	<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	Either a JSON Object string or a {@link Map} whose valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"sms"</js>,<js>"oms"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"smi"</js>,<js>"omi"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"smc"</js>,<js>"omc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"smo"</js>,<js>"omo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	@FluentSetter
	public ContextBuilder putAllTo(String name, Object value) {
		cpb.putAllTo(name, value);
		return this;
	}

	/**
	 * Removes a free-form value from a SET, LIST, or MAP property.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that specifies the concrete implementation class for an interface.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.swaps(TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Clone the previous serializer but remove the swap.</jc>
	 * 	<jv>serializer</jv> = <jv>serializer</jv>
	 * 		.<jsm>builder</jsm>()
	 * 		.removeFrom(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The property value in the SET/LIST/MAP property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET/LIST/MAP property.
	 */
	@FluentSetter
	public ContextBuilder removeFrom(String name, Object value) {
		cpb.removeFrom(name, value);
		return this;
	}

	@SuppressWarnings("javadoc")
	public ContextBuilder setIfNotEmpty(String name, Object value) {
		cpb.setIfNotEmpty(name, value);
		return this;
	}

	@SuppressWarnings("javadoc")
	public ContextBuilder setIf(boolean b, String name, Object value) {
		cpb.setIf(b, name, value);
		return this;
	}

	@SuppressWarnings("javadoc")
	public ContextBuilder appendToIfNotEmpty(String name, Object value) {
		cpb.appendToIfNotEmpty(name, value);
		return this;
	}

	@SuppressWarnings("javadoc")
	public ContextBuilder addToIfNotEmpty(String name, Object value) {
		cpb.addToIfNotEmpty(name, value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Looks up a default value from the environment.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param name The property name.
	 * @param def The default value if not found.
	 * @return The default value.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T> T env(String name, T def) {
		String s = System.getProperty(name);
		if (s == null)
			s = System.getenv(envName(name));
		if (s == null)
			return def;
		Class<?> c = def.getClass();
		if (c.isEnum())
			return (T)Enum.valueOf((Class<? extends Enum>) c, s);
		Function<String,T> f = (Function<String,T>)ENV_FUNCTIONS.get(c);
		if (f == null)
			throw runtimeException("Invalid env type: {0}", c);
		return f.apply(s);
	}

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();
	static {
		ENV_FUNCTIONS.put(String.class, x -> x);
		ENV_FUNCTIONS.put(Boolean.class, x -> Boolean.valueOf(x));
		ENV_FUNCTIONS.put(Charset.class, x -> Charset.forName(x));
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();
	private static String envName(String name) {
		String name2 = PROPERTY_TO_ENV.get(name);
		if (name2 == null) {
			name2 = name.toUpperCase().replace(".", "_");
			PROPERTY_TO_ENV.put(name, name2);
		}
		return name2;
	}

	/**
	 * Creates a list from an array of objects.
	 *
	 * @param objects The objects to create a list from.
	 * @return A new list not backed by the array.
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> list(T...objects) {
		return new ArrayList<>(Arrays.asList(objects));
	}

	/**
	 * Appends to an existing list.
	 *
	 * @param list The list to append to.
	 * @param objects The objects to append.
	 * @return The same list, or a new list if the list was <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> append(List<T> list, T...objects) {
		if (list == null)
			return list(objects);
		list.addAll(Arrays.asList(objects));
		return list;
	}

	/**
	 * Prepends to an existing list.
	 *
	 * @param list The list to prepend to.
	 * @param objects The objects to prepend.
	 * @return The same list, or a new list if the list was <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> prepend(List<T> list, T...objects) {
		if (list == null)
			return list(objects);
		list.addAll(0, Arrays.asList(objects));
		return list;
	}

	// <FluentSetters>

	// </FluentSetters>
}