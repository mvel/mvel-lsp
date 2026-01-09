package org.mvel3.completion;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.completion.Mvel3CompletionHelper.completionItemStrings;

class Mvel3CompletionHelperIncompleteCodeTest {

    @Test
    void emptyInput() {
        String text = "";
        Position caretPosition = new Position(0, 0);

        List<CompletionItem> result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("var", "int", "return");
    }

    @Test
    void incompleteRule_pattern() {
        String text = """
                class Foo {
                    rule R1 {
                        var a : /
                """;

        Position caretPosition = new Position();
        caretPosition.setLine(2);
        caretPosition.setCharacter(17); // After the '/'

        List<CompletionItem> result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).containsOnly("IDENTIFIER"); // datasource name is IDENTIFIER
    }

    @Test
    void incompleteRule_consequence_System() {
        String text = """
                class Foo {
                    rule R1 {
                        var a : /as,
                        do { System.
                """;

        Position caretPosition = new Position();
        caretPosition.setLine(3);
        caretPosition.setCharacter(20); // After the 'System.'

        List<CompletionItem> result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("out", "in", "gc"); // System fields, methods
    }

    @Test
    void incompleteRule_consequence_SystemOut() {
        String text = """
                class Foo {
                    rule R1 {
                        var a : /as,
                        do { System.out.
                """;

        Position caretPosition = new Position();
        caretPosition.setLine(3);
        caretPosition.setCharacter(24); // After the 'System.out.'

        List<CompletionItem> result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("println"); // System.out fields, methods
    }

    @Test
    void incompleteClass_consequence() {
        String text = """
                public class Foo {
                    public void bar() {
                        System.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after 'System.'
        caretPosition.setLine(2);
        caretPosition.setCharacter(15);
        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("out", "in", "gc"); // System fields, methods
    }

    @Test
    void incompleteClass_inlineCast() {
        String text = """
                import java.util.ArrayList;
                
                class Foo {
                    rule R1 {
                       var a : /as,
                       do { list#ArrayList#.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after 'list#ArrayList#.'
        caretPosition.setLine(5);
        caretPosition.setCharacter(28);
        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("trimToSize");
        assertThat(completionItemStrings(result)).doesNotContain("removeRange"); // 'removeRange' is a protected method, so not included in suggestions

    }

    @Test
    void incompleteClass_BigDecimalLiteral() {
        String text = """
                class Foo {
                    rule R1 {
                       var a : /as,
                       do { 10.5B.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after '10.5B..'
        caretPosition.setLine(3);
        caretPosition.setCharacter(18);
        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("precision");
    }

    @Test
    void incompleteClass_PropertyAccessor() {
        String text = """
                import org.mvel3.domain.Person;
                import org.mvel3.domain.Address;
                
                class Foo {
                    rule R1 {
                        var a : /as,
                        do {
                            Person p = new Person("John", new Address("Tokyo"));
                            p.address.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after '10.5B..'
        caretPosition.setLine(8);
        caretPosition.setCharacter(22);
        result = Mvel3CompletionHelper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("city", "getCity", "setCity"); // `city` can be directly accessed in mvel
    }
}
