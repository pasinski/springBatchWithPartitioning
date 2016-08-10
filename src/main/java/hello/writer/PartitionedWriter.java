package hello.writer;

import com.db.json.Portfolio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by pn9160 on 09.08.2016.
 */


public class PartitionedWriter extends ItemStreamSupport implements ItemWriter<Portfolio> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionedWriter.class);

    private static final AtomicInteger integer = new AtomicInteger();

    private FlatFileItemWriter<Portfolio> delegate;

    public PartitionedWriter(FlatFileItemWriter<Portfolio> delegate) {
        LOGGER.debug("Creating wirter wit delegate {}", delegate);
        this.delegate = delegate;
        final ObjectMapper objectMapper = new ObjectMapper();
        delegate.setLineAggregator(portfolio -> {
            try {
                return objectMapper.writeValueAsString(portfolio);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        delegate.setHeaderCallback(writer1 -> writer1.append("[\n"));
        delegate.setFooterCallback(writer1 -> writer1.append("]\n"));
        this.delegate.setResource(new PathResource(Paths.get("out.txt"+integer.getAndIncrement())));
    }

    @Override
    public void open(ExecutionContext executionContext) {
        super.open(executionContext);
        this.delegate.open(executionContext);
    }

    @Override
    public void close() {
        super.close();
        this.delegate.close();
    }

    @Override
    public void update(ExecutionContext executionContext) {
        super.update(executionContext);
        this.delegate.update(executionContext);
    }


    @Override
    public void write(List<? extends Portfolio> list) throws Exception {
        LOGGER.debug("about to write a list {} to a delegate {}", list != null ? printPortfolios(list): "null", this.delegate);
        this.delegate.write(list);
    }

    private Object printPortfolios(List<? extends Portfolio> list) {
        return list.stream().map(item -> String.valueOf(item.getFakeId()) + " " + item.getTransactions().size()).collect(Collectors.joining(","));
    }

    @BeforeStep
    void beforeStep(StepExecution stepExecution) {
        LOGGER.debug("********************* Before Step execution in writer");
        PathResource pathResource = new PathResource(Paths.get(String.format("outputFile.json.%s.txt", stepExecution.getExecutionContext().get("index"))));
        LOGGER.info("For stepExecution {} created resource {}", stepExecution, pathResource);
        this.delegate.setResource(pathResource);
    }

    @AfterStep
    void afterStep(StepExecution stepExecution){
        LOGGER.info("After step in writer");
    }

}
