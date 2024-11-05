package com.br.calculator.services;

import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.Operation;
import com.br.calculator.entities.Record;
import com.br.calculator.entities.User;
import com.br.calculator.enums.OperationTypeEnum;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.repositories.OperationRepository;
import com.br.calculator.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
import software.amazon.awssdk.services.lambda.model.ServiceException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OperationService {

    private static final Logger logger = LoggerFactory.getLogger(OperationService.class);

    @Value("${aws.lambda.function}")
    private String LAMBDA_FUNCTION;

    private final OperationRepository operationRepository;
    private final RecordRepository recordRepository;
    private final LambdaClient lambdaClient;

    public OperationService(OperationRepository operationRepository, RecordRepository recordRepository) {
        this.lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        this.operationRepository = operationRepository;
        this.recordRepository = recordRepository;
    }

    @Cacheable("userOperations")
    public Page<RecordResponse> getUserRecords(User user, Pageable pageable) {
        var recordsPage = recordRepository.findAllByUserAndActive(user, true, pageable);
        logger.info("Data fetched. Mapping response data");
        return recordsPage.map(this::getRecordResponse);
    }

    public int getNewAmount(List<Record> records) {
        int totalCost = 0;
        if (!records.isEmpty()) {
            totalCost = records.stream().map(Record::getOperation).mapToInt(Operation::getCost).sum();
        }
        return 200 - totalCost;
    }

    public UserStatsResponse getUserStats(User user) {
        logger.info("Fetching user stats for user: " + user.getUsername());
        List<Record> records = recordRepository.findAllByUserAndActive(user, true).orElse(new ArrayList<>());
        UserStatsResponse userStatsResponse = new UserStatsResponse();
        userStatsResponse.setTotalOperations((long) records.size());
        userStatsResponse.setCurrentBalance(getNewAmount(records));
        return userStatsResponse;
    }

    private RecordResponse getRecordResponse(Record record) {
        var response = new RecordResponse();
        response.setId(record.getId());
        response.setOperationId(record.getId());
        response.setDate(record.getDate());
        response.setOperationCost(record.getOperation().getCost());
        response.setOperationType(record.getOperation().getType().name());
        response.setUserBalance(record.getAmount());
        return response;
    }

    public OperationResponse executeOperation(OperationRequest operationRequest, User user) {
        var operationType = OperationTypeEnum.fromString(operationRequest.getOperationType());
        Integer operationCost = getOperationCost(operationType);
        UserStatsResponse userStats = getUserStats(user);
        validateRequestExecutioon(operationRequest, userStats, operationCost);
        logger.info("Invoking lambda function");
        var result = invokeLambda(operationType, operationRequest.getValue1(), operationRequest.getValue2());
        logger.info("Saving operation execution");
        Operation operation = saveOperation(operationCost, operationType);
        List<Record> records = recordRepository.findAllByUserAndActive(user, true).orElse(new ArrayList<>());
        records.sort(Comparator.comparingLong(Record::getId).reversed());
        int newAmount = getNewAmount(records) - operationCost;
        Record record = getRecord(user, operation, result, newAmount);
        recordRepository.save(record);
        logger.info("Clearing existing application cache data");
        return new OperationResponse(result, newAmount);
    }

    private static Record getRecord(User user, Operation operation, String result, int newAmount) {
        Record record = new Record();
        record.setOperation(operation);
        record.setOperationResponse(result);
        record.setActive(Boolean.TRUE);
        record.setUser(user);
        record.setAmount(newAmount);
        return record;
    }

    private static void validateRequestExecutioon(OperationRequest operationRequest, UserStatsResponse userStats, Integer operationCost) {
        logger.info("Validating request execution");
        if (userStats.getCurrentBalance() < operationCost) {
            throw new OperationException("Insufficient credits to execute this operation");
        }

        if (operationRequest.getOperationType().equals("DIVISION") && operationRequest.getValue2() == 0) {
            throw new OperationException("Is not possible execute division by zero");
        }

        if (operationRequest.getOperationType().equals("SQUARE_ROOT") && operationRequest.getValue1() < 0) {
            throw new OperationException("Operation error: Is not possible get the square root of a negative value");
        }
    }

    private Operation saveOperation(Integer operationCost, OperationTypeEnum operationType) {
        Operation operation = new Operation();
        operation.setCost(operationCost);
        operation.setType(operationType);
        operation = operationRepository.save(operation);
        return operation;
    }

    private Integer getOperationCost(OperationTypeEnum operationTypeEnum) {
        return (switch (operationTypeEnum) {
            case ADDITION -> 1;
            case SUBTRACTION -> 2;
            case MULTIPLICATION -> 3;
            case DIVISION -> 4;
            case SQUARE_ROOT -> 5;
            case RANDOM_STRING -> 6;
        });
    }

    public String invokeLambda(OperationTypeEnum operationType, Double value1, Double value2) {
        String payload = String.format("{ \"operationType\": \"%s\", \"value1\": %s, \"value2\": %s }", operationType.name(), value1, value2);
        SdkBytes payloadBytes = SdkBytes.fromString(payload, StandardCharsets.UTF_8);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(getLambdaFunction())
                .payload(payloadBytes)
                .build();
        try {
            InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
            return invokeResponse.payload().asUtf8String();
        } catch (ServiceException | ResourceNotFoundException e) {
            logger.debug("Error executing lambda function: " + e.getMessage());
            return "Error: Could not invoke Lambda function.";
        }
    }

    // Only for test purposes
    public void setLambdaFunction(String lambdaFunction) {
        LAMBDA_FUNCTION = lambdaFunction;
    }

    private String getLambdaFunction() {
        if (LAMBDA_FUNCTION != null) {
            return LAMBDA_FUNCTION;
        }
        LAMBDA_FUNCTION = System.getenv("AWS_LAMBDA_FUNCTION");
        return LAMBDA_FUNCTION;
    }
}

