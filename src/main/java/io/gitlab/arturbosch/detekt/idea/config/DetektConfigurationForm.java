package io.gitlab.arturbosch.detekt.idea.config;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.io.File;

public class DetektConfigurationForm {

    private JCheckBox enableDetekt;
    private JCheckBox buildUponDefaultConfig;
    private JCheckBox enableAllRules;
    private TextFieldWithBrowseButton configurationFilePath;
    private JPanel myMainPanel;
    private JCheckBox treatAsErrors;
    private JCheckBox enableFormatting;
    private TextFieldWithBrowseButton baselineFilePath;
    private TextFieldWithBrowseButton pluginPaths;

    private DetektConfigStorage detektConfigStorage;
    private final Project project;

    public DetektConfigurationForm(Project project) {
        this.project = project;
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
                "",
                "detekt rules file",
                project,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        baselineFilePath.addBrowseFolderListener(
                "",
                "detekt baseline file",
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
        detektConfigStorage.setRulesPath(configurationFilePath.getText());
        detektConfigStorage.setBaselinePath(baselineFilePath.getText());
        detektConfigStorage.setPluginPaths(pluginPaths.getText());
    }

    public void reset() {
        enableDetekt.setSelected(detektConfigStorage.getEnableDetekt());
        enableFormatting.setSelected(detektConfigStorage.getEnableFormatting());
        buildUponDefaultConfig.setSelected(detektConfigStorage.getBuildUponDefaultConfig());
        enableAllRules.setSelected(detektConfigStorage.getEnableAllRules());
        treatAsErrors.setSelected(detektConfigStorage.getTreatAsError());
        configurationFilePath.setText(detektConfigStorage.getRulesPath());
        baselineFilePath.setText(detektConfigStorage.getBaselinePath());
        pluginPaths.setText(detektConfigStorage.getPluginPaths());
    }

    public boolean isModified() {
        return !Comparing.equal(detektConfigStorage.getEnableDetekt(), enableDetekt.isSelected())
                || !Comparing.equal(detektConfigStorage.getEnableFormatting(), enableFormatting.isSelected())
                || !Comparing.equal(detektConfigStorage.getBuildUponDefaultConfig(), buildUponDefaultConfig.isSelected())
                || !Comparing.equal(detektConfigStorage.getEnableAllRules(), enableAllRules.isSelected())
                || !Comparing.equal(detektConfigStorage.getTreatAsError(), treatAsErrors.isSelected())
                || !Comparing.equal(detektConfigStorage.getRulesPath(), configurationFilePath.getText())
                || !Comparing.equal(detektConfigStorage.getBaselinePath(), baselineFilePath.getText())
                || !Comparing.equal(detektConfigStorage.getPluginPaths(), pluginPaths.getText());
    }
}
