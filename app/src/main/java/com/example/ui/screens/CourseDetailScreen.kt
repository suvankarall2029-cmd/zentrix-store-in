package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Course
import com.example.model.Lesson
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSuccess
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel

@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onBack: () -> Unit,
    onNavigateToPayment: (String) -> Unit,
    onNavigateToLesson: (String, String) -> Unit
) {
    BackHandler { onBack() }

    var course by remember { mutableStateOf<Course?>(null) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isEnrolling by remember { mutableStateOf(false) }

    val enrollmentsState by viewModel.enrollments.collectAsState()
    val isEnrolled = when (val state = enrollmentsState) {
        is UiState.Success -> state.data.any { it.courseId == courseId }
        else -> false
    }

    LaunchedEffect(courseId) {
        val loadedCourse = viewModel.repository.getCourse(courseId)
        course = loadedCourse
        isLoading = false
    }

    LaunchedEffect(courseId) {
        viewModel.repository.observeLessons(courseId).collect {
            lessons = it
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZentrixSurface)
                    .border(width = 0.5.dp, color = ZentrixCardBorder)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("course_detail_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZentrixTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "কোর্সের বিবরণ" else "Course Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
            }
        },
        bottomBar = {
            if (course != null) {
                val currentCourse = course!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZentrixSurface)
                        .border(width = 0.5.dp, color = ZentrixCardBorder)
                        .padding(16.dp)
                ) {
                    if (isEnrolled) {
                        Button(
                            onClick = {
                                val firstLesson = lessons.firstOrNull()
                                if (firstLesson != null) {
                                    onNavigateToLesson(courseId, firstLesson.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_continue_learning")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.continue_learning),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZentrixTextPrimary
                            )
                        }
                    } else if (currentCourse.isFree) {
                        Button(
                            onClick = {
                                isEnrolling = true
                                viewModel.enrollInFree(currentCourse) {
                                    isEnrolling = false
                                }
                            },
                            enabled = !isEnrolling,
                            colors = ButtonDefaults.buttonColors(containerColor = ZentrixSuccess),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_enroll_free")
                        ) {
                            if (isEnrolling) {
                                CircularProgressIndicator(color = ZentrixTextPrimary, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = stringResource(R.string.enroll_free),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ZentrixTextPrimary
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onNavigateToPayment(courseId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_buy_course")
                        ) {
                            Text(
                                text = "${stringResource(R.string.buy_now)} • ₹${currentCourse.price.toInt()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZentrixTextPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZentrixPrimaryBlue)
            }
        } else if (course == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Course not found",
                    color = ZentrixTextSecondary
                )
            }
        } else {
            val c = course!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                        border = BorderStroke(1.dp, ZentrixCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Category & Free/Paid badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ZentrixSurfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = c.level.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = ZentrixSecondaryBlue
                                    )
                                }

                                if (c.isFree) {
                                    Text(
                                        text = "FREE ENROLLMENT",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ZentrixSuccess
                                    )
                                } else {
                                    Text(
                                        text = "₹${c.price.toInt()}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ZentrixSecondaryBlue
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val title = if (isBengali && c.titleBn.isNotBlank()) c.titleBn else c.title
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ZentrixTextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "By ${c.instructor}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ZentrixTextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem(label = "Rating", value = "★ ${c.rating}")
                                StatItem(label = "Duration", value = c.duration)
                                StatItem(label = "Students", value = "${c.studentsCount}+")
                                StatItem(label = "Lessons", value = "${c.lessonsCount}")
                            }
                        }
                    }
                }

                // Description Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                        border = BorderStroke(1.dp, ZentrixCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.course_overview),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZentrixTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val desc = if (isBengali && c.descriptionBn.isNotBlank()) c.descriptionBn else c.description
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ZentrixTextSecondary,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // Curriculum & Lessons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.curriculum),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                        Text(
                            text = "${lessons.size} lessons",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZentrixTextSecondary
                        )
                    }
                }

                if (lessons.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Curriculum lessons are being scheduled.",
                                color = ZentrixTextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(lessons) { lesson ->
                        val canAccess = isEnrolled || lesson.isPreview
                        val lessonTitle = if (isBengali && lesson.titleBn.isNotBlank()) lesson.titleBn else lesson.title

                        Card(
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                            border = BorderStroke(1.dp, ZentrixCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canAccess) {
                                    if (canAccess) {
                                        onNavigateToLesson(courseId, lesson.id)
                                    }
                                }
                                .testTag("lesson_item_${lesson.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (canAccess) ZentrixPrimaryBlue.copy(alpha = 0.2f) else ZentrixSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (canAccess) Icons.Default.PlayCircleOutline else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (canAccess) ZentrixSecondaryBlue else ZentrixTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lessonTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (canAccess) ZentrixTextPrimary else ZentrixTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lesson.duration,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ZentrixTextSecondary
                                        )
                                        if (lesson.pdfUrl != null) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = ZentrixSecondaryBlue,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PDF notes included",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ZentrixSecondaryBlue
                                            )
                                        }
                                    }
                                }

                                if (lesson.isPreview && !isEnrolled) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ZentrixSecondaryBlue.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "PREVIEW",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ZentrixSecondaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ZentrixTextSecondary
        )
    }
}
