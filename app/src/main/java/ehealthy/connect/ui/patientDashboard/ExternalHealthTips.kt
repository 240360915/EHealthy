package ehealthy.connect.ui.patientDashboard

import android.text.Html
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar
@Serializable
private data class ItemListResponse(val Result: ItemListResult)
@Serializable
private data class ItemListResult(val Items: ItemListItems)
@Serializable
private data class ItemListItems(val Item: List<ItemListEntry> = emptyList())
@Serializable
private data class ItemListEntry(val Id: String, val Title: String)

@Serializable
private data class TopicSearchResponse(val Result: TopicSearchResult)
@Serializable
private data class TopicSearchResult(val Resources: TopicResources)
@Serializable
private data class TopicResources(val Resource: List<TopicResource> = emptyList())
@Serializable
private data class TopicResource(
    val Title: String,
    val ImageUrl: String? = null,
    val AccessibleVersion: String? = null,
    val Sections: TopicSections? = null
)
@Serializable
private data class TopicSections(val section: List<TopicSection> = emptyList())
@Serializable
private data class TopicSection(val Title: String, val Content: String)

/**
 * A tip fully loaded up front (title, photo, teaser, full text, source link)
 * so opening its detail popup never needs another network round trip.
 */
data class ExternalHealthTip(
    val id: String,
    val title: String,
    val category: TipCategory,
    val imageUrl: String,
    val teaser: String,
    val fullSummary: String,
    val sourceUrl: String?
)


private fun daySeed(): Long {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
}
private fun categorizeExternal(title: String): TipCategory {
    val t = title.lowercase()
    return when {
        "screen" in t || "vaccin" in t || "test" in t || "checkup" in t || "check" in t || "exam" in t -> TipCategory.PREVENTION
        "stress" in t || "depress" in t || "anxiety" in t || "relationship" in t || "caregiver" in t || "mental" in t -> TipCategory.MENTAL
        "eat" in t || "active" in t || "exercise" in t || "sleep" in t || "weight" in t || "smok" in t || "alcohol" in t -> TipCategory.DAILY
        else -> TipCategory.HOME
    }
}

private val healthApiClient: HttpClient by lazy {
    HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

private fun plainText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

private fun teaserFrom(fullText: String, maxLen: Int = 140): String {
    val singleLine = fullText.replace(Regex("\\s+"), " ").trim()
    return if (singleLine.length <= maxLen) singleLine
    else singleLine.take(maxLen).substringBeforeLast(' ') + "…"
}

private suspend fun fetchTopicDetail(id: String): ExternalHealthTip? {
    return try {
        val response: TopicSearchResponse = healthApiClient
            .get("https://odphp.health.gov/myhealthfinder/api/v4/topicsearch.json?TopicId=$id")
            .body()
        val resource = response.Result.Resources.Resource.firstOrNull() ?: return null
        val image = resource.ImageUrl
        if (image.isNullOrBlank()) return null // every card here needs a real photo

        val sections = resource.Sections?.section.orEmpty()
        val fullSummary = sections.joinToString("\n\n") { "${it.Title}\n${plainText(it.Content)}" }
        val firstParagraph = sections.firstOrNull()?.let { plainText(it.Content) } ?: resource.Title

        ExternalHealthTip(
            id = id,
            title = resource.Title,
            category = categorizeExternal(resource.Title),
            imageUrl = image,
            teaser = teaserFrom(firstParagraph),
            fullSummary = fullSummary.ifBlank { "No further details available." },
            sourceUrl = resource.AccessibleVersion
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Fetches the full MyHealthfinder topic list once (fast, single request),
 * buckets titles into our four categories, then pulls full detail (photo +
 * text) for up to [perCategory] topics per category — all concurrently, so
 * this doesn't turn into dozens of sequential round trips. Topics with no
 * photo, or that fail to load, are simply skipped.
 */
suspend fun fetchCuratedHealthTips(perCategory: Int = 6): Result<List<ExternalHealthTip>> {
    return try {
        val listResponse: ItemListResponse = healthApiClient
            .get("https://odphp.health.gov/myhealthfinder/api/v4/itemlist.json?Type=topic")
            .body()

        val grouped = listResponse.Result.Items.Item.groupBy { categorizeExternal(it.Title) }

        // Fetch a few extra candidates per category (some will lack images or fail),
        // then trim down to perCategory once we know which ones actually succeeded.
        val candidateIds = TipCategory.entries
            .filter { it != TipCategory.ALL }
            .flatMap { category ->
                grouped[category].orEmpty()
                    .shuffled(kotlin.random.Random(daySeed()))
                    .take(perCategory + 4)
                    .map { it.Id }
            }

        val fetched = coroutineScope {
            candidateIds.map { id -> async { fetchTopicDetail(id) } }.mapNotNull { it.await() }
        }

        val result = TipCategory.entries
            .filter { it != TipCategory.ALL }
            .flatMap { category -> fetched.filter { it.category == category }.take(perCategory) }

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}