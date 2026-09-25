const borrowRequestForm =
    document.getElementById("borrow-request-form");

const currentUserIdInput =
    document.getElementById("current-user-id");

const equipmentSelect =
    document.getElementById("equipment-id");

const borrowDateInput =
    document.getElementById("borrow-date");

const dueDateInput =
    document.getElementById("due-date");

const borrowNoteInput =
    document.getElementById("borrow-note");

const borrowSubmitButton =
    document.getElementById("borrow-submit-button");

const borrowFormError =
    document.getElementById("borrow-form-error");

const borrowSuccessModal =
    document.getElementById("borrow-success-modal");

const borrowSuccessCloseButton =
    document.getElementById("borrow-success-close");


function formatDate(date) {
    const year = date.getFullYear();

    const month = String(
        date.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        date.getDate()
    ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}


function initializeBorrowDates() {
    if (!borrowDateInput || !dueDateInput) {
        return;
    }

    const today = new Date();
    const tomorrow = new Date(today);

    tomorrow.setDate(today.getDate() + 1);

    borrowDateInput.value = formatDate(today);
    dueDateInput.min = formatDate(tomorrow);

    if (!dueDateInput.value) {
        dueDateInput.value = formatDate(tomorrow);
    }
}


function showBorrowError(message) {
    if (!borrowFormError) {
        return;
    }

    borrowFormError.textContent = message;
    borrowFormError.hidden = false;
}


function clearBorrowError() {
    if (!borrowFormError) {
        return;
    }

    borrowFormError.textContent = "";
    borrowFormError.hidden = true;
}


function setBorrowSubmitting(isSubmitting) {
    if (!borrowSubmitButton) {
        return;
    }

    borrowSubmitButton.disabled = isSubmitting;
    borrowSubmitButton.textContent =
        isSubmitting
            ? "Submitting..."
            : "Submit Request";
}


function setBorrowSuccessOpen(isOpen) {
    if (!borrowSuccessModal) {
        return;
    }

    borrowSuccessModal.hidden = !isOpen;
    document.body.classList.toggle("modal-open", isOpen);

    if (isOpen) {
        borrowSuccessCloseButton?.focus();
    }
}


function validateBorrowForm() {
    const userId = currentUserIdInput?.value.trim();
    const equipmentId = equipmentSelect?.value;
    const borrowDate = borrowDateInput?.value;
    const dueDate = dueDateInput?.value;

    if (!userId) {
        return "ไม่พบข้อมูลผู้ใช้งาน กรุณาเข้าสู่ระบบอีกครั้ง";
    }

    if (!equipmentId) {
        return "กรุณาเลือกอุปกรณ์ที่ต้องการยืม";
    }

    if (!borrowDate) {
        return "ไม่พบวันที่ยืม";
    }

    if (!dueDate) {
        return "กรุณาเลือกวันที่กำหนดคืน";
    }

    if (dueDate <= borrowDate) {
        return "วันที่กำหนดคืนต้องอยู่หลังวันที่ยืม";
    }

    return "";
}


borrowRequestForm?.addEventListener(
    "submit",
    async (event) => {
        event.preventDefault();
        clearBorrowError();

        const validationMessage =
            validateBorrowForm();

        if (validationMessage) {
            showBorrowError(validationMessage);
            return;
        }

        const requestBody = {
            userId: Number(currentUserIdInput.value),
            borrowDate: borrowDateInput.value,
            dueDate: dueDateInput.value,
            note: borrowNoteInput.value.trim(),
            items: [
                {
                    equipmentId: Number(equipmentSelect.value),
                    quantity: 1
                }
            ]
        };

        setBorrowSubmitting(true);

        try {
            const response = await fetch(
                "/api/v1/borrow-requests",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(requestBody)
                }
            );

            if (!response.ok) {
                let message =
                    "ไม่สามารถส่งคำขอยืมได้ กรุณาลองอีกครั้ง";

                try {
                    const errorResponse =
                        await response.json();

                    message =
                        errorResponse.message ||
                        errorResponse.error ||
                        message;
                } catch (error) {
                    // ใช้ข้อความเริ่มต้นเมื่อ API ไม่ได้ส่ง JSON
                }

                throw new Error(message);
            }

            borrowRequestForm.reset();
            initializeBorrowDates();
            setBorrowSuccessOpen(true);
        } catch (error) {
            showBorrowError(error.message);
        } finally {
            setBorrowSubmitting(false);
        }
    }
);


borrowSuccessCloseButton?.addEventListener(
    "click",
    () => {
        setBorrowSuccessOpen(false);
    }
);


borrowSuccessModal?.addEventListener(
    "click",
    (event) => {
        if (event.target === borrowSuccessModal) {
            setBorrowSuccessOpen(false);
        }
    }
);


document.addEventListener("keydown", (event) => {
    if (
        event.key === "Escape" &&
        borrowSuccessModal &&
        !borrowSuccessModal.hidden
    ) {
        setBorrowSuccessOpen(false);
    }
});


initializeBorrowDates();