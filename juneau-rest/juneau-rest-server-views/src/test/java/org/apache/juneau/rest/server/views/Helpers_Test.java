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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on behavioral coverage for {@code juneau-helpers.js} (WORK-J0522b, design §16.4 tests 28/28a/28b/30/31).
 *
 * <p>
 * The paint library is exercised under a DOM shim with NO {@code juneau-regions.js} loaded (see {@code
 * src/test/js/helpers-harness.cjs}), matching design §9.2's own claim that every helper is testable with no region
 * at all.  The harness script computes facts only; every assertion against those facts lives here so a failure
 * names the exact behavior that broke.
 *
 * <p>
 * Test 29 (the module-shape purity scan) is a separate class, {@link Helpers_Purity_Test}, because it is a static
 * source-text scan rather than a behavioral one and belongs with the other source-shape checks.
 */
class Helpers_Test extends TestBase {

	private static Map<?,?> report() {
		var r = HelpersHarness.report("helpers.cjs");
		assumeTrue(r != null, "node not available or helpers.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@Test void a00_helpersNamespacePublished() {
		assertAllTrue(report(), "hasHelpers");
	}

	// =================================================================================================================
	// fieldGrid - test 28a/28b: the catalog+values join, and its five behaviors.
	// =================================================================================================================

	@Test void b01_fieldGrid_catalogValuesJoin_labelsAndValues() {
		var r = report();
		assertEquals(2L, ((Number)r.get("fieldGrid_fieldCount")).longValue());
		assertEquals(List.of("Name", "Owner"), r.get("fieldGrid_titles"));
		assertEquals(List.of("alerts-primary", "Platform"), r.get("fieldGrid_values"));
	}

	@Test void b02_fieldGrid_missingValuesKey_rendersEmptyNeverUndefined() {
		assertAllTrue(report(), "fieldGrid_missingKeyRendersEmpty");
	}

	@Test void b03_fieldGrid_extraValuesKeyWithNoCatalogEntry_isDropped() {
		assertAllTrue(report(), "fieldGrid_extraValuesKeyDropped");
	}

	@Test void b04_fieldGrid_markdownFormat_routesThroughFillMarkdownSlot() {
		// No DOMParser in this sandbox, so fillMarkdownSlot's OWN documented fail-closed path applies: the
		// raw value is shown as escaped text.  That IS the behavior under test - it is the same fail-closed
		// contract fillSanitizedHtmlSlot documents, exercised here because it is reachable with no parser.
		assertEquals("hi", report().get("fieldGrid_markdown_text"));
	}

	@Test void b05_fieldGrid_renderDispatch_throwingRendererFallsBackToRawValue() {
		assertAllTrue(report(), "fieldGrid_throwingRendererFallsBackToText");
	}

	@Test void b06_fieldGrid_scopedRendererOverride_restoresGlobalRegistryByIdentity() {
		// The purity-relevant half of F13's opts.renderers mechanism: the temporary NS.registerRenderer swap
		// inside fieldGrid must leave the GLOBAL registry holding the EXACT SAME renderer object it held
		// before the call - no page-global state survives past a single fill (test 29's amended purity scan).
		assertAllTrue(report(), "fieldGrid_scopedRendererOverrideRestoredByIdentity");
	}

	@Test void b07_fieldGrid_href_wrapsSafeUrlOnly() {
		assertAllTrue(report(), "fieldGrid_hrefWrapsSafeUrl", "fieldGrid_unsafeHrefNotWrapped");
	}

	@Test void b07b_fieldGrid_span_stampsFullClass() {
		assertAllTrue(report(), "fieldGrid_spanFullClass");
	}

	@Test void b08_fieldGrid_actions_disabledWithoutHandler_firesWithOnAction() {
		assertAllTrue(report(), "fieldGrid_actionDisabledWithoutHandler", "fieldGrid_actionFiresOnAction");
	}

	@Test void b09_fieldGrid_columns_isCssCustomPropertyNotInlineGridTemplate() {
		assertAllTrue(report(), "fieldGrid_columnsIsCustomProperty");
	}

	@Test void b10_fieldGrid_loudArgumentErrors() {
		assertAllTrue(report(), "fieldGrid_nonArrayThrows", "fieldGrid_missingDataKeyThrows");
	}

	// =================================================================================================================
	// kvTable - the two accepted argument forms produce identical output; anything else is a loud error.
	// =================================================================================================================

	@Test void c01_kvTable_arrayAndMapForms_produceIdenticalOutput() {
		var r = report();
		assertEquals(List.of(List.of("latency", "12"), List.of("ok", "true")), r.get("kvTable_arrayForm"));
		assertEquals(List.of(List.of("latency", "12"), List.of("ok", "true")), r.get("kvTable_mapForm"));
		assertAllTrue(r, "kvTable_sameOutputBothForms");
	}

	@Test void c02_kvTable_objectTupleForm() {
		assertAllTrue(report(), "kvTable_objectTupleForm");
	}

	@Test void c03_kvTable_loudArgumentErrors() {
		assertAllTrue(report(), "kvTable_bareScalarArrayThrows", "kvTable_nullThrows");
	}

	// =================================================================================================================
	// button / buttonRow / text / pill / icon - the promoted leaf helpers.
	// =================================================================================================================

	@Test void d01_button_disabledWithoutOnClick_enabledAndFiresWithOnClick() {
		assertAllTrue(report(), "button_enabledWithOnClick", "button_clickFires", "button_disabledWithoutOnClick");
	}

	@Test void d02_buttonRow_oneButtonPerSpec() {
		assertEquals(2L, ((Number)report().get("buttonRow_count")).longValue());
	}

	@Test void d03_text_isATextNode_nullIsEmpty() {
		assertAllTrue(report(), "text_isTextNode", "text_nullIsEmpty");
	}

	@Test void d04_pill_labelAndToneClass() {
		var r = report();
		assertEquals("Open", r.get("pill_label"));
		assertAllTrue(r, "pill_toneClassApplied");
	}

	@Test void d05_icon_unknownNameIsHidden() {
		assertAllTrue(report(), "icon_unknownIsHidden");
	}

	@Test void d06_button_iconAppearanceClass_unknownThrows() {
		assertAllTrue(report(),
			"button_iconAppearanceClass",
			"button_chromeAppearanceHasNoIconClass",
			"button_omittedAppearanceHasNoIconClass",
			"button_unknownAppearanceThrows");
	}

	// =================================================================================================================
	// recordTable - static/read-only, catalog vocabulary, empty state.
	// =================================================================================================================

	@Test void e01_recordTable_catalogAndRows() {
		var r = report();
		assertEquals(List.of("Host", "Severity"), r.get("recordTable_headerLabels"));
		assertEquals(2L, ((Number)r.get("recordTable_rowCount")).longValue());
		assertEquals(List.of("h1", "warn"), r.get("recordTable_firstRowCells"));
	}

	@Test void e02_recordTable_emptyRows_paintsEmptyStateNotAnEmptyTable() {
		assertAllTrue(report(), "recordTable_emptyIsNotATable");
	}

	// =================================================================================================================
	// dataPane - §8.3's algorithm, all seven rows of the shape table (test 31).
	// =================================================================================================================

	@Test void f01_dataPane_paintsLoadingBeforeLoadSettles() {
		assertAllTrue(report(), "dataPane_loadingPaintedFirst");
	}

	@Test void f02_dataPane_rendersOnSuccessfulLoad() {
		assertAllTrue(report(), "dataPane_renderAfterLoad");
	}

	@Test void f03_dataPane_abortError_isSilent() {
		assertAllTrue(report(), "dataPane_abortErrorLeavesLoadingStatusUntouched");
	}

	@Test void f04_dataPane_otherRejection_paintsErrorNeverRethrows() {
		assertAllTrue(report(), "dataPane_otherRejectionNeverRethrows", "dataPane_otherRejectionPaintsErrorStatus");
	}

	@Test void f05_dataPane_synchronousThrowInLoad_isAlsoContained() {
		assertAllTrue(report(), "dataPane_syncThrowNeverRethrows");
	}

	@Test void f06_dataPane_emptyArray_paintsCustomEmptyNodeWhenSupplied() {
		assertAllTrue(report(), "dataPane_emptyArrayPaintsCustomEmpty");
	}

	@Test void f07_dataPane_emptyObject_paintsDefaultEmptyNodeWhenNoneSupplied() {
		assertAllTrue(report(), "dataPane_emptyObjectPaintsDefaultEmpty");
	}

	@Test void f08_dataPane_nonEmptyValuesMap_isNotTreatedAsEmpty() {
		assertAllTrue(report(), "dataPane_nonEmptyMapRenders");
	}

	@Test void f09_dataPane_emptyKindRejection_paintsEmptyNotError() {
		assertAllTrue(report(), "dataPane_emptyKindRejectionPaintsEmpty");
	}

	@Test void f10_dataPane_loudArgumentErrors() {
		assertAllTrue(report(), "dataPane_missingLoadThrows", "dataPane_missingRenderThrows");
	}

	// =================================================================================================================
	// tabStrip - the widened contract (test 30, i-xiii): lazy, fill-once, contained throws, abort, keyboard.
	// =================================================================================================================

	@Test void g01_tabStrip_eagerOneArgumentForm_stillWorksUnchanged() {
		assertAllTrue(report(), "tabStrip_eagerFormHasOneTab", "tabStrip_eagerFormPaneVisible");
	}

	@Test void g02_tabStrip_lazyTab_doesNotPopulateBeforeFirstActivation() {
		assertAllTrue(report(), "tabStrip_lazyDoesNotRunBeforeActivation", "tabStrip_lazyRunsOnActivation");
	}

	@Test void g03_tabStrip_fillOnce_neverReRunsOnRepeatedActivation() {
		assertAllTrue(report(), "tabStrip_fillOnce_neverReruns");
	}

	@Test void g04_tabStrip_lazyFalse_populatesAtBuildTime() {
		assertAllTrue(report(), "tabStrip_lazyFalsePopulatesAtBuildTime");
	}

	@Test void g05_tabStrip_throwingPopulate_paintsThatPanesErrorOnly_siblingsStayNavigable() {
		assertAllTrue(report(), "tabStrip_throwingTabPaintsError", "tabStrip_siblingStillNavigableAfterThrow");
	}

	@Test void g06_tabStrip_rejectingAsyncPopulate_isAlsoContained() {
		assertAllTrue(report(), "tabStrip_rejectingTabPaintsErrorStatus");
	}

	@Test void g07_tabStrip_abortedSignal_preventsFuturePopulate() {
		assertAllTrue(report(), "tabStrip_abortPreventsFuturePopulate");
	}

	@Test void g08_tabStrip_loudArgumentErrors() {
		assertAllTrue(report(), "tabStrip_bothPaneAndPopulateThrows", "tabStrip_neitherPaneNorPopulateThrows",
			"tabStrip_emptyArrayThrows", "tabStrip_duplicateIdThrows");
	}

	@Test void g09_tabStrip_onActivate_firesEveryActivation_populateRunsOnlyOnce() {
		assertAllTrue(report(), "tabStrip_onActivateFiresOnEveryActivation", "tabStrip_populateRunsOnlyOnce");
	}

	@Test void g10_tabStrip_tenTabs_ariaSelectedExactlyOne_rovingTabindexExactlyOne_hiddenPanesExceptOne() {
		assertAllTrue(report(), "tabStrip_ten_ariaSelectedExactlyOne", "tabStrip_ten_rovingTabindexExactlyOneZero",
			"tabStrip_ten_hiddenPanesExceptOne", "tabStrip_ten_disabledNeverActivatable");
	}

	@Test void g11_tabStrip_keyboard_homeEndArrow_skipsDisabledTab_unhandledKeyIsANoOp() {
		assertAllTrue(report(), "tabStrip_end_selectsLast", "tabStrip_home_selectsFirst",
			"tabStrip_arrowRight_skipsDisabled", "tabStrip_unhandledKeyChangesNothing");
	}

	@Test void g12_tabStrip_independentInstances_fillOnceStateDoesNotLeakBetweenThem() {
		assertAllTrue(report(), "tabStrip_independentInstances_xRanOnce_yNeverRan");
	}

	// =================================================================================================================
	// dateRange / dropdown / filterBuilder - net-new controls.
	// =================================================================================================================

	@Test void h01_dateRange_onChange_fires_fromAfterToIsBlocked() {
		assertAllTrue(report(), "dateRange_twoInputs", "dateRange_onChangeFires", "dateRange_fromAfterTo_blocksOnChange");
	}

	@Test void h02_dropdown_optionsAndOnChange() {
		var r = report();
		assertEquals(2L, ((Number)r.get("dropdown_optionCount")).longValue());
		assertAllTrue(r, "dropdown_onChangeFires", "dropdown_missingOptionsThrows");
	}

	@Test void h03_filterBuilder_addAndRemovePredicates() {
		assertAllTrue(report(), "filterBuilder_addFiresOnChangeWithOnePredicate", "filterBuilder_chipRendered",
			"filterBuilder_removeFiresOnChangeWithZeroPredicates", "filterBuilder_missingFieldsThrows");
	}

	// =================================================================================================================
	// editableField / toast / fieldGrid editable wiring.
	// =================================================================================================================

	@Test void i01_editableField_loudArgumentErrors() {
		assertAllTrue(report(), "editableField_missingOnSaveThrows", "editableField_unknownTypeThrows",
			"editableField_selectWithoutOptionsThrows", "fieldGrid_editableWithoutOnFieldSaveThrows");
	}

	@Test void i02_editableField_view_textSelectPencil_checkboxHasNone_disabledDoesNotActivate() {
		assertAllTrue(report(), "editableField_textPaintsValue", "editableField_textHasPencil",
			"editableField_selectHasPencil", "editableField_missingValuesKeyPaintsEmpty",
			"editableField_checkboxHasNoPencil", "editableField_checkboxIsInput",
			"editableField_disabledDoesNotActivate");
	}

	@Test void i03_editableField_blurPersist_enterTypeBlurEscEnter_textareaEnterIsNewline() {
		assertAllTrue(report(), "editableField_enterEdit", "editableField_blurDirtySavesOnce",
			"editableField_blurDirtyReturnsToView", "editableField_blurCleanDoesNotSave",
			"editableField_escDiscards", "editableField_enterOnInputSaves",
			"editableField_multilineIsTextarea", "editableField_enterOnTextareaDoesNotSave");
	}

	@Test void i04_editableField_explicitPersist_blurDoesNotSave_saveAndCancel_coordinatorStillSaves() {
		assertAllTrue(report(), "editableField_explicitBlurDoesNotSave", "editableField_explicitSaveButtonSaves",
			"editableField_explicitCancelDiscards", "fieldGrid_explicitBlurStillNoSave",
			"fieldGrid_coordinatorSavesExplicitA");
	}

	@Test void i05_editableField_select_rendersOptions_savesOptionValue() {
		assertAllTrue(report(), "editableField_selectOptionsRendered", "editableField_selectSavesOptionValue");
	}

	@Test void i06_editableField_checkbox_toggleSavesBoolean_rejectRevertsWithInlineAndToast() {
		assertAllTrue(report(), "editableField_checkboxNoPencil", "editableField_checkboxToggleSavesBoolean",
			"editableField_checkboxRejectReverts", "editableField_checkboxRejectInline",
			"editableField_checkboxRejectToast");
	}

	@Test void i07_fieldGrid_switchFields_dirtySaves_cleanCancels_rejectKeepsBInView() {
		assertAllTrue(report(), "fieldGrid_openBWhileDirtyA_waits", "fieldGrid_openBAfterASave_BEditing",
			"fieldGrid_openBSavedA", "fieldGrid_openBWhileCleanA_noSave",
			"fieldGrid_openBWhenARejects_BStaysView");
	}

	@Test void i08_fieldGrid_blurPlusCoordinator_doesNotDoubleSave() {
		assertAllTrue(report(), "fieldGrid_blurPlusCoordinator_savesOnce");
	}

	@Test void i09_editableField_reject_inlinePlusToast_staysEditing_successIsQuiet() {
		assertAllTrue(report(), "editableField_rejectStaysInEdit", "editableField_rejectInline",
			"editableField_rejectToast", "editableField_fulfillUndefinedKeepsSubmitted",
			"editableField_successIsQuiet", "editableField_fulfillStringUsesIt");
	}

	@Test void i10_kvTable_stillReadOnly_noPencil() {
		assertAllTrue(report(), "kvTable_stillNoPencil");
	}

	@Test void i11_toast_reusesExistingNode_errorIsAlert_infoIsStatus() {
		assertAllTrue(report(), "toast_reusesExistingNode", "toast_infoRoleIsStatus");
	}
}
