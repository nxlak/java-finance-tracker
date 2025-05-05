# Finance Tracker

This project is a financial tracking system consisting of two modules:

* **scrapper** – a REST API service built with Spring Boot for managing users and transactions
* **bot** – a Telegram bot built with Spring Boot and the Java Telegram Bot API for user interaction via chat

## Getting Started

### Step 1. Clone the Repository

```bash
git clone https://github.com/nxlak/java-finance-tracker
cd java-finance-tracker
```

### Step 2. Configure Environment Variables

* `TELEGRAM_TOKEN` – your Telegram bot token

### Step 3. Build and Run

You can build both modules and run tests from the project root:

```bash
./mvnw clean verify
```

Or run each module individually:

```bash
# Run scrapper (port 8081)
cd scrapper
../mvnw spring-boot:run

# In another terminal, run bot (port 8080)
cd ../bot
../mvnw spring-boot:run
```

## Using the Telegram Bot

1. Find your bot in Telegram by its token or name and send the `/start` command.
2. Follow the menu.
3. When adding an expense, the bot will ask for the amount, category, description, and date.
4. To view statistics, you can choose a time period.
