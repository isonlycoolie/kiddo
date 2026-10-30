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

