const previewSessionKeys = {
    authenticated: "leaditPreviewAuthenticated",
    username: "leaditPreviewUsername",
    role: "leaditPreviewRole"
};


function isPreviewAuthenticated() {
    return sessionStorage.getItem(
        previewSessionKeys.authenticated
    ) === "true";
}


function getPreviewUsername() {
    return sessionStorage.getItem(
        previewSessionKeys.username
    ) || "Preview User";
}


function getPreviewRole() {
    return sessionStorage.getItem(
        previewSessionKeys.role
    ) || "USER";
}


function savePreviewLogin(username) {
    sessionStorage.setItem(
        previewSessionKeys.authenticated,
        "true"
    );

    sessionStorage.setItem(
        previewSessionKeys.username,
        username || "Preview User"
    );

    sessionStorage.setItem(
        previewSessionKeys.role,
        "USER"
    );
}


function clearPreviewLogin() {
    Object.values(previewSessionKeys).forEach((key) => {
        sessionStorage.removeItem(key);
    });
}


async function loadSiteTemplates() {
    const response = await fetch("../fragments/site.html");

    if (!response.ok) {
        throw new Error(
            "ไม่สามารถโหลด fragments/site.html ได้"
        );
    }

    const html = await response.text();

    const fragmentDocument =
        new DOMParser().parseFromString(
            html,
            "text/html"
        );

    injectTemplate(
        fragmentDocument,
        "site-header-template",
        "site-header"
    );

    injectTemplate(
        fragmentDocument,
        "site-footer-template",
        "site-footer"
    );

    injectTemplate(
        fragmentDocument,
        "login-modal-template",
        "site-modals"
    );

    injectTemplate(
        fragmentDocument,
        "register-modal-template",
        "site-modals"
    );
}


function injectTemplate(
    fragmentDocument,
    templateId,
    targetId
) {
    const template =
        fragmentDocument.getElementById(templateId);

    const target =
        document.getElementById(targetId);

    if (!template || !target) {
        return;
    }

    target.appendChild(
        template.content.cloneNode(true)
    );
}


function initializeActiveNavigation() {
    const currentPage =
        document.body.dataset.page || "";

    document
        .querySelectorAll("[data-nav-page]")
        .forEach((link) => {
            link.classList.toggle(
                "active",
                link.dataset.navPage === currentPage
            );
        });
}


function initializeMobileMenu() {
    const menuButton =
        document.querySelector(".menu-toggle");

    const navigation =
        document.getElementById("main-navigation");

    function setMenuOpen(isOpen) {
        document.body.classList.toggle(
            "menu-open",
            isOpen
        );

        menuButton?.setAttribute(
            "aria-expanded",
            String(isOpen)
        );

        menuButton?.setAttribute(
            "aria-label",
            isOpen ? "ปิดเมนู" : "เปิดเมนู"
        );
    }

    menuButton?.addEventListener("click", () => {
        const isOpen =
            document.body.classList.contains(
                "menu-open"
            );

        setMenuOpen(!isOpen);
    });

    navigation?.addEventListener("click", (event) => {
        if (event.target.closest("a")) {
            setMenuOpen(false);
        }
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 820) {
            setMenuOpen(false);
        }
    });
}


function initializeSearch() {
    const searchForm =
        document.querySelector(".header-search");

    const searchButton =
        searchForm?.querySelector(".search-toggle");

    const searchInput =
        searchForm?.querySelector(
            ".header-search-input"
        );

    const currentPage =
        document.body.dataset.page || "";

    if (currentPage === "requests") {
        searchForm.action =
            "../borrow/my-history.html";

        searchInput.placeholder =
            "Search your requests...";
    } else {
        searchForm.action =
            "../equipment/list.html";

        searchInput.placeholder =
            "Search equipment...";
    }

    function setSearchOpen(isOpen) {
        searchForm?.classList.toggle(
            "search-open",
            isOpen
        );

        searchButton?.setAttribute(
            "aria-expanded",
            String(isOpen)
        );

        if (isOpen) {
            window.setTimeout(() => {
                searchInput?.focus();
            }, 150);
        }
    }

    searchButton?.addEventListener(
        "click",
        (event) => {
            event.preventDefault();
            event.stopPropagation();

            const isOpen =
                searchForm.classList.contains(
                    "search-open"
                );

            setSearchOpen(!isOpen);
        }
    );

    searchForm?.addEventListener(
        "submit",
        (event) => {
            if (!searchInput.value.trim()) {
                event.preventDefault();
                setSearchOpen(true);
            }
        }
    );

    document.addEventListener("click", (event) => {
        if (
            searchForm?.classList.contains(
                "search-open"
            ) &&
            !searchForm.contains(event.target)
        ) {
            setSearchOpen(false);
        }
    });

    window.previewSearch = {
        open: () => setSearchOpen(true),
        close: () => setSearchOpen(false),
        input: searchInput
    };
}


function initializeProfileMenu() {
    const profileDropdown =
        document.querySelector(".profile-dropdown");

    const profileTrigger =
        profileDropdown?.querySelector(
            ".profile-trigger"
        );

    const profileMenu =
        profileDropdown?.querySelector(
            ".profile-menu"
        );

    function setProfileMenuOpen(isOpen) {
        if (!profileMenu || !profileTrigger) {
            return;
        }

        profileMenu.hidden = !isOpen;

        profileTrigger.setAttribute(
            "aria-expanded",
            String(isOpen)
        );
    }

    profileTrigger?.addEventListener(
        "click",
        (event) => {
            event.stopPropagation();

            setProfileMenuOpen(
                profileMenu.hidden
            );
        }
    );

    document.addEventListener("click", (event) => {
        if (
            profileDropdown &&
            !profileDropdown.contains(event.target)
        ) {
            setProfileMenuOpen(false);
        }
    });
}


function setModalOpen(modalId, isOpen) {
    const modal = document.getElementById(modalId);

    if (!modal) {
        return;
    }

    modal.hidden = !isOpen;

    const hasOpenModal =
        Array.from(
            document.querySelectorAll(
                ".modal-backdrop"
            )
        ).some((currentModal) => {
            return !currentModal.hidden;
        });

    document.body.classList.toggle(
        "modal-open",
        hasOpenModal
    );

    if (isOpen) {
        modal.querySelector(
            ".modal-close"
        )?.focus();
    }
}


function initializeAuthenticationModals() {
    document.addEventListener(
        "click",
        (event) => {
            const openLoginButton =
                event.target.closest(
                    "[data-open-login]"
                );

            const openRegisterButton =
                event.target.closest(
                    "[data-open-register]"
                );

            const closeButton =
                event.target.closest(
                    "[data-close-modal]"
                );

            const protectedAction =
                event.target.closest(
                    "[data-requires-login]"
                );

            if (
                protectedAction &&
                !isPreviewAuthenticated()
            ) {
                event.preventDefault();
                setModalOpen("login-modal", true);
                return;
            }

            if (openLoginButton) {
                setModalOpen(
                    "register-modal",
                    false
                );

                setModalOpen(
                    "login-modal",
                    true
                );

                return;
            }

            if (openRegisterButton) {
                setModalOpen(
                    "login-modal",
                    false
                );

                setModalOpen(
                    "register-modal",
                    true
                );

                return;
            }

            if (closeButton) {
                const modal =
                    closeButton.closest(
                        ".modal-backdrop"
                    );

                setModalOpen(modal.id, false);
            }
        }
    );

    document
        .querySelectorAll(".modal-backdrop")
        .forEach((modal) => {
            modal.addEventListener(
                "click",
                (event) => {
                    if (event.target === modal) {
                        setModalOpen(
                            modal.id,
                            false
                        );
                    }
                }
            );
        });

    const loginForm =
        document.getElementById(
            "preview-login-form"
        );

    loginForm?.addEventListener(
        "submit",
        (event) => {
            event.preventDefault();

            const username =
                document
                    .getElementById(
                        "login-username"
                    )
                    .value
                    .trim();

            savePreviewLogin(username);

            setModalOpen("login-modal", false);
            updatePreviewAuthentication();
        }
    );

    const registerForm =
        document.getElementById(
            "preview-register-form"
        );

    registerForm?.addEventListener(
        "submit",
        (event) => {
            event.preventDefault();

            const name =
                document
                    .getElementById(
                        "register-name"
                    )
                    .value
                    .trim();

            savePreviewLogin(name);

            setModalOpen(
                "register-modal",
                false
            );

            updatePreviewAuthentication();
        }
    );

    document
        .querySelectorAll(
            "[data-preview-provider-login]"
        )
        .forEach((button) => {
            button.addEventListener(
                "click",
                () => {
                    savePreviewLogin(
                        "KKU Preview User"
                    );

                    setModalOpen(
                        "login-modal",
                        false
                    );

                    updatePreviewAuthentication();
                }
            );
        });

    document
        .querySelector(
            "[data-preview-logout]"
        )
        ?.addEventListener("click", () => {
            clearPreviewLogin();
            updatePreviewAuthentication();
        });

    document.addEventListener(
        "keydown",
        (event) => {
            if (event.key !== "Escape") {
                return;
            }

            document
                .querySelectorAll(
                    ".modal-backdrop"
                )
                .forEach((modal) => {
                    setModalOpen(
                        modal.id,
                        false
                    );
                });
        }
    );
}


function updatePreviewAuthentication() {
    const authenticated =
        isPreviewAuthenticated();

    document
        .querySelectorAll("[data-guest-only]")
        .forEach((element) => {
            element.hidden = authenticated;
        });

    document
        .querySelectorAll("[data-auth-only]")
        .forEach((element) => {
            element.hidden = !authenticated;
        });

    const username = getPreviewUsername();
    const role = getPreviewRole();

    document
        .querySelectorAll("[data-profile-name]")
        .forEach((element) => {
            element.textContent = username;
        });

    document
        .querySelectorAll("[data-profile-role]")
        .forEach((element) => {
            element.textContent = role;
        });

    document
        .querySelectorAll("[data-profile-initial]")
        .forEach((element) => {
            element.textContent =
                username.charAt(0).toUpperCase();
        });

    document.body.dataset.authenticated =
        String(authenticated);
}


async function initializeSitePreview() {
    try {
        await loadSiteTemplates();

        initializeActiveNavigation();
        initializeMobileMenu();
        initializeSearch();
        initializeProfileMenu();
        initializeAuthenticationModals();
        updatePreviewAuthentication();

        document.dispatchEvent(
            new CustomEvent("leadit:site-ready")
        );
    } catch (error) {
        console.error(error);
    }
}


initializeSitePreview();