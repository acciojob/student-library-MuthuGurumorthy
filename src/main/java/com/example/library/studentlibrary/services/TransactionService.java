package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        Book book = bookRepository5.findById(bookId).orElseThrow(()->new Exception("Book is either unavailable or not present"));
        if(book==null || !book.isAvailable()) throw new Exception("Book is either unavailable or not present");

        Card card = cardRepository5.findById(cardId).orElseThrow(()->new Exception("Card is invalid"));
        if(card==null || card.getCardStatus().toString().equals("DEACTIVATED")) throw new Exception("Card is invalid");

        if(card.getBooks().size()>=max_allowed_books) throw new Exception("Book limit has reached for this card");

        book.setAvailable(false);
        bookRepository5.save(book);

        Transaction transaction = Transaction.builder().card(card).
                book(book).
                transactionStatus(TransactionStatus.SUCCESSFUL).
                isIssueOperation(true).build();
        transactionRepository5.save(transaction);

        return transaction.getTransactionId();
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        Date transactionDate = transaction.getTransactionDate();
        Date currentDate = new Date();
        long timeDiff = currentDate.getTime()-transactionDate.getTime();
        long dayDiff = (timeDiff / (1000 * 60 * 60 * 24)) % 365;
        int totalFineAmount = fine_per_day * (int) dayDiff;

        transaction.getBook().setAvailable(true);
        bookRepository5.save(transaction.getBook());

        Transaction returnBookTransaction = Transaction.builder().transactionStatus(TransactionStatus.SUCCESSFUL).
                card(transaction.getCard()).book(transaction.getBook()).
                fineAmount(totalFineAmount).isIssueOperation(true).build();
        transactionRepository5.save(returnBookTransaction);

        return returnBookTransaction;
    }
}