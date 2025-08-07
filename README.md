# FileScanAPI

## Overview

**FileScanAPI** is a Spring Boot microservice designed to securely scan uploaded files for viruses using ClamAV and manage file storage with MinIO. It supports asynchronous scanning, stores scan results in a PostgreSQL database, and sends alert notifications via email if infected files are detected.

---

## Features

- File upload API with asynchronous virus scanning  
- ClamAV integration for virus detection  
- File storage in MinIO object storage, with buckets for temp, clean, and quarantine files  
- Scan results stored in PostgreSQL database  
- Email alerts to administrators on virus detection  
- Simple CRUD for managing scan results (get, create/upload, delete)  
- Configurable via `application.yaml` and environment variables  
- Support for multiple admin emails for alert notifications

---

## Technology Stack

- Java 21  
- Spring Boot 3.5.4  
- PostgreSQL  
- MinIO (S3-compatible object storage)  
- ClamAV (virus scanner)  
- JavaMail (email notifications)  
- Lombok  
- Gradle build system

---

## Prerequisites

- Java JDK 21 or higher  
- PostgreSQL database  
- MinIO server running locally or remotely  
- ClamAV daemon running and accessible  
- SMTP email account for sending alerts (e.g., Gmail SMTP)

---

## Configuration

Configure your application in `src/main/resources/application.yaml`. For security, **do not commit secrets** directly—use environment variables or external config files that are `.gitignore`d.

Example config snippet:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scan-result-database
    username: your_db_user
    password: your_db_password
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@example.com
    password: your_email_password
    properties:
      mail:
        smtp:
          auth: true
          starttls.enable: true
          ssl.trust: smtp.gmail.com

minio:
  url: http://localhost:9000
  access-key: your_minio_access_key
  secret-key: your_minio_secret_key
  bucket:
    temp: temp
    clean: scanned-clean-files
    quarantine: scanned-quarantine-files

admin-email: admin1@example.com,admin2@example.com
````

---

## Running the Application

1. Clone the repository:

   ```bash
   git clone https://github.com/Aqsin211/FileScanAPI.git
   cd FileScanAPI
   ```

2. Set up your environment variables or `application.yaml` with correct database, MinIO, and email credentials.

3. Start ClamAV daemon (`clamd`).

4. Start MinIO server.

5. Build and run the Spring Boot app:

   ```bash
   ./gradlew bootRun
   ```

---

## API Endpoints

| Method | Path                        | Description                    |
| ------ | --------------------------- | ------------------------------ |
| POST   | `/filescan/upload`          | Upload file and start scan     |
| GET    | `/filescan/status/{scanId}` | Retrieve scan result by scanId |
| DELETE | `/filescan/delete/{scanId}` | Delete scanned file and result |

---

## Usage Example (curl)

Upload a file for scanning:

```bash
curl -X POST http://localhost:8080/filescan/upload -H "Accept: application/json" -F "file=@/path/to/file.txt"
```

Check scan result by `scanId`:

```bash
curl http://localhost:8080/filescan/status{scanId}
```

Delete scan result and file by `scanId`:

```bash
curl -X DELETE http://localhost:8080/filescan/delete/{scanId}
```

---

## Important Notes

* The service stores files temporarily in the MinIO `temp` bucket until scanned. Clean files move to the `clean` bucket; infected files move to the `quarantine` bucket.
* Email notifications are sent asynchronously to all configured admin emails on virus detection.
* Scan results are stored with UUID scan IDs. Users interact mainly through these IDs.
* Secrets **must not** be committed to version control. Use environment variables or secure vault solutions in production.
* Make sure ClamAV and MinIO services are accessible and configured properly before running the app.

---

## Contact

For issues or feature requests, please open an issue on the GitHub repository.
