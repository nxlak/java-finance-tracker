package backend.academy.scrapper.controller;

import backend.academy.scrapper.controller.dto.TransactionRequestDto;
import backend.academy.scrapper.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{chatId}/transactions")
    public void addTransaction(
            @PathVariable Long chatId,
            @RequestBody TransactionRequestDto dto
    ) {
        transactionService.addTransaction(chatId, dto);
    }

}
