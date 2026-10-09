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
        document.querySelector(".site-header");

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
        if (isOpen) document.dispatchEvent(new CustomEvent("header-menu-open", { detail: "navigation" }));
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

    document.addEventListener("header-menu-open", (event) => {
        if (event.detail !== "navigation") setMenuOpen(false);
    });

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

    document.addEventListener("click", (event) => {
        if (!menuButton?.contains(event.target) && !navigation?.contains(event.target)) {
            setMenuOpen(false);
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && document.body.classList.contains("menu-open")) {
            setMenuOpen(false);
            menuButton?.focus();
        }
    });

    window.addEventListener(
        "resize",
        () => {
            if (window.innerWidth > 1000) {
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

        if (isOpen) document.dispatchEvent(new CustomEvent("header-menu-open", { detail: "profile" }));
        profileMenu.hidden = !isOpen;

        profileTrigger.setAttribute(
            "aria-expanded",
            String(isOpen)
        );
    }

    document.addEventListener("header-menu-open", (event) => {
        if (event.detail !== "profile") setProfileMenuOpen(false);
    });

    profileDropdown?.addEventListener("keydown", event => {
        if (event.key === "Escape" && !profileMenu.hidden) {
            setProfileMenuOpen(false); profileTrigger.focus(); event.stopPropagation();
        }
    });
    profileDropdown?.addEventListener("focusout", event => {
        if (!profileDropdown.contains(event.relatedTarget)) setProfileMenuOpen(false);
    });

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

    if (isOpen) document.dispatchEvent(new CustomEvent("header-menu-open", { detail: "modal" }));
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

    const registrationError = document.getElementById("registration-error");
    function showRegistrationError(message, field) {
        registrationError.textContent = message;
        registrationError.hidden = true;
        toast.error(message);
        if (field) { field.setAttribute("aria-invalid", "true"); field.focus(); }

    }
    registerForm?.addEventListener("invalid", event => {
        event.preventDefault();
        showRegistrationError(event.target.validationMessage, event.target);
    }, true);
    registerForm?.addEventListener("input", event => event.target.removeAttribute("aria-invalid"));
    registerForm?.addEventListener("submit", async event => {
        event.preventDefault();
        const data = new FormData(registerForm);
        const password = data.get("password");
        if (password !== data.get("confirmPassword")) {
            showRegistrationError("Passwords do not match.", registerForm.elements.confirmPassword); return;
        }
        if (new TextEncoder().encode(password).length > 72) {
            showRegistrationError("Password must not exceed 72 UTF-8 bytes.", registerForm.elements.password); return;
        }
        if (isStaticPreview) {
            savePreviewLogin(data.get("fullName")); setModalOpen("register-modal", false); updateAuthenticationView(); return;
        }
        const button = registerForm.querySelector('[type="submit"]');
        button.disabled = true; registrationError.hidden = true;
        try {
            const response = await fetch("/api/v1/users", {
                method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": data.get("_csrf") },
                body: JSON.stringify(Object.fromEntries(data))
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.details?.join(" ") || result.message || "Unable to register. Check your details and try again.");
            registerForm.reset(); setModalOpen("register-modal", false); setModalOpen("login-modal", true);
            document.getElementById("login-username").value = data.get("username");
            toast.success("Account created. Sign in to continue.");
            document.getElementById("login-password").focus();
        } catch (error) { showRegistrationError(error.message); }
        finally { button.disabled = false; }
    });
    if (registrationError?.textContent.trim()) {
        setModalOpen("register-modal", true);
    }

    if (!isStaticPreview) {
        fetch('/api/v1/auth/providers').then(response => response.ok ? response.json() : {}).then(providers => {
            document.querySelectorAll('[data-google-login]').forEach(control => {
                if (control instanceof HTMLButtonElement) control.disabled = !providers.google;
                else control.hidden = !providers.google || (control.hasAttribute('data-google-link') && providers.googleLinked);
                control.title = providers.google ? 'Continue securely with Google' : 'Google login is not configured yet';
                if (providers.google) control.addEventListener('click', () => location.assign('/oauth2/authorization/google'));
            });
        }).catch(() => {});
    }

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

                const form = document.createElement("form");
                form.method = "POST"; form.action = "/logout";
                const token = document.createElement("input"); token.type = "hidden"; token.name = "_csrf";
                token.value = document.querySelector('meta[name="_csrf"]')?.content || "";
                form.append(token); document.body.append(form); form.submit();
            }
        );
}


