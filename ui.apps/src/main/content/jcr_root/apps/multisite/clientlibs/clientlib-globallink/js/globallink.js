(function () {
    "use strict";

    var SERVLET = "/bin/multisite/translate";

    function setStatus(message, state) {
        var status = document.getElementById("globallink-status");
        status.textContent = message;
        status.dataset.state = state || "";
    }

    function renderPages(pages) {
        var select = document.getElementById("globallink-page");
        select.replaceChildren(new Option("Choose a site page", ""));

        pages.forEach(function (page) {
            var option = new Option(page.title + " - " + page.path, page.path);
            option.dataset.sourceLanguage = page.sourceLanguage;
            select.add(option);
        });
        select.disabled = pages.length === 0;
    }

    function renderLanguages(languages) {
        var container = document.getElementById("globallink-languages");
        container.replaceChildren();

        languages.forEach(function (language) {
            var label = document.createElement("label");
            label.className = "globallink__language";

            var checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "targetLangs";
            checkbox.value = language[0];

            var text = document.createElement("span");
            text.textContent = language[1] + " (" + language[0] + ")";
            label.append(checkbox, text);
            container.append(label);
        });
    }

    function updateSourceLanguage() {
        var page = document.getElementById("globallink-page").selectedOptions[0];
        var sourceLanguage = page && page.dataset.sourceLanguage;
        var source = document.getElementById("globallink-source-language");
        source.textContent = sourceLanguage
            ? "Source language: " + sourceLanguage.toUpperCase()
            : "Source language is detected from the selected page.";

        document.querySelectorAll('#globallink-languages input[name="targetLangs"]').forEach(function (checkbox) {
            checkbox.checked = false;
            checkbox.disabled = checkbox.value === sourceLanguage;
            checkbox.closest("label").classList.toggle("is-disabled", checkbox.disabled);
        });
    }

    function appendList(parent, headingText, entries) {
        if (!entries.length) {
            return;
        }
        var heading = document.createElement("h3");
        heading.textContent = headingText;
        parent.append(heading);

        var list = document.createElement("ul");
        entries.forEach(function (entry) {
            var item = document.createElement("li");
            item.textContent = entry;
            list.append(item);
        });
        parent.append(list);
    }

    function renderResult(result) {
        var output = document.getElementById("globallink-result");
        output.replaceChildren();

        var translated = Object.entries(result.translated || {}).map(function (entry) {
            return entry[0].replace("|", " to ") + " -> " + entry[1];
        });
        var failures = Object.entries(result.failures || {}).map(function (entry) {
            var details = entry[0].split("|");
            var sourcePath = details[0];
            var targetLanguage = details[1] || "";
            var label = sourcePath + " to " + targetLanguage;
            var message = entry[1] || "";
            return label + ": " + (message || "Translation failed. Check the AEM error log.");
        });
        appendList(output, "Translated pages (" + translated.length + ")", translated);
        appendList(output, "Failed (" + failures.length + ")", failures);

        var count = document.createElement("p");
        count.textContent = "Properties translated: " + (result.propertiesTranslatedCount || 0);
        output.append(count);
    }

    async function loadOptions() {
        var response = await fetch(SERVLET + "?mode=pages", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });
        if (!response.ok) {
            throw new Error("Could not load site pages (" + response.status + ").");
        }

        var options = await response.json();
        renderPages(options.pages || []);
        renderLanguages(options.languages || []);
        updateSourceLanguage();
        setStatus(options.pages && options.pages.length
            ? "Select a page and target language(s)."
            : "No translatable pages found under /content/multisite.", options.pages && options.pages.length ? "ready" : "error");
    }

    async function submitTranslation(event) {
        event.preventDefault();
        var page = document.getElementById("globallink-page");
        var selectedPage = page.selectedOptions[0];
        var targetLanguages = Array.from(document.querySelectorAll('#globallink-languages input[name="targetLangs"]:checked'));

        if (!page.value || !selectedPage.dataset.sourceLanguage) {
            setStatus("Select a page with a supported source language.", "error");
            return;
        }
        if (!targetLanguages.length) {
            setStatus("Select at least one target language.", "error");
            return;
        }

        var button = document.getElementById("globallink-translate-button");
        button.disabled = true;
        setStatus("Translating the selected page and its content...", "loading");
        document.getElementById("globallink-result").replaceChildren();

        try {
            var tokenResponse = await fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" });
            if (!tokenResponse.ok) {
                throw new Error("Could not get an AEM security token. Refresh the page and try again.");
            }
            var token = await tokenResponse.json();
            var body = new URLSearchParams();
            body.append("paths", page.value);
            body.append("sourceLang", selectedPage.dataset.sourceLanguage);
            targetLanguages.forEach(function (checkbox) { body.append("targetLangs", checkbox.value); });

            var response = await fetch(SERVLET, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                    "CSRF-Token": token.token
                },
                body: body.toString()
            });
            if (!response.ok) {
                throw new Error("Translation request failed (" + response.status + ").");
            }

            var result = await response.json();
            renderResult(result);
            var translatedCount = Object.keys(result.translated || {}).length;
            var failureCount = Object.keys(result.failures || {}).length;
            var message = failureCount
                ? "Some target languages failed. Check the result details."
                : translatedCount ? "Translation complete." : "No page copies were created. Check the result details.";
            setStatus(message, failureCount ? "error" : translatedCount ? "success" : "error");
        } catch (error) {
            setStatus(error.message || "Translation failed. Check the AEM logs.", "error");
        } finally {
            button.disabled = false;
        }
    }

    function init() {
        var form = document.getElementById("globallink-form");
        if (!form) {
            return;
        }
        document.getElementById("globallink-page").addEventListener("change", updateSourceLanguage);
        form.addEventListener("submit", submitTranslation);
        loadOptions().catch(function (error) {
            setStatus(error.message || "Could not load GlobalLink options.", "error");
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();