package io.gitlab.arturbosch.detekt.config;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Dmytro Primshyts
 */
public class DetektConfigurationForm {
    private JCheckBox enableDetekt;
    private JCheckBox buildUponDefaultConfig;
    private JCheckBox failFast;
    private TextFieldWithBrowseButton configurationFilePath;
    private JPanel myMainPanel;
    private JCheckBox treatAsErrors;
    private JCheckBox enableFormatting;
    private JCheckBox autoCorrect;
    private TextFieldWithBrowseButton baselineFilePath;

    private DetektConfigStorage detektConfigStorage;

    @NotNull
    public JComponent createPanel(@NotNull DetektConfigStorage detektConfigStorage) {
        this.detektConfigStorage = detektConfigStorage;

        myMainPanel.setBorder(IdeBorderFactory.createTitledBorder("Detekt settings"));

        enableDetekt.addChangeListener(changeEvent -> {
            boolean enabled = enableDetekt.isSelected();
            buildUponDefaultConfig.setEnabled(enabled);
            failFast.setEnabled(enabled);
            configurationFilePath.setEnabled(enabled);
            treatAsErrors.setEnabled(enabled);
            enableFormatting.setEnabled(enabled);
            autoCorrect.setEnabled(enabled);
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
                "Detekt rules file",
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        baselineFilePath.addBrowseFolderListener(
                "",
                "Detekt baseline file",
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        return myMainPanel;
    }

    public void apply() {
        detektConfigStorage.setEnableDetekt(enableDetekt.isSelected());
        detektConfigStorage.setEnableFormatting(enableFormatting.isSelected());
        detektConfigStorage.setAutoCorrect(autoCorrect.isSelected());
        detektConfigStorage.setBuildUponDefaultConfig(buildUponDefaultConfig.isSelected());
        detektConfigStorage.setFailFast(failFast.isSelected());
        detektConfigStorage.setTreatAsError(treatAsErrors.isSelected());
        detektConfigStorage.setRulesPath(configurationFilePath.getText());
        detektConfigStorage.setBaselinePath(baselineFilePath.getText());
    }

    public void reset() {
        enableDetekt.setSelected(detektConfigStorage.getEnableDetekt());
        enableFormatting.setSelected(detektConfigStorage.getEnableFormatting());
        autoCorrect.setSelected(detektConfigStorage.getAutoCorrect());
        buildUponDefaultConfig.setSelected(detektConfigStorage.getBuildUponDefaultConfig());
        failFast.setSelected(detektConfigStorage.getFailFast());
        treatAsErrors.setSelected(detektConfigStorage.getTreatAsError());
        configurationFilePath.setText(detektConfigStorage.getRulesPath());
        baselineFilePath.setText(detektConfigStorage.getBaselinePath());
    }

    public boolean isModified() {
        return !Comparing.equal(detektConfigStorage.getEnableDetekt(), enableDetekt.isSelected())
                || !Comparing.equal(detektConfigStorage.getEnableFormatting(), enableFormatting.isSelected())
                || !Comparing.equal(detektConfigStorage.getAutoCorrect(), autoCorrect.isSelected())
                || !Comparing.equal(detektConfigStorage.getBuildUponDefaultConfig(), buildUponDefaultConfig.isSelected())
                || !Comparing.equal(detektConfigStorage.getFailFast(), failFast.isSelected())
                || !Comparing.equal(detektConfigStorage.getTreatAsError(), treatAsErrors.isSelected())
                || !Comparing.equal(detektConfigStorage.getRulesPath(), configurationFilePath.getText())
                || !Comparing.equal(detektConfigStorage.getBaselinePath(), baselineFilePath.getText());
    }
}
