package com.zykajan.sample.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds System.out.println("...") statement to the beginning of annotated method via AST rewrite
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.zykajan.sample.processor.LogEntrance")
public class AddLogAnnotationProcessor extends AbstractProcessor {

    private static final Logger log = LoggerFactory.getLogger(AddLogAnnotationProcessor.class);

    private Trees trees;
    private TreeMaker make;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        // Black magic starts here, supports only javac
        Context context = ((JavacProcessingEnvironment)
                processingEnv).getContext();

        this.trees = Trees.instance(processingEnv);
        this.make = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        log.info("Running processor {}", this);
        if (!roundEnv.processingOver()) {

            for (Element methodElement: roundEnv.getElementsAnnotatedWith(LogEntrance.class)) {
                log.info("Processing: {}", methodElement);

                JCTree tree = (JCTree) trees.getTree(methodElement);
                TreeTranslator logAdder = new LogAdder();

                tree.accept(logAdder);
            }
        } else {
            log.info("Done");
        }

        return true;
    }

    private class LogAdder extends TreeTranslator {

        @Override
        public void visitBlock(JCTree.JCBlock jcBlock) {
            super.visitBlock(jcBlock);

            result = make.Block(jcBlock.flags, jcBlock.getStatements().prepend(createPrintln("Look at me")));
        }
    }

    /**
     * Creates AST for:
     *      System.out.println(message)
     */
    private JCTree.JCStatement createPrintln(String message) {
        return make.Exec(
            make.Apply(
                List.nil(), // Return value
                make.Select(
                    make.Select(
                        make.Ident(names.fromString("System")),
                        names.fromString("out")
                    ),
                    names.fromString("println")
                ),
                List.of(make.Literal(message)))
        );
    }
}
