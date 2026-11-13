package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Announcement
import com.example.model.Category
import com.example.model.Course
import com.example.model.Enrollment
import com.example.model.Lesson
import com.example.model.OrderRecord
import com.example.model.RegistrationDraft
import com.example.model.StoreNotification
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
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
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun isBootstrappedAdminSession(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified &&
            user.email.equals(RegistrationConstants.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
    }

    suspend fun syncUserProfileOnLogin(): Result<Unit> {
        val uid = requireUserId()
        val currentUser = auth.currentUser
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val resolvedName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Zentrix Learner"
            val resolvedEmail = currentUser?.email?.takeIf { it.isNotBlank() } ?: "$uid@zentrixstore.app"
            val resolvedPhone = currentUser?.phoneNumber?.takeIf { it.isNotBlank() }
            val resolvedPhoto = currentUser?.photoUrl?.toString()
            val isEmailVerified = currentUser?.isEmailVerified == true
            val isPhoneVerified = !resolvedPhone.isNullOrBlank()
            val publicUserId = RegistrationValidator.generatePublicUserId(uid)

            if (!snapshot.exists()) {
                val nameParts = resolvedName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                val firstName = nameParts.firstOrNull().orEmpty()
                val lastName = if (nameParts.size > 1) nameParts.last() else ""
                val middleName = if (nameParts.size > 2) nameParts.subList(1, nameParts.size - 1).joinToString(" ") else null

                val data = mapOf(
                    "userId" to uid,
                    "publicUserId" to publicUserId,
                    "displayName" to resolvedName,
                    "firstName" to firstName,
                    "middleName" to middleName,
                    "lastName" to lastName,
                    "fullName" to resolvedName,
                    "email" to resolvedEmail,
                    "phoneNumber" to resolvedPhone,
                    "photoUrl" to resolvedPhoto,
                    "language" to "en",
                    "emailVerified" to isEmailVerified,
                    "phoneVerified" to isPhoneVerified,
                    "isVerified" to (isEmailVerified || isPhoneVerified),
                    "status" to "ACTIVE",
                    "role" to "USER",
                    "registrationCompleted" to false,
                    "authProvider" to "GOOGLE",
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(data).await()
            } else {
                val existingFullName = snapshot.getString("fullName")
                val existingPublicId = snapshot.getString("publicUserId")
                val existingEmailVerified = snapshot.getBoolean("emailVerified") ?: false
                val existingPhoneVerified = snapshot.getBoolean("phoneVerified") ?: false
                val mergedEmailVerified = existingEmailVerified || isEmailVerified
                val mergedPhoneVerified = existingPhoneVerified || isPhoneVerified

                val updateData = mutableMapOf<String, Any>(
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "emailVerified" to mergedEmailVerified,
                    "phoneVerified" to mergedPhoneVerified,
                    "isVerified" to (mergedEmailVerified || mergedPhoneVerified)
                )
                if (existingPublicId.isNullOrBlank()) {
                    updateData["publicUserId"] = publicUserId
                }
                if (existingFullName.isNullOrBlank() && !currentUser?.displayName.isNullOrBlank()) {
                    updateData["displayName"] = currentUser!!.displayName!!
                }
                if (!resolvedPhoto.isNullOrBlank()) {
                    updateData["photoUrl"] = resolvedPhoto
                }
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

    suspend fun checkContactAvailable(
        contactType: String,
        normalizedValue: String,
        currentUid: String?
    ): Result<Boolean> {
        if (normalizedValue.isBlank()) return Result.success(true)
        if (auth.currentUser == null) return Result.success(true)
        val lockId = RegistrationValidator.contactLockDocumentId(contactType, normalizedValue)
        val docRef = db.collection("contactLocks").document(lockId)
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                Result.success(true)
            } else {
                val ownerUid = doc.getString("userId")
                Result.success(ownerUid == null || ownerUid == currentUid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeRegistration(draft: RegistrationDraft): Result<UserProfile> {
        val uid = requireUserId()
        val currentUser = auth.currentUser

        val step1 = RegistrationValidator.validateStep1(draft.firstName, draft.middleName, draft.lastName)
        if (!step1.isValid) {
            return Result.failure(IllegalArgumentException(step1.firstNameError ?: step1.lastNameError ?: "Invalid name"))
        }

        val step2 = RegistrationValidator.validateStep2(draft.email, draft.countryCode, draft.phoneLocal)
        if (!step2.isValid) {
            return Result.failure(
                IllegalArgumentException(step2.generalError ?: step2.emailError ?: step2.phoneError ?: "Invalid contact info")
            )
        }

        val step4 = RegistrationValidator.validateStep4(
            age = draft.computedAge,
            purpose = draft.purpose,
            otherPurpose = draft.otherPurpose,
            interests = draft.interests
        )
        if (!step4.isValid || step4.resolvedAge == null) {
            return Result.failure(
                IllegalArgumentException(step4.ageError ?: step4.purposeError ?: step4.interestsError ?: "Invalid profile details")
            )
        }

        val formattedFirst = RegistrationValidator.formatPersonName(draft.firstName)
        val formattedMiddle = RegistrationValidator.formatPersonName(draft.middleName).takeIf { it.isNotBlank() }
        val formattedLast = RegistrationValidator.formatPersonName(draft.lastName)
        val fullName = RegistrationValidator.buildFullName(formattedFirst, formattedMiddle.orEmpty(), formattedLast)

        val resolvedEmail = draft.email.trim().lowercase().ifBlank {
            currentUser?.email?.trim()?.lowercase().orEmpty()
        }
        val normalizedPhone = if (draft.phoneLocal.isNotBlank()) {
            RegistrationValidator.normalizePhoneNumber(draft.countryCode, draft.phoneLocal)
        } else {
            currentUser?.phoneNumber?.takeIf { it.isNotBlank() }
        }

        val emailVerified = draft.emailVerified || (currentUser?.isEmailVerified == true && resolvedEmail.isNotBlank())
        val phoneVerified = draft.phoneVerified || (!currentUser?.phoneNumber.isNullOrBlank())
        val isVerified = emailVerified || phoneVerified
        val publicUserId = RegistrationValidator.generatePublicUserId(uid)

        // Check and acquire duplicate contact locks
        if (resolvedEmail.isNotBlank()) {
            val emailAvailable = checkContactAvailable("EMAIL", resolvedEmail, uid)
            if (emailAvailable.getOrNull() == false) {
                return Result.failure(IllegalStateException("This email address is already registered to another account."))
            }
        }
        if (!normalizedPhone.isNullOrBlank()) {
            val phoneAvailable = checkContactAvailable("PHONE", normalizedPhone, uid)
            if (phoneAvailable.getOrNull() == false) {
                return Result.failure(IllegalStateException("This mobile number is already registered to another account."))
            }
        }

        val docRef = db.collection("users").document(uid)
        return try {
            // Claim contact locks
            if (resolvedEmail.isNotBlank()) {
                val emailLockId = RegistrationValidator.contactLockDocumentId("EMAIL", resolvedEmail)
                val emailLockRef = db.collection("contactLocks").document(emailLockId)
                if (!emailLockRef.get().await().exists()) {
                    emailLockRef.set(
                        mapOf(
                            "userId" to uid,
                            "contactType" to "EMAIL",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (!normalizedPhone.isNullOrBlank()) {
                val phoneLockId = RegistrationValidator.contactLockDocumentId("PHONE", normalizedPhone)
                val phoneLockRef = db.collection("contactLocks").document(phoneLockId)
                if (!phoneLockRef.get().await().exists()) {
                    phoneLockRef.set(
                        mapOf(
                            "userId" to uid,
                            "contactType" to "PHONE",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingSnapshot = docRef.get().await()
            val resolvedPhoto = currentUser?.photoUrl?.toString()
            val commonFields = mutableMapOf<String, Any?>(
                "publicUserId" to publicUserId,
                "displayName" to fullName,
                "firstName" to formattedFirst,
                "middleName" to formattedMiddle,
                "lastName" to formattedLast,
                "fullName" to fullName,
                "email" to resolvedEmail,
                "phoneNumber" to normalizedPhone,
                "countryCode" to if (!normalizedPhone.isNullOrBlank()) draft.countryCode else null,
                "photoUrl" to resolvedPhoto,
                "age" to step4.resolvedAge,
                "dateOfBirth" to draft.dateOfBirth.takeIf { it.isNotBlank() },
                "ageGroup" to step4.ageGroup,
                "purpose" to draft.purpose,
                "otherPurpose" to if (draft.purpose == "Other") draft.otherPurpose.trim() else null,
                "interests" to draft.interests.toList(),
                "emailVerified" to emailVerified,
                "phoneVerified" to phoneVerified,
                "isVerified" to isVerified,
                "registrationCompleted" to true,
                "authProvider" to if (phoneVerified && !emailVerified) "PHONE" else "GOOGLE",
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.toMutableMap()

            if (!existingSnapshot.exists()) {
                commonFields["userId"] = uid
                commonFields["language"] = "en"
                commonFields["role"] = "USER"
                commonFields["status"] = "ACTIVE"
                commonFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(commonFields).await()
            } else {
                docRef.update(commonFields).await()
            }

            // Update Firebase Auth profile display name cleanly (non-fatal)
            try {
                if (currentUser != null && currentUser.displayName != fullName) {
                    currentUser.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
                    ).await()
                }
            } catch (_: Exception) {
                // Non-critical if offline
            }

            // Welcome notification on completing registration
            createNotification(
                title = "Welcome to Zentrix Store, $formattedFirst!",
                titleBn = "জেনট্রিক্স স্টোরে স্বাগতম, $formattedFirst!",
                message = "Your Zentrix Store account ($publicUserId) is verified and active. Explore courses tailored for ${draft.purpose}.",
                messageBn = "আপনার জেনট্রিক্স স্টোর অ্যাকাউন্ট ($publicUserId) সফলভাবে সক্রিয় হয়েছে।",
                type = "WELCOME"
            )

            val savedProfile = docRef.get().await().toObject(UserProfile::class.java)
                ?: UserProfile(
                    id = uid,
                    userId = uid,
                    publicUserId = publicUserId,
                    displayName = fullName,
                    firstName = formattedFirst,
                    middleName = formattedMiddle,
                    lastName = formattedLast,
                    fullName = fullName,
                    email = resolvedEmail,
                    phoneNumber = normalizedPhone,
                    countryCode = draft.countryCode,
                    age = step4.resolvedAge,
                    dateOfBirth = draft.dateOfBirth.takeIf { it.isNotBlank() },
                    ageGroup = step4.ageGroup,
                    purpose = draft.purpose,
                    otherPurpose = draft.otherPurpose.takeIf { draft.purpose == "Other" },
                    interests = draft.interests.toList(),
                    emailVerified = emailVerified,
                    phoneVerified = phoneVerified,
                    isVerified = isVerified,
                    status = "ACTIVE",
                    role = "USER",
                    registrationCompleted = true
                )
            Result.success(savedProfile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val currentUser = auth.currentUser
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val resolvedName = profile.displayName.ifBlank {
                currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Zentrix Learner"
            }
            val resolvedEmail = profile.email.ifBlank {
                currentUser?.email?.takeIf { it.isNotBlank() } ?: "$uid@zentrixstore.app"
            }
            val resolvedPhoto = profile.photoUrl ?: currentUser?.photoUrl?.toString()

            if (!snapshot.exists()) {
                val data = mapOf(
                    "userId" to uid,
                    "publicUserId" to RegistrationValidator.generatePublicUserId(uid),
                    "displayName" to resolvedName,
                    "email" to resolvedEmail,
                    "photoUrl" to resolvedPhoto,
                    "language" to profile.language.ifBlank { "en" },
                    "role" to "USER",
                    "status" to "ACTIVE",
                    "registrationCompleted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(data).await()
            } else {
                val updateData = mapOf(
                    "displayName" to resolvedName,
                    "photoUrl" to resolvedPhoto,
                    "language" to profile.language.ifBlank { "en" },
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

    // --- Admin User Management ---
    fun observeAllUsers(): Flow<List<UserProfile>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun adminUpdateUserStatus(targetUserId: String, newStatus: String): Result<Unit> {
        requireUserId()
        require(newStatus in listOf("ACTIVE", "SUSPENDED")) { "Invalid status: $newStatus" }
        val docRef = db.collection("users").document(targetUserId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserVerification(targetUserId: String, verified: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("users").document(targetUserId)
        return try {
            docRef.update(
                mapOf(
                    "isVerified" to verified,
                    "emailVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserRole(targetUserId: String, targetEmail: String, newRole: String): Result<Unit> {
        val adminUid = requireUserId()
        require(newRole in listOf("USER", "ADMIN")) { "Invalid role: $newRole" }
        val userDocRef = db.collection("users").document(targetUserId)
        val adminRegistryRef = db.collection("admins").document(targetUserId)
        return try {
            userDocRef.update(
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (newRole == "ADMIN") {
                adminRegistryRef.set(
                    mapOf(
                        "userId" to targetUserId,
                        "email" to targetEmail,
                        "grantedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val existingAdmin = adminRegistryRef.get().await()
                if (existingAdmin.exists()) {
                    adminRegistryRef.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
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
            val userDoc = db.collection("users").document(userId).get().await()
            val email = userDoc.getString("email")
            val phone = userDoc.getString("phoneNumber")
            if (!email.isNullOrBlank()) {
                val emailLockId = RegistrationValidator.contactLockDocumentId("EMAIL", email)
                db.collection("contactLocks").document(emailLockId).delete().await()
            }
            if (!phone.isNullOrBlank()) {
                val phoneLockId = RegistrationValidator.contactLockDocumentId("PHONE", phone)
                db.collection("contactLocks").document(phoneLockId).delete().await()
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
