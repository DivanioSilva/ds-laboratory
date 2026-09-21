<#import "/spring.ftl" as spring>
<!DOCTYPE html>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><@spring.message "addresses.form.create"/></title>
    <script src="/js/theme.js"></script>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<main class="container narrow">
    <div class="form-topbar">
        <a class="back-link" href="/persons">← <@spring.message "persons.back"/></a>
        <div class="display-controls">
            <button class="theme-toggle" type="button" data-theme-toggle
                    aria-label="<@spring.message "theme.toggle"/>" title="<@spring.message "theme.toggle"/>">☀</button>
            <nav class="language-switcher" aria-label="<@spring.message "language.label"/>">
                <a href="/addresses/new?lang=pt" class="<#if .locale == 'pt'>active</#if>">PT</a>
                <a href="/addresses/new?lang=en" class="<#if .locale == 'en'>active</#if>">EN</a>
                <a href="/addresses/new?lang=it" class="<#if .locale == 'it'>active</#if>">IT</a>
                <a href="/addresses/new?lang=es" class="<#if .locale == 'es'>active</#if>">ES</a>
                <a href="/addresses/new?lang=fr" class="<#if .locale == 'fr'>active</#if>">FR</a>
                <a href="/addresses/new?lang=de" class="<#if .locale == 'de'>active</#if>">DE</a>
            </nav>
        </div>
    </div>

    <section class="panel form-panel">
        <p class="eyebrow"><@spring.message "addresses.form.context"/></p>
        <h1><@spring.message "addresses.form.create"/></h1>
        <p class="subtitle"><@spring.message "addresses.form.subtitle"/></p>

        <form action="/addresses" method="post" class="person-form">
            <div class="field-grid">
                <div class="field">
                    <label for="country"><@spring.message "addresses.form.country"/></label>
                    <input id="country" name="country" value="${address.country!}" required maxlength="100">
                </div>
                <div class="field">
                    <label for="state"><@spring.message "addresses.form.state"/></label>
                    <input id="state" name="state" value="${address.state!}" required maxlength="100">
                </div>
                <div class="field">
                    <label for="city"><@spring.message "addresses.form.city"/></label>
                    <input id="city" name="city" value="${address.city!}" required maxlength="100">
                </div>
                <div class="field">
                    <label for="door"><@spring.message "addresses.form.door"/></label>
                    <input id="door" name="door" value="${address.door!}" required maxlength="30">
                </div>
                <div class="field">
                    <label for="floor"><@spring.message "addresses.form.floor"/></label>
                    <input id="floor" name="floor" value="${address.floor!}" maxlength="30"
                           placeholder="<@spring.message "addresses.form.floor.placeholder"/>">
                </div>
            </div>
            <div class="field">
                <label for="street"><@spring.message "addresses.form.street"/></label>
                <input id="street" name="street" value="${address.street!}" required maxlength="200">
            </div>
            <div class="form-actions">
                <a class="button ghost" href="/persons"><@spring.message "persons.form.cancel"/></a>
                <button class="button primary" type="submit"><@spring.message "persons.form.save"/></button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
