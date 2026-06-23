# Photographer Portfolio Marketplace

## Overview

Photographer Portfolio Marketplace is a Spring Boot web application that connects photographers and clients through photography offers, bookings, portfolio photos, and client reviews.

Photographers can publish offers, attach portfolio photos to those offers, choose a cover photo, manage booking requests, and receive reviews. Clients can browse offers, request bookings, cancel eligible bookings, and review offers after an approved or completed booking.

The application uses server-rendered Thymeleaf pages with `ModelAndView`, session-based login, server-side validation, and role-aware business rules.

---

## Technology Stack

* Java 21
* Spring Boot 3.4
* Spring MVC
* Spring Data JPA
* Thymeleaf
* MySQL
* Hibernate
* Maven
* Lombok
* HTML5
* CSS3
* JavaScript

---

## User Roles

### Guest

* View the public landing page
* Register
* Login
* Browse public offers and public offer details

### Client

* Browse photography offers
* View offer details, offer photos, and offer reviews
* Create booking requests for available offers
* View own bookings
* Edit pending bookings
* Cancel pending or approved bookings
* Create reviews for approved or completed bookings
* Edit and delete own reviews

### Photographer

* Create, edit, and delete own offers
* View own offers
* Add, edit, and delete photos for own offers
* Set an offer cover photo
* View bookings related to own offers
* Approve or reject pending booking requests
* View reviews related to own offers

---

## Implemented Features

### Authentication

* User registration
* User login and logout
* Session-based authentication with `HttpSession`
* BCrypt password hashing
* Photographer and Client account types

### Offers

* Public offer listing
* Offer details page
* Photographer-only create, edit, and delete operations for owned offers
* My Offers page for photographers
* Availability, price, duration, location, and cover photo display

### Photos

* Offer photo gallery
* Photographer portfolio page
* Photo details page
* Photographer-only create, edit, and delete operations for photos on owned offers
* Set photo as offer cover photo
* Photo images are currently stored as image URLs

### Bookings

* Booking creation from an offer
* My Bookings page
* Booking details page
* Client edit for pending bookings
* Client cancellation for pending or approved bookings
* Photographer approval or rejection for pending bookings
* Booking statuses: `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`, `CANCELLED`

### Reviews

* Offer reviews page
* Review details page
* My Reviews page
* Five-star rating system
* Written review comments
* Client review creation for approved or completed bookings
* One review per booking
* Review author can edit or delete own reviews

### UI

* Thymeleaf templates for all main CRUD flows
* Shared header, navbar, and footer fragments
* User-friendly confirmation dialogs
* Custom 404 page
* Responsive card and action button styling

---

## Domain Model

### User

Represents an application user.

Key fields and relationships:

* `id`
* `firstName`
* `lastName`
* `username`
* `email`
* `password`
* `role`
* `profileImageUrl`
* `isActive`
* `offers`
* `bookings`
* `reviews`
* `createdAt`
* `updatedAt`

### Offer

Represents a photography offer published by a photographer.

Key fields and relationships:

* `id`
* `title`
* `description`
* `price`
* `durationHours`
* `location`
* `isAvailable`
* `photographer`
* `coverPhoto`
* `photos`
* `bookings`
* `reviews`
* `createdAt`
* `updatedAt`

### Photo

Represents an image attached to an offer.

Key fields and relationships:

* `id`
* `title`
* `imageUrl`
* `description`
* `offer`
* `createdAt`
* `updatedAt`

### Booking

Represents a booking request for an offer.

Key fields and relationships:

* `id`
* `eventDate`
* `location`
* `notes`
* `status`
* `client`
* `offer`
* `review`
* `createdAt`
* `updatedAt`

### Review

Represents feedback connected to a booking and offer.

Key fields and relationships:

* `id`
* `rating`
* `comment`
* `author`
* `offer`
* `booking`
* `createdAt`
* `updatedAt`

---

## Main Routes

### Public and Auth

* `GET /` - landing page
* `GET /login` - login page
* `POST /login` - login action
* `GET /register` - registration page
* `POST /register` - registration action
* `GET /home` - authenticated home page
* `POST /logout` - logout action

### Offers

* `GET /offers` - all offers
* `GET /offers/{id}` - offer details
* `GET /my-offers` - photographer's own offers
* `GET /offers/create` - create offer page
* `POST /offers/create` - create offer
* `GET /offers/edit/{id}` - edit offer page
* `POST /offers/edit/{id}` - update offer
* `POST /offers/delete/{id}` - delete offer

### Photos

* `GET /portfolio` - photographer portfolio photos
* `GET /photos` - redirect to portfolio
* `GET /offers/{offerId}/photos` - photos for an offer
* `GET /photos/{id}` - photo details
* `GET /photos/create/{offerId}` - create photo page
* `POST /photos/create/{offerId}` - create photo
* `GET /photos/edit/{id}` - edit photo page
* `POST /photos/edit/{id}` - update photo
* `POST /photos/delete/{id}` - delete photo
* `POST /photos/{id}/cover` - set offer cover photo

