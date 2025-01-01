/*
 * Copyright 2022-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.views.factories

import android.icu.number.NumberFormatter
import android.icu.text.DecimalFormat
import android.os.Build
import android.text.Editable
import android.text.InputType
import androidx.annotation.RequiresApi
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.extensions.getValidationErrorMessage
import com.google.android.fhir.datacapture.validation.MAX_VALUE_EXTENSION_URL
import com.google.android.fhir.datacapture.validation.MIN_VALUE_EXTENSION_URL
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.QuestionnaireResponse

internal object EditTextIntegerViewHolderFactory :
  EditTextViewHolderFactory(R.layout.edit_text_single_line_view) {
  override fun getQuestionnaireItemViewHolderDelegate() =
    object :
      QuestionnaireItemEditTextViewHolderDelegate(
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
      ) {

      private val minValue: Int by lazy {
        getExtensionValueOrDefault(MIN_VALUE_EXTENSION_URL, Int.MIN_VALUE)
      }

      private val maxValue: Int by lazy {
        getExtensionValueOrDefault(MAX_VALUE_EXTENSION_URL, Int.MAX_VALUE)
      }

      private fun getExtensionValueOrDefault(url: String, defaultValue: Int): Int {
        return questionnaireViewItem.questionnaireItem.extension
          .find { it.url == url }
          ?.let { (it.value as? IntegerType)?.value }
          ?: defaultValue
      }

      override suspend fun handleInput(
        editable: Editable,
        questionnaireViewItem: QuestionnaireViewItem,
      ) {
        val input = editable.toString()
        if (input.isEmpty()) {
          questionnaireViewItem.clearAnswer()
          return
        }

        val inputInteger = input.toIntOrNull()
        if (inputInteger != null) {
          questionnaireViewItem.setAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
              .setValue(IntegerType(input)),
          )
        } else {
          questionnaireViewItem.setDraftAnswer(input)
        }
      }

      override fun updateInputTextUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputEditText: TextInputEditText,
      ) {
        val answer =
          questionnaireViewItem.answers.singleOrNull()?.valueIntegerType?.value?.toString()
        val draftAnswer = questionnaireViewItem.draftAnswer?.toString()

        // Update the text on the UI only if the value of the saved answer or draft answer
        // is different from what the user is typing. We compare the two fields as integers to
        // avoid shifting focus if the text values are different, but their integer representation
        // is the same (e.g. "001" compared to "1")
        if (answer.isNullOrEmpty() && draftAnswer.isNullOrEmpty()) {
          textInputEditText.setText("")
        } else if (answer?.toIntOrNull() != textInputEditText.text.toString().toIntOrNull()) {
          textInputEditText.setText(answer)
        } else if (draftAnswer != null && draftAnswer != textInputEditText.text.toString()) {
          textInputEditText.setText(draftAnswer)
        }
      }

      override fun updateValidationTextUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputLayout: TextInputLayout,
      ) {
        textInputLayout.error =
          getValidationErrorMessage(
            textInputLayout.context,
            questionnaireViewItem,
            questionnaireViewItem.validationResult,
          )
        // Update error message if draft answer present
        if (questionnaireViewItem.draftAnswer != null) {
          textInputLayout.error =
            textInputLayout.context.getString(
              R.string.integer_format_validation_error_msg,
              formatInteger(minValue),
              formatInteger(maxValue),
            )
        }
      }
    }

  private fun formatInteger(value: Int): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      numberFormatter.format(value).toString()
    } else {
      decimalFormat.format(value)
    }
  }

  private val numberFormatter
    @RequiresApi(Build.VERSION_CODES.R) get() = NumberFormatter.withLocale(Locale.getDefault())

  private val decimalFormat
    get() = DecimalFormat.getInstance(Locale.getDefault())
}
