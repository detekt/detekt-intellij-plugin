package io.gitlab.arturbosch.detekt.config;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Dmytro Primshyts
 */
public class DetektConfigurationForm {
	private JCheckBox enableDetekt;
	private JCheckBox checkTestSources;
	private TextFieldWithBrowseButton configurationFilePath;
	private JPanel myMainPanel;
	private JCheckBox treatAsErrors;
	private JCheckBox enableFormatting;

	private DetektConfigStorage detektConfigStorage;

	@NotNull
	public JComponent createPanel(@NotNull DetektConfigStorage detektConfigStorage) {
		this.detektConfigStorage = detektConfigStorage;

		myMainPanel.setBorder(IdeBorderFactory.createTitledBorder("Detekt settings"));

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

		return myMainPanel;
	}

	public void apply() {
		detektConfigStorage.setEnableDetekt(enableDetekt.isSelected());
		detektConfigStorage.setEnableFormatting(enableFormatting.isSelected());
		detektConfigStorage.setCheckTestFiles(checkTestSources.isSelected());
		detektConfigStorage.setTreatAsError(treatAsErrors.isSelected());
		detektConfigStorage.setRulesPath(configurationFilePath.getText());
	}

	public void reset() {
		enableDetekt.setSelected(detektConfigStorage.getEnableDetekt());
		enableFormatting.setSelected(detektConfigStorage.getEnableFormatting());
		checkTestSources.setSelected(detektConfigStorage.getCheckTestFiles());
		treatAsErrors.setSelected(detektConfigStorage.getTreatAsError());
		configurationFilePath.setText(detektConfigStorage.getRulesPath());
	}

	public boolean isModified() {
		return !Comparing.equal(detektConfigStorage.getEnableDetekt(), enableDetekt.isSelected())
				|| !Comparing.equal(detektConfigStorage.getEnableFormatting(), enableFormatting.isSelected())
				|| !Comparing.equal(detektConfigStorage.getCheckTestFiles(), checkTestSources.isSelected())
				|| !Comparing.equal(detektConfigStorage.getTreatAsError(), treatAsErrors.isSelected())
				|| !Comparing.equal(detektConfigStorage.getRulesPath(), configurationFilePath.getText());
	}
}
