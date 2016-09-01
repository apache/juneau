/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;

/**
 * Used to tailor how POJOs get interpreted by the framework.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Pojo {

	/**
	 * An identifying name for this class.
	 * <p>
	 * The name is used to identify the class type during parsing when it cannot be inferred through reflection.
	 * For example, if a bean property is of type <code>Object</code>, then the serializer will add the name to the
	 * 	output so that the class can be determined during parsing.
	 * It is also used to specify element names in XML.
	 * <p>
	 * The name is used in combination with the lexicon defined through {@link #classLexicon()}.  Together, they make up
	 * 	a simple name/value mapping of names to classes.
	 * Names do not need to be universally unique.  However, they must be unique within a lexicon.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<ja>@Bean</ja>(name=<js>"foo"</js>)
	 * 	<jk>public class</jk> Foo {
	 * 		<jc>// A bean property where the object types cannot be inferred since it's an Object[].</jc>
	 * 	   <ja>@BeanProperty</ja>(lexicon={Bar.<jk>class</jk>,Baz.<jk>class</jk>})
	 * 	   <jk>public</jk> Object[] x = <jk>new</jk> Object[]{<jk>new</jk> Bar(), <jk>new</jk> Baz()};
	 * 	}
	 *
	 * 	<ja>@Pojo</ja>(name=<js>"bar"</js>)
	 * 	<jk>public class</jk> Bar <jk>extends</jk> HashMap {}
	 *
	 * 	<ja>@Pojo</ja>(name=<js>"baz"</js>)
	 * 	<jk>public class</jk> Baz <jk>extends</jk> HashMap {}
	 * 		</p>
	 * 		<p>
	 * 			When serialized as XML, the bean is rendered as:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<xt>&lt;foo&gt;</xt>
	 * 	   <xt>&lt;x&gt;</xt>
	 * 	      <xt>&lt;bar/&gt;v
	 * 	      <xt>&lt;baz/&gt;</xt>
	 * 	   <xt>&lt;/x&gt;</xt>
	 * 	<xt>&lt;/foo&gt;</xt>
	 * 		</p>
	 * 		<p>
	 * 			When serialized as JSON, <js>'n'</js> attributes would be added when needed to infer the type during parsing:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	{
	 * 	   <jsa>x</jsa>: [
	 * 	      {<jsa>n</jsa>:<jss>'bar'</jss>},
	 * 	      {<jsa>n</jsa>:<jss>'baz'</jss>}
	 * 	   ]
	 * 	}	 *
	 * 	</dd>
	 * </dl>
	 */
	String name() default "";

	/**
	 * The list of classes that make up the class lexicon for this class.
	 * <p>
	 * The lexicon is a name/class mapping used to find class types during parsing when they cannot be inferred through reflection.
	 * The names are defined through the {@link #name()} annotation defined on the bean or POJO classes.
	 * <p>
	 * This list can consist of the following class types:
	 * <ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#name() @Bean.name()};
	 * 	<li>Any POJO class that specifies a value for {@link Pojo#name() @Pojo.name()};
	 * 	<li>Any subclass of {@link ClassLexicon} that defines an entire set of mappings.
	 * 		Note that the subclass MUST implement a no-arg constructor so that it can be instantiated.
	 * </ul>
	 */
	Class<?>[] classLexicon() default {};

	/**
	 * Associate a {@link PojoSwap} or {@link SurrogateSwap} with this class type.
	 *
	 * <p>
	 * Supports the following class types:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Any other class.  Will get interpreted as a {@link SurrogateSwap}.
	 * </ul>
	 *
	 * <h6 class='topic'>Example</h6>
	 * <p>
	 * 	In this case, a swap is being applied to a bean that will force it to be serialized as a <code>String</code>
	 * <p class='bcode'>
	 * 	<jc>// Our bean class</jc>
	 * 	<ja>@Pojo</ja>(swap=BSwap.<jk>class</jk>)
	 * 	<jk>public class</jk> B {
	 * 		<jk>public</jk> String <jf>f1</jf>;
	 * 	}
	 *
	 * 	<jc>// Our POJO swap to force the bean to be serialized as a String</jc>
	 * 	<jk>public class</jk> BSwap <jk>extends</jk> PojoSwap&lt;B,String&gt; {
	 * 		<jk>public</jk> String swap(B o) <jk>throws</jk> SerializeException {
	 * 			<jk>return</jk> o.f1;
	 * 		}
	 * 		<jk>public</jk> B unswap(String f) <jk>throws</jk> ParseException {
	 * 			B b1 = <jk>new</jk> B();
	 * 			b1.<jf>f1</jf> = f;
	 * 			<jk>return</jk> b1;
	 * 		}
	 * 	}
	 *
	 * 	<jk>public void</jk> test() <jk>throws</jk> Exception {
	 * 		WriterSerializer s = JsonSerializer.<jsf>DEFAULT</jsf>;
	 * 		B b = <jk>new</jk> B();
	 * 		b.<jf>f1</jf> = <js>"bar"</js>;
	 * 		String json = s.serialize(b);
	 * 		<jsm>assertEquals</jsm>(<js>"'bar'"</js>, json);
	 *
	 * 		ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 		b = p.parse(json, B.<jk>class</jk>);
	 * 		<jsm>assertEquals</jsm>(<js>"bar"</js>, t.<jf>f1</jf>);
	 * 	}
	 * </p>
	 * <p>
	 * 	Note that using this annotation is functionally equivalent to adding swaps to the serializers and parsers:
	 * <p class='bcode'>
	 * 	WriterSerializer s = <jk>new</jk> JsonSerializer.addPojoSwaps(BSwap.<jk>class</jk>);
	 * 	ReaderParser p = <jk>new</jk> JsonParser.addPojoSwaps(BSwap.<jk>class</jk>);
	 * </p>
	 */
	Class<?> swap() default Null.class;
}