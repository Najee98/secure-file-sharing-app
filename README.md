# Secure File Sharing Portal

A full-stack Spring Boot application for secure file sharing with passwordless OTP authentication and SMS notifications. Users can upload, organize, and share files via time-bound links sent through SMS.

## 📋 Table of Contents
- [Features](#features)
- [System Architecture](#system-architecture)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Testing](#testing)
- [Screenshots](#screenshots)
- [Troubleshooting](#troubleshooting)

## ✨ Features

### Core Functionality
- 🔐 **Passwordless Authentication**: OTP-based login via SMS (5-minute validity)
- 🔑 **JWT Security**: Token-based authentication for all protected endpoints
- 📁 **File Management**: Upload, download, and organize files in nested folders
- 📂 **Folder Organization**: Create hierarchical folder structures
- 🔗 **Secure File Sharing**: Generate time-bound shareable links (7-day expiration)
- 📱 **SMS Notifications**: Automatic SMS for OTP delivery and file sharing
- 🚫 **Access Control**: Users can only access their own files or explicitly shared content
- 💾 **Local Storage**: Files stored on filesystem with abstracted storage layer for future cloud migration

### Security Features
- JWT-based authentication and authorization
- Protection against XSS, CSRF, and SQL Injection
- Secure OTP generation and validation
- Time-bound shared links with unique tokens
- User-isolated file storage

## 🏗️ System Architecture
```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Frontend   │─────▶│   Backend    │─────▶│  MySQL DB    │
│  (Angular)   │◀─────│ (Spring Boot)│◀─────│              │
└──────────────┘      └──────────────┘      └──────────────┘
                             │
                             │ REST API
                             ▼
                      ┌──────────────┐
                      │ SMS Service  │
                      │ (Mock/Real)  │
                      └──────────────┘
```

### Component Breakdown

**Backend (Spring Boot)**
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic and SMS integration
- **Repository Layer**: Database operations via Spring Data JPA
- **Security Layer**: JWT authentication filter and Spring Security configuration
- **Storage Layer**: Abstracted file storage service

**Frontend (Angular)**
- Simple UI for file management
- JWT token management with HTTP interceptors
- File upload with progress tracking
- Folder navigation and file operations

**SMS Service**
- Mock service for OTP delivery
- File sharing link notifications

**Database (MySQL)**
- User management
- File and folder hierarchy
- Shared link tracking with expiration

## 🛠️ Technologies Used

### Backend
- **Java 23**
- **Spring Boot 3.5.6**
- **Spring Security 6.5.6** with JWT
- **Spring Data JPA** (Hibernate)
- **MySQL 8.0** (Production)
- **H2 Database** (Testing)
- **Maven** (Build tool)
- **Springdoc OpenAPI** (Swagger UI)
- **JUnit 5 & Mockito** (Testing)

### Frontend
- **Angular 18**
- **TypeScript**
- **Angular Material**
- **HTTP Interceptors** for JWT

### SMS Service
- Mock REST service for SMS delivery
- Configurable retry mechanism (3 attempts)

### DevOps
- **Docker & Docker Compose**
- **Nginx** (Frontend server, but discarded due to using basic HTTP config)

## 📦 Prerequisites

### For Docker Deployment (Recommended):
- Docker Desktop (latest version)
- Docker Compose v2.32.4+
- Git

### For Local Development:
- JDK 17 or higher
- Maven 3.6+
- MySQL 8.0
- Node.js 22+ and npm (for frontend)
- Git


## 🚀 Installation & Setup

### Step 1: Clone the Repository
```bash or cmd
git clone <repository-url>
cd project-root
```

### Step 2: Verify Project Structure

### Step 3: Review Database Schema

The `schema.sql` file contains the complete database structure with 5 main tables:

- **app_users**: User accounts with phone numbers
- **storage_paths**: User-specific storage directories (each user should have only one storage path to serve as root folder, and a folder is created to match it)
- **folders**: Hierarchical folder structure
- **files**: File metadata and references
- **shared_links**: Time-bound sharing tokens

### Step 4: Environment Configuration

The application uses environment variables for configuration. These are set in `docker-compose.yml` for Docker deployment.

**Key Environment Variables:**
- db-url: MySQL JDBC connection string (for example: jdbc:mysql://localhost:3306/file_sharing_db)
- db-username: Database username
- db-password: Database password
- sms-service-url: SMS service endpoint

## 🏃 Running the Application

### Method 1: Docker Compose (Recommended)

This is the easiest way to run the entire stack (frontend, backend, SMS service, and MySQL).
```bash
# Build and start all services
docker-compose up --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove all data (clean slate)
docker-compose down -v
```

**Services will be available at:**
- 🌐 **Frontend**: http://localhost:4200
- 🔧 **Backend API**: http://backend:8080
- 📱 **SMS Service**: http://sms-service:8081
- 🗄️ **MySQL**: mysql:3306
- 📚 **Swagger UI**: http://localhost:8080/swagger-ui.html

### Method 2: Local Development

#### 2.1 Start MySQL Database
```bash
# Start MySQL (if installed locally)

# Create database
mysql -u root -p
CREATE DATABASE file_sharing_db;
exit

# Import schema
mysql -u root -p file_sharing_db < schema.sql
```

#### 2.2 Run SMS Service
```bash
cd sms-service
npm install
npm start
# SMS Service will run on http://localhost:8081
```

#### 2.3 Run Backend
```bash
cd backend

# Set environment variables (I used IntelliJ environment variables)
db-url="jdbc:mysql://localhost:3306/file_sharing_db"
db-username="mysql_user"
db-password="mysql_password"
sms-service-url="http://localhost:8081"

# Or Windows (Command Prompt)
set db-url=jdbc:mysql://localhost:3306/file_sharing_db
set db-username= mysql_user
set db-password= mysql_password
set sms-service-url=http://localhost:8081

# Run application
run in IntelliJ (Shift+F10)
# or run in terminal
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/secure-file-sharing-app-0.0.1-SNAPSHOT.jar
```

Backend will start on http://localhost:8080

#### 2.4 Run Frontend
```bash
cd frontend
npm install
ng serve
```

Frontend will start on http://localhost:4200

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Interactive API Docs
Access Swagger UI for interactive API testing:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON specification:
```
http://localhost:8080/api-docs
```

---

## 🗄️ Database Schema

### Entity Relationship Diagram
```
app_users (1) ─────< (1) storage_paths
    │
    │
    ▼
storage_paths (1) ─────< (*) folders
    │                        │
    │                        │ (self-referencing)
    │                        ▼
    │                    folders (nested)
    │
    └─────────────────< (*) files
                             │
                             ▼
                        shared_links (*)
                             │
                             └──< folders (optional)
```

### Tables

#### 1. app_users
Stores user account information.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| phone_number | VARCHAR(255) | Unique phone number |
| created_at | DATETIME | Account creation timestamp |
| updated_at | DATETIME | Last update timestamp |

#### 2. storage_paths
User-specific base storage directory.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| base_path | VARCHAR(255) | Physical storage path |
| app_user_id | BIGINT | Foreign key to app_users (one-to-one) |
| created_at | DATETIME | Creation timestamp |

#### 3. folders
Hierarchical folder structure.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| name | VARCHAR(255) | Folder name |
| parent_folder_id | BIGINT | Self-referencing foreign key (nullable) |
| storage_path_id | BIGINT | Foreign key to storage_paths |
| created_at | DATETIME | Creation timestamp |
| updated_at | DATETIME | Last update timestamp |

#### 4. files
File metadata and storage information.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| display_name | VARCHAR(255) | User-visible filename |
| physical_name | VARCHAR(255) | Unique physical filename (UUID-based) |
| physical_path | VARCHAR(255) | Full storage path |
| size | BIGINT | File size in bytes |
| mime_type | VARCHAR(255) | Content type |
| folder_id | BIGINT | Foreign key to folders (nullable for root) |
| storage_path_id | BIGINT | Foreign key to storage_paths |
| created_at | DATETIME | Upload timestamp |
| updated_at | DATETIME | Last update timestamp |

#### 5. shared_links
Time-bound sharing tokens.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| link_token | VARCHAR(255) | Unique shareable token |
| file_id | BIGINT | Foreign key to files (nullable) |
| folder_id | BIGINT | Foreign key to folders (nullable) |
| expires_at | DATETIME | Link expiration (7 days) |
| created_at | DATETIME | Creation timestamp |

**Constraint:** `CHK_file_or_folder` ensures exactly one of file_id or folder_id is set.

---

## ⚙️ Configuration

### Backend Configuration

#### application.properties (Production)
```properties
# Application Name
spring.application.name=secure-file-sharing-app

# MySQL Configuration
spring.datasource.url=${db-url}
spring.datasource.username=${db-username}
spring.datasource.password=${db-password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=create-drop

# Swagger Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.disable-swagger-default-url=true

# File Storage
app.storage.root-path=/app-storage

# SMS Service Integration
sms.service.url=${sms-service-url}
sms.service.retry.attempts=3
```

#### application-test.properties (Testing)
```properties
# H2 In-Memory Database (MySQL Mode)
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Disable Swagger in Tests
springdoc.api-docs.enabled=false

# Test File Storage
app.storage.root-path=${java.io.tmpdir}/file-sharing-test

# Mock SMS Service
sms.service.url=http://localhost:8081
```

### Docker Configuration

#### docker-compose.yml

Key service configurations:

**MySQL Service:**
- Port: 3307 (host) → 3306 (container)
- Database: `file_sharing_db`
- Credentials: root/rootpass, dbuser/dbpass

**Backend Service:**
- Port: 8080
- Volume: `file_storage:/app-storage`
- Depends on: MySQL (with health check), SMS service

**Frontend Service:**
- Port: 4200 → 80 (nginx)
- Depends on: Backend

**SMS Service:**
- Port: 8081
- Mock service for SMS delivery

### Storage Configuration

Files are stored with the following structure:
```
/app-storage/
└── {user-id}/
    ├── {folder-id}/
    │   └── {uuid}-{filename}
    └── {uuid}-{filename}
```

**Production Note:** For production deployment, change `spring.jpa.hibernate.ddl-auto` to `validate` and use database migration tools (Flyway/Liquibase).

---

## 🧪 Testing

### Run All Tests
```bash
cd backend
mvn test
```

### Test Configuration

Tests use **H2 in-memory database** with MySQL compatibility mode:
- No MySQL installation required for testing
- Automatic schema creation
- Isolated test environment
- Temp directory for file storage

### Test Coverage
```bash
# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report (Linux/macOS)
open target/site/jacoco/index.html

# View coverage report (Windows)
start target/site/jacoco/index.html
```

**Target Coverage:** >80%

### Test Categories

1. **Unit Tests**
   - Service layer logic
   - Utility functions
   - OTP generation/validation
   - JWT token handling

2. **Integration Tests**
   - Controller endpoints
   - Database operations
   - File upload/download
   - Authentication flow

3. **Security Tests**
   - JWT validation
   - Access control
   - SQL injection protection

---

## 🐛 Troubleshooting

### Common Issues

#### 1. MySQL Connection Failed

**Error:**
```
Communications link failure
```

**Solutions:**
```bash
# Check if MySQL container is running
docker ps | grep mysql

# View MySQL logs
docker logs mysql-db

# Verify MySQL is ready
docker exec mysql-db mysqladmin ping -h localhost -u root -prootpass

# Check network connectivity
docker exec backend ping mysql
```

#### 2. Backend Can't Connect to MySQL

**Error:**
```
Access denied for user 'root'@'...'
```

**Solutions:**
- Verify credentials in docker-compose.yml match
- Check environment variables are set correctly
- Ensure MySQL health check passes before backend starts
- Try connecting manually:
```bash
  docker exec -it mysql-db mysql -u root -prootpass file_sharing_db
```

#### 3. File Upload Failed

**Error:**
```
Could not create directory
```

**Solutions:**
- Check `/app-storage` has write permissions
- Verify volume mount in docker-compose.yml:
```yaml
  volumes:
    - file_storage:/app-storage
```
- Check disk space:
```bash
  docker exec backend df -h
```

#### 4. OTP Not Received

**Possible Causes:**
- SMS service not running
- SMS service URL misconfigured

**Solutions:**
```bash
# Check SMS service status
docker ps | grep sms-service

# Test SMS service manually
curl -X POST http://localhost:8081/api/sms/send \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+1234567890","message":"Test"}'

# Check backend logs
docker logs backend | grep SMS
```

#### 5. JWT Token Expired

**Error:**
```
401 Unauthorized
```

**Solution:**
- Request new OTP and re-authenticate
- Check token expiration time in JWT payload
- Ensure system time is synchronized

#### 6. Shared Link Not Working

**Possible Causes:**
- Link expired (>7 days old)
- Link token invalid
- File deleted

**Solutions:**
```bash
# Check link expiration in database
docker exec mysql-db mysql -u root -prootpass -e \
  "USE file_sharing_db; SELECT * FROM shared_links WHERE link_token='YOUR_TOKEN';"

# Generate new share link if expired
```

#### 7. Port Already in Use

**Error:**
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**Solutions:**
```bash
# Find process using port (Linux/macOS)
lsof -i :8080
kill -9 <PID>

# Find process using port (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in docker-compose.yml
ports:
  - "8081:8080"  # Use different host port
```

#### 8. Docker Build Fails

**Solutions:**
```bash
# Clear Docker cache
docker system prune -a

# Rebuild without cache
docker-compose build --no-cache

# Check Docker disk space
docker system df
```

### Performance Issues

#### Slow File Upload

**Solutions:**
- Check network bandwidth
- Increase Docker memory allocation
- Consider file size limits
- Check disk I/O performance

#### Database Performance

**Solutions:**
```bash
# Check database size
docker exec mysql-db mysql -u root -prootpass -e \
  "SELECT table_schema AS 'Database', 
   ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' 
   FROM information_schema.tables 
   GROUP BY table_schema;"

# Optimize tables
docker exec mysql-db mysql -u root -prootpass -e \
  "USE file_sharing_db; OPTIMIZE TABLE files, folders, shared_links;"
```

### Getting Help

If you encounter issues not covered here:

1. Check application logs:
```bash
   docker-compose logs backend
   docker-compose logs frontend
   docker-compose logs sms-service
```

2. Enable debug logging in `application.properties`:
```properties
   logging.level.root=DEBUG
```

3. Test individual components:
   - MySQL: `docker exec -it mysql-db mysql -u root -prootpass`
   - Backend: `curl http://localhost:8080/actuator/health`
   - SMS Service: `curl http://localhost:8081/health`

---

## 🔒 Security Considerations

### Production Deployment Checklist

- [ ] Change default database passwords
- [ ] Use environment variables for all secrets
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Set up rate limiting for OTP requests
- [ ] Implement file size limits
- [ ] Add virus scanning for uploads
- [ ] Use cloud storage (AWS S3, Azure Blob)
- [ ] Set up database backups
- [ ] Enable logging and monitoring
- [ ] Use robust production database instead of MySQL and H2
- [ ] Use production-grade SMS service
- [ ] Implement proper session management
- [ ] Add request throttling
- [ ] Configure firewall rules

### Security Features Implemented

✅ JWT-based authentication  
✅ OTP with time-bound validation  
✅ Password-less authentication  
✅ SQL injection protection (JPA)  
✅ CSRF protection (Spring Security)  
✅ XSS protection (Content Security Policy)  
✅ Access control (user-isolated storage)  
✅ Secure file storage with unique names  
✅ Time-bound shareable links  
✅ Input validation and sanitization  

---

## 📖 Additional Documentation

- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Database Schema**: See `schema.sql`

---

## 👤 Author

**Your Name**  
Email: najee.shaheen.98@gmail.com  
GitHub: Najee98

**Submission Date:** October 26, 2025
