(function (window, document, $) {
    "use strict";

    var SERVLET = "/bin/multisite/bulkrollout";
    var registry = $(window).adaptTo("foundation-registry");

    function esc(s) {
        return $("<div/>").text(s == null ? "" : String(s)).html();
    }

    function getPaths(selections) {
        return selections.map(function (item) {
            return $(item).data("foundationCollectionItemId");
        });
    }

    function buildContent(paths, res) {
        var available = res.availableLocales || {};
        var skipped = res.skippedPaths || [];
        var willRoll = Object.keys(available);

        // locale -> kitne paths me available
        var localeCount = {};
        willRoll.forEach(function (p) {
            available[p].forEach(function (l) { localeCount[l] = (localeCount[l] || 0) + 1; });
        });

        var html = '<p>' + willRoll.length + ' item(s) will be rolled out &middot; '
            + Object.keys(localeCount).length + ' locale(s) available</p>';
        html += '<coral-alert variant="warning" size="S"><coral-alert-content>'
            + 'Rollout syncs content to live copies only. It does <b>not</b> publish. '
            + 'Use Quick Publish / Manage Publication separately.</coral-alert-content></coral-alert>';

        html += '<h4>Will roll out (' + willRoll.length + ')</h4><ul class="msbr-list">';
        willRoll.forEach(function (p) { html += '<li>' + esc(p) + '</li>'; });
        html += '</ul>';

        if (skipped.length) {
            html += '<h4>Skipped - no applicable live copies (' + skipped.length + ')</h4><ul class="msbr-list msbr-skipped">';
            skipped.forEach(function (p) { html += '<li>' + esc(p) + '</li>'; });
            html += '</ul>';
        }

        html += '<h4>Target locales</h4>';
        Object.keys(localeCount).sort().forEach(function (l) {
            var badge = localeCount[l] === willRoll.length ? "All selected"
                : localeCount[l] + " of " + willRoll.length + " items";
            html += '<div class="msbr-locale"><coral-checkbox name="locale" value="' + esc(l) + '">'
                + esc(l) + ' <span class="msbr-badge">' + esc(badge) + '</span></coral-checkbox></div>';
        });

        html += '<h4>Options</h4><coral-checkbox name="deep" checked>Include child pages '
            + '<span class="msbr-badge">Recommended for folders</span></coral-checkbox>';
        return html;
    }

    function showResult(dialog, res) {
        var html = '<h4>Rolled out (' + (res.rolledOutPaths || []).length + ')</h4><ul class="msbr-list">';
        (res.rolledOutPaths || []).forEach(function (p) { html += '<li>' + esc(p) + '</li>'; });
        html += '</ul>';
        if ((res.skippedPaths || []).length) {
            html += '<h4>Skipped</h4><ul class="msbr-list">';
            res.skippedPaths.forEach(function (p) { html += '<li>' + esc(p) + '</li>'; });
            html += '</ul>';
        }
        var failures = res.failures || {};
        var keys = Object.keys(failures);
        if (keys.length) {
            html += '<h4>Failed</h4><ul class="msbr-list msbr-failed">';
            keys.forEach(function (k) { html += '<li>' + esc(k) + ' - ' + esc(failures[k]) + '</li>'; });
            html += '</ul>';
        }
        dialog.content.innerHTML = html;
        dialog.footer.innerHTML = '<button is="coral-button" variant="primary" coral-close>Close</button>';
    }

    function openDialog(paths, res) {
        var dialog = new Coral.Dialog();
        dialog.id = "multisite-bulk-rollout-dialog";
        dialog.header.innerHTML = "Bulk Rollout";
        dialog.content.innerHTML = buildContent(paths, res);
        dialog.footer.innerHTML =
            '<button is="coral-button" variant="default" coral-close>Cancel</button>'
            + '<button is="coral-button" variant="primary" id="msbr-confirm">Rollout</button>';
        document.body.appendChild(dialog);

        dialog.on("coral-overlay:close", function () {
            dialog.remove();
        });

        $(dialog).on("click", "#msbr-confirm", function () {
            var locales = [];
            dialog.content.querySelectorAll('coral-checkbox[name="locale"]').forEach(function (cb) {
                if (cb.checked) { locales.push(cb.value); }
            });
            if (!locales.length) {
                var ui = $(window).adaptTo("foundation-ui");
                ui.alert("Bulk Rollout", "Please select at least one locale.", "warning");
                return;
            }
            var deep = dialog.content.querySelector('coral-checkbox[name="deep"]').checked;
            var eligiblePaths = Object.keys(res.availableLocales || {});

            $("#msbr-confirm").prop("disabled", true);
            $.ajax({
                url: SERVLET,
                type: "POST",
                data: { paths: eligiblePaths, locales: locales, deep: deep },
                traditional: true,
                dataType: "json"
            }).done(function (r) {
                showResult(dialog, r);
            }).fail(function () {
                showResult(dialog, { failures: { "*": "Request failed. Check server logs." } });
            });
        });

        dialog.show();
    }

    registry.register("foundation.collection.action.action", {
        name: "multisite.bulkrollout",
        handler: function (name, el, config, collection, selections) {
            var paths = getPaths(selections);
            $.ajax({
                url: SERVLET,
                type: "GET",
                data: { paths: paths },
                traditional: true,
                dataType: "json"
            }).done(function (res) {
                openDialog(paths, res);
            }).fail(function () {
                $(window).adaptTo("foundation-ui").alert("Bulk Rollout", "Could not load rollout details.", "error");
            });
        }
    });
})(window, document, Granite.$);