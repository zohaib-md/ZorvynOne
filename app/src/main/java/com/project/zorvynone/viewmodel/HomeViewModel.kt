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
import java.util.Calendar

/**
 * The Wealth Intelligence Engine for ZorvynOne.
 * Manages Smart Vaults, Round-Up Engine, Savings Streaks,
 * AI Scoring, and Transaction Processing.
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

    // ═══════════════════════════════════════════════════════════════════
    // ── SMART VAULTS ENGINE ──────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

    val allVaults: StateFlow<List<SavingsGoal>> = dao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRoundUpSavings: StateFlow<Double> = dao.getTotalRoundUpSavings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAllSavings: StateFlow<Double> = dao.getTotalAllSavings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun createVault(title: String, target: Double, deadlineMillis: Long, emoji: String, roundUp: Int) {
        viewModelScope.launch {
            val isFirst = allVaults.value.isEmpty()
            dao.insertGoal(
                SavingsGoal(
                    title = title,
                    targetAmount = target,
                    deadline = deadlineMillis,
                    iconEmoji = emoji,
                    roundUpRule = roundUp,
                    isDefaultRoundUpVault = isFirst // First vault auto-becomes round-up target
                )
            )
        }
    }

    fun deleteVault(goal: SavingsGoal) {
        viewModelScope.launch { dao.deleteGoal(goal) }
    }

    fun depositToVault(goalId: Int, amount: Double, source: String = "manual") {
        viewModelScope.launch {
            dao.addToVault(goalId, amount)
            dao.insertDeposit(SavingsDeposit(goalId = goalId, amount = amount, source = source))
        }
    }

    fun setDefaultRoundUpVault(goalId: Int) {
        viewModelScope.launch {
            dao.clearDefaultRoundUpVault()
            dao.setDefaultRoundUpVault(goalId)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ── ROUND-UP ENGINE ──────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Called automatically after every expense transaction.
     * Rounds up to the nearest rule amount and deposits the difference.
     */
    private fun processRoundUp(expenseAmount: Int) {
        val defaultVault = allVaults.value.firstOrNull { it.isDefaultRoundUpVault && it.roundUpRule > 0 }
            ?: return

        val rule = defaultVault.roundUpRule
        val roundedUp = ((expenseAmount + rule - 1) / rule) * rule
        val roundUpAmount = roundedUp - expenseAmount

        if (roundUpAmount > 0) {
            depositToVault(defaultVault.id, roundUpAmount.toDouble(), "round_up")
            Log.d("ZorvynVaults", "Round-up: ₹$expenseAmount → ₹$roundedUp (saved ₹$roundUpAmount to '${defaultVault.title}')")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ── SAVINGS STREAK ENGINE ────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

    // Categories that are fixed/non-discretionary — excluded from daily budget tracking
    private val FIXED_CATEGORIES = setOf(
        "housing", "rent", "home", "bills", "utilities",
        "transport", "transportation", "travel", "uber", "cab",
        "insurance", "emi", "loan", "subscription"
    )

    private fun isDiscretionary(category: String) = category.lowercase() !in FIXED_CATEGORIES

    private val _dailyBudget = MutableStateFlow(500.0)
    val dailyBudget = _dailyBudget.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak = _longestStreak.asStateFlow()

    // Today's expenses — only discretionary spending counts against daily budget
    val todaySpent: StateFlow<Int> = transactions.map { txns ->
        val todayStart = getTodayStartMillis()
        txns.filter { !it.isIncome && it.date >= todayStart && isDiscretionary(it.category) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isUnderBudgetToday: StateFlow<Boolean> = combine(todaySpent, _dailyBudget) { spent, budget ->
        spent <= budget
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDailyBudget(amount: Double) {
        _dailyBudget.value = amount
    }

    /**
     * Streak check logic:
     * Look at the last 7 days of spending. For each day where total expense ≤ daily budget,
     * count consecutive days from today backward. That's the streak.
     */
    fun checkAndUpdateStreak() {
        viewModelScope.launch {
            val txns = transactions.value.filter { !it.isIncome && isDiscretionary(it.category) }
            val budget = _dailyBudget.value
            var streak = 0

            // Check each day from yesterday backward (today is still in progress)
            for (daysAgo in 1..90) {
                val dayStart = getDayStartMillis(daysAgo)
                val dayEnd = getDayStartMillis(daysAgo - 1)
                val daySpend = txns.filter { it.date in dayStart until dayEnd }.sumOf { it.amount }

                // A day with no transactions doesn't break streak (user may not have spent)
                if (daySpend <= budget) {
                    streak++
                } else {
                    break
                }
            }

            _currentStreak.value = streak
            if (streak > _longestStreak.value) {
                _longestStreak.value = streak
            }
        }
    }

    fun getStreakMilestone(streak: Int): String? = when {
        streak >= 90 -> "🏆 LEGENDARY"
        streak >= 60 -> "💎 DIAMOND"
        streak >= 30 -> "🥇 GOLD"
        streak >= 14 -> "🥈 SILVER"
        streak >= 7  -> "🥉 BRONZE"
        else -> null
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayStartMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ═══════════════════════════════════════════════════════════════════
    // ── AI SAVINGS COACH ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

    private val _savingsCoachAdvice = MutableStateFlow("Set up a vault to receive personalized savings advice.")
    val savingsCoachAdvice = _savingsCoachAdvice.asStateFlow()

    private val _isSavingsCoachLoading = MutableStateFlow(false)
    val isSavingsCoachLoading = _isSavingsCoachLoading.asStateFlow()

    fun generateSavingsCoachAdvice() {
        viewModelScope.launch {
            _isSavingsCoachLoading.value = true
            try {
                val vaults = allVaults.value
                val streak = _currentStreak.value
                val roundUpTotal = totalRoundUpSavings.value
                val budget = _dailyBudget.value
                val todaySpending = todaySpent.value

                val categoryHistory = transactions.value
                    .filter { !it.isIncome }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val vaultSummary = if (vaults.isEmpty()) "No vaults set up yet."
                else vaults.joinToString("\n") { v ->
                    val pct = if (v.targetAmount > 0) ((v.savedAmount / v.targetAmount) * 100).toInt() else 0
                    "  • ${v.iconEmoji} ${v.title}: ₹${v.savedAmount.toInt()} / ₹${v.targetAmount.toInt()} ($pct%)"
                }

                val prompt = """
                    Act as an Elite Private Savings Coach inside a premium fintech app.
                    
                    USER'S SAVINGS STATE:
                    Active Vaults:
                    $vaultSummary
                    Round-Up Savings Total: ₹${roundUpTotal.toInt()}
                    Current Streak: $streak days under budget
                    Daily Budget: ₹${budget.toInt()} | Spent Today: ₹$todaySpending
                    
                    Category Spending: $categoryHistory
                    
                    Give exactly 3 lines of advice:
                    Line 1: Projected completion date for their closest vault (or encouragement to start)
                    Line 2: One specific spending category to cut based on their data
                    Line 3: A motivational streak message or challenge
                    
                    Rules:
                    - Max 20 words per line
                    - Use ₹ for amounts
                    - Be direct, sharp, like a smart friend
                    - Return ONLY 3 lines starting with dash (-). No headers.
                """.trimIndent()

                val result = callGeminiApi(prompt, GEMINI_API_KEY)
                val parsed = result.lines()
                    .map { it.replace("**", "").replace("*", "").trim() }
                    .map { it.replace(Regex("^\\d+[.):]+\\s*"), "") }
                    .map { it.removePrefix("-").removePrefix("•").trim() }
                    .filter { it.length > 5 }
                    .take(3)

                if (parsed.isNotEmpty()) {
                    _savingsCoachAdvice.value = parsed.joinToString("\n\n")
                }
            } catch (e: Exception) {
                Log.e("ZorvynAI", "Savings coach failed", e)
                _savingsCoachAdvice.value = "Coach is offline. Check back soon."
            } finally {
                _isSavingsCoachLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ── AI SCORING & HABIT LOGIC ─────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

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

    /**
     * EXPECTR HEALTH SCORE — Multi-factor weighted composite
     *
     * Factor 1: Savings Rate (35 pts) — (income - expenses) / income
     * Factor 2: Budget Discipline (25 pts) — streak days under budget
     * Factor 3: Vault Progress (20 pts) — actively saving toward goals
     * Factor 4: Spending Diversity (20 pts) — not over-concentrated in one category
     */
    val baseScore: StateFlow<Int> = combine(
        combine(totalIncome, totalExpenses, _currentStreak) { inc, exp, streak -> Triple(inc, exp, streak) },
        allVaults,
        transactions
    ) { (inc, exp, streak), vaults, txns ->

        // F1: Savings Rate (0-35 pts)
        val savingsRate = if (inc > 0) ((inc - exp).toFloat() / inc).coerceIn(0f, 1f) else 0f
        val f1 = when {
            savingsRate >= 0.40f -> 35
            savingsRate >= 0.25f -> 28
            savingsRate >= 0.15f -> 22
            savingsRate >= 0.05f -> 15
            savingsRate > 0f -> 8
            else -> 0
        }

        // F2: Budget Discipline (0-25 pts) — based on streak
        val f2 = when {
            streak >= 30 -> 25
            streak >= 14 -> 20
            streak >= 7 -> 15
            streak >= 3 -> 10
            streak >= 1 -> 5
            else -> 0
        }

        // F3: Vault Progress (0-20 pts)
        val f3 = if (vaults.isEmpty()) 0 else {
            val avgProgress = vaults.map { v ->
                if (v.targetAmount > 0) (v.savedAmount / v.targetAmount).coerceIn(0.0, 1.0) else 0.0
            }.average()
            when {
                avgProgress >= 0.75 -> 20
                avgProgress >= 0.50 -> 16
                avgProgress >= 0.25 -> 12
                avgProgress > 0.0 -> 8
                else -> 4 // Has vaults but no deposits yet
            }
        }

        // F4: Spending Diversity (0-20 pts) — penalize if one category > 60% of spend
        val expenses = txns.filter { !it.isIncome }
        val totalExp = expenses.sumOf { it.amount }
        val f4 = if (totalExp == 0 || expenses.isEmpty()) 10 else {
            val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
            val topCatShare = (categories.maxOf { it.value }.toFloat() / totalExp)
            val numCategories = categories.size
            when {
                topCatShare <= 0.35f && numCategories >= 4 -> 20
                topCatShare <= 0.50f && numCategories >= 3 -> 16
                topCatShare <= 0.60f -> 12
                topCatShare <= 0.75f -> 8
                else -> 4
            }
        }

        (f1 + f2 + f3 + f4).coerceIn(15, 99)
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
                    description = "Cutting back here will directly accelerate your vault progress.",
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
        val icon = when(category.lowercase()) {
            "food" -> IconType.FOOD
            "café", "cafe", "coffee" -> IconType.CAFE
            "shopping" -> IconType.SHOPPING
            "transport", "transportation", "travel", "uber", "cab" -> IconType.TRANSPORT
            "housing", "rent", "home", "bills", "utilities" -> IconType.HOUSING
            "salary", "income", "freelance" -> IconType.SALARY
            "wallet", "transfer" -> IconType.WALLET
            else -> IconType.DEFAULT
        }
        viewModelScope.launch {
            dao.insertTransaction(Transaction(0, note ?: category, category, amount, isIncome, category, dateMillis, note, icon))

            // 🔄 Auto Round-Up: process after every expense
            if (!isIncome) {
                processRoundUp(amount)
            }

            // 🔥 Update streak after each transaction
            checkAndUpdateStreak()
        }
    }

    fun deleteTransaction(t: Transaction) = viewModelScope.launch { dao.deleteTransaction(t) }
}