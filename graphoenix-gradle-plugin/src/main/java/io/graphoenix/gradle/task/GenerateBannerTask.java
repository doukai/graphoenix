package io.graphoenix.gradle.task;

import io.graphoenix.core.config.BannerConfig;
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
import java.util.stream.Stream;

public class GenerateBannerTask extends BaseTask {

    @TaskAction
    public void GenerateIntrospectionSQL() {
        BannerConfig bannerConfig = getProject().getExtensions().findByType(BannerConfig.class);
        if (bannerConfig == null) {
            bannerConfig = new BannerConfig();
        }

        try {
            SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            String resourcePath = sourceSet.getResources().getSourceDirectories().getAsPath();
            Files.writeString(
                    Path.of(resourcePath.concat(File.separator).concat("banner.text")),
                    BananaUtils.bananansi(
                            bannerConfig.getText(),
                            Font.get(bannerConfig.getFont()),
                            Stream.of(bannerConfig.getArgs()).map(Ansi::get).toArray(Ansi[]::new)
                    )
            );
        } catch (IOException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
