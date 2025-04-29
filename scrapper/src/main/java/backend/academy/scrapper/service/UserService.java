package backend.academy.scrapper.service;

import backend.academy.scrapper.entity.Transaction;
import backend.academy.scrapper.entity.User;
import backend.academy.scrapper.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(Long chatId) {

        if (!isRegistered(chatId)) {
            List<Transaction> transactionList = new ArrayList<>();
            User user = new User(chatId, transactionList);
            userRepository.save(user);
        }

    }

    private boolean isRegistered(Long chatId) {
        return userRepository.existsById(chatId);
    }

}
