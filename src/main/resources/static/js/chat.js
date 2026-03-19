document.addEventListener("DOMContentLoaded", () => {
    const app = document.getElementById("chat-app");
    if (!app) {
        return;
    }

    const conversationId = app.dataset.conversationId;
    const userEmail = app.dataset.userEmail;
    const form = document.getElementById("chatForm");
    const input = document.getElementById("chatInput");
    const messagesContainer = document.getElementById("messagesContainer");

    if (!conversationId || !form || !input || !messagesContainer) {
        return;
    }

    let stompClient = null;

    const socket = new SockJS("/ws-chat");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        stompClient.subscribe(`/topic/chat.${conversationId}`, (messageFrame) => {
            const payload = JSON.parse(messageFrame.body);
            appendMessage(payload, userEmail, messagesContainer);
        });
    });

    form.addEventListener("submit", (event) => {
        event.preventDefault();

        const contenido = input.value.trim();
        if (!contenido) {
            return;
        }

        if (!stompClient || !stompClient.connected) {
            return;
        }

        stompClient.send("/app/chat.send", {}, JSON.stringify({
            conversacionId: Number(conversationId),
            contenido
        }));

        input.value = "";
    });

    scrollToBottom(messagesContainer);
});

function appendMessage(payload, userEmail, container) {
    const wrap = document.createElement("div");
    wrap.className = "mb-2 flex";

    const ownMessage = payload.remitenteEmail === userEmail;
    wrap.classList.add(ownMessage ? "justify-end" : "justify-start");

    const bubble = document.createElement("div");
    bubble.className = "max-w-[75%] rounded-xl px-3 py-2";
    bubble.classList.add(ownMessage ? "bg-blue-600" : "bg-white");
    bubble.classList.add(ownMessage ? "text-white" : "text-slate-800", ...(!ownMessage ? ["border"] : []));

    const sender = document.createElement("p");
    sender.className = "text-xs opacity-80";
    sender.textContent = payload.remitenteNombre || "Usuario";

    const body = document.createElement("p");
    body.className = "text-sm";
    body.textContent = payload.contenido || "";

    const date = document.createElement("p");
    date.className = "mt-1 text-[10px] opacity-70";
    date.textContent = formatTimestamp(payload.fechaEnvio);

    bubble.appendChild(sender);
    bubble.appendChild(body);
    bubble.appendChild(date);
    wrap.appendChild(bubble);
    container.appendChild(wrap);

    scrollToBottom(container);
}

function formatTimestamp(value) {
    if (!value) {
        return "";
    }

    try {
        if (Array.isArray(value)) {
            const [year, month, day, hour, minute] = value;
            return `${String(day).padStart(2, "0")}/${String(month).padStart(2, "0")} ${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }

        const day = String(date.getDate()).padStart(2, "0");
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const hour = String(date.getHours()).padStart(2, "0");
        const minute = String(date.getMinutes()).padStart(2, "0");

        return `${day}/${month} ${hour}:${minute}`;
    } catch (e) {
        return "";
    }
}

function scrollToBottom(container) {
    container.scrollTop = container.scrollHeight;
}
