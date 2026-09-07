package app.kelkoo.music.ui.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.kelkoo.music.R
import coil3.compose.AsyncImage

enum class LibraryCapsule {
    LIKED, DOWNLOADED, EXPORTED, LOCAL
}

@Composable
fun LibraryCapsuleRow(
    selected: LibraryCapsule?,
    onSelect: (LibraryCapsule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CapsuleChip(
            label = stringResource(R.string.capsule_liked),
            icon = R.drawable.favorite,
            selected = selected == LibraryCapsule.LIKED,
            onClick = { onSelect(LibraryCapsule.LIKED) },
        )
        CapsuleChip(
            label = stringResource(R.string.capsule_downloaded),
            icon = R.drawable.arrow_downward,
            selected = selected == LibraryCapsule.DOWNLOADED,
            onClick = { onSelect(LibraryCapsule.DOWNLOADED) },
        )
        CapsuleChip(
            label = stringResource(R.string.capsule_exported),
            icon = R.drawable.arrow_upward,
            selected = selected == LibraryCapsule.EXPORTED,
            onClick = { onSelect(LibraryCapsule.EXPORTED) },
        )
        CapsuleChip(
            label = stringResource(R.string.capsule_local),
            icon = R.drawable.library_music,
            selected = selected == LibraryCapsule.LOCAL,
            onClick = { onSelect(LibraryCapsule.LOCAL) },
        )
    }
}

@Composable
private fun CapsuleChip(
    label: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AuraGold else Color(0xFF141414))
            .border(1.dp, AuraGold.copy(alpha = if (selected) 1f else 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) Color.Black else AuraGold,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) Color.Black else AuraGold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

data class DeckCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
)

/**
 * Richer Library 3D card deck — perspective rotationY, staggered fan,
 * swipe to cycle front card (closer to FINAL library mock).
 */
@Composable
fun LibraryFanDeck(
    cards: List<DeckCard>,
    onFrontClick: (DeckCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    var frontIndex by remember(cards) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val ordered = remember(cards, frontIndex) {
        val n = cards.size
        val idx = ((frontIndex % n) + n) % n
        cards.drop(idx) + cards.take(idx)
    }
    val visible = ordered.take(5)
    val front = visible.first()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 20.dp)
                .pointerInput(cards.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, amount -> dragAccum += amount },
                        onDragCancel = { dragAccum = 0f },
                        onDragEnd = {
                            val threshold = with(density) { 48.dp.toPx() }
                            when {
                                dragAccum < -threshold -> frontIndex = (frontIndex + 1) % cards.size
                                dragAccum > threshold -> frontIndex =
                                    (frontIndex - 1 + cards.size) % cards.size
                            }
                            dragAccum = 0f
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            visible.asReversed().forEachIndexed { revIndex, card ->
                val indexFromFront = visible.size - 1 - revIndex
                val rotZ = indexFromFront * 8.5f
                val rotY = indexFromFront * -12f
                val xOff = indexFromFront * 22f
                val yOff = indexFromFront * 8f
                val scale = 1f - indexFromFront * 0.05f
                DeckCardFace(
                    card = card,
                    isFront = indexFromFront == 0,
                    modifier = Modifier
                        .zIndex((visible.size - indexFromFront).toFloat())
                        .graphicsLayer {
                            rotationZ = rotZ
                            rotationY = rotY
                            translationX = xOff
                            translationY = yOff
                            scaleX = scale
                            scaleY = scale
                            cameraDistance = 12f * density.density
                            shadowElevation = (16 - indexFromFront * 2).dp.toPx()
                            alpha = 1f - indexFromFront * 0.08f
                        }
                        .then(
                            if (indexFromFront == 0) Modifier.clickable { onFrontClick(front) }
                            else Modifier
                        ),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = front.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = front.subtitle,
                color = AuraGold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeckCardFace(
    card: DeckCard,
    isFront: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .size(width = 210.dp, height = 240.dp)
            .shadow(if (isFront) 18.dp else 8.dp, shape, ambientColor = AuraGold.copy(alpha = 0.28f))
            .clip(shape)
            .border(1.dp, AuraGold.copy(alpha = if (isFront) 0.6f else 0.28f), shape)
            .background(Color(0xFF1A1A1A)),
    ) {
        if (!card.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = card.thumbnailUrl,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.library_music),
                    contentDescription = null,
                    tint = AuraGold.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        if (isFront) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = AuraGold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(22.dp),
            )
        }
    }
}
