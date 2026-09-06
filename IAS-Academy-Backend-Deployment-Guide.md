# IAS Academy Backend — Deployment Guide

## 1. Project Overview

| Item | Detail |
|---|---|
| **Framework** | Spring Boot 4.1.0 |
| **Java Version** | 17 |
| **Build Tool** | Maven (wrapper included) |
| **Database** | MySQL |
| **Auth** | JWT (jjwt 0.13.0) + Spring Security |
| **Other Dependencies** | Mail (SMTP), Validation, Lombok |
| **Artifact** | `com.academy:project:0.0.1-SNAPSHOT` |
| **Default Port** | 8080 |
| **Profiles** | `dev`, `prod` |

---

## 2. Prerequisites on Server

1. **Java 17+** (JDK or JRE)
2. **MySQL 8.x** with a database named `academy_db`
3. **Git** (if cloning from the repository)

---

## 3. Build Commands

```bash
# Clean + build JAR (skip tests for faster deploy)
./mvnw clean package -DskipTests
```

```bash
# On Windows use:
mvnw.cmd clean package -DskipTests
```

**Output JAR location:** `target/project-0.0.1-SNAPSHOT.jar`

---

## 4. Run Commands

**Development (local):**
```bash
java -jar target/project-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**Production:**
```bash
java -jar target/project-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 5. Production Configuration

Before running in production, update `src/main/resources/application-prod.properties`, **or** pass values via environment variables / command-line arguments.

| Property | What to Set |
|---|---|
| `spring.datasource.url` | Your production MySQL JDBC URL |
| `spring.datasource.username` | DB username |
| `spring.datasource.password` | DB password |
| `app.jwt.secret` | A strong secret key |
| `app.jwt.access-token-expiration-ms` | Access token TTL |
| `app.jwt.refresh-token-expiration-ms` | Refresh token TTL |
| `spring.mail.host` | SMTP host |
| `spring.mail.username` | SMTP username |
| `spring.mail.password` | SMTP password |
| `academy.contact.email` | Contact email |
| `app.cors.allowed-origins` | Frontend URL(s) |

### Overriding Properties at Launch

Any property can be overridden directly on the command line:

```bash
java -jar target/project-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:mysql://db-host:3306/academy_db \
  --spring.datasource.username=prod_user \
  --spring.datasource.password=secret \
  --app.jwt.secret=your-strong-secret \
  --app.cors.allowed-origins=https://yourdomain.com
```

---

## 7. Quick Reference

| Task | Command |
|---|---|
| Build JAR | `./mvnw clean package -DskipTests` |
| Run (dev) | `java -jar target/project-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev` |
| Run (prod) | `java -jar target/project-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod` |
| Run tests | `./mvnw test` |

---

## 6. Important Notes ⚠️

- **Schema creation:** Prod uses `ddl-auto=validate`, meaning the schema must already exist before the first run. Run the app with the `dev` profile first (which uses `update`) to auto-create tables, then switch to `prod`.
- **CORS:** Set `app.cors.allowed-origins` to your actual frontend domain before deploying to production.
- **SQL logging:** Disabled in the prod profile for performance.
- **SMTP credentials:** The dev profile uses hardcoded Mailtrap credentials — make sure production is configured with real SMTP credentials before going live.
