package com.township.controller;

import com.township.dto.HealthResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    @DisplayName("Health endpoint returns status and database info when DataSource is absent")
    void testHealthEndpointWithoutDataSource() {
        HealthController controller = new HealthController(Optional.empty());
        ResponseEntity<HealthResponseDto> response = controller.getHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEGRADED", response.getBody().status());
        assertEquals("PostgreSQL", response.getBody().database().get("type"));
        assertEquals("NOT_CONFIGURED", response.getBody().database().get("status"));
        assertNotNull(response.getBody().timestamp());
        assertNotNull(response.getBody().system());
    }

    @Test
    @DisplayName("Health endpoint returns UP and CONNECTED when DataSource connection succeeds")
    void testHealthEndpointWithConnectedDataSource() throws Exception {
        DataSource mockDataSource = mock(DataSource.class);
        Connection mockConnection = mock(Connection.class);
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(mockMetaData.getDatabaseProductVersion()).thenReturn("16.15");

        HealthController controller = new HealthController(Optional.of(mockDataSource));
        ResponseEntity<HealthResponseDto> response = controller.getHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().status());
        assertEquals("PostgreSQL", response.getBody().database().get("type"));
        assertEquals("CONNECTED", response.getBody().database().get("status"));
        assertEquals("16.15", response.getBody().database().get("productVersion"));
    }
}
