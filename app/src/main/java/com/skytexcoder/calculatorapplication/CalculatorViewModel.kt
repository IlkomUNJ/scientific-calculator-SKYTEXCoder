package com.skytexcoder.calculatorapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.math.BigDecimal

class CalculatorViewModel : ViewModel() {

    private val _equationText = MutableLiveData("")
    val equationText : LiveData<String> = _equationText

    private val _resultText = MutableLiveData("0")
    val resultText: LiveData<String> = _resultText

    private val pressedButtonsHistory = mutableListOf<String>()

    private var lastButtonPressed: String? = null

    private val numberButtons = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

    private val operators = setOf("+", "-", "*", "/")

    private val wasLastButtonANumber: Boolean
        get() = lastButtonPressed in numberButtons

    fun onCalculatorButtonClick(btn: String?) {
        pressedButtonsHistory.add(btn!!)
        Log.d("ButtonHistory", "History: $pressedButtonsHistory")
        Log.i("Clicked Button", btn!!)

        val currentEquation = _equationText.value ?: "0"
        val currentResultText = _resultText.value ?: "0"

        _equationText.value?.let {
            when (btn) {
                "AC" -> {
                    _equationText.value = "0"
                    _resultText.value = "0"
                    return
                }
                "BACKSPACE" -> {
                    if (currentEquation.isNotEmpty()) {
                        if (currentEquation.length == 1) {
                            _equationText.value = "0"
                            _resultText.value = "0"
                        } else {
                            _equationText.value = currentEquation.dropLast(1)
                        }
                        try {
                            _resultText.value = calculateResult(_equationText.value.toString())
                        } catch (_ : Exception) {

                        }
                        Log.i("Current Equation Text: ", _equationText.value.toString())
                        Log.i("Current Result Text: ", _resultText.value.toString())
                    }
                    return
                }
                "=" -> {
                    try {
                        _resultText.value = calculateResult(_equationText.value.toString())
                    } catch (_ : Exception) {

                    }
                    _equationText.value = _resultText.value
                    return
                }
                "." -> {
                    val lastCharacter = currentEquation.lastOrNull()?.toString()

                    if (lastCharacter in operators) {
                        _equationText.value += "0."
                        return
                    }

                    val lastOperatorIndex = currentEquation.findLast { it.toString() in operators }?.let {
                        currentEquation.lastIndexOf(it)
                    } ?: -1

                    val currentNumber = currentEquation.substring(lastOperatorIndex + 1)

                    if (!currentNumber.contains(".")) {
                        _equationText.value += "."
                        // return
                    }
                    try {
                        _resultText.value = calculateResult(_equationText.value.toString())
                    } catch (_ : Exception) {

                    }
                    return
                }
            }

            val characterToAppend = when (btn) {
                "×" -> "*"
                "÷" -> "/"
                else -> btn
            }

            /* if (currentEquation == "0") {
                if (characterToAppend !in operators) {
                    _equationText.value = characterToAppend
                } else {
                    _equationText.value += characterToAppend
                }
                Log.i("Current Equation Text: ", _equationText.value.toString())
                Log.i("Current Result Text: ", _resultText.value.toString())
                return
            } */

            if (currentEquation == "0" && lastButtonPressed != "0" && characterToAppend == "-") {
                _equationText.value = "-"
            } else {
                val lastCharacterDetected = currentEquation.lastOrNull()?.toString()
                val isLastCharacterOperator = lastCharacterDetected in operators
                val isCurrentButtonOperator = characterToAppend in operators
                if (isLastCharacterOperator && isCurrentButtonOperator) {
                    _equationText.value = currentEquation.dropLast(1) + characterToAppend
                } else if (currentEquation == "0" && !isCurrentButtonOperator) {
                    _equationText.value = characterToAppend
                } else {
                    _equationText.value += characterToAppend
                }
            }

            Log.i("[PRINT] EQT Before C.: ", _equationText.value.toString())

            try {
                _resultText.value = calculateResult(_equationText.value.toString())
            } catch (_ : Exception) {

            }

            Log.i("[PRINT] EQT After Calc.", _equationText.value.toString())
            Log.i("[PRINT] Result Text: ", _resultText.value.toString())

            lastButtonPressed = btn
        }
    }

    fun calculateResult(equationText: String): String {
        val sanitizedEquationText = StringBuilder()
        for (i in equationText.indices) {
            val currentCharacter = equationText[i]
            if (i > 0) {
                val previousCharacter = equationText[i - 1]
                if (previousCharacter.isDigit() && currentCharacter == '(') {
                    sanitizedEquationText.append('*')
                } else if (previousCharacter == ')' && currentCharacter.isDigit()) {
                    sanitizedEquationText.append('*')
                } else if (previousCharacter == ')' && currentCharacter == '(') {
                    sanitizedEquationText.append('*')
                }
            }
            sanitizedEquationText.append(currentCharacter)
        }
        val finalEquationTextToBeEvaluated = sanitizedEquationText.toString()
        Log.i("[PRINT] SNTZD Equation", "Original: '$equationText', Processed: '$finalEquationTextToBeEvaluated'")
        val context: Context = Context.enter()
        context.optimizationLevel = -1
        val scriptable: Scriptable = context.initStandardObjects()
        val resultFromRhino = context.evaluateString(scriptable, finalEquationTextToBeEvaluated, "Javascript", 1, null)
        // Log.i("[PRINT] RawRhinoOutput", resultFromRhino)
        val rawResultString = resultFromRhino.toString()
        Log.i("[PRINT] RawRhinoResult", rawResultString)
        try {
            val resultAsBigDecimal = BigDecimal(rawResultString)
            var plainStringResult = resultAsBigDecimal.toPlainString()
            Log.i("[PRINT] BgDecimalResult", plainStringResult)
            if (plainStringResult.endsWith(".0")) {
                plainStringResult = plainStringResult.removeSuffix(".0")
            }
            return plainStringResult
        } catch (e: NumberFormatException) {
            Log.i("[PRINT] Exception: ", e.toString())
            return rawResultString
        }
    }
}