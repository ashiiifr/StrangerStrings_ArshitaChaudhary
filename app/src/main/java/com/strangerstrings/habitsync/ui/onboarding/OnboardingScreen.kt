package com.strangerstrings.habitsync.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.strangerstrings.habitsync.viewmodel.OnboardingUiState
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val lottieUrl: String,
    val keywordHint: String,
    val fallbackIcon: ImageVector,
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Build Habits That Actually Stick",
        subtitle = "Create meaningful routines, track momentum daily, and stay consistent with visual progress.",
        lottieUrl = "https://lottie.host/4cf6d68e-7197-4b01-b7cb-7948a98fb54a/N5m0wLa8cv.json",
        keywordHint = "habit tracking animation",
        fallbackIcon = Icons.Default.AutoGraph,
    ),
    OnboardingPage(
        title = "Proof-Based Accountability",
        subtitle = "Attach quick photo proof to your completions and make every streak truly credible.",
        lottieUrl = "https://lottie.host/9f69b74c-078b-447d-aa8a-033f8dfb4fdb/wXGndv0o2A.json",
        keywordHint = "goal progress animation",
        fallbackIcon = Icons.Default.Image,
    ),
    OnboardingPage(
        title = "Compete On Real Leaderboards",
        subtitle = "Climb rankings through consistency and challenge friends to stay ahead every week.",
        lottieUrl = "https://lottie.host/ce2f1db6-f7d4-42b4-a5d4-b4a67f8fec4e/JzebQ7QjJR.json",
        keywordHint = "leaderboard animation",
        fallbackIcon = Icons.Default.EmojiEvents,
    ),
    OnboardingPage(
        title = "See Every Win In Your Feed",
        subtitle = "Get a live social feed of milestones, streaks, and proof-backed achievements.",
        lottieUrl = "https://lottie.host/4f94f35d-9829-451f-87b6-d4f1f8f26771/4PjL1fJdvS.json",
        keywordHint = "success achievement animation",
        fallbackIcon = Icons.Default.EmojiEvents,
    ),
    OnboardingPage(
        title = "Improve One Day At A Time",
        subtitle = "Small daily actions compound into stronger discipline, confidence, and long-term growth.",
        lottieUrl = "https://lottie.host/e4ecf96f-fc3f-4a48-8cb4-c3a47ec95bf2/kM5dix0ehX.json",
        keywordHint = "self improvement animation",
        fallbackIcon = Icons.Default.SelfImprovement,
    ),
    OnboardingPage(
        title = "Ready To Start Your Streak?",
        subtitle = "Join HabitSync and turn your goals into measurable, competitive results.",
        lottieUrl = "https://lottie.host/1175d8f9-8f44-40ef-95dc-2526f4ad6cf6/i7yX2lAN8u.json",
        keywordHint = "habit success animation",
        fallbackIcon = Icons.Default.AutoGraph,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onSkipClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            ),
    ) {
        TextButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSkipClick()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 14.dp),
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 56.dp),
        ) { page ->
            val pageData = onboardingPages[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - pageOffset.coerceAtMost(1f) * 0.24f)
                        translationX = pageOffset * 72f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OnboardingIllustration(
                    pageData = pageData,
                    pageOffset = pageOffset,
                )

                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 28.dp),
                )
                Text(
                    text = pageData.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.9f),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExpandingDotsIndicator(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            val progressTarget = (pagerState.currentPage + 1f) / onboardingPages.size.toFloat()
            val progress by animateFloatAsState(
                targetValue = progressTarget,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "onboarding_progress",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            if (isLastPage) {
                val buttonScale by animateFloatAsState(
                    targetValue = if (uiState.isSaving) 0.98f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "onboarding_cta_scale",
                )
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGetStartedClick()
                    },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .height(58.dp)
                        .scale(buttonScale),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = "Let's Go",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Text(
                    text = "Swipe to continue",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingIllustration(
    pageData: OnboardingPage,
    pageOffset: Float,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Url(pageData.lottieUrl))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE,
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        translationX = pageOffset * 28f
                        scaleX = 1f - (pageOffset * 0.06f)
                        scaleY = 1f - (pageOffset * 0.06f)
                    },
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(
                    imageVector = pageData.fallbackIcon,
                    contentDescription = pageData.keywordHint,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(82.dp),
                )
                Text(
                    text = pageData.keywordHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpandingDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val dotWidth by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "onboarding_indicator_width_$index",
            )
            val dotColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
            }
            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = 8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}
