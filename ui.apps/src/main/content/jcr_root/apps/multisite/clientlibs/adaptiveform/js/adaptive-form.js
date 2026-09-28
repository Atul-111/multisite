(function (window, document, $) {

    "use strict";

    console.log("======================================");
    console.log("Custom Adaptive Form JS Loaded");
    console.log("======================================");


    /**
     * ---------------------------------------------------------
     * 1. Wait for GuideBridge initialization
     * ---------------------------------------------------------
     */

    $(document).on("bridgeInitializeStart", function (event) {

        console.log("GuideBridge initialization started...");

        if (typeof guideBridge === "undefined") {
            console.error("GuideBridge is not available.");
            return;
        }

        guideBridge.connect(function () {

            console.log("======================================");
            console.log("GuideBridge Connected Successfully");
            console.log("======================================");

            initializeForm();
            prefillEmployeeForm();

        });

    });


    /**
     * ---------------------------------------------------------
     * 2. Initialize Adaptive Form
     * ---------------------------------------------------------
     */

    function initializeForm() {

        console.log("Initializing Adaptive Form...");

        var idField = guideBridge.resolveNode("id");
        var nameField = guideBridge.resolveNode("name");

        if (idField) {
            console.log("ID field found:", idField);
        } else {
            console.error("ID field not found.");
        }

        if (nameField) {
            console.log("Name field found:", nameField);
        } else {
            console.error("Name field not found.");
        }

        console.log("Adaptive Form initialization completed.");

    }

    /**
     * Loads configured employee values after GuideBridge is ready.
     */
    function prefillEmployeeForm() {

        window.fetch("/bin/multisite/forms/prefill?form=employee", {
            credentials: "same-origin"
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Prefill request failed: " + response.status);
                }

                return response.json();
            })
            .then(function (values) {
                var idField = guideBridge.resolveNode("id");
                var nameField = guideBridge.resolveNode("name");

                if (idField && values.id) {
                    idField.value = values.id;
                }

                if (nameField && values.name) {
                    nameField.value = values.name;
                }

                console.log("Employee form prefilled.");
            })
            .catch(function (error) {
                console.error("Unable to prefill employee form.", error);
            });
    }


    /**
     * ---------------------------------------------------------
     * 3. Get ID value
     * ---------------------------------------------------------
     */

    window.getEmployeeId = function () {

        var idField = guideBridge.resolveNode("id");

        if (!idField) {

            console.error("Employee ID field not found.");

            return null;
        }

        var idValue = idField.value;

        console.log("Employee ID:", idValue);

        return idValue;
    };


    /**
     * ---------------------------------------------------------
     * 4. Get Name value
     * ---------------------------------------------------------
     */

    window.getEmployeeName = function () {

        var nameField = guideBridge.resolveNode("name");

        if (!nameField) {

            console.error("Employee Name field not found.");

            return null;
        }

        var nameValue = nameField.value;

        console.log("Employee Name:", nameValue);

        return nameValue;
    };


    /**
     * ---------------------------------------------------------
     * 5. Get complete form data
     * ---------------------------------------------------------
     */

    window.getEmployeeData = function () {

        var idField = guideBridge.resolveNode("id");
        var nameField = guideBridge.resolveNode("name");

        if (!idField || !nameField) {

            console.error("Required fields are not available.");

            return null;
        }

        var employeeData = {

            id: idField.value,

            name: nameField.value

        };

        console.log("Employee Data:");
        console.log(employeeData);

        return employeeData;
    };


    /**
     * ---------------------------------------------------------
     * 6. Set ID value
     * ---------------------------------------------------------
     */

    window.setEmployeeId = function (id) {

        var idField = guideBridge.resolveNode("id");

        if (!idField) {

            console.error("Employee ID field not found.");

            return;
        }

        idField.value = id;

        console.log("Employee ID set to:", id);

    };


    /**
     * ---------------------------------------------------------
     * 7. Set Name value
     * ---------------------------------------------------------
     */

    window.setEmployeeName = function (name) {

        var nameField = guideBridge.resolveNode("name");

        if (!nameField) {

            console.error("Employee Name field not found.");

            return;
        }

        nameField.value = name;

        console.log("Employee Name set to:", name);

    };


    /**
     * ---------------------------------------------------------
     * 8. Clear form fields
     * ---------------------------------------------------------
     */

    window.clearEmployeeForm = function () {

        var idField = guideBridge.resolveNode("id");
        var nameField = guideBridge.resolveNode("name");

        if (idField) {

            idField.value = "";

        }

        if (nameField) {

            nameField.value = "";

        }

        console.log("Employee form cleared.");

    };


    /**
     * ---------------------------------------------------------
     * 9. Validate Employee ID
     * ---------------------------------------------------------
     */

    window.validateEmployeeId = function () {

        var idField = guideBridge.resolveNode("id");

        if (!idField) {

            console.error("Employee ID field not found.");

            return false;
        }

        var idValue = idField.value;

        if (!idValue || idValue.trim() === "") {

            console.error("Employee ID is required.");

            return false;
        }

        if (!/^[0-9]+$/.test(idValue)) {

            console.error("Employee ID must contain only numbers.");

            return false;
        }

        console.log("Employee ID validation successful.");

        return true;
    };


    /**
     * ---------------------------------------------------------
     * 10. Validate Employee Name
     * ---------------------------------------------------------
     */

    window.validateEmployeeName = function () {

        var nameField = guideBridge.resolveNode("name");

        if (!nameField) {

            console.error("Employee Name field not found.");

            return false;
        }

        var nameValue = nameField.value;

        if (!nameValue || nameValue.trim() === "") {

            console.error("Employee Name is required.");

            return false;
        }

        console.log("Employee Name validation successful.");

        return true;
    };


    /**
     * ---------------------------------------------------------
     * 11. Validate complete form
     * ---------------------------------------------------------
     */

    window.validateEmployeeForm = function () {

        var idValid = validateEmployeeId();

        var nameValid = validateEmployeeName();

        if (idValid && nameValid) {

            console.log("======================================");
            console.log("Form validation successful.");
            console.log("======================================");

            return true;

        }

        console.error("Form validation failed.");

        return false;
    };


    /**
     * ---------------------------------------------------------
     * 12. Listen for field value changes
     * ---------------------------------------------------------
     */

    $(document).on("elementValueChanged", function (event) {

        console.log("A field value has changed.");

        try {

            var element = event.target;

            if (element) {

                console.log("Changed element:", element.name);

                console.log("New value:", element.value);

            }

        } catch (error) {

            console.error(
                "Error while processing field change:",
                error
            );

        }

    });


    /**
     * ---------------------------------------------------------
     * 13. Listen for form submission
     * ---------------------------------------------------------
     */

    $(document).on("submitSuccess", function (event) {

        console.log("======================================");
        console.log("Adaptive Form submitted successfully.");
        console.log("======================================");

        var employeeData = window.getEmployeeData();

        console.log("Submitted Employee Data:");

        console.log(employeeData);

    });


    /**
     * ---------------------------------------------------------
     * 14. Listen for submit failure
     * ---------------------------------------------------------
     */

    $(document).on("submitError", function (event) {

        console.error("======================================");
        console.error("Adaptive Form submission failed.");
        console.error("======================================");

    });


    /**
     * ---------------------------------------------------------
     * 15. Print form data
     * ---------------------------------------------------------
     */

    window.printEmployeeData = function () {

        var employeeData = window.getEmployeeData();

        if (!employeeData) {

            console.error("Unable to read employee data.");

            return;
        }

        console.log("======================================");
        console.log("EMPLOYEE FORM DATA");
        console.log("======================================");

        console.log(
            "Employee ID : " + employeeData.id
        );

        console.log(
            "Employee Name : " + employeeData.name
        );

        console.log("======================================");

    };


})(window, document, jQuery);