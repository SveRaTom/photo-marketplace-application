# Photographer Portfolio Marketplace

## Overview

A modern Spring Boot web application that connects photographers and clients through an online marketplace.

Photographers can showcase their portfolio, publish photography offers, manage bookings, and build their reputation through client reviews. Clients can browse photographer profiles, explore available offers, request bookings, and leave reviews after completed photography sessions.

The application follows a marketplace model where photographers promote their work and services while clients can easily discover and hire professionals for various photography needs.

---

## Technology Stack

* Java 21
* Spring Boot 3
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

## Features

### Authentication & Authorization

* User registration and login
* Role-based access control
* Photographer and Client account types

### Photographer Features

* Create and manage photography offers
* Upload portfolio photos
* Manage incoming bookings
* View client reviews
* Maintain a professional profile

### Client Features

* Browse photographers
* Explore photography offers
* View photographer portfolios
* Submit booking requests
* Leave reviews and ratings

### Booking Management

* Request photography sessions
* Approve or reject bookings
* Track booking status
* Manage event information

### Review System

* Five-star rating system
* Written feedback
* Photographer reputation building
* Public review visibility

---

## Domain Entities

### User

Represents an application user.

A user can be either a photographer or a client.

**Properties:**

* `id` (UUID)
* `firstName`
* `lastName`
* `username`
* `email`
* `password`
* `role`
* `profileImageUrl`
* `isActive`
* `createdAt`
* `updatedAt`

### Offer

Represents a photography offer published by a photographer.

**Properties:**

* `id` (UUID)
* `title`
* `description`
* `price`
* `durationHours`
* `isAvailable`
* `photographer`
* `coverPhoto`
* `createdAt`
* `updatedAt`

### Photo

Represents a portfolio image uploaded by a photographer.

**Properties:**

* `id` (UUID)
* `title`
* `imageUrl`
* `description`
* `createdAt`
* `updatedAt`

### Booking

Represents a booking request submitted by a client.

**Properties:**

* `id` (UUID)
* `eventDate`
* `location`
* `notes`
* `status`
* `createdAt`
* `updatedAt`

### Review

Represents feedback left by a client after a completed booking.

**Properties:**

* `id` (UUID)
* `rating`
* `comment`
* `createdAt`
* `updatedAt`

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

# User Workflow

## Photographer

1. Register as PHOTOGRAPHER
2. Create photography offers
3. Upload portfolio photos
4. Receive booking requests
5. Approve or reject requests
6. Complete bookings
7. Receive reviews

## Client

1. Register as CLIENT
2. Browse offers
3. View portfolios
4. Submit booking requests
5. Attend photography session
6. Leave review and rating

---

# Future Enhancements

## Marketplace Features

* Advanced search and filtering
* Offer categories
* Photographer verification
* Favorites and bookmarks

## Scheduling

* Availability calendar
* Time slot management
* Booking conflict prevention

## Communication

* Internal messaging system
* Email notifications
* Booking reminders

## Payments

* Online payment integration
* Deposits and invoices
* Refund management

## Media Management

* Image upload support
* Cloud storage integration
* Watermarking

## Administration

* Admin dashboard
* User moderation
* Review moderation
* Platform analytics
