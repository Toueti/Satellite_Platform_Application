package com.enit.satellite_platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogWebSocketHandler.class);
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    /**
     * Called after a WebSocket connection is established. Adds the session to the list of active sessions and sends
     * a confirmation message to the client. This is a good time to send any initial data or configuration to the client.
     * @param session the newly established WebSocket session
     * @throws Exception if there's an error during the establishment of the connection
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket connection established: {}", session.getId());
        //*send connection confirmation or initial data
        session.sendMessage(new TextMessage("Connected to log stream"));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //TODO Handle incoming messages if needed, e.g., filtering logs
        logger.debug("Received message from {}: {}", session.getId(), message.getPayload());
        // !For now, we primarily broadcast, not receive commands via WebSocket
    }

    /**
     * Called after a WebSocket connection is closed. Removes the session from the list of active sessions.
     * @param session the closed WebSocket session
     * @param status the close status of the connection
     * @throws Exception if there's an error during the closing of the connection
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} - Status: {}", session.getId(), status);
    }

    /**
     * Handles WebSocket transport errors. If the session is still open, it will be closed with a server error status.
     * The session is then removed from the list of active sessions.
     * @param session the session with the transport error
     * @param exception the error that occurred during the transport
     * @throws Exception if there's an error during the handling of the transport error
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(session);
    }

    /**
     * Broadcasts a message to all active WebSocket sessions. Iterates through each session and sends the provided
     * message as a TextMessage, ensuring that the session is open before sending. If an IOException occurs during
     * the send operation, logs the error and removes the session from the list. If the session is open, it is
     * closed after an error occurs.
     * 
     * @param message the message to broadcast to all active WebSocket sessions
     */

    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                logger.error("Error broadcasting message to session {}: {}", session.getId(), e.getMessage());
                // Consider removing session if sending fails repeatedly
                sessions.remove(session);
                try {
                    session.close();
                } catch (IOException closeEx) {
                    // *Ignore errors during close
                }
            }
        }
    }
}
