<#include "/header.ftl">
<main role="main">
    <section class="jumbotron  bg-dark text-center py-5">
        <div class="container">
            <h1 class="text-white display-4">S2SVG</h1>
            <p class="lead text-white">Powered by the drawing engine of RNArtist</p>
        </div>
    </section>
    <div class="container">
        <h2>Overview</h2>
        <div class="row">
            <div class="col-12">
                <p>S2SVG (for Structure to  SVG) transforms a dot-bracket description of an RNA 2D structure into a beautiful 2D sketch. You can then further customize your 2D with tools like Adobe illustator or <a href="https://affinity.serif.com/" target="_blank">the Affinity Suite</a>. Like RNArtist, S2SVG is powered by the drawing engine implemented in the project <a href="https://github.com/fjossinet/RNArtistCore" target="_blank">RNArtistCore</a>.</p>
                <div class="alert alert-danger" role="alert">
                    <h4 class="alert-heading">This is an early release.</h4>
                    <p>The drawing algorithm needs improvements. Each plot will reload the entire page (that's why you will loose your options). The next version will based on WebSocket.</p>
                    <p>S2SVG runs on a public cloud (<a href="http://heroku.com" target="_blank">Heroku</a>) with a free plan. Free means that the server is slow. If you find this tool useful, <a href="https://twitter.com/rnartist_app" target="_blank">let me know</a> to improve the chances to move to a paying option (as a "laboratoryless" bioinformatician, I'm developing all these projects without any support). </p>
                    <p>No data are stored on the server. S2SVG just keeps track of the number of RNA plots computed.</p>
                </div>
            </div>
        </div>
        <h2>Examples</h2>
        <div class="row">
            <div class="col-12">
                <form action="/s2svg" method="post">
                    <fieldset>
                        <p>Choose a structure from RNA Central and click on the Plot! button</p>
                        <div class="form-group row">
                            <div class="col-sm-6">
                                <select class="form-control" name="examples" id="examples">
                                    <option>Thermus thermophilus 5S rRNA</option>
                                    <option>Lysine riboswitch RNA from Thermotoga maritima</option>
                                    <option>Homo sapiens RNA component of 7SK nuclear ribonucleoprotein</option>
                                    <option>Homo sapiens small nucleolar RNA, C/D box 3A</option>
                                    <option>Homo sapiens FGF-2 internal ribosome entry site</option>
                                    <option>Schizosaccharomyces pombe 18S ribosomal RNA</option>
                                </select>
                            </div>
                            <div class="col-sm-2">
                                <button type="submit" class="btn btn-primary">Plot!</button>
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
        <h2>Plot your RNA</h2>
        <div class="row">
            <div class="col-12">
                <form action="/s2svg" method="post">
                    <fieldset>
                        <legend><strong>Step1:</strong> describe your RNA</legend>
                        <div class="form-group">
                            <label for="seq">Your sequence</label>
                            <div class="col-sm-12">
                                <textarea class="form-control" style="min-width: 100%" name="seq" id="seq" rows="3"><#if seq??>${seq}<#else>GGGACCGCCCGGGAAACGGGCGAAAAACGAGGUGCGGGCACCUCGUGACGACGGGAGUUCGACCGUGACGCAUGCGGAAAUUGGAGGUGAGUUCGCGAAUACGCAAGCGAAUACGCCCUGCUUACCGAAGCAAGCG</#if></textarea>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="bn">Your structure (dot-bracket notation)</label>
                            <div class="col-sm-12">
                                <textarea class="form-control" style="min-width: 100%" name="bn" id="bn" rows="3"><#if bn??>${bn}<#else>.....((((((.....))))))....((((((((....))))))))....((((........))))..(((.(((..........(((((((..(((....)))..(((....)))...)))))))...))).)))</#if></textarea>
                            </div>
                        </div>
                    </fieldset>
                    <fieldset>
                        <legend><strong>Step2:</strong> choose your options</legend>
                        <div class="form-group alert alert-secondary" role="alert">
                            more options will be available after the first plot.
                        </div>
                        <div class="form-group row">
                            <label for="color-schemes" class="col-sm-2 col-form-label">Color Schemes</label>
                            <div class="col-sm-2">
                                <select class="form-control" name="color-schemes" id="color-schemes">
                                    <option>Persian Carolina</option>
                                    <option>Snow Lavender</option>
                                    <option>Fuzzy French</option>
                                    <option>Chestnut Navajo</option>
                                    <option>Charm Jungle</option>
                                    <option>Atomic Xanadu</option>
                                    <option>Pale Coral</option>
                                    <option>Maximum Salmon</option>
                                    <option>Pacific Dream</option>
                                    <option>New York Camel</option>
                                    <option>Screamin' Olive</option>
                                    <option>Baby Lilac</option>
                                    <option>Celeste Olivine</option>
                                    <option>Midnight Paradise</option>
                                    <option>African Lavender</option>
                                    <option>Charcoal Lazuli</option>
                                    <option>Pumpkin Vegas</option>
                                </select>
                            </div>
                            <!--<label for="color-schemes" class="col-sm-2 col-form-label">LW Symbols</label>
                            <div class="col-sm-2">
                                <select class="form-control" name="lw-symbols" id="lw-symbols">
                                    <option>Display</option>
                                    <option>Hide</option>
                                </select>
                            </div>-->
                        </div>
                    </fieldset>
                    <button type="submit" class="btn btn-primary">Plot!</button>
                </form>
            </div>
        </div>
        <div class="row">
            <div class="col-12" style="margin-bottom: 20px;">
            </div>
        </div>
        <#if svg??>
        <div class="shadow-lg p-3 mb-5 bg-white rounded" id="sketch">
        ${svg}
        </div>
            <center><button onclick="downloadSVG()" class="btn btn-primary">Download SVG File</button></center>
            <script>
                function downloadSVG() {
                    var data = document.getElementById('sketch').innerHTML;
                    var DOMURL = window.URL || window.webkitURL || window;
                    var svgBlob = new Blob([data], {type: 'image/svg+xml;charset=utf-8'});
                    var url = DOMURL.createObjectURL(svgBlob);
                    var a = document.createElement('a');
                    a.style = "display: none";
                    a.href = url;
                    a.download = "sketch.svg";
                    document.getElementById('sketch').appendChild(a);
                    a.click();
                }

                function bottom() {
                    window.scrollTo(0,document.body.scrollHeight);
                }
                bottom()
            </script>
        </#if>
    </div>
<#include "/footer.ftl">