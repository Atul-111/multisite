(function () {

    function initializeSteppers() {

        document.querySelectorAll(".custom-stepper-wrapper").forEach(function(wrapper){

            if(wrapper.dataset.initialized){
                return;
            }

            wrapper.dataset.initialized = true;

            const input = wrapper.querySelector("input");

            if(!input){
                return;
            }

            const plusBtn = wrapper.querySelector(".stepper-plus");
            const minusBtn = wrapper.querySelector(".stepper-minus");

            plusBtn.addEventListener("click", function(){

                let current = parseInt(input.value || 0);

                current++;

                input.value = current;

                input.dispatchEvent(new Event("input", { bubbles: true }));
                input.dispatchEvent(new Event("change", { bubbles: true }));

            });

            minusBtn.addEventListener("click", function(){

                let current = parseInt(input.value || 0);

                current--;

                input.value = current;

                input.dispatchEvent(new Event("input", { bubbles: true }));
                input.dispatchEvent(new Event("change", { bubbles: true }));

            });

        });

    }

    setTimeout(initializeSteppers, 1000);

})();