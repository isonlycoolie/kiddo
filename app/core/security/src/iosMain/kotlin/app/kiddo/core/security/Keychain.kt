package app.kiddo.core.security

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

// Generic-password items for one service: readable after the first unlock (the background check needs them), never
// synced to iCloud and never restored to another device.
@OptIn(ExperimentalForeignApi::class)
internal class Keychain(private val service: String) {
    fun get(account: String): NSData? = memScoped {
        val found = alloc<CFTypeRefVar>()
        val status = query(kSecAttrAccount to account, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
            SecItemCopyMatching(it, found.ptr)
        }
        if (status != errSecSuccess) null else CFBridgingRelease(found.value) as? NSData
    }

    fun put(account: String, data: NSData) {
        remove(account)
        query(kSecAttrAccount to account, kSecValueData to data, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly) {
            SecItemAdd(it, null)
        }
    }

    fun remove(account: String) {
        query(kSecAttrAccount to account) { SecItemDelete(it) }
    }

    fun removeAll() {
        query { SecItemDelete(it) }
    }

    // Kotlin values are bridged for the call only.
    private fun <T> query(vararg entries: Pair<CFTypeRef?, Any?>, call: (CFDictionaryRef?) -> T): T {
        val retained = mutableListOf<CFTypeRef?>()
        val dict = CFDictionaryCreateMutable(null, (entries.size + 2).convert(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        fun add(key: CFTypeRef?, value: Any?) {
            val ref = value as? CPointer<*> ?: CFBridgingRetain(value).also { retained += it }
            CFDictionaryAddValue(dict, key, ref)
        }
        add(kSecClass, kSecClassGenericPassword)
        add(kSecAttrService, service)
        entries.forEach { (key, value) -> add(key, value) }
        return try {
            call(dict)
        } finally {
            retained.forEach { CFBridgingRelease(it) }
            CFRelease(dict)
        }
    }
}
