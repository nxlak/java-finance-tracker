package backend.academy.scrapper.service;

import backend.academy.scrapper.controller.dto.TransactionRequestDto;
import backend.academy.scrapper.controller.dto.TransactionResponseDto;
import backend.academy.scrapper.entity.Transaction;
import backend.academy.scrapper.entity.User;
import backend.academy.scrapper.exceptions.FinanceTrackerException;
import backend.academy.scrapper.repository.TransactionRepository;
import backend.academy.scrapper.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public void addTransaction(Long chatId, TransactionRequestDto transactionRequestDto) {

        User user = userRepository.findById(chatId).orElseThrow(() -> new FinanceTrackerException("User not found with chatId: " + chatId));

        Transaction transaction = new Transaction();
        transaction.setAmount(transactionRequestDto.amount());
        transaction.setCategory(transactionRequestDto.category());
        transaction.setDescription(transactionRequestDto.description());
        transaction.setCreatedAt(transactionRequestDto.timestamp());
        transaction.setUser(user);

        transactionRepository.save(transaction);

    }

    public List<Transaction> getCommonInfo(Long chatId, LocalDateTime time) {

        User user = userRepository.findById(chatId).orElseThrow(() -> new FinanceTrackerException("User not found with chatId: " + chatId));

        if (user.getTransactionList().isEmpty()) {
            throw new FinanceTrackerException("User " + chatId + " hasn't transactions");
        }

        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : user.getTransactionList()) {
            if (!transaction.getCreatedAt().isBefore(time)) {
                result.add(transaction);
            }
        }

        return result;
    }

    public List<Transaction> getTransactionsByCategory(Long chatId, String categoryName, LocalDateTime time) {

        User user = userRepository.findById(chatId).orElseThrow(() -> new FinanceTrackerException("User not found with chatId: " + chatId));

        if (user.getTransactionList().isEmpty()) {
            throw new FinanceTrackerException("User " + chatId + " hasn't transactions");
        }

        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : user.getTransactionList()) {
            if (!transaction.getCreatedAt().isBefore(time) && transaction.getCategory().equals(categoryName)) {
                result.add(transaction);
            }
        }

        return result;

    }

    public void remove(Long chatId, Long transactionId) {

        Transaction transaction = getTransaction(chatId, transactionId);

        transactionRepository.delete(transaction);

    }

    public void change(Long chatId, Long transactionId, TransactionRequestDto dto) {

        Transaction transaction = getTransaction(chatId, transactionId);

        transaction.setAmount(dto.amount());
        transaction.setCategory(dto.category());
        transaction.setDescription(dto.description());
        transaction.setCreatedAt(dto.timestamp());

        transactionRepository.save(transaction);

    }

    public TransactionResponseDto showTransaction(Long chatId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new FinanceTrackerException("No transaction with id " + transactionId));
        if (!(transaction.getUser().getChatId().equals(chatId))) {
            throw new FinanceTrackerException("User with id " + chatId + " doesn't have transaction with id " + transactionId);
        }

        TransactionResponseDto dto = new TransactionResponseDto(transaction.getAmount(), transaction.getCategory(),
                                                                transaction.getDescription(), transaction.getCreatedAt());

        return dto;
    }

    public Transaction getTransaction(Long chatId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new FinanceTrackerException("No transaction with id " + transactionId));
        if (!(transaction.getUser().getChatId().equals(chatId))) {
            throw new FinanceTrackerException("User with id " + chatId + " doesn't have transaction with id " + transactionId);
        }

        return transaction;
    }

}
