# Flight Tracker Android App

## Description

This Android application allows users to track real-time flight information using the AviationStack API and also calculates the historical average flight time between specific locations based on data collected over time.

The app demonstrates the use of modern Android development practices including Jetpack Compose for the UI, Kotlin Coroutines for asynchronous operations, WorkManager for background data collection, Room for local database storage, and Retrofit/Moshi for networking and JSON parsing.

## Features

* **Live Flight Tracking:**
    * Enter a flight code (e.g., "BA123") to view live flight data.
    * Displays key metrics like latitude, longitude, altitude, speed, heading, etc.
    * Data refreshes automatically (currently set to 1-minute intervals).
* **Average Flight Time Calculation:**
    * Calculates the average actual flight duration (including delays) between two pre-defined airports (currently hardcoded as **DEL -> BOM**).
    * Uses data collected and stored locally over a period (target is one week).
    * Demonstrates background data fetching, database storage, and data aggregation.
* **Background Data Collection:**
    * Uses `WorkManager` to schedule a daily background job.
    * The job fetches data for landed flights on the specified route (DEL -> BOM) for the past few days.
    * Stores relevant flight details (departure/arrival times, status) in a local Room database.

## Technologies Used

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Architecture:** MVVM (ViewModel, Repository, View) - basic implementation
* **Asynchronous Programming:** Kotlin Coroutines, Flow
* **State Management:** ViewModel, StateFlow, `collectAsStateWithLifecycle`
* **Networking:** Retrofit 2, OkHttp 3 (with Logging Interceptor)
* **JSON Parsing:** Moshi (with Codegen via KSP for performance)
* **Annotation Processing:** KSP (Kotlin Symbol Processing)
* **Database:** Room Persistence Library
* **Background Processing:** WorkManager
* **Dependency Management:** Gradle with Version Catalog (`libs.versions.toml`)
* **API:** AviationStack API (requires an access key)
* **Build System:** Gradle

## Setup Instructions

1.  **Prerequisites:**
    * Android Studio (latest stable version recommended)
    * JDK 11 or higher

2.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    cd <your-project-directory>
    ```

3.  **Get AviationStack API Key:**
    * Sign up for a free or paid plan at [AviationStack](https://aviationstack.com/).
    * Obtain your **Access Key**.

4.  **Configure API Key:**
    * In the **root directory** of the cloned project, create a file named `local.properties` (if it doesn't exist).
    * Add your API key to this file in the following format:
        ```properties
        AVIATIONSTACK_API_KEY="YOUR_ACTUAL_AVIATIONSTACK_API_KEY_HERE"
        ```
    * **Important:** Add `local.properties` to your project's `.gitignore` file to prevent accidentally committing your secret key.

5.  **Open in Android Studio:**
    * Open the project directory in Android Studio.

6.  **Sync Gradle:**
    * Allow Android Studio to sync the project with Gradle files. This will download all the required dependencies defined in `libs.versions.toml`. (Go to `File > Sync Project with Gradle Files` if needed).

7.  **Build Project (Crucial for KSP):**
    * Perform a **Clean Build**: `Build > Clean Project`.
    * Perform a **Rebuild Project**: `Build > Rebuild Project`.
    * *This step is essential for KSP to generate the necessary Moshi adapter code.*

## Running the App

1.  Connect an Android device (with USB debugging enabled) or start an Android Emulator.
2.  Select the device/emulator in Android Studio.
3.  Click the "Run 'app'" button (green play icon) or use the menu `Run > Run 'app'`.
4.  Android Studio will build, install, and launch the app on the selected device/emulator.

## Usage Notes

* **Live Tracking:**
    * Enter the full **Flight Code** (IATA Airline Code + Flight Number, e.g., `BA123`, `AI101`, `UA934`) in the input field.
    * Press "Track Flight". The app will fetch and display live data if the flight is currently active and found by the API.
    * The live data currently refreshes every **1 minute**. Be mindful that the AviationStack free tier has API call limits, and frequent refreshes might exhaust your quota quickly. Consider increasing the `REFRESH_INTERVAL_MS` constant in `FlightViewModel.kt` for longer-term use.
* **Average Flight Time Calculation:**
    * This feature calculates the average time for flights between **Delhi (DEL) and Mumbai (BOM)** based on historical data.
    * **IMPORTANT:** This feature **will show "Not enough data..." when you first run the app**.
    * The data required for this calculation is collected by a **background job (`FlightDataCollectorWorker`)** that is scheduled to run roughly **once per day** by `WorkManager`.
    * The background job needs time (potentially several days) to run successfully multiple times and accumulate enough flight records in the local database.
    * Press the "Avg Time DEL -> BOM" button to trigger the calculation. Once sufficient data is collected in the background, it will display the calculated average duration in minutes.
    * *(If using the `MainApplication` version with seeding enabled for demonstration, sample data is inserted on the first run, allowing the calculation to work immediately for DEL->BOM).*

## Project Structure (Simplified)
* **`app/src/main/`**: Main application source set.
    * **`java/com/example/flighttracker/`**: Root package for Kotlin code.
        * **`data/`**: Contains Room database components (Entity, DAO, Database), Repository, and data model classes (e.g., `LiveInfo`, `FlightRecord`).
        * **`network/`**: Holds Retrofit API service interface (`FlightApiService`) and client setup (`RetrofitClient`).
        * **`ui/`**: Contains Jetpack Compose UI (`FlightTrackingScreen.kt`), ViewModel (`FlightViewModel.kt`), and theme files (`theme/`).
        * **`worker/`**: Contains the `WorkManager` worker class (`FlightDataCollectorWorker.kt`).
        * `MainActivity.kt`: The main Activity hosting the Compose UI.
        * `MainApplication.kt` / `MainApplicationAlternative.kt`: Custom application class for initialization (e.g., WorkManager scheduling).
    * **`AndroidManifest.xml`**: Core Android manifest file (permissions, component declarations).
    * **`res/`**: Android resource directory (layouts, drawables, values, etc.).
* **`gradle/`**: Contains the Gradle Version Catalog (`libs.versions.toml`).
* **`local.properties`**: Stores the API key (Must be created manually and added to `.gitignore`).
* **`build.gradle.kts`**: Project-level Gradle build script.
* **`app/build.gradle.kts`**: Module-level Gradle build script (dependencies, plugins, app configuration).

## Future Improvements (TODO)

* Implement Dependency Injection (e.g., Hilt) for better dependency management.
* Allow users to select the origin and destination airports for average time calculation.
* Add more robust error handling and user feedback (e.g., specific API error messages).
* Improve UI/UX (e.g., show flight route on a map, better loading states).
* Add unit and integration tests.
* Refine background worker logic (e.g., more sophisticated date range handling, error retries).
* Implement data cleanup strategy for the database more robustly.
* Securely store the API key using secrets management tools instead of `local.properties` for production.



