document.addEventListener("DOMContentLoaded", () => {
    const profileForm =
        document.getElementById("profile-details-form");

    const editButton =
        document.getElementById("profile-edit-button");

    const editLabel =
        document.getElementById("profile-edit-label");

    const formActions =
        document.getElementById("profile-form-actions");

    const cancelButton =
        document.getElementById("profile-cancel-button");

    const formMessage =
        document.getElementById("profile-form-message");

    const verifyEmailButton =
        document.getElementById("verify-email-button");

    const changePasswordButton =
        document.getElementById("change-password-button");

    const editableInputs = [
        document.getElementById("profile-email"),
        document.getElementById("profile-full-name"),
        document.getElementById("profile-phone"),
        document.getElementById("profile-department")
    ].filter(Boolean);

    const originalValues = new Map();

    let editing = false;

    function isLiveServerPreview() {
        return (
            window.location.port === "5500" ||
            window.location.port === "5501" ||
            window.location.protocol === "file:"
        );
    }

    function rememberOriginalValues() {
        editableInputs.forEach((input) => {
            originalValues.set(input.id, input.value);
        });
    }

    function restoreOriginalValues() {
        editableInputs.forEach((input) => {
            const originalValue =
                originalValues.get(input.id);

            if (originalValue !== undefined) {
                input.value = originalValue;
            }
        });
    }

    function setFormMessage(message, type = "success") {
        if (!formMessage) {
            return;
        }

        formMessage.textContent = message;
        formMessage.dataset.type = type;
        formMessage.hidden = !message;
    }

    function clearFormMessage() {
        if (!formMessage) {
            return;
        }

        formMessage.textContent = "";
        formMessage.removeAttribute("data-type");
        formMessage.hidden = true;
    }

    function setEditingState(nextEditingState) {
        editing = nextEditingState;

        editableInputs.forEach((input) => {
            input.readOnly = !editing;
            input.classList.toggle(
                "profile-input-editing",
                editing
            );
        });

        if (formActions) {
            formActions.hidden = !editing;
        }

        if (editLabel) {
            editLabel.textContent =
                editing ? "Editing" : "Edit";
        }

        if (editButton) {
            editButton.setAttribute(
                "aria-pressed",
                String(editing)
            );

            editButton.disabled = editing;
        }

        if (editing && editableInputs.length > 0) {
            editableInputs[0].focus();
        }
    }

    function savePreviewProfile() {
        const profileData = {};

        editableInputs.forEach((input) => {
            profileData[input.name] = input.value.trim();
        });

        sessionStorage.setItem(
            "leaditPreviewProfile",
            JSON.stringify(profileData)
        );

        rememberOriginalValues();
        setEditingState(false);

        setFormMessage(
            "Profile information updated successfully."
        );
    }

    function restorePreviewProfile() {
        if (!isLiveServerPreview()) {
            return;
        }

        const savedProfile =
            sessionStorage.getItem("leaditPreviewProfile");

        if (!savedProfile) {
            return;
        }

        try {
            const profileData =
                JSON.parse(savedProfile);

            editableInputs.forEach((input) => {
                const savedValue =
                    profileData[input.name];

                if (typeof savedValue === "string") {
                    input.value = savedValue;
                }
            });
        } catch (error) {
            console.error(
                "Unable to restore preview profile.",
                error
            );
        }
    }

    function validateProfileForm() {
        const emailInput =
            document.getElementById("profile-email");

        const fullNameInput =
            document.getElementById("profile-full-name");

        if (
            emailInput &&
            !emailInput.value.trim()
        ) {
            setFormMessage(
                "Please enter your email.",
                "error"
            );

            emailInput.focus();

            return false;
        }

        if (
            emailInput &&
            !emailInput.validity.valid
        ) {
            setFormMessage(
                "Please enter a valid email address.",
                "error"
            );

            emailInput.focus();

            return false;
        }

        if (
            fullNameInput &&
            !fullNameInput.value.trim()
        ) {
            setFormMessage(
                "Please enter your full name.",
                "error"
            );

            fullNameInput.focus();

            return false;
        }

        return true;
    }

    if (editButton) {
        editButton.addEventListener("click", () => {
            clearFormMessage();
            rememberOriginalValues();
            setEditingState(true);
        });
    }

    if (cancelButton) {
        cancelButton.addEventListener("click", () => {
            restoreOriginalValues();
            clearFormMessage();
            setEditingState(false);
        });
    }

    if (profileForm) {
        profileForm.addEventListener(
            "submit",
            (event) => {
                if (!validateProfileForm()) {
                    event.preventDefault();

                    return;
                }

                if (isLiveServerPreview()) {
                    event.preventDefault();
                    savePreviewProfile();
                }
            }
        );
    }

    if (verifyEmailButton) {
        verifyEmailButton.addEventListener(
            "click",
            () => {
                if (isLiveServerPreview()) {
                    verifyEmailButton.classList.add(
                        "is-verified"
                    );

                    verifyEmailButton.innerHTML = `
                        <span
                            class="profile-button-icon"
                            aria-hidden="true"
                        >
                            <!-- Lucide icon: BadgeCheck -->
                        </span>

                        <span>Email Verified</span>
                    `;

                    verifyEmailButton.disabled = true;

                    setFormMessage(
                        "Email verified successfully."
                    );

                    return;
                }

                window.location.href =
                    "/profile/verify-email";
            }
        );
    }

    if (changePasswordButton) {
        changePasswordButton.addEventListener(
            "click",
            () => {
                if (isLiveServerPreview()) {
                    setFormMessage(
                        "Password changes will be available after connecting Spring Security.",
                        "information"
                    );

                    return;
                }

                window.location.href =
                    "/profile/change-password";
            }
        );
    }

    restorePreviewProfile();
    rememberOriginalValues();
    setEditingState(false);
});