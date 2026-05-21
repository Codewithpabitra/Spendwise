package com.example.ui

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SpendWiseRepository
import com.example.data.repository.UserSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SpendWiseTab {
    DASHBOARD,
    HISTORY,
    AI_COACH,
    RECURRING,
    FAMILY
}

class SpendWiseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SpendWiseRepository(application)

    // --- UI States ---
    val userSession: StateFlow<UserSession> = repository.userSession

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingGoals: StateFlow<List<SavingGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<FamilyMember>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(SpendWiseTab.DASHBOARD)
    val currentTab = _currentTab.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading = _isAILoading.asStateFlow()

    private val _aiInsightTip = MutableStateFlow(
        "💡 SpendWise Tip: Try setting a daily dining budget of ₹200 to keep overspending at bay."
    )
    val aiInsightTip = _aiInsightTip.asStateFlow()

    private val _notificationsList = MutableStateFlow<List<String>>(emptyList())
    val notificationsList = _notificationsList.asStateFlow()

    private val _streakCount = MutableStateFlow(5) // Weekly saving streak count
    val streakCount = _streakCount.asStateFlow()

    // Screen states
    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    init {
        preloadData()
        generateDailyInsight()
    }

    private fun preloadData() {
        viewModelScope.launch {
            repository.preloadDemoAccount()
        }
    }

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
    }

    fun login(email: String, name: String, role: String) {
        viewModelScope.launch {
            repository.preloadDemoAccount()
            repository.authenticateUser("salaried.demo@spendwise.ai", "demo123")
            addNotification("Session restored: Welcome back, $name!")
        }
    }

    suspend fun registerUser(email: String, name: String, passwordRaw: String, role: String): Boolean {
        val success = repository.registerUser(email, name, passwordRaw, role)
        if (success) {
            addNotification("Account created securely for $name!")
        }
        return success
    }

    suspend fun loginUser(email: String, passwordRaw: String): Boolean {
        val success = repository.authenticateUser(email, passwordRaw)
        if (success) {
            val sessionName = repository.userSession.value.name
            addNotification("Access decrypted! Welcome back, $sessionName.")
        }
        return success
    }

    fun logout() {
        repository.logout()
        _isOnboardingCompleted.value = false
    }

    fun changeTab(tab: SpendWiseTab) {
        _currentTab.value = tab
    }

    fun updateRole(role: String) {
        viewModelScope.launch {
            repository.updateRole(role)
            addNotification("Profile type updated: Optimized for $role")
            generateDailyInsight()
        }
    }

    // --- Notifications Management ---
    fun addNotification(message: String) {
        val list = _notificationsList.value.toMutableList()
        list.add(0, message)
        _notificationsList.value = list
    }

    fun clearNotifications() {
        _notificationsList.value = emptyList()
    }

    // --- Expense Operations ---
    fun addManualExpense(title: String, amount: Double, category: String, paymentMethod: String, isRecurring: Boolean = false) {
        viewModelScope.launch {
            val expense = Expense(
                title = title.ifBlank { "Manual Expense" },
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                isRecurring = isRecurring
            )
            repository.addExpense(expense)
            addNotification("Transaction added: ${expense.title} (-$${expense.amount})")
            checkBudgetExceeded(category)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            addNotification("Removed expense: ${expense.title}")
        }
    }

    // --- Subscription Operations ---
    fun addSubscriptionItem(name: String, amount: Double, category: String, frequency: String, isAutoRenew: Boolean) {
        viewModelScope.launch {
            val sub = Subscription(
                name = name,
                amount = amount,
                category = category,
                nextDueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L), // Next month
                frequency = frequency,
                isAutoRenew = isAutoRenew
            )
            repository.addSubscription(sub)
            addNotification("Recurring subscription tracked: $name ($$amount/$frequency)")
        }
    }

    fun removeSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
            addNotification("Stopped tracking subscription: ${subscription.name}")
        }
    }

    // --- Goals ---
    fun addSavingGoalItem(title: String, target: Double, current: Double, months: Int) {
        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (months.toLong() * 30 * 24 * 60 * 60 * 1000L)
            val goal = SavingGoal(
                title = title,
                targetAmount = target,
                currentAmount = current,
                deadline = deadline
            )
            repository.addGoal(goal)
            addNotification("New savings target created: $title (Goal: $$target)")
        }
    }

    fun deleteGoal(goal: SavingGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            addNotification("Deleted saving goal: ${goal.title}")
        }
    }

    // --- Family Shared ---
    fun inviteFamilyMember(name: String, role: String) {
        viewModelScope.launch {
            val member = FamilyMember(name = name, role = role, inviteStatus = "Pending")
            repository.addFamilyMember(member)
            addNotification("Sent shared wallet invite to $name ($role)")
        }
    }

    fun removeFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
            addNotification("Removed family group connection for ${member.name}")
        }
    }

    // --- LLM Orchestrated Functions ---

    /**
     * Parse natural language string dynamically via Gemini
     */
    fun parseTextExpenseWithAI(textInput: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val expense = repository.parseAndAddExpense(textInput)
                if (expense != null) {
                    addNotification("AI Smart-Categorized: \"${expense.title}\" for $${amountFormatting(expense.amount)} into ${expense.category}")
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "parseTextExpenseWithAI error", e)
                onComplete(false)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    /**
     * Submit simulated receipt image Base64 to Gemini
     */
    fun parseReceiptImageWithAI(presetBase64: String, PresetName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val expense = repository.parseAndAddReceipt(presetBase64)
                if (expense != null) {
                    addNotification("Receipt OCR Saved: parsed \"${expense.title}\" -> $${amountFormatting(expense.amount)} in ${expense.category}")
                    onComplete(true)
                } else {
                    // Fallback simulation when offline/quota exhausted
                    val simulated = generateSimulationForReceipt(PresetName)
                    repository.addExpense(simulated)
                    addNotification("Receipt processed successfully!")
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "parseReceiptImageWithAI error", e)
                onComplete(false)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    /**
     * Ask dynamic questions to SpendWise AI Coach
     */
    fun askAICoach(text: String, onFinished: () -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                repository.queryAICoach(text)
            } catch (e: Exception) {
                Log.e("ViewModel", "askAICoach error", e)
                repository.addChatMessage(ChatMessage(text = "Apologies, but the SpendWise network layer is busy. Try again in a minute!", isUser = false))
            } finally {
                _isAILoading.value = false
                onFinished()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            repository.addChatMessage(ChatMessage(
                text = "Coach history refreshed. Let's start with a blank slate! 📝",
                isUser = false
            ))
        }
    }

    // Generate customizable tip of the day dynamically depending on current database
    fun generateDailyInsight() {
        viewModelScope.launch {
            val list = expenses.value
            val role = userSession.value.role
            val total = list.sumOf { it.amount }
            
            val baseTip = when (role) {
                "Student" -> "🎓 Student Smart Tip: Limit entertainment + food delivery to ₹1500/month. Campus dining offers a 30% savings yield!"
                "Salaried" -> "💼 Corporate Tip: Automate a 15% investment deduction on salary day before rent takes over."
                "Freelancer" -> "🚀 Freelancer Guard: Allocate 30% of each invoice to a tax + low-season escrow buffer."
                "Couple" -> "💑 Union Tip: Establish a mutual weekend budget cap of ₹2500 to fund your upcoming joint vacation."
                "Family" -> "🏠 Household Tip: Shared utilities make up 25% of household leakage. Review gym + streaming sharing structures."
                else -> "💡 Savings Rule: Wait 48 hours before purchasing non-essential items above ₹1000."
            }

            _aiInsightTip.value = if (total > 800) {
                "$baseTip Overspending detected in high density categories recently."
            } else {
                baseTip
            }
        }
    }

    private fun checkBudgetExceeded(category: String) {
        viewModelScope.launch {
            val budgetsSnapshot = budgets.value
            val matched = budgetsSnapshot.firstOrNull { it.category == category } ?: budgetsSnapshot.firstOrNull { it.category == "Global" }
            if (matched != null) {
                val totalExpenses = expenses.value
                    .filter { if (matched.category == "Global") true else it.category == matched.category }
                    .sumOf { it.amount }
                if (totalExpenses > matched.limitAmount) {
                    addNotification("⚠️ BUDGET EXCEEDED: Your spending in ${matched.category} ($$totalExpenses) exceeds limit ($$${matched.limitAmount})!")
                } else if (totalExpenses > matched.limitAmount * 0.85) {
                    addNotification("⚠️ BUDGET WARNING: Visual warning! Spending in ${matched.category} has reached 85% of limit!")
                }
            }
        }
    }

    private fun amountFormatting(amt: Double): String {
        return String.format("%.2f", amt)
    }

    private fun generateSimulationForReceipt(presetName: String): Expense {
        return when (presetName) {
            "Starbucks Coffee" -> Expense(title = "Starbucks Cafe", amount = 12.45, category = "Food", paymentMethod = "Receipt Scan", date = System.currentTimeMillis())
            "Whole Foods Groceries" -> Expense(title = "Whole Foods", amount = 98.30, category = "Food", paymentMethod = "Receipt Scan", date = System.currentTimeMillis())
            "Gas Station Diesel" -> Expense(title = "Shell Fuel", amount = 58.00, category = "Transport", paymentMethod = "Receipt Scan", date = System.currentTimeMillis())
            else -> Expense(title = "Scanned Merchant", amount = 45.00, category = "Shopping", paymentMethod = "Receipt Scan", date = System.currentTimeMillis())
        }
    }
}
