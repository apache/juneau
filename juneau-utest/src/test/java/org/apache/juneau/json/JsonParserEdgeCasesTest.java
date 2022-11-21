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
package org.apache.juneau.json;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.parser.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class JsonParserEdgeCasesTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {
			{ 0, "is_structure_500_nested_arrays", repeat(500, "[") + repeat(500, "]"), null },
			{ 1, "ix_object_key_lone_2nd_surrogate", "7B 22 5C 75 44 46 41 41 22 3A 30 7D"/*{"buDFAA":0}*/, null },
			{ 2, "ix_string_1st_surrogate_but_2nd_missing", "5B 22 5C 75 44 41 44 41 22 5D"/*["buDADA"]*/, null },
			{ 3, "ix_string_1st_valid_surrogate_2nd_invalid", "5B 22 5C 75 44 38 38 38 5C 75 31 32 33 34 22 5D"/*["buD888bu1234"]*/, null },
			{ 4, "ix_string_incomplete_surrogate_and_escape_valid", "5B 22 5C 75 44 38 30 30 5C 6E 22 5D"/*["buD800\n"]*/, null },
			{ 5, "ix_string_incomplete_surrogate_pair", "5B 22 5C 75 44 64 31 65 61 22 5D"/*["buDd1ea"]*/, null },
			{ 6, "ix_string_incomplete_surrogates_escape_valid", "5B 22 5C 75 44 38 30 30 5C 75 44 38 30 30 5C 6E 22 5D"/*["buD800buD800\n"]*/, null },
			{ 7, "ix_string_invalid_lonely_surrogate", "5B 22 5C 75 64 38 30 30 22 5D"/*["bud800"]*/, null },
			{ 8, "ix_string_invalid_surrogate", "5B 22 5C 75 64 38 30 30 61 62 63 22 5D"/*["bud800abc"]*/, null },
			{ 9, "ix_string_inverted_surrogates_U+1D11E", "5B 22 5C 75 44 64 31 65 5C 75 44 38 33 34 22 5D"/*["buDd1ebuD834"]*/, null },
			{ 10, "ix_string_lone_second_surrogate", "5B 22 5C 75 44 46 41 41 22 5D"/*["buDFAA"]*/, null },
			{ 11, "ix_string_not_in_unicode_range", "5B 22 F4 BF BF BF 22 5D"/*["[fffd][fffd][fffd][fffd]"]*/, null},
			{ 12, "ix_string_truncated-utf-8", "5B 22 E0 FF 22 5D"/*["[fffd][fffd]"]*/, null },
			{ 13, "ix_string_unicode_U+10FFFE_nonchar", "5B 22 5C 75 44 42 46 46 5C 75 44 46 46 45 22 5D"/*["buDBFFbuDFFE"]*/, null },
			{ 14, "ix_string_unicode_U+1FFFE_nonchar", "5B 22 5C 75 44 38 33 46 5C 75 44 46 46 45 22 5D"/*["buD83FbuDFFE"]*/, null },
			{ 15, "ix_string_unicode_U+FDD0_nonchar", "5B 22 5C 75 46 44 44 30 22 5D"/*["buFDD0"]*/, null },
			{ 16, "ix_string_unicode_U+FFFE_nonchar", "5B 22 5C 75 46 46 46 45 22 5D"/*["buFFFE"]*/, null },
			{ 17, "ix_string_UTF-16LE_with_BOM", "FF FE 5B 00 22 00 E9 00 22 00 5D 00"/*[fffd][fffd][[0]"[0][fffd][0]"[0]][0]*/, null },
			{ 18, "ix_string_UTF-8_invalid_sequence", "5B 22 E6 97 A5 D1 88 FA 22 5D"/*["[65e5][448][fffd]"]*/, null },
			{ 19, "ix_structure_UTF-8_BOM_empty_object", "EF BB BF 7B 7D"/*[feff]{}*/, "Unrecognized syntax" },
			{ 20, "n_array_1_true_without_comma", "[1 true]", "Expected ',' or ']'" },
			{ 21, "n_array_colon_instead_of_comma", "[\"\": 1]", "Expected ',' or ']'" },
			{ 22, "n_array_comma_after_close", "[\"\"],", "Remainder after parse: ','" },
			{ 23, "n_array_comma_and_number", "[,1]", "Missing value detected" },
			{ 24, "n_array_double_comma", "[1,,2]", "Missing value detected" },
			{ 25, "n_array_double_extra_comma", "[\"x\",,]", null },
			{ 26, "n_array_extra_close", "[\"x\"]]", "Remainder after parse: ']'" },
			{ 27, "n_array_extra_comma", "[\"\",]", "Unexpected trailing comma in array" },
			{ 28, "n_array_incomplete", "[\"x\"", "Expected ',' or ']'" },
			{ 29, "n_array_incomplete_invalid_value", "[x", "Unrecognized syntax" },
			{ 30, "n_array_inner_array_no_comma", "[3[4]]", "Expected ',' or ']'" },
			{ 31, "n_array_items_separated_by_semicolon", "[1:2]", "Expected ',' or ']'" },
			{ 32, "n_array_just_comma", "[,]", null },
			{ 33, "n_array_just_minus", "[-]", "NumberFormatException" },
			{ 34, "n_array_missing_value", "[   , \"\"]", "Missing value detected" },
			{ 35, "n_array_number_and_comma", "[1,]", "Unexpected trailing comma in array" },
			{ 36, "n_array_number_and_several_commas", "[1,,]", null },
			{ 37, "n_array_star_inside", "[*]", "Unrecognized syntax" },
			{ 38, "n_array_unclosed", "[\"\"", "Expected ',' or ']'" },
			{ 39, "n_array_unclosed_trailing_comma", "[1,", "Unexpected trailing comma in array" },
			{ 40, "n_array_unclosed_with_object_inside", "[{}", "Expected ',' or ']'" },
			{ 41, "n_incomplete_false", "[fals]", "Unrecognized syntax" },
			{ 42, "n_incomplete_null", "[nul]", "Unrecognized syntax" },
			{ 43, "n_incomplete_true", "[tru]", "Unrecognized syntax" },
			{ 44, "n_number_++", "[++1234]", "Unrecognized syntax" },
			{ 45, "n_number_+1", "[+1]", "Unrecognized syntax" },
			{ 46, "n_number_+Inf", "[+Inf]", "Unrecognized syntax" },
			{ 47, "n_number_-01", "[-01]", "Invalid JSON number" },
			{ 48, "n_number_-1.0.", "[-1.0.]", "NumberFormatException" },
			{ 49, "n_number_-2.", "[-2.]", "Invalid JSON number" },
			{ 50, "n_number_-NaN", "[-NaN]", null },
			{ 51, "n_number_.-1", "[.-1]", null },
			{ 52, "n_number_.2e-3", "[.2e-3]", "Invalid JSON number" },
			{ 53, "n_number_0.1.2", "[0.1.2]", "NumberFormatException" },
			{ 54, "n_number_0.3e+", "[0.3e+]", "NumberFormatException" },
			{ 55, "n_number_0.3e", "[0.3e]", "NumberFormatException" },
			{ 56, "n_number_0.e1", "[0.e1]", "Invalid JSON number" },
			{ 57, "n_number_0_capital_E+", "[0E+]", "NumberFormatException" },
			{ 58, "n_number_0_capital_E", "[0E]", "NumberFormatException" },
			{ 59, "n_number_0e+", "[0e+]", "NumberFormatException" },
			{ 60, "n_number_0e", "[0e]", "NumberFormatException" },
			{ 61, "n_number_1.0e+", "[1.0e+]", "NumberFormatException" },
			{ 62, "n_number_1.0e-", "[1.0e-]", "NumberFormatException" },
			{ 63, "n_number_1.0e", "[1.0e]", "NumberFormatException" },
			{ 64, "n_number_1_000", "[1 000.0]", "Expected ',' or ']'" },
			{ 65, "n_number_1eE2", "[1eE2]", "NumberFormatException" },
			{ 66, "n_number_2.e+3", "[2.e+3]", "Invalid JSON number" },
			{ 67, "n_number_2.e-3", "[2.e-3]", "Invalid JSON number" },
			{ 68, "n_number_2.e3", "[2.e3]", "Invalid JSON number" },
			{ 69, "n_number_9.e+", "[9.e+]", null },
			{ 70, "n_number_expression", "[1+2]", "NumberFormatException" },
			{ 71, "n_number_hex_1_digit", "[0x1]", "Invalid JSON number" },
			{ 72, "n_number_hex_2_digits", "[0x42]", "Invalid JSON number" },
			{ 73, "n_number_Inf", "[Inf]", "Unrecognized syntax" },
			{ 74, "n_number_infinity", "[Infinity]", "Unrecognized syntax" },
			{ 75, "n_number_invalid+-", "[0e+-1]", "NumberFormatException" },
			{ 76, "n_number_invalid-negative-real", "[-123.123foo]", "Expected ',' or ']'" },
			{ 77, "n_number_minus_infinity", "[-Infinity]", null },
			{ 78, "n_number_minus_sign_with_trailing_garbage", "[-foo]", "NumberFormatException" },
			{ 79, "n_number_minus_space_1", "[- 1]", null },
			{ 80, "n_number_NaN", "[NaN]", "Unrecognized syntax" },
			{ 81, "n_number_neg_int_starting_with_zero", "[-012]", "Invalid JSON number" },
			{ 82, "n_number_neg_real_without_int_part", "[-.123]", "Invalid JSON number" },
			{ 83, "n_number_neg_with_garbage_at_end", "[-1x]", "NumberFormatException" },
			{ 84, "n_number_real_garbage_after_e", "[1ea]", "NumberFormatException" },
			{ 85, "n_number_real_without_fractional_part", "[1.]", "Invalid" },
			{ 86, "n_number_starting_with_dot", "[.123]", "Invalid JSON number" },
			{ 87, "n_number_U+FF11_fullwidth_digit_one", "[１]", "Unrecognized syntax" },
			{ 88, "n_number_with_alpha", "[1.2a-3]", "NumberFormatException" },
			{ 89, "n_number_with_alpha_char", "[1.8011670033376514H-308]", "Expected ',' or ']'" },
			{ 90, "n_number_with_leading_zero", "[012]", "Invalid JSON number" },
			{ 91, "n_object_bad_value", "[\"x\", truth]", "Unrecognized syntax" },
			{ 92, "n_object_comma_instead_of_colon", "{\"x\", null}", "Could not find ':'" },
			{ 93, "n_object_double_colon", "{\"x\"::\"b\"}", "Unrecognized syntax" },
			{ 94, "n_object_garbage_at_end", "{\"a\":\"a\" 123}", "Could not find '}'" },
			{ 95, "n_object_key_with_single_quotes", "{key: 'value'}", "Unquoted attribute detected" },
			{ 96, "n_object_missing_colon", "{\"a\" b}", "Could not find ':'" },
			{ 97, "n_object_missing_key", "{:\"b\"}", null },
			{ 98, "n_object_missing_semicolon", "{\"a\" \"b\"}", "Could not find ':'" },
			{ 99, "n_object_missing_value", "{\"a\":", "Unrecognized syntax" },
			{ 100, "n_object_no-colon", "{\"a\"", "Could not find ':'" },
			{ 101, "n_object_non_string_key", "{1:1}", "Unquoted attribute detected" },
			{ 102, "n_object_non_string_key_but_huge_number_instead", "{9999E9999:1}", "Unquoted attribute detected" },
			{ 103, "n_object_repeated_null_null", "{null:null,null:null}", "Unquoted attribute detected" },
			{ 104, "n_object_several_trailing_commas", "{\"id\":0,,,,,}", null },
			{ 105, "n_object_single_quote", "{'a':0}", "Invalid quote character" },
			{ 106, "n_object_trailing_comma", "{\"id\":0,}", "Unexpected '}' found" },
			{ 107, "n_object_trailing_comment", "{\"a\":\"b\"}/**/", "Javascript comment detected" },
			{ 108, "n_object_trailing_comment_open", "{\"a\":\"b\"}/**//", null },
			{ 109, "n_object_trailing_comment_slash_open", "{\"a\":\"b\"}//", "Javascript comment detected" },
			{ 110, "n_object_trailing_comment_slash_open_incomplete", "{\"a\":\"b\"}/", null },
			{ 111, "n_object_two_commas_in_a_row", "{\"a\":\"b\",,\"c\":\"d\"}", null },
			{ 112, "n_object_unquoted_key", "{a: \"b\"}", "Unquoted attribute detected" },
			{ 113, "n_object_unterminated-value", "{\"a\":\"a", null },
			{ 114, "n_object_with_single_string", "{ \"foo\" : \"bar\", \"a\" }", "Could not find ':'" },
			{ 115, "n_object_with_trailing_garbage", "{\"a\":\"b\"}#", "Remainder after parse" },
			{ 116, "n_single_space", "", "Empty input" },
			{ 117, "n_string_single_doublequote", "\"", null },
			{ 118, "n_string_single_quote", "['single quote']", "Invalid quote character" },
			{ 119, "n_string_single_string_no_double_quotes", "abc", "Unrecognized syntax" },
			{ 120, "n_string_with_trailing_garbage", "\"\"x", "Remainder after parse" },
			{ 121, "n_structure_<.>", "<.>", "Unrecognized syntax" },
			{ 122, "n_structure_<null>", "[<null>]", "Unrecognized syntax" },
			{ 123, "n_structure_array_trailing_garbage", "[1]x", "Remainder after parse" },
			{ 124, "n_structure_array_with_extra_array_close", "[1]]", "Remainder after parse" },
			{ 125, "n_structure_array_with_unclosed_string", "[\"asd]", null },
			{ 126, "n_structure_capitalized_True", "[True]", "Unrecognized syntax" },
			{ 127, "n_structure_close_unopened_array", "1]", "Remainder after parse" },
			{ 128, "n_structure_comma_instead_of_closing_brace", "{\"x\": true,", null },
			{ 129, "n_structure_double_array", "[][]", "Remainder after parse" },
			{ 130, "n_structure_end_array", "]", null },
			{ 131, "n_structure_lone-open-bracket", "[", "Expected one of the following characters: {,[," },
			{ 132, "n_structure_no_data", "", "Empty input" },
			{ 133, "n_structure_null-byte-outside-string", "[ ]", "Unrecognized syntax" },
			{ 134, "n_structure_number_with_trailing_garbage", "2@", "Remainder after parse" },
			{ 135, "n_structure_object_followed_by_closing_object", "{}}", "Remainder after parse" },
			{ 136, "n_structure_object_unclosed_no_value", "{\"\":", "Unrecognized syntax" },
			{ 137, "n_structure_object_with_comment", "{\"a\":/*comment*/\"b\"}", "Javascript comment detected" },
			{ 138, "n_structure_object_with_trailing_garbage", "{\"a\": true} \"x\"", "Remainder after parse" },
			{ 139, "n_structure_open_array_apostrophe", "['", null },
			{ 140, "n_structure_open_array_comma", "[,", null },
			{ 141, "n_structure_open_array_open_object", "[{", null },
			{ 142, "n_structure_open_array_open_string", "[\"a", null },
			{ 143, "n_structure_open_array_string", "[\"a\"", "Expected ',' or ']'" },
			{ 144, "n_structure_open_object", "{", null },
			{ 145, "n_structure_open_object_close_array", "{]", null },
			{ 146, "n_structure_open_object_comma", "{,", null },
			{ 147, "n_structure_open_object_open_array", "{[", null },
			{ 148, "n_structure_open_object_open_string", "{\"a", null },
			{ 149, "n_structure_open_object_string_with_apostrophes", "{'a'", null },
			{ 150, "n_structure_single_star", "*", "Unrecognized syntax" },
			{ 151, "n_structure_trailing_#", "{\"a\":\"b\"}#{}", "Remainder after parse" },
			{ 152, "n_structure_unclosed_array", "[1", "Expected ',' or ']" },
			{ 153, "n_structure_unclosed_array_partial_null", "[ false, nul", "Unrecognized syntax" },
			{ 154, "n_structure_unclosed_array_unfinished_false", "[ true, fals", "Unrecognized syntax" },
			{ 155, "n_structure_unclosed_array_unfinished_true", "[ false, tru", "Unrecognized syntax" },
			{ 156, "n_structure_unclosed_object", "{\"asd\":\"asd\"", "Could not find '}'" },
			{ 157, "ns_structure_100000_opening_arrays", repeat(100000, "["), "Depth too deep" },
			{ 158, "ns_structure_open_array_object", repeat(50000, "[{\"\":"), "Depth too deep" },
			{ 159, "nx_array_a_invalid_utf8", "5B 61 E5 5D"/*[a[fffd]]*/, null },
			{ 160, "nx_array_invalid_utf8", "5B FF 5D"/*[[fffd]]*/, null },
			{ 161, "nx_array_newlines_unclosed", "5B 22 61 22 2C 0A 34 0A 2C 31 2C"/*["a",[a]4[a],1,*/, null },
			{ 162, "nx_array_spaces_vertical_tab_formfeed", "5B 22 0B 61 22 5C 66 5D"/*["[b]a"\f]*/, null },
			{ 163, "nx_array_unclosed_with_new_lines", "5B 31 2C 0A 31 0A 2C 31"/*[1,[a]1[a],1*/, null },
			{ 164, "nx_multidigit_number_then_00", "31 32 33 00"/*123[0]*/, null },
			{ 165, "nx_number_invalid-utf-8-in-bigger-int", "5B 31 32 33 E5 5D"/*[123[fffd]]*/, null },
			{ 166, "nx_number_invalid-utf-8-in-exponent", "5B 31 65 31 E5 5D"/*[1e1[fffd]]*/, null },
			{ 167, "nx_number_invalid-utf-8-in-int", "5B 30 E5 5D 0A"/*[0[fffd]][a]*/, null },
			{ 168, "nx_number_real_with_invalid_utf8_after_e", "5B 31 65 E5 5D"/*[1e[fffd]]*/, null },
			{ 169, "nx_object_bracket_key", "7B 5B 3A 20 22 78 22 7D 0A"/*{[: "x"}[a]*/, null },
			{ 170, "nx_object_emoji", "7B F0 9F 87 A8 F0 9F 87 AD 7D"/*{[d83c][dde8][d83c][dded]}*/, null },
			{ 171, "nx_object_pi_in_key_and_trailing_comma", "7B 22 B9 22 3A 22 30 22 2C 7D"/*{"[fffd]":"0",}*/, null },
			{ 172, "nx_string_1_surrogate_then_escape u", "5B 22 5C 75 44 38 30 30 5C 75 22 5D"/*["buD800bu"]*/, "Invalid Unicode escape sequence in string" },
			{ 173, "nx_string_1_surrogate_then_escape u1", "5B 22 5C 75 44 38 30 30 5C 75 31 22 5D"/*["buD800bu1"]*/, "Invalid Unicode escape sequence in string" },
			{ 174, "nx_string_1_surrogate_then_escape u1x", "5B 22 5C 75 44 38 30 30 5C 75 31 78 22 5D"/*["buD800bu1x"]*/, "Invalid Unicode escape sequence in string" },
			{ 175, "nx_string_1_surrogate_then_escape", "5B 22 5C 75 44 38 30 30 5C 22 5D"/*["buD800\"]*/, null },
			{ 176, "nx_string_accentuated_char_no_quotes", "5B C3 A9 5D"/*[[e9]]*/, "Unrecognized syntax" },
			{ 177, "nx_string_backslash_00", "5B 22 5C 00 22 5D"/*["\[0]"]*/, null },
			{ 178, "nx_string_escape_x", "5B 22 5C 78 30 30 22 5D"/*["\x00"]*/, "Invalid escape sequence in string" },
			{ 179, "nx_string_escaped_backslash_bad", "5B 22 5C 5C 5C 22 5D"/*["\\\"]*/, null },
			{ 180, "nx_string_escaped_ctrl_char_tab", "5B 22 5C 09 22 5D"/*["\[9]"]*/, null },
			{ 181, "nx_string_escaped_emoji", "5B 22 5C F0 9F 8C 80 22 5D"/*["\[d83c][df00]"]*/, "Invalid escape sequence in string" },
			{ 182, "nx_string_incomplete_escape", "5B 22 5C 22 5D"/*["\"]*/, null },
			{ 183, "nx_string_incomplete_escaped_character", "5B 22 5C 75 30 30 41 22 5D"/*["bu00A"]*/, "Invalid Unicode escape sequence in string" },
			{ 184, "nx_string_incomplete_surrogate", "5B 22 5C 75 44 38 33 34 5C 75 44 64 22 5D"/*["buD834buDd"]*/, "Invalid Unicode escape sequence in string" },
			{ 185, "nx_string_incomplete_surrogate_escape_invalid", "5B 22 5C 75 44 38 30 30 5C 75 44 38 30 30 5C 78 22 5D"/*["buD800buD800\x"]*/, "Invalid escape sequence" },
			{ 186, "nx_string_invalid-utf-8-in-escape", "5B 22 5C 75 E5 22 5D"/*["bu[fffd]"]*/, null },
			{ 187, "nx_string_invalid_backslash_esc", "5B 22 5C 61 22 5D"/*["\a"]*/, "Invalid escape sequence" },
			{ 188, "nx_string_invalid_unicode_escape", "5B 22 5C 75 71 71 71 71 22 5D"/*["buqqqq"]*/, "Invalid Unicode escape sequence in string" },
			{ 189, "nx_string_invalid_utf-8", "5B 22 FF 22 5D"/*["[fffd]"]*/, null },
			{ 190, "nx_string_invalid_utf8_after_escape", "5B 22 5C E5225D"/*["\[fffd]"]*/, null },
			{ 191, "nx_string_iso_latin_1", "5B 22 E9 22 5D"/*["[fffd]"]*/, null },
			{ 192, "nx_string_leading_uescaped_thinspace", "5B 5C 75 30 30 32 30 22 61 73 64 22 5D"/*[bu0020"asd"]*/, "Unrecognized syntax" },
			{ 193, "nx_string_lone_utf8_continuation_byte", "5B 22 81 22 5D"/*["[fffd]"]*/, null },
			{ 194, "nx_string_no_quotes_with_bad_escape", "5B 5C 6E 5D"/*[\n]*/, "Unrecognized syntax" },
			{ 195, "nx_string_overlong_sequence_2_bytes", "5B 22 C0 AF 22 5D"/*["[fffd][fffd]"]*/, null },
			{ 196, "nx_string_overlong_sequence_6_bytes", "5B 22 FC 83 BF BF BF BF 22 5D"/*["[fffd][fffd][fffd][fffd][fffd][fffd]"]*/, null },
			{ 197, "nx_string_overlong_sequence_6_bytes_null", "5B 22 FC 80 80 80 80 80 22 5D"/*["[fffd][fffd][fffd][fffd][fffd][fffd]"]*/, null },
			{ 198, "nx_string_start_escape_unclosed", "5B 22 5C"/*["\*/, null },
			{ 199, "nx_string_unescaped_crtl_char", "5B 22 61 00 61 22 5D"/*["a[0]a"]*/, null },
			{ 200, "nx_string_unescaped_newline", "5B 22 6E 65 77 0A 6C 69 6E 65 22 5D"/*["new[a]line"]*/, null },
			{ 201, "nx_string_unescaped_tab", "5B 22 09 22 5D"/*["[9]"]*/, null },
			{ 202, "nx_string_unicode_CapitalU", "22 5C 55 41 36 36 44 22"/*"\UA66D"*/, "Invalid escape sequence" },
			{ 203, "ix_string_UTF8_surrogate_U+D800", "5B 22 ED A0 80 22 5D"/*["[fffd]"]*/, null },  // Succeeds on Java 8, fails on Java 6 & 7.
			{ 204, "nx_structure_ascii-unicode-identifier", "61 C3 A5"/*a[e5]*/, "Unrecognized syntax" },
			{ 205, "nx_structure_incomplete_UTF8_BOM", "EF BB 7B 7D"/*[fffd]{}*/, null },
			{ 206, "nx_structure_lone-invalid-utf-8", "E5"/*[fffd]*/, null },
			{ 207, "nx_structure_open_open", "5B 22 5C 7B 5B 22 5C 7B 5B 22 5C 7B 5B 22 5C 7B"/*["\{["\{["\{["\{*/, "Invalid escape sequence" },
			{ 208, "nx_structure_single_point", "E9"/*[fffd]*/, null },
			{ 209, "nx_structure_U+2060_word_joined", "5B E2 81 A0 5D"/*[[2060]]*/, "Unrecognized syntax" },
			{ 210, "nx_structure_uescaped_LF_before_string", "5B 5C 75 30 30 30 41 22 22 5D"/*[bu000A""]*/, "Unrecognized syntax" },
			{ 211, "nx_structure_unicode-identifier", "C3 A5"/*[e5]*/, "Unrecognized syntax" },
			{ 212, "nx_structure_UTF8_BOM_no_data", "EF BB BF"/*[feff]*/, "Unrecognized syntax" },
			{ 213, "nx_structure_whitespace_formfeed", "5B 0C 5D"/*[[c]]*/, "Unrecognized syntax" },
			{ 214, "nx_structure_whitespace_U+2060_word_joiner", "5B E2 81 A0 5D"/*[[2060]]*/, "Unrecognized syntax" },
			{ 215, "y_array_arraysWithSpaces", "[[]   ]", null },
			{ 216, "y_array_empty-string", "[\"\"]", null },
			{ 217, "y_array_empty", "[]", null },
			{ 218, "y_array_ending_with_newline", "[\"a\"]", null },
			{ 219, "y_array_false", "[false]", null },
			{ 220, "y_array_heterogeneous", "[null, 1, \"1\", {}]", null },
			{ 221, "y_array_null", "[null]", null },
			{ 222, "y_array_with_leading_space", "[1]", null },
			{ 223, "y_array_with_several_null", "[1,null,null,null,2]", null },
			{ 224, "y_array_with_trailing_space", "[2]", null },
			{ 225, "y_number", "[123e65]", null },
			{ 226, "y_number_0e+1", "[0e+1]", null },
			{ 227, "y_number_0e1", "[0e1]", null },
			{ 228, "y_number_after_space", "[ 4]", null },
			{ 229, "y_number_double_close_to_zero", "[-0.000000000000000000000000000000000000000000000000000000000000000000000000000001]", null },
			{ 230, "y_number_double_huge_neg_exp", "[123.456e-789]", null },
			{ 231, "y_number_huge_exp", "[0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]", null },
			{ 232, "y_number_int_with_exp", "[20e1]", null },
			{ 233, "y_number_minus_zero", "[-0]", null },
			{ 234, "y_number_neg_int_huge_exp", "[-1e+9999]", null },
			{ 235, "y_number_negative_int", "[-123]", null },
			{ 236, "y_number_negative_one", "[-1]", null },
			{ 237, "y_number_negative_zero", "[-0]", null },
			{ 238, "y_number_pos_double_huge_exp", "[1.5e+9999]", null },
			{ 239, "y_number_real_capital_e", "[1E22]", null },
			{ 240, "y_number_real_capital_e_neg_exp", "[1E-2]", null },
			{ 241, "y_number_real_capital_e_pos_exp", "[1E+2]", null },
			{ 242, "y_number_real_exponent", "[123e45]", null },
			{ 243, "y_number_real_fraction_exponent", "[123.456e78]", null },
			{ 244, "y_number_real_neg_exp", "[1e-2]", null },
			{ 245, "y_number_real_neg_overflow", "[-123123e100000]", null },
			{ 246, "y_number_real_pos_exponent", "[1e+2]", null },
			{ 247, "y_number_real_pos_overflow", "[123123e100000]", null },
			{ 248, "y_number_real_underflow", "[123e-10000000]", null },
			{ 249, "y_number_simple_int", "[123]", null },
			{ 250, "y_number_simple_real", "[123.456789]", null },
			{ 251, "y_number_too_big_neg_int", "[-123123123123123123123123123123]", null },
			{ 252, "y_number_too_big_pos_int", "[100000000000000000000]", null },
			{ 253, "y_number_very_big_negative_int", "[-237462374673276894279832749832423479823246327846]", null },
			{ 254, "y_object", "{\"asd\":\"sdf\", \"dfg\":\"fgh\"}", null },
			{ 255, "y_object_basic", "{\"asd\":\"sdf\"}", null },
			{ 256, "y_object_duplicated_key", "{\"a\":\"b\",\"a\":\"c\"}", null },
			{ 257, "y_object_duplicated_key_and_value", "{\"a\":\"b\",\"a\":\"b\"}", null },
			{ 258, "y_object_empty", "{}", null },
			{ 259, "y_object_empty_key", "{\"\":0}", null },
			{ 260, "y_object_extreme_numbers", "{ \"min\": -1.0e+28, \"max\": 1.0e+28 }", null },
			{ 261, "y_object_long_strings", "{\"x\":[{\"id\": \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}], \"id\": \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}", null },
			{ 262, "y_object_simple", "{\"a\":[]}", null },
			{ 263, "y_string_comments", "[\"a/*b*/c/*d//e\"]", null },
			{ 264, "y_string_in_array", "[\"asd\"]", null },
			{ 265, "y_string_in_array_with_leading_space", "[ \"asd\"]", null },
			{ 266, "y_string_simple_ascii", "[\"asd \"]", null },
			{ 267, "y_string_space", "\" \"", null },
			{ 268, "y_structure_lonely_false", "false", null },
			{ 269, "y_structure_lonely_int", "42", null },
			{ 270, "y_structure_lonely_negative_real", "-0.1", null },
			{ 271, "y_structure_lonely_null", "null", null },
			{ 272, "y_structure_lonely_string", "\"asd\"", null },
			{ 273, "y_structure_lonely_true", "true", null },
			{ 274, "y_structure_string_empty", "\"\"", null },
			{ 275, "y_structure_true_in_array", "[true]", null },
			{ 276, "y_structure_whitespace_array", "[]", null },
			{ 277, "yx_array_with_1_and_newline", "5B 31 0A 5D"/*[1[a]]*/, null },
			{ 278, "yx_object_escaped_null_in_key", "7B 22 66 6F 6F 5C 75 30 30 30 30 62 61 72 22 3A 20 34 32 7D"/*{"foobu0000bar": 42}*/, null },
			{ 279, "yx_object_string_unicode", "7B 22 74 69 74 6C 65 22 3A 22 5C 75 30 34 31 66 5C 75 30 34 33 65 5C 75 30 34 33 62 5C 75 30 34 34 32 5C 75 30 34 33 65 5C 75 30 34 34 30 5C 75 30 34 33 30 20 5C 75 30 34 31 37 5C 75 30 34 33 35 5C 75 30 34 33 63 5C 75 30 34 33 62 5C 75 30 34 33 35 5C 75 30 34 33 61 5C 75 30 34 33 65 5C 75 30 34 33 66 5C 75 30 34 33 30 22 20 7D"/*{"title":"bu041fbu043ebu043bbu0442bu043ebu0440bu0430 bu0417bu0435bu043cbu043bbu0435bu043abu043ebu043fbu0430" }*/, null },
			{ 280, "yx_object_with_newlines", "7B 0A 22 61 22 3A 20 22 62 22 0A 7D"/*{[a]"a": "b"[a]}*/, null },
			{ 281, "yx_string_1_2_3_bytes_UTF-8_sequences", "5B 22 5C 75 30 30 36 30 5C 75 30 31 32 61 5C 75 31 32 41 42 22 5D"/*["bu0060bu012abu12AB"]*/, null },
			{ 282, "yx_string_accepted_surrogate_pair", "5B 22 5C 75 44 38 30 31 5C 75 64 63 33 37 22 5D"/*["buD801budc37"]*/, null },
			{ 283, "yx_string_accepted_surrogate_pairs", "5B 22 5C 75 64 38 33 64 5C 75 64 65 33 39 5C 75 64 38 33 64 5C 75 64 63 38 64 22 5D"/*["bud83dbude39bud83dbudc8d"]*/, null },
			{ 284, "yx_string_allowed_escapes", "5B 22 5C 22 5C 5C 5C 2F 5C 62 5C 66 5C 6E 5C 72 5C 74 22 5D"/*["\"\\\/\b\f\n\r\t"]*/, null },
			{ 285, "yx_string_backslash_and_u_escaped_zero", "5B 22 5C 5C 75 30 30 30 30 22 5D"/*["\bu0000"]*/, null },
			{ 286, "yx_string_backslash_doublequotes", "5B 22 5C 22 22 5D"/*["\""]*/, null },
			{ 287, "yx_string_double_escape_a", "5B 22 5C 5C 61 22 5D"/*["\\a"]*/, null },
			{ 288, "yx_string_double_escape_n", "5B 22 5C 5C 6E 22 5D"/*["\\n"]*/, null },
			{ 289, "yx_string_escaped_control_character", "5B 22 5C 75 30 30 31 32 22 5D"/*["bu0012"]*/, null },
			{ 290, "yx_string_escaped_noncharacter", "5B 22 5C 75 46 46 46 46 22 5D"/*["buFFFF"]*/, null },
			{ 291, "yx_string_last_surrogates_1_and_2", "5B 22 5C 75 44 42 46 46 5C 75 44 46 46 46 22 5D"/*["buDBFFbuDFFF"]*/, null },
			{ 292, "yx_string_nbsp_uescaped", "5B 22 6E 65 77 5C 75 30 30 41 30 6C 69 6E 65 22 5D"/*["newbu00A0line"]*/, null },
			{ 293, "yx_string_nonCharacterInUTF-8_U+10FFFF", "5B 22 F4 8F BF BF 22 5D"/*["[dbff][dfff]"]*/, null },
			{ 294, "yx_string_nonCharacterInUTF-8_U+1FFFF", "5B 22 F0 9B BF BF 22 5D"/*["[d82f][dfff]"]*/, null },
			{ 295, "yx_string_nonCharacterInUTF-8_U+FFFF", "5B 22 EF BF BF 22 5D"/*["[ffff]"]*/, null },
			{ 296, "yx_string_null_escape", "5B 22 5C 75 30 30 30 30 22 5D"/*["bu0000"]*/, null },
			{ 297, "yx_string_one-byte-utf-8", "5B 22 5C 75 30 30 32 63 22 5D"/*["bu002c"]*/, null },
			{ 298, "yx_string_pi", "5B 22 CF 80 22 5D"/*["[3c0]"]*/, null },
			{ 299, "yx_string_surrogates_U+1D11E_MUSICAL_SYMBOL_G_CLEF", "5B 22 5C 75 44 38 33 34 5C 75 44 64 31 65 22 5D"/*["buD834buDd1e"]*/, null },
			{ 300, "yx_string_three-byte-utf-8", "5B 22 5C 75 30 38 32 31 22 5D"/*["bu0821"]*/, null },
			{ 301, "yx_string_two-byte-utf-8", "5B 22 5C 75 30 31 32 33 22 5D"/*["bu0123"]*/, null },
			{ 302, "yx_string_u+2028_line_sep", "5B 22 E2 80 A8 22 5D"/*["[2028]"]*/, null },
			{ 303, "yx_string_u+2029_par_sep", "5B 22 E2 80 A9 22 5D"/*["[2029]"]*/, null },
			{ 304, "yx_string_uEscape", "5B 22 5C 75 30 30 36 31 5C 75 33 30 61 66 5C 75 33 30 45 41 5C 75 33 30 62 39 22 5D"/*["bu0061bu30afbu30EAbu30b9"]*/, null },
			{ 305, "yx_string_uescaped_newline", "5B 22 6E 65 77 5C 75 30 30 30 41 6C 69 6E 65 22 5D"/*["newbu000Aline"]*/, null },
			{ 306, "yx_string_unescaped_char_delete", "5B 22 7F 22 5D"/*["[7f]"]*/, null },
			{ 307, "yx_string_unicode", "5B 22 5C 75 41 36 36 44 22 5D"/*["buA66D"]*/, null },
			{ 308, "yx_string_unicode_2", "5B 22 E2 8D 82 E3 88 B4 E2 8D 82 22 5D"/*["[2342][3234][2342]"]*/, null },
			{ 309, "yx_string_unicode_escaped_double_quote", "5B 22 5C 75 30 30 32 32 22 5D"/*["bu0022"]*/, null },
			{ 310, "yx_string_unicode_U+200B_ZERO_WIDTH_SPACE", "5B 22 5C 75 32 30 30 42 22 5D"/*["bu200B"]*/, null },
			{ 311, "yx_string_unicode_U+2064_invisible_plus", "5B 22 5C 75 32 30 36 34 22 5D"/*["bu2064"]*/, null },
			{ 312, "yx_string_unicodeEscapedBackslash", "5B 22 5C 75 30 30 35 43 22 5D"/*["bu005C"]*/, null },
			{ 313, "yx_string_utf16BE_no_BOM", "00 5B 00 22 00 E9 00 22 00 5D"/*[0][[0]"[0][fffd][0]"[0]]*/, null },
			{ 314, "yx_string_utf16LE_no_BOM", "5B 00 22 00 E9 00 22 00 5D 00"/*[[0]"[0][fffd][0]"[0]][0]*/, null },
			{ 315, "yx_string_utf8", "5B 22 E2 82 AC F0 9D 84 9E 22 5D"/*["[20ac][d834][dd1e]"]*/, null },
			{ 316, "yx_string_with_del_character", "5B 22 61 7F 61 22 5D"/*["a[7f]a"]*/, null },
			{ 317, "yx_structure_trailing_newline", "5B 22 61 22 5D 0A"/*["a"][a]*/, null },
		});
	}

	private final String name, errorText, jsonReadable;
	private final Object json;
	private final char expected;
	public boolean debug = false;

	public JsonParserEdgeCasesTest(Integer testNum, String name, String json, String errorText) throws Exception {
		this.name = name;
		this.json = name.charAt(1) == 'x' ? fromSpacedHex(json) : json;
		this.jsonReadable = name.charAt(1) == 'x' ? fromSpacedHexToUTF8(json) : json;
		this.expected = name.charAt(0);
		this.errorText = errorText;
	}

	@Test
	public void testStrict() throws Exception {
		JsonParser p = JsonParser.DEFAULT_STRICT;
		if (name.contains("utf16LE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16LE")).build();
		else if (name.contains("utf16BE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16BE")).build();

		// 'y' tests should always succeed.
		if (expected == 'y') {
			p.parse(json, Object.class);

		// 'n' tests should always fail.
		} else if (expected == 'n') {
			try {
				p.parse(json, Object.class);
				fail("ParseException expected.  Test="+name+", Input=" + jsonReadable);
			} catch (ParseException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getRootCause().getMessage(), e.getRootCause().getMessage().contains(errorText));
			} catch (IOException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getMessage(), e.getMessage().contains(errorText));
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+name+", Input=" + jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}

		// 'i' tests may or may not fail, but should through a ParseException and not kill the JVM.
		} else if (expected == 'i') {
			try {
				p.parse(json, Object.class);
			} catch (ParseException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getRootCause().getMessage(), e.getRootCause().getMessage().contains(errorText));
			} catch (IOException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getMessage(), e.getMessage().contains(errorText));
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+name+", Input=" + jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}
		}
	}

	@Test
	public void testLax() throws Exception {
		JsonParser p = JsonParser.DEFAULT;
		if (name.contains("utf16LE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16LE")).build();
		else if (name.contains("utf16BE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16BE")).build();

		// 'y' tests should always succeed.
		if (expected == 'y') {
			p.parse(json, Object.class);

		// 'n' tests may or may not fail for lax parser.
		} else if (expected == 'n') {
			try {
				p.parse(json, Object.class);
				//fail("ParseException expected.  Test="+name+", Input=" + json);
			} catch (ParseException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getRootCause().getMessage(), e.getRootCause().getMessage().contains(errorText));
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+name+", Input=" + jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}

		// 'i' tests may or may not fail, but should through a ParseException and not kill the JVM.
		} else if (expected == 'i') {
			try {
				p.parse(json, Object.class);
			} catch (ParseException e) {
				if (errorText != null)
					assertTrue("Got ParseException but didn't contain expected text '"+errorText+"'.  Test="+name+", Input=" + jsonReadable + ", Message=" + e.getRootCause().getMessage(), e.getRootCause().getMessage().contains(errorText));
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+name+", Input=" + jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}
		}
	}


	public static void main(String[] generateParams) throws Exception {
		File f = new File("src/test/resources/org/apache/juneau/json/jsonTestSuite");
		int i = 0;
		String pattern = "\n\t\t'{' {0}, \"{1}\", {2}, {3} '}',";
		StringBuilder sb = new StringBuilder();
		for (File fc : f.listFiles()) {
			String n = fc.getName();
			if (n.endsWith(".json")) {
				n = n.replaceAll("\\.json", "");
				String contents = specials.get(n);
				if (contents == null) {
					if (n.charAt(1) == 'x')
						contents = '"' + toHex(readBytes(fc)) + '"' + "/*" + decodeHex(read(fc)).replaceAll("\\\\u", "bu") + "*/";
					else
						contents = '"' + read(fc).replaceAll("\"", "\\\\\"").trim() + '"';
				}
				String errorText = errors.get(n);
				if (errorText != null)
					errorText = '"' + errorText + '"';
				sb.append(format(pattern, i++, fc.getName().replace(".json", ""), contents, errorText));
			}
		}
		System.err.println(sb); // NOT DEBUG
	}

	public static final Map<String,String> specials = new HashMap<>();
	static {
		specials.put("is_structure_500_nested_arrays", "StringUtils.repeat(500, \"[\") + StringUtils.repeat(500, \"]\")");
		specials.put("ns_structure_100000_opening_arrays", "StringUtils.repeat(100000, \"[\")");
		specials.put("ns_structure_open_array_object", "StringUtils.repeat(50000, \"[{\\\"\\\":\")");
	}

	public static final Map<String,String> errors = new HashMap<>();
	static {
		errors.put(/*11*/ "ix_string_not_in_unicode_range", "I/O exception occurred.  exception=MalformedInputException");
		errors.put(/*12*/ "ix_string_truncated-utf-8", "I/O exception occurred.  exception=MalformedInputException");
		errors.put(/*19*/ "ix_structure_UTF-8_BOM_empty_object", "Unrecognized syntax");
		errors.put(/*20*/ "n_array_1_true_without_comma", "Expected ',' or ']'");
		errors.put(/*21*/ "n_array_colon_instead_of_comma", "Expected ',' or ']'");
		errors.put(/*22*/ "n_array_comma_after_close", "Remainder after parse: ','");
		errors.put(/*23*/ "n_array_comma_and_number", "Missing value detected");
		errors.put(/*24*/ "n_array_double_comma", "Missing value detected");
		errors.put(/*26*/ "n_array_extra_close", "Remainder after parse: ']'");
		errors.put(/*27*/ "n_array_extra_comma", "Unexpected trailing comma in array");
		errors.put(/*28*/ "n_array_incomplete", "Expected ',' or ']'");
		errors.put(/*29*/ "n_array_incomplete_invalid_value", "Unrecognized syntax");
		errors.put(/*30*/ "n_array_inner_array_no_comma", "Expected ',' or ']'");
		errors.put(/*31*/ "n_array_items_separated_by_semicolon", "Expected ',' or ']'");
		errors.put(/*33*/ "n_array_just_minus", "NumberFormatException");
		errors.put(/*34*/ "n_array_missing_value", "Missing value detected");
		errors.put(/*35*/ "n_array_number_and_comma", "Unexpected trailing comma in array");
		errors.put(/*37*/ "n_array_star_inside", "Unrecognized syntax");
		errors.put(/*38*/ "n_array_unclosed", "Expected ',' or ']'");
		errors.put(/*39*/ "n_array_unclosed_trailing_comma", "Unexpected trailing comma in array");
		errors.put(/*40*/ "n_array_unclosed_with_object_inside", "Expected ',' or ']'");
		errors.put(/*41*/ "n_incomplete_false", "Unrecognized syntax");
		errors.put(/*42*/ "n_incomplete_null", "Unrecognized syntax");
		errors.put(/*43*/ "n_incomplete_true", "Unrecognized syntax");
		errors.put(/*44*/ "n_number_++", "Unrecognized syntax");
		errors.put(/*45*/ "n_number_+1", "Unrecognized syntax");
		errors.put(/*46*/ "n_number_+Inf", "Unrecognized syntax");
		errors.put(/*47*/ "n_number_-01", "Invalid JSON number");
		errors.put(/*48*/ "n_number_-1.0.", "NumberFormatException");
		errors.put(/*49*/ "n_number_-2.", "Invalid JSON number");
		errors.put(/*51*/ "n_number_.-1", "Invalid");
		errors.put(/*52*/ "n_number_.2e-3", "Invalid JSON number");
		errors.put(/*53*/ "n_number_0.1.2", "NumberFormatException");
		errors.put(/*54*/ "n_number_0.3e+", "NumberFormatException");
		errors.put(/*55*/ "n_number_0.3e", "NumberFormatException");
		errors.put(/*56*/ "n_number_0.e1", "Invalid JSON number");
		errors.put(/*57*/ "n_number_0_capital_E+", "NumberFormatException");
		errors.put(/*58*/ "n_number_0_capital_E", "NumberFormatException");
		errors.put(/*59*/ "n_number_0e+", "NumberFormatException");
		errors.put(/*60*/ "n_number_0e", "NumberFormatException");
		errors.put(/*61*/ "n_number_1.0e+", "NumberFormatException");
		errors.put(/*62*/ "n_number_1.0e-", "NumberFormatException");
		errors.put(/*63*/ "n_number_1.0e", "NumberFormatException");
		errors.put(/*64*/ "n_number_1_000", "Expected ',' or ']'");
		errors.put(/*65*/ "n_number_1eE2", "NumberFormatException");
		errors.put(/*66*/ "n_number_2.e+3", "Invalid JSON number");
		errors.put(/*67*/ "n_number_2.e-3", "Invalid JSON number");
		errors.put(/*68*/ "n_number_2.e3", "Invalid JSON number");
		errors.put(/*70*/ "n_number_expression", "NumberFormatException");
		errors.put(/*71*/ "n_number_hex_1_digit", "Invalid JSON number");
		errors.put(/*72*/ "n_number_hex_2_digits", "Invalid JSON number");
		errors.put(/*73*/ "n_number_Inf", "Unrecognized syntax");
		errors.put(/*74*/ "n_number_infinity", "Unrecognized syntax");
		errors.put(/*75*/ "n_number_invalid+-", "NumberFormatException");
		errors.put(/*76*/ "n_number_invalid-negative-real", "Expected ',' or ']'");
		errors.put(/*78*/ "n_number_minus_sign_with_trailing_garbage", "NumberFormatException");
		errors.put(/*80*/ "n_number_NaN", "Unrecognized syntax");
		errors.put(/*81*/ "n_number_neg_int_starting_with_zero", "Invalid JSON number");
		errors.put(/*82*/ "n_number_neg_real_without_int_part", "Invalid JSON number");
		errors.put(/*83*/ "n_number_neg_with_garbage_at_end", "NumberFormatException");
		errors.put(/*84*/ "n_number_real_garbage_after_e", "NumberFormatException");
		errors.put(/*85*/ "n_number_real_without_fractional_part", "Invalid");
		errors.put(/*86*/ "n_number_starting_with_dot", "Invalid JSON number");
		errors.put(/*87*/ "n_number_U+FF11_fullwidth_digit_one", "Unrecognized syntax");
		errors.put(/*88*/ "n_number_with_alpha", "NumberFormatException");
		errors.put(/*89*/ "n_number_with_alpha_char", "Expected ',' or ']'");
		errors.put(/*90*/ "n_number_with_leading_zero", "Invalid JSON number");
		errors.put(/*91*/ "n_object_bad_value", "Unrecognized syntax");
		errors.put(/*92*/ "n_object_comma_instead_of_colon", "Could not find ':'");
		errors.put(/*93*/ "n_object_double_colon", "Unrecognized syntax");
		errors.put(/*94*/ "n_object_garbage_at_end", "Could not find '}'");
		errors.put(/*95*/ "n_object_key_with_single_quotes", "Unquoted attribute detected");
		errors.put(/*96*/ "n_object_missing_colon", "Could not find ':'");
		errors.put(/*97*/ "n_object_missing_key", "Unquoted attribute detected");
		errors.put(/*98*/ "n_object_missing_semicolon", "Could not find ':'");
		errors.put(/*99*/ "n_object_missing_value", "Unrecognized syntax");
		errors.put(/*100*/ "n_object_no-colon", "Could not find ':'");
		errors.put(/*101*/ "n_object_non_string_key", "Unquoted attribute detected");
		errors.put(/*102*/ "n_object_non_string_key_but_huge_number_instead", "Unquoted attribute detected");
		errors.put(/*103*/ "n_object_repeated_null_null", "Unquoted attribute detected");
		errors.put(/*105*/ "n_object_single_quote", "Invalid quote character");
		errors.put(/*106*/ "n_object_trailing_comma", "Unexpected '}' found");
		errors.put(/*107*/ "n_object_trailing_comment", "Javascript comment detected");
		errors.put(/*109*/ "n_object_trailing_comment_slash_open", "Javascript comment detected");
		errors.put(/*112*/ "n_object_unquoted_key", "Unquoted attribute detected");
		errors.put(/*114*/ "n_object_with_single_string", "Could not find ':'");
		errors.put(/*115*/ "n_object_with_trailing_garbage", "Remainder after parse");
		errors.put(/*116*/ "n_single_space", "Empty input");
		errors.put(/*118*/ "n_string_single_quote", "Invalid quote character");
		errors.put(/*119*/ "n_string_single_string_no_double_quotes", "Unrecognized syntax");
		errors.put(/*120*/ "n_string_with_trailing_garbage", "Remainder after parse");
		errors.put(/*121*/ "n_structure_<.>", "Unrecognized syntax");
		errors.put(/*122*/ "n_structure_<null>", "Unrecognized syntax");
		errors.put(/*123*/ "n_structure_array_trailing_garbage", "Remainder after parse");
		errors.put(/*124*/ "n_structure_array_with_extra_array_close", "Remainder after parse");
		errors.put(/*126*/ "n_structure_capitalized_True", "Unrecognized syntax");
		errors.put(/*127*/ "n_structure_close_unopened_array", "Remainder after parse");
		errors.put(/*129*/ "n_structure_double_array", "Remainder after parse");
		errors.put(/*131*/ "n_structure_lone-open-bracket", "Expected one of the following characters: {,[,");
		errors.put(/*132*/ "n_structure_no_data", "Empty input");
		errors.put(/*133*/ "n_structure_null-byte-outside-string", "Unrecognized syntax");
		errors.put(/*134*/ "n_structure_number_with_trailing_garbage", "Remainder after parse");
		errors.put(/*135*/ "n_structure_object_followed_by_closing_object", "Remainder after parse");
		errors.put(/*136*/ "n_structure_object_unclosed_no_value", "Unrecognized syntax");
		errors.put(/*137*/ "n_structure_object_with_comment", "Javascript comment detected");
		errors.put(/*138*/ "n_structure_object_with_trailing_garbage", "Remainder after parse");
		errors.put(/*143*/ "n_structure_open_array_string", "Expected ',' or ']'");
		errors.put(/*150*/ "n_structure_single_star", "Unrecognized syntax");
		errors.put(/*151*/ "n_structure_trailing_#", "Remainder after parse");
		errors.put(/*152*/ "n_structure_unclosed_array", "Expected ',' or ']");
		errors.put(/*153*/ "n_structure_unclosed_array_partial_null", "Unrecognized syntax");
		errors.put(/*154*/ "n_structure_unclosed_array_unfinished_false", "Unrecognized syntax");
		errors.put(/*155*/ "n_structure_unclosed_array_unfinished_true", "Unrecognized syntax");
		errors.put(/*156*/ "n_structure_unclosed_object", "Could not find '}'");
		errors.put(/*157*/ "ns_structure_100000_opening_arrays", "Depth too deep");
		errors.put(/*158*/ "ns_structure_open_array_object", "Depth too deep");
		errors.put(/*172*/ "nx_string_1_surrogate_then_escape u", "Invalid Unicode escape sequence in string");
		errors.put(/*173*/ "nx_string_1_surrogate_then_escape u1", "Invalid Unicode escape sequence in string");
		errors.put(/*174*/ "nx_string_1_surrogate_then_escape u1x", "Invalid Unicode escape sequence in string");
		errors.put(/*176*/ "nx_string_accentuated_char_no_quotes", "Unrecognized syntax");
		errors.put(/*178*/ "nx_string_escape_x", "Invalid escape sequence in string");
		errors.put(/*181*/ "nx_string_escaped_emoji", "Invalid escape sequence in string");
		errors.put(/*183*/ "nx_string_incomplete_escaped_character", "Invalid Unicode escape sequence in string");
		errors.put(/*184*/ "nx_string_incomplete_surrogate", "Invalid Unicode escape sequence in string");
		errors.put(/*185*/ "nx_string_incomplete_surrogate_escape_invalid", "Invalid escape sequence");
		errors.put(/*187*/ "nx_string_invalid_backslash_esc", "Invalid escape sequence");
		errors.put(/*188*/ "nx_string_invalid_unicode_escape", "Invalid Unicode escape sequence in string");
		errors.put(/*189*/ "nx_string_invalid_utf-8", "MalformedInputException");
		errors.put(/*191*/ "nx_string_iso_latin_1", "MalformedInputException");
		errors.put(/*192*/ "nx_string_leading_uescaped_thinspace", "Unrecognized syntax");
		errors.put(/*193*/ "nx_string_lone_utf8_continuation_byte", "MalformedInputException");
		errors.put(/*194*/ "nx_string_no_quotes_with_bad_escape", "Unrecognized syntax");
		errors.put(/*195*/ "nx_string_overlong_sequence_2_bytes", "MalformedInputException");
		errors.put(/*196*/ "nx_string_overlong_sequence_6_bytes", "MalformedInputException");
		errors.put(/*197*/ "nx_string_overlong_sequence_6_bytes_null", "MalformedInputException");
		errors.put(/*202*/ "nx_string_unicode_CapitalU", "Invalid escape sequence");
		errors.put(/*203*/ "nx_string_UTF8_surrogate_U+D800", "MalformedInputException");
		errors.put(/*204*/ "nx_structure_ascii-unicode-identifier", "Unrecognized syntax");
		errors.put(/*207*/ "nx_structure_open_open", "Invalid escape sequence");
		errors.put(/*209*/ "nx_structure_U+2060_word_joined", "Unrecognized syntax");
		errors.put(/*210*/ "nx_structure_uescaped_LF_before_string", "Unrecognized syntax");
		errors.put(/*211*/ "nx_structure_unicode-identifier", "Unrecognized syntax");
		errors.put(/*212*/ "nx_structure_UTF8_BOM_no_data", "Unrecognized syntax");
		errors.put(/*213*/ "nx_structure_whitespace_formfeed", "Unrecognized syntax");
		errors.put(/*214*/ "nx_structure_whitespace_U+2060_word_joiner", "Unrecognized syntax");
	}
}