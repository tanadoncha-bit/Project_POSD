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
        if (formMessage) formMessage.hidden = true;
        if (message) toast[type === "error" ? "error" : type === "success" ? "success" : "info"](message);
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
            editableInputs[0].focus({ preventScroll: true });
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
            editButton?.focus({ preventScroll: true });
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
        workflowApi('/api/v1/profile/verification').then(info => {
            verifyEmailButton.disabled = info.verified || !info.configured;
            verifyEmailButton.title = info.verified ? 'Email verified' : info.configured ? 'Verify your email' : 'Email delivery is not configured';
            if(info.verified) verifyEmailButton.querySelector('span:last-child').textContent='Email verified';
        }).catch(() => {});
        verifyEmailButton.onclick = async () => {
            try {
                await workflowApi('/api/v1/profile/verification',{method:'POST'});
                const done=await workflowForm('Verify email','A code has been queued for your current email address. It expires in 30 minutes.','Verification code',token=>workflowApi('/api/v1/profile/verification/confirm',{method:'POST',body:JSON.stringify({token})}));
                if(done) { verifyEmailButton.disabled=true; verifyEmailButton.querySelector('span:last-child').textContent='Email verified'; }
            } catch(e) { toast.error(e.message); }
        };
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
// Clicking the portrait opens the picker; selecting a valid image submits the existing upload form.
const avatarButton = document.getElementById("change-avatar-button");
const avatarInput = document.getElementById("avatar-image");
const avatarForm = document.querySelector(".avatar-upload-form");
const avatarMessage = document.getElementById("avatar-upload-message");
avatarButton?.addEventListener("click", () => avatarInput.click());
avatarInput?.addEventListener("change", () => {
    const file = avatarInput.files[0];
    if (!file) return;
    let error = "";
    if (file.size > 2 * 1024 * 1024) error = "Choose an image up to 2 MB.";
    else if (!["image/jpeg", "image/png"].includes(file.type)) error = "Choose a JPG or PNG image.";
    avatarMessage.hidden = Boolean(error);
    avatarMessage.textContent = error || "Saving profile picture...";
    avatarMessage.classList.toggle("form-feedback", Boolean(error));
    if (error) { toast.error(error); avatarInput.value = ""; avatarButton.focus(); return; }
    avatarButton.disabled = true;
    avatarButton.setAttribute("aria-busy", "true");
    avatarForm.requestSubmit();
});
