/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.codegenerate.stepbuilder;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;

import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext;
import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderGenerator;
import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  Step Builder class for {@link StepBuilderContext}
 * <p>
 * Context for whatever {@link StepBuilderGenerator} is currently processing.
 *
 *  @author Sergey Olefir
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class StepBuilderContextBuilder {

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builder {

        public StepBuilderContext buildStepBuilderContext();
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg6 {

        /**
         * Source constructor parameter for which we are currently generating.
         * <p>
         * Live view.
         * <p>
         * Can be null -- when processing something that is not tied to specific
         * parameter, e.g.
         * {@link StepBuilderPreprocessor#processInterfaceBuildMethod(StepBuilderContext, ClassOrInterfaceDeclaration, com.github.javaparser.ast.body.MethodDeclaration)}
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builder srcParam(@Nullable Parameter srcParam);
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5 {

        /**
         * Source constructor for which we are currently generating.
         * <p>
         * Live view.
         * <p>
         * Can be null -- in {@link StepBuilderPreprocessor#processBuilderClass(StepBuilderContext, ClassOrInterfaceDeclaration)}
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg6 srcConstructor(@Nullable ConstructorDeclaration srcConstructor);
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg4 {

        /**
         * Generated top-level builder class which we are currently generating.
         * <p>
         * Live view.
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5 generatedBuilderClass(ClassOrInterfaceDeclaration generatedBuilderClass);
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg3 {

        /**
         * Generated compilation unit which we are currently generating.
         * <p>
         * Live view.
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg4 generatedCompilationUnit(CompilationUnit generatedCompilationUnit);
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg2 {

        /**
         * Source class for which we are currently generating.
         * <p>
         * Live view.
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg3 srcClass(ClassOrInterfaceDeclaration srcClass);
    }

    public interface ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg1 {

        /**
         * Source compilation unit for which we are currently generating.
         * <p>
         * Live view.
         */
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg2 srcCompilationUnit(CompilationUnit srcCompilationUnit);
    }

    private static final class ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builderClass implements ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builder, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg6, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg4, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg3, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg2, ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg1 {

        @Nullable
        @SuppressWarnings("all")
        private Parameter srcParam;

        @Nullable
        @SuppressWarnings("all")
        private ConstructorDeclaration srcConstructor;

        @SuppressWarnings("all")
        private ClassOrInterfaceDeclaration generatedBuilderClass;

        @SuppressWarnings("all")
        private CompilationUnit generatedCompilationUnit;

        @SuppressWarnings("all")
        private ClassOrInterfaceDeclaration srcClass;

        @SuppressWarnings("all")
        private CompilationUnit srcCompilationUnit;

        /**
         * Source constructor parameter for which we are currently generating.
         * <p>
         * Live view.
         * <p>
         * Can be null -- when processing something that is not tied to specific
         * parameter, e.g.
         * {@link StepBuilderPreprocessor#processInterfaceBuildMethod(StepBuilderContext, ClassOrInterfaceDeclaration, com.github.javaparser.ast.body.MethodDeclaration)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builder srcParam(@Nullable Parameter srcParam) {
            this.srcParam = srcParam;
            return this;
        }

        /**
         * Source constructor for which we are currently generating.
         * <p>
         * Live view.
         * <p>
         * Can be null -- in {@link StepBuilderPreprocessor#processBuilderClass(StepBuilderContext, ClassOrInterfaceDeclaration)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg6 srcConstructor(@Nullable ConstructorDeclaration srcConstructor) {
            this.srcConstructor = srcConstructor;
            return this;
        }

        /**
         * Generated top-level builder class which we are currently generating.
         * <p>
         * Live view.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5 generatedBuilderClass(ClassOrInterfaceDeclaration generatedBuilderClass) {
            this.generatedBuilderClass = generatedBuilderClass;
            return this;
        }

        /**
         * Generated compilation unit which we are currently generating.
         * <p>
         * Live view.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg4 generatedCompilationUnit(CompilationUnit generatedCompilationUnit) {
            this.generatedCompilationUnit = generatedCompilationUnit;
            return this;
        }

        /**
         * Source class for which we are currently generating.
         * <p>
         * Live view.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg3 srcClass(ClassOrInterfaceDeclaration srcClass) {
            this.srcClass = srcClass;
            return this;
        }

        /**
         * Source compilation unit for which we are currently generating.
         * <p>
         * Live view.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg2 srcCompilationUnit(CompilationUnit srcCompilationUnit) {
            this.srcCompilationUnit = srcCompilationUnit;
            return this;
        }

        @Override
        public StepBuilderContext buildStepBuilderContext() {
            return new StepBuilderContext(srcCompilationUnit, srcClass, generatedCompilationUnit, generatedBuilderClass, srcConstructor, srcParam);
        }
    }

    /**
     *  FIELD COMMENT: Source compilation unit for which we are currently generating.
     *  <p>
     *  Live view.
     * <p>
     * CONSTRUCTOR COMMENT: Source compilation unit for which we are currently generating.
     *  <p>
     *  Live view.
     */
    public static ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg2 srcCompilationUnit(CompilationUnit srcCompilationUnit) {
        return new ZBSI_StepBuilderContextBuilder_srcCompilationUnit_builderClass().srcCompilationUnit(srcCompilationUnit);
    }
}
