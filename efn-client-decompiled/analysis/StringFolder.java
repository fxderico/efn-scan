import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * AST-based string de-obfuscator for the EFN stage-2 payload.
 *
 * The obfuscator rebuilds every string literal at runtime from
 *   (char)(a OP b)                      OP in + - ^
 *   new char[]{...} / new String(char[])
 *   StringBuilder sb = new StringBuilder(); sb.append('x'); ... sb.toString()
 * and sprinkles opaque predicates (int v = C; if (f(v)) { junk }).
 * This folds all of those back to literals, iterating to a fixpoint, then optionally
 * applies a rename map.
 *
 * Input should be decompiled from bytecode with debug attributes stripped (StripDebug.java):
 * the obfuscator plants bogus LocalVariableTable entries.
 *
 * Usage: java -cp javaparser-core.jar StringFolder.java <in-dir> <out-dir> [rename-map.txt]
 *   rename map lines:  oldName=newName           (global; for unique obfuscated names)
 *                      Class#oldName=newName     (only inside Class; for real names like run)
 */
public class StringFolder {

    public static void main(String[] args) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        Path in = Path.of(args[0]), out = Path.of(args[1]);
        Map<String, String> renames = new LinkedHashMap<>();
        if (args.length > 2) {
            for (String line : Files.readAllLines(Path.of(args[2]))) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] kv = line.split("=", 2);
                renames.put(kv[0].strip(), kv[1].strip());
            }
        }
        try (Stream<Path> files = Files.walk(in)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                CompilationUnit cu = StaticJavaParser.parse(p);
                String before;
                int rounds = 0;
                do {
                    before = cu.toString();
                    splitAliasedBuilders(cu);
                    hoistBuilderInit(cu);
                    foldCasts(cu);
                    foldNewString(cu);
                    inlineCharArrays(cu);
                    foldBuilders(cu);
                    foldConcat(cu);
                    foldAppendChains(cu);
                    removeDeadBuilders(cu);
                    removeOpaquePredicates(cu);
                    rounds++;
                } while (!before.equals(cu.toString()) && rounds < 50);
                if (!renames.isEmpty()) applyRenames(cu, renames);
                Path dst = out.resolve(in.relativize(p));
                Files.createDirectories(dst.getParent());
                Files.writeString(dst, cu.toString());
                System.out.println(in.relativize(p) + ": " + rounds + " rounds");
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    static Expression unwrap(Expression e) {
        while (e instanceof EnclosedExpr en) e = en.getInner();
        return e;
    }

    /** Integer value of a char/int literal (after unwrapping parens / unary minus), or null. */
    static Long intVal(Expression e) {
        e = unwrap(e);
        if (e instanceof CharLiteralExpr c) return (long) c.asChar();
        if (e instanceof IntegerLiteralExpr i) return i.asNumber().longValue();
        if (e instanceof LongLiteralExpr l) return l.asNumber().longValue();
        if (e instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.MINUS) {
            Long v = intVal(u.getExpression());
            return v == null ? null : -v;
        }
        if (e instanceof CastExpr c && c.getType().asString().equals("char")) {
            Long v = intVal(c.getExpression());
            return v == null ? null : (v & 0xFFFF);
        }
        return null;
    }

    /** String value of a string/char literal, or null. */
    static String strVal(Expression e) {
        e = unwrap(e);
        if (e instanceof StringLiteralExpr s) return s.asString();
        if (e instanceof CharLiteralExpr c) return String.valueOf(c.asChar());
        return null;
    }

    static StringLiteralExpr str(String s) {
        StringLiteralExpr l = new StringLiteralExpr();
        l.setString(s);
        return l;
    }

    static CharLiteralExpr chr(char c) {
        return new CharLiteralExpr(c);
    }

    static boolean isCharArrayInit(Expression e) {
        e = unwrap(e);
        ArrayInitializerExpr init = null;
        if (e instanceof ArrayCreationExpr ac && ac.getElementType().asString().equals("char"))
            init = ac.getInitializer().orElse(null);
        else if (e instanceof ArrayInitializerExpr ai)
            init = ai;
        return init != null && init.getValues().stream().allMatch(v -> intVal(v) != null);
    }

    static String charArrayVal(Expression e) {
        e = unwrap(e);
        ArrayInitializerExpr init = e instanceof ArrayCreationExpr ac ? ac.getInitializer().get() : (ArrayInitializerExpr) e;
        StringBuilder sb = new StringBuilder();
        for (Expression v : init.getValues()) sb.append((char) (intVal(v) & 0xFFFF));
        return sb.toString();
    }

    /** All NameExpr usages of `name` inside `scope`. */
    static List<NameExpr> usages(Node scope, String name) {
        List<NameExpr> r = new ArrayList<>();
        scope.walk(NameExpr.class, n -> { if (n.getNameAsString().equals(name)) r.add(n); });
        return r;
    }

    static Optional<BlockStmt> enclosingBlock(Node n) {
        Node cur = n.getParentNode().orElse(null);
        while (cur != null && !(cur instanceof BlockStmt)) cur = cur.getParentNode().orElse(null);
        return Optional.ofNullable((BlockStmt) cur);
    }

    static int stmtIndex(BlockStmt block, Node n) {
        Node cur = n;
        while (cur.getParentNode().isPresent() && cur.getParentNode().get() != block) cur = cur.getParentNode().get();
        return identityIndex(block.getStatements(), cur);
    }

    /** NodeList.indexOf() uses structural equals(); identical statements must not collide. */
    static int identityIndex(List<? extends Node> list, Node n) {
        for (int i = 0; i < list.size(); i++) if (list.get(i) == n) return i;
        return -1;
    }

    static void removeDeclarator(VariableDeclarator vd) {
        Node parent = vd.getParentNode().orElse(null);
        if (parent instanceof VariableDeclarationExpr vde) {
            if (vde.getVariables().size() == 1) {
                vde.getParentNode().ifPresent(Node::remove);
            } else {
                vd.remove();
            }
        }
    }

    // ---------------------------------------------------------------- passes

    /** (char)(a op b) -> 'c' */
    static void foldCasts(CompilationUnit cu) {
        for (CastExpr c : cu.findAll(CastExpr.class)) {
            if (!c.getType().asString().equals("char")) continue;
            Expression inner = unwrap(c.getExpression());
            Long v = null;
            if (inner instanceof BinaryExpr b) {
                Long l = intVal(b.getLeft()), r = intVal(b.getRight());
                if (l == null || r == null) continue;
                v = switch (b.getOperator()) {
                    case PLUS -> l + r;
                    case MINUS -> l - r;
                    case XOR -> l ^ r;
                    case MULTIPLY -> l * r;
                    case BINARY_AND -> l & r;
                    case BINARY_OR -> l | r;
                    default -> null;
                };
            } else {
                v = intVal(inner);
            }
            if (v != null) c.replace(chr((char) (v & 0xFFFF)));
        }
    }

    /** new String(new char[]{...}) -> "..." */
    static void foldNewString(CompilationUnit cu) {
        for (ObjectCreationExpr oc : cu.findAll(ObjectCreationExpr.class)) {
            if (!oc.getType().asString().equals("String") || oc.getArguments().size() != 1) continue;
            Expression a = oc.getArgument(0);
            if (isCharArrayInit(a)) oc.replace(str(charArrayVal(a)));
        }
    }

    /** char[] v = {...}; ... new String(v)  ->  "..." when v is only used that way. */
    static void inlineCharArrays(CompilationUnit cu) {
        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (!vd.getType().asString().equals("char[]") || vd.getInitializer().isEmpty()) continue;
            if (!isCharArrayInit(vd.getInitializer().get())) continue;
            Optional<BlockStmt> blk = enclosingBlock(vd);
            if (blk.isEmpty()) continue;
            List<NameExpr> uses = usages(blk.get(), vd.getNameAsString());
            boolean ok = uses.stream().allMatch(u -> u.getParentNode().orElse(null) instanceof ObjectCreationExpr oc
                    && oc.getType().asString().equals("String") && oc.getArguments().size() == 1);
            if (!ok) continue;
            String val = charArrayVal(vd.getInitializer().get());
            for (NameExpr u : uses) u.getParentNode().get().replace(str(val));
            removeDeclarator(vd);
        }
    }

    /**
     * StringBuilder sb = new StringBuilder(); sb.append(lit); ... sb.toString()  ->  "..."
     * Only when every use of sb is a literal append statement or a toString() that comes
     * after all appends, so semantics are preserved.
     */
    static void foldBuilders(CompilationUnit cu) {
        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (!vd.getType().asString().equals("StringBuilder") || vd.getInitializer().isEmpty()) continue;
            Expression init = unwrap(vd.getInitializer().get());
            if (!(init instanceof ObjectCreationExpr oc) || !oc.getType().asString().equals("StringBuilder")) continue;
            String seed = "";
            if (oc.getArguments().size() == 1) {
                seed = strVal(oc.getArgument(0));
                if (seed == null) continue;
            } else if (!oc.getArguments().isEmpty()) continue;

            Optional<BlockStmt> blkOpt = enclosingBlock(vd);
            if (blkOpt.isEmpty()) continue;
            BlockStmt blk = blkOpt.get();
            String name = vd.getNameAsString();
            List<NameExpr> uses = usages(blk, name);
            if (uses.isEmpty()) continue;

            List<MethodCallExpr> appends = new ArrayList<>(), reads = new ArrayList<>();
            boolean ok = true;
            for (NameExpr u : uses) {
                if (u.getParentNode().orElse(null) instanceof MethodCallExpr mc && mc.getScope().orElse(null) == u) {
                    if (mc.getNameAsString().equals("append") && mc.getArguments().size() == 1
                            && strVal(mc.getArgument(0)) != null
                            && mc.getParentNode().orElse(null) instanceof ExpressionStmt es
                            && es.getParentNode().orElse(null) == blk) {
                        appends.add(mc);
                        continue;
                    }
                    if (mc.getNameAsString().equals("toString") && mc.getArguments().isEmpty()) {
                        reads.add(mc);
                        continue;
                    }
                }
                ok = false;
                break;
            }
            if (!ok || reads.isEmpty()) continue;
            int lastAppend = appends.stream().mapToInt(a -> stmtIndex(blk, a)).max().orElse(-1);
            int firstRead = reads.stream().mapToInt(r -> stmtIndex(blk, r)).min().orElse(Integer.MAX_VALUE);
            int declIdx = stmtIndex(blk, vd);
            if (lastAppend >= firstRead || firstRead < 0 || declIdx < 0 || declIdx > firstRead) continue;

            StringBuilder val = new StringBuilder(seed);
            appends.sort(Comparator.comparingInt(a -> stmtIndex(blk, a)));
            for (MethodCallExpr a : appends) val.append(strVal(a.getArgument(0)));
            for (MethodCallExpr r : reads) r.replace(str(val.toString()));
            for (MethodCallExpr a : appends) a.getParentNode().ifPresent(Node::remove);
            removeDeclarator(vd);
        }
    }

    /** "a" + "b" -> "ab" (also char literals mixed with string literals) */
    static void foldConcat(CompilationUnit cu) {
        for (BinaryExpr b : cu.findAll(BinaryExpr.class)) {
            if (b.getOperator() != BinaryExpr.Operator.PLUS) continue;
            Expression l = unwrap(b.getLeft()), r = unwrap(b.getRight());
            boolean anyString = l instanceof StringLiteralExpr || r instanceof StringLiteralExpr;
            String ls = strVal(l), rs = strVal(r);
            if (anyString && ls != null && rs != null) b.replace(str(ls + rs));
        }
    }

    /** x.append("a").append('b')  ->  x.append("ab") */
    static void foldAppendChains(CompilationUnit cu) {
        for (MethodCallExpr outer : cu.findAll(MethodCallExpr.class)) {
            if (!outer.getNameAsString().equals("append") || outer.getArguments().size() != 1) continue;
            String o = strVal(outer.getArgument(0));
            if (o == null) continue;
            if (!(outer.getScope().orElse(null) instanceof MethodCallExpr inner)) continue;
            if (!inner.getNameAsString().equals("append") || inner.getArguments().size() != 1) continue;
            String i = strVal(inner.getArgument(0));
            if (i == null) continue;
            inner.setArgument(0, str(i + o));
            outer.replace(inner);
        }
    }

    /** StringBuilder x = new StringBuilder(); with no remaining uses -> removed */
    static void removeDeadBuilders(CompilationUnit cu) {
        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (!vd.getType().asString().equals("StringBuilder")) continue;
            Expression init = vd.getInitializer().map(StringFolder::unwrap).orElse(null);
            if (init != null && !(init instanceof ObjectCreationExpr)) continue;
            Optional<BlockStmt> blk = enclosingBlock(vd);
            if (blk.isPresent() && usages(blk.get(), vd.getNameAsString()).isEmpty()) removeDeclarator(vd);
        }
    }

    /**
     * Opaque predicates: [int v = C;] if (cond) { junk }  -> removed.
     *
     * Removal is semantics-preserving regardless of what cond evaluates to, because we require:
     *  - no else branch; cond and body contain no calls, object/array creation, field access,
     *    or array access (nothing that can throw or have side effects besides arithmetic);
     *  - the body only touches locals it declares itself, plus the optional seed local v;
     *  - v (if present) is declared by the statement right before the if, initialized with a
     *    literal, and not referenced anywhere after the if.
     * Note: arithmetic on int/long can't throw except integer division, which we also reject.
     */
    static void removeOpaquePredicates(CompilationUnit cu) {
        for (IfStmt ifs : cu.findAll(IfStmt.class)) {
            if (ifs.getElseStmt().isPresent()) continue;
            Node parent = ifs.getParentNode().orElse(null);
            NodeList<Statement> stmts = parent instanceof BlockStmt b ? b.getStatements()
                    : parent instanceof SwitchEntry se ? se.getStatements() : null;
            if (stmts == null) continue;
            Expression cond = ifs.getCondition();
            Statement body = ifs.getThenStmt();
            if (!sideEffectFree(cond) || !sideEffectFree(body)) continue;

            Set<String> condNames = new HashSet<>();
            cond.walk(NameExpr.class, n -> condNames.add(n.getNameAsString()));
            if (condNames.size() > 1) continue;

            Set<String> allowed = new HashSet<>();
            body.walk(VariableDeclarator.class, v -> allowed.add(v.getNameAsString()));
            Statement seedDecl = null;
            if (condNames.size() == 1) {
                String v = condNames.iterator().next();
                int idx = identityIndex(stmts, ifs);
                if (idx < 1) continue;
                Statement prev = stmts.get(idx - 1);
                if (!(prev instanceof ExpressionStmt pes) || !(pes.getExpression() instanceof VariableDeclarationExpr pvde)) continue;
                if (pvde.getVariables().size() != 1) continue;
                VariableDeclarator pv = pvde.getVariable(0);
                if (!pv.getNameAsString().equals(v) || pv.getInitializer().isEmpty() || intVal(pv.getInitializer().get()) == null) continue;
                // v must not be used after the if
                boolean usedAfter = false;
                for (int i = idx + 1; i < stmts.size() && !usedAfter; i++)
                    usedAfter = !usages(stmts.get(i), v).isEmpty();
                if (usedAfter) continue;
                allowed.add(v);
                seedDecl = prev;
            }
            boolean pure = true;
            for (NameExpr n : body.findAll(NameExpr.class)) {
                if (!allowed.contains(n.getNameAsString())) { pure = false; break; }
            }
            if (!pure) continue;
            ifs.remove();
            if (seedDecl != null) seedDecl.remove();
        }
    }

    static boolean sideEffectFree(Node n) {
        if (!n.findAll(MethodCallExpr.class).isEmpty()) return false;
        if (!n.findAll(ObjectCreationExpr.class).isEmpty()) return false;
        if (!n.findAll(ArrayCreationExpr.class).isEmpty()) return false;
        if (!n.findAll(FieldAccessExpr.class).isEmpty()) return false;
        if (!n.findAll(ArrayAccessExpr.class).isEmpty()) return false;
        if (!n.findAll(LambdaExpr.class).isEmpty()) return false;
        if (!n.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).isEmpty()) return false;
        if (!n.findAll(com.github.javaparser.ast.stmt.BreakStmt.class).isEmpty()) return false;
        if (!n.findAll(com.github.javaparser.ast.stmt.ContinueStmt.class).isEmpty()) return false;
        if (!n.findAll(com.github.javaparser.ast.stmt.ThrowStmt.class).isEmpty()) return false;
        for (BinaryExpr b : n.findAll(BinaryExpr.class))
            if (b.getOperator() == BinaryExpr.Operator.DIVIDE || b.getOperator() == BinaryExpr.Operator.REMAINDER) return false;
        for (AssignExpr a : n.findAll(AssignExpr.class))
            if (a.getOperator() == AssignExpr.Operator.DIVIDE || a.getOperator() == AssignExpr.Operator.REMAINDER) return false;
        return true;
    }

    /**
     * Vineflower mis-merges `new SB().append(new SB(){appends}.toString())` (two distinct objects
     * in the bytecode) into `outer = inner = new StringBuilder(); inner.append(..)...;
     * outer.append(inner.toString())`, which reads as if the builder were appended to itself.
     * Split it back into two allocations:  inner = new SB();  outer = new SB();
     * (Verified against javap: two `new java/lang/StringBuilder` + dup + <init>.)
     */
    static void splitAliasedBuilders(CompilationUnit cu) {
        // Declaration form: StringBuilder outer = inner = new StringBuilder();
        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (vd.getInitializer().isEmpty()) continue;
            if (!(unwrap(vd.getInitializer().get()) instanceof AssignExpr ae) || !isNewBuilderAssign(ae)) continue;
            if (!(vd.getParentNode().orElse(null) instanceof VariableDeclarationExpr vde)
                    || !(vde.getParentNode().orElse(null) instanceof ExpressionStmt es)
                    || !(es.getParentNode().orElse(null) instanceof BlockStmt blk)) continue;
            int idx = identityIndex(blk.getStatements(), es);
            blk.addStatement(idx, new ExpressionStmt(new AssignExpr(ae.getTarget().clone(), newBuilder(), AssignExpr.Operator.ASSIGN)));
            vd.setInitializer(newBuilder());
        }
        // Statement form: outer = inner = new StringBuilder();
        for (ExpressionStmt es : cu.findAll(ExpressionStmt.class)) {
            if (!(es.getExpression() instanceof AssignExpr outer) || outer.getOperator() != AssignExpr.Operator.ASSIGN) continue;
            if (!(unwrap(outer.getValue()) instanceof AssignExpr inner) || !isNewBuilderAssign(inner)) continue;
            if (!(es.getParentNode().orElse(null) instanceof BlockStmt blk)) continue;
            int idx = identityIndex(blk.getStatements(), es);
            blk.addStatement(idx, new ExpressionStmt(new AssignExpr(inner.getTarget().clone(), newBuilder(), AssignExpr.Operator.ASSIGN)));
            outer.setValue(newBuilder());
        }
    }

    static boolean isNewBuilderAssign(AssignExpr ae) {
        return ae.getOperator() == AssignExpr.Operator.ASSIGN && ae.getTarget() instanceof NameExpr
                && unwrap(ae.getValue()) instanceof ObjectCreationExpr oc
                && oc.getType().asString().equals("StringBuilder") && oc.getArguments().isEmpty();
    }

    static ObjectCreationExpr newBuilder() {
        return new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("StringBuilder"), new NodeList<>());
    }

    /**
     * StringBuilder b;  ...  b = new StringBuilder();   (single assignment, same block)
     *   -> StringBuilder b = new StringBuilder();  so foldBuilders can see it.
     * Safe: the allocation has no side effects and b is not read before the assignment.
     */
    static void hoistBuilderInit(CompilationUnit cu) {
        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (!vd.getType().asString().equals("StringBuilder") || vd.getInitializer().isPresent()) continue;
            Optional<BlockStmt> blkOpt = enclosingBlock(vd);
            if (blkOpt.isEmpty()) continue;
            BlockStmt blk = blkOpt.get();
            String name = vd.getNameAsString();
            List<NameExpr> uses = usages(blk, name);
            List<AssignExpr> assigns = new ArrayList<>();
            for (NameExpr u : uses)
                if (u.getParentNode().orElse(null) instanceof AssignExpr ae && ae.getTarget() == u) assigns.add(ae);
            if (assigns.size() != 1) continue;
            AssignExpr ae = assigns.get(0);
            if (!isNewBuilderAssign(ae) || !(ae.getParentNode().orElse(null) instanceof ExpressionStmt es)
                    || es.getParentNode().orElse(null) != blk) continue;
            int declIdx = stmtIndex(blk, vd), asgIdx = identityIndex(blk.getStatements(), es);
            boolean readBefore = false;
            for (NameExpr u : uses) if (u != ae.getTarget() && stmtIndex(blk, u) <= asgIdx) readBefore = true;
            if (readBefore || declIdx < 0 || declIdx > asgIdx) continue;
            es.remove();
            vd.setInitializer(newBuilder());
        }
    }

    // ---------------------------------------------------------------- renaming

    static void applyRenames(CompilationUnit cu, Map<String, String> renames) {
        for (SimpleName sn : cu.findAll(SimpleName.class)) {
            String id = sn.getIdentifier();
            String cls = sn.findAncestor(com.github.javaparser.ast.body.TypeDeclaration.class)
                    .map(t -> ((com.github.javaparser.ast.body.TypeDeclaration<?>) t).getNameAsString()).orElse("");
            String scoped = renames.get(cls + "#" + id);
            if (scoped != null && isLocalMemberRef(sn)) { sn.setIdentifier(scoped); continue; }
            String global = renames.get(id);
            if (global != null) sn.setIdentifier(global);
        }
    }

    /** Declaration of a member, or an unqualified / this-qualified call or field ref to one. */
    static boolean isLocalMemberRef(SimpleName sn) {
        Node p = sn.getParentNode().orElse(null);
        if (p instanceof com.github.javaparser.ast.body.MethodDeclaration
                || p instanceof com.github.javaparser.ast.body.VariableDeclarator vd && vd.getParentNode().orElse(null) instanceof com.github.javaparser.ast.body.FieldDeclaration)
            return true;
        if (p instanceof MethodCallExpr mc && mc.getName() == sn)
            return mc.getScope().isEmpty() || mc.getScope().get() instanceof ThisExpr;
        if (p instanceof FieldAccessExpr fa && fa.getName() == sn) return fa.getScope() instanceof ThisExpr;
        return false;
    }
}
