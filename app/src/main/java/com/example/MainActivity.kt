package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.credentials.CredentialManager
import com.example.data.ZentrixRepository
import com.example.ui.components.LanguageState
import com.example.ui.components.ProvideAppLanguage
import com.example.ui.components.ZentrixBottomBar
import com.example.ui.components.ZentrixTopBar
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.navigation.Screen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LessonPlayerScreen
import com.example.ui.screens.MyLearningScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.theme.ZentrixTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZentrixTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var hasAttemptedAutoSignIn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null && !hasAttemptedAutoSignIn) {
            hasAttemptedAutoSignIn = true
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Stay on AuthScreen for interactive sign-in
                },
                scope = coroutineScope
            )
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainFeatureApp(currentUserId = uid)
        }
    }
}

@Composable
fun MainFeatureApp(currentUserId: String) {
    val context = LocalContext.current
    val languageState = remember { LanguageState("en") }

    val viewModel: ZentrixViewModel = viewModel(
        key = currentUserId,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                return ZentrixViewModel(ZentrixRepository(db), currentUserId) as T
            }
        }
    )

    val userProfileState by viewModel.userProfile.collectAsState()
    val notificationsState by viewModel.notifications.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Sync saved language
    LaunchedEffect(userProfileState) {
        if (userProfileState is UiState.Success) {
            val lang = (userProfileState as UiState.Success).data?.language
            if (!lang.isNullOrBlank()) {
                languageState.setLanguage(lang)
            }
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val unreadNotifs = when (val state = notificationsState) {
        is UiState.Success -> state.data.count { !it.read }
        else -> 0
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val isTopLevelScreen = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.MyLearning.route,
        Screen.Notifications.route,
        Screen.Profile.route
    )

    ProvideAppLanguage(languageState = languageState) {
        Scaffold(
            topBar = {
                if (isTopLevelScreen) {
                    ZentrixTopBar(
                        unreadNotificationCount = unreadNotifs,
                        currentLanguage = languageState.currentLanguage,
                        onLanguageToggle = {
                            val nextLang = if (languageState.isBengali) "en" else "bn"
                            languageState.setLanguage(nextLang)
                            viewModel.updateLanguage(nextLang)
                        },
                        onNotificationClick = {
                            navController.navigate(Screen.Notifications.route) {
                                launchSingleTop = true
                            }
                        },
                        onProfileClick = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelScreen) {
                    ZentrixBottomBar(
                        currentRoute = currentRoute,
                        isBengali = languageState.isBengali,
                        unreadNotifications = unreadNotifs,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onCourseClick = { courseId ->
                                navController.navigate(Screen.CourseDetail.createRoute(courseId))
                            },
                            onMyLearningClick = {
                                navController.navigate(Screen.MyLearning.route)
                            },
                            onNotificationsClick = {
                                navController.navigate(Screen.Notifications.route)
                            },
                            onProfileClick = {
                                navController.navigate(Screen.Profile.route)
                            },
                            onExploreCategory = { catId ->
                                viewModel.selectCategory(catId)
                                navController.navigate(Screen.Explore.route)
                            }
                        )
                    }

                    composable(Screen.Explore.route) {
                        ExploreScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onCourseClick = { courseId ->
                                navController.navigate(Screen.CourseDetail.createRoute(courseId))
                            }
                        )
                    }

                    composable(Screen.MyLearning.route) {
                        MyLearningScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onCourseClick = { courseId ->
                                navController.navigate(Screen.CourseDetail.createRoute(courseId))
                            },
                            onExploreClick = {
                                navController.navigate(Screen.Explore.route)
                            },
                            onResumeLesson = { courseId, lastLessonId ->
                                val targetLessonId = lastLessonId.orEmpty()
                                navController.navigate(
                                    Screen.LessonPlayer.createRoute(courseId, targetLessonId.ifBlank { "first" })
                                )
                            }
                        )
                    }

                    composable(Screen.Notifications.route) {
                        NotificationsScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onLanguageChange = { lang ->
                                languageState.setLanguage(lang)
                            },
                            onNavigateMyLearning = {
                                navController.navigate(Screen.MyLearning.route)
                            },
                            onNavigateNotifications = {
                                navController.navigate(Screen.Notifications.route)
                            },
                            onNavigateSupport = {
                                navController.navigate(Screen.Support.route)
                            }
                        )
                    }

                    composable(Screen.Support.route) {
                        SupportScreen(
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.CourseDetail.route,
                        arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
                        CourseDetailScreen(
                            courseId = courseId,
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onBack = { navController.popBackStack() },
                            onNavigateToPayment = { cId ->
                                navController.navigate(Screen.Payment.createRoute(cId))
                            },
                            onNavigateToLesson = { cId, lessonId ->
                                navController.navigate(Screen.LessonPlayer.createRoute(cId, lessonId))
                            }
                        )
                    }

                    composable(
                        route = Screen.LessonPlayer.route,
                        arguments = listOf(
                            navArgument("courseId") { type = NavType.StringType },
                            navArgument("lessonId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
                        val lessonId = backStackEntry.arguments?.getString("lessonId").orEmpty()
                        LessonPlayerScreen(
                            courseId = courseId,
                            initialLessonId = lessonId,
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Payment.route,
                        arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
                        PaymentScreen(
                            courseId = courseId,
                            viewModel = viewModel,
                            isBengali = languageState.isBengali,
                            onBack = { navController.popBackStack() },
                            onPaymentSuccess = {
                                navController.popBackStack()
                                navController.navigate(Screen.MyLearning.route)
                            }
                        )
                    }
                }
            }
        }
    }
}
