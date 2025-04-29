package backend.academy.scrapper.service;

import backend.academy.scrapper.controller.dto.TransactionRequestDto;
import backend.academy.scrapper.entity.Transaction;
import backend.academy.scrapper.entity.User;
import backend.academy.scrapper.exceptions.FinanceTrackerException;
import backend.academy.scrapper.repository.TransactionRepository;
import backend.academy.scrapper.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

}
