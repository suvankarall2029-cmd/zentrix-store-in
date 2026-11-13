package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Enrollment
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
fun MyLearningScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onCourseClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    onResumeLesson: (String, String?) -> Unit
) {
    val enrollmentsState by viewModel.enrollments.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("my_learning_screen")
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZentrixSurface)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.my_learning_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = ZentrixTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBengali) "আপনার সমস্ত নথিভুক্ত কোর্স ও প্রগতি ট্র্যাক করুন" else "Track your progress, resume lessons and download course resources.",
                style = MaterialTheme.typography.bodyMedium,
                color = ZentrixTextSecondary
            )
        }

        when (val state = enrollmentsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZentrixPrimaryBlue)
                }
            }
            is UiState.Success -> {
                val enrollments = state.data
                if (enrollments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = ZentrixTextSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.my_learning_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = ZentrixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onExploreClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = stringResource(R.string.explore_courses),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZentrixTextPrimary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Stats
                        item {
                            val completedCount = enrollments.count { it.progressPercent >= 100L }
                            val inProgressCount = enrollments.size - completedCount

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LearningStatCard(
                                    label = if (isBengali) "মোট কোর্স" else "Enrolled",
                                    value = enrollments.size.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                LearningStatCard(
                                    label = if (isBengali) "চলমান" else "In Progress",
                                    value = inProgressCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                LearningStatCard(
                                    label = if (isBengali) "সম্পন্ন" else "Completed",
                                    value = completedCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = if (isBengali) "আপনার কোর্স তালিকা" else "Your Courses",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZentrixTextPrimary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(enrollments) { enrollment ->
                            EnrollmentCard(
                                enrollment = enrollment,
                                isBengali = isBengali,
                                onCourseClick = { onCourseClick(enrollment.courseId) },
                                onResumeClick = { onResumeLesson(enrollment.courseId, enrollment.lastLessonId) }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = ZentrixTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun LearningStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
        border = BorderStroke(1.dp, ZentrixCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = ZentrixSecondaryBlue
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ZentrixTextSecondary
            )
        }
    }
}

@Composable
private fun EnrollmentCard(
    enrollment: Enrollment,
    isBengali: Boolean,
    onCourseClick: () -> Unit,
    onResumeClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
        border = BorderStroke(1.dp, ZentrixCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCourseClick() }
            .testTag("enrolled_card_${enrollment.courseId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (enrollment.isPaid) ZentrixPrimaryBlue.copy(alpha = 0.2f) else ZentrixSuccess.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (enrollment.isPaid) "PREMIUM ACCESS" else "FREE COURSE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (enrollment.isPaid) ZentrixSecondaryBlue else ZentrixSuccess
                    )
                }

                Text(
                    text = "${enrollment.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZentrixSecondaryBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = enrollment.courseTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ZentrixTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (enrollment.progressPercent / 100f).coerceIn(0f, 1f) },
                color = if (enrollment.progressPercent >= 100L) ZentrixSuccess else ZentrixSecondaryBlue,
                trackColor = ZentrixSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${enrollment.completedLessonIds.size} lessons completed",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = ZentrixTextSecondary
                )

                Button(
                    onClick = onResumeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("resume_btn_${enrollment.courseId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBengali) "পাঠ চালান" else "Resume",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary
                    )
                }
            }
        }
    }
}
