package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.network.Content
import com.example.data.network.GeminiApiClient
import com.example.data.network.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

data class UserSession(
    val email: String,
    val name: String,
    val role: String = "Student", // "Student", "Salaried", "Freelancer", "Couple", "Family"
    val avatarEmoji: String = "👤",
    val isLoggedIn: Boolean = false
)

class SpendWiseRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    
    private val userAccountDao = db.userAccountDao()
    private val expenseDao = db.expenseDao()
    private val budgetDao = db.budgetDao()
    private val subscriptionDao = db.subscriptionDao()
    private val chatDao = db.chatDao()
    private val savingGoalDao = db.savingGoalDao()
    private val familyDao = db.familyDao()

    // --- Authentication Session (Shared preferences persisted) ---
    private val _userSession = MutableStateFlow(
        UserSession("user@spendwise.ai", "Guest User", "Student", "🎓", false)
    )
    val userSession = _userSession.asStateFlow()

    init {
        // Automatically recover session on application launch
        val prefs = context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            val email = prefs.getString("logged_in_email", "user@spendwise.ai") ?: "user@spendwise.ai"
            val name = prefs.getString("logged_in_name", "Guest User") ?: "Guest User"
            val role = prefs.getString("logged_in_role", "Student") ?: "Student"
            val emoji = prefs.getString("logged_in_emoji", "🎓") ?: "🎓"
            _userSession.value = UserSession(email, name, role, emoji, true)
        }
    }

    suspend fun registerUser(email: String, name: String, passwordRaw: String, role: String): Boolean = withContext(Dispatchers.IO) {
        val lowercaseEmail = email.lowercase().trim()
        val existing = userAccountDao.getUserByEmail(lowercaseEmail)
        if (existing != null) return@withContext false // Email already taken
        
        val emoji = when (role) {
            "Student" -> "🎓"
            "Salaried" -> "💼"
            "Freelancer" -> "🚀"
            "Couple" -> "💑"
            "Family" -> "🏠"
            else -> "👤"
        }
        val user = UserAccount(lowercaseEmail, name, passwordRaw, role, emoji)
        userAccountDao.insertUser(user)
        
        // Feed personalized visual sandbox starting data directly under this user!
        preloadTypicalDataForUser(lowercaseEmail, role)
        return@withContext true
    }

    suspend fun authenticateUser(email: String, passwordRaw: String): Boolean = withContext(Dispatchers.IO) {
        val lowercaseEmail = email.lowercase().trim()
        val user = userAccountDao.getUserByEmail(lowercaseEmail)
        if (user != null && user.passwordHash == passwordRaw) {
            val prefs = context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("logged_in_email", user.email)
                .putString("logged_in_name", user.name)
                .putString("logged_in_role", user.role)
                .putString("logged_in_emoji", user.avatarEmoji)
                .putBoolean("is_logged_in", true)
                .apply()

            _userSession.value = UserSession(user.email, user.name, user.role, user.avatarEmoji, true)
            return@withContext true
        }
        return@withContext false
    }

    fun logout() {
        val prefs = context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _userSession.value = UserSession("user@spendwise.ai", "Guest User", "Student", "🎓", false)
    }

    suspend fun updateRole(newRole: String) = withContext(Dispatchers.IO) {
        val curr = _userSession.value
        val emoji = when (newRole) {
            "Student" -> "🎓"
            "Salaried" -> "💼"
            "Freelancer" -> "🚀"
            "Couple" -> "💑"
            "Family" -> "🏠"
            else -> "👤"
        }
        val updatedSession = curr.copy(role = newRole, avatarEmoji = emoji)
        
        // Write back profile details in database
        val user = userAccountDao.getUserByEmail(curr.email)
        if (user != null) {
            userAccountDao.insertUser(user.copy(role = newRole, avatarEmoji = emoji))
        }

        // Write back in SharedPreferences
        val prefs = context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("logged_in_role", newRole)
            .putString("logged_in_emoji", emoji)
            .apply()

        _userSession.value = updatedSession
    }

    // --- Dynamic Multi-User Flow Mapping ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val allExpenses: Flow<List<Expense>> = userSession.flatMapLatest { session ->
        expenseDao.getAllExpensesFlow(session.email)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBudgets: Flow<List<Budget>> = userSession.flatMapLatest { session ->
        budgetDao.getAllBudgetsFlow(session.email)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSubscriptions: Flow<List<Subscription>> = userSession.flatMapLatest { session ->
        subscriptionDao.getAllSubscriptionsFlow(session.email)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allGoals: Flow<List<SavingGoal>> = userSession.flatMapLatest { session ->
        savingGoalDao.getAllGoalsFlow(session.email)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allFamilyMembers: Flow<List<FamilyMember>> = userSession.flatMapLatest { session ->
        familyDao.getFamilyMembersFlow(session.email)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: Flow<List<ChatMessage>> = userSession.flatMapLatest { session ->
        chatDao.getChatHistoryFlow(session.email)
    }

    // --- Database Operations ---

    suspend fun addExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        expenseDao.insertExpense(expense.copy(userEmail = currentEmail))
        updateCategoryBudgets()
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        expenseDao.updateExpense(expense.copy(userEmail = currentEmail))
        updateCategoryBudgets()
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
        updateCategoryBudgets()
    }

    suspend fun deleteExpenseById(id: Int) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
        updateCategoryBudgets()
    }

    suspend fun addBudget(budget: Budget) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        budgetDao.insertBudget(budget.copy(userEmail = currentEmail))
    }

    suspend fun deleteBudget(category: String) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        budgetDao.deleteBudgetByCategory(currentEmail, category)
    }

    /**
     * Recalculates limits and spending for each Category based on stored values
     */
    private suspend fun updateCategoryBudgets() {
        val currentEmail = userSession.value.email
        val expenses = expenseDao.getAllExpenses(currentEmail)
        val budgets = budgetDao.getAllBudgets(currentEmail)
        
        budgets.forEach { budget ->
            val totalForCategory = expenses
                .filter { if (budget.category == "Global") true else it.category == budget.category }
                .sumOf { it.amount }
            
            budgetDao.insertBudget(budget.copy(spentAmount = totalForCategory, userEmail = currentEmail))
        }
    }

    suspend fun addSubscription(subscription: Subscription) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        subscriptionDao.insertSubscription(subscription.copy(userEmail = currentEmail))
    }

    suspend fun deleteSubscription(subscription: Subscription) = withContext(Dispatchers.IO) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun addGoal(goal: SavingGoal) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        savingGoalDao.insertGoal(goal.copy(userEmail = currentEmail))
    }

    suspend fun deleteGoal(goal: SavingGoal) = withContext(Dispatchers.IO) {
        savingGoalDao.deleteGoal(goal)
    }

    suspend fun addFamilyMember(member: FamilyMember) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        familyDao.insertMember(member.copy(userEmail = currentEmail))
    }

    suspend fun deleteFamilyMember(member: FamilyMember) = withContext(Dispatchers.IO) {
        familyDao.deleteMember(member)
    }

    suspend fun addChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        chatDao.insertMessage(message.copy(userEmail = currentEmail))
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        val currentEmail = userSession.value.email
        chatDao.clearHistory(currentEmail)
    }

    // --- LLM Powered Integrations ---

    /**
     * Use Gemini NLP to parse a text descriptor and automatically insert the parsed expense
     */
    suspend fun parseAndAddExpense(rawText: String): Expense? {
        val parsed = GeminiApiClient.parseExpenseInput(rawText) ?: return null
        val currentEmail = userSession.value.email
        val expense = Expense(
            title = parsed.title,
            amount = parsed.amount,
            category = parsed.category,
            paymentMethod = "AI Auto-Parsed",
            date = System.currentTimeMillis(),
            userEmail = currentEmail
        )
        addExpense(expense)
        return expense
    }

    /**
     * Send a prompt to the AI financial coach, incorporating historical chats and spending summaries
     */
    suspend fun queryAICoach(userPrompt: String): String {
        val currentEmail = userSession.value.email
        val expenses = expenseDao.getAllExpenses(currentEmail)
        val expenseSummary = if (expenses.isEmpty()) {
            "No expenses recorded yet. Total Spending: $0"
        } else {
            val total = expenses.sumOf { it.amount }
            val byCategory = expenses.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.joinToString { "${it.key}: ${it.value}" }
            "Total spending overall: $total. Category breakdown: $byCategory."
        }

        val rawHistory = chatDao.getChatHistory(currentEmail)
        val recentHistory = rawHistory.takeLast(10).map { msg ->
            Content(
                parts = listOf(Part(text = msg.text)),
                role = if (msg.isUser) "user" else "model"
            )
        }

        addChatMessage(ChatMessage(text = userPrompt, isUser = true, userEmail = currentEmail))

        val responseText = GeminiApiClient.getCoachResponse(userPrompt, expenseSummary, recentHistory)
        
        addChatMessage(ChatMessage(text = responseText, isUser = false, userEmail = currentEmail))

        return responseText
    }

    /**
     * Submit base64 image data of a bill/receipt to automatically parse and save the expense
     */
    suspend fun parseAndAddReceipt(base64Image: String): Expense? {
        val parsed = GeminiApiClient.scanReceiptImage(base64Image) ?: return null
        val currentEmail = userSession.value.email
        val expense = Expense(
            title = parsed.title,
            amount = parsed.amount,
            category = parsed.category,
            paymentMethod = "Receipt scan",
            date = System.currentTimeMillis(),
            userEmail = currentEmail
        )
        addExpense(expense)
        return expense
    }

    /**
     * Preloaded live verified demo profiles and startup credentials
     */
    suspend fun preloadDemoAccount() = withContext(Dispatchers.IO) {
        val demoEmail = "salaried.demo@spendwise.ai"
        val existing = userAccountDao.getUserByEmail(demoEmail)
        if (existing == null) {
            val user = UserAccount(
                email = demoEmail,
                name = "Alex Mercer",
                passwordHash = "demo123",
                role = "Salaried",
                avatarEmoji = "💼"
            )
            userAccountDao.insertUser(user)
            preloadTypicalDataForUser(demoEmail, "Salaried")
        }
    }

    /**
     * Initial preparation of sample sandbox records for high-fidelity interactive visualization
     */
    suspend fun preloadTypicalDataForUser(email: String, role: String) = withContext(Dispatchers.IO) {
        val lowercaseEmail = email.lowercase().trim()
        
        // 1. Default budgets
        val existingBudgets = budgetDao.getAllBudgets(lowercaseEmail)
        if (existingBudgets.isEmpty()) {
            budgetDao.insertBudget(Budget("Global", 5000.0, 1850.0, lowercaseEmail))
            budgetDao.insertBudget(Budget("Food", 1000.0, 320.0, lowercaseEmail))
            budgetDao.insertBudget(Budget("Transport", 500.0, 140.0, lowercaseEmail))
            budgetDao.insertBudget(Budget("Shopping", 800.0, 390.0, lowercaseEmail))
            budgetDao.insertBudget(Budget("Entertainment", 600.0, 250.0, lowercaseEmail))
        }

        // 2. Default expenses
        val existingExpenses = expenseDao.getAllExpenses(lowercaseEmail)
        if (existingExpenses.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            expenseDao.insertExpense(Expense(title = "Subway Combo Meal", amount = 15.0, category = "Food", paymentMethod = "Credit Card", date = now - 2 * hourMs(), userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Monthly Gym Membership", amount = 80.0, category = "Health", paymentMethod = "UPI", date = now - dayMs * 1, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Weekly Gas Filling", amount = 45.0, category = "Transport", paymentMethod = "Debit Card", date = now - dayMs * 2, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Uber Ride downtown", amount = 22.0, category = "Transport", paymentMethod = "Cash", date = now - dayMs * 3, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Walmart Weekly Groceries", amount = 124.0, category = "Food", paymentMethod = "Credit Card", date = now - dayMs * 4, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Netflix Premium Plan", amount = 15.49, category = "Bills", paymentMethod = "Card Autopay", date = now - dayMs * 5, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Running Sneakers", amount = 110.0, category = "Shopping", paymentMethod = "UPI", date = now - dayMs * 6, userEmail = lowercaseEmail))
            expenseDao.insertExpense(Expense(title = "Weekend Movie Tickets", amount = 24.50, category = "Entertainment", paymentMethod = "Cash", date = now - dayMs * 7, userEmail = lowercaseEmail))
        }

        // 3. Default recurring subscriptions
        val existingSubscriptions = subscriptionDao.getAllSubscriptions(lowercaseEmail)
        if (existingSubscriptions.isEmpty()) {
            val now = System.currentTimeMillis()
            val monthMs = 30 * 24 * 60 * 60 * 1000L
            subscriptionDao.insertSubscription(Subscription(name = "Netflix Standard", amount = 15.49, category = "Bills", nextDueDate = now + monthMs / 3, frequency = "Monthly", userEmail = lowercaseEmail))
            subscriptionDao.insertSubscription(Subscription(name = "Spotify Premium Duo", amount = 14.99, category = "Entertainment", nextDueDate = now + monthMs / 2, frequency = "Monthly", userEmail = lowercaseEmail))
            subscriptionDao.insertSubscription(Subscription(name = "PlayStation Plus", amount = 9.99, category = "Entertainment", nextDueDate = now + monthMs / 4, frequency = "Monthly", userEmail = lowercaseEmail))
            subscriptionDao.insertSubscription(Subscription(name = "Amazon Prime Video", amount = 8.99, category = "Bills", nextDueDate = now + monthMs * 2 / 3, frequency = "Monthly", userEmail = lowercaseEmail))
            subscriptionDao.insertSubscription(Subscription(name = "Gym Club membership", amount = 45.00, category = "Health", nextDueDate = now + 5 * monthMs / 6, frequency = "Monthly", userEmail = lowercaseEmail))
        }

        // 4. Default saving goals
        val existingGoals = savingGoalDao.getAllGoals(lowercaseEmail)
        if (existingGoals.isEmpty()) {
            val monthMs = 30 * 24 * 60 * 60 * 1000L
            savingGoalDao.insertGoal(SavingGoal(title = "Sovereign Emergency Fund", targetAmount = 5000.0, currentAmount = 1850.0, deadline = System.currentTimeMillis() + monthMs * 6, userEmail = lowercaseEmail))
            savingGoalDao.insertGoal(SavingGoal(title = "Summer Vacation 2026", targetAmount = 2500.0, currentAmount = 800.0, deadline = System.currentTimeMillis() + monthMs * 3, userEmail = lowercaseEmail))
        }

        // 5. Default family group members
        val existingMembers = familyDao.getAllFamilyMembers(lowercaseEmail)
        if (existingMembers.isEmpty()) {
            familyDao.insertMember(FamilyMember(name = "Alex (Self)", role = "Owner", inviteStatus = "Joined", userEmail = lowercaseEmail))
            familyDao.insertMember(FamilyMember(name = "Sophia (Wife/Partner)", role = "Partner", inviteStatus = "Joined", userEmail = lowercaseEmail))
            familyDao.insertMember(FamilyMember(name = "Mom (Dependent)", role = "Member", inviteStatus = "Pending", userEmail = lowercaseEmail))
        }

        // 6. Preload initial Coach welcome message
        val rawChats = chatDao.getChatHistory(lowercaseEmail)
        if (rawChats.isEmpty()) {
            chatDao.insertMessage(ChatMessage(
                text = "Welcome to **SpendWise AI Coached Hub**! 🎓\n\nI'm your modern mobile financial wellness guide. I've analyzed your category budgets and initial typical transactions.\n\n*What can I help you optimize today?* Try asking me how to save more effectively, or whether you can afford a new purchase!",
                isUser = false,
                userEmail = lowercaseEmail
            ))
        }

        updateCategoryBudgets()
    }

    private fun hourMs() = 60 * 60 * 1000L
}
