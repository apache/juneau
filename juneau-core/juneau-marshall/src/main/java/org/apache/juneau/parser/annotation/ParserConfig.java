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
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(ParserConfigApply.class)
public @interface ParserConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// InputStreamParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"SPACED_HEX"</js>
	 * 			<li><js>"HEX"</js> (default)
	 * 			<li><js>"BASE64"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"InputStreamParser.binaryFormat.s"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
	 * </ul>
	 */
	String binaryFormat() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// Parser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Auto-close streams.
	 *
	 * <p>
	 * If <js>"true"</js>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"Parser.autoCloseStreams.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 */
	String autoCloseStreams() default "";

	/**
	 * Configuration property:  Debug output lines.
	 *
	 * <p>
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: integer
	 * 	<li>
	 * 		Default: 5
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"Parser.debugOutputLines.i"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
	 * </ul>
	 */
	String debugOutputLines() default "";

	/**
	 * Configuration property:  Parser listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 */
	Class<? extends ParserListener> listener() default ParserListener.Null.class;

	/**
	 * Configuration property:  Strict mode.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"Parser.strict.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 */
	String strict() default "";

	/**
	 * Configuration property:  Trim parsed strings.
	 *
	 * <p>
	 * If <js>"true"</js>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"Parser.trimStrings.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 */
	String trimStrings() default "";

	/**
	 * Configuration property:  Unbuffered.
	 *
	 * <p>
	 * If <js>"true"</js>, don't use internal buffering during parsing.
	 *
	 * <p>
	 * This is useful in cases when you want to parse the same input stream or reader multiple times
	 * because it may contain multiple independent POJOs to parse.
	 * <br>Buffering would cause the parser to read past the current POJO in the stream.
	 *
	 * <ul class='notes'>
	 * 	<li>
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
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"Parser.unbuffered.b"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 */
	String unbuffered() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// ReaderParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for reading <c>Files</c> from the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"ReaderParser.fileCharset.s"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_fileCharset}
	 * </ul>
	 */
	String fileCharset() default "";

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 *
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"ReaderParser.inputStreamCharset.s"</js>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_streamCharset}
	 * </ul>
	 */
	String streamCharset() default "";
}
