package org.mvel3.completion;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.completion.Mvel3CompletionHelper.completionItemStrings;

/**
 * Tests for Mvel3CompletionHelper completion suggestions in various positions
 * within a simple complete mvel block.
 */
class Mvel3CompletionHelperTest {

    @Test
    void testSimpleBlock() {
        String text = "{ l#ArrayList#removeRange(0, 10); }";

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion at the beginning of the block
//        caretPosition.setLine(0);
//        caretPosition.setCharacter(2);
//        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
//        assertThat(completionItemStrings(result)).contains("var", "int", "return");

        // Test completion before 'rule '
        caretPosition.setLine(0);
        caretPosition.setCharacter(4);
        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);

        System.out.println(completionItemStrings(result));

        assertThat(completionItemStrings(result)).contains("ArrayList");
    }

    @Test
    void testCreateCompletionItem() {
        CompletionItem item = Mvel3CompletionHelper.createCompletionItem("test", org.eclipse.lsp4j.CompletionItemKind.Keyword);
        
        assertThat(item.getLabel()).isEqualTo("test");
        assertThat(item.getInsertText()).isEqualTo("test");
        assertThat(item.getKind()).isEqualTo(org.eclipse.lsp4j.CompletionItemKind.Keyword);
    }
}
