package song.vault.data.remote.youtube

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for n8n webhook calls
 */
interface N8nWebhookService {

    @POST("webhook/ab94a551-ec95-41a8-b73f-e8a4a039931b")
    suspend fun searchMusic(@Body request: N8nSearchRequest): N8nSearchResponse
}
