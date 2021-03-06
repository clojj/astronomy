package com.github.clojj.astronomy

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit


@SpringBootApplication
@EnableScheduling
class AstronomyApplication

fun main(args: Array<String>) {
    runApplication<AstronomyApplication>(*args)
}


@ExperimentalCoroutinesApi
@RestController
@RequestMapping("/astronomy")
class Controller(configProperties: ConfigProperties) {

    private val webClient = createWebClientWithConnectAndReadTimeOuts(token = configProperties.token)

    private val scope = CoroutineScope(Job() + Dispatchers.Default + CoroutineName("parent scope for processing"))

    @GetMapping("/firstpage", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun page(): Flow<Map<String, String>> {
        val result: Mono<Flow<Map<String, String>>> = webClient
            .get()
            .uri("/user/starred")
            .accept(MediaType.APPLICATION_JSON)
            .exchange().map { value: ClientResponse ->
                next(value.headers().header("Link"))
                value.bodyToFlow<Repository>().map { mapOf(Pair(it.name, it.description ?: "NO_DESCRIPTON")) }
            }
        return result.awaitFirst()
    }

    private fun next(link: List<String>) {
        println(link)
    }
}

@Component
@ConfigurationProperties(prefix = "application.github")
data class ConfigProperties(
    var token: String = ""
)

private const val GITHUB_V3_MIME_TYPE = "application/vnd.github.v3+json"
private const val GITHUB_API_BASE_URL = "https://api.github.com"

private fun createWebClientWithConnectAndReadTimeOuts(connectTimeOut: Int = 5000, readTimeOut: Long = 5000, token: String): WebClient {
    // create reactor netty HTTP client
    val httpClient: HttpClient = HttpClient.create()
        .tcpConfiguration { tcpClient ->
            tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOut)
            tcpClient.doOnConnected { conn -> conn.addHandlerLast(ReadTimeoutHandler(readTimeOut, TimeUnit.MILLISECONDS)) }
            tcpClient
        }
    // create a client http connector using above http client
    val connector: ClientHttpConnector = ReactorClientHttpConnector(httpClient)
    // use this configured http connector to build the web client
    return WebClient.builder()
        .baseUrl(GITHUB_API_BASE_URL)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, GITHUB_V3_MIME_TYPE)
        .filter(ExchangeFilterFunctions.basicAuthentication("clojj", token))
        .clientConnector(connector).build()
}

data class User(
    val login: String,
    val id: Long,
    val node_id: String,
    val avatar_url: String,
    val gravatar_id: String,
    val url: String,
    val html_url: String,
    val followers_url: String,
    val following_url: String,
    val gists_url: String,
    val starred_url: String,
    val subscriptions_url: String,
    val organizations_url: String,
    val repos_url: String,
    val events_url: String,
    val received_events_url: String,
    val type: String,
    val site_admin: Boolean
)

data class Repository(
    val id: Long,
    val node_id: String,
    val name: String,
    val full_name: String,
    val owner: User,
    val private: Boolean,
    val html_url: String,
    val description: String?,
    val fork: Boolean,
    val url: String,
    val archive_url: String,
    val assignees_url: String,
    val blobs_url: String,
    val branches_url: String,
    val collaborators_url: String,
    val comments_url: String,
    val commits_url: String,
    val compare_url: String,
    val contents_url: String,
    val contributors_url: String,
    val deployments_url: String,
    val downloads_url: String,
    val events_url: String,
    val forks_url: String,
    val git_commits_url: String,
    val git_refs_url: String,
    val git_tags_url: String,
    val git_url: String,
    val issue_comment_url: String,
    val issue_events_url: String,
    val issues_url: String,
    val keys_url: String,
    val labels_url: String,
    val languages_url: String,
    val merges_url: String,
    val milestones_url: String,
    val notifications_url: String,
    val pulls_url: String,
    val releases_url: String,
    val ssh_url: String,
    val stargazers_url: String,
    val statuses_url: String,
    val subscribers_url: String,
    val subscription_url: String,
    val tags_url: String,
    val teams_url: String,
    val trees_url: String
)

/*
@Component
class ScheduledTasks {

    private val webClient = createWebClientWithConnectAndReadTimeOuts()

    @Scheduled(fixedRate = 4000)
    fun githubAPI() {
        log.info("The time is now ${dateFormat.format(Date())}")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScheduledTasks::class.java)
        private val dateFormat = SimpleDateFormat("HH:mm:ss")
    }
}*/

@ExperimentalCoroutinesApi
@Configuration
class Routes {
    @Bean
    fun route() =
        router {
            "/experimental".nest {
                (GET("/test")).invoke {
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(mapOf("result" to "TODO functional style")))
                }
            }
        }
}
