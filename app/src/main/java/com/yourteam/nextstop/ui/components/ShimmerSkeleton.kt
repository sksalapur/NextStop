package com.yourteam.nextstop.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A shimmer-like skeleton loading composable.
 * Animates alpha between 0.3 and 0.7 to simulate content loading.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/**
 * Skeleton placeholder for a card-style list item (used in Admin Dashboard, Assignments).
 */
@Composable
fun ShimmerCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ShimmerBox(modifier = Modifier.size(24.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(modifier = Modifier.height(20.dp).width(120.dp))
                Spacer(modifier = Modifier.weight(1f))
                ShimmerBox(modifier = Modifier.height(20.dp).width(60.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.height(12.dp).width(50.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.height(14.dp).width(100.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.height(12.dp).width(50.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.height(14.dp).width(80.dp))
                }
            }
        }
    }
}

/**
 * A full-screen skeleton loading state with multiple shimmer cards.
 */
@Composable
fun ShimmerListSkeleton(
    cardCount: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(cardCount) {
            ShimmerCardSkeleton()
        }
    }
}

/**
 * Driver screen skeleton: large centered card + button.
 */
@Composable
fun DriverShimmerSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bus info card skeleton
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.size(40.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    ShimmerBox(modifier = Modifier.height(12.dp).width(80.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.height(24.dp).width(120.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerBox(modifier = Modifier.height(32.dp).width(100.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(modifier = Modifier.height(32.dp))
        // Speed card skeleton
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerBox(modifier = Modifier.size(32.dp).clip(CircleShape))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.height(56.dp).width(100.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.height(16.dp).width(40.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(28.dp)))
        Spacer(modifier = Modifier.height(16.dp))
    }
}
