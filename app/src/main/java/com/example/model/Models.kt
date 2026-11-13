package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val firstName: String = "",
    val middleName: String? = null,
    val lastName: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val countryCode: String? = null,
    val photoUrl: String? = null,
    val language: String = "en",
    val age: Int = 0,
    val dateOfBirth: String? = null,
    val ageGroup: String = "",
    val purpose: String = "",
    val otherPurpose: String? = null,
    val interests: List<String> = emptyList(),
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val isVerified: Boolean = false,
    val status: String = "ACTIVE",
    val role: String = "USER",
    val registrationCompleted: Boolean = false,
    val authProvider: String = "GOOGLE",
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RegistrationDraft(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val email: String = "",
    val countryCode: String = "+91",
    val phoneLocal: String = "",
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val verificationId: String? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val dateOfBirth: String = "",
    val ageInput: String = "",
    val computedAge: Int? = null,
    val purpose: String = "",
    val otherPurpose: String = "",
    val interests: Set<String> = emptySet()
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val icon: String = "code",
    val courseCount: Long = 0L
)

@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleBn: String = "",
    val description: String = "",
    val descriptionBn: String = "",
    val instructor: String = "",
    val category: String = "development",
    val price: Double = 0.0,
    val isFree: Boolean = false,
    val duration: String = "0 hours",
    val level: String = "Beginner",
    val rating: Double = 5.0,
    val studentsCount: Long = 0L,
    val lessonsCount: Long = 0L,
    val published: Boolean = true,
    val featured: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val titleBn: String = "",
    val duration: String = "10 min",
    val order: Long = 1L,
    val videoUrl: String = "",
    val pdfUrl: String? = null,
    val isPreview: Boolean = false
)

@IgnoreExtraProperties
data class Enrollment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val completedLessonIds: List<String> = emptyList(),
    val progressPercent: Long = 0L,
    val lastLessonId: String? = null,
    val isPaid: Boolean = false,
    val orderId: String? = null,
    @ServerTimestamp val enrolledAt: Timestamp? = null,
    @ServerTimestamp val lastAccessedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val status: String = "SUCCESS",
    val paymentProvider: String = "Razorpay",
    val paymentMethod: String = "UPI",
    val transactionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val titleBn: String = "",
    val message: String = "",
    val messageBn: String = "",
    val type: String = "WELCOME",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleBn: String = "",
    val message: String = "",
    val messageBn: String = "",
    val priority: String = "NORMAL",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null
)
