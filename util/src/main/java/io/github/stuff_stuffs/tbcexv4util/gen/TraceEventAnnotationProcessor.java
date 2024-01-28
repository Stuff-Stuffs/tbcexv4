package io.github.stuff_stuffs.tbcexv4util.gen;

import com.squareup.javapoet.*;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SupportedAnnotationTypes({"io.github.stuff_stuffs.tbcexv4util.gen.GenTraceEvent"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TraceEventAnnotationProcessor extends AbstractProcessor {
    private static final Pattern PATTERN = Pattern.compile("\\{[a-zA-Z0-9]+}");

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final TypeElement annotation : annotations) {
            for (final Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() != ElementKind.METHOD) {
                    throw new UnsupportedOperationException();
                }
                final ExecutableElement executableElement = (ExecutableElement) element;
                final String packagePath;
                {
                    Element e = executableElement;
                    while (e.getAnnotation(TracePackage.class) == null) {
                        e = e.getEnclosingElement();
                        if (e == null) {
                            throw new RuntimeException();
                        }
                    }
                    packagePath = e.getAnnotation(TracePackage.class).value();
                }
                final GenTraceEvent event = executableElement.getAnnotation(GenTraceEvent.class);
                final TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packagePath, executableElement.getSimpleName().toString()));
                final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
                constructorBuilder.addModifiers(Modifier.PUBLIC);
                final MethodSpec.Builder involvesBuilder = MethodSpec.methodBuilder("involves").returns(Boolean.TYPE).addAnnotation(Override.class).addParameter(Object.class, "o", Modifier.FINAL).addModifiers(Modifier.PUBLIC);
                for (final VariableElement parameter : executableElement.getParameters()) {
                    final TypeName fieldType = TypeName.get(parameter.asType());
                    final String fieldName = parameter.getSimpleName().toString();
                    final FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PUBLIC, Modifier.FINAL);
                    constructorBuilder.addParameter(fieldType, fieldName, Modifier.FINAL);
                    for (final AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                        final TypeMirror typeMirror = mirror.getAnnotationType();
                        final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder((ClassName) TypeName.get(typeMirror));
                        fieldBuilder.addAnnotation(annotationBuilder.build());
                    }
                    final TypeKind kind = parameter.asType().getKind();
                    if (!kind.isPrimitive()) {
                        if (kind == TypeKind.DECLARED && processingEnv.getTypeUtils().directSupertypes(parameter.asType()).isEmpty()) {
                            involvesBuilder.beginControlFlow("if(o.equals(this.$N))", fieldName);
                            involvesBuilder.addStatement("return true");
                            involvesBuilder.endControlFlow();
                        } else {
                            involvesBuilder.addCode(CodeBlock.builder().beginControlFlow("if(o instanceof $T casted)", processingEnv.getTypeUtils().erasure(parameter.asType())).beginControlFlow("if(casted.equals(this.$N))", fieldName).addStatement("return true").endControlFlow().endControlFlow().build());
                        }
                    }
                    final FieldSpec fieldSpec = fieldBuilder.build();
                    builder.addField(fieldSpec);
                    constructorBuilder.addStatement("this.$1L = $1L", fieldSpec.name);
                }
                involvesBuilder.addStatement("return false");
                builder.addMethod(constructorBuilder.build());
                builder.addMethod(involvesBuilder.build());
                if (event.level() != BattleLogLevel.NONE) {
                    final MethodSpec.Builder logBuilder = MethodSpec.methodBuilder("log").addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).addParameter(BattleTracerView.class, "tracer", Modifier.FINAL).addParameter(ParameterizedTypeName.get(ClassName.get(BattleTracerView.Handle.class), TypeVariableName.get(processingEnv.getTypeUtils().getWildcardType(null, null))), "handle", Modifier.FINAL).addParameter(BattleLogContext.class, "context", Modifier.FINAL);
                    logBuilder.beginControlFlow("if(!$T.$L.enabled(context.level()))", ClassName.get(BattleLogLevel.class), event.level());
                    logBuilder.addStatement("BattleTraceEvent.super.log(tracer, handle, context)");
                    logBuilder.addStatement("return");
                    logBuilder.endControlFlow();
                    logBuilder.addCode(CodeBlock.builder().beginControlFlow("try(var messageBuilder = context.createMessage(tracer.byHandle(handle).timestamp()))").build());
                    final String format = event.format();
                    final Matcher matcher = PATTERN.matcher(format);
                    int last = 0;
                    while (matcher.find()) {
                        if (matcher.start() > last + 1) {
                            logBuilder.addStatement("messageBuilder.append(\"" + format.substring(last, matcher.start()) + "\")");
                        }
                        final String arg = format.substring(matcher.start() + 1, matcher.end() - 1);
                        logBuilder.addStatement("messageBuilder.append(this." + arg + ")");
                        last = matcher.end();
                    }
                    if (last != format.length()) {
                        logBuilder.addStatement("messageBuilder.append(\"" + format.substring(last) + "\")");
                    }
                    logBuilder.endControlFlow();
                    logBuilder.addStatement("context.pushIndent()");
                    logBuilder.addStatement("BattleTraceEvent.super.log(tracer, handle, context)");
                    logBuilder.addStatement("context.popIndent()");
                    builder.addMethod(logBuilder.build());
                }
                builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
                builder.addSuperinterface(BattleTraceEvent.class);
                final TypeSpec built = builder.build();
                final JavaFile file = JavaFile.builder(packagePath, built).indent("    ").build();
                try (final var writer = processingEnv.getFiler().createSourceFile(packagePath + "." + built.name).openWriter()) {
                    writer.write(file.toString());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}
