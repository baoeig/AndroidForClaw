package com.xiaomo.telegram

import org.junit.Assert.*
import org.junit.Test

class TelegramChannelTest {

    // ==================== Config ====================

    @Test
    fun `default config has channel disabled`() {
        val config = TelegramConfig()
        assertFalse(config.enabled)
    }

    @Test
    fun `default config has open dm policy`() {
        val config = TelegramConfig()
        assertEquals("open", config.dmPolicy)
    }

    @Test
    fun `default config has correct domain`() {
        val config = TelegramConfig()
        assertEquals("telegram", config.domain)
    }

    @Test
    fun `config with custom values`() {
        val config = TelegramConfig(
            enabled = true,
            token = "test-token",
            dmPolicy = "allowlist",
            historyLimit = 100
        )
        assertTrue(config.enabled)
        assertEquals("test-token", config.token)
        assertEquals("allowlist", config.dmPolicy)
        assertEquals(100, config.historyLimit)
    }

    // ==================== Channel ====================

    @Test
    fun `channel not connected initially`() {
        val config = TelegramConfig()
        val channel = TelegramChannel(config)
        assertFalse(channel.isConnected())
    }

    // ==================== Policy ====================

    @Test
    fun `open dm policy allows all senders`() {
        val config = TelegramConfig(dmPolicy = "open")
        val policy = com.xiaomo.telegram.policy.TelegramPolicy(config)
        assertTrue(policy.isDmAllowed("any_user"))
    }

    @Test
    fun `closed dm policy blocks senders`() {
        val config = TelegramConfig(dmPolicy = "closed")
        val policy = com.xiaomo.telegram.policy.TelegramPolicy(config)
        assertFalse(policy.isDmAllowed("any_user"))
    }

    @Test
    fun `require mention respects config`() {
        val config = TelegramConfig(requireMention = true)
        val policy = com.xiaomo.telegram.policy.TelegramPolicy(config)
        assertTrue(policy.requiresMention())
    }

    // ==================== Dedup ====================

    @Test
    fun `dedup detects duplicate messages`() {
        val dedup = com.xiaomo.telegram.session.TelegramDedup()
        assertFalse(dedup.isDuplicate("msg1"))
        assertTrue(dedup.isDuplicate("msg1"))
        assertFalse(dedup.isDuplicate("msg2"))
    }

    // ==================== Session ====================

    @Test
    fun `session manager creates new session`() {
        val sm = com.xiaomo.telegram.session.TelegramSessionManager()
        val session = sm.getOrCreate("test-session")
        assertEquals("test-session", session.key)
        assertEquals(0, session.messageCount)
    }

    @Test
    fun `session manager returns existing session`() {
        val sm = com.xiaomo.telegram.session.TelegramSessionManager()
        val s1 = sm.getOrCreate("test")
        val s2 = sm.getOrCreate("test")
        assertSame(s1, s2)
    }

    // ==================== History ====================

    @Test
    fun `history manager stores messages`() {
        val hm = com.xiaomo.telegram.session.TelegramHistoryManager(historyLimit = 10)
        hm.addMessage("s1", "user", "hello")
        hm.addMessage("s1", "assistant", "hi")
        val history = hm.getHistory("s1")
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
    }

    @Test
    fun `history manager respects limit`() {
        val hm = com.xiaomo.telegram.session.TelegramHistoryManager(historyLimit = 3)
        for (i in 1..5) {
            hm.addMessage("s1", "user", "msg$i")
        }
        assertEquals(3, hm.getHistory("s1").size)
    }

    // ==================== Mention ====================

    @Test
    fun `mention detection stub returns false`() {
        assertFalse(com.xiaomo.telegram.messaging.TelegramMention.isMentioned("hello", "bot123"))
    }

    // ==================== Accounts ====================

    @Test
    fun `accounts resolve returns default config`() {
        val accounts = TelegramAccounts()
        val config = accounts.resolveAccount("default")
        assertFalse(config.enabled)
    }

    // ==================== Probe ====================

    @Test
    fun `probe result data class works`() {
        val result = TelegramProbe.ProbeResult(ok = true)
        assertTrue(result.ok)
        assertNull(result.error)
    }
}
