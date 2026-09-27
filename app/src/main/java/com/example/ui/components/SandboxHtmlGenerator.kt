package com.example.ui.components

import com.example.domain.model.Account
import com.example.domain.model.Platform

object SandboxHtmlGenerator {

    fun generateHtml(
        platform: Platform,
        account: Account,
        isNativeProfile: Boolean = true,
        profileName: String = "default"
    ): String {
        val platformColor = platform.accentColorHex
        val platformName = platform.name
        val accountName = account.name
        val accountId = account.id

        val profileBadge = if (isNativeProfile) "NATIVE PROFILE: $profileName" else "FALLBACK ISOLATION"

        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>$platformName - $accountName</title>
            <style>
                :root {
                    --accent-color: $platformColor;
                    --bg-dark: #090D16;
                    --card-bg: #131B2E;
                    --card-border: #1E293B;
                    --text-main: #F8FAFC;
                    --text-muted: #94A3B8;
                }
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                }
                body {
                    background-color: var(--bg-dark);
                    color: var(--text-main);
                    padding: 16px;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                }
                .header-card {
                    background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
                    border: 1px solid var(--accent-color);
                    border-radius: 12px;
                    padding: 16px;
                    box-shadow: 0 4px 20px rgba(0,0,0,0.5);
                }
                .badge-row {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    margin-bottom: 8px;
                    flex-wrap: wrap;
                }
                .badge {
                    background-color: var(--accent-color);
                    color: #000000;
                    font-weight: 800;
                    font-size: 11px;
                    padding: 3px 8px;
                    border-radius: 6px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .badge-acc {
                    background-color: rgba(255,255,255,0.1);
                    color: #E2E8F0;
                    font-size: 11px;
                    padding: 3px 8px;
                    border-radius: 6px;
                    font-family: monospace;
                }
                .badge-profile {
                    background-color: rgba(0, 229, 255, 0.15);
                    border: 1px solid #00E5FF;
                    color: #00E5FF;
                    font-size: 10px;
                    font-weight: bold;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                }
                h1 {
                    font-size: 20px;
                    font-weight: 700;
                    color: #FFFFFF;
                }
                p.sub {
                    font-size: 12px;
                    color: var(--text-muted);
                    margin-top: 4px;
                }
                .panel {
                    background-color: var(--card-bg);
                    border: 1px solid var(--card-border);
                    border-radius: 12px;
                    padding: 14px;
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                }
                .panel h2 {
                    font-size: 13px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    color: var(--accent-color);
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .data-display {
                    background-color: #0A0F1D;
                    border: 1px solid #1E293B;
                    border-radius: 8px;
                    padding: 10px;
                    font-family: 'Courier New', monospace;
                    font-size: 12px;
                    color: #38BDF8;
                    white-space: pre-wrap;
                    word-break: break-all;
                    min-height: 44px;
                    max-height: 120px;
                    overflow-y: auto;
                }
                .input-row {
                    display: flex;
                    gap: 8px;
                }
                input {
                    flex: 1;
                    background-color: #0A0F1D;
                    border: 1px solid #334155;
                    border-radius: 6px;
                    color: #FFFFFF;
                    padding: 8px 12px;
                    font-size: 13px;
                }
                input:focus {
                    outline: none;
                    border-color: var(--accent-color);
                }
                button {
                    background-color: var(--accent-color);
                    color: #000000;
                    border: none;
                    border-radius: 6px;
                    font-weight: 700;
                    font-size: 12px;
                    padding: 8px 14px;
                    cursor: pointer;
                    transition: opacity 0.2s;
                }
                button:active {
                    opacity: 0.8;
                }
                button.secondary {
                    background-color: #334155;
                    color: #FFFFFF;
                }
                button.danger {
                    background-color: #EF4444;
                    color: #FFFFFF;
                }
                .btn-group {
                    display: flex;
                    gap: 8px;
                    flex-wrap: wrap;
                }
                .status-chip {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    font-size: 11px;
                    color: #10B981;
                    font-weight: 600;
                }
                .status-chip::before {
                    content: '';
                    width: 8px;
                    height: 8px;
                    background-color: #10B981;
                    border-radius: 50%;
                    box-shadow: 0 0 8px #10B981;
                }
                .auth-box {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    background-color: #0A0F1D;
                    padding: 10px;
                    border-radius: 8px;
                }
            </style>
        </head>
        <body>
            <div class="header-card">
                <div class="badge-row">
                    <span class="badge">$platformName</span>
                    <span class="badge-acc">$accountName</span>
                    <span class="badge-profile">$profileBadge</span>
                    <div style="flex:1"></div>
                    <span class="status-chip">Sessão Ativa</span>
                </div>
                <h1>Ambiente Isolado: $accountName</h1>
                <p class="sub">ID: $accountId</p>
                <p class="sub">Profile: <strong>$profileName</strong> | Destino: ${platform.defaultUrl}</p>
            </div>

            <!-- Autenticação Nativa -->
            <div class="panel">
                <h2>1. Sessão & Identidade da Conta</h2>
                <div class="auth-box">
                    <div>
                        <div style="font-size:11px; color:#94A3B8;">Usuário Logado</div>
                        <div id="authUsername" style="font-size:14px; font-weight:700; color:#F8FAFC;">(Não autenticado)</div>
                    </div>
                    <div>
                        <div style="font-size:11px; color:#94A3B8;">Token de Autenticação</div>
                        <div id="authToken" style="font-size:12px; font-family:monospace; color:#38BDF8;">Nenhum</div>
                    </div>
                </div>
                <div class="btn-group">
                    <button onclick="loginAs('${accountName.lowercase().replace(" ", "_")}')">Login como $accountName</button>
                    <button class="secondary" onclick="loginRandom()">Login Aleatório</button>
                    <button class="danger" onclick="logout()">Deslogar</button>
                </div>
            </div>

            <!-- LocalStorage Nativo do WebView -->
            <div class="panel">
                <h2>2. Local Storage Nativo (Nível WebView)</h2>
                <div id="localDisplay" class="data-display">Carregando...</div>
                <div class="input-row">
                    <input type="text" id="localKey" placeholder="Chave (ex: saldo, pref)">
                    <input type="text" id="localVal" placeholder="Valor">
                    <button onclick="setLocalItem()">Salvar</button>
                </div>
                <div class="btn-group">
                    <button class="secondary" onclick="refreshDisplay()">Atualizar</button>
                    <button class="danger" onclick="clearLocalStorage()">Limpar LocalStorage</button>
                </div>
            </div>

            <!-- SessionStorage Nativo do WebView -->
            <div class="panel">
                <h2>3. Session Storage Nativo (Nível WebView)</h2>
                <div id="sessionDisplay" class="data-display">Carregando...</div>
                <div class="input-row">
                    <input type="text" id="sessionKey" placeholder="Chave de sessão">
                    <input type="text" id="sessionVal" placeholder="Valor temporário">
                    <button onclick="setSessionItem()">Salvar</button>
                </div>
                <div class="btn-group">
                    <button class="secondary" onclick="refreshDisplay()">Atualizar</button>
                </div>
            </div>

            <!-- Cookies Nativos do WebView Profile -->
            <div class="panel">
                <h2>4. Cookies Nativos do WebView</h2>
                <div id="cookieDisplay" class="data-display">Carregando cookies...</div>
                <div class="input-row">
                    <input type="text" id="cookieName" placeholder="Nome do Cookie">
                    <input type="text" id="cookieValue" placeholder="Valor">
                    <button onclick="saveNativeCookie()">Gravar Cookie</button>
                </div>
                <div class="btn-group">
                    <button class="secondary" onclick="refreshCookies()">Atualizar Cookies</button>
                </div>
            </div>

            <script>
                function refreshDisplay() {
                    try {
                        // Real standard window.localStorage
                        const localObj = {};
                        for (let i = 0; i < window.localStorage.length; i++) {
                            const k = window.localStorage.key(i);
                            localObj[k] = window.localStorage.getItem(k);
                        }
                        document.getElementById('localDisplay').textContent = Object.keys(localObj).length ? JSON.stringify(localObj, null, 2) : "(vazio)";

                        // Real standard window.sessionStorage
                        const sessObj = {};
                        for (let i = 0; i < window.sessionStorage.length; i++) {
                            const k = window.sessionStorage.key(i);
                            sessObj[k] = window.sessionStorage.getItem(k);
                        }
                        document.getElementById('sessionDisplay').textContent = Object.keys(sessObj).length ? JSON.stringify(sessObj, null, 2) : "(vazio)";

                        // Check auth in localStorage
                        const user = window.localStorage.getItem('auth_user') || '(Não autenticado)';
                        const token = window.localStorage.getItem('auth_token') || 'Nenhum';
                        document.getElementById('authUsername').textContent = user;
                        document.getElementById('authToken').textContent = token;
                    } catch(e) {
                        document.getElementById('localDisplay').textContent = "Erro: " + e.message;
                    }
                    refreshCookies();
                }

                function setLocalItem() {
                    const k = document.getElementById('localKey').value.trim();
                    const v = document.getElementById('localVal').value.trim();
                    if (k) {
                        window.localStorage.setItem(k, v);
                        document.getElementById('localKey').value = '';
                        document.getElementById('localVal').value = '';
                        refreshDisplay();
                    }
                }

                function setSessionItem() {
                    const k = document.getElementById('sessionKey').value.trim();
                    const v = document.getElementById('sessionVal').value.trim();
                    if (k) {
                        window.sessionStorage.setItem(k, v);
                        document.getElementById('sessionKey').value = '';
                        document.getElementById('sessionVal').value = '';
                        refreshDisplay();
                    }
                }

                function clearLocalStorage() {
                    window.localStorage.clear();
                    refreshDisplay();
                }

                function loginAs(name) {
                    const token = "TOK_" + Math.random().toString(36).substring(2, 10).toUpperCase();
                    window.localStorage.setItem('auth_user', name);
                    window.localStorage.setItem('auth_token', token);
                    window.localStorage.setItem('auth_timestamp', Date.now());
                    document.cookie = "auth_session=" + token + "; path=/";
                    refreshDisplay();
                }

                function loginRandom() {
                    const rnd = "user_" + Math.floor(Math.random() * 9000 + 1000);
                    loginAs(rnd);
                }

                function logout() {
                    window.localStorage.removeItem('auth_user');
                    window.localStorage.removeItem('auth_token');
                    window.localStorage.removeItem('auth_timestamp');
                    document.cookie = "auth_session=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
                    refreshDisplay();
                }

                function saveNativeCookie() {
                    const n = document.getElementById('cookieName').value.trim();
                    const v = document.getElementById('cookieValue').value.trim();
                    if (n && v) {
                        document.cookie = n + "=" + v + "; path=/";
                        document.getElementById('cookieName').value = '';
                        document.getElementById('cookieValue').value = '';
                        refreshCookies();
                    }
                }

                function refreshCookies() {
                    try {
                        const c = document.cookie || "(Nenhum cookie visível via document.cookie)";
                        document.getElementById('cookieDisplay').textContent = c;
                    } catch(e) {
                        document.getElementById('cookieDisplay').textContent = "Erro cookies: " + e.message;
                    }
                }

                // Initial run
                setTimeout(refreshDisplay, 300);
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
