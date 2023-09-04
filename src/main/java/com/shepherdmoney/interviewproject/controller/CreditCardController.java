package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    CreditCardRepository creditCardRepository;

    @Autowired
    UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        Optional<User> user = userRepository.findById(payload.getUserId());

        if (user.isEmpty()){
            return new ResponseEntity<>(payload.getUserId(), HttpStatus.NOT_FOUND);
        }

        CreditCard creditCard = new CreditCard();
        creditCard.setOwner(user.get());
        creditCard.setNumber(payload.getCardNumber());

        newCard = creditCardRepository.save(creditCard);

        return new ResponseEntity.ok("Successfully Added Credit Card " + creditCard.getId() + " for user " + payload.getUserId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()){
            return new ResponseEntity(new ArrayList<>(), HttpStatus.NOT_FOUND);
        }

        List<CreditCard> allCreditCards = user.get().getCreditCards().toList();

        List<CreditCardView> creditCardViews = new ArrayList<>();

        for (CreditCard creditCard : allCreditCards){
            creditCardViews.add(new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber()));
        }

        return new ResponseEntity.ok(creditCardViews);

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCard.isEmpty()){
            return new ResponseEntity.badRequest().build();
        }
        return new ResponseEntity.ok(creditCard.get().getOwner().getId());
    }

    @PostMapping("/credit-card:update-balance")
    public SomeEnityData postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        Arrays.sort(payload, Comparator.comparing(UpdateBalancePayload::getTransactionTime));

        for (UpdateBalancePayload update : payload){
            Optional<CreditCard> opCreditCard = creditCardRepository.findByNumber(update.getCreditCardNumber());

            if (opCreditCard.isEmpty()) {
                return new ResponseEntity.badRequest().build();
            }

            CreditCard creditCard = opCreditCard.get();
            List<BalanceHistory> balanceHistories = creditCard.getBalanceHistories();

            if (balanceHistories.isEmpty()) {
                balanceHistories.add(new BalanceHistory(update.getTransactionTime(), update.getTransactionAmount()));
            }
            else {
                int i = balanceHistories.size() - 1;
                while (update.getTransactionTime().compareTo(balanceHistories.getDate()) > 0) {
                    i--;
                }

                LocalDate curBalanceHistoryDate = balanceHistories.get(i).getDate().truncatedTo(ChronoUnit.DAYS);
                double curBalance = balanceHistories.get(i).getBalance();

                while (i >= 0) {
                    BalanceHistory balanceHistory = balanceHistories.get(i);
                    LocalDate balanceHistoryDate = balanceHistory.getDate().truncatedTo(ChronoUnit.DAYS);

                    if (curBalanceHistoryDate.isEqual(balanceHistoryDate)) {
                        balanceHistory.setBalance(balanceHistory.getBalance() + update.getTransactionAmount());
                        curBalance = balanceHistory.getBalance();
                        curBalanceHistoryDate = curBalanceHistoryDate.plusDays(1);
                        i--;
                    }
                    else if (curBalanceHistoryDate.isBefore(balanceHistoryDate)) {
                        balanceHistories.add(i, new BalanceHistory(curBalanceHistoryDate.atStartOfDay(ZoneId.systemDefault()).toInstant(), curBalance));
                        curBalanceHistoryDate = curBalanceHistoryDate.plusDays(1);
                        i--;
                    }
                }
            }
            creditCardRepository.save(creditCard);
        }
        return ResponseEntity.ok().build();
    }
    
}
