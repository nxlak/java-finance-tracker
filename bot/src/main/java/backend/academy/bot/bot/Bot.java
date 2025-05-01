package backend.academy.bot.bot;

import backend.academy.bot.BotConfig;
import backend.academy.bot.dto.TransactionRequest;
import backend.academy.bot.dto.TransactionResponse;
import backend.academy.bot.util.BotState;
import backend.academy.scrapper.exceptions.FinanceTrackerException;
import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class Bot {

    private final TelegramBot bot;
    private final WebClient webClient;

    @Getter
    @Setter
    private Map<Long, BotState> userState = new HashMap<>();

    private final Map<Long, TransactionDraft> userDrafts = new HashMap<>();
    private final Map<Long, LocalDateTime> userStatPeriod = new HashMap<>();
    private final Map<Long, Long> editingTransactionIds = new HashMap<>();

    private final List<String> categories = List.of(
            "🍔 Еда", "🚌 Транспорт", "🎮 Развлечения",
            "💊 Здоровье", "👗 Одежда", "🎓 Образование",
            "💰 Сбережения", "📚 Прочее"
    );

    @Autowired
    public Bot(BotConfig botConfig, WebClient webClient) {
        this.bot = new TelegramBot(botConfig.telegramToken());
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleUpdate(Update update) {
        if (update.message() != null && update.message().text() != null) {
            long chatId = update.message().chat().id();
            String messageText = update.message().text();
            BotState state = userState.getOrDefault(chatId, BotState.IDLE);

            switch (messageText) {
                case "/start" -> {
                    try {
                        webClient.post()
                                .uri("/users/{chatId}", chatId)
                                .retrieve()
                                .toBodilessEntity()
                                .block();
                        sendMenu(chatId);
                        userState.put(chatId, BotState.IDLE);
                    } catch (WebClientResponseException e) {
                        sendMessage(chatId, "Ошибка при запуске бота: " + e.getStatusCode());
                    } catch (Exception e) {
                        sendMessage(chatId, "Неизвестная ошибка при запуске бота.");
                    }
                }
                case "➕ Добавить трату" -> {
                    sendMessage(chatId, "Введите сумму:");
                    userState.put(chatId, BotState.WAITING_FOR_AMOUNT);
                    userDrafts.put(chatId, new TransactionDraft());
                }
                case "📊 Статистика" -> {
                    sendPeriodSelectionKeyboard(chatId);
                    userState.put(chatId, BotState.IDLE);
                }
                case "❌ Отмена" -> {
                    userState.put(chatId, BotState.IDLE);
                    userDrafts.remove(chatId);
                    userStatPeriod.remove(chatId);
                    sendMessage(chatId, "Действие отменено.");
                    sendMenu(chatId);
                }
                case "✏ Изменить" -> {
                    userState.put(chatId, BotState.EDITING_TRANSACTION);
                    sendMessage(chatId, "Введите *ID* транзакции, которую вы хотите изменить:");
                }
                case "🗑 Удалить" -> {
                    userState.put(chatId, BotState.DELETING_TRANSACTION);
                    sendMessage(chatId, "Введите *ID* транзакции, которую вы хотите удалить:");
                }
                default -> handleState(chatId, messageText, state);
            }
        } else if (update.callbackQuery() != null) {
            handleCallback(update.callbackQuery());
        }
    }

    private void handleState(long chatId, String messageText, BotState state) {
        TransactionDraft draft = userDrafts.getOrDefault(chatId, new TransactionDraft());

        switch (state) {
            case WAITING_FOR_AMOUNT -> {
                try {
                    draft.setAmount(Double.parseDouble(messageText));
                    sendCategoryMenu(chatId);
                    userState.put(chatId, BotState.WAITING_FOR_CATEGORY);
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Введите корректную сумму.");
                }
            }
            case WAITING_FOR_CATEGORY -> {
                draft.setCategory(messageText);
                removeCustomKeyboard(chatId, "Категория выбрана: " + messageText);
                sendDescriptionPrompt(chatId);
                userState.put(chatId, BotState.WAITING_FOR_DESCRIPTION);
            }
            case WAITING_FOR_DESCRIPTION -> {
                if (!messageText.equals("Пропустить")) {
                    draft.setDescription(messageText);
                }
                sendDateSelection(chatId);
                userState.put(chatId, BotState.WAITING_FOR_DATE);
            }
            case WAITING_FOR_DATE -> {
                try {
                    draft.setTimestamp(LocalDateTime.parse(messageText));
                    confirmTransaction(chatId, draft);
                    userState.put(chatId, BotState.IDLE);
                    userDrafts.remove(chatId);
                    sendMenu(chatId);
                } catch (Exception e) {
                    sendMessage(chatId, "Неверный формат даты. Используйте: 2025-04-29T14:00");
                }
            }
            case WAITING_FOR_STAT_CATEGORY -> {
                removeCustomKeyboard(chatId, "Категория выбрана: " + messageText);
                if ("📊 Все категории".equals(messageText)) {
                    sendCategoryStats(chatId, null); // передаём null = все категории
                } else {
                    sendCategoryStats(chatId, messageText);
                }
                userState.put(chatId, BotState.IDLE);
                userStatPeriod.remove(chatId);
                sendMenu(chatId);
            }
            case DELETING_TRANSACTION -> {
                try {
                    webClient.delete()
                            .uri("/{chatId}/transactions/{transactionId}", chatId, messageText)
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                    sendMessage(chatId, "✅ Транзакция удалена");
                } catch (FinanceTrackerException e) {
                    sendMessage(chatId, "❌ Ошибка при удалении транзакции");
                }
            }
            case EDITING_TRANSACTION -> {
                editingTransactionIds.put(chatId, Long.valueOf(messageText));

                TransactionResponse tr = webClient.get()
                        .uri("/{chatId}/transactions/{transactionId}", chatId, messageText)
                        .retrieve()
                        .bodyToMono(TransactionResponse.class)
                        .block();

                TransactionDraft draftEdit = new TransactionDraft();
                draftEdit.setAmount(tr.amount());
                draftEdit.setCategory(tr.category());
                draftEdit.setDescription(tr.description());
                draftEdit.setTimestamp(tr.timestamp());
                userDrafts.put(chatId, draftEdit);

                sendMessage(chatId, "Введите новую сумму (текущая: " + tr.amount() + "):");
                userState.put(chatId, BotState.WAITING_FOR_AMOUNT);
            }
            default -> sendMessage(chatId, "Неизвестная команда. Введите /start");
        }
    }

    private void handleCallback(CallbackQuery query) {
        long chatId = query.message().chat().id();
        String data = query.data();

        switch (data) {
            case "skip_description" -> {
                userDrafts.get(chatId).setDescription("");
                sendDateSelection(chatId);
                userState.put(chatId, BotState.WAITING_FOR_DATE);
            }
            case "date_today" -> {
                userDrafts.get(chatId).setTimestamp(LocalDateTime.now().withHour(0).withMinute(0));
                confirmTransaction(chatId, userDrafts.get(chatId));
                userState.put(chatId, BotState.IDLE);
                userDrafts.remove(chatId);
                sendMenu(chatId);
            }
            case "stat_today", "stat_week", "stat_month", "stat_year" -> {
                LocalDateTime from = switch (data) {
                    case "stat_today" -> LocalDateTime.now().withHour(0).withMinute(0);
                    case "stat_week" -> LocalDateTime.now().minusWeeks(1);
                    case "stat_month" -> LocalDateTime.now().minusMonths(1);
                    case "stat_year" -> LocalDateTime.now().minusYears(1);
                    default -> LocalDateTime.now();
                };
                userStatPeriod.put(chatId, from);
                sendCategoryMenu(chatId, "Выберите категорию для статистики:", BotState.WAITING_FOR_STAT_CATEGORY, true);
            }
            case String callbackData when callbackData.startsWith("edit_") -> {
                long transactionId = Long.parseLong(callbackData.substring(5));

                editingTransactionIds.put(chatId, transactionId);

                TransactionResponse tr = webClient.get()
                        .uri("/{chatId}/transactions/{transactionId}", chatId, transactionId)
                        .retrieve()
                        .bodyToMono(TransactionResponse.class)
                        .block();

                TransactionDraft draft = new TransactionDraft();
                draft.setAmount(tr.amount());
                draft.setCategory(tr.category());
                draft.setDescription(tr.description());
                draft.setTimestamp(tr.timestamp());
                userDrafts.put(chatId, draft);

                sendMessage(chatId, "Введите новую сумму (текущая: " + tr.amount() + "):");
                userState.put(chatId, BotState.WAITING_FOR_AMOUNT);
            }

            case String callbackData when callbackData.startsWith("delete_") -> {
                long transactionId = Long.parseLong(callbackData.substring(7));

                webClient.delete()
                        .uri("/{chatId}/transactions/{transactionId}", chatId, transactionId)
                        .retrieve()
                        .toBodilessEntity()
                        .block();

                sendMessage(chatId, "✅ Транзакция удалена");
            }
            default -> {
                sendMessage(chatId, "Неизвестная команда. Пожалуйста, используйте кнопки меню.");
                userState.put(chatId, BotState.IDLE);
            }
        }

        bot.execute(new AnswerCallbackQuery(query.id()));
    }

    private void confirmTransaction(long chatId, TransactionDraft draft) {
        try {
            TransactionRequest transactionRequest = new TransactionRequest(
                    draft.getAmount(),
                    draft.getCategory(),
                    draft.getDescription(),
                    draft.getTimestamp()
            );

            Long transactionId = editingTransactionIds.get(chatId);

            if (transactionId != null) {
                webClient.post()
                        .uri("/{chatId}/transactions/{transactionId}", chatId, transactionId)
                        .bodyValue(transactionRequest)
                        .retrieve()
                        .toBodilessEntity()
                        .block();

                TransactionResponse tr = webClient.get()
                        .uri("/{chatId}/transactions/{transactionId}", chatId, transactionId)
                        .retrieve()
                        .bodyToMono(TransactionResponse.class)
                        .block();

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                        new InlineKeyboardButton("✏ Изменить").callbackData("edit_" + tr.transactionId()),
                        new InlineKeyboardButton("🗑 Удалить").callbackData("delete_" + tr.transactionId())
                );
                sendMessage(chatId, "✏ Трата обновлена.\n\n" +
                        "*ID:* " + transactionId + "\n" +
                        "*Сумма:* " + draft.getAmount() + "\n" +
                        "*Категория:* " + draft.getCategory() + "\n" +
                        "*Описание:* " + draft.getDescription() + "\n" +
                        "*Дата:* " + draft.getTimestamp(), markup);

                editingTransactionIds.remove(chatId);
            } else {
                transactionId = webClient.post()
                        .uri("/{chatId}/transactions", chatId)
                        .bodyValue(transactionRequest)
                        .retrieve()
                        .bodyToMono(Long.class)
                        .block();

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                        new InlineKeyboardButton("✏ Изменить").callbackData("edit_" + transactionId),
                        new InlineKeyboardButton("🗑 Удалить").callbackData("delete_" + transactionId)
                );
                sendMessage(chatId, "✅ Трата добавлена:\n\n" +
                        "*ID:* " + transactionId + "\n" +
                        "*Сумма:* " + draft.getAmount() + "\n" +
                        "*Категория:* " + draft.getCategory() + "\n" +
                        "*Описание:* " + draft.getDescription() + "\n" +
                        "*Дата:* " + draft.getTimestamp(), markup);
            }

        } catch (FinanceTrackerException e) {
            sendMessage(chatId, "Ошибка при добавлении транзакции");
        }
    }

    private void sendMenu(long chatId) {
        Keyboard keyboard = new ReplyKeyboardMarkup(
                new String[]{"➕ Добавить трату", "📊 Статистика"},
                new String[]{"✏ Изменить", "🗑 Удалить"},
                new String[]{"❌ Отмена"}
        ).resizeKeyboard(true);
        sendMessage(chatId, "Выберите действие:", keyboard);
    }

    private void sendPeriodSelectionKeyboard(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                new InlineKeyboardButton("📅 Сегодня").callbackData("stat_today"),
                new InlineKeyboardButton("🗓 Неделя").callbackData("stat_week"),
                new InlineKeyboardButton("📆 Месяц").callbackData("stat_month"),
                new InlineKeyboardButton("📈 Год").callbackData("stat_year")
        );
        sendMessage(chatId, "Выберите период для статистики:", markup);
    }

    private void sendCategoryMenu(long chatId) {
        sendCategoryMenu(chatId, "Выберите категорию:", BotState.WAITING_FOR_CATEGORY, false);
    }

    private void sendCategoryMenu(long chatId, String prompt, BotState newState, boolean includeAllButton) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < categories.size(); i += 2) {
            if (i + 1 < categories.size()) {
                rows.add(new String[]{categories.get(i), categories.get(i + 1)});
            } else {
                rows.add(new String[]{categories.get(i)});
            }
        }
        if (includeAllButton) {
            rows.add(new String[]{"📊 Все категории"});
        }
        Keyboard keyboard = new ReplyKeyboardMarkup(rows.toArray(String[][]::new)).resizeKeyboard(true);
        sendMessage(chatId, prompt, keyboard);
        userState.put(chatId, newState);
    }

    private void sendDescriptionPrompt(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить").callbackData("skip_description")
        );
        sendMessage(chatId, "Введите описание или нажмите 'Пропустить':", markup);
    }

    private void sendDateSelection(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                new InlineKeyboardButton("📅 Сегодня").callbackData("date_today")
        );
        sendMessage(chatId, "Введите дату в формате *2025-04-29T14:00* или выберите:", markup);
    }

    private void sendCategoryStats(long chatId, String category) {
        LocalDateTime from = userStatPeriod.getOrDefault(chatId, LocalDateTime.now().minusDays(7));

        try {
            if (category == null) {
                sendMessage(chatId, "📊 Общая статистика с " + from.toLocalDate());

                List<TransactionResponse> transactions = webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/{chatId}/common-info")
                                .queryParam("time", from)
                                .build(chatId))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<TransactionResponse>>() {})
                        .block();

                if (transactions == null || transactions.isEmpty()) {
                    sendMessage(chatId, "За указанный период нет транзакций.");
                    return;
                }

                Map<String, BigDecimal> stats = new HashMap<>();

                for (TransactionResponse tr : transactions) {
                    stats.merge(tr.category(), BigDecimal.valueOf(tr.amount()), BigDecimal::add);
                }

                StringBuilder sb = new StringBuilder();
                BigDecimal sum = BigDecimal.ZERO;
                for (Map.Entry<String, BigDecimal> entry : stats.entrySet()) {
                    sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("₽\n");
                    sum = sum.add(entry.getValue());
                }

                sb.append("\nОбщая сумма: ").append(sum);

                sendMessage(chatId, sb.toString());
            } else {
                sendMessage(chatId, "📊 Статистика с " + from.toLocalDate() + " по категории: " + category);

                List<TransactionResponse> response = webClient.post()
                        .uri("/info/{chatId}/category/{categoryName}?time={time}", chatId, category, from)
                        .retrieve()
                        .bodyToFlux(TransactionResponse.class)
                        .collectList()
                        .block();

                if (response == null || response.isEmpty()) {
                    sendMessage(chatId, "Нет трат по категории за указанный период.");
                    return;
                }

                BigDecimal total = response.stream()
                        .map(tr -> BigDecimal.valueOf(tr.amount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                StringBuilder message = new StringBuilder("Общая сумма: " + total + "₽" + "\n\nТраты:\n");
                for (TransactionResponse tr : response) {
                    message.append("• ")
                            .append("*[ID:* ")
                            .append(tr.transactionId())
                            .append("*]*")
                            .append(" *Сумма*: ")
                            .append(tr.amount())
                            .append(" ₽")
                            .append(" — ").append(tr.description())
                            .append(" (").append(tr.timestamp().toLocalDate()).append(")\n");
                }

                sendMessage(chatId, message.toString());
            }

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении статистики.");
            e.printStackTrace();
        }
    }


    public void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(long chatId, String text, Keyboard keyboard) {
        SendMessage request = new SendMessage(chatId, text)
                .parseMode(ParseMode.Markdown)
                .disableWebPagePreview(true);
        if (keyboard != null) request.replyMarkup(keyboard);
        SendResponse response = bot.execute(request);
        if (!response.isOk()) {
            System.out.println("Ошибка отправки сообщения: " + response.description());
        }
    }

    private void removeCustomKeyboard(long chatId, String message) {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove(true);
        SendMessage request = new SendMessage(chatId, message)
                .replyMarkup(remove);
        bot.execute(request);
    }

    private static class TransactionDraft {
        private Double amount;
        private String category;
        private String description;
        private LocalDateTime timestamp;

        public void setAmount(Double amount) { this.amount = amount; }
        public void setCategory(String category) { this.category = category; }
        public void setDescription(String description) { this.description = description; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public Double getAmount() { return amount; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}