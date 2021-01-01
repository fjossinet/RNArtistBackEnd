package io.github.fjossinet.rnartist.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import freemarker.cache.ClassTemplateLoader
import io.github.fjossinet.rnartist.core.model.RNA
import io.github.fjossinet.rnartist.core.model.SecondaryStructure
import io.github.fjossinet.rnartist.core.model.SecondaryStructureDrawing
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.gson.*
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.*
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.netty.EngineMain.main
import kotlinx.css.*
import kotlinx.html.*
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import java.io.File
import java.io.FilenameFilter

lateinit var rootDir:File
lateinit var db:Nitrite

fun main(args: Array<String>): Unit  {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println(port)
    embeddedServer(Netty, port, module = Application::module).start(wait = true)
}

@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
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

        get("/viewer") {
            var bn = call.request.queryParameters["bn"]
            var ss = SecondaryStructure(RNA(name = "myRNA", seq = "CGCUGAAUUCAGCG"), bracketNotation = bn)
            var drawing = SecondaryStructureDrawing(secondaryStructure = ss)

            call.respond(FreeMarkerContent("viewer.ftl",null))
        }

        get("/api/draw_2d") {
            var bn = call.request.queryParameters["bn"]
            var ss = SecondaryStructure(RNA(name = "myRNA", seq = "CGCUGAAUUCAGCG"), bracketNotation = bn)
            var drawing = SecondaryStructureDrawing(secondaryStructure = ss)
            call.respond(drawing)

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

        data class Theme(val picture:String)

        get("/api/register_user") {
            val queryParameters: Parameters = call.request.queryParameters
            for (p in queryParameters.entries()) {
                println(p.key)
                println(p.value)
            }
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

        get("/themes") {
            val themes = mutableListOf<Theme>()
            for (doc in db.getCollection("themes").find()) {
                themes.add(Theme(doc.get("picture") as String))
            }
            call.respond(FreeMarkerContent("themes.ftl", mapOf("themes" to themes)))
        }

        static("/static") {
            resources("static")
        }

        static("/captures") {
            staticRootFolder = rootDir
            files("captures")
        }
    }
}
