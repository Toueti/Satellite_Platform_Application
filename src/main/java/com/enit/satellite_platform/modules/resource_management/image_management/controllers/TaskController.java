package com.enit.satellite_platform.modules.resource_management.image_management.controllers;

// import com.enit.satellite_platform.modules.resources_management.services.GeeService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/tasks")
@Tag(name = "Task Controller", description = "Endpoints for managing asynchronous GEE tasks")

public class TaskController {
    //@Autowired
    //private GeeService geeService;
}
