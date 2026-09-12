package id.hiltons.linksanitiser

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkSanitiserTest {

    private val defaultConfig = SanitiserConfig()

    @Test
    fun `strips utm parameters`() {
        val input = "https://example.com/article?utm_source=twitter&utm_medium=social&id=42"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/article?id=42", result.text)
        assertEquals(2, result.paramsRemoved)
        assertEquals(1, result.urlsChanged)
    }

    @Test
    fun `strips click id parameters`() {
        val input = "https://example.com/?fbclid=abc123"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/", result.text)
        assertEquals(1, result.paramsRemoved)
    }

    @Test
    fun `leaves referral parameters alone by default`() {
        val input = "https://example.com/?ref=homepage"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals(input, result.text)
        assertEquals(0, result.paramsRemoved)
    }

    @Test
    fun `strips referral parameters when enabled`() {
        val input = "https://example.com/?ref=homepage&id=1"
        val config = defaultConfig.copy(stripReferral = true)
        val result = LinkSanitiser.sanitise(input, config)
        assertEquals("https://example.com/?id=1", result.text)
    }

    @Test
    fun `strips custom parameters`() {
        val input = "https://example.com/?cid=999&id=1"
        val config = defaultConfig.copy(customParams = setOf("cid"))
        val result = LinkSanitiser.sanitise(input, config)
        assertEquals("https://example.com/?id=1", result.text)
    }

    @Test
    fun `removes trailing question mark when all params stripped`() {
        val input = "https://example.com/page?utm_source=x"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/page", result.text)
    }

    @Test
    fun `preserves url fragment`() {
        val input = "https://example.com/page?utm_source=x#section-2"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/page#section-2", result.text)
    }

    @Test
    fun `leaves url with no tracking params untouched`() {
        val input = "https://example.com/page?id=42"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals(input, result.text)
        assertEquals(0, result.paramsRemoved)
        assertEquals(0, result.urlsChanged)
    }

    @Test
    fun `sanitises a url embedded in surrounding message text`() {
        val input = "Check this out: https://example.com/a?utm_source=ig&fbclid=xyz - thoughts?"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("Check this out: https://example.com/a - thoughts?", result.text)
        assertEquals(2, result.paramsRemoved)
    }

    @Test
    fun `strips trailing sentence punctuation but keeps it after the url`() {
        val input = "See https://example.com/?utm_source=x."
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("See https://example.com/.", result.text)
    }

    @Test
    fun `keeps a matching closing paren that belongs to the url`() {
        val input = "(see https://example.com/wiki/Foo_(bar)?utm_source=x)"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("(see https://example.com/wiki/Foo_(bar))", result.text)
    }

    @Test
    fun `handles multiple urls in one message`() {
        val input = "https://a.com/?utm_source=x and https://b.com/?fbclid=y"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://a.com/ and https://b.com/", result.text)
        assertEquals(2, result.urlsChanged)
        assertEquals(2, result.paramsRemoved)
    }

    @Test
    fun `matches utm parameters by prefix even if not in the known list`() {
        val input = "https://example.com/?utm_totally_custom=1&id=2"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/?id=2", result.text)
    }

    @Test
    fun `is case insensitive when matching parameter names`() {
        val input = "https://example.com/?UTM_Source=ig&id=2"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals("https://example.com/?id=2", result.text)
    }

    @Test
    fun `plain text with no url is unchanged`() {
        val input = "no links here, just words"
        val result = LinkSanitiser.sanitise(input, defaultConfig)
        assertEquals(input, result.text)
        assertEquals(0, result.urlsChanged)
    }
}
