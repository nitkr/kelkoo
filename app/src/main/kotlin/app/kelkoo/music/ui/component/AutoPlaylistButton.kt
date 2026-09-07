package app.kelkoo.music.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kelkoo.music.ui.theme.DefaultThemeColor

@Composable
fun AutoPlaylistButton(
    title: String,
    icon: Int,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val gold = DefaultThemeColor
    val shape = RoundedCornerShape(22.dp)
    Surface(
        shape = shape,
        color = if (selected) gold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .border(1.dp, gold.copy(alpha = if (selected) 1f else 0.45f), shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = if (selected) Color.Black else iconTint.takeIf { it != Color.Unspecified } ?: gold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.Black else gold
            )
        }
    }
}
