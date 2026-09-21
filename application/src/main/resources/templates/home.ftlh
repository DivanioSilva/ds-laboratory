<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><@spring.message "home.title"/></title>
    <script src="/js/theme.js"></script>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<main class="landing-shell">
    <section class="landing-card">
        <div class="landing-actions">
            <nav class="language-links" aria-label="<@spring.message 'language.label'/>">
                <span><@spring.message "language.label"/>:</span>
                <a href="/?lang=pt" class="<#if .locale?starts_with('pt')>active</#if>">PT</a>
                <a href="/?lang=en" class="<#if .locale?starts_with('en')>active</#if>">EN</a>
                <a href="/?lang=it" class="<#if .locale?starts_with('it')>active</#if>">IT</a>
                <a href="/?lang=es" class="<#if .locale?starts_with('es')>active</#if>">ES</a>
                <a href="/?lang=fr" class="<#if .locale?starts_with('fr')>active</#if>">FR</a>
                <a href="/?lang=de" class="<#if .locale?starts_with('de')>active</#if>">DE</a>
            </nav>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="<@spring.message 'theme.toggle'/>">☀</button>
        </div>
        <p class="eyebrow"><@spring.message "persons.form.context"/></p>
        <h1><@spring.message "home.title"/></h1>
        <p><@spring.message "home.description"/></p>
        <ul>
            <li><@spring.message "home.feature.records"/></li>
            <li><@spring.message "home.feature.import"/></li>
            <li><@spring.message "home.feature.security"/></li>
        </ul>
        <#if authenticated>
            <p class="authenticated-user"><@spring.message "home.signedIn"/> <strong>${username}</strong></p>
            <div class="landing-button-row">
                <a class="button primary login-button" href="/persons"><@spring.message "home.openApp"/></a>
                <form action="/logout" method="post"><button class="button ghost login-button" type="submit"><@spring.message "home.logout"/></button></form>
            </div>
        <#else>
            <a class="button primary login-button" href="/oauth2/authorization/keycloak"><@spring.message "home.login"/></a>
            <small><@spring.message "home.loginHint"/></small>
        </#if>
    </section>
</main>
</body>
</html>
