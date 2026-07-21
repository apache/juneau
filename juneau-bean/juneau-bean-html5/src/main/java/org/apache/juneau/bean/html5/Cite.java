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
package org.apache.juneau.bean.html5;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-cite-element">&lt;cite&gt;</a>
 * element.
 *
 * <p>
 * The cite element represents the title of a work (e.g., a book, a paper, an essay, a poem, a score,
 * a song, a script, a film, a TV show, a game, a sculpture, a painting, a theatre production, a play,
 * an opera, a musical, an exhibition, a legal case report, a computer program, a web site, a web page,
 * a blog post or comment, a forum post or comment, a tweet, a written or oral statement, etc.). It is
 * used to mark up the title of a referenced work, making it clear to both users and search engines
 * what is being cited.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple citation</jc>
 * 	Cite <jv>simple</jv> = <jsm>cite</jsm>(<js>"The Great Gatsby"</js>);
 *
 * 	<jc>// Citation with styling</jc>
 * 	Cite <jv>styled</jv> = <jsm>cite</jsm>(<js>"To Kill a Mockingbird"</js>)
 * 		.class_(<js>"book-title"</js>);
 *
 * 	<jc>// Citation in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"As mentioned in "</js>,
 * 		<jsm>cite</jsm>(<js>"The Art of War"</js>),
 * 		<js>", strategy is key to success."</js>
 * 	);
 *
 * 	<jc>// Citation with link</jc>
 * 	Cite <jv>withLink</jv> = <jsm>cite</jsm>(
 * 		<jsm>a</jsm>(<js>"/books/1984"</js>, <js>"1984"</js>)
 * 	);
 *
 * 	<jc>// Multiple citations</jc>
 * 	P <jv>multiple</jv> = <jsm>p</jsm>(
 * 		<js>"Several works discuss this topic: "</js>,
 * 		<jsm>cite</jsm>(<js>"Book A"</js>),
 * 		<js>", "</js>,
 * 		<jsm>cite</jsm>(<js>"Book B"</js>),
 * 		<js>", and "</js>,
 * 		<jsm>cite</jsm>(<js>"Book C"</js>),
 * 		<js>"."</js>
 * 	);
 *
 * 	<jc>// Citation with author</jc>
 * 	P <jv>withAuthor</jv> = <jsm>p</jsm>(
 * 		<js>"According to "</js>,
 * 		<jsm>strong</jsm>(<js>"John Doe"</js>),
 * 		<js>" in "</js>,
 * 		<jsm>cite</jsm>(<js>"The Future of Technology"</js>),
 * 		<js>", we are entering a new era."</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#cite() cite()}
 * 		<li class='jm'>{@link HtmlBuilder#cite(Object...) cite(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "cite")
public class Cite extends HtmlElementMixed<Cite> {

	/**
	 * Creates an empty {@link Cite} element.
	 */
	public Cite() {}

	/**
	 * Creates a {@link Cite} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Cite(Object...children) {
		children(children);
	}

}