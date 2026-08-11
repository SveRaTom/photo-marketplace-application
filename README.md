# LensMarket — Photographer Portfolio Marketplace

## Overview

Photographer Portfolio Marketplace connects photographers and clients through photography offers, bookings, portfolio photos, reviews, and custom offer requests. The solution contains two independently runnable Spring Boot applications:

- The main MVC application provides the Thymeleaf user interface, authentication, authorization, and the core marketplace workflows.
- `custom-offer-service` provides a domain-specific REST API for creating, deciding, and withdrawing custom photography offer requests.

The applications communicate through Spring Cloud OpenFeign and use separate MySQL databases.

## Technology Stack

- Java 21
- Spring Boot 3.4.0
- Spring MVC and Thymeleaf
- Spring Security with CSRF protection
- Spring Data JPA and Hibernate
- Spring Cloud OpenFeign
- Spring Cache
- Spring Scheduling
- MySQL and H2 for tests
- Jakarta Bean Validation
- Maven
- Lombok
- JUnit 5, Mockito, MockMvc, and JaCoCo
- HTML5, CSS3, and JavaScript

## Project Structure

```text
photo-marketplace-application/
├── src/                         Main Spring Boot MVC application
├── custom-offer-service/        Independent Spring Boot REST microservice
│   ├── pom.xml
│   └── src/
├── pom.xml                      Main application Maven build
└── README.md
```

The main application runs on port `8080`. The custom-offer service runs on port `8081` by default and can be configured independently.

## User Roles and Security

Spring Security provides form-based authentication, BCrypt password hashing, role-based authorization, session management, and CSRF protection.

### Guest

- View the landing page
- Register and log in
- Browse offers, public offer details, photos, and reviews

### Client

- Browse offers and photographer portfolios
- Create, edit, and cancel eligible bookings
- Create, edit, and delete eligible reviews
- Request a custom offer from a photographer
- View and withdraw pending custom offer requests
- View and edit their own profile

### Photographer

- Create, edit, and delete owned offers
- Add, edit, delete, and select cover photos for owned offers
- Approve or reject booking requests
- View portfolio, bookings, and received reviews
- Review custom requests and accept them with a proposed price or decline them
- View dashboard statistics, upcoming bookings, and recent reviews
- View and edit their own profile

### Administrator

- View registered users
- Change another user's role
- Access administrator-only routes
- View and edit their own profile

Public, authenticated, and role-protected endpoints are configured separately. Ownership and status rules are also enforced in the service layer.

## Main Application Features

### Offers

- Public offer catalog and offer details
- Photographer-owned offer creation, editing, and deletion
- Availability, price, duration, location, and cover photo management
- Cached offer catalog, offer details, and photographer offer lists
- Cache eviction after offer state changes

### Photos

- Offer galleries and photographer portfolios
- Photo details
- Photographer-owned photo creation, editing, and deletion
- Cover photo selection
- Image URL-based media storage

### Bookings

- Client booking requests for available offers
- Client editing of pending bookings
- Client cancellation of pending or approved bookings
- Photographer approval or rejection of pending requests
- Status lifecycle: `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`, and `CANCELLED`

### Reviews

- Reviews associated with an offer and a booking
- One review per eligible booking
- Ratings from one to five and written feedback
- Client-owned review creation, editing, and deletion

### Custom Offers

- A client requests a tailored photography service for an existing offer
- A photographer accepts the request with a proposed price or declines it
- A client withdraws a pending request
- Both roles can view their relevant custom offer requests
- Each request identifies its original offer and, for photographers, the requesting client
- Opening the original offer preserves the return path to the exact request on the relevant custom-offers page
- All custom offer state is persisted by the separate REST microservice

### User Management

- Registration and login
- Profile viewing and editing for authenticated users
- Photographer dashboard
- Administrator user and role management

## Domain Model

All persisted entities use UUID primary keys.

### Main application

- `User` represents a client, photographer, or administrator.
- `Offer` represents a photography service published by a photographer.
- `Photo` represents an image attached to an offer.
- `Booking` represents a client's reservation request for an offer.
- `Review` represents client feedback linked to a booking and offer.

The model includes JPA relationships between users, offers, photos, bookings, and reviews.

### Custom-offer service

- `CustomOfferRequest` stores the client, photographer, and offer identifiers together with event details, the client's message, an optional proposed price, and its status.
- Status lifecycle: `PENDING`, `ACCEPTED`, `DECLINED`, and `WITHDRAWN`.

## REST Microservice API

The custom-offer service exposes this API. The main application uses `CustomOfferClient` to create and list requests, record decisions, and withdraw pending requests:

| Method   | Endpoint                                                                      | Purpose                             |
|----------|-------------------------------------------------------------------------------|-------------------------------------|
| `POST`   | `/api/custom-offers`                                                          | Create a custom offer request       |
| `GET`    | `/api/custom-offers/{customOfferId}`                                          | Retrieve one request                |
| `GET`    | `/api/custom-offers?clientId={clientId}`                                      | Retrieve a client's requests        |
| `GET`    | `/api/custom-offers?photographerId={photographerId}`                          | Retrieve a photographer's requests  |
| `PUT`    | `/api/custom-offers/{customOfferId}/decision?photographerId={photographerId}` | Accept or decline a pending request |
| `DELETE` | `/api/custom-offers/{customOfferId}?clientId={clientId}`                      | Withdraw a pending request          |

