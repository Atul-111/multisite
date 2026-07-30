(function() {
    window.addEventListener("bridgeInitializeStart", function(event) {
        guideBridge.on("elementInitialized", function(event, data) {
            
            // Resource type check matching your path
            if (data.model.resourceType === "multisite/components/adaptiveforms/custom-js-input") {
                var model = data.model;
                var element = document.getElementById(model.id);
                
                if (element) {
                    var input = element.querySelector('.custom-js-field');

                    // Sync UI -> Model
                    input.addEventListener('input', function(e) {
                        model.value = e.target.value;
                    });

                    // Sync Model -> UI
                    model.on('valueChanged', function(ev, val) {
                        if(input.value !== val) input.value = val || '';
                    });

                    // Handle Visibility
                    model.on('visibleChanged', function(ev, isVisible) {
                        element.style.display = isVisible ? 'block' : 'none';
                    });
                }
            }
        });
    });
})();