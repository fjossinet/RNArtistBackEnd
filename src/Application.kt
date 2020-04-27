package fr.unistra.rnartist.backend

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.content.*
import io.ktor.http.content.*
import com.fasterxml.jackson.databind.*
import freemarker.cache.ClassTemplateLoader
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import org.dizitart.no2.Nitrite
import java.io.File

lateinit var db:Nitrite

fun main(args: Array<String>): Unit  {
    db = Nitrite.builder()
            .compressed()
            .filePath(File.createTempFile("rnartist","backend").absolutePath)
            .openOrCreate()
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    routing {
        get("/") {
            data class User(val name: String, val email: String)
            val user = User("Fabrice", "user@example.com")
            call.respond(FreeMarkerContent("index.ftl", mapOf("user" to user)))
        }

        get("/news") {
            call.respond(FreeMarkerContent("news.ftl",null))
        }

        get("/layouts") {
            call.respond(FreeMarkerContent("layouts.ftl",null))
        }

        get("/themes") {
            call.respond(FreeMarkerContent("themes.ftl",null))
        }

        get("/downloads") {
            call.respond(FreeMarkerContent("downloads.ftl",null))
        }

        get("/contact") {
            call.respond(FreeMarkerContent("contact.ftl",null))
        }

        get("/register") {
            val queryParameters: Parameters = call.request.queryParameters
            for (p in queryParameters.entries()) {
                println(p.key)
                println(p.value)
            }
        }

        get("/submit_theme") {
            val queryParameters: Parameters = call.request.queryParameters
            for (p in queryParameters.entries()) {
                println(p.key)
                println(p.value)
            }
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
