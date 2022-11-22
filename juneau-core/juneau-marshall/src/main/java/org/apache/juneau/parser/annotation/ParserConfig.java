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
package org.apache.juneau.parser.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;

/**
 * Annotation for specifying config properties defined in {@link Parser}, {@link InputStreamParser}, and {@link ReaderParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({ParserConfigAnnotation.ParserApply.class,ParserConfigAnnotation.InputStreamParserApply.class,ParserConfigAnnotation.ReaderParserApply.class})
public @interface ParserConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// InputStreamParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <ul class='values'>
	 * 	<li><js>"SPACED_HEX"</js>
	 * 	<li><js>"HEX"</js> (default)
	 * 	<li><js>"BASE64"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.InputStreamParser.Builder#binaryFormat(BinaryFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String binaryFormat() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// Parser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Auto-close streams.
	 *
	 * <p>
	 * If <js>"true"</js>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#autoCloseStreams()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String autoCloseStreams() default "";

	/**
	 * Debug output lines.
	 *
	 * <p>
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 * 	<li class='note'>
	 * 		Default: 5
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#debugOutputLines(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String debugOutputLines() default "";

	/**
	 * Parser listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#listener(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends ParserListener> listener() default ParserListener.Void.class;

	/**
	 * Strict mode.
	 *
	 * <p>
	 * If <js>"true"</js>, strict mode for the parser is enabled.
	 *
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 *
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#strict()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String strict() default "";

	/**
	 * Trim parsed strings.
	 *
	 * <p>
	 * If <js>"true"</js>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#trimStrings()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String trimStrings() default "";

	/**
	 * Unbuffered.
	 *
	 * <p>
	 * If <js>"true"</js>, don't use internal buffering during parsing.
	 *
	 * <p>
	 * This is useful in cases when you want to parse the same input stream or reader multiple times
	 * because it may contain multiple independent POJOs to parse.
	 * <br>Buffering would cause the parser to read past the current POJO in the stream.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This only allows for multi-input streams for the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link JsonParser}
	 * 			<li class='jc'>{@link UonParser}
	 * 		</ul>
	 * 		It has no effect on the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link MsgPackParser} - It already doesn't use buffering.
	 * 			<li class='jc'>{@link XmlParser}, {@link HtmlParser} - These use StAX which doesn't allow for more than one root element anyway.
	 * 			<li>RDF parsers - These read everything into an internal model before any parsing begins.
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#unbuffered()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String unbuffered() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// ReaderParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * <p>
	 * The character set to use for reading <c>Files</c> from the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.ReaderParser.Builder#fileCharset(Charset)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String fileCharset() default "";

	/**
	 * Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 *
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String streamCharset() default "";
}
