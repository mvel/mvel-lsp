package org.mvel3.completion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.completion.Mvel3CompletionHelper.completionItemStrings;

class Mvel3CompletionHelperIncompleteCodeTest {

    @Test
    void emptyInput() {
        Mvel3CompletionHelper helper = new Mvel3CompletionHelper();

        String text = "";
        Position caretPosition = new Position(0, 0);

        List<CompletionItem> result = helper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("package", "import", "class"); // top level statement
    }

    @Test
    void testInlineCast() {

        Mvel3CompletionHelper helper = new Mvel3CompletionHelper();
        helper.addImportedClass(ArrayList.class);

        String text = """
                package org.example;
                
                import java.util.List;
                import java.util.ArrayList;
                
                public class GeneratorEvaluator__ {
                    public void eval(List l) {
                        l#ArrayList#.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after '#ArrayList#.'
        caretPosition.setLine(7);
        caretPosition.setCharacter(21);
        result = helper.getCompletionItems(text, caretPosition);

        assertThat(completionItemStrings(result)).contains("trimToSize");
    }

    @Test
    void incompleteClass_PropertyAccessor() {
        Mvel3CompletionHelper helper = new Mvel3CompletionHelper();

        String text = """
                import org.mvel3.domain.Person;
                import org.mvel3.domain.Address;
                
                class Foo {
                    void work() {
                        Person p = new Person("John", new Address("Tokyo"));
                        p.address.
                """;

        Position caretPosition = new Position();
        List<CompletionItem> result;

        // Test completion after 'p.address.'
        caretPosition.setLine(6);
        caretPosition.setCharacter(18);
        result = helper.getCompletionItems(text, caretPosition);
        assertThat(completionItemStrings(result)).contains("city", "getCity", "setCity"); // `city` can be directly accessed in mvel
    }
}
