package com.example.ui.components

import com.example.domain.model.Account
import com.example.domain.model.Platform

object SandboxHtmlGenerator {

    fun generateHtml(platform: Platform, account: Account): String {
        val platformColor = platform.accentColorHex
        val platformName = platform.name
        val accountName = account.name
        val accountId = account.id

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
                    font-size: 14px;
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
                    min-height: 48px;
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
                    font-size: 12px;
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
                    <div style="flex:1"></div>
                    <span class="status-chip">Sessão Ativa</span>
                </div>
                <h1>Ambiente Isolado: $accountName</h1>
                <p class="sub">ID: $accountId</p>
                <p class="sub" style="margin-top:2px;">Plataforma Oficial: <strong>${platform.defaultUrl}</strong></p>
            </div>

            <!-- Autenticação Simulada -->
            <div class="panel">
                <h2>1. Sessão & Autenticação da Conta</h2>
                <div class="auth-box">
                    <div>
                        <div style="font-size:11px; color:#94A3B8;">Usuário Logado</div>
                        <div id="authUsername" style="font-size:14px; font-weight:700; color:#F8FAFC;">(Não autenticado)</div>
                    </div>
                    <div>
                        <div style="font-size:11px; color:#94A3B8;">Token de Sessão</div>
                        <div id="authToken" style="font-size:12px; font-family:monospace; color:#38BDF8;">Nenhum</div>
                    </div>
                </div>
                <div class="btn-group">
                    <button onclick="loginAs('${accountName.lowercase().replace(" ", "_")}')">Login como $accountName</button>
                    <button class="secondary" onclick="loginRandom()">Login Aleatório</button>
                    <button class="danger" onclick="logout()">Deslogar</button>
                </div>
            </div>

            <!-- LocalStorage -->
            <div class="panel">
                <h2>2. Local Storage (Persistente)</h2>
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

            <!-- Cookies -->
            <div class="panel">
                <h2>3. Cookies da Sessão</h2>
                <div id="cookieDisplay" class="data-display">Carregando cookies...</div>
                <div class="input-row">
                    <input type="text" id="cookieName" placeholder="Nome do Cookie">
                    <input type="text" id="cookieValue" placeholder="Valor">
                    <button onclick="saveCustomCookie()">Gravar Cookie</button>
                </div>
                <div class="btn-group">
                    <button class="secondary" onclick="refreshCookies()">Atualizar Cookies</button>
                </div>
            </div>

            <script>
                function refreshDisplay() {
                    try {
                        const local = window.localStorage;
                        let text = "";
                        if (local.getAll) {
                            const all = local.getAll();
                            text = JSON.stringify(all, null, 2);
                        } else {
                            const obj = {};
                            for (let i = 0; i < local.length; i++) {
                                const k = local.key(i);
                                obj[k] = local.getItem(k);
                            }
                            text = JSON.stringify(obj, null, 2);
                        }
                        document.getElementById('localDisplay').textContent = text || "(vazio)";

                        // Check auth
                        const user = local.getItem('auth_user') || '(Não autenticado)';
                        const token = local.getItem('auth_token') || 'Nenhum';
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

                function clearLocalStorage() {
                    window.localStorage.clear();
                    refreshDisplay();
                }

                function loginAs(name) {
                    const token = "TOK_" + Math.random().toString(36).substring(2, 10).toUpperCase();
                    window.localStorage.setItem('auth_user', name);
                    window.localStorage.setItem('auth_token', token);
                    window.localStorage.setItem('auth_timestamp', Date.now());
                    if (window.NaveHub && window.NaveHub.setCookie) {
                        window.NaveHub.setCookie("${platform.defaultUrl}", "auth_session", token);
                    }
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
                    refreshDisplay();
                }

                function saveCustomCookie() {
                    const n = document.getElementById('cookieName').value.trim();
                    const v = document.getElementById('cookieValue').value.trim();
                    if (n && v) {
                        if (window.NaveHub && window.NaveHub.setCookie) {
                            window.NaveHub.setCookie("${platform.defaultUrl}", n, v);
                        } else {
                            document.cookie = n + "=" + v + "; path=/";
                        }
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
