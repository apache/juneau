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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class JsonParserEdgeCases_Test extends SimpleTestBase {

	private static final Input[] INPUTS = {
		input(1, "is_structure_500_nested_arrays", repeat(500, "[") + repeat(500, "]"), null),
		input(2, "ix_object_key_lone_2nd_surrogate", "7B 22 5C 75 44 46 41 41 22 3A 30 7D"/*{"buDFAA":0}*/, null),  // NOSONAR
		input(3, "ix_string_1st_surrogate_but_2nd_missing", "5B 22 5C 75 44 41 44 41 22 5D"/*["buDADA"]*/, null),
		input(4, "ix_string_1st_valid_surrogate_2nd_invalid", "5B 22 5C 75 44 38 38 38 5C 75 31 32 33 34 22 5D"/*["buD888bu1234"]*/, null),
		input(5, "ix_string_incomplete_surrogate_and_escape_valid", "5B 22 5C 75 44 38 30 30 5C 6E 22 5D"/*["buD800\n"]*/, null),
		input(6, "ix_string_incomplete_surrogate_pair", "5B 22 5C 75 44 64 31 65 61 22 5D"/*["buDd1ea"]*/, null),
		input(7, "ix_string_incomplete_surrogates_escape_valid", "5B 22 5C 75 44 38 30 30 5C 75 44 38 30 30 5C 6E 22 5D"/*["buD800buD800\n"]*/, null),
		input(8, "ix_string_invalid_lonely_surrogate", "5B 22 5C 75 64 38 30 30 22 5D"/*["bud800"]*/, null),
		input(9, "ix_string_invalid_surrogate", "5B 22 5C 75 64 38 30 30 61 62 63 22 5D"/*["bud800abc"]*/, null),
		input(10, "ix_string_inverted_surrogates_U+1D11E", "5B 22 5C 75 44 64 31 65 5C 75 44 38 33 34 22 5D"/*["buDd1ebuD834"]*/, null),
		input(11, "ix_string_lone_second_surrogate", "5B 22 5C 75 44 46 41 41 22 5D"/*["buDFAA"]*/, null),
		input(12, "ix_string_not_in_unicode_range", "5B 22 F4 BF BF BF 22 5D"/*["[fffd][fffd][fffd][fffd]"]*/, null),
		input(13, "ix_string_truncated-utf-8", "5B 22 E0 FF 22 5D"/*["[fffd][fffd]"]*/, null),
		input(14, "ix_string_unicode_U+10FFFE_nonchar", "5B 22 5C 75 44 42 46 46 5C 75 44 46 46 45 22 5D"/*["buDBFFbuDFFE"]*/, null),
		input(15, "ix_string_unicode_U+1FFFE_nonchar", "5B 22 5C 75 44 38 33 46 5C 75 44 46 46 45 22 5D"/*["buD83FbuDFFE"]*/, null),
		input(16, "ix_string_unicode_U+FDD0_nonchar", "5B 22 5C 75 46 44 44 30 22 5D"/*["buFDD0"]*/, null),
		input(17, "ix_string_unicode_U+FFFE_nonchar", "5B 22 5C 75 46 46 46 45 22 5D"/*["buFFFE"]*/, null),
		input(18, "ix_string_UTF-16LE_with_BOM", "FF FE 5B 00 22 00 E9 00 22 00 5D 00"/*[fffd][fffd][[0]"[0][fffd][0]"[0]][0]*/, null),
		input(19, "ix_string_UTF-8_invalid_sequence", "5B 22 E6 97 A5 D1 88 FA 22 5D"/*["[65e5][448][fffd]"]*/, null),
		input(20, "ix_structure_UTF-8_BOM_empty_object", "EF BB BF 7B 7D"/*[feff]{}*/, "Unrecognized syntax"),  // NOSONAR
		input(21, "n_array_1_true_without_comma", "[1 true]", "Expected ',' or ']'"),
		input(22, "n_array_colon_instead_of_comma", "[\"\": 1]", "Expected ',' or ']'"),
		input(23, "n_array_comma_after_close", "[\"\"],", "Remainder after parse: ','"),
		input(24, "n_array_comma_and_number", "[,1]", "Missing value detected"),
		input(25, "n_array_double_comma", "[1,,2]", "Missing value detected"),
		input(26, "n_array_double_extra_comma", "[\"x\",,]", null),
		input(27, "n_array_extra_close", "[\"x\"]]", "Remainder after parse: ']'"),
		input(28, "n_array_extra_comma", "[\"\",]", "Unexpected trailing comma in array"),
		input(29, "n_array_incomplete", "[\"x\"", "Expected ',' or ']'"),
		input(30, "n_array_incomplete_invalid_value", "[x", "Unrecognized syntax"),
		input(31, "n_array_inner_array_no_comma", "[3[4]]", "Expected ',' or ']'"),
		input(32, "n_array_items_separated_by_semicolon", "[1:2]", "Expected ',' or ']'"),
		input(33, "n_array_just_comma", "[,]", null),
		input(34, "n_array_just_minus", "[-]", "NumberFormatException"),
		input(35, "n_array_missing_value", "[   , \"\"]", "Missing value detected"),
		input(36, "n_array_number_and_comma", "[1,]", "Unexpected trailing comma in array"),
		input(37, "n_array_number_and_several_commas", "[1,,]", null),
		input(38, "n_array_star_inside", "[*]", "Unrecognized syntax"),
		input(39, "n_array_unclosed", "[\"\"", "Expected ',' or ']'"),
		input(40, "n_array_unclosed_trailing_comma", "[1, ", "Unexpected trailing comma in array"),
		input(41, "n_array_unclosed_with_object_inside", "[{}", "Expected ',' or ']'"),
		input(42, "n_incomplete_false", "[fals]", "Unrecognized syntax"),
		input(43, "n_incomplete_null", "[nul]", "Unrecognized syntax"),
		input(44, "n_incomplete_true", "[tru]", "Unrecognized syntax"),
		input(45, "n_number_++", "[++1234]", "Unrecognized syntax"),
		input(46, "n_number_+1", "[+1]", "Unrecognized syntax"),
		input(47, "n_number_+Inf", "[+Inf]", "Unrecognized syntax"),
		input(48, "n_number_-01", "[-01]", "Invalid JSON number"),
		input(49, "n_number_-1.0.", "[-1.0.]", "NumberFormatException"),
		input(50, "n_number_-2.", "[-2.]", "Invalid JSON number"),
		input(51, "n_number_-NaN", "[-NaN]", null),
		input(52, "n_number_.-1", "[.-1]", null),
		input(53, "n_number_.2e-3", "[.2e-3]", "Invalid JSON number"),
		input(54, "n_number_0.1.2", "[0.1.2]", "NumberFormatException"),
		input(55, "n_number_0.3e+", "[0.3e+]", "NumberFormatException"),
		input(56, "n_number_0.3e", "[0.3e]", "NumberFormatException"),
		input(57, "n_number_0.e1", "[0.e1]", "Invalid JSON number"),
		input(58, "n_number_0_capital_E+", "[0E+]", "NumberFormatException"),
		input(59, "n_number_0_capital_E", "[0E]", "NumberFormatException"),
		input(60, "n_number_0e+", "[0e+]", "NumberFormatException"),
		input(61, "n_number_0e", "[0e]", "NumberFormatException"),
		input(62, "n_number_1.0e+", "[1.0e+]", "NumberFormatException"),
		input(63, "n_number_1.0e-", "[1.0e-]", "NumberFormatException"),
		input(64, "n_number_1.0e", "[1.0e]", "NumberFormatException"),
		input(65, "n_number_1_000", "[1 000.0]", "Expected ',' or ']'"),
		input(66, "n_number_1eE2", "[1eE2]", "NumberFormatException"),
		input(67, "n_number_2.e+3", "[2.e+3]", "Invalid JSON number"),
		input(68, "n_number_2.e-3", "[2.e-3]", "Invalid JSON number"),
		input(69, "n_number_2.e3", "[2.e3]", "Invalid JSON number"),
		input(70, "n_number_9.e+", "[9.e+]", null),
		input(71, "n_number_expression", "[1+2]", "NumberFormatException"),
		input(72, "n_number_hex_1_digit", "[0x1]", "Invalid JSON number"),
		input(73, "n_number_hex_2_digits", "[0x42]", "Invalid JSON number"),
		input(74, "n_number_Inf", "[Inf]", "Unrecognized syntax"),
		input(75, "n_number_infinity", "[Infinity]", "Unrecognized syntax"),
		input(76, "n_number_invalid+-", "[0e+-1]", "NumberFormatException"),
		input(77, "n_number_invalid-negative-real", "[-123.123foo]", "Expected ',' or ']'"),
		input(78, "n_number_minus_infinity", "[-Infinity]", null),
		input(79, "n_number_minus_sign_with_trailing_garbage", "[-foo]", "NumberFormatException"),
		input(80, "n_number_minus_space_1", "[- 1]", null),
		input(81, "n_number_NaN", "[NaN]", "Unrecognized syntax"),
		input(82, "n_number_neg_int_starting_with_zero", "[-012]", "Invalid JSON number"),
		input(83, "n_number_neg_real_without_int_part", "[-.123]", "Invalid JSON number"),
		input(84, "n_number_neg_with_garbage_at_end", "[-1x]", "NumberFormatException"),
		input(85, "n_number_real_garbage_after_e", "[1ea]", "NumberFormatException"),
		input(86, "n_number_real_without_fractional_part", "[1.]", "Invalid"),
		input(87, "n_number_starting_with_dot", "[.123]", "Invalid JSON number"),
		input(88, "n_number_U+FF11_fullwidth_digit_one", "[１]", "Unrecognized syntax"),
		input(89, "n_number_with_alpha", "[1.2a-3]", "NumberFormatException"),
		input(90, "n_number_with_alpha_char", "[1.8011670033376514H-308]", "Expected ',' or ']'"),
		input(91, "n_number_with_leading_zero", "[012]", "Invalid JSON number"),
		input(92, "n_object_bad_value", "[\"x\"", null),
		input(93, "n_object_comma_instead_of_colon", "{\"x\", null)", "Could not find ':'"),
		input(94, "n_object_double_colon", "{\"x\"::\"b\"}", "Unrecognized syntax"),
		input(95, "n_object_garbage_at_end", "{\"a\":\"a\" 123}", "Could not find '}'"),
		input(96, "n_object_key_with_single_quotes", "{key: 'value'}", "Unquoted attribute detected"),
		input(97, "n_object_missing_colon", "{\"a\" b}", "Could not find ':'"),
		input(98, "n_object_missing_key", "{:\"b\"}", null),
		input(99, "n_object_missing_semicolon", "{\"a\" \"b\"}", "Could not find ':'"),
		input(100, "n_object_missing_value", "{\"a\":", "Unrecognized syntax"),
		input(101, "n_object_no-colon", "{\"a\"", "Could not find ':'"),
		input(102, "n_object_non_string_key", "{1:1}", "Unquoted attribute detected"),
		input(103, "n_object_non_string_key_but_huge_number_instead", "{9999E9999:1}", "Unquoted attribute detected"),
		input(104, "n_object_repeated_null_null", "{null:null,null:null}", "Unquoted attribute detected"),
		input(105, "n_object_several_trailing_commas", "{\"id\":0,,,,,}", null),
		input(106, "n_object_single_quote", "{'a':0}", "Invalid quote character"),
		input(107, "n_object_trailing_comma", "{\"id\":0,}", "Unexpected '}' found"),
		input(108, "n_object_trailing_comment", "{\"a\":\"b\"}/**/", "Javascript comment detected"),
		input(109, "n_object_trailing_comment_open", "{\"a\":\"b\"}/**//", null),
		input(110, "n_object_trailing_comment_slash_open", "{\"a\":\"b\"}//", "Javascript comment detected"),
		input(111, "n_object_trailing_comment_slash_open_incomplete", "{\"a\":\"b\"}/", null),
		input(112, "n_object_two_commas_in_a_row", "{\"a\":\"b\",,\"c\":\"d\"}", null),
		input(113, "n_object_unquoted_key", "{a: \"b\"}", "Unquoted attribute detected"),
		input(114, "n_object_unterminated-value", "{\"a\":\"a", null),
		input(115, "n_object_with_single_string", "{ \"foo\" : \"bar\", \"a\" }", "Could not find ':'"),
		input(116, "n_object_with_trailing_garbage", "{\"a\":\"b\"}#", "Remainder after parse"),
		input(117, "n_single_space", "", "Empty input"),
		input(118, "n_string_single_doublequote", "\"", null),
		input(119, "n_string_single_quote", "['single quote']", "Invalid quote character"),
		input(120, "n_string_single_string_no_double_quotes", "abc", "Unrecognized syntax"),
		input(121, "n_string_with_trailing_garbage", "\"\"x", "Remainder after parse"),
		input(122, "n_structure_<.>", "<.>", "Unrecognized syntax"),
		input(123, "n_structure_<null>", "[<null>]", "Unrecognized syntax"),
		input(124, "n_structure_array_trailing_garbage", "[1]x", "Remainder after parse"),
		input(125, "n_structure_array_with_extra_array_close", "[1]]", "Remainder after parse"),
		input(126, "n_structure_array_with_unclosed_string", "[\"asd]", null),
		input(127, "n_structure_capitalized_True", "[True]", "Unrecognized syntax"),
		input(128, "n_structure_close_unopened_array", "1]", "Remainder after parse"),
		input(129, "n_structure_comma_instead_of_closing_brace", "{\"x\": true", null),
		input(130, "n_structure_double_array", "[][]", "Remainder after parse"),
		input(131, "n_structure_end_array", "]", null),
		input(132, "n_structure_lone-open-bracket", "[", "Expected one of the following characters: {,[,"),
		input(133, "n_structure_no_data", "", "Empty input"),
		input(134, "n_structure_null-byte-outside-string", "[ ]", "Unrecognized syntax"),
		input(135, "n_structure_number_with_trailing_garbage", "2@", "Remainder after parse"),
		input(136, "n_structure_object_followed_by_closing_object", "{}}", "Remainder after parse"),
		input(137, "n_structure_object_unclosed_no_value", "{\"\":", "Unrecognized syntax"),
		input(138, "n_structure_object_with_comment", "{\"a\":/*comment*/\"b\"}", "Javascript comment detected"),
		input(139, "n_structure_object_with_trailing_garbage", "{\"a\": true} \"x\"", "Remainder after parse"),
		input(140, "n_structure_open_array_apostrophe", "['", null),
		input(141, "n_structure_open_array_comma", "[", null),
		input(142, "n_structure_open_array_open_object", "[{", null),
		input(143, "n_structure_open_array_open_string", "[\"a", null),
		input(144, "n_structure_open_array_string", "[\"a\"", "Expected ',' or ']'"),
		input(145, "n_structure_open_object", "{", null),
		input(146, "n_structure_open_object_close_array", "{]", null),
		input(147, "n_structure_open_object_comma", "{", null),
		input(148, "n_structure_open_object_open_array", "{[", null),
		input(149, "n_structure_open_object_open_string", "{\"a", null),
		input(150, "n_structure_open_object_string_with_apostrophes", "{'a'", null),
		input(151, "n_structure_single_star", "*", "Unrecognized syntax"),
		input(152, "n_structure_trailing_#", "{\"a\":\"b\"}#{}", "Remainder after parse"),
		input(153, "n_structure_unclosed_array", "[1", "Expected ',' or ']"),
		input(154, "n_structure_unclosed_array_partial_null", "[ false", null),
		input(155, "n_structure_unclosed_array_unfinished_false", "[ true", null),
		input(156, "n_structure_unclosed_array_unfinished_true", "[ false", null),
		input(157, "n_structure_unclosed_object", "{\"asd\":\"asd\"", null),
		input(158, "ns_structure_100000_opening_arrays", repeat(100000, "["), "Depth too deep"),
		input(159, "ns_structure_open_array_object", repeat(50000, "[{\"\":"), "Depth too deep"),
		input(160, "nx_array_a_invalid_utf8", "5B 61 E5 5D"/*[a[fffd]]*/, null),
		input(161, "nx_array_invalid_utf8", "5B FF 5D"/*[[fffd]]*/, null),
		input(161, "nx_array_newlines_unclosed", "5B 22 61 22 2C 0A 34 0A 2C 31 2C"/*["a",[a]4[a],1,*/, null),
		input(162, "nx_array_spaces_vertical_tab_formfeed", "5B 22 0B 61 22 5C 66 5D"/*["[b]a"\f]*/, null),
		input(163, "nx_array_unclosed_with_new_lines", "5B 31 2C 0A 31 0A 2C 31"/*[1,[a]1[a],1*/, null),
		input(164, "nx_multidigit_number_then_00", "31 32 33 00"/*123[0]*/, null),
		input(166, "nx_number_invalid-utf-8-in-bigger-int", "5B 31 32 33 E5 5D"/*[123[fffd]]*/, null),
		input(167, "nx_number_invalid-utf-8-in-exponent", "5B 31 65 31 E5 5D"/*[1e1[fffd]]*/, null),
		input(168, "nx_number_invalid-utf-8-in-int", "5B 30 E5 5D 0A"/*[0[fffd]][a]*/, null),
		input(169, "nx_number_real_with_invalid_utf8_after_e", "5B 31 65 E5 5D"/*[1e[fffd]]*/, null),
		input(170, "nx_object_bracket_key", "7B 5B 3A 20 22 78 22 7D 0A"/*{[: "x"}[a]*/, null),
		input(171, "nx_object_emoji", "7B F0 9F 87 A8 F0 9F 87 AD 7D"/*{[d83c][dde8][d83c][dded]}*/, null),  // NOSONAR
		input(172, "nx_object_pi_in_key_and_trailing_comma", "7B 22 B9 22 3A 22 30 22 2C 7D"/*{"[fffd]":"0", null)*/, null),  //. NOSONAR
		input(173, "nx_string_1_surrogate_then_escape u", "5B 22 5C 75 44 38 30 30 5C 75 22 5D"/*["buD800bu"]*/, "Invalid Unicode escape sequence in string"),
		input(174, "nx_string_1_surrogate_then_escape u1", "5B 22 5C 75 44 38 30 30 5C 75 31 22 5D"/*["buD800bu1"]*/, "Invalid Unicode escape sequence in string"),
		input(175, "nx_string_1_surrogate_then_escape u1x", "5B 22 5C 75 44 38 30 30 5C 75 31 78 22 5D"/*["buD800bu1x"]*/, "Invalid Unicode escape sequence in string"),
		input(176, "nx_string_1_surrogate_then_escape", "5B 22 5C 75 44 38 30 30 5C 22 5D"/*["buD800\"]*/, null),
		input(177, "nx_string_accentuated_char_no_quotes", "5B C3 A9 5D"/*[[e9]]*/, "Unrecognized syntax"),
		input(178, "nx_string_backslash_00", "5B 22 5C 00 22 5D"/*["\[0]"]*/, null),
		input(179, "nx_string_escape_x", "5B 22 5C 78 30 30 22 5D"/*["\x00"]*/, "Invalid escape sequence in string"),
		input(180, "nx_string_escaped_backslash_bad", "5B 22 5C 5C 5C 22 5D"/*["\\\"]*/, null),
		input(181, "nx_string_escaped_ctrl_char_tab", "5B 22 5C 09 22 5D"/*["\[9]"]*/, null),
		input(182, "nx_string_escaped_emoji", "5B 22 5C F0 9F 8C 80 22 5D"/*["\[d83c][df00]"]*/, "Invalid escape sequence in string"),
		input(183, "nx_string_incomplete_escape", "5B 22 5C 22 5D"/*["\"]*/, null),
		input(184, "nx_string_incomplete_escaped_character", "5B 22 5C 75 30 30 41 22 5D"/*["bu00A"]*/, "Invalid Unicode escape sequence in string"),
		input(185, "nx_string_incomplete_surrogate", "5B 22 5C 75 44 38 33 34 5C 75 44 64 22 5D"/*["buD834buDd"]*/, "Invalid Unicode escape sequence in string"),
		input(186, "nx_string_incomplete_surrogate_escape_invalid", "5B 22 5C 75 44 38 30 30 5C 75 44 38 30 30 5C 78 22 5D"/*["buD800buD800\x"]*/, "Invalid escape sequence"),
		input(187, "nx_string_invalid-utf-8-in-escape", "5B 22 5C 75 E5 22 5D"/*["bu[fffd]"]*/, null),
		input(188, "nx_string_invalid_backslash_esc", "5B 22 5C 61 22 5D"/*["\a"]*/, "Invalid escape sequence"),
		input(189, "nx_string_invalid_unicode_escape", "5B 22 5C 75 71 71 71 71 22 5D"/*["buqqqq"]*/, "Invalid Unicode escape sequence in string"),
		input(190, "nx_string_invalid_utf-8", "5B 22 FF 22 5D"/*["[fffd]"]*/, null),
		input(191, "nx_string_invalid_utf8_after_escape", "5B 22 5C E5225D"/*["\[fffd]"]*/, null),
		input(192, "nx_string_iso_latin_1", "5B 22 E9 22 5D"/*["[fffd]"]*/, null),
		input(193, "nx_string_leading_uescaped_thinspace", "5B 5C 75 30 30 32 30 22 61 73 64 22 5D"/*[bu0020"asd"]*/, "Unrecognized syntax"),
		input(194, "nx_string_lone_utf8_continuation_byte", "5B 22 81 22 5D"/*["[fffd]"]*/, null),
		input(195, "nx_string_no_quotes_with_bad_escape", "5B 5C 6E 5D"/*[\n]*/, "Unrecognized syntax"),
		input(196, "nx_string_overlong_sequence_2_bytes", "5B 22 C0 AF 22 5D"/*["[fffd][fffd]"]*/, null),
		input(197, "nx_string_overlong_sequence_6_bytes", "5B 22 FC 83 BF BF BF BF 22 5D"/*["[fffd][fffd][fffd][fffd][fffd][fffd]"]*/, null),
		input(198, "nx_string_overlong_sequence_6_bytes_null", "5B 22 FC 80 80 80 80 80 22 5D"/*["[fffd][fffd][fffd][fffd][fffd][fffd]"]*/, null),
		input(199, "nx_string_start_escape_unclosed", "5B 22 5C"/*["\*/, null),
		input(200, "nx_string_unescaped_crtl_char", "5B 22 61 00 61 22 5D"/*["a[0]a"]*/, null),
		input(201, "nx_string_unescaped_newline", "5B 22 6E 65 77 0A 6C 69 6E 65 22 5D"/*["new[a]line"]*/, null),
		input(202, "nx_string_unescaped_tab", "5B 22 09 22 5D"/*["[9]"]*/, null),
		input(203, "nx_string_unicode_CapitalU", "22 5C 55 41 36 36 44 22"/*"\UA66D"*/, "Invalid escape sequence"),
		input(204, "ix_string_UTF8_surrogate_U+D800", "5B 22 ED A0 80 22 5D"/*["[fffd]"]*/, null),  // Succeeds on Java 8, fails on Java 6 & 7.
		input(205, "nx_structure_ascii-unicode-identifier", "61 C3 A5"/*a[e5]*/, "Unrecognized syntax"),
		input(206, "nx_structure_incomplete_UTF8_BOM", "EF BB 7B 7D"/*[fffd]{}*/, null),  // NOSONAR
		input(207, "nx_structure_lone-invalid-utf-8", "E5"/*[fffd]*/, null),
		input(208, "nx_structure_open_open", "5B 22 5C 7B 5B 22 5C 7B 5B 22 5C 7B 5B 22 5C 7B"/*["\{["\{["\{["\{*/, "Invalid escape sequence"),  // NOSONAR
		input(209, "nx_structure_single_point", "E9"/*[fffd]*/, null),
		input(210, "nx_structure_U+2060_word_joined", "5B E2 81 A0 5D"/*[[2060]]*/, "Unrecognized syntax"),
		input(211, "nx_structure_uescaped_LF_before_string", "5B 5C 75 30 30 30 41 22 22 5D"/*[bu000A""]*/, "Unrecognized syntax"),
		input(212, "nx_structure_unicode-identifier", "C3 A5"/*[e5]*/, "Unrecognized syntax"),
		input(213, "nx_structure_UTF8_BOM_no_data", "EF BB BF"/*[feff]*/, "Unrecognized syntax"),
		input(214, "nx_structure_whitespace_formfeed", "5B 0C 5D"/*[[c]]*/, "Unrecognized syntax"),
		input(215, "nx_structure_whitespace_U+2060_word_joiner", "5B E2 81 A0 5D"/*[[2060]]*/, "Unrecognized syntax"),
		input(216, "y_array_arraysWithSpaces", "[[]   ]", null),
		input(217, "y_array_empty-string", "[\"\"]", null),
		input(218, "y_array_empty", "[]", null),
		input(219, "y_array_ending_with_newline", "[\"a\"]", null),
		input(220, "y_array_false", "[false]", null),
		input(221, "y_array_heterogeneous", "[null, 1, \"1\", {}]", null),
		input(222, "y_array_null", "[null]", null),
		input(223, "y_array_with_leading_space", "[1]", null),
		input(224, "y_array_with_several_null", "[1,null,null,null,2]", null),
		input(225, "y_array_with_trailing_space", "[2]", null),
		input(226, "y_number", "[123e65]", null),
		input(227, "y_number_0e+1", "[0e+1]", null),
		input(228, "y_number_0e1", "[0e1]", null),
		input(229, "y_number_after_space", "[ 4]", null),
		input(230, "y_number_double_close_to_zero", "[-0.000000000000000000000000000000000000000000000000000000000000000000000000000001]", null),
		input(231, "y_number_double_huge_neg_exp", "[123.456e-789]", null),
		input(232, "y_number_huge_exp", "[0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]", null),
		input(233, "y_number_int_with_exp", "[20e1]", null),
		input(234, "y_number_minus_zero", "[-0]", null),
		input(235, "y_number_neg_int_huge_exp", "[-1e+9999]", null),
		input(236, "y_number_negative_int", "[-123]", null),
		input(237, "y_number_negative_one", "[-1]", null),
		input(238, "y_number_negative_zero", "[-0]", null),
		input(239, "y_number_pos_double_huge_exp", "[1.5e+9999]", null),
		input(240, "y_number_real_capital_e", "[1E22]", null),
		input(241, "y_number_real_capital_e_neg_exp", "[1E-2]", null),
		input(242, "y_number_real_capital_e_pos_exp", "[1E+2]", null),
		input(243, "y_number_real_exponent", "[123e45]", null),
		input(244, "y_number_real_fraction_exponent", "[123.456e78]", null),
		input(245, "y_number_real_neg_exp", "[1e-2]", null),
		input(246, "y_number_real_neg_overflow", "[-123123e100000]", null),
		input(247, "y_number_real_pos_exponent", "[1e+2]", null),
		input(248, "y_number_real_pos_overflow", "[123123e100000]", null),
		input(249, "y_number_real_underflow", "[123e-10000000]", null),
		input(250, "y_number_simple_int", "[123]", null),
		input(251, "y_number_simple_real", "[123.456789]", null),
		input(252, "y_number_too_big_neg_int", "[-123123123123123123123123123123]", null),
		input(253, "y_number_too_big_pos_int", "[100000000000000000000]", null),
		input(254, "y_number_very_big_negative_int", "[-237462374673276894279832749832423479823246327846]", null),
		input(255, "y_object", "{\"asd\":\"sdf\", \"dfg\":\"fgh\"}", null),
		input(256, "y_object_basic", "{\"asd\":\"sdf\"}", null),
		input(257, "y_object_duplicated_key", "{\"a\":\"b\",\"a\":\"c\"}", null),
		input(258, "y_object_duplicated_key_and_value", "{\"a\":\"b\",\"a\":\"b\"}", null),
		input(259, "y_object_empty", "{}", null),
		input(260, "y_object_empty_key", "{\"\":0}", null),
		input(261, "y_object_extreme_numbers", "{ \"min\": -1.0e+28, \"max\": 1.0e+28 }", null),
		input(262, "y_object_long_strings", "{\"x\":[{\"id\": \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}], \"id\": \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}", null),
		input(263, "y_object_simple", "{\"a\":[]}", null),
		input(264, "y_string_comments", "[\"a/*b*/c/*d//e\"]", null),
		input(265, "y_string_in_array", "[\"asd\"]", null),
		input(266, "y_string_in_array_with_leading_space", "[ \"asd\"]", null),
		input(267, "y_string_simple_ascii", "[\"asd \"]", null),
		input(268, "y_string_space", "\" \"", null),
		input(269, "y_structure_lonely_false", "false", null),
		input(270, "y_structure_lonely_int", "42", null),
		input(271, "y_structure_lonely_negative_real", "-0.1", null),
		input(272, "y_structure_lonely_null", "null", null),
		input(273, "y_structure_lonely_string", "\"asd\"", null),
		input(274, "y_structure_lonely_true", "true", null),
		input(275, "y_structure_string_empty", "\"\"", null),
		input(276, "y_structure_true_in_array", "[true]", null),
		input(277, "y_structure_whitespace_array", "[]", null),
		input(278, "yx_array_with_1_and_newline", "5B 31 0A 5D"/*[1[a]]*/, null),
		input(279, "yx_object_escaped_null_in_key", "7B 22 66 6F 6F 5C 75 30 30 30 30 62 61 72 22 3A 20 34 32 7D"/*{"foobu0000bar": 42}*/, null),  // NOSONAR
		input(280, "yx_object_string_unicode", "7B 22 74 69 74 6C 65 22 3A 22 5C 75 30 34 31 66 5C 75 30 34 33 65 5C 75 30 34 33 62 5C 75 30 34 34 32 5C 75 30 34 33 65 5C 75 30 34 34 30 5C 75 30 34 33 30 20 5C 75 30 34 31 37 5C 75 30 34 33 35 5C 75 30 34 33 63 5C 75 30 34 33 62 5C 75 30 34 33 35 5C 75 30 34 33 61 5C 75 30 34 33 65 5C 75 30 34 33 66 5C 75 30 34 33 30 22 20 7D"/*{"title":"bu041fbu043ebu043bbu0442bu043ebu0440bu0430 bu0417bu0435bu043cbu043bbu0435bu043abu043ebu043fbu0430" }*/, null),  // NOSONAR
		input(281, "yx_object_with_newlines", "7B 0A 22 61 22 3A 20 22 62 22 0A 7D"/*{[a]"a": "b"[a]}*/, null),  // NOSONAR
		input(282, "yx_string_1_2_3_bytes_UTF-8_sequences", "5B 22 5C 75 30 30 36 30 5C 75 30 31 32 61 5C 75 31 32 41 42 22 5D"/*["bu0060bu012abu12AB"]*/, null),
		input(283, "yx_string_accepted_surrogate_pair", "5B 22 5C 75 44 38 30 31 5C 75 64 63 33 37 22 5D"/*["buD801budc37"]*/, null),
		input(284, "yx_string_accepted_surrogate_pairs", "5B 22 5C 75 64 38 33 64 5C 75 64 65 33 39 5C 75 64 38 33 64 5C 75 64 63 38 64 22 5D"/*["bud83dbude39bud83dbudc8d"]*/, null),
		input(285, "yx_string_allowed_escapes", "5B 22 5C 22 5C 5C 5C 2F 5C 62 5C 66 5C 6E 5C 72 5C 74 22 5D"/*["\"\\\/\b\f\n\r\t"]*/, null),
		input(286, "yx_string_backslash_and_u_escaped_zero", "5B 22 5C 5C 75 30 30 30 30 22 5D"/*["\bu0000"]*/, null),
		input(287, "yx_string_backslash_doublequotes", "5B 22 5C 22 22 5D"/*["\""]*/, null),
		input(288, "yx_string_double_escape_a", "5B 22 5C 5C 61 22 5D"/*["\\a"]*/, null),
		input(289, "yx_string_double_escape_n", "5B 22 5C 5C 6E 22 5D"/*["\\n"]*/, null),
		input(290, "yx_string_escaped_control_character", "5B 22 5C 75 30 30 31 32 22 5D"/*["bu0012"]*/, null),
		input(291, "yx_string_escaped_noncharacter", "5B 22 5C 75 46 46 46 46 22 5D"/*["buFFFF"]*/, null),
		input(292, "yx_string_last_surrogates_1_and_2", "5B 22 5C 75 44 42 46 46 5C 75 44 46 46 46 22 5D"/*["buDBFFbuDFFF"]*/, null),
		input(293, "yx_string_nbsp_uescaped", "5B 22 6E 65 77 5C 75 30 30 41 30 6C 69 6E 65 22 5D"/*["newbu00A0line"]*/, null),
		input(294, "yx_string_nonCharacterInUTF-8_U+10FFFF", "5B 22 F4 8F BF BF 22 5D"/*["[dbff][dfff]"]*/, null),
		input(295, "yx_string_nonCharacterInUTF-8_U+1FFFF", "5B 22 F0 9B BF BF 22 5D"/*["[d82f][dfff]"]*/, null),
		input(296, "yx_string_nonCharacterInUTF-8_U+FFFF", "5B 22 EF BF BF 22 5D"/*["[ffff]"]*/, null),
		input(297, "yx_string_null_escape", "5B 22 5C 75 30 30 30 30 22 5D"/*["bu0000"]*/, null),
		input(298, "yx_string_one-byte-utf-8", "5B 22 5C 75 30 30 32 63 22 5D"/*["bu002c"]*/, null),
		input(299, "yx_string_pi", "5B 22 CF 80 22 5D"/*["[3c0]"]*/, null),
		input(300, "yx_string_surrogates_U+1D11E_MUSICAL_SYMBOL_G_CLEF", "5B 22 5C 75 44 38 33 34 5C 75 44 64 31 65 22 5D"/*["buD834buDd1e"]*/, null),
		input(301, "yx_string_three-byte-utf-8", "5B 22 5C 75 30 38 32 31 22 5D"/*["bu0821"]*/, null),
		input(302, "yx_string_two-byte-utf-8", "5B 22 5C 75 30 31 32 33 22 5D"/*["bu0123"]*/, null),
		input(303, "yx_string_u+2028_line_sep", "5B 22 E2 80 A8 22 5D"/*["[2028]"]*/, null),
		input(304, "yx_string_u+2029_par_sep", "5B 22 E2 80 A9 22 5D"/*["[2029]"]*/, null),
		input(305, "yx_string_uEscape", "5B 22 5C 75 30 30 36 31 5C 75 33 30 61 66 5C 75 33 30 45 41 5C 75 33 30 62 39 22 5D"/*["bu0061bu30afbu30EAbu30b9"]*/, null),
		input(306, "yx_string_uescaped_newline", "5B 22 6E 65 77 5C 75 30 30 30 41 6C 69 6E 65 22 5D"/*["newbu000Aline"]*/, null),
		input(307, "yx_string_unescaped_char_delete", "5B 22 7F 22 5D"/*["[7f]"]*/, null),
		input(308, "yx_string_unicode", "5B 22 5C 75 41 36 36 44 22 5D"/*["buA66D"]*/, null),
		input(309, "yx_string_unicode_2", "5B 22 E2 8D 82 E3 88 B4 E2 8D 82 22 5D"/*["[2342][3234][2342]"]*/, null),
		input(310, "yx_string_unicode_escaped_double_quote", "5B 22 5C 75 30 30 32 32 22 5D"/*["bu0022"]*/, null),
		input(311, "yx_string_unicode_U+200B_ZERO_WIDTH_SPACE", "5B 22 5C 75 32 30 30 42 22 5D"/*["bu200B"]*/, null),
		input(312, "yx_string_unicode_U+2064_invisible_plus", "5B 22 5C 75 32 30 36 34 22 5D"/*["bu2064"]*/, null),
		input(313, "yx_string_unicodeEscapedBackslash", "5B 22 5C 75 30 30 35 43 22 5D"/*["bu005C"]*/, null),
		input(314, "yx_string_utf16BE_no_BOM", "00 5B 00 22 00 E9 00 22 00 5D"/*[0][[0]"[0][fffd][0]"[0]]*/, null),
		input(315, "yx_string_utf16LE_no_BOM", "5B 00 22 00 E9 00 22 00 5D 00"/*[[0]"[0][fffd][0]"[0]][0]*/, null),
		input(316, "yx_string_utf8", "5B 22 E2 82 AC F0 9D 84 9E 22 5D"/*["[20ac][d834][dd1e]"]*/, null),
		input(317, "yx_string_with_del_character", "5B 22 61 7F 61 22 5D"/*["a[7f]a"]*/, null),
		input(318, "yx_structure_trailing_newline", "5B 22 61 22 5D 0A"/*["a"][a]*/, null),
	};

	static Input[] inputs() {
		return INPUTS;
	}

	private static Input input(Integer testNum, String name, String jsonInput, String errorText) {
		return new Input(testNum, name, jsonInput, errorText);
	}

	public static class Input {
		public final Integer testNum;
		public final String name;
		public final String jsonInput;
		public final String errorText;
		public final Object json;
		public final String jsonReadable;
		public final char expected;

		public Input(Integer testNum, String name, String jsonInput, String errorText) {
			this.testNum = testNum;
			this.name = name;
			this.jsonInput = jsonInput;
			this.errorText = errorText;
			this.json = name.charAt(1) == 'x' ? fromSpacedHex(jsonInput) : jsonInput;
			this.jsonReadable = name.charAt(1) == 'x' ? fromSpacedHexToUTF8(jsonInput) : jsonInput;
			this.expected = name.charAt(0);
		}
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void a01_testStrict(Input input) throws Exception {
		var p = JsonParser.DEFAULT_STRICT;
		if (input.name.contains("utf16LE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16LE")).build();
		else if (input.name.contains("utf16BE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16BE")).build();

		// 'y' tests should always succeed.
		if (input.expected == 'y') {
			p.parse(input.json, Object.class);

		// 'n' tests should always fail.
		} else if (input.expected == 'n') {
			try {
				p.parse(input.json, Object.class);
				fail("ParseException expected.  Test="+input.name+", Input=" + input.jsonReadable);
			} catch (ParseException e) {
				if (input.errorText != null)
					assertTrue(e.getRootCause().getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getRootCause().getMessage()));
			} catch (IOException e) {
				if (input.errorText != null)
					assertTrue(e.getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getMessage()));
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+input.name+", Input=" + input.jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}

		// 'i' tests may or may not fail, but should through a ParseException and not kill the JVM.
		} else if (input.expected == 'i') {
			try {
				p.parse(input.json, Object.class);
			} catch (ParseException e) {
				if (input.errorText != null)
					assertTrue(e.getRootCause().getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getRootCause().getMessage()));
			} catch (IOException e) {
				if (input.errorText != null)
					assertTrue(e.getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getMessage()));
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+input.name+", Input=" + input.jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}
		}
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void a02_testLax(Input input) throws Exception {
		var p = JsonParser.DEFAULT;
		if (input.name.contains("utf16LE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16LE")).build();
		else if (input.name.contains("utf16BE"))
			p = p.copy().streamCharset(Charset.forName("UTF-16BE")).build();

		// 'y' tests should always succeed.
		if (input.expected == 'y') {
			p.parse(input.json, Object.class);

		// 'n' tests may or may not fail for lax parser.
		} else if (input.expected == 'n') {
			try {
				p.parse(input.json, Object.class);
			} catch (ParseException e) {
				if (input.errorText != null)
					assertTrue(e.getRootCause().getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getRootCause().getMessage()));
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+input.name+", Input=" + input.jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}

		// 'i' tests may or may not fail, but should through a ParseException and not kill the JVM.
		} else if (input.expected == 'i') {
			try {
				p.parse(input.json, Object.class);
			} catch (ParseException e) {
				if (input.errorText != null)
					assertTrue(e.getRootCause().getMessage().contains(input.errorText), fms("Got ParseException but didn't contain expected text ''{0}''.  Test={1}, Input={2}, Message={3}", input.errorText, input.name, input.jsonReadable, e.getRootCause().getMessage()));
			} catch (Throwable t) {
				fail("Expected ParseException.  Test="+input.name+", Input=" + input.jsonReadable + ", Exception=" + t.getClass().getName() + "," +t.getLocalizedMessage());
			}
		}
	}
}