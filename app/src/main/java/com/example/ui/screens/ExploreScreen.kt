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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
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
fun ExploreScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onCourseClick: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filterFreeOnly by viewModel.filterFreeOnly.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val filteredCourses by viewModel.filteredCourses.collectAsState()
    val enrollmentsState by viewModel.enrollments.collectAsState()

    val enrolledIds = when (val state = enrollmentsState) {
        is UiState.Success -> state.data.map { it.courseId }.toSet()
        else -> emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("explore_screen")
    ) {
        // Search Input Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZentrixSurface)
                .padding(16.dp)
        ) {
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
                    .testTag("explore_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs: All, Free, Paid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterTabChip(
                    text = stringResource(R.string.filter_all),
                    isSelected = filterFreeOnly == null,
                    onClick = { viewModel.setFilterFreeOnly(null) },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterTabChip(
                    text = stringResource(R.string.filter_free),
                    isSelected = filterFreeOnly == true,
                    onClick = { viewModel.setFilterFreeOnly(true) },
                    modifier = Modifier.testTag("filter_free")
                )
                FilterTabChip(
                    text = stringResource(R.string.filter_paid),
                    isSelected = filterFreeOnly == false,
                    onClick = { viewModel.setFilterFreeOnly(false) },
                    modifier = Modifier.testTag("filter_paid")
                )
            }

            // Categories horizontal list
            if (categoriesState is UiState.Success) {
                val cats = (categoriesState as UiState.Success).data
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterTabChip(
                            text = if (isBengali) "সব ক্যাটাগরি" else "All Categories",
                            isSelected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) }
                        )
                    }
                    items(cats) { cat ->
                        FilterTabChip(
                            text = if (isBengali && cat.nameBn.isNotBlank()) cat.nameBn else cat.name,
                            isSelected = selectedCategory == cat.id,
                            onClick = { viewModel.selectCategory(cat.id) }
                        )
                    }
                }
            }
        }

        // Search Results List
        if (filteredCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ZentrixTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBengali) "কোনো কোর্স পাওয়া যায়নি" else "No courses found matching your search.",
                        style = MaterialTheme.typography.titleMedium,
                        color = ZentrixTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCourses) { course ->
                    CourseCard(
                        course = course,
                        isBengali = isBengali,
                        isEnrolled = enrolledIds.contains(course.id),
                        onClick = { onCourseClick(course.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) ZentrixPrimaryBlue else ZentrixSurfaceVariant
    val textColor = if (isSelected) ZentrixTextPrimary else ZentrixTextSecondary
    val border = if (isSelected) ZentrixSecondaryBlue else ZentrixCardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}
