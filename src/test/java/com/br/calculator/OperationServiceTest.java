package com.br.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.entities.User;
import com.br.calculator.enums.OperationTypeEnum;
import com.br.calculator.repositories.OperationRepository;
import com.br.calculator.repositories.RecordRepository;
import com.br.calculator.services.OperationService;
import com.br.calculator.services.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@TestPropertySource("classpath:test-application.properties")
@SpringBootTest
public class OperationServiceTest {

    @InjectMocks
    private OperationService operationService;
    @Mock
    private RecordService recordService;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private RecordRepository recordRepository;
    @Mock
    private LambdaClient lambdaClient;

    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        operationService.setLambdaFunction(System.getenv("AWS_LAMBDA_FUNCTION"));
    }

    @Test
    public void testExecuteOperationAddition() {
        OperationRequest request = new OperationRequest("ADDITION", 5.0, 3.0);
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals(200 - 1, response.getAmount());
        assertEquals("\"Result: 8.0\"", response.getOperationResult());
    }

    @Test
    public void testExecuteOperationSubtraction() {
        OperationRequest request = new OperationRequest("SUBTRACTION", 10.0, 4.0);
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals(200 - 2, response.getAmount()); // Previous record amount - operation cost for SUBTRACTION
        assertEquals("\"Result: 6.0\"", response.getOperationResult());
    }

    @Test
    public void testExecuteOperationMultiplication() {
        OperationRequest request = new OperationRequest(OperationTypeEnum.MULTIPLICATION.name(), 10.0, 4.0);
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals(200 - 3, response.getAmount()); // Previous record amount - operation cost for SUBTRACTION
        assertEquals("\"Result: 40.0\"", response.getOperationResult());
    }

    @Test
    public void testExecuteOperationDivision() {
        OperationRequest request = new OperationRequest(OperationTypeEnum.DIVISION.name(), 10.0, 2.0);
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals(200 - 4, response.getAmount());
        assertEquals("\"Result: 5.0\"", response.getOperationResult());
    }

    @Test
    public void testExecuteOperationSquareRoot() {
        OperationRequest request = new OperationRequest(OperationTypeEnum.SQUARE_ROOT.name(), 144.0);
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals(200 - 5, response.getAmount());
        assertEquals("\"Result: 12.0\"", response.getOperationResult());
    }

    @Test
    public void testInvokeLambdaError() {
        operationService.setLambdaFunction("testFail");
        OperationRequest request = new OperationRequest("ADDITION", 5.0, 3.0);
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenThrow(new RuntimeException("Lambda invocation failed"));
        OperationResponse response = operationService.executeOperation(request, user);
        assertNotNull(response);
        assertEquals("Error: Could not invoke Lambda function.", response.getOperationResult());
    }

}