package com.github.novotnyr.idea.jwt.ui;

import com.github.novotnyr.idea.jwt.rs256.RsaUtils;
import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.EditorTextInsertHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Producer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collections;
import java.util.function.BiFunction;

public class FormatOnPasteEditorActionHandler extends EditorActionHandler implements EditorTextInsertHandler {
    private final EditorActionHandler delegateActionHandler;
    private final BiFunction<String, String, String> formatter;

    public FormatOnPasteEditorActionHandler(EditorActionHandler delegateActionHandler, BiFunction<String, String, String> formatter) {
        this.delegateActionHandler = delegateActionHandler;
        this.formatter = formatter;
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (!supports(editor)) {
            delegateActionHandler.execute(editor, caret, dataContext);
            return;
        }

        String clipboardString = getClipboardString(editor);
        if (clipboardString == null) {
            return;
        }

        final Document document = editor.getDocument();
        if (!EditorModificationUtil.requestWriting(editor)) {
            return;
        }
        document.startGuardedBlockChecking();
        CopyPasteManager.getInstance().stopKillRings();
        try {
            String pastedText = TextBlockTransferable.convertLineSeparators(editor, clipboardString, Collections
                    .emptyList());

            final CaretModel caretModel = editor.getCaretModel();
            final SelectionModel selectionModel = editor.getSelectionModel();

            String pastedAndFormattedText;
            if (this.formatter != null) {
                pastedAndFormattedText = this.formatter.apply(clipboardString, pastedText);
            } else {
                pastedAndFormattedText = pastedText;
            }

            ApplicationManager.getApplication().runWriteAction(
                    () -> {
                        EditorModificationUtil.insertStringAtCaret(editor, pastedAndFormattedText, false, true);
                    }
            );
           int length = pastedAndFormattedText.length();
            int offset = caretModel.getOffset() - length;
            if (offset < 0) {
                length += offset;
                offset = 0;
            }
            final RangeMarker bounds = document.createRangeMarker(offset, offset + length);

            caretModel.moveToOffset(bounds.getEndOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            selectionModel.removeSelection();

            if (bounds.isValid()) {
                caretModel.moveToOffset(bounds.getEndOffset());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                selectionModel.removeSelection();
                editor.putUserData(EditorEx.LAST_PASTED_REGION, TextRange.create(bounds));
            }
        } catch (ReadOnlyFragmentModificationException e) {
            EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document).handle(e);
        } finally {
            document.stopGuardedBlockChecking();
        }
    }

    protected boolean supports(Editor editor) {
        return true;
    }

    @Nullable
    public String getClipboardString(Editor editor) {
        try {
            CopyPasteManager manager = CopyPasteManager.getInstance();
            if (manager.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
                Transferable contents = manager.getContents();
                if (contents == null) {
                    return null;
                }
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException e) {
            editor.getComponent().getToolkit().beep();
        }
        return null;
    }

    public static String sanitize(@SuppressWarnings("unused") String rawClipboard, String formattedClipboard) {
        return RsaUtils.sanitizeWhitespace(formattedClipboard);
    }

    @Override
    public void execute(Editor editor, DataContext dataContext, @Nullable Producer<? extends Transferable> producer) {
        doExecute(editor, null, dataContext);
    }
}
