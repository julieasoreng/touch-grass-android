package com.julieasoreng.touchgrass.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julieasoreng.touchgrass.ui.onboarding.components.AssistantChatBubble
import com.julieasoreng.touchgrass.ui.onboarding.components.CustomActivityInput
import com.julieasoreng.touchgrass.ui.onboarding.components.CustomGoalInput
import com.julieasoreng.touchgrass.ui.onboarding.components.OnboardingBottomCta
import com.julieasoreng.touchgrass.ui.onboarding.components.OptionButton
import com.julieasoreng.touchgrass.ui.onboarding.components.ProgressPills
import com.julieasoreng.touchgrass.ui.onboarding.components.ScreenTimeSummaryBubble
import com.julieasoreng.touchgrass.ui.onboarding.components.ScrollTimeInsightBubble
import com.julieasoreng.touchgrass.ui.onboarding.components.SelectableChip
import com.julieasoreng.touchgrass.ui.onboarding.components.UserChatBubble
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.CreamBackground
import com.julieasoreng.touchgrass.ui.theme.SageGreen

private val replacementOptions = listOf("Reading", "Writing", "Painting", "Dancing", "Exercise", "Journaling")

private sealed interface TranscriptItem {
    data class Assistant(val text: String) : TranscriptItem
    data class UserAnswer(val text: String) : TranscriptItem
    data object ScreenTimeBaseline : TranscriptItem
    data object TargetOptions : TranscriptItem
    data object ScrollPatternCard : TranscriptItem
    data object ReplacementChips : TranscriptItem
}