The microservice returns JSON responses and meaningful validation, not-found, and invalid-operation errors. The main application translates integration failures into user-facing MVC error pages or form messages.

## Validation and Error Handling

Both applications validate incoming DTOs, persisted entities, and service-layer business rules.

Examples include:

- Required and correctly formatted account data
- Positive offer prices and durations
- Future booking and custom-offer dates
- Required locations and descriptions
- Review ratings between one and five
- Ownership, role, state-transition, and duplicate-request rules

Each application has centralized handlers for built-in and custom exceptions. The main application uses branded error views instead of the Spring Whitelabel page; the microservice returns structured JSON errors.

## Caching and Scheduled Jobs

The main application uses Spring's caching abstraction for offer reads. Relevant caches are evicted when offers or associated cover-photo state changes.

Two scheduled jobs affect marketplace state:

- A cron job marks offers unavailable after the configured stale period.
- A fixed-delay job marks past approved bookings as completed.

The schedule values are configurable in `src/main/resources/application.properties`.

## Logging

State-changing marketplace and custom-offer workflows include SLF4J log statements. Scheduled jobs also log their results, and integration failures are logged without exposing sensitive credentials.

## Configuration

### Main application

Shared defaults are in `src/main/resources/application.properties`. Production overrides are in
`application-prod.properties`. The default active profile is `dev`, but `application-dev.properties` is intentionally
ignored by Git so local credentials remain outside source control.

Create `src/main/resources/application-dev.properties` locally when using property-based development configuration:

```properties
spring.datasource.username=<mysql-username>
spring.datasource.password=<mysql-password>
app.photographer.password=<local-seed-password>
app.client.password=<local-seed-password>
app.admin.password=<local-seed-password>
```

The main application also supports these environment variables:

- `MAIN_APP_PORT`
- `MAIN_DB_URL`
- `MAIN_DB_USERNAME`
- `MAIN_DB_PASSWORD`
- `APP_PHOTOGRAPHER_PASSWORD`
- `APP_CLIENT_PASSWORD`
- `APP_ADMIN_PASSWORD`
- `CUSTOM_OFFER_SERVICE_URL`

The default port is `8080`, and the default database is `photo_marketplace_app_db`. The main application connects to
the microservice at `http://localhost:8081` unless `CUSTOM_OFFER_SERVICE_URL` overrides it.

When an account is missing and its password is configured, the application seeds one account for each role:

| Role          | Username        | Email                      |
|---------------|-----------------|----------------------------|
| Photographer  | `photographer`  | `photographer@example.com` |
| Client        | `client`        | `client@example.com`       |
| Administrator | `administrator` | `admin@example.com`        |

Seed passwords are read from configuration and stored BCrypt-hashed in the database. Accounts with missing or blank
seed passwords are not created. Do not commit real database or account passwords.

### Custom-offer service

The service configuration is in `custom-offer-service/src/main/resources/application.properties`. It supports these environment variables:

- `CUSTOM_OFFER_SERVICE_PORT`
- `CUSTOM_OFFER_DB_URL`
- `CUSTOM_OFFER_DB_USERNAME`
- `CUSTOM_OFFER_DB_PASSWORD`

Its default database is `photo_marketplace_custom_offer_db`, separate from the main database.

## Build and Run

Prerequisites:

- JDK 21
- Maven 3.9 or later
- A running MySQL server

Build both applications from the repository root:

```bash
mvn clean package
mvn -f custom-offer-service/pom.xml clean package
```

Start the custom-offer service first:

```bash
mvn -f custom-offer-service/pom.xml spring-boot:run
```

Then start the main application in another terminal:

```bash
mvn spring-boot:run
```

Open the main application at `http://localhost:8080`. The custom-offer API is available on port `8081` by default, with service health reported at `http://localhost:8081/actuator/health`.

To run packaged JARs instead, use:

```bash
java -jar custom-offer-service/target/photo-marketplace-custom-offer-service-0.0.1-SNAPSHOT.jar
java -jar target/photo-marketplace-application-0.0.1-SNAPSHOT.jar
```

## Testing and Coverage

Both applications contain unit, Spring integration, and API/controller tests. H2 supplies isolated test databases, MockMvc tests HTTP behavior, and JaCoCo generates line-coverage reports.

Run the main application tests:

```bash
mvn clean test
```

Run the custom-offer service tests:

```bash
mvn -f custom-offer-service/pom.xml clean test
```

Coverage reports are generated at:

- `target/site/jacoco/index.html`
- `custom-offer-service/target/site/jacoco/index.html`

## Main Web Routes

- `/` - public landing page
- `/offers` - public offer catalog
- `/dashboard` - photographer dashboard
- `/profile` - authenticated user's profile
- `/my-offers` - photographer's offers
- `/portfolio` - photographer's photos
- `/bookings` - current user's bookings
- `/reviews` - reviews relevant to the current user
- `/custom-offers` - client's custom requests
- `/photographer/custom-offers` - photographer's custom requests
- `/admin/users` - administrator user management
