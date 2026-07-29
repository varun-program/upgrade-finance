# Upgrade Finance - AI-Powered Personal Finance Tracker

Upgrade Finance is a modern, privacy-first, offline-first personal finance tracker. It automates expense tracking by reading incoming banking/UPI SMS and payment app push notifications on Android, categorizing them via localized rules or Gemini AI, and synchronizing data seamlessly across all devices to a responsive web dashboard.

## 🚀 Key Features

* **Zero Manual Effort**: Automatically parses UPI & banking text alerts (SBI, HDFC, ICICI, etc.) and notifications (Google Pay, PhonePe, Paytm).
* **Smart Deduplication**: Merges SMS and notification records using reference numbers and timestamps to prevent duplicates.
* **Offline-First & Local Mode**: Run the entire app locally in your browser storage or Android Room DB without registering.
* **Bi-directional Sync**: Seamless delta sync with last-write-wins conflict resolution.
* **AI Financial Assistant**: Ask questions like *"How much did I spend on food this month?"* using a free Gemini API key integration.
* **Budgets & Goals**: Set limits with 50%/80%/100% alerts, and track saving targets with progress and ETA.

---

## 🛠️ Technology Stack

* **Android App**: Java, Android Studio, MVVM, Room DB, Material Design 3, WorkManager, SMS BroadcastReceiver, Notification Listener.
* **Backend REST API**: Java, Spring Boot, Spring Security, JWT Auth, JPA, PostgreSQL / MySQL / H2.
* **Web Dashboard**: React, Vite, Tailwind CSS, React Router, Axios, Recharts.
* **AI Engine**: Google Gemini API (Free Tier).

---

## 📁 Directory Structure

```
upgrade-finance/
├── android/      # Android Mobile Application (MVVM, Room)
├── backend/      # Spring Boot REST API
├── web/          # React Dashboard (Vite, Tailwind, Recharts)
├── database/     # DB schema design and migration scripts
├── api/          # OpenAPI spec / REST API contract definitions
├── docs/         # Installation, deployment, and architecture docs
└── shared/       # Shared default configurations (default-rules.json)
```

---

## 📖 Documentation

* [Installation Guide](docs/installation.md)
* [Architecture and Design Doc](docs/architecture.md)
* [REST API Specifications](api/api-spec.yaml)
* [Shared Rules Config](shared/default-rules.json)
* [Database Schema SQL](database/schema.sql)