### Bookings

* `GET /bookings` - bookings visible to the current user
* `GET /my-bookings` - current user's bookings
* `GET /bookings/{id}` - booking details
* `GET /bookings/create/{offerId}` - create booking page
* `POST /bookings/create/{offerId}` - create booking
* `GET /bookings/edit/{id}` - edit booking page
* `POST /bookings/edit/{id}` - update booking
* `POST /bookings/delete/{id}` - cancel booking
* `POST /bookings/{id}/approve` - approve booking
* `POST /bookings/{id}/reject` - reject booking

### Reviews

* `GET /reviews` - reviews visible to the current user
* `GET /reviews/{id}` - review details
* `GET /offers/{offerId}/reviews` - reviews for an offer
* `GET /reviews/create/{bookingId}` - create review page
* `POST /reviews/create/{bookingId}` - create review
* `GET /reviews/edit/{id}` - edit review page
* `POST /reviews/edit/{id}` - update review
* `POST /reviews/delete/{id}` - delete review

---

## Authorization Rules

* Only the owner photographer can manage an offer.
* Only the owner photographer can manage photos for an offer.
* Only the owner photographer can set an offer cover photo.
* A user cannot book their own offer.
* A booking is visible to the booking client and to the photographer who owns the booked offer.
* Only the booking client can edit a pending booking.
* Only the booking client can cancel a pending or approved booking.
* Only the offer photographer can approve or reject a pending booking.
* Only the booking client can review an approved or completed booking.
* A booking can have only one review.
* Only the review author can edit or delete the review.

---

## Validation

All create and edit forms use server-side validation with Jakarta Bean Validation and display field-level errors through Thymeleaf.

Examples:

* Offers require title, description, price, duration, location, and availability.
* Photos require a non-empty image URL value.
* Bookings require a future event date and location.
* Reviews require a rating from 1 to 5 and a comment.
* Users require valid credentials and account data.

---

## Configuration

The project uses one shared configuration file and environment-specific profile files:

* `src/main/resources/application.properties`
* `src/main/resources/application-dev.properties`
* `src/main/resources/application-prod.properties`

The shared `application.properties` defines the application name and uses the development profile as the default profile:

```properties
spring.application.name=Photo Marketplace Application
spring.profiles.default=dev
```

Production can still be selected explicitly through the command line, IDE run configuration, or environment.

Required settings:

* MySQL JDBC URL
* MySQL username
* MySQL password
* `app.photographer.password`
* `app.client.password`

The application creates two seed users only when the user table is empty:

* Photographer username: `photographer`
* Photographer email: `photographer@example.com`
* Client username: `client`
* Client email: `client@example.com`

The seed passwords come from `app.photographer.password` and `app.client.password`.

---

## Installation and Run

1. Clone the repository.
2. Create or start a MySQL database server.
3. Configure `application-dev.properties` or `application-prod.properties`.
4. Build the project:

   ```bash
   mvn clean package
   ```

5. Run the application with the default development profile:

   ```bash
   mvn spring-boot:run
   ```

6. Or run the packaged JAR with the default development profile:

   ```bash
   java -jar target/photo-marketplace-application-0.0.1-SNAPSHOT.jar
   ```

   To run with the production profile, pass it explicitly:

   ```bash
   java -jar target/photo-marketplace-application-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

7. Open the application:

   ```text
   http://localhost:8080
   ```

---

## User Workflow

### Photographer

1. Register or log in as a photographer.
2. Create photography offers.
3. Add photos to offers.
4. Set cover photos.
5. Review incoming booking requests.
6. Approve or reject pending bookings.
7. View reviews related to owned offers.

### Client

1. Register or log in as a client.
2. Browse available offers.
3. View offer details, photos, and reviews.
4. Create a booking request.
5. Edit the booking while it is pending.
6. Cancel a pending or approved booking if needed.
7. Leave a review after an approved or completed booking.

---

## Current Media Handling

Photos are currently saved by URL through the `imageUrl` field. The application does not yet upload binary image files to local storage or cloud storage.

---

## Future Enhancements

### Marketplace

* Advanced search and filtering
* Offer categories
* Photographer verification
* Favorites and bookmarks

### Scheduling

* Availability calendar
* Time slot management
* Booking conflict prevention
* Automatic completion of past approved bookings

### Communication

* Internal messaging
* Email notifications
* Booking reminders

### Payments

* Online payment integration
* Deposits and invoices
* Refund management

### Media

* Local image upload support
* Cloud storage integration
* Image resizing and optimization
* Watermarking

### Administration

* Admin role
* Admin dashboard
* User moderation
* Review moderation
* Platform analytics
