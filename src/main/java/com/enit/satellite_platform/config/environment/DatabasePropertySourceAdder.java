package com.enit.satellite_platform.config.environment;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.context.ApplicationListener;
    import org.springframework.context.event.ContextRefreshedEvent;
    import org.springframework.core.env.ConfigurableEnvironment;
    import org.springframework.core.env.MutablePropertySources;
    import org.springframework.stereotype.Component;

import com.enit.satellite_platform.modules.user_management.admin_privileges.repository.ConfigPropertyRepository;

    /**
     * Application listener that adds the DatabasePropertySource to the Spring Environment
     * after the application context has been refreshed, ensuring the repository bean is available.
     */
    @Component
    public class DatabasePropertySourceAdder implements ApplicationListener<ContextRefreshedEvent> {

        private static final Logger log = LoggerFactory.getLogger(DatabasePropertySourceAdder.class);

        @Autowired
        private ConfigurableEnvironment environment;

        @Autowired
        private ConfigPropertyRepository configPropertyRepository;

        private boolean initialized = false; // Ensure it runs only once

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            // Prevent running multiple times (e.g., in web apps with parent/child contexts)
            // Check if the event's context is the root context.
            if (initialized || event.getApplicationContext().getParent() != null) {
                 // log.debug("DatabasePropertySourceAdder already initialized or event is not for root context.");
                return;
            }

            synchronized (this) {
                if (initialized) {
                    return;
                }

                log.info("ContextRefreshedEvent received. Adding DatabasePropertySource to environment.");
                try {
                    MutablePropertySources propertySources = environment.getPropertySources();
                    DatabasePropertySource dbPropertySource = new DatabasePropertySource(configPropertyRepository);

                    // Initialize the source (load values from DB)
                    dbPropertySource.initialize();

                    // Add it with high precedence (e.g., after systemProperties but before most others)
                    // Add 'first' means it has the highest precedence among custom sources.
                    propertySources.addFirst(dbPropertySource);

                    log.info("Successfully added DatabasePropertySource to environment.");
                    initialized = true;

                } catch (Exception e) {
                    log.error("Failed to add DatabasePropertySource to environment.", e);
                    // Decide if this should prevent startup or just log the error.
                    // For now, we log and continue.
                }
            }
        }
    }
