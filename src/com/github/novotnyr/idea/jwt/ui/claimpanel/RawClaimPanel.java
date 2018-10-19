package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.novotnyr.idea.jwt.core.RawClaim;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import java.io.IOException;

public class RawClaimPanel extends AbstractClaimPanel<RawClaim, TreeNode> {
    private ObjectMapper objectMapper;

    private JTextArea claimValueTextArea;

    public RawClaimPanel(RawClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        this.objectMapper = new ObjectMapper();

        super.claimValueTextField.setEditable(false);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.claimValueTextArea = new JTextArea(15, 45);
        add(new JBScrollPane(this.claimValueTextArea));
        this.claimValueTextArea.setText(toString(super.value.getValue()));
    }

    private String toString(TreeNode treeNode) {
        try {
            if (treeNode instanceof MissingNode) {
                return "";
            }
            return this.objectMapper.writeValueAsString(treeNode);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    @Override
    public TreeNode getClaimValue() {
        try {
            return new ObjectMapper().readTree(this.claimValueTextArea.getText());
        } catch (IOException e) {
            return MissingNode.getInstance();
        }
    }

    @Override
    public ValidationInfo getValidationInfo() {
        try {
            new ObjectMapper().readTree(this.claimValueTextArea.getText());
            return null;
        } catch (IOException e) {
            return new ValidationInfo("Value is not a valid JSON", this.claimValueTextArea);
        }
    }
}
