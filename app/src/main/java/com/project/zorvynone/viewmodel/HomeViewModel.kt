package com.project.zorvynone.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.zorvynone.BuildConfig
import com.project.zorvynone.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Wealth Intelligence Engine for ZorvynOne.
 * Manages the AI Achievement Navigator, Real-Time Scoring, and Transaction Processing.
 */
class HomeViewModel(private val dao: TransactionDao) : ViewModel() {

    // --- SECURE CONFIGURATION (from local.properties via BuildConfig) ---
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

    // --- CORE DATA FLOWS ---
    val transactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Int> = dao.getTotalIncome().map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalExpenses: StateFlow<Int> = dao.getTotalExpenses().map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBalance: StateFlow<Int> = combine(totalIncome, totalExpenses) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- AI ACHIEVEMENT NAVIGATOR ENGINE ---
    val userProfile: StateFlow<UserFinancialProfile?> = dao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _aiMissionBriefing = MutableStateFlow("Configure your architect to receive a mission briefing.")
    val aiMissionBriefing = _aiMissionBriefing.asStateFlow()

    /**
     * TRUE SURPLUS PROGRESS LOGIC
     */
    val realGoalCoverage: StateFlow<Float> = combine(totalBalance, userProfile) { balance, profile ->
        if (profile == null || profile.targetAmount <= 0.0) return@combine 0f
        val trueSurplus = (balance - profile.fixedExpenses).coerceAtLeast(0.0)
        (trueSurplus.toFloat() / profile.targetAmount.toFloat()).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun updateProfile(salary: Double, fixed: Double, goalName: String, target: Double, months: Int) {
        viewModelScope.launch {
            dao.insertProfile(UserFinancialProfile(0, salary, fixed, goalName, target, months))
        }
    }

    /**
     * AI MISSION BRIEFING (DYNAMIC PLAN)
     */
    fun generateAchievementPlan(ignoredKey: String = "") {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            _isScoreLoading.value = true
            try {
                val categoryHistory = transactions.value
                    .filter { !it.isIncome }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val prompt = """
                    Act as an Elite Private Wealth Manager. 
                    User is saving for: ${profile.goalName} (Cost: ₹${profile.targetAmount})
                    Timeline: ${profile.targetMonths} months.
                    Monthly Income: ₹${profile.monthlySalary}.
                    Committed Fixed Bills: ₹${profile.fixedExpenses}.
                    Real Category Spending: $categoryHistory.
                    
                    Provide a concise Mission Briefing (max 50 words). 
                    1. Evaluate if this timeline is realistic given the surplus.
                    2. Identify exactly ONE category from their habits they must cut.
                    3. State a Daily Budget that protects both the goal and the rent.
                """.trimIndent()

                val response = callGeminiApi(prompt, GEMINI_API_KEY)
                _aiMissionBriefing.value = response.trim()
            } catch (e: Exception) {
                _aiMissionBriefing.value = "Briefing service temporarily offline."
            } finally {
                _isScoreLoading.value = false
            }
        }
    }

    // --- AI SCORING & HABIT LOGIC ---
    data class AiHabitData(
        val category: String, val title: String, val description: String,
        val points: Int, val statAmount: String, val statLabel: String,
        val acceptText: String, val rejectText: String
    )

    private val _isScoreLoading = MutableStateFlow(false)
    val isScoreLoading = _isScoreLoading.asStateFlow()

    private val _scoreHabits = MutableStateFlow<List<AiHabitData>>(emptyList())
    val scoreHabits = _scoreHabits.asStateFlow()

    private val _acceptedHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val acceptedHabitIds = _acceptedHabitIds.asStateFlow()

    fun toggleHabit(id: String) {
        _acceptedHabitIds.value = if (_acceptedHabitIds.value.contains(id)) _acceptedHabitIds.value - id else _acceptedHabitIds.value + id
    }

