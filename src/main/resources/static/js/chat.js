
document.addEventListener("DOMContentLoaded", function() {
    const sendButton = document.getElementById("send-button");
    const chatInput = document.getElementById("chat-input");
    const messagesContainer = document.getElementById("messages");
    const chatListContainer = document.getElementById("chat-list");
    const inputArea = document.getElementById("input-area");
    const welcomeMessage = document.getElementById("welcome-message");

    let currentChatId = null;
    let allChats = [];
    
    // Get CSRF token and header from meta tags
    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    // Initialize the application
    init();

    async function init() {
        // Get chat ID from URL if present
        const pathParts = window.location.pathname.split("/").filter(part => part);
        if (pathParts.length >= 2 && pathParts[0] === "chat") {
            currentChatId = parseInt(pathParts[1]);
        }

        try {
            // Load all chats
            await loadAllChats();

            // Load current chat if ID is present
            if (currentChatId) {
                await loadChat(currentChatId);
                showChatInterface();
            } else {
                showWelcomeMessage();
            }
        } catch (error) {
            console.error('Error initializing app:', error);
            showWelcomeMessage();
        }
    }

    // Load all chats and render sidebar
    async function loadAllChats() {
        try {
            allChats = await getAllChats();
            renderChatList();
        } catch (error) {
            console.error('Error loading chats:', error);
            chatListContainer.innerHTML = '<li>Error loading chats</li>';
        }
    }

    // Load specific chat
    async function loadChat(chatId) {
        try {
            const chat = await getChat(chatId);
            renderMessages(chat.history || []);
            updatePageTitle(chat.title);
            currentChatId = chatId;

            // Update URL without reloading page
            window.history.pushState({chatId: chatId}, '', `/chat/${chatId}`);

            return chat;
        } catch (error) {
            console.error('Error loading chat:', error);
            throw error;
        }
    }

    // Render chat list in sidebar
    function renderChatList() {
        if (!allChats || allChats.length === 0) {
            chatListContainer.innerHTML = '<li style="text-align: center; color: #999; font-style: italic;">No chats yet</li>';
            return;
        }

        chatListContainer.innerHTML = allChats.map(chat => `
            <li class="${currentChatId === chat.id ? 'active' : ''}" data-chat-id="${chat.id}">
                <a href="/chat/${chat.id}" data-chat-id="${chat.id}">
                    <span title="${escapeHtml(chat.title)}">${escapeHtml(chat.title)}</span>
                </a>
                <div class="delete-chat-form">
                    <button type="button" class="delete-chat-btn" data-chat-id="${chat.id}">✕</button>
                </div>
            </li>
        `).join('');
    }

    // Render messages in chat area
    function renderMessages(messages) {
        // Clear the "No messages yet" placeholder if present
        const placeholder = messagesContainer.querySelector('[style*="No messages yet"]');
        if (placeholder) {
            messagesContainer.innerHTML = '';
        }

        if (!messages || messages.length === 0) {
            messagesContainer.innerHTML = '<div style="text-align: center; color: #999; font-style: italic; margin-top: 50px;">No messages yet. Start the conversation!</div>';
            return;
        }

        messagesContainer.innerHTML = messages.map(entry => {
            // Fix: role is a string, not an object
            const isUser = entry.role === 'USER';
            const avatarSrc = isUser ? '/images/user.png' : '/images/mentor.png';
            const messageClass = isUser ? 'message user' : 'message mentor';
            const processedContent = isUser ?
                escapeHtml(entry.content).replace(/\n/g, '<br>') :
                (typeof marked !== 'undefined' ? marked.parse(entry.content) : escapeHtml(entry.content).replace(/\n/g, '<br>'));

            return `
            <div class="${messageClass}">
                <img src="${avatarSrc}" alt="avatar">
                <div class="bubble">${processedContent}</div>
            </div>
        `;
        }).join('');

        scrollToBottom();
    }

    // Add a single message to the UI
    function addMessageToUI(role, content) {
        // Clear the "No messages yet" placeholder if present
        const placeholder = messagesContainer.querySelector('[style*="No messages yet"]');
        if (placeholder) {
            placeholder.remove();
        }

        const isUser = role === 'user';
        const avatarSrc = isUser ? '/images/user.png' : '/images/mentor.png';
        const messageClass = isUser ? 'message user' : 'message mentor';
        const processedContent = isUser ?
            escapeHtml(content).replace(/\n/g, '<br>') :
            (typeof marked !== 'undefined' ? marked.parse(content) : escapeHtml(content).replace(/\n/g, '<br>'));

        const messageDiv = document.createElement('div');
        messageDiv.className = messageClass;
        messageDiv.innerHTML = `
            <img src="${avatarSrc}" alt="avatar">
            <div class="bubble">${processedContent}</div>
        `;

        messagesContainer.appendChild(messageDiv);
        scrollToBottom();
    }

    // Show chat interface
    function showChatInterface() {
        inputArea.style.display = 'block';
        welcomeMessage.style.display = 'none';
    }

    // Show welcome message
    function showWelcomeMessage() {
        inputArea.style.display = 'none';
        welcomeMessage.style.display = 'flex';
        messagesContainer.innerHTML = '';
        updatePageTitle();
        currentChatId = null;

        // Update URL to home
        window.history.pushState({}, '', '/');

        // Update active chat in sidebar
        renderChatList();
    }

    // Update page title
    function updatePageTitle(chatTitle) {
        document.title = chatTitle ? `Library Chatbot — ${chatTitle}` : 'Library Chatbot';
    }

    // Handle send button click
    sendButton?.addEventListener("click", function() {
        if (!currentChatId) return;

        const prompt = chatInput.value.trim();
        if (!prompt) return;

        // Clear input and disable send button
        chatInput.value = "";
        sendButton.disabled = true;

        // Send message to ChatController
        sendMessage(prompt);
    });

    // Handle Enter key in textarea
    chatInput?.addEventListener("keydown", function(event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            sendButton.click();
        }
    });

// Send message to ChatController
    async function sendMessage(prompt) {
        if (!currentChatId) return;

        console.log('Sending message:', prompt, 'to chat:', currentChatId);

        // Add user message immediately (optimistic UI)
        addMessageToUI('user', prompt);

        // Show loading indicator
        showTypingIndicator();

        const requestData = {
            content: prompt
        };

        try {
            // Open SSE stream to receive assistant response
            const streamUrl = `/chat/${currentChatId}/stream?q=${encodeURIComponent(prompt)}`;
            console.log('Opening SSE stream:', streamUrl);

            // We'll show typing indicator until the first chunk arrives
            let assistantDiv = null;
            let contentSpan = null;
            let accumulated = '';

            const es = new EventSource(streamUrl, { withCredentials: true });

            es.onmessage = (event) => {
                // On first chunk, replace typing indicator with assistant message container
                if (!assistantDiv) {
                    hideTypingIndicator();
                    assistantDiv = document.createElement('div');
                    assistantDiv.className = 'message mentor';
                    assistantDiv.innerHTML = `
                        <img src="/images/mentor.png" alt="avatar">
                        <div class="bubble"><span id="assistant-stream-content"></span></div>
                    `;
                    messagesContainer.appendChild(assistantDiv);
                    contentSpan = assistantDiv.querySelector('#assistant-stream-content');
                }

                // Default message event carries token chunk (URL-encoded)
                let chunk = event.data || '';

                // Decode the URL-encoded chunk to restore spaces
                try {
                    chunk = decodeURIComponent(chunk);
                } catch (e) {
                    console.error('Error decoding chunk:', e);
                    // If decoding fails, use the chunk as-is
                }
                accumulated += chunk;
                // During streaming, render plain text to preserve whitespace; finalize with markdown on complete
                if (contentSpan) {
                    contentSpan.textContent = accumulated;
                }
                scrollToBottom();
            };

            es.addEventListener('complete', () => {
                console.log('SSE complete');
                es.close();
                // On completion, render final markdown if available
                if (assistantDiv) {
                    const bubble = assistantDiv.querySelector('.bubble');
                    if (typeof marked !== 'undefined') {
                        bubble.innerHTML = marked.parse(accumulated);
                    } else {
                        const span = assistantDiv.querySelector('#assistant-stream-content');
                        if (span) span.textContent = accumulated;
                    }
                }
                hideTypingIndicator();
                // Refresh chat list (e.g., title updates)
                loadAllChats().catch(() => {});
                sendButton.disabled = false;
            });

            es.addEventListener('error', (e) => {
                console.error('SSE stream error:', e);
                es.close();
                hideTypingIndicator();
                sendButton.disabled = false;
            });

        } catch (error) {
            hideTypingIndicator();
            console.error('Error sending message:', error);
            // Add error message to UI
            addMessageToUI('assistant', 'Sorry, there was an error processing your message. Please try again.');
            sendButton.disabled = false;
        }
    }


    // Show typing indicator
    function showTypingIndicator() {
        hideTypingIndicator(); // Remove any existing indicator

        const typingDiv = document.createElement("div");
        typingDiv.className = "message mentor typing-indicator";
        typingDiv.id = "typing-indicator";
        typingDiv.innerHTML = `<img src="/images/mentor.png" alt="Mentor"><div class="bubble typing-dots"><span></span><span></span><span></span></div>`;
        messagesContainer.appendChild(typingDiv);
        scrollToBottom();
    }

    // Hide typing indicator
    function hideTypingIndicator() {
        const typingIndicator = document.getElementById("typing-indicator");
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    // Scroll to bottom of messages
    function scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Create new chat
    async function createNewChat(title) {
        const requestData = {
            title: title
        };

        const response = await fetch('/chat/new', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return response.json();
    }

    // Delete chat
    async function deleteChat(chatId) {
        const response = await fetch(`/chat/${chatId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
    }

    // Get all chats
    async function getAllChats() {
        const response = await fetch('/chats', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Cache-Control': 'no-store'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return response.json();
    }

    // Get specific chat
    async function getChat(chatId) {
        const response = await fetch(`/chat/${chatId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Cache-Control': 'no-store'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return response.json();
    }

    // Event delegation for chat list clicks
    chatListContainer?.addEventListener("click", function(event) {
        const chatLink = event.target.closest("a[data-chat-id]");
        const deleteBtn = event.target.closest(".delete-chat-btn");

        if (chatLink) {
            event.preventDefault();
            const chatId = parseInt(chatLink.dataset.chatId);
            loadChat(chatId).then(() => {
                showChatInterface();
                renderChatList(); // Update active chat highlighting
            });
        } else if (deleteBtn) {
            event.preventDefault();
            const chatId = parseInt(deleteBtn.dataset.chatId);
            if (confirm("Are you sure you want to delete this chat?")) {
                deleteChat(chatId)
                    .then(() => loadAllChats())
                    .then(() => {
                        if (currentChatId === chatId) {
                            showWelcomeMessage();
                        }
                    })
                    .catch(error => {
                        console.error('Error deleting chat:', error);
                        alert('Failed to delete chat. Please try again.');
                    });
            }
        }
    });

    // New chat button
    const newChatBtn = document.getElementById("new-chat-btn");
    const chatTitleInput = document.getElementById("chat-title-input");

    newChatBtn?.addEventListener("click", function() {
        const title = chatTitleInput.value.trim() || "New chat";

        createNewChat(title)
            .then(chat => {
                chatTitleInput.value = "";
                return loadAllChats().then(() => chat);
            })
            .then(chat => {
                return loadChat(chat.id);
            })
            .then(() => {
                showChatInterface();
                renderChatList(); // Update active chat highlighting
            })
            .catch(error => {
                console.error('Error creating new chat:', error);
                alert('Failed to create new chat. Please try again.');
            });
    });

    // Handle Enter key in new chat input
    chatTitleInput?.addEventListener("keydown", function(event) {
        if (event.key === "Enter") {
            event.preventDefault();
            newChatBtn.click();
        }
    });

    // Utility function to escape HTML
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Handle browser back/forward
    window.addEventListener('popstate', function(event) {
        const pathParts = window.location.pathname.split("/").filter(part => part);
        if (pathParts.length >= 2 && pathParts[0] === "chat") {
            const chatId = parseInt(pathParts[1]);
            loadChat(chatId).then(() => {
                showChatInterface();
                renderChatList(); // Update active chat highlighting
            });
        } else {
            showWelcomeMessage();
        }
    });
});