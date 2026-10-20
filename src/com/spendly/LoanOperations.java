
package com.spendly;

import java.util.List;

public interface LoanOperations {

    boolean addLoan(Loan loan);

    List<Loan> getAllLoans();

    boolean updateLoanStatus(int loanId, String status);

    boolean deleteLoan(int loanId);
}
