# Job Scraper (MyThings)

A Quarkus-based job scraper and aggregation service that fetches jobs from specified sites, filters them by keywords, deduplicates them, and emails a daily digest.

## 🛠️ Prerequisites

- **Java 21**
- **Gradle** (or use the included wrapper `./gradlew`)
- **GraalVM** (optional, for local native compilation)

## ⚙️ Configuration (.env)

Before running or deploying the application, you must configure your environment variables. 
1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Fill out your `.env` file with your Brevo API credentials and recipient details:
   ```env
   BREVO_API_KEY=your_brevo_api_key_here
   BREVO_SENDER_NAME=My Job Scraper
   BREVO_SENDER_EMAIL=your-verified-sender@example.com
   MYJOBS_EMAIL_RECIPIENT=your-recipient@gmail.com
   ```

*(Note: `.env` is ignored by git to protect your secrets.)*

## 💻 Local Development (Windows & Linux)

To run the application locally in development mode (with live reloading):

**On Windows:**
```cmd
gradlew.bat quarkusDev
```

**On Linux / macOS:**
```bash
./gradlew quarkusDev
```

The app will start and automatically pick up the variables from your `.env` file.

Note: Quarkus loads `.env` from the *current working directory* as a config source (it does **not** export values to `System.getenv()` / `printenv`).

## 🚀 Deployment: Linux VPS (Native Binary - Recommended)

We use a GitHub Actions CI pipeline that automatically provisions a native Linux binary whenever you push to `main`. This binary is incredibly memory-efficient and perfect for a small VPS (around 20MB-30MB RAM usage).

Note: On DigitalOcean Droplets, outbound SMTP ports 25, 465, and 587 are blocked by default (so Gmail SMTP will time out). Use a third-party email provider/API instead: https://docs.digitalocean.com/support/why-is-smtp-blocked/

### Step-by-Step Linux VPS Deployment:

1. **Push your code** to the `main` branch to trigger the GitHub Action.
2. **SSH into your VPS**.
3. **Create your `.env` file** on the VPS in the directory where you'll run the app.
4. **Download the binary** from your GitHub repository releases:
   ```bash
   # Replace YOUR_GITHUB_USERNAME and YOUR_REPO_NAME and with the version (e.g. v261.0.1)
   wget https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/releases/download/v261.0.1/mythings-native-runner
   ```
5. **Make it executable**:
   ```bash
   chmod +x mythings-native-runner
   ```
6. **Run it** in the background:
   ```bash
   nohup ./mythings-native-runner > application.log 2>&1 &
   ```
   *(Quarkus reads `.env` from the current working directory at startup. Run the binary from the folder that contains `.env`.)*

## 🏁 Deployment: Windows Machine

Because the GitHub CI process compiles the native binary exclusively for Linux (`ubuntu-latest`), you cannot run that specific binary directly on Windows without WSL.

To deploy on a standard Windows machine, you should use the standard Java JVM run:

### Step-by-Step Windows Deployment:

1. Ensure **Java 21** is installed on the target Windows machine.
2. Build the standard JAR locally (or copy the project over):
   ```cmd
   gradlew.bat build
   ```
3. Copy the `build/quarkus-app/` folder to your deployment directory.
4. Ensure your `.env` file is in the same directory. 
5. Run the application via Java:
   ```cmd
   java -jar build/quarkus-app/quarkus-run.jar
   ```

Alternatively, if you strictly need a Native Windows executable, you will need to install **GraalVM** and **Visual Studio Build Tools (MSVC)** on your Windows machine and compile it locally via:
```cmd
gradlew.bat build -Dquarkus.package.type=native
```
