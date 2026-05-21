package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userEmail = :userEmail ORDER BY date DESC")
    fun getAllExpensesFlow(userEmail: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userEmail = :userEmail ORDER BY date DESC")
    suspend fun getAllExpenses(userEmail: String): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("SELECT * FROM expenses WHERE userEmail = :userEmail AND category = :category ORDER BY date DESC")
    fun getExpensesByCategory(userEmail: String, category: String): Flow<List<Expense>>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userEmail = :userEmail")
    fun getAllBudgetsFlow(userEmail: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userEmail = :userEmail")
    suspend fun getAllBudgets(userEmail: String): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE userEmail = :userEmail AND category = :category")
    suspend fun deleteBudgetByCategory(userEmail: String, category: String)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE userEmail = :userEmail ORDER BY nextDueDate ASC")
    fun getAllSubscriptionsFlow(userEmail: String): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE userEmail = :userEmail")
    suspend fun getAllSubscriptions(userEmail: String): List<Subscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history WHERE userEmail = :userEmail ORDER BY timestamp ASC")
    fun getChatHistoryFlow(userEmail: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_history WHERE userEmail = :userEmail ORDER BY timestamp ASC")
    suspend fun getChatHistory(userEmail: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_history WHERE userEmail = :userEmail")
    suspend fun clearHistory(userEmail: String)
}

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals WHERE userEmail = :userEmail ORDER BY deadline ASC")
    fun getAllGoalsFlow(userEmail: String): Flow<List<SavingGoal>>

    @Query("SELECT * FROM saving_goals WHERE userEmail = :userEmail")
    suspend fun getAllGoals(userEmail: String): List<SavingGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingGoal)
}

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family_members WHERE userEmail = :userEmail")
    fun getFamilyMembersFlow(userEmail: String): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE userEmail = :userEmail")
    suspend fun getAllFamilyMembers(userEmail: String): List<FamilyMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)
}
