package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.Course
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class ZentrixRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun observePublishedCourses_authenticatedUser_receivesCourses() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val courseId = "course_${UUID.randomUUID().toString().take(8)}"
        firestore.collection("courses").document(courseId).set(
            mapOf(
                "title" to "Kotlin Advanced",
                "instructor" to "Instructor",
                "category" to "cat_android",
                "isFree" to true,
                "published" to true
            )
        )

        val repository = ZentrixRepository(firestore)
        val courses = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedCourses().first { list -> list.any { it.id == courseId } }
        }
        assertTrue(courses.any { it.id == courseId })
    }

    @Test
    fun enrollInFreeCourse_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val course = Course(
            id = "test_course_${UUID.randomUUID().toString().take(6)}",
            title = "Test Free Course",
            price = 0.0,
            isFree = true,
            published = true
        )
        val repository = ZentrixRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.enrollInFreeCourse(course)
        }
        assertTrue(result.isSuccess)

        val enrollments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserEnrollments(uid).first()
        }
        assertTrue(enrollments.any { it.courseId == course.id })
    }

    @Test
    fun observeEnrollments_crossUserAccess_isPreventedByRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val course = Course(
            id = "course_${UUID.randomUUID().toString().take(6)}",
            title = "Alice Course",
            price = 0.0,
            isFree = true,
            published = true
        )
        val aliceRepo = ZentrixRepository(firestore)
        aliceRepo.enrollInFreeCourse(course)

        // Bob signs in and tries to query Alice's enrollments directly
        signInTestUser(BOB_EMAIL)
        val bobRepo = ZentrixRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                // Bob queries with Alice's userId -> must fail with PERMISSION_DENIED
                bobRepo.observeUserEnrollments(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user enrollment query")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun observePublishedCourses_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ZentrixRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublishedCourses().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated query")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun syncUserProfileOnLogin_authenticatedUser_createsAndUpdatesSafely() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ZentrixRepository(firestore)

        val firstSync = withTimeout(DEFAULT_TIMEOUT_MS) { repository.syncUserProfileOnLogin() }
        assertTrue(firstSync.isSuccess)

        val secondSync = withTimeout(DEFAULT_TIMEOUT_MS) { repository.syncUserProfileOnLogin() }
        assertTrue(secondSync.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals(uid, profile?.userId)
        assertTrue(!profile?.displayName.isNullOrBlank())
    }

    private companion object {
        const val ALICE_EMAIL = "alice@zentrixstore.app"
        const val BOB_EMAIL = "bob@zentrixstore.app"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