private fun buildTranscript(state: OnboardingUiState): List<TranscriptItem> {
    val items = mutableListOf<TranscriptItem>()

    if (!state.hasUsagePermission) {
        items += TranscriptItem.Assistant(
            "Hi, I'm Bloom 🌱 To show you your actual screen time, we need access to your phone's usage data."
        )
        return items
    }

    items += TranscriptItem.Assistant(
        "Hi, I'm Bloom 🌱 Here's your actual screen time, based on your phone's usage data."
    )
    items += TranscriptItem.ScreenTimeBaseline

    if (state.step.ordinal >= OnboardingStep.TARGET.ordinal) {
        items += TranscriptItem.Assistant("Thanks for taking a look. What would you like to bring that down to, ideally?")
        if (state.step == OnboardingStep.TARGET) items += TranscriptItem.TargetOptions
        val targetMillis = state.answers.targetScreenTimeMillis
        if (targetMillis != null) {
            val preset = state.answers.targetPreset
            val label = if (preset != null) {
                "${preset.label} (down ${preset.reductionPercent}%) — ~${formatDuration(targetMillis)} per day"
            } else {
                "Custom goal — ${formatDuration(targetMillis)} per day"
            }
            items += TranscriptItem.UserAnswer(label)
        }
    }

    if (state.step.ordinal >= OnboardingStep.SCROLL_TIMES.ordinal) {
        items += TranscriptItem.Assistant("Let's see when you tend to scroll the most.")
        items += TranscriptItem.ScrollPatternCard
    }

    if (state.step.ordinal >= OnboardingStep.REPLACEMENT.ordinal) {
        items += TranscriptItem.Assistant("Last one — what would you like to be doing instead, when you'd normally be scrolling?")
        if (state.step == OnboardingStep.REPLACEMENT) items += TranscriptItem.ReplacementChips
    }

    return items
}

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = remember { OnboardingViewModelFactory(context.applicationContext) }
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onOnboardingComplete() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsagePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = state.step.ordinal > OnboardingStep.USAGE.ordinal) {
        viewModel.goBack()
    }

    val transcript = remember(state) { buildTranscript(state) }
    val listState = rememberLazyListState()
    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            ProgressPills(
                currentStepIndex = state.step.ordinal,
                totalSteps = OnboardingStep.entries.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        },
        bottomBar = {
            when (state.step) {
                OnboardingStep.USAGE_PERMISSION -> OnboardingBottomCta(
                    text = "Open Settings",
                    enabled = true,
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    modifier = Modifier.padding(20.dp)
                )
                OnboardingStep.USAGE -> OnboardingBottomCta(
                    text = "Continue",
                    enabled = !state.isLoadingScreenTimeData,
                    onClick = viewModel::confirmUsageBaseline,
                    modifier = Modifier.padding(20.dp)
                )
                OnboardingStep.SCROLL_TIMES -> OnboardingBottomCta(
                    text = "Continue",
                    enabled = !state.isLoadingScreenTimeData,
                    onClick = viewModel::confirmScrollTimeInsight,
                    modifier = Modifier.padding(20.dp)
                )
                OnboardingStep.REPLACEMENT -> OnboardingBottomCta(
                    text = "Let's start",
                    enabled = state.selectedReplacementActivities.isNotEmpty(),
                    onClick = viewModel::completeOnboarding,
                    modifier = Modifier.padding(20.dp)
                )
                else -> Unit
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(transcript) { _, item ->
                AnimatedTranscriptItem {
                    TranscriptItemContent(item = item, state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun AnimatedTranscriptItem(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 3 }
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptItemContent(
    item: TranscriptItem,
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    when (item) {
        is TranscriptItem.Assistant -> AssistantChatBubble(item.text)
        is TranscriptItem.UserAnswer -> UserChatBubble(item.text)

        TranscriptItem.ScreenTimeBaseline -> ScreenTimeSummaryBubble(
            isLoading = state.isLoadingScreenTimeData,
            dailyAverageMillis = state.answers.dailyAverageScreenTimeMillis,
            daysOfData = state.answers.screenTimeDaysOfData
        )

        TranscriptItem.TargetOptions -> Column(
            modifier = Modifier.padding(start = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val baselineMillis = state.answers.dailyAverageScreenTimeMillis
            val hasBaseline = state.answers.screenTimeDaysOfData > 0

            if (hasBaseline) {
                TargetPreset.entries.forEach { preset ->
                    OptionButton(
                        text = "${preset.label} (down ${preset.reductionPercent}%)",
                        subtitle = "~${formatDuration(preset.targetMillis(baselineMillis))} per day",
                        selected = state.answers.targetPreset == preset,
                        onClick = { viewModel.selectTargetPreset(preset) }
                    )
                }
            } else {
                Text(
                    text = "We don't have enough data yet to suggest a target — check back after a day of tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalText.copy(alpha = 0.7f)
                )
            }

            if (state.isEnteringCustomTarget) {
                CustomGoalInput(
                    value = state.customTargetInputText,
                    onValueChange = viewModel::updateCustomTargetInput,
                    onConfirm = viewModel::confirmCustomTarget
                )
            } else {
                OptionButton(
                    text = "Set my own goal",
                    selected = state.answers.targetPreset == null && state.answers.targetScreenTimeMillis != null,
                    onClick = viewModel::startCustomTargetEntry
                )
            }
        }

        TranscriptItem.ScrollPatternCard -> ScrollTimeInsightBubble(
            isLoading = state.isLoadingScreenTimeData,
            dominantPattern = state.answers.scrollTimePattern,
            daysOfData = state.answers.scrollTimePatternDaysOfData
        )

        TranscriptItem.ReplacementChips -> Column(
            modifier = Modifier.padding(start = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val customActivities = state.selectedReplacementActivities.filter { it !in replacementOptions }
                replacementOptions.forEach { option ->
                    SelectableChip(
                        text = option,
                        selected = option in state.selectedReplacementActivities,
                        selectedColor = SageGreen,
                        onClick = { viewModel.toggleReplacementActivity(option) }
                    )
                }
                customActivities.forEach { custom ->
                    SelectableChip(
                        text = custom,
                        selected = true,
                        selectedColor = SageGreen,
                        onClick = { viewModel.toggleReplacementActivity(custom) }
                    )
                }
            }
            CustomActivityInput(
                value = state.customActivityText,
                onValueChange = viewModel::updateCustomActivityText,
                onConfirm = viewModel::addCustomActivity
            )
        }
    }
}
