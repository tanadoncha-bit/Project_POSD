document.addEventListener("DOMContentLoaded", () => {
    const borrowForm =
        document.getElementById("borrow-request-form");

    const userIdInput =
        document.getElementById("current-user-id");

    const borrowDateInput =
        document.getElementById("borrow-date");

    const dueDateInput =
        document.getElementById("borrow-due-date");

    const noteInput =
        document.getElementById("borrow-note");

    const noteCount =
        document.getElementById("borrow-note-count");

    const submitButton =
        document.getElementById("borrow-submit-button");

    const submitLabel =
        document.getElementById("borrow-submit-label");

    const selectedCount =
        document.getElementById(
            "selected-equipment-count"
        );

    const formMessage =
        document.getElementById("borrow-form-message");

    const closeButton =
        document.getElementById("borrow-form-close");

    const cancelButton =
        document.getElementById("borrow-form-cancel");

    const successModal =
        document.getElementById("borrow-success-modal");

    const successCloseButton =
        document.getElementById("success-modal-close");

    const successStayButton =
        document.getElementById("success-stay-button");


    if (!borrowForm) {
        return;
    }


    function isStaticPreview() {
        return (
            window.location.port === "5500" ||
            window.location.port === "5501" ||
            window.location.pathname.includes(
                "/src/main/resources/templates/"
            )
        );
    }


    function formatLocalDate(date) {
        const year = date.getFullYear();

        const month = String(
            date.getMonth() + 1
        ).padStart(2, "0");

        const day = String(
            date.getDate()
        ).padStart(2, "0");

        return `${year}-${month}-${day}`;
    }


    function getEquipmentInputs() {
        return Array.from(
            document.querySelectorAll(
                'input[name="equipmentIds"]'
            )
        );
    }


    function getSelectedEquipmentIds() {
        return getEquipmentInputs()
            .filter((input) => input.checked)
            .map((input) => Number(input.value))
            .filter((equipmentId) => {
                return Number.isInteger(equipmentId) &&
                    equipmentId > 0;
            });
    }


    function updateSelectedEquipment() {
        const equipmentInputs =
            getEquipmentInputs();

        equipmentInputs.forEach((input) => {
            const equipmentChoice =
                input.closest(".equipment-choice");

            equipmentChoice?.classList.toggle(
                "selected",
                input.checked
            );
        });

        if (selectedCount) {
            selectedCount.textContent = String(
                getSelectedEquipmentIds().length
            );
        }
    }


    function initializeSelectedEquipment() {
        const parameters =
            new URLSearchParams(
                window.location.search
            );

        const selectedEquipmentId =
            parameters.get("equipmentId");

        if (!selectedEquipmentId) {
            updateSelectedEquipment();
            return;
        }

        const matchingInput =
            getEquipmentInputs().find((input) => {
                return input.value === selectedEquipmentId;
            });

        if (matchingInput) {
            matchingInput.checked = true;
        }

        updateSelectedEquipment();
    }


    function initializeDates() {
        const today = new Date();

        const tomorrow = new Date(today);

        tomorrow.setDate(
            today.getDate() + 1
        );

        const todayValue =
            formatLocalDate(today);

        const tomorrowValue =
            formatLocalDate(tomorrow);

        if (borrowDateInput) {
            borrowDateInput.value = todayValue;
        }

        if (dueDateInput) {
            dueDateInput.min = tomorrowValue;

            if (!dueDateInput.value) {
                dueDateInput.value =
                    tomorrowValue;
            }
        }
    }


    function updateNoteCount() {
        if (!noteInput || !noteCount) {
            return;
        }

        noteCount.textContent = String(
            noteInput.value.length
        );
    }


    function showMessage(message, type = "error") {
        if (!formMessage) {
            return;
        }

        formMessage.textContent = message;
        formMessage.hidden = !message;

        formMessage.classList.toggle(
            "message-error",
            type === "error"
        );

        formMessage.classList.toggle(
            "message-success",
            type === "success"
        );
    }


    function setSubmitting(isSubmitting) {
        if (!submitButton) {
            return;
        }

        submitButton.disabled = isSubmitting;

        submitButton.setAttribute(
            "aria-busy",
            String(isSubmitting)
        );

        if (submitLabel) {
            submitLabel.textContent =
                isSubmitting
                    ? "Submitting..."
                    : "Submit";
        }
    }


    function setSuccessModalOpen(isOpen) {
        if (!successModal) {
            return;
        }

        successModal.hidden = !isOpen;

        document.body.classList.toggle(
            "modal-open",
            isOpen
        );

        if (isOpen) {
            successCloseButton?.focus();
        }
    }


    function validateForm() {
        if (!userIdInput?.value.trim()) {
            return (
                "User information was not found. " +
                "Please sign in again."
            );
        }

        if (
            !Number.isInteger(
                Number(userIdInput.value)
            ) ||
            Number(userIdInput.value) <= 0
        ) {
            return "The user ID is invalid.";
        }

        if (
            getSelectedEquipmentIds().length === 0
        ) {
            return (
                "Please select at least one " +
                "equipment item."
            );
        }

        if (!dueDateInput?.value) {
            return "Please select a return due date.";
        }

        if (
            borrowDateInput?.value &&
            dueDateInput.value <=
                borrowDateInput.value
        ) {
            return (
                "The return date must be after " +
                "the borrow date."
            );
        }

        if (
            noteInput &&
            noteInput.value.length > 500
        ) {
            return (
                "The note must not exceed " +
                "500 characters."
            );
        }

        return "";
    }


    function createRequestBody() {
        return {
            userId: Number(userIdInput.value),

            borrowDate:
                borrowDateInput.value,

            dueDate:
                dueDateInput.value,

            note:
                noteInput?.value.trim() || "",

            items:
                getSelectedEquipmentIds().map(
                    (equipmentId) => {
                        return {
                            equipmentId,
                            quantity: 1
                        };
                    }
                )
        };
    }


    function getCsrfHeaders() {
        const csrfToken =
            document.querySelector(
                'meta[name="_csrf"]'
            )?.content;

        const csrfHeader =
            document.querySelector(
                'meta[name="_csrf_header"]'
            )?.content;

        if (!csrfToken || !csrfHeader) {
            return {};
        }

        return {
            [csrfHeader]: csrfToken
        };
    }


    async function readErrorMessage(response) {
        const contentType =
            response.headers.get(
                "content-type"
            ) || "";

        if (
            contentType.includes(
                "application/json"
            )
        ) {
            const errorBody =
                await response.json();

            return (
                errorBody.message ||
                errorBody.error ||
                "Unable to submit the borrow request."
            );
        }

        const errorText =
            await response.text();

        return (
            errorText ||
            "Unable to submit the borrow request."
        );
    }


    async function submitBorrowRequest(
        requestBody
    ) {
        const response = await fetch(
            "/api/v1/borrow-requests",
            {
                method: "POST",

                credentials: "same-origin",

                headers: {
                    "Content-Type":
                        "application/json",

                    ...getCsrfHeaders()
                },

                body: JSON.stringify(
                    requestBody
                )
            }
        );

        if (!response.ok) {
            throw new Error(
                await readErrorMessage(response)
            );
        }

        return response.json();
    }


    function goBackToEquipment() {
        if (isStaticPreview()) {
            window.location.href =
                "../equipment/list.html";

            return;
        }

        window.location.href =
            "/equipment";
    }


    getEquipmentInputs().forEach((input) => {
        input.addEventListener(
            "change",
            updateSelectedEquipment
        );
    });


    noteInput?.addEventListener(
        "input",
        updateNoteCount
    );


    borrowForm.addEventListener(
        "submit",
        async (event) => {
            event.preventDefault();

            showMessage("");

            const validationMessage =
                validateForm();

            if (validationMessage) {
                showMessage(validationMessage);
                return;
            }

            const requestBody =
                createRequestBody();

            setSubmitting(true);

            try {
                /*
                 * Live Server:
                 * ไม่เรียก API เพื่อให้ตรวจหน้าตาและ Flow ได้
                 *
                 * Spring Boot:
                 * ส่ง JSON ไป BorrowRequestController จริง
                 */

                if (!isStaticPreview()) {
                    await submitBorrowRequest(
                        requestBody
                    );
                }

                setSuccessModalOpen(true);

            } catch (error) {
                showMessage(
                    error.message ||
                    "Unable to submit the borrow request."
                );

            } finally {
                setSubmitting(false);
            }
        }
    );


    closeButton?.addEventListener(
        "click",
        goBackToEquipment
    );


    cancelButton?.addEventListener(
        "click",
        goBackToEquipment
    );


    successCloseButton?.addEventListener(
        "click",
        () => {
            setSuccessModalOpen(false);
        }
    );


    successStayButton?.addEventListener(
        "click",
        () => {
            setSuccessModalOpen(false);
        }
    );


    successModal?.addEventListener(
        "click",
        (event) => {
            if (event.target === successModal) {
                setSuccessModalOpen(false);
            }
        }
    );


    document.addEventListener(
        "keydown",
        (event) => {
            if (
                event.key === "Escape" &&
                successModal &&
                !successModal.hidden
            ) {
                setSuccessModalOpen(false);
            }
        }
    );


    initializeDates();
    initializeSelectedEquipment();
    updateNoteCount();
});