# Cloud Deployment Guide - Upgrade Finance (Free Tier)

This guide provides step-by-step instructions to deploy **Upgrade Finance** to the cloud using 100% free-tier services, allowing your Android app and Web dashboard to sync remotely.

---

## 1. Cloud Database Setup (Free PostgreSQL)

We will use **Neon** or **Supabase** for a permanent, free serverless PostgreSQL database.

### Option A: Neon.tech (Recommended)
1. Go to [Neon.tech](https://neon.tech/) and sign up for a free account.
2. Create a new project named `upgrade-finance`.
3. Copy the **Connection String** from the dashboard. It will look like:
   `postgresql://neondb_owner:password@ep-cool-name.aws.neon.tech/neondb?sslmode=require`
4. Keep this connection string ready.

### Option B: Supabase
1. Go to [Supabase.com](https://supabase.com/) and create a free account.
2. Create a new project. Set your database password.
3. Once provisioned, go to **Settings -> Database** and copy the **URI Connection String** under the transaction pooler.

---

## 2. Deploy Spring Boot Backend (Free Web Service on Render)

We will use **Render** to deploy the Spring Boot API from a GitHub repository.

### Step 1: Push Code to GitHub
1. Create a private GitHub repository.
2. Push your `upgrade-finance` repository contents to GitHub.

### Step 2: Deploy on Render
1. Go to [Render.com](https://render.com/) and sign up.
2. Click **New +** -> **Web Service**.
3. Connect your GitHub account and select your `upgrade-finance` repository.
4. Configure the service:
   - **Name**: `upgrade-finance-backend`
   - **Root Directory**: `backend`
   - **Runtime**: `Docker` or `Java`
     - *If choosing Java*:
       - **Build Command**: `mvn clean package -DskipTests`
       - **Start Command**: `java -jar target/backend-0.0.1-SNAPSHOT.jar`
   - **Instance Type**: `Free`
5. Click **Advanced** and add the following **Environment Variables**:
   - `SPRING_DATASOURCE_URL` = (Your Neon/Supabase connection string, change `postgresql://` to `jdbc:postgresql://`)
   - `SPRING_DATASOURCE_USERNAME` = (Your DB username)
   - `SPRING_DATASOURCE_PASSWORD` = (Your DB password)
   - `SPRING_DATASOURCE_DRIVER` = `org.postgresql.Driver`
   - `SPRING_JPA_DIALECT` = `org.hibernate.dialect.PostgreSQLDialect`
   - `JWT_SECRET` = (A secure random 256-bit string)
   - `GEMINI_API_KEY` = (Your Google Gemini API Key)
6. Click **Deploy Web Service**. Render will compile and launch the backend. Copy the generated URL (e.g., `https://upgrade-finance-backend.onrender.com`).

*Note: Free tier services on Render sleep after 15 minutes of inactivity and take ~50 seconds to spin up on the first request.*

---

## 3. Deploy React Web Dashboard (Free Static Hosting on Vercel)

Vercel is the fastest, free way to deploy static Vite sites.

### Steps
1. Go to [Vercel.com](https://vercel.com/) and sign up.
2. Click **Add New** -> **Project**.
3. Select your GitHub repository.
4. Configure the Vite deployment:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `web`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Under **Environment Variables**, configure the API base url:
   - `VITE_API_BASE` = `https://upgrade-finance-backend.onrender.com/api` (Replace with your Render URL)
6. Click **Deploy**. Vercel will build and host your website for free (e.g. `https://upgrade-finance.vercel.app`).

---

## 4. Connect the Android App to Cloud

Now update the Android app configuration to connect to your live Render backend instead of localhost.

### Steps
1. Open the project in Android Studio.
2. Open **[SyncWorker.java](file:///Users/varun/.gemini/antigravity/scratch/upgrade-finance/android/app/src/main/java/com/upgradefinance/worker/SyncWorker.java)**.
3. Replace the local loopback URL `http://10.0.2.2:8080/api/sync` with your Render API endpoint:
   `https://upgrade-finance-backend.onrender.com/api/sync`
4. Build the APK (`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`).
5. Transfer the APK to your mobile phone and install it.
6. Register an account on the mobile app, and log in to the same account on your Vercel web dashboard. Your transactions will now sync automatically in the cloud!
