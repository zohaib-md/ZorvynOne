package com.project.zorvynone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.zorvynone.model.IconType
import com.project.zorvynone.model.Transaction
import com.project.zorvynone.model.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class HomeViewModel(private val dao: TransactionDao) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Int> = dao.getTotalIncome().map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalExpenses: StateFlow<Int> = dao.getTotalExpense().map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBalance: StateFlow<Int> = dao.getAllTransactions().map { list ->
        list.sumOf { if (it.isIncome) it.amount else -it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addTransaction(amount: Int, isIncome: Boolean, category: String, dateMillis: Long, note: String?) {
        val iconType = when(category) {
            "Food" -> IconType.FOOD
            "Café" -> IconType.CAFE
            "Shopping" -> IconType.SHOPPING
            "Housing" -> IconType.HOUSING
            "Transport" -> IconType.TRANSPORT
            "Salary" -> IconType.SALARY
            else -> IconType.DEFAULT
        }

        val dateString = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dateMillis))
        val finalTitle = if (!note.isNullOrBlank()) note else category
        val finalSubtitle = if (isIncome) "Income • $dateString" else "$category • $dateString"

        val newTxn = Transaction(
            title = finalTitle,
            subtitle = finalSubtitle,
            amount = amount,
            isIncome = isIncome,
            category = category,
            date = dateMillis,
            note = note,
            iconType = iconType
        )

        viewModelScope.launch {
            dao.insertTransaction(newTxn)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }

    // ==========================================
    //      MAGIC RECEIPT SCANNER INTEGRATION
    // ==========================================

    private val _isScannerLoading = MutableStateFlow(false)
    val isScannerLoading: StateFlow<Boolean> = _isScannerLoading.asStateFlow()

    fun processReceiptText(rawReceiptText: String, apiKey: String) {
        if (_isScannerLoading.value) return

        viewModelScope.launch {
            _isScannerLoading.value = true
            try {
                // 1. Upgraded Prompt: Forces rounded integers and strict JSON formatting
                val prompt = """
                    You are a financial AI data extractor. I have scanned a receipt using OCR. The raw text is below:
                    
                    "$rawReceiptText"
                    
                    Extract the primary transaction details and return ONLY a valid JSON object. DO NOT include markdown, backticks, or any conversational text.
                    
                    Rules:
                    1. amount: Integer (Find the 'Total'. If it has decimals like 45.99, round it to 46. Return ONLY the number).
                    2. title: String (Identify the merchant name).
                    3. isIncome: Boolean (Always false).
                    4. category: String (Choose exactly one: "Food", "Transport", "Shopping", "Housing", "Miscellaneous").
                    5. iconType: String (Choose exactly one: "FOOD", "TRANSPORT", "SHOPPING", "HOUSING", "DEFAULT").
                    
                    Example Output:
                    {
                      "title": "Starbucks Coffee",
                      "amount": 350,
                      "isIncome": false,
                      "category": "Food",
                      "iconType": "FOOD"
                    }
                """.trimIndent()

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val requestBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                    }.toString()

                    connection.outputStream.use { it.write(requestBody.toByteArray()) }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                        throw Exception("API Error $responseCode: $errorText")
                    }
                }

                val responseJson = JSONObject(result)
                var text = responseJson.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text")

                // 2. Safely extract ONLY the JSON block (ignores any conversational text Gemini might add)
                val startIndex = text.indexOf('{')
                val endIndex = text.lastIndexOf('}')
                if (startIndex != -1 && endIndex != -1) {
                    text = text.substring(startIndex, endIndex + 1)
                }

                val txnJson = JSONObject(text)

                val categoryStr = txnJson.getString("category")
                val dateMillis = System.currentTimeMillis()
                val dateString = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dateMillis))
                val finalSubtitle = "AI Scanned • $dateString"

                // 3. Safe Double-to-Int conversion (Prevents crash if Gemini returns 450.50)
                val safeAmount = txnJson.optDouble("amount", 0.0).toInt()

                val newTxn = Transaction(
                    title = txnJson.getString("title"),
                    subtitle = finalSubtitle,
                    amount = safeAmount,
                    isIncome = txnJson.optBoolean("isIncome", false),
                    category = categoryStr,
                    date = dateMillis,
                    note = "Scanned Receipt via ML Kit",
                    iconType = IconType.valueOf(txnJson.getString("iconType"))
                )

                dao.insertTransaction(newTxn)

            } catch (e: Exception) {
                // If it fails, print the exact error to Android Studio Logcat so we can see it!
                println("ZORVYN_SCANNER_ERROR: ${e.message}")
                e.printStackTrace()
            } finally {
                _isScannerLoading.value = false
            }
        }
    }

    // ==========================================
    //       VOICE AI ASSISTANT INTEGRATION
    // ==========================================

    private val _isVoiceLoading = MutableStateFlow(false)
    val isVoiceLoading: StateFlow<Boolean> = _isVoiceLoading.asStateFlow()

    fun processVoiceTransaction(spokenText: String, apiKey: String) {
        if (_isVoiceLoading.value) return

        viewModelScope.launch {
            _isVoiceLoading.value = true
            try {
                val prompt = """
                    You are a financial AI assistant. The user just said: "$spokenText".
                    Extract the transaction details and return ONLY a valid JSON object. Do not include markdown formatting, backticks, or the word "json".
                    
                    Rules:
                    1. amount: Integer (extract the exact number spoken).
                    2. title: String (e.g., "Uber Ride", "Starbucks", "Groceries").
                    3. isIncome: Boolean (true if receiving money/salary, false if spending).
                    4. category: String (Choose the best fit from: "Food", "Transport", "Shopping", "Housing", "Salary", "Miscellaneous").
                    5. iconType: String (Choose the exact match: "FOOD", "TRANSPORT", "SHOPPING", "HOUSING", "SALARY", "DEFAULT").
                    
                    Example Output:
                    {
                      "title": "Uber Ride",
                      "amount": 450,
                      "isIncome": false,
                      "category": "Transport",
                      "iconType": "TRANSPORT"
                    }
                """.trimIndent()

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val requestBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                    }.toString()

                    connection.outputStream.use { it.write(requestBody.toByteArray()) }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                        throw Exception("API Error $responseCode: $errorText")
                    }
                }

                val responseJson = JSONObject(result)
                var text = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                text = text.replace("```json", "").replace("```", "").trim()

                val txnJson = JSONObject(text)

                val categoryStr = txnJson.getString("category")
                val isIncomeVal = txnJson.getBoolean("isIncome")
                val dateMillis = System.currentTimeMillis()
                val dateString = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dateMillis))
                val finalSubtitle = if (isIncomeVal) "Voice Income • $dateString" else "$categoryStr • $dateString"

                val newTxn = Transaction(
                    title = txnJson.getString("title"),
                    subtitle = finalSubtitle,
                    amount = txnJson.getInt("amount"),
                    isIncome = isIncomeVal,
                    category = categoryStr,
                    date = dateMillis,
                    note = "Added via Voice AI",
                    iconType = IconType.valueOf(txnJson.getString("iconType"))
                )

                dao.insertTransaction(newTxn)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isVoiceLoading.value = false
            }
        }
    }


    // ==========================================
    //          HOME SCREEN AI INTEGRATION
    // ==========================================

    private val _aiInsights = MutableStateFlow<List<String>>(emptyList())
    val aiInsights = _aiInsights.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    fun generateInsights(apiKey: String) {
        if (_isAiLoading.value) return

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val currentTxns = transactions.value
                if (currentTxns.isEmpty()) {
                    _aiInsights.value = listOf("Add some transactions so I can analyze your spending!")
                    return@launch
                }

                val spendingSummary = currentTxns.joinToString("\n") {
                    "${it.title} (${it.category}): ₹${it.amount} [${if(it.isIncome) "Income" else "Expense"}]"
                }

                val prompt = """You are an expert financial advisor AI built into an app. 
Analyze the following user transaction data:

$spendingSummary

Provide exactly 3 short, actionable financial insights based on this data.
Make them punchy and specific. Maximum 1 sentence per insight.
Format your response as a simple list where each insight starts with a dash (-)."""

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val requestBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                    }.toString()

                    connection.outputStream.use { it.write(requestBody.toByteArray()) }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                        throw Exception("API Error $responseCode: $errorText")
                    }
                }

                val json = JSONObject(result)
                val text = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedInsights = text.split("\n")
                    .filter { it.isNotBlank() && it.trim().startsWith("-") }
                    .map { it.trim().removePrefix("-").trim() }
                    .ifEmpty { listOf("Unable to generate insights right now.") }

                _aiInsights.value = parsedInsights

            } catch (e: Exception) {
                _aiInsights.value = listOf("Error: ${e.message ?: e.toString()}")
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // ==========================================
    //       SCORE SCREEN AI INTEGRATION
    // ==========================================

    data class AiHabitData(
        val category: String, val title: String, val description: String,
        val points: Int, val statAmount: String, val statLabel: String,
        val acceptText: String, val rejectText: String
    )

    val defaultScoreHabits: StateFlow<List<AiHabitData>> = transactions.map { txns ->
        val expenses = txns.filter { !it.isIncome }
        if (expenses.isEmpty()) return@map emptyList()

        val topCategories = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(3)

        val formatter = java.text.NumberFormat.getNumberInstance(Locale("en", "IN"))

        topCategories.mapIndexed { index, entry ->
            val points = 6 - index
            val formattedAmount = formatter.format(entry.value)

            AiHabitData(
                category = entry.key.uppercase(Locale.getDefault()),
                title = "Reduce ${entry.key} spending",
                description = "You spent ₹$formattedAmount on ${entry.key} this month. Try setting a strict budget.",
                points = points,
                statAmount = "₹$formattedAmount",
                statLabel = "on ${entry.key.lowercase(Locale.getDefault())}",
                acceptText = "Set Limit",
                rejectText = "Skip"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _scoreHabits = MutableStateFlow<List<AiHabitData>>(emptyList())
    val scoreHabits = _scoreHabits.asStateFlow()

    private val _isScoreLoading = MutableStateFlow(false)
    val isScoreLoading = _isScoreLoading.asStateFlow()

    fun generateScoreHabits(apiKey: String) {
        if (_isScoreLoading.value) return

        viewModelScope.launch {
            _isScoreLoading.value = true
            try {
                val currentTxns = transactions.value
                val spendingSummary = currentTxns.joinToString("\n") {
                    "${it.title} (${it.category}): ₹${it.amount} [${if(it.isIncome) "Income" else "Expense"}]"
                }

                val prompt = """
                    You are a financial AI. Analyze this user's spending data:
                    $spendingSummary

                    Provide exactly 3 actionable habits to improve their credit score and save money.
                    Format your response EXACTLY like this, with each habit on a new line separated by the pipe (|) symbol. Do not add markdown, bullet points, or extra text.
                    Category|Title|Description|Points (number 1-10)|Stat Amount|Stat Label|Accept Text|Reject Text

                    Example:
                    FOOD & DRINKS|Cut café visits by 2x a week|You spent ₹3,200 at coffee shops this month.|4|₹3,200|on cafés|Cut to 2x/wk|Keep as is
                """.trimIndent()

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val requestBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                    }.toString()

                    connection.outputStream.use { it.write(requestBody.toByteArray()) }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                        throw Exception("API Error $responseCode: $errorText")
                    }
                }

                val json = JSONObject(result)
                val text = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedHabits = text.lines()
                    .filter { it.contains("|") }
                    .take(3)
                    .mapNotNull { line ->
                        val parts = line.split("|").map { it.trim() }
                        if (parts.size >= 8) {
                            AiHabitData(
                                category = parts[0], title = parts[1], description = parts[2],
                                points = parts[3].toIntOrNull() ?: 5, statAmount = parts[4],
                                statLabel = parts[5], acceptText = parts[6], rejectText = parts[7]
                            )
                        } else null
                    }

                if (parsedHabits.isNotEmpty()) {
                    _scoreHabits.value = parsedHabits
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScoreLoading.value = false
            }
        }
    }

    // ==========================================
    //       PERSISTENT PLAN STATE
    // ==========================================
    private val _savedBonus = MutableStateFlow(0)

    val baseScore: StateFlow<Int> = kotlinx.coroutines.flow.combine(totalIncome, totalExpenses) { income, expense ->
        if (income == 0) {
            if (expense == 0) 50 else 45
        } else {
            val savings = income - expense
            val ratio = savings.toFloat() / income.toFloat()
            (50 + (ratio * 100)).toInt()
        }
    }.map { it.coerceIn(30, 99) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    val currentScore: StateFlow<Int> = kotlinx.coroutines.flow.combine(baseScore, _savedBonus) { base, bonus ->
        (base + bonus).coerceAtMost(100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    private val _acceptedHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val acceptedHabitIds = _acceptedHabitIds.asStateFlow()

    fun toggleHabit(id: String) {
        val current = _acceptedHabitIds.value
        _acceptedHabitIds.value = if (current.contains(id)) current - id else current + id
    }

    fun savePlan(bonus: Int) {
        _savedBonus.value = bonus
    }
}