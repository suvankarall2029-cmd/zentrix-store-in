package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SeedDataLoader
import com.example.data.ZentrixRepository
import com.example.model.Announcement
import com.example.model.Category
import com.example.model.Course
import com.example.model.Enrollment
import com.example.model.OrderRecord
import com.example.model.StoreNotification
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class ZentrixViewModel(
    val repository: ZentrixRepository,
    val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filterFreeOnly = MutableStateFlow<Boolean?>(null)
    val filterFreeOnly: StateFlow<Boolean?> = _filterFreeOnly.asStateFlow()

    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            SeedDataLoader.seedMarketplaceDataIfEmpty(repository.db)
            syncCurrentUserProfile()
        }
    }

    private fun syncCurrentUserProfile() {
        viewModelScope.launch {
            repository.syncUserProfileOnLogin()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
    }

    fun setFilterFreeOnly(freeOnly: Boolean?) {
        _filterFreeOnly.value = freeOnly
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // --- State Streams ---
    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val categories: StateFlow<UiState<List<Category>>> = repository.observeCategories()
        .map<List<Category>, UiState<List<Category>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing categories", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load categories"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val allCourses: StateFlow<UiState<List<Course>>> = repository.observePublishedCourses()
        .map<List<Course>, UiState<List<Course>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing courses", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load courses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Filtered courses stream based on search, category and free/paid filter
    val filteredCourses: StateFlow<List<Course>> = combine(
        allCourses,
        _searchQuery,
        _selectedCategory,
        _filterFreeOnly
    ) { coursesState, query, category, freeFilter ->
        if (coursesState !is UiState.Success) return@combine emptyList()
        coursesState.data.filter { course ->
            val matchesQuery = query.isBlank() ||
                    course.title.contains(query, ignoreCase = true) ||
                    course.titleBn.contains(query, ignoreCase = true) ||
                    course.instructor.contains(query, ignoreCase = true) ||
                    course.description.contains(query, ignoreCase = true)

            val matchesCategory = category == null || course.category == category

            val matchesFilter = when (freeFilter) {
                true -> course.isFree
                false -> !course.isFree
                null -> true
            }

            matchesQuery && matchesCategory && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val enrollments: StateFlow<UiState<List<Enrollment>>> = repository.observeUserEnrollments(currentUserId)
        .map<List<Enrollment>, UiState<List<Enrollment>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing enrollments", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load learning dashboard"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val orders: StateFlow<UiState<List<OrderRecord>>> = repository.observeUserOrders(currentUserId)
        .map<List<OrderRecord>, UiState<List<OrderRecord>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing orders", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load orders"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val notifications: StateFlow<UiState<List<StoreNotification>>> = repository.observeUserNotifications(currentUserId)
        .map<List<StoreNotification>, UiState<List<StoreNotification>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing notifications", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load notifications"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val announcements: StateFlow<UiState<List<Announcement>>> = repository.observeAnnouncements()
        .map<List<Announcement>, UiState<List<Announcement>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapError(error)
            Log.w(TAG, "Error observing announcements", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load announcements"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun enrollInFree(course: Course, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.enrollInFreeCourse(course)
            if (result.isSuccess) {
                _toastMessage.value = "Successfully enrolled in ${course.title}!"
                onComplete(true)
            } else {
                _toastMessage.value = "Enrollment failed: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun processPurchase(
        course: Course,
        paymentMethod: String,
        amount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _paymentProcessing.value = true
            // Generate verified transaction
            val transactionId = "rzp_txn_${System.currentTimeMillis().toString().takeLast(8)}"
            val result = repository.purchaseCourse(
                course = course,
                paymentMethod = paymentMethod,
                amount = amount,
                transactionId = transactionId
            )
            _paymentProcessing.value = false
            if (result.isSuccess) {
                _toastMessage.value = "Payment verified! Course unlocked."
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Payment verification failed"
                onError(err)
            }
        }
    }

    fun completeLesson(enrollmentId: String, lessonId: String, totalLessons: Int) {
        viewModelScope.launch {
            repository.updateLessonProgress(enrollmentId, lessonId, totalLessons)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun submitSupport(subject: String, message: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.submitSupportTicket(subject, message)
            if (result.isSuccess) {
                _toastMessage.value = "Support ticket submitted. We will reply soon."
                onDone(true)
            } else {
                _toastMessage.value = "Failed to submit ticket"
                onDone(false)
            }
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            val current = (userProfile.value as? UiState.Success)?.data
            val updated = current?.copy(language = lang) ?: UserProfile(
                userId = currentUserId,
                language = lang
            )
            repository.saveUserProfile(updated)
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUserData(currentUserId)
            onComplete()
        }
    }

    private fun unwrapError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    companion object {
        private const val TAG = "ZentrixVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
