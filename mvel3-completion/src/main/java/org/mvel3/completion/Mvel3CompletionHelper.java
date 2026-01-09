package org.mvel3.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder;
import com.vmware.antlr4c3.CodeCompletionCore;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.mvel3.parser.antlr4.Mvel3Lexer;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.TolerantMvel3ToJavaParserVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mvel3CompletionHelper {

    private static final Logger logger = LoggerFactory.getLogger(Mvel3CompletionHelper.class);

    private static final Set<Integer> PREFERRED_RULES = Set.of(
            Mvel3Parser.RULE_typeIdentifier
    );

    private Mvel3CompletionHelper() {
    }

    // TODO: create wrapper methods to complement text for the context (e.g. expression, block, etc.)
    public static List<CompletionItem> getCompletionItemsAsBlock(String text, Position caretPosition) {
        String blockString =  "{ " + text + " }";
        caretPosition.setCharacter(caretPosition.getCharacter() + "{ ".length());
        return getCompletionItems(blockString, caretPosition);
    }

    // This method takes the valid (adjusted) text and caret position
    public static List<CompletionItem> getCompletionItems(String text, Position caretPosition) {
        Mvel3Parser parser = createMvel3Parser(text);

        int row = caretPosition == null ? -1 : caretPosition.getLine() + 1;
        int col = caretPosition == null ? -1 : caretPosition.getCharacter();

        ParseTree parseTree = parser.block(); // consider block/expression or other?
        Integer caretTokenIndex = computeTokenIndex(parser, row, col);

        return getCompletionItems(parser, caretTokenIndex, parseTree);
    }


    static List<CompletionItem> getCompletionItems(Mvel3Parser parser, int caretTokenIndex, ParseTree parseTree) {
        CodeCompletionCore core = new CodeCompletionCore(parser, PREFERRED_RULES, Tokens.IGNORED);
        CodeCompletionCore.CandidatesCollection candidates = core.collectCandidates(caretTokenIndex, (ParserRuleContext) parseTree);

        if (candidates.rules.size() > 0) {
            return processPrefferedRules(candidates, parser, parseTree, caretTokenIndex);
        }

        return candidates.tokens.keySet().stream()
                .filter(Objects::nonNull)
                .map(integer -> parser.getVocabulary().getDisplayName(integer).replace("'", ""))
                .map(String::toLowerCase)
                .map(k -> createCompletionItem(k, CompletionItemKind.Keyword))
                .collect(Collectors.toList());
    }

    private static List<CompletionItem> processPrefferedRules(CodeCompletionCore.CandidatesCollection candidates, Mvel3Parser parser, ParseTree parseTree, int caretTokenIndex) {
        List<CompletionItem> items = new ArrayList<>();
        for ( Map.Entry<Integer, java.util.List<Integer>> ruleEntry : candidates.rules.entrySet()) {
            Integer ruleIndex = ruleEntry.getKey();
            switch (ruleIndex) {
                case Mvel3Parser.RULE_typeIdentifier:
                    items.add(createCompletionItem("ArrayList", CompletionItemKind.Text));
                    break;
                default:
                    // no-op
            }
        }
        return items;
    }

    private static List<CompletionItem> createSemanticCompletions(Mvel3Parser parser, ParseTree parseTree, int caretTokenIndex) {

        logger.info("createSemanticCompletions");

        List<CompletionItem> semanticItems = new ArrayList<>();

        // caret is waiting on completion, check a previous token
        int previousTokenIndex = caretTokenIndex - 1;

        Token token = parser.getTokenStream().get(previousTokenIndex);

        logger.info("previousToken : [" + token.getText() + "]");

        if (token.getType() == Mvel3Lexer.DOT) {
            // Let's assume the user is typing a method or field access
            int scopeTokenIndex = previousTokenIndex - 1;

            TolerantMvel3ToJavaParserVisitor visitor = new TolerantMvel3ToJavaParserVisitor();
            CompilationUnit compilationUnit = (CompilationUnit) visitor.visit(parseTree);

            // We can adjust the paths for the vscode project where the user is working (e.g. dependencies by pom.xml)
            TypeSolver typeSolver = new TypeSolverBuilder()
                    .withCurrentClassloader() // equivalent to ReflectionTypeSolver
                    .withSourceCode("src/main/java") // project source code
                    .build();

            JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);
            solver.inject(compilationUnit);

            Map<Integer, Node> tokenIdJPNodeMap = visitor.getTokenIdJPNodeMap();
            Expression scopeNode = (Expression) tokenIdJPNodeMap.get(scopeTokenIndex);
            if (scopeNode == null) {
                logger.info("scopeNode is null");
            } else {
                logger.info("scopeNode: " + scopeNode.getClass() + " , text => [" + scopeNode.toString() + "]");

                // Use the symbol solver to resolve the scope node
                ResolvedType resolvedType = scopeNode.calculateResolvedType();

                // Populate semantic items with the resolved type's fields and methods
                semanticItems.addAll(createTypeBasedCompletions(resolvedType));
            }
        }

        if (semanticItems.isEmpty()) {
            semanticItems.add(createCompletionItem("IDENTIFIER", CompletionItemKind.Text));
        }
        return semanticItems;
    }

    static CompletionItem createCompletionItem(String label, CompletionItemKind itemKind) {
        CompletionItem completionItem = new CompletionItem();
        completionItem.setInsertText(label);
        completionItem.setLabel(label);
        completionItem.setKind(itemKind);
        return completionItem;
    }

    private static Mvel3Parser createMvel3Parser(String text) {
        ANTLRInputStream input = new ANTLRInputStream(text);
        Mvel3Lexer lexer = new Mvel3Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new Mvel3Parser(tokens);
    }

    private static Integer computeTokenIndex(Mvel3Parser parser, int row, int col) {
        CommonTokenStream tokens = (CommonTokenStream) parser.getTokenStream();
        int tokenIndex = 0;

        for (Token token : tokens.getTokens()) {
            if (token.getLine() > row || (token.getLine() == row && token.getCharPositionInLine() >= col)) {
                break;
            }
            tokenIndex++;
        }

        return tokenIndex;
    }

    /**
     * Create completion items based on the resolved type's members
     */
    private static List<CompletionItem> createTypeBasedCompletions(ResolvedType resolvedType) {
        List<CompletionItem> items = new ArrayList<>();

        try {
            if (resolvedType.isReferenceType()) {
                ResolvedReferenceType referenceType = resolvedType.asReferenceType();

                // Add accessible fields
                for (ResolvedFieldDeclaration field : referenceType.getAllFieldsVisibleToInheritors()) {
                    if (isAccessible(field)) {
                        CompletionItem item = createCompletionItem(field.getName(), CompletionItemKind.Field);
                        item.setDetail(field.getType().describe());
                        items.add(item);
                    }
                }

                // Add accessible methods
                referenceType.getAllMethods().stream()
                        .filter(method -> isAccessible(method))
                        .filter(method -> !method.getName().startsWith("$")) // Skip synthetic methods
                        .map(method -> method.getName())
                        .distinct()
                        // TODO: We may add detail and modify insertText, but for now keep it simple
                        .forEach(methodName -> items.add(createCompletionItem(methodName, CompletionItemKind.Method)));

                // Add direct property access for getters/setters. mvel syntax sugar
                addDirectPropertyAccess(items);

                // Add static members if it's a class type
                // Note: Check if it's a class using getTypeDeclaration()
                // For now, focusing on instance members

            } else if (resolvedType.isPrimitive()) {
                // Primitive types don't have accessible members in Java
                // Could add boxing type members here if needed
            } else if (resolvedType.isArray()) {
                // Array types have length field and some methods
                items.add(createCompletionItem("length", CompletionItemKind.Field));
            }
        } catch (Exception e) {
            // Handle resolution errors gracefully
            System.err.println("Error resolving type members: " + e.getMessage());
        }

        return items;
    }

    private static void addDirectPropertyAccess(List<CompletionItem> items) {
        // if items contain getXxx or isXxx methods, add xxx as a property access like a public field
        Set<CompletionItem> propertyNames = items.stream()
                .filter(item -> item.getKind() == CompletionItemKind.Method)
                .map(CompletionItem::getInsertText)
                .filter(name -> name.startsWith("get") || name.startsWith("is"))
                .map(name -> {
                    if (name.startsWith("get")) {
                        return name.substring(3, 4).toLowerCase() + name.substring(4);
                    } else {
                        return name.substring(2, 3).toLowerCase() + name.substring(3);
                    }
                })
                .map(propName -> createCompletionItem(propName, CompletionItemKind.Field))
                .collect(Collectors.toSet());

        items.addAll(propertyNames);
    }

    /**
     * Check if a field is accessible (public)
     */
    private static boolean isAccessible(ResolvedFieldDeclaration field) {
        try {
            if (field instanceof ReflectionFieldDeclaration reflectionField) {
                AccessSpecifier accessSpecifier = reflectionField.accessSpecifier();
                return accessSpecifier == AccessSpecifier.PUBLIC;
            }
            return true;
        } catch (Exception e) {
            return true; // Default to accessible if we can't determine
        }
    }

    /**
     * Check if a method is accessible (public)
     */
    private static boolean isAccessible(ResolvedMethodDeclaration method) {
        try {
            if (method instanceof ReflectionMethodDeclaration reflectionMethod) {
                AccessSpecifier accessSpecifier = reflectionMethod.accessSpecifier();
                return accessSpecifier == AccessSpecifier.PUBLIC;
            }
            return true;
        } catch (Exception e) {
            return true; // Default to accessible if we can't determine
        }
    }

    // convenient method. good for logging or testing
    public static List<String> completionItemStrings(List<CompletionItem> result) {
        return result.stream().map(CompletionItem::getInsertText).toList();
    }
}