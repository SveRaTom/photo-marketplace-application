# Photographer Portfolio Marketplace

## Overview

Photographer Portfolio Marketplace is a Spring Boot web application that connects photographers with potential clients. Photographers can create professional portfolios, upload photographs, manage booking requests, and receive reviews. Clients can browse portfolios, submit hire requests, and leave reviews after working with photographers.

---

## Technology Stack

* Java 23
* Spring Boot 3.4.0
* Spring MVC
* Spring Data JPA
* Thymeleaf
* MySQL
* Maven
* Hibernate
* HTML5
* CSS3
* Git

---

## User Roles

### Guest

* Register
* Login
* Browse portfolios

### Client

* Browse portfolios
* Submit hire requests
* View own requests
* Cancel requests
* Leave reviews

### Photographer

* Create portfolios
* Edit portfolios
* Delete portfolios
* Upload photos
* Manage photos
* Approve requests
* Reject requests

---

## Main Functionalities

### Portfolio Management

Photographers can create, edit, view, and delete portfolios.

### Photo Management

Photographers can upload, update, and remove photos from portfolios.

### Hire Request Management

Clients can create hire requests. Photographers can approve or reject them.

### Review System

Clients can leave ratings and reviews for photographers.

---

## Domain Entities

### User

Represents an application user.

**Properties:**

* `id` (UUID)
* `username`
* `email`
* `password`
* `firstName`
* `lastName`
* `role`
* `createdAt`

### PhotographyService

Represents a photography service offered by a photographer.

**Properties:**

* `id` (UUID)
* `title`
* `description`
* `price`
* `durationHours`
* `category`
* `location`
* `active`
* `createdAt`

### Booking

Represents a reservation made by a client for a photography service.

**Properties:**

* `id` (UUID)
* `bookingDate`
* `eventLocation`
* `notes`
* `status`
* `createdAt`

### Review

Represents feedback left by a client after a completed photography service.

**Properties:**

* `id` (UUID)
* `rating`
* `comment`
* `createdAt`

---

## Security

* Session-based authentication
* Password hashing using BCrypt
* Role-based access control
* Protected routes for authenticated users

---

## Validation

All forms perform server-side validation and display field-level error messages using Thymeleaf.

---

## Installation

1. Clone repository
2. Create MySQL database
3. Configure the environment-specific properties files:

    - `application-dev.properties` – local development configuration
    - `application-prod.properties` – production configuration
   
   Ensure the MySQL database URL, username, and password are correctly configured for the selected profile.
4. Set the active Spring profile:
   ```properties
   spring.profiles.active=dev
   ```
   or
    ```properties
    spring.profiles.active=prod
   ```
5. Run Maven install
6. Start application

---

## Future Improvements

* Image upload storage
* Notifications
* Portfolio likes
* Advanced search filters
* Photographer verification
* Chat system
