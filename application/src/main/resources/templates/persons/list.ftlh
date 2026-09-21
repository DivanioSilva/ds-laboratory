<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><@spring.message "persons.title"/></title>
    <script src="/js/theme.js"></script>
    <link rel="stylesheet" href="/css/app.css">
    <script src="/js/live-search.js" defer></script>
</head>
<body>
<main class="shell">
    <header class="topbar">
        <div>
            <p class="eyebrow"><@spring.message "app.technology"/></p>
            <h1><@spring.message "persons.title"/></h1>
            <p class="subtitle"><@spring.message "persons.subtitle"/></p>
        </div>
        <div class="topbar-actions">
            <nav class="language-links" aria-label="<@spring.message 'language.label'/>">
                <span><@spring.message "language.label"/>:</span>
                <a href="/persons?lang=pt" class="<#if .locale?starts_with('pt')>active</#if>">PT</a>
                <a href="/persons?lang=en" class="<#if .locale?starts_with('en')>active</#if>">EN</a>
                <a href="/persons?lang=it" class="<#if .locale?starts_with('it')>active</#if>">IT</a>
                <a href="/persons?lang=es" class="<#if .locale?starts_with('es')>active</#if>">ES</a>
                <a href="/persons?lang=fr" class="<#if .locale?starts_with('fr')>active</#if>">FR</a>
                <a href="/persons?lang=de" class="<#if .locale?starts_with('de')>active</#if>">DE</a>
            </nav>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="<@spring.message 'theme.toggle'/>">
                <span data-theme-icon>☾</span> <span data-theme-label><@spring.message "theme.dark"/></span>
            </button>
            <span class="authenticated-user">${username}</span>
            <form action="/logout" method="post" class="inline-form"><button class="button ghost compact" type="submit"><@spring.message "home.logout"/></button></form>
            <a class="button ghost" href="/addresses/new?lang=${.locale}"><@spring.message "addresses.add"/></a>
        </div>
    </header>

    <#if canCreate || canImport><section class="grid action-grid<#if canCreate && !canImport> single-action</#if>" aria-label="<@spring.message 'persons.title'/>">
        <#if canCreate>
        <article class="card quick-action-card">
            <span class="step">01</span>
            <h2><@spring.message "persons.add"/></h2>
            <p><@spring.message "persons.create.description"/></p>
            <a class="button primary" href="/persons/new?lang=${.locale}"><@spring.message "persons.add"/> <span aria-hidden="true">→</span></a>
        </article>
        </#if>
        <#if canImport>
        <article class="card import-card">
            <span class="step">02</span>
            <h2><@spring.message "persons.import.title"/></h2>
            <p><@spring.message "persons.import.help"/></p>
            <form data-csv-import data-success="<@spring.message 'persons.import.success'/>" data-error="<@spring.message 'persons.import.error'/>">
                <label class="file-zone">
                    <input type="file" name="file" accept=".csv,text/csv" required>
                    <span aria-hidden="true">↥</span>
                    <strong><@spring.message "persons.import.choose"/></strong>
                    <small><@spring.message "persons.import.limit"/></small>
                </label>
                <button type="submit" class="button secondary wide"><@spring.message "persons.import.action"/></button>
            </form>
            <p class="import-feedback" data-import-feedback aria-live="polite"></p>
        </article>
        </#if>
    </section></#if>

    <section id="people-results" class="card list-card" aria-live="polite">
        <div class="list-header">
            <div>
                <span class="step">03</span>
                <h2><@spring.message "persons.list.title"/></h2>
                <p>${totalPeople} <@spring.message "persons.list.results"/></p>
            </div>
            <div class="list-header-actions">
                <#if canDelete>
                    <form action="/persons/clean" method="post" class="inline-form" onsubmit="return confirm('<@spring.message "persons.clean.confirm"/>');">
                        <button class="button danger compact" type="submit"><@spring.message "persons.clean"/></button>
                    </form>
                </#if>
                <form action="/persons" method="get" class="search" data-live-search>
                    <input type="hidden" name="lang" value="${.locale}">
                    <input type="hidden" name="sort" value="${sort}">
                    <input type="hidden" name="direction" value="${direction}">
                    <input type="hidden" name="size" value="${size}">
                    <label for="query"><span aria-hidden="true">⌕</span><span class="sr-only"><@spring.message "persons.search.label"/></span></label>
                    <input id="query" name="query" value="${query!}" autocomplete="off" placeholder="<@spring.message 'persons.search.placeholder'/>">
                </form>
            </div>
        </div>

        <#macro sortLink field messageCode>
            <#assign nextDirection = (sort == field && direction == "asc")?then("desc", "asc")>
            <a class="sort-link <#if sort == field>selected</#if>" href="/persons?query=${query?url}&sort=${field}&direction=${nextDirection}&size=${size}&lang=${.locale}">
                <@spring.message messageCode/> <#if sort == field><span aria-hidden="true"><#if direction == "asc">↑<#else>↓</#if></span></#if>
            </a>
        </#macro>

        <div class="table-scroll">
            <table>
                <thead>
                <tr>
                    <th><@sortLink "id" "persons.table.id"/></th>
                    <th><@sortLink "name" "persons.table.name"/></th>
                    <th><@sortLink "age" "persons.table.age"/></th>
                    <th><@spring.message "persons.table.address"/></th>
                    <#if canEdit || canDelete><th><@spring.message "persons.table.actions"/></th></#if>
                </tr>
                </thead>
                <tbody>
                <#list people as person>
                    <tr>
                        <td><span class="id-badge">#${person.id}</span></td>
                        <td><strong>${person.firstName} ${person.lastName}</strong></td>
                        <td>${person.age} <@spring.message "persons.years"/></td>
                        <td><#if person.addressId??>${person.addressId}<#else>—</#if></td>
                        <#if canEdit || canDelete><td class="actions">
                            <#if canEdit><a class="icon-button" href="/persons/${person.id}/edit?lang=${.locale}" aria-label="<@spring.message 'persons.action.edit'/>" title="<@spring.message 'persons.action.edit'/>">✎</a></#if>
                            <#if canDelete>
                            <form action="/persons/${person.id}/delete" method="post" class="inline-form">
                                <input type="hidden" name="lang" value="${.locale}">
                                <button class="icon-button danger" type="submit" aria-label="<@spring.message 'persons.action.delete'/>" title="<@spring.message 'persons.action.delete'/>">🗑</button>
                            </form>
                            </#if>
                        </td></#if>
                    </tr>
                <#else>
                    <tr><td colspan="<#if canEdit || canDelete>5<#else>4</#if>" class="empty-state"><strong><@spring.message "persons.empty.title"/></strong><p><@spring.message "persons.empty.description"/></p></td></tr>
                </#list>
                </tbody>
            </table>
        </div>

        <#if totalPeople gt 0>
            <footer class="pagination">
                <p><@spring.message "persons.showing"/> ${firstVisible}–${lastVisible} <@spring.message "persons.of"/> ${totalPeople}</p>
                <div>
                    <label for="page-size"><@spring.message "persons.perPage"/></label>
                    <select id="page-size" data-page-size aria-label="<@spring.message 'persons.perPage'/>">
                        <#list [5, 10, 20, 50] as pageSize><option value="${pageSize}" <#if size == pageSize>selected</#if>>${pageSize}</option></#list>
                    </select>
                    <#if page gt 1><a class="button ghost compact" href="/persons?query=${query?url}&sort=${sort}&direction=${direction}&size=${size}&page=${page - 1}&lang=${.locale}">←</a></#if>
                    <span><@spring.message "persons.page"/> ${page} / ${totalPages}</span>
                    <#if page lt totalPages><a class="button ghost compact" href="/persons?query=${query?url}&sort=${sort}&direction=${direction}&size=${size}&page=${page + 1}&lang=${.locale}">→</a></#if>
                </div>
            </footer>
        </#if>
    </section>
</main>
</body>
</html>
