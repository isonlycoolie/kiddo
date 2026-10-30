import CommonCrypto
import CryptoKit
import Foundation
import KiddoKit
import Security

// Kiddo's keys on iOS. Each device key is a random AES-256 key used with AES-GCM; it is stored only wrapped by a P-256
// key that lives in the Secure Enclave and never leaves it, so the stored form is useless off this iPhone. The
// simulator has no Enclave: there the device key is kept in the Keychain alone. Everything is readable after the
// first unlock (the background check needs it) and never synced or restored to another device.
final class AppleCryptoBridge: NSObject, AppleCrypto {
    private let service = "app.kiddo.keys"
    private let algorithm = SecKeyAlgorithm.eciesEncryptionCofactorVariableIVX963SHA256AESGCM
    private var cache: [String: SymmetricKey] = [:]
    private let lock = NSLock()

    func sealWithDeviceKey(name: String, plain: Data) -> Data? {
        guard let key = deviceKey(name) else { return nil }
        return try? AES.GCM.seal(plain, using: key).combined
    }

    func openWithDeviceKey(name: String, sealed: Data) -> Data? {
        guard let key = deviceKey(name), let box = try? AES.GCM.SealedBox(combined: sealed) else { return nil }
        return try? AES.GCM.open(box, using: key)
    }

    func eraseDeviceKeys() {
        lock.lock()
        defer { lock.unlock() }
        cache.removeAll()
        SecItemDelete([kSecClass: kSecClassGenericPassword, kSecAttrService: service] as [CFString: Any] as CFDictionary)
        for name in ["secret", "field"] {
            SecItemDelete([kSecClass: kSecClassKey, kSecAttrApplicationTag: tag(name)] as [CFString: Any] as CFDictionary)
        }
    }

    func hasSecureEnclave() -> Bool {
        SecureEnclave.isAvailable
    }

    func deriveKey(passphrase: String, salt: Data, rounds: Int32) -> Data? {
        var out = Data(count: 32)
        let password = Array(passphrase.utf8).map { CChar(bitPattern: $0) }
        let status = out.withUnsafeMutableBytes { outBytes in
            salt.withUnsafeBytes { saltBytes in
                CCKeyDerivationPBKDF(
                    CCPBKDFAlgorithm(kCCPBKDF2),
                    password, password.count,
                    saltBytes.bindMemory(to: UInt8.self).baseAddress, salt.count,
                    CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                    UInt32(rounds),
                    outBytes.bindMemory(to: UInt8.self).baseAddress, 32
                )
            }
        }
        return status == kCCSuccess ? out : nil
    }

    func sealGcm(key: Data, nonce: Data, plain: Data) -> Data? {
        guard let nonce = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.seal(plain, using: SymmetricKey(data: key), nonce: nonce) else { return nil }
        return box.ciphertext + box.tag
    }

    func openGcm(key: Data, nonce: Data, sealed: Data) -> Data? {
        guard sealed.count >= 16, let nonce = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.SealedBox(nonce: nonce, ciphertext: sealed.dropLast(16), tag: sealed.suffix(16)) else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key))
    }

    // The device key, unwrapped once per launch and kept in memory while Kiddo runs.
    private func deviceKey(_ name: String) -> SymmetricKey? {
        lock.lock()
        defer { lock.unlock() }
        if let key = cache[name] { return key }
        let key = load(name) ?? create(name)
        cache[name] = key
        return key
    }

    private func load(_ name: String) -> SymmetricKey? {
        guard let stored = keychainData(name) else { return nil }
        guard SecureEnclave.isAvailable else { return SymmetricKey(data: stored) }
        guard let enclave = enclaveKey(name, create: false),
              let raw = SecKeyCreateDecryptedData(enclave, algorithm, stored as CFData, nil) as Data? else { return nil }
        return SymmetricKey(data: raw)
    }

