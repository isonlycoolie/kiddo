package app.kiddo.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.kiddo.core.model.CvFormat
import app.kiddo.core.model.toByteArray
import app.kiddo.core.model.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject

// Files on iOS go through the system's document picker (Files, iCloud Drive, other apps' folders). Kiddo gets a
// private copy of the one file chosen, reads it and deletes the copy.

@Composable
actual fun rememberCvPicker(onResult: (PickResult) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    val delegate = remember { PickerDelegate { url -> url?.let { latest(readCv(it)) } } }
    return remember(delegate) {
        {
            val types = listOfNotNull(UTTypePDF, UTType.typeWithIdentifier(DOCX))
            present(UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true), delegate)
        }
    }
}

@Composable
actual fun rememberFileOpener(onFile: (ByteArray?) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onFile)
    val delegate = remember {
        PickerDelegate { url ->
            val bytes = url?.let { NSData.dataWithContentsOfURL(it)?.toByteArray() }
            url?.let(::remove)
            latest(bytes)
        }
    }
    return remember(delegate) {
        { present(UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData, UTTypeItem), asCopy = true), delegate) }
    }
}

// "Save to Files": the file is written to a temporary copy, the picker moves a copy where the user chooses, and the
// temporary one is deleted either way.
@Composable
actual fun rememberFileSaver(onDone: (Boolean) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val latest by rememberUpdatedState(onDone)
    val pending = remember { arrayOfNulls<NSURL>(1) }
    val delegate = remember {
        PickerDelegate { url ->
            pending[0]?.let(::remove)
            pending[0] = null
            latest(url != null)
        }
    }
    return remember(delegate) {
        { name, bytes ->
            val file = NSURL.fileURLWithPath(NSTemporaryDirectory() + name)
            if (!bytes.toNSData().writeToURL(file, atomically = true)) {
                latest(false)
            } else {
                pending[0] = file
                present(UIDocumentPickerViewController(forExportingURLs = listOf(file), asCopy = true), delegate)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readCv(url: NSURL): PickResult {
    try {
        val format = when (url.pathExtension?.lowercase()) {
            "pdf" -> CvFormat.Pdf
            "docx" -> CvFormat.Docx
            else -> return PickResult.Unsupported
        }
        val size = (url.path?.let { NSFileManager.defaultManager.attributesOfItemAtPath(it, error = null) }?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
        if (size > MAX_CV_BYTES) return PickResult.TooLarge
        val bytes = NSData.dataWithContentsOfURL(url)?.toByteArray() ?: return PickResult.Unsupported
        if (bytes.size > MAX_CV_BYTES) return PickResult.TooLarge
        val name = url.lastPathComponent?.substringBeforeLast('.') ?: "CV"
        return PickResult.Picked(PickedFile(name, format, bytes))
    } finally {
        remove(url)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun remove(url: NSURL) {
    NSFileManager.defaultManager.removeItemAtURL(url, error = null)
}

private fun present(picker: UIDocumentPickerViewController, delegate: PickerDelegate) {
    picker.delegate = delegate
    topController()?.presentViewController(picker, animated = true, completion = null)
}

// Calls back with the chosen file, or null when the user cancels.
private class PickerDelegate(private val onPicked: (NSURL?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onPicked(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
    }
}

// The controller on top of everything, so system sheets present over the Compose host and anything it shows.
internal fun topController(): UIViewController? {
    @Suppress("DEPRECATION")
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private const val DOCX = "org.openxmlformats.wordprocessingml.document"
