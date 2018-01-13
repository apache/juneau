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

import org.apache.juneau.*;

/**
 * Used tailor how bean properties get interpreted by the framework.
 *
 * <p>
 * This annotation is applied to public fields and public getter/setter methods of beans.
 * 
 * <h6 class='topic'>Documentation</h6>
 *	<ul>
 *		<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.BeanPropertyAnnotation">Overview &gt; @BeanProperty Annotation</a>
 *	</ul>
 */
@Documented
@Target({FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface BeanProperty {

	/**
	 * Identifies the name of the property.
	 *
	 * <p>
	 * Normally, this is automatically inferred from the field name or getter method name of the property.
	 * However, this property can be used to assign a different property name from the automatically inferred value.
	 *
	 * <p>
	 * If the {@link BeanContext#BEAN_beanFieldVisibility} setting on the bean context excludes this field (e.g. the
	 * visibility is set to PUBLIC, but the field is PROTECTED), this annotation can be used to force the field to be
	 * identified as a property.
	 *
	 * <h6 class='topic'>Dynamic beans</h6>
	 * <p>
	 * The bean property named <js>"*"</js> is the designated "dynamic property" which allows for "extra" bean
	 * properties not otherwise defined.
	 * This is similar in concept to the Jackson <ja>@JsonGetterAll</ja> and <ja>@JsonSetterAll</ja> annotations.
	 * The primary purpose is for backwards compatibility in parsing newer streams with addition information into older
	 * beans.
	 *
	 * <p>
	 * The following examples show how to define dynamic bean properties.
	 * <p class='bcode'>
	 * 	<jc>// Option #1 - A simple public Map field.
	 * 	// The field name can be anything.</jc>
	 * 	<jk>public class</jk> BeanWithDynaField {
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; extraStuff = <jk>new</jk> LinkedHashMap&lt;String,Object&gt;();
	 * 	}
	 *
	 * 	<jc>// Option #2 - Getters and setters.
	 * 	// Method names can be anything.
	 * 	// Getter must return a Map with String keys.
	 * 	// Setter must take in two arguments.</jc>
	 * 	<jk>public class</jk> BeanWithDynaMethods {
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>)
	 * 		<jk>public void</jk> setAnExtraField(String name, Object value) {
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #3 - Getter only.
	 * 	// Properties will be added through the getter.</jc>
	 * 	<jk>public class</jk> BeanWithDynaGetterOnly {
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 *<p>
	 * Similar rules apply for value types and swaps.
	 * The property values optionally can be any serializable type or use swaps.
	 * <p class='bcode'>
	 * 	<jc>// A serializable type other than Object.</jc>
	 * 	<jk>public class</jk> BeanWithDynaFieldWithListValues {
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,List&lt;String&gt;&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// A swapped value.</jc>
	 * 	<jk>public class</jk> BeanWithDynaFieldWithSwappedValues {
	 *
	 * 		<ja>@BeanProperty</ja>(name=<js>"*"</js>, swap=CalendarSwap.<jsf>ISO8601DTZ</jsf>.<jk>class</jk>)
	 * 		<jk>public</jk> Map&lt;String,Calendar&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		Note that if you're not interested in these additional properties, you can also use the
	 * 		{@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting to ignore values that don't fit into existing
	 * 		properties.
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * The following annotations are equivalent:
	 *
	 * <p class='bcode'>
	 * 	<ja>@BeanProperty</ja>(name=<js>"foo"</js>)
	 *
	 * 	<ja>@BeanProperty</ja>(<js>"foo"</js>)
	 * </p>
	 */
	String value() default "";

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
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type.</jc>
	 * 		<ja>@BeanProperty</ja>(type=HashMap.<jk>class</jk>)
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
	 */
	Class<?> type() default Object.class;

	/**
	 * For bean properties of maps and collections, this annotation can be used to identify the class types of the
	 * contents of the bean property object when the generic parameter types are interfaces or abstract classes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type with String keys and Integer values.</jc>
	 * 		<ja>@BeanProperty</ja>(type=HashMap.<jk>class</jk>, params={String.<jk>class</jk>,Integer.<jk>class</jk>})
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
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
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyClass {
	 *
	 * 		<jc>// Only render 'f1' when serializing this bean property.</jc>
	 * 		<ja>@BeanProperty</ja>(properties=<js>"f1"</js>)
	 * 		<jk>public</jk> MyChildClass x1 = <jk>new</jk> MyChildClass();
	 * 	}
	 *
	 * 	<jk>public class</jk> MyChildClass {
	 * 		<jk>public int</jk> f1 = 1;
	 * 		<jk>public int</jk> f2 = 2;
	 * 	}
	 *
	 * 	<jc>// Renders "{x1:{f1:1}}"</jc>
	 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> MyClass());
	 * </p>
	 */
	String properties() default "";

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary this bean property.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 */
	Class<?>[] beanDictionary() default {};

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
	 * <p class='bcode'>
	 * 	<ja>@BeanProperty</ja>(format=<js>"$%.2f"</js>)
	 * 	<jk>public float</jk> <jf>price</jf>;
	 * </p>
	 */
	String format() default "";
}
