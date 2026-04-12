package com.project.zorvynone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // --- SECURE INTERNAL CONFIGURATION ---
    private val GEMINI_API_KEY = "AIzaSyCQbidjtYufdF4_K79nZFBl8-iU59j9pK8"

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
                    FORMAT RULE: Use ONLY pipe-separated lines. No markdown, no bold, no bullet points.
                    Format: Category|Title|Description (Include a Pro-Tip)|Points(1-10)|StatAmount|StatLabel|AcceptText|RejectText
                """.trimIndent()

                val result = callGeminiApi(prompt, GEMINI_API_KEY)

                // Robust Parsing: Strips markdown and conversational fluff
                val habits = result.lines()
                    .filter { it.contains("|") }
                    .mapNotNull { line ->
                        val cleaned = line.replace("`", "").replace("*", "").trim()
                        val p = cleaned.split("|").map { it.trim() }
                        if (p.size >= 8) {
                            AiHabitData(p[0], p[1], p[2], p[3].toIntOrNull() ?: 5, p[4], p[5], p[6], p[7])
                        } else null
                    }
                    .take(3)

                if (habits.isNotEmpty()) {
                    _scoreHabits.value = habits
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private val _isVoiceLoading = MutableStateFlow(false)
    val isVoiceLoading = _isVoiceLoading.asStateFlow()

    private val _isScannerLoading = MutableStateFlow(false)
    val isScannerLoading = _isScannerLoading.asStateFlow()

    fun generateInsights(ignoredKey: String = "") {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val summary = transactions.value.joinToString { "${it.title}: ₹${it.amount}" }
                val res = callGeminiApi("Provide 3 financial insights for: $summary. Return as dash-separated list.", GEMINI_API_KEY)
                _aiInsights.value = res.lines().filter { it.contains("-") }.map { it.trim().removePrefix("-").trim() }
            } catch (e: Exception) { e.printStackTrace() } finally { _isAiLoading.value = false }
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
        // FIXED: Cleaned URL string (removed markdown artifacts)
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().readText()
            JSONObject(responseText).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown Error"
            throw Exception("API Error ${conn.responseCode}: $error")
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