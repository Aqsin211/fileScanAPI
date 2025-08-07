# FileScan — Virus Scanning Service Integration with Spring Boot and ClamAV

## Overview

**FileScan** is a Spring Boot backend service that securely accepts file uploads and scans them for viruses and malware using the ClamAV antivirus engine via its TCP socket interface. It performs asynchronous scanning to ensure efficient throughput, quarantines infected files, and sends email alerts to administrators upon detection of threats.

The service logs all scan results and actions, providing an audit trail for uploaded files and their virus scan status.

---

## Features

- **Multipart file upload via REST API**
- **Integration with ClamAV antivirus daemon** for virus scanning over TCP socket
- **Asynchronous scanning** using a dedicated thread pool to improve performance
- **Infection handling:** quarantines infected files and blocks unsafe content
- **Scan result persistence** in PostgreSQL database for auditing
- **MinIO object storage** buckets for temp, clean, and quarantine file management
- **Email alerts** to admins when infected files are detected
- **Graceful error handling** for failures in ClamAV or storage services

---

## Architecture & Components

- **Spring Boot** application exposing REST endpoints to upload files, check scan status, and delete scans
- **ClamAVClientService:** communicates with ClamAV daemon via TCP socket to scan files
- **FileScanService:** orchestrates async scanning, file uploads/downloads to MinIO buckets (temp, clean, quarantine), DB updates, and email alerts
- **MinIOStorageService:** handles file operations on MinIO object storage server
- **EmailNotificationService:** sends virus detection alerts to administrators via SMTP
- **PostgreSQL** used for scan result persistence via Spring Data JPA
- **AsyncConfig:** configures a thread pool executor for asynchronous scan processing

---

## API Endpoints

| Method | Endpoint               | Description                            | Response                    |
|--------|------------------------|------------------------------------|-----------------------------|
| POST   | `/filescan/upload`     | Uploads a file for virus scanning   | Accepted with generated `scanId` |
| GET    | `/filescan/status/{scanId}` | Returns the scan result for a given scanId | Scan status and details     |
| DELETE | `/filescan/delete/{scanId}` | Deletes the file and scan record   | Confirmation message         |

---

## Setup & Configuration

### Prerequisites

- Java 21
- PostgreSQL database
- ClamAV daemon running locally or accessible on TCP port 3310
- MinIO object storage server with buckets for temp, clean, and quarantine files
- SMTP mail server credentials for sending alerts (e.g., Gmail SMTP)

### Environment Variables

The application reads configuration values from environment variables or `application.yml` placeholders:

| Variable                 | Description                      |
|--------------------------|---------------------------------|
| `DB_USERNAME`            | PostgreSQL database username    |
| `DB_PASSWORD`            | PostgreSQL database password    |
| `EMAIL_USERNAME`         | SMTP email username             |
| `EMAIL_PASSWORD`         | SMTP email password             |
| `MINIO_URL`              | MinIO server URL                |
| `MINIO_ACCESS_KEY`       | MinIO access key                |
| `MINIO_SECRET_KEY`       | MinIO secret key                |
| `MINIO_BUCKET_TEMP`      | MinIO bucket for temporary files|
| `MINIO_BUCKET_CLEAN`     | MinIO bucket for clean files   |
| `MINIO_BUCKET_QUARANTINE`| MinIO bucket for quarantined files |
| `ADMIN_EMAIL`            | Comma-separated admin emails for alerts |

### Running the Application

1. Clone the repository and navigate to the project directory.
2. Set the required environment variables.
3. Start the ClamAV daemon (`clamd`) on port 3310.
4. Ensure PostgreSQL and MinIO are running and accessible.
5. Build and run the application using Gradle:

```bash
./gradlew bootRun
```

---

## How It Works

1. Client uploads a file via `/filescan/upload`.
2. The server generates a unique `scanId` and stores an initial pending record.
3. The file is uploaded to the MinIO **temp bucket**.
4. The scan runs asynchronously:
   - The file is downloaded from the temp bucket.
   - It is scanned by ClamAV via TCP socket.
   - If clean, file is moved to the **clean bucket**.
   - If infected, file is moved to the **quarantine bucket**, and an email alert is sent to admins.
5. Scan results are updated in the database with status, virus name (if any), and timestamp.
6. Clients can poll `/filescan/status/{scanId}` for scan results.
7. Files and scan records can be deleted via `/filescan/delete/{scanId}`.

---

## Dependencies

- Spring Boot 3.5.4
- Spring Data JPA
- PostgreSQL Driver
- MinIO Java Client 8.5.17
- Spring Boot Starter Mail
- Lombok
- ClamAV antivirus (external daemon)

---

## Error Handling

- If a scan result is not found, the API returns HTTP 404 with a descriptive error message.
- Unexpected exceptions return HTTP 500 with generic error info logged server-side.
- Failures in file upload, scanning, or storage update the scan result status as `ERROR`.

---

## Bonus Features

- Quarantined files are stored separately to prevent accidental use.
- Email notifications alert administrators immediately on virus detection.
