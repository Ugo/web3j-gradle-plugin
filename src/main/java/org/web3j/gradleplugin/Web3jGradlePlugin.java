package org.web3j.gradleplugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class Web3jGradlePlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getTasks().create("hello", SolcCompilerTask.class, (task) -> {

        });
    }
}