    val baseScore: StateFlow<Int> = combine(totalIncome, totalExpenses) { inc, exp ->
        if (inc == 0) 50 else (50 + (((inc - exp).toFloat() / inc) * 100)).toInt().coerceIn(30, 99)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    private val _savedBonus = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = combine(baseScore, _savedBonus) { base, bonus ->
        (base + bonus).coerceAtMost(100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    val defaultScoreHabits: StateFlow<List<AiHabitData>> = transactions.map { txns ->
        val expenses = txns.filter { !it.isIncome }
        expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
            .entries.sortedByDescending { it.value }.take(3).mapIndexed { i, entry ->
                AiHabitData(
                    category = entry.key,
                    title = "Optimize ${entry.key}",
                    description = "Cutting back here will directly accelerate your Achievement Navigator plan.",
                    points = 6 - i,
                    statAmount = "₹${entry.value}",
                    statLabel = "this month",
                    acceptText = "Set Limit",
                    rejectText = "Skip"
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * REFRESH HABITS (The Gemini Score Banner)
     * Robust implementation to handle markdown and conversational AI noise.
     */
    fun generateScoreHabits(ignoredKey: String = "") {
        viewModelScope.launch {
            _isScoreLoading.value = true
            _scoreError.value = null
            try {
                val expenses = transactions.value.filter { !it.isIncome }
                val summary = if (expenses.isEmpty()) {
                    "User has no expenses logged. Suggest 3 elite general saving habits for a high-net-worth individual."
                } else {
                    expenses.joinToString("\n") { "${it.title}: ₹${it.amount} (${it.category})" }
                }

                val prompt = """
                    I am using 'expectr', a luxury financial app. Analyze these spends:
                    $summary
                    
                    Return exactly 3 specific saving habits.
                    CRITICAL FORMAT RULE: Return ONLY 3 pipe-separated lines with NO other text.
                    Each line must have exactly 8 fields separated by | (pipe character):
                    Category|Title|Description|Points(1-10)|StatAmount|StatLabel|AcceptText|RejectText
                    
                    Example:
                    Food|Cut Dining Out|Cook at home 4 days a week to save ₹2000|7|₹3,500|this month|Set Limit|Skip
                """.trimIndent()

                val result = callGeminiApi(prompt, GEMINI_API_KEY)
                Log.d("ZorvynAI", "ScoreHabits raw response: $result")

                // Robust Parsing: Strips markdown and conversational fluff
                val habits = result.lines()
                    .map { it.replace("`", "").replace("*", "").trim() }
                    .filter { it.contains("|") && !it.startsWith("Category") } // skip header rows
                    .mapNotNull { line ->
                        val p = line.split("|").map { it.trim() }
                        if (p.size >= 8) {
                            AiHabitData(p[0], p[1], p[2], p[3].toIntOrNull() ?: 5, p[4], p[5], p[6], p[7])
                        } else if (p.size >= 3) {
                            // Partial fallback: at least use category + title + description
                            AiHabitData(p[0], p.getOrElse(1){"Optimize"}, p.getOrElse(2){"Review this spending area"}, p.getOrElse(3){"5"}.toIntOrNull() ?: 5, p.getOrElse(4){"—"}, p.getOrElse(5){"this month"}, p.getOrElse(6){"Accept"}, p.getOrElse(7){"Skip"})
                        } else null
                    }
                    .take(3)

                if (habits.isNotEmpty()) {
                    _scoreHabits.value = habits
                    _scoreError.value = null
                } else {
                    _scoreError.value = "AI returned an unexpected format. Try again."
                    Log.w("ZorvynAI", "Parsing produced 0 habits from: $result")
                }
            } catch (e: Exception) {
                Log.e("ZorvynAI", "generateScoreHabits failed", e)
                _scoreError.value = "Failed to connect: ${e.message?.take(80)}"
            } finally {
                _isScoreLoading.value = false
            }
        }
    }

    // --- HOME SCREEN FEATURES ---
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _aiInsights = MutableStateFlow<List<String>>(emptyList())
    val aiInsights = _aiInsights.asStateFlow()

    private val _aiInsightsError = MutableStateFlow<String?>(null)
    val aiInsightsError = _aiInsightsError.asStateFlow()

    private val _scoreError = MutableStateFlow<String?>(null)
    val scoreError = _scoreError.asStateFlow()

    private val _isVoiceLoading = MutableStateFlow(false)
    val isVoiceLoading = _isVoiceLoading.asStateFlow()

    private val _isScannerLoading = MutableStateFlow(false)
    val isScannerLoading = _isScannerLoading.asStateFlow()

    fun generateInsights(ignoredKey: String = "") {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiInsightsError.value = null
            try {
                val allTxns = transactions.value
                val expenses = allTxns.filter { !it.isIncome }
                val income = totalIncome.value
                val totalExp = totalExpenses.value

                if (expenses.isEmpty()) {
                    _aiInsightsError.value = "Add some expenses first so Gemini can analyse your spending patterns."
                    return@launch
                }

                // Build rich category breakdown for the prompt
                val categoryBreakdown = expenses
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                    .joinToString("\n") { (cat, amt) ->
                        val pct = if (totalExp > 0) (amt * 100 / totalExp) else 0
                        "  • $cat: ₹$amt ($pct% of total spending)"
                    }

                val topCategory = expenses
                    .groupBy { it.category }
                    .maxByOrNull { it.value.sumOf { t -> t.amount } }
                    ?.key ?: "Unknown"

                val prompt = """
                    You are an elite personal finance advisor inside a premium app called 'expectr'.
                    
                    USER'S FINANCIAL SNAPSHOT:
                    Total Income: ₹$income
                    Total Expenses: ₹$totalExp
                    Savings Rate: ${if (income > 0) ((income - totalExp) * 100 / income) else 0}%
                    Number of transactions: ${expenses.size}
                    Top spending category: $topCategory
                    
                    SPENDING BY CATEGORY:
                    $categoryBreakdown
                    
                    Generate exactly 3 sharp, specific insights about their SPENDING habits.
                    Rules:
                    - Each insight must mention a specific category name and/or amount
                    - Insight 1: Identify their BIGGEST spending leak and quantify it
                    - Insight 2: Spot an interesting PATTERN or COMPARISON between categories
                    - Insight 3: Give ONE specific actionable tip to save money this month
                    - Keep each insight under 25 words
                    - Be direct and conversational, like a smart friend — not a boring report
                    - Use ₹ symbol for amounts
                    
                    Return ONLY 3 lines, each starting with a dash (-). No headers, no markdown.
                """.trimIndent()

                val res = callGeminiApi(prompt, GEMINI_API_KEY)
                Log.d("ZorvynAI", "Insights raw response: $res")
                // Robust parsing: handle numbered lists (1. 2. 3.), bullets (• * -), or plain lines
                val parsed = res.lines()
                    .map { it.replace("**", "").replace("*", "").trim() }
                    .map { it.replace(Regex("^\\d+[.):]+\\s*"), "") }
                    .map { it.removePrefix("-").removePrefix("•").trim() }
                    .filter { it.length > 10 }
                    .take(3)
                if (parsed.isNotEmpty()) {
                    _aiInsights.value = parsed
                } else {
                    _aiInsightsError.value = "AI returned an unexpected format. Try again."
                    Log.w("ZorvynAI", "Parsing produced 0 insights from: $res")
                }
            } catch (e: Exception) {
                Log.e("ZorvynAI", "generateInsights failed", e)
                _aiInsightsError.value = "Failed to connect: ${e.message?.take(80)}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun processReceiptText(rawReceiptText: String, k: String = "") {
        viewModelScope.launch {
            _isScannerLoading.value = true
            try {
                val res = callGeminiApi("Extract details from receipt: $rawReceiptText. Return JSON: {title, amount, category}", GEMINI_API_KEY)
                val json = JSONObject(res.substring(res.indexOf('{'), res.lastIndexOf('}') + 1))
                addTransaction(json.getInt("amount"), false, json.getString("category"), System.currentTimeMillis(), json.getString("title"))
            } catch (e: Exception) { e.printStackTrace() } finally { _isScannerLoading.value = false }
        }
    }

    fun processVoiceTransaction(spokenText: String, k: String = "") {
        viewModelScope.launch {
            _isVoiceLoading.value = true
            try {
                val res = callGeminiApi("User said: $spokenText. Return JSON {title, amount, isIncome, category}", GEMINI_API_KEY)
                val json = JSONObject(res.substring(res.indexOf('{'), res.lastIndexOf('}') + 1))
                addTransaction(json.getInt("amount"), json.getBoolean("isIncome"), json.getString("category"), System.currentTimeMillis(), json.getString("title"))
            } catch (e: Exception) { e.printStackTrace() } finally { _isVoiceLoading.value = false }
        }
    }

    // --- UTILITIES ---
    private suspend fun callGeminiApi(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Gemini API key is missing. Add GEMINI_API_KEY to local.properties and rebuild.")
        }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        Log.d("ZorvynAI", "Calling Gemini API...")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
        }
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().readText()
            val text = JSONObject(responseText).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            Log.d("ZorvynAI", "API success, response length: ${text.length}")
            text
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown Error"
            Log.e("ZorvynAI", "API Error ${conn.responseCode}: $error")
            throw Exception("API Error ${conn.responseCode}: ${error.take(200)}")
        }
    }

    fun savePlan(bonus: Int) {
        _savedBonus.value = bonus
    }

    fun addTransaction(amount: Int, isIncome: Boolean, category: String, dateMillis: Long, note: String?) {
        val icon = when(category) { "Food" -> IconType.FOOD; "Salary" -> IconType.SALARY; else -> IconType.DEFAULT }
        viewModelScope.launch { dao.insertTransaction(Transaction(0, note ?: category, category, amount, isIncome, category, dateMillis, note, icon)) }
    }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { dao.deleteTransaction(t) }
}