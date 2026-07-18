package com.limeday.app.sync

import kotlinx.coroutines.test.runTest
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebDavClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: WebDavClient
    private lateinit var config: WebDavConfig

    @Before
    fun setUp() {
        val certificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()

        server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        client = WebDavClient(
            OkHttpClient.Builder()
                .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
                .build()
        )
        config = WebDavConfig(
            baseUrl = server.url("/dav/").toString(),
            username = "user",
            password = "secret",
            directory = "LimeDay"
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connection test uses authenticated propfind`() = runTest {
        server.enqueue(MockResponse().setResponseCode(207))

        client.test(config)

        val request = server.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("0", request.getHeader("Depth"))
        assertEquals(Credentials.basic("user", "secret"), request.getHeader("Authorization"))
    }

    @Test
    fun `missing remote snapshot is treated as first sync`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))

        val snapshot = client.download(config)

        assertNull(snapshot)
        assertTrue(server.takeRequest().path?.endsWith("/LimeDay/limeday-sync-v2.json") == true)
        assertTrue(server.takeRequest().path?.endsWith("/LimeDay/limeday-sync-v1.json") == true)
    }

    @Test
    fun `download falls back to v1 only when v2 is absent`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                SyncSnapshot(
                    formatVersion = 1,
                    generatedAt = 1,
                    deviceId = "legacy-device",
                    todos = emptyList(),
                    reviews = emptyList(),
                    summaries = emptyList()
                ).toJson()
            )
        )

        val snapshot = client.download(config)

        assertEquals(2, snapshot?.formatVersion)
        assertTrue(server.takeRequest().path?.endsWith("/LimeDay/limeday-sync-v2.json") == true)
        assertTrue(server.takeRequest().path?.endsWith("/LimeDay/limeday-sync-v1.json") == true)
    }

    @Test
    fun `upload creates collection and never serializes password`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        server.enqueue(MockResponse().setResponseCode(201))
        val snapshot = SyncSnapshot(
            generatedAt = 1,
            deviceId = "device-a",
            todos = emptyList(),
            reviews = emptyList(),
            summaries = emptyList()
        )

        client.upload(config, snapshot)

        val create = server.takeRequest()
        val upload = server.takeRequest()
        assertEquals("MKCOL", create.method)
        assertEquals("PUT", upload.method)
        assertTrue(upload.path?.endsWith("/LimeDay/limeday-sync-v2.json") == true)
        val body = upload.body.readUtf8()
        assertTrue(body.contains("formatVersion"))
        assertFalse(body.contains("secret"))
    }
}
