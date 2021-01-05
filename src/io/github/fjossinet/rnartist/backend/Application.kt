package io.github.fjossinet.rnartist.backend

import freemarker.cache.ClassTemplateLoader
import io.github.fjossinet.rnartist.core.model.*
import io.github.fjossinet.rnartist.core.model.io.parseVienna
import io.github.fjossinet.rnartist.core.model.io.toSVG
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.io.File
import java.io.FilenameFilter
import java.io.StringReader

import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*


lateinit var rootDir:File
lateinit var db:Nitrite
var plotsDone = 0
val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())

fun main(args: Array<String>): Unit  {
    val port = System.getenv("PORT")?.toInt() ?: 8080
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

        get("/s2svg") {
            call.respond(FreeMarkerContent("s2svg.ftl",null))
        }

        post("/s2svg") {
            val postParameters: Parameters = call.receiveParameters()
            var ss:SecondaryStructure? = null
            var sequence: String? = null
            var bn: String? = null
            var colorScheme: String? = null
            var lwSymbols: String? = null
            if (postParameters.contains("examples")) {
                when(postParameters["examples"]) {
                    "Homo sapiens small nucleolar RNA, C/D box 3A" -> {
                        sequence = "AAGACUAUACUUUCAGGGAUCAUUUCUAUAGUGUGUUACUAGAGAAGUUUCUCUGAACGUGUAGAGCACCGAAAACCACGAGGAAGAGAGGUAGCGUUUUCUCCUGAGCGUGAAGCCGGCUUUCUGGCGUUGCUUGGCUGCAACUGCCGUCAGCCAUUGAUGAUCGUUCUUCUCUCCGUAUUGGGGAGUGAGAGGGAGAGAACGCGGUCUGAGUGGU"
                        bn = "(((((((((((..(((((.....))))).)))))).))))).......(((.((......))))).........(((((..............((((((((((((..(((.......((((...(((((((((......))))).))))..)))).........))).(((((((((....))))))).))..)))))))))))).......)))))"
                        colorScheme = "Persian Carolina"
                    }
                    "Thermus thermophilus 5S rRNA" -> {
                        sequence = "AAUCCCCCGUGCCCAUAGCGGCGUGGAACCACCCGUUCCCAUUCCGAACACGGAAGUGAAACGCGCCAGCGCCGAUGGUACUGGGCGGGCGACCGCCUGGGAGAGUAGGUCGGUGCGGGGGA"
                        bn = "..((((((((((.....((((((((....(((((((.............))))..)))...)))))).)).((.((....((((((((....))))))))....)).))...))))))))))"
                        colorScheme = "Charcoal Lazuli"
                    }
                    "Lysine riboswitch RNA from Thermotoga maritima" -> {
                        sequence = "GGACGGAGGCGCGCCCGAGAUGAGUAGGCUGUCCCAUCAGGGGAGGAAUCGGGGACGGCUGAAAGGCGAGGGCGCCGAAGCGAGCAGAGUUCCUCCCGCUCUGCUUGGCUGGGGGUGAGGGGAAUACCCUUACCACUGUCGCGAAAGCGGAGAGCCGUCCA"
                        bn = "((((((....(((((((((.......((((((((..................))))))))......)))))))))...((((((((((((.......))))))))).)))(((((((((((.....))))))))))).((((....))))....))))))."
                        colorScheme = "African Lavender"
                    }
                    "Homo sapiens FGF-2 internal ribosome entry site" -> {
                        sequence = "UUGUGGCCGAAGCCGCCGAACUCAGAGGCCGGCCCCAGAAAACCCGAGCGAGUAGGGGGCGGCGCGCAGGAGGGAGGAGAACUGGGGGCGCGGGAGGCUGGUGGGUGUGGGGGGUGGAGAUGUAGAAGAUGUGACGCCGCGGCCCGGCGGGUGCCAGAUUAGCGGACGGCUGCCCGCGGUUGCAACGGGAUCCCGGGCGCUGCAGCUUGGGAGGCGGCUCUCCCCAGGCGGCGUCCGCGGAGACACCCAUCUGUGAACCCCAGGUCCCGGGCCGCCGGCUCGCCGCGCACCAGGGGCCGGCGGACAGAAGAGCGGCCGAGCGGCUCGAGGCUGGGGGACCGCGGGCGCGGCCGCGCGCUGCCGGGCGGGAGGCUGGGGGGCCGGGGCCGGGGCCGUGCCCGGAGCGGGUCGGAGGCCGGGGCCGGGGCCGGGGGACGGCGGCUCCCCGCGCGGCUCCAGCGGCUCGGGGAUCCCGGCCGGGCCCCGCAGGGACCAUG"
                        bn = ".............................(((((((.......(((...........((((((((((..............((((((.((((.........((((((...................................(((.(((((((((.............))))))))).........)))....((((((((((.....((((((....))))))...))))))))))......))))))..))))..))))))....((((((((((((................))))))....................)))))).........................))))))))))...)))........)))))))..............((((...)))).........((((((.....(((................((((..........))))(((...)))..)))))))))............"
                        colorScheme = "Midnight Paradise"
                    }
                    "Schizosaccharomyces pombe 18S ribosomal RNA" -> {
                        sequence = "UACCUGGUUGAUCCUGCCAGUAGUCAUAUGCUUGUCUCAAAGAUUAAGCCAUGCAUGUCUAAGUAUAAGCAAUUUUGUACUGUGAAACUGCGAAUGGCUCAUUAAAUCAGUUAUCGUUUAUUUGAUAGUACCUCAACUACUUGGAUAACCGUGGUAAUUCUAGAGCUAAUACAUGCUAAAAAUCCCGACUUUUUUGGAAGGGAUGUAUUUAUUAGAUAAAAAACCAAUGCCUUCGGGCUUUUUUUGGUGAGUCAUAAUAACUUUUCGAAUCGCAUGGCCUUGCGCCGGCGAUGGUUCAUUCAAAUUUCUGCCCUAUCAACUUUCGAUGGUAGGAUAGAGGCCUACCAUGGUUUUAACGGGUAACGGGGAAUUAGGGUUCGAUUCCGGAGAGGGAGCCUGAGAAACGGCUACCACAUCCAAGGAAGGCAGCAGGCGCGCAAAUUACCCAAUCCCGACACGGGGAGGUAGUGACAAGAAAUAACAAUGCAGGGCCCUUUCGGGUCUUGUAAUUGGAAUGAGUACAAUGUAAAUACCUUAACGAGGAACAAUUGGAGGGCAAGUCUGGUGCCAGCAGCCGCGGUAAUUCCAGCUCCAAUAGCGUAUAUUAAAGUUGUUGCAGUUAAAAAGCUCGUAGUUGAACUUUGAGCCUGGUCGACUGGUCCGCCGCAAGGCGUGUUUACUGGUCAUGACCGGGGUCGUUAACCUUCUGGCAAACUACUCAUGUUCUUUAUUGAGCGUGGUAGGGAACCAGGACUUUUACCUUGAAAAAAUUAGAGUGUUCAAAGCAGGCAAGUUUUGCUCGAAUACAUUAGCAUGGAAUAAUAAAAUAGGACGUGUGGUUCUAUUUUGUUGGUUUCUAGGACCGCCGUAAUGAUUAAUAGGGAUAGUCGGGGGCAUUCGUAUUCAAUUGUCAGAGGUGAAAUUCUUGGAUUUAUUGAAGACGAACUACUGCGAAAGCAUUUGCCAAGGAUGUUUUCAUUAAUCAAGAACGAAAGUUAGGGGAUCGAAGACGAUCAGAUACCGUCGUAGUCUUAACCAUAAACUAUGCCGACUAGGGAUCGGGCAAUGUUUCAUUUAUCGACUUGCUCGGCACCUUACGAGAAAUCAAAGUCUUUGGGUUCCGGGGGGAGUAUGGUCGCAAGGCUGAAACUUAAAGGAAUUGACGGAAGGGCACCACAAUGGAGUGGAGCCUGCGGCUUAAUUUGACUCAACACGGGGAAACUCACCAGGUCCAGACAUAGUAAGGAUUGACAGAUUGAGAGCUCUUUCUUGAUUCUAUGGGUGGUGGUGCAUGGCCGUUCUUAGUUGGUGGAGUGAUUUGUCUGCUUAAUUGCGAUAACGAACGAGACCUUAACCUGCUAAAUAGCUGGAUCAGCCAUUUUGGCUGAUCAUUAGCUUCUUAGAGGGACUAUUGGCAUAAAGCCAAUGGAAGUUUGAGGCAAUAACAGGUCUGUGAUGCCCUUAGAUGUUCUGGGCCGCACGCGCGCUACACUGACGGAGCCAACGAGUUGAAAAAAAUCUUUUGAUUUUUUAUCCUUGGCCGGAAGGUCUGGGUAAUCUUGUUAAACUCCGUCGUGCUGGGGAUAGAGCAUUGCAAUUAUUGCUCUUCAACGAGGAAUUCCUAGUAAGCGCAAGUCAUCAGCUUGCGUUGAAUACGUCCCUGCCCUUUGUACACACCGCCCGUCGCUACUACCGAUUGAAUGGCUUAGUGAGGCCUCUGGAUUGGCUUGUUUCUGCUGGCAACGGCGGAAACAUUGCCGAGAAGUUGGACAAACUUGGUCAUUUAGAGGAAGUAAAAGUCGUAACAAGGUUUCCGUAGGUGAACCUGCGGAAGGAUCAUUA"
                        bn = "...((((.........))))((((.(((((((.(((((((((.....(((.(((..((...(((..(.((...........)))..))))).....((((.......(((((((..((..(((((((............(((((...(((((((.....)))))))....)))))......(((((((((.....)))))))))(((.(((((((.......(((((.(((....))).....))))).....))))))).)..))...((((.((((.....))))))))..))))))).))))))))).(((..(.(((....((((((((.......))))))))))).....))))...((((((((....))))...))))))))((((((..........)))))).((((....))))...)))))))......(.(((...(((((...))))).)))).)).))))))....((((..(((((((....)))))))..).))).....((((((((.......))))))))........((.((......(.((((((..(((....)))....))))))))).)).))))))))))).....(...(((.......((((...(((.((....((((((((((...((((.(((........)))...)))).....)))))))))).......((((((....((((..(((((........))))).))))....))))))..(((((((((.......(((..(.(...).)..(((.......)))...)))......)))))..)))).....(.((....(.((.(((.............))).))..)..)).)..))...((((((((((.((((((((((((((((((((...(((......)))......))))))))))))....(..((....)))))))))))))))).))))..))))...)))).(..((((((...(((.(((((.........))))).)))))))))..).......((((((.(((..(((((((...((...........)))))))))..)))...((....))...)))....))).))))(((((.((.((((....)))))))))))........(((((.(((((((..((...(((((((((((((((((.(.)((((........))))........(((((((....(((((....(((((((((..........)))))))))..))))).(.((.((((..((((((((((..(((((((((....)))..((((......))))..)))))).....((((((((.((((..(((((.((((((.......))))))...)))))..))))))).((.(((((((...)))))))))....)))))...))))).)))...).))))))))....)))))))...)).)))))))))((..(((((((.(...(((..........................(((.((((....)))).)))....)))....).)))))))....).((((((((((((........))))))))))))..).))))))(...(((((((((.......)))))))))..)..))...)))))))))).))....((.((...(((((((((((.((((((((((((..(((((((((((((((((((((((((((....))))))))))).))))))))))))))))..)))))))))))))))))))))))....))..))....((((((((((....))))))))))........"
                        colorScheme = "Pacific Dream"
                    }
                    "Homo sapiens RNA component of 7SK nuclear ribonucleoprotein" -> {
                        sequence = "GGAUGUGAGGGCGAUCUGGCUGCGACAUCUGUCACCCCAUUGAUCGCCAGGGUUGAUUCGGCUGAUCUGGCUGGCUAGGCGGGUGUCCCCUUCCUCCCUCACCGCUCCAUGUGCGUCCCUCCCGAAGCUGCGCGCUCGGUCGAAGAGGACGACCAUCCCCGAUAGAGGAGGACCGGUCUUCGGUCAAGGGUAUACGAGUAGCUGCGCUCCCCUGCUAGAACCUCCAAACAAGCUCUCAAGGUCCAUUUGUAGGAGAACGUAGGGUAGUCAAGCUUCCAAGACUCCAGACACAUCCAAAUGAGGCGCUGCAUGUGGCAGUCUGCCUUUCUUUU"
                        bn = "(((.(((((((.((...((....((((((((((.(((((..(((((((...........))).)))))))..))...)))))))))).)).))..)))))))...))).......((((((((((.((.....))))))(((....(((......))).)))..)))).)).............................(((...((.((((((......((((..(((((.............))))).))))...)))))).))...)))......................(((.((((((((((.....)))))..)))))...)))"
                        colorScheme = "Atomic Xanadu"
                    }
                }
            } else
                postParameters.forEach { s, list ->
                    when(s) {
                           "seq" -> sequence = list.get(0).trim()
                           "bn" -> bn = list.get(0).trim()
                           "color-schemes" -> colorScheme = list.get(0).trim()
                           "lw-symbols" -> lwSymbols = list.get(0).trim()
                       }
                }
            try {
                ss = parseVienna(StringReader(">A\n$sequence\n$bn"))
                val ws = WorkingSession()
                val t = Theme()
                t.setConfigurationFor(SecondaryStructureType.Helix, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.SecondaryInteraction, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.SingleStrand, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.SecondaryInteraction, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.InteractionSymbol, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.PhosphodiesterBond, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.Junction, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.AShape, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.UShape, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.GShape, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.CShape, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.XShape, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.A, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.U, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.G, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.C, DrawingConfigurationParameter.fulldetails, "true")
                t.setConfigurationFor(SecondaryStructureType.X, DrawingConfigurationParameter.fulldetails, "true")

                RnartistConfig.colorSchemes.get(colorScheme)!!.forEach { elementType, config ->
                    config.forEach {
                        t.setConfigurationFor(SecondaryStructureType.valueOf(elementType), DrawingConfigurationParameter.valueOf(it.key), it.value)
                    }
                }

                val drawing = SecondaryStructureDrawing(ss, workingSession = ws)

                val frame = Rectangle(0, 0, 1920, 1080)

                //we compute the zoomLevel to fit the structure in the frame of the canvas2D
                val widthRatio = drawing.getBounds().bounds2D.width / frame.bounds2D.width
                val heightRatio = drawing.getBounds().bounds2D.height / frame.bounds2D.height
                drawing.workingSession.finalZoomLevel =
                    if (widthRatio > heightRatio) 1.0 / widthRatio else 1.0 / heightRatio
                var at = AffineTransform()
                at.scale(drawing.workingSession.finalZoomLevel, drawing.workingSession.finalZoomLevel)
                val transformedBounds = at.createTransformedShape(drawing.getBounds())
                drawing.workingSession.viewX = frame.bounds2D.centerX - transformedBounds.bounds2D.centerX
                drawing.workingSession.viewY = frame.bounds2D.centerY - transformedBounds.bounds2D.centerY

                val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
                val g = image.createGraphics()

                drawing.workingSession.setFont(g, drawing.residues.first())

                drawing.applyTheme(t)
                drawing.workingSession.junctionsDrawn.addAll(drawing.allJunctions)
                drawing.workingSession.helicesDrawn.addAll(drawing.allHelices)
                drawing.workingSession.singleStrandsDrawn.addAll(drawing.singleStrands)
                drawing.workingSession.phosphoBondsLinkingBranchesDrawn.addAll(drawing.phosphoBonds)
                drawing.workingSession.locationDrawn = Location(1, drawing.secondaryStructure.length)

                at = AffineTransform()
                at.translate(drawing.workingSession.viewX, drawing.workingSession.viewY)
                at.scale(drawing.workingSession.finalZoomLevel, drawing.workingSession.finalZoomLevel)
                plotsDone++
                System.out.println("Plots done since ${currentDate}: ${plotsDone}");
                call.respond(
                    FreeMarkerContent(
                        "s2svg.ftl", mapOf(
                            "svg" to toSVG(drawing, frame, at, TertiariesDisplayLevel.None),
                            "seq" to sequence,
                            "bn" to bn
                        )
                    )
                )

            } catch (e:Exception) {
                call.respond(
                    FreeMarkerContent(
                        "s2svg.ftl", mapOf(
                            "svg" to "<div class=\"alert alert-danger\" role=\"alert\">\n" +
                                    "I cannot plot your structure. Please check your data.\n" +
                                    "</div>",
                            "seq" to sequence,
                            "bn" to bn
                        )
                    )
                )
            }

        }

        get("/news") {
            call.respond(FreeMarkerContent("news.ftl",null))
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

        post("/api/submit_layout") {
            // retrieve all multipart data (suspending)
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                val layout = Document()
                if (part is PartData.FormItem) {
                    layout.put(part.name, part.value)
                }
                // if part is a file (could be form item)
                if(part is PartData.FileItem) {
                    // retrieve file name of upload
                    val name = part.originalFileName!!
                    val filter = FilenameFilter { dir: File?, name: String -> name.endsWith(".png") }
                    val i = File(rootDir, "captures").listFiles(filter).size+1
                    val file = File(File(rootDir, "captures"),"theme_$i.png")
                    layout.put("picture", "theme_$i.png")

                    // use InputStream from part to save file
                    part.streamProvider().use { its ->
                        // copy the stream to the file with buffering
                        file.outputStream().buffered().use {
                            // note that this is blocking
                            its.copyTo(it)
                        }
                    }

                    //db.getCollection("themes").insert(layout)
                }
                println(layout.toString())
                // make sure to dispose of the part after use to prevent leaks
                part.dispose()
            }
        }

        get("/themes") {
            call.respond(FreeMarkerContent("themes.ftl", null))
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
