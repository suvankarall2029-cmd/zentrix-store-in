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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Course
import com.example.model.Enrollment
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
fun LessonPlayerScreen(
    courseId: String,
    initialLessonId: String,
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var currentLessonId by remember { mutableStateOf(initialLessonId) }
    var course by remember { mutableStateOf<Course?>(null) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var enrollment by remember { mutableStateOf<Enrollment?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var pdfOpenedNotice by remember { mutableStateOf<String?>(null) }

    val enrollmentsState by viewModel.enrollments.collectAsState()

    LaunchedEffect(courseId) {
        course = viewModel.repository.getCourse(courseId)
    }

    LaunchedEffect(courseId) {
        viewModel.repository.observeLessons(courseId).collect {
            lessons = it
            if (currentLessonId.isBlank() && it.isNotEmpty()) {
                currentLessonId = it.first().id
            }
        }
    }

    LaunchedEffect(enrollmentsState, courseId) {
        if (enrollmentsState is UiState.Success) {
            val list = (enrollmentsState as UiState.Success).data
            enrollment = list.firstOrNull { it.courseId == courseId }
        }
    }

    val currentLesson = lessons.firstOrNull { it.id == currentLessonId } ?: lessons.firstOrNull()
    val isEnrolled = enrollment != null
    val canAccessLesson = isEnrolled || (currentLesson?.isPreview == true)

    val isCompleted = enrollment?.completedLessonIds?.contains(currentLesson?.id) == true

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
                IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_player_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZentrixTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val cTitle = if (isBengali && course?.titleBn?.isNotBlank() == true) course?.titleBn else course?.title
                    Text(
                        text = cTitle ?: "Course Lesson",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary,
                        maxLines = 1
                    )
                    if (enrollment != null) {
                        Text(
                            text = "${enrollment?.progressPercent ?: 0}% Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixSecondaryBlue
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (course == null || currentLesson == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZentrixPrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Player Container
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                        border = BorderStroke(1.dp, ZentrixCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Player Viewport
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .background(ZentrixBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!canAccessLesson) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = ZentrixTextSecondary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.locked_lesson),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ZentrixTextSecondary
                                        )
                                    }
                                } else {
                                    // Simulated active video playback container
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(ZentrixPrimaryBlue)
                                                .clickable { isPlaying = !isPlaying },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = ZentrixTextPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (isPlaying) "Playing: ${currentLesson.duration}" else "Tap to Play Lesson Video",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ZentrixSecondaryBlue
                                        )
                                    }
                                }
                            }

                            // Lesson Info & Actions
                            Column(modifier = Modifier.padding(16.dp)) {
                                val lesTitle = if (isBengali && currentLesson.titleBn.isNotBlank()) currentLesson.titleBn else currentLesson.title
                                Text(
                                    text = lesTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ZentrixTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Duration: ${currentLesson.duration}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZentrixTextSecondary
                                    )
                                    if (currentLesson.isPreview && !isEnrolled) {
                                        Text(
                                            text = "FREE PREVIEW",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ZentrixSecondaryBlue
                                        )
                                    }
                                }

                                if (enrollment != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = ZentrixCardBorder)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            viewModel.completeLesson(
                                                enrollmentId = enrollment!!.id,
                                                lessonId = currentLesson.id,
                                                totalLessons = lessons.size
                                            )
                                        },
                                        enabled = !isCompleted,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCompleted) ZentrixSuccess else ZentrixPrimaryBlue
                                        ),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("btn_complete_lesson")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isCompleted) stringResource(R.string.lesson_completed) else stringResource(R.string.mark_as_completed),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ZentrixTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Downloadable Protected PDF Resources
                if (currentLesson.pdfUrl != null && canAccessLesson) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                            border = BorderStroke(1.dp, ZentrixCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.pdf_resources),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ZentrixTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ZentrixSurfaceVariant)
                                        .clickable {
                                            pdfOpenedNotice = "Opening protected curriculum document: ${currentLesson.title} Notes.pdf"
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = ZentrixSecondaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lecture Notes & Cheatsheet",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                            color = ZentrixTextPrimary
                                        )
                                        Text(
                                            text = "Verified student document (PDF)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                            color = ZentrixTextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = ZentrixTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (pdfOpenedNotice != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pdfOpenedNotice.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ZentrixSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                // All Lessons Navigation List
                item {
                    Text(
                        text = "Curriculum Lessons (${lessons.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary
                    )
                }

                items(lessons) { lesson ->
                    val isCurrent = lesson.id == currentLessonId
                    val isLessonDone = enrollment?.completedLessonIds?.contains(lesson.id) == true
                    val accessible = isEnrolled || lesson.isPreview

                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) ZentrixPrimaryBlue.copy(alpha = 0.15f) else ZentrixSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) ZentrixSecondaryBlue else ZentrixCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = accessible) {
                                if (accessible) {
                                    currentLessonId = lesson.id
                                    isPlaying = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isLessonDone -> ZentrixSuccess.copy(alpha = 0.2f)
                                            accessible -> ZentrixPrimaryBlue.copy(alpha = 0.2f)
                                            else -> ZentrixSurfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isLessonDone -> Icons.Default.Check
                                        accessible -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isLessonDone -> ZentrixSuccess
                                        accessible -> ZentrixSecondaryBlue
                                        else -> ZentrixTextSecondary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val itemTitle = if (isBengali && lesson.titleBn.isNotBlank()) lesson.titleBn else lesson.title
                                Text(
                                    text = itemTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                    color = if (accessible) ZentrixTextPrimary else ZentrixTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lesson.duration,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ZentrixTextSecondary
                                )
                            }

                            if (isCurrent) {
                                Text(
                                    text = "NOW PLAYING",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
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
