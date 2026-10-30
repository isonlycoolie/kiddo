package app.kiddo.core.model

enum class CvFormat(val mimeType: String) {
    Pdf("application/pdf"),
    Docx("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
}

// A CV the user added. The original file stays as it is; `text` is what Kiddo read from it, null when the phone
// could not read the text (older Android with a PDF), in which case the file itself goes to the model.
data class Cv(
    val id: String,
    val name: String,
    val format: CvFormat,
    val pages: Int,
    val text: String?,
    val isDefault: Boolean,
    val byKiddo: Boolean,
    val addedAt: Long,
)
