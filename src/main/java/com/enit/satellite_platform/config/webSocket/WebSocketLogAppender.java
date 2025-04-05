package com.enit.satellite_platform.config.webSocket;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.LogWebSocketHandler;

// Note: Standard Logback appenders are configured via XML and don't use Spring's @Component or @Autowired directly.
// We need a way to access the Spring-managed LogWebSocketHandler bean from this appender.
// A common approach is using a static holder or ApplicationContextAware.

@Component // Make this component discoverable by Spring
public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    // Static holder for the handler instance
    private static LogWebSocketHandler logWebSocketHandler;

    // Use @Autowired on a setter method for static field injection (recommended)
    @Autowired
    public void setLogWebSocketHandler(LogWebSocketHandler handler) {
        WebSocketLogAppender.logWebSocketHandler = handler;
    }

    private Encoder<ILoggingEvent> encoder;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (logWebSocketHandler != null && encoder != null && isStarted()) {
            try {
                byte[] encodedBytes = encoder.encode(eventObject);
                String logMessage = new String(encodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                logWebSocketHandler.broadcast(logMessage);
            } catch (Exception e) {
                addError("Error encoding or broadcasting log event: " + eventObject, e);
            }
        } else if (logWebSocketHandler == null) {
             addWarn("LogWebSocketHandler not initialized. Cannot broadcast log: " + eventObject.getFormattedMessage());
        } else if (encoder == null) {
             addWarn("Encoder not set for WebSocketLogAppender. Cannot format log: " + eventObject.getFormattedMessage());
        }
    }

    // Standard Logback appender methods
    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return; // Indicate failure
        }
        // Logback manages encoder initialization via XML configuration.
        // No need to call encoder.init() here.
        super.start();
    }

    @Override
    public void stop() {
        // Logback manages encoder closing via XML configuration.
        // No need to call encoder.close() here.
        super.stop();
    }

    // Getter and Setter for the encoder (required by Logback configuration)
    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
