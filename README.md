# Wear OS Sensor Monitor

An Android Wear OS application that collects smartwatch sensor data, including heart rate and accelerometer readings, and transfers it to a companion Android mobile application for real-time monitoring and visualisation.

## Project Overview

This project demonstrates how wearable devices can collect health and movement data and communicate with an Android mobile application.

The Wear OS application collects sensor readings from the smartwatch, while the companion mobile application receives and displays the information through the Wear OS Data Layer API.

## Features

- **Heart Rate Monitoring:** Collects live heart-rate readings in BPM using Android Health Services.
- **Accelerometer Monitoring:** Captures X, Y and Z movement values using Android SensorManager.
- **Real-Time Data Transfer:** Transfers sensor readings from the smartwatch to the companion mobile application.
- **Live Data Visualisation:** Displays heart rate and accelerometer readings on both devices.
- **Wear OS Support:** Designed for a round smartwatch display.

## Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- Wear OS
- Android Health Services
- SensorManager
- Wear OS Data Layer API

## System Architecture

The application consists of two main modules:

**1. Wear OS Application**
- Collects heart-rate data from the smartwatch.
- Captures accelerometer readings.
- Sends sensor data to the connected mobile application.

**2. Android Mobile Application**
- Receives sensor data from the smartwatch.
- Displays heart-rate information.
- Visualises accelerometer readings.

### Data Flow

Smartwatch Sensors → Wear OS Application → Data Layer API → Android Mobile Application → Data Visualisation

## Screenshots

Add screenshots of your smartwatch and mobile application here.

## Installation and Setup

1. Clone this repository.
2. Open the project in Android Studio.
3. Allow Gradle to sync.
4. Start a Wear OS emulator and an Android phone emulator, or connect compatible physical devices.
5. Run the Wear OS application.
6. Run the companion mobile application.
7. Test the sensor data collection and communication between both devices.

## Learning Outcomes

Through this project, I gained practical experience with:

- Android wearable application development.
- Collecting sensor data from smartwatch hardware.
- Working with Android Health Services and SensorManager.
- Communicating between Wear OS and Android applications.
- Processing and displaying real-time sensor readings.
- Developing user interfaces using Jetpack Compose.