function initializeSearchOverlay() {
    const overlay = document.getElementById("search-overlay");
    if (!overlay) return;
    const input = overlay.querySelector("input[type=search]");
    const grid = overlay.querySelector(".search-results-grid");
    const message = overlay.querySelector(".search-empty-message");
    const more = overlay.querySelector(".search-load-more");
    const title = overlay.querySelector(".search-result-preview h2");
    let controller, timer, page = 0, trigger;
    const statusNames = { AVAILABLE: "Available", IN_USE: "In use", MAINTENANCE: "Maintenance", DISPOSED: "Unavailable" };
    function element(tag, className, text) {
        const node = document.createElement(tag); node.className = className;
        if (text) node.textContent = text; return node;
    }
    async function search(append = false) {
        controller?.abort(); controller = new AbortController();
        const current = controller;
        if (!append) { page = 0; grid.replaceChildren(); }
        more.hidden = true; message.hidden = false; message.textContent = "Loading equipment...";
        title.textContent = input.value.trim() ? "Search results" : "Equipment";
        try {
            const url = new URL("/api/v1/equipment", location.origin);
            url.search = new URLSearchParams({ keyword: input.value.trim(), size: "8", page: String(page), sort: "id,asc" });
            const response = await fetch(url, { signal: current.signal });
            if (!response.ok) throw new Error("Unable to load equipment. Please search again.");
            const data = await response.json();
            const categories = await fetch("/api/v1/categories", { signal: current.signal }).then(r => r.ok ? r.json() : []);
            for (const equipment of data.content) {
                const card = element("a", "search-equipment-card equipment-card"); card.href = `/equipment/${equipment.id}`;
                const media = element("div", "equipment-card-image");
                // Use the same photo and status treatment as the equipment catalog.
                if (equipment.imageUrl && (equipment.imageUrl.startsWith("https://") || equipment.imageUrl.startsWith("/images/"))) {
                    const image = document.createElement("img"); image.src = equipment.imageUrl; image.alt = equipment.name;
                    image.onerror = () => media.replaceChildren(element("span", "equipment-image-placeholder", "No image")); media.append(image);
                } else media.append(element("span", "equipment-image-placeholder", "No image"));
                const badge = element("span", "availability-badge", statusNames[equipment.status] || "Unavailable");
                badge.dataset.status = equipment.status;
                media.append(badge);
                const body = element("div", "equipment-card-body");
                body.append(element("h3", "", equipment.name), element("p", "", categories.find(category => category.id === equipment.categoryId)?.name || "IT Equipment"));
                card.append(media, body); grid.append(card);
            }
            message.hidden = grid.children.length > 0; message.textContent = "No equipment found.";
            more.hidden = data.last !== false;
        } catch (error) { if (error.name !== "AbortError") { message.hidden = false; message.textContent = error.message; } }
    }
    let closingTimer, openingFrame, opening = false;
    function finishSearchClose() {
        if (opening) return;
        clearTimeout(closingTimer);
        setModalOpen("search-overlay", false);
        trigger?.focus({ preventScroll: true });
    }
    overlay.addEventListener("transitionend", event => {
        if (event.target === overlay && event.propertyName === "transform" && !opening) finishSearchClose();
    });
    function setSearchOpen(open) {
        if (open === opening && (open || overlay.hidden)) return;
        clearTimeout(closingTimer);
        cancelAnimationFrame(openingFrame);
        opening = open;
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        document.querySelectorAll("[data-open-search]").forEach(button => button.setAttribute("aria-expanded", String(open)));
        if (open) {
            if (overlay.hidden) trigger = document.activeElement;
            setModalOpen("search-overlay", true);
            // Establish the off-screen position before starting the transition.
            void overlay.offsetHeight;
            openingFrame = requestAnimationFrame(() => {
                if (!opening) return;
                overlay.classList.add("is-open");
                input.focus({ preventScroll: true });
            });
            search();
        } else {
            clearTimeout(timer); controller?.abort();
            overlay.classList.remove("is-open");
            if (reducedMotion) finishSearchClose();
            else closingTimer = setTimeout(finishSearchClose, 750);
        }
    }
    input.addEventListener("input", () => { controller?.abort(); clearTimeout(timer); timer = setTimeout(() => search(), 250); });
    overlay.querySelector("form").addEventListener("submit", event => { event.preventDefault(); clearTimeout(timer); search(); });
    more.addEventListener("click", () => { page++; search(true); });
    document.addEventListener("click", event => {
        if (event.target.closest("[data-open-search]")) { event.preventDefault(); setSearchOpen(true); }
        if (event.target.closest("[data-close-search]")) { event.preventDefault(); setSearchOpen(false); }
        const keyword = event.target.closest("[data-search-keyword]");
        if (keyword) { input.value = keyword.dataset.searchKeyword; clearTimeout(timer); search(); input.focus(); }
    });
    document.addEventListener("keydown", event => {
        if (overlay.hidden || document.querySelector("dialog[open]")) return;
        if (event.key === "Escape") setSearchOpen(false);
        if (event.key === "Tab") {
            const items = [...overlay.querySelectorAll('a[href],button,input')].filter(node => !node.disabled && node.getClientRects().length);
            const first = items[0], last = items[items.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
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
    if (!isStaticPreview && (new URLSearchParams(window.location.search).has("loginError") || new URLSearchParams(window.location.search).has("googleLoginError") || new URLSearchParams(window.location.search).has("credentialsSet"))) {
        setModalOpen("login-modal", true);
    }

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
