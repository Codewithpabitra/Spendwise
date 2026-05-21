package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val role: String,
    val avatarEmoji: String
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val date: Long,
    val isRecurring: Boolean = false,
    val familyGroupName: String? = null,
    val userEmail: String = "test@spendwise.ai"
)

@Entity(tableName = "budgets", primaryKeys = ["category", "userEmail"])
data class Budget(
    val category: String, // "Total" or category name
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val userEmail: String = "test@spendwise.ai"
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String,
    val nextDueDate: Long,
    val frequency: String, // "Monthly", "Yearly"
    val isAutoRenew: Boolean = true,
    val userEmail: String = "test@spendwise.ai"
)

@Entity(tableName = "chat_history")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = "test@spendwise.ai"
)

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long,
    val userEmail: String = "test@spendwise.ai"
)

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "Owner", "Member", "Partner"
    val inviteStatus: String = "Joined", // "Pending", "Joined"
    val userEmail: String = "test@spendwise.ai"
)
