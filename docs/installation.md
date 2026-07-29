# Installation Guide - Upgrade Finance

Follow these steps to run the backend, React Web Dashboard, and Android App locally.

---

## 1. Backend REST API Setup

The Spring Boot backend can run using an in-memory H2 database out-of-the-box, or connect to a PostgreSQL instance.

### Prerequisites
* JDK 17 or higher
* Gradle (built-in wrapper) or Maven

### Configuration (`backend/src/main/resources/application.properties`)
```properties
# To use PostgreSQL, set these variables or specify them in environment:
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5412/upgradefinance
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secret

# Optional: Add your Google Gemini API Key to enable the AI financial assistant chat
GEMINI_API_KEY=your_free_tier_gemini_api_key
```

### Run Command
```bash
cd backend
./gradlew bootRun
```
The server will start at `http://localhost:8080`.

---

## 2. Web Dashboard Setup

### Prerequisites
* Node.js (v18+) and npm

### Run Command
```bash
cd web
npm install
npm run dev
```
The web dashboard will start at `http://localhost:5173`. Open this URL in your browser and click "Use Anonymous Local Mode" to start testing offline.

---

## 3. Android Mobile App Setup

### Prerequisites
* Android Studio (Koala or newer)
* Android SDK 34 (Target)

### Steps
1. Import the `android/` directory into Android Studio.
2. Build and run the app on an Android Emulator or physical device.
3. **SMS Permissions**: Grant SMS Read permissions to verify automated parsing of bank messages.
4. **Notification Access**: Go to Settings -> Notification Access and enable it for "Upgrade Finance" to read notifications from UPI payment apps.
5. **Backend Connection**: The app connects to `http://10.0.2.2:8080` (default emulator loopback alias) to reach the local Spring Boot backend.
