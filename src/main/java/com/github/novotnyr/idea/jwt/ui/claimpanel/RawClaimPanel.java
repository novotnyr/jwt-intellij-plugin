package com.github.novotnyr.idea.jwt.ui.claimpanel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.novotnyr.idea.jwt.core.RawClaim;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;

import java.awt.BorderLayout;
import java.io.IOException;

public class RawClaimPanel extends AbstractClaimPanel<RawClaim, TreeNode> {
    private ObjectMapper objectMapper;

    private EditorTextField claimValueEditorTextField;

    public RawClaimPanel(RawClaim value) {
        super(value);
    }

    @Override
    protected void initialize() {
        this.objectMapper = new ObjectMapper();

        super.claimValueTextField.setEditable(false);
        setLayout(new BorderLayout());

        Document document = EditorFactory.getInstance().createDocument(toString(super.value.getValue()));
        this.claimValueEditorTextField = new EditorTextField(document, null, JsonFileType.INSTANCE, false ,false) {
            @Override
            protected EditorEx createEditor() {
                EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
                EditorHighlighter editorHighlighter = highlighterFactory.createEditorHighlighter(null, JsonFileType.INSTANCE);
                EditorEx editor = super.createEditor();
                editor.setHighlighter(editorHighlighter);
                return editor;
            }
        };
        add(new JBScrollPane(this.claimValueEditorTextField), BorderLayout.CENTER);
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
            return new ObjectMapper().readTree(this.claimValueEditorTextField.getText());
        } catch (IOException e) {
            return MissingNode.getInstance();
        }
    }

    @Override
    public ValidationInfo getValidationInfo() {
        try {
            new ObjectMapper().readTree(this.claimValueEditorTextField.getText());
            return null;
        } catch (IOException e) {
            return new ValidationInfo("Value is not a valid JSON", this.claimValueEditorTextField);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.claimValueEditorTextField.setEnabled(!readOnly);
    }
}
