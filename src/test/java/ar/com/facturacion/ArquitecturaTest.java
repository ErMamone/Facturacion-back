package ar.com.facturacion;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Las reglas de Clean Architecture se verifican en cada build, no quedan solo en la documentación. */
@AnalyzeClasses(packages = "ar.com.facturacion", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy("..domain..")
            .layer("Aplicacion").definedBy("..application..")
            .layer("Infraestructura").definedBy("..infrastructure..")
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura");

    @ArchTest
    static final ArchRule dominioYAplicacionSinFrameworks = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "software.amazon..",
                    "org.thymeleaf..", "com.openhtmltopdf..", "tools.jackson..", "com.fasterxml..")
            .because("el núcleo no debe depender de frameworks ni de detalles de infraestructura");

    @ArchTest
    static final ArchRule controladoresSoloUsanCasosDeUso = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.out..", "..infrastructure.persistence..")
            .because("la web entra por los puertos de entrada (casos de uso), nunca directo a servicios o repos");
}
