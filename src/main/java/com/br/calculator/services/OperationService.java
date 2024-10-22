package com.br.calculator.services;

import com.br.calculator.controllers.OperationController;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OperationService {

    private static final Logger logger = LoggerFactory.getLogger(OperationService.class);

    private static final int INITIAL_AMOUNT = 200;
    @Value("${aws.lambda.function}")
    private String LAMBDA_FUNCTION;

    private final CacheManager cacheManager;
    private final RecordService recordService;
    private final OperationRepository operationRepository;
    private final RecordRepository recordRepository;
    private final LambdaClient lambdaClient;

    public OperationService(RecordService recordService, OperationRepository operationRepository, RecordRepository recordRepository, CacheManager cacheManager) {
        this.lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        this.recordService = recordService;
        this.operationRepository = operationRepository;
        this.recordRepository = recordRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable("userOperations")
    public Page<RecordResponse> getUserRecords(User user, Pageable pageable) {
        var recordsPage = recordRepository.findAllByUser(user, pageable);
        logger.info("Data fetched. Mapping response data");
        return recordsPage.map(this::getRecordResponse);
    }


    @Cacheable("userStats")
    public UserStatsResponse getUserStats(User user) {
        logger.info("Fetching user stats");
        var records = recordRepository.findAllByUser(user).orElse(new ArrayList<>());
        Optional<Record> lastRecord = records.stream().max(Comparator.comparingLong(Record::getId));
        UserStatsResponse userStatsResponse = new UserStatsResponse();
        userStatsResponse.setTotalOperations((long) records.size());
        userStatsResponse.setCurrentBalance(lastRecord.map(Record::getAmount).orElse(200));
        return userStatsResponse;
    }

    private RecordResponse getRecordResponse(Record record) {
        var response = new RecordResponse();
        response.setOperationId(record.getId());
        response.setDate(record.getDate());
        response.setOperationCost(record.getOperation().getCost());
        response.setOperationType(record.getOperation().getType().name());
        response.setUserBalance(record.getAmount());
        return response;
    }

    public OperationResponse executeOperation(OperationRequest operationRequest, User user) {
        var operationType = OperationTypeEnum.fromString(operationRequest.getOperationType());
        var operationCost = getOperationCost(operationType);
        var userStats = getUserStats(user);
        validateRequestExecutioon(operationRequest, userStats, operationCost);
        logger.info("Invoking lambda function");
        var result = invokeLambda(operationType, operationRequest.getValue1(), operationRequest.getValue2());
        logger.info("Saving operation execution");
        Operation operation = saveOperation(operationCost, operationType);
        List<Record> records = recordService.findRecordsByUser(user).orElse(new ArrayList<>());
        records.sort(Comparator.comparingLong(Record::getId).reversed());
        Record record = new Record();
        record.setOperation(operation);
        record.setOperationResponse(result);
        record.setUser(user);

        if (records.isEmpty()) {
            record.setAmount(INITIAL_AMOUNT - operationCost);
        } else {
            var lastRecord = records.stream().findFirst();
            record.setAmount(lastRecord.get().getAmount() - operationCost);
        }
        recordRepository.save(record);
        logger.info("Clearing existing application cache data");
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        return new OperationResponse(result, record.getAmount());
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

