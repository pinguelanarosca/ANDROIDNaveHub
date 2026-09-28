package com.example.manager

object PlatformPopupBlockers {

    fun getBlockerScriptForPlatform(platformId: String): String {
        return when (platformId.lowercase()) {
            "8u" -> EIGHTU_POPUP_BLOCKER_JS
            "777" -> SEVEN_SEVEN_POPUP_BLOCKER_JS
            "365gg", "365" -> THREE_SIXTY_FIVE_GG_POPUP_BLOCKER_JS
            "93h", "93has" -> NINETY_THREE_H_POPUP_BLOCKER_JS
            else -> ""
        }
    }

    fun getTitleScript(title: String): String {
        val escapedTitle = title.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
        (function() {
            'use strict';
            const accountTitle = "$escapedTitle";
            const applyTitle = () => {
                if (document.title !== accountTitle) {
                    document.title = accountTitle;
                }
            };
            const observeTitle = () => {
                const root = document.documentElement;
                if (!root) {
                    document.addEventListener('DOMContentLoaded', observeTitle, { once: true });
                    return;
                }
                applyTitle();
                new MutationObserver(applyTitle).observe(root, {
                    childList: true,
                    subtree: true,
                    characterData: true,
                });
            };
            observeTitle();
        })();
        """.trimIndent()
    }

    private val EIGHTU_POPUP_BLOCKER_JS = """
    (function() {
        'use strict';

        if (window.__navehub8uPopupBlockerInstalled) {
            return;
        }
        window.__navehub8uPopupBlockerInstalled = true;

        const popupSelectors = [
            '.van-popup',
            '.van-dialog',
            '.dialog-apknoty',
            '[role="dialog"]',
            '.popup',
            '.modal'
        ];

        const findPopup = (element) => {
            if (!element || !element.closest) {
                return null;
            }

            const closest = element.closest(popupSelectors.join(','));
            if (closest && closest !== document.body && closest !== document.documentElement) {
                return closest;
            }

            let current = element.parentElement;
            let depth = 0;
            while (current && current !== document.body && current !== document.documentElement && depth < 6) {
                const role = current.getAttribute('role');
                const className = String(current.className || '').toLowerCase();
                if (
                    role === 'dialog' ||
                    className.includes('popup') ||
                    className.includes('dialog') ||
                    className.includes('modal')
                ) {
                    return current;
                }
                current = current.parentElement;
                depth += 1;
            }

            return null;
        };

        const preservedTexts = [
            'Tempo de Gravação',
            'Selecione um pagamento',
        ];

        const shouldRemovePopup = (popup) => {
            if (!popup || popup === document.body || popup === document.documentElement) {
                return false;
            }
            const text = popup.textContent || '';
            if (preservedTexts.some((preserved) => text.includes(preserved))) {
                return false;
            }
            return true;
        };

        const findAssociatedOverlay = (popup) => {
            if (!popup || !popup.parentElement) {
                return null;
            }

            if (popup.previousElementSibling && popup.previousElementSibling.matches('.van-overlay')) {
                return popup.previousElementSibling;
            }
            if (popup.nextElementSibling && popup.nextElementSibling.matches('.van-overlay')) {
                return popup.nextElementSibling;
            }

            let previous = popup.previousElementSibling;
            let previousDistance = 0;
            while (previous && previousDistance < 3) {
                if (previous.matches('.van-overlay')) {
                    return previous;
                }
                previous = previous.previousElementSibling;
                previousDistance += 1;
            }

            let next = popup.nextElementSibling;
            let nextDistance = 0;
            while (next && nextDistance < 3) {
                if (next.matches('.van-overlay')) {
                    return next;
                }
                next = next.nextElementSibling;
                nextDistance += 1;
            }

            return null;
        };

        const removeTargetPopups = () => {
            const targets = new Set();
            const overlays = new Set();

            document.querySelectorAll(popupSelectors.join(',')).forEach((popup) => {
                if (shouldRemovePopup(popup)) {
                    targets.add(popup);
                    const overlay = findAssociatedOverlay(popup);
                    if (overlay) {
                        overlays.add(overlay);
                    }
                }
            });

            // Also remove any orphaned van-overlay if no popup is currently visible
            const visiblePopups = document.querySelectorAll('.van-popup:not([style*="display: none"]), .van-dialog:not([style*="display: none"])');
            if (visiblePopups.length === 0) {
                document.querySelectorAll('.van-overlay').forEach((overlay) => {
                    overlays.add(overlay);
                });
            }

            targets.forEach((popup) => popup.remove());
            overlays.forEach((overlay) => overlay.remove());

            if (document.body) {
                document.body.style.overflow = 'auto';
                document.body.classList.remove('van-overflow-hidden');
            }
            document.documentElement.style.overflow = 'auto';
        };

        const start = () => {
            if (!document.body) {
                return;
            }

            const observer = new MutationObserver((mutations) => {
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length) {
                        removeTargetPopups();
                    }
                }
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true
            });

            removeTargetPopups();
            setInterval(removeTargetPopups, 1000); // Periodic check to clear stuck translucent overlays
        };

        if (document.body) {
            start();
        } else {
            document.addEventListener('DOMContentLoaded', start, { once: true });
        }
    })();
    """.trimIndent()

    private val SEVEN_SEVEN_POPUP_BLOCKER_JS = """
    (function() {
        'use strict';
        if (window.__navehub777PopupBlockerInstalled) return;
        window.__navehub777PopupBlockerInstalled = true;

        const permissionText = 'Aviso de Permissão';
        const imageSources = [
            'oss.goodofs.com/images/poster/nurk1787078713673359.png',
            'static/images/c4/syscom/pwav2/pwa_update_banner.png?v=1.0'
        ];
        const popupSelectors = [
            '.van-popup',
            '.van-dialog',
            '.dialog-apknoty',
            '[role="dialog"]',
            '.popup',
            '.modal'
        ];

        const containsTargetImage = (popup) => {
            return Array.from(popup.querySelectorAll('img')).some((img) => {
                const src = img.getAttribute('src') || '';
                return imageSources.some((target) => src.includes(target));
            });
        };

        const shouldRemovePopup = (popup) => {
            if (!popup || popup === document.body || popup === document.documentElement) return false;
            return popup.textContent.includes(permissionText) || containsTargetImage(popup);
        };

        const findAssociatedOverlay = (popup) => {
            if (!popup || !popup.parentElement) return null;
            if (popup.previousElementSibling && popup.previousElementSibling.matches('.van-overlay')) return popup.previousElementSibling;
            if (popup.nextElementSibling && popup.nextElementSibling.matches('.van-overlay')) return popup.nextElementSibling;
            return null;
        };

        const removeTargetPopups = () => {
            const targets = new Set();
            const overlays = new Set();
            document.querySelectorAll(popupSelectors.join(',')).forEach((popup) => {
                if (shouldRemovePopup(popup)) {
                    targets.add(popup);
                    const overlay = findAssociatedOverlay(popup);
                    if (overlay) overlays.add(overlay);
                }
            });
            targets.forEach((popup) => popup.remove());
            overlays.forEach((overlay) => overlay.remove());
        };

        const start = () => {
            if (!document.body) return;
            const observer = new MutationObserver((mutations) => {
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length) removeTargetPopups();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            removeTargetPopups();
        };

        if (document.body) start();
        else document.addEventListener('DOMContentLoaded', start, { once: true });
    })();
    """.trimIndent()

    private val THREE_SIXTY_FIVE_GG_POPUP_BLOCKER_JS = """
    (function() {
        'use strict';
        if (window.__navehub365ggPopupBlockerInstalled) return;
        window.__navehub365ggPopupBlockerInstalled = true;

        const selectors = [
            '.van-overlay',
            '.van-popup',
            '.goldbox-content',
            '#pop-recode',
            '#rb-layer',
            '#rb-layer2',
            '#gold-coin-container'
        ];

        const hideTargets = () => {
            selectors.forEach((selector) => {
                document.querySelectorAll(selector).forEach((el) => {
                    el.style.display = 'none';
                });
            });
        };

        const start = () => {
            if (!document.body) return;
            const observer = new MutationObserver((mutations) => {
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length) hideTargets();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            hideTargets();
        };

        if (document.body) start();
        else document.addEventListener('DOMContentLoaded', start, { once: true });
    })();
    """.trimIndent()

    private val NINETY_THREE_H_POPUP_BLOCKER_JS = """
    (function() {
        'use strict';
        if (window.__navehub93hPopupBlockerInstalled) return;
        window.__navehub93hPopupBlockerInstalled = true;

        const shouldRemovePopup = (popup) => {
            if (popup.querySelector('.dialog-apknoty')) return true;
            return Boolean(
                popup.querySelector('img[src*="/images/poster/"]') &&
                !popup.textContent.trim()
            );
        };

        const removeTargetPopups = () => {
            document.querySelectorAll('.van-popup').forEach((popup) => {
                if (!shouldRemovePopup(popup)) return;
                [popup.previousElementSibling, popup.nextElementSibling]
                    .filter((element) => element && element.matches('.van-overlay'))
                    .forEach((backdrop) => backdrop.remove());
                popup.remove();
            });
        };

        const start = () => {
            if (!document.body) return;
            const observer = new MutationObserver((mutations) => {
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length) removeTargetPopups();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            removeTargetPopups();
        };

        if (document.body) start();
        else document.addEventListener('DOMContentLoaded', start, { once: true });
    })();
    """.trimIndent()
}
