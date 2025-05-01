package backend.academy.scrapper.controller;

import backend.academy.scrapper.controller.dto.TransactionRequestDto;
import backend.academy.scrapper.controller.dto.TransactionResponseDto;
import backend.academy.scrapper.entity.Transaction;
import backend.academy.scrapper.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{chatId}/transactions")
    public Long addTransaction(
            @PathVariable Long chatId,
            @RequestBody TransactionRequestDto dto
    ) {
        return transactionService.addTransaction(chatId, dto);
    }

    @PostMapping("/{chatId}/common-info")
    public List<TransactionResponseDto> getCommonInfo(@PathVariable Long chatId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {

        return transactionService.getCommonInfo(chatId, time);

    }

    @PostMapping("/info/{chatId}/category/{categoryName}")
    public List<TransactionResponseDto> getTransactionsByCategory(@PathVariable Long chatId, @PathVariable String categoryName, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {

        return transactionService.getTransactionsByCategory(chatId, categoryName, time);

    }

    @DeleteMapping("/{chatId}/transactions/{transactionId}")
    public void deleteTransaction(@PathVariable Long chatId, @PathVariable Long transactionId) {
        transactionService.remove(chatId, transactionId);
    }

    @PostMapping("/{chatId}/transactions/{transactionId}")
    public void changeTransaction(@PathVariable Long chatId, @PathVariable Long transactionId, @RequestBody TransactionRequestDto dto) {
        transactionService.change(chatId, transactionId, dto);
    }

    @GetMapping("/{chatId}/transactions/{transactionId}")
    public TransactionResponseDto showTransaction(@PathVariable Long chatId, @PathVariable Long transactionId) {
        return transactionService.showTransaction(chatId, transactionId);
    }

}
