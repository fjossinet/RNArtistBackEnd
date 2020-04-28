<#include "/header.ftl">
<main role="main">
    <section class="jumbotron  bg-dark text-center py-5">
        <div class="container">
            <h1 class="text-white display-4">Themes</h1>
            <p class="lead text-white">RNA 2D Themes submitted by the User Community</p>
        </div>
    </section>

    <div class="container">

        <div class="row text-center text-lg-left">

            <#list themes as theme>
                <div class="col-lg-3 col-md-4 col-6">
                    <a href="#" class="d-block mb-4 h-100">
                        <img class="img-fluid img-thumbnail" src="/captures/${theme.picture}" alt="">
                    </a>
                </div>
            </#list>

        </div>

    </div>

    <#include "/footer.ftl">