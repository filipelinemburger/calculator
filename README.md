# Calculator Application

## Overview
This Calculator Application provides a web interface to perform various mathematical operations, including addition, subtraction, multiplication, division, square root calculations, and random string generation. Built with a Spring Boot backend and a React frontend, this application enables users to execute operations with each transaction deducted from their balance.

## Features
- User authentication and authorization
- Basic arithmetic operations
- Square root calculation
- Random string generation
- Real-time balance updates
- Operation history tracking
- Responsive user interface

## Technologies Used
### Backend
- **Java 17**
- **Spring Boot**
- **AWS Lambda** for serverless execution of operations
- **MySQL** for data persistence
- **Spring Security** for JWT-based authentication
- **Gradle** as the build tool

### Frontend
- **React**
- **Bootstrap** for responsive design
- **React Router** for routing
- **Axios** for API requests

## Getting Started

### Prerequisites
- **Java 17**
- **Node.js** and **npm**
- **MySQL** database
- **Gradle**

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/filipelinemburger/calculator.git
   cd calculator

2. **Backend Setup**

    Configure AWS lambda function name, JWT secret and the MySQL database connection in src/main/resources/application.properties:
    properties:

    ```bash 
    spring.datasource.url=jdbc:mysql://localhost:3306/calculator_db
    spring.datasource.username=yourusername
    spring.datasource.password=yourpassword
    jwt.secret=yourJwtSecret
    aws.lambda.function=yourAwsLambdaFunction

3. **Build and start the Spring Boot application:**

    bash
    
        ./gradlew bootRun
    
    Frontend Setup
    
        Navigate to the calculator-frontend directory.
        Install the dependencies:
    
    bash

        npm install
    
    Start the frontend application:
    
    bash
    
            npm start
    
    The frontend will be available at http://localhost:3000.
    
    Running Tests:
    
        Backend Tests
            ./gradlew test

    API Endpoints: The backend provides the following main API endpoints:
    
        POST /api/auth/login - User login
        POST /api/auth/register - User registration
        GET /api/calculate - Perform an operation
        GET /api/history - Retrieve the user's operation history
    
    Project Structure
    Backend
    
        src/main/java - Java source code
        src/main/resources - Application properties and static resources
    
    Deployment
    
    Instructions for deploying to AWS or another cloud provider can be added here.
    Contributing
    
    Contributions are welcome! Please fork the repository and create a pull request.
    License
    
    This project is licensed under the MIT License - see the LICENSE file for details.
