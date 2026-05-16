/**
 * chat-widget.js — ETA Assistant Chatbot Widget
 * Maneja la lógica del widget de chat IA en el frontend.
 * Vanilla JS sin dependencias externas.
 */
(function () {
    'use strict';

    // ── Referencias al DOM ─────────────────────────────────────────────────────
    const widget      = document.getElementById('eta-chat-widget');
    const panel       = document.getElementById('chat-panel');
    const toggleBtn   = document.getElementById('chat-toggle-btn');
    const closeBtn    = document.getElementById('chat-close-btn');
    const messagesEl  = document.getElementById('chat-messages');
    const inputEl     = document.getElementById('chat-input');
    const sendBtn     = document.getElementById('chat-send-btn');
    const iconOpen    = document.getElementById('chat-icon-open');
    const iconClose   = document.getElementById('chat-icon-close');

    if (!widget) return; // Widget no presente en esta página

    // ── Estado ─────────────────────────────────────────────────────────────────
    const actividadId = widget.dataset.actividadId
        ? parseInt(widget.dataset.actividadId, 10)
        : null;

    /** Historial de la conversación en memoria (solo mientras la página esté abierta). */
    let conversationHistory = [];
    let isWaiting = false;

    // ── Toggle apertura/cierre ─────────────────────────────────────────────────
    function openPanel() {
        panel.style.display = 'flex';
        iconOpen.style.display = 'none';
        iconClose.style.display = 'block';
        toggleBtn.setAttribute('aria-expanded', 'true');
        inputEl.focus();
        scrollToBottom();
    }

    function closePanel() {
        panel.style.display = 'none';
        iconOpen.style.display = 'block';
        iconClose.style.display = 'none';
        toggleBtn.setAttribute('aria-expanded', 'false');
    }

    toggleBtn.addEventListener('click', function () {
        if (panel.style.display === 'none' || panel.style.display === '') {
            openPanel();
        } else {
            closePanel();
        }
    });

    closeBtn.addEventListener('click', closePanel);

    // ── Enviar mensaje ─────────────────────────────────────────────────────────
    sendBtn.addEventListener('click', handleSend);
    inputEl.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    });

    function handleSend() {
        const texto = inputEl.value.trim();
        if (!texto || isWaiting) return;

        inputEl.value = '';
        sendMessage(texto);
    }

    // ── Lógica principal de envío ──────────────────────────────────────────────
    async function sendMessage(texto) {
        isWaiting = true;
        sendBtn.disabled = true;
        inputEl.disabled = true;

        appendMessage('user', texto);
        showTypingIndicator();

        const payload = {
            mensaje: texto,
            historial: conversationHistory.slice(-20), // máx 10 turnos (20 mensajes)
            contextoActividad: actividadId
        };

        try {
            // Usar endpoint de recomendaciones que devuelve JSON
            const res = await fetch('/chat/recomendar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            removeTypingIndicator();

            if (data.respuesta) {
                // Mostrar respuesta del bot
                const tieneRecomendacion = data.tieneRecomendacion && data.filtros;
                
                if (tieneRecomendacion) {
                    // Respuesta con botón de recomendación
                    appendMessageWithRecommendation(data.respuesta, data.filtros);
                } else {
                    // Respuesta normal sin recomendación
                    appendMessage('bot', data.respuesta);
                }

                // Guardar en historial
                conversationHistory.push(
                    { rol: 'user',      contenido: texto          },
                    { rol: 'assistant', contenido: data.respuesta }
                );
            } else {
                appendMessage('bot-error', 'Respuesta inesperada del servidor.');
            }

        } catch (e) {
            removeTypingIndicator();
            appendMessage('bot-error', 'Sin conexión con el asistente. Intenta de nuevo.');
        } finally {
            isWaiting = false;
            sendBtn.disabled = false;
            inputEl.disabled = false;
            inputEl.focus();
        }
    }

    // ── Renderizado de mensajes ────────────────────────────────────────────────

    /**
     * Agrega una burbuja de mensaje al área de chat.
     * @param {'user'|'bot'|'bot-error'} tipo
     * @param {string} contenido
     */
    function appendMessage(tipo, contenido) {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-msg ' + tipo;

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble';

        // SEGURIDAD: usar textContent para evitar XSS (nunca innerHTML con datos del servidor)
        bubble.textContent = contenido;

        wrapper.appendChild(bubble);
        messagesEl.appendChild(wrapper);
        scrollToBottom();
    }

    /**
     * Agrega un mensaje con botón de recomendación.
     * @param {string} respuesta - Texto de la respuesta del bot
     * @param {object} filtros - Objeto con filtros recomendados
     */
    function appendMessageWithRecommendation(respuesta, filtros) {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-msg bot';

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble';
        bubble.textContent = respuesta;

        // Crear botón de acción
        const button = document.createElement('button');
        button.className = 'chat-recommend-btn';
        button.textContent = filtros.textoBoton || 'Ver actividades';
        button.onclick = function() {
            redirigirConFiltros(filtros);
        };

        wrapper.appendChild(bubble);
        wrapper.appendChild(button);
        messagesEl.appendChild(wrapper);
        scrollToBottom();
    }

    /**
     * Construye la URL de /actividades/buscar con los filtros y redirige.
     * @param {object} filtros - Objeto con filtros (nombre, categoriaId, idiomaId, precioMin, precioMax)
     */
    function redirigirConFiltros(filtros) {
        const params = new URLSearchParams();

        if (filtros.nombre) {
            params.append('nombre', filtros.nombre);
        }
        if (filtros.categoriaId) {
            params.append('categoriaId', filtros.categoriaId);
        }
        if (filtros.idiomaId) {
            params.append('idiomaId', filtros.idiomaId);
        }
        if (filtros.precioMin) {
            params.append('precioMin', filtros.precioMin);
        }
        if (filtros.precioMax) {
            params.append('precioMax', filtros.precioMax);
        }

        window.location.href = `/actividades/buscar?${params.toString()}`;
    }

    // ── Indicador de "escribiendo..." ─────────────────────────────────────────
    function showTypingIndicator() {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-msg bot';
        wrapper.id = 'chat-typing-indicator';

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble typing-bubble';
        bubble.innerHTML =
            '<span class="typing-dot"></span>' +
            '<span class="typing-dot"></span>' +
            '<span class="typing-dot"></span>';

        wrapper.appendChild(bubble);
        messagesEl.appendChild(wrapper);
        scrollToBottom();
    }

    function removeTypingIndicator() {
        const indicator = document.getElementById('chat-typing-indicator');
        if (indicator) indicator.remove();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────
    function scrollToBottom() {
        requestAnimationFrame(function () {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        });
    }

})();
