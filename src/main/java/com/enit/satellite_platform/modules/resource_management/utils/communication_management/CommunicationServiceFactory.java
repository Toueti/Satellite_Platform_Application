package com.enit.satellite_platform.modules.resource_management.utils.communication_management;

import java.util.Map;


import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.GrpcCommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageManager; // Added
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.RabbitMqCommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.RestApiCommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.serialization.SerializerFactory;

@Component
public class CommunicationServiceFactory {

    private final SerializerFactory serializerFactory;
    private final GrpcCommunicationService grpcCommunicationService;
    private final WebClient webClient;
    private final StorageManager storageManager; // Added

    public CommunicationServiceFactory(
            GrpcCommunicationService grpcCommunicationService,
            SerializerFactory serializerFactory,
            WebClient webClient,
            StorageManager storageManager) { // Inject StorageManager
        this.grpcCommunicationService = grpcCommunicationService;
        this.serializerFactory = serializerFactory;
        this.webClient = webClient;
        this.storageManager = storageManager; // Assign injected manager
    }

    /**
     * Create a communication service with support for generic types
     * 
     * @param <T>        Input type
     * @param <R>        Output type
     * @param technology The communication technology to use
     * @param config     Configuration parameters
     * @return The communication service instance
     */
    @SuppressWarnings("unchecked")
    public <T, R> CommunicationService<T, R> createService(
            CommunicationTechnology technology, Map<String, String> config) {
        
        String endpoint = config.get("rest.endpoint");
        if (technology == CommunicationTechnology.REST_API && (endpoint == null || endpoint.trim().isEmpty())) {
            throw new IllegalArgumentException("Endpoint URL is required for REST API communication");
        }

        switch (technology) {
            case GRPC:
                // Assuming GrpcCommunicationService doesn't need StorageManager directly for this pattern
                return (CommunicationService<T, R>) grpcCommunicationService;
            case REST_API:
                // Pass StorageManager to RestApiCommunicationService constructor
                return (CommunicationService<T, R>) new RestApiCommunicationService<>(endpoint, serializerFactory, webClient, storageManager);
            case RABBITMQ:
                 // Assuming RabbitMqCommunicationService doesn't need StorageManager directly
                return new RabbitMqCommunicationService<>(config, serializerFactory);
            default:
                throw new IllegalArgumentException("Unsupported communication technology: " + technology);
        }
    }
}
