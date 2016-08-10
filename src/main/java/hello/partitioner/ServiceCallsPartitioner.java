package hello.partitioner;

import com.db.json.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by michal on 08.08.16.
 */
public class ServiceCallsPartitioner implements Partitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCallsPartitioner.class);

    private List<Portfolio> portfolios;

    public ServiceCallsPartitioner(List<Portfolio> portfolios){
        this.portfolios = portfolios;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        if(portfolios.size() != gridSize){
            throw new IllegalStateException("Number of Portfolios does not match number of sub-steps");
        }
        Map<String, ExecutionContext> executionContextMap = new HashMap<>(portfolios.size());
        int i = 0;
        for(Portfolio portfolio : portfolios){
            ExecutionContext executionContext = new ExecutionContext();
            executionContext.put("portfolio", portfolio);
            executionContext.put("index", i++);
            executionContextMap.put(portfolio.getFakeId().toString(), executionContext);
        }
        LOGGER.debug("Created partitions {}", executionContextMap);
        return executionContextMap;
    }
}
