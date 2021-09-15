# Noisycamp.com

Source code of the [NoisyCamp](https://noisycamp.com) platform.

## Setup

These environment variables are required to be defined for the web-app to launch:

    AUTH_CRYPTER_KEY='change-me'
    AUTH_SIGNER_KEY='change-me'
    FACEBOOK_CLIENT_ID='change-me'
    FACEBOOK_CLIENT_SECRET='change-me'
    FACEBOOK_PIXEL_ID='change-me'
    GOOGLE_CLIENT_ID='change-me'
    GOOGLE_CLIENT_SECRET='change-me'
    JDBC_DATABASE_URL='jdbc:postgres://host:port/database?user=user&password=password&sslmode=require'
    MAPBOX_TOKEN='change-me'
    PANELBEAR_SITEID='change-me'
    PLAY_APP_SECRET='change-me'
    RSS2JSON_API_KEY='change-me'
    SENDGRID_API_KEY='change-me'
    STRIPE_PUBLIC_KEY='change-me'
    STRIPE_SECRET_KEY='change-me'
    STRIPE_WEBHOOK_SECRET='change-me'

## Run

Uses `sbt` to compile and run the web-app:

    sbt run