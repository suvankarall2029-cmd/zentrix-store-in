package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object MyLearning : Screen("my_learning")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
    data object Support : Screen("support")

    data object CourseDetail : Screen("course_detail/{courseId}") {
        fun createRoute(courseId: String) = "course_detail/$courseId"
    }

    data object LessonPlayer : Screen("lesson_player/{courseId}/{lessonId}") {
        fun createRoute(courseId: String, lessonId: String) = "lesson_player/$courseId/$lessonId"
    }

    data object Payment : Screen("payment/{courseId}") {
        fun createRoute(courseId: String) = "payment/$courseId"
    }
}
