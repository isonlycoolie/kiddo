package app.kiddo.core.model

import kotlinx.serialization.Serializable

// A CV as structure, written by the model from the user's own CV and rendered to a clean PDF on the phone.
@Serializable
data class CvData(
    val name: String,
    val headline: String = "",
    val contact: List<String> = emptyList(),
    val summary: String = "",
    val sections: List<CvSection> = emptyList(),
)

@Serializable
data class CvSection(val title: String, val items: List<CvItem> = emptyList())

@Serializable
data class CvItem(
    val title: String = "",
    val subtitle: String = "",
    val dates: String = "",
    val bullets: List<String> = emptyList(),
)
