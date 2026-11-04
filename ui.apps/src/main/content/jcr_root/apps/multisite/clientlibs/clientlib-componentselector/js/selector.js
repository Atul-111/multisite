(function ($) {
    "use strict";

    $(document).on("foundation-contentloaded", function (e) {
        var $form = $(e.target).find("form.cq-dialog");
        var $select = $form.find("coral-select[name='./componentType']");
        if (!$form.length || !$select.length || $form.data("selectorBound")) {
            return;
        }
        $form.data("selectorBound", true);

        $form.on("foundation-form-submitted", function (ev, data) {
            var resourceType = $select.val();
            var path = $form.attr("action"); // selector ka content path
            if (!resourceType || !path) { return; }

            $.ajax({
                url: "/bin/multisite/addcomponent",
                type: "POST",
                data: { path: path, resourceType: resourceType }
            }).done(function () {
                Granite.author.ContentFrame.reload();
            }).fail(function () {
                Granite.author.ui.helpers.notify
                    ? console.error("Component add failed")
                    : null;
            });
        });
    });
})(jQuery);