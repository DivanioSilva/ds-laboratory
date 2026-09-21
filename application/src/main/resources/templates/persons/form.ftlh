<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><#if person.id??><@spring.message "persons.form.edit"/><#else><@spring.message "persons.form.create"/></#if></title>
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
                <#if person.id??>
                    <#assign languagePath="/persons/${person.id}/edit">
                <#else>
                    <#assign languagePath="/persons/new">
                </#if>
                <a href="${languagePath}?lang=pt" class="<#if .locale?starts_with('pt')>active</#if>">PT</a>
                <a href="${languagePath}?lang=en" class="<#if .locale?starts_with('en')>active</#if>">EN</a>
                <a href="${languagePath}?lang=it" class="<#if .locale?starts_with('it')>active</#if>">IT</a>
                <a href="${languagePath}?lang=es" class="<#if .locale?starts_with('es')>active</#if>">ES</a>
                <a href="${languagePath}?lang=fr" class="<#if .locale?starts_with('fr')>active</#if>">FR</a>
                <a href="${languagePath}?lang=de" class="<#if .locale?starts_with('de')>active</#if>">DE</a>
            </nav>
        </div>
    </div>

    <section class="panel form-panel">
        <p class="eyebrow"><#if person.id??><@spring.message "persons.form.edit.context"/><#else><@spring.message "persons.form.create.context"/></#if></p>
        <h1><#if person.id??><@spring.message "persons.form.edit"/><#else><@spring.message "persons.form.create"/></#if></h1>
        <p class="subtitle"><#if person.id??><@spring.message "persons.form.edit.subtitle"/><#else><@spring.message "persons.form.create.subtitle"/></#if></p>

        <#if person.id??>
            <#assign formAction="/persons/${person.id}">
        <#else>
            <#assign formAction="/persons">
        </#if>

        <form action="${formAction}" method="post" class="person-form">
            <#if person.id??>
                <input type="hidden" name="id" value="${person.id}">
            </#if>
            <div class="field-grid">
                <div class="field">
                    <label for="firstName"><@spring.message "persons.form.firstName"/></label>
                    <input id="firstName" name="firstName" value="${person.firstName!}" required maxlength="100">
                </div>
                <div class="field">
                    <label for="lastName"><@spring.message "persons.form.lastName"/></label>
                    <input id="lastName" name="lastName" value="${person.lastName!}" required maxlength="100">
                </div>
            </div>
            <div class="field age-field">
                <label for="age"><@spring.message "persons.form.age"/></label>
                <input id="age" name="age" type="number" value="${person.age!}"
                       min="1" max="150" required
                       <@spring.bind "person.age"/><#if spring.status.error>aria-invalid="true" aria-describedby="age-error"</#if>>
                <@spring.bind "person.age"/>
                <#if spring.status.error>
                    <div id="age-error" class="field-error">
                        <#list spring.status.errorMessages as error>${error}<#sep><br></#list>
                    </div>
                </#if>
            </div>
            <div class="field">
                <label for="addressId"><@spring.message "persons.form.address"/></label>
                <select id="addressId" name="addressId">
                    <option value=""><@spring.message "persons.form.address.none"/></option>
                    <#list addresses as address>
                        <option value="${address.id}"
                                <#if person.addressId?? && person.addressId == address.id>selected</#if>>
                            ${address.street}, ${address.door}<#if address.floor?has_content>, ${address.floor}</#if> — ${address.city}, ${address.country}
                        </option>
                    </#list>
                </select>
                <#if !addresses?has_content>
                    <p class="field-hint"><@spring.message "persons.form.address.empty"/> <a href="/addresses/new"><@spring.message "addresses.add"/></a></p>
                </#if>
            </div>
            <div class="form-actions">
                <#if person.id?? && (canDelete!false)>
                    <button class="button danger" type="submit" formaction="/persons/${person.id}/delete" formmethod="post"
                            onclick="return confirm('<@spring.message "persons.form.delete.confirm"/>');"><@spring.message "persons.form.delete"/></button>
                </#if>
                <a class="button ghost" href="/persons"><@spring.message "persons.form.cancel"/></a>
                <button class="button primary" type="submit"><#if person.id??><@spring.message "persons.form.edit.save"/><#else><@spring.message "persons.form.create.save"/></#if></button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
