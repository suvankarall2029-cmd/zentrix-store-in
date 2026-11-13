package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Announcement
import com.example.model.Category
import com.example.model.Course
import com.example.model.Enrollment
import com.example.model.Lesson
import com.example.model.OrderRecord
import com.example.model.StoreNotification
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ZentrixRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Zentrix Learner" },
                    "email" to profile.email.ifBlank { auth.currentUser?.email.orEmpty() },
                    "photoUrl" to profile.photoUrl,
                    "language" to profile.language,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(data).await()
            } else {
                val updateData = mapOf(
                    "displayName" to profile.displayName,
                    "photoUrl" to profile.photoUrl,
                    "language" to profile.language,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateData).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Categories ---
    fun observeCategories(): Flow<List<Category>> {
        return db.collection("categories")
            .snapshots()
            .map { it.toObjects(Category::class.java) }
    }

    // --- Courses ---
    fun observePublishedCourses(): Flow<List<Course>> {
        return db.collection("courses")
            .whereEqualTo("published", true)
            .snapshots()
            .map { it.toObjects(Course::class.java) }
    }

    suspend fun getCourse(courseId: String): Course? {
        val doc = db.collection("courses").document(courseId).get().await()
        return doc.toObject(Course::class.java)
    }

    // --- Lessons ---
    fun observeLessons(courseId: String): Flow<List<Lesson>> {
        return db.collection("lessons")
            .whereEqualTo("courseId", courseId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Lesson::class.java).sortedBy { it.order }
            }
    }

    // --- Enrollments ---
    fun observeUserEnrollments(userId: String): Flow<List<Enrollment>> {
        return db.collection("enrollments")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(Enrollment::class.java) }
    }

    suspend fun getEnrollment(userId: String, courseId: String): Enrollment? {
        val snapshot = db.collection("enrollments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", courseId)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(Enrollment::class.java)
    }

    suspend fun enrollInFreeCourse(course: Course): Result<String> {
        val uid = requireUserId()
        val enrollmentId = "enr_${uid}_${course.id}"
        val docRef = db.collection("enrollments").document(enrollmentId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "courseId" to course.id,
                "courseTitle" to course.title,
                "completedLessonIds" to emptyList<String>(),
                "progressPercent" to 0L,
                "lastLessonId" to null,
                "isPaid" to false,
                "orderId" to null,
                "enrolledAt" to FieldValue.serverTimestamp(),
                "lastAccessedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()

            // Send enrollment confirmation notification
            createNotification(
                title = "Enrolled: ${course.title}",
                titleBn = "ভর্তি সফল: ${course.titleBn.ifBlank { course.title }}",
                message = "You have successfully enrolled in ${course.title}. Start learning today!",
                messageBn = "${course.titleBn.ifBlank { course.title }}-এ আপনার ভর্তি নিশ্চিত হয়েছে। আজই শেখা শুরু করুন!",
                type = "WELCOME"
            )

            Result.success(enrollmentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun purchaseCourse(
        course: Course,
        paymentMethod: String,
        amount: Double,
        transactionId: String
    ): Result<String> {
        val uid = requireUserId()
        val orderId = "ord_${UUID.randomUUID().toString().take(12)}"
        val orderRef = db.collection("orders").document(orderId)

        return try {
            val orderPayload = mapOf(
                "userId" to uid,
                "courseId" to course.id,
                "courseTitle" to course.title,
                "amount" to amount,
                "currency" to "INR",
                "status" to "SUCCESS",
                "paymentProvider" to "Razorpay",
                "paymentMethod" to paymentMethod,
                "transactionId" to transactionId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            orderRef.set(orderPayload).await()

            // Unlock course enrollment
            val enrollmentId = "enr_${uid}_${course.id}"
            val enrollRef = db.collection("enrollments").document(enrollmentId)
            val enrollPayload = mapOf(
                "userId" to uid,
                "courseId" to course.id,
                "courseTitle" to course.title,
                "completedLessonIds" to emptyList<String>(),
                "progressPercent" to 0L,
                "lastLessonId" to null,
                "isPaid" to true,
                "orderId" to orderId,
                "enrolledAt" to FieldValue.serverTimestamp(),
                "lastAccessedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            enrollRef.set(enrollPayload).await()

            // Send payment confirmation notification
            createNotification(
                title = "Payment Successful: ₹${amount.toInt()}",
                titleBn = "পেমেন্ট সফল হয়েছে: ₹${amount.toInt()}",
                message = "Your payment for ${course.title} was verified. Transaction ID: $transactionId",
                messageBn = "${course.titleBn.ifBlank { course.title }}-এর পেমেন্ট সফল হয়েছে। লেনদেন নম্বর: $transactionId",
                type = "PURCHASE"
            )

            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLessonProgress(
        enrollmentId: String,
        lessonId: String,
        totalLessons: Int
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("enrollments").document(enrollmentId)
        return try {
            val snapshot = docRef.get().await()
            val existingEnrollment = snapshot.toObject(Enrollment::class.java)
            if (existingEnrollment != null && existingEnrollment.userId == uid) {
                val currentCompleted = existingEnrollment.completedLessonIds.toMutableSet()
                currentCompleted.add(lessonId)
                val newProgress = if (totalLessons > 0) {
                    ((currentCompleted.size.toDouble() / totalLessons) * 100).toLong().coerceIn(0L, 100L)
                } else 0L

                val updates = mapOf(
                    "completedLessonIds" to currentCompleted.toList(),
                    "progressPercent" to newProgress,
                    "lastLessonId" to lessonId,
                    "lastAccessedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Orders ---
    fun observeUserOrders(userId: String): Flow<List<OrderRecord>> {
        return db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(OrderRecord::class.java) }
    }

    // --- Notifications ---
    fun observeUserNotifications(userId: String): Flow<List<StoreNotification>> {
        return db.collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(StoreNotification::class.java) }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            docRef.update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        title: String,
        titleBn: String,
        message: String,
        messageBn: String,
        type: String
    ): Result<String> {
        val uid = requireUserId()
        val notifId = "notif_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("notifications").document(notifId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "title" to title,
                "titleBn" to titleBn,
                "message" to message,
                "messageBn" to messageBn,
                "type" to type,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(notifId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Announcements ---
    fun observeAnnouncements(): Flow<List<Announcement>> {
        return db.collection("announcements")
            .snapshots()
            .map { it.toObjects(Announcement::class.java) }
    }

    // --- Support Tickets ---
    suspend fun submitSupportTicket(subject: String, message: String): Result<String> {
        val uid = requireUserId()
        val ticketId = "tkt_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("supportTickets").document(ticketId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "userEmail" to (auth.currentUser?.email ?: "user@zentrixstore.app"),
                "subject" to subject,
                "message" to message,
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(ticketId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Account Deletion ---
    suspend fun deleteUserData(userId: String): Result<Unit> {
        return try {
            val enrollments = db.collection("enrollments").whereEqualTo("userId", userId).get().await()
            for (doc in enrollments.documents) {
                // Delete user's notifications and profile
            }
            val notifs = db.collection("notifications").whereEqualTo("userId", userId).get().await()
            for (doc in notifs.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }
}
