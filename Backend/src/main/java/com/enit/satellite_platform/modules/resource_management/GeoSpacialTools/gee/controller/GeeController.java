package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.service.GeeService;
import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.modules.resource_management.dto.ServiceRequest;

@RestController
@RequestMapping("/geospatial/gee")
@Tag(name = "GEE Controller", description = "Endpoints for interacting with Google Earth Engine")
public class GeeController {

        @Autowired
        private GeeService geeService;

        @PostMapping("/service")
        @Operation(summary = "Provide a service", description = "Provide a service based on the provided request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Analysis performed successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ProcessingResponse> performRequest(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Analysis request details", required = true) @Valid @org.springframework.web.bind.annotation.RequestBody ServiceRequest requestDto) {
                ProcessingResponse response = geeService.processGeeRequest(requestDto);
                return ResponseEntity.ok(response);
        }

}
