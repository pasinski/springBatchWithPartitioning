package hello;

import com.db.json.Portfolio;
import com.db.json.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hello.partitioner.ServiceCallsPartitioner;
import hello.processor.PortfolioTransactionsProcessor;
import hello.reader.PartitionedListReader;
import hello.service.FakePortfoliosService;
import hello.service.FakeTransactionsService;
import hello.tasklet.SysoutTasklet;
import hello.writer.PartitionedWriter;
import javafx.animation.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.xml.TaskletParser;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.List;

@Configuration
@ComponentScan({"hello", "hello.*"})
@EnableBatchProcessing
@EnableAsync
public class PortfoliosBatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfoliosBatchConfiguration.class);

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;


    @Autowired
    public FakeTransactionsService fakeService;

    @Autowired
    public FakePortfoliosService fakePortfoliosService;

    @Autowired
    public DataSource dataSource;


    @Bean
    public TaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        return executor;
    }


    @Bean
    @StepScope
    public PartitionedListReader readerForFakeList(){
        return new PartitionedListReader(fakeService);
    }

    @Bean
    @StepScope
    public PortfolioTransactionsProcessor processorForFakeList() {
        return new PortfolioTransactionsProcessor();
    }

    @Bean
    @StepScope
    public PartitionedWriter writer() {
        FlatFileItemWriter<Portfolio> flatFileItemWriter = new FlatFileItemWriter<>();
        return new PartitionedWriter(flatFileItemWriter);
    }
    // end::readerwriterprocessor[]

    // tag::listener[]

    @Bean
    public JobExecutionListener listener() {
        return new JobCompletionNotificationListener(new JdbcTemplate(dataSource));
    }

    // end::listener[]

    // tag::jobstep[]
    @Bean
    public Job exportPortfoliosJob() {
        return jobBuilderFactory.get("exportPortfolios")
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .flow(masterStep())
                .next(sysoutStep())
                .end()
                .build();
    }


    @Bean
    @StepScope
    public StepExecutionListener masterExecutionListener(){
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {

            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                LOGGER.debug("finished executing masterstep {};", stepExecution.getStepName());
                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    @StepScope
    public Tasklet getSysoutTasklet(){
        return new SysoutTasklet();
    }

    @Bean
    public Step sysoutStep(){
        return stepBuilderFactory.get("sysoutStep")
                .tasklet(getSysoutTasklet())
                .build();
    }



    @Bean
    public Step masterStep(){
        List<Portfolio> portfolios = fakePortfoliosService.getPortfolios();
        return stepBuilderFactory.get("masterStep")
                .listener(masterExecutionListener())
                .partitioner(slave())
                .partitioner("slave", new ServiceCallsPartitioner(portfolios))
                .taskExecutor(taskExecutor())
                .gridSize(portfolios.size())
                .build();
    }

    @Bean
    @StepScope
    public StepExecutionListener slaveExecListener(){
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {

            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                LOGGER.debug("finished executing slave step {}", stepExecution.getStepName());
                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    public Step slave() {
        return stepBuilderFactory.get("slave")
                .listener(slaveExecListener())
                .<List<Transaction>, Portfolio> chunk(1)
                .reader(readerForFakeList())
                .processor(processorForFakeList())
                .writer(writer())
                .build();
    }
    // end::jobstep[]
}
