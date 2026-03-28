package io.github.mobdev

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityMainBinding
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var expression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.button0.setOnClickListener { append("0") }
        binding.button1.setOnClickListener { append("1") }
        binding.button2.setOnClickListener { append("2") }
        binding.button3.setOnClickListener { append("3") }
        binding.button4.setOnClickListener { append("4") }
        binding.button5.setOnClickListener { append("5") }
        binding.button6.setOnClickListener { append("6") }
        binding.button7.setOnClickListener { append("7") }
        binding.button8.setOnClickListener { append("8") }
        binding.button9.setOnClickListener { append("9") }
        binding.buttonDot.setOnClickListener { append(".") }
        binding.buttonPlus.setOnClickListener { append("+") }
        binding.buttonMinus.setOnClickListener { append("-") }
        binding.buttonMultiply.setOnClickListener { append("*") }
        binding.buttonDivide.setOnClickListener { append("/") }
        binding.buttonAllClear.setOnClickListener {
            expression = ""
            binding.resultTv.text = "0"
            binding.solutionTv.text = ""
        }
        binding.buttonBackspace.setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                binding.resultTv.text = expression.ifEmpty { "0" }
            }
        }
        binding.buttonCloseBracket.setOnClickListener { append(")") }

        binding.buttonEquals.setOnClickListener {
            try {
                val result = ExpressionBuilder(expression).build().evaluate()
                binding.solutionTv.text = expression
                expression = result.toString()
                if (expression.endsWith(".0")) {
                    expression = expression.removeSuffix(".0")
                }
                binding.resultTv.text = expression
            } catch (e: Exception) {
                binding.resultTv.text = "Error"
                expression = ""
            }
        }
    }

    private fun append(value: String) {
        expression += value
        binding.resultTv.text = expression
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("expression", expression)
        outState.putString("solution", binding.solutionTv.text.toString())
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        expression = savedInstanceState.getString("expression", "")
        binding.resultTv.text = expression.ifEmpty { "0" }
        binding.solutionTv.text = savedInstanceState.getString("solution", "")
    }
}