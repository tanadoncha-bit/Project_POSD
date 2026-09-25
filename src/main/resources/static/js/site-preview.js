const previewSessionKeys = {
    authenticated: "leaditPreviewAuthenticated",
    username: "leaditPreviewUsername",
    role: "leaditPreviewRole"
};

const isStaticPreview =
    window.location.pathname.includes(
        "/src/main/resources/templates/"
    );


function isAuthenticated() {
    if (isStaticPreview) {
        return sessionStorage.getItem(
            previewSessionKeys.authenticated
        ) === "true";
    }

    return document.body.dataset.authenticated === "true";
}


function getCurrentUsername() {
    if (isStaticPreview) {
        return sessionStorage.getItem(
            previewSessionKeys.username
        ) || "Preview User";
    }

    return document.body.dataset.username || "";
}


function getCurrentRole() {
    if (isStaticPreview) {
        return sessionStorage.getItem(
            previewSessionKeys.role
        ) || "USER";
    }

    return document.body.dataset.role || "";
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


function getFragmentUrl() {
    if (isStaticPreview) {
        return `../fragments/site.html?v=${Date.now()}`;
    }

    return `/fragments/site.html?v=${Date.now()}`;
}


async function loadSiteTemplates() {
    const headerAlreadyRendered =
        document.querySelector("#site-header .site-header");

    if (headerAlreadyRendered) {
        return;
    }

    const response = await fetch(
        getFragmentUrl(),
        {
            cache: "no-store"
        }
    );

    if (!response.ok) {
        throw new Error(
            "Unable to load fragments/site.html"
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

    injectTemplate(
        fragmentDocument,
        "search-overlay-template",
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

    const templateContent =
        document.importNode(
            template.content,
            true
        );

    target.appendChild(templateContent);
}


function applyEnvironmentRoutes() {
    if (isStaticPreview) {
        return;
    }

    document
        .querySelectorAll("[data-spring-href]")
        .forEach((element) => {
            element.href =
                element.dataset.springHref;
        });

    document
        .querySelectorAll("[data-spring-src]")
        .forEach((element) => {
            element.src =
                element.dataset.springSrc;
        });

    document
        .querySelectorAll("[data-spring-action]")
        .forEach((element) => {
            element.action =
                element.dataset.springAction;
        });
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
        document.getElementById(
            "main-navigation"
        );

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
            isOpen
                ? "Close menu"
                : "Open menu"
        );
    }

    menuButton?.addEventListener(
        "click",
        () => {
            const isOpen =
                document.body.classList.contains(
                    "menu-open"
                );

            setMenuOpen(!isOpen);
        }
    );

    navigation?.addEventListener(
        "click",
        (event) => {
            if (event.target.closest("a")) {
                setMenuOpen(false);
            }
        }
    );

    window.addEventListener(
        "resize",
        () => {
            if (window.innerWidth > 900) {
                setMenuOpen(false);
            }
        }
    );
}


function initializeProfileMenu() {
    const profileDropdown =
        document.querySelector(
            ".profile-dropdown"
        );

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

    document.addEventListener(
        "click",
        (event) => {
            if (
                profileDropdown &&
                !profileDropdown.contains(
                    event.target
                )
            ) {
                setProfileMenuOpen(false);
            }
        }
    );
}


function setModalOpen(modalId, isOpen) {
    const modal =
        document.getElementById(modalId);

    if (!modal) {
        return;
    }

    modal.hidden = !isOpen;

    const hasOpenModal =
        Array.from(
            document.querySelectorAll(
                ".modal-backdrop, .search-overlay"
            )
        ).some((currentModal) => {
            return !currentModal.hidden;
        });

    document.body.classList.toggle(
        "modal-open",
        hasOpenModal
    );

    if (isOpen) {
        modal
            .querySelector(
                "button, input, select, textarea"
            )
            ?.focus();
    }
}


function initializeAuthenticationModals() {
    document.addEventListener(
        "click",
        (event) => {
            const protectedAction =
                event.target.closest(
                    "[data-requires-login]"
                );

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

            if (
                protectedAction &&
                !isAuthenticated()
            ) {
                event.preventDefault();

                document
                    .querySelectorAll(
                        ".equipment-detail-backdrop, .success-modal-backdrop"
                    )
                    .forEach((modal) => {
                        modal.hidden = true;
                    });

                setModalOpen(
                    "login-modal",
                    true
                );

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

                if (modal) {
                    setModalOpen(
                        modal.id,
                        false
                    );
                }
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
            if (!isStaticPreview) {
                return;
            }

            event.preventDefault();

            const username =
                document
                    .getElementById(
                        "login-username"
                    )
                    ?.value.trim();

            savePreviewLogin(username);

            setModalOpen(
                "login-modal",
                false
            );

            updateAuthenticationView();
        }
    );

    const registerForm =
        document.getElementById(
            "preview-register-form"
        );

    registerForm?.addEventListener(
        "submit",
        (event) => {
            if (!isStaticPreview) {
                return;
            }

            event.preventDefault();

            const password =
                document
                    .getElementById(
                        "register-password"
                    )
                    ?.value;

            const confirmPassword =
                document
                    .getElementById(
                        "register-confirm-password"
                    )
                    ?.value;

            if (password !== confirmPassword) {
                window.alert(
                    "Password and Confirm Password must match."
                );

                return;
            }

            const name =
                document
                    .getElementById(
                        "register-name"
                    )
                    ?.value.trim();

            savePreviewLogin(name);

            setModalOpen(
                "register-modal",
                false
            );

            updateAuthenticationView();
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
                    if (!isStaticPreview) {
                        return;
                    }

                    savePreviewLogin(
                        "KKU Preview User"
                    );

                    setModalOpen(
                        "login-modal",
                        false
                    );

                    updateAuthenticationView();
                }
            );
        });

    document
        .querySelector(
            "[data-preview-logout]"
        )
        ?.addEventListener(
            "click",
            () => {
                if (isStaticPreview) {
                    clearPreviewLogin();
                    updateAuthenticationView();
                    return;
                }

                window.location.href = "/logout";
            }
        );
}


function initializeSearchOverlay() {
    const searchOverlay = document.getElementById("search-overlay");
    const searchInput = document.getElementById("global-search-input");

    // เลือกแท็กคำค้นหายอดนิยม (Laptop, MacBook, ฯลฯ)
    document.addEventListener("click", (event) => {
        const keywordButton = event.target.closest("[data-search-keyword]");

        if (keywordButton && searchInput) {
            searchInput.value = keywordButton.dataset.searchKeyword;
            searchInput.focus();
        }
    });

    // ปิด Overlay เมื่อคลิกพื้นที่ว่างภายนอก Panel
    searchOverlay?.addEventListener("click", (event) => {
        if (event.target === searchOverlay) {
            if (typeof setSearchOpen === "function") {
                setSearchOpen(false);
            }
        }
    });
}


function updateAuthenticationView() {
    const authenticated =
        isAuthenticated();

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

    const username =
        getCurrentUsername();

    const role =
        getCurrentRole();

    document
        .querySelectorAll(
            "[data-profile-name]"
        )
        .forEach((element) => {
            element.textContent = username;
        });

    document
        .querySelectorAll(
            "[data-profile-role]"
        )
        .forEach((element) => {
            element.textContent = role;
        });

    document
        .querySelectorAll(
            "[data-profile-initial]"
        )
        .forEach((element) => {
            element.textContent =
                username
                    ? username.charAt(0).toUpperCase()
                    : "U";
        });

    document.body.dataset.authenticated =
        String(authenticated);
}


function initializeEscapeKey() {
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
                    if (!modal.hidden) {
                        setModalOpen(
                            modal.id,
                            false
                        );
                    }
                });
        }
    );
}


async function initializeSite() {
    try {
        await loadSiteTemplates();
    } catch (error) {
        console.warn(
            "Shared templates were not fetched; using server-rendered fragments.",
            error
        );
    }

    applyEnvironmentRoutes();
    initializeActiveNavigation();
    initializeMobileMenu();
    initializeProfileMenu();
    initializeAuthenticationModals();
    initializeSearchOverlay();
    initializeEscapeKey();
    updateAuthenticationView();

    document.dispatchEvent(
        new CustomEvent(
            "leadit:site-ready"
        )
    );
}


document.addEventListener(
    "DOMContentLoaded",
    initializeSite
);