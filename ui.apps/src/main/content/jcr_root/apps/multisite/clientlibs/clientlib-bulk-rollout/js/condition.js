(function (window, document, $) {
    "use strict";

    var SERVLET = "/bin/multisite/bulkrollout";
    var SELECTOR = '[data-foundation-collection-action*="multisite.bulkrollout"]';
    var timer = null;

    function setVisible($btn, show) {
        $btn.toggleClass("foundation-collection-action-hidden", !show);
        $btn.prop("hidden", !show);
        $btn.css("display", show ? "" : "none");
        var $item = $btn.closest("coral-actionbar-item");
        if ($item.length) {
            $item.prop("hidden", !show);
        }
    }

    function selectedPaths() {
        return $(".foundation-selections-item").map(function () {
            return $(this).data("foundationCollectionItemId");
        }).get();
    }

    function refresh() {
        var $btn = $(SELECTOR);
        if (!$btn.length) {
            return;
        }
        var paths = selectedPaths();
        if (!paths.length) {
            setVisible($btn, false);
            return;
        }
        $.ajax({
            url: SERVLET,
            type: "GET",
            data: { paths: paths, check: "eligible" },
            traditional: true,
            dataType: "json"
        }).done(function (res) {
            // soft eligibility: >=1 selected path ke paas active live copy
            setVisible($btn, Object.keys(res.availableLocales || {}).length > 0);
        }).fail(function () {
            setVisible($btn, false);
        });
    }

    function schedule(delay) {
        clearTimeout(timer);
        // delay isliye ki Foundation apna show/hide pehle kar le, phir hum override karein
        timer = setTimeout(refresh, delay);
    }

    $(document).on("foundation-selections-change", ".foundation-collection", function () {
        schedule(150);
    });
    $(document).on("foundation-contentloaded", function () {
        schedule(300);
    });
})(window, document, Granite.$);