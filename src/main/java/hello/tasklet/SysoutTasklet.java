package hello.tasklet;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by michal on 10.08.16.
 */
public class SysoutTasklet implements Tasklet {

    private Resource directory;

    public Resource getDirectory() {
        return directory;
    }

    public void setDirectory(Resource directory) {
        this.directory = directory;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String directory = (String) chunkContext.getStepContext().getJobExecutionContext().get("directory");
        this.directory = new PathResource(Paths.get(directory));
        File[] files = this.directory.getFile().listFiles(((dir, name) -> name.contains("outputFile.json")));
        Path outputFile = Files.createFile(Paths.get("summary.txt"));
        Files.write(outputFile, files.toString().getBytes());
        return RepeatStatus.FINISHED;
    }

}
