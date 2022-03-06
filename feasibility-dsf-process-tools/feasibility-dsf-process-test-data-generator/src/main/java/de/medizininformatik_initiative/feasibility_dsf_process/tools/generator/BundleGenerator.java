package de.medizininformatik_initiative.feasibility_dsf_process.tools.generator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.highmed.dsf.fhir.service.ReferenceCleaner;
import org.highmed.dsf.fhir.service.ReferenceCleanerImpl;
import org.highmed.dsf.fhir.service.ReferenceExtractor;
import org.highmed.dsf.fhir.service.ReferenceExtractorImpl;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class BundleGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

    private final FhirContext fhirContext = FhirContext.forR4();
    private final ReferenceExtractor extractor = new ReferenceExtractorImpl();
    private final ReferenceCleaner cleaner = new ReferenceCleanerImpl(extractor);

    private Bundle dic1Bundle;
    private Bundle dic2Bundle;
    private Bundle zarsBundle;

    private Bundle readAndCleanBundle(Path bundleTemplateFile) {
        try (InputStream in = Files.newInputStream(bundleTemplateFile)) {
            Bundle bundle = newXmlParser().parseResource(Bundle.class, in);

            // FIXME hapi parser can't handle embedded resources and creates them while parsing bundles
            return cleaner.cleanReferenceResourcesIfBundle(bundle);
        } catch (IOException e) {
            logger.error("Error while reading bundle from " + bundleTemplateFile, e);
            throw new RuntimeException(e);
        }
    }

    private void writeBundle(Path bundleFile, Bundle bundle) {
        try (OutputStream out = Files.newOutputStream(bundleFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            newXmlParser().encodeResourceToWriter(bundle, writer);
        } catch (IOException e) {
            logger.error("Error while writing bundle to " + bundleFile, e);
            throw new RuntimeException(e);
        }
    }

    private IParser newXmlParser() {
        IParser parser = fhirContext.newXmlParser();
        parser.setStripVersionsFromReferences(false);
        parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
        parser.setPrettyPrint(true);
        return parser;
    }

    public void createDockerTestBundles(Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName) {
        createDockerTestDic1Bundle(clientCertificateFilesByCommonName);
        createDockerTestDic2Bundle(clientCertificateFilesByCommonName);
        createDockerTestZarsBundle(clientCertificateFilesByCommonName);
    }

    private void createDockerTestDic1Bundle(Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName) {
        Path bundleTemplateFile = Paths.get("src/main/resources/bundle-templates/dic-1-bundle.xml");

        dic1Bundle = readAndCleanBundle(bundleTemplateFile);

        Organization organizationZars = (Organization) dic1Bundle.getEntry().get(0).getResource();
        setThumbprint(organizationZars, clientCertificateFilesByCommonName.get("zars-client"));

        Organization organizationDic1 = (Organization) dic1Bundle.getEntry().get(1).getResource();
        setThumbprint(organizationDic1, clientCertificateFilesByCommonName.get("dic-1-client"));

        writeBundle(Paths.get("bundle/dic-1-bundle.xml"), dic1Bundle);
    }

    private void setThumbprint(Organization organization, CertificateGenerator.CertificateFiles files) {
        organization
                .getExtensionByUrl("http://highmed.org/fhir/StructureDefinition/extension-certificate-thumbprint")
                .setValue(new StringType(files.getCertificateSha512ThumbprintHex()));
    }

    private void createDockerTestDic2Bundle(Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName) {
        Path bundleTemplateFile = Paths.get("src/main/resources/bundle-templates/dic-2-bundle.xml");

        dic2Bundle = readAndCleanBundle(bundleTemplateFile);

        Organization organizationZars = (Organization) dic2Bundle.getEntry().get(0).getResource();
        setThumbprint(organizationZars, clientCertificateFilesByCommonName.get("zars-client"));

        Organization organizationDic2 = (Organization) dic2Bundle.getEntry().get(1).getResource();
        setThumbprint(organizationDic2, clientCertificateFilesByCommonName.get("dic-2-client"));

        writeBundle(Paths.get("bundle/dic-2-bundle.xml"), dic2Bundle);
    }

    private void createDockerTestZarsBundle(Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName) {
        Path bundleTemplateFile = Paths.get("src/main/resources/bundle-templates/zars-bundle.xml");

        zarsBundle = readAndCleanBundle(bundleTemplateFile);

        Organization organizationZars = (Organization) zarsBundle.getEntry().get(0).getResource();
        setThumbprint(organizationZars, clientCertificateFilesByCommonName.get("zars-client"));

        Organization organizationDic1 = (Organization) zarsBundle.getEntry().get(1).getResource();
        setThumbprint(organizationDic1, clientCertificateFilesByCommonName.get("dic-1-client"));

        Organization organizationDic2 = (Organization) zarsBundle.getEntry().get(2).getResource();
        setThumbprint(organizationDic2, clientCertificateFilesByCommonName.get("dic-2-client"));

        writeBundle(Paths.get("bundle/zars-bundle.xml"), zarsBundle);
    }

    public void copyDockerTestBundles() {
        Path dic1BundleFile = Paths.get("../../feasibility-dsf-process-docker-test-setup/dic-1/fhir/conf/bundle.xml");
        logger.info("Copying fhir bundle to {}", dic1BundleFile);
        writeBundle(dic1BundleFile, dic1Bundle);

        Path dic2BundleFile = Paths.get("../../feasibility-dsf-process-docker-test-setup/dic-2/fhir/conf/bundle.xml");
        logger.info("Copying fhir bundle to {}", dic2BundleFile);
        writeBundle(dic2BundleFile, dic2Bundle);

        Path dic3BundleFile = Paths.get("../../feasibility-dsf-process-docker-test-setup/zars/fhir/conf/bundle.xml");
        logger.info("Copying fhir bundle to {}", dic3BundleFile);
        writeBundle(dic3BundleFile, zarsBundle);

    }
}
