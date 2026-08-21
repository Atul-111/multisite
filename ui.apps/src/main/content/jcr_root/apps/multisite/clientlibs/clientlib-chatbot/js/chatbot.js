(function (document) {

    "use strict";

    function initChatbot(root) {

        const form =
            root.querySelector(".multisite-chatbot__form");

        const input =
            root.querySelector(".multisite-chatbot__input");

        const messages =
            root.querySelector(".multisite-chatbot__messages");

        const endpoint =
            root.dataset.chatEndpoint;

        function addMessage(text, type) {

            const item =
                document.createElement("div");

            item.className =
                "multisite-chatbot__message " +
                "multisite-chatbot__message--" +
                type;

            item.textContent = text;

            messages.appendChild(item);

            messages.scrollTop =
                messages.scrollHeight;
        }

        form.addEventListener(
            "submit",
            async function (event) {

                event.preventDefault();

                const message =
                    input.value.trim();

                if (!message) {
                    return;
                }

                addMessage(message, "user");

                input.value = "";
                input.disabled = true;

                try {

                    // Get AEM CSRF token
                    const tokenResponse =
                        await fetch(
                            "/libs/granite/csrf/token.json"
                        );

                    if (!tokenResponse.ok) {
                        throw new Error(
                            "Unable to get CSRF token"
                        );
                    }

                    const tokenData =
                        await tokenResponse.json();

                    const body =
                        new URLSearchParams();

                    body.set("message", message);

                    // Call AEM servlet
                    const response =
                        await fetch(
                            endpoint,
                            {
                                method: "POST",

                                headers: {
                                    "Content-Type":
                                        "application/x-www-form-urlencoded",

                                    "CSRF-Token":
                                        tokenData.token
                                },

                                body: body.toString()
                            }
                        );

                    const data =
                        await response.json();

                    if (!response.ok) {
                        throw new Error(
                            data.error ||
                            "Request failed"
                        );
                    }

                    addMessage(
                        data.answer,
                        "assistant"
                    );

                } catch (error) {

                    console.error(
                        "Chatbot request failed",
                        error
                    );

                    addMessage(
                        "Sorry, I could not process that request.",
                        "assistant"
                    );

                } finally {

                    input.disabled = false;
                    input.focus();
                }
            }
        );
    }

    document.addEventListener(
        "DOMContentLoaded",
        function () {

            document
                .querySelectorAll(
                    ".multisite-chatbot"
                )
                .forEach(initChatbot);
        }
    );

})(document);