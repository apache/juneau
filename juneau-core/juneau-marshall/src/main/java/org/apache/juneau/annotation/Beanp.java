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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Used tailor how bean properties get interpreted by the framework.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Methods/Fields - Bean getters/setters and properties.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified.
 * </ul>
 * <p>
 * This annotation is applied to public fields and public getter/setter methods of beans.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.BeanpAnnotation">@Beanp Annotation</a>
 * </ul>
 */
@Documented
@Target({FIELD,METHOD,PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(BeanpAnnotation.Array.class)
@ContextApply(BeanpAnnotation.Applier.class)
public @interface Beanp {

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary this bean property.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#dictionary()}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary()}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property.
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Specifies a String format for converting the bean property value to a formatted string.
	 *
	 * <p>
	 * Note that this is usually a one-way conversion during serialization.
	 *
	 * <p>
	 * During parsing, we will attempt to convert the value to the original form by using the
	 * {@link BeanSession#convertToType(Object, Class)} but there is no guarantee that this will succeed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Beanp</ja>(format=<js>"$%.2f"</js>)
	 * 	<jk>public float</jk> <jf>price</jf>;
	 * </p>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property like so:
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<ja>@Beanp</ja>(format=<js>"$%.2f"</js>)
	 * 		<jk>private float</jk> <jf>price</jf>;
	 *
	 * 		<jk>public float</jk> getPrice() {
	 * 			<jk>return</jk> <jf>price</jf>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * Identifies the name of the property.
	 *
	 * <p>
	 * Normally, this is automatically inferred from the field name or getter method name of the property.
	 * However, this property can be used to assign a different property name from the automatically inferred value.
	 *
	 * <p>
	 * If the {@link org.apache.juneau.BeanContext.Builder#beanFieldVisibility(Visibility)} setting on the bean context excludes this field (e.g. the
	 * visibility is set to PUBLIC, but the field is PROTECTED), this annotation can be used to force the field to be
	 * identified as a property.
	 *
	 * <h5 class='topic'>Dynamic beans</h5>
	 * <p>
	 * The bean property named <js>"*"</js> is the designated "dynamic property" which allows for "extra" bean
	 * properties not otherwise defined.
	 * This is similar in concept to the Jackson <ja>@JsonGetterAll</ja> and <ja>@JsonSetterAll</ja> annotations.
	 * The primary purpose is for backwards compatibility in parsing newer streams with addition information into older
	 * beans.
	 *
	 * <p>
	 * The following examples show how to define dynamic bean properties.
	 * <p class='bjava'>
	 * 	<jc>// Option #1 - A simple public Map field.
	 * 	// The field name can be anything.</jc>
	 * 	<jk>public class</jk> BeanWithDynaField {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; <jf>extraStuff</jf> = <jk>new</jk> LinkedHashMap&lt;&gt;();
	 * 	}
	 *
	 * 	<jc>// Option #2 - Getters and setters.
	 * 	// Method names can be anything.
	 * 	// Getter must return a Map with String keys.
	 * 	// Setter must take in two arguments.</jc>
	 * 	<jk>public class</jk> BeanWithDynaMethods {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public void</jk> setAnExtraField(String <jv>name</jv>, Object <jv>value</jv>) {
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #3 - Getter only.
	 * 	// Properties will be added through the getter.</jc>
	 * 	<jk>public class</jk> BeanWithDynaGetterOnly {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #4 - Getter, setter, and extra-keys method .
	 * 	// Define a method that returns a Collection&lt;String&gt; with currently-set property names.</jc>
	 * 	<jk>public class</jk> BeanWithDynaExtraKeys {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Object get(String <jv>name</jv>) {
	 * 			...
	 * 		}
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public void</jk> set(String <jv>name</jv>, Object <jv>value</jv>) {
	 * 			...
	 * 		}
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Collection&lt;String&gt; extraKeys() {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 *<p>
	 * Similar rules apply for value types and swaps.
	 * The property values optionally can be any serializable type or use swaps.
	 * <p class='bjava'>
	 * 	<jc>// A serializable type other than Object.</jc>
	 * 	<jk>public class</jk> BeanWithDynaFieldWithListValues {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,List&lt;String&gt;&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// A swapped value.</jc>
	 * 	<jk>public class</jk> BeanWithDynaFieldWithSwappedValues {
	 *
	 * 		<ja>@Beanp</ja>(name=<js>"*"</js>, swap=TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
	 * 		<jk>public</jk> Map&lt;String,Calendar&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <div class='info'>
	 * 	Note that if you're not interested in these additional properties, you can also use the
	 * 	{@link org.apache.juneau.BeanContext.Builder#ignoreUnknownBeanProperties()} setting to ignore values that don't fit into existing
	 * 	properties.
	 * </div>
	 * <div class='info'>
	 * 		Note that the {@link Name @Name} annotation can also be used for identifying a property name.
	 * </div>
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * Dynamically apply this annotation to the specified fields/methods.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing field/method.
	 * It is ignored when the annotation is applied directly to fields/methods.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
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
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * For bean properties of maps and collections, this annotation can be used to identify the class types of the
	 * contents of the bean property object when the generic parameter types are interfaces or abstract classes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type with String keys and Integer values.</jc>
	 * 		<ja>@Beanp</ja>(type=HashMap.<jk>class</jk>, params={String.<jk>class</jk>,Integer.<jk>class</jk>})
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property like so:
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<ja>@Beanp</ja>(type=HashMap.<jk>class</jk>, params={String.<jk>class</jk>,Integer.<jk>class</jk>})
	 * 		<jk>private</jk> Map <jf>p1</jf>;
	 *
	 * 		<jk>public</jk> Map getP1() {
	 * 			<jk>return</jk> <jf>p1</jf>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] params() default {};

	/**
	 * Used to limit which child properties are rendered by the serializers.
	 *
	 * <p>
	 * Can be used on any of the following bean property types:
	 * <ul class='spaced-list'>
	 * 	<li>Beans - Only render the specified properties of the bean.
	 * 	<li>Maps - Only render the specified entries in the map.
	 * 	<li>Bean/Map arrays - Same, but applied to each element in the array.
	 * 	<li>Bean/Map collections - Same, but applied to each element in the collection.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyClass {
	 *
	 * 		<jc>// Only render 'f1' when serializing this bean property.</jc>
	 * 		<ja>@Beanp</ja>(properties=<js>"f1"</js>)
	 * 		<jk>public</jk> MyChildClass <jf>x1</jf> = <jk>new</jk> MyChildClass();
	 * 	}
	 *
	 * 	<jk>public class</jk> MyChildClass {
	 * 		<jk>public int</jk> <jf>f1</jf> = 1;
	 * 		<jk>public int</jk> <jf>f2</jf> = 2;
	 * 	}
	 *
	 * 	<jc>// Renders "{x1:{f1:1}}"</jc>
	 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> MyClass());
	 * </p>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property like so:
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<ja>@Beanp</ja>(properties=<js>"f1"</js>)
	 * 		<jk>private</jk> MyChildClass <jf>x1</jf>;
	 *
	 * 		<jk>public</jk> MyChildClass getX1() {
	 * 			<jk>return</jk> <jf>x1</jf>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String properties() default "";

	/**
	 * Identifies a property as read-only.
	 *
	 * <p>
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@Beanp</ja>(ro=<js>"true"</js>)
	 * 		<jk>public float</jk> <jf>price</jf>;
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(Map)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ro() default "";

	/**
	 * Identifies a specialized class type for the property.
	 *
	 * <p>
	 * Normally this can be inferred through reflection of the field type or getter return type.
	 * However, you'll want to specify this value if you're parsing beans where the bean property class is an interface
	 * or abstract class to identify the bean type to instantiate.
	 * Otherwise, you may cause an {@link InstantiationException} when trying to set these fields.
	 *
	 * <p>
	 * This property must denote a concrete bean class with a no-arg constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type.</jc>
	 * 		<ja>@Beanp</ja>(type=HashMap.<jk>class</jk>)
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property like so:
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<ja>@Beanp</ja>(type=HashMap.<jk>class</jk>)
	 * 		<jk>private</jk> Map <jf>p1</jf>;
	 *
	 * 		<jk>public</jk> Map getP1() {
	 * 			<jk>return</jk> <jf>p1</jf>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<?> type() default void.class;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * The following annotations are equivalent:
	 *
	 * <p class='bjava'>
	 * 	<ja>@Beanp</ja>(name=<js>"foo"</js>)
	 *
	 * 	<ja>@Beanp</ja>(<js>"foo"</js>)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";

	/**
	 * Identifies a property as write-only.
	 *
	 * <p>
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@Beanp</ja>(wo=<js>"true"</js>)
	 * 		<jk>public float</jk> <jf>price</jf>;
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(Map)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String wo() default "";
}
