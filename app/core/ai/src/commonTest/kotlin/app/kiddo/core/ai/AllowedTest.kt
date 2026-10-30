package app.kiddo.core.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowedTest {
    @Test
    fun httpsIsAlwaysAllowed() = assertTrue(allowed("https://api.anthropic.com"))

    @Test
    fun plainHttpOnlyReachesTheLocalNetwork() {
        assertTrue(allowed("http://192.168.1.10:11434/v1"))
        assertTrue(allowed("http://10.0.0.5/v1"))
        assertTrue(allowed("http://localhost:1234/v1"))
        assertTrue(allowed("http://172.20.3.4:8080"))
        assertFalse(allowed("http://api.example.com/v1"))
        assertFalse(allowed("http://172.32.0.1"))
        assertFalse(allowed("ftp://192.168.1.1"))
    }
}
