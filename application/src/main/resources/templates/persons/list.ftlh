<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><@spring.message "persons.title"/></title>
    <script src="/js/theme.js"></script>
    <link rel="stylesheet" href="/css/app.css">
    <script src="/js/live-search.js" defer></script>
</head>
<body>
<main class="container">
    <header class="page-header">
        <div>
            <p class="eyebrow"><@spring.message "app.technology"/></p>
            <h1><@spring.message "persons.title"/></h1>
            <p class="subtitle"><@spring.message "persons.subtitle"/></p>
        </div>
        <div class="header-actions">
            <button class="theme-toggle" type="button" data-theme-toggle
                    aria-label="<@spring.message "theme.toggle"/>" title="<@spring.message "theme.toggle"/>">☀</button>
            <nav class="language-switcher" aria-label="<@spring.message "language.label"/>">
                <a href="/persons?lang=pt" class="<#if .locale == 'pt'>active</#if>">PT</a>
                <a href="/persons?lang=en" class="<#if .locale == 'en'>active</#if>">EN</a>
                <a href="/persons?lang=it" class="<#if .locale == 'it'>active</#if>">IT</a>
            </nav>
            <div class="create-actions">
                <a class="button ghost" href="/addresses/new"><@spring.message "addresses.add"/></a>
                <a class="button primary" href="/persons/new"><@spring.message "persons.add"/></a>
            </div>
        </div>
    </header>

    <section class="panel search-panel">
        <form action="/persons" method="get" class="search-form" data-live-search>
            <label for="firstName"><@spring.message "persons.search.label"/></label>
            <div class="search-row">
                <input id="firstName" name="firstName" value="${firstName!}"
                       placeholder="<@spring.message "persons.search.placeholder"/>"
                       autocomplete="off" aria-controls="people-results">
                <button class="button secondary" type="submit"><@spring.message "persons.search.action"/></button>
                <a class="button ghost" href="/persons"><@spring.message "persons.search.clear"/></a>
            </div>
        </form>
    </section>

    <section id="people-results" class="panel table-panel" aria-live="polite" aria-busy="false">
        <#if people?has_content>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th><@spring.message "persons.table.id"/></th>
                        <th><@spring.message "persons.table.firstName"/></th>
                        <th><@spring.message "persons.table.lastName"/></th>
                        <th><@spring.message "persons.table.age"/></th>
                        <th class="actions-heading"><@spring.message "persons.table.actions"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list people as person>
                        <tr>
                            <td class="muted">#${person.id}</td>
                            <td><strong>${person.firstName}</strong></td>
                            <td>${person.lastName}</td>
                            <td>${person.age}</td>
                            <td class="actions">
                                <a class="text-link" href="/persons/${person.id}/edit"><@spring.message "persons.action.edit"/></a>
                                <form action="/persons/${person.id}/delete" method="post"
                                      onsubmit="return confirm('<@spring.message "persons.delete.confirm"/>');">
                                    <button class="text-button danger" type="submit"><@spring.message "persons.action.delete"/></button>
                                </form>
                            </td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
        <#else>
            <div class="empty-state">
                <h2><@spring.message "persons.empty.title"/></h2>
                <p><@spring.message "persons.empty.description"/></p>
            </div>
        </#if>
    </section>
</main>
</body>
</html>
