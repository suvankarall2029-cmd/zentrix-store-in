package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Course
import com.example.ui.components.CourseCard
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel

@Composable
fun HomeScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onCourseClick: (String) -> Unit,
    onMyLearningClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onExploreCategory: (String) -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val coursesState by viewModel.allCourses.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val enrollmentsState by viewModel.enrollments.collectAsState()
    val announcementsState by viewModel.announcements.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val enrolledCourseIds = when (val state = enrollmentsState) {
        is UiState.Success -> state.data.map { it.courseId }.toSet()
        else -> emptySet()
    }

    val userName = when (val state = userProfileState) {
        is UiState.Success -> {
            val p = state.data
            p?.firstName?.takeIf { it.isNotBlank() }
                ?: p?.displayName?.takeIf { it.isNotBlank() }
                ?: "Learner"
        }
        else -> "Learner"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Welcome Header & Shortcuts
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZentrixSurface)
                    .border(width = 0.5.dp, color = ZentrixCardBorder)
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isBengali) "স্বাগতম, $userName!" else "Welcome back, $userName!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBengali) "নতুন কিছু শিখুন এবং আপনার লক্ষ্য অর্জন করুন" else "Continue your learning journey with world-class curriculum.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZentrixTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = ZentrixTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ZentrixSecondaryBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = ZentrixTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZentrixBackground,
                        unfocusedContainerColor = ZentrixBackground,
                        focusedBorderColor = ZentrixPrimaryBlue,
                        unfocusedBorderColor = ZentrixCardBorder,
                        focusedTextColor = ZentrixTextPrimary,
                        unfocusedTextColor = ZentrixTextPrimary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dashboard Quick Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickShortcutCard(
                        title = if (isBengali) "আমার শিক্ষা" else "My Learning",
                        icon = Icons.Default.School,
                        onClick = onMyLearningClick,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    QuickShortcutCard(
                        title = if (isBengali) "বিজ্ঞপ্তি" else "Notifications",
                        icon = Icons.Default.Notifications,
                        onClick = onNotificationsClick,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    QuickShortcutCard(
                        title = if (isBengali) "প্রোফাইল" else "Profile",
                        icon = Icons.Default.Person,
                        onClick = onProfileClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Platform Announcements Banner
        if (announcementsState is UiState.Success) {
            val announcements = (announcementsState as UiState.Success).data
            if (announcements.isNotEmpty()) {
                item {
                    val latest = announcements.first()
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = ZentrixSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZentrixPrimaryBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcement",
                                tint = ZentrixSecondaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBengali && latest.titleBn.isNotBlank()) latest.titleBn else latest.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = ZentrixTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBengali && latest.messageBn.isNotBlank()) latest.messageBn else latest.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = ZentrixTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Categories Row
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                when (val state = categoriesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ZentrixPrimaryBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    is UiState.Success -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data) { cat ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ZentrixSurface)
                                        .border(1.dp, ZentrixCardBorder, RoundedCornerShape(20.dp))
                                        .clickable { onExploreCategory(cat.id) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("category_chip_${cat.id}")
                                ) {
                                    Text(
                                        text = if (isBengali && cat.nameBn.isNotBlank()) cat.nameBn else cat.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ZentrixTextPrimary
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {}
                }
            }
        }

        // Courses Sections
        when (val state = coursesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZentrixPrimaryBlue)
                    }
                }
            }
            is UiState.Success -> {
                val courses = state.data
                val featured = courses.filter { it.featured }
                val freeCourses = courses.filter { it.isFree }
                val paidCourses = courses.filter { !it.isFree }

                // 1. Featured Courses
                if (featured.isNotEmpty()) {
                    item {
                        CourseSectionHeader(
                            title = stringResource(R.string.featured_courses),
                            count = featured.size
                        )
                    }
                    items(featured) { course ->
                        CourseCard(
                            course = course,
                            isBengali = isBengali,
                            isEnrolled = enrolledCourseIds.contains(course.id),
                            onClick = { onCourseClick(course.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // 2. Free Courses Section
                if (freeCourses.isNotEmpty()) {
                    item {
                        CourseSectionHeader(
                            title = stringResource(R.string.free_courses),
                            count = freeCourses.size
                        )
                    }
                    items(freeCourses) { course ->
                        CourseCard(
                            course = course,
                            isBengali = isBengali,
                            isEnrolled = enrolledCourseIds.contains(course.id),
                            onClick = { onCourseClick(course.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // 3. Premium Courses Section
                if (paidCourses.isNotEmpty()) {
                    item {
                        CourseSectionHeader(
                            title = stringResource(R.string.paid_courses),
                            count = paidCourses.size
                        )
                    }
                    items(paidCourses) { course ->
                        CourseCard(
                            course = course,
                            isBengali = isBengali,
                            isEnrolled = enrolledCourseIds.contains(course.id),
                            onClick = { onCourseClick(course.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = state.message,
                        color = ZentrixTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = ZentrixSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZentrixCardBorder),
        modifier = modifier
            .clickable { onClick() }
            .testTag("shortcut_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ZentrixSecondaryBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = ZentrixTextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CourseSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Text(
            text = "$count available",
            style = MaterialTheme.typography.labelMedium,
            color = ZentrixTextSecondary
        )
    }
}
