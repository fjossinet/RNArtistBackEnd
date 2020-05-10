package fr.unistra.fjossinet.rnartist.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.*
import io.ktor.jackson.jackson
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.css.*
import kotlinx.html.*
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import java.io.File
import java.io.FilenameFilter

lateinit var rootDir:File
lateinit var db:Nitrite

fun main(args: Array<String>): Unit  {
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

    rootDir = File(System.getProperty("user.home"),".rnartistbackend")
    if (!rootDir.exists()) {
        rootDir.mkdir()
        File(rootDir,"captures").mkdir()
    }
    db = Nitrite.builder()
        .compressed()
        .filePath(File(rootDir,"db").absolutePath)
        .openOrCreate()

    routing {
        get("/") {
            call.respond(FreeMarkerContent("index.ftl",null))
        }

        get("/news") {
            call.respond(FreeMarkerContent("news.ftl",null))
        }

        get("/layouts") {
            call.respond(FreeMarkerContent("layouts.ftl",null))
        }

        get("/downloads") {
            call.respond(FreeMarkerContent("downloads.ftl",null))
        }

        get("/contact") {
            call.respond(FreeMarkerContent("contact.ftl",null))
        }

        get("/api/register_user") {
            val queryParameters: Parameters = call.request.queryParameters
            for (p in queryParameters.entries()) {
                println(p.key)
                println(p.value)
            }
        }

        data class Theme(val picture:String)

        get("/themes") {
            val themes = mutableListOf<Theme>()
            for (doc in db.getCollection("themes").find()) {
                themes.add(Theme(doc.get("picture") as String))
            }
            call.respond(FreeMarkerContent("themes.ftl", mapOf("themes" to themes)))
        }

        get("/api/all_themes") {
            val themes = mutableListOf<Theme>()
            for (doc in db.getCollection("themes").find()) {
                themes.add(Theme(doc.get("picture") as String))
            }
            val mapper = ObjectMapper()
            call.respond(mapper.writeValueAsString(themes))
        }

        post("/api/submit_theme") {
            // retrieve all multipart data (suspending)
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                val theme = Document()
                if (part is PartData.FormItem) {
                    theme.put(part.name, part.value)
                }
                // if part is a file (could be form item)
                if(part is PartData.FileItem) {
                    // retrieve file name of upload
                    val name = part.originalFileName!!
                    val filter = FilenameFilter { dir: File?, name: String -> name.endsWith(".png") }
                    val i = File(rootDir, "captures").listFiles(filter).size+1
                    val file = File(File(rootDir, "captures"),"theme_$i.png")
                    theme.put("picture", "theme_$i.png")

                    // use InputStream from part to save file
                    part.streamProvider().use { its ->
                        // copy the stream to the file with buffering
                        file.outputStream().buffered().use {
                            // note that this is blocking
                            its.copyTo(it)
                        }
                    }

                    db.getCollection("themes").insert(theme)
                }
                // make sure to dispose of the part after use to prevent leaks
                part.dispose()
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

        static("/static") {
            resources("static")
        }

        static("/captures") {
            staticRootFolder = rootDir
            files("captures")
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
