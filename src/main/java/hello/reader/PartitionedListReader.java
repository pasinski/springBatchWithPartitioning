package hello.reader;

import com.db.json.Portfolio;
import com.db.json.Transaction;
import hello.service.FakeTransactionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;

import javax.batch.runtime.StepExecution;
import java.util.List;

/**
 * Created by michal on 08.08.16.
 */
public class PartitionedListReader implements ItemReader<List<Transaction>>{

    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionedListReader.class);

    private Portfolio portfolio;
    private FakeTransactionsService fakeTransactionsService;

    private List<Transaction> transactions;

    public PartitionedListReader(FakeTransactionsService fakeTransactionsService){
        this.fakeTransactionsService = fakeTransactionsService;
    }

    @Override
    public List<Transaction> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        LOGGER.debug("reading objects");
        if(this.transactions != null){
            List<Transaction> transactions = this.transactions;
            this.transactions = null;
            return transactions;
        }
        return null;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }


    @BeforeStep
    public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
        LOGGER.info("beforestep in reader");
        ExecutionContext ctx = stepExecution.getExecutionContext();
        this.setPortfolio((Portfolio) ctx.get("portfolio"));
        this.transactions = fakeTransactionsService.getTransactionsForPortfolio(this.portfolio);
    }

    public ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
        return null;
    }
}
