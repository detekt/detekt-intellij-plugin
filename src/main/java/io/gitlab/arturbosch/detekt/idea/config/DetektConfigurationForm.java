package io.gitlab.arturbosch.detekt.idea.config;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.PathUtil;
import io.gitlab.arturbosch.detekt.idea.DetektBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class DetektConfigurationForm {

    private final DetektBundle detektBundle = DetektBundle.INSTANCE;

    private JCheckBox enableDetekt;
    private JCheckBox buildUponDefaultConfig;
    private JCheckBox enableAllRules;
    private TextFieldWithBrowseButton configurationFilePath;
    private JPanel myMainPanel;
    private JCheckBox treatAsErrors;
    private JCheckBox enableFormatting;
    private TextFieldWithBrowseButton baselineFilePath;
    private TextFieldWithBrowseButton pluginPaths;
    private JPanel configurationFilesPanel;
    private JPanel baselineFilePanel;
    private JPanel pluginJarsPanel;

    private DetektConfigStorage detektConfigStorage;
    private final Project project;

    public DetektConfigurationForm(Project project) {
        this.project = project;

        configurationFilesPanel.setBorder(IdeBorderFactory.createTitledBorder(
            detektBundle.message("detekt.configuration.configurationFiles.title")));
        baselineFilePanel.setBorder(IdeBorderFactory.createTitledBorder(
            detektBundle.message("detekt.configuration.baselineFile.title")));
        pluginJarsPanel.setBorder(IdeBorderFactory.createTitledBorder(
            detektBundle.message("detekt.configuration.pluginJars.title")));
    }

    @NotNull
    public JComponent createPanel(@NotNull DetektConfigStorage detektConfigStorage) {
        this.detektConfigStorage = detektConfigStorage;

        myMainPanel.setBorder(IdeBorderFactory.createTitledBorder("Detekt Settings"));

        enableDetekt.addChangeListener(changeEvent -> {
            boolean enabled = enableDetekt.isSelected();
            buildUponDefaultConfig.setEnabled(enabled);
            enableAllRules.setEnabled(enabled);
            configurationFilePath.setEnabled(enabled);
            treatAsErrors.setEnabled(enabled);
            enableFormatting.setEnabled(enabled);
            baselineFilePath.setEditable(enabled);
            pluginPaths.setEditable(enabled);
        });

        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false);

        configurationFilePath.addBrowseFolderListener(
                detektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
                detektBundle.message("detekt.configuration.configurationFiles.dialog.description"),
                project,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        baselineFilePath.addBrowseFolderListener(
            detektBundle.message("detekt.configuration.baselineFile.dialog.title"),
            detektBundle.message("detekt.configuration.baselineFile.dialog.description"),
                project,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        pluginPaths.addActionListener(action -> {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor();
            final VirtualFile[] files = FileChooser.chooseFiles(descriptor, pluginPaths, project, null);
            if (files.length > 0) {
                final StringBuilder builder = new StringBuilder();
                for (VirtualFile file : files) {
                    if (builder.length() > 0) {
                        builder.append(File.pathSeparator);
                    }
                    builder.append(FileUtil.toSystemDependentName(file.getPath()));
                }
                pluginPaths.setText(builder.toString());
            }
        });

        return myMainPanel;
    }

    public void apply() {
        detektConfigStorage.setEnableDetekt(enableDetekt.isSelected());
        detektConfigStorage.setEnableFormatting(enableFormatting.isSelected());
        detektConfigStorage.setBuildUponDefaultConfig(buildUponDefaultConfig.isSelected());
        detektConfigStorage.setEnableAllRules(enableAllRules.isSelected());
        detektConfigStorage.setTreatAsError(treatAsErrors.isSelected());
        detektConfigStorage.setConfigPaths(configurationFilePath.getText());
        detektConfigStorage.setBaselinePath(baselineFilePath.getText());
        detektConfigStorage.setPluginPaths(pluginPaths.getText());
    }

    public void reset() {
        enableDetekt.setSelected(detektConfigStorage.getEnableDetekt());
        enableFormatting.setSelected(detektConfigStorage.getEnableFormatting());
        buildUponDefaultConfig.setSelected(detektConfigStorage.getBuildUponDefaultConfig());
        enableAllRules.setSelected(detektConfigStorage.getEnableAllRules());
        treatAsErrors.setSelected(detektConfigStorage.getTreatAsError());
        configurationFilePath.setText(detektConfigStorage.getConfigPaths());
        baselineFilePath.setText(detektConfigStorage.getBaselinePath());
        pluginPaths.setText(detektConfigStorage.getPluginPaths());
    }

    public boolean isNotModified() {
        return Objects.equals(detektConfigStorage.getEnableDetekt(), enableDetekt.isSelected())
                && Objects.equals(detektConfigStorage.getEnableFormatting(), enableFormatting.isSelected())
                && Objects.equals(detektConfigStorage.getBuildUponDefaultConfig(), buildUponDefaultConfig.isSelected())
                && Objects.equals(detektConfigStorage.getEnableAllRules(), enableAllRules.isSelected())
                && Objects.equals(detektConfigStorage.getTreatAsError(), treatAsErrors.isSelected())
                && Objects.equals(detektConfigStorage.getConfigPaths(), configurationFilePath.getText())
                && Objects.equals(detektConfigStorage.getBaselinePath(), baselineFilePath.getText())
                && Objects.equals(detektConfigStorage.getPluginPaths(), pluginPaths.getText());
    }
}
