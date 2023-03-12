package io.graphoenix.gradle.task;

import io.graphoenix.core.config.BannerConfig;
import io.graphoenix.core.context.BeanContext;
import io.leego.banana.Ansi;
import io.leego.banana.BananaUtils;
import io.leego.banana.Font;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.BannerUtil.BANNER_FILE_NAME;

public class GenerateBannerTask extends BaseTask {

    @TaskAction
    public void GenerateIntrospectionSQL() {
        init();
        BannerConfig bannerConfig = BeanContext.get(BannerConfig.class);
        try {
            SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            String resourcePath = sourceSet.getResources().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\resource")).getAsPath();
            Ansi[] styles = Stream.of(bannerConfig.getArgs()).map(Ansi::get).toArray(Ansi[]::new);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(
                    BananaUtils.bananansi(
                            bannerConfig.getText(),
                            Font.get(bannerConfig.getFont()),
                            styles
                    )
            );
            if (bannerConfig.getLines() != null) {
                Arrays.stream(bannerConfig.getLines())
                        .map(line -> Ansi.ansify(line, styles))
                        .forEach(line -> stringBuilder.append(System.lineSeparator()).append(line));
            }
            stringBuilder.append(System.lineSeparator());
            Files.writeString(
                    Path.of(resourcePath.concat(File.separator).concat(BANNER_FILE_NAME)),
                    stringBuilder.toString()
            );
        } catch (IOException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
