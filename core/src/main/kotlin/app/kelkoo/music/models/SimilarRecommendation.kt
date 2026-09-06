

package app.kelkoo.music.models

import com.music.innertube.models.YTItem
import app.kelkoo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
